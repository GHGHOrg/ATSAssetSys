package app.ghghorg.atsassets.core

import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.util.Locale

/** A split's ratio, "to for from" (4.5, wireframe 10b): 2 for 1 doubles the shares, 1 for 10 is a reverse split. */
data class SplitRatio(
    val to: Int,
    val from: Int,
) {
    /** What a quantity is multiplied by, exact (4.10). Throws IllegalArgumentException when [from] is 0. */
    val factor: Fraction get() = Fraction.of(BigInteger.valueOf(to.toLong()), BigInteger.valueOf(from.toLong()))
}

/** The shares the holding has at a split's place in the 4.1 order, before and after it (the split review). */
data class SplitEffect(
    val before: Fraction,
    val after: Fraction,
)

/** Why a split was refused (spec 4.1, 4.2, 4.5). Reasons only; the wording is D9. */
sealed interface SplitRefusal {
    data object NoOpeningBalance : SplitRefusal

    data object PortfolioNotFound : SplitRefusal

    /** The ticker has no buy in that portfolio (section 6: the ticker is picked from those). */
    data object TickerNotBought : SplitRefusal

    /** A number below 1 or above [Splits.MAX_RATIO] (12 decision 3). */
    data object RatioOutOfRange : SplitRefusal

    data object RatioEqual : SplitRefusal

    data class BeforeOpeningBalance(
        val openingDate: LocalDate,
    ) : SplitRefusal

    /** With the split, the sell dated [date] would take more from a lot than the lot holds there (4.1). */
    data class SellShort(
        val date: LocalDate,
    ) : SplitRefusal
}

sealed interface SplitResult {
    /** The split; it has no cash line (4.5). */
    data class Created(
        val transaction: Transaction,
    ) : SplitResult

    data class Refused(
        val reason: SplitRefusal,
    ) : SplitResult
}

/** K1: splits (spec 4.1, 4.5, 4.10). A split never triggers the ticker check (4.6). */
object Splits {
    /** The largest number on either side of a ratio (12 decision 3). */
    const val MAX_RATIO: Int = 1_000_000

    /** The tickers with a buy in the portfolio, in ticker order: the ones a split can be entered for (section 6). */
    fun tickers(
        transactions: List<Transaction>,
        portfolioId: String,
    ): List<String> = transactions
        .filter { it.type == TransactionType.BUY && it.portfolioId == portfolioId }
        .map { it.ticker }
        .distinct()
        .sorted()

    /**
     * A split, applying to the lots of that ticker in that portfolio that come before it in the 4.1 order (4.5). The
     * ticker is stored trimmed and in capitals. The first rule that fails decides the reason: opening balance,
     * portfolio, ticker, ratio, date, then the replay of the holding's lots (4.1): every later sell must still find
     * what it took in its lots. A holding at 0 shares may be split (12 decision 2).
     */
    fun split(
        cashEntries: List<CashEntry>,
        transactions: List<Transaction>,
        portfolios: List<Portfolio>,
        transactionId: String,
        portfolioId: String,
        ticker: String,
        date: LocalDate,
        ratio: SplitRatio,
    ): SplitResult {
        val upperTicker = ticker.trim().uppercase(Locale.ROOT)
        val refusal =
            when {
                cashEntries.none { it.type == CashType.OPENING_BALANCE } -> SplitRefusal.NoOpeningBalance
                portfolios.none { it.id == portfolioId } -> SplitRefusal.PortfolioNotFound
                upperTicker !in tickers(transactions, portfolioId) -> SplitRefusal.TickerNotBought
                ratio.to !in 1..MAX_RATIO || ratio.from !in 1..MAX_RATIO -> SplitRefusal.RatioOutOfRange
                ratio.to == ratio.from -> SplitRefusal.RatioEqual
                else -> null
            }
        if (refusal != null) return SplitResult.Refused(refusal)
        val openingDate = cashEntries.first { it.type == CashType.OPENING_BALANCE }.date
        if (date.isBefore(openingDate)) {
            return SplitResult.Refused(SplitRefusal.BeforeOpeningBalance(openingDate))
        }
        val transaction =
            Transaction(
                id = transactionId,
                portfolioId = portfolioId,
                ticker = upperTicker,
                type = TransactionType.SPLIT,
                date = date,
                enteredSeq = Transactions.nextSeq(cashEntries, transactions),
                quantity = BigDecimal.ZERO,
                totalCents = 0L,
                split = ratio,
            )
        val short = Holdings.firstNegative(transactions + transaction, portfolioId, upperTicker)?.place?.date
        return if (short == null) SplitResult.Created(transaction) else SplitResult.Refused(SplitRefusal.SellShort(short))
    }

    /** The shares of [split]'s holding just before it and just after it, in the 4.1 order (the split review). */
    fun effect(
        transactions: List<Transaction>,
        split: Transaction,
    ): SplitEffect {
        val justBefore = Place(split.date, split.enteredSeq - 1)
        val ledger = Ledger(transactions)
        val before =
            transactions
                .filter { it.type == TransactionType.BUY && it.portfolioId == split.portfolioId && it.ticker == split.ticker }
                .filter { it.place <= justBefore }
                .fold(Fraction.ZERO) { sum, buy -> sum + ledger.quantityAt(buy, justBefore) }
        return SplitEffect(before, before * (split.split?.factor ?: Fraction.ONE))
    }
}
