package app.ghghorg.atsassets

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.Tab
import app.ghghorg.atsassets.ui.AppState
import app.ghghorg.atsassets.ui.CashEntryDetail
import app.ghghorg.atsassets.ui.CashEntryForm
import app.ghghorg.atsassets.ui.CashReversal
import app.ghghorg.atsassets.ui.FormState
import app.ghghorg.atsassets.ui.ReversalForm
import app.ghghorg.atsassets.ui.Screen
import app.ghghorg.atsassets.ui.theme.ATSAssetSystemTheme
import app.ghghorg.atsassets.ui.theme.Asap
import app.ghghorg.atsassets.ui.theme.TokenColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * The cash entry flow's look (44a step 8, group (d) part 2; spec 6, Visual design; AC-61 (e); drawing
 * 10b-split-cash-reversal frames A, C, H and I, D-31): the form under its band with the type as the catalog's chips, the
 * date's calendar and Review gold; the opening balance's form with its one chip; the detail's link row with its chevron
 * and Reverse as an outline button; the reversal's Cancel as a text button. Data: an opening balance and a deposit.
 */
@RunWith(AndroidJUnit4::class)
class CashEntryDesignTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val opening = CashEntry("o1", LocalDate.of(2026, 1, 1), 1_000_000L, CashType.OPENING_BALANCE, "", 1L)
    private val deposit = CashEntry("d2", LocalDate.of(2026, 4, 20), 50_000L, CashType.DEPOSIT, "Deposit entered by mistake", 2L)

    private fun state(
        screen: Screen,
        form: FormState = FormState(dateText = "2026-04-20", amountText = "500", type = CashType.DEPOSIT),
    ) = AppState(
        screen = screen,
        tab = Tab.CASH,
        entries = listOf(opening, deposit),
        form = form,
        detailId = "d2",
        reversal = ReversalForm(dateText = "2026-04-20", noteText = "Reversal of the deposit of 2026-04-20 ET"),
        today = LocalDate.of(2026, 6, 1),
    )

    private fun icons(): List<String> = composeRule.drawnIcons().map { it.first }

    @Test
    fun theForm_hasItsBand_theTypeChips_theCalendar_andTheGoldReview() {
        composeRule.setContent { ATSAssetSystemTheme { CashEntryForm(state(Screen.FORM), {}, {}, {}, {}, {}, {}, {}) } }
        composeRule.waitForIdle()
        val title = composeRule.onNode(isHeading() and hasText(string(R.string.cash_entry_button)), useUnmergedTree = true).laidOutStyle()
        assertEquals(Asap, title.fontFamily)
        assertEquals(22.sp, title.fontSize)
        assertTrue("Back, Deposit's check and the date's calendar: ${icons()}", icons().containsAll(listOf("chevron-left", "check", "calendar")))
        val review = composeRule.onNode(hasText(string(R.string.review_button)) and hasClickAction())
        review.performScrollTo()
        val bounds = review.fetchSemanticsNode().boundsInRoot
        val fill = composeRule.onRoot().colorAt(bounds.left + with(composeRule.density) { 12.dp.toPx() }, bounds.center.y)
        assertTrue("Review ${describe(fill)}", sameColor(fill, TokenColors.light.gold))
    }

    @Test
    fun theOpeningBalancesForm_hasItsOneChip_selected() {
        composeRule.setContent { ATSAssetSystemTheme { CashEntryForm(state(Screen.FORM, FormState()), {}, {}, {}, {}, {}, {}, {}) } }
        composeRule.waitForIdle()
        composeRule.onNode(isHeading() and hasText(string(R.string.opening_balance)), useUnmergedTree = true).assertExists()
        assertEquals("The one chip's check: ${icons()}", 1, icons().count { it == "check" })
    }

    @Test
    fun theDetail_hasReverse_asAnOutlineButton_andTheReversalsCancel_isATextButton() {
        composeRule.setContent { ATSAssetSystemTheme { CashEntryDetail(state(Screen.DETAIL), {}, {}, {}, {}, {}) } }
        composeRule.waitForIdle()
        composeRule.onNode(hasText(string(R.string.reverse_button)) and hasClickAction()).assertExists()
        val status = composeRule.onNode(hasText(string(R.string.detail_not_reversed)), useUnmergedTree = true).laidOutStyle()
        assertEquals(14.sp, status.fontSize)
        assertTrue("Status ${describe(status.color)}", sameColor(status.color, TokenColors.light.textSecondary, tolerance = 0))
    }

    @Test
    fun theReversal_hasItsBand_andCancelAsATextButton() {
        composeRule.setContent { ATSAssetSystemTheme { CashReversal(state(Screen.REVERSAL), {}, {}, {}, {}) } }
        composeRule.waitForIdle()
        assertTrue("Back and the date's calendar: ${icons()}", icons().containsAll(listOf("chevron-left", "calendar")))
        val cancel = composeRule.onNode(hasText(string(R.string.cancel_button)) and hasClickAction())
        cancel.performScrollTo()
        val bounds = cancel.fetchSemanticsNode().boundsInRoot
        // A text button has no fill and no outline: its left edge shows the page.
        val edge = composeRule.onRoot().colorAt(bounds.left + with(composeRule.density) { 2.dp.toPx() }, bounds.center.y)
        assertTrue("Cancel's edge ${describe(edge)}, expected the page ${describe(TokenColors.light.page)}", sameColor(edge, TokenColors.light.page))
    }
}
