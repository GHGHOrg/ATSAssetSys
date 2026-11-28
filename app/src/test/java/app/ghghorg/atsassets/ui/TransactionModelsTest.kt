package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.RestoredLot
import app.ghghorg.atsassets.core.ReversalEffect
import app.ghghorg.atsassets.core.SoldPiece
import app.ghghorg.atsassets.core.SplitEffect
import app.ghghorg.atsassets.core.SplitRatio
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

// 44a step 8 group (d) part 3 (CR2, open item 48): the split entry and review, a transaction's detail and the reversal
// screen as the screens draw them (wireframe 10b frames E to I).
class TransactionModelsTest {
    private val portfolios = listOf(Portfolio("p1", "Portfolio P", 1L))

    private fun fraction(text: String) = Fraction.of(BigDecimal(text))

    private val buy =
        Transaction(
            id = "b1",
            portfolioId = "p1",
            ticker = "AAPL",
            type = TransactionType.BUY,
            date = LocalDate.of(2026, 2, 1),
            enteredSeq = 2L,
            quantity = BigDecimal("10"),
            totalCents = 150_000L,
        )
    private val split =
        Transaction(
            id = "s1",
            portfolioId = "p1",
            ticker = "AAPL",
            type = TransactionType.SPLIT,
            date = LocalDate.of(2026, 3, 1),
            enteredSeq = 3L,
            quantity = BigDecimal.ZERO,
            totalCents = 0L,
            split = SplitRatio(2, 1),
        )

    private fun detail(
        transaction: Transaction,
        lot: app.ghghorg.atsassets.core.Lot? = null,
        pieces: List<SoldPiece> = emptyList(),
        splitEffect: SplitEffect? = null,
        shownEffect: ReversalEffect? = null,
        cashCents: Long? = null,
        original: Transaction? = null,
        reversal: Transaction? = null,
    ) = TransactionDetails.of(transaction, listOf(buy, split), portfolios, lot, pieces, splitEffect, shownEffect, cashCents, original, reversal, true, true)

    @Test
    fun splitEntry_hasThePickLists_andWhileReplacing_theOriginalRatioOnlyWhenItDiffers() {
        val form = SplitForm(portfolioId = "p1", ticker = "AAPL", dateText = "20260301", toText = "2", fromText = "1")
        val plain = SplitEntry.of(form, portfolios, listOf("AAPL"), original = null)
        assertEquals(listOf(PickOption("p1", "Portfolio P")), plain.portfolios)
        assertEquals("Portfolio P", plain.portfolio)
        assertNull(plain.banner)
        val same = SplitEntry.of(form, portfolios, listOf("AAPL"), original = split)
        assertEquals(SplitBannerModel("2026-03-01", SplitRatio(2, 1), "AAPL"), same.banner)
        assertNull(same.originalRatio)
        assertEquals(OriginalHints(), same.originals)
        val changed = SplitEntry.of(form.copy(toText = "3", dateText = "20260302"), portfolios, listOf("AAPL"), original = split)
        assertEquals(SplitRatio(2, 1), changed.originalRatio)
        assertEquals(OriginalHints(date = OriginalValue("2026-03-01")), changed.originals)
    }

    @Test
    fun splitReview_showsTheSharesBeforeAndAfter() {
        assertEquals(
            SplitReviewModel("Portfolio P", "AAPL", "2026-03-01", SplitRatio(2, 1), "10", "20"),
            SplitReview.of(split, SplitEffect(fraction("10"), fraction("20")), portfolios),
        )
    }

    @Test
    fun aBuysDetail_showsItAsTraded_itsLot_itsCashLine_andNotReversed() {
        val model = detail(buy, cashCents = -150_000L)
        assertEquals(TradeTitle.BUY, model.title)
        assertEquals("2026-02-01", model.date)
        assertEquals("Portfolio P", model.portfolio)
        // No lot as of today: the buy is after today, so "10 from 2026-02-01".
        assertEquals(TransactionBodyModel.Trade("10", "$1,500.00", "$150.00", LotLineModel.From("10", "2026-02-01"), null), model.body)
        assertEquals(-150_000L, model.cashCents)
        assertEquals(TransactionStatusModel.NotReversed(reverseOffered = true, reenterOffered = true), model.status)
    }

    @Test
    fun aSellsDetail_showsItsLotsUsed_byLargestRemainder() {
        val sell = buy.copy(id = "x1", type = TransactionType.SELL, date = LocalDate.of(2026, 3, 1), quantity = BigDecimal("2"), totalCents = 35_000L)
        val model = detail(sell, pieces = listOf(SoldPiece("b1", buy.date, fraction("2"), fraction("300"), fraction("350"))))
        val body = model.body as TransactionBodyModel.Trade
        assertNull(body.lot)
        assertEquals(SoldPiecesModel(listOf(SoldPieceModel("2026-02-01", SharesModel("2", false), "$300.00", "$350.00", 5_000L)), "$300.00", 5_000L), body.sold)
    }

    @Test
    fun aSplitsDetail_showsItsRatioAndShares_andNoCash() {
        val model = detail(split, splitEffect = SplitEffect(fraction("10"), fraction("20")))
        assertEquals(TransactionBodyModel.Split(SplitRatio(2, 1), "10", "20"), model.body)
        assertNull(model.cashCents)
    }

    @Test
    fun aReversalsDetail_namesItsOriginal_andAReversedOneItsReversal() {
        val reversal = buy.copy(id = "r1", type = TransactionType.REVERSAL, date = LocalDate.of(2026, 4, 5), reversesId = "b1")
        val ofIt = detail(reversal, shownEffect = ReversalEffect.OfBuy(fraction("10"), 150_000L), original = buy)
        assertEquals(TradeTitle.BUY, detail(buy).title)
        assertEquals(TransactionStatusModel.ReversalOf(TransactionType.BUY, "2026-02-01"), ofIt.status)
        assertEquals(TransactionBodyModel.Reversal(ReversalEffectModel.OfBuy("10", 150_000L)), ofIt.body)
        assertEquals(TransactionStatusModel.ReversedOn("2026-04-05"), detail(buy, reversal = reversal).status)
    }

    @Test
    fun aSellsReversalEffect_showsTheLotsPutBack_andTheGainRemovedInCents() {
        val effect = ReversalEffect.OfSell(listOf(RestoredLot("b1", buy.date, fraction("1"))), -35_000L, fraction("-50.005"))
        assertEquals(
            ReversalEffectModel.OfSell(listOf(RestoredLotModel("2026-02-01", SharesModel("1", true))), -35_000L, -5_001L),
            ReversalEffects.of(effect),
        )
    }

    @Test
    fun theReversalScreen_showsTheOriginal_andTheEffectOnceThereIsOne() {
        val model = ReversalScreens.of(buy, portfolios, null)
        assertEquals(ReversalScreenModel(TransactionType.BUY, "AAPL", "2026-02-01", "Portfolio P", null, SharesModel("10", false), "$1,500.00", null), model)
        val withEffect = ReversalScreens.of(split, portfolios, ReversalEffect.OfSplit(SplitRatio(1, 2), fraction("20"), fraction("10")))
        assertEquals(SplitRatio(2, 1), withEffect.ratio)
        assertEquals(ReversalEffectModel.OfSplit(SplitRatio(1, 2), "20", "10"), withEffect.effect)
    }
}
