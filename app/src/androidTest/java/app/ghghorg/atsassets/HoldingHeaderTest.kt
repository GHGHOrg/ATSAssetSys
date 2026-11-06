package app.ghghorg.atsassets

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.ghghorg.atsassets.core.Holdings
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import app.ghghorg.atsassets.ui.AppState
import app.ghghorg.atsassets.ui.HoldingActions
import app.ghghorg.atsassets.ui.HoldingDetailScreen
import app.ghghorg.atsassets.ui.Screen
import app.ghghorg.atsassets.ui.Tab
import app.ghghorg.atsassets.ui.theme.ATSAssetSystemTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Defect 10 (10 phone check 6a): Holding detail's fixed part stays in place only while it takes at most half the
 * screen's height; taller, it scrolls up with the list. Measured on the emulator, turned and at a set font scale.
 */
@RunWith(AndroidJUnit4::class)
class HoldingHeaderLargestFontLandscapeTest {
    private val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val rules: RuleChain =
        RuleChain
            .outerRule(DeviceUnlockedRule())
            .around(DeviceSettingsRule(fontScale = 2.0f))
            .around(composeRule)

    @Test
    fun header_scrollsWithTheList() {
        composeRule.turn(landscape = true)
        composeRule.checkHeader(expectFixed = false)
    }
}

@RunWith(AndroidJUnit4::class)
class HoldingHeaderNormalFontPortraitTest {
    private val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val rules: RuleChain =
        RuleChain
            .outerRule(DeviceUnlockedRule())
            .around(DeviceSettingsRule(fontScale = 1.0f))
            .around(composeRule)

    @Test
    fun header_staysInPlace() {
        composeRule.turn(landscape = false)
        composeRule.checkHeader(expectFixed = true)
    }
}

private const val TICKER = "SPKE"

// Thirty buys, so the Transactions list is longer than the screen.
private fun holdingState(): AppState {
    val today = LocalDate.of(2026, 11, 4)
    val transactions =
        (1..30).map { day ->
            Transaction(
                id = "t$day",
                portfolioId = "p1",
                ticker = TICKER,
                type = TransactionType.BUY,
                date = LocalDate.of(2026, 10, 1).plusDays(day.toLong()),
                enteredSeq = day.toLong(),
                quantity = BigDecimal.ONE,
                totalCents = 10_000,
            )
        }
    return AppState(
        screen = Screen.HOLDING,
        tab = Tab.PORTFOLIOS,
        transactions = transactions,
        holdings = Holdings.holdings(transactions, today),
        today = today,
        portfolios = listOf(Portfolio(id = "p1", name = "Tests", createdSeq = 1)),
        portfolioId = "p1",
        holdingTicker = TICKER,
    )
}

private fun ComposeContentTestRule.checkHeader(expectFixed: Boolean) {
    setContent {
        ATSAssetSystemTheme {
            HoldingDetailScreen(holdingState(), HoldingActions({}, {}, {}, {}, {}, {}, {}))
        }
    }
    val rootHeight = onRoot().fetchSemanticsNode().size.height
    // The tab row is the fixed part's last line.
    val fixedBottom = onNode(isSelectable() and hasText(string(R.string.holding_tab_transactions))).fetchSemanticsNode().boundsInRoot.bottom
    assertEquals(
        "Setup: the fixed part ends at $fixedBottom px of $rootHeight px, so this case does not occur here",
        !expectFixed,
        fixedBottom > rootHeight / 2f,
    )
    val before = onNodeWithText(TICKER).fetchSemanticsNode().boundsInRoot.top
    onRoot().performTouchInput { swipeUp(startY = bottom * 0.95f, endY = bottom * 0.55f) }
    waitForIdle()
    val after = onNodeWithText(TICKER).fetchSemanticsNode().boundsInRoot.top
    if (expectFixed) {
        assertEquals("The fixed part moved though it takes at most half the height", before, after, 0.5f)
    } else {
        assertTrue("The fixed part stayed in place though it takes more than half the height (10): $before px, then $after px", after < before)
    }
}
