package app.ghghorg.atsassets.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CashEntriesTest {
    private fun stored(
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

    private fun opening(
        date: String = "2026-01-01",
        cents: Long = 100_000L,
    ): CashEntry = stored(date, cents, 1L, CashType.OPENING_BALANCE)

    private fun create(
        existing: List<CashEntry>,
        type: CashType,
        date: String,
        cents: Long,
        note: String = "",
        direction: CashDirection = CashDirection.IN,
    ): CashEntryResult = CashEntries.create(existing, "new", type, LocalDate.parse(date), cents, note, direction)

    private fun created(result: CashEntryResult): CashEntry = (result as CashEntryResult.Created).entry

    private fun refusal(result: CashEntryResult): CashEntryRefusal = (result as CashEntryResult.Refused).reason

    @Test
    fun deposit_isPositiveWithTheNextSeq() {
        val entry = created(create(listOf(opening()), CashType.DEPOSIT, "2026-01-05", 50_000L, "Pay"))
        assertEquals("new", entry.id)
        assertEquals(CashType.DEPOSIT, entry.type)
        assertEquals(LocalDate.of(2026, 1, 5), entry.date)
        assertEquals(50_000L, entry.amountCents)
        assertEquals("Pay", entry.note)
        assertEquals(2L, entry.enteredSeq)
    }

    @Test
    fun withdrawal_isStoredNegative() {
        val entry = created(create(listOf(opening()), CashType.WITHDRAWAL, "2026-01-05", 30_000L))
        assertEquals(CashType.WITHDRAWAL, entry.type)
        assertEquals(-30_000L, entry.amountCents)
    }

    @Test
    fun nextSeq_isTheHighestPlusOne_whateverTheListOrder() {
        val existing = listOf(stored("2026-01-03", 1_000L, 5L), opening(), stored("2026-01-02", 1_000L, 3L))
        assertEquals(6L, created(create(existing, CashType.DEPOSIT, "2026-01-04", 1_000L)).enteredSeq)
    }

    @Test
    fun withoutAnOpeningBalance_isRefused() {
        assertEquals(CashEntryRefusal.NoOpeningBalance, refusal(create(emptyList(), CashType.DEPOSIT, "2026-01-05", 1_000L)))
    }

    @Test
    fun aDateBeforeTheOpeningBalance_isRefused_namingThatDate() {
        val result = create(listOf(opening("2026-01-10")), CashType.DEPOSIT, "2026-01-09", 1_000L)
        assertEquals(CashEntryRefusal.BeforeOpeningBalance(LocalDate.of(2026, 1, 10)), refusal(result))
    }

    @Test
    fun theOpeningBalanceDateItself_isAllowed() {
        val entry = created(create(listOf(opening("2026-01-10")), CashType.WITHDRAWAL, "2026-01-10", 1_000L))
        assertEquals(LocalDate.of(2026, 1, 10), entry.date)
    }

    @Test
    fun anAmountOfZeroOrLess_isRefused() {
        for (cents in listOf(0L, -1L)) {
            assertEquals(
                "amount $cents",
                CashEntryRefusal.AmountNotPositive,
                refusal(create(listOf(opening()), CashType.DEPOSIT, "2026-01-05", cents)),
            )
        }
    }

    @Test
    fun aNoteWithALineBreak_isRefused() {
        for (note in listOf("a\nb", "a\rb")) {
            assertEquals(
                CashEntryRefusal.NoteHasLineBreak,
                refusal(create(listOf(opening()), CashType.DEPOSIT, "2026-01-05", 1_000L, note)),
            )
        }
    }

    // Narrowed in 04 (your decision, step 2): Interest/dividend and Adjustment are allowed from 04 and are
    // covered by the tests below; the opening balance and trade lines stay refused here.
    @Test
    fun theOpeningBalanceAndTradeTypes_areRefused() {
        val others = listOf(CashType.OPENING_BALANCE, CashType.TRADE)
        for (type in others) {
            assertEquals("$type", CashEntryRefusal.TypeNotAllowed, refusal(create(listOf(opening()), type, "2026-01-05", 1_000L)))
        }
    }

    // Example C with a withdrawal in place of the buy (03 file, section 3).
    @Test
    fun exampleC_cashOnly_isRefusedOnJan10ByOneHundredDollars() {
        val existing = listOf(opening(), stored("2026-01-10", -80_000L, 2L, CashType.WITHDRAWAL))
        val result = create(existing, CashType.WITHDRAWAL, "2026-01-05", 30_000L)
        assertEquals(CashEntryRefusal.NegativeCash(LocalDate.of(2026, 1, 10), 10_000L), refusal(result))
    }

    @Test
    fun withdrawingTheWholeBalance_isAllowed() {
        val entry = created(create(listOf(opening()), CashType.WITHDRAWAL, "2026-01-05", 100_000L))
        assertEquals(-100_000L, entry.amountCents)
    }

    @Test
    fun theFirstNegativeDateIsNamed_withTheShortfallOnThatDate() {
        val existing =
            listOf(
                opening(),
                stored("2026-01-10", -95_000L, 2L, CashType.WITHDRAWAL),
                stored("2026-02-01", -2_000L, 3L, CashType.WITHDRAWAL),
            )
        val result = create(existing, CashType.WITHDRAWAL, "2026-01-05", 10_000L)
        assertEquals(CashEntryRefusal.NegativeCash(LocalDate.of(2026, 1, 10), 5_000L), refusal(result))
    }

    @Test
    fun aWithdrawalAfterADepositOnTheSameDate_countsTheDeposit() {
        val existing = listOf(opening(), stored("2026-01-05", 50_000L, 2L))
        val entry = created(create(existing, CashType.WITHDRAWAL, "2026-01-05", 120_000L))
        assertEquals(-120_000L, entry.amountCents)
    }

    // 03 decision 5: judged after every entry, so a dip inside a later date is refused even if that date ends positive.
    @Test
    fun aDipInsideALaterDate_isRefused_evenIfThatDateEndsPositive() {
        val existing =
            listOf(
                opening(),
                stored("2026-01-10", -90_000L, 2L, CashType.WITHDRAWAL),
                stored("2026-01-10", 50_000L, 3L),
            )
        val result = create(existing, CashType.WITHDRAWAL, "2026-01-05", 20_000L)
        assertEquals(CashEntryRefusal.NegativeCash(LocalDate.of(2026, 1, 10), 10_000L), refusal(result))
    }

    @Test
    fun aFutureDatedDeposit_isAllowed() {
        val entry = created(create(listOf(opening()), CashType.DEPOSIT, "2099-12-31", 1_000L))
        assertEquals(LocalDate.of(2099, 12, 31), entry.date)
    }

    @Test
    fun aBalanceAboveTheLargestLong_isRefusedAsTooLarge() {
        val existing = listOf(opening(cents = Long.MAX_VALUE - 10L))
        assertEquals(CashEntryRefusal.TooLarge, refusal(create(existing, CashType.DEPOSIT, "2026-01-05", 11L)))
    }

    @Test
    fun aBalanceOfExactlyTheLargestLong_isAllowed() {
        val existing = listOf(opening(cents = Long.MAX_VALUE - 10L))
        assertTrue(create(existing, CashType.DEPOSIT, "2026-01-05", 10L) is CashEntryResult.Created)
    }

    // Iteration 04: interest/dividend and adjustment (spec 4.2; 04 decisions 1 and 4).
    @Test
    fun interestDividend_isAdded() {
        val entry = created(create(listOf(opening()), CashType.INTEREST_DIVIDEND, "2026-01-05", 1_234L))
        assertEquals(CashType.INTEREST_DIVIDEND, entry.type)
        assertEquals(1_234L, entry.amountCents)
    }

    @Test
    fun adjustmentIn_isAdded() {
        val result = create(listOf(opening()), CashType.ADJUSTMENT, "2026-01-05", 2_000L, "Match broker balance", CashDirection.IN)
        val entry = created(result)
        assertEquals(CashType.ADJUSTMENT, entry.type)
        assertEquals(2_000L, entry.amountCents)
        assertEquals("Match broker balance", entry.note)
    }

    @Test
    fun adjustmentOut_isTakenOut() {
        val entry = created(create(listOf(opening()), CashType.ADJUSTMENT, "2026-01-05", 500L, "Fee", CashDirection.OUT))
        assertEquals(-500L, entry.amountCents)
    }

    @Test
    fun anOutgoingAdjustmentThatOverdraws_isRefusedLikeAWithdrawal() {
        val existing = listOf(opening(), stored("2026-01-10", -80_000L, 2L, CashType.WITHDRAWAL))
        val result = create(existing, CashType.ADJUSTMENT, "2026-01-05", 30_000L, "Fix", CashDirection.OUT)
        assertEquals(CashEntryRefusal.NegativeCash(LocalDate.of(2026, 1, 10), 10_000L), refusal(result))
    }

    @Test
    fun anAdjustmentWithoutANote_isRefused() {
        assertEquals(
            CashEntryRefusal.NoteRequired,
            refusal(create(listOf(opening()), CashType.ADJUSTMENT, "2026-01-05", 100L, "", CashDirection.IN)),
        )
    }

    @Test
    fun anAdjustmentWithANoteOfOnlySpaces_isRefused() {
        assertEquals(
            CashEntryRefusal.NoteRequired,
            refusal(create(listOf(opening()), CashType.ADJUSTMENT, "2026-01-05", 100L, "   ", CashDirection.IN)),
        )
    }

    @Test
    fun anAdjustmentNote_isStoredAsTyped() {
        val entry = created(create(listOf(opening()), CashType.ADJUSTMENT, "2026-01-05", 100L, "  fix  ", CashDirection.IN))
        assertEquals("  fix  ", entry.note)
    }

    @Test
    fun anAdjustmentNoteWithALineBreak_isRefusedForTheLineBreak() {
        assertEquals(
            CashEntryRefusal.NoteHasLineBreak,
            refusal(create(listOf(opening()), CashType.ADJUSTMENT, "2026-01-05", 100L, "a\nb", CashDirection.IN)),
        )
    }

    @Test
    fun theNoteIsOptionalForTheOtherTypes() {
        for (type in listOf(CashType.DEPOSIT, CashType.WITHDRAWAL, CashType.INTEREST_DIVIDEND)) {
            assertTrue("$type", create(listOf(opening()), type, "2026-01-05", 100L, "") is CashEntryResult.Created)
        }
    }

    @Test
    fun theDirectionIsIgnoredForTypesOtherThanAdjustment() {
        val existing = listOf(opening())
        assertEquals(100L, created(create(existing, CashType.DEPOSIT, "2026-01-05", 100L, "", CashDirection.OUT)).amountCents)
        assertEquals(-100L, created(create(existing, CashType.WITHDRAWAL, "2026-01-05", 100L, "", CashDirection.IN)).amountCents)
        assertEquals(
            100L,
            created(create(existing, CashType.INTEREST_DIVIDEND, "2026-01-05", 100L, "", CashDirection.OUT)).amountCents,
        )
    }
}
