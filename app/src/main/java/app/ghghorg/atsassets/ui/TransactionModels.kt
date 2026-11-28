package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.Lot
import app.ghghorg.atsassets.core.Money
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.Quantity
import app.ghghorg.atsassets.core.ReversalEffect
import app.ghghorg.atsassets.core.SoldPiece
import app.ghghorg.atsassets.core.SplitEffect
import app.ghghorg.atsassets.core.SplitRatio
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import app.ghghorg.atsassets.core.Transactions

/*
 * The split entry and its review, a transaction's detail and the reversal screen as they are drawn (wireframes 10b and
 * 10c; CR2, open item 48; 44a step 8 group (d) part 3): every shown date, name and figure is worked out here; the screens
 * pick the approved string. Plain Kotlin. The refusals' messages stay in the screens (open item 48), as in parts 1 and 2.
 */

/** The banner over a replacing split (14b wording 1): its date, ratio and ticker. */
data class SplitBannerModel(
    val date: String,
    val ratio: SplitRatio?,
    val ticker: String,
)

/**
 * Split entry (wireframe 10b frame E; 14b): the chosen portfolio and ticker with their pick lists (the tickers with a buy
 * there), the replaced split's [banner], the originals under changed fields, and its ratio while the typed one differs.
 */
data class SplitEntryModel(
    val banner: SplitBannerModel?,
    val portfolio: String?,
    val portfolioId: String?,
    val portfolios: List<PickOption>,
    val ticker: String?,
    val tickers: List<String>,
    val originals: OriginalHints,
    val originalRatio: SplitRatio?,
) {
    val replacing: Boolean get() = banner != null
}

object SplitEntry {
    fun of(
        form: SplitForm,
        portfolios: List<Portfolio>,
        tickers: List<String>,
        original: Transaction?,
    ): SplitEntryModel {
        val ratio = original?.split
        val sameRatio = ratio != null && ratioNumber(form.toText) == ratio.to && ratioNumber(form.fromText) == ratio.from
        return SplitEntryModel(
            banner = original?.let { SplitBannerModel(it.date.toString(), it.split, it.ticker) },
            portfolio = portfolios.firstOrNull { it.id == form.portfolioId }?.name,
            portfolioId = form.portfolioId,
            portfolios = portfolios.map { PickOption(it.id, it.name) },
            ticker = form.ticker,
            tickers = tickers,
            // A split has no quantity or total: only the portfolio, ticker and date are compared.
            originals = Replacing.hints(original, portfolios, form.portfolioId, form.ticker, form.dateText, "", null).copy(quantity = null, total = null),
            originalRatio = ratio?.takeIf { !sameRatio },
        )
    }
}

/** Split review (wireframe 10b frame F; 12 wording 2, option A): the values, and the shares at this point before and after. */
data class SplitReviewModel(
    val portfolio: String,
    val ticker: String,
    val date: String,
    val ratio: SplitRatio,
    val before: String,
    val after: String,
)

object SplitReview {
    fun of(
        split: Transaction,
        effect: SplitEffect,
        portfolios: List<Portfolio>,
    ): SplitReviewModel? {
        val ratio = split.split ?: return null
        return SplitReviewModel(
            portfolio = portfolios.firstOrNull { it.id == split.portfolioId }?.name.orEmpty(),
            ticker = split.ticker,
            date = split.date.toString(),
            ratio = ratio,
            before = Quantity.format(effect.before),
            after = Quantity.format(effect.after),
        )
    }
}

/** What a reversal does (10b frame I; 13b wording 1 to 3), on the reversal screen and on a saved reversal's detail. */
sealed interface ReversalEffectModel {
    data class OfBuy(
        val sharesRemoved: String,
        val cashCents: Long,
    ) : ReversalEffectModel

    data class OfSell(
        val lots: List<RestoredLotModel>,
        val cashCents: Long,
        val realizedRemovedCents: Long,
    ) : ReversalEffectModel

    data class OfSplit(
        val inverse: SplitRatio,
        val before: String,
        val after: String,
    ) : ReversalEffectModel
}

/** A lot a sell's reversal puts back: its buy date and its shares. */
data class RestoredLotModel(
    val date: String,
    val shares: SharesModel,
)

object ReversalEffects {
    fun of(effect: ReversalEffect): ReversalEffectModel = when (effect) {
        is ReversalEffect.OfBuy -> ReversalEffectModel.OfBuy(Quantity.format(effect.sharesRemoved), effect.cashCents)

        is ReversalEffect.OfSell ->
            ReversalEffectModel.OfSell(
                lots = effect.lots.map { RestoredLotModel(it.buyDate.toString(), Replacing.shares(it.quantity)) },
                cashCents = effect.cashCents,
                // A value shown alone, rounded half away from zero to cents (4.10).
                realizedRemovedCents = centsOf(effect.realizedRemoved),
            )

        is ReversalEffect.OfSplit -> ReversalEffectModel.OfSplit(effect.inverse, Quantity.format(effect.before), Quantity.format(effect.after))
    }
}

/** A buy's lot as of today (13a decision 2): from a date after today, sold out, or what is left and its cost per share. */
sealed interface LotLineModel {
    data class From(
        val quantity: String,
        val date: String,
    ) : LotLineModel

    object SoldOut : LotLineModel

    data class Left(
        val left: String,
        val perShare: String,
    ) : LotLineModel
}

/** What a transaction's card shows under its portfolio and date (10b frame G; 13a, 13b). */
sealed interface TransactionBodyModel {
    /** A buy or a sell: as traded (CR1), then a buy's lot or a sell's lots used. */
    data class Trade(
        val quantity: String,
        val total: String,
        val pricePerShare: String,
        val lot: LotLineModel?,
        val sold: SoldPiecesModel?,
    ) : TransactionBodyModel

    data class Split(
        val ratio: SplitRatio?,
        val before: String?,
        val after: String?,
    ) : TransactionBodyModel

    data class Reversal(
        val effect: ReversalEffectModel?,
    ) : TransactionBodyModel
}

/** The detail's last part (10b frame G marker 14; 13b wording 5): what the transaction is to its reversal chain. */
sealed interface TransactionStatusModel {
    data class ReversalOf(
        val type: TransactionType,
        val date: String,
    ) : TransactionStatusModel

    data class ReversedOn(
        val date: String,
    ) : TransactionStatusModel

    data class NotReversed(
        val reverseOffered: Boolean,
        val reenterOffered: Boolean,
    ) : TransactionStatusModel
}

/**
 * A transaction's detail (wireframe 10b frame G): its title (as the Cash list's trade titles), portfolio and date, its
 * body, its cash line in cents (none for a split), and its status.
 */
data class TransactionDetailModel(
    val title: TradeTitle,
    val type: TransactionType,
    val ticker: String,
    val portfolio: String,
    val date: String,
    val body: TransactionBodyModel,
    val cashCents: Long?,
    val status: TransactionStatusModel,
)

object TransactionDetails {
    fun of(
        transaction: Transaction,
        transactions: List<Transaction>,
        portfolios: List<Portfolio>,
        lot: Lot?,
        pieces: List<SoldPiece>,
        splitEffect: SplitEffect?,
        shownEffect: ReversalEffect?,
        cashCents: Long?,
        original: Transaction?,
        reversal: Transaction?,
        reverseOffered: Boolean,
        reenterOffered: Boolean,
    ): TransactionDetailModel = TransactionDetailModel(
        title = CashList.titleOf(transaction, transactions.associateBy { it.id }),
        type = transaction.type,
        ticker = transaction.ticker,
        portfolio = portfolios.firstOrNull { it.id == transaction.portfolioId }?.name.orEmpty(),
        date = transaction.date.toString(),
        body =
        when (transaction.type) {
            TransactionType.BUY, TransactionType.SELL ->
                TransactionBodyModel.Trade(
                    quantity = Quantity.format(transaction.boughtQuantity),
                    total = Money.format(transaction.totalCents),
                    pricePerShare = Money.formatPerShare(Transactions.pricePerShare(transaction)),
                    lot = if (transaction.type == TransactionType.BUY) lotLine(transaction, lot) else null,
                    sold = if (transaction.type == TransactionType.SELL) SoldPieces.of(pieces) else null,
                )

            TransactionType.SPLIT ->
                TransactionBodyModel.Split(transaction.split, splitEffect?.let { Quantity.format(it.before) }, splitEffect?.let { Quantity.format(it.after) })

            TransactionType.REVERSAL -> TransactionBodyModel.Reversal(shownEffect?.let { ReversalEffects.of(it) })
        },
        cashCents = cashCents,
        status =
        when {
            original != null -> TransactionStatusModel.ReversalOf(original.type, original.date.toString())
            reversal != null -> TransactionStatusModel.ReversedOn(reversal.date.toString())
            else -> TransactionStatusModel.NotReversed(reverseOffered, reenterOffered)
        },
    )

    private fun lotLine(
        buy: Transaction,
        lot: Lot?,
    ): LotLineModel = when {
        lot == null -> LotLineModel.From(Quantity.format(buy.boughtQuantity), buy.date.toString())
        lot.remainingQuantity.signum() == 0 -> LotLineModel.SoldOut
        else -> LotLineModel.Left(Quantity.format(lot.remainingQuantity), Money.formatPerShare(lot.costPerShare))
    }
}

/**
 * The reversal of a buy, sell or split (wireframe 10b frame I; 13b): the original fixed (its type, date, portfolio, and
 * its shares and total or its ratio), and the effect at the typed date once it is a whole date.
 */
data class ReversalScreenModel(
    val type: TransactionType,
    val ticker: String,
    val date: String,
    val portfolio: String,
    val ratio: SplitRatio?,
    val shares: SharesModel,
    val total: String,
    val effect: ReversalEffectModel?,
)

object ReversalScreens {
    fun of(
        original: Transaction,
        portfolios: List<Portfolio>,
        effect: ReversalEffect?,
    ): ReversalScreenModel = ReversalScreenModel(
        type = original.type,
        ticker = original.ticker,
        date = original.date.toString(),
        portfolio = portfolios.firstOrNull { it.id == original.portfolioId }?.name.orEmpty(),
        ratio = original.split,
        shares = Replacing.shares(original.boughtQuantity),
        total = Money.format(original.totalCents),
        effect = effect?.let { ReversalEffects.of(it) },
    )
}
