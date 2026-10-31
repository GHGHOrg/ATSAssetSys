package app.ghghorg.atsassets.core

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

/** K4, verify only in iteration 09 (spec 7: the source sits behind an interface so it can be replaced). */
interface PriceSource {
    /** Looks up [ticker], already trimmed and in capitals (TickerCheck.normalize). Never throws for a network failure. */
    suspend fun lookUp(ticker: String): Lookup
}
