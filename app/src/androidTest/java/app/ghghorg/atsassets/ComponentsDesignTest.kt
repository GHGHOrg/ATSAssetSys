package app.ghghorg.atsassets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.ghghorg.atsassets.ui.design.AtsBlockedCard
import app.ghghorg.atsassets.ui.design.AtsButton
import app.ghghorg.atsassets.ui.design.AtsChip
import app.ghghorg.atsassets.ui.design.AtsRadioRow
import app.ghghorg.atsassets.ui.design.AtsTextField
import app.ghghorg.atsassets.ui.design.ButtonKind
import app.ghghorg.atsassets.ui.theme.ATSAssetSystemTheme
import app.ghghorg.atsassets.ui.theme.Asap
import app.ghghorg.atsassets.ui.theme.AtsTheme
import app.ghghorg.atsassets.ui.theme.TokenColors
import app.ghghorg.atsassets.ui.theme.TokenPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The catalog's components drawn from the tokens (44a step 6; spec 6, Visual design (1), (3) and (6); AC-61 (e); D-16, D-17,
 * D-27, D-36): a filled button is gold with navy Asap 14 medium; a button that cannot be tapped yet is read as unavailable;
 * a selected chip shows its check in the selected colors; a refused field shows circle-alert and its message in bronze; the
 * blocked card has a bronze outline; a selected radio row has its dot. The texts are test placeholders, not app wording.
 */
@RunWith(AndroidJUnit4::class)
class ComponentsDesignTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun show(
        dark: Boolean = false,
        content: @Composable () -> Unit,
    ) {
        composeRule.setContent {
            ATSAssetSystemTheme(darkTheme = dark) {
                Column(modifier = Modifier.background(AtsTheme.colors.page).padding(12.dp)) { content() }
            }
        }
        composeRule.waitForIdle()
    }

    private fun palette(dark: Boolean): TokenPalette = if (dark) TokenColors.dark else TokenColors.light

    private fun checkFilledButton(dark: Boolean) {
        show(dark) { AtsButton("Save", onClick = {}, kind = ButtonKind.FILLED) }
        val colors = palette(dark)
        val style = composeRule.onNode(hasText("Save"), useUnmergedTree = true).laidOutStyle()
        assertEquals(Asap, style.fontFamily)
        assertEquals(14.sp, style.fontSize)
        assertEquals(FontWeight.W500, style.fontWeight)
        assertTrue("Text ${describe(style.color)}", sameColor(style.color, colors.onGold, tolerance = 0))
        val bounds = composeRule.onNode(hasText("Save") and hasClickAction()).fetchSemanticsNode().boundsInRoot
        val fill = composeRule.onRoot().colorAt(bounds.left + 4f, bounds.center.y)
        assertTrue("Fill ${describe(fill)}, expected ${describe(colors.gold)}", sameColor(fill, colors.gold))
    }

    @Test
    fun filledButton_isGold_withNavyAsap14Medium_light() = checkFilledButton(dark = false)

    @Test
    fun filledButton_isGold_withNavyAsap14Medium_dark() = checkFilledButton(dark = true)

    @Test
    fun unavailableButton_isReadAsUnavailable() {
        show { AtsButton("Delete", onClick = {}, enabled = false) }
        composeRule.onNode(hasText("Delete") and hasClickAction()).assertIsNotEnabled()
    }

    @Test
    fun selectedChip_showsItsCheck_inTheSelectedColors() {
        show { AtsChip("All", selected = true, onClick = {}) }
        val colors = palette(dark = false)
        assertEquals(listOf("check"), composeRule.drawnIcons().map { it.first })
        val chip = composeRule.onNode(hasText("All") and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
        chip.assertIsSelectedOrOn()
        val style = composeRule.onNode(hasText("All"), useUnmergedTree = true).laidOutStyle()
        assertTrue("Text ${describe(style.color)}", sameColor(style.color, colors.onChipOn, tolerance = 0))
        val icon = composeRule.drawnIcons().first().second
        val fill = composeRule.onRoot().colorAt(icon.left - with(composeRule.density) { 4.dp.toPx() }, icon.center.y)
        assertTrue("Chip ${describe(fill)}, expected ${describe(colors.chipOn)}", sameColor(fill, colors.chipOn))
    }

    @Test
    fun refusedField_showsCircleAlert_andItsMessageInBronze() {
        show { AtsTextField("13/40", onValueChange = {}, label = "Date", refusal = "Not a date") }
        val colors = palette(dark = false)
        assertTrue(composeRule.drawnIcons().any { it.first == "circle-exclamation-point" })
        val style = composeRule.onNode(hasText("Not a date"), useUnmergedTree = true).laidOutStyle()
        assertEquals(Asap, style.fontFamily)
        assertEquals(12.sp, style.fontSize)
        assertEquals(FontWeight.W500, style.fontWeight)
        assertTrue("Message ${describe(style.color)}", sameColor(style.color, colors.error, tolerance = 0))
    }

    @Test
    fun blockedCard_hasABronzeOutline() {
        show { AtsBlockedCard("Not saved", "Cash would be negative.") }
        val colors = palette(dark = false)
        assertTrue(composeRule.drawnIcons().any { it.first == "circle-exclamation-point" })
        val root = composeRule.onRoot()
        val card = composeRule.onNode(hasText("Not saved"), useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val rootBounds = root.fetchSemanticsNode().boundsInRoot
        // The outline is 2 dp wide at the card's left edge, 12 dp in from the screen's (the test's padding).
        val edge = with(composeRule.density) { (12.dp + 1.dp).toPx() }
        val outline = root.colorAt(rootBounds.left + edge, card.center.y)
        assertTrue("Outline ${describe(outline)}, expected ${describe(colors.error)}", sameColor(outline, colors.error))
    }

    @Test
    fun selectedRadioRow_hasItsDot() {
        show { AtsRadioRow("After 1 minute idle", selected = true, onClick = {}) }
        val colors = palette(dark = false)
        val row = composeRule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        row.assertIsSelected()
        val bounds = row.fetchSemanticsNode().boundsInRoot
        // The circle's center: the row's inset (10) and half the circle (10) in from its start.
        val center = with(composeRule.density) { (10.dp + 10.dp).toPx() }
        val dot = composeRule.onRoot().colorAt(bounds.left + center, bounds.center.y)
        assertTrue("Dot ${describe(dot)}, expected ${describe(colors.check)}", sameColor(dot, colors.check))
    }
}

private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertIsSelectedOrOn() {
    val config = fetchSemanticsNode().config
    val on = config.contains(SemanticsProperties.ToggleableState) || config.contains(SemanticsProperties.Selected)
    assertTrue("The chip reports its state", on)
}
