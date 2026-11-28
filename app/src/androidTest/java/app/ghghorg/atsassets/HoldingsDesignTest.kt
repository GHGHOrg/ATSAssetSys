package app.ghghorg.atsassets

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.ghghorg.atsassets.core.AssetType
import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.HiddenHolding
import app.ghghorg.atsassets.core.Instrument
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.Price
import app.ghghorg.atsassets.core.SellAllocation
import app.ghghorg.atsassets.core.Tab
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import app.ghghorg.atsassets.ui.AppState
import app.ghghorg.atsassets.ui.HiddenHoldingsActions
import app.ghghorg.atsassets.ui.HiddenHoldingsScreen
import app.ghghorg.atsassets.ui.HoldingActions
import app.ghghorg.atsassets.ui.HoldingDetailScreen
import app.ghghorg.atsassets.ui.HoldingTab
import app.ghghorg.atsassets.ui.PortfolioScreen
import app.ghghorg.atsassets.ui.PortfolioScreenActions
import app.ghghorg.atsassets.ui.PortfoliosScreen
import app.ghghorg.atsassets.ui.Screen
import app.ghghorg.atsassets.ui.theme.ATSAssetSystemTheme
import app.ghghorg.atsassets.ui.theme.Asap
import app.ghghorg.atsassets.ui.theme.AsapCondensed
import app.ghghorg.atsassets.ui.theme.TokenColors
import app.ghghorg.atsassets.ui.theme.TokenPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/**
 * The holdings screens' look (44a step 8, group (b); spec 6, Visual design; AC-61 (e); drawings 03-portfolios,
 * 06-holding-detail and 09-11-hidden-holdings-about, D-31, D-38 to D-44): Portfolios under its band with a chevron on each
 * row and New portfolio's plus; a portfolio's band with Back and ⋮, its value as the summary card in light and dark, and
 * the holdings' column head in Asap Condensed 14 medium; Holding detail's header as the summary card in light and dark;
 * Hidden holdings with Unhide's eye and the row's chevron. Data: 10 AAPL bought for $1,000.00 after an opening of
 * $10,000.00, priced at $160.00; OLDCO bought and sold out, then hidden.
 */
@RunWith(AndroidJUnit4::class)
class HoldingsDesignTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun trade(
        id: String,
        ticker: String,
        type: TransactionType,
        date: String,
        seq: Long,
        cents: Long,
        lot: String? = null,
    ) = Transaction(
        id = id,
        portfolioId = "p1",
        ticker = ticker,
        type = type,
        date = LocalDate.parse(date),
        enteredSeq = seq,
        quantity = BigDecimal("10"),
        totalCents = cents,
        allocations = lot?.let { listOf(SellAllocation(it, Fraction.of(BigDecimal("10")))) } ?: emptyList(),
    )

    private fun state(screen: Screen): AppState {
        val buy = trade("b1", "AAPL", TransactionType.BUY, "2026-01-02", 2L, 100_000L)
        val old = trade("o1", "OLDCO", TransactionType.BUY, "2026-01-03", 3L, 10_000L)
        val sold = trade("o2", "OLDCO", TransactionType.SELL, "2026-03-10", 4L, 12_000L, lot = "o1")
        val transactions = listOf(buy, old, sold)
        val opening = CashEntry("c0", LocalDate.of(2026, 1, 1), 1_000_000L, CashType.OPENING_BALANCE, "", 1L)
        val lines =
            transactions.map {
                val cents = if (it.type == TransactionType.BUY) -it.totalCents else it.totalCents
                CashEntry("c-${it.id}", it.date, cents, CashType.TRADE, "", it.enteredSeq, transactionId = it.id)
            }
        // 2026-05-29 16:00 New York.
        val price = Price(BigDecimal("160"), Instant.ofEpochSecond(1_780_084_800L))
        return AppState(
            screen = screen,
            tab = Tab.PORTFOLIOS,
            entries = listOf(opening) + lines,
            transactions = transactions,
            today = LocalDate.of(2026, 6, 1),
            portfolios = listOf(Portfolio("p1", "Portfolio P", 1L)),
            portfolioId = "p1",
            holdingTicker = "AAPL",
            holdingTab = HoldingTab.LOTS,
            instruments =
            listOf(
                Instrument("AAPL", AssetType.STOCK, verified = true, price = price),
                Instrument("OLDCO", AssetType.STOCK, verified = true),
            ),
            hidden = listOf(HiddenHolding("p1", "OLDCO")),
            priceKeySet = true,
        )
    }

    private fun palette(dark: Boolean): TokenPalette = if (dark) TokenColors.dark else TokenColors.light

    private fun icons(): List<String> = composeRule.drawnIcons().map { it.first }

    // The summary card's color inside its padding, left of the node whose TalkBack text starts with [start].
    private fun assertSummaryLeftOf(
        start: String,
        dark: Boolean,
    ) {
        val node =
            composeRule.onAllNodes(
                SemanticsMatcher("the sentence starting \"$start\"") { node ->
                    node.config.getOrElseNullable(SemanticsProperties.ContentDescription) { null }?.firstOrNull()?.startsWith(start) == true
                },
            ).fetchSemanticsNodes().first().boundsInRoot
        val color = composeRule.onRoot().colorAt(node.left - with(composeRule.density) { 5.dp.toPx() }, node.center.y)
        val expected = palette(dark).summary
        assertTrue("Summary ${describe(color)}, expected ${describe(expected)}", sameColor(color, expected))
    }

    @Test
    fun portfolios_hasItsBand_aChevronOnTheRow_andNewPortfoliosPlus() {
        composeRule.setContent { ATSAssetSystemTheme { PortfoliosScreen(state(Screen.TABS), {}, {}, {}) } }
        composeRule.waitForIdle()
        val title = composeRule.onNode(isHeading() and hasText(string(R.string.tab_portfolios)), useUnmergedTree = true)
        val style = title.laidOutStyle()
        assertEquals(Asap, style.fontFamily)
        assertEquals(22.sp, style.fontSize)
        assertTrue("Title ${describe(style.color)}", sameColor(style.color, TokenColors.light.onBand, tolerance = 0))
        val drawn = icons()
        assertTrue("Portfolio P's chevron: $drawn", "chevron-right" in drawn)
        assertTrue("New portfolio's plus: $drawn", "plus" in drawn)
    }

    private fun checkPortfolio(dark: Boolean) {
        composeRule.setContent {
            ATSAssetSystemTheme(darkTheme = dark) { PortfolioScreen(state(Screen.PORTFOLIO), PortfolioScreenActions({}, {}, {}, {}, {}, {})) }
        }
        composeRule.waitForIdle()
        val drawn = icons()
        assertTrue("Back, ⋮, AAPL's and the three links' chevrons: $drawn", drawn.containsAll(listOf("chevron-left", "ellipsis-vertical")))
        assertTrue("AAPL's and the three links' chevrons: $drawn", drawn.count { it == "chevron-right" } >= 4)
        assertSummaryLeftOf(string(R.string.portfolio_value_label), dark)
        // The summary card's line has the same words in Asap 16; the column head is the one in Asap Condensed.
        val labels = composeRule.onAllNodes(hasText(string(R.string.portfolio_unrealized_label)), useUnmergedTree = true)
        val styles = labels.fetchSemanticsNodes().indices.map { labels[it].laidOutStyle() }
        val head =
            checkNotNull(styles.singleOrNull { it.fontFamily == AsapCondensed }) {
                "One column head in Asap Condensed among ${styles.map { it.fontFamily }}"
            }
        assertEquals(14.sp, head.fontSize)
        assertEquals(FontWeight.W500, head.fontWeight)
        assertTrue("Column head ${describe(head.color)}", sameColor(head.color, palette(dark).textSecondary, tolerance = 0))
    }

    @Test
    fun portfolio_bandSummaryCardAndColumnHead_light() = checkPortfolio(dark = false)

    @Test
    fun portfolio_bandSummaryCardAndColumnHead_dark() = checkPortfolio(dark = true)

    private fun checkHolding(dark: Boolean) {
        composeRule.setContent {
            ATSAssetSystemTheme(darkTheme = dark) { HoldingDetailScreen(state(Screen.HOLDING), HoldingActions({}, {}, {}, {}, {}, {}, {})) }
        }
        composeRule.waitForIdle()
        assertTrue(icons().contains("chevron-left"))
        // The header's sentence starts with "AAPL in Portfolio P".
        assertSummaryLeftOf("AAPL", dark)
    }

    @Test
    fun holdingDetail_headerIsTheSummaryCard_light() = checkHolding(dark = false)

    @Test
    fun holdingDetail_headerIsTheSummaryCard_dark() = checkHolding(dark = true)

    @Test
    fun hiddenHoldings_hasUnhidesEye_andTheRowsChevron() {
        composeRule.setContent { ATSAssetSystemTheme { HiddenHoldingsScreen(state(Screen.HIDDEN_HOLDINGS), HiddenHoldingsActions({}, {}, {})) } }
        composeRule.waitForIdle()
        val drawn = icons()
        assertTrue("Back, Unhide's eye and OLDCO's chevron: $drawn", drawn.containsAll(listOf("chevron-left", "eye", "chevron-right")))
        val unhide = composeRule.onNode(hasText(string(R.string.unhide_button)), useUnmergedTree = true).laidOutStyle()
        assertEquals(AsapCondensed, unhide.fontFamily)
        assertTrue("Unhide ${describe(unhide.color)}", sameColor(unhide.color, TokenColors.light.buttonText, tolerance = 0))
    }
}
