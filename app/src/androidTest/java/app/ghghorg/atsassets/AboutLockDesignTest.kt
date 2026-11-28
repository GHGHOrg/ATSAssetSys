package app.ghghorg.atsassets

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.ghghorg.atsassets.ui.About
import app.ghghorg.atsassets.ui.AboutActions
import app.ghghorg.atsassets.ui.AboutScreen
import app.ghghorg.atsassets.ui.theme.ATSAssetSystemTheme
import app.ghghorg.atsassets.ui.theme.Asap
import app.ghghorg.atsassets.ui.theme.TokenColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private fun aboutText(
    id: Int,
    vararg args: Any,
): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

/**
 * About and the lock screens' look (44a step 8, group (e) part 4; spec 6, Visual design; AC-61 (e), AC-70, C27; drawings
 * 09-11 frames C and D and remaining-screens frame 4): About's band, the font's and the icons' rows with their licences,
 * an open row's chevron-up and its text taken from the pinned file; the locked screen's Unlock gold; the cover's name in
 * Asap 22.
 */
@RunWith(AndroidJUnit4::class)
class AboutLockDesignTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun icons(): List<String> = composeRule.drawnIcons().map { it.first }

    @Test
    fun about_hasItsBand_andTheFontsAndTheIconsRows() {
        composeRule.setContent { ATSAssetSystemTheme { AboutScreen(About.of("1.3.0", emptySet()), AboutActions({}, {})) } }
        composeRule.waitForIdle()
        val title = composeRule.onNode(isHeading() and hasText(string(R.string.about_title)), useUnmergedTree = true).laidOutStyle()
        assertEquals(Asap, title.fontFamily)
        assertEquals(22.sp, title.fontSize)
        val rows =
            listOf(
                "Asap and Asap Condensed (fonts)" to string(R.string.licence_ofl_1_1),
                "Lucide (icons)" to string(R.string.licence_isc_mit),
            )
        for ((name, licence) in rows) {
            composeRule.onNode(hasContentDescription(aboutText(R.string.about_row_spoken, name, licence)) and hasClickAction()).performScrollTo()
        }
        assertTrue("A closed row's chevron-down: ${icons()}", "chevron-down" in icons())
    }

    @Test
    fun anOpenRow_turnsItsChevronUp_andShowsTheLicenceFromThePinnedFile() {
        composeRule.setContent { ATSAssetSystemTheme { AboutScreen(About.of("1.3.0", setOf("Lucide (icons)")), AboutActions({}, {})) } }
        composeRule.waitForIdle()
        val row = hasContentDescription(aboutText(R.string.about_row_spoken, "Lucide (icons)", string(R.string.licence_isc_mit))) and hasClickAction()
        composeRule.onNode(row).performScrollTo()
        composeRule.onNode(hasText("ISC License", substring = true), useUnmergedTree = true).assertExists()
        assertTrue("The open row's chevron-up: ${icons()}", "chevron-up" in icons())
    }

    @Test
    fun theLockedScreen_hasUnlockGold() {
        composeRule.setContent { ATSAssetSystemTheme { LockedScreen(onUnlock = {}) } }
        composeRule.waitForIdle()
        val bounds = composeRule.onNode(hasText(string(R.string.unlock_button)) and hasClickAction()).fetchSemanticsNode().boundsInRoot
        val fill = composeRule.onRoot().colorAt(bounds.left + with(composeRule.density) { 12.dp.toPx() }, bounds.center.y)
        assertTrue("Unlock ${describe(fill)}, expected ${describe(TokenColors.light.gold)}", sameColor(fill, TokenColors.light.gold))
    }

    @Test
    fun theCover_showsTheNameInAsap22_onThePage() {
        composeRule.setContent { ATSAssetSystemTheme { CoverScreen() } }
        composeRule.waitForIdle()
        val name = composeRule.onNode(hasText(string(R.string.app_name)), useUnmergedTree = true)
        val style = name.laidOutStyle()
        assertEquals(Asap, style.fontFamily)
        assertEquals(22.sp, style.fontSize)
        val bounds = name.fetchSemanticsNode().boundsInRoot
        val page = composeRule.onRoot().colorAt(bounds.left - with(composeRule.density) { 8.dp.toPx() }, bounds.center.y)
        assertTrue("The cover ${describe(page)}, expected ${describe(TokenColors.light.page)}", sameColor(page, TokenColors.light.page))
    }
}
