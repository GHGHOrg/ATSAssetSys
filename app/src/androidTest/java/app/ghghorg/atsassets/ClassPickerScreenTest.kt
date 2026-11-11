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
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.ghghorg.atsassets.core.ClassList
import app.ghghorg.atsassets.core.ClassLists
import app.ghghorg.atsassets.core.ClassOverride
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.Tab
import app.ghghorg.atsassets.ui.AppState
import app.ghghorg.atsassets.ui.BottomBar
import app.ghghorg.atsassets.ui.ClassPickerActions
import app.ghghorg.atsassets.ui.ClassPickerScreen
import app.ghghorg.atsassets.ui.DEFAULT_TAB_ORDER
import app.ghghorg.atsassets.ui.SaveRefusal
import app.ghghorg.atsassets.ui.Screen
import app.ghghorg.atsassets.ui.theme.ATSAssetSystemTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

private fun text(
    id: Int,
    vararg args: Any,
): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

private val pickerRow = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton) and hasClickAction()

// Twelve sectors, more than the screen holds at the largest font in landscape.
private val twelveSectors =
    listOf(
        "Technology",
        "Health Care",
        "Financials",
        "Energy",
        "Utilities",
        "Materials",
        "Industrials",
        "Real Estate",
        "Consumer Staples",
        "Consumer Discretionary",
        "Communication Services",
        "Diversified",
    )

// The picker for AAPL in Portfolio P, opened on [list], with [choice] picked.
private fun pickerState(
    list: ClassList = ClassList.SECTOR,
    sectors: List<String> = listOf("Technology", "Health Care", "Diversified"),
    choice: String? = "Health Care",
    refusal: SaveRefusal? = null,
    overrides: List<ClassOverride> = emptyList(),
): AppState = AppState(
    screen = Screen.CLASS_PICKER,
    tab = Tab.PORTFOLIOS,
    portfolios = listOf(Portfolio("p1", "Portfolio P", 1L)),
    portfolioId = "p1",
    holdingTicker = "AAPL",
    classLists = ClassLists(sectors = sectors),
    classPicker = list,
    classPickerChoice = choice,
    overrideRefusal = refusal,
    overrides = overrides,
)

/**
 * The Edit picker for TalkBack (24, frame H; AC-61 (d), AC-22): the title is a heading naming the list and the holding;
 * each value is a radio row in the list's order, the pick selected, and a tap names its value; the note; Save is on with
 * a pick, and off while the value the row shows is still picked (step 7 fix). An empty list reads its line and has no
 * Save (marker 17, decision 1). A failed save says so.
 */
@RunWith(AndroidJUnit4::class)
class ClassPickerTalkBackTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun show(
        state: AppState,
        actions: ClassPickerActions = ClassPickerActions({}, {}, {}),
    ) {
        composeRule.setContent { ATSAssetSystemTheme { ClassPickerScreen(state, actions) } }
    }

    private fun row(value: String): SemanticsNodeInteraction = composeRule.onNode(pickerRow and hasText(value))

    private fun saveButton(): SemanticsMatcher = hasText(text(R.string.save_button)) and hasClickAction()

    @Test
    fun eachValue_isARadioRow_inTheListsOrder_thePickSelected() {
        val chosen = mutableListOf<String>()
        show(pickerState(), ClassPickerActions({}, { chosen += it }, {}))
        composeRule.onNode(isHeading() and hasText(text(R.string.class_picker_title, text(R.string.class_sector), "AAPL"))).assertExists()
        assertEquals("Three rows", 3, composeRule.onAllNodes(pickerRow).fetchSemanticsNodes().size)
        val tops = listOf("Technology", "Health Care", "Diversified").map { row(it).fetchSemanticsNode().boundsInRoot.top }
        assertTrue("Technology, Health Care, Diversified from top to bottom: $tops", tops[0] < tops[1] && tops[1] < tops[2])
        row("Health Care").assertIsSelected()
        row("Technology").assertIsNotSelected()
        row("Diversified").performClick()
        composeRule.waitForIdle()
        assertEquals("The tap names its value", listOf("Diversified"), chosen)
        composeRule.onNodeWithText(text(R.string.class_picker_note)).assertExists()
        composeRule.onNode(saveButton()).assertIsEnabled()
    }

    // Step 7 fix (your A): the value the row shows still picked, Save is off; another pick turns it on (the model's rule).
    @Test
    fun theValueShownStillPicked_saveIsOff() {
        show(pickerState(overrides = listOf(ClassOverride("p1", "AAPL", ClassList.SECTOR, "Health Care"))))
        row("Health Care").assertIsSelected()
        composeRule.onNode(saveButton()).assertIsNotEnabled()
    }

    @Test
    fun anEmptyList_readsItsLine_andHasNoSave() {
        show(pickerState(list = ClassList.GEOGRAPHY, choice = null))
        composeRule.onNode(isHeading() and hasText(text(R.string.class_picker_title, text(R.string.class_geography), "AAPL"))).assertExists()
        composeRule.onNodeWithText(text(R.string.class_picker_no_geographies)).assertExists()
        composeRule.onNode(pickerRow).assertDoesNotExist()
        composeRule.onNode(saveButton()).assertDoesNotExist()
        composeRule.onNode(hasText(text(R.string.cancel_button)) and hasClickAction()).assertIsEnabled()
    }

    @Test
    fun aFailedSave_saysSo() {
        show(pickerState(refusal = SaveRefusal.SAVE_FAILED))
        composeRule.onNodeWithText(text(R.string.save_failed)).assertExists()
        row("Health Care").assertIsSelected()
    }
}

/**
 * Risk 3 (AC-39, AC-61 (a)): the Edit picker at the largest font in landscape, drawn above the bottom bar as in the app,
 * with twelve sectors: every row can be scrolled to, is shown and takes a tap, and so can the note, Save and Cancel.
 */
@RunWith(AndroidJUnit4::class)
class ClassPickerLargestFontLandscapeTest {
    private val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val rules: RuleChain =
        RuleChain
            .outerRule(DeviceUnlockedRule())
            .around(DeviceSettingsRule(fontScale = 2.0f))
            .around(composeRule)

    @Test
    fun everyRow_theNote_saveAndCancel_canBeReached_andTapped() {
        composeRule.turn(landscape = true)
        var chosen: String? = null
        var saved = false
        var cancelled = false
        val actions = ClassPickerActions(onCancel = { cancelled = true }, onChoose = { chosen = it }, onSave = { saved = true })
        composeRule.setContent {
            ATSAssetSystemTheme {
                Surface {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f)) {
                            ClassPickerScreen(pickerState(sectors = twelveSectors, choice = "Technology"), actions)
                        }
                        BottomBar(DEFAULT_TAB_ORDER, Tab.PORTFOLIOS, {})
                    }
                }
            }
        }
        twelveSectors.forEach { value ->
            composeRule.onNode(pickerRow and hasText(value)).performScrollTo().assertIsDisplayed().performClick()
            composeRule.waitForIdle()
            assertEquals("The tap on $value reached it", value, chosen)
        }
        composeRule.onNodeWithText(text(R.string.class_picker_note)).performScrollTo().assertIsDisplayed()
        composeRule.onNode(hasText(text(R.string.save_button)) and hasClickAction()).performScrollTo().assertIsDisplayed().performClick()
        composeRule.onNode(hasText(text(R.string.cancel_button)) and hasClickAction()).performScrollTo().assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertTrue("Save and Cancel took their taps", saved && cancelled)
        // Upright again before the next test (18, #150).
        composeRule.turn(landscape = false)
    }
}
