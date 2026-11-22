package app.ghghorg.atsassets.core

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Random

/**
 * A generated history (15c, section 9, part 1, layer 2): what K1 saved, in the order of entry, and the [candidates]:
 * transactions K1 refused or that were never saved, which the comparison replays to compare refusals.
 */
internal data class GeneratedHistory(
    val seed: Long,
    val cashEntries: List<CashEntry>,
    val transactions: List<Transaction>,
    val candidates: List<Transaction>,
    val counts: Map<String, Int>,
)

/**
 * Random histories built through the real K1 functions (buy, sell with picks, split, reversal), so each is a history you
 * could have entered. A fixed seed gives the same history every run, so a failure can be replayed. The mix: two
 * portfolios with the same three tickers, splits and reverse splits (1 for 3, 3 for 7 among them), reversals of buys,
 * sells and splits, quantities whose remainders round up as well as down (2/3, 5/3, 3/7, 1/7), several lots per
 * holding, entries backdated among earlier ones, and partial picks.
 */
internal object HistoryGenerator {
    private val START: LocalDate = LocalDate.of(2025, 1, 2)
    private val PORTFOLIOS = listOf(Portfolio("p1", "First", 1), Portfolio("p2", "Second", 2))
    private val TICKERS = listOf("AAA", "BBB", "CCC")
    private val QUANTITIES = listOf("1", "2.5", "0.33333333", "3", "7", "0.5", "10", "1.66666667")
    private val RATIOS = listOf(SplitRatio(2, 1), SplitRatio(3, 2), SplitRatio(1, 3), SplitRatio(3, 7), SplitRatio(7, 3), SplitRatio(1, 2))
    private val PARTS = listOf(BigDecimal.ONE, BigDecimal("0.5"), BigDecimal("0.33333333"))

    fun generate(seed: Long): GeneratedHistory {
        val random = Random(seed)
        val size = 20 + random.nextInt(31)
        val opening =
            (OpeningBalance.create(emptyList(), "c0", START, 1_000_000_000L, "") as OpeningBalanceResult.Created).entry
        val cash = mutableListOf(opening)
        val transactions = mutableListOf<Transaction>()
        val candidates = mutableListOf<Transaction>()
        val counts = sortedMapOf<String, Int>()
        var today = START
        var next = 0
        var attempts = 0

        fun count(kind: String) {
            counts[kind] = (counts[kind] ?: 0) + 1
        }

        while (transactions.size < size && attempts < size * 6) {
            attempts++
            today = today.plusDays(random.nextInt(3).toLong())
            val date = if (random.nextInt(100) < 15) maxOf(START, today.minusDays(random.nextInt(11).toLong())) else today
            val portfolio = PORTFOLIOS[random.nextInt(PORTFOLIOS.size)].id
            val ticker = TICKERS[random.nextInt(TICKERS.size)]
            val id = "t${next++}"
            val cashId = "c$next"
            val roll = random.nextInt(100)
            when {
                roll < 40 -> {
                    val quantity = BigDecimal(QUANTITIES[random.nextInt(QUANTITIES.size)])
                    val total = 100L + random.nextInt(100_000)
                    when (val result = Transactions.buy(cash, transactions, PORTFOLIOS, id, cashId, portfolio, ticker, date, quantity, total)) {
                        is BuyResult.Created -> {
                            transactions += result.transaction
                            cash += result.cashLine
                            count("buy")
                        }

                        is BuyResult.Refused -> count("buy refused")
                    }
                }

                roll < 70 -> {
                    val seq = Transactions.nextSeq(cash, transactions)
                    val open = Sells.lotChoices(transactions, portfolio, ticker, date, seq).filter { it.unavailable == null }
                    if (open.isEmpty()) continue
                    val picks =
                        open.shuffled(random).take(1 + random.nextInt(minOf(3, open.size))).map { choice ->
                            val shown = Sells.shown(choice.available)
                            val part = PARTS[random.nextInt(PARTS.size)]
                            val quantity = shown.multiply(part).setScale(Quantity.MAX_DECIMALS, RoundingMode.DOWN)
                            Pick(choice.buyId, if (quantity.signum() > 0) quantity else shown)
                        }
                    val partial = picks.any { it.quantity < Sells.shown(open.first { c -> c.buyId == it.lotId }.available) }
                    val quantity = picks.fold(BigDecimal.ZERO) { sum, pick -> sum + pick.quantity }
                    val total = random.nextInt(150_000).toLong()
                    val result = Sells.sell(cash, transactions, PORTFOLIOS, id, cashId, portfolio, ticker, date, quantity, total, picks)
                    when (result) {
                        is SellResult.Created -> {
                            transactions += result.transaction
                            cash += result.cashLine
                            count(if (partial) "sell, partial" else "sell, whole lots")
                        }

                        is SellResult.Refused -> count("sell refused")
                    }
                }

                roll < 80 -> {
                    val ratio = RATIOS[random.nextInt(RATIOS.size)]
                    when (val result = Splits.split(cash, transactions, PORTFOLIOS, id, portfolio, ticker, date, ratio)) {
                        is SplitResult.Created -> {
                            transactions += result.transaction
                            count(if (ratio.to < ratio.from) "split, reverse" else "split")
                        }

                        is SplitResult.Refused -> {
                            if (result.reason is SplitRefusal.SellShort) {
                                candidates += splitDraft(cash, transactions, id, portfolio, ticker, date, ratio)
                                count("split refused, sell short")
                            }
                        }
                    }
                }

                else -> {
                    val reversible = transactions.filter { Reversals.offered(transactions, it) }
                    if (reversible.isEmpty()) continue
                    val original = reversible[random.nextInt(reversible.size)]
                    val span = ChronoUnit.DAYS.between(original.date, today).coerceAtLeast(0L).toInt()
                    val reversalDate = original.date.plusDays(random.nextInt(span + 1).toLong())
                    when (val result = Reversals.reverse(cash, transactions, id, cashId, original.id, reversalDate)) {
                        is ReversalResult.Created -> {
                            transactions += result.transaction
                            result.cashLine?.let { cash += it }
                            count("reversal of ${original.type.name.lowercase()}")
                        }

                        is ReversalResult.Refused -> {
                            candidates += Reversals.draft(cash, transactions, original, id, reversalDate)
                            count("reversal refused, ${result.reason::class.simpleName}")
                        }
                    }
                }
            }
        }
        return GeneratedHistory(seed, cash, transactions, candidates, counts)
    }

    // The split Splits.split would have made: the same fields, for a refused split (4.5).
    private fun splitDraft(
        cash: List<CashEntry>,
        transactions: List<Transaction>,
        id: String,
        portfolioId: String,
        ticker: String,
        date: LocalDate,
        ratio: SplitRatio,
    ) = Transaction(
        id = id,
        portfolioId = portfolioId,
        ticker = ticker,
        type = TransactionType.SPLIT,
        date = date,
        enteredSeq = Transactions.nextSeq(cash, transactions),
        quantity = BigDecimal.ZERO,
        totalCents = 0L,
        split = ratio,
    )
}
