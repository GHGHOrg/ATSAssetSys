package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.DayChange
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.Holding
import app.ghghorg.atsassets.core.Money
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.Price
import app.ghghorg.atsassets.core.Valuation

/*
 * Net Worth (Total) as the screen draws it (view 2, wireframe 02 markers 1, 2 and 6; CR2, 17): net worth with its price
 * age line and the refresh state, then the composition. Plain Kotlin: the screen picks the approved string and puts
 * these in. The breakdowns are built by Allocations (18, 23b).
 */

/** One portfolio in the composition: its name and its value as shown there (by largest remainder, 4.10). */
data class CompositionRowModel(
    val portfolioId: String,
    val name: String,
    val amount: String,
    val spokenAmount: String,
)

/**
 * The composition (marker 2): cash as of today, then each portfolio in the order created, shown by largest remainder so
 * that they add up to the net worth shown (4.10). The screen ends it with net worth.
 */
data class CompositionModel(
    val cash: String,
    val spokenCash: String,
    val portfolios: List<CompositionRowModel>,
)

/**
 * Net worth (marker 1) and its digits for TalkBack, the age of the oldest price used with "some stale" (4.8), null when
 * no price is used; the refresh state; and the composition, null while net worth shows as $0.00 (marker 6; decision 4).
 * CR5 (41): [investments], the sum of every portfolio's value, exact and rounded once (4.8, 4.10).
 */
data class NetWorthModel(
    val netWorth: String,
    val spokenNetWorth: String,
    val investments: String,
    val spokenInvestments: String,
    val prices: PricesAsOfModel?,
    val refresh: PriceRefreshModel,
    val composition: CompositionModel?,
    // CR5 (41): net worth's 1-day line in the shared card, as on Overall; null for none (4.8).
    val day: DayLineModel? = null,
)

object NetWorthTotal {
    /**
     * Net worth as cash as of today ([cashCents]) plus each portfolio's value, its holdings ([holdingsOf]) at their price
     * in [prices] or at cost (4.8), exact; shown rounded once. The age line is over every one of [holdings].
     */
    fun of(
        cashCents: Long,
        holdings: List<Holding>,
        portfolios: List<Portfolio>,
        holdingsOf: (String) -> List<Holding>,
        prices: Map<String, Price>,
        refresh: PriceRefreshModel,
        days: Map<Pair<String, String>, DayChange?> = emptyMap(),
    ): NetWorthModel {
        val cash = Fraction.ofCents(cashCents)
        val values =
            portfolios.map { portfolio ->
                holdingsOf(portfolio.id).fold(Fraction.ZERO) { sum, holding -> sum + Valuation.valued(holding, prices[holding.ticker]).value }
            }
        val netWorth = values.fold(cash) { sum, value -> sum + value }
        val shown = Money.format(netWorth)
        // CR5 (41): the same sum as Overall's (Investments.of), exact, rounded once.
        val investments = Money.format(values.fold(Fraction.ZERO) { sum, value -> sum + value })
        return NetWorthModel(
            netWorth = shown,
            spokenNetWorth = shown.substringAfter('$'),
            investments = investments,
            spokenInvestments = investments.substringAfter('$'),
            prices = PortfolioFigures.pricesAsOf(holdings, prices),
            refresh = refresh,
            composition = if (netWorth.rounded(2).signum() == 0) null else composition(cash, values, portfolios),
            day = PortfolioFigures.dayOf(holdings, days),
        )
    }

    private fun composition(
        cash: Fraction,
        values: List<Fraction>,
        portfolios: List<Portfolio>,
    ): CompositionModel {
        val shown = Money.partsInCents(listOf(cash) + values).map { Money.format(it) }
        return CompositionModel(
            cash = shown.first(),
            spokenCash = shown.first().substringAfter('$'),
            portfolios =
            portfolios.mapIndexed { index, portfolio ->
                val amount = shown[index + 1]
                CompositionRowModel(portfolio.id, portfolio.name, amount, amount.substringAfter('$'))
            },
        )
    }
}
