package app.ghghorg.atsassets.prices

import app.ghghorg.atsassets.core.HiddenHolding
import app.ghghorg.atsassets.core.Instrument
import app.ghghorg.atsassets.core.Price
import app.ghghorg.atsassets.core.PriceSource
import app.ghghorg.atsassets.core.Quote
import app.ghghorg.atsassets.core.RecordStore
import app.ghghorg.atsassets.core.Transaction

/**
 * K4's price refresh (16a; spec 7, 4.8): which tickers it asks for, and what the answers mean. It knows only core's
 * PriceSource, not the source behind it. Nothing is logged (spec 9).
 */
object PriceRefresh {
    /**
     * A refresh's result: the [fresh] prices by ticker, the tickers that got none ([failed]; their last price stays,
     * marked stale), and whether the refresh failed ([refreshFailed]: offline, or an answer that was not the source's
     * own answer about a ticker). A ticker the source does not price (NoQuote) fails alone and does not fail the refresh.
     */
    data class Outcome(
        val fresh: Map<String, Price>,
        val failed: List<String>,
        val refreshFailed: Boolean,
    )

    /**
     * Every ticker with a holding that is not hidden, sold-out ones included (16a decision 3), in ticker order. A ticker
     * hidden in one portfolio and held in another is still asked for.
     */
    fun tickers(
        transactions: List<Transaction>,
        hidden: List<HiddenHolding>,
    ): List<String> {
        val hiddenHoldings = hidden.toSet()
        return transactions
            .filter { HiddenHolding(it.portfolioId, it.ticker) !in hiddenHoldings }
            .map { it.ticker }
            .distinct()
            .sorted()
    }

    /**
     * Asks [source] for each of [tickers] in turn. Offline it asks nothing. It stops at an answer that would be the
     * same for the next ticker (no key, the key refused, too many calls, no connection); every ticker not asked fails.
     */
    suspend fun run(
        tickers: List<String>,
        online: Boolean,
        source: PriceSource,
    ): Outcome {
        if (tickers.isEmpty()) return Outcome(emptyMap(), emptyList(), refreshFailed = false)
        if (!online) return Outcome(emptyMap(), tickers, refreshFailed = true)
        val fresh = LinkedHashMap<String, Price>()
        var failedAnswer = false
        for (ticker in tickers) {
            val quote = source.quote(ticker)
            when (quote) {
                is Quote.Priced -> fresh[ticker] = Price(quote.price, quote.time)
                Quote.NoQuote -> Unit
                Quote.Unreadable, is Quote.NoAnswer, Quote.NoKey, is Quote.KeyRefused, Quote.TooManyCalls -> failedAnswer = true
            }
            val stop =
                quote == Quote.NoKey ||
                    quote is Quote.KeyRefused ||
                    quote == Quote.TooManyCalls ||
                    (quote is Quote.NoAnswer && quote.status == null)
            if (stop) break
        }
        return Outcome(fresh, tickers.filter { it !in fresh }, failedAnswer)
    }

    /**
     * A whole refresh: [run], then the outcome saved to [store] in one batch (K4 writes the prices itself, section 14;
     * Deploy finding L1). Store failures propagate; the prices then stay as stored.
     */
    suspend fun refresh(
        tickers: List<String>,
        online: Boolean,
        source: PriceSource,
        store: RecordStore,
    ): Outcome = run(tickers, online, source).also { store.savePrices(it.fresh, it.failed) }

    /** [instruments] after [outcome], as the store saves it: fresh prices set, the failed tickers' prices marked stale. */
    fun applied(
        instruments: List<Instrument>,
        outcome: Outcome,
    ): List<Instrument> {
        val failed = outcome.failed.toSet()
        return instruments.map { instrument ->
            val fresh = outcome.fresh[instrument.ticker]
            when {
                fresh != null -> instrument.copy(price = fresh.copy(stale = false))
                instrument.ticker in failed -> instrument.copy(price = instrument.price?.copy(stale = true))
                else -> instrument
            }
        }
    }
}
