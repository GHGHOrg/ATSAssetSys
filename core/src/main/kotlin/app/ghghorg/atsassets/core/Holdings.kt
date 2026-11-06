package app.ghghorg.atsassets.core

import java.math.BigDecimal
import java.time.LocalDate

/**
 * A lot (spec section 3), worked out from its buy by replay; there is no lot table (09 decision 7). Sells (11)
 * and splits (12) change the remaining quantity; the cost is the buy's total scaled by remaining over the original
 * quantity with the splits since (4.5: a split never changes a lot's total cost). A sold-out lot has 0 left and cost 0.
 */
data class Lot(
    val buyId: String,
    val portfolioId: String,
    val ticker: String,
    val buyDate: LocalDate,
    val enteredSeq: Long,
    val originalQuantity: BigDecimal,
    val remainingQuantity: Fraction,
    val cost: Fraction,
    val originalCost: Fraction,
    val splitFactor: Fraction,
    val boughtQuantity: Fraction = Fraction.of(originalQuantity),
) {
    /** The buy's total over its exact quantity with the splits since (spec 3, 4.10); a sell does not change it. */
    val costPerShare: Fraction get() = originalCost / (boughtQuantity * splitFactor)
}

/** A holding: a ticker within a portfolio (spec section 3), with the shares and cost of its lots (4.8). */
data class Holding(
    val portfolioId: String,
    val ticker: String,
    val shares: Fraction,
    val cost: Fraction,
)

/** A place in the 4.1 order: a date, then the order of entry. */
internal data class Place(
    val date: LocalDate,
    val seq: Long,
) : Comparable<Place> {
    override fun compareTo(other: Place): Int = compareValuesBy(this, other, { it.date }, { it.seq })

    companion object {
        /** After everything dated [date]. */
        fun endOf(date: LocalDate): Place = Place(date, Long.MAX_VALUE)

        /** After everything. */
        val END: Place = endOf(LocalDate.MAX)
    }
}

internal val Transaction.place: Place get() = Place(date, enteredSeq)

/** Where a lot would go below 0 (4.1): the place, the lot's buy id and what it would hold there, in that place's units. */
internal data class Shortfall(
    val place: Place,
    val lotId: String,
    val held: Fraction,
)

/**
 * One transaction list, indexed once (iteration 15c; open item 45): a lot's quantity is worked out from the entries that
 * touch it, and a holding's splits from a running product, not from every transaction on every call (15c section 4).
 * The results are exactly those of working them out from the whole list (HoldingsComparisonTest compares them with the
 * code before 15c). Build one for a list and ask it as many questions as needed; the list must not change meanwhile.
 */
internal class Ledger(
    private val transactions: List<Transaction>,
) {
    // The first transaction with each id, as Holdings.originalOf finds it.
    private val byId: Map<String, Transaction> by lazy(LazyThreadSafetyMode.NONE) {
        val map = HashMap<String, Transaction>()
        for (transaction in transactions) map.putIfAbsent(transaction.id, transaction)
        map
    }

    // Each holding's transactions, in the order of the list.
    private val ofHolding: Map<Pair<String, String>, List<Transaction>> by lazy(LazyThreadSafetyMode.NONE) {
        transactions.groupBy { it.portfolioId to it.ticker }
    }

    private val steps = HashMap<Pair<String, String>, Steps>()

    // What touches each lot, by its buy's id, from every holding, as the lot's quantity counts it (4.4, 4.6).
    private val touches: Map<String, Touches> by lazy(LazyThreadSafetyMode.NONE) {
        val map = HashMap<String, Touches>()
        for (transaction in transactions) {
            when (transaction.type) {
                TransactionType.SELL ->
                    for ((lotId, quantity) in takenByLot(transaction)) {
                        map.getOrPut(lotId) { Touches() }.sells += Touch(transaction.place, transaction.place, quantity)
                    }

                TransactionType.REVERSAL -> {
                    val original = originalOf(transaction) ?: continue
                    if (original.type == TransactionType.SELL) {
                        for ((lotId, quantity) in takenByLot(original)) {
                            map.getOrPut(lotId) { Touches() }.sellReversals += Touch(transaction.place, original.place, quantity)
                        }
                    } else {
                        map.getOrPut(original.id) { Touches() }.reversals += transaction.place
                    }
                }

                else -> Unit
            }
        }
        map
    }

    /** The transaction [reversal] reverses, if it is a reversal and its original is there (4.6). */
    fun originalOf(reversal: Transaction): Transaction? = reversal.reversesId?.let { byId[it] }

    /** As [Holdings.splitFactor]. */
    fun splitFactor(
        portfolioId: String,
        ticker: String,
        after: Place,
        upTo: Place,
    ): Fraction = stepsOf(portfolioId, ticker).factor(after, upTo)

    /**
     * As [Holdings.quantityAt]: the lot's quantity as traded with the splits since, less each sell from it placed up to
     * [at], plus each reversal of such a sell placed up to [at], each with the splits since the sell, less the whole
     * quantity for each reversal of the buy itself placed up to [at] (4.6, 4.10).
     */
    fun quantityAt(
        buy: Transaction,
        at: Place,
    ): Fraction {
        val splits = stepsOf(buy.portfolioId, buy.ticker)
        val bought = buy.boughtQuantity * splits.factor(buy.place, at)
        val touched = touches[buy.id] ?: return bought
        var left = bought
        for (sell in touched.sells) if (sell.place <= at) left -= sell.quantity * splits.factor(sell.anchor, at)
        for (back in touched.sellReversals) if (back.place <= at) left += back.quantity * splits.factor(back.anchor, at)
        for (place in touched.reversals) if (place <= at) left -= bought
        return left
    }

    /**
     * As [Holdings.firstNegative]. A split multiplies every lot by a number above 0, so a lot's sign can change only at
     * a place that one of its own entries starts: its buy, a sell from it, the reversal of such a sell (and that sell's
     * place, where the splits it counts start), or its own reversal. Each lot is checked at the first place of the
     * holding from each of those on, not at every place. With a factor that is not above 0, which K1 and the loader do
     * not allow, every place is checked, as before 15c.
     */
    fun firstNegative(
        portfolioId: String,
        ticker: String,
    ): Shortfall? {
        val entries = ofHolding[portfolioId to ticker].orEmpty()
        val buys = entries.filter { it.type == TransactionType.BUY }
        val places = entries.map { it.place }.sorted()
        if (!stepsOf(portfolioId, ticker).positive) return everyPlace(buys, places)
        var found: Shortfall? = null
        for (buy in buys) {
            val place = firstNegativePlace(buy, places) ?: continue
            // On a shared place the earlier buy in the list wins, as in the check of every place.
            if (found == null || place < found.place) found = Shortfall(place, buy.id, quantityAt(buy, place))
        }
        return found
    }

    /** As [Holdings.lots]; with [holding], only that holding's lots, in the same order. */
    fun lots(
        asOf: LocalDate,
        holding: Pair<String, String>? = null,
    ): List<Lot> {
        val at = Place.endOf(asOf)
        val source = if (holding == null) transactions else ofHolding[holding].orEmpty()
        return source
            .filter { it.type == TransactionType.BUY && !it.date.isAfter(asOf) }
            .sortedWith(compareBy<Transaction>({ it.date }, { it.enteredSeq }))
            .map { buy ->
                val factor = splitFactor(buy.portfolioId, buy.ticker, buy.place, at)
                val remaining = quantityAt(buy, at)
                Lot(
                    buyId = buy.id,
                    portfolioId = buy.portfolioId,
                    ticker = buy.ticker,
                    buyDate = buy.date,
                    enteredSeq = buy.enteredSeq,
                    originalQuantity = buy.quantity,
                    remainingQuantity = remaining,
                    cost = Fraction.ofCents(buy.totalCents) * remaining / (buy.boughtQuantity * factor),
                    originalCost = Fraction.ofCents(buy.totalCents),
                    splitFactor = factor,
                    boughtQuantity = buy.boughtQuantity,
                )
            }
    }

    /** As [Holdings.holdings]. */
    fun holdings(asOf: LocalDate): List<Holding> = lots(asOf)
        .groupBy { it.portfolioId to it.ticker }
        .map { (key, lots) ->
            Holding(
                portfolioId = key.first,
                ticker = key.second,
                shares = lots.fold(Fraction.ZERO) { sum, lot -> sum + lot.remainingQuantity },
                cost = lots.fold(Fraction.ZERO) { sum, lot -> sum + lot.cost },
            )
        }.sortedWith(compareBy<Holding>({ it.portfolioId }, { it.ticker }))

    private fun stepsOf(
        portfolioId: String,
        ticker: String,
    ): Steps = steps.getOrPut(portfolioId to ticker) { Steps(ofHolding[portfolioId to ticker].orEmpty()) }

    // The first place of [places], sorted, at or after [place]; null when there is none.
    private fun firstAtOrAfter(
        places: List<Place>,
        place: Place,
    ): Place? {
        var low = 0
        var high = places.size
        while (low < high) {
            val middle = (low + high) / 2
            if (places[middle] < place) low = middle + 1 else high = middle
        }
        return places.getOrNull(low)
    }

    // The sign of a lot stays the same from one of its marks to the next, so the first place of the holding in each
    // stretch decides it.
    private fun firstNegativePlace(
        buy: Transaction,
        places: List<Place>,
    ): Place? {
        val touched = touches[buy.id]
        val marks =
            buildList {
                add(buy.place)
                touched?.sells?.forEach { add(it.place) }
                touched?.sellReversals?.forEach {
                    add(it.place)
                    add(it.anchor)
                }
                touched?.reversals?.let { addAll(it) }
            }.distinct().sorted()
        for ((index, mark) in marks.withIndex()) {
            val place = firstAtOrAfter(places, maxOf(mark, buy.place)) ?: return null
            val next = marks.getOrNull(index + 1)
            if (next != null && place >= next) continue
            if (quantityAt(buy, place).signum() < 0) return place
        }
        return null
    }

    // The check before 15c: every place of the holding, each lot bought by then.
    private fun everyPlace(
        buys: List<Transaction>,
        places: List<Place>,
    ): Shortfall? {
        for (place in places) {
            for (buy in buys.filter { it.place <= place }) {
                val held = quantityAt(buy, place)
                if (held.signum() < 0) return Shortfall(place, buy.id, held)
            }
        }
        return null
    }

    // What a sell took from each lot, summed by lot (a lot appears once in a saved sell).
    private fun takenByLot(sell: Transaction): Map<String, Fraction> = sell.allocations
        .groupBy { it.lotId }
        .mapValues { (_, allocations) -> allocations.fold(Fraction.ZERO) { sum, it -> sum + it.quantity } }

    // What a split or a split's reversal multiplies a quantity by (4.5, 4.6); 1 for any other transaction.
    private fun factorOf(transaction: Transaction): Fraction = when (transaction.type) {
        TransactionType.SPLIT -> transaction.split?.factor ?: Fraction.ONE

        TransactionType.REVERSAL ->
            originalOf(transaction)?.takeIf { it.type == TransactionType.SPLIT }?.split?.let { Fraction.ONE / it.factor } ?: Fraction.ONE

        else -> Fraction.ONE
    }

    // A place where a lot is touched, and the place its splits are counted from (a sell's own place, also for its
    // reversal), with the quantity in that sell's units.
    private class Touch(
        val place: Place,
        val anchor: Place,
        val quantity: Fraction,
    )

    private class Touches {
        val sells = ArrayList<Touch>()
        val sellReversals = ArrayList<Touch>()
        val reversals = ArrayList<Place>()
    }

    /**
     * One holding's splits and splits' reversals in the 4.1 order, with the running product of their factors, so the
     * factor between two places is one division (4.5). A factor that is not above 0, or cannot be worked out, leaves
     * no running product, and the factors in between are then multiplied one by one, as before 15c.
     */
    private inner class Steps(
        holding: List<Transaction>,
    ) {
        private val entries =
            holding.filter { it.type == TransactionType.SPLIT || it.type == TransactionType.REVERSAL }.sortedBy { it.place }
        private val places = entries.map { it.place }
        private val running: List<Fraction>? =
            try {
                val products = ArrayList<Fraction>(entries.size + 1)
                products += Fraction.ONE
                for (entry in entries) {
                    val factor = factorOf(entry)
                    if (factor.signum() <= 0) break
                    products += products.last() * factor
                }
                if (products.size == entries.size + 1) products else null
            } catch (e: IllegalArgumentException) {
                null
            }

        /** Every factor is above 0: a split never changes a lot's sign. */
        val positive: Boolean get() = running != null

        fun factor(
            after: Place,
            upTo: Place,
        ): Fraction {
            if (upTo <= after) return Fraction.ONE
            val products =
                running ?: return entries
                    .filter { it.place > after && it.place <= upTo }
                    .fold(Fraction.ONE) { factor, entry -> factor * factorOf(entry) }
            val to = countUpTo(upTo)
            val from = countUpTo(after)
            return if (to == from) Fraction.ONE else products[to] / products[from]
        }

        // How many entries are placed at or before [place].
        private fun countUpTo(place: Place): Int {
            var low = 0
            var high = places.size
            while (low < high) {
                val middle = (low + high) / 2
                if (places[middle] <= place) low = middle + 1 else high = middle
            }
            return low
        }
    }
}

/** K3, read only: lots and holdings from the transactions, as of a date (4.8). */
object Holdings {
    /**
     * The lots of the buys dated on or before [asOf], in the 4.1 order (date, then entered_seq), with the splits and
     * sells dated on or before [asOf] applied. Sold-out lots are included, at 0.
     */
    fun lots(
        transactions: List<Transaction>,
        asOf: LocalDate,
    ): List<Lot> = Ledger(transactions).lots(asOf)

    /** The transaction [reversal] reverses, if it is a reversal and its original is there (4.6). */
    internal fun originalOf(
        transactions: List<Transaction>,
        reversal: Transaction,
    ): Transaction? = reversal.reversesId?.let { id -> transactions.firstOrNull { it.id == id } }

    /**
     * The product of the ratios of one holding's splits placed after [after] and not after [upTo] (4.5): what a
     * quantity in the units of [after] is multiplied by to be in the units of [upTo]. A split's reversal counts as a
     * split with the inverse ratio at its own place (4.6).
     */
    internal fun splitFactor(
        transactions: List<Transaction>,
        portfolioId: String,
        ticker: String,
        after: Place,
        upTo: Place,
    ): Fraction = Ledger(transactions).splitFactor(portfolioId, ticker, after, upTo)

    /**
     * What the lot of [buy] holds at [at], in the units of [at] (4.10): its quantity as traded with the splits since,
     * less what each sell placed up to [at] took from it and plus what each reversal of such a sell put back, each
     * allocation (in its sell's units) with the splits since the sell; a reversal of the buy itself takes its whole
     * quantity, with the splits since, back out (4.6).
     */
    internal fun quantityAt(
        transactions: List<Transaction>,
        buy: Transaction,
        at: Place,
    ): Fraction = Ledger(transactions).quantityAt(buy, at)

    /**
     * The first place, in the 4.1 order, where one of the holding's lots would be below 0 (4.1): the place, the lot and
     * what it would hold there. Null when every lot stays at or above 0.
     */
    internal fun firstNegative(
        transactions: List<Transaction>,
        portfolioId: String,
        ticker: String,
    ): Shortfall? = Ledger(transactions).firstNegative(portfolioId, ticker)

    /**
     * One holding's transactions for Holding detail (view 6): newest first, and on a shared date the later entry first,
     * as the Cash list (view 7). Entries dated after today are listed too: a history shows everything.
     */
    fun transactionsOf(
        transactions: List<Transaction>,
        portfolioId: String,
        ticker: String,
    ): List<Transaction> = transactions
        .filter { it.portfolioId == portfolioId && it.ticker == ticker }
        .sortedWith(compareByDescending<Transaction> { it.date }.thenByDescending { it.enteredSeq })

    /** One holding's lots that still have shares as of [asOf], oldest first (4.4, view 6: sold-out lots are not listed). */
    fun lotsOf(
        transactions: List<Transaction>,
        portfolioId: String,
        ticker: String,
        asOf: LocalDate,
    ): List<Lot> = Ledger(transactions).lots(asOf, portfolioId to ticker).filter {
        it.portfolioId == portfolioId && it.ticker == ticker && it.remainingQuantity.signum() > 0
    }

    /**
     * One holding per portfolio and ticker, with the sums of its lots; by portfolio id, then in ticker order. A sold-out
     * holding stays, at 0 shares and cost 0 (4.4, view 3).
     */
    fun holdings(
        transactions: List<Transaction>,
        asOf: LocalDate,
    ): List<Holding> = Ledger(transactions).holdings(asOf)
}
