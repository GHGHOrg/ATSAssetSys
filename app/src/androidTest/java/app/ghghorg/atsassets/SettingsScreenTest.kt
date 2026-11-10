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
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.ghghorg.atsassets.core.AutoLock
import app.ghghorg.atsassets.ui.AppState
import app.ghghorg.atsassets.ui.BottomBar
import app.ghghorg.atsassets.ui.DEFAULT_TAB_ORDER
import app.ghghorg.atsassets.ui.More
import app.ghghorg.atsassets.ui.MoreScreen
import app.ghghorg.atsassets.ui.SaveRefusal
import app.ghghorg.atsassets.ui.Screen
import app.ghghorg.atsassets.ui.SettingsActions
import app.ghghorg.atsassets.ui.SettingsScreen
import app.ghghorg.atsassets.ui.Tab
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

// The four choices' words in the order of section 9, the first with "(default)" as the layer shows it (marker 7).
private fun choiceLabels(): List<Pair<AutoLock, String>> = listOf(
    AutoLock.EVERY_LEAVE to text(R.string.auto_lock_default, text(R.string.auto_lock_every_leave)),
    AutoLock.IDLE_1_MIN to text(R.string.auto_lock_idle_1_min),
    AutoLock.IDLE_5_MIN to text(R.string.auto_lock_idle_5_min),
    AutoLock.PHONE_LOCK_OR_RESTART to text(R.string.auto_lock_phone_lock),
)

private val radioOption = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton) and hasClickAction()

private fun settingsState(
    autoLock: AutoLock = AutoLock.EVERY_LEAVE,
    choiceOpen: Boolean = false,
    refusal: SaveRefusal? = null,
): AppState = AppState(screen = Screen.SETTINGS, tab = Tab.MORE, autoLock = autoLock, autoLockChoiceOpen = choiceOpen, autoLockRefusal = refusal)

/**
 * CLAUDE.md's pitfall on layers and menus (07; 20, AC-61 (b)): on the real MainActivity, unlocked, More's gear opens
 * Settings, and the Auto-lock choice layer keeps the window's focus and takes a tap. The test taps the choice already
 * made, "every time you leave", so it saves nothing that later tests would find.
 */
@RunWith(AndroidJUnit4::class)
class AutoLockChoiceFocusTest {
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(DeviceUnlockedRule()).around(composeRule)

    @Test
    fun theChoiceLayer_keepsTheFocus_andTakesATap() {
        composeRule.unlockApp()
        composeRule.onNode(isSelectable() and hasText(string(R.string.tab_more))).performClick()
        composeRule.onNode(hasContentDescription(string(R.string.settings_title)) and hasClickAction()).performClick()
        val row = hasContentDescription(text(R.string.settings_auto_lock_spoken, ""), substring = true) and hasClickAction()
        composeRule.waitUntil(5_000) { composeRule.onAllNodes(row).fetchSemanticsNodes().isNotEmpty() }
        composeRule.assertFocusHeld("the Auto-lock choice layer") {
            composeRule.onNode(row).performScrollTo().performClick()
            composeRule.waitUntil(5_000) { composeRule.onAllNodes(radioOption).fetchSemanticsNodes().isNotEmpty() }
        }
        composeRule.onNode(radioOption and hasText(choiceLabels().first().second)).performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodes(radioOption).fetchSemanticsNodes().isEmpty() }
        composeRule.onNode(row).assertExists()
    }
}

/**
 * Risk 5 (as defect 18-6, AC-61 (a)): Settings' choice layer at the largest font in landscape, drawn above the bottom bar
 * as in the app: each of the four choices can be scrolled to, is shown and takes a tap, and so can the note under them.
 */
@RunWith(AndroidJUnit4::class)
class AutoLockChoiceLargestFontLandscapeTest {
    private val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val rules: RuleChain =
        RuleChain
            .outerRule(DeviceUnlockedRule())
            .around(DeviceSettingsRule(fontScale = 2.0f))
            .around(composeRule)

    @Test
    fun everyChoiceAndTheNote_canBeReached_andEachChoiceTapped() {
        composeRule.turn(landscape = true)
        var chosen: AutoLock? = null
        composeRule.setContent {
            ATSAssetSystemTheme {
                Surface {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f)) {
                            SettingsScreen(settingsState(choiceOpen = true), SettingsActions({}, {}, {}, onChoose = { chosen = it }))
                        }
                        BottomBar(DEFAULT_TAB_ORDER, Tab.MORE, {})
                    }
                }
            }
        }
        choiceLabels().forEach { (choice, label) ->
            composeRule.onNode(radioOption and hasText(label)).performScrollTo().assertIsDisplayed().performClick()
            composeRule.waitForIdle()
            assertEquals("The tap on $label reached it", choice, chosen)
        }
        composeRule.onNodeWithText(text(R.string.auto_lock_note)).performScrollTo().assertIsDisplayed()
        // Upright again before the next test (18, #150).
        composeRule.turn(landscape = false)
    }
}

/**
 * The gear and Settings for TalkBack (20, AC-61 (d), AC-58): the gear reads "Settings", is a button and at least 48 dp;
 * the Auto-lock row reads its current choice and is a button; the layer's four choices with the default marked, the
 * chosen one selected, and the note; a failed save says so.
 */
@RunWith(AndroidJUnit4::class)
class SettingsTalkBackTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun show(state: AppState) {
        composeRule.setContent { ATSAssetSystemTheme { SettingsScreen(state, SettingsActions({}, {}, {}, {})) } }
    }

    @Test
    fun theGear_readsSettings_isAButton_andAtLeast48dp() {
        composeRule.setContent { ATSAssetSystemTheme { MoreScreen(More.of(), onOpenNetWorth = {}) } }
        composeRule
            .onNode(hasContentDescription(string(R.string.settings_title)) and hasClickAction())
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun theAutoLockRow_readsTheDefault() {
        show(settingsState())
        composeRule.onNode(hasText(string(R.string.settings_security))).assertExists()
        composeRule.onNode(hasContentDescription(text(R.string.settings_auto_lock_spoken, text(R.string.auto_lock_every_leave))) and hasClickAction()).assertExists()
    }

    @Test
    fun theAutoLockRow_readsAnotherChoice() {
        show(settingsState(AutoLock.PHONE_LOCK_OR_RESTART))
        composeRule.onNode(hasContentDescription(text(R.string.settings_auto_lock_spoken, text(R.string.auto_lock_phone_lock))) and hasClickAction()).assertExists()
    }

    @Test
    fun theLayer_showsTheFourChoices_theDefaultMarked_theChosenSelected_andTheNote() {
        show(settingsState(AutoLock.IDLE_5_MIN, choiceOpen = true))
        val labels = choiceLabels()
        labels.forEach { (_, label) -> composeRule.onNode(radioOption and hasText(label)).assertExists() }
        assertEquals("Four choices", 4, composeRule.onAllNodes(radioOption).fetchSemanticsNodes().size)
        composeRule.onNode(radioOption and hasText(text(R.string.auto_lock_idle_5_min))).assertIsSelected()
        composeRule.onNodeWithText(text(R.string.auto_lock_note)).assertExists()
    }

    @Test
    fun aFailedSave_saysSo() {
        show(settingsState(refusal = SaveRefusal.SAVE_FAILED))
        composeRule.onNodeWithText(text(R.string.save_failed)).assertExists()
    }
}
