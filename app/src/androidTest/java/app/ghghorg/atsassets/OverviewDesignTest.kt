package app.ghghorg.atsassets

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.Tab
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import app.ghghorg.atsassets.ui.AppState
import app.ghghorg.atsassets.ui.NetWorthActions
import app.ghghorg.atsassets.ui.NetWorthScreen
import app.ghghorg.atsassets.ui.OverallScreen
import app.ghghorg.atsassets.ui.Screen
import app.ghghorg.atsassets.ui.theme.ATSAssetSystemTheme
import app.ghghorg.atsassets.ui.theme.Asap
import app.ghghorg.atsassets.ui.theme.TokenColors
import app.ghghorg.atsassets.ui.theme.TokenPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDate

/**
 * The overview screens' look (44a step 8, group (a); spec 6, Visual design; AC-61 (e); drawings 01-overall and
 * 02-net-worth, D-38 and D-39): the title band with its title in 22 in the band's title color; net worth as the summary
 * card in light and dark; Overall's chevrons and the allocation's pick field; the cash slice's swatch in cash's color; Net
 * Worth (Total) under its band with Back. Data: a buy of 10 AAPL for $1,000.00 after an opening of $10,000.00, no price.
 */
@RunWith(AndroidJUnit4::class)
class OverviewDesignTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun state(screen: Screen): AppState {
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
        val opening = CashEntry("o1", LocalDate.of(2026, 1, 1), 1_000_000L, CashType.OPENING_BALANCE, "", 1L)
        val line = CashEntry("c-b1", buy.date, -100_000L, CashType.TRADE, "", 2L, transactionId = "b1")
        return AppState(
            screen = screen,
            tab = Tab.OVERALL,
            entries = listOf(opening, line),
            transactions = listOf(buy),
            today = LocalDate.of(2026, 6, 1),
            portfolios = listOf(Portfolio("p1", "Portfolio P", 1L)),
            priceKeySet = true,
        )
    }

    private fun palette(dark: Boolean): TokenPalette = if (dark) TokenColors.dark else TokenColors.light

    private fun showOverall(dark: Boolean) {
        composeRule.setContent { ATSAssetSystemTheme(darkTheme = dark) { OverallScreen(state(Screen.TABS), {}, {}) } }
        composeRule.waitForIdle()
    }

    private fun netWorthBounds() = composeRule.onAllNodes(
        androidx.compose.ui.test.SemanticsMatcher("net worth's sentence") { node ->
            node.config.getOrElseNullable(SemanticsProperties.ContentDescription) { null }?.firstOrNull()?.startsWith(string(R.string.net_worth_label)) == true
        },
    ).fetchSemanticsNodes().first().boundsInRoot

    private fun checkBandAndSummary(dark: Boolean) {
        showOverall(dark)
        val colors = palette(dark)
        val title = composeRule.onNode(isHeading() and hasText(string(R.string.overall_title)), useUnmergedTree = true)
        val style = title.laidOutStyle()
        assertEquals(Asap, style.fontFamily)
        assertEquals(22.sp, style.fontSize)
        assertEquals(FontWeight.W500, style.fontWeight)
        assertTrue("Title ${describe(style.color)}", sameColor(style.color, colors.onBand, tolerance = 0))
        val root = composeRule.onRoot()
        val titleBounds = title.fetchSemanticsNode().boundsInRoot
        val band = root.colorAt(with(composeRule.density) { 4.dp.toPx() }, titleBounds.center.y)
        assertTrue("Band ${describe(band)}, expected ${describe(colors.band)}", sameColor(band, colors.band))
        // Inside the summary card's padding, left of net worth's figures: the summary color.
        val figures = netWorthBounds()
        val summary = root.colorAt(figures.left - with(composeRule.density) { 5.dp.toPx() }, figures.center.y)
        assertTrue("Summary ${describe(summary)}, expected ${describe(colors.summary)}", sameColor(summary, colors.summary))
    }

    @Test
    fun overall_bandAndSummaryCard_light() = checkBandAndSummary(dark = false)

    @Test
    fun overall_bandAndSummaryCard_dark() = checkBandAndSummary(dark = true)

    @Test
    fun overall_hasItsChevrons_andThePickFieldsChevron() {
        showOverall(dark = false)
        val icons = composeRule.drawnIcons().map { it.first }
        assertTrue("Net worth's and the portfolio row's chevrons: $icons", icons.count { it == "chevron-right" } >= 2)
        assertTrue("Allocation by's chevron-down: $icons", "chevron-down" in icons)
    }

    @Test
    fun overall_cashSwatch_isCashsSliceColor() {
        showOverall(dark = false)
        val cash = string(R.string.net_worth_composition_cash)
        val row =
            composeRule.onAllNodes(
                androidx.compose.ui.test.SemanticsMatcher("the cash row") { node ->
                    node.config.getOrElseNullable(SemanticsProperties.ContentDescription) { null }?.firstOrNull()?.startsWith("$cash,") == true
                },
            ).fetchSemanticsNodes().first().boundsInRoot
        // The 16 dp swatch is first in the row: its middle is 8 dp in.
        val swatch = composeRule.onRoot().colorAt(row.left + with(composeRule.density) { 8.dp.toPx() }, row.center.y)
        assertTrue("Swatch ${describe(swatch)}, expected ${describe(TokenColors.light.sliceCash)}", sameColor(swatch, TokenColors.light.sliceCash))
    }

    @Test
    fun netWorth_hasItsBandWithBack_andTheSummaryCard() {
        composeRule.setContent { ATSAssetSystemTheme { NetWorthScreen(state(Screen.NET_WORTH), NetWorthActions({}, {})) } }
        composeRule.waitForIdle()
        assertTrue(composeRule.drawnIcons().any { it.first == "chevron-left" })
        composeRule.onNode(isHeading() and hasText(string(R.string.net_worth_title)), useUnmergedTree = true).assertExists()
        val figures = netWorthBounds()
        val summary = composeRule.onRoot().colorAt(figures.left - with(composeRule.density) { 5.dp.toPx() }, figures.center.y)
        assertTrue("Summary ${describe(summary)}", sameColor(summary, TokenColors.light.summary))
    }
}
