package app.ghghorg.atsassets.core

import java.math.BigDecimal
import java.time.LocalDate

/**
 * The code before iteration 15c, kept for tests only (15c decision 1): HoldingsComparisonTest compares the rewrite with
 * it, and HoldingsScaleTest times it. Copied from main at 02b24d5 (2026-11-07): Holdings (lots, holdings, lotsOf,
 * splitFactor, quantityAt, firstNegative, originalOf), Sells (lotChoices, pieces, realizedGain), Splits.effect,
 * Reversals (reversalOf, sharesRefusal, effect), Hiding (offered, unhiddenBy), Reentries (startingPicks,
 * startingQuantity) and Valuation.netWorth. The bodies are unchanged; only the calls between them point here, so the
 * copy never runs the new code. Never changed after 15c's step 2.
 */
internal object HoldingsReference {
    fun lots(
        transactions: List<Transaction>,
        asOf: LocalDate,
    ): List<Lot> {
        val at = Place.endOf(asOf)
        return transactions
            .filter { it.type == TransactionType.BUY && !it.date.isAfter(asOf) }
            .sortedWith(compareBy<Transaction>({ it.date }, { it.enteredSeq }))
            .map { buy ->
                val factor = splitFactor(transactions, buy.portfolioId, buy.ticker, buy.place, at)
                val remaining = quantityAt(transactions, buy, at)
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

    fun originalOf(
        transactions: List<Transaction>,
        reversal: Transaction,
    ): Transaction? = reversal.reversesId?.let { id -> transactions.firstOrNull { it.id == id } }

    fun splitFactor(
        transactions: List<Transaction>,
        portfolioId: String,
        ticker: String,
        after: Place,
        upTo: Place,
    ): Fraction = transactions
        .filter { it.portfolioId == portfolioId && it.ticker == ticker && it.place > after && it.place <= upTo }
        .fold(Fraction.ONE) { factor, transaction ->
            when (transaction.type) {
                TransactionType.SPLIT -> factor * (transaction.split?.factor ?: Fraction.ONE)

                TransactionType.REVERSAL -> {
                    val ratio = originalOf(transactions, transaction)?.takeIf { it.type == TransactionType.SPLIT }?.split
                    if (ratio == null) factor else factor / ratio.factor
                }

                else -> factor
            }
        }

    fun quantityAt(
        transactions: List<Transaction>,
        buy: Transaction,
        at: Place,
    ): Fraction {
        fun since(from: Place) = splitFactor(transactions, buy.portfolioId, buy.ticker, from, at)

        fun taken(sell: Transaction): Fraction {
            val quantity = sell.allocations.filter { it.lotId == buy.id }.fold(Fraction.ZERO) { sum, it -> sum + it.quantity }
            return quantity * since(sell.place)
        }
        val bought = buy.boughtQuantity * since(buy.place)
        return transactions
            .filter { it.place <= at && (it.type == TransactionType.SELL || it.type == TransactionType.REVERSAL) }
            .fold(bought) { left, transaction ->
                if (transaction.type == TransactionType.SELL) return@fold left - taken(transaction)
                val original = originalOf(transactions, transaction)
                when {
                    original == null -> left
                    original.type == TransactionType.SELL -> left + taken(original)
                    original.id == buy.id -> left - bought
                    else -> left
                }
            }
    }

    fun firstNegative(
        transactions: List<Transaction>,
        portfolioId: String,
        ticker: String,
    ): Shortfall? {
        val ofHolding = transactions.filter { it.portfolioId == portfolioId && it.ticker == ticker }
        val buys = ofHolding.filter { it.type == TransactionType.BUY }
        for (place in ofHolding.map { it.place }.sorted()) {
            for (buy in buys.filter { it.place <= place }) {
                val held = quantityAt(transactions, buy, place)
                if (held.signum() < 0) return Shortfall(place, buy.id, held)
            }
        }
        return null
    }

    fun lotsOf(
        transactions: List<Transaction>,
        portfolioId: String,
        ticker: String,
        asOf: LocalDate,
    ): List<Lot> = lots(transactions, asOf).filter {
        it.portfolioId == portfolioId && it.ticker == ticker && it.remainingQuantity.signum() > 0
    }

    fun holdings(
        transactions: List<Transaction>,
        asOf: LocalDate,
    ): List<Holding> = lots(transactions, asOf)
        .groupBy { it.portfolioId to it.ticker }
        .map { (key, lots) ->
            Holding(
                portfolioId = key.first,
                ticker = key.second,
                shares = lots.fold(Fraction.ZERO) { sum, lot -> sum + lot.remainingQuantity },
                cost = lots.fold(Fraction.ZERO) { sum, lot -> sum + lot.cost },
            )
        }.sortedWith(compareBy<Holding>({ it.portfolioId }, { it.ticker }))

    fun netWorth(
        entries: List<CashEntry>,
        transactions: List<Transaction>,
        today: LocalDate,
    ): Fraction {
        val cash = Fraction.ofCents(CashRules.balanceOn(entries, today))
        return holdings(transactions, today).fold(cash) { sum, holding -> sum + holding.cost }
    }

    fun lotChoices(
        transactions: List<Transaction>,
        portfolioId: String,
        ticker: String,
        sellDate: LocalDate,
        sellSeq: Long,
    ): List<LotChoice> {
        val sellPlace = Place(sellDate, sellSeq)
        val places = transactions.filter { it.portfolioId == portfolioId && it.ticker == ticker }.map { it.place }
        return transactions
            .filter { it.type == TransactionType.BUY && it.portfolioId == portfolioId && it.ticker == ticker }
            .sortedWith(compareBy<Transaction>({ it.date }, { it.enteredSeq }))
            .map { buy ->
                val from = maxOf(buy.place, sellPlace)
                val before = splitFactor(transactions, portfolioId, ticker, buy.place, from)
                val least =
                    (listOf(from) + places.filter { it > from }).minOf { place ->
                        quantityAt(transactions, buy, place) / splitFactor(transactions, portfolioId, ticker, from, place)
                    }
                LotChoice(
                    buyId = buy.id,
                    buyDate = buy.date,
                    enteredSeq = buy.enteredSeq,
                    available = least,
                    costPerShare = Fraction.ofCents(buy.totalCents) / (buy.boughtQuantity * before),
                    unavailable =
                    when {
                        buy.date.isAfter(sellDate) -> LotUnavailable.BOUGHT_AFTER_SELL_DATE
                        buy.date == sellDate && buy.enteredSeq >= sellSeq -> LotUnavailable.ENTERED_AFTER_SELL
                        else -> null
                    },
                )
            }.filter { it.available.signum() > 0 }
    }

    fun pieces(
        transactions: List<Transaction>,
        sell: Transaction,
    ): List<SoldPiece> {
        val buys = transactions.filter { it.type == TransactionType.BUY }.associateBy { it.id }
        val sumTaken = sell.allocations.fold(Fraction.ZERO) { sum, allocation -> sum + allocation.quantity }
        return sell.allocations.map { allocation ->
            val buy = buys.getValue(allocation.lotId)
            val factor = splitFactor(transactions, buy.portfolioId, buy.ticker, buy.place, sell.place)
            SoldPiece(
                lotId = allocation.lotId,
                buyDate = buy.date,
                quantity = allocation.quantity,
                cost = Fraction.ofCents(buy.totalCents) * allocation.quantity / (buy.boughtQuantity * factor),
                proceeds = Fraction.ofCents(sell.totalCents) * allocation.quantity / sumTaken,
            )
        }
    }

    fun realizedGain(pieces: List<SoldPiece>): Fraction = pieces.fold(Fraction.ZERO) { sum, piece -> sum + piece.gain }

    fun splitEffect(
        transactions: List<Transaction>,
        split: Transaction,
    ): SplitEffect {
        val justBefore = Place(split.date, split.enteredSeq - 1)
        val before =
            transactions
                .filter { it.type == TransactionType.BUY && it.portfolioId == split.portfolioId && it.ticker == split.ticker }
                .filter { it.place <= justBefore }
                .fold(Fraction.ZERO) { sum, buy -> sum + quantityAt(transactions, buy, justBefore) }
        return SplitEffect(before, before * (split.split?.factor ?: Fraction.ONE))
    }

    fun reversalOf(
        transactions: List<Transaction>,
        originalId: String,
    ): Transaction? = transactions.firstOrNull { it.type == TransactionType.REVERSAL && it.reversesId == originalId }

    fun sharesRefusal(
        transactions: List<Transaction>,
        original: Transaction,
        reversal: Transaction,
        short: Shortfall,
    ): ReversalRefusal {
        if (original.type != TransactionType.BUY) return ReversalRefusal.SellShort(short.place.date)
        val sells =
            transactions
                .filter { sell -> sell.type == TransactionType.SELL && sell.allocations.any { it.lotId == original.id } }
                .sortedWith(compareBy<Transaction>({ it.date }, { it.enteredSeq }))
        val notReversed = sells.firstOrNull { reversalOf(transactions, it.id) == null }
        if (notReversed != null) return ReversalRefusal.SellNotReversed(short.place.date, short.held, notReversed.date)
        val earliest = sells.mapNotNull { reversalOf(transactions, it.id)?.date }.maxOrNull() ?: short.place.date
        return ReversalRefusal.TooEarly(short.place.date, short.held, reversal.date, earliest)
    }

    fun reversalEffect(
        transactions: List<Transaction>,
        reversal: Transaction,
    ): ReversalEffect? {
        val original = originalOf(transactions, reversal) ?: return null
        fun since(from: Place) = splitFactor(transactions, original.portfolioId, original.ticker, from, reversal.place)
        return when (original.type) {
            TransactionType.BUY ->
                ReversalEffect.OfBuy(sharesRemoved = original.boughtQuantity * since(original.place), cashCents = original.totalCents)

            TransactionType.SELL -> {
                val buys = transactions.filter { it.type == TransactionType.BUY }.associateBy { it.id }
                ReversalEffect.OfSell(
                    lots =
                    original.allocations.mapNotNull { allocation ->
                        buys[allocation.lotId]?.let { RestoredLot(it.id, it.date, allocation.quantity * since(original.place)) }
                    },
                    cashCents = -original.totalCents,
                    realizedRemoved = realizedGain(pieces(transactions, original)),
                )
            }

            TransactionType.SPLIT -> {
                val ratio = original.split ?: return null
                val justBefore = Place(reversal.date, reversal.enteredSeq - 1)
                val before =
                    transactions
                        .filter { it.type == TransactionType.BUY && it.portfolioId == original.portfolioId && it.ticker == original.ticker }
                        .filter { it.place <= justBefore }
                        .fold(Fraction.ZERO) { sum, buy -> sum + quantityAt(transactions, buy, justBefore) }
                val inverse = SplitRatio(ratio.from, ratio.to)
                ReversalEffect.OfSplit(inverse = inverse, before = before, after = before * inverse.factor)
            }

            TransactionType.REVERSAL -> null
        }
    }

    fun hidingOffered(
        transactions: List<Transaction>,
        hidden: Set<HiddenHolding>,
        portfolioId: String,
        ticker: String,
    ): Boolean {
        if (HiddenHolding(portfolioId, ticker) in hidden) return false
        val shares = sharesAfterEveryEntry(transactions, portfolioId, ticker) ?: return false
        return shares.signum() == 0
    }

    fun unhiddenBy(
        transactions: List<Transaction>,
        hidden: Collection<HiddenHolding>,
    ): List<HiddenHolding> = hidden.filter { (sharesAfterEveryEntry(transactions, it.portfolioId, it.ticker)?.signum() ?: 0) > 0 }

    private fun sharesAfterEveryEntry(
        transactions: List<Transaction>,
        portfolioId: String,
        ticker: String,
    ): Fraction? = holdings(transactions, LocalDate.MAX).firstOrNull { it.portfolioId == portfolioId && it.ticker == ticker }?.shares

    fun startingPicks(
        transactions: List<Transaction>,
        original: Transaction,
        sellDate: LocalDate,
        sellSeq: Long,
    ): List<Pick> {
        val factor = splitFactor(transactions, original.portfolioId, original.ticker, original.place, Place(sellDate, sellSeq))
        return original.allocations.map { Pick(it.lotId, Sells.shown(it.quantity * factor)) }
    }

    fun startingQuantity(
        transactions: List<Transaction>,
        original: Transaction,
        date: LocalDate,
        seq: Long,
    ): BigDecimal {
        val factor = splitFactor(transactions, original.portfolioId, original.ticker, original.place, Place(date, seq))
        val exact = original.boughtQuantity * factor
        return if (exact == Fraction.of(original.quantity)) original.quantity else Sells.shown(exact)
    }
}
