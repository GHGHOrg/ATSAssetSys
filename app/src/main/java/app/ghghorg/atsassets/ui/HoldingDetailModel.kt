package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.Holding
import app.ghghorg.atsassets.core.Lot
import app.ghghorg.atsassets.core.MarketDate
import app.ghghorg.atsassets.core.Money
import app.ghghorg.atsassets.core.Price
import app.ghghorg.atsassets.core.Quantity
import app.ghghorg.atsassets.core.Valuation
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

/*
 * Holding detail's header and Lots tab as the screen draws them (view 6, wireframe 06; CR2, 16a step 6). Plain Kotlin:
 * no Compose, no resources. The screen picks the approved string (D9) and puts these values in.
 */

/** An unrealized gain or loss as shown (section 6): a figure with its sign, or "no price" when valued at cost (4.8). */
sealed interface GainModel {
    /** "+$80.00", "−$80.00" or "$0.00"; [spoken]'s sign also gives the color: green, red or none. */
    data class Figure(
        val text: String,
        val spoken: SpokenFigure,
    ) : GainModel

    data object NoPrice : GainModel
}

/** The price and its own time in New York (16a decision 4), and whether it is stale (4.8). */
data class PriceLineModel(
    val perShare: String,
    val spokenPerShare: String,
    // YYYY-MM-DD HH:MM in New York; the screen adds "ET".
    val asOf: String,
    val stale: Boolean,
)

/** Holding detail's header (wireframe 06 marker 1). Shares as shown, with [oneShare] for "1 share". */
data class HoldingHeaderModel(
    val ticker: String,
    val portfolio: String,
    val value: String,
    val spokenValue: String,
    val shares: String,
    val oneShare: Boolean,
    val cost: String,
    val spokenCost: String,
    val gain: GainModel,
    val price: PriceLineModel?,
    // A refresh runs (the "updating" indicator), and the last one failed (the quiet line) (section 6, AC-42).
    val refreshing: Boolean,
    val refreshFailed: Boolean,
)

/** One lot on the Lots tab (wireframe 06 marker 5); [stale] when its gain comes from a stale price. */
data class LotRowModel(
    val key: String,
    // YYYY-MM-DD; the screen adds "ET".
    val date: String,
    val left: String,
    val perShare: String,
    val spokenPerShare: String,
    val cost: String,
    val spokenCost: String,
    val gain: GainModel,
    val stale: Boolean,
)

object HoldingDetail {
    private val AS_OF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT)

    /**
     * [holding] at [price] (4.8): its value, cost and gain, or at cost with "no price". A sold-out holding shows $0.00
     * as its gain, with no "no price" flag (11 wording 4).
     */
    fun header(
        holding: Holding,
        portfolio: String,
        price: Price?,
        refreshing: Boolean,
        refreshFailed: Boolean,
    ): HoldingHeaderModel {
        val valued = Valuation.valued(holding, price)
        val shares = Quantity.format(holding.shares)
        val soldOut = holding.shares.signum() == 0
        return HoldingHeaderModel(
            ticker = holding.ticker,
            portfolio = portfolio,
            value = Money.format(valued.value),
            spokenValue = digits(Money.format(valued.value)),
            shares = shares,
            oneShare = shares == "1",
            cost = Money.format(holding.cost),
            spokenCost = digits(Money.format(holding.cost)),
            gain = if (soldOut) gainOf(Fraction.ZERO) else gainOf(valued.gain),
            price = price?.let { priceLine(it) },
            refreshing = refreshing,
            refreshFailed = refreshFailed,
        )
    }

    /** Each lot with its gain at [price], in the order given (oldest first, 4.4). */
    fun lots(
        lots: List<Lot>,
        price: Price?,
    ): List<LotRowModel> = lots.map { lot ->
        val perShare = Money.formatPerShare(lot.costPerShare)
        val cost = Money.format(lot.cost)
        LotRowModel(
            key = lot.buyId,
            date = lot.buyDate.toString(),
            left = Quantity.format(lot.remainingQuantity),
            perShare = perShare,
            spokenPerShare = digits(perShare),
            cost = cost,
            spokenCost = digits(cost),
            gain = gainOf(Valuation.lotGain(lot, price)),
            stale = price?.stale ?: false,
        )
    }

    /** A gain rounded to cents once (4.10); its sign, and so its color, is the shown figure's: $0.00 has none. */
    fun gainOf(gain: Fraction?): GainModel {
        if (gain == null) return GainModel.NoPrice
        val cents = gain.rounded(2).movePointRight(2).longValueExact()
        return GainModel.Figure(signedAmount(cents), SpokenFigure.ofAmount(cents))
    }

    private fun priceLine(price: Price): PriceLineModel {
        val perShare = Money.formatPerShare(Fraction.of(price.perShare))
        return PriceLineModel(perShare, digits(perShare), asOf(price.time), price.stale)
    }

    /** [time] in New York, to the minute (16a decision 4). */
    fun asOf(time: Instant): String = time.atZone(MarketDate.ZONE).format(AS_OF)

    private fun digits(dollars: String): String = dollars.substringAfter('$')
}
