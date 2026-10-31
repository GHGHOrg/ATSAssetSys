package app.ghghorg.atsassets.core

import java.util.Locale

/** The asset types of spec section 3. */
enum class AssetType {
    STOCK,
    ETF,
}

/**
 * An instrument (spec section 3) as far as iteration 09 needs it: a ticker that passed the check of 4.3 and its asset
 * type. Prices (16) and source sector and geography (23) join later. Written only by K4.
 */
data class Instrument(
    val ticker: String,
    val assetType: AssetType,
    val verified: Boolean,
)

/** What the price source says about a ticker. Null means the source does not state it (4.3: then accepted). */
data class Listing(
    val type: String?,
    val exchange: String?,
    val currency: String?,
)

/** Why the source's answer refuses a ticker (spec 4.3). Reasons only; the wording is D9. */
sealed interface TickerRefusal {
    data object NotFound : TickerRefusal

    data object MutualFund : TickerRefusal

    /** The exchange as the source reported it. */
    data class NotUsExchange(
        val reported: String,
    ) : TickerRefusal

    /** The currency as the source reported it. */
    data class NotUsd(
        val reported: String,
    ) : TickerRefusal
}

sealed interface TickerVerdict {
    data class Accepted(
        val assetType: AssetType,
    ) : TickerVerdict

    data class Refused(
        val reason: TickerRefusal,
    ) : TickerVerdict
}

/**
 * The ticker check of spec 4.3 on the source's answer (K4 fetches it, K5 enforces it; 09 decisions 4 and 6). The
 * network is not known here.
 */
object TickerCheck {
    /**
     * The exchange table of 4.3, kept in plan.md as well: the 29 national securities exchanges registered under
     * Section 6(a), as the SEC page listed them on 2026-10-30 (price-source note 10.1), each with its former names
     * from that page and the ISO 10383 MIC code Finnhub reports (note 10.3; spike decision 4). Matched ignoring case
     * and spaces as in 4.9.
     */
    val EXCHANGES: Map<String, List<String>> =
        linkedMapOf(
            "24X National Exchange LLC" to emptyList(),
            "BOX Exchange LLC" to listOf("BOX Options Exchange LLC"),
            "Cboe BYX Exchange, Inc." to listOf("Bats BYX Exchange, Inc.", "BATS Y-Exchange, Inc."),
            "Cboe BZX Exchange, Inc." to listOf("Bats BZX Exchange, Inc.", "BATS Exchange, Inc.", "BATS"),
            "Cboe C2 Exchange, Inc." to emptyList(),
            "Cboe EDGA Exchange, Inc." to listOf("Bats EDGA Exchange, Inc.", "EDGA Exchange, Inc."),
            "Cboe EDGX Exchange, Inc." to listOf("Bats EDGX Exchange, Inc.", "EDGX Exchange, Inc."),
            "Cboe Exchange, Inc." to emptyList(),
            "Green Impact Exchange, LLC" to emptyList(),
            "Investors Exchange LLC" to listOf("IEXG"),
            "Long-Term Stock Exchange, Inc." to emptyList(),
            "MEMX, LLC" to emptyList(),
            "Miami International Securities Exchange" to emptyList(),
            "MIAX Emerald, LLC" to emptyList(),
            "MIAX PEARL, LLC" to emptyList(),
            "MIAX Sapphire, LLC" to emptyList(),
            "MX2 LLC" to emptyList(),
            "Nasdaq GEMX, LLC" to listOf("ISE Gemini"),
            "Nasdaq ISE, LLC" to listOf("International Securities Exchange, LLC"),
            "Nasdaq MRX, LLC" to listOf("ISE Mercury"),
            "Nasdaq PHLX LLC" to listOf("NASDAQ OMX PHLX, LLC", "Philadelphia Stock Exchange"),
            "Nasdaq Texas, LLC" to listOf("Nasdaq BX, Inc.", "NASDAQ OMX BX, Inc.", "Boston Stock Exchange"),
            "The Nasdaq Stock Market" to listOf("XNAS"),
            "New York Stock Exchange LLC" to listOf("XNYS"),
            "NYSE Arca, Inc." to listOf("ARCX"),
            "NYSE Texas, Inc." to listOf("NYSE Chicago, Inc.", "Chicago Stock Exchange"),
            "NYSE American LLC" to listOf("NYSE MKT LLC", "NYSE Amex", "NYSE Alternet US", "American Stock Exchange", "XASE"),
            "NYSE National, Inc." to listOf("National Stock Exchange, Inc."),
            "Texas Stock Exchange LLC" to emptyList(),
        )

    private const val ETF_TYPE = "ETP"
    private const val MUTUAL_FUND_TYPE = "Open-End Fund"
    private const val USD = "USD"

    private val SEC_NAME_BY_KEY: Map<String, String> =
        EXCHANGES.flatMap { (name, aliases) -> (aliases + name).map { Portfolios.nameKey(it) to name } }.toMap()

    /** The SEC name for [reported], an SEC name, former name or MIC code; null if the table does not hold it. */
    fun secName(reported: String): String? = SEC_NAME_BY_KEY[Portfolios.nameKey(reported)]

    /** A typed ticker trimmed and in capitals; null when nothing is left. */
    fun normalize(typed: String): String? = typed.trim().uppercase(Locale.ROOT).ifEmpty { null }

    /**
     * The verdict on the source's [listing] of a ticker, null when the source does not list it. The first rule that
     * fails decides the reason: found, mutual fund, exchange, currency (the order of 4.3). A field the source does
     * not state passes. Type "ETP" is an ETF and every other type a stock (09 decision 6).
     */
    fun verdict(listing: Listing?): TickerVerdict {
        if (listing == null) return TickerVerdict.Refused(TickerRefusal.NotFound)
        val type = listing.type?.takeIf { it.isNotBlank() }
        val exchange = listing.exchange?.takeIf { it.isNotBlank() }
        val currency = listing.currency?.takeIf { it.isNotBlank() }
        val refusal =
            when {
                type != null && Portfolios.nameKey(type) == Portfolios.nameKey(MUTUAL_FUND_TYPE) -> TickerRefusal.MutualFund
                exchange != null && secName(exchange) == null -> TickerRefusal.NotUsExchange(exchange)
                currency != null && Portfolios.nameKey(currency) != Portfolios.nameKey(USD) -> TickerRefusal.NotUsd(currency)
                else -> null
            }
        if (refusal != null) return TickerVerdict.Refused(refusal)
        val isEtf = type != null && Portfolios.nameKey(type) == Portfolios.nameKey(ETF_TYPE)
        return TickerVerdict.Accepted(if (isEtf) AssetType.ETF else AssetType.STOCK)
    }
}
