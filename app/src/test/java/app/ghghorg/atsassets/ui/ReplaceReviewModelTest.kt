package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashPairResult
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.ReentryResult
import app.ghghorg.atsassets.core.RestoredLot
import app.ghghorg.atsassets.core.ReversalEffect
import app.ghghorg.atsassets.core.SoldPiece
import app.ghghorg.atsassets.core.SplitEffect
import app.ghghorg.atsassets.core.SplitPairResult
import app.ghghorg.atsassets.core.SplitRatio
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate

// 15a (CR2): the Reverse and re-enter combined review as a model, worked out without drawing the screen.
class ReplaceReviewModelTest {
    private val main = Portfolio(id = "p1", name = "Main", createdSeq = 1L)

    private fun fraction(
        numerator: Long,
        denominator: Long = 1L,
    ): Fraction = Fraction.of(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator))

    private fun transaction(
        id: String,
        type: TransactionType,
        date: String,
        quantity: String = "0",
        totalCents: Long = 0L,
        quantityRatio: SplitRatio? = null,
        split: SplitRatio? = null,
        reversesId: String? = null,
    ): Transaction = Transaction(
        id = id,
        portfolioId = "p1",
        ticker = "MA",
        type = type,
        date = LocalDate.parse(date),
        enteredSeq = 1L,
        quantity = BigDecimal(quantity),
        totalCents = totalCents,
        reversesId = reversesId,
        split = split,
        quantityRatio = quantityRatio,
    )

    private fun line(
        id: String,
        date: String,
        cents: Long,
        type: CashType = CashType.TRADE,
        note: String = "",
    ): CashEntry = CashEntry(id, LocalDate.parse(date), cents, type, note, 1L)

    private fun review(
        original: Transaction? = null,
        originalCash: CashEntry? = null,
        reversalNote: String = "",
        effect: ReversalEffect? = null,
        trade: ReentryResult.Created? = null,
        pieces: List<SoldPiece> = emptyList(),
        split: SplitPairResult.Created? = null,
        splitEffect: SplitEffect? = null,
        cash: CashPairResult.Created? = null,
        combinedCents: Long? = null,
    ): ReplaceReviewModel? = ReplaceReview.of(original, originalCash, reversalNote, effect, trade, pieces, split, splitEffect, cash, combinedCents, listOf(main))

    // CR1's Example I on the phone (14c check 3): MA's buy of 1 for $100.00 after a 1 for 3, re-entered as filled in.
    @Test
    fun aBuyPair_showsTheSharesRemoved_andTheNewBuysExactSharesAndPrice() {
        val original = transaction("b1", TransactionType.BUY, "2026-11-03", "1", 10_000L)
        val reversal = transaction("r1", TransactionType.REVERSAL, "2026-11-03", reversesId = "b1")
        val replacement = transaction("n1", TransactionType.BUY, "2026-11-03", "1", 10_000L, quantityRatio = SplitRatio(1, 3))
        val trade = ReentryResult.Created(reversal, line("cr1", "2026-11-03", 10_000L), replacement, line("cn1", "2026-11-03", -10_000L))
        val model = review(original = original, effect = ReversalEffect.OfBuy(fraction(1, 3), 10_000L), trade = trade, combinedCents = 0L)
        assertEquals(
            ReplaceReviewModel(
                reverses = ReversedEntry.Trade(TransactionType.BUY, "2026-11-03"),
                partOne = ReversalPartModel.OfBuy(sharesRemoved = "0.33333333", cashCents = 10_000L),
                partTwo = ReplacementPartModel.Buy("MA", "Main", "2026-11-03", quantity = "0.33333333", total = "$100.00", pricePerShare = "$300.00", cashCents = -10_000L),
                combinedCents = 0L,
            ),
            model,
        )
    }

    // Example G's sell, re-entered at $2,200.00: lots put back 10 + 2, realized $800.00 removed; the new sell's lots,
    // cost $1,300.00 and realized $900.00.
    @Test
    fun aSellPair_showsTheLotsPutBack_andTheNewSellsLotsCostAndRealizedGain() {
        val original = transaction("s1", TransactionType.SELL, "2026-03-01", "12", 210_000L)
        val reversal = transaction("r1", TransactionType.REVERSAL, "2026-03-01", reversesId = "s1")
        val replacement = transaction("n1", TransactionType.SELL, "2026-03-01", "12", 220_000L)
        val trade = ReentryResult.Created(reversal, line("cr1", "2026-03-01", -210_000L), replacement, line("cn1", "2026-03-01", 220_000L))
        val effect =
            ReversalEffect.OfSell(
                lots = listOf(RestoredLot("b1", LocalDate.of(2026, 1, 2), fraction(10)), RestoredLot("b2", LocalDate.of(2026, 2, 1), fraction(2))),
                cashCents = -210_000L,
                realizedRemoved = fraction(800),
            )
        val pieces =
            listOf(
                SoldPiece("b1", LocalDate.of(2026, 1, 2), fraction(10), cost = fraction(1_000), proceeds = fraction(5_500, 3)),
                SoldPiece("b2", LocalDate.of(2026, 2, 1), fraction(2), cost = fraction(300), proceeds = fraction(1_100, 3)),
            )
        val model = review(original = original, effect = effect, trade = trade, pieces = pieces, combinedCents = 10_000L)
        assertEquals(ReversalPartModel.OfSell(lotsPutBack = ShareList(listOf("10", "2"), oneShare = false), realizedRemovedCents = -80_000L, cashCents = -210_000L), model?.partOne)
        assertEquals(
            ReplacementPartModel.Sell(
                "MA",
                "Main",
                "2026-03-01",
                quantity = "12",
                total = "$2,200.00",
                lotsUsed = ShareList(listOf("10", "2"), oneShare = false),
                cost = "$1,300.00",
                realizedCents = 90_000L,
                cashCents = 220_000L,
            ),
            model?.partTwo,
        )
        assertEquals(10_000L, model?.combinedCents)
    }

    // 14b check 5: the 2 for 1 re-entered as 3 for 1: part 1 applies 1 for 2 (10 to 5), part 2 takes 5 to 15; no cash.
    @Test
    fun aSplitPair_showsTheInverseAndTheNewRatio_withTheSharesBeforeAndAfter_andNoCash() {
        val original = transaction("x1", TransactionType.SPLIT, "2026-11-01", split = SplitRatio(2, 1))
        val reversal = transaction("r1", TransactionType.REVERSAL, "2026-11-01", reversesId = "x1")
        val replacement = transaction("n1", TransactionType.SPLIT, "2026-11-01", split = SplitRatio(3, 1))
        val model =
            review(
                original = original,
                effect = ReversalEffect.OfSplit(SplitRatio(1, 2), fraction(10), fraction(5)),
                split = SplitPairResult.Created(reversal, replacement),
                splitEffect = SplitEffect(fraction(5), fraction(15)),
            )
        assertEquals(ReversedEntry.Trade(TransactionType.SPLIT, "2026-11-01"), model?.reverses)
        assertEquals(ReversalPartModel.OfSplit(inverse = SplitRatio(1, 2), before = "10", after = "5"), model?.partOne)
        assertEquals(ReplacementPartModel.Split("MA", "Main", "2026-11-01", SplitRatio(3, 1), before = "5", after = "15"), model?.partTwo)
        assertNull(model?.combinedCents)
    }

    // 14b check 3: the review must show the typed note, an empty one as empty, never the stand-in "check".
    @Test
    fun aCashPair_showsTheFixedReversalNote_andTheTypedNote_anEmptyOneIncluded() {
        val deposit = line("d1", "2026-04-20", 50_000L, CashType.DEPOSIT, "Salary")
        val reversalNote = "Reversal of the deposit of 2026-04-20 ET"
        val adjustment = line("r1", "2026-04-20", -50_000L, CashType.ADJUSTMENT, reversalNote)
        val replacement = line("n1", "2026-04-20", 5_000L, CashType.DEPOSIT, "")
        val model = review(originalCash = deposit, reversalNote = reversalNote, cash = CashPairResult.Created(adjustment, replacement), combinedCents = -45_000L)
        assertEquals(ReversedEntry.Cash(CashType.DEPOSIT, "2026-04-20"), model?.reverses)
        assertEquals(ReversalPartModel.OfCash(note = reversalNote, cashCents = -50_000L), model?.partOne)
        assertEquals(ReplacementPartModel.Cash(CashType.DEPOSIT, "2026-04-20", amountCents = 5_000L, note = ""), model?.partTwo)
        assertEquals(-45_000L, model?.combinedCents)
    }

    // Deploy finding 1: "1 share" for one lot of 1, "shares" otherwise, and "0 shares" with no lot, decided in the model.
    @Test
    fun aShareList_decidesShareOrShares_andShowsZeroWithNoLot() {
        assertEquals(ShareList(listOf("1"), oneShare = true), ShareList.of(listOf(fraction(1))))
        assertEquals(ShareList(listOf("1", "1"), oneShare = false), ShareList.of(listOf(fraction(1), fraction(1))))
        assertEquals(ShareList(listOf("2.5"), oneShare = false), ShareList.of(listOf(fraction(5, 2))))
        assertEquals(ShareList(listOf("0"), oneShare = false), ShareList.of(emptyList()))
    }

    // Deploy finding 3: while the reversal date is not a whole date, a trade's part 1 has no effect yet.
    @Test
    fun aTradePair_withNoEffectYet_hasNoPartOne_butStillNamesWhatIsReversed() {
        val original = transaction("b1", TransactionType.BUY, "2026-11-03", "1", 10_000L)
        val model = review(original = original, effect = null)
        assertEquals(ReversedEntry.Trade(TransactionType.BUY, "2026-11-03"), model?.reverses)
        assertNull(model?.partOne)
        assertNull(model?.partTwo)
    }

    @Test
    fun withNothingReplaced_thereIsNoModel() {
        assertNull(review())
    }
}
