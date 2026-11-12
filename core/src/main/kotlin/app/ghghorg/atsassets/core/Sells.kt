package app.ghghorg.atsassets.core

import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale

/** One lot pick on a sell: the lot (its buy's id) and the quantity as typed, up to 8 decimals (4.4). */
data class Pick(
    val lotId: String,
    val quantity: BigDecimal,
)

/** Why a lot cannot be picked for a sell on a date (4.4 lot rules). */
enum class LotUnavailable {
    BOUGHT_AFTER_SELL_DATE,
    ENTERED_AFTER_SELL,
}

/**
 * A lot as the lot picker lists it (4.4): what it has left for a pick (the least it holds on the sell date or any later
 * date, exact), its cost per share, and why it cannot be picked, if it cannot.
 */
data class LotChoice(
    val buyId: String,
    val buyDate: LocalDate,
    val enteredSeq: Long,
    val available: Fraction,
    val costPerShare: Fraction,
    val unavailable: LotUnavailable?,
)

/** One piece of a sell: the exact quantity taken from a lot, its cost share and its proceeds share (4.4, 4.10). */
data class SoldPiece(
    val lotId: String,
    val buyDate: LocalDate,
    val quantity: Fraction,
    val cost: Fraction,
    val proceeds: Fraction,
) {
    val gain: Fraction get() = proceeds - cost
}

/** Why a sell was refused (spec 4.1, 4.2, 4.4). Reasons only; the wording is D9. */
sealed interface SellRefusal {
    data object NoOpeningBalance : SellRefusal

    data object PortfolioNotFound : SellRefusal

    data object TickerEmpty : SellRefusal

    data object QuantityNotPositive : SellRefusal

    data object TotalNegative : SellRefusal

    data class BeforeOpeningBalance(
        val openingDate: LocalDate,
    ) : SellRefusal

    /**
     * The shares the portfolio can sell on [date]: the lots that pass the lot rules, each at its remainder to 8 places
     * (4.4), and the sell quantity.
     */
    data class MoreThanHeld(
        val date: LocalDate,
        val held: Fraction,
        val quantity: BigDecimal,
    ) : SellRefusal

    /** Not a lot of this holding: an unknown id, another portfolio or ticker, or a lot picked twice. */
    data class PickNotALot(
        val lotId: String,
    ) : SellRefusal

    data class PickNotAvailable(
        val lotId: String,
        val reason: LotUnavailable,
    ) : SellRefusal

    data class PickNotPositive(
        val lotId: String,
    ) : SellRefusal

    /** The pick is above what the lot has left, compared at 8 places (4.4). */
    data class PickOverRemainder(
        val lotId: String,
        val remainder: Fraction,
    ) : SellRefusal

    data class PicksDoNotAddUp(
        val picked: BigDecimal,
        val quantity: BigDecimal,
    ) : SellRefusal

    data class NegativeCash(
        val date: LocalDate,
        val shortfallCents: Long,
    ) : SellRefusal

    data object TooLarge : SellRefusal
}

sealed interface SellResult {
    /** The sell (with its allocations) and its trade line, to be saved together as one action. */
    data class Created(
        val transaction: Transaction,
        val cashLine: CashEntry,
    ) : SellResult

    data class Refused(
        val reason: SellRefusal,
    ) : SellResult
}

/** K1: sells (spec 4.1, 4.2, 4.4, 4.10). The ticker check of 4.3 never applies to a sell (4.6). */
object Sells {
    /**
     * The lots of one holding for a sell dated [sellDate] with entry order [sellSeq], oldest first (4.4). A lot's
     * available quantity is the least it holds on the sell date or any later date, in the units of the sell date: the
     * least over the sell's place and every later entry of the holding, each taken back through the splits since the
     * sell. A reversal of a sell can raise a lot again (13b), so the last figure is not always the least. A lot bought
     * after the sell is shown in its own units. Lots with nothing available are left out; lots that fail the lot rules
     * stay in their place with the reason.
     */
    fun lotChoices(
        transactions: List<Transaction>,
        portfolioId: String,
        ticker: String,
        sellDate: LocalDate,
        sellSeq: Long,
    ): List<LotChoice> {
        val sellPlace = Place(sellDate, sellSeq)
        val ledger = Ledger(transactions)
        return transactions
            .filter { it.type == TransactionType.BUY && it.portfolioId == portfolioId && it.ticker == ticker }
            .sortedWith(compareBy<Transaction>({ it.date }, { it.enteredSeq }))
            .map { buy ->
                val from = maxOf(buy.place, sellPlace)
                val before = ledger.splitFactor(portfolioId, ticker, buy.place, from)
                val least = ledger.leastHeld(buy, from)
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

    /**
     * What a quantity in the units of a sell dated [sellDate] with entry order [sellSeq] (as [lotChoices] gives it) is
     * multiplied by to be in today's shares (16c decision 4): the splits after the sell and up to the end of [today], or,
     * for a sell dated after today, the inverse of those after today and up to the sell. 1 with no split between.
     */
    fun unitsToToday(
        transactions: List<Transaction>,
        portfolioId: String,
        ticker: String,
        sellDate: LocalDate,
        sellSeq: Long,
        today: LocalDate,
    ): Fraction {
        val sell = Place(sellDate, sellSeq)
        val now = Place.endOf(today)
        return if (sell <= now) {
            Holdings.splitFactor(transactions, portfolioId, ticker, sell, now)
        } else {
            Fraction.ONE / Holdings.splitFactor(transactions, portfolioId, ticker, now, sell)
        }
    }

    /**
     * A lot choice's unrealized gain at [price] (4.4, 4.8; 16c decision 4): its available quantity in today's shares
     * ([toToday] from [unitsToToday]) × the price, less what that quantity cost; exact (4.10). Null with no price.
     */
    fun choiceGain(
        choice: LotChoice,
        toToday: Fraction,
        price: Price?,
    ): Fraction? = price?.let { choice.available * toToday * Fraction.of(it.perShare) - choice.available * choice.costPerShare }

    /** Sell all (4.4): every lot that passes the lot rules, each at its available quantity rounded to 8 places. */
    fun sellAll(choices: List<LotChoice>): List<Pick> = choices
        .filter { it.unavailable == null }
        .map { Pick(it.buyId, shown(it.available)) }
        .filter { it.quantity.signum() > 0 }

    /** A quantity as the lot picker shows it: [Quantity.shown], trailing zeros dropped (4.10). */
    fun shown(quantity: Fraction): BigDecimal {
        val stripped = Quantity.shown(quantity).stripTrailingZeros()
        return if (stripped.scale() < 0) stripped.setScale(0) else stripped
    }

    /**
     * A sell and its `trade` cash line, which puts the total into cash on the trade date (4.2). Both carry one
     * entered_seq. The first rule that fails decides the reason: opening balance, portfolio, ticker, quantity, total,
     * date, more than held, each pick in turn, the picks' sum, then the cash replay (4.1). A pick equal to its lot's
     * available quantity at 8 places takes the exact remainder (4.4, 4.10). The allocations are kept oldest lot first.
     */
    fun sell(
        cashEntries: List<CashEntry>,
        transactions: List<Transaction>,
        portfolios: List<Portfolio>,
        transactionId: String,
        cashEntryId: String,
        portfolioId: String,
        ticker: String,
        date: LocalDate,
        quantity: BigDecimal,
        totalCents: Long,
        picks: List<Pick>,
    ): SellResult {
        val refusal =
            when {
                cashEntries.none { it.type == CashType.OPENING_BALANCE } -> SellRefusal.NoOpeningBalance
                portfolios.none { it.id == portfolioId } -> SellRefusal.PortfolioNotFound
                ticker.isBlank() -> SellRefusal.TickerEmpty
                quantity.signum() <= 0 -> SellRefusal.QuantityNotPositive
                totalCents < 0L -> SellRefusal.TotalNegative
                else -> null
            }
        if (refusal != null) return SellResult.Refused(refusal)
        val openingDate = cashEntries.first { it.type == CashType.OPENING_BALANCE }.date
        if (date.isBefore(openingDate)) {
            return SellResult.Refused(SellRefusal.BeforeOpeningBalance(openingDate))
        }
        val seq = Transactions.nextSeq(cashEntries, transactions)
        val upperTicker = ticker.trim().uppercase(Locale.ROOT)
        val choices = lotChoices(transactions, portfolioId, upperTicker, date, seq)
        // What the picks can take: each lot that passes at its remainder to 8 places, the figure the picker shows and a
        // pick may reach (4.4; 12, fix after phone check 6). The exact sum can be below it when remainders round up.
        val held =
            choices
                .filter { it.unavailable == null }
                .fold(Fraction.ZERO) { sum, choice -> sum + Fraction.of(Quantity.shown(choice.available)) }
        if (Fraction.of(quantity) > held) {
            return SellResult.Refused(SellRefusal.MoreThanHeld(date, held, quantity))
        }
        val taken = mutableMapOf<String, Fraction>()
        for (pick in picks) {
            val pickRefusal = pickRefusal(transactions, portfolioId, upperTicker, choices, taken.keys, pick)
            if (pickRefusal != null) return SellResult.Refused(pickRefusal)
            val choice = choices.first { it.buyId == pick.lotId }
            val asShown = Quantity.shown(choice.available)
            taken[pick.lotId] = if (pick.quantity.compareTo(asShown) == 0) choice.available else Fraction.of(pick.quantity)
        }
        val picked = picks.fold(BigDecimal.ZERO) { sum, pick -> sum + pick.quantity }
        if (picked.compareTo(quantity) != 0) {
            return SellResult.Refused(SellRefusal.PicksDoNotAddUp(picked, quantity))
        }
        val transaction =
            Transaction(
                id = transactionId,
                portfolioId = portfolioId,
                ticker = upperTicker,
                type = TransactionType.SELL,
                date = date,
                enteredSeq = seq,
                quantity = quantity,
                totalCents = totalCents,
                allocations = choices.mapNotNull { choice -> taken[choice.buyId]?.let { SellAllocation(choice.buyId, it) } },
            )
        val cashLine =
            CashEntry(
                id = cashEntryId,
                date = date,
                amountCents = totalCents,
                type = CashType.TRADE,
                note = "",
                enteredSeq = seq,
                transactionId = transactionId,
            )
        return when (val replayed = CashRules.replay(cashEntries + cashLine, from = date)) {
            null -> SellResult.Created(transaction, cashLine)
            is CashEntryRefusal.NegativeCash -> SellResult.Refused(SellRefusal.NegativeCash(replayed.date, replayed.shortfallCents))
            else -> SellResult.Refused(SellRefusal.TooLarge)
        }
    }

    /** Null when [pick] passes: a lot of this holding, picked once, passing the lot rules, above 0, within what it has left. */
    private fun pickRefusal(
        transactions: List<Transaction>,
        portfolioId: String,
        ticker: String,
        choices: List<LotChoice>,
        alreadyPicked: Set<String>,
        pick: Pick,
    ): SellRefusal? {
        val choice = choices.firstOrNull { it.buyId == pick.lotId }
        val ofThisHolding =
            transactions.any {
                it.type == TransactionType.BUY && it.id == pick.lotId && it.portfolioId == portfolioId && it.ticker == ticker
            }
        return when {
            pick.lotId in alreadyPicked || !ofThisHolding -> SellRefusal.PickNotALot(pick.lotId)
            choice?.unavailable != null -> SellRefusal.PickNotAvailable(pick.lotId, choice.unavailable)
            pick.quantity.signum() <= 0 -> SellRefusal.PickNotPositive(pick.lotId)
            choice == null -> SellRefusal.PickOverRemainder(pick.lotId, Fraction.ZERO)
            pick.quantity > Quantity.shown(choice.available) -> SellRefusal.PickOverRemainder(pick.lotId, choice.available)
            else -> null
        }
    }

    /**
     * The pieces of a saved or reviewed [sell], in its allocation order (4.10): a piece's cost is the lot's cost per
     * share on the sell's date (the splits before the sell applied) times the exact quantity taken, and its proceeds
     * are the sell total times the quantity taken over the sum taken by the sell.
     */
    fun pieces(
        transactions: List<Transaction>,
        sell: Transaction,
    ): List<SoldPiece> = pieces(Ledger(transactions), transactions.filter { it.type == TransactionType.BUY }.associateBy { it.id }, sell)

    /** As [pieces], with the [ledger] and the [buys] by id built once for many sells (26: the realized gains report). */
    internal fun pieces(
        ledger: Ledger,
        buys: Map<String, Transaction>,
        sell: Transaction,
    ): List<SoldPiece> {
        val sumTaken = sell.allocations.fold(Fraction.ZERO) { sum, allocation -> sum + allocation.quantity }
        return sell.allocations.map { allocation ->
            val buy = buys.getValue(allocation.lotId)
            val factor = ledger.splitFactor(buy.portfolioId, buy.ticker, buy.place, sell.place)
            SoldPiece(
                lotId = allocation.lotId,
                buyDate = buy.date,
                quantity = allocation.quantity,
                cost = Fraction.ofCents(buy.totalCents) * allocation.quantity / (buy.boughtQuantity * factor),
                proceeds = Fraction.ofCents(sell.totalCents) * allocation.quantity / sumTaken,
            )
        }
    }

    /** The realized gain of a sell: the sum of its pieces' gains, exact (4.4, 4.10). */
    fun realizedGain(pieces: List<SoldPiece>): Fraction = pieces.fold(Fraction.ZERO) { sum, piece -> sum + piece.gain }
}
