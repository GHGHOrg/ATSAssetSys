package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.LotChoice
import app.ghghorg.atsassets.core.LotUnavailable
import app.ghghorg.atsassets.core.Money
import app.ghghorg.atsassets.core.Price
import app.ghghorg.atsassets.core.Quantity
import app.ghghorg.atsassets.core.Sells

/*
 * The sell screen's lot list as it draws it (wireframe 10a markers 12 to 14; CR2, 16c decision 3). Plain Kotlin: the
 * screen picks the approved string and puts these values in. The picks themselves stay in the sell form.
 */

/**
 * One lot: its date (the screen adds "ET"), what is left for this sell and its cost per share, in the units of the sell
 * date; its unrealized gain at the latest price (16c decision 4), or "no price"; [staleAsOf], its stale price's own time
 * in New York; and [unavailable], why it cannot be picked. A lot that cannot be picked has no gain.
 */
data class SellLotRowModel(
    val key: String,
    val date: String,
    val left: String,
    val perShare: String,
    val spokenPerShare: String,
    val gain: GainModel?,
    val staleAsOf: String?,
    val unavailable: LotUnavailable?,
)

object SellLots {
    fun rows(
        choices: List<LotChoice>,
        toToday: Fraction,
        price: Price?,
    ): List<SellLotRowModel> = choices.map { choice ->
        val perShare = Money.formatPerShare(choice.costPerShare)
        val pickable = choice.unavailable == null
        SellLotRowModel(
            key = choice.buyId,
            date = choice.buyDate.toString(),
            left = Quantity.format(choice.available),
            perShare = perShare,
            spokenPerShare = perShare.substringAfter('$'),
            gain = if (pickable) HoldingDetail.gainOf(Sells.choiceGain(choice, toToday, price)) else null,
            staleAsOf = price?.takeIf { pickable && it.stale }?.let { HoldingDetail.asOf(it.time) },
            unavailable = choice.unavailable,
        )
    }
}
