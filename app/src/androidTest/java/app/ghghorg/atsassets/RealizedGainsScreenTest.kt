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
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.ghghorg.atsassets.core.AutoLock
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.SellAllocation
import app.ghghorg.atsassets.core.Tab
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import app.ghghorg.atsassets.ui.AppState
import app.ghghorg.atsassets.ui.BottomBar
import app.ghghorg.atsassets.ui.DEFAULT_TAB_ORDER
import app.ghghorg.atsassets.ui.RealizedGainsActions
import app.ghghorg.atsassets.ui.RealizedGainsScreen
import app.ghghorg.atsassets.ui.Screen
import app.ghghorg.atsassets.ui.theme.ATSAssetSystemTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

private fun plural(
    id: Int,
    count: Int,
    vararg args: Any,
): String = InstrumentationRegistry.getInstrumentation().targetContext.resources.getQuantityString(id, count, *args)

private val noActions = RealizedGainsActions({}, {}, {}, {}, { _, _ -> }, {})

private fun bought(
    id: String,
    ticker: String,
    seq: Long,
    quantity: String = "10",
    totalCents: Long = 100_000L,
) = Transaction(id, "p1", ticker, TransactionType.BUY, LocalDate.of(2026, 1, 2), seq, BigDecimal(quantity), totalCents)

private fun sold(
    id: String,
    ticker: String,
    date: String,
    totalCents: Long,
    seq: Long,
    vararg picks: Pair<String, String>,
) = Transaction(
    id = id,
    portfolioId = "p1",
    ticker = ticker,
    type = TransactionType.SELL,
    date = LocalDate.parse(date),
    enteredSeq = seq,
    quantity = picks.fold(BigDecimal.ZERO) { sum, pick -> sum + BigDecimal(pick.second) },
    totalCents = totalCents,
    allocations = picks.map { SellAllocation(it.first, Fraction.of(BigDecimal(it.second))) },
)

// The sample file's sales: AAPL Mar 1 (+$800: 12 for $2,100, lots $1,300) and the hidden OLDCO Mar 10 (−$200).
private val sample =
    listOf(
        bought("a1", "AAPL", 2L, totalCents = 100_000L),
        bought("a2", "AAPL", 3L, totalCents = 150_000L),
        sold("as", "AAPL", "2026-03-01", 210_000L, 4L, "a1" to "10", "a2" to "2"),
        bought("o1", "OLDCO", 5L, quantity = "5", totalCents = 20_000L),
        sold("os", "OLDCO", "2026-03-10", 0L, 6L, "o1" to "5"),
    )

private fun gainsState(
    transactions: List<Transaction> = sample,
    fromText: String = "",
    toText: String = "",
    expanded: Set<Pair<String, String>> = emptySet(),
): AppState = AppState(
    screen = Screen.REALIZED_GAINS,
    tab = Tab.MORE,
    autoLock = AutoLock.EVERY_LEAVE,
    portfolios = listOf(Portfolio("p1", "Portfolio P", 1L)),
    transactions = transactions,
    gainsFromText = fromText,
    gainsToText = toText,
    gainsExpanded = expanded,
)

/**
 * Realized gains for TalkBack (26, AC-61 (d), AC-47): the total reads its figure with its sign; each holding row reads
 * its name, portfolio, count and total with its sign, as a button; an expanded sale reads its date, gain, quantity,
 * proceeds and cost; an empty range reads its plain line.
 */
@RunWith(AndroidJUnit4::class)
class RealizedGainsTalkBackTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun show(state: AppState) {
        composeRule.setContent { ATSAssetSystemTheme { RealizedGainsScreen(state.realizedGainsScreen, noActions) } }
    }

    @Test
    fun theTotalAndEachHoldingRow_readTheirFiguresWithTheirSigns() {
        show(gainsState())
        composeRule.onNode(isHeading() and hasText(string(R.string.realized_gains_title))).assertExists()
        composeRule.onNode(hasContentDescription(text(R.string.gains_total_spoken, text(R.string.spoken_plus, "600.00")))).assertExists()
        composeRule.onNodeWithText(plural(R.plurals.gains_count_all, 2, 2)).assertExists()
        val aapl = plural(R.plurals.gains_holding_spoken, 1, "AAPL", "Portfolio P", 1, text(R.string.spoken_plus, "800.00"))
        val oldco = plural(R.plurals.gains_holding_spoken, 1, "OLDCO", "Portfolio P", 1, text(R.string.spoken_minus, "200.00"))
        composeRule.onNode(hasContentDescription(aapl) and hasClickAction()).assertExists()
        composeRule.onNode(hasContentDescription(oldco) and hasClickAction()).assertExists()
    }

    @Test
    fun anExpandedSale_readsItsDateGainQuantityProceedsAndCost() {
        show(gainsState(expanded = setOf("p1" to "AAPL")))
        val sale = text(R.string.gains_sale_spoken, "2026-03-01", text(R.string.spoken_plus, "800.00"), "12", "2,100.00", "1,300.00")
        composeRule.onNode(hasContentDescription(sale) and hasClickAction()).performScrollTo().assertExists()
    }

    @Test
    fun anEmptyRange_readsItsPlainLine_andNoRow() {
        show(gainsState(fromText = "20260401", toText = "20260430"))
        composeRule.onNodeWithText(string(R.string.gains_none)).assertExists()
        composeRule.onNode(hasContentDescription(text(R.string.gains_total_spoken, "0.00"))).assertExists()
        composeRule.onNodeWithText(string(R.string.gains_by_holding)).assertDoesNotExist()
    }
}

// Twelve holdings with one sale each, and AAA with three, more than the screen holds at the largest font in landscape.
private val many: List<Transaction> =
    (1..12).flatMap { n ->
        val ticker = "T%02d".format(n)
        listOf(bought("b$n", ticker, 10L * n), sold("s$n", ticker, "2026-03-01", 60_000L, 10L * n + 1, "b$n" to "5"))
    } +
        listOf(
            bought("aa", "AAA", 200L),
            sold("aa1", "AAA", "2026-02-01", 20_000L, 201L, "aa" to "1"),
            sold("aa2", "AAA", "2026-03-01", 20_000L, 202L, "aa" to "1"),
            sold("aa3", "AAA", "2026-04-01", 20_000L, 203L, "aa" to "1"),
        )

/**
 * AC-39, AC-61 (a): Realized gains at the largest font in landscape, drawn above the bottom bar as in the app, with
 * thirteen holdings and AAA expanded: From, To and Clear dates, every holding row and each of AAA's sales can be
 * scrolled to, are shown and take a tap.
 */
@RunWith(AndroidJUnit4::class)
class RealizedGainsLargestFontLandscapeTest {
    private val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val rules: RuleChain =
        RuleChain
            .outerRule(DeviceUnlockedRule())
            .around(DeviceSettingsRule(fontScale = 2.0f))
            .around(composeRule)

    @Test
    fun theDatesEveryHoldingAndEachSale_canBeReached_andTapped() {
        composeRule.turn(landscape = true)
        var cleared = false
        val toggled = mutableListOf<String>()
        val opened = mutableListOf<String>()
        val actions = RealizedGainsActions({}, {}, {}, { cleared = true }, { _, ticker -> toggled += ticker }, { opened += it })
        val state = gainsState(transactions = many, expanded = setOf("p1" to "AAA"))
        composeRule.setContent {
            ATSAssetSystemTheme {
                Surface {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f)) { RealizedGainsScreen(state.realizedGainsScreen, actions) }
                        BottomBar(DEFAULT_TAB_ORDER, Tab.MORE, {})
                    }
                }
            }
        }
        composeRule.onNode(hasSetTextAction() and hasText(string(R.string.filter_from))).performScrollTo().assertIsDisplayed()
        composeRule.onNode(hasSetTextAction() and hasText(string(R.string.filter_to))).performScrollTo().assertIsDisplayed()
        composeRule.onNode(hasText(string(R.string.gains_clear_dates)) and hasClickAction()).performScrollTo().assertIsDisplayed().performClick()
        val holdings = state.realizedGainsScreen.holdings
        holdings.forEach { holding ->
            val spoken = plural(R.plurals.gains_holding_spoken, holding.count, holding.ticker, "Portfolio P", holding.count, spokenOf(holding.total.text))
            composeRule.onNode(hasContentDescription(spoken) and hasClickAction()).performScrollTo().assertIsDisplayed().performClick()
        }
        val sales = holdings.first { it.ticker == "AAA" }.sales
        sales.forEach { sale ->
            val spoken = text(R.string.gains_sale_spoken, sale.date, spokenOf(sale.gain.text), sale.quantity, sale.spokenProceeds, sale.spokenCost)
            composeRule.onNode(hasContentDescription(spoken) and hasClickAction()).performScrollTo().assertIsDisplayed().performClick()
        }
        composeRule.waitForIdle()
        assertTrue("Clear dates took its tap", cleared)
        assertEquals("Each holding row took its tap", holdings.map { it.ticker }, toggled)
        assertEquals("Each sale took its tap, newest first", listOf("aa3", "aa2", "aa1"), opened)
        composeRule.turn(landscape = false)
    }

    // "+$10.00" read as "plus 10.00", "−$13.33" as "minus 13.33", "$0.00" as "0.00".
    private fun spokenOf(shown: String): String {
        val digits = shown.substringAfter('$')
        return when {
            shown.startsWith("+") -> text(R.string.spoken_plus, digits)
            shown.startsWith("−") -> text(R.string.spoken_minus, digits)
            else -> digits
        }
    }
}
