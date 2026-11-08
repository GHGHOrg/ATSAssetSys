package app.ghghorg.atsassets.core

import java.time.Instant
import java.time.LocalDate

/** K3 (spec 4.8). From 16a a holding is valued at its latest price; without one it is valued at cost ("no price"). */
object Valuation {
    /** The cash part of net worth: the cash balance as of today. Since iteration 09 the screens use [netWorth]. */
    fun netWorthCents(
        entries: List<CashEntry>,
        today: LocalDate,
    ): Long = CashRules.balanceOn(entries, today)

    /** The cash balance as of today plus every holding at cost, both as of today (4.8): net worth with no prices. */
    fun netWorth(
        entries: List<CashEntry>,
        transactions: List<Transaction>,
        today: LocalDate,
    ): Fraction = netWorthOf(entries, Holdings.holdings(transactions, today), today)

    /** [netWorth] from [holdings] already worked out as of [today], so the app builds them only once (15c); at cost. */
    fun netWorthOf(
        entries: List<CashEntry>,
        holdings: List<Holding>,
        today: LocalDate,
    ): Fraction = netWorthOf(entries, holdings, emptyMap(), today)

    /** The value of one portfolio's holdings as of today, each at its price in [prices] (by ticker) or at cost (4.8). */
    fun portfolioValue(
        transactions: List<Transaction>,
        portfolioId: String,
        today: LocalDate,
        prices: Map<String, Price> = emptyMap(),
    ): Fraction = Holdings
        .holdings(transactions, today)
        .filter { it.portfolioId == portfolioId }
        .fold(Fraction.ZERO) { sum, holding -> sum + valued(holding, prices[holding.ticker]).value }

    /**
     * [holding] at [price] (4.8): shares × price, with the unrealized gain market value − cost; a stale price keeps its
     * figures. With no price the holding is valued at cost and has no gain ("no price").
     */
    fun valued(
        holding: Holding,
        price: Price?,
    ): HoldingValue {
        if (price == null) return HoldingValue(holding.cost, gain = null, price = null)
        val value = holding.shares * Fraction.of(price.perShare)
        return HoldingValue(value, value - holding.cost, price)
    }

    /** A lot's unrealized gain at its holding's [price]: remaining quantity × price − remaining cost (4.4); null with none. */
    fun lotGain(
        lot: Lot,
        price: Price?,
    ): Fraction? = price?.let { lot.remainingQuantity * Fraction.of(it.perShare) - lot.cost }

    /** The cash balance as of today plus every holding at its price in [prices] (by ticker), or at cost (4.8). */
    fun netWorthOf(
        entries: List<CashEntry>,
        holdings: List<Holding>,
        prices: Map<String, Price>,
        today: LocalDate,
    ): Fraction {
        val cash = Fraction.ofCents(CashRules.balanceOn(entries, today))
        return holdings.fold(cash) { sum, holding -> sum + valued(holding, prices[holding.ticker]).value }
    }

    /**
     * The time of the oldest price used among [holdings] (4.8, the age line): only holdings with shares and a price
     * count; a holding valued at cost does not. Null when no price is used.
     */
    fun oldestPriceTime(
        holdings: List<Holding>,
        prices: Map<String, Price>,
    ): Instant? = holdings
        .filter { it.shares.signum() > 0 }
        .mapNotNull { prices[it.ticker]?.time }
        .minOrNull()
}

/** A holding's figures at its latest price (4.8): [gain] and [price] are null when it is valued at cost ("no price"). */
data class HoldingValue(
    val value: Fraction,
    val gain: Fraction?,
    val price: Price?,
)
