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
import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.SellAllocation
import app.ghghorg.atsassets.core.Tab
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import app.ghghorg.atsassets.ui.AppState
import app.ghghorg.atsassets.ui.CashFilterActions
import app.ghghorg.atsassets.ui.CashScreen
import app.ghghorg.atsassets.ui.RealizedGainsActions
import app.ghghorg.atsassets.ui.RealizedGainsScreen
import app.ghghorg.atsassets.ui.Screen
import app.ghghorg.atsassets.ui.TransactionHistoryActions
import app.ghghorg.atsassets.ui.TransactionHistoryScreen
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
import java.time.LocalDate

/**
 * The money lists' look (44a step 8, group (c); spec 6, Visual design; AC-61 (e); drawings 05-realized-gains, 07-cash and
 * 08-transaction-history, D-28 to D-31, D-38): Cash under its band with the balance as the summary card and Cash entry in
 * its own outline colors, in light and dark, the dense dates' calendars, the Ticker pick field, the selected chip's check
 * and the rows' chevrons; Transaction history under its band with Back and the same filters; Realized gains' total as the
 * summary card in its gain color, the column head in Asap Condensed 14 medium and a holding's chevron-down. Data: Example
 * A, 10 AAPL bought for $1,000.00 and sold for $1,800.00 after an opening of $10,000.00.
 */
@RunWith(AndroidJUnit4::class)
class MoneyListsDesignTest {
    @get:Rule
    val composeRule = createComposeRule()

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
    private val sell =
        Transaction(
            id = "s1",
            portfolioId = "p1",
            ticker = "AAPL",
            type = TransactionType.SELL,
            date = LocalDate.of(2026, 3, 1),
            enteredSeq = 3L,
            quantity = BigDecimal("10"),
            totalCents = 180_000L,
            allocations = listOf(SellAllocation("b1", Fraction.of(BigDecimal("10")))),
        )

    private fun state(screen: Screen): AppState = AppState(
        screen = screen,
        tab = if (screen == Screen.TABS) Tab.CASH else Tab.MORE,
        entries =
        listOf(
            CashEntry("o1", LocalDate.of(2026, 1, 1), 1_000_000L, CashType.OPENING_BALANCE, "", 1L),
            CashEntry("c-b1", buy.date, -100_000L, CashType.TRADE, "", 2L, transactionId = "b1"),
            CashEntry("c-s1", sell.date, 180_000L, CashType.TRADE, "", 3L, transactionId = "s1"),
        ),
        transactions = listOf(buy, sell),
        today = LocalDate.of(2026, 6, 1),
        portfolios = listOf(Portfolio("p1", "Portfolio P", 1L)),
        historyPortfolioId = "p1",
    )

    private fun palette(dark: Boolean): TokenPalette = if (dark) TokenColors.dark else TokenColors.light

    private fun icons(): List<String> = composeRule.drawnIcons().map { it.first }

    private fun boundsOfSentence(start: String) = composeRule.onAllNodes(
        SemanticsMatcher("the sentence starting \"$start\"") { node ->
            node.config.getOrElseNullable(SemanticsProperties.ContentDescription) { null }?.firstOrNull()?.startsWith(start) == true
        },
    ).fetchSemanticsNodes().first().boundsInRoot

    private fun assertSummaryLeftOf(
        start: String,
        dark: Boolean,
    ) {
        val node = boundsOfSentence(start)
        val color = composeRule.onRoot().colorAt(node.left - with(composeRule.density) { 5.dp.toPx() }, node.center.y)
        assertTrue("Summary ${describe(color)}, expected ${describe(palette(dark).summary)}", sameColor(color, palette(dark).summary))
    }

    private fun checkCash(dark: Boolean) {
        composeRule.setContent {
            ATSAssetSystemTheme(darkTheme = dark) { CashScreen(state(Screen.TABS), {}, {}, {}, CashFilterActions({}, {}, {}, {})) }
        }
        composeRule.waitForIdle()
        val title = composeRule.onNode(isHeading() and hasText(string(R.string.tab_cash)), useUnmergedTree = true).laidOutStyle()
        assertEquals(Asap, title.fontFamily)
        assertEquals(22.sp, title.fontSize)
        assertSummaryLeftOf(string(R.string.cash_balance_label), dark)
        val entry = composeRule.onNode(hasText(string(R.string.cash_entry_button)), useUnmergedTree = true).laidOutStyle()
        val ink = palette(dark).buttonOnSummaryText
        assertTrue("Cash entry ${describe(entry.color)}, expected ${describe(ink)}", sameColor(entry.color, ink, tolerance = 0))
        val drawn = icons()
        assertEquals("From's and To's calendars: $drawn", 2, drawn.count { it == "calendar" })
        assertTrue("The Ticker field's chevron-down and All's check: $drawn", drawn.containsAll(listOf("chevron-down", "check")))
        assertTrue("The three rows' chevrons: $drawn", drawn.count { it == "chevron-right" } >= 3)
    }

    @Test
    fun cash_bandSummaryCardFiltersAndRows_light() = checkCash(dark = false)

    @Test
    fun cash_bandSummaryCardFiltersAndRows_dark() = checkCash(dark = true)

    @Test
    fun transactionHistory_hasItsBandWithBack_theFilters_andTheRowsChevrons() {
        val actions = TransactionHistoryActions({}, {}, {}, {}, {}, {}, {}, {}, {})
        composeRule.setContent { ATSAssetSystemTheme { TransactionHistoryScreen(state(Screen.TRANSACTION_HISTORY).transactionHistoryScreen, actions) } }
        composeRule.waitForIdle()
        composeRule.onNode(isHeading() and hasText(string(R.string.history_title)), useUnmergedTree = true).assertExists()
        val drawn = icons()
        assertTrue("Back: $drawn", "chevron-left" in drawn)
        assertEquals("From's and To's calendars: $drawn", 2, drawn.count { it == "calendar" })
        // The sell and the buy, each with its cash line.
        assertTrue("The rows' and their cash lines' chevrons: $drawn", drawn.count { it == "chevron-right" } >= 4)
        val name = composeRule.onNode(hasText("Portfolio P"), useUnmergedTree = true).laidOutStyle()
        assertEquals(FontWeight.W500, name.fontWeight)
        assertEquals(16.sp, name.fontSize)
    }

    @Test
    fun realizedGains_totalIsTheSummaryCardInItsGainColor_andTheColumnHeadIsCondensed() {
        val actions = RealizedGainsActions({}, {}, {}, {}, { _, _ -> }, {})
        composeRule.setContent { ATSAssetSystemTheme { RealizedGainsScreen(state(Screen.REALIZED_GAINS).realizedGainsScreen, actions) } }
        composeRule.waitForIdle()
        assertSummaryLeftOf(string(R.string.gains_total_label), dark = false)
        // AAPL's row shows the same figure in 16; the total is the one in 32.
        val figures = composeRule.onAllNodes(hasText("+$800.00"), useUnmergedTree = true)
        val styles = figures.fetchSemanticsNodes().indices.map { figures[it].laidOutStyle() }
        val total = checkNotNull(styles.singleOrNull { it.fontSize == 32.sp }) { "One total in 32 among ${styles.map { it.fontSize }}" }
        assertTrue("Total ${describe(total.color)}", sameColor(total.color, TokenColors.light.gainOnSummary, tolerance = 0))
        val head = composeRule.onNode(hasText(string(R.string.gains_column)), useUnmergedTree = true).laidOutStyle()
        assertEquals(AsapCondensed, head.fontFamily)
        assertEquals(14.sp, head.fontSize)
        assertEquals(FontWeight.W500, head.fontWeight)
        assertTrue("AAPL's chevron-down and both calendars: ${icons()}", icons().containsAll(listOf("chevron-left", "chevron-down", "calendar")))
    }
}
