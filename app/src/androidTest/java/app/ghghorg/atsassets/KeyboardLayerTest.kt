package app.ghghorg.atsassets

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * Defect 08 (08 phone check 9a): at the largest font (plan.md, 2026-10-30), with the keyboard open, New portfolio's
 * field and both buttons lie fully above it.
 */
@RunWith(AndroidJUnit4::class)
class KeyboardLayerTest {
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain =
        RuleChain
            .outerRule(DeviceUnlockedRule())
            .around(DeviceSettingsRule(fontScale = 2.0f))
            .around(composeRule)

    @Test
    fun newPortfolioLayer_staysAboveTheKeyboard() {
        composeRule.unlockApp()
        composeRule.openNewPortfolioLayer()
        composeRule.onNode(nameField()).performClick()
        val keyboardTop = composeRule.keyboardTop()
        composeRule.waitForIdle()
        val parts =
            listOf(
                "the Name field" to nameField(),
                "Cancel" to button(R.string.cancel_button),
                "Create" to button(R.string.create_button),
            )
        parts.forEach { (label, matcher) ->
            val bottom = composeRule.onNode(matcher).fetchSemanticsNode().boundsInWindow.bottom
            assertTrue("$label ends at $bottom px, below the keyboard's top at $keyboardTop px (08)", bottom <= keyboardTop)
        }
    }
}
