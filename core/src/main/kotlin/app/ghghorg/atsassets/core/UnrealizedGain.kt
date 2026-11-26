package app.ghghorg.atsassets.core

/**
 * An unrealized gain/loss with the cost it is over (CR6, 43; spec 4.8; K3): [gain] and [cost], both exact (4.10). A total
 * sums both over the lots or holdings that have a gain/loss, so "no price" is left out of both.
 */
data class UnrealizedGain(
    val gain: Fraction,
    val cost: Fraction,
) {
    /** The gain over its cost, the return since purchase; none at a cost of $0.00 (4.8). */
    val percent: Fraction? get() = if (cost.signum() == 0) null else gain / cost

    operator fun plus(other: UnrealizedGain): UnrealizedGain = UnrealizedGain(gain + other.gain, cost + other.cost)
}

object UnrealizedGains {
    /** [holding] at [price]: its gain and cost; null with no price ("no price", 4.8). */
    fun ofHolding(
        holding: Holding,
        price: Price?,
    ): UnrealizedGain? = Valuation.valued(holding, price).gain?.let { UnrealizedGain(it, holding.cost) }

    /** A lot at its holding's [price]: its gain and its remaining cost (4.4); null with no price. */
    fun ofLot(
        lot: Lot,
        price: Price?,
    ): UnrealizedGain? = Valuation.lotGain(lot, price)?.let { UnrealizedGain(it, lot.cost) }

    /** A sell lot choice at [price] (16c decision 4): its available quantity in today's shares and what it cost. */
    fun ofChoice(
        choice: LotChoice,
        toToday: Fraction,
        price: Price?,
    ): UnrealizedGain? = Sells.choiceGain(choice, toToday, price)?.let { UnrealizedGain(it, choice.available * choice.costPerShare) }

    /** The sum of [gains], leaving out the ones with none; $0.00 over $0.00 when none has one (4.8). */
    fun total(gains: List<UnrealizedGain?>): UnrealizedGain = gains.filterNotNull().fold(NONE) { sum, it -> sum + it }

    private val NONE = UnrealizedGain(Fraction.ZERO, Fraction.ZERO)
}
