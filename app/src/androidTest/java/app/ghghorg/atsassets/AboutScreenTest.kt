package app.ghghorg.atsassets

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.ghghorg.atsassets.ui.About
import app.ghghorg.atsassets.ui.AboutActions
import app.ghghorg.atsassets.ui.AboutScreen
import app.ghghorg.atsassets.ui.theme.ATSAssetSystemTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

// A string with its arguments, from the app's resources.
private fun text(
    id: Int,
    vararg args: Any,
): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

// Each licence row for TalkBack: "<name>, <licence> licence", and its state, collapsed or expanded (wireframe 11 marker 3).
private fun licenceRow(
    name: String,
    licence: Int,
): SemanticsMatcher = hasContentDescription(text(R.string.about_row_spoken, name, string(licence))) and hasClickAction()

private fun state(id: Int): SemanticsMatcher = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, string(id))

/**
 * About (CR5, 41 step 8; AC-70, AC-61 (b) and (d)): from More's fifth entry, MainActivity keeps its window focus while
 * About opens and while a licence row opens in place (07: no pop-up window); the card reads the app's name and version;
 * each row reads its library and licence, collapsed, then expanded with the licence's text under it.
 */
@RunWith(AndroidJUnit4::class)
class AboutScreenTest {
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(DeviceUnlockedRule()).around(composeRule)

    @Test
    fun moresFifthEntry_opensAbout_andARowOpensInPlace_withTheFocusKept() {
        composeRule.unlockApp()
        composeRule.onNode(isSelectable() and hasText(string(R.string.tab_more))).performClick()
        val entry = hasContentDescription(string(R.string.about_title)) and hasClickAction()
        composeRule.waitUntil(5_000) { composeRule.onAllNodes(entry).fetchSemanticsNodes().isNotEmpty() }
        val heading = isHeading() and hasText(string(R.string.about_licences))
        composeRule.assertFocusHeld("About") {
            composeRule.onNode(entry).performScrollTo().performClick()
            composeRule.waitUntil(5_000) { composeRule.onAllNodes(heading).fetchSemanticsNodes().isNotEmpty() }
        }
        val version = text(R.string.about_version, BuildConfig.VERSION_NAME)
        composeRule.onNode(hasContentDescription(text(R.string.about_card_spoken, string(R.string.app_name), version))).assertExists()
        val sqlcipher = licenceRow("SQLCipher for Android", R.string.licence_sqlcipher)
        composeRule.onNode(sqlcipher and state(R.string.about_row_closed)).performScrollTo()
        composeRule.assertFocusHeld("a licence row opened in place") {
            composeRule.onNode(sqlcipher).performClick()
            composeRule.waitUntil(5_000) { composeRule.onAllNodes(sqlcipher and state(R.string.about_row_open)).fetchSemanticsNodes().isNotEmpty() }
        }
        composeRule.onNode(hasText("Copyright (c) 2008-2026, ZETETIC, LLC", substring = true)).performScrollTo()
    }
}

/** About drawn alone: every library row starts collapsed, and the Apache rows read their licence (AC-70; TalkBack). */
@RunWith(AndroidJUnit4::class)
class AboutTalkBackTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun everyRow_readsItsLibraryAndLicence_andStartsCollapsed() {
        val model = About.of("1.1.0", emptySet())
        composeRule.setContent { ATSAssetSystemTheme { AboutScreen(model, AboutActions({}, {})) } }
        composeRule.onNode(licenceRow("AndroidX Room", R.string.licence_apache_2_0) and state(R.string.about_row_closed)).assertExists()
        val collapsed = composeRule.onAllNodes(state(R.string.about_row_closed)).fetchSemanticsNodes().size
        assertEquals("every row collapsed", model.rows.size, collapsed)
    }
}
