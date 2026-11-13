package app.ghghorg.atsassets

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.DeletionCheck
import app.ghghorg.atsassets.core.DeletionSummary
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.Tab
import app.ghghorg.atsassets.ui.AppState
import app.ghghorg.atsassets.ui.BottomBar
import app.ghghorg.atsassets.ui.DEFAULT_TAB_ORDER
import app.ghghorg.atsassets.ui.DeleteDialog
import app.ghghorg.atsassets.ui.DeleteDialogActions
import app.ghghorg.atsassets.ui.PortfolioScreen
import app.ghghorg.atsassets.ui.PortfolioScreenActions
import app.ghghorg.atsassets.ui.Screen
import app.ghghorg.atsassets.ui.theme.ATSAssetSystemTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.time.LocalDate

private fun deleteText(
    id: Int,
    vararg args: Any,
): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

private val portfolioP = Portfolio("p1", "Portfolio P", 1L)

// Example A's summary (AC-55): 3 transactions, 3 trade cash lines, +$800.00, $9,600.00 to $10,000.00.
private val exampleA = DeletionSummary(portfolioP, 3, 3, Fraction.ofCents(80_000L), 960_000L, 1_000_000L)

private fun portfolioState(
    dialog: DeleteDialog? = null,
    blocked: DeletionCheck? = null,
): AppState = AppState(
    screen = Screen.PORTFOLIO,
    tab = Tab.PORTFOLIOS,
    entries = listOf(CashEntry("o1", LocalDate.of(2026, 1, 1), 1_000_000L, CashType.OPENING_BALANCE, "", 1L)),
    portfolios = listOf(portfolioP),
    portfolioId = "p1",
    deleteDialog = dialog,
    deleteBlocked = blocked,
)

private fun noActions(
    dialog: DeleteDialogActions = DeleteDialogActions({}, {}, {}),
    onCloseBlocked: () -> Unit = {},
) = PortfolioScreenActions(
    onBack = {},
    onOpenMenu = {},
    onCloseMenu = {},
    onRename = {},
    onOpenHolding = {},
    onOpenHidden = {},
    deleteDialog = dialog,
    onCloseBlocked = onCloseBlocked,
)

private val confirm = hasText(string(R.string.delete_confirm_button)) and hasClickAction()

/**
 * The Delete dialog and the blocked message for TalkBack (29, AC-61 (d), AC-55): the title as a heading, the summary as
 * one sentence with Example A's figures, Delete portfolio unavailable with its line until the name is typed, then
 * available; Example E's message read as written with Close.
 */
@RunWith(AndroidJUnit4::class)
class PortfolioDeleteTalkBackTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theSummary_readsItsFigures_andDeletePortfolio_waitsForTheName() {
        composeRule.setContent { ATSAssetSystemTheme { PortfolioScreen(portfolioState(DeleteDialog(exampleA)), noActions()) } }
        composeRule.onNode(isHeading() and hasText(deleteText(R.string.delete_title, "Portfolio P"))).assertExists()
        val summary =
            deleteText(R.string.delete_summary_spoken, 3, 3, deleteText(R.string.spoken_plus, "800.00"), "9,600.00", "10,000.00")
        composeRule.onNode(hasContentDescription(summary)).assertExists()
        composeRule.onNode(confirm).assertIsNotEnabled()
        composeRule.onNodeWithText(string(R.string.delete_not_matching)).assertExists()
    }

    @Test
    fun withTheName_deletePortfolio_isAvailable() {
        val state = portfolioState(DeleteDialog(exampleA, typedName = "portfolio p"))
        composeRule.setContent { ATSAssetSystemTheme { PortfolioScreen(state, noActions()) } }
        composeRule.onNode(confirm).assertIsEnabled()
        composeRule.onNodeWithText(string(R.string.delete_not_matching)).assertDoesNotExist()
    }

    @Test
    fun theBlockedMessage_readsAsWritten_withClose() {
        val state = portfolioState(blocked = DeletionCheck.Blocked(LocalDate.parse("2026-02-10"), 20_000L))
        composeRule.setContent { ATSAssetSystemTheme { PortfolioScreen(state, noActions()) } }
        composeRule.onNodeWithText(deleteText(R.string.delete_blocked, "2026-02-10", "$200.00", "2026-02-09")).assertExists()
        composeRule.onNode(button(R.string.close_button)).assertExists()
        composeRule.onNode(confirm).assertDoesNotExist()
    }
}

/**
 * AC-39, AC-61 (a): the Delete dialog and the blocked message at the largest font in landscape, above the bottom bar as
 * in the app: the title, the summary, the warning, the field, Cancel and Delete portfolio reached, shown and tapped;
 * the message and Close reached, shown and tapped.
 */
@RunWith(AndroidJUnit4::class)
class PortfolioDeleteLargestFontLandscapeTest {
    private val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val rules: RuleChain =
        RuleChain
            .outerRule(DeviceUnlockedRule())
            .around(DeviceSettingsRule(fontScale = 2.0f))
            .around(composeRule)

    private fun show(
        state: AppState,
        actions: PortfolioScreenActions,
    ) {
        composeRule.setContent {
            ATSAssetSystemTheme {
                Surface {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f)) { PortfolioScreen(state, actions) }
                        BottomBar(DEFAULT_TAB_ORDER, Tab.PORTFOLIOS, {})
                    }
                }
            }
        }
    }

    @Test
    fun theDialog_canBeReached_andTapped() {
        composeRule.turn(landscape = true)
        var cancelled = false
        var confirmed = false
        val actions = noActions(DeleteDialogActions(onNameChange = {}, onConfirm = { confirmed = true }, onCancel = { cancelled = true }))
        show(portfolioState(DeleteDialog(exampleA, typedName = "Portfolio P")), actions)
        composeRule.onNode(isHeading() and hasText(deleteText(R.string.delete_title, "Portfolio P"))).performScrollTo().assertIsDisplayed()
        val summary =
            deleteText(R.string.delete_summary_spoken, 3, 3, deleteText(R.string.spoken_plus, "800.00"), "9,600.00", "10,000.00")
        composeRule.onNode(hasContentDescription(summary)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.delete_warning)).performScrollTo().assertIsDisplayed()
        composeRule.onNode(hasSetTextAction() and hasText(string(R.string.portfolio_name_label))).performScrollTo().assertIsDisplayed()
        composeRule.onNode(button(R.string.cancel_button)).performScrollTo().assertIsDisplayed().performClick()
        composeRule.onNode(confirm).performScrollTo().assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertTrue("Cancel took its tap", cancelled)
        assertTrue("Delete portfolio took its tap", confirmed)
        composeRule.turn(landscape = false)
    }

    @Test
    fun theBlockedMessage_canBeReached_andClosed() {
        composeRule.turn(landscape = true)
        var closed = false
        val state = portfolioState(blocked = DeletionCheck.CannotDelete(LocalDate.parse("2026-01-01"), 30_000L))
        show(state, noActions(onCloseBlocked = { closed = true }))
        composeRule.onNodeWithText(deleteText(R.string.delete_cannot, "2026-01-01", "$300.00")).performScrollTo().assertIsDisplayed()
        composeRule.onNode(button(R.string.close_button)).performScrollTo().assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals("Close took its tap", true, closed)
        composeRule.turn(landscape = false)
    }
}

/**
 * CLAUDE.md's pitfall on layers (07; 29, AC-61 (b)) and the removal on the real app (risk 1): on MainActivity, unlocked,
 * a new portfolio (no transactions: the summary's three zeros, AC-55) is opened, its ⋮ menu's Delete opens the dialog
 * and the window keeps its focus; its name is typed, Delete portfolio removes it, and the Portfolios tab no longer lists
 * it.
 */
@RunWith(AndroidJUnit4::class)
class PortfolioDeleteFocusTest {
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(DeviceUnlockedRule()).around(composeRule)

    @Test
    fun theDialog_keepsTheFocus_andDeletesTheNewPortfolio() {
        composeRule.unlockApp()
        val name = createPortfolio(composeRule, "Delete")
        composeRule.onNode(hasContentDescription(name, substring = true) and hasClickAction()).performScrollTo().performClick()
        composeRule.onNode(hasContentDescription(string(R.string.portfolio_menu)) and hasClickAction()).performClick()
        val field = hasSetTextAction() and hasText(string(R.string.portfolio_name_label))
        composeRule.assertFocusHeld("the Delete dialog") {
            composeRule.onNode(button(R.string.portfolio_delete_button)).performClick()
            composeRule.waitUntil(5_000) { composeRule.onAllNodes(field).fetchSemanticsNodes().isNotEmpty() }
        }
        composeRule.onNode(field).performTextInput(name)
        composeRule.onNode(confirm).performScrollTo().performClick()
        composeRule.waitUntil(15_000) { composeRule.onAllNodes(field).fetchSemanticsNodes().isEmpty() }
        val row = hasContentDescription(name, substring = true) and hasClickAction()
        composeRule.waitUntil(5_000) { composeRule.onAllNodes(row).fetchSemanticsNodes().isEmpty() }
    }
}

/**
 * AC-61 (c): at the largest font, with the keyboard open on the Delete dialog's Name field, the field lies fully above
 * the keyboard (the layer's imePadding).
 */
@RunWith(AndroidJUnit4::class)
class PortfolioDeleteKeyboardTest {
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain =
        RuleChain
            .outerRule(DeviceUnlockedRule())
            .around(DeviceSettingsRule(fontScale = 2.0f))
            .around(composeRule)

    @Test
    fun theNameField_staysAboveTheKeyboard() {
        composeRule.unlockApp()
        val name = createPortfolio(composeRule, "Keys")
        composeRule.onNode(hasContentDescription(name, substring = true) and hasClickAction()).performScrollTo().performClick()
        composeRule.onNode(hasContentDescription(string(R.string.portfolio_menu)) and hasClickAction()).performClick()
        composeRule.onNode(button(R.string.portfolio_delete_button)).performClick()
        val field = hasSetTextAction() and hasText(string(R.string.portfolio_name_label))
        composeRule.waitUntil(5_000) { composeRule.onAllNodes(field).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNode(field).performScrollTo().performClick()
        val keyboardTop = composeRule.keyboardTop()
        composeRule.waitForIdle()
        val bottom = composeRule.onNode(field).fetchSemanticsNode().boundsInWindow.bottom
        assertTrue("The Name field ends at $bottom px, below the keyboard's top at $keyboardTop px", bottom <= keyboardTop)
    }
}

/**
 * 29 step 10 (phone check 9, versionCode 39): at the largest font in landscape, the Delete dialog vanished once the
 * keyboard opened on its Name field. The dialog was drawn above the bottom bar, and its layer's imePadding, measured from
 * the window's bottom, took the bar's height as well as the keyboard's. On the real MainActivity: the dialog opened in
 * portrait, then turned (as on the phone), the field tapped; it must show, fully above the keyboard.
 */
@RunWith(AndroidJUnit4::class)
class PortfolioDeleteKeyboardLandscapeTest {
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain =
        RuleChain
            .outerRule(DeviceUnlockedRule())
            .around(DeviceSettingsRule(fontScale = 2.0f))
            .around(composeRule)

    @Test
    fun theNameField_staysAboveTheKeyboard_inLandscape() {
        composeRule.unlockApp()
        val name = createPortfolio(composeRule, "Wide")
        openDelete(composeRule, name)
        composeRule.turn(landscape = true)
        composeRule.onNode(nameField()).performScrollTo().performClick()
        val keyboardTop = composeRule.keyboardTop()
        composeRule.waitForIdle()
        composeRule.onNode(nameField()).assertIsDisplayed()
        val bounds = composeRule.onNode(nameField()).fetchSemanticsNode().boundsInWindow
        assertTrue("The Name field ends at ${bounds.bottom} px, below the keyboard's top at $keyboardTop px", bounds.bottom <= keyboardTop)
        composeRule.turn(landscape = false)
    }
}

/**
 * 29 step 10 (phone check 9): while the Delete dialog is open, the bottom bar under it takes no tap and TalkBack does not
 * reach it, as for Rename's dialog (08). On the phone a tab took a tap behind the dialog. The blocked message is drawn
 * by the same code; it cannot be reached on the real app here (it needs a trade, which needs the price source).
 */
@RunWith(AndroidJUnit4::class)
class PortfolioDeleteOverTheBarTest {
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(DeviceUnlockedRule()).around(composeRule)

    private val cashTab = isSelectable() and hasText(string(R.string.tab_cash))

    @Test
    fun theBottomBar_takesNoTap_whileTheDialogIsOpen() {
        composeRule.unlockApp()
        val name = createPortfolio(composeRule, "Tap")
        val cashAt = composeRule.onNode(cashTab).fetchSemanticsNode().boundsInRoot.center
        openDelete(composeRule, name)
        composeRule.onRoot().performTouchInput { click(cashAt) }
        composeRule.waitForIdle()
        assertTrue(
            "A tap on the Cash tab under the Delete dialog closed the dialog",
            composeRule.onAllNodes(nameField()).fetchSemanticsNodes().isNotEmpty(),
        )
    }

    @Test
    fun theBottomBar_isHiddenFromTalkBack_whileTheDialogIsOpen() {
        composeRule.unlockApp()
        val name = createPortfolio(composeRule, "Read")
        openDelete(composeRule, name)
        assertTrue(
            "TalkBack still reaches the bottom bar's Cash tab under the Delete dialog",
            composeRule.onAllNodes(cashTab).fetchSemanticsNodes().isEmpty(),
        )
    }
}

/** From the Portfolios tab: [name]'s screen, its ⋮ menu, Delete, and the dialog's Name field shown. */
private fun openDelete(
    composeRule: MainRule,
    name: String,
) {
    composeRule.onNode(hasContentDescription(name, substring = true) and hasClickAction()).performScrollTo().performClick()
    composeRule.onNode(hasContentDescription(string(R.string.portfolio_menu)) and hasClickAction()).performClick()
    composeRule.onNode(button(R.string.portfolio_delete_button)).performClick()
    composeRule.waitUntil(5_000) { composeRule.onAllNodes(nameField()).fetchSemanticsNodes().isNotEmpty() }
}

/**
 * A new portfolio through New portfolio's layer (the opening balance set first if needed, ScreenTestSupport), named
 * uniquely so a run on data left by an earlier run is not refused as a duplicate; returns its name.
 */
private fun createPortfolio(
    composeRule: MainRule,
    prefix: String,
): String {
    composeRule.openNewPortfolioLayer()
    val name = "$prefix ${System.currentTimeMillis() % 100_000}"
    composeRule.onNode(nameField()).performTextInput(name)
    composeRule.onNode(button(R.string.create_button)).performClick()
    if (!poll(15_000) { composeRule.onAllNodes(nameField()).fetchSemanticsNodes().isEmpty() }) {
        fail("New portfolio's layer was still open 15 s after Create; window focused: ${composeRule.windowFocused()}")
    }
    return name
}
