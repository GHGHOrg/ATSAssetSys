package app.ghghorg.atsassets

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.ghghorg.atsassets.core.AutoLock
import app.ghghorg.atsassets.core.ClassList
import app.ghghorg.atsassets.core.ClassLists
import app.ghghorg.atsassets.core.Tab
import app.ghghorg.atsassets.ui.AppState
import app.ghghorg.atsassets.ui.ClassListActions
import app.ghghorg.atsassets.ui.ClassListScreen
import app.ghghorg.atsassets.ui.More
import app.ghghorg.atsassets.ui.MoreScreen
import app.ghghorg.atsassets.ui.Screen
import app.ghghorg.atsassets.ui.SettingsActions
import app.ghghorg.atsassets.ui.SettingsScreen
import app.ghghorg.atsassets.ui.TabOrderActions
import app.ghghorg.atsassets.ui.TabOrderScreen
import app.ghghorg.atsassets.ui.theme.ATSAssetSystemTheme
import app.ghghorg.atsassets.ui.theme.Asap
import app.ghghorg.atsassets.ui.theme.TokenColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * More, Settings, Tab order and a list's look (44a step 8, group (e) part 3; spec 6, Visual design; AC-61 (e); drawing
 * 10f-more-settings, D-34 to D-36): More's band with Lucide's settings icon and five rows with chevrons; Settings' section
 * headings in medium 14 over cards of rows with chevrons; Tab order's arrow-up and arrow-down icon buttons; a list's Add
 * value gold.
 */
@RunWith(AndroidJUnit4::class)
class MoreSettingsDesignTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun icons(): List<String> = composeRule.drawnIcons().map { it.first }

    @Test
    fun more_hasItsBand_theSettingsIcon_andFiveRowsWithChevrons() {
        composeRule.setContent { ATSAssetSystemTheme { MoreScreen(More.of(), onOpenNetWorth = {}) } }
        composeRule.waitForIdle()
        val title = composeRule.onNode(isHeading() and hasText(string(R.string.tab_more)), useUnmergedTree = true).laidOutStyle()
        assertEquals(Asap, title.fontFamily)
        assertEquals(22.sp, title.fontSize)
        assertTrue("The band's settings icon: ${icons()}", "settings" in icons())
        assertEquals("A chevron on each of the five rows: ${icons()}", 5, icons().count { it == "chevron-right" })
    }

    @Test
    fun settings_hasMedium14SectionHeadings_andRowsWithChevrons() {
        val state = AppState(screen = Screen.SETTINGS, tab = Tab.MORE, autoLock = AutoLock.EVERY_LEAVE)
        composeRule.setContent { ATSAssetSystemTheme { SettingsScreen(state, SettingsActions({}, {}, {}, {})) } }
        composeRule.waitForIdle()
        val heading = composeRule.onNode(isHeading() and hasText(string(R.string.settings_security)), useUnmergedTree = true).laidOutStyle()
        assertEquals(14.sp, heading.fontSize)
        assertEquals(FontWeight.W500, heading.fontWeight)
        assertTrue("Back and the rows' chevrons: ${icons()}", "chevron-left" in icons() && icons().count { it == "chevron-right" } >= 3)
    }

    @Test
    fun tabOrder_hasArrowIconButtons_onEachRow() {
        val state = AppState(screen = Screen.TAB_ORDER, tab = Tab.MORE, autoLock = AutoLock.EVERY_LEAVE)
        composeRule.setContent { ATSAssetSystemTheme { TabOrderScreen(state, TabOrderActions({}, {}, {})) } }
        composeRule.waitForIdle()
        assertEquals("Four arrow-up: ${icons()}", 4, icons().count { it == "arrow-up" })
        assertEquals("Four arrow-down: ${icons()}", 4, icons().count { it == "arrow-down" })
    }

    @Test
    fun aList_hasAddValueGold() {
        val state =
            AppState(
                screen = Screen.CLASS_LIST,
                tab = Tab.MORE,
                autoLock = AutoLock.EVERY_LEAVE,
                classLists = ClassLists(sectors = listOf("Technology", "Diversified"), geographies = emptyList(), remembered = emptyList()),
                classListShown = ClassList.SECTOR,
            )
        composeRule.setContent { ATSAssetSystemTheme { ClassListScreen(state, ClassListActions({}, {}, {}, {}, {}, {}, {})) } }
        composeRule.waitForIdle()
        val add = composeRule.onNode(hasText(string(R.string.list_add_value)) and hasClickAction())
        add.performScrollTo()
        val bounds = add.fetchSemanticsNode().boundsInRoot
        val fill = composeRule.onRoot().colorAt(bounds.left + with(composeRule.density) { 12.dp.toPx() }, bounds.center.y)
        assertTrue("Add value ${describe(fill)}, expected ${describe(TokenColors.light.gold)}", sameColor(fill, TokenColors.light.gold))
    }
}
