package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.HistoryRow
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.SellAllocation
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

// 29 step 8 (decision 4): a filter's To before its From shows its line and counts as no bound, on Cash, Transaction
// history and Realized gains (the date-range defect of 06, 26 and 27; the 29 file, 13.3).
class DateRangeOrderTest {
    private val march = "20260301"
    private val february = "20260201"

    // The shared rule.
    @Test
    fun toBeforeFrom_isRefused_andCountsAsNoBound() {
        assertEquals(DateRefusal.BEFORE_FROM, DateDigits.toRefusal(march, february))
        assertNull(DateDigits.toBound(march, february))
    }

    @Test
    fun toOnOrAfterFrom_isABound_withNoLine() {
        assertNull(DateDigits.toRefusal(february, march))
        assertEquals(LocalDate.of(2026, 3, 1), DateDigits.toBound(february, march))
        assertNull(DateDigits.toRefusal(march, march))
        assertEquals(LocalDate.of(2026, 3, 1), DateDigits.toBound(march, march))
    }

    // To's own refusal comes first; a From that is not a whole date judges nothing.
    @Test
    fun toItsOwnRefusalFirst_andNoComparisonWithoutAWholeFrom() {
        assertEquals(DateRefusal.NOT_DIGITS, DateDigits.toRefusal(march, "2026-02"))
        assertEquals(DateRefusal.NOT_A_DATE, DateDigits.toRefusal(march, "20261340"))
        assertNull(DateDigits.toRefusal(march, "2026"))
        assertNull(DateDigits.toRefusal("2026", february))
        assertEquals(LocalDate.of(2026, 2, 1), DateDigits.toBound("2026", february))
        assertEquals(LocalDate.of(2026, 2, 1), DateDigits.toBound("2026-03", february))
    }

    private fun day(text: String): LocalDate = LocalDate.parse(text)

    private val opening = CashEntry("o1", day("2026-01-01"), 1_000_000L, CashType.OPENING_BALANCE, "", 1L)

    private fun deposit(
        id: String,
        date: String,
        seq: Long,
    ) = CashEntry(id, day(date), 1_000L, CashType.DEPOSIT, "", seq)

    // Cash (06): From Mar 1, To Feb 1: To's line, and the list from Mar 1 on.
    @Test
    fun cash_toBeforeFrom_showsItsLine_andTheListFromFromOn() {
        val entries = listOf(opening, deposit("d2", "2026-02-01", 2L), deposit("d3", "2026-03-01", 3L), deposit("d4", "2026-04-01", 4L))
        val state = AppState(entries = entries, cashFilterForm = CashFilterForm(fromText = march, toText = february))
        assertNull(state.cashList.fromRefusal)
        assertEquals(DateRefusal.BEFORE_FROM, state.cashList.toRefusal)
        assertEquals(listOf("d4", "d3"), state.cashList.rows.map { it.key })
        assertEquals(DateRefusal.BEFORE_FROM, state.filterToRefusal)
        assertEquals(listOf("d4", "d3"), state.cashFilteredRows.map { it.entry.id })
    }

    private val main = Portfolio("p1", "Main", 1L)

    private fun bought(
        id: String,
        date: String,
        seq: Long,
    ) = Transaction(id, "p1", "AAPL", TransactionType.BUY, day(date), seq, BigDecimal.TEN, 1_000L)

    // Transaction history (27): the same.
    @Test
    fun history_toBeforeFrom_showsItsLine_andTheRowsFromFromOn() {
        val trades = listOf(bought("b2", "2026-02-01", 2L), bought("b3", "2026-03-01", 3L), bought("b4", "2026-04-01", 4L))
        val state =
            AppState(
                screen = Screen.TRANSACTION_HISTORY,
                portfolios = listOf(main),
                transactions = trades,
                historyPortfolioId = "p1",
                historyFromText = march,
                historyToText = february,
            )
        assertEquals(listOf("b4", "b3"), state.historyRows.map { (it as HistoryRow.Entry).transaction.id })
        assertNull(state.transactionHistoryScreen.fromRefusal)
        assertEquals(DateRefusal.BEFORE_FROM, state.transactionHistoryScreen.toRefusal)
    }

    private fun sold(
        id: String,
        date: String,
        seq: Long,
    ) = Transaction(
        id = id,
        portfolioId = "p1",
        ticker = "AAPL",
        type = TransactionType.SELL,
        date = day(date),
        enteredSeq = seq,
        quantity = BigDecimal.ONE,
        totalCents = 200L,
        allocations = listOf(SellAllocation("b1", Fraction.of(BigDecimal.ONE))),
    )

    // Realized gains (26): the same, and its count line says "from" Mar 1, not "between".
    @Test
    fun gains_toBeforeFrom_showsItsLine_andTheSalesFromFromOn() {
        val trades = listOf(bought("b1", "2026-01-02", 1L), sold("s2", "2026-02-01", 2L), sold("s3", "2026-03-01", 3L), sold("s4", "2026-04-01", 4L))
        val state =
            AppState(
                screen = Screen.REALIZED_GAINS,
                portfolios = listOf(main),
                transactions = trades,
                gainsFromText = march,
                gainsToText = february,
            )
        assertEquals(2, state.realizedGains.count)
        val model = state.realizedGainsScreen
        assertNull(model.fromRefusal)
        assertEquals(DateRefusal.BEFORE_FROM, model.toRefusal)
        assertEquals(GainsCountLine.From(2, "2026-03-01"), model.countLine)
    }
}
