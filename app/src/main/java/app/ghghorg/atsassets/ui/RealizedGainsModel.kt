package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.Money
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.Quantity
import app.ghghorg.atsassets.core.RealizedReport

/*
 * Realized gains as the screen draws it (view 5, wireframe 05; CR2, 26): the dates, the total and its count line, the
 * holdings and the sales of each expanded one, every figure worked out here. Plain Kotlin: the screen picks the
 * approved strings and puts these values in.
 */

/** A gain or loss as shown (section 6): "+$800.00", "−$200.00" or "$0.00", its sign for the color and for TalkBack. */
data class GainFigure(
    val text: String,
    val spoken: SpokenFigure,
) {
    /** The sign of the shown figure: 1, -1, or 0 at $0.00 (no color, 4.10). */
    val signum: Int get() = sign(spoken.sign)

    companion object {
        fun ofCents(cents: Long): GainFigure = GainFigure(signedAmount(cents), SpokenFigure.ofAmount(cents))

        private fun sign(sign: FigureSign): Int = when (sign) {
            FigureSign.PLUS -> 1
            FigureSign.MINUS -> -1
            FigureSign.NONE -> 0
        }
    }
}

/** The count line under the total (wireframe 05 marker 2): the number of sales and the range shown. */
sealed interface GainsCountLine {
    val count: Int

    data class AllDates(
        override val count: Int,
    ) : GainsCountLine

    data class Between(
        override val count: Int,
        val from: String,
        val to: String,
    ) : GainsCountLine

    data class From(
        override val count: Int,
        val from: String,
    ) : GainsCountLine

    data class UpTo(
        override val count: Int,
        val to: String,
    ) : GainsCountLine
}

/**
 * One sale under an expanded holding (marker 6): the sell's id (it opens the sell, decision 1), its date as YYYY-MM-DD
 * (the screen adds "ET"), its gain by largest remainder with the holding's other sales (4.10), the quantity as entered,
 * the proceeds and the cost of the lots used, each shown alone.
 */
data class RealizedSaleModel(
    val sellId: String,
    val date: String,
    val gain: GainFigure,
    val quantity: String,
    val proceeds: String,
    val spokenProceeds: String,
    val cost: String,
    val spokenCost: String,
)

/** One holding (marker 3): ticker, portfolio name, number of sales and total; its sales only while expanded, newest first. */
data class RealizedHoldingModel(
    val portfolioId: String,
    val ticker: String,
    val portfolioName: String,
    val count: Int,
    val total: GainFigure,
    val expanded: Boolean,
    val sales: List<RealizedSaleModel>,
)

/** The whole screen: the two fields as typed with their lines, the total, the count line, and the holdings (none: the plain line). */
data class RealizedGainsModel(
    val fromText: String,
    val toText: String,
    val fromRefusal: DateRefusal?,
    val toRefusal: DateRefusal?,
    val total: GainFigure,
    val countLine: GainsCountLine,
    val holdings: List<RealizedHoldingModel>,
)

object RealizedGainsScreens {
    /**
     * The screen from K3's [report] for [fromText] and [toText] as typed (an end that is not a whole date counts as open,
     * decision 3), the [portfolios] for their names, and the [expanded] holdings.
     */
    fun of(
        report: RealizedReport,
        portfolios: List<Portfolio>,
        fromText: String,
        toText: String,
        expanded: Set<Pair<String, String>>,
    ): RealizedGainsModel {
        val from = DateDigits.toDate(fromText)?.toString()
        val to = DateDigits.toBound(fromText, toText)?.toString()
        val count = report.count
        val countLine =
            when {
                from != null && to != null -> GainsCountLine.Between(count, from, to)
                from != null -> GainsCountLine.From(count, from)
                to != null -> GainsCountLine.UpTo(count, to)
                else -> GainsCountLine.AllDates(count)
            }
        val names = portfolios.associate { it.id to it.name }
        val holdings =
            report.holdings.map { holding ->
                val key = holding.portfolioId to holding.ticker
                val open = key in expanded
                val gains = if (open) Money.partsInCents(holding.sales.map { it.gain }) else emptyList()
                RealizedHoldingModel(
                    portfolioId = holding.portfolioId,
                    ticker = holding.ticker,
                    portfolioName = names[holding.portfolioId].orEmpty(),
                    count = holding.sales.size,
                    total = GainFigure.ofCents(cents(holding.total)),
                    expanded = open,
                    sales =
                    holding.sales.zip(gains) { sale, gain ->
                        val cost = Money.format(sale.cost)
                        RealizedSaleModel(
                            sellId = sale.sell.id,
                            date = sale.sell.date.toString(),
                            gain = GainFigure.ofCents(gain),
                            quantity = Quantity.format(sale.sell.quantity),
                            proceeds = Money.format(sale.sell.totalCents),
                            spokenProceeds = Money.format(sale.sell.totalCents).substringAfter('$'),
                            cost = cost,
                            spokenCost = cost.substringAfter('$'),
                        )
                    },
                )
            }
        return RealizedGainsModel(
            fromText = fromText,
            toText = toText,
            fromRefusal = DateDigits.refusal(fromText),
            toRefusal = DateDigits.toRefusal(fromText, toText),
            total = GainFigure.ofCents(cents(report.total)),
            countLine = countLine,
            holdings = holdings,
        )
    }

    // A sum of exact values, rounded once to cents, half away from zero (4.10).
    private fun cents(value: Fraction): Long = value.rounded(2).movePointRight(2).longValueExact()
}
