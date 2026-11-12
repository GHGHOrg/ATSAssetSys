package app.ghghorg.atsassets

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.ghghorg.atsassets.core.AutoLock
import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.HistoryChip
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.SplitRatio
import app.ghghorg.atsassets.core.Tab
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import app.ghghorg.atsassets.ui.AppState
import app.ghghorg.atsassets.ui.BottomBar
import app.ghghorg.atsassets.ui.DEFAULT_TAB_ORDER
import app.ghghorg.atsassets.ui.HistoryRowModel
import app.ghghorg.atsassets.ui.HistoryShares
import app.ghghorg.atsassets.ui.Screen
import app.ghghorg.atsassets.ui.TransactionHistoryActions
import app.ghghorg.atsassets.ui.TransactionHistoryScreen
import app.ghghorg.atsassets.ui.theme.ATSAssetSystemTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDate

private fun text(
    id: Int,
    vararg args: Any,
): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

private fun noActions() = TransactionHistoryActions({}, {}, {}, {}, {}, {}, {}, {}, {})

private fun bought(
    id: String,
    ticker: String,
    date: String,
    seq: Long,
    quantity: String,
    totalCents: Long,
) = Transaction(id, "p1", ticker, TransactionType.BUY, LocalDate.parse(date), seq, BigDecimal(quantity), totalCents)

private fun lineOf(
    transaction: Transaction,
    cents: Long,
) = CashEntry("c-${transaction.id}", transaction.date, cents, CashType.TRADE, "", transaction.enteredSeq, transactionId = transaction.id)

// The sample file's Main and AAPL chain (AC-50): the Mar 1 sell, the Mar 20 buy, the Apr 1 split, the Apr 5 reversal.
private val s1 = Transaction("s1", "p1", "AAPL", TransactionType.SELL, LocalDate.parse("2026-03-01"), 4L, BigDecimal("12"), 210_000L)
private val b3 = bought("b3", "AAPL", "2026-03-20", 5L, "2", 36_000L)
private val sp = Transaction("sp", "p1", "AAPL", TransactionType.SPLIT, LocalDate.parse("2026-04-01"), 6L, BigDecimal.ZERO, 0L, split = SplitRatio(2, 1))
private val rv = Transaction("rv", "p1", "AAPL", TransactionType.REVERSAL, LocalDate.parse("2026-04-05"), 7L, BigDecimal.ZERO, 0L, reversesId = "b3")
private val chain = listOf(s1, b3, sp, rv)
private val chainLines = listOf(lineOf(s1, 210_000L), lineOf(b3, -36_000L), lineOf(rv, 36_000L))

private fun historyState(
    transactions: List<Transaction> = chain,
    entries: List<CashEntry> = chainLines,
    chip: HistoryChip = HistoryChip.ALL,
    fromText: String = "",
    toText: String = "",
    menuOpen: Boolean = false,
): AppState = AppState(
    screen = Screen.TRANSACTION_HISTORY,
    tab = Tab.PORTFOLIOS,
    autoLock = AutoLock.EVERY_LEAVE,
    portfolios = listOf(Portfolio("p1", "Main", 1L)),
    transactions = transactions,
    entries = entries,
    historyPortfolioId = "p1",
    historyFromText = fromText,
    historyToText = toText,
    historyChip = chip,
    historyTickerMenuOpen = menuOpen,
)

private fun shares(shares: HistoryShares): String = text(if (shares.one) R.string.holding_share_one else R.string.holding_shares, shares.text)

/**
 * Transaction history for TalkBack (27, AC-61 (d), AC-50): each row reads its type, ticker, date, quantity, total and cash
 * effect in full as a button; "Reversed on" and the cash-effect line are buttons naming their date or signed amount;
 * under Cash each line reads its name, date and signed amount; with no match, the plain line.
 */
@RunWith(AndroidJUnit4::class)
class TransactionHistoryTalkBackTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun show(state: AppState) {
        composeRule.setContent { ATSAssetSystemTheme { TransactionHistoryScreen(state.transactionHistoryScreen, noActions()) } }
    }

    private fun assertButton(spoken: String) {
        composeRule.onNode(hasContentDescription(spoken) and hasClickAction()).performScrollTo().assertExists()
    }

    @Test
    fun eachRow_readsItsFiguresInFull() {
        show(historyState())
        composeRule.onNode(isHeading() and hasText(string(R.string.history_title))).assertExists()
        composeRule.onNode(hasContentDescription(text(R.string.allocation_portfolio_spoken, "Main"))).assertExists()
        assertButton(text(R.string.history_ticker_spoken, string(R.string.history_all_tickers)))
        val sell = text(R.string.trade_sell_title, "AAPL")
        assertButton(text(R.string.history_trade_cash_spoken, sell, "2026-03-01", text(R.string.holding_shares, "12"), "2,100.00", text(R.string.spoken_plus, "2,100.00")))
        val buy = text(R.string.trade_buy_title, "AAPL")
        assertButton(text(R.string.history_trade_cash_spoken, buy, "2026-03-20", text(R.string.holding_shares, "2"), "360.00", text(R.string.spoken_minus, "360.00")))
        assertButton(text(R.string.history_reversed_on_spoken, "2026-04-05"))
        assertButton(text(R.string.history_cash_effect_spoken, text(R.string.spoken_minus, "360.00")))
        val split = text(R.string.transaction_split_title, "AAPL")
        assertButton(text(R.string.history_split_spoken, split, "2026-04-01", text(R.string.split_ratio, "2", "1")))
        val buyWord = string(R.string.transaction_type_buy_lower)
        assertButton(
            text(R.string.history_reversal_spoken, buyWord, "2026-03-20", "AAPL", "2026-04-05", text(R.string.holding_shares, "2"), "360.00", text(R.string.spoken_plus, "360.00")),
        )
        assertButton(text(R.string.history_cash_effect_spoken, text(R.string.spoken_plus, "360.00")))
    }

    @Test
    fun theCashChip_readsEachLine() {
        show(historyState(chip = HistoryChip.CASH))
        val reversal = text(R.string.reversal_of_buy_title, "AAPL")
        assertButton(text(R.string.history_cash_row_spoken, reversal, "2026-04-05", text(R.string.spoken_plus, "360.00")))
        val buy = text(R.string.history_cash_line_buy, "AAPL")
        assertButton(text(R.string.history_cash_row_spoken, buy, "2026-03-20", text(R.string.spoken_minus, "360.00")))
        val sell = text(R.string.history_cash_line_sell, "AAPL")
        assertButton(text(R.string.history_cash_row_spoken, sell, "2026-03-01", text(R.string.spoken_plus, "2,100.00")))
    }

    @Test
    fun noMatch_readsItsPlainLine_andNoRow() {
        show(historyState(fromText = "20260501", toText = "20260531"))
        composeRule.onNodeWithText(string(R.string.history_none)).assertExists()
        composeRule.onNode(hasContentDescription(text(R.string.history_reversed_on_spoken, "2026-04-05"))).assertDoesNotExist()
    }
}

// Twelve buys with distinct totals, then the chain: more than the screen holds at the largest font in landscape.
private val many: List<Transaction> = (1..12).map { n -> bought("t$n", "T%02d".format(n), "2026-02-%02d".format(n), 10L + n, "5", 10_000L * n) }
private val manyLines: List<CashEntry> = many.map { lineOf(it, -it.totalCents) } + chainLines

/**
 * AC-39, AC-61 (a): Transaction history at the largest font in landscape, drawn above the bottom bar as in the app: From,
 * To, the Ticker field, each chip, every row, every cash-effect line and the "Reversed on" link can be scrolled to, are
 * shown and take a tap; and the Ticker menu with twenty tickers, each reached and tapped.
 */
@RunWith(AndroidJUnit4::class)
class TransactionHistoryLargestFontLandscapeTest {
    private val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val rules: RuleChain =
        RuleChain
            .outerRule(DeviceUnlockedRule())
            .around(DeviceSettingsRule(fontScale = 2.0f))
            .around(composeRule)

    private fun show(
        state: AppState,
        actions: TransactionHistoryActions,
    ) {
        composeRule.setContent {
            ATSAssetSystemTheme {
                Surface {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f)) { TransactionHistoryScreen(state.transactionHistoryScreen, actions) }
                        BottomBar(DEFAULT_TAB_ORDER, Tab.PORTFOLIOS, {})
                    }
                }
            }
        }
    }

    private fun tap(spoken: String) {
        composeRule.onNode(hasContentDescription(spoken) and hasClickAction()).performScrollTo().assertIsDisplayed().performClick()
    }

    @Test
    fun theFieldsChipsRowsAndLinks_canBeReached_andTapped() {
        composeRule.turn(landscape = true)
        val chips = mutableListOf<HistoryChip>()
        var menuOpened = false
        val opened = mutableListOf<String>()
        val cashOpened = mutableListOf<String>()
        val actions =
            TransactionHistoryActions(
                onBack = {},
                onFromChange = {},
                onToChange = {},
                onChip = { chips += it },
                onOpenTickerMenu = { menuOpened = true },
                onCloseTickerMenu = {},
                onPickTicker = {},
                onOpenTransaction = { opened += it },
                onOpenCashLine = { cashOpened += it },
            )
        val state = historyState(transactions = many + chain, entries = manyLines)
        show(state, actions)
        composeRule.onNode(hasSetTextAction() and hasText(string(R.string.filter_from))).performScrollTo().assertIsDisplayed()
        composeRule.onNode(hasSetTextAction() and hasText(string(R.string.filter_to))).performScrollTo().assertIsDisplayed()
        tap(text(R.string.history_ticker_spoken, string(R.string.history_all_tickers)))
        val chipLabels = listOf(R.string.filter_all, R.string.history_chip_buy, R.string.history_chip_sell, R.string.history_chip_split, R.string.history_chip_cash)
        chipLabels.forEach { label -> composeRule.onNode(isSelectable() and hasText(string(label))).performScrollTo().assertIsDisplayed().performClick() }
        val expectedOpened = mutableListOf<String>()
        val expectedCash = mutableListOf<String>()
        state.transactionHistoryScreen.rows.forEach { row ->
            when (row) {
                is HistoryRowModel.Trade -> {
                    val title = text(if (row.sell) R.string.trade_sell_title else R.string.trade_buy_title, row.ticker)
                    val cash = checkNotNull(row.cash) { "a trade without its cash line: ${row.transactionId}" }
                    tap(text(R.string.history_trade_cash_spoken, title, row.date, shares(row.shares), row.spokenTotal, spokenOf(cash.text)))
                    expectedOpened += row.transactionId
                    row.reversedOn?.let {
                        tap(text(R.string.history_reversed_on_spoken, it.date))
                        expectedOpened += it.reversalId
                    }
                    tap(text(R.string.history_cash_effect_spoken, spokenOf(cash.text)))
                    expectedCash += cash.transactionId
                }

                is HistoryRowModel.Split -> {
                    val title = text(R.string.transaction_split_title, row.ticker)
                    tap(text(R.string.history_split_spoken, title, row.date, text(R.string.split_ratio, row.ratio.to.toString(), row.ratio.from.toString())))
                    expectedOpened += row.transactionId
                }

                is HistoryRowModel.Reversal -> {
                    val cash = checkNotNull(row.cash) { "a reversal of a buy without its cash line" }
                    val type = string(R.string.transaction_type_buy_lower)
                    val quantity = shares(checkNotNull(row.shares) { "a reversal of a buy without its quantity" })
                    tap(text(R.string.history_reversal_spoken, type, row.originalDate, row.ticker, row.date, quantity, row.spokenTotal.orEmpty(), spokenOf(cash.text)))
                    expectedOpened += row.transactionId
                    tap(text(R.string.history_cash_effect_spoken, spokenOf(cash.text)))
                    expectedCash += cash.transactionId
                }

                is HistoryRowModel.CashLine -> fail("no Cash chip here")
            }
        }
        composeRule.waitForIdle()
        assertEquals("The Ticker field took its tap", true, menuOpened)
        assertEquals("Each chip took its tap", HistoryChip.entries.toList(), chips)
        assertEquals("Each row and link took its tap", expectedOpened, opened)
        assertEquals("Each cash-effect line took its tap", expectedCash, cashOpened)
        // 27 step 6 (phone check 6): the reversal row opens the reversal itself, as the original's "Reversed on" link does.
        assertEquals("The reversal row and the original's link both open the reversal", 2, opened.count { it == "rv" })
        assertEquals("Only the original's own row opens the original", 1, opened.count { it == "b3" })
        composeRule.turn(landscape = false)
    }

    @Test
    fun theTickerMenu_reachesEveryTicker() {
        composeRule.turn(landscape = true)
        val picked = mutableListOf<String?>()
        val actions = TransactionHistoryActions({}, {}, {}, {}, {}, {}, { picked += it }, {}, {})
        val twenty = (1..20).map { n -> bought("m$n", "M%02d".format(n), "2026-02-01", 100L + n, "1", 1_000L) }
        show(historyState(transactions = twenty, entries = emptyList(), menuOpen = true), actions)
        val all = string(R.string.history_all_tickers)
        composeRule.onNode(hasText(all) and hasClickAction()).performScrollTo().assertIsDisplayed().performClick()
        val tickers = twenty.map { it.ticker }
        tickers.forEach { ticker -> composeRule.onNode(hasText(ticker) and hasClickAction()).performScrollTo().assertIsDisplayed().performClick() }
        composeRule.waitForIdle()
        assertEquals("Each item took its tap", listOf<String?>(null) + tickers, picked)
        composeRule.turn(landscape = false)
    }

    // "+$10.00" read as "plus 10.00", "−$13.33" as "minus 13.33".
    private fun spokenOf(shown: String): String {
        val digits = shown.substringAfter('$')
        return when {
            shown.startsWith("+") -> text(R.string.spoken_plus, digits)
            shown.startsWith("−") -> text(R.string.spoken_minus, digits)
            else -> digits
        }
    }
}

/**
 * CLAUDE.md's pitfall on layers and menus (07; 27 decision 3, AC-61 (b)): on the real MainActivity, unlocked, the Ticker
 * menu of a portfolio's Transaction history keeps the window's focus, so the cover screen does not replace it, and a tap
 * on "All tickers" reaches it. The emulator's portfolio has no transaction (a buy needs the price source), so the menu
 * holds "All tickers" only (27 risk 5).
 */
@RunWith(AndroidJUnit4::class)
class TransactionHistoryMenuFocusTest {
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(DeviceUnlockedRule()).around(composeRule)

    @Test
    fun theTickerMenu_keepsTheFocus_andTakesATap() {
        composeRule.unlockApp()
        composeRule.openNewPortfolioLayer()
        // Unique, so a run on data left by an earlier run is not refused as a duplicate.
        val name = "History ${System.currentTimeMillis() % 100_000}"
        composeRule.onNode(nameField()).performTextInput(name)
        composeRule.onNode(button(R.string.create_button)).performClick()
        if (!poll(15_000) { composeRule.onAllNodes(nameField()).fetchSemanticsNodes().isEmpty() }) {
            fail("New portfolio's layer was still open 15 s after Create; window focused: ${composeRule.windowFocused()}")
        }
        composeRule.onNode(hasContentDescription(name, substring = true) and hasClickAction()).performScrollTo().performClick()
        composeRule.onNode(hasContentDescription(string(R.string.history_link_spoken)) and hasClickAction()).performScrollTo().performClick()
        val field = hasContentDescription(text(R.string.history_ticker_spoken, string(R.string.history_all_tickers))) and hasClickAction()
        composeRule.waitUntil(5_000) { composeRule.onAllNodes(field).fetchSemanticsNodes().isNotEmpty() }
        val item = hasText(string(R.string.history_all_tickers)) and hasClickAction()
        composeRule.assertFocusHeld("the Ticker menu") {
            composeRule.onNode(field).performScrollTo().performClick()
            composeRule.waitUntil(5_000) { composeRule.onAllNodes(item).fetchSemanticsNodes().isNotEmpty() }
        }
        composeRule.onNode(item).performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodes(item).fetchSemanticsNodes().isEmpty() }
    }
}
