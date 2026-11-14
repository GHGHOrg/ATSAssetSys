package app.ghghorg.atsassets

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
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
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.ghghorg.atsassets.core.AutoLock
import app.ghghorg.atsassets.core.Tab
import app.ghghorg.atsassets.ui.AppState
import app.ghghorg.atsassets.ui.BottomBar
import app.ghghorg.atsassets.ui.DEFAULT_TAB_ORDER
import app.ghghorg.atsassets.ui.KeyEntry
import app.ghghorg.atsassets.ui.KeyEntryActions
import app.ghghorg.atsassets.ui.KeyEntryLayer
import app.ghghorg.atsassets.ui.KeyRefusal
import app.ghghorg.atsassets.ui.PriceRefreshLines
import app.ghghorg.atsassets.ui.Screen
import app.ghghorg.atsassets.ui.SettingsActions
import app.ghghorg.atsassets.ui.SettingsScreen
import app.ghghorg.atsassets.ui.theme.ATSAssetSystemTheme
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

private const val SOURCE = "Finnhub"

private fun keyText(
    id: Int,
    vararg args: Any,
): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

/** Settings with a key saved (its ending "a1b2") or none, and the key entry open when [entry] is given. */
private fun keyState(
    keySet: Boolean = true,
    entry: KeyEntry? = null,
): AppState = AppState(
    screen = Screen.SETTINGS,
    tab = Tab.MORE,
    autoLock = AutoLock.EVERY_LEAVE,
    priceSourceName = SOURCE,
    priceKeySet = keySet,
    priceKeyEnding = if (keySet) "a1b2" else "",
    keyEntry = entry,
)

private fun noKeyActions() = KeyEntryActions({}, {}, {}, {}, {}, {})

/** Settings and the bottom bar, with the key entry over both, as AppContent draws them (30). */
@Composable
private fun SettingsWithEntry(
    state: AppState,
    actions: KeyEntryActions = noKeyActions(),
) {
    Surface {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) { SettingsScreen(state, SettingsActions({}, {}, {}, {})) }
                BottomBar(DEFAULT_TAB_ORDER, Tab.MORE, {})
            }
            KeyEntryLayer(state.keyEntryModel, actions)
        }
    }
}

private fun keyField(): SemanticsMatcher = hasSetTextAction() and hasText(string(R.string.key_entry_field))

// The Price source row, whatever its key's state: TalkBack's text starts with the row's name.
private fun priceSourceRow(): SemanticsMatcher {
    val start = keyText(R.string.settings_price_source_spoken, "")
    return hasContentDescription(start, substring = true) and hasClickAction()
}

/**
 * The Price source row and the key entry for TalkBack (30, AC-61 (d)): the row reads the source and the saved key's last
 * characters one by one, or that there is no key; the entry's title is a heading, its line reads the same characters, its
 * field is a password field; a refusal is said in its words; the clear question takes the field's place; the refresh line
 * names a missing key.
 */
@RunWith(AndroidJUnit4::class)
class PriceKeyTalkBackTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theRow_readsTheSource_andTheKeysEnding_oneCharacterAtATime() {
        composeRule.setContent { ATSAssetSystemTheme { SettingsWithEntry(keyState()) } }
        composeRule.onNode(isHeading() and hasText(string(R.string.settings_prices))).assertExists()
        val value = keyText(R.string.price_source_key_ending, SOURCE, "a 1 b 2")
        composeRule.onNode(hasContentDescription(keyText(R.string.settings_price_source_spoken, value)) and hasClickAction()).assertExists()
    }

    @Test
    fun theRow_withoutAKey_saysSo() {
        composeRule.setContent { ATSAssetSystemTheme { SettingsWithEntry(keyState(keySet = false)) } }
        val value = keyText(R.string.price_source_no_key, SOURCE)
        composeRule.onNode(hasContentDescription(keyText(R.string.settings_price_source_spoken, value)) and hasClickAction()).assertExists()
    }

    @Test
    fun theEntry_readsItsTitle_theSavedKeysEnding_andHasAPasswordField() {
        composeRule.setContent { ATSAssetSystemTheme { SettingsWithEntry(keyState(entry = KeyEntry())) } }
        composeRule.onNode(isHeading() and hasText(keyText(R.string.key_entry_title, SOURCE))).assertExists()
        composeRule.onNode(hasContentDescription(keyText(R.string.key_entry_current, "a 1 b 2"))).assertExists()
        composeRule.onNodeWithText(keyText(R.string.key_entry_note, SOURCE)).assertExists()
        composeRule.onNode(keyField() and SemanticsMatcher.keyIsDefined(SemanticsProperties.Password)).assertExists()
        // Nothing typed: Save rests; a key is saved, so Clear key is offered.
        composeRule.onNode(button(R.string.save_button)).assertIsNotEnabled()
        composeRule.onNode(button(R.string.key_entry_clear)).assertIsEnabled()
    }

    @Test
    fun theEntry_withoutAKey_saysSo_andOffersNoClear() {
        composeRule.setContent { ATSAssetSystemTheme { SettingsWithEntry(keyState(keySet = false, entry = KeyEntry(text = "abc123"))) } }
        composeRule.onNodeWithText(string(R.string.key_entry_none)).assertExists()
        composeRule.onNode(button(R.string.key_entry_clear)).assertDoesNotExist()
        composeRule.onNode(button(R.string.save_button)).assertIsEnabled()
    }

    @Test
    fun aRefusedKey_saysWhy_withTheSourceAndTheStatus() {
        val refused = KeyEntry(text = "abc123", refusal = KeyRefusal.Refused(401))
        composeRule.setContent { ATSAssetSystemTheme { SettingsWithEntry(keyState(entry = refused)) } }
        composeRule.onNodeWithText(keyText(R.string.key_refused, SOURCE, 401)).assertExists()
    }

    @Test
    fun theClearQuestion_takesTheFieldsPlace() {
        composeRule.setContent { ATSAssetSystemTheme { SettingsWithEntry(keyState(entry = KeyEntry(confirmingClear = true))) } }
        composeRule.onNodeWithText(string(R.string.key_clear_question)).assertExists()
        composeRule.onNode(keyField()).assertDoesNotExist()
        composeRule.onNode(button(R.string.key_entry_clear)).assertIsEnabled()
        composeRule.onNode(button(R.string.cancel_button)).assertIsEnabled()
    }

    @Test
    fun theRefreshLine_withoutAKey_namesTheKey_andWithOneSaysItFailed() {
        composeRule.setContent {
            ATSAssetSystemTheme {
                Surface {
                    Column {
                        PriceRefreshLines(refreshing = false, failed = true, noKey = true)
                    }
                }
            }
        }
        composeRule.onNodeWithText(string(R.string.price_refresh_no_key)).assertExists()
        composeRule.onNodeWithText(string(R.string.price_refresh_failed)).assertDoesNotExist()
    }
}

/**
 * AC-39, AC-61 (a): the key entry at the largest font in landscape, over the bottom bar as in the app: its title, the
 * saved key's line, the note, the field, Show key, Clear key, Cancel and Save reached and shown, and the buttons tapped.
 */
@RunWith(AndroidJUnit4::class)
class PriceKeyLargestFontLandscapeTest {
    private val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val rules: RuleChain =
        RuleChain
            .outerRule(DeviceUnlockedRule())
            .around(DeviceSettingsRule(fontScale = 2.0f))
            .around(composeRule)

    @Test
    fun theEntry_canBeReached_andItsButtonsTapped() {
        composeRule.turn(landscape = true)
        var saved = false
        var cancelled = false
        var asked = false
        val actions =
            KeyEntryActions(
                onTextChange = {},
                onSave = { saved = true },
                onCancel = { cancelled = true },
                onAskClear = { asked = true },
                onCancelClear = {},
                onConfirmClear = {},
            )
        composeRule.setContent { ATSAssetSystemTheme { SettingsWithEntry(keyState(entry = KeyEntry(text = "abc123")), actions) } }
        composeRule.onNode(isHeading() and hasText(keyText(R.string.key_entry_title, SOURCE))).performScrollTo().assertIsDisplayed()
        composeRule.onNode(hasContentDescription(keyText(R.string.key_entry_current, "a 1 b 2"))).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(keyText(R.string.key_entry_note, SOURCE)).performScrollTo().assertIsDisplayed()
        composeRule.onNode(keyField()).performScrollTo().assertIsDisplayed()
        composeRule.onNode(button(R.string.key_entry_show)).performScrollTo().assertIsDisplayed()
        composeRule.onNode(button(R.string.key_entry_clear)).performScrollTo().assertIsDisplayed().performClick()
        composeRule.onNode(button(R.string.cancel_button)).performScrollTo().assertIsDisplayed().performClick()
        composeRule.onNode(button(R.string.save_button)).performScrollTo().assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertTrue("Clear key took its tap", asked)
        assertTrue("Cancel took its tap", cancelled)
        assertTrue("Save took its tap", saved)
        // Upright again before the next test (18, #150).
        composeRule.turn(landscape = false)
    }
}

/** On the real MainActivity, unlocked: More, its gear, and Settings' Price source row shown. */
private fun MainRule.openSettingsWithPriceSource() {
    onNode(isSelectable() and hasText(string(R.string.tab_more))).performClick()
    onNode(hasContentDescription(string(R.string.settings_title)) and hasClickAction()).performClick()
    waitUntil(5_000) { onAllNodes(priceSourceRow()).fetchSemanticsNodes().isNotEmpty() }
}

/**
 * CLAUDE.md's pitfall on layers (07; 30, AC-61 (b)): on the real MainActivity, unlocked, Settings' Price source row
 * opens the key entry and the window keeps its focus. A text that is no key is typed and Save says so, which sends
 * nothing and stores nothing (the emulator has no key and must not call the source); Cancel closes the entry.
 */
@RunWith(AndroidJUnit4::class)
class PriceKeyFocusTest {
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(DeviceUnlockedRule()).around(composeRule)

    @Test
    fun theEntry_keepsTheFocus_refusesATextThatIsNoKey_andCloses() {
        composeRule.unlockApp()
        composeRule.openSettingsWithPriceSource()
        composeRule.assertFocusHeld("the key entry") {
            composeRule.onNode(priceSourceRow()).performScrollTo().performClick()
            composeRule.waitUntil(5_000) { composeRule.onAllNodes(keyField()).fetchSemanticsNodes().isNotEmpty() }
        }
        composeRule.onNode(keyField()).performTextInput("abc 123")
        composeRule.onNode(button(R.string.save_button)).performScrollTo().performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithKeyMessage().isNotEmpty() }
        composeRule.onNode(button(R.string.cancel_button)).performScrollTo().performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodes(keyField()).fetchSemanticsNodes().isEmpty() }
        composeRule.onNode(priceSourceRow()).assertExists()
    }
}

private fun MainRule.onAllNodesWithKeyMessage() = onAllNodes(hasText(string(R.string.key_not_a_key))).fetchSemanticsNodes()

/**
 * AC-61 (c): at the largest font, with the keyboard open on the key entry's field, the field lies fully above the
 * keyboard (the layer's imePadding).
 */
@RunWith(AndroidJUnit4::class)
class PriceKeyKeyboardTest {
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain =
        RuleChain
            .outerRule(DeviceUnlockedRule())
            .around(DeviceSettingsRule(fontScale = 2.0f))
            .around(composeRule)

    @Test
    fun theKeyField_staysAboveTheKeyboard() {
        composeRule.unlockApp()
        composeRule.openSettingsWithPriceSource()
        composeRule.onNode(priceSourceRow()).performScrollTo().performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodes(keyField()).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNode(keyField()).performScrollTo().performClick()
        val keyboardTop = composeRule.keyboardTop()
        composeRule.waitForIdle()
        val bottom = composeRule.onNode(keyField()).fetchSemanticsNode().boundsInWindow.bottom
        assertTrue("The key field ends at $bottom px, below the keyboard's top at $keyboardTop px", bottom <= keyboardTop)
    }
}

/**
 * 29's pitfalls for a layer with a field (CLAUDE.md; 30, risk 8): at the largest font in landscape, on the real
 * MainActivity, the key entry must reach down to the keyboard and show its field. Settings is turned first: the turn
 * relaunches MainActivity, which locks under the emulator's auto-lock choice, and an unlock drops an open key entry (30
 * step 4), so the entry is opened after the lock prompt is passed. At this font the entry is taller than its room, so it
 * fills the room, and its bottom lies its own margin (24 dp) above the keyboard's top, give or take 4 dp; the field lies
 * fully above the keyboard.
 */
@RunWith(AndroidJUnit4::class)
class PriceKeyKeyboardLandscapeTest {
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain =
        RuleChain
            .outerRule(DeviceUnlockedRule())
            .around(DeviceSettingsRule(fontScale = 2.0f))
            .around(composeRule)

    @Test
    fun theEntry_reachesDownToTheKeyboard_andShowsItsField_inLandscape() {
        composeRule.unlockApp()
        composeRule.openSettingsWithPriceSource()
        composeRule.turn(landscape = true)
        passLockAfterTurn()
        if (!poll(10_000) { composeRule.windowFocused() && composeRule.onAllNodes(priceSourceRow()).fetchSemanticsNodes().isNotEmpty() }) {
            fail("No Price source row 10 s after the turn; window focused: ${composeRule.windowFocused()}")
        }
        composeRule.onNode(priceSourceRow()).performScrollTo().performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodes(keyField()).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNode(keyField()).performScrollTo().performClick()
        val keyboardTop = composeRule.keyboardTop()
        composeRule.waitForIdle()
        val pane = SemanticsMatcher.keyIsDefined(SemanticsProperties.PaneTitle)
        val paneBottom = composeRule.onNode(pane).fetchSemanticsNode().boundsInWindow.bottom
        val (margin, tolerance) = with(composeRule.density) { 24.dp.toPx() to 4.dp.toPx() }
        val gap = keyboardTop - paneBottom
        assertTrue(
            "The key entry ends at $paneBottom px, $gap px above the keyboard's top at $keyboardTop px; expected its margin, $margin px",
            gap >= margin - tolerance && gap <= margin + tolerance,
        )
        composeRule.onNode(keyField()).assertIsDisplayed()
        val bounds = composeRule.onNode(keyField()).fetchSemanticsNode().boundsInWindow
        assertTrue("The key field ends at ${bounds.bottom} px, below the keyboard's top at $keyboardTop px", bounds.bottom <= keyboardTop)
    }
}
