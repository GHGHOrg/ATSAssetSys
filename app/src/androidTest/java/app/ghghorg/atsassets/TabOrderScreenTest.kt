package app.ghghorg.atsassets

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.ghghorg.atsassets.core.AutoLock
import app.ghghorg.atsassets.core.Tab
import app.ghghorg.atsassets.core.TabOrder
import app.ghghorg.atsassets.ui.AppState
import app.ghghorg.atsassets.ui.SaveRefusal
import app.ghghorg.atsassets.ui.Screen
import app.ghghorg.atsassets.ui.SettingsActions
import app.ghghorg.atsassets.ui.SettingsScreen
import app.ghghorg.atsassets.ui.TabOrderActions
import app.ghghorg.atsassets.ui.TabOrderScreen
import app.ghghorg.atsassets.ui.theme.ATSAssetSystemTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

private fun text(
    id: Int,
    vararg args: Any,
): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

private val overallFirst = TabOrder.of(listOf(Tab.OVERALL, Tab.MORE, Tab.CASH, Tab.PORTFOLIOS))!!

private fun tabOrderState(
    order: TabOrder = TabOrder.DEFAULT,
    screen: Screen = Screen.TAB_ORDER,
    refusal: SaveRefusal? = null,
): AppState = AppState(screen = screen, tab = Tab.MORE, autoLock = AutoLock.EVERY_LEAVE, tabOrder = order, tabOrderRefusal = refusal)

/**
 * The Tab order screen and Settings' row for TalkBack (22, AC-61 (d), AC-58): the row reads the order in force; each tab
 * reads its place, and its Up and Down name it (decision 2); Up on the first and Down on the last are inactive
 * (decision 1); a failed save says so.
 */
@RunWith(AndroidJUnit4::class)
class TabOrderTalkBackTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun show(state: AppState) {
        composeRule.setContent { ATSAssetSystemTheme { TabOrderScreen(state, TabOrderActions({}, {}, {})) } }
    }

    private fun button(
        id: Int,
        name: String,
    ): SemanticsNodeInteraction = composeRule.onNode(hasContentDescription(text(id, name)) and hasClickAction())

    @Test
    fun settingsTabOrderRow_readsTheOrderInForce() {
        val state = tabOrderState(overallFirst, Screen.SETTINGS)
        composeRule.setContent { ATSAssetSystemTheme { SettingsScreen(state, SettingsActions({}, {}, {}, {})) } }
        composeRule.onNode(hasText(string(R.string.settings_navigation))).assertExists()
        val names = listOf(R.string.overall_title, R.string.tab_more, R.string.tab_cash, R.string.tab_portfolios).map { string(it) }
        val order = text(R.string.tab_order_value, *names.toTypedArray())
        composeRule.onNode(hasContentDescription(text(R.string.settings_tab_order_spoken, order)) and hasClickAction()).assertExists()
    }

    @Test
    fun eachRow_readsItsPlace_andItsButtonsNameIt() {
        show(tabOrderState(overallFirst))
        val names = listOf(R.string.overall_title, R.string.tab_more, R.string.tab_cash, R.string.tab_portfolios).map { string(it) }
        names.forEachIndexed { index, name ->
            composeRule.onNode(hasContentDescription(text(R.string.tab_order_row_spoken, name, index + 1, 4))).assertExists()
            button(R.string.tab_order_up_spoken, name).assertExists()
            button(R.string.tab_order_down_spoken, name).assertExists()
        }
        composeRule.onNodeWithText(text(R.string.tab_order_note)).assertExists()
    }

    @Test
    fun upOnTheFirst_andDownOnTheLast_areInactive() {
        show(tabOrderState())
        button(R.string.tab_order_up_spoken, string(R.string.tab_more)).assertIsNotEnabled()
        button(R.string.tab_order_down_spoken, string(R.string.tab_more)).assertIsEnabled()
        button(R.string.tab_order_up_spoken, string(R.string.overall_title)).assertIsEnabled()
        button(R.string.tab_order_down_spoken, string(R.string.overall_title)).assertIsNotEnabled()
    }

    @Test
    fun aFailedSave_saysSo() {
        show(tabOrderState(refusal = SaveRefusal.SAVE_FAILED))
        composeRule.onNodeWithText(text(R.string.save_failed)).assertExists()
    }
}

/**
 * AC-58's "the bottom bar follows at once" on the real MainActivity, unlocked: More, the gear, Tab order, then Down on
 * More puts Cash left of More in the bar; Up on More puts the default order back, so later tests find it.
 */
@RunWith(AndroidJUnit4::class)
class TabOrderBarTest {
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(DeviceUnlockedRule()).around(composeRule)

    private fun barLeft(label: Int): Float = composeRule.onNode(isSelectable() and hasText(string(label))).fetchSemanticsNode().boundsInRoot.left

    private fun cashBeforeMore(): Boolean = barLeft(R.string.tab_cash) < barLeft(R.string.tab_more)

    @Test
    fun downOnMore_movesItInTheBar_andUpPutsItBack() {
        composeRule.unlockApp()
        composeRule.onNode(isSelectable() and hasText(string(R.string.tab_more))).performClick()
        composeRule.onNode(hasContentDescription(string(R.string.settings_title)) and hasClickAction()).performClick()
        val row = hasContentDescription(text(R.string.settings_tab_order_spoken, ""), substring = true) and hasClickAction()
        composeRule.waitUntil(5_000) { composeRule.onAllNodes(row).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNode(row).performScrollTo().performClick()
        val more = string(R.string.tab_more)
        val down = hasContentDescription(text(R.string.tab_order_down_spoken, more)) and hasClickAction()
        composeRule.waitUntil(5_000) { composeRule.onAllNodes(down).fetchSemanticsNodes().isNotEmpty() }
        assertTrue("More is left of Cash before the move", !cashBeforeMore())
        composeRule.onNode(down).performScrollTo().performClick()
        composeRule.waitUntil(5_000) { cashBeforeMore() }
        composeRule.onNode(hasContentDescription(text(R.string.tab_order_up_spoken, more)) and hasClickAction()).performScrollTo().performClick()
        composeRule.waitUntil(5_000) { !cashBeforeMore() }
    }
}
