package app.ghghorg.atsassets.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
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

    private val overflowing =
        listOf(
            entry("2026-01-01", Long.MAX_VALUE, 1L, CashType.OPENING_BALANCE),
            entry("2026-01-02", 1L, 2L),
        )

    @Test
    fun balance_overflow_throwsInsteadOfWrapping() {
        assertThrows(ArithmeticException::class.java) { CashAccount.balanceCents(overflowing) }
    }

    @Test
    fun rows_overflow_throwsInsteadOfWrapping() {
        assertThrows(ArithmeticException::class.java) { CashAccount.rows(overflowing) }
    }

    // Iteration 06: filters (view 7). One entry of each direct type, Jan 1 to Jan 10.
    private val mixed =
        listOf(
            entry("2026-01-01", 100_000L, 1L, CashType.OPENING_BALANCE),
            entry("2026-01-05", 50_000L, 2L, CashType.DEPOSIT),
            entry("2026-01-06", -30_000L, 3L, CashType.WITHDRAWAL),
            entry("2026-01-07", 1_234L, 4L, CashType.INTEREST_DIVIDEND),
            entry("2026-01-08", -500L, 5L, CashType.ADJUSTMENT),
            entry("2026-01-10", 5_000L, 6L, CashType.DEPOSIT),
        )

    private fun ids(filter: CashFilter): List<String> = CashAccount.rows(mixed, filter).map { it.entry.id }

    private fun day(text: String): LocalDate = LocalDate.parse(text)

    @Test
    fun filter_none_givesEveryRow_asRowsDoes() {
        assertEquals(CashAccount.rows(mixed), CashAccount.rows(mixed, CashFilter()))
        assertFalse(CashFilter().isActive)
    }

    @Test
    fun filter_fromOnly_includesTheFromDate() {
        assertEquals(listOf("e6", "e5", "e4", "e3"), ids(CashFilter(from = day("2026-01-06"))))
    }

    @Test
    fun filter_toOnly_includesTheToDate() {
        assertEquals(listOf("e3", "e2", "e1"), ids(CashFilter(to = day("2026-01-06"))))
    }

    @Test
    fun filter_fromAndTo_bothEndsIncluded() {
        assertEquals(listOf("e4", "e3", "e2"), ids(CashFilter(from = day("2026-01-05"), to = day("2026-01-07"))))
    }

    @Test
    fun filter_byEachType() {
        assertEquals(listOf("e6", "e2"), ids(CashFilter(type = CashType.DEPOSIT)))
        assertEquals(listOf("e3"), ids(CashFilter(type = CashType.WITHDRAWAL)))
        assertEquals(listOf("e4"), ids(CashFilter(type = CashType.INTEREST_DIVIDEND)))
        assertEquals(listOf("e5"), ids(CashFilter(type = CashType.ADJUSTMENT)))
    }

    @Test
    fun filter_theOpeningBalanceShowsOnlyUnderAll() {
        assertTrue("e1" in ids(CashFilter()))
        for (type in listOf(CashType.DEPOSIT, CashType.WITHDRAWAL, CashType.INTEREST_DIVIDEND, CashType.ADJUSTMENT)) {
            assertFalse("$type", "e1" in ids(CashFilter(type = type)))
        }
    }

    @Test
    fun filter_typeAndDatesCombine() {
        assertEquals(listOf("e6"), ids(CashFilter(from = day("2026-01-06"), type = CashType.DEPOSIT)))
    }

    @Test
    fun filter_fromAfterTo_givesNoRows() {
        assertTrue(ids(CashFilter(from = day("2026-01-08"), to = day("2026-01-05"))).isEmpty())
    }

    @Test
    fun filter_keepsTheRealBalanceAfterEachEntry() {
        val rows = CashAccount.rows(mixed, CashFilter(type = CashType.DEPOSIT))
        assertEquals(listOf(125_734L, 150_000L), rows.map { it.balanceAfterCents })
    }

    @Test
    fun filter_isActive_withAnyBoundOrAType() {
        assertTrue(CashFilter(from = day("2026-01-01")).isActive)
        assertTrue(CashFilter(to = day("2026-01-01")).isActive)
        assertTrue(CashFilter(type = CashType.DEPOSIT).isActive)
    }
}
