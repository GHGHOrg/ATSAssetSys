package app.ghghorg.atsassets.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class CashAccountTest {
    private fun entry(
        date: String,
        cents: Long,
        seq: Long,
        type: CashType = CashType.DEPOSIT,
    ): CashEntry = CashEntry(
        id = "e$seq",
        date = LocalDate.parse(date),
        amountCents = cents,
        type = type,
        note = "",
        enteredSeq = seq,
    )

    // Example A as cash lines (criterion 49): opening, two buys, one sell.
    private val exampleA =
        listOf(
            entry("2026-01-01", 1_000_000L, 1L, CashType.OPENING_BALANCE),
            entry("2026-01-02", -100_000L, 2L, CashType.TRADE),
            entry("2026-02-01", -150_000L, 3L, CashType.TRADE),
            entry("2026-03-01", 210_000L, 4L, CashType.TRADE),
        )

    @Test
    fun balance_noEntries_isZero() {
        assertEquals(0L, CashAccount.balanceCents(emptyList()))
    }

    @Test
    fun balance_isTheBalanceOnTheLatestEntryDate() {
        assertEquals(960_000L, CashAccount.balanceCents(exampleA))
    }

    @Test
    fun balance_includesFutureDatedEntries_whileNetWorthDoesNot() {
        val entries = listOf(entry("2026-11-01", 1_000_000L, 1L, CashType.OPENING_BALANCE))
        assertEquals(1_000_000L, CashAccount.balanceCents(entries))
        assertEquals(0L, Valuation.netWorthCents(entries, LocalDate.of(2026, 10, 31)))
    }

    @Test
    fun rows_noEntries_isEmpty() {
        assertEquals(emptyList<CashRow>(), CashAccount.rows(emptyList()))
    }

    @Test
    fun rows_exampleA_newestFirstWithBalanceAfter() {
        val rows = CashAccount.rows(exampleA)
        assertEquals(listOf("e4", "e3", "e2", "e1"), rows.map { it.entry.id })
        assertEquals(listOf(960_000L, 750_000L, 900_000L, 1_000_000L), rows.map { it.balanceAfterCents })
    }

    @Test
    fun rows_sameDate_laterEntryFirst_balanceFollowsEntryOrder() {
        val entries =
            listOf(
                entry("2026-01-01", 1_000_000L, 1L, CashType.OPENING_BALANCE),
                entry("2026-01-05", 20_000L, 2L),
                entry("2026-01-05", -5_000L, 3L, CashType.WITHDRAWAL),
            )
        val rows = CashAccount.rows(entries)
        assertEquals(listOf("e3", "e2", "e1"), rows.map { it.entry.id })
        assertEquals(listOf(1_015_000L, 1_020_000L, 1_000_000L), rows.map { it.balanceAfterCents })
    }

    @Test
    fun rows_doNotDependOnListOrder() {
        assertEquals(CashAccount.rows(exampleA), CashAccount.rows(exampleA.reversed()))
    }

    @Test
    fun rows_topBalanceEqualsTheAccountBalance() {
        assertEquals(CashAccount.balanceCents(exampleA), CashAccount.rows(exampleA).first().balanceAfterCents)
    }

    private val overflowing =
        listOf(
            entry("2026-01-01", Long.MAX_VALUE, 1L, CashType.OPENING_BALANCE),
            entry("2026-01-02", 1L, 2L),
        )

    @Test
    fun balance_overflow_throwsInsteadOfWrapping() {
        assertThrows(ArithmeticException::class.java) { CashAccount.balanceCents(overflowing) }
    }

    @Test
    fun rows_overflow_throwsInsteadOfWrapping() {
        assertThrows(ArithmeticException::class.java) { CashAccount.rows(overflowing) }
    }

    // Iteration 06: filters (view 7). One entry of each direct type, Jan 1 to Jan 10.
    private val mixed =
        listOf(
            entry("2026-01-01", 100_000L, 1L, CashType.OPENING_BALANCE),
            entry("2026-01-05", 50_000L, 2L, CashType.DEPOSIT),
            entry("2026-01-06", -30_000L, 3L, CashType.WITHDRAWAL),
            entry("2026-01-07", 1_234L, 4L, CashType.INTEREST_DIVIDEND),
            entry("2026-01-08", -500L, 5L, CashType.ADJUSTMENT),
            entry("2026-01-10", 5_000L, 6L, CashType.DEPOSIT),
        )

    private fun ids(filter: CashFilter): List<String> = CashAccount.rows(mixed, filter).map { it.entry.id }

    private fun day(text: String): LocalDate = LocalDate.parse(text)

    @Test
    fun filter_none_givesEveryRow_asRowsDoes() {
        assertEquals(CashAccount.rows(mixed), CashAccount.rows(mixed, CashFilter()))
        assertFalse(CashFilter().isActive)
    }

    @Test
    fun filter_fromOnly_includesTheFromDate() {
        assertEquals(listOf("e6", "e5", "e4", "e3"), ids(CashFilter(from = day("2026-01-06"))))
    }

    @Test
    fun filter_toOnly_includesTheToDate() {
        assertEquals(listOf("e3", "e2", "e1"), ids(CashFilter(to = day("2026-01-06"))))
    }

    @Test
    fun filter_fromAndTo_bothEndsIncluded() {
        assertEquals(listOf("e4", "e3", "e2"), ids(CashFilter(from = day("2026-01-05"), to = day("2026-01-07"))))
    }

    @Test
    fun filter_byEachType() {
        assertEquals(listOf("e6", "e2"), ids(CashFilter(type = CashType.DEPOSIT)))
        assertEquals(listOf("e3"), ids(CashFilter(type = CashType.WITHDRAWAL)))
        assertEquals(listOf("e4"), ids(CashFilter(type = CashType.INTEREST_DIVIDEND)))
        assertEquals(listOf("e5"), ids(CashFilter(type = CashType.ADJUSTMENT)))
    }

    @Test
    fun filter_theOpeningBalanceShowsOnlyUnderAll() {
        assertTrue("e1" in ids(CashFilter()))
        for (type in listOf(CashType.DEPOSIT, CashType.WITHDRAWAL, CashType.INTEREST_DIVIDEND, CashType.ADJUSTMENT)) {
            assertFalse("$type", "e1" in ids(CashFilter(type = type)))
        }
    }

    @Test
    fun filter_typeAndDatesCombine() {
        assertEquals(listOf("e6"), ids(CashFilter(from = day("2026-01-06"), type = CashType.DEPOSIT)))
    }

    @Test
    fun filter_fromAfterTo_givesNoRows() {
        assertTrue(ids(CashFilter(from = day("2026-01-08"), to = day("2026-01-05"))).isEmpty())
    }

    @Test
    fun filter_keepsTheRealBalanceAfterEachEntry() {
        val rows = CashAccount.rows(mixed, CashFilter(type = CashType.DEPOSIT))
        assertEquals(listOf(125_734L, 150_000L), rows.map { it.balanceAfterCents })
    }

    @Test
    fun filter_isActive_withAnyBoundOrAType() {
        assertTrue(CashFilter(from = day("2026-01-01")).isActive)
        assertTrue(CashFilter(to = day("2026-01-01")).isActive)
        assertTrue(CashFilter(type = CashType.DEPOSIT).isActive)
    }

    // Iteration 28: the ticker and the Buy and Sell chips (view 7, wireframe 07 frames B and D; AC-49).

    private fun transaction(
        id: String,
        type: TransactionType,
        date: String,
        seq: Long,
        totalCents: Long,
        ticker: String = "AAPL",
        reversesId: String? = null,
    ) = Transaction(
        id = id,
        portfolioId = "main",
        ticker = ticker,
        type = type,
        date = day(date),
        enteredSeq = seq,
        quantity = BigDecimal.ONE,
        totalCents = totalCents,
        reversesId = reversesId,
    )

    // A trade's cash line: a buy's total out, a sell's in, a reversal's the original's back (4.6).
    private fun lineOf(
        trade: Transaction,
        cents: Long,
    ) = CashEntry("c-${trade.id}", trade.date, cents, CashType.TRADE, "", trade.enteredSeq, transactionId = trade.id)

    // spec/example-backup.csv, in file order: Main's buys, sells, the split and the reversal of L3, and the cash rows.
    private val l1 = transaction("L1", TransactionType.BUY, "2026-01-02", 2L, 100_000L)
    private val l2 = transaction("L2", TransactionType.BUY, "2026-02-01", 3L, 150_000L)
    private val o1 = transaction("O1", TransactionType.BUY, "2026-02-10", 4L, 20_000L, ticker = "OLDCO")
    private val s1 = transaction("S1", TransactionType.SELL, "2026-03-01", 5L, 210_000L)
    private val s2 = transaction("S2", TransactionType.SELL, "2026-03-10", 6L, 0L, ticker = "OLDCO")
    private val l3 = transaction("L3", TransactionType.BUY, "2026-03-20", 7L, 36_000L)
    private val sp = transaction("SP", TransactionType.SPLIT, "2026-04-01", 8L, 0L)
    private val rv = transaction("RV", TransactionType.REVERSAL, "2026-04-05", 9L, 0L, reversesId = "L3")
    private val sampleTransactions = listOf(l1, l2, o1, s1, s2, l3, sp, rv)
    private val sampleEntries =
        listOf(
            entry("2026-01-01", 1_000_000L, 1L, CashType.OPENING_BALANCE),
            lineOf(l1, -100_000L),
            lineOf(l2, -150_000L),
            lineOf(o1, -20_000L),
            lineOf(s1, 210_000L),
            lineOf(s2, 0L),
            lineOf(l3, -36_000L),
            lineOf(rv, 36_000L),
            entry("2026-04-15", 2_500L, 10L, CashType.INTEREST_DIVIDEND),
            entry("2026-04-20", 50_000L, 11L, CashType.DEPOSIT),
            entry("2026-04-21", -50_000L, 12L, CashType.DEPOSIT),
        )

    // Example A (AC-49): the opening balance and Portfolio P's two buys and one sell, each line with its trade.
    private val exampleATransactions = listOf(l1, l2, s1)
    private val exampleAEntries =
        listOf(
            entry("2026-01-01", 1_000_000L, 1L, CashType.OPENING_BALANCE),
            lineOf(l1, -100_000L),
            lineOf(l2, -150_000L),
            lineOf(s1, 210_000L),
        )

    private fun shown(
        filter: CashFilter,
        entries: List<CashEntry> = sampleEntries,
        transactions: List<Transaction> = sampleTransactions,
    ): List<Pair<String, Long>> = CashAccount.rows(entries, transactions, filter).map { it.entry.id to it.balanceAfterCents }

    // AC-49: ticker AAPL with All shows Example A's three trade lines, with their real balances, and no opening balance.
    @Test
    fun ticker_withAll_exampleA_theThreeTradeLines_noOpeningBalance() {
        assertEquals(
            listOf("c-S1" to 960_000L, "c-L2" to 750_000L, "c-L1" to 900_000L),
            shown(CashFilter(ticker = "AAPL"), exampleAEntries, exampleATransactions),
        )
    }

    // AC-49: ticker AAPL with Sell shows only the Mar 1 sell, still with balance $9,600.00.
    @Test
    fun ticker_withSell_exampleA_onlyTheMar1Sell() {
        assertEquals(
            listOf("c-S1" to 960_000L),
            shown(CashFilter(ticker = "AAPL", trade = TradeSide.SELL), exampleAEntries, exampleATransactions),
        )
    }

    // AC-49: ticker AAPL with Apr 1 to Apr 30 on Example A matches nothing.
    @Test
    fun ticker_withARangeOfNoTrade_exampleA_matchesNothing() {
        val filter = CashFilter(from = day("2026-04-01"), to = day("2026-04-30"), ticker = "AAPL")
        assertTrue(shown(filter, exampleAEntries, exampleATransactions).isEmpty())
    }

    // AC-49 and wireframe 07 frame D: AAPL and Buy on the sample file: the reversal of L3 first, then the three buys.
    @Test
    fun ticker_withBuy_sampleFile_theReversalUnderBuy_withTheRealBalances() {
        assertEquals(
            listOf("c-RV" to 940_000L, "c-L3" to 904_000L, "c-L2" to 750_000L, "c-L1" to 900_000L),
            shown(CashFilter(ticker = "AAPL", trade = TradeSide.BUY)),
        )
    }

    // AC-49: the reversal of a buy is not under Sell.
    @Test
    fun theReversalOfABuy_isNotUnderSell() {
        assertEquals(listOf("c-S1" to 940_000L), shown(CashFilter(ticker = "AAPL", trade = TradeSide.SELL)))
        assertFalse("c-RV" in shown(CashFilter(trade = TradeSide.SELL)).map { it.first })
    }

    // Buy and Sell without a ticker: every portfolio's ticker; a reversal of a sell sits under Sell.
    @Test
    fun buyAndSell_withoutATicker_takeEveryTicker_andAReversalOfASellUnderSell() {
        val rs = transaction("RS", TransactionType.REVERSAL, "2026-03-12", 13L, 0L, ticker = "OLDCO", reversesId = "S2")
        val transactions = sampleTransactions + rs
        val entries = sampleEntries + lineOf(rs, 0L)
        assertEquals(
            listOf("c-RV", "c-L3", "c-O1", "c-L2", "c-L1"),
            shown(CashFilter(trade = TradeSide.BUY), entries, transactions).map { it.first },
        )
        assertEquals(
            listOf("c-RS", "c-S2", "c-S1"),
            shown(CashFilter(trade = TradeSide.SELL), entries, transactions).map { it.first },
        )
    }

    // A ticker never takes a direct entry, the opening balance or a trade line whose trade is not there.
    @Test
    fun aTicker_neverTakesADirectEntry_orALineWithoutItsTrade() {
        val orphan = CashEntry("orphan", day("2026-03-02"), -1_000L, CashType.TRADE, "", 14L, transactionId = "gone")
        val ids = shown(CashFilter(ticker = "AAPL"), sampleEntries + orphan).map { it.first }
        assertEquals(listOf("c-RV", "c-L3", "c-S1", "c-L2", "c-L1"), ids)
    }

    // A reversal of a split has no cash line; a reversal whose original is not there shows under All only.
    @Test
    fun aReversalWithoutItsOriginal_showsUnderAllOnly() {
        val lost = transaction("RX", TransactionType.REVERSAL, "2026-04-06", 15L, 0L, reversesId = "missing")
        val transactions = sampleTransactions + lost
        val entries = sampleEntries + lineOf(lost, 500L)
        assertTrue("c-RX" in shown(CashFilter(), entries, transactions).map { it.first })
        assertTrue("c-RX" in shown(CashFilter(ticker = "AAPL"), entries, transactions).map { it.first })
        assertFalse("c-RX" in shown(CashFilter(trade = TradeSide.BUY), entries, transactions).map { it.first })
        assertFalse("c-RX" in shown(CashFilter(trade = TradeSide.SELL), entries, transactions).map { it.first })
    }

    // The direct chips with the transactions: the same rows as before 28.
    @Test
    fun theDirectChips_withTheTransactions_giveTheSameRowsAsBefore() {
        for (type in listOf(null, CashType.DEPOSIT, CashType.INTEREST_DIVIDEND, CashType.OPENING_BALANCE)) {
            val filter = CashFilter(from = day("2026-01-02"), type = type)
            assertEquals("$type", CashAccount.rows(sampleEntries, filter), CashAccount.rows(sampleEntries, sampleTransactions, filter))
        }
    }

    // Without the transactions a ticker or a trade side cannot be judged, so nothing matches.
    @Test
    fun withoutTheTransactions_aTickerOrATradeSide_matchesNothing() {
        assertTrue(CashAccount.rows(sampleEntries, CashFilter(ticker = "AAPL")).isEmpty())
        assertTrue(CashAccount.rows(sampleEntries, CashFilter(trade = TradeSide.BUY)).isEmpty())
    }

    @Test
    fun filter_isActive_withATickerOrATradeSide() {
        assertTrue(CashFilter(ticker = "AAPL").isActive)
        assertTrue(CashFilter(trade = TradeSide.SELL).isActive)
    }

    // The ticker picker: each ticker with a trade line, once, in alphabetical order; none without lines.
    @Test
    fun tickers_eachTickerWithATradeLine_onceAlphabetically() {
        val msft = transaction("M1", TransactionType.BUY, "2026-05-01", 16L, 1_000L, ticker = "MSFT")
        assertEquals(listOf("AAPL", "OLDCO"), CashAccount.tickers(sampleEntries, sampleTransactions + msft))
        assertEquals(listOf("AAPL", "MSFT", "OLDCO"), CashAccount.tickers(sampleEntries + lineOf(msft, -1_000L), sampleTransactions + msft))
        assertTrue(CashAccount.tickers(sampleEntries.take(1), sampleTransactions).isEmpty())
    }
}
