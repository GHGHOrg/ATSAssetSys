package app.ghghorg.atsassets

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * Defect 07 (07 phone check 2): on the real MainActivity, unlocked, New portfolio's layer, the ⋮ menu and the Rename
 * layer keep the window's focus, so the cover screen does not replace them, and typing and tapping reach them.
 */
@RunWith(AndroidJUnit4::class)
class LockAndLayerTest {
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(DeviceUnlockedRule()).around(composeRule)

    @Test
    fun layersAndMenu_keepTheFocusAndTakeInput() {
        composeRule.unlockApp()
        composeRule.assertFocusHeld("New portfolio's layer") { composeRule.openNewPortfolioLayer() }
        // Unique, so a run on data left by an earlier run is not refused as a duplicate.
        val name = "Test ${System.currentTimeMillis() % 100_000}"
        composeRule.onNode(nameField()).performTextInput(name)
        composeRule.onNode(nameField()).assertTextContains(name)
        composeRule.onNode(button(R.string.create_button)).performClick()
        // 15 s: the first save after a cold boot took longer than 5 s (plan/spikes/screen-tests.md 11.4, series 3).
        // The focus checks are unchanged.
        if (!poll(15_000) { composeRule.onAllNodes(nameField()).fetchSemanticsNodes().isEmpty() }) {
            fail("New portfolio's layer was still open 15 s after Create; window focused: ${composeRule.windowFocused()}")
        }

        composeRule.onNode(hasContentDescription(name, substring = true) and hasClickAction()).performScrollTo().performClick()
        composeRule.assertFocusHeld("the ⋮ menu") {
            composeRule.onNode(hasContentDescription(string(R.string.portfolio_menu))).performClick()
        }
        composeRule.assertFocusHeld("the Rename layer") {
            composeRule.onNode(button(R.string.rename_button)).performClick()
            composeRule.waitUntil(5_000) { composeRule.onAllNodes(nameField()).fetchSemanticsNodes().isNotEmpty() }
        }
        val renamed = "$name R"
        composeRule.onNode(nameField()).performTextReplacement(renamed)
        composeRule.onNode(button(R.string.save_button)).performClick()
        // 15 s as for Create: 5 s ran out on GitHub's runner (plan/spikes/screen-tests.md 11.6, run 1 on 02031f0).
        if (!poll(15_000) { composeRule.onAllNodes(nameField()).fetchSemanticsNodes().isEmpty() }) {
            fail("The Rename layer was still open 15 s after Save; window focused: ${composeRule.windowFocused()}")
        }
        if (!poll(15_000) { composeRule.onAllNodesWithText(renamed).fetchSemanticsNodes().isNotEmpty() }) {
            fail("The new name was not shown 15 s after the Rename layer closed")
        }
    }
}
