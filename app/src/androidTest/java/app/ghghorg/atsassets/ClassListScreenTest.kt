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
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.ghghorg.atsassets.core.AssetType
import app.ghghorg.atsassets.core.AutoLock
import app.ghghorg.atsassets.core.ClassList
import app.ghghorg.atsassets.core.ClassLists
import app.ghghorg.atsassets.core.Instrument
import app.ghghorg.atsassets.core.ListRefusal
import app.ghghorg.atsassets.core.ListResult
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.RememberedText
import app.ghghorg.atsassets.core.SourceProfile
import app.ghghorg.atsassets.core.Tab
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import app.ghghorg.atsassets.ui.AppState
import app.ghghorg.atsassets.ui.BottomBar
import app.ghghorg.atsassets.ui.ClassListActions
import app.ghghorg.atsassets.ui.ClassListScreen
import app.ghghorg.atsassets.ui.ClassValueEdit
import app.ghghorg.atsassets.ui.DEFAULT_TAB_ORDER
import app.ghghorg.atsassets.ui.Screen
import app.ghghorg.atsassets.ui.SettingsActions
import app.ghghorg.atsassets.ui.SettingsScreen
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

private val noActions = ClassListActions({}, {}, {}, {}, {}, {}, {})

// AC-58's sample: Technology also matches "Information Technology"; AAPL, held in Portfolio P, uses it.
private val sampleLists =
    ClassLists(
        sectors = listOf("Technology", "Diversified"),
        geographies = listOf("United States"),
        remembered = listOf(RememberedText(ClassList.SECTOR, "Information Technology", "Technology")),
    )

private val aaplBuy =
    Transaction(
        id = "b1",
        portfolioId = "p1",
        ticker = "AAPL",
        type = TransactionType.BUY,
        date = LocalDate.of(2026, 1, 2),
        enteredSeq = 2L,
        quantity = BigDecimal("10"),
        totalCents = 150_000L,
    )

private val aaplProfile = SourceProfile("Information Technology", "United States")

private fun listState(
    lists: ClassLists = sampleLists,
    edit: ClassValueEdit? = null,
    outcome: ListResult? = null,
    screen: Screen = Screen.CLASS_LIST,
): AppState = AppState(
    screen = screen,
    tab = Tab.MORE,
    autoLock = AutoLock.EVERY_LEAVE,
    portfolios = listOf(Portfolio("p1", "Portfolio P", 1L)),
    transactions = listOf(aaplBuy),
    instruments = listOf(Instrument("AAPL", AssetType.STOCK, verified = true, profile = aaplProfile)),
    classLists = lists,
    classListShown = if (screen == Screen.CLASS_LIST) ClassList.SECTOR else null,
    classValueEdit = edit,
    listOutcome = outcome,
)

private fun nameInput(): SemanticsMatcher = hasSetTextAction() and hasText(string(R.string.list_name_label))

/**
 * Settings' Lists and a list screen for TalkBack (25, AC-61 (d), AC-58): each list row reads its number of values; each
 * value reads its holdings and the texts that map to it, and its Rename and Delete name it; the line under the list and
 * the Name layer's line are read; an empty list reads its line.
 */
@RunWith(AndroidJUnit4::class)
class ClassListTalkBackTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun show(state: AppState) {
        composeRule.setContent { ATSAssetSystemTheme { ClassListScreen(state, noActions) } }
    }

    @Test
    fun settingsListRows_readEachListsNumberOfValues() {
        val state = listState(screen = Screen.SETTINGS)
        composeRule.setContent { ATSAssetSystemTheme { SettingsScreen(state, SettingsActions({}, {}, {}, {})) } }
        composeRule.onNode(hasText(string(R.string.settings_lists))).performScrollTo().assertExists()
        val two = plural(R.plurals.settings_list_values, 2, 2)
        val one = plural(R.plurals.settings_list_values, 1, 1)
        val sectors = text(R.string.settings_list_spoken, string(R.string.settings_sectors), two)
        val geographies = text(R.string.settings_list_spoken, string(R.string.settings_geographies), one)
        composeRule.onNode(hasContentDescription(sectors) and hasClickAction()).assertExists()
        composeRule.onNode(hasContentDescription(geographies) and hasClickAction()).assertExists()
    }

    @Test
    fun eachValue_readsItsHoldingsAndTexts_andItsButtonsNameIt() {
        show(listState())
        composeRule.onNode(isHeading() and hasText(string(R.string.settings_sectors))).assertExists()
        val technology =
            text(R.string.list_row_also_spoken, "Technology", plural(R.plurals.list_used_by, 1, 1), "Information Technology")
        composeRule.onNode(hasContentDescription(technology)).assertExists()
        val diversified = text(R.string.list_row_spoken, "Diversified", plural(R.plurals.list_used_by, 0, 0))
        composeRule.onNode(hasContentDescription(diversified)).assertExists()
        for (value in sampleLists.sectors) {
            composeRule.onNode(hasContentDescription(text(R.string.list_rename_spoken, value)) and hasClickAction()).assertExists()
            composeRule.onNode(hasContentDescription(text(R.string.list_delete_spoken, value)) and hasClickAction()).assertExists()
        }
        composeRule.onNode(hasText(string(R.string.list_add_value)) and hasClickAction()).assertExists()
    }

    @Test
    fun theLineUnderTheList_andTheNameLayersLine_areRead() {
        show(listState(outcome = ListResult.Refused(ListRefusal.InUse("Technology", 1))))
        composeRule.onNodeWithText(plural(R.plurals.list_in_use, 1, "Technology", 1)).assertExists()
    }

    @Test
    fun theNameLayer_readsItsTitleFieldAndLine_andNothingUnderIt() {
        val edit = ClassValueEdit(renaming = null, text = "technology ")
        show(listState(edit = edit, outcome = ListResult.Refused(ListRefusal.Duplicate("Technology"))))
        composeRule.onNode(isHeading() and hasText(string(R.string.list_add_value))).assertExists()
        composeRule.onNode(nameInput()).assertExists()
        composeRule.onNodeWithText(text(R.string.list_name_duplicate, "Technology")).assertExists()
        // While the layer is open, TalkBack reads only the layer: the rows under it are not in the tree.
        composeRule.onNode(hasContentDescription(text(R.string.list_delete_spoken, "Technology"))).assertDoesNotExist()
    }

    @Test
    fun anEmptyList_readsItsLine() {
        show(listState(lists = ClassLists()))
        composeRule.onNodeWithText(string(R.string.list_no_sectors)).assertExists()
    }
}

// Twelve sectors with long texts, more than the screen holds at the largest font in landscape.
private val twelve =
    ClassLists(
        sectors =
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
        ),
        remembered =
        listOf(
            RememberedText(ClassList.SECTOR, "Information Technology", "Technology"),
            RememberedText(ClassList.SECTOR, "Health Care Equipment and Services", "Health Care"),
        ),
    )

/**
 * AC-39, AC-61 (a): a list screen and its Name layer at the largest font in landscape, drawn above the bottom bar as in
 * the app: every value's Rename and Delete and Add value can be scrolled to, are shown and take a tap; the layer's field,
 * Cancel and Save too.
 */
@RunWith(AndroidJUnit4::class)
class ClassListLargestFontLandscapeTest {
    private val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val rules: RuleChain =
        RuleChain
            .outerRule(DeviceUnlockedRule())
            .around(DeviceSettingsRule(fontScale = 2.0f))
            .around(composeRule)

    private fun show(
        state: AppState,
        actions: ClassListActions,
    ) {
        composeRule.setContent {
            ATSAssetSystemTheme {
                Surface {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f)) { ClassListScreen(state, actions) }
                        BottomBar(DEFAULT_TAB_ORDER, Tab.MORE, {})
                    }
                }
            }
        }
    }

    @Test
    fun everyValuesButtons_andAddValue_canBeReached_andTapped() {
        composeRule.turn(landscape = true)
        val renamed = mutableListOf<String>()
        val deleted = mutableListOf<String>()
        var added = false
        val actions = ClassListActions({}, { added = true }, { renamed += it }, { deleted += it }, {}, {}, {})
        show(listState(lists = twelve), actions)
        twelve.sectors.forEach { value ->
            composeRule.onNode(hasContentDescription(text(R.string.list_rename_spoken, value)) and hasClickAction())
                .performScrollTo().assertIsDisplayed().performClick()
            composeRule.onNode(hasContentDescription(text(R.string.list_delete_spoken, value)) and hasClickAction())
                .performScrollTo().assertIsDisplayed().performClick()
        }
        composeRule.onNode(hasText(string(R.string.list_add_value)) and hasClickAction()).performScrollTo().assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals("Each Rename took its tap", twelve.sectors, renamed)
        assertEquals("Each Delete took its tap", twelve.sectors, deleted)
        assertTrue("Add value took its tap", added)
        composeRule.turn(landscape = false)
    }

    @Test
    fun theNameLayer_fieldCancelAndSave_canBeReached_andTapped() {
        composeRule.turn(landscape = true)
        var saved = false
        var cancelled = false
        val actions = ClassListActions({}, {}, {}, {}, {}, { saved = true }, { cancelled = true })
        val edit = ClassValueEdit(renaming = "Communication Services", text = "Communication Services")
        val refused = ListResult.Refused(ListRefusal.Remembered("Information Technology", "Technology"))
        show(listState(lists = twelve, edit = edit, outcome = refused), actions)
        composeRule.onNode(nameInput()).performScrollTo().assertIsDisplayed()
        composeRule.onNode(hasText(text(R.string.save_button)) and hasClickAction()).performScrollTo().assertIsDisplayed().performClick()
        composeRule.onNode(hasText(text(R.string.cancel_button)) and hasClickAction()).performScrollTo().assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertTrue("Save and Cancel took their taps", saved && cancelled)
        composeRule.turn(landscape = false)
    }
}

/**
 * AC-61 (b) and (c) on the real MainActivity, unlocked, at the largest font: Settings, Sectors, then Add value opens the
 * Name layer without MainActivity's window losing the focus (07); with the keyboard open, the field, Cancel and Save lie
 * above it (08). Each ends with Cancel, so nothing is stored.
 */
@RunWith(AndroidJUnit4::class)
class ClassListLayerTest {
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain =
        RuleChain
            .outerRule(DeviceUnlockedRule())
            .around(DeviceSettingsRule(fontScale = 2.0f))
            .around(composeRule)

    private val addValue = hasText(string(R.string.list_add_value)) and hasClickAction()

    private fun openSectors() {
        composeRule.unlockApp()
        composeRule.onNode(isSelectable() and hasText(string(R.string.tab_more))).performClick()
        composeRule.onNode(hasContentDescription(string(R.string.settings_title)) and hasClickAction()).performClick()
        val spoken = text(R.string.settings_list_spoken, string(R.string.settings_sectors), "")
        val sectors = hasContentDescription(spoken, substring = true) and hasClickAction()
        composeRule.waitUntil(5_000) { composeRule.onAllNodes(sectors).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNode(sectors).performScrollTo().performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodes(addValue).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun cancel() {
        composeRule.onNode(hasText(text(R.string.cancel_button)) and hasClickAction()).performScrollTo().performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodes(nameInput()).fetchSemanticsNodes().isEmpty() }
    }

    @Test
    fun theNameLayer_keepsTheWindowsFocus() {
        openSectors()
        composeRule.assertFocusHeld("the Name layer") {
            composeRule.onNode(addValue).performScrollTo().performClick()
            composeRule.waitUntil(5_000) { composeRule.onAllNodes(nameInput()).fetchSemanticsNodes().isNotEmpty() }
        }
        cancel()
    }

    @Test
    fun theNameLayer_staysAboveTheKeyboard() {
        openSectors()
        composeRule.onNode(addValue).performScrollTo().performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodes(nameInput()).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNode(nameInput()).performClick()
        val keyboardTop = composeRule.keyboardTop()
        composeRule.waitForIdle()
        val parts =
            listOf(
                "the Name field" to nameInput(),
                "Cancel" to (hasText(text(R.string.cancel_button)) and hasClickAction()),
                "Save" to (hasText(text(R.string.save_button)) and hasClickAction()),
            )
        parts.forEach { (label, matcher) ->
            val bottom = composeRule.onNode(matcher).fetchSemanticsNode().boundsInWindow.bottom
            assertTrue("$label ends at $bottom px, below the keyboard's top at $keyboardTop px (08)", bottom <= keyboardTop)
        }
        cancel()
    }
}
