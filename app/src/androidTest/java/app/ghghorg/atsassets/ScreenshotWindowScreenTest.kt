package app.ghghorg.atsassets

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
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
import app.ghghorg.atsassets.core.Tab
import app.ghghorg.atsassets.ui.AppState
import app.ghghorg.atsassets.ui.BottomBar
import app.ghghorg.atsassets.ui.DEFAULT_TAB_ORDER
import app.ghghorg.atsassets.ui.Screen
import app.ghghorg.atsassets.ui.SettingsActions
import app.ghghorg.atsassets.ui.SettingsScreen
import app.ghghorg.atsassets.ui.theme.ATSAssetSystemTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

private fun text(
    id: Int,
    vararg args: Any,
): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

private fun plural(
    id: Int,
    count: Int,
): String = InstrumentationRegistry.getInstrumentation().targetContext.resources.getQuantityString(id, count, count)

private fun layerState(leftMs: Long? = null): AppState = AppState(screen = Screen.SETTINGS, tab = Tab.MORE, screenshotWindowLeftMs = leftMs, screenshotLayerOpen = true)

private val screenshotRow = hasContentDescription(text(R.string.settings_screenshot_spoken, ""), substring = true) and hasClickAction()

/** From an unlocked MainActivity: More, the gear, then the Screenshot window's layer. */
private fun MainRule.openScreenshotLayer() {
    onNode(isSelectable() and hasText(string(R.string.tab_more))).performClick()
    onNode(hasContentDescription(string(R.string.settings_title)) and hasClickAction()).performClick()
    waitUntil(5_000) { onAllNodes(screenshotRow).fetchSemanticsNodes().isNotEmpty() }
    onNode(screenshotRow).performScrollTo().performClick()
    waitUntil(5_000) { onAllNodes(button(R.string.screenshot_allow)).fetchSemanticsNodes().isNotEmpty() }
}

private fun MainRule.flagSecure(): Boolean = runOnUiThread { activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0 }

/**
 * CLAUDE.md's pitfall on layers and menus (07; 21, AC-61 (b)): on the real MainActivity, unlocked, the Screenshot
 * window's layer keeps the window's focus, so the cover screen does not replace it.
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotLayerFocusTest {
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(DeviceUnlockedRule()).around(composeRule)

    @Test
    fun theLayer_keepsTheFocus() {
        composeRule.unlockApp()
        composeRule.onNode(isSelectable() and hasText(string(R.string.tab_more))).performClick()
        composeRule.onNode(hasContentDescription(string(R.string.settings_title)) and hasClickAction()).performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodes(screenshotRow).fetchSemanticsNodes().isNotEmpty() }
        composeRule.assertFocusHeld("the Screenshot window's layer") {
            composeRule.onNode(screenshotRow).performScrollTo().performClick()
            composeRule.waitUntil(5_000) { composeRule.onAllNodes(button(R.string.screenshot_allow)).fetchSemanticsNodes().isNotEmpty() }
        }
    }
}

/**
 * Spec section 9 and AC-20 (21, risk 3): blocked by default; after the check `FLAG_SECURE` is cleared, Turn off sets it
 * again, and leaving the app sets it again. The emulator reads the flag, not whether a screenshot was really taken (C19).
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotWindowFlagTest {
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(DeviceUnlockedRule()).around(composeRule)

    @Test
    fun theCheckClearsTheFlag_andTurnOffAndLeavingSetItAgain() {
        composeRule.unlockApp()
        assertEquals("Blocked by default", true, composeRule.flagSecure())
        composeRule.openScreenshotLayer()

        composeRule.onNode(button(R.string.screenshot_allow)).performScrollTo().performClick()
        passCheck()
        waitFor("FLAG_SECURE cleared after the check") { !composeRule.flagSecure() }
        composeRule.waitUntil(5_000) { composeRule.onAllNodes(button(R.string.screenshot_turn_off)).fetchSemanticsNodes().isNotEmpty() }

        composeRule.onNode(button(R.string.screenshot_turn_off)).performScrollTo().performClick()
        waitFor("FLAG_SECURE set again by Turn off") { composeRule.flagSecure() }
        composeRule.waitUntil(5_000) { composeRule.onAllNodes(button(R.string.screenshot_allow)).fetchSemanticsNodes().isNotEmpty() }

        composeRule.onNode(button(R.string.screenshot_allow)).performScrollTo().performClick()
        passCheck()
        waitFor("FLAG_SECURE cleared after the second check") { !composeRule.flagSecure() }
        shell("input keyevent KEYCODE_HOME")
        waitFor("FLAG_SECURE set again on leaving the app") { composeRule.flagSecure() }
    }
}

/**
 * Risk 5 (as defect 18-6, AC-61 (a)): the Screenshot window's layer at the largest font in landscape, above the bottom
 * bar as in the app: off, Allow can be scrolled to, is shown and takes a tap, and so can its note; on, the warning line
 * and Turn off.
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotLayerLargestFontLandscapeTest {
    private val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val rules: RuleChain =
        RuleChain
            .outerRule(DeviceUnlockedRule())
            .around(DeviceSettingsRule(fontScale = 2.0f))
            .around(composeRule)

    @Test
    fun offAndOn_everyLineAndButton_canBeReached_andTheButtonsTapped() {
        composeRule.turn(landscape = true)
        var leftMs by mutableStateOf<Long?>(null)
        var allowed = 0
        var turnedOff = 0
        composeRule.setContent {
            ATSAssetSystemTheme {
                Surface {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f)) {
                            SettingsScreen(
                                layerState(leftMs),
                                SettingsActions({}, {}, {}, {}, onAllowScreenshots = { allowed++ }, onTurnOffScreenshots = { turnedOff++ }),
                            )
                        }
                        BottomBar(DEFAULT_TAB_ORDER, Tab.MORE, {})
                    }
                }
            }
        }
        composeRule.onNode(button(R.string.screenshot_allow)).performScrollTo().assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals("The tap on Allow reached it", 1, allowed)
        composeRule.onNodeWithText(text(R.string.screenshot_allow_note)).performScrollTo().assertIsDisplayed()

        leftMs = 161_000L
        composeRule.waitForIdle()
        composeRule.onNodeWithText(text(R.string.screenshot_on_warning)).performScrollTo().assertIsDisplayed()
        composeRule.onNode(button(R.string.screenshot_turn_off)).performScrollTo().assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals("The tap on Turn off reached it", 1, turnedOff)
        // Upright again before the next test (18, #150).
        composeRule.turn(landscape = false)
    }
}

/**
 * The Screenshot window for TalkBack and on screen (21, AC-61 (d), AC-58): the row reads its state and is a button; off,
 * the layer's blocked line, Allow and the check line; on, the time left with "Ends when you leave the app.", the warning
 * and Turn off.
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotTalkBackTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun show(state: AppState) {
        composeRule.setContent { ATSAssetSystemTheme { SettingsScreen(state, SettingsActions({}, {}, {}, {})) } }
    }

    private fun shown(value: String): Boolean = composeRule.onAllNodes(hasText(value)).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun theRow_readsOff() {
        show(AppState(screen = Screen.SETTINGS, tab = Tab.MORE))
        val spoken = text(R.string.settings_screenshot_spoken, text(R.string.screenshot_off))
        composeRule.onNode(hasContentDescription(spoken) and hasClickAction()).assertExists()
    }

    @Test
    fun theRow_readsTheTimeLeftInWords() {
        show(AppState(screen = Screen.SETTINGS, tab = Tab.MORE, screenshotWindowLeftMs = 161_000L))
        val on = text(R.string.screenshot_on_spoken, plural(R.plurals.screenshot_minutes_spoken, 2), plural(R.plurals.screenshot_seconds_spoken, 41))
        composeRule.onNode(hasContentDescription(text(R.string.settings_screenshot_spoken, on)) and hasClickAction()).assertExists()
    }

    @Test
    fun theRow_readsOneMinuteAndOneSecond_inTheSingular() {
        show(AppState(screen = Screen.SETTINGS, tab = Tab.MORE, screenshotWindowLeftMs = 61_000L))
        val on = text(R.string.screenshot_on_spoken, plural(R.plurals.screenshot_minutes_spoken, 1), plural(R.plurals.screenshot_seconds_spoken, 1))
        composeRule.onNode(hasContentDescription(text(R.string.settings_screenshot_spoken, on))).assertExists()
    }

    @Test
    fun theLayer_off_showsTheBlockedLine_Allow_andTheCheckLine() {
        show(layerState())
        assertEquals("The title shown", true, shown(text(R.string.settings_screenshot)))
        assertEquals("The blocked line shown", true, shown(text(R.string.screenshot_off_line)))
        composeRule.onNode(button(R.string.screenshot_allow)).assertExists()
        assertEquals("The check line shown", true, shown(text(R.string.screenshot_allow_note)))
        assertEquals("No Turn off while off", false, shown(text(R.string.screenshot_turn_off)))
    }

    @Test
    fun theLayer_on_showsTheTimeLeft_theWarning_andTurnOff() {
        show(layerState(161_000L))
        assertEquals("The time left shown", true, shown(text(R.string.screenshot_on_line, "2:41")))
        assertEquals("The warning shown", true, shown(text(R.string.screenshot_on_warning)))
        composeRule.onNode(button(R.string.screenshot_turn_off)).assertExists()
        assertEquals("No Allow while on", false, shown(text(R.string.screenshot_allow)))
    }
}
