package app.ghghorg.atsassets.core

import java.math.BigDecimal
import java.time.Instant

/** What a look-up of one ticker at the price source gave (09 decision 8, amended in step 6). */
sealed interface Lookup {
    /** The source lists the ticker; [listing] holds what it says (4.3). */
    data class Found(
        val listing: Listing,
    ) : Lookup

    /** The source answered and does not list the ticker. */
    data object NotFound : Lookup

    /** The source answered with status 200, but the body is not the list the app expects: a content problem. */
    data object Unreadable : Lookup

    /** The source refused the key: status 401 or 403. Waiting does not help; a new key and build do. */
    data class KeyRefused(
        val status: Int,
    ) : Lookup

    /** Too many calls in a short time: status 429. */
    data object TooManyCalls : Lookup

    /** No usable answer: no connection or a timeout ([status] null), or another status than 200, 401, 403 and 429. */
    data class NoAnswer(
        val status: Int?,
    ) : Lookup

    /** This build has no key for the source (spike decision 5: an empty key, as in CI). */
    data object NoKey : Lookup
}

/** What a quote of one ticker at the price source gave (16a). Every kind but [Priced] leaves the last price, stale (4.8). */
sealed interface Quote {
    /** The latest [price] per share, exact as sent (4.10), and its own [time] at the source (16a decision 4). */
    data class Priced(
        val price: BigDecimal,
        val time: Instant,
    ) : Quote

    /** The source answered and has no price for the ticker: Finnhub sends c = 0 and t = 0 (16a, checked 2026-11-08). */
    data object NoQuote : Quote

    /** The source answered with status 200, but the body is not a quote the app can read. */
    data object Unreadable : Quote

    /** The source refused the key: status 401 or 403. */
    data class KeyRefused(
        val status: Int,
    ) : Quote

    /** Too many calls in a short time: status 429. */
    data object TooManyCalls : Quote

    /** No usable answer: no connection or a timeout ([status] null), or another status than 200, 401, 403 and 429. */
    data class NoAnswer(
        val status: Int?,
    ) : Quote

    /** This build has no key for the source. */
    data object NoKey : Quote
}

/** K4: verify (09) and quote (16a). Spec 7: the source sits behind an interface so it can be replaced. */
interface PriceSource {
    /** Looks up [ticker], already trimmed and in capitals (TickerCheck.normalize). Never throws for a network failure. */
    suspend fun lookUp(ticker: String): Lookup

    /** The latest price of [ticker], already in capitals. Never throws for a network failure. */
    suspend fun quote(ticker: String): Quote
}
