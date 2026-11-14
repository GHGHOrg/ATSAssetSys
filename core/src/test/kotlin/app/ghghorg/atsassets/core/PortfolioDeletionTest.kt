package app.ghghorg.atsassets.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

// 29 step 2: K1's portfolio deletion (spec 4.7, wireframe 10d; AC-28, AC-55, Examples A and E).
class PortfolioDeletionTest {
    private val p = Portfolio("p", "Portfolio P", 1L)
    private val q = Portfolio("q", "Portfolio Q", 2L)
    private val r = Portfolio("r", "Retirement", 3L)

    private fun day(text: String): LocalDate = LocalDate.parse(text)

    private fun buy(
        id: String,
        portfolio: Portfolio,
        date: String,
        seq: Long,
        quantity: String,
        cents: Long,
    ) = Transaction(id, portfolio.id, "AAPL", TransactionType.BUY, day(date), seq, BigDecimal(quantity), cents)

    private fun sell(
        id: String,
        portfolio: Portfolio,
        date: String,
        seq: Long,
        cents: Long,
        vararg picks: Pair<String, String>,
    ) = Transaction(
        id = id,
        portfolioId = portfolio.id,
        ticker = "AAPL",
        type = TransactionType.SELL,
        date = day(date),
        enteredSeq = seq,
        quantity = picks.fold(BigDecimal.ZERO) { sum, pick -> sum + BigDecimal(pick.second) },
        totalCents = cents,
        allocations = picks.map { SellAllocation(it.first, Fraction.of(BigDecimal(it.second))) },
    )

    private fun reversal(
        id: String,
        of: Transaction,
        date: String,
        seq: Long,
    ) = Transaction(id, of.portfolioId, of.ticker, TransactionType.REVERSAL, day(date), seq, BigDecimal.ZERO, 0L, reversesId = of.id)

    // A trade's line: a buy's total out, a sell's in; a reversal's gives back the original's.
    private fun lineOf(
        trade: Transaction,
        original: Transaction? = null,
    ): CashEntry {
        val source = original ?: trade
        val cents = if (source.type == TransactionType.BUY) -source.totalCents else source.totalCents
        return CashEntry("c-${trade.id}", trade.date, if (original == null) cents else -cents, CashType.TRADE, "", trade.enteredSeq, transactionId = trade.id)
    }

    private fun cash(
        id: String,
        date: String,
        cents: Long,
        seq: Long,
        type: CashType = if (cents < 0) CashType.WITHDRAWAL else CashType.DEPOSIT,
        reversesId: String? = null,
    ) = CashEntry(id, day(date), cents, type, if (type == CashType.ADJUSTMENT) "fix" else "", seq, reversesId = reversesId)

    // Example A opens with $10,000.00, Example E and AC-55's last case with $1,000.00.
    private val openingA = cash("open", "2026-01-01", 1_000_000L, 1L, CashType.OPENING_BALANCE)
    private val openingE = cash("open", "2026-01-01", 100_000L, 1L, CashType.OPENING_BALANCE)

    private fun check(
        transactions: List<Transaction>,
        entries: List<CashEntry>,
        portfolioId: String = "q",
        hidden: List<HiddenHolding> = emptyList(),
        overrides: List<ClassOverride> = emptyList(),
    ): DeletionCheck = PortfolioDeletion.check(listOf(p, q, r), transactions, entries, hidden, overrides, portfolioId)

    private fun allowed(result: DeletionCheck): DeletionCheck.Allowed {
        assertTrue("expected Allowed, was $result", result is DeletionCheck.Allowed)
        return result as DeletionCheck.Allowed
    }

    // Example A in Portfolio P (AC-55): 3 transactions, 3 trade cash lines, +$800.00, $9,600.00 becomes $10,000.00.
    @Test
    fun exampleA_summary() {
        val b1 = buy("b1", p, "2026-01-02", 2L, "10", 100_000L)
        val b2 = buy("b2", p, "2026-02-01", 3L, "10", 150_000L)
        val s1 = sell("s1", p, "2026-03-01", 4L, 210_000L, "b1" to "10", "b2" to "2")
        val trades = listOf(b1, b2, s1)
        val result = allowed(check(trades, listOf(openingA) + trades.map { lineOf(it) }, "p"))
        val summary = result.summary
        assertEquals(p, summary.portfolio)
        assertEquals(3, summary.transactionCount)
        assertEquals(3, summary.tradeLineCount)
        assertEquals(Fraction.ofCents(80_000L), summary.realizedGain)
        assertEquals(960_000L, summary.balanceNowCents)
        assertEquals(1_000_000L, summary.balanceAfterCents)
        assertEquals(setOf("b1", "b2", "s1"), result.removal.transactionIds)
        assertEquals(setOf("c-b1", "c-b2", "c-s1"), result.removal.cashEntryIds)
    }

    // Example E (AC-28, AC-55): Portfolio Q bought for $400 on Jan 5 and sold for $900 on Feb 1; $1,200 withdrawn Feb 10.
    private val qBuy = buy("qb", q, "2026-01-05", 2L, "4", 40_000L)
    private val qSell = sell("qs", q, "2026-02-01", 3L, 90_000L, "qb" to "4")
    private val exampleE = listOf(qBuy, qSell)
    private val exampleELines = listOf(openingE, lineOf(qBuy), lineOf(qSell), cash("w", "2026-02-10", -120_000L, 4L))

    @Test
    fun exampleE_isBlocked_onFeb10_by200() {
        assertEquals(DeletionCheck.Blocked(day("2026-02-10"), 20_000L), check(exampleE, exampleELines))
    }

    // A deposit adjustment of $200.00 dated Feb 9 lets it through; one dated Feb 10 comes after the withdrawal (4.1).
    @Test
    fun exampleE_anAdjustmentBeforeFeb10_letsItThrough_butNotOnFeb10() {
        val feb9 = cash("a", "2026-02-09", 20_000L, 5L, CashType.ADJUSTMENT)
        val through = allowed(check(exampleE, exampleELines + feb9))
        assertEquals(50_000L, through.summary.balanceNowCents)
        assertEquals(0L, through.summary.balanceAfterCents)
        val feb10 = cash("a", "2026-02-10", 20_000L, 5L, CashType.ADJUSTMENT)
        assertEquals(DeletionCheck.Blocked(day("2026-02-10"), 20_000L), check(exampleE, exampleELines + feb10))
    }

    // AC-55: opening $1,000, a buy of $500 and a sell of $900 in Q, a withdrawal of $1,300, all Jan 1: cannot be
    // deleted, and a deposit on Jan 1 changes nothing.
    @Test
    fun negativeOnTheOpeningBalancesDate_cannotBeDeleted() {
        val b = buy("qb", q, "2026-01-01", 2L, "5", 50_000L)
        val s = sell("qs", q, "2026-01-01", 3L, 90_000L, "qb" to "5")
        val entries = listOf(openingE, lineOf(b), lineOf(s), cash("w", "2026-01-01", -130_000L, 4L))
        assertEquals(DeletionCheck.CannotDelete(day("2026-01-01"), 30_000L), check(listOf(b, s), entries))
        val sameDay = cash("d", "2026-01-01", 50_000L, 5L)
        assertEquals(DeletionCheck.CannotDelete(day("2026-01-01"), 30_000L), check(listOf(b, s), entries + sameDay))
    }

    // Two negative dates: the first date is the first, the shortfall the largest (4.7): -$100.00 on Mar 1, -$250.00 on
    // Apr 1.
    @Test
    fun twoNegativeDates_theFirstDate_andTheLargestShortfall() {
        val b = buy("qb", q, "2026-01-02", 2L, "1", 10_000L)
        val s = sell("qs", q, "2026-02-01", 3L, 60_000L, "qb" to "1")
        val entries =
            listOf(openingE, lineOf(b), lineOf(s), cash("w1", "2026-03-01", -110_000L, 4L), cash("w2", "2026-04-01", -15_000L, 5L))
        assertEquals(DeletionCheck.Blocked(day("2026-03-01"), 25_000L), check(listOf(b, s), entries))
    }

    // A reversed sell's pair lowers the balance between its dates when removed: a withdrawal there blocks.
    @Test
    fun aReversedSell_withAWithdrawalInItsWindow_blocks() {
        val rv = reversal("qr", qSell, "2026-03-01", 6L)
        val entries =
            listOf(
                openingE,
                lineOf(qBuy),
                lineOf(qSell),
                cash("w", "2026-02-10", -120_000L, 4L),
                cash("d", "2026-02-20", 100_000L, 5L),
                lineOf(rv, qSell),
            )
        assertEquals(DeletionCheck.Blocked(day("2026-02-10"), 20_000L), check(exampleE + rv, entries))
    }

    // A reversal is the portfolio's: counted and removed with its line; a reversed buy alone never blocks.
    @Test
    fun aReversedBuy_isCountedAndRemoved_andDoesNotBlock() {
        val rv = reversal("qr", qBuy, "2026-02-01", 3L)
        val entries = listOf(openingE, lineOf(qBuy), lineOf(rv, qBuy), cash("w", "2026-01-20", -60_000L, 4L))
        val result = allowed(check(listOf(qBuy, rv), entries))
        assertEquals(2, result.summary.transactionCount)
        assertEquals(2, result.summary.tradeLineCount)
        assertEquals(setOf("qb", "qr"), result.removal.transactionIds)
        assertEquals(setOf("c-qb", "c-qr"), result.removal.cashEntryIds)
        assertEquals(Fraction.ZERO, result.summary.realizedGain)
        assertEquals(40_000L, result.summary.balanceNowCents)
        assertEquals(40_000L, result.summary.balanceAfterCents)
    }

    // A reversed sale is not a realized gain (view 5): a portfolio whose only sale was reversed shows $0.00.
    @Test
    fun aReversedSale_isNotARealizedGain() {
        val rv = reversal("qr", qSell, "2026-02-05", 4L)
        val entries = listOf(openingE, lineOf(qBuy), lineOf(qSell), lineOf(rv, qSell))
        val result = allowed(check(exampleE + rv, entries))
        assertEquals(Fraction.ZERO, result.summary.realizedGain)
        assertEquals(3, result.summary.transactionCount)
        assertEquals(3, result.summary.tradeLineCount)
    }

    // A reversed direct cash entry is not the portfolio's: both stay in the replay and are not removed. Now $600.00;
    // after, $100.00 (with the deposit and its reversal still there).
    @Test
    fun aReversedDirectCashEntry_staysAndIsReplayed() {
        val d1 = cash("d1", "2026-01-03", 50_000L, 2L)
        val undo = cash("u1", "2026-01-04", -50_000L, 3L, reversesId = "d1")
        val b = buy("qb", q, "2026-01-05", 4L, "4", 40_000L)
        val s = sell("qs", q, "2026-02-01", 5L, 90_000L, "qb" to "4")
        val entries = listOf(openingE, d1, undo, lineOf(b), lineOf(s), cash("w", "2026-02-10", -90_000L, 6L))
        val result = allowed(check(listOf(b, s), entries))
        assertEquals(setOf("c-qb", "c-qs"), result.removal.cashEntryIds)
        assertEquals(60_000L, result.summary.balanceNowCents)
        assertEquals(10_000L, result.summary.balanceAfterCents)
    }

    // A portfolio with no transactions: allowed, three zeros, the balance unchanged (AC-55: the name is still typed).
    @Test
    fun aPortfolioWithNoTransactions_isAllowed_withZeros() {
        val result = allowed(check(emptyList(), listOf(openingA), "r"))
        assertEquals(0, result.summary.transactionCount)
        assertEquals(0, result.summary.tradeLineCount)
        assertEquals(Fraction.ZERO, result.summary.realizedGain)
        assertEquals(1_000_000L, result.summary.balanceNowCents)
        assertEquals(1_000_000L, result.summary.balanceAfterCents)
    }

    // 4.8: the summary's balances are on the latest entry date, so an entry dated after today counts.
    @Test
    fun theBalances_includeEntriesDatedLater() {
        val future = cash("f", "2099-01-01", 5_000L, 9L)
        val result = allowed(check(exampleE, listOf(openingE, lineOf(qBuy), lineOf(qSell), future)))
        assertEquals(155_000L, result.summary.balanceNowCents)
        assertEquals(105_000L, result.summary.balanceAfterCents)
    }

    // Another portfolio's records, hidden rows and overrides, even for the same ticker, are never removed (decision 1).
    @Test
    fun anotherPortfolio_isNeverTouched() {
        val other = buy("rb", r, "2026-01-06", 5L, "1", 1_000L)
        val hidden = listOf(HiddenHolding("q", "AAPL"), HiddenHolding("r", "AAPL"))
        val overrides =
            listOf(ClassOverride("q", "AAPL", ClassList.SECTOR, "Technology"), ClassOverride("r", "AAPL", ClassList.SECTOR, "Technology"))
        val entries = listOf(openingE, lineOf(qBuy), lineOf(qSell), lineOf(other))
        val result = allowed(check(exampleE + other, entries, hidden = hidden, overrides = overrides))
        assertEquals("q", result.removal.portfolioId)
        assertEquals(setOf("qb", "qs"), result.removal.transactionIds)
        assertEquals(setOf("c-qb", "c-qs"), result.removal.cashEntryIds)
        assertEquals(listOf(HiddenHolding("q", "AAPL")), result.removal.hidden)
        assertEquals(listOf(ClassOverride("q", "AAPL", ClassList.SECTOR, "Technology")), result.removal.overrides)
    }

    @Test
    fun anUnknownPortfolio_isNotFound() {
        assertEquals(DeletionCheck.NotFound, check(exampleE, exampleELines, "nope"))
    }

    // AC-55: the typed name matches ignoring case and spaces (D7), not otherwise.
    @Test
    fun theTypedName_matchesIgnoringCaseAndSpaces() {
        assertTrue(PortfolioDeletion.nameMatches(p, "Portfolio P"))
        assertTrue(PortfolioDeletion.nameMatches(p, "  portfolio   p "))
        assertFalse(PortfolioDeletion.nameMatches(p, "Portfolio"))
        assertFalse(PortfolioDeletion.nameMatches(p, ""))
        assertFalse(PortfolioDeletion.nameMatches(p, "PortfolioP"))
    }
}
