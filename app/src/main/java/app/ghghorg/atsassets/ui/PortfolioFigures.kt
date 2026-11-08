package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.Holding
import app.ghghorg.atsassets.core.Money
import app.ghghorg.atsassets.core.Price
import app.ghghorg.atsassets.core.Quantity
import app.ghghorg.atsassets.core.Valuation

/*
 * A portfolio's figures and its holding rows at the latest prices (view 3, 4.8; 16a decision 1, your A after step 6),
 * with the price age line and the stale flags (16b). Plain Kotlin (CR2): the Portfolios list and a portfolio's screen
 * pick the approved string and put these values in.
 */

/**
 * The summary's price age line (4.8; 16b): the time of the oldest price used, in New York to the minute (the screen adds
 * "ET"), and whether any price used is stale (16b decision 1).
 */
data class PricesAsOfModel(
    val asOf: String,
    val someStale: Boolean,
)

/**
 * A portfolio's value, cost and unrealized gain/loss: each holding at its price, or at cost with no gain (4.8).
 * [allAtCost] while there are shares and none of them has a price, for "(all holdings at cost)" (09 wording 4).
 * [prices] is null when no price is used: every holding is at cost or sold out.
 */
data class PortfolioFiguresModel(
    val value: String,
    val spokenValue: String,
    val cost: String,
    val spokenCost: String,
    val gain: GainModel.Figure,
    val allAtCost: Boolean,
    val prices: PricesAsOfModel?,
)

/**
 * One holding on a portfolio's screen: its value and gain at its price, "no price" when valued at cost, and $0.00 with
 * no flag when sold out (11 wording 4). [staleAsOf] is its stale price's own time in New York (the screen adds "ET"),
 * null unless it has shares and a stale price (16b).
 */
data class PortfolioHoldingRowModel(
    val ticker: String,
    val shares: String,
    val oneShare: Boolean,
    val value: String,
    val spokenValue: String,
    val gain: GainModel,
    val soldOut: Boolean,
    val staleAsOf: String?,
)

/** The price refresh as the Portfolios list and a portfolio's screen show it (section 6, error and loading states; 16b). */
data class PriceRefreshModel(
    val refreshing: Boolean,
    val failed: Boolean,
)

object PortfolioFigures {
    /** [holdings] of one portfolio, each at its price in [prices] (by ticker) or at cost. */
    fun of(
        holdings: List<Holding>,
        prices: Map<String, Price>,
    ): PortfolioFiguresModel {
        val valued = holdings.map { Valuation.valued(it, prices[it.ticker]) }
        val value = valued.fold(Fraction.ZERO) { sum, it -> sum + it.value }
        val cost = holdings.fold(Fraction.ZERO) { sum, it -> sum + it.cost }
        val gain = valued.fold(Fraction.ZERO) { sum, it -> sum + (it.gain ?: Fraction.ZERO) }
        val withShares = holdings.filter { it.shares.signum() > 0 }
        return PortfolioFiguresModel(
            value = Money.format(value),
            spokenValue = Money.format(value).substringAfter('$'),
            cost = Money.format(cost),
            spokenCost = Money.format(cost).substringAfter('$'),
            gain = HoldingDetail.gainOf(gain) as GainModel.Figure,
            allAtCost = withShares.isNotEmpty() && withShares.none { prices[it.ticker] != null },
            prices = pricesAsOf(holdings, prices),
        )
    }

    /** One row per holding, in the order given. */
    fun rows(
        holdings: List<Holding>,
        prices: Map<String, Price>,
    ): List<PortfolioHoldingRowModel> = holdings.map { holding ->
        val valued = Valuation.valued(holding, prices[holding.ticker])
        val shares = Quantity.format(holding.shares)
        val soldOut = holding.shares.signum() == 0
        PortfolioHoldingRowModel(
            ticker = holding.ticker,
            shares = shares,
            oneShare = shares == "1",
            value = Money.format(valued.value),
            spokenValue = Money.format(valued.value).substringAfter('$'),
            gain = if (soldOut) HoldingDetail.gainOf(Fraction.ZERO) else HoldingDetail.gainOf(valued.gain),
            soldOut = soldOut,
            staleAsOf = prices[holding.ticker]?.takeIf { it.stale && !soldOut }?.let { HoldingDetail.asOf(it.time) },
        )
    }

    /**
     * The oldest price used (4.8: holdings with shares and a price only) and whether any price used is stale (16b); for a
     * portfolio and, over every holding, for net worth on Overall (16c).
     */
    fun pricesAsOf(
        holdings: List<Holding>,
        prices: Map<String, Price>,
    ): PricesAsOfModel? {
        val oldest = Valuation.oldestPriceTime(holdings, prices) ?: return null
        val used = holdings.filter { it.shares.signum() > 0 }.mapNotNull { prices[it.ticker] }
        return PricesAsOfModel(HoldingDetail.asOf(oldest), someStale = used.any { it.stale })
    }
}
