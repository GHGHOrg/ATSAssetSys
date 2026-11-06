package app.ghghorg.atsassets.core

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.time.LocalDate
import java.util.Locale

/** The two shapes of a timing history (15c, section 9, part 2). */
enum class ScaleShape {
    /** About 30 transactions per holding, so n / 30 holdings: the usual case. */
    SPREAD,

    /** Every transaction in one holding: the worst case for the calls on one holding. */
    ONE_HOLDING,
}

/**
 * A history for the timing tests: the portfolios, the opening balance and every trade line, and the transactions in
 * the order of entry. [largestPortfolioId] and [largestTicker] name the holding with the most transactions.
 */
data class ScaleHistory(
    val portfolios: List<Portfolio>,
    val cashEntries: List<CashEntry>,
    val transactions: List<Transaction>,
    val largestPortfolioId: String,
    val largestTicker: String,
) {
    val lastDate: LocalDate get() = transactions.maxOf { it.date }
}

/**
 * Timing histories by a fixed recipe, not at random, built without K1's save functions (15c, section 9, part 2): on the
 * code before 15c, building 1,000 transactions through them would itself take O(n⁴). The recipe keeps each lot's
 * remaining shares, so a sell never takes more than a lot holds and every history is valid by construction.
 *
 * Each holding repeats a cycle of 10: buys at 0, 1, 3, 6 and 8; sells at 2, 5 and 9, each taking all of the oldest lot
 * with shares and half of the next; a split at 4, alternating 3 for 2 and 2 for 3; and a reversal at 7, in turn of a
 * sell, a split, or a buy no sell took from. Three transactions a day. The splits cancel out in pairs and every lot is
 * halved at most once, so the fractions stay short and the timing measures the calculations, not long numbers (15c,
 * section 14).
 */
object ScaleHistories {
    fun build(
        size: Int,
        shape: ScaleShape,
        start: LocalDate = LocalDate.of(2024, 1, 2),
    ): ScaleHistory {
        val holdingCount = if (shape == ScaleShape.SPREAD) maxOf(1, size / PER_HOLDING) else 1
        val portfolioCount = minOf(PORTFOLIOS, holdingCount)
        val portfolios = (1..portfolioCount).map { Portfolio(id = "p$it", name = "Timing $it", createdSeq = it.toLong()) }
        val recipes =
            (0 until holdingCount).map {
                Recipe(portfolios[it % portfolioCount].id, String.format(Locale.ROOT, "S%03d", it + 1))
            }
        val cash =
            mutableListOf(
                CashEntry(id = "c0", date = start, amountCents = OPENING_CENTS, type = CashType.OPENING_BALANCE, note = "", enteredSeq = 1),
            )
        val transactions = mutableListOf<Transaction>()
        for (k in 0 until size) {
            val date = start.plusDays((k / PER_DAY).toLong())
            val made = recipes[k % holdingCount].next(id = "t$k", date = date, seq = k + 2L)
            transactions += made.transaction
            if (made.cashCents != null) {
                cash +=
                    CashEntry(
                        id = "c${k + 1}",
                        date = date,
                        amountCents = made.cashCents,
                        type = CashType.TRADE,
                        note = "",
                        enteredSeq = k + 2L,
                        transactionId = "t$k",
                    )
            }
        }
        val largest = recipes.maxBy { it.count }
        return ScaleHistory(portfolios, cash, transactions, largest.portfolioId, largest.ticker)
    }

    private const val PER_HOLDING = 30
    private const val PORTFOLIOS = 3
    private const val PER_DAY = 3
    private const val CYCLE = 10
    private const val OPENING_CENTS = 100_000_000_000L
    private val QUANTITIES = listOf("3", "2.5", "0.33333333", "7", "1.5")
    private val HALF = Fraction.of(BigInteger.ONE, BigInteger.TWO)
    private val SPLITS = listOf(SplitRatio(3, 2), SplitRatio(2, 3))

    private class Made(
        val transaction: Transaction,
        val cashCents: Long?,
    )

    private class LotLeft(
        val buyId: String,
        var remaining: Fraction,
        val totalCents: Long,
        var takenFrom: Boolean = false,
    )

    private class SellMade(
        val transaction: Transaction,
        val factorAtSell: Fraction,
        var reversed: Boolean = false,
    )

    private class SplitMade(
        val id: String,
        val factor: Fraction,
        var reversed: Boolean = false,
    )

    // One holding's lots in the units of its latest place: every split so far applied, a reversed split as its inverse.
    private class Recipe(
        val portfolioId: String,
        val ticker: String,
    ) {
        var count = 0
        private val lots = mutableListOf<LotLeft>()
        private val sells = mutableListOf<SellMade>()
        private val splits = mutableListOf<SplitMade>()
        private var factor = Fraction.ONE

        fun next(
            id: String,
            date: LocalDate,
            seq: Long,
        ): Made {
            val position = count % CYCLE
            val cycle = count / CYCLE
            val made =
                when (position) {
                    2, 5, 9 -> sell(id, date, seq) ?: buy(id, date, seq)
                    4 -> split(id, date, seq, cycle)
                    7 -> reversal(id, date, seq, cycle) ?: buy(id, date, seq)
                    else -> buy(id, date, seq)
                }
            count++
            return made
        }

        private fun transaction(
            id: String,
            type: TransactionType,
            date: LocalDate,
            seq: Long,
            quantity: BigDecimal = BigDecimal.ZERO,
            totalCents: Long = 0L,
        ) = Transaction(
            id = id,
            portfolioId = portfolioId,
            ticker = ticker,
            type = type,
            date = date,
            enteredSeq = seq,
            quantity = quantity,
            totalCents = totalCents,
        )

        private fun buy(
            id: String,
            date: LocalDate,
            seq: Long,
        ): Made {
            val quantity = BigDecimal(QUANTITIES[count % QUANTITIES.size])
            val priceCents = 1_000L + (count % 97) * 13L
            val totalCents = maxOf(1L, quantity.multiply(BigDecimal.valueOf(priceCents)).setScale(0, RoundingMode.HALF_UP).toLong())
            lots += LotLeft(id, Fraction.of(quantity), totalCents)
            return Made(transaction(id, TransactionType.BUY, date, seq, quantity, totalCents), -totalCents)
        }

        private fun sell(
            id: String,
            date: LocalDate,
            seq: Long,
        ): Made? {
            val open = lots.filter { it.remaining.signum() > 0 }
            val first = open.firstOrNull() ?: return null
            val takes = listOfNotNull(first to first.remaining, open.getOrNull(1)?.let { it to it.remaining * HALF })
            val allocations =
                takes.map { (lot, quantity) ->
                    lot.remaining -= quantity
                    lot.takenFrom = true
                    SellAllocation(lot.buyId, quantity)
                }
            val sum = takes.fold(Fraction.ZERO) { total, take -> total + take.second }
            val totalCents = 500L + count * 7L
            val sell =
                transaction(id, TransactionType.SELL, date, seq, Quantity.shown(sum), totalCents).copy(allocations = allocations)
            sells += SellMade(sell, factor)
            return Made(sell, totalCents)
        }

        private fun split(
            id: String,
            date: LocalDate,
            seq: Long,
            cycle: Int,
        ): Made {
            val ratio = SPLITS[cycle % SPLITS.size]
            applySplit(ratio.factor)
            splits += SplitMade(id, ratio.factor)
            return Made(transaction(id, TransactionType.SPLIT, date, seq).copy(split = ratio), null)
        }

        private fun applySplit(by: Fraction) {
            factor *= by
            lots.forEach { it.remaining *= by }
        }

        private fun reversal(
            id: String,
            date: LocalDate,
            seq: Long,
            cycle: Int,
        ): Made? {
            val kinds = List(3) { (cycle + it) % 3 }
            for (kind in kinds) {
                val made =
                    when (kind) {
                        0 -> reverseSell(id, date, seq)
                        1 -> reverseSplit(id, date, seq)
                        else -> reverseBuy(id, date, seq)
                    }
                if (made != null) return made
            }
            return null
        }

        private fun reversalOf(
            id: String,
            originalId: String,
            date: LocalDate,
            seq: Long,
        ) = transaction(id, TransactionType.REVERSAL, date, seq).copy(reversesId = originalId)

        private fun reverseSell(
            id: String,
            date: LocalDate,
            seq: Long,
        ): Made? {
            val sell = sells.lastOrNull { !it.reversed } ?: return null
            val since = factor / sell.factorAtSell
            for (allocation in sell.transaction.allocations) {
                lots.first { it.buyId == allocation.lotId }.remaining += allocation.quantity * since
            }
            sell.reversed = true
            return Made(reversalOf(id, sell.transaction.id, date, seq), -sell.transaction.totalCents)
        }

        private fun reverseSplit(
            id: String,
            date: LocalDate,
            seq: Long,
        ): Made? {
            val split = splits.lastOrNull { !it.reversed } ?: return null
            applySplit(Fraction.ONE / split.factor)
            split.reversed = true
            return Made(reversalOf(id, split.id, date, seq), null)
        }

        private fun reverseBuy(
            id: String,
            date: LocalDate,
            seq: Long,
        ): Made? {
            val lot = lots.lastOrNull { !it.takenFrom } ?: return null
            lots.remove(lot)
            return Made(reversalOf(id, lot.buyId, date, seq), lot.totalCents)
        }
    }
}
