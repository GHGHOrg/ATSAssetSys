package app.ghghorg.atsassets.core

import java.math.BigInteger

/**
 * What a slice of an allocation stands for (4.8): cash, an asset type, a portfolio, a ticker, or a sector or geography
 * value and the holdings with none (23b).
 */
sealed interface SliceKey {
    data object Cash : SliceKey

    data class Type(
        val type: AssetType,
    ) : SliceKey

    data class OfPortfolio(
        val portfolioId: String,
    ) : SliceKey

    data class OfTicker(
        val ticker: String,
    ) : SliceKey

    data class OfClass(
        val value: String,
    ) : SliceKey

    data object Unclassified : SliceKey
}

/** One slice of an allocation and its exact value (4.10). */
data class Slice(
    val key: SliceKey,
    val value: Fraction,
)

/*
 * K3's allocation of net worth (4.8, C12; 18): cash as of today and each holding at its price or at cost, sliced by
 * asset type, portfolio, ticker, or sector or geography (23b), with the shares in tenths of a percent by largest
 * remainder (section 6).
 */
object Allocation {
    /**
     * Cash, stocks and ETFs, always all three, even at $0.00 (view 1). A ticker's type is its instrument's ([types]); a
     * ticker without one counts as a stock (18 risk 4).
     */
    fun byAssetType(
        cash: Fraction,
        holdings: List<Holding>,
        prices: Map<String, Price>,
        types: Map<String, AssetType>,
    ): List<Slice> {
        val held = withShares(holdings)
        return listOf(Slice(SliceKey.Cash, cash)) +
            AssetType.entries.map { type ->
                Slice(SliceKey.Type(type), sum(held.filter { (types[it.ticker] ?: AssetType.STOCK) == type }, prices))
            }
    }

    /** Cash, then each of [portfolios] in the order given (as created; 18 decision 2); only what has value (view 1). */
    fun byPortfolio(
        cash: Fraction,
        portfolios: List<Portfolio>,
        holdings: List<Holding>,
        prices: Map<String, Price>,
    ): List<Slice> {
        val held = withShares(holdings)
        val slices = portfolios.map { portfolio -> Slice(SliceKey.OfPortfolio(portfolio.id), sum(held.filter { it.portfolioId == portfolio.id }, prices)) }
        return withValue(listOf(Slice(SliceKey.Cash, cash)) + slices)
    }

    /** Cash, then one slice per ticker over every portfolio (C12), by ticker (18 decision 2); only what has value (view 1). */
    fun byHolding(
        cash: Fraction,
        holdings: List<Holding>,
        prices: Map<String, Price>,
    ): List<Slice> {
        val slices =
            withShares(holdings)
                .groupBy { it.ticker }
                .toSortedMap()
                .map { (ticker, held) -> Slice(SliceKey.OfTicker(ticker), sum(held, prices)) }
        return withValue(listOf(Slice(SliceKey.Cash, cash)) + slices)
    }

    /**
     * One portfolio's [holdings] by asset type (4.8 portfolio level; view 4): stocks and ETFs, always both, even at $0.00;
     * no cash. As [byAssetType] without its cash slice.
     */
    fun portfolioByAssetType(
        holdings: List<Holding>,
        prices: Map<String, Price>,
        types: Map<String, AssetType>,
    ): List<Slice> = byAssetType(Fraction.ZERO, holdings, prices, types).filter { it.key != SliceKey.Cash }

    /**
     * One portfolio's [holdings], one slice per holding (C12), by ticker, only what has value (view 4); no cash. As
     * [byHolding] with no cash, which leaves cash out.
     */
    fun portfolioByHolding(
        holdings: List<Holding>,
        prices: Map<String, Price>,
    ): List<Slice> = byHolding(Fraction.ZERO, holdings, prices)

    /**
     * Cash, then one slice per sector or geography value (4.8, 4.9; 23b): each holding's value from [valueOf] (null when
     * it has none, C14), values that match (D7) merged in [list]'s spelling and order (decision 1), a value not on [list]
     * after them in the order of [holdings], then "Unclassified" last; only what has value (view 1). The value is each
     * holding's, not each ticker's, as an override (24) will be. Cash is never "Unclassified" (AC-32).
     */
    fun byClass(
        cash: Fraction,
        holdings: List<Holding>,
        prices: Map<String, Price>,
        list: List<String>,
        valueOf: (Holding) -> String?,
    ): List<Slice> {
        var names = list
        val sums = HashMap<String, Fraction>()
        var unclassified = Fraction.ZERO
        for (holding in withShares(holdings)) {
            val worth = Valuation.valued(holding, prices[holding.ticker]).value
            val text = valueOf(holding)
            names = Classification.withSource(names, text)
            val name = Classification.valueFor(names, text)
            if (name == null) unclassified += worth else sums[name] = (sums[name] ?: Fraction.ZERO) + worth
        }
        val slices = names.filter { it in sums }.map { Slice(SliceKey.OfClass(it), sums.getValue(it)) }
        return withValue(listOf(Slice(SliceKey.Cash, cash)) + slices + Slice(SliceKey.Unclassified, unclassified))
    }

    /** One portfolio's [holdings] by sector or geography (4.8 portfolio level; view 4): as [byClass] with no cash. */
    fun portfolioByClass(
        holdings: List<Holding>,
        prices: Map<String, Price>,
        list: List<String>,
        valueOf: (Holding) -> String?,
    ): List<Slice> = byClass(Fraction.ZERO, holdings, prices, list, valueOf)

    /**
     * Each value's share of their total in tenths of a percent (section 6: 1 decimal, exactly 100.0%): each rounded down,
     * the tenths left over to reach 1,000 given one each to the largest dropped remainders, ties to the earlier row. All
     * 0 when the total is 0 (4.8: no shares of $0.00).
     */
    fun tenths(values: List<Fraction>): List<Int> {
        val total = values.fold(Fraction.ZERO) { sum, value -> sum + value }
        if (total.signum() == 0) return values.map { 0 }
        val scaled = values.map { it * THOUSAND / total }
        val floors = scaled.map { it.numerator.divide(it.denominator) }
        val leftover = (THOUSAND_INT - floors.fold(BigInteger.ZERO) { sum, floor -> sum + floor }).toInt()
        val remainders = scaled.indices.map { scaled[it] - Fraction.of(floors[it], BigInteger.ONE) }
        val topped = scaled.indices.sortedWith(compareByDescending<Int> { remainders[it] }.thenBy { it }).take(leftover).toSet()
        return floors.mapIndexed { index, floor -> floor.toInt() + if (index in topped) 1 else 0 }
    }

    private fun withShares(holdings: List<Holding>): List<Holding> = holdings.filter { it.shares.signum() > 0 }

    private fun withValue(slices: List<Slice>): List<Slice> = slices.filter { it.value.signum() > 0 }

    private fun sum(
        holdings: List<Holding>,
        prices: Map<String, Price>,
    ): Fraction = holdings.fold(Fraction.ZERO) { sum, holding -> sum + Valuation.valued(holding, prices[holding.ticker]).value }

    private val THOUSAND_INT = BigInteger.valueOf(1_000L)
    private val THOUSAND = Fraction.of(THOUSAND_INT, BigInteger.ONE)
}
