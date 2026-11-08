package app.ghghorg.atsassets

import androidx.compose.ui.test.hasContentDescription
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
import app.ghghorg.atsassets.ui.HoldingActions
import app.ghghorg.atsassets.ui.HoldingDetailScreen
import app.ghghorg.atsassets.ui.HoldingTab
import app.ghghorg.atsassets.ui.Screen
import app.ghghorg.atsassets.ui.Tab
import app.ghghorg.atsassets.ui.theme.ATSAssetSystemTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/**
 * Holding detail (16a, AC-61 (d)): with Example A, the header's and the lot's TalkBack text read the value, the cost,
 * the unrealized gain/loss with its sign and the price with its own time in full, and say "stale" when the price is.
 */
@RunWith(AndroidJUnit4::class)
class HoldingTalkBackTest {
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

    // Example A: 8 AAPL left in Portfolio P, one lot of 2026-02-01 at $150.00, cost $1,200.00; its Lots tab.
    private fun show(price: Price) {
        val transactions =
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
            )
        val state =
            AppState(
                screen = Screen.HOLDING,
                tab = Tab.PORTFOLIOS,
                transactions = transactions,
                today = LocalDate.of(2026, 6, 1),
                portfolios = listOf(Portfolio("p1", "Portfolio P", 1L)),
                portfolioId = "p1",
                holdingTicker = "AAPL",
                holdingTab = HoldingTab.LOTS,
                instruments = listOf(Instrument("AAPL", AssetType.STOCK, verified = true, price = price)),
            )
        composeRule.setContent {
            ATSAssetSystemTheme { HoldingDetailScreen(state, HoldingActions({}, {}, {}, {}, {}, {}, {})) }
        }
    }

    @Test
    fun atAFreshPrice_theHeaderAndTheLotReadTheGainWithItsSign() {
        show(Price(BigDecimal("160"), close))
        val expected =
            listOf(
                text(
                    R.string.holding_header_gain_spoken,
                    "AAPL",
                    "Portfolio P",
                    "1,280.00",
                    text(R.string.holding_shares, "8"),
                    "1,200.00",
                    text(R.string.spoken_plus, "80.00"),
                    text(R.string.holding_price_spoken, "160.00", "2026-10-29 16:00"),
                ),
                text(R.string.holding_lot_gain_spoken, "2026-02-01", "8", "150.00", "1,200.00", text(R.string.spoken_plus, "80.00")),
            )
        expected.forEach { composeRule.onNode(hasContentDescription(it)).assertExists() }
    }

    @Test
    fun atAStalePrice_theHeaderAndTheLotSayStale() {
        show(Price(BigDecimal("140"), close, stale = true))
        val expected =
            listOf(
                text(
                    R.string.holding_header_gain_spoken,
                    "AAPL",
                    "Portfolio P",
                    "1,120.00",
                    text(R.string.holding_shares, "8"),
                    "1,200.00",
                    text(R.string.spoken_minus, "80.00"),
                    text(R.string.holding_price_stale_spoken, "140.00", "2026-10-29 16:00"),
                ),
                text(R.string.holding_lot_gain_stale_spoken, "2026-02-01", "8", "150.00", "1,200.00", text(R.string.spoken_minus, "80.00")),
            )
        expected.forEach { composeRule.onNode(hasContentDescription(it)).assertExists() }
    }
}
