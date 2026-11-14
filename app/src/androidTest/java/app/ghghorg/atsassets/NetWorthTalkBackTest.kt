package app.ghghorg.atsassets

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.ghghorg.atsassets.core.AssetType
import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.Instrument
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.Price
import app.ghghorg.atsassets.core.SellAllocation
import app.ghghorg.atsassets.core.Tab
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import app.ghghorg.atsassets.ui.AppState
import app.ghghorg.atsassets.ui.More
import app.ghghorg.atsassets.ui.MoreScreen
import app.ghghorg.atsassets.ui.NetWorthActions
import app.ghghorg.atsassets.ui.NetWorthScreen
import app.ghghorg.atsassets.ui.OverallScreen
import app.ghghorg.atsassets.ui.Screen
import app.ghghorg.atsassets.ui.theme.ATSAssetSystemTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/**
 * Net Worth (Total) (17, AC-61 (d), AC-44): with Example A, net worth's TalkBack text reads it with the price age line
 * ("some stale" when it is), each composition row reads its name and amount, and the rows add up to net worth; the
 * "updating" and refresh-failed lines show only while a refresh runs and after one failed; at $0.00 one plain line
 * replaces the composition. Overall's net worth and More's entry are buttons that open the screen.
 */
@RunWith(AndroidJUnit4::class)
class NetWorthTalkBackTest {
    @get:Rule
    val composeRule = createComposeRule()

    // 2026-10-29 16:00 New York.
    private val close = Instant.ofEpochSecond(1_793_304_000L)

    private fun text(
        id: Int,
        vararg args: Any,
    ): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private fun trade(
        id: String,
        type: TransactionType,
        date: String,
        quantity: String,
        totalCents: Long,
        seq: Long,
        allocations: List<SellAllocation> = emptyList(),
    ): Transaction = Transaction(
        id = id,
        portfolioId = "p1",
        ticker = "AAPL",
        type = type,
        date = LocalDate.parse(date),
        enteredSeq = seq,
        quantity = BigDecimal(quantity),
        totalCents = totalCents,
        allocations = allocations,
    )

    private fun line(
        transaction: Transaction,
        cents: Long,
    ): CashEntry = CashEntry("c-${transaction.id}", transaction.date, cents, CashType.TRADE, "", transaction.enteredSeq, transactionId = transaction.id)

    // Example A: $9,600.00 cash and 8 AAPL left in Portfolio P, one lot of 2026-02-01 at $150.00.
    private fun state(
        price: Price,
        refreshing: Boolean = false,
        refreshFailed: Boolean = false,
    ): AppState {
        val b1 = trade("b1", TransactionType.BUY, "2026-01-02", "10", 100_000L, 2L)
        val b2 = trade("b2", TransactionType.BUY, "2026-02-01", "10", 150_000L, 3L)
        val s1 =
            trade(
                "s1",
                TransactionType.SELL,
                "2026-03-01",
                "12",
                210_000L,
                4L,
                listOf(SellAllocation("b1", Fraction.of(BigDecimal("10"))), SellAllocation("b2", Fraction.of(BigDecimal("2")))),
            )
        val opening = CashEntry("o1", LocalDate.of(2026, 1, 1), 1_000_000L, CashType.OPENING_BALANCE, "", 1L)
        return AppState(
            screen = Screen.NET_WORTH,
            tab = Tab.OVERALL,
            entries = listOf(opening, line(b1, -100_000L), line(b2, -150_000L), line(s1, 210_000L)),
            transactions = listOf(b1, b2, s1),
            today = LocalDate.of(2026, 6, 1),
            portfolios = listOf(Portfolio("p1", "Portfolio P", 1L)),
            instruments = listOf(Instrument("AAPL", AssetType.STOCK, verified = true, price = price)),
            refreshing = refreshing,
            refreshFailed = refreshFailed,
            // 30: a key is saved, so a failed refresh is not for want of one and its line is the refresh-failed line.
            priceKeySet = true,
        )
    }

    private fun shown(id: Int): Boolean = composeRule.onAllNodes(hasText(text(id))).fetchSemanticsNodes().isNotEmpty()

    private fun showNetWorth(state: AppState) {
        composeRule.setContent { ATSAssetSystemTheme { NetWorthScreen(state, NetWorthActions({}, {})) } }
    }

    private fun row(
        name: String,
        amount: String,
    ): String = text(R.string.net_worth_composition_row_spoken, name, amount)

    @Test
    fun atAFreshPrice_readsNetWorthWithItsAgeLine_andTheCompositionAddingUpToIt() {
        showNetWorth(state(Price(BigDecimal("160"), close)))
        val age = text(R.string.portfolio_prices_as_of_spoken, "2026-10-29 16:00")
        val netWorth = text(R.string.net_worth_prices_spoken, text(R.string.net_worth_spoken, "10,880.00"), age)
        composeRule.onNode(hasContentDescription(netWorth)).assertExists()
        composeRule.onNode(hasContentDescription(row(text(R.string.net_worth_composition_cash), "9,600.00"))).assertExists()
        composeRule.onNode(hasContentDescription(row("Portfolio P", "1,280.00"))).assertExists()
        composeRule.onNode(hasContentDescription(row(text(R.string.net_worth_label), "10,880.00"))).assertExists()
        assertEquals("The refresh-failed line shown", false, shown(R.string.price_refresh_failed))
        assertEquals("The updating line shown", false, shown(R.string.prices_updating))
    }

    @Test
    fun atAStalePrice_whileAFailedRefreshIsRetried_saysSomeStale_updating_andFailed() {
        showNetWorth(state(Price(BigDecimal("140"), close, stale = true), refreshing = true, refreshFailed = true))
        // $9,600.00 cash and 8 × $140.00.
        val age = text(R.string.portfolio_prices_as_of_some_stale_spoken, "2026-10-29 16:00")
        val netWorth = text(R.string.net_worth_prices_spoken, text(R.string.net_worth_spoken, "10,720.00"), age)
        composeRule.onNode(hasContentDescription(netWorth)).assertExists()
        composeRule.onNode(hasContentDescription(row("Portfolio P", "1,120.00"))).assertExists()
        composeRule.onNode(hasContentDescription(row(text(R.string.net_worth_label), "10,720.00"))).assertExists()
        assertEquals("The updating line shown", true, shown(R.string.prices_updating))
        assertEquals("The refresh-failed line shown", true, shown(R.string.price_refresh_failed))
    }

    // Marker 6: no cash and no holding, net worth $0.00, one plain line and no composition.
    @Test
    fun atZero_showsOnePlainLine_andNoComposition() {
        showNetWorth(AppState(screen = Screen.NET_WORTH, tab = Tab.MORE, today = LocalDate.of(2026, 6, 1)))
        composeRule.onNode(hasContentDescription(text(R.string.net_worth_spoken, "0.00"))).assertExists()
        assertEquals("The $0.00 line shown", true, shown(R.string.net_worth_zero))
        assertEquals("The composition shown", false, shown(R.string.net_worth_composition_heading))
    }

    // Navigation outline: Overall's net worth is a button that opens the screen; its TalkBack sentence is unchanged.
    @Test
    fun overallsNetWorth_isAButtonThatOpensIt() {
        var opened = 0
        val state = state(Price(BigDecimal("160"), close)).copy(screen = Screen.TABS)
        composeRule.setContent { ATSAssetSystemTheme { OverallScreen(state, {}, {}, onOpenNetWorth = { opened++ }) } }
        val age = text(R.string.portfolio_prices_as_of_spoken, "2026-10-29 16:00")
        val netWorth = text(R.string.net_worth_prices_spoken, text(R.string.net_worth_spoken, "10,880.00"), age)
        composeRule.onNode(hasContentDescription(netWorth) and hasClickAction()).performClick()
        assertEquals("Opened from Overall", 1, opened)
    }

    // Decision 1: More's entry is a button that opens the screen.
    @Test
    fun moresEntry_isAButtonThatOpensIt() {
        var opened = 0
        composeRule.setContent { ATSAssetSystemTheme { MoreScreen(More.of(), onOpenNetWorth = { opened++ }) } }
        composeRule.onNode(hasContentDescription(text(R.string.net_worth_title)) and hasClickAction()).performClick()
        assertEquals("Opened from More", 1, opened)
    }
}
