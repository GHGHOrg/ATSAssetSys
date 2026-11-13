package app.ghghorg.atsassets

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.SellAllocation
import app.ghghorg.atsassets.core.Tab
import app.ghghorg.atsassets.core.TradeSide
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import app.ghghorg.atsassets.ui.AppState
import app.ghghorg.atsassets.ui.BottomBar
import app.ghghorg.atsassets.ui.CashFilterActions
import app.ghghorg.atsassets.ui.CashFilterForm
import app.ghghorg.atsassets.ui.CashScreen
import app.ghghorg.atsassets.ui.DEFAULT_TAB_ORDER
import app.ghghorg.atsassets.ui.Screen
import app.ghghorg.atsassets.ui.theme.ATSAssetSystemTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDate

private fun cashText(
    id: Int,
    vararg args: Any,
): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

private fun entriesShown(count: Int): String {
    val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
    return resources.getQuantityString(R.plurals.cash_entries_shown, count, count)
}

private fun cashTrade(
    id: String,
    type: TransactionType,
    date: String,
    seq: Long,
    totalCents: Long,
    ticker: String = "AAPL",
    reversesId: String? = null,
) = Transaction(
    id = id,
    portfolioId = "p1",
    ticker = ticker,
    type = type,
    date = LocalDate.parse(date),
    enteredSeq = seq,
    quantity = if (type == TransactionType.REVERSAL) BigDecimal.ZERO else BigDecimal("10"),
    totalCents = totalCents,
    reversesId = reversesId,
    allocations = if (type == TransactionType.SELL) listOf(SellAllocation("b1", Fraction.of(BigDecimal("10")))) else emptyList(),
)

private fun cashLine(
    trade: Transaction,
    cents: Long,
) = CashEntry("c-${trade.id}", trade.date, cents, CashType.TRADE, "", trade.enteredSeq, transactionId = trade.id)

// Example A in Main, then the sample file's Mar 20 buy of AAPL and its Apr 5 reversal (AC-49).
private val cashOpening = CashEntry("o1", LocalDate.of(2026, 1, 1), 1_000_000L, CashType.OPENING_BALANCE, "", 1L)
private val cb1 = cashTrade("b1", TransactionType.BUY, "2026-01-02", 2L, 100_000L)
private val cb2 = cashTrade("b2", TransactionType.BUY, "2026-02-01", 3L, 150_000L)
private val cs1 = cashTrade("s1", TransactionType.SELL, "2026-03-01", 4L, 210_000L)
private val cb3 = cashTrade("b3", TransactionType.BUY, "2026-03-20", 5L, 36_000L)
private val crv = cashTrade("rv", TransactionType.REVERSAL, "2026-04-05", 6L, 0L, reversesId = "b3")
private val cashTrades = listOf(cb1, cb2, cs1, cb3, crv)
private val cashLines =
    listOf(cashOpening, cashLine(cb1, -100_000L), cashLine(cb2, -150_000L), cashLine(cs1, 210_000L), cashLine(cb3, -36_000L), cashLine(crv, 36_000L))

private fun cashState(
    form: CashFilterForm = CashFilterForm(),
    transactions: List<Transaction> = cashTrades,
    entries: List<CashEntry> = cashLines,
    menuOpen: Boolean = false,
): AppState = AppState(
    screen = Screen.TABS,
    tab = Tab.CASH,
    entries = entries,
    transactions = transactions,
    portfolios = listOf(Portfolio("p1", "Main", 1L)),
    cashFilterForm = form,
    cashTickerMenuOpen = menuOpen,
)

// A filter chip is a Checkbox; the bottom bar's Cash tab, also selectable, is a Tab (CLAUDE.md; pull request #178).
private val chip: SemanticsMatcher = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox) and isSelectable()

private val directChips = listOf(R.string.cash_type_deposit, R.string.cash_type_withdrawal, R.string.cash_type_interest_dividend, R.string.cash_type_adjustment)

/**
 * The Cash tab with a ticker for TalkBack (28, AC-61 (d), AC-49): the Ticker field reads its ticker as a button; with AAPL
 * and Buy the reversal's line and the buys read their title, portfolio, date, signed amount and balance in full; the four
 * direct chips are disabled with their line, Buy and Sell enabled; the count line counts the rows.
 */
@RunWith(AndroidJUnit4::class)
class CashTickerTalkBackTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun withATicker_theRowsReadInFull_andTheDirectChipsAreDisabled() {
        val state = cashState(CashFilterForm(ticker = "AAPL", trade = TradeSide.BUY))
        composeRule.setContent { ATSAssetSystemTheme { CashScreen(state, {}, {}, {}, CashFilterActions({}, {}, {}, {})) } }
        composeRule.onNode(hasContentDescription(cashText(R.string.history_ticker_spoken, "AAPL")) and hasClickAction()).assertExists()
        directChips.forEach { composeRule.onNode(chip and hasText(string(it))).assertIsNotEnabled() }
        composeRule.onNode(chip and hasText(string(R.string.history_chip_buy))).assertIsEnabled().assertIsSelected()
        composeRule.onNode(chip and hasText(string(R.string.history_chip_sell))).assertIsEnabled()
        composeRule.onNodeWithText(string(R.string.cash_chips_unavailable)).assertExists()
        composeRule.onNodeWithText(entriesShown(4)).assertExists()
        val list = composeRule.onNode(hasScrollToNodeAction())
        val reversal = cashText(R.string.reversal_of_buy_title, "AAPL")
        val expected =
            listOf(
                cashText(R.string.trade_reversal_spoken, reversal, "Main", "2026-04-05", cashText(R.string.spoken_plus, "360.00"), "9,600.00"),
                cashText(R.string.trade_buy_spoken, "AAPL", "Main", "2026-03-20", cashText(R.string.spoken_minus, "360.00"), "9,240.00"),
                cashText(R.string.trade_buy_spoken, "AAPL", "Main", "2026-02-01", cashText(R.string.spoken_minus, "1,500.00"), "7,500.00"),
                cashText(R.string.trade_buy_spoken, "AAPL", "Main", "2026-01-02", cashText(R.string.spoken_minus, "1,000.00"), "9,000.00"),
            )
        expected.forEach { spoken ->
            val row = hasContentDescription(spoken) and hasClickAction()
            list.performScrollToNode(row)
            composeRule.onNode(row).assertExists()
        }
        val sell = cashText(R.string.trade_sell_spoken, "AAPL", "Main", "2026-03-01", cashText(R.string.spoken_plus, "2,100.00"), "9,600.00")
        composeRule.onNode(hasContentDescription(sell)).assertDoesNotExist()
    }
}

/**
 * AC-39, AC-61 (a): the Cash tab at the largest font in landscape, drawn above the bottom bar as in the app: From, To,
 * the Ticker field, each of the seven chips, the first and the last row reached, shown and tapped; with a ticker the
 * disabled chips, their line and Clear filters reached and shown; and the Ticker menu with twenty tickers, each reached
 * and tapped.
 */
@RunWith(AndroidJUnit4::class)
class CashTickerLargestFontLandscapeTest {
    private val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val rules: RuleChain =
        RuleChain
            .outerRule(DeviceUnlockedRule())
            .around(DeviceSettingsRule(fontScale = 2.0f))
            .around(composeRule)

    private fun show(
        state: AppState,
        actions: CashFilterActions,
        onOpenEntry: (String) -> Unit = {},
        onOpenTransaction: (String) -> Unit = {},
    ) {
        composeRule.setContent {
            ATSAssetSystemTheme {
                Surface {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f)) { CashScreen(state, {}, onOpenEntry, onOpenTransaction, actions) }
                        BottomBar(DEFAULT_TAB_ORDER, Tab.CASH, {})
                    }
                }
            }
        }
    }

    // Twelve buys of distinct tickers with distinct totals before the chain: more than the screen holds.
    private val many: List<Transaction> =
        (1..12).map { n -> cashTrade("t$n", TransactionType.BUY, "2026-01-%02d".format(n + 2), 10L + n, 1_000L * n, ticker = "T%02d".format(n)) }

    @Test
    fun theFieldsChipsAndRows_canBeReached_andTapped() {
        composeRule.turn(landscape = true)
        val types = mutableListOf<CashType?>()
        val sides = mutableListOf<TradeSide>()
        var menuOpened = false
        val entries = mutableListOf<String>()
        val transactions = mutableListOf<String>()
        val actions =
            CashFilterActions(
                onFromChange = {},
                onToChange = {},
                onTypeChange = { types += it },
                onClear = {},
                onTradeChange = { sides += it },
                onOpenTickerMenu = { menuOpened = true },
            )
        val state = cashState(transactions = many + cashTrades, entries = cashLines + many.map { cashLine(it, -it.totalCents) })
        show(state, actions, onOpenEntry = { entries += it }, onOpenTransaction = { transactions += it })
        composeRule.onNode(hasSetTextAction() and hasText(string(R.string.filter_from))).performScrollTo().assertIsDisplayed()
        composeRule.onNode(hasSetTextAction() and hasText(string(R.string.filter_to))).performScrollTo().assertIsDisplayed()
        val field = hasContentDescription(cashText(R.string.history_ticker_spoken, string(R.string.history_all_tickers))) and hasClickAction()
        composeRule.onNode(field).performScrollTo().assertIsDisplayed().performClick()
        val labels = listOf(R.string.filter_all) + directChips + listOf(R.string.history_chip_buy, R.string.history_chip_sell)
        labels.forEach { label -> composeRule.onNode(chip and hasText(string(label))).performScrollTo().assertIsDisplayed().performClick() }
        val list = composeRule.onNode(hasScrollToNodeAction())
        val reversal = cashText(R.string.reversal_of_buy_title, "AAPL")
        val first =
            hasContentDescription(cashText(R.string.trade_reversal_spoken, reversal, "Main", "2026-04-05", cashText(R.string.spoken_plus, "360.00"), "8,820.00")) and
                hasClickAction()
        list.performScrollToNode(first)
        composeRule.onNode(first).assertIsDisplayed().performClick()
        val opening = cashText(R.string.opening_balance)
        val last = hasContentDescription(cashText(R.string.cash_row_spoken, opening, "2026-01-01", cashText(R.string.spoken_plus, "10,000.00"), "10,000.00")) and hasClickAction()
        list.performScrollToNode(last)
        composeRule.onNode(last).assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals("The Ticker field took its tap", true, menuOpened)
        assertEquals(
            "All and the direct chips took their taps",
            listOf(null, CashType.DEPOSIT, CashType.WITHDRAWAL, CashType.INTEREST_DIVIDEND, CashType.ADJUSTMENT),
            types,
        )
        assertEquals("Buy and Sell took their taps", TradeSide.entries.toList(), sides)
        assertEquals("The reversal's line opened its transaction", listOf("rv"), transactions)
        assertEquals("The opening balance opened its detail", listOf("o1"), entries)
        composeRule.turn(landscape = false)
    }

    @Test
    fun withATicker_theDisabledChipsTheirLineAndClearFilters_canBeReached() {
        composeRule.turn(landscape = true)
        var cleared = false
        val actions = CashFilterActions(onFromChange = {}, onToChange = {}, onTypeChange = {}, onClear = { cleared = true })
        show(cashState(CashFilterForm(ticker = "AAPL")), actions)
        directChips.forEach { label -> composeRule.onNode(chip and hasText(string(label))).performScrollTo().assertIsDisplayed().assertIsNotEnabled() }
        composeRule.onNodeWithText(string(R.string.cash_chips_unavailable)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(entriesShown(5)).performScrollTo().assertIsDisplayed()
        composeRule.onNode(button(R.string.filter_clear)).performScrollTo().assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals("Clear filters took its tap", true, cleared)
        composeRule.turn(landscape = false)
    }

    @Test
    fun theTickerMenu_reachesEveryTicker() {
        composeRule.turn(landscape = true)
        val picked = mutableListOf<String?>()
        val actions = CashFilterActions(onFromChange = {}, onToChange = {}, onTypeChange = {}, onClear = {}, onPickTicker = { picked += it })
        val twenty = (1..20).map { n -> cashTrade("m$n", TransactionType.BUY, "2026-02-01", 100L + n, 1_000L, ticker = "M%02d".format(n)) }
        show(cashState(transactions = twenty, entries = listOf(cashOpening) + twenty.map { cashLine(it, -1_000L) }, menuOpen = true), actions)
        val all = string(R.string.history_all_tickers)
        composeRule.onNode(hasText(all) and hasClickAction()).performScrollTo().assertIsDisplayed().performClick()
        val tickers = twenty.map { it.ticker }
        tickers.forEach { ticker -> composeRule.onNode(hasText(ticker) and hasClickAction()).performScrollTo().assertIsDisplayed().performClick() }
        composeRule.waitForIdle()
        assertEquals("Each item took its tap", listOf<String?>(null) + tickers, picked)
        composeRule.turn(landscape = false)
    }
}

/**
 * CLAUDE.md's pitfall on layers and menus (07; 28 decision 2, AC-61 (b)): on the real MainActivity, unlocked, the Cash
 * tab's Ticker menu keeps the window's focus, so the cover screen does not replace it, and a tap on "All tickers"
 * reaches it. The filters show once an entry exists, so the opening balance is set first if it is not there; the
 * emulator has no trade (a buy needs the price source), so the menu holds "All tickers" only (28 risk 4).
 */
@RunWith(AndroidJUnit4::class)
class CashTickerMenuFocusTest {
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(DeviceUnlockedRule()).around(composeRule)

    @Test
    fun theTickerMenu_keepsTheFocus_andTakesATap() {
        composeRule.unlockApp()
        val setOpening = string(R.string.guided_set_opening_balance)
        if (composeRule.onAllNodesWithText(setOpening).fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithText(setOpening).performClick()
            composeRule.onNode(hasSetTextAction() and hasText(string(R.string.amount_label))).performTextInput("1000")
            composeRule.onNode(button(R.string.review_button)).performScrollTo().performClick()
            composeRule.onNode(button(R.string.confirm_button)).performScrollTo().performClick()
            composeRule.waitUntil(10_000) { composeRule.onAllNodes(button(R.string.confirm_button)).fetchSemanticsNodes().isEmpty() }
        }
        composeRule.onNode(isSelectable() and hasText(string(R.string.tab_cash))).performClick()
        val field = hasContentDescription(cashText(R.string.history_ticker_spoken, string(R.string.history_all_tickers))) and hasClickAction()
        composeRule.waitUntil(5_000) { composeRule.onAllNodes(field).fetchSemanticsNodes().isNotEmpty() }
        val item = hasText(string(R.string.history_all_tickers)) and hasClickAction()
        composeRule.assertFocusHeld("the Cash tab's Ticker menu") {
            composeRule.onNode(field).performScrollTo().performClick()
            composeRule.waitUntil(5_000) { composeRule.onAllNodes(item).fetchSemanticsNodes().isNotEmpty() }
        }
        composeRule.onNode(item).performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodes(item).fetchSemanticsNodes().isEmpty() }
    }
}
