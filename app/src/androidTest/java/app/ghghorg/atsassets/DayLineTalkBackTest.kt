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
import app.ghghorg.atsassets.ui.DayLineModel
import app.ghghorg.atsassets.ui.DayRowText
import app.ghghorg.atsassets.ui.FigureSign
import app.ghghorg.atsassets.ui.HoldingActions
import app.ghghorg.atsassets.ui.HoldingDetailScreen
import app.ghghorg.atsassets.ui.OverallScreen
import app.ghghorg.atsassets.ui.PortfolioScreen
import app.ghghorg.atsassets.ui.PortfolioScreenActions
import app.ghghorg.atsassets.ui.PortfoliosScreen
import app.ghghorg.atsassets.ui.Screen
import app.ghghorg.atsassets.ui.SpokenFigure
import app.ghghorg.atsassets.ui.theme.ATSAssetSystemTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/**
 * The 1-day line for TalkBack (CR5, 41 step 8; AC-61 (d), AC-67): with Example A at $160 and a previous close of $158,
 * every card and row that shows the line reads it in full, "1-day gain/loss plus 16.00 dollars, plus 1.27 percent":
 * Overall's net worth card and portfolio row, the Portfolios list's row, a portfolio's summary and holding row, and
 * Holding detail's header. Without a previous close, none of them has a day line.
 */
@RunWith(AndroidJUnit4::class)
class DayLineTalkBackTest {
    @get:Rule
    val composeRule = createComposeRule()

    // 2026-10-29 16:00 New York, the price's day; the lot of Feb 1 was held before it.
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

    private fun state(
        screen: Screen,
        tab: Tab,
        previousClose: String?,
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
        val price = Price(BigDecimal("160"), close, previousClose = previousClose?.let { BigDecimal(it) })
        return AppState(
            screen = screen,
            tab = tab,
            entries = listOf(CashEntry("o1", LocalDate.of(2026, 1, 1), 960_000L, CashType.OPENING_BALANCE, "", 1L)),
            transactions = listOf(b1, b2, s1),
            today = LocalDate.of(2026, 6, 1),
            portfolios = listOf(Portfolio("p1", "Portfolio P", 1L)),
            portfolioId = "p1",
            holdingTicker = "AAPL",
            instruments = listOf(Instrument("AAPL", AssetType.STOCK, verified = true, price = price)),
            priceKeySet = true,
        )
    }

    // The part a card's or row's sentence ends with: ", 1-day gain/loss plus 16.00 dollars, plus 1.27 percent".
    private val daySuffix: String
        get() = text(R.string.day_spoken, "", text(R.string.spoken_plus, "16.00"), text(R.string.spoken_plus, "1.27"))

    private fun readsTheDay(): Int = composeRule.onAllNodes(hasContentDescription(daySuffix, substring = true)).fetchSemanticsNodes().size

    private fun anyDayLine(): Boolean = composeRule.onAllNodes(hasContentDescription(text(R.string.day_label), substring = true)).fetchSemanticsNodes().isNotEmpty() ||
        composeRule.onAllNodes(hasText(text(R.string.day_label), substring = true)).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun overall_theNetWorthCardAndThePortfolioRow_readTheDayLine() {
        composeRule.setContent { ATSAssetSystemTheme { OverallScreen(state(Screen.TABS, Tab.OVERALL, "158"), {}, {}) } }
        assertEquals("net worth's card and Portfolio P's row", 2, readsTheDay())
    }

    @Test
    fun thePortfoliosList_theRowReadsTheDayLine() {
        composeRule.setContent { ATSAssetSystemTheme { PortfoliosScreen(state(Screen.TABS, Tab.PORTFOLIOS, "158"), {}, {}, {}) } }
        assertEquals("Portfolio P's row", 1, readsTheDay())
    }

    @Test
    fun aPortfolio_theSummaryLineAndTheHoldingRow_readTheDayLine() {
        composeRule.setContent {
            ATSAssetSystemTheme { PortfolioScreen(state(Screen.PORTFOLIO, Tab.PORTFOLIOS, "158"), PortfolioScreenActions({}, {}, {}, {}, {}, {})) }
        }
        val summary = text(R.string.day_line_spoken, text(R.string.spoken_plus, "16.00"), text(R.string.spoken_plus, "1.27"))
        composeRule.onNode(hasContentDescription(summary)).assertExists()
        assertEquals("AAPL's row", 1, readsTheDay())
    }

    @Test
    fun holdingDetail_theHeaderReadsTheDayLine() {
        composeRule.setContent {
            ATSAssetSystemTheme { HoldingDetailScreen(state(Screen.HOLDING, Tab.PORTFOLIOS, "158"), HoldingActions({}, {}, {}, {}, {}, {}, {})) }
        }
        assertEquals("the header", 1, readsTheDay())
    }

    // Your phone check 2026-11-25: in a row, "1-day" is a text of its own in the plain color and only the figures are
    // colored. Drawn alone, since a row's own sentence hides its texts; the color itself is checked on the phone.
    @Test
    fun aRowsDayLine_hasTheWord1DayApart_fromTheColoredFigures() {
        val day = DayLineModel("+$16.00", SpokenFigure("16.00", FigureSign.PLUS), "+1.27%", SpokenFigure("1.27", FigureSign.PLUS))
        composeRule.setContent { ATSAssetSystemTheme { DayRowText(day) } }
        composeRule.onNode(hasText(text(R.string.day_row))).assertExists()
        composeRule.onNode(hasText(text(R.string.day_figures, "+$16.00", "+1.27%"))).assertExists()
    }

    // AC-71: no previous close (before the first refresh after the upgrade), no day line anywhere.
    @Test
    fun withoutAPreviousClose_noScreenHasADayLine() {
        composeRule.setContent { ATSAssetSystemTheme { OverallScreen(state(Screen.TABS, Tab.OVERALL, null), {}, {}) } }
        assertEquals(false, anyDayLine())
    }
}
