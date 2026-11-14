package app.ghghorg.atsassets

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.Tab
import app.ghghorg.atsassets.ui.AppState
import app.ghghorg.atsassets.ui.CashFilterActions
import app.ghghorg.atsassets.ui.CashFilterForm
import app.ghghorg.atsassets.ui.CashScreen
import app.ghghorg.atsassets.ui.RealizedGainsActions
import app.ghghorg.atsassets.ui.RealizedGainsScreen
import app.ghghorg.atsassets.ui.Screen
import app.ghghorg.atsassets.ui.TransactionHistoryActions
import app.ghghorg.atsassets.ui.TransactionHistoryScreen
import app.ghghorg.atsassets.ui.theme.ATSAssetSystemTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

private const val MARCH = "20260301"
private const val FEBRUARY = "20260201"

private val rangeOpening = CashEntry("o1", LocalDate.of(2026, 1, 1), 1_000_000L, CashType.OPENING_BALANCE, "", 1L)
private val rangePortfolio = Portfolio("p1", "Main", 1L)

/**
 * 29 step 8 (decision 4; the 29 file, 13.3): with From after To, the To field shows "To is before From" on Cash, Realized
 * gains and Transaction history, the shared date field, and the From field shows no line.
 */
@RunWith(AndroidJUnit4::class)
class DateRangeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun assertOnlyToHasTheLine() {
        val line = string(R.string.date_before_from)
        composeRule.onNodeWithText(line).performScrollTo().assertExists()
        assertEquals("one field has the line", 1, composeRule.onAllNodesWithText(line).fetchSemanticsNodes().size)
    }

    @Test
    fun cash_toBeforeFrom_showsTheLine() {
        val state =
            AppState(
                screen = Screen.TABS,
                tab = Tab.CASH,
                entries = listOf(rangeOpening),
                cashFilterForm = CashFilterForm(fromText = MARCH, toText = FEBRUARY),
            )
        composeRule.setContent { ATSAssetSystemTheme { CashScreen(state, {}, {}, {}, CashFilterActions({}, {}, {}, {})) } }
        assertOnlyToHasTheLine()
    }

    @Test
    fun realizedGains_toBeforeFrom_showsTheLine() {
        val state =
            AppState(
                screen = Screen.REALIZED_GAINS,
                portfolios = listOf(rangePortfolio),
                gainsFromText = MARCH,
                gainsToText = FEBRUARY,
            )
        val actions = RealizedGainsActions({}, {}, {}, {}, { _, _ -> }, {})
        composeRule.setContent { ATSAssetSystemTheme { RealizedGainsScreen(state.realizedGainsScreen, actions) } }
        assertOnlyToHasTheLine()
    }

    @Test
    fun transactionHistory_toBeforeFrom_showsTheLine() {
        val state =
            AppState(
                screen = Screen.TRANSACTION_HISTORY,
                portfolios = listOf(rangePortfolio),
                historyPortfolioId = "p1",
                historyFromText = MARCH,
                historyToText = FEBRUARY,
            )
        val actions = TransactionHistoryActions({}, {}, {}, {}, {}, {}, {}, {}, {})
        composeRule.setContent { ATSAssetSystemTheme { TransactionHistoryScreen(state.transactionHistoryScreen, actions) } }
        assertOnlyToHasTheLine()
    }
}
