package app.ghghorg.atsassets.prices

import app.ghghorg.atsassets.core.HiddenHolding
import app.ghghorg.atsassets.core.Instrument
import app.ghghorg.atsassets.core.Price
import app.ghghorg.atsassets.core.PriceSource
import app.ghghorg.atsassets.core.ProfileAnswer
import app.ghghorg.atsassets.core.Quote
import app.ghghorg.atsassets.core.RecordStore
import app.ghghorg.atsassets.core.SourceProfile
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
        // 23a: the profiles the source gave this time, by ticker; a ticker asked without an answer is not in it.
        val profiles: Map<String, SourceProfile> = emptyMap(),
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

    // A status from 500 up: the source itself failed, so the next ticker would fail the same way (16b decision 3).
    private const val SERVER_ERROR = 500

    /**
     * Asks [source] for each of [tickers] in turn. Offline it asks nothing. It stops at an answer that would be the
     * same for the next ticker (no key, the key refused, too many calls, no connection, a 5xx answer); every ticker not
     * asked fails.
     */
    suspend fun run(
        tickers: List<String>,
        online: Boolean,
        source: PriceSource,
        unasked: Set<String> = emptySet(),
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
                    (quote is Quote.NoAnswer && (quote.status ?: SERVER_ERROR) >= SERVER_ERROR)
            if (stop) break
        }
        return Outcome(fresh, tickers.filter { it !in fresh }, failedAnswer, profiles(tickers.filter { it in unasked }, source))
    }

    /**
     * 23a (decision 3): after every quote, the profile of each ticker in [tickers], in turn. Only a Given answer is kept.
     * It stops at an answer that would be the same for the next ticker, as the quotes do; a profile without an answer
     * fails neither a price nor the refresh, and is asked again at the next refresh.
     */
    private suspend fun profiles(
        tickers: List<String>,
        source: PriceSource,
    ): Map<String, SourceProfile> {
        val given = LinkedHashMap<String, SourceProfile>()
        for (ticker in tickers) {
            val answer = source.profile(ticker)
            if (answer is ProfileAnswer.Given) given[ticker] = answer.profile
            val stop =
                answer == ProfileAnswer.NoKey ||
                    answer is ProfileAnswer.KeyRefused ||
                    answer == ProfileAnswer.TooManyCalls ||
                    (answer is ProfileAnswer.NoAnswer && (answer.status ?: SERVER_ERROR) >= SERVER_ERROR)
            if (stop) break
        }
        return given
    }

    /**
     * A whole refresh: [run], then the outcome saved to [store] in one batch (K4 writes the prices itself, section 14;
     * Deploy finding L1), with the [held] tickers whose texts that map to no value the store adds (25 decision 4). Store
     * failures propagate; the prices then stay as stored.
     */
    suspend fun refresh(
        tickers: List<String>,
        online: Boolean,
        source: PriceSource,
        store: RecordStore,
        unasked: Set<String> = emptySet(),
        held: Collection<String> = emptyList(),
    ): Outcome = run(tickers, online, source, unasked).also { store.savePrices(it.fresh, it.failed, it.profiles, held) }

    /**
     * [instruments] after [outcome], as the store saves it: fresh prices set, the failed tickers' prices marked stale,
     * and each given profile set (23a).
     */
    fun applied(
        instruments: List<Instrument>,
        outcome: Outcome,
    ): List<Instrument> {
        val failed = outcome.failed.toSet()
        return instruments.map { instrument ->
            val fresh = outcome.fresh[instrument.ticker]
            val priced =
                when {
                    fresh != null -> instrument.copy(price = fresh.copy(stale = false))
                    instrument.ticker in failed -> instrument.copy(price = instrument.price?.copy(stale = true))
                    else -> instrument
                }
            outcome.profiles[instrument.ticker]?.let { priced.copy(profile = it) } ?: priced
        }
    }
}
