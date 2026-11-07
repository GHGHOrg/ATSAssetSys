package app.ghghorg.atsassets.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Iteration 15c (section 9, part 1, layer 2; decision 1): the code is compared with HoldingsReference, the code before
 * 15c, on generated histories. For every history, at every date in it, both must give exactly equal results: lots,
 * holdings, net worth, the shortfall, the lot choices, the pieces, the effects, the refusal reasons, Hide and the
 * starting figures of Reverse and re-enter. Exact means Fraction equality, never close enough.
 */
class HoldingsComparisonTest {
    @Test
    fun theCodeGivesExactlyWhatTheReferenceGives() {
        val counts = sortedMapOf<String, Int>()
        var transactions = 0
        for (seed in 1L..HISTORIES) {
            val history = HistoryGenerator.generate(seed)
            history.counts.forEach { (kind, n) -> counts[kind] = (counts[kind] ?: 0) + n }
            transactions += history.transactions.size
            compare(history)
        }
        // The mix the comparison saw (15c, section 8: the comparison is only as good as the generator).
        println("HoldingsComparisonTest: $HISTORIES histories, $transactions transactions")
        counts.forEach { (kind, n) -> println("  $kind: $n") }
        assertTrue("Too few reversals of splits to compare", (counts["reversal of split"] ?: 0) >= 20)
        assertTrue("Too few refused reversals to compare", counts.keys.count { it.startsWith("reversal refused") } >= 1)
        assertTrue("Too few reverse splits to compare", (counts["split, reverse"] ?: 0) >= 20)
    }

    private fun compare(history: GeneratedHistory) {
        val transactions = history.transactions
        val cash = history.cashEntries
        val seed = history.seed
        val dates = (transactions.map { it.date } + transactions.minOf { it.date }.minusDays(1) + LocalDate.MAX).distinct().sorted()
        val keys = transactions.map { it.portfolioId to it.ticker }.distinct()
        val nextSeq = Transactions.nextSeq(cash, transactions)

        for (date in dates) {
            assertEquals("seed $seed, lots on $date", HoldingsReference.lots(transactions, date), Holdings.lots(transactions, date))
            assertEquals(
                "seed $seed, holdings on $date",
                HoldingsReference.holdings(transactions, date),
                Holdings.holdings(transactions, date),
            )
            if (date != LocalDate.MAX) {
                assertEquals(
                    "seed $seed, net worth on $date",
                    HoldingsReference.netWorth(cash, transactions, date),
                    Valuation.netWorth(cash, transactions, date),
                )
            }
        }
        val hidden = keys.map { (portfolio, ticker) -> HiddenHolding(portfolio, ticker) }
        assertEquals("seed $seed, unhidden by", HoldingsReference.unhiddenBy(transactions, hidden), Hiding.unhiddenBy(transactions, hidden))
        for ((portfolio, ticker) in keys) {
            val holding = "seed $seed, $portfolio $ticker"
            assertEquals(
                "$holding, shortfall",
                HoldingsReference.firstNegative(transactions, portfolio, ticker),
                Holdings.firstNegative(transactions, portfolio, ticker),
            )
            assertEquals(
                "$holding, Hide offered",
                HoldingsReference.hidingOffered(transactions, emptySet(), portfolio, ticker),
                Hiding.offered(transactions, emptySet(), portfolio, ticker),
            )
            for (date in dates) {
                assertEquals(
                    "$holding, lots of the holding on $date",
                    HoldingsReference.lotsOf(transactions, portfolio, ticker, date),
                    Holdings.lotsOf(transactions, portfolio, ticker, date),
                )
            }
            // The lot list where a sell would go: on each date the holding has an entry, at the end of that date.
            val sellDates = transactions.filter { it.portfolioId == portfolio && it.ticker == ticker }.map { it.date }.distinct()
            for (date in sellDates) {
                assertEquals(
                    "$holding, lot choices on $date",
                    HoldingsReference.lotChoices(transactions, portfolio, ticker, date, nextSeq),
                    Sells.lotChoices(transactions, portfolio, ticker, date, nextSeq),
                )
            }
        }
        for (transaction in transactions) {
            val what = "seed $seed, ${transaction.type} ${transaction.id}"
            when (transaction.type) {
                TransactionType.SELL -> {
                    assertEquals("$what, pieces", HoldingsReference.pieces(transactions, transaction), Sells.pieces(transactions, transaction))
                    val last = dates.last { it != LocalDate.MAX }
                    assertEquals(
                        "$what, starting picks",
                        HoldingsReference.startingPicks(transactions, transaction, last, nextSeq),
                        Reentries.startingPicks(transactions, transaction, last, nextSeq),
                    )
                }

                TransactionType.BUY -> {
                    val last = dates.last { it != LocalDate.MAX }
                    assertEquals(
                        "$what, starting quantity",
                        HoldingsReference.startingQuantity(transactions, transaction, last, nextSeq),
                        Reentries.startingQuantity(transactions, transaction, last, nextSeq),
                    )
                }

                TransactionType.SPLIT ->
                    assertEquals(
                        "$what, split effect",
                        HoldingsReference.splitEffect(transactions, transaction),
                        Splits.effect(transactions, transaction),
                    )

                TransactionType.REVERSAL ->
                    assertEquals(
                        "$what, reversal effect",
                        HoldingsReference.reversalEffect(transactions, transaction),
                        Reversals.effect(transactions, transaction),
                    )
            }
        }
        // Every transaction not yet reversed, reversed on the last date, and every refused candidate (section 9: the
        // refusals get compared too).
        val last = dates.last { it != LocalDate.MAX }
        val drafts =
            transactions
                .filter { Reversals.offered(transactions, it) }
                .map { Reversals.draft(cash, transactions, it, "draft-${it.id}", last) }
        for (candidate in drafts + history.candidates) {
            val after = transactions + candidate
            val what = "seed $seed, candidate ${candidate.type} ${candidate.id} on ${candidate.date}"
            val short = HoldingsReference.firstNegative(after, candidate.portfolioId, candidate.ticker)
            assertEquals("$what, shortfall", short, Holdings.firstNegative(after, candidate.portfolioId, candidate.ticker))
            if (candidate.type == TransactionType.REVERSAL) {
                assertEquals("$what, effect", HoldingsReference.reversalEffect(after, candidate), Reversals.effect(after, candidate))
                val original = HoldingsReference.originalOf(after, candidate)
                if (short != null && original != null) {
                    assertEquals(
                        "$what, refusal",
                        HoldingsReference.sharesRefusal(after, original, candidate, short),
                        Reversals.sharesRefusal(after, original, candidate, short),
                    )
                }
            }
            if (candidate.type == TransactionType.SPLIT) {
                assertEquals("$what, split effect", HoldingsReference.splitEffect(after, candidate), Splits.effect(after, candidate))
            }
        }
    }

    private companion object {
        const val HISTORIES = 200L
    }
}
