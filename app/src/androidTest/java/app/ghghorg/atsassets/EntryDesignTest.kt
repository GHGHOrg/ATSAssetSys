package app.ghghorg.atsassets

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.Tab
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import app.ghghorg.atsassets.ui.AppState
import app.ghghorg.atsassets.ui.BuyActions
import app.ghghorg.atsassets.ui.BuyEntryScreen
import app.ghghorg.atsassets.ui.BuyForm
import app.ghghorg.atsassets.ui.BuyRefusals
import app.ghghorg.atsassets.ui.BuyReviewScreen
import app.ghghorg.atsassets.ui.SaveRefusal
import app.ghghorg.atsassets.ui.Screen
import app.ghghorg.atsassets.ui.SellActions
import app.ghghorg.atsassets.ui.SellEntryScreen
import app.ghghorg.atsassets.ui.SellForm
import app.ghghorg.atsassets.ui.theme.ATSAssetSystemTheme
import app.ghghorg.atsassets.ui.theme.Asap
import app.ghghorg.atsassets.ui.theme.TokenColors
import app.ghghorg.atsassets.ui.theme.tokenDp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDate

private val entryPortfolio = Portfolio("p1", "Portfolio P", 1L)
private val entryOpening = CashEntry("o1", LocalDate.of(2026, 1, 1), 1_000_000L, CashType.OPENING_BALANCE, "", 1L)
private val entryBuy =
    Transaction(
        id = "b1",
        portfolioId = "p1",
        ticker = "AAPL",
        type = TransactionType.BUY,
        date = LocalDate.of(2026, 1, 2),
        enteredSeq = 2L,
        quantity = BigDecimal("10"),
        totalCents = 100_000L,
    )

private fun entryState(screen: Screen) = AppState(
    screen = screen,
    tab = Tab.OVERALL,
    entries = listOf(entryOpening, CashEntry("c-b1", entryBuy.date, -100_000L, CashType.TRADE, "", 2L, transactionId = "b1")),
    transactions = listOf(entryBuy),
    today = LocalDate.of(2026, 6, 1),
    portfolios = listOf(entryPortfolio),
    buyForm = BuyForm(portfolioId = "p1", tickerText = "AAPL", dateText = "20260201", quantityText = "10", totalText = "1500"),
    sellForm = SellForm(portfolioId = "p1", ticker = "AAPL", dateText = "20260301", quantityText = "10", totalText = "2100", picks = mapOf("b1" to "10")),
    priceKeySet = true,
)

private val noBuyActions = BuyActions({}, {}, {}, {}, {}, {}, {})

/**
 * The buy and sell entries' look (44a step 8, group (d) part 1; spec 6, Visual design; AC-61 (e); drawing
 * 10a-buy-sell, D-31): Buy under its band with the portfolio's pick field and the date's calendar, Review the gold button;
 * a refused save as the blocked card with its bronze outline; the review's cash effect as the total line in medium 16;
 * Sell's picked lot with the catalog's checkbox. Data: Example A's first buy.
 */
@RunWith(AndroidJUnit4::class)
class EntryDesignTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun buy_hasItsBand_thePickField_theCalendar_andTheGoldReview() {
        composeRule.setContent { ATSAssetSystemTheme { BuyEntryScreen(entryState(Screen.BUY), noBuyActions) } }
        composeRule.waitForIdle()
        val title = composeRule.onNode(isHeading() and hasText(string(R.string.buy_button)), useUnmergedTree = true).laidOutStyle()
        assertEquals(Asap, title.fontFamily)
        assertEquals(22.sp, title.fontSize)
        val icons = composeRule.drawnIcons().map { it.first }
        assertTrue("Back, the portfolio's chevron-down and the date's calendar: $icons", icons.containsAll(listOf("chevron-left", "chevron-down", "calendar")))
        val review = composeRule.onNode(hasText(string(R.string.review_button)) and hasClickAction())
        review.performScrollTo()
        val bounds = review.fetchSemanticsNode().boundsInRoot
        val fill = composeRule.onRoot().colorAt(bounds.left + with(composeRule.density) { 12.dp.toPx() }, bounds.center.y)
        assertTrue("Review ${describe(fill)}, expected ${describe(TokenColors.light.gold)}", sameColor(fill, TokenColors.light.gold))
    }

    @Test
    fun aRefusedSave_isTheBlockedCard_withItsBronzeOutline() {
        val state = entryState(Screen.BUY).copy(buyRefusals = BuyRefusals(save = SaveRefusal.SAVE_FAILED))
        composeRule.setContent { ATSAssetSystemTheme { BuyEntryScreen(state, noBuyActions) } }
        composeRule.waitForIdle()
        val text = composeRule.onNode(hasText(string(R.string.save_failed)), useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        // The card's outline: left of the text by the icon, its gap and the card's inset, then 1 dp into the 2 dp line.
        val x = text.left - with(composeRule.density) { (tokenDp("fieldMarkSize") + tokenDp("fieldDensePadding") + tokenDp("spaceInset") - 1.dp).toPx() }
        val line = composeRule.onRoot().colorAt(x, text.center.y)
        assertTrue("Outline ${describe(line)}, expected ${describe(TokenColors.light.error)}", sameColor(line, TokenColors.light.error))
    }

    @Test
    fun theReviewsCashEffect_isTheTotalLine_inMedium16() {
        composeRule.setContent { ATSAssetSystemTheme { BuyReviewScreen(entryState(Screen.BUY_REVIEW), {}, {}) } }
        composeRule.waitForIdle()
        val effect = composeRule.onNode(hasText(string(R.string.cash_effect_label)), useUnmergedTree = true).laidOutStyle()
        assertEquals(16.sp, effect.fontSize)
        assertEquals(FontWeight.W500, effect.fontWeight)
        val icons = composeRule.drawnIcons().map { it.first }
        assertTrue("Back: $icons", "chevron-left" in icons)
    }

    @Test
    fun sell_hasBothPickFields_andThePickedLotsCheckbox() {
        val actions = SellActions({}, {}, {}, {}, {}, {}, { _, _ -> }, {}, {}, {})
        composeRule.setContent { ATSAssetSystemTheme { SellEntryScreen(entryState(Screen.SELL), actions) } }
        composeRule.waitForIdle()
        val icons = composeRule.drawnIcons().map { it.first }
        assertEquals("The portfolio's and the ticker's chevron-down: $icons", 2, icons.count { it == "chevron-down" })
        assertTrue("The picked lot's check: $icons", "check" in icons)
    }
}

/**
 * CLAUDE.md's pitfall on layers and menus (07; AC-61 (b)): on the real MainActivity, unlocked, Buy's portfolio list keeps
 * the window's focus, and a tap on a portfolio reaches it and closes the list (44a step 8 group (d) part 1). A portfolio is
 * created first, the opening balance with it if missing.
 */
@RunWith(AndroidJUnit4::class)
class BuyPortfolioChoiceFocusTest {
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(DeviceUnlockedRule()).around(composeRule)

    @Test
    fun theList_keepsTheFocus_andTakesATap() {
        composeRule.unlockApp()
        composeRule.openNewPortfolioLayer()
        val name = "Pick ${System.currentTimeMillis() % 100_000}"
        composeRule.onNode(nameField()).performTextInput(name)
        composeRule.onNode(button(R.string.create_button)).performClick()
        composeRule.waitUntil(15_000) { composeRule.onAllNodes(nameField()).fetchSemanticsNodes().isEmpty() }
        composeRule.onNode(isSelectable() and hasText(string(R.string.tab_portfolios))).performClick()
        composeRule.onNode(hasContentDescription(string(R.string.buy_sell_spoken)) and hasClickAction()).performClick()
        composeRule.onNode(button(R.string.buy_button)).performClick()
        val field = hasContentDescription(portfolioSpoken(""), substring = true) and hasClickAction()
        composeRule.waitUntil(5_000) { composeRule.onAllNodes(field).fetchSemanticsNodes().isNotEmpty() }
        val option = hasText(name) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected)
        composeRule.assertFocusHeld("Buy's portfolio list") {
            composeRule.onNode(field).performClick()
            composeRule.waitUntil(5_000) { composeRule.onAllNodes(option).fetchSemanticsNodes().isNotEmpty() }
        }
        composeRule.onNode(option).performScrollTo().performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodes(option).fetchSemanticsNodes().isEmpty() }
        composeRule.waitUntil(5_000) { composeRule.onAllNodes(hasContentDescription(portfolioSpoken(name))).fetchSemanticsNodes().isNotEmpty() }
    }

    // "Portfolio, Main": the pick field's sentence.
    private fun portfolioSpoken(name: String): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.allocation_portfolio_spoken, name)
}
