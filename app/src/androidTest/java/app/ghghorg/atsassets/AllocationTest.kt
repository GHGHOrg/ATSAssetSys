package app.ghghorg.atsassets

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
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
import app.ghghorg.atsassets.ui.AllocationBy
import app.ghghorg.atsassets.ui.AllocationChoiceLayer
import app.ghghorg.atsassets.ui.AppState
import app.ghghorg.atsassets.ui.BottomBar
import app.ghghorg.atsassets.ui.DEFAULT_TAB_ORDER
import app.ghghorg.atsassets.ui.NetWorthActions
import app.ghghorg.atsassets.ui.NetWorthScreen
import app.ghghorg.atsassets.ui.OverallScreen
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

/**
 * CLAUDE.md's pitfall on layers and menus (07; 18, AC-61 (b)): on the real MainActivity, unlocked, Overall's "Allocation
 * by" choice keeps the window's focus, so the cover screen does not replace it, and a tap on an option reaches it.
 */
@RunWith(AndroidJUnit4::class)
class AllocationChoiceFocusTest {
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(DeviceUnlockedRule()).around(composeRule)

    private fun choiceButton(by: Int): SemanticsMatcher {
        val spoken = InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.allocation_by_spoken, string(by))
        return hasContentDescription(spoken) and hasClickAction()
    }

    @Test
    fun theChoice_keepsTheFocus_andTakesATap() {
        composeRule.unlockApp()
        // A fresh app shows the guided steps, not the allocation: set an opening balance first.
        val setOpening = string(R.string.guided_set_opening_balance)
        if (composeRule.onAllNodes(hasText(setOpening)).fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithText(setOpening).performClick()
            composeRule.onNode(hasSetTextAction() and hasText(string(R.string.amount_label))).performTextInput("1000")
            composeRule.onNode(button(R.string.review_button)).performScrollTo().performClick()
            composeRule.onNode(button(R.string.confirm_button)).performScrollTo().performClick()
            composeRule.waitUntil(10_000) { composeRule.onAllNodes(button(R.string.confirm_button)).fetchSemanticsNodes().isEmpty() }
        }
        composeRule.onNode(isSelectable() and hasText(string(R.string.overall_title))).performClick()
        // Each unlock starts on Asset type (view 1).
        composeRule.waitUntil(5_000) { composeRule.onAllNodes(choiceButton(R.string.allocation_asset_type)).fetchSemanticsNodes().isNotEmpty() }
        val holding = hasText(string(R.string.allocation_holding)) and hasClickAction()
        composeRule.assertFocusHeld("the Allocation by choice") {
            composeRule.onNode(choiceButton(R.string.allocation_asset_type)).performScrollTo().performClick()
            composeRule.waitUntil(5_000) { composeRule.onAllNodes(holding).fetchSemanticsNodes().isNotEmpty() }
        }
        composeRule.onNode(holding).performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodes(choiceButton(R.string.allocation_holding)).fetchSemanticsNodes().isNotEmpty() }
        assertEquals("The layer closed after the choice", true, composeRule.onAllNodes(holding).fetchSemanticsNodes().isEmpty())
    }
}

/**
 * Defect 18-6 (18 phone check 6, item 3): at the largest font in landscape the "Allocation by" card showed only its first
 * option; the other two could not be read or tapped. Drawn as on Overall, above the bottom bar, turned and at font scale
 * 2.0 (AC-61 (a)): each option can be scrolled to, is shown and takes a tap; from 23b, five options.
 */
@RunWith(AndroidJUnit4::class)
class AllocationChoiceLargestFontLandscapeTest {
    private val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val rules: RuleChain =
        RuleChain
            .outerRule(DeviceUnlockedRule())
            .around(DeviceSettingsRule(fontScale = 2.0f))
            .around(composeRule)

    @Test
    fun everyOption_canBeReachedAndTapped() {
        composeRule.turn(landscape = true)
        var chosen: AllocationBy? = null
        composeRule.setContent {
            ATSAssetSystemTheme {
                Surface {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f)) {
                            AllocationChoiceLayer(AllocationBy.ASSET_TYPE, onChoose = { chosen = it }, onClose = {})
                        }
                        BottomBar(DEFAULT_TAB_ORDER, Tab.OVERALL, {})
                    }
                }
            }
        }
        listOf(
            AllocationBy.ASSET_TYPE to R.string.allocation_asset_type,
            AllocationBy.PORTFOLIO to R.string.allocation_portfolio,
            AllocationBy.HOLDING to R.string.allocation_holding,
            // 23b: five options.
            AllocationBy.SECTOR to R.string.allocation_sector,
            AllocationBy.GEOGRAPHY to R.string.allocation_geography,
        ).forEach { (by, id) ->
            val option = composeRule.onNode(hasText(string(id)) and isSelectable())
            option.performScrollTo().assertIsDisplayed().performClick()
            composeRule.waitForIdle()
            assertEquals("The tap on ${string(id)} reached its option", by, chosen)
        }
        // Upright again before the next test: turning back after it re-created its activity (#150, AllocationTalkBackTest).
        composeRule.turn(landscape = false)
    }
}

/**
 * The allocation's rows (18, AC-61 (d), AC-43, AC-32): with Example A at $160, each row's TalkBack text reads its name,
 * amount and percentage in full; on Overall the rows are buttons that open Net Worth (Total), and the choice reads its
 * option; Net Worth (Total) shows the three breakdowns; at $0.00 one
 * plain line stands in.
 */
@RunWith(AndroidJUnit4::class)
class AllocationTalkBackTest {
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

    private fun line(
        transaction: Transaction,
        cents: Long,
    ): CashEntry = CashEntry("c-${transaction.id}", transaction.date, cents, CashType.TRADE, "", transaction.enteredSeq, transactionId = transaction.id)

    // Example A: $9,600.00 cash and 8 AAPL left in Portfolio P at $160; from 23b, AAPL's source profile and the lists.
    private fun state(
        screen: Screen,
        by: AllocationBy = AllocationBy.ASSET_TYPE,
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
            screen = screen,
            tab = Tab.OVERALL,
            entries = listOf(opening, line(b1, -100_000L), line(b2, -150_000L), line(s1, 210_000L)),
            transactions = listOf(b1, b2, s1),
            today = LocalDate.of(2026, 6, 1),
            portfolios = listOf(Portfolio("p1", "Portfolio P", 1L)),
            instruments = listOf(Instrument("AAPL", AssetType.STOCK, verified = true, price = Price(BigDecimal("160"), close), profile = profile)),
            allocationBy = by,
            classLists = classLists,
        )
    }

    private fun row(
        name: String,
        amount: String,
        percent: String,
    ): String = text(R.string.allocation_row_spoken, name, amount, percent)

    private fun shown(text: String): Boolean = composeRule.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()

    // AC-43: Asset type, 88.2% and 11.8% on the donut, ETFs 0.0% in its row; each row a button that opens Net Worth (Total).
    @Test
    fun overall_byAssetType_readsEachRowInFull_asAButtonThatOpensNetWorth() {
        var opened = 0
        composeRule.setContent { ATSAssetSystemTheme { OverallScreen(state(Screen.TABS), {}, {}, onOpenNetWorth = { opened++ }) } }
        composeRule.onNode(hasContentDescription(text(R.string.allocation_by_spoken, text(R.string.allocation_asset_type))) and hasClickAction()).assertExists()
        composeRule.onNode(hasContentDescription(row(text(R.string.allocation_stocks), "1,280.00", "11.8")) and hasClickAction()).assertExists()
        composeRule.onNode(hasContentDescription(row(text(R.string.allocation_etfs), "0.00", "0.0")) and hasClickAction()).assertExists()
        // Where a percentage shows (in the row only under 5%) is not visible here: the row replaces its texts with its
        // TalkBack sentence. AllocationModelTest checks it (percentInRow) (18, your A after #150).
        composeRule.onNode(hasContentDescription(row(text(R.string.net_worth_composition_cash), "9,600.00", "88.2")) and hasClickAction()).performClick()
        assertEquals("Opened from a row", 1, opened)
    }

    @Test
    fun overall_byHolding_readsTheTickersRow() {
        composeRule.setContent { ATSAssetSystemTheme { OverallScreen(state(Screen.TABS, AllocationBy.HOLDING), {}, {}) } }
        composeRule.onNode(hasContentDescription(text(R.string.allocation_by_spoken, text(R.string.allocation_holding)))).assertExists()
        composeRule.onNode(hasContentDescription(row("AAPL", "1,280.00", "11.8"))).assertExists()
    }

    // Wireframe 02 markers 3 and 4: the three breakdowns under the composition, rows read in full, not buttons.
    @Test
    fun netWorth_showsTheThreeBreakdowns_withEachRowReadInFull() {
        composeRule.setContent { ATSAssetSystemTheme { NetWorthScreen(state(Screen.NET_WORTH), NetWorthActions({}, {})) } }
        listOf(R.string.allocation_by_asset_type, R.string.allocation_by_portfolio, R.string.allocation_by_holding).forEach {
            assertEquals("${text(it)} shown", true, shown(text(it)))
        }
        composeRule.onNode(hasContentDescription(row("Portfolio P", "1,280.00", "11.8"))).assertExists()
        composeRule.onNode(hasContentDescription(row("AAPL", "1,280.00", "11.8"))).assertExists()
        assertEquals(
            "A row is not a button here",
            true,
            composeRule.onAllNodes(hasContentDescription(row("AAPL", "1,280.00", "11.8")) and hasClickAction()).fetchSemanticsNodes().isEmpty(),
        )
    }

    // 23b, AC-44: the sector and geography breakdowns under the other three, the source's values in the lists' spelling.
    @Test
    fun netWorth_showsSectorAndGeography_withTheSourcesValuesReadInFull() {
        val state = state(Screen.NET_WORTH, profile = SourceProfile("technology", "US"), classLists = ClassLists(listOf("Technology"), listOf("US")))
        composeRule.setContent { ATSAssetSystemTheme { NetWorthScreen(state, NetWorthActions({}, {})) } }
        listOf(R.string.allocation_by_sector, R.string.allocation_by_geography).forEach { assertEquals("${text(it)} shown", true, shown(text(it))) }
        composeRule.onNode(hasContentDescription(row("Technology", "1,280.00", "11.8"))).assertExists()
        composeRule.onNode(hasContentDescription(row("US", "1,280.00", "11.8"))).assertExists()
    }

    // 23b, C14 and AC-32: a holding with no value is read as "Unclassified"; cash keeps its own row.
    @Test
    fun overall_bySector_readsAHoldingWithNoValueAsUnclassified() {
        composeRule.setContent { ATSAssetSystemTheme { OverallScreen(state(Screen.TABS, AllocationBy.SECTOR), {}, {}) } }
        composeRule.onNode(hasContentDescription(text(R.string.allocation_by_spoken, text(R.string.allocation_sector)))).assertExists()
        composeRule.onNode(hasContentDescription(row(text(R.string.class_unclassified), "1,280.00", "11.8"))).assertExists()
        composeRule.onNode(hasContentDescription(row(text(R.string.net_worth_composition_cash), "9,600.00", "88.2"))).assertExists()
    }

    // 4.8: net worth of $0.00 (cash withdrawn to 0, no holding): one plain line in place of the allocation.
    @Test
    fun overall_atZero_showsOnePlainLine_inPlaceOfTheAllocation() {
        val opening = CashEntry("o1", LocalDate.of(2026, 1, 1), 100_000L, CashType.OPENING_BALANCE, "", 1L)
        val out = CashEntry("w1", LocalDate.of(2026, 1, 2), -100_000L, CashType.WITHDRAWAL, "", 2L)
        val state = AppState(screen = Screen.TABS, tab = Tab.OVERALL, entries = listOf(opening, out), today = LocalDate.of(2026, 6, 1))
        composeRule.setContent { ATSAssetSystemTheme { OverallScreen(state, {}, {}) } }
        assertEquals("The $0.00 line shown", true, shown(text(R.string.net_worth_zero)))
        assertEquals(
            "No allocation row",
            true,
            composeRule.onAllNodes(hasContentDescription(text(R.string.net_worth_composition_cash), substring = true)).fetchSemanticsNodes().isEmpty(),
        )
    }
}
