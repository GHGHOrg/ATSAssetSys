package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.CashDirection
import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

// 44a step 8 group (d) part 2 (CR2, open item 48): the cash entry form, its review and the cash entry detail as the
// screens draw them (wireframes 10b and 10c).
class CashEntryModelsTest {
    private val deposit = CashEntry("d1", LocalDate.of(2026, 4, 20), 50_000L, CashType.DEPOSIT, "Deposit entered by mistake", 3L)
    private val opening = CashEntry("o1", LocalDate.of(2026, 1, 1), 1_000_000L, CashType.OPENING_BALANCE, "", 1L)

    @Test
    fun theFormsTitle_isTheOpeningBalance_anEntry_orAReplacement() {
        assertEquals(CashFormTitle.OPENING, CashForms.of(FormState(), null).title)
        assertEquals(CashFormTitle.ENTRY, CashForms.of(FormState(type = CashType.DEPOSIT), null).title)
        assertEquals(CashFormTitle.REPLACING, CashForms.of(FormState(type = CashType.DEPOSIT), deposit).title)
    }

    @Test
    fun whileReplacing_theBannerShowsTheOriginal_andOnlyChangedFieldsHaveHints() {
        val same = FormState(dateText = "2026-04-20", amountText = "500", noteText = "Deposit entered by mistake", type = CashType.DEPOSIT)
        val model = CashForms.of(same, deposit)
        assertEquals(CashEntryShown(CashType.DEPOSIT, "2026-04-20", 50_000L), model.banner)
        assertEquals(CashOriginals(), model.originals)
        val changed = FormState(dateText = "2026-04-21", amountText = "50", noteText = "", type = CashType.WITHDRAWAL)
        assertEquals(
            CashOriginals(
                type = CashType.DEPOSIT,
                date = OriginalValue("2026-04-20"),
                amount = OriginalValue("$500.00", dollars = true),
                note = "Deposit entered by mistake",
            ),
            CashForms.of(changed, deposit).originals,
        )
    }

    @Test
    fun anAdjustmentsDirection_hasAHint_onlyWhileBothAreAdjustments_andItDiffers() {
        val out = CashEntry("a1", LocalDate.of(2026, 1, 10), -40_000L, CashType.ADJUSTMENT, "Match broker", 4L)
        val form = FormState(dateText = "2026-01-10", amountText = "400", noteText = "Match broker", type = CashType.ADJUSTMENT, direction = CashDirection.IN)
        assertEquals(CashOriginals(direction = CashDirection.OUT), CashForms.of(form, out).originals)
        assertEquals(CashOriginals(), CashForms.of(form.copy(direction = CashDirection.OUT), out).originals)
    }

    @Test
    fun aReplacedEntryWithNoNote_hasAnEmptyNoteHint_onceANoteIsTyped() {
        val bare = deposit.copy(note = "")
        val form = FormState(dateText = "2026-04-20", amountText = "500", noteText = "Corrected", type = CashType.DEPOSIT)
        assertEquals("", CashForms.of(form, bare).originals.note)
    }

    @Test
    fun theReview_signsAWithdrawal_andAnAdjustmentOut_asPayments() {
        assertEquals(CashReviewModel(CashType.WITHDRAWAL, "2026-01-10", -30_000L, ""), CashReviews.of(FormState(" 2026-01-10 ", "300", "", CashType.WITHDRAWAL)))
        assertEquals(-20_000L, CashReviews.of(FormState("2026-01-10", "200", "x", CashType.ADJUSTMENT, CashDirection.OUT)).cashCents)
        assertEquals(20_000L, CashReviews.of(FormState("2026-01-10", "200", "x", CashType.ADJUSTMENT, CashDirection.IN)).cashCents)
        assertNull(CashReviews.of(FormState("2026-01-10", "x", "", CashType.DEPOSIT)).cashCents)
    }

    @Test
    fun theDetail_ofTheOpeningBalance_saysItCannotBeReversed() {
        val model = CashDetails.of(opening, null, null, null, emptyList(), emptyList(), reverseOffered = false, reenterOffered = false)
        assertEquals(CashDetailTitle.Entry(CashType.OPENING_BALANCE), model.title)
        assertEquals(CashDetailStatus.OpeningNotReversible, model.status)
        assertEquals(CashEntryShown(CashType.OPENING_BALANCE, "2026-01-01", 1_000_000L), model.entry)
    }

    @Test
    fun theDetail_ofADirectEntry_linksToItsReversal_orToWhatItReverses() {
        val reversal = CashEntry("r1", LocalDate.of(2026, 4, 21), -50_000L, CashType.ADJUSTMENT, "Reversal", 5L, reversesId = "d1")
        assertEquals(CashDetailStatus.NotReversed, CashDetails.of(deposit, null, null, null, emptyList(), emptyList(), true, true).status)
        assertEquals(CashDetailStatus.ReversedBy("r1", "2026-04-21"), CashDetails.of(deposit, reversal, null, null, emptyList(), emptyList(), false, false).status)
        assertEquals(CashDetailStatus.Reverses("d1", CashType.DEPOSIT, "2026-04-20"), CashDetails.of(reversal, null, deposit, null, emptyList(), emptyList(), false, false).status)
    }

    @Test
    fun theDetail_ofATradeLine_isTitledByItsTrade_andLinksToIt() {
        val buy =
            Transaction(
                id = "b1",
                portfolioId = "p1",
                ticker = "AAPL",
                type = TransactionType.BUY,
                date = LocalDate.of(2026, 1, 2),
                enteredSeq = 2L,
                quantity = BigDecimal("10"),
                totalCents = 100_000L,
            )
        val line = CashEntry("c-b1", buy.date, -100_000L, CashType.TRADE, "", 2L, transactionId = "b1")
        val model = CashDetails.of(line, null, null, buy, listOf(buy), listOf(Portfolio("p1", "Main", 1L)), false, false)
        assertEquals(CashDetailTitle.Trade(TradeTitle.BUY, "AAPL", "Main"), model.title)
        assertEquals(CashDetailStatus.TradeLine(hint = true, TradeTitle.BUY, "AAPL", "2026-01-02"), model.status)
    }
}
