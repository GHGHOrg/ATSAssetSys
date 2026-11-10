package app.ghghorg.atsassets

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.SellAllocation
import app.ghghorg.atsassets.core.Tab
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import app.ghghorg.atsassets.ui.AppState
import app.ghghorg.atsassets.ui.CashFilterActions
import app.ghghorg.atsassets.ui.CashScreen
import app.ghghorg.atsassets.ui.Screen
import app.ghghorg.atsassets.ui.theme.ATSAssetSystemTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDate

/**
 * The Cash screen (15d, AC-61 (c)): at the largest font, with the keyboard open on the filter's From field, the field
 * lies fully above the keyboard.
 */
@RunWith(AndroidJUnit4::class)
class CashFilterKeyboardTest {
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain =
        RuleChain
            .outerRule(DeviceUnlockedRule())
            .around(DeviceSettingsRule(fontScale = 2.0f))
            .around(composeRule)

    @Test
    fun theFromField_staysAboveTheKeyboard() {
        composeRule.unlockApp()
        // The filters show once an entry exists.
        val setOpening = string(R.string.guided_set_opening_balance)
        if (composeRule.onAllNodesWithText(setOpening).fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithText(setOpening).performClick()
            composeRule.onNode(hasSetTextAction() and hasText(string(R.string.amount_label))).performTextInput("1000")
            composeRule.onNode(button(R.string.review_button)).performScrollTo().performClick()
            composeRule.onNode(button(R.string.confirm_button)).performScrollTo().performClick()
            composeRule.waitUntil(10_000) { composeRule.onAllNodes(button(R.string.confirm_button)).fetchSemanticsNodes().isEmpty() }
        }
        composeRule.onNode(isSelectable() and hasText(string(R.string.tab_cash))).performClick()
        val from = hasSetTextAction() and hasText(string(R.string.filter_from))
        composeRule.waitUntil(5_000) { composeRule.onAllNodes(from).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNode(from).performScrollTo().performClick()
        val keyboardTop = composeRule.keyboardTop()
        composeRule.waitForIdle()
        val bottom = composeRule.onNode(from).fetchSemanticsNode().boundsInWindow.bottom
        assertTrue("The From field ends at $bottom px, below the keyboard's top at $keyboardTop px", bottom <= keyboardTop)
    }
}

/**
 * The Cash screen (15d, AC-61 (d)): with Example A, each row's TalkBack text reads its kind, date, signed amount and
 * the balance after it in full.
 */
@RunWith(AndroidJUnit4::class)
class CashTalkBackTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun text(
        id: Int,
        vararg args: Any,
    ): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private fun trade(
        id: String,
        type: TransactionType,
        date: String,
        seq: Long,
        totalCents: Long,
    ): Transaction = Transaction(
        id = id,
        portfolioId = "p1",
        ticker = "AAPL",
        type = type,
        date = LocalDate.parse(date),
        enteredSeq = seq,
        quantity = BigDecimal("10"),
        totalCents = totalCents,
        allocations = if (type == TransactionType.SELL) listOf(SellAllocation("b1", Fraction.of(BigDecimal("10")))) else emptyList(),
    )

    private fun line(
        transaction: Transaction,
        cents: Long,
    ): CashEntry = CashEntry("c-${transaction.id}", transaction.date, cents, CashType.TRADE, "", transaction.enteredSeq, transactionId = transaction.id)

    @Test
    fun eachRow_readsItsAmountAndBalanceInFull() {
        val b1 = trade("b1", TransactionType.BUY, "2026-01-02", 2L, 100_000L)
        val s1 = trade("s1", TransactionType.SELL, "2026-03-01", 3L, 210_000L)
        val opening = CashEntry("o1", LocalDate.of(2026, 1, 1), 1_000_000L, CashType.OPENING_BALANCE, "", 1L)
        val state =
            AppState(
                screen = Screen.TABS,
                tab = Tab.CASH,
                entries = listOf(opening, line(b1, -100_000L), line(s1, 210_000L)),
                transactions = listOf(b1, s1),
                portfolios = listOf(Portfolio("p1", "Main", 1L)),
            )
        composeRule.setContent {
            ATSAssetSystemTheme { CashScreen(state, {}, {}, {}, CashFilterActions({}, {}, {}, {})) }
        }
        val expected =
            listOf(
                text(R.string.trade_sell_spoken, "AAPL", "Main", "2026-03-01", text(R.string.spoken_plus, "2,100.00"), "11,100.00"),
                text(R.string.trade_buy_spoken, "AAPL", "Main", "2026-01-02", text(R.string.spoken_minus, "1,000.00"), "9,000.00"),
                text(R.string.cash_row_spoken, text(R.string.opening_balance), "2026-01-01", text(R.string.spoken_plus, "10,000.00"), "10,000.00"),
            )
        expected.forEach { composeRule.onNode(hasContentDescription(it)).assertExists() }
    }
}
