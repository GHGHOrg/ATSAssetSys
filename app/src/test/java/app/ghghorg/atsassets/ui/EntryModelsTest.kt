package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.SoldPiece
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

// 44a step 8 group (d) part 1 (CR2, open item 48): Buy and Sell, their reviews, and the replacing banner and hints as
// the screens draw them (wireframes 10a and 10c), Example A.
class EntryModelsTest {
    private val p = Portfolio("p1", "Portfolio P", 1L)
    private val main = Portfolio("p2", "Main", 2L)
    private val portfolios = listOf(p, main)

    private fun fraction(text: String) = Fraction.of(BigDecimal(text))

    private val buy =
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

    @Test
    fun theBanner_namesTheReplacedEntry_withItsShownFigures() {
        assertEquals(ReplacingBannerModel(TransactionType.BUY, "2026-01-02", "10", "AAPL", "$1,000.00"), Replacing.banner(buy))
    }

    @Test
    fun theHints_showOnlyTheFieldsThatDiffer() {
        val same = Replacing.hints(buy, portfolios, "p1", "AAPL", "20260102", "10.000", 100_000L)
        assertEquals(OriginalHints(), same)
        val changed = Replacing.hints(buy, portfolios, "p2", "MSFT", "20260103", "100", 90_000L)
        assertEquals(
            OriginalHints(
                portfolio = OriginalValue("Portfolio P"),
                ticker = OriginalValue("AAPL"),
                date = OriginalValue("2026-01-02"),
                quantity = OriginalValue("10"),
                total = OriginalValue("$1,000.00", dollars = true),
            ),
            changed,
        )
    }

    @Test
    fun theHints_showTheQuantityAndTotal_whileTheyAreNotValid() {
        val hints = Replacing.hints(buy, portfolios, "p1", "AAPL", "20260102", "", null)
        assertEquals(OriginalValue("10"), hints.quantity)
        assertEquals(OriginalValue("$1,000.00", dollars = true), hints.total)
    }

    @Test
    fun noOriginal_noHints() {
        assertEquals(OriginalHints(), Replacing.hints(null, portfolios, "p1", "AAPL", "", "", null))
    }

    @Test
    fun shares_readOneShareOnlyWhenShownAsExactlyOne() {
        assertEquals(SharesModel("1", true), Replacing.shares(fraction("1")))
        assertEquals(SharesModel("2.5", false), Replacing.shares(fraction("2.5")))
        assertEquals(SharesModel("0.00000001", false), Replacing.shares(Fraction.of(java.math.BigInteger.ONE, java.math.BigInteger.valueOf(1_000_000_000L))))
    }

    @Test
    fun buyEntry_hasThePortfolioAndItsList_theTickerInCapitals_andThePricePerShare() {
        val form = BuyForm(portfolioId = "p1", tickerText = " aapl", dateText = "20260201", quantityText = "10", totalText = "1500")
        val model = BuyEntry.of(form, portfolios, original = null, pricePerShare = fraction("150"))
        assertEquals("Portfolio P", model.portfolio)
        assertEquals(listOf(PickOption("p1", "Portfolio P"), PickOption("p2", "Main")), model.portfolios)
        assertEquals("AAPL", model.ticker)
        assertEquals("$150.00", model.pricePerShare)
        assertNull(model.banner)
        assertFalse(model.replacing)
    }

    @Test
    fun buyEntry_withNoPortfolioChosen_hasNone() {
        val model = BuyEntry.of(BuyForm(), portfolios, original = null, pricePerShare = null)
        assertNull(model.portfolio)
        assertNull(model.pricePerShare)
    }

    @Test
    fun buyEntry_whileReplacing_hasTheBanner_andTheChangedQuantitysOriginal() {
        val form = BuyForm(portfolioId = "p1", tickerText = "AAPL", dateText = "20260102", quantityText = "100", totalText = "1000")
        val model = BuyEntry.of(form, portfolios, original = buy, pricePerShare = fraction("10"))
        assertTrue(model.replacing)
        assertEquals(OriginalHints(quantity = OriginalValue("10")), model.originals)
    }

    @Test
    fun buyReview_showsTheValues_thePrice_andTheCashEffectAsAPayment() {
        val form = BuyForm(portfolioId = "p1", tickerText = "aapl", dateText = "20260201", quantityText = "10", totalText = "1500")
        assertEquals(
            BuyReviewModel("Portfolio P", "AAPL", "2026-02-01", "10", "$1,500.00", "$150.00", -150_000L),
            BuyReview.of(form, portfolios, fraction("150")),
        )
    }

    @Test
    fun sellEntry_hasTheTickerList_andThePickedLine() {
        val form = SellForm(portfolioId = "p1", ticker = "AAPL", dateText = "20260301", quantityText = "12", totalText = "2100", picks = mapOf("b1" to "10", "b2" to "2"))
        val model = SellEntry.of(form, portfolios, listOf("AAPL", "MSFT"), original = null, pricePerShare = fraction("175"), picked = BigDecimal("12"), startingPicks = emptyMap())
        assertEquals("AAPL", model.ticker)
        assertEquals(listOf("AAPL", "MSFT"), model.tickers)
        assertEquals("$175.00", model.pricePerShare)
        assertEquals(PickedLineModel(SharesModel("12", false), SharesModel("12", false), originals = false), model.picked)
    }

    @Test
    fun sellEntry_beforeAValidQuantity_thePickedLineHasNoTotal_andOneShareReadsSo() {
        val model = SellEntry.of(SellForm(quantityText = "x"), portfolios, emptyList(), null, null, BigDecimal.ONE, emptyMap())
        assertEquals(PickedLineModel(SharesModel("1", true), null, originals = false), model.picked)
    }

    @Test
    fun sellEntry_whileReplacing_saysThePicksAreTheOriginals_untilOneChanges() {
        val sell = buy.copy(id = "s1", type = TransactionType.SELL, totalCents = 210_000L)
        val picks = mapOf("b1" to "10")
        val form = SellForm(portfolioId = "p1", ticker = "AAPL", dateText = "20260102", quantityText = "10", totalText = "2100", picks = picks)
        assertTrue(SellEntry.of(form, portfolios, listOf("AAPL"), sell, null, BigDecimal.TEN, picks).picked.originals)
        val changed = form.copy(picks = mapOf("b1" to "9"))
        assertFalse(SellEntry.of(changed, portfolios, listOf("AAPL"), sell, null, BigDecimal("9"), picks).picked.originals)
    }

    @Test
    fun sellReview_showsEachLotUsed_byLargestRemainder_andTheRealizedGain() {
        val sell = buy.copy(id = "s1", type = TransactionType.SELL, date = LocalDate.of(2026, 3, 1), quantity = BigDecimal("12"), totalCents = 210_000L)
        val pieces =
            listOf(
                SoldPiece("b1", LocalDate.of(2026, 1, 2), fraction("10"), fraction("1000"), fraction("1750")),
                SoldPiece("b2", LocalDate.of(2026, 2, 1), fraction("2"), fraction("300"), fraction("350")),
            )
        val model = SellReview.of(sell, pieces, portfolios, fraction("175"))
        assertEquals(
            SellReviewModel(
                portfolio = "Portfolio P",
                ticker = "AAPL",
                date = "2026-03-01",
                quantity = "12",
                total = "$2,100.00",
                pricePerShare = "$175.00",
                pieces =
                listOf(
                    SoldPieceModel("2026-01-02", SharesModel("10", false), "$1,000.00", "$1,750.00", 75_000L),
                    SoldPieceModel("2026-02-01", SharesModel("2", false), "$300.00", "$350.00", 5_000L),
                ),
                cost = "$1,300.00",
                realizedCents = 80_000L,
                cashCents = 210_000L,
            ),
            model,
        )
    }

    @Test
    fun sellReview_partsAddUpToTheTotal_whenThirdsRound() {
        val sell = buy.copy(id = "s1", type = TransactionType.SELL, quantity = BigDecimal("3"), totalCents = 100L)
        val third = Fraction.of(java.math.BigInteger.ONE, java.math.BigInteger.valueOf(3))
        val pieces = (1..3).map { SoldPiece("b$it", LocalDate.of(2026, 1, it), fraction("1"), third, third) }
        val model = SellReview.of(sell, pieces, portfolios, null)
        assertEquals(listOf("$0.34", "$0.33", "$0.33").sorted(), model.pieces.map { it.cost }.sorted())
        assertEquals("$1.00", model.cost)
    }
}
