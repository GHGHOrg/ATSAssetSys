package app.ghghorg.atsassets

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.ghghorg.atsassets.core.AssetType
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.Instrument
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.Price
import app.ghghorg.atsassets.core.SellAllocation
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import app.ghghorg.atsassets.ui.AppState
import app.ghghorg.atsassets.ui.PortfolioScreen
import app.ghghorg.atsassets.ui.PortfolioScreenActions
import app.ghghorg.atsassets.ui.PortfoliosScreen
import app.ghghorg.atsassets.ui.Screen
import app.ghghorg.atsassets.ui.Tab
import app.ghghorg.atsassets.ui.theme.ATSAssetSystemTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/**
 * A portfolio's screen and the Portfolios list (16b, AC-61 (d)): with Example A, the summary's, a holding row's and a
 * portfolio row's TalkBack text read the figures, the gain/loss with its sign and the price age line in full, with
 * "some stale" and the row's stale price when it is, and the refresh-failed line shows only after a failed refresh.
 */
@RunWith(AndroidJUnit4::class)
class PortfolioTalkBackTest {
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

    // Example A: 8 AAPL left in Portfolio P at a cost of $1,200.00.
    private fun state(
        screen: Screen,
        price: Price,
        refreshFailed: Boolean,
    ): AppState = AppState(
        screen = screen,
        tab = Tab.PORTFOLIOS,
        transactions =
        listOf(
            trade("b1", TransactionType.BUY, "2026-01-02", "10", 100_000L, 2L),
            trade("b2", TransactionType.BUY, "2026-02-01", "10", 150_000L, 3L),
            trade(
                "s1",
                TransactionType.SELL,
                "2026-03-01",
                "12",
                210_000L,
                4L,
                listOf(SellAllocation("b1", Fraction.of(BigDecimal("10"))), SellAllocation("b2", Fraction.of(BigDecimal("2")))),
            ),
        ),
        today = LocalDate.of(2026, 6, 1),
        portfolios = listOf(Portfolio("p1", "Portfolio P", 1L)),
        portfolioId = "p1",
        instruments = listOf(Instrument("AAPL", AssetType.STOCK, verified = true, price = price)),
        refreshFailed = refreshFailed,
    )

    private fun showPortfolio(state: AppState) {
        composeRule.setContent {
            ATSAssetSystemTheme { PortfolioScreen(state, PortfolioScreenActions({}, {}, {}, {}, {}, {})) }
        }
    }

    private fun assertFailedLine(shown: Boolean) {
        val failed = composeRule.onAllNodes(hasText(text(R.string.price_refresh_failed))).fetchSemanticsNodes()
        assertEquals("The refresh-failed line shown", shown, failed.isNotEmpty())
    }

    @Test
    fun aPortfolio_atAFreshPrice_readsItsAgeLineAndARowWithItsSign() {
        showPortfolio(state(Screen.PORTFOLIO, Price(BigDecimal("160"), close), refreshFailed = false))
        val expected =
            listOf(
                text(R.string.spoken_label_dollars, text(R.string.portfolio_value_label), "1,280.00"),
                text(R.string.spoken_label_dollars, text(R.string.portfolio_unrealized_label), text(R.string.spoken_plus, "80.00")),
                text(R.string.portfolio_prices_as_of_spoken, "2026-10-29 16:00"),
                text(R.string.holding_row_gain_spoken, "AAPL", text(R.string.holding_shares, "8"), "1,280.00", text(R.string.spoken_plus, "80.00")),
            )
        expected.forEach { composeRule.onNode(hasContentDescription(it)).assertExists() }
        assertFailedLine(shown = false)
    }

    @Test
    fun aPortfolio_atAStalePrice_saysSomeStale_andTheRowsStalePrice_andTheRefreshFailed() {
        showPortfolio(state(Screen.PORTFOLIO, Price(BigDecimal("140"), close, stale = true), refreshFailed = true))
        val minus = text(R.string.spoken_minus, "80.00")
        val expected =
            listOf(
                text(R.string.spoken_label_dollars, text(R.string.portfolio_value_label), "1,120.00"),
                text(R.string.spoken_label_dollars, text(R.string.portfolio_unrealized_label), minus),
                text(R.string.portfolio_prices_as_of_some_stale_spoken, "2026-10-29 16:00"),
                text(R.string.holding_row_gain_stale_spoken, "AAPL", text(R.string.holding_shares, "8"), "1,120.00", minus, "2026-10-29 16:00"),
            )
        expected.forEach { composeRule.onNode(hasContentDescription(it)).assertExists() }
        assertFailedLine(shown = true)
    }

    @Test
    fun thePortfoliosList_readsARowWithItsFigures_andShowsTheFailedLine() {
        val state = state(Screen.TABS, Price(BigDecimal("160"), close), refreshFailed = true)
        composeRule.setContent {
            ATSAssetSystemTheme { PortfoliosScreen(state, {}, {}, {}) }
        }
        val count = InstrumentationRegistry.getInstrumentation().targetContext.resources.getQuantityString(R.plurals.portfolio_holding_count, 1, 1)
        composeRule.onNode(hasContentDescription(text(R.string.portfolio_row_spoken, "Portfolio P", count, "1,280.00", text(R.string.spoken_plus, "80.00")))).assertExists()
        assertFailedLine(shown = true)
    }
}
