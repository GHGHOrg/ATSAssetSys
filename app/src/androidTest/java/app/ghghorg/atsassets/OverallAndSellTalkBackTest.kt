package app.ghghorg.atsassets

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.ghghorg.atsassets.core.AssetType
import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.Instrument
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.Price
import app.ghghorg.atsassets.core.SellAllocation
import app.ghghorg.atsassets.core.Tab
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import app.ghghorg.atsassets.ui.AppState
import app.ghghorg.atsassets.ui.OverallScreen
import app.ghghorg.atsassets.ui.Screen
import app.ghghorg.atsassets.ui.SellActions
import app.ghghorg.atsassets.ui.SellEntryScreen
import app.ghghorg.atsassets.ui.SellForm
import app.ghghorg.atsassets.ui.theme.ATSAssetSystemTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/**
 * Overall and the sell screen's lot list (16c, AC-61 (d)): with Example A, net worth's TalkBack text reads it with the
 * price age line ("some stale" when it is), a portfolio row reads its value and gain/loss with its sign, and a lot reads
 * its unrealized gain/loss and, when stale, its price's time; the "updating" and refresh-failed lines show only while a
 * refresh runs and after one failed.
 */
@RunWith(AndroidJUnit4::class)
class OverallAndSellTalkBackTest {
    @get:Rule
    val composeRule = createComposeRule()

    // 2026-10-29 16:00 New York.
    private val close = Instant.ofEpochSecond(1_793_304_000L)

    private fun text(
        id: Int,
        vararg args: Any,
    ): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private fun trade(
        id: String,
        type: TransactionType,
        date: String,
        quantity: String,
        totalCents: Long,
        seq: Long,
        allocations: List<SellAllocation> = emptyList(),
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
    )

    private fun line(
        transaction: Transaction,
        cents: Long,
    ): CashEntry = CashEntry("c-${transaction.id}", transaction.date, cents, CashType.TRADE, "", transaction.enteredSeq, transactionId = transaction.id)

    // Example A: $9,600.00 cash and 8 AAPL left in Portfolio P, one lot of 2026-02-01 at $150.00.
    private fun state(
        screen: Screen,
        price: Price,
        refreshing: Boolean = false,
        refreshFailed: Boolean = false,
    ): AppState {
        val b1 = trade("b1", TransactionType.BUY, "2026-01-02", "10", 100_000L, 2L)
        val b2 = trade("b2", TransactionType.BUY, "2026-02-01", "10", 150_000L, 3L)
        val s1 =
            trade(
                "s1",
                TransactionType.SELL,
                "2026-03-01",
                "12",
                210_000L,
                4L,
                listOf(SellAllocation("b1", Fraction.of(BigDecimal("10"))), SellAllocation("b2", Fraction.of(BigDecimal("2")))),
            )
        val opening = CashEntry("o1", LocalDate.of(2026, 1, 1), 1_000_000L, CashType.OPENING_BALANCE, "", 1L)
        return AppState(
            screen = screen,
            tab = Tab.OVERALL,
            entries = listOf(opening, line(b1, -100_000L), line(b2, -150_000L), line(s1, 210_000L)),
            transactions = listOf(b1, b2, s1),
            today = LocalDate.of(2026, 6, 1),
            portfolios = listOf(Portfolio("p1", "Portfolio P", 1L)),
            instruments = listOf(Instrument("AAPL", AssetType.STOCK, verified = true, price = price)),
            sellForm = SellForm(portfolioId = "p1", ticker = "AAPL", dateText = "20260601"),
            refreshing = refreshing,
            refreshFailed = refreshFailed,
        )
    }

    private fun shown(id: Int): Boolean = composeRule.onAllNodes(hasText(text(id))).fetchSemanticsNodes().isNotEmpty()

    private fun showSell(state: AppState) {
        composeRule.setContent {
            ATSAssetSystemTheme { SellEntryScreen(state, SellActions({}, {}, {}, {}, {}, {}, { _, _ -> }, {}, {}, {})) }
        }
    }

    @Test
    fun overall_atAFreshPrice_readsNetWorthWithItsAgeLine_andThePortfolioRow() {
        val state = state(Screen.TABS, Price(BigDecimal("160"), close))
        composeRule.setContent { ATSAssetSystemTheme { OverallScreen(state, {}, {}) } }
        val age = text(R.string.portfolio_prices_as_of_spoken, "2026-10-29 16:00")
        val netWorth = text(R.string.net_worth_prices_spoken, text(R.string.net_worth_spoken, "10,880.00"), age)
        val row = text(R.string.overall_portfolio_row_spoken, "Portfolio P", "1,280.00", text(R.string.spoken_plus, "80.00"))
        composeRule.onNode(hasContentDescription(netWorth)).assertExists()
        composeRule.onNode(hasContentDescription(row)).assertExists()
        assertEquals("The refresh-failed line shown", false, shown(R.string.price_refresh_failed))
    }

    @Test
    fun overall_atAStalePrice_whileAFailedRefreshIsRetried_saysSomeStale_updating_andFailed() {
        val state = state(Screen.TABS, Price(BigDecimal("140"), close, stale = true), refreshing = true, refreshFailed = true)
        composeRule.setContent { ATSAssetSystemTheme { OverallScreen(state, {}, {}) } }
        // $9,600.00 cash and 8 × $140.00.
        val age = text(R.string.portfolio_prices_as_of_some_stale_spoken, "2026-10-29 16:00")
        val netWorth = text(R.string.net_worth_prices_spoken, text(R.string.net_worth_spoken, "10,720.00"), age)
        val row = text(R.string.overall_portfolio_row_spoken, "Portfolio P", "1,120.00", text(R.string.spoken_minus, "80.00"))
        composeRule.onNode(hasContentDescription(netWorth)).assertExists()
        composeRule.onNode(hasContentDescription(row)).assertExists()
        assertEquals("The updating line shown", true, shown(R.string.prices_updating))
        assertEquals("The refresh-failed line shown", true, shown(R.string.price_refresh_failed))
    }

    @Test
    fun theSellLotList_atAFreshPrice_readsTheLotsGainWithItsSign() {
        showSell(state(Screen.SELL, Price(BigDecimal("160"), close)))
        val expected =
            text(R.string.sell_lot_gain_spoken, "2026-02-01", "8", "150.00", text(R.string.spoken_plus, "80.00"), text(R.string.sell_lot_not_picked))
        composeRule.onNode(hasContentDescription(expected)).assertExists()
        assertEquals("The refresh-failed line shown", false, shown(R.string.price_refresh_failed))
    }

    @Test
    fun theSellLotList_atAStalePrice_readsThePricesTime_andShowsTheFailedLine() {
        showSell(state(Screen.SELL, Price(BigDecimal("140"), close, stale = true), refreshFailed = true))
        val expected =
            text(
                R.string.sell_lot_gain_stale_spoken,
                "2026-02-01",
                "8",
                "150.00",
                text(R.string.spoken_minus, "80.00"),
                "2026-10-29 16:00",
                text(R.string.sell_lot_not_picked),
            )
        composeRule.onNode(hasContentDescription(expected)).assertExists()
        assertEquals("The refresh-failed line shown", true, shown(R.string.price_refresh_failed))
    }
}
