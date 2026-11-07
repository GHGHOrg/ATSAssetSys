package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.HiddenHolding
import app.ghghorg.atsassets.core.Holdings
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.SellAllocation
import app.ghghorg.atsassets.core.SplitRatio
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import app.ghghorg.atsassets.core.Valuation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

/** Each kept field equals a fresh calculation from the state's own inputs (15c, section 9 part 1 layer 3). */
internal fun assertKeptFieldsFresh(
    state: AppState,
    case: String = "",
) = with(state) {
    assertEquals("$case: holdings", Holdings.holdings(transactions, today), holdings)
    assertEquals("$case: net worth", Valuation.netWorth(entries, transactions, today), netWorth)
    assertEquals("$case: holding lots", KeptFields.holdingLots(transactions, portfolioId, holdingTicker, today), holdingLots)
    assertEquals("$case: hide offered", KeptFields.hideOffered(transactions, hidden, portfolioId, holdingTicker), hideOffered)
    assertEquals(
        "$case: sell tickers",
        KeptFields.sellTickers(
            entries,
            transactions,
            Holdings.holdings(transactions, today),
            hidden,
            sellForm.portfolioId,
            replacingId,
            replaceReversalDate,
            today,
        ),
        sellTickers,
    )
    assertEquals(
        "$case: sell lot choices",
        KeptFields.sellLotChoices(
            entries,
            transactions,
            sellForm.portfolioId,
            sellForm.ticker,
            sellForm.dateText,
            replacingId,
            replaceReversalDate,
            today,
        ),
        sellLotChoices,
    )
    assertEquals("$case: transaction lot", KeptFields.transactionLot(transactions, transactionId, today), transactionLot)
    assertEquals("$case: cash list", CashList.of(entries, transactions, portfolios, cashFilterForm), cashList)
}

// 15c: the kept fields of AppState follow their inputs, and a field whose inputs did not change stays the same object.
class KeptFieldsTest {
    private fun bought(
        id: String,
        ticker: String,
        date: String,
        quantity: String,
        totalCents: Long,
        seq: Long,
    ): Transaction = Transaction(
        id = id,
        portfolioId = "p1",
        ticker = ticker,
        type = TransactionType.BUY,
        date = LocalDate.parse(date),
        enteredSeq = seq,
        quantity = BigDecimal(quantity),
        totalCents = totalCents,
    )

    private fun sold(
        id: String,
        ticker: String,
        date: String,
        quantity: String,
        totalCents: Long,
        seq: Long,
        lotId: String,
    ): Transaction = Transaction(
        id = id,
        portfolioId = "p1",
        ticker = ticker,
        type = TransactionType.SELL,
        date = LocalDate.parse(date),
        enteredSeq = seq,
        quantity = BigDecimal(quantity),
        totalCents = totalCents,
        allocations = listOf(SellAllocation(lotId, Fraction.of(BigDecimal(quantity)))),
    )

    private fun lineOf(transaction: Transaction): CashEntry = CashEntry(
        "c-${transaction.id}",
        transaction.date,
        if (transaction.type == TransactionType.BUY) -transaction.totalCents else transaction.totalCents,
        CashType.TRADE,
        "",
        transaction.enteredSeq,
        transactionId = transaction.id,
    )

    // AAPL: b1 sold by s1, b2 held, a 2 for 1 split on Apr 1 (x1). KO sold out. MSFT held.
    private val b1 = bought("b1", "AAPL", "2026-01-02", "10", 100_000L, 2L)
    private val s1 = sold("s1", "AAPL", "2026-01-03", "10", 120_000L, 3L, "b1")
    private val b2 = bought("b2", "AAPL", "2026-02-01", "10", 150_000L, 4L)
    private val x1 =
        Transaction(
            id = "x1",
            portfolioId = "p1",
            ticker = "AAPL",
            type = TransactionType.SPLIT,
            date = LocalDate.of(2026, 4, 1),
            enteredSeq = 5L,
            quantity = BigDecimal.ZERO,
            totalCents = 0L,
            split = SplitRatio(2, 1),
        )
    private val k1 = bought("k1", "KO", "2026-01-04", "5", 5_000L, 6L)
    private val k2 = sold("k2", "KO", "2026-02-10", "5", 5_000L, 7L, "k1")
    private val m1 = bought("m1", "MSFT", "2026-01-05", "2", 2_000L, 8L)
    private val transactions = listOf(b1, s1, b2, x1, k1, k2, m1)

    private val base =
        AppState(
            screen = Screen.HOLDING,
            entries = listOf(opening()) + transactions.filter { it.type != TransactionType.SPLIT }.map { lineOf(it) },
            transactions = transactions,
            portfolios = listOf(Portfolio("p1", "Main", 1L), Portfolio("p2", "Second", 2L)),
            today = LocalDate.of(2026, 10, 26),
            portfolioId = "p1",
            holdingTicker = "AAPL",
            transactionId = "b2",
            sellForm = SellForm(portfolioId = "p1", ticker = "AAPL", dateText = "20261026"),
        )

    // One input changed: [field] is one the change must move, so the case cannot pass by staying as it was.
    private class Case(
        val input: String,
        val start: AppState,
        val change: (AppState) -> AppState,
        val field: (AppState) -> Any?,
    )

    private val cases =
        listOf(
            Case("entries", base, { it.copy(entries = it.entries + deposit()) }, { it.netWorth }),
            Case(
                "transactions",
                base,
                { it.copy(transactions = it.transactions + bought("b4", "AAPL", "2026-03-02", "5", 50_000L, 9L)) },
                { it.holdingLots },
            ),
            Case(
                "hidden",
                base.copy(holdingTicker = "KO").refreshed(),
                { it.copy(hidden = listOf(HiddenHolding("p1", "KO"))) },
                { it.hideOffered },
            ),
            Case("today", base, { it.copy(today = LocalDate.of(2026, 1, 15)) }, { it.transactionLot }),
            Case("portfolioId", base, { it.copy(portfolioId = "p2") }, { it.holdingLots }),
            Case("holdingTicker", base, { it.copy(holdingTicker = "MSFT") }, { it.holdingLots }),
            Case("transactionId", base, { it.copy(transactionId = "m1") }, { it.transactionLot }),
            Case("sell portfolio", base, { it.copy(sellForm = it.sellForm.copy(portfolioId = "p2", ticker = null)) }, { it.sellTickers }),
            Case("sell ticker", base, { it.copy(sellForm = it.sellForm.copy(ticker = "MSFT")) }, { it.sellLotChoices }),
            Case("sell date", base, { it.copy(sellForm = it.sellForm.copy(dateText = "20260115")) }, { it.sellLotChoices }),
            Case("replacingId", base, { it.copy(replacingId = "b2") }, { it.sellTickers }),
            Case(
                "replaceReversalDate",
                base.copy(replacingId = "s1", replaceReversalDate = "20260103").refreshed(),
                { it.copy(replaceReversalDate = "20261027") },
                { it.sellLotChoices },
            ),
            // 15d: the Cash list's own inputs.
            Case(
                "portfolios",
                base,
                { it.copy(portfolios = listOf(Portfolio("p1", "Renamed", 1L), Portfolio("p2", "Second", 2L))) },
                { it.cashList },
            ),
            Case("cash filter", base, { it.copy(cashFilterForm = CashFilterForm(type = CashType.DEPOSIT)) }, { it.cashList }),
        )

    private fun deposit(): CashEntry = CashEntry("d1", LocalDate.of(2026, 3, 1), 50_000L, CashType.DEPOSIT, "", 10L)

    @Test
    fun aStateBuiltDirectly_isAlreadyFresh_andRefreshingItChangesNothing() {
        assertKeptFieldsFresh(base, "built")
        assertSame(base, base.refreshed())
    }

    @Test
    fun eachInputChangedInTurn_givesFieldsEqualToAFreshCalculation() {
        for (case in cases) {
            assertKeptFieldsFresh(case.start, "${case.input}, before")
            val next = case.change(case.start).refreshed()
            assertNotEquals("${case.input}: the change must move its field", case.field(case.start), case.field(next))
            assertKeptFieldsFresh(next, case.input)
        }
    }

    @Test
    fun refreshingTwice_changesNothing() {
        for (case in cases) {
            val next = case.change(case.start).refreshed()
            assertSame(case.input, next, next.refreshed())
        }
    }

    @Test
    fun typingInTheSellForm_leavesEveryKeptFieldTheSameObject() {
        val typed =
            base.copy(sellForm = base.sellForm.copy(quantityText = "5", totalText = "10.00", picks = mapOf("b2" to "5"))).refreshed()
        assertSame(base.keptFrom, typed.keptFrom)
        assertSame(base.sellLotChoices, typed.sellLotChoices)
        assertSame(base.sellTickers, typed.sellTickers)
        assertSame(base.holdingLots, typed.holdingLots)
        assertSame(base.holdings, typed.holdings)
        assertSame(base.cashList, typed.cashList)
    }

    @Test
    fun anotherShownHolding_givesNewLots_andLeavesTheSellListsAndHoldingsTheSameObjects() {
        val next = base.copy(holdingTicker = "MSFT").refreshed()
        assertNotSame(base.holdingLots, next.holdingLots)
        assertSame(base.sellLotChoices, next.sellLotChoices)
        assertSame(base.sellTickers, next.sellTickers)
        assertSame(base.holdings, next.holdings)
        assertSame(base.netWorth, next.netWorth)
        assertSame(base.transactionLot, next.transactionLot)
        assertSame(base.cashList, next.cashList)
    }

    @Test
    fun aNewSellDate_givesANewLotList_andLeavesTheHoldingLotsTheSameObject() {
        val next = base.copy(sellForm = base.sellForm.copy(dateText = "20260115")).refreshed()
        assertNotSame(base.sellLotChoices, next.sellLotChoices)
        assertSame(base.holdingLots, next.holdingLots)
        assertSame(base.sellTickers, next.sellTickers)
    }
}
