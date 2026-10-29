package app.ghghorg.atsassets.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** Reversing a cash entry (spec 4.6; iteration 05). */
class CashEntriesReverseTest {
    private fun stored(
        id: String,
        date: String,
        cents: Long,
        seq: Long,
        type: CashType,
        reversesId: String? = null,
    ): CashEntry = CashEntry(
        id = id,
        date = LocalDate.parse(date),
        amountCents = cents,
        type = type,
        note = "",
        enteredSeq = seq,
        reversesId = reversesId,
    )

    // Opening $1,000 on Jan 1, then one entry of each direct type.
    private val data =
        listOf(
            stored("ob", "2026-01-01", 100_000L, 1L, CashType.OPENING_BALANCE),
            stored("dep", "2026-01-05", 50_000L, 2L, CashType.DEPOSIT),
            stored("wd", "2026-01-06", -30_000L, 3L, CashType.WITHDRAWAL),
            stored("int", "2026-01-07", 1_234L, 4L, CashType.INTEREST_DIVIDEND),
            stored("adj", "2026-01-08", -500L, 5L, CashType.ADJUSTMENT),
        )

    private fun reverse(
        existing: List<CashEntry>,
        originalId: String,
        date: String,
        note: String = "Reversal",
    ): CashEntryResult = CashEntries.reverse(existing, "rev", originalId, LocalDate.parse(date), note)

    private fun created(result: CashEntryResult): CashEntry = (result as CashEntryResult.Created).entry

    private fun refusal(result: CashEntryResult): CashEntryRefusal = (result as CashEntryResult.Refused).reason

    @Test
    fun reversingADeposit_givesTheOppositeAdjustmentLinkedToIt() {
        val entry = created(reverse(data, "dep", "2026-01-05", "Reversal of the deposit of 2026-01-05 ET"))
        assertEquals("rev", entry.id)
        assertEquals(CashType.ADJUSTMENT, entry.type)
        assertEquals(-50_000L, entry.amountCents)
        assertEquals("dep", entry.reversesId)
        assertEquals(LocalDate.of(2026, 1, 5), entry.date)
        assertEquals(6L, entry.enteredSeq)
        assertEquals("Reversal of the deposit of 2026-01-05 ET", entry.note)
    }

    @Test
    fun theAmountIsTheOppositeOfTheStoredAmount_forEachType() {
        val expected = mapOf("wd" to 30_000L, "int" to -1_234L, "adj" to 500L)
        for ((id, cents) in expected) {
            val original = data.single { it.id == id }
            assertEquals(id, cents, created(reverse(data, id, original.date.toString())).amountCents)
        }
    }

    @Test
    fun aLaterDate_isAllowed() {
        val entry = created(reverse(data, "dep", "2026-03-01"))
        assertEquals(LocalDate.of(2026, 3, 1), entry.date)
    }

    @Test
    fun aDateBeforeTheOriginal_isRefused_namingIt() {
        assertEquals(CashEntryRefusal.BeforeOriginal(LocalDate.of(2026, 1, 5)), refusal(reverse(data, "dep", "2026-01-04")))
    }

    @Test
    fun theOpeningBalance_isNotReversible() {
        assertEquals(
            CashEntryRefusal.NotReversible(NotReversibleReason.OPENING_BALANCE),
            refusal(reverse(data, "ob", "2026-01-01")),
        )
    }

    @Test
    fun aTradeLine_isNotReversible() {
        val withTrade = data + stored("tr", "2026-01-09", -10_000L, 6L, CashType.TRADE)
        assertEquals(
            CashEntryRefusal.NotReversible(NotReversibleReason.TRADE),
            refusal(reverse(withTrade, "tr", "2026-01-09")),
        )
    }

    @Test
    fun aReversal_isNotReversible() {
        val withReversal = data + stored("r1", "2026-01-05", -50_000L, 6L, CashType.ADJUSTMENT, reversesId = "dep")
        assertEquals(
            CashEntryRefusal.NotReversible(NotReversibleReason.IS_REVERSAL),
            refusal(reverse(withReversal, "r1", "2026-01-05")),
        )
    }

    @Test
    fun anEntryAlreadyReversed_isNotReversedAgain() {
        val withReversal = data + stored("r1", "2026-01-05", -50_000L, 6L, CashType.ADJUSTMENT, reversesId = "dep")
        assertEquals(
            CashEntryRefusal.NotReversible(NotReversibleReason.ALREADY_REVERSED),
            refusal(reverse(withReversal, "dep", "2026-01-05")),
        )
    }

    @Test
    fun anUnknownOriginal_isRefused() {
        assertEquals(CashEntryRefusal.OriginalNotFound, refusal(reverse(data, "nope", "2026-01-05")))
    }

    @Test
    fun aBlankNote_isRefused_andALineBreakToo() {
        assertEquals(CashEntryRefusal.NoteRequired, refusal(reverse(data, "dep", "2026-01-05", "   ")))
        assertEquals(CashEntryRefusal.NoteHasLineBreak, refusal(reverse(data, "dep", "2026-01-05", "a\nb")))
    }

    // The phone's 03 data (05 file, check 7): reversing the Jan 5 deposit leaves Jan 10 short by $300.
    @Test
    fun aReversalThatMakesLaterCashNegative_isRefusedWithTheDateAndShortfall() {
        val phone =
            listOf(
                stored("ob", "2026-01-01", 100_000L, 1L, CashType.OPENING_BALANCE),
                stored("d5", "2026-01-05", 50_000L, 2L, CashType.DEPOSIT),
                stored("w6", "2026-01-06", -50_000L, 3L, CashType.WITHDRAWAL),
                stored("w10", "2026-01-10", -80_000L, 4L, CashType.WITHDRAWAL),
                stored("d10", "2026-01-10", 5_000L, 5L, CashType.DEPOSIT),
            )
        assertEquals(
            CashEntryRefusal.NegativeCash(LocalDate.of(2026, 1, 10), 30_000L),
            refusal(reverse(phone, "d5", "2026-01-05")),
        )
    }

    @Test
    fun aReversalThatWouldOverflowTheBalance_isRefusedAsTooLarge() {
        val big =
            listOf(
                stored("ob", "2026-01-01", Long.MAX_VALUE - 10L, 1L, CashType.OPENING_BALANCE),
                stored("wd", "2026-01-05", -100L, 2L, CashType.WITHDRAWAL),
                stored("dep", "2026-01-06", 100L, 3L, CashType.DEPOSIT),
            )
        assertEquals(CashEntryRefusal.TooLarge, refusal(reverse(big, "wd", "2026-01-07")))
    }
}
