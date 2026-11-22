package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.Holding
import app.ghghorg.atsassets.core.Money
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.Price

/*
 * Overall as the screen draws it (view 1, wireframe 01 state A; CR2, 16c): net worth with its price age line, the refresh
 * state and each portfolio with its figures. Plain Kotlin: the screen picks the approved string and puts these in.
 */

/** One portfolio on Overall (wireframe 01 marker 4): its name and its figures as the Portfolios list has them. */
data class OverallPortfolioRowModel(
    val id: String,
    val name: String,
    val figures: PortfolioFiguresModel,
)

/**
 * Net worth (marker 2) as "$10,880.00" and its digits for TalkBack, the age of the oldest price used in it with "some
 * stale" (4.8; 16b decision 1), null when no price is used; the refresh state (markers 2 and 5; 16c decision 1); and the
 * portfolios in the order they were created.
 */
data class OverallModel(
    val netWorth: String,
    val spokenNetWorth: String,
    val prices: PricesAsOfModel?,
    val refresh: PriceRefreshModel,
    val portfolios: List<OverallPortfolioRowModel>,
)

object Overall {
    fun of(
        netWorth: Fraction,
        holdings: List<Holding>,
        portfolios: List<Portfolio>,
        holdingsOf: (String) -> List<Holding>,
        prices: Map<String, Price>,
        refresh: PriceRefreshModel,
    ): OverallModel {
        val shown = Money.format(netWorth)
        return OverallModel(
            netWorth = shown,
            spokenNetWorth = shown.replace("$", ""),
            prices = PortfolioFigures.pricesAsOf(holdings, prices),
            refresh = refresh,
            portfolios = portfolios.map { OverallPortfolioRowModel(it.id, it.name, PortfolioFigures.of(holdingsOf(it.id), prices)) },
        )
    }
}
