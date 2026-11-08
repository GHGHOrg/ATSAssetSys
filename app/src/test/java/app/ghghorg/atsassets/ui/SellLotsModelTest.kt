package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.AssetType
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.Instrument
import app.ghghorg.atsassets.core.LotChoice
import app.ghghorg.atsassets.core.LotUnavailable
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.Price
import app.ghghorg.atsassets.core.SellAllocation
import app.ghghorg.atsassets.core.SplitRatio
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.LocalDate

// 16c: the sell screen's lot rows (wireframe 10a markers 12 to 14; decisions 3 and 4; CR2).
class SellLotsModelTest {
    private fun dollars(
        numerator: Long,
        denominator: Long = 1L,
    ): Fraction = Fraction.of(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator))

    // 2026-10-29 16:00 New York.
    private val close = Instant.ofEpochSecond(1_793_304_000L)

    private fun price(
        text: String,
        stale: Boolean = false,
    ): Price = Price(BigDecimal(text), close, stale)

    // 10 left at $100.00 a share, and a lot bought after the sell date.
    private val lot = LotChoice("b1", LocalDate.of(2026, 1, 2), 2L, dollars(10), dollars(100), unavailable = null)
    private val later = LotChoice("b2", LocalDate.of(2026, 3, 2), 3L, dollars(5), dollars(150), LotUnavailable.BOUGHT_AFTER_SELL_DATE)

    @Test
    fun aLotThatCanBePicked_showsItsGainWithItsSign() {
        assertEquals(
            listOf(
                SellLotRowModel(
                    key = "b1",
                    date = "2026-01-02",
                    left = "10",
                    perShare = "$100.00",
                    spokenPerShare = "100.00",
                    gain = GainModel.Figure("+$600.00", SpokenFigure("600.00", FigureSign.PLUS)),
                    staleAsOf = null,
                    unavailable = null,
                ),
            ),
            SellLots.rows(listOf(lot), Fraction.ONE, price("160")),
        )
    }

    // Wireframe 10a marker 14: a stale price's gain with the stale marker and its time; "no price" without one.
    @Test
    fun aStalePrice_carriesItsTime_andNoPrice_hasNoGain() {
        val stale = SellLots.rows(listOf(lot), Fraction.ONE, price("60", stale = true)).single()
        assertEquals(GainModel.Figure("−$400.00", SpokenFigure("400.00", FigureSign.MINUS)), stale.gain)
        assertEquals("2026-10-29 16:00", stale.staleAsOf)
        val none = SellLots.rows(listOf(lot), Fraction.ONE, null).single()
        assertEquals(GainModel.NoPrice, none.gain)
        assertEquals(null, none.staleAsOf)
    }

    // Decision 4: 10 shares on the sell date are 20 today after a 2 for 1 split.
    @Test
    fun theGainIsInTodaysShares() {
        assertEquals(
            GainModel.Figure("+$200.00", SpokenFigure("200.00", FigureSign.PLUS)),
            SellLots.rows(listOf(lot), dollars(2), price("60")).single().gain,
        )
    }

    // Marker 13: a lot that cannot be picked keeps its place and its reason, with no gain and no flag.
    @Test
    fun aLotThatCannotBePicked_hasNoGain() {
        val row = SellLots.rows(listOf(lot, later), Fraction.ONE, price("160", stale = true))[1]
        assertEquals(null, row.gain)
        assertEquals(null, row.staleAsOf)
        assertEquals(LotUnavailable.BOUGHT_AFTER_SELL_DATE, row.unavailable)
        assertEquals("5", row.left)
    }

    private fun trade(
        id: String,
        type: TransactionType,
        date: String,
        quantity: String,
        totalCents: Long,
        seq: Long,
        allocations: List<SellAllocation> = emptyList(),
        split: SplitRatio? = null,
    ): Transaction = Transaction(
        id = id,
        portfolioId = "p1",
        ticker = "AAPL",
        type = type,
        date = LocalDate.parse(date),
        enteredSeq = seq,
        quantity = BigDecimal(quantity),
        totalCents = totalCents,
        allocations = allocations,
        split = split,
    )

    private fun state(
        transactions: List<Transaction>,
        dateText: String,
        replacingId: String? = null,
    ): AppState = AppState(
        screen = Screen.SELL,
        transactions = transactions,
        today = LocalDate.of(2026, 6, 1),
        portfolios = listOf(Portfolio("p1", "Portfolio P", 1L)),
        instruments = listOf(Instrument("AAPL", AssetType.STOCK, verified = true, price = price("60"))),
        sellForm = SellForm(portfolioId = "p1", ticker = "AAPL", dateText = dateText),
        replacingId = replacingId,
    )

    // Wiring: the screen state hands the lot choices, today's shares and the ticker's price to the builder.
    @Test
    fun theSellScreen_showsEachLotsGainInTodaysShares_afterASplitSinceTheSellDate() {
        val transactions =
            listOf(
                trade("b1", TransactionType.BUY, "2026-01-02", "10", 100_000L, 2L),
                trade("x1", TransactionType.SPLIT, "2026-03-01", "0", 0L, 3L, split = SplitRatio(2, 1)),
            )
        val row = state(transactions, "20260201").sellLotRows.single()
        assertEquals("10", row.left)
        assertEquals(GainModel.Figure("+$200.00", SpokenFigure("200.00", FigureSign.PLUS)), row.gain)
    }

    // Wiring, Reverse and re-enter of a sell (10c): the lots as after the reversal, so the sold lot is back in full.
    @Test
    fun reverseAndReenter_showsTheLotsAsAfterTheReversal() {
        val transactions =
            listOf(
                trade("b1", TransactionType.BUY, "2026-01-02", "10", 100_000L, 2L),
                trade("s1", TransactionType.SELL, "2026-02-01", "4", 50_000L, 3L, listOf(SellAllocation("b1", dollars(4)))),
            )
        assertEquals("6", state(transactions, "20260201").sellLotRows.single().left)
        val replacing = state(transactions, "20260201", replacingId = "s1").sellLotRows.single()
        assertEquals("10", replacing.left)
        // 10 × $60.00 − $1,000.00.
        assertEquals(GainModel.Figure("−$400.00", SpokenFigure("400.00", FigureSign.MINUS)), replacing.gain)
    }
}
