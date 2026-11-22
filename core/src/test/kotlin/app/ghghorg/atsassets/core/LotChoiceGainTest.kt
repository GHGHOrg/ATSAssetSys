package app.ghghorg.atsassets.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.LocalDate

// 16c decision 4: a lot's gain in the sell picker is for the quantity left on the sell date, in today's shares through
// any split between the two dates, at the latest price (4.4, 4.8, 4.10).
class LotChoiceGainTest {
    private val at = Instant.ofEpochSecond(1_793_304_000L)

    private fun price(text: String): Price = Price(BigDecimal(text), at)

    private fun dollars(
        numerator: Long,
        denominator: Long = 1L,
    ): Fraction = Fraction.of(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator))

    // 10 AAPL for $1,000.00 on 2026-01-02: $100.00 a share.
    private val buy =
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

    // 2 for 1 on 2026-03-01.
    private val split =
        Transaction(
            id = "x1",
            portfolioId = "p1",
            ticker = "AAPL",
            type = TransactionType.SPLIT,
            date = LocalDate.of(2026, 3, 1),
            enteredSeq = 3L,
            quantity = BigDecimal.ZERO,
            totalCents = 0L,
            split = SplitRatio(2, 1),
        )

    private fun gain(
        transactions: List<Transaction>,
        sellDate: LocalDate,
        today: LocalDate,
        price: Price?,
    ): Fraction? {
        val choice = Sells.lotChoices(transactions, "p1", "AAPL", sellDate, 99L).single()
        val toToday = Sells.unitsToToday(transactions, "p1", "AAPL", sellDate, 99L, today)
        return Sells.choiceGain(choice, toToday, price)
    }

    @Test
    fun aSellDatedToday_withNoSplit_isTheLotsGainAtThePrice() {
        val today = LocalDate.of(2026, 2, 1)
        assertEquals(dollars(600), gain(listOf(buy), today, today, price("160")))
        assertEquals(dollars(-400), gain(listOf(buy), today, today, price("60")))
        // Under $1, four decimals as sent (4.10): 10 × $0.5025 − $1,000.00 = −$994.975, exactly.
        assertEquals(dollars(-39_799, 40), gain(listOf(buy), today, today, price("0.5025")))
    }

    @Test
    fun withNoPrice_thereIsNoGain() {
        val today = LocalDate.of(2026, 2, 1)
        assertNull(gain(listOf(buy), today, today, null))
    }

    // The pitfall of the 16c file, section 4: 10 shares at $100.00 on the sell date are 20 shares today.
    @Test
    fun aSplitAfterTheSellDate_putsTheLotInTodaysShares() {
        val transactions = listOf(buy, split)
        val sellDate = LocalDate.of(2026, 2, 1)
        val today = LocalDate.of(2026, 6, 1)
        assertEquals(dollars(2), Sells.unitsToToday(transactions, "p1", "AAPL", sellDate, 99L, today))
        // 20 × $60.00 − $1,000.00, not 10 × ($60.00 − $100.00).
        assertEquals(dollars(200), gain(transactions, sellDate, today, price("60")))
    }

    // A sell dated after today, with the split between: 20 shares on the sell date are 10 today.
    @Test
    fun aSellDatedAfterToday_withASplitBetween_putsTheLotInTodaysSharesToo() {
        val transactions = listOf(buy, split)
        val sellDate = LocalDate.of(2026, 4, 1)
        val today = LocalDate.of(2026, 2, 1)
        assertEquals(dollars(1, 2), Sells.unitsToToday(transactions, "p1", "AAPL", sellDate, 99L, today))
        // 10 × $120.00 − $1,000.00.
        assertEquals(dollars(200), gain(transactions, sellDate, today, price("120")))
    }

    @Test
    fun aSplitBeforeBothDates_changesNothing() {
        val transactions = listOf(buy, split)
        val sellDate = LocalDate.of(2026, 4, 1)
        val today = LocalDate.of(2026, 6, 1)
        assertEquals(Fraction.ONE, Sells.unitsToToday(transactions, "p1", "AAPL", sellDate, 99L, today))
        // 20 × $60.00 − $1,000.00.
        assertEquals(dollars(200), gain(transactions, sellDate, today, price("60")))
    }
}
