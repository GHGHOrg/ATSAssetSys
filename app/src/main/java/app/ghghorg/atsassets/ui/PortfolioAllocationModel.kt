package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.AssetType
import app.ghghorg.atsassets.core.Holding
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.Price

/*
 * Allocation, one portfolio at a time, as the screen draws it (view 4, wireframe 04; CR2, 19). Plain Kotlin: the screen
 * picks the approved string and puts these in.
 */

/** A portfolio in the chooser (marker 1). */
data class PortfolioChoiceModel(
    val id: String,
    val name: String,
)

/**
 * The chooser's portfolios as created and the shown one, null with no portfolio (marker 4); the shown portfolio's value
 * with its digits for TalkBack, the age of the oldest price used in it and the refresh state (marker 2); and its
 * breakdowns by asset type and holding (marker 3), null while the value shows as $0.00 (marker 5).
 */
data class PortfolioAllocationModel(
    val portfolios: List<PortfolioChoiceModel>,
    val shown: PortfolioChoiceModel?,
    val value: String,
    val spokenValue: String,
    val prices: PricesAsOfModel?,
    val refresh: PriceRefreshModel,
    val breakdowns: List<AllocationModel>?,
)

object PortfolioAllocation {
    /**
     * [portfolios] as created, the one with [shownId] shown, or the first when it is null or gone (view 4: the first from
     * More); its holdings ([holdingsOf]) at [prices] or at cost (4.8).
     */
    fun of(
        portfolios: List<Portfolio>,
        shownId: String?,
        holdingsOf: (String) -> List<Holding>,
        prices: Map<String, Price>,
        types: Map<String, AssetType>,
        refresh: PriceRefreshModel,
    ): PortfolioAllocationModel {
        val choices = portfolios.map { PortfolioChoiceModel(it.id, it.name) }
        val shown = choices.firstOrNull { it.id == shownId } ?: choices.firstOrNull()
        if (shown == null) return PortfolioAllocationModel(choices, null, "", "", null, refresh, null)
        val holdings = holdingsOf(shown.id)
        val figures = PortfolioFigures.of(holdings, prices)
        val breakdowns = listOf(AllocationBy.ASSET_TYPE, AllocationBy.HOLDING).mapNotNull { Allocations.ofPortfolio(it, holdings, prices, types) }
        return PortfolioAllocationModel(
            portfolios = choices,
            shown = shown,
            value = figures.value,
            spokenValue = figures.spokenValue,
            prices = figures.prices,
            refresh = refresh,
            breakdowns = breakdowns.takeIf { it.isNotEmpty() },
        )
    }
}
