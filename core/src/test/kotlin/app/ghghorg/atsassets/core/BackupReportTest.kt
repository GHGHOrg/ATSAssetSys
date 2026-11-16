package app.ghghorg.atsassets.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * K6's reading half, step 5 (spec section 8, the report; 33a decisions 4 and 5): "N errors in M lines", the report in
 * line order, and the time a large file takes.
 */
class BackupReportTest {
    private fun context(): RestoreContext {
        var made = 0
        return RestoreContext({ "app-${++made}" }, setOf("finnhub"), "finnhub", { type, date -> "Reversal of the ${type.name.lowercase()} of $date ET" })
    }

    /** A file with [columns] as its header and each row given as its named cells. */
    private fun file(
        columns: List<String>,
        vararg rows: Map<String, String>,
    ): ByteArray {
        val lines = listOf(columns.joinToString(",")) + (listOf(mapOf("row_type" to "format", "value" to "1")) + rows).map { cells -> columns.joinToString(",") { cells[it] ?: "" } }
        return lines.joinToString("\n").toByteArray()
    }

    private val opening = mapOf("row_type" to "cash", "date" to "2026-01-01", "cash_type" to "opening_balance", "amount" to "1000.00")
    private val main = mapOf("row_type" to "portfolio", "portfolio" to "Main")
    private val buy = mapOf("row_type" to "buy", "id" to "L1", "portfolio" to "Main", "date" to "2026-01-02", "ticker" to "AAPL", "quantity" to "1", "amount" to "10.00")

    private fun failed(bytes: ByteArray): RestoreCheck.Failed = BackupCheck.check(bytes, context()) as RestoreCheck.Failed

    // Section 8: the report starts with "N errors in M lines".
    @Test
    fun theCounts_errorsAndTheLinesThatHaveThem() {
        val report =
            failed(
                file(
                    BackupFile.COLUMNS,
                    opening,
                    main,
                    buy + mapOf("date" to "2026/01/02", "quantity" to "0"),
                    buy + mapOf("id" to "L2", "amount" to "-1.00"),
                ),
            )
        assertEquals(3, report.errorCount)
        assertEquals(2, report.lineCount)
        // The file as a whole is no line.
        val none = failed(file(BackupFile.COLUMNS, main))
        assertEquals(1, none.errorCount)
        assertEquals(0, none.lineCount)
    }

    // 33a decision 4: line order; within a line, the form errors in the file's own column order, then the rule error.
    @Test
    fun theReport_isInLineOrder_withinALineInTheFilesColumnOrder() {
        val columns = BackupFile.COLUMNS.reversed()
        val report =
            failed(
                file(
                    columns,
                    opening,
                    main,
                    mapOf("row_type" to "list_value", "list" to "sector", "value" to "Tech"),
                    buy + mapOf("id" to "L2", "ticker" to "MSFT", "date" to "2026-01-05", "amount" to "5000.00"),
                    mapOf("row_type" to "override", "portfolio" to "Main", "ticker" to "AAPL", "sector" to "Energy", "geography" to "Mars"),
                    buy + mapOf("id" to "L3", "date" to "2026-01-04", "ticker" to "MS FT", "quantity" to "-1"),
                    // Replayed before line 6 (the earlier date), listed after it.
                    buy + mapOf("id" to "L4", "ticker" to "MSFT", "date" to "2026-01-01", "amount" to "2000.00"),
                    buy,
                ),
            )
        assertEquals(
            listOf(
                RestoreError(6, null, RestoreReason.CashNegative(LocalDate.of(2026, 1, 5), 401_000L)),
                // The header is reversed: geography's column comes before sector's.
                RestoreError(7, "geography", RestoreReason.OverrideValueNotOnList),
                RestoreError(7, "sector", RestoreReason.OverrideValueNotOnList),
                // Reversed too: quantity comes before ticker.
                RestoreError(8, "quantity", RestoreReason.QuantityNotPositive),
                RestoreError(8, "ticker", RestoreReason.TickerBadCharacters),
                RestoreError(9, null, RestoreReason.CashNegative(LocalDate.of(2026, 1, 1), 100_000L)),
            ),
            report.errors,
        )
    }

    @Test
    fun theFileAsAWhole_comesFirst() {
        val report = failed(file(BackupFile.COLUMNS, main, mapOf("row_type" to "portfolio", "portfolio" to "AB")))
        assertEquals(listOf(null, 4), report.errors.map { it.line })
    }

    // 33a decision 5: 3,000 transactions with their cash lines, timed once; the bound is set from the first measurement
    // (33a file, 13.1, step 5), generous enough not to fail by chance.
    @Test
    fun aFileOf3000Transactions_isCheckedInTime() {
        val rows = ArrayList<Map<String, String>>()
        rows.add(mapOf("row_type" to "cash", "date" to "2020-01-01", "cash_type" to "opening_balance", "amount" to "1000000.00"))
        rows.add(main)
        var date = LocalDate.of(2020, 1, 2)
        for (i in 1..1500) {
            val ticker = "T${i % 10}"
            rows.add(mapOf("row_type" to "buy", "id" to "L$i", "portfolio" to "Main", "date" to "$date", "ticker" to ticker, "quantity" to "10", "amount" to "100.00"))
            rows.add(
                mapOf(
                    "row_type" to "sell",
                    "portfolio" to "Main",
                    "date" to "$date",
                    "ticker" to ticker,
                    "quantity" to "10",
                    "amount" to "101.00",
                    "lot_picks" to "L$i:10",
                ),
            )
            date = date.plusDays(1)
        }
        val bytes = file(BackupFile.COLUMNS, *rows.toTypedArray())
        val start = System.nanoTime()
        val passed = BackupCheck.check(bytes, context()) as RestoreCheck.Passed
        val millis = (System.nanoTime() - start) / 1_000_000
        println("33a timing: 3,000 transactions checked in $millis ms")
        assertEquals(3_000, passed.summary.transactions)
        assertEquals(100_000_000L + 1_500 * 100L, passed.summary.cashBalanceCents)
        assertTrue("3,000 transactions took $millis ms, above the bound of $BOUND_MS ms", millis <= BOUND_MS)
    }

    private companion object {
        // Measured on this PC 2026-11-16: 1,062 ms alone, 2,230 ms in the full run. 5 s, the line at which decision 5
        // stops to ask whether the replay needs another design (your A, 2026-11-16).
        const val BOUND_MS = 5_000L
    }
}
