package app.ghghorg.atsassets

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.ghghorg.atsassets.core.Holdings
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.SplitRatio
import app.ghghorg.atsassets.core.Tab
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import app.ghghorg.atsassets.ui.AppState
import app.ghghorg.atsassets.ui.HoldingActions
import app.ghghorg.atsassets.ui.HoldingDetailScreen
import app.ghghorg.atsassets.ui.ReversalActions
import app.ghghorg.atsassets.ui.Screen
import app.ghghorg.atsassets.ui.TransactionReversalScreen
import app.ghghorg.atsassets.ui.theme.ATSAssetSystemTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDate

/**
 * TalkBack labels (14c, Deploy finding 2): a buy stored as 1 share with a ratio of 1/3 is read with its exact shares,
 * 0.33333333, on Holding detail's Transactions row and on the reversal's card.
 */
@RunWith(AndroidJUnit4::class)
class TalkBackLabelTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val today = LocalDate.of(2026, 11, 4)
    private val buy =
        Transaction(
            id = "t1",
            portfolioId = "p1",
            ticker = "SPKE",
            type = TransactionType.BUY,
            date = LocalDate.of(2026, 10, 1),
            enteredSeq = 1,
            quantity = BigDecimal.ONE,
            totalCents = 10_000,
            quantityRatio = SplitRatio(to = 1, from = 3),
        )
    private val portfolios = listOf(Portfolio(id = "p1", name = "Tests", createdSeq = 1))

    @Test
    fun transactionRow_readsTheExactShares() {
        val state =
            AppState(
                screen = Screen.HOLDING,
                tab = Tab.PORTFOLIOS,
                transactions = listOf(buy),
                holdings = Holdings.holdings(listOf(buy), today),
                today = today,
                portfolios = portfolios,
                portfolioId = "p1",
                holdingTicker = "SPKE",
            )
        composeRule.setContent {
            ATSAssetSystemTheme { HoldingDetailScreen(state, HoldingActions({}, {}, {}, {}, {}, {}, {})) }
        }
        composeRule.onNode(hasContentDescription("buy, 0.33333333 shares", substring = true)).assertExists()
    }

    @Test
    fun reversalCard_readsTheExactShares() {
        val state =
            AppState(
                screen = Screen.REVERSE_TRANSACTION,
                tab = Tab.PORTFOLIOS,
                transactions = listOf(buy),
                today = today,
                transactionId = "t1",
                portfolios = portfolios,
            )
        composeRule.setContent {
            ATSAssetSystemTheme { TransactionReversalScreen(state, ReversalActions({}, {}, {})) }
        }
        composeRule.onNode(hasContentDescription("Tests, 0.33333333 shares", substring = true)).assertExists()
    }
}
