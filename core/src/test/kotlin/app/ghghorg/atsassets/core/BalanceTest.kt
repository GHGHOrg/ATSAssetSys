package app.ghghorg.atsassets.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate

class BalanceTest {
    private fun entry(
        date: String,
        cents: Long,
        seq: Long = 1L,
        type: CashType = CashType.DEPOSIT,
    ): CashEntry = CashEntry(
        id = "e$seq",
        date = LocalDate.parse(date),
        amountCents = cents,
        type = type,
        note = "",
        enteredSeq = seq,
    )

    private fun balance(
        entries: List<CashEntry>,
        date: String,
    ): Long = CashRules.balanceOn(entries, LocalDate.parse(date))

    @Test
    fun balanceOn_noEntries_isZero() {
        assertEquals(0L, balance(emptyList(), "2026-10-24"))
    }

    @Test
    fun balanceOn_sumsSignedEntries() {
        val entries =
            listOf(
                entry("2026-01-01", 1_000_000L, 1L, CashType.OPENING_BALANCE),
                entry("2026-01-02", -100_000L, 2L),
                entry("2026-01-03", 2_500L, 3L),
            )
        assertEquals(902_500L, balance(entries, "2026-01-03"))
    }

    @Test
    fun balanceOn_includesEntriesDatedOnTheDate() {
        val entries = listOf(entry("2026-01-01", 1_000_000L))
        assertEquals(1_000_000L, balance(entries, "2026-01-01"))
    }

    @Test
    fun balanceOn_leavesOutLaterEntries() {
        val entries =
            listOf(
                entry("2026-01-01", 1_000_000L, 1L),
                entry("2026-02-01", 50_000L, 2L),
            )
        assertEquals(1_000_000L, balance(entries, "2026-01-31"))
        assertEquals(1_050_000L, balance(entries, "2026-02-01"))
    }

    @Test
    fun balanceOn_doesNotDependOnListOrder() {
        val first = entry("2026-01-01", 1_000_000L, 1L)
        val second = entry("2026-01-05", -250_000L, 2L)
        assertEquals(balance(listOf(first, second), "2026-01-10"), balance(listOf(second, first), "2026-01-10"))
    }

    @Test
    fun netWorth_isTheCashBalanceAsOfToday() {
        val entries =
            listOf(
                entry("2026-01-01", 1_000_000L, 1L, CashType.OPENING_BALANCE),
                entry("2026-10-25", 500_000L, 2L),
            )
        val today = LocalDate.of(2026, 10, 24)
        assertEquals(1_000_000L, Valuation.netWorthCents(entries, today))
    }

    @Test
    fun netWorth_futureDatedOpeningBalance_isZeroUntilItsDate() {
        val entries = listOf(entry("2026-11-01", 1_000_000L, 1L, CashType.OPENING_BALANCE))
        assertEquals(0L, Valuation.netWorthCents(entries, LocalDate.of(2026, 10, 31)))
        assertEquals(1_000_000L, Valuation.netWorthCents(entries, LocalDate.of(2026, 11, 1)))
    }

    @Test
    fun netWorth_noEntries_isZero() {
        assertEquals(0L, Valuation.netWorthCents(emptyList(), LocalDate.of(2026, 10, 24)))
    }

    @Test
    fun balanceOn_aSumAboveTheLargestLong_throwsInsteadOfWrapping() {
        val entries =
            listOf(
                entry("2026-01-01", Long.MAX_VALUE, 1L, CashType.OPENING_BALANCE),
                entry("2026-01-02", 1L, 2L),
            )
        assertThrows(ArithmeticException::class.java) { balance(entries, "2026-01-02") }
    }

    @Test
    fun balanceOn_aSumBelowTheSmallestLong_throwsInsteadOfWrapping() {
        val entries =
            listOf(
                entry("2026-01-01", Long.MIN_VALUE, 1L),
                entry("2026-01-02", -1L, 2L),
            )
        assertThrows(ArithmeticException::class.java) { balance(entries, "2026-01-02") }
    }

    @Test
    fun balanceOn_theLargestLongItself_isStillAllowed() {
        val entries =
            listOf(
                entry("2026-01-01", Long.MAX_VALUE - 5L, 1L, CashType.OPENING_BALANCE),
                entry("2026-01-02", 5L, 2L),
            )
        assertEquals(Long.MAX_VALUE, balance(entries, "2026-01-02"))
    }
}
