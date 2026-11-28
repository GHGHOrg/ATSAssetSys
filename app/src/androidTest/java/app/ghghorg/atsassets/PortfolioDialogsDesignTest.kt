package app.ghghorg.atsassets

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.ghghorg.atsassets.core.DeletionCheck
import app.ghghorg.atsassets.core.DeletionSummary
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.PortfolioRefusal
import app.ghghorg.atsassets.ui.DeleteDialog
import app.ghghorg.atsassets.ui.DeleteDialogActions
import app.ghghorg.atsassets.ui.PortfolioDeleteLayers
import app.ghghorg.atsassets.ui.PortfolioDeletions
import app.ghghorg.atsassets.ui.PortfolioDialog
import app.ghghorg.atsassets.ui.PortfolioNameDialog
import app.ghghorg.atsassets.ui.PortfolioNameWording
import app.ghghorg.atsassets.ui.theme.ATSAssetSystemTheme
import app.ghghorg.atsassets.ui.theme.Asap
import app.ghghorg.atsassets.ui.theme.TokenColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

private fun dialogText(
    id: Int,
    vararg args: Any,
): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

private val dialogPortfolio = Portfolio("p1", "Portfolio P", 1L)

// Example A's summary (AC-55): 3 transactions, 3 trade cash lines, +$800.00, $9,600.00 to $10,000.00.
private val dialogSummary = DeletionSummary(dialogPortfolio, 3, 3, Fraction.ofCents(80_000L), 960_000L, 1_000_000L)

/**
 * The portfolio dialogs' look (44a step 8, group (e) part 1; spec 6, Visual design; AC-61 (e); drawing
 * 10d-portfolio-dialogs, D-34): New portfolio's title in Asap 22, Create gold and Cancel a text button on the raised
 * surface; a refused name on the field with circle-alert, in place of the note; Delete's warning with triangle-alert and
 * Delete portfolio unavailable until the name is typed, then gold; the blocked dialog titled with the portfolio, its
 * blocked card and Close gold.
 */
@RunWith(AndroidJUnit4::class)
class PortfolioDialogsDesignTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun nameDialog(dialog: PortfolioDialog) {
        composeRule.setContent {
            ATSAssetSystemTheme {
                PortfolioNameDialog(
                    PortfolioNameWording(string(R.string.new_portfolio), string(R.string.portfolio_name_helper), string(R.string.create_button)),
                    dialog,
                    saving = false,
                    onNameChange = {},
                    onConfirm = {},
                    onCancel = {},
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun fillAt(
        id: Int,
        inset: Int,
    ) = composeRule.onNode(hasText(string(id)) and hasClickAction()).fetchSemanticsNode().boundsInRoot.let { bounds ->
        composeRule.onRoot().colorAt(bounds.left + with(composeRule.density) { inset.dp.toPx() }, bounds.center.y)
    }

    @Test
    fun newPortfolio_hasItsTitle_theNote_aGoldCreate_andATextCancel() {
        nameDialog(PortfolioDialog(nameText = "Growth"))
        val title = composeRule.onNode(isHeading() and hasText(string(R.string.new_portfolio)), useUnmergedTree = true).laidOutStyle()
        assertEquals(Asap, title.fontFamily)
        assertEquals(22.sp, title.fontSize)
        composeRule.onNode(hasText(string(R.string.portfolio_name_helper)), useUnmergedTree = true).assertExists()
        val create = fillAt(R.string.create_button, 12)
        assertTrue("Create ${describe(create)}, expected ${describe(TokenColors.light.gold)}", sameColor(create, TokenColors.light.gold))
        val cancel = fillAt(R.string.cancel_button, 2)
        assertTrue("Cancel's edge ${describe(cancel)}, expected ${describe(TokenColors.light.float)}", sameColor(cancel, TokenColors.light.float))
    }

    @Test
    fun aRefusedName_isOnTheField_inPlaceOfTheNote() {
        nameDialog(PortfolioDialog(nameText = "retirement", refusal = PortfolioRefusal.Duplicate("Retirement")))
        composeRule.onNode(hasText(dialogText(R.string.portfolio_name_duplicate, "Retirement")), useUnmergedTree = true).assertExists()
        composeRule.onNode(hasText(string(R.string.portfolio_name_helper)), useUnmergedTree = true).assertDoesNotExist()
        val icons = composeRule.drawnIcons().map { it.first }
        assertTrue("The field's circle-alert: $icons", "circle-exclamation-point" in icons)
    }

    @Test
    fun delete_warnsWithTriangleAlert_andDeletePortfolio_turnsGoldWithTheName() {
        var typed by mutableStateOf("")
        composeRule.setContent {
            ATSAssetSystemTheme {
                PortfolioDeleteLayers(PortfolioDeletions.dialog(DeleteDialog(dialogSummary, typed), false), null, DeleteDialogActions({}, {}, {}), {})
            }
        }
        composeRule.waitForIdle()
        val icons = composeRule.drawnIcons().map { it.first }
        assertTrue("The warning's triangle-alert: $icons", "triangle-exclamation-point" in icons)
        composeRule.onNode(hasText(string(R.string.delete_confirm_button)) and hasClickAction()).assertIsNotEnabled()
        typed = "portfolio p"
        composeRule.waitForIdle()
        val fill = fillAt(R.string.delete_confirm_button, 12)
        assertTrue("Delete portfolio ${describe(fill)}, expected ${describe(TokenColors.light.gold)}", sameColor(fill, TokenColors.light.gold))
    }

    @Test
    fun aBlockedDeletion_isADialogTitledWithThePortfolio_withItsBlockedCard_andAGoldClose() {
        val blocked = PortfolioDeletions.blocked(DeletionCheck.Blocked(LocalDate.parse("2026-02-10"), 20_000L), "Portfolio Q")
        composeRule.setContent { ATSAssetSystemTheme { PortfolioDeleteLayers(null, blocked, DeleteDialogActions({}, {}, {}), {}) } }
        composeRule.waitForIdle()
        composeRule.onNode(isHeading() and hasText(dialogText(R.string.delete_blocked_title, "Portfolio Q")), useUnmergedTree = true).assertExists()
        val icons = composeRule.drawnIcons().map { it.first }
        assertTrue("The blocked card's circle-alert: $icons", "circle-exclamation-point" in icons)
        val close = fillAt(R.string.close_button, 12)
        assertTrue("Close ${describe(close)}, expected ${describe(TokenColors.light.gold)}", sameColor(close, TokenColors.light.gold))
    }
}
