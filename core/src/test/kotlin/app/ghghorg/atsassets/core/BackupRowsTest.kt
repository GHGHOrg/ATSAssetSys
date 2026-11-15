package app.ghghorg.atsassets.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate

/** K6's reading half, step 3 (spec section 8, records, settings keys and rejection reasons; 33a): each row's own rules. */
class BackupRowsTest {
    private val header = BackupFile.COLUMNS.joinToString(",")

    private fun row(vararg cells: Pair<String, String>): String {
        val values = cells.toMap()
        return BackupFile.COLUMNS.joinToString(",") { values[it] ?: "" }
    }

    private val format = row("row_type" to "format", "value" to "1")

    private fun parse(vararg lines: String): ParsedRows {
        val read = BackupReader.read(("$header\n$format\n" + lines.joinToString("\n")).toByteArray(Charsets.UTF_8)) as FileRead.Read
        return BackupRows.parse(read.rows, setOf("finnhub"))
    }

    /** The errors of one row, which is line 3. */
    private fun errors(line: String): List<RestoreError> = parse(line).errors

    private fun error(
        column: String?,
        reason: RestoreReason,
    ): List<RestoreError> = listOf(RestoreError(3, column, reason))

    private fun cash(
        type: String,
        amount: String,
        note: String = "",
    ): String = row("row_type" to "cash", "date" to "2026-01-01", "cash_type" to type, "amount" to amount, "note" to note)

    private fun buy(vararg cells: Pair<String, String>): String {
        val base = mapOf("row_type" to "buy", "portfolio" to "Main", "date" to "2026-01-02", "ticker" to "AAPL", "quantity" to "10", "amount" to "1000.00")
        return row(*(base + cells.toMap()).toList().toTypedArray())
    }

    private fun sell(vararg cells: Pair<String, String>): String {
        val base =
            mapOf(
                "row_type" to "sell",
                "portfolio" to "Main",
                "date" to "2026-03-01",
                "ticker" to "AAPL",
                "quantity" to "12",
                "amount" to "2100.00",
                "lot_picks" to "L1:10;L2:2",
            )
        return row(*(base + cells.toMap()).toList().toTypedArray())
    }

    private fun split(vararg cells: Pair<String, String>): String {
        val base = mapOf("row_type" to "split", "portfolio" to "Main", "date" to "2026-04-01", "ticker" to "AAPL", "split_to" to "2", "split_from" to "1")
        return row(*(base + cells.toMap()).toList().toTypedArray())
    }

    private fun setting(
        key: String,
        value: String,
    ): String = row("row_type" to "setting", "key" to key, "value" to value)

    // The sample file: every row is a record, in file order, with the values section 8 gives them.
    @Test
    fun theSampleFile_givesARecordForEveryRow() {
        val read = BackupReader.read(File("../spec/example-backup.csv").readBytes()) as FileRead.Read
        val parsed = BackupRows.parse(read.rows, setOf("finnhub"))
        assertEquals(emptyList<RestoreError>(), parsed.errors)
        assertEquals(23, parsed.records.size)
        assertEquals((3..25).toList(), parsed.records.map { it.line })
        assertEquals(SettingRecord(3, "auto_lock", "5_min"), parsed.records[0])
        assertEquals(PortfolioRecord(5, "Main"), parsed.records[2])
        assertEquals(ListValueRecord(7, ClassList.SECTOR, "Technology"), parsed.records[4])
        assertEquals(SourceTextRecord(11, ClassList.SECTOR, "Technology", "Information Technology"), parsed.records[8])
        assertEquals(CashRecord(12, null, LocalDate.parse("2026-01-01"), CashType.OPENING_BALANCE, 1_000_000L, ""), parsed.records[9])
        assertEquals(BuyRecord(13, "L1", "Main", LocalDate.parse("2026-01-02"), "AAPL", BigDecimal("10"), 100_000L, null), parsed.records[10])
        assertEquals(
            SellRecord(
                16,
                null,
                "Main",
                LocalDate.parse("2026-03-01"),
                "AAPL",
                BigDecimal("12"),
                210_000L,
                listOf(Pick("L1", BigDecimal("10")), Pick("L2", BigDecimal("2"))),
            ),
            parsed.records[13],
        )
        assertEquals(SplitRecord(19, null, "Main", LocalDate.parse("2026-04-01"), "AAPL", SplitRatio(2, 1)), parsed.records[16])
        assertEquals(ReversalRecord(20, "L3", LocalDate.parse("2026-04-05"), ""), parsed.records[17])
        assertEquals(HiddenRecord(24, "Main", "OLDCO"), parsed.records[21])
        assertEquals(OverrideRecord(25, "Main", "AAPL", "Technology", "United States"), parsed.records[22])
    }

    // Every row: a missing or unknown row_type; a required value missing; a cell in a column the row type does not use.
    @Test
    fun aMissingOrUnknownRowType() {
        assertEquals(error("row_type", RestoreReason.RowTypeMissing), errors(row("portfolio" to "Main")))
        assertEquals(error("row_type", RestoreReason.RowTypeUnknown("trade")), errors(row("row_type" to "trade", "portfolio" to "Main")))
        // Case is ignored.
        assertEquals(emptyList<RestoreError>(), errors(row("row_type" to "Portfolio", "portfolio" to "Main")))
    }

    @Test
    fun aRequiredValueMissing_andACellNotUsed() {
        assertEquals(error("portfolio", RestoreReason.ValueMissing), errors(row("row_type" to "portfolio")))
        assertEquals(
            listOf(RestoreError(3, "ticker", RestoreReason.CellNotUsed), RestoreError(3, "amount", RestoreReason.CellNotUsed)),
            errors(row("row_type" to "portfolio", "portfolio" to "Main", "ticker" to "AAPL", "amount" to "1.00")),
        )
        // Section 8's own example: lot picks on a row that is not a sell.
        assertEquals(error("lot_picks", RestoreReason.CellNotUsed), errors(buy("lot_picks" to "L1:1")))
        // An id on a row that never has one.
        assertEquals(error("id", RestoreReason.CellNotUsed), errors(row("row_type" to "hidden", "id" to "H1", "portfolio" to "Main", "ticker" to "OLDCO")))
        // A reversal has no id, so no reversal can be the target of another (section 8, reasons: "another reversal").
        assertEquals(error("id", RestoreReason.CellNotUsed), errors(row("row_type" to "reversal", "id" to "R1", "reverses" to "L1", "date" to "2026-01-02")))
    }

    @Test
    fun aSecondFormatRow_isNotFirst() {
        assertEquals(error("row_type", RestoreReason.FormatNotFirst), errors(format))
        val withNote = BackupReader.read("$header\n${row("row_type" to "format", "value" to "1", "note" to "x")}\n".toByteArray()) as FileRead.Read
        assertEquals(listOf(RestoreError(2, "note", RestoreReason.CellNotUsed)), BackupRows.parse(withNote.rows, emptySet()).errors)
    }

    // Dates and numbers.
    @Test
    fun aDateNotInTheFormOrNotReal() {
        assertEquals(error("date", RestoreReason.DateForm), errors(buy("date" to "2026/01/02")))
        assertEquals(error("date", RestoreReason.DateForm), errors(buy("date" to "02.01.2026")))
        assertEquals(error("date", RestoreReason.DateForm), errors(buy("date" to "2026-1-2")))
        assertEquals(error("date", RestoreReason.DateNotReal), errors(buy("date" to "2026-02-30")))
        assertEquals(error("date", RestoreReason.DateNotReal), errors(buy("date" to "2026-13-01")))
    }

    @Test
    fun aNumberInAFormNotAllowed() {
        // Quoted, as a spreadsheet writes a cell with a comma.
        for (text in listOf("1,000.00", "\$1000", "1000%", "(1000)", "1e3", "+1000", ".5", "5.", "1 000")) {
            assertEquals(text, error("amount", RestoreReason.NumberForm), errors(buy("amount" to "\"$text\"")))
        }
        assertEquals(error("quantity", RestoreReason.NumberForm), errors(buy("quantity" to "\"1,5\"")))
    }

    @Test
    fun aQuantityOfZeroOrLess_orWithMoreThan8Decimals() {
        assertEquals(error("quantity", RestoreReason.QuantityNotPositive), errors(buy("quantity" to "0")))
        assertEquals(error("quantity", RestoreReason.QuantityNotPositive), errors(buy("quantity" to "-2")))
        assertEquals(error("quantity", RestoreReason.QuantityTooManyDecimals), errors(buy("quantity" to "0.123456789")))
        assertEquals(emptyList<RestoreError>(), errors(buy("quantity" to "0.12345678")))
    }

    @Test
    fun anAmountWithMoreThan2Decimals_orTooLarge() {
        assertEquals(error("amount", RestoreReason.AmountTooManyDecimals), errors(buy("amount" to "10.001")))
        // 33a decision 3: beyond exact cents, a reason of its own.
        assertEquals(error("amount", RestoreReason.AmountTooLarge), errors(buy("amount" to "92233720368547758.08")))
    }

    // Section 8 and 4.2: a wrong sign, a zero cash amount, a buy with a zero total; a sell may be 0.
    @Test
    fun theSigns_andTheZeros() {
        assertEquals(error("amount", RestoreReason.WrongSign), errors(cash("deposit", "-5.00")))
        assertEquals(error("amount", RestoreReason.WrongSign), errors(cash("opening_balance", "-5.00")))
        assertEquals(error("amount", RestoreReason.WrongSign), errors(cash("interest_dividend", "-5.00")))
        assertEquals(error("amount", RestoreReason.WrongSign), errors(cash("withdrawal", "5.00")))
        assertEquals(emptyList<RestoreError>(), errors(cash("withdrawal", "-5.00")))
        assertEquals(emptyList<RestoreError>(), errors(cash("adjustment", "-5.00", "Fix")))
        assertEquals(emptyList<RestoreError>(), errors(cash("adjustment", "5.00", "Fix")))
        assertEquals(error("amount", RestoreReason.CashAmountZero), errors(cash("deposit", "0.00")))
        assertEquals(error("amount", RestoreReason.CashAmountZero), errors(cash("adjustment", "-0", "Fix")))
        assertEquals(error("amount", RestoreReason.BuyTotalZero), errors(buy("amount" to "0.00")))
        assertEquals(error("amount", RestoreReason.WrongSign), errors(buy("amount" to "-1.00")))
        assertEquals(emptyList<RestoreError>(), errors(sell("amount" to "0.00")))
        assertEquals(error("amount", RestoreReason.WrongSign), errors(sell("amount" to "-1.00")))
    }

    // Ids and portfolios: a badly formed id; a portfolio name shorter than 3 characters or with no letter (4.7).
    @Test
    fun aBadlyFormedId() {
        for (id in listOf("1A", "L 1", "L.1", "-L1", "L" + "1".repeat(32))) {
            assertEquals(id, error("id", RestoreReason.IdBadlyFormed), errors(buy("id" to id)))
        }
        assertEquals(emptyList<RestoreError>(), errors(buy("id" to "a_1-B" + "2".repeat(27))))
        assertEquals(error("reverses", RestoreReason.IdBadlyFormed), errors(row("row_type" to "reversal", "date" to "2026-04-05", "reverses" to "3")))
    }

    @Test
    fun aPortfolioNameTooShort_orWithNoLetter() {
        assertEquals(error("portfolio", RestoreReason.PortfolioNameTooShort), errors(row("row_type" to "portfolio", "portfolio" to "AB")))
        assertEquals(error("portfolio", RestoreReason.PortfolioNameNoLetter), errors(row("row_type" to "portfolio", "portfolio" to "2026")))
        assertEquals(emptyList<RestoreError>(), errors(row("row_type" to "portfolio", "portfolio" to "株式口座")))
    }

    // Cash, tickers and splits.
    @Test
    fun anUnknownCashType_orTrade_andAnAdjustmentWithoutANote() {
        assertEquals(error("cash_type", RestoreReason.CashTypeUnknown), errors(cash("dividend", "5.00")))
        assertEquals(error("cash_type", RestoreReason.CashTypeTrade), errors(cash("trade", "5.00")))
        assertEquals(error("note", RestoreReason.AdjustmentWithoutNote), errors(cash("adjustment", "5.00", "   ")))
        assertEquals(emptyList<RestoreError>(), errors(cash("Deposit", "5.00")))
    }

    @Test
    fun aTickerWithBadCharacters() {
        for (ticker in listOf("AA PL", "AAPL$", "ÄPFEL", "ABCDEFGHIJKLM")) {
            assertEquals(ticker, error("ticker", RestoreReason.TickerBadCharacters), errors(buy("ticker" to ticker)))
        }
        val parsed = parse(buy("ticker" to "brk.b")).records.single() as BuyRecord
        assertEquals("BRK.B", parsed.ticker)
    }

    @Test
    fun aSplitNumberNotWholeAbove0_orTwoEqualNumbers() {
        assertEquals(error("split_to", RestoreReason.SplitNumberNotWhole), errors(split("split_to" to "1.5")))
        assertEquals(error("split_from", RestoreReason.SplitNumberNotWhole), errors(split("split_from" to "0")))
        assertEquals(error("split_from", RestoreReason.SplitNumberNotWhole), errors(split("split_from" to "-1")))
        assertEquals(error("split_from", RestoreReason.SplitNumbersEqual), errors(split("split_to" to "3", "split_from" to "3")))
        // 33a decision 3: above the app's limit, a reason of its own.
        assertEquals(error("split_to", RestoreReason.SplitNumberTooLarge), errors(split("split_to" to "1000001")))
    }

    // A re-entered buy's ratio: both numbers or neither, whole and above 0 (section 8; 4.6).
    @Test
    fun aBuysRatio_bothOrNeither() {
        assertEquals(error("split_from", RestoreReason.ValueMissing), errors(buy("split_to" to "2")))
        assertEquals(error("split_to", RestoreReason.SplitNumberNotWhole), errors(buy("split_to" to "0", "split_from" to "1")))
        // 33a decision 3: too large to keep as the ratio (14c decision 1).
        assertEquals(error("split_to", RestoreReason.BuyRatioTooLarge), errors(buy("split_to" to "2147483648", "split_from" to "1")))
        assertEquals(SplitRatio(3, 1), (parse(buy("split_to" to "3", "split_from" to "1")).records.single() as BuyRecord).ratio)
    }

    // Sells and picks: missing or badly written picks; a pick's own quantity.
    @Test
    fun missingOrBadlyWrittenPicks() {
        assertEquals(error("lot_picks", RestoreReason.ValueMissing), errors(sell("lot_picks" to "")))
        for (picks in listOf("L1", "L1:", "L1:10;", "L1:10:2", ":10", "L1=10", "1L:10", "L1:ten", "L1:10,L2:2")) {
            assertEquals(picks, error("lot_picks", RestoreReason.PicksBadlyWritten), errors(sell("lot_picks" to "\"$picks\"")))
        }
        assertEquals(error("lot_picks", RestoreReason.QuantityNotPositive), errors(sell("lot_picks" to "L1:0;L2:12")))
        assertEquals(error("lot_picks", RestoreReason.QuantityTooManyDecimals), errors(sell("lot_picks" to "L1:0.123456789")))
        val spaced = parse(sell("lot_picks" to " L1 : 10 ; l2:2 ")).records.single() as SellRecord
        assertEquals(listOf(Pick("L1", BigDecimal("10")), Pick("l2", BigDecimal("2"))), spaced.picks)
    }

    // State rows: an override with both values empty; an unknown list name.
    @Test
    fun anOverrideWithBothValuesEmpty_andAnUnknownList() {
        assertEquals(error(null, RestoreReason.OverrideBothEmpty), errors(row("row_type" to "override", "portfolio" to "Main", "ticker" to "AAPL")))
        assertEquals(error("list", RestoreReason.ListUnknown), errors(row("row_type" to "list_value", "list" to "industry", "value" to "Tech")))
        assertEquals(
            error("list", RestoreReason.ListUnknown),
            errors(row("row_type" to "source_text", "list" to "region", "value" to "Asia", "source_text" to "APAC")),
        )
        assertEquals(ListValueRecord(3, ClassList.GEOGRAPHY, "Global"), parse(row("row_type" to "list_value", "list" to "Geography", "value" to "Global")).records.single())
    }

    // Settings: an unknown key or value; a tab order that is not each tab once. Case is ignored.
    @Test
    fun anUnknownSettingKeyOrValue() {
        assertEquals(error("key", RestoreReason.SettingKeyUnknown), errors(setting("theme", "dark")))
        assertEquals(error("value", RestoreReason.SettingValueUnknown), errors(setting("auto_lock", "2_min")))
        assertEquals(error("value", RestoreReason.SettingValueUnknown), errors(setting("price_source", "yahoo")))
        assertEquals(SettingRecord(3, "auto_lock", "phone_lock_only"), parse(setting("Auto_Lock", "PHONE_LOCK_ONLY")).records.single())
        assertEquals(SettingRecord(3, "price_source", "finnhub"), parse(setting("price_source", "Finnhub")).records.single())
    }

    @Test
    fun aTabOrderThatIsNotEachTabOnce() {
        for (order in listOf("more;cash;portfolios", "more;cash;cash;overall", "more;cash;portfolios;overall;more", "more;wallet;portfolios;overall")) {
            assertEquals(order, error("value", RestoreReason.TabOrderNotEachOnce), errors(setting("tab_order", order)))
        }
        assertEquals(SettingRecord(3, "tab_order", "overall;portfolios;cash;more"), parse(setting("tab_order", "Overall; Portfolios;cash ;MORE")).records.single())
    }

    // A row with errors gives no record; what can be read of it is kept for "depends on line N" (33a decision 2).
    @Test
    fun aFailedRow_keepsWhatTheDependentsNeed() {
        val parsed = parse(buy("id" to "L9", "quantity" to "0"), row("row_type" to "portfolio", "portfolio" to "X"), cash("opening_balance", "-1.00"))
        assertTrue(parsed.records.isEmpty())
        assertEquals(
            listOf(
                FailedRow(3, RowKind.BUY, "L9", "Main", "AAPL", null),
                FailedRow(4, RowKind.PORTFOLIO, null, "X", null, null),
                FailedRow(5, RowKind.CASH, null, null, null, CashType.OPENING_BALANCE),
            ),
            parsed.failed,
        )
        // A row the reader already refused (a line break, too few cells) is failed too, with its cells read by position.
        val short = parse("buy,L7,Main")
        assertEquals(listOf(FailedRow(3, RowKind.BUY, "L7", "Main", null, null)), short.failed)
        assertEquals(listOf(RestoreError(3, null, RestoreReason.CellCount(3, 19))), short.errors)
    }
}
