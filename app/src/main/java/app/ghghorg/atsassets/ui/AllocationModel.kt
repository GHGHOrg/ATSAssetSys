package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.Allocation
import app.ghghorg.atsassets.core.AssetType
import app.ghghorg.atsassets.core.ClassList
import app.ghghorg.atsassets.core.ClassLists
import app.ghghorg.atsassets.core.ClassOverride
import app.ghghorg.atsassets.core.Classification
import app.ghghorg.atsassets.core.EffectiveClass
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.Holding
import app.ghghorg.atsassets.core.Money
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.Price
import app.ghghorg.atsassets.core.Slice
import app.ghghorg.atsassets.core.SliceKey
import app.ghghorg.atsassets.core.SourceProfile

/*
 * The allocation of net worth as Overall and Net Worth (Total) draw it (view 1, view 2; wireframe 01 marker 3, wireframe
 * 02 markers 3 and 4; CR2, 18). Plain Kotlin: the screens pick the approved string for a name and put these in.
 */

/** "Allocation by" (view 1): Asset type first (18 decision 1); Sector and Geography from 23b. */
enum class AllocationBy {
    ASSET_TYPE,
    PORTFOLIO,
    HOLDING,
    SECTOR,
    GEOGRAPHY,
}

/** Each holding's effective sector and geography (4.9, K10; 23b) and the two lists, for the breakdowns. */
class HoldingClasses(
    val lists: ClassLists = ClassLists(),
    private val profiles: Map<String, SourceProfile> = emptyMap(),
    // 24: the holdings' overrides, each first for its holding and list.
    private val overrides: List<ClassOverride> = emptyList(),
) {
    /** [holding]'s effective value on [list]; see [valueFor]. */
    fun valueOf(
        list: ClassList,
        holding: Holding,
    ): String? = valueFor(list, holding.portfolioId, holding.ticker)

    /**
     * The holding's effective value on [list] (4.9): its override (24), else its instrument's source text through the list,
     * else null ("Unclassified").
     */
    fun valueFor(
        list: ClassList,
        portfolioId: String,
        ticker: String,
    ): String? = effective(list, portfolioId, ticker).value

    /**
     * The holding's effective value on [list] with where it comes from (view 6's labels); the source's text maps through
     * the remembered texts first (25).
     */
    fun effective(
        list: ClassList,
        portfolioId: String,
        ticker: String,
    ): EffectiveClass {
        val override = overrides.firstOrNull { it.portfolioId == portfolioId && it.ticker == ticker && it.list == list }?.value
        return Classification.effective(lists[list], Classification.textOn(list, profiles[ticker]), override, lists.rememberedOn(list))
    }

    /** [valueOf] on [list], for K3's breakdowns. */
    fun of(list: ClassList): (Holding) -> String? = { valueOf(list, it) }
}

/** A slice's name: cash and the asset types take the approved words; a portfolio's name or a ticker is shown as it is. */
sealed interface SliceName {
    data object Cash : SliceName

    data class Type(
        val type: AssetType,
    ) : SliceName

    data class Named(
        val name: String,
    ) : SliceName

    data object Unclassified : SliceName
}

/**
 * One row: its name, its amount by largest remainder with the other rows (18 decision 3) and the digits for TalkBack, its
 * percentage with 1 decimal ("88.2"), shown in the row only under 5% (view 1), and [tenths], the share drawn on the donut
 * (0: no arc).
 */
data class AllocationRowModel(
    val name: SliceName,
    val amount: String,
    val spokenAmount: String,
    val percent: String,
    val percentInRow: Boolean,
    val tenths: Int,
)

/** One breakdown: its rows in order, adding up to the net worth shown and to 100.0%. */
data class AllocationModel(
    val by: AllocationBy,
    val rows: List<AllocationRowModel>,
)

object Allocations {
    /**
     * The breakdown [by] of cash as of today ([cashCents]) and [holdings] at [prices] or at cost, portfolios in the order
     * given (as created; 18 decision 2). Null while net worth shows as $0.00 (4.8; 17 decision 4).
     */
    fun of(
        by: AllocationBy,
        cashCents: Long,
        holdings: List<Holding>,
        portfolios: List<Portfolio>,
        prices: Map<String, Price>,
        types: Map<String, AssetType>,
        classes: HoldingClasses = HoldingClasses(),
    ): AllocationModel? {
        val cash = Fraction.ofCents(cashCents)
        val slices =
            when (by) {
                AllocationBy.ASSET_TYPE -> Allocation.byAssetType(cash, holdings, prices, types)
                AllocationBy.PORTFOLIO -> Allocation.byPortfolio(cash, portfolios, holdings, prices)
                AllocationBy.HOLDING -> Allocation.byHolding(cash, holdings, prices)
                AllocationBy.SECTOR -> Allocation.byClass(cash, holdings, prices, classes.lists.sectors, classes.of(ClassList.SECTOR))
                AllocationBy.GEOGRAPHY -> Allocation.byClass(cash, holdings, prices, classes.lists.geographies, classes.of(ClassList.GEOGRAPHY))
            }
        return model(by, slices, portfolios.associate { it.id to it.name })
    }

    /**
     * One portfolio's breakdown [by] asset type, holding, sector or geography (view 4; 19, 23b), its [holdings] at [prices]
     * or at cost, no cash.
     * Null for [AllocationBy.PORTFOLIO], which this level has not, and while the portfolio's value shows as $0.00.
     */
    fun ofPortfolio(
        by: AllocationBy,
        holdings: List<Holding>,
        prices: Map<String, Price>,
        types: Map<String, AssetType>,
        classes: HoldingClasses = HoldingClasses(),
    ): AllocationModel? {
        val slices =
            when (by) {
                AllocationBy.ASSET_TYPE -> Allocation.portfolioByAssetType(holdings, prices, types)
                AllocationBy.HOLDING -> Allocation.portfolioByHolding(holdings, prices)
                AllocationBy.SECTOR -> Allocation.portfolioByClass(holdings, prices, classes.lists.sectors, classes.of(ClassList.SECTOR))
                AllocationBy.GEOGRAPHY -> Allocation.portfolioByClass(holdings, prices, classes.lists.geographies, classes.of(ClassList.GEOGRAPHY))
                AllocationBy.PORTFOLIO -> return null
            }
        return model(by, slices, emptyMap())
    }

    /** The rows of [slices]: amounts and percentages by largest remainder; null while their total shows as $0.00. */
    private fun model(
        by: AllocationBy,
        slices: List<Slice>,
        names: Map<String, String>,
    ): AllocationModel? {
        val values = slices.map { it.value }
        if (values.fold(Fraction.ZERO) { sum, value -> sum + value }.rounded(2).signum() == 0) return null
        val amounts = Money.partsInCents(values).map { Money.format(it) }
        val tenths = Allocation.tenths(values)
        val rows =
            slices.indices.map { index ->
                AllocationRowModel(
                    name = nameOf(slices[index].key, names),
                    amount = amounts[index],
                    spokenAmount = amounts[index].substringAfter('$'),
                    percent = "${tenths[index] / 10}.${tenths[index] % 10}",
                    percentInRow = tenths[index] < ON_DONUT,
                    tenths = tenths[index],
                )
            }
        return AllocationModel(by, rows)
    }

    private fun nameOf(
        key: SliceKey,
        portfolioNames: Map<String, String>,
    ): SliceName = when (key) {
        SliceKey.Cash -> SliceName.Cash
        is SliceKey.Type -> SliceName.Type(key.type)
        is SliceKey.OfPortfolio -> SliceName.Named(portfolioNames[key.portfolioId].orEmpty())
        is SliceKey.OfTicker -> SliceName.Named(key.ticker)
        is SliceKey.OfClass -> SliceName.Named(key.value)
        SliceKey.Unclassified -> SliceName.Unclassified
    }

    // View 1: a slice of 5% or more shows its percentage on the donut, a smaller one in its row.
    private const val ON_DONUT = 50
}
