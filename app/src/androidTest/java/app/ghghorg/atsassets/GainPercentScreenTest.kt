package app.ghghorg.atsassets

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.ghghorg.atsassets.core.AssetType
import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.Instrument
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.Price
import app.ghghorg.atsassets.core.Tab
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import app.ghghorg.atsassets.ui.AppState
import app.ghghorg.atsassets.ui.HoldingActions
import app.ghghorg.atsassets.ui.HoldingDetailScreen
import app.ghghorg.atsassets.ui.HoldingTab
import app.ghghorg.atsassets.ui.OverallScreen
import app.ghghorg.atsassets.ui.PortfolioScreen
import app.ghghorg.atsassets.ui.PortfolioScreenActions
import app.ghghorg.atsassets.ui.PortfoliosScreen
import app.ghghorg.atsassets.ui.Screen
import app.ghghorg.atsassets.ui.SellActions
import app.ghghorg.atsassets.ui.SellEntryScreen
import app.ghghorg.atsassets.ui.SellForm
import app.ghghorg.atsassets.ui.theme.ATSAssetSystemTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/*
 * CR6 (43; spec C26, section 6, AC-61 (a) and (d), AC-72): the unrealized gain/loss with its percentage, at the largest
 * font, upright and turned. A large gain, "+$666,660.00 (+66.67%)", so the figures are as long as they get here. Each row,
 * card and header that shows it reads it in full for TalkBack and lies within the screen's width; where the figures' own
 * text is in the tree (the sell lot list), it is not cut off. The wrap itself is seen on the phone (step 6).
 */

@RunWith(AndroidJUnit4::class)
class GainPercentLargestFontPortraitTest {
    private val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val rules: RuleChain =
        RuleChain
            .outerRule(DeviceUnlockedRule())
            .around(DeviceSettingsRule(fontScale = 2.0f))
            .around(composeRule)

    @Test
    fun holdingDetail_headerAndLot() = composeRule.checkHoldingDetail(landscape = false)

    @Test
    fun portfolio_summaryAndHoldingRow() = composeRule.checkPortfolio(landscape = false)

    @Test
    fun overall_portfolioRow() = composeRule.checkOverall(landscape = false)

    @Test
    fun portfolios_row() = composeRule.checkPortfolios(landscape = false)

    @Test
    fun sell_lotRow() = composeRule.checkSell(landscape = false)
}

@RunWith(AndroidJUnit4::class)
class GainPercentLargestFontLandscapeTest {
    private val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val rules: RuleChain =
        RuleChain
            .outerRule(DeviceUnlockedRule())
            .around(DeviceSettingsRule(fontScale = 2.0f))
            .around(composeRule)

    @Test
    fun holdingDetail_headerAndLot() = composeRule.checkHoldingDetail(landscape = true)

    @Test
    fun portfolio_summaryAndHoldingRow() = composeRule.checkPortfolio(landscape = true)

    @Test
    fun overall_portfolioRow() = composeRule.checkOverall(landscape = true)

    @Test
    fun portfolios_row() = composeRule.checkPortfolios(landscape = true)

    @Test
    fun sell_lotRow() = composeRule.checkSell(landscape = true)
}

private typealias GainRule = AndroidComposeTestRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity>

private fun text(
    id: Int,
    vararg args: Any,
): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

private const val BIG = "BIGC"

// 2026-10-29 16:00 New York.
private val CLOSE = Instant.ofEpochSecond(1_793_304_000L)

// 1,000 shares bought for $1,000,000.00 on 2026-01-02, priced at $1,666.66: worth $1,666,660.00, a gain of $666,660.00
// over a cost of $1,000,000.00, 66.666% shown +66.67% (4.8).
private fun bigState(screen: Screen): AppState {
    val buy =
        Transaction(
            id = "b1",
            portfolioId = "p1",
            ticker = BIG,
            type = TransactionType.BUY,
            date = LocalDate.of(2026, 1, 2),
            enteredSeq = 2L,
            quantity = BigDecimal("1000"),
            totalCents = 100_000_000L,
        )
    val opening = CashEntry("o1", LocalDate.of(2026, 1, 1), 200_000_000L, CashType.OPENING_BALANCE, "", 1L)
    val line = CashEntry("c-b1", buy.date, -100_000_000L, CashType.TRADE, "", buy.enteredSeq, transactionId = buy.id)
    return AppState(
        screen = screen,
        tab = if (screen == Screen.TABS) Tab.OVERALL else Tab.PORTFOLIOS,
        entries = listOf(opening, line),
        transactions = listOf(buy),
        today = LocalDate.of(2026, 6, 1),
        portfolios = listOf(Portfolio("p1", "Big", 1L)),
        portfolioId = "p1",
        holdingTicker = BIG,
        holdingTab = HoldingTab.LOTS,
        instruments = listOf(Instrument(BIG, AssetType.STOCK, verified = true, price = Price(BigDecimal("1666.66"), CLOSE))),
        sellForm = SellForm(portfolioId = "p1", ticker = BIG, dateText = "20260601"),
        priceKeySet = true,
    )
}

// "plus 666,660.00 dollars, plus 66.67 percent", as every sentence that carries the gain reads it.
private fun bigGainSpoken(): String = text(R.string.gain_spoken, text(R.string.spoken_plus, "666,660.00"), text(R.string.spoken_plus, "66.67"))

private fun GainRule.show(
    landscape: Boolean,
    content: @Composable () -> Unit,
) {
    turn(landscape)
    setContent { ATSAssetSystemTheme { content() } }
    waitForIdle()
}

// Brings a node into view in a scrolling list, where there is one (a lazy list draws only what is in view; 33b).
private fun GainRule.reach(matcher: SemanticsMatcher) {
    val scrollers = onAllNodes(hasScrollToNodeAction()).fetchSemanticsNodes()
    if (scrollers.isNotEmpty()) runCatching { onAllNodes(hasScrollToNodeAction())[0].performScrollToNode(matcher) }
    waitForIdle()
}

// For each of [keys], the one node whose TalkBack text carries that key and the gain with its percentage, brought into
// view and within the screen's width.
private fun GainRule.assertGainRead(vararg keys: String) {
    keys.forEach { key ->
        val matcher = hasContentDescription(bigGainSpoken(), substring = true) and hasContentDescription(key, substring = true)
        reach(matcher)
        val nodes = onAllNodes(matcher).fetchSemanticsNodes()
        assertEquals("Nodes that read \"$key\" with the gain and its percentage", 1, nodes.size)
        assertWithin(nodes.single(), onRoot().fetchSemanticsNode().size.width)
    }
}

private fun assertWithin(
    node: SemanticsNode,
    width: Int,
) {
    val left = node.positionInRoot.x
    val right = left + node.size.width
    assertTrue("Starts left of the screen: $left px", left >= -0.5f)
    assertTrue("Ends right of the screen: $right px of $width px", right <= width + 0.5f)
}

private fun GainRule.checkHoldingDetail(landscape: Boolean) {
    show(landscape) { HoldingDetailScreen(bigState(Screen.HOLDING), HoldingActions({}, {}, {}, {}, {}, {}, {})) }
    // The header ("BIGC in Big") and the lot of 2026-01-02.
    assertGainRead("$BIG in Big", "2026-01-02")
}

private fun GainRule.checkPortfolio(landscape: Boolean) {
    show(landscape) { PortfolioScreen(bigState(Screen.PORTFOLIO), PortfolioScreenActions({}, {}, {}, {}, {}, {})) }
    // The summary's gain/loss line and the holding row.
    assertGainRead(string(R.string.portfolio_unrealized_label), BIG)
}

private fun GainRule.checkOverall(landscape: Boolean) {
    show(landscape) { OverallScreen(bigState(Screen.TABS), {}, {}) }
    assertGainRead("Big")
}

private fun GainRule.checkPortfolios(landscape: Boolean) {
    show(landscape) { PortfoliosScreen(bigState(Screen.TABS), {}, {}, {}) }
    assertGainRead("Big")
}

private fun GainRule.checkSell(landscape: Boolean) {
    show(landscape) { SellEntryScreen(bigState(Screen.SELL), SellActions({}, {}, {}, {}, {}, {}, { _, _ -> }, {}, {}, {})) }
    assertGainRead("2026-01-02")
    // The lot's figures are their own text here: shown whole, not cut off, within the screen (C26).
    val figures = hasText(text(R.string.gain_figures, "+$666,660.00", "+66.67%"))
    reach(figures)
    val node = onNode(figures).fetchSemanticsNode()
    assertWithin(node, onRoot().fetchSemanticsNode().size.width)
    val layouts = mutableListOf<TextLayoutResult>()
    node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
    assertTrue("No text layout for the lot's figures", layouts.isNotEmpty())
    assertFalse("The lot's figures are cut off", layouts.first().hasVisualOverflow)
}
