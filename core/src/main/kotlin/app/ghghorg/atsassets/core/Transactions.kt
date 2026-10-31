package app.ghghorg.atsassets.core

import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale

/** The transaction types of spec section 3. Sell (11) and split (12) join later. */
enum class TransactionType {
    BUY,
}

/** One transaction in a portfolio (spec section 3). The quantity is the exact decimal as typed; the total is in cents (4.10). */
data class Transaction(
    val id: String,
    val portfolioId: String,
    val ticker: String,
    val type: TransactionType,
    val date: LocalDate,
    val enteredSeq: Long,
    val quantity: BigDecimal,
    val totalCents: Long,
    val reversesId: String? = null,
)

/** Why a buy was refused (spec 4.1, 4.2, 4.3). Reasons only; the wording is D9. */
sealed interface BuyRefusal {
    data object NoOpeningBalance : BuyRefusal

    data object PortfolioNotFound : BuyRefusal

    data object TickerEmpty : BuyRefusal

    data object QuantityNotPositive : BuyRefusal

    data object TotalNotPositive : BuyRefusal

    data class BeforeOpeningBalance(
        val openingDate: LocalDate,
    ) : BuyRefusal

    /** The first date the cash would be negative, and the shortfall on that date (as for cash entries, 03 decision 1). */
    data class NegativeCash(
        val date: LocalDate,
        val shortfallCents: Long,
    ) : BuyRefusal

    data object TooLarge : BuyRefusal
}

sealed interface BuyResult {
    /** The buy and its trade line, to be saved together as one action (open item 14). */
    data class Created(
        val transaction: Transaction,
        val cashLine: CashEntry,
    ) : BuyResult

    data class Refused(
        val reason: BuyRefusal,
    ) : BuyResult
}

/** K1: buys (spec 4.1, 4.2, 4.3). The ticker check of 4.3 is done by K4 and K5; the rules engine does not know the network. */
object Transactions {
    /**
     * A buy and its `trade` cash line, which takes the total out of cash on the trade date (4.2). Both carry one
     * entered_seq. The ticker is stored trimmed and in capitals. The first rule that fails decides the reason:
     * opening balance, portfolio, ticker, quantity, total, date, then the cash replay (4.1).
     */
    fun buy(
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
    ): BuyResult {
        val refusal =
            when {
                cashEntries.none { it.type == CashType.OPENING_BALANCE } -> BuyRefusal.NoOpeningBalance
                portfolios.none { it.id == portfolioId } -> BuyRefusal.PortfolioNotFound
                ticker.isBlank() -> BuyRefusal.TickerEmpty
                quantity.signum() <= 0 -> BuyRefusal.QuantityNotPositive
                totalCents <= 0L -> BuyRefusal.TotalNotPositive
                else -> null
            }
        if (refusal != null) return BuyResult.Refused(refusal)
        val openingDate = cashEntries.first { it.type == CashType.OPENING_BALANCE }.date
        if (date.isBefore(openingDate)) {
            return BuyResult.Refused(BuyRefusal.BeforeOpeningBalance(openingDate))
        }
        val seq = nextSeq(cashEntries, transactions)
        val transaction =
            Transaction(
                id = transactionId,
                portfolioId = portfolioId,
                ticker = ticker.trim().uppercase(Locale.ROOT),
                type = TransactionType.BUY,
                date = date,
                enteredSeq = seq,
                quantity = quantity,
                totalCents = totalCents,
            )
        val cashLine =
            CashEntry(
                id = cashEntryId,
                date = date,
                amountCents = -totalCents,
                type = CashType.TRADE,
                note = "",
                enteredSeq = seq,
                transactionId = transactionId,
            )
        return when (val replayed = CashRules.replay(cashEntries + cashLine, from = date)) {
            null -> BuyResult.Created(transaction, cashLine)
            is CashEntryRefusal.NegativeCash -> BuyResult.Refused(BuyRefusal.NegativeCash(replayed.date, replayed.shortfallCents))
            else -> BuyResult.Refused(BuyRefusal.TooLarge)
        }
    }

    /** The entered_seq for a new record: the highest of cash entries and transactions, plus one (4.1 order of entry). */
    fun nextSeq(
        cashEntries: List<CashEntry>,
        transactions: List<Transaction>,
    ): Long = maxOf(CashRules.nextSeq(cashEntries), (transactions.maxOfOrNull { it.enteredSeq } ?: 0L) + 1L)

    /** The derived price per share: total over quantity, exact (4.3, 4.10). */
    fun pricePerShare(
        totalCents: Long,
        quantity: BigDecimal,
    ): Fraction = Fraction.ofCents(totalCents) / Fraction.of(quantity)
}
