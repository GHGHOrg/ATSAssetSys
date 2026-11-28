package app.ghghorg.atsassets

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.Tab
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import app.ghghorg.atsassets.ui.AppState
import app.ghghorg.atsassets.ui.ReversalActions
import app.ghghorg.atsassets.ui.Screen
import app.ghghorg.atsassets.ui.SplitActions
import app.ghghorg.atsassets.ui.SplitEntryScreen
import app.ghghorg.atsassets.ui.SplitForm
import app.ghghorg.atsassets.ui.TransactionActions
import app.ghghorg.atsassets.ui.TransactionDetailScreen
import app.ghghorg.atsassets.ui.TransactionReversalScreen
import app.ghghorg.atsassets.ui.theme.ATSAssetSystemTheme
import app.ghghorg.atsassets.ui.theme.TokenColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Split, a transaction's detail and its reversal in the design system (44a step 8, group (d) part 3; spec 6, Visual
 * design; AC-61 (e); drawing 10b-split-cash-reversal frames E, G and I, D-31): Split's portfolio and ticker as pick fields;
 * the detail's cash line as a link row with its chevron and Reverse as an outline button; the reversal's band and Cancel
 * as a text button. Data: one buy of 10 AAPL for $1,500.00 after an opening of $10,000.00.
 */
@RunWith(AndroidJUnit4::class)
class TransactionDesignTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val buy =
        Transaction(
            id = "b1",
            portfolioId = "p1",
            ticker = "AAPL",
            type = TransactionType.BUY,
            date = LocalDate.of(2026, 2, 1),
            enteredSeq = 2L,
            quantity = BigDecimal("10"),
            totalCents = 150_000L,
        )

    private fun state(screen: Screen) = AppState(
        screen = screen,
        tab = Tab.PORTFOLIOS,
        entries =
        listOf(
            CashEntry("o1", LocalDate.of(2026, 1, 1), 1_000_000L, CashType.OPENING_BALANCE, "", 1L),
            CashEntry("c-b1", buy.date, -150_000L, CashType.TRADE, "", 2L, transactionId = "b1"),
        ),
        transactions = listOf(buy),
        today = LocalDate.of(2026, 6, 1),
        portfolios = listOf(Portfolio("p1", "Portfolio P", 1L)),
        portfolioId = "p1",
        holdingTicker = "AAPL",
        transactionId = "b1",
        transactionReversalDate = "20260201",
        splitForm = SplitForm(portfolioId = "p1", ticker = "AAPL", dateText = "20260301", toText = "2", fromText = "1"),
    )

    private fun icons(): List<String> = composeRule.drawnIcons().map { it.first }

    @Test
    fun split_hasBothPickFields_andTheDatesCalendar() {
        composeRule.setContent { ATSAssetSystemTheme { SplitEntryScreen(state(Screen.SPLIT), SplitActions({}, {}, {}, {}, {}, {}, {})) } }
        composeRule.waitForIdle()
        composeRule.onNode(isHeading() and hasText(string(R.string.split_button)), useUnmergedTree = true).assertExists()
        assertEquals("The portfolio's and the ticker's chevron-down: ${icons()}", 2, icons().count { it == "chevron-down" })
        assertTrue("Back and the date's calendar: ${icons()}", icons().containsAll(listOf("chevron-left", "calendar")))
    }

    @Test
    fun theDetail_linksToItsCashLine_andReverseIsAnOutlineButton() {
        composeRule.setContent { ATSAssetSystemTheme { TransactionDetailScreen(state(Screen.TRANSACTION), TransactionActions({}, {}, {}, {}, {})) } }
        composeRule.waitForIdle()
        assertTrue("Back and the cash line's chevron: ${icons()}", icons().containsAll(listOf("chevron-left", "chevron-right")))
        val reverse = composeRule.onNode(hasText(string(R.string.reverse_button)) and hasClickAction())
        reverse.performScrollTo()
        val bounds = reverse.fetchSemanticsNode().boundsInRoot
        // An outline button has no fill: inside its line, a few dp in, the page shows.
        val inside = composeRule.onRoot().colorAt(bounds.left + with(composeRule.density) { 6.dp.toPx() }, bounds.center.y)
        assertTrue("Reverse inside ${describe(inside)}, expected the page ${describe(TokenColors.light.page)}", sameColor(inside, TokenColors.light.page))
    }

    @Test
    fun theReversal_hasItsBand_andCancelAsATextButton() {
        composeRule.setContent { ATSAssetSystemTheme { TransactionReversalScreen(state(Screen.REVERSE_TRANSACTION), ReversalActions({}, {}, {})) } }
        composeRule.waitForIdle()
        assertTrue("Back and the date's calendar: ${icons()}", icons().containsAll(listOf("chevron-left", "calendar")))
        val cancel = composeRule.onNode(hasText(string(R.string.cancel_button)) and hasClickAction())
        cancel.performScrollTo()
        val bounds = cancel.fetchSemanticsNode().boundsInRoot
        val edge = composeRule.onRoot().colorAt(bounds.left + with(composeRule.density) { 2.dp.toPx() }, bounds.center.y)
        assertTrue("Cancel's edge ${describe(edge)}", sameColor(edge, TokenColors.light.page))
    }
}
