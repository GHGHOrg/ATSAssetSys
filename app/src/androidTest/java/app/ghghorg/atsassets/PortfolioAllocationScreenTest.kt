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
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.ghghorg.atsassets.core.AssetType
import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.ClassLists
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.Instrument
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.Price
import app.ghghorg.atsassets.core.SellAllocation
import app.ghghorg.atsassets.core.SourceProfile
import app.ghghorg.atsassets.core.Tab
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import app.ghghorg.atsassets.ui.AppState
import app.ghghorg.atsassets.ui.BottomBar
import app.ghghorg.atsassets.ui.ChoiceLayer
import app.ghghorg.atsassets.ui.DEFAULT_TAB_ORDER
import app.ghghorg.atsassets.ui.PortfolioAllocationActions
import app.ghghorg.atsassets.ui.PortfolioAllocationScreen
import app.ghghorg.atsassets.ui.Screen
import app.ghghorg.atsassets.ui.theme.ATSAssetSystemTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

private fun text(
    id: Int,
    vararg args: Any,
): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

/**
 * CLAUDE.md's pitfall on layers and menus (07; 19, AC-61 (b)): on the real MainActivity, unlocked, Allocation's portfolio
 * chooser keeps the window's focus, so the cover screen does not replace it, and a tap on a portfolio reaches it.
 */
@RunWith(AndroidJUnit4::class)
class PortfolioChoiceFocusTest {
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(DeviceUnlockedRule()).around(composeRule)

    private val chooser = hasContentDescription(text(R.string.allocation_portfolio_spoken, ""), substring = true) and hasClickAction()

    @Test
    fun theChooser_keepsTheFocus_andTakesATap() {
        composeRule.unlockApp()
        // A fresh app shows the guided steps: set an opening balance first.
        val setOpening = string(R.string.guided_set_opening_balance)
        if (composeRule.onAllNodes(hasText(setOpening)).fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithText(setOpening).performClick()
            composeRule.onNode(hasSetTextAction() and hasText(string(R.string.amount_label))).performTextInput("1000")
            composeRule.onNode(button(R.string.review_button)).performScrollTo().performClick()
            composeRule.onNode(button(R.string.confirm_button)).performScrollTo().performClick()
            composeRule.waitUntil(10_000) { composeRule.onAllNodes(button(R.string.confirm_button)).fetchSemanticsNodes().isEmpty() }
        }
        composeRule.onNode(isSelectable() and hasText(string(R.string.tab_more))).performClick()
        composeRule.onNode(hasContentDescription(string(R.string.allocation_title)) and hasClickAction()).performClick()
        // With no portfolio yet, create one from this screen (wireframe 04 marker 4).
        val create = button(R.string.create_portfolio)
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(create).fetchSemanticsNodes().isNotEmpty() || composeRule.onAllNodes(chooser).fetchSemanticsNodes().isNotEmpty()
        }
        if (composeRule.onAllNodes(create).fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNode(create).performClick()
            composeRule.waitUntil(5_000) { composeRule.onAllNodes(nameField()).fetchSemanticsNodes().isNotEmpty() }
            composeRule.onNode(nameField()).performTextInput("Test ${System.currentTimeMillis() % 100_000}")
            composeRule.onNode(button(R.string.create_button)).performClick()
            composeRule.waitUntil(15_000) { composeRule.onAllNodes(chooser).fetchSemanticsNodes().isNotEmpty() }
        }
        // The chooser's options are radio buttons; the bottom bar's tabs are selectable too, but not radio buttons.
        val option = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton) and hasClickAction()
        composeRule.assertFocusHeld("the portfolio chooser") {
            composeRule.onNode(chooser).performScrollTo().performClick()
            composeRule.waitUntil(5_000) { composeRule.onAllNodes(option).fetchSemanticsNodes().isNotEmpty() }
        }
        composeRule.onAllNodes(option)[0].performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodes(option).fetchSemanticsNodes().isEmpty() }
    }
}

/**
 * Risk 1 (as defect 18-6, AC-61 (a)): the chooser with six portfolios at the largest font in landscape, drawn above the
 * bottom bar: each portfolio can be scrolled to, is shown and takes a tap.
 */
@RunWith(AndroidJUnit4::class)
class PortfolioChoiceLargestFontLandscapeTest {
    private val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val rules: RuleChain =
        RuleChain
            .outerRule(DeviceUnlockedRule())
            .around(DeviceSettingsRule(fontScale = 2.0f))
            .around(composeRule)

    @Test
    fun everyPortfolio_canBeReachedAndTapped() {
        composeRule.turn(landscape = true)
        val names = listOf("Main", "Retirement", "Trust", "Growth fund", "Kids", "Rainy day")
        var chosen: String? = null
        composeRule.setContent {
            ATSAssetSystemTheme {
                Surface {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f)) {
                            ChoiceLayer(string(R.string.allocation_portfolio), names.map { it to it }, "Main", onChoose = { chosen = it }, onClose = {})
                        }
                        BottomBar(DEFAULT_TAB_ORDER, Tab.MORE, {})
                    }
                }
            }
        }
        names.forEach { name ->
            composeRule.onNode(hasText(name) and isSelectable()).performScrollTo().assertIsDisplayed().performClick()
            composeRule.waitForIdle()
            assertEquals("The tap on $name reached it", name, chosen)
        }
        // Upright again before the next test (18, #150).
        composeRule.turn(landscape = false)
    }
}

/**
 * Allocation's TalkBack text (19, AC-61 (d), AC-46, AC-33): with Example A at $160, the value with its age line, the
 * chooser, and each row with its percentage in full; not buttons; at $0.00 one plain line; with no portfolio "No
 * portfolios yet" and Create a portfolio; offline "some stale" and the failed line.
 */
@RunWith(AndroidJUnit4::class)
class PortfolioAllocationTalkBackTest {
    @get:Rule
    val composeRule = createComposeRule()

    // 2026-10-29 16:00 New York.
    private val close = Instant.ofEpochSecond(1_793_304_000L)

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

    private fun line(
        transaction: Transaction,
        cents: Long,
    ): CashEntry = CashEntry("c-${transaction.id}", transaction.date, cents, CashType.TRADE, "", transaction.enteredSeq, transactionId = transaction.id)

    // Example A: 8 AAPL left in Portfolio P at a cost of $1,200; Portfolio Q empty; from 23b, AAPL's profile and the lists.
    private fun state(
        shownId: String?,
        price: Price = Price(BigDecimal("160"), close),
        refreshing: Boolean = false,
        refreshFailed: Boolean = false,
        profile: SourceProfile? = null,
        classLists: ClassLists = ClassLists(),
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
        val opening = CashEntry("o1", LocalDate.of(2026, 1, 1), 1_000_000L, CashType.OPENING_BALANCE, "", 1L)
        return AppState(
            screen = Screen.ALLOCATION,
            tab = Tab.MORE,
            entries = listOf(opening, line(b1, -100_000L), line(b2, -150_000L), line(s1, 210_000L)),
            transactions = listOf(b1, b2, s1),
            today = LocalDate.of(2026, 6, 1),
            portfolios = listOf(Portfolio("p1", "Portfolio P", 1L), Portfolio("p2", "Portfolio Q", 2L)),
            instruments = listOf(Instrument("AAPL", AssetType.STOCK, verified = true, price = price, profile = profile)),
            allocationPortfolioId = shownId,
            refreshing = refreshing,
            refreshFailed = refreshFailed,
            classLists = classLists,
        )
    }

    private fun show(state: AppState) {
        composeRule.setContent { ATSAssetSystemTheme { PortfolioAllocationScreen(state, PortfolioAllocationActions({}, {}, {}, {}, {}, {}, {})) } }
    }

    private fun row(
        name: String,
        amount: String,
        percent: String,
    ): String = text(R.string.allocation_row_spoken, name, amount, percent)

    private fun shown(value: String): Boolean = composeRule.onAllNodes(hasText(value)).fetchSemanticsNodes().isNotEmpty()

    // AC-46: Portfolio P at $160, $1,280.00 with its age; Stocks 100.0%, ETFs 0.0%; AAPL 100.0%; no cash; rows not buttons.
    @Test
    fun portfolioP_readsItsValueAndEachRowInFull() {
        show(state("p1"))
        val age = text(R.string.portfolio_prices_as_of_spoken, "2026-10-29 16:00")
        composeRule.onNode(hasContentDescription(text(R.string.net_worth_prices_spoken, text(R.string.allocation_value_spoken, "1,280.00"), age))).assertExists()
        composeRule.onNode(hasContentDescription(text(R.string.allocation_portfolio_spoken, "Portfolio P")) and hasClickAction()).assertExists()
        composeRule.onNode(hasContentDescription(row(text(R.string.allocation_stocks), "1,280.00", "100.0"))).assertExists()
        composeRule.onNode(hasContentDescription(row(text(R.string.allocation_etfs), "0.00", "0.0"))).assertExists()
        composeRule.onNode(hasContentDescription(row("AAPL", "1,280.00", "100.0"))).assertExists()
        listOf(R.string.allocation_by_asset_type, R.string.allocation_by_holding).forEach { assertEquals("${text(it)} shown", true, shown(text(it))) }
        assertEquals(
            "No cash row",
            true,
            composeRule.onAllNodes(hasContentDescription(text(R.string.net_worth_composition_cash), substring = true)).fetchSemanticsNodes().isEmpty(),
        )
        assertEquals(
            "A row is not a button here",
            true,
            composeRule.onAllNodes(hasContentDescription(row("AAPL", "1,280.00", "100.0")) and hasClickAction()).fetchSemanticsNodes().isEmpty(),
        )
    }

    // 23b, AC-46: Portfolio P's sector and geography at 100.0% each, the geography as the source spells it (decision 2);
    // before the source is asked, both read "Unclassified" (C14).
    @Test
    fun portfolioP_readsItsSectorAndGeography_orUnclassified() {
        show(state("p1", profile = SourceProfile("Technology", "US"), classLists = ClassLists(listOf("Technology"), listOf("US"))))
        listOf(R.string.allocation_by_sector, R.string.allocation_by_geography).forEach { assertEquals("${text(it)} shown", true, shown(text(it))) }
        composeRule.onNode(hasContentDescription(row("Technology", "1,280.00", "100.0"))).assertExists()
        composeRule.onNode(hasContentDescription(row("US", "1,280.00", "100.0"))).assertExists()
    }

    @Test
    fun portfolioP_notAskedYet_readsUnclassifiedInBoth() {
        show(state("p1"))
        val unclassified = composeRule.onAllNodes(hasContentDescription(row(text(R.string.class_unclassified), "1,280.00", "100.0")))
        assertEquals("Sector and geography each read Unclassified", 2, unclassified.fetchSemanticsNodes().size)
    }

    // Marker 5: Portfolio Q has no holding: $0.00 and one plain line, no breakdown.
    @Test
    fun aPortfolioWorthZero_showsOnePlainLine() {
        show(state("p2"))
        composeRule.onNode(hasContentDescription(text(R.string.allocation_value_spoken, "0.00"))).assertExists()
        assertEquals("The $0.00 line shown", true, shown(text(R.string.allocation_portfolio_zero)))
        assertEquals("No breakdown", false, shown(text(R.string.allocation_by_asset_type)))
    }

    // Marker 4: no portfolio.
    @Test
    fun withNoPortfolio_saysSo_withCreateAPortfolio() {
        show(state(null).copy(portfolios = emptyList(), transactions = emptyList()))
        assertEquals("No portfolios yet shown", true, shown(text(R.string.no_portfolios_yet)))
        composeRule.onNode(hasText(text(R.string.create_portfolio)) and hasClickAction()).assertExists()
    }

    // Marker 2 and AC-46: offline, "some stale", "Updating prices" and the failed line, no pop-up.
    @Test
    fun atAStalePrice_whileAFailedRefreshIsRetried_saysSomeStale_updating_andFailed() {
        show(state("p1", Price(BigDecimal("140"), close, stale = true), refreshing = true, refreshFailed = true))
        val age = text(R.string.portfolio_prices_as_of_some_stale_spoken, "2026-10-29 16:00")
        composeRule.onNode(hasContentDescription(text(R.string.net_worth_prices_spoken, text(R.string.allocation_value_spoken, "1,120.00"), age))).assertExists()
        assertEquals("The updating line shown", true, shown(text(R.string.prices_updating)))
        assertEquals("The refresh-failed line shown", true, shown(text(R.string.price_refresh_failed)))
    }
}
