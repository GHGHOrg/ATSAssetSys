package app.ghghorg.atsassets.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CashAccountTest {
    private fun entry(
        date: String,
        cents: Long,
        seq: Long,
        type: CashType = CashType.DEPOSIT,
    ): CashEntry = CashEntry(
        id = "e$seq",
        date = LocalDate.parse(date),
        amountCents = cents,
        type = type,
        note = "",
        enteredSeq = seq,
    )

    // Example A as cash lines (criterion 49): opening, two buys, one sell.
    private val exampleA =
        listOf(
            entry("2026-01-01", 1_000_000L, 1L, CashType.OPENING_BALANCE),
            entry("2026-01-02", -100_000L, 2L, CashType.TRADE),
            entry("2026-02-01", -150_000L, 3L, CashType.TRADE),
            entry("2026-03-01", 210_000L, 4L, CashType.TRADE),
        )

    @Test
    fun balance_noEntries_isZero() {
        assertEquals(0L, CashAccount.balanceCents(emptyList()))
    }

    @Test
    fun balance_isTheBalanceOnTheLatestEntryDate() {
        assertEquals(960_000L, CashAccount.balanceCents(exampleA))
    }

    @Test
    fun balance_includesFutureDatedEntries_whileNetWorthDoesNot() {
        val entries = listOf(entry("2026-11-01", 1_000_000L, 1L, CashType.OPENING_BALANCE))
        assertEquals(1_000_000L, CashAccount.balanceCents(entries))
        assertEquals(0L, Valuation.netWorthCents(entries, LocalDate.of(2026, 10, 31)))
    }

    @Test
    fun rows_noEntries_isEmpty() {
        assertEquals(emptyList<CashRow>(), CashAccount.rows(emptyList()))
    }

    @Test
    fun rows_exampleA_newestFirstWithBalanceAfter() {
        val rows = CashAccount.rows(exampleA)
        assertEquals(listOf("e4", "e3", "e2", "e1"), rows.map { it.entry.id })
        assertEquals(listOf(960_000L, 750_000L, 900_000L, 1_000_000L), rows.map { it.balanceAfterCents })
    }

    @Test
    fun rows_sameDate_laterEntryFirst_balanceFollowsEntryOrder() {
        val entries =
            listOf(
                entry("2026-01-01", 1_000_000L, 1L, CashType.OPENING_BALANCE),
                entry("2026-01-05", 20_000L, 2L),
                entry("2026-01-05", -5_000L, 3L, CashType.WITHDRAWAL),
            )
        val rows = CashAccount.rows(entries)
        assertEquals(listOf("e3", "e2", "e1"), rows.map { it.entry.id })
        assertEquals(listOf(1_015_000L, 1_020_000L, 1_000_000L), rows.map { it.balanceAfterCents })
    }

    @Test
    fun rows_doNotDependOnListOrder() {
        assertEquals(CashAccount.rows(exampleA), CashAccount.rows(exampleA.reversed()))
    }

    @Test
    fun rows_topBalanceEqualsTheAccountBalance() {
        assertEquals(CashAccount.balanceCents(exampleA), CashAccount.rows(exampleA).first().balanceAfterCents)
    }
}
