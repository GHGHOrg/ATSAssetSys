package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.Holding
import app.ghghorg.atsassets.core.Money
import app.ghghorg.atsassets.core.Price
import app.ghghorg.atsassets.core.Quantity
import app.ghghorg.atsassets.core.Valuation

/*
 * A portfolio's figures and its holding rows at the latest prices (view 3, 4.8; 16a decision 1, your A after step 6).
 * Plain Kotlin (CR2): the Portfolios list and a portfolio's screen pick the approved string and put these values in.
 * Their age line, stale flags and refresh-failed line follow in 16b.
 */

/**
 * A portfolio's value, cost and unrealized gain/loss: each holding at its price, or at cost with no gain (4.8).
 * [allAtCost] while there are shares and none of them has a price, for "(all holdings at cost)" (09 wording 4).
 */
data class PortfolioFiguresModel(
    val value: String,
    val spokenValue: String,
    val cost: String,
    val spokenCost: String,
    val gain: GainModel.Figure,
    val allAtCost: Boolean,
)

/**
 * One holding on a portfolio's screen: its value and gain at its price, "no price" when valued at cost, and $0.00 with
 * no flag when sold out (11 wording 4).
 */
data class PortfolioHoldingRowModel(
    val ticker: String,
    val shares: String,
    val oneShare: Boolean,
    val value: String,
    val spokenValue: String,
    val gain: GainModel,
    val soldOut: Boolean,
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
        )
    }
}
