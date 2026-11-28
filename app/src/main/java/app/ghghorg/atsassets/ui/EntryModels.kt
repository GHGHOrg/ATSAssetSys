package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.AmountResult
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.Money
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.Quantity
import app.ghghorg.atsassets.core.QuantityResult
import app.ghghorg.atsassets.core.SoldPiece
import app.ghghorg.atsassets.core.TickerCheck
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import java.math.BigDecimal

/*
 * The buy and sell entry screens and their reviews as they are drawn (wireframes 10a and 10c; CR2, open item 48; 44a step
 * 8 group (d) part 1): every shown figure, date and name is worked out here, and the screens only pick the approved
 * string and put these values in. Plain Kotlin. The refusals' messages are still picked and filled by the screens (open
 * item 48: not moved here).
 */

/** One choice in a pick field's list (D-30, D-31): its id and what is shown. */
data class PickOption(
    val id: String,
    val label: String,
)

/** "Original: …" under a changed field (10c marker 2): the original as shown; [dollars] when TalkBack reads it as dollars. */
data class OriginalValue(
    val shown: String,
    val dollars: Boolean = false,
)

/** The originals shown under the fields that differ from the replaced entry (10c marker 2); null where a field matches. */
data class OriginalHints(
    val portfolio: OriginalValue? = null,
    val ticker: OriginalValue? = null,
    val date: OriginalValue? = null,
    val quantity: OriginalValue? = null,
    val total: OriginalValue? = null,
)

/** The banner over a replacing buy or sell (10c marker 1): what is replaced, as shown. */
data class ReplacingBannerModel(
    val type: TransactionType,
    val date: String,
    val quantity: String,
    val ticker: String,
    val total: String,
)

/** A quantity of shares as shown, and whether it reads "1 share" (section 6 quantity format; 12 decision 4). */
data class SharesModel(
    val shown: String,
    val one: Boolean,
)

object Replacing {
    /** The banner for [original], with its exact shares (CR1: boughtQuantity). */
    fun banner(original: Transaction): ReplacingBannerModel = ReplacingBannerModel(
        type = original.type,
        date = original.date.toString(),
        quantity = Quantity.format(original.boughtQuantity),
        ticker = original.ticker,
        total = Money.format(original.totalCents),
    )

    /**
     * The originals of [original] under each field whose value differs: the portfolio by id, the ticker, the date as typed
     * digits, the quantity as the same shown number (CR1), the total as the same cents ([totalCents] null when not valid).
     */
    fun hints(
        original: Transaction?,
        portfolios: List<Portfolio>,
        portfolioId: String?,
        ticker: String?,
        dateText: String,
        quantityText: String,
        totalCents: Long?,
    ): OriginalHints {
        if (original == null) return OriginalHints()
        val typed = (Quantity.parse(quantityText) as? QuantityResult.Exact)?.value
        val sameQuantity = typed != null && typed.compareTo(Quantity.shown(original.boughtQuantity)) == 0
        return OriginalHints(
            portfolio = if (portfolioId == original.portfolioId) null else OriginalValue(portfolios.firstOrNull { it.id == original.portfolioId }?.name.orEmpty()),
            ticker = if (ticker == original.ticker) null else OriginalValue(original.ticker),
            date = if (DateDigits.toDate(dateText) == original.date) null else OriginalValue(original.date.toString()),
            quantity = if (sameQuantity) null else OriginalValue(Quantity.format(original.boughtQuantity)),
            total = if (totalCents == original.totalCents) null else OriginalValue(Money.format(original.totalCents), dollars = true),
        )
    }

    /** [quantity] as shown, with "1 share" only when the shown text is exactly "1". */
    fun shares(quantity: Fraction): SharesModel {
        val shown = Quantity.format(quantity)
        return SharesModel(shown, shown == "1")
    }
}

/**
 * Buy entry (wireframe 10a frame A; 10c frame A): [portfolio] the chosen portfolio's name, [portfolios] the pick list in
 * creation order, [ticker] as it will be checked (capitals), the replaced buy's [banner] and [originals], and the derived
 * [pricePerShare] once quantity and total are valid.
 */
data class BuyEntryModel(
    val banner: ReplacingBannerModel?,
    val portfolio: String?,
    val portfolioId: String?,
    val portfolios: List<PickOption>,
    val ticker: String,
    val originals: OriginalHints,
    val pricePerShare: String?,
) {
    val replacing: Boolean get() = banner != null
}

object BuyEntry {
    fun of(
        form: BuyForm,
        portfolios: List<Portfolio>,
        original: Transaction?,
        pricePerShare: Fraction?,
    ): BuyEntryModel {
        val ticker = TickerCheck.normalize(form.tickerText).orEmpty()
        return BuyEntryModel(
            banner = original?.let { Replacing.banner(it) },
            portfolio = portfolios.firstOrNull { it.id == form.portfolioId }?.name,
            portfolioId = form.portfolioId,
            portfolios = portfolios.map { PickOption(it.id, it.name) },
            ticker = ticker,
            originals =
            Replacing.hints(
                original,
                portfolios,
                form.portfolioId,
                ticker,
                form.dateText,
                form.quantityText,
                (Money.parseCents(form.totalText) as? AmountResult.Cents)?.value,
            ),
            pricePerShare = pricePerShare?.let { Money.formatPerShare(it) },
        )
    }
}

/**
 * Buy review (wireframe 10a frame B): the values as entered, the price per share, and the cash effect in cents (signed
 * by the screen, uncolored). The figures are null while not valid; the review opens only once they are.
 */
data class BuyReviewModel(
    val portfolio: String,
    val ticker: String,
    val date: String,
    val quantity: String,
    val total: String?,
    val pricePerShare: String?,
    val cashCents: Long?,
)

object BuyReview {
    fun of(
        form: BuyForm,
        portfolios: List<Portfolio>,
        pricePerShare: Fraction?,
    ): BuyReviewModel {
        val cents = (Money.parseCents(form.totalText) as? AmountResult.Cents)?.value
        return BuyReviewModel(
            portfolio = portfolios.firstOrNull { it.id == form.portfolioId }?.name.orEmpty(),
            ticker = TickerCheck.normalize(form.tickerText).orEmpty(),
            date = DateDigits.toDate(form.dateText)?.toString() ?: DateDigits.format(form.dateText),
            quantity = (Quantity.parse(form.quantityText) as? QuantityResult.Exact)?.value?.let { Quantity.format(it) }.orEmpty(),
            total = cents?.let { Money.format(it) },
            pricePerShare = pricePerShare?.takeIf { cents != null }?.let { Money.formatPerShare(it) },
            cashCents = cents?.let { -it },
        )
    }
}

/**
 * "Picked 12 of 12 shares" (wireframe 10a marker 11): the valid picks' sum, and the sell's quantity once it is valid;
 * [originals] while the picks are still the replaced sell's own (10c marker 9).
 */
data class PickedLineModel(
    val picked: SharesModel,
    val of: SharesModel?,
    val originals: Boolean,
)

/**
 * Sell entry (wireframe 10a frames C and D; 10c frame C): the chosen portfolio and ticker with their pick lists (the
 * tickers held there, in ticker order), the replaced sell's [banner] and [originals], the derived [pricePerShare], and
 * the picked line. The lot rows have their own model (SellLotsModel, 16c).
 */
data class SellEntryModel(
    val banner: ReplacingBannerModel?,
    val portfolio: String?,
    val portfolioId: String?,
    val portfolios: List<PickOption>,
    val ticker: String?,
    val tickers: List<String>,
    val originals: OriginalHints,
    val pricePerShare: String?,
    val picked: PickedLineModel,
) {
    val replacing: Boolean get() = banner != null
}

object SellEntry {
    fun of(
        form: SellForm,
        portfolios: List<Portfolio>,
        tickers: List<String>,
        original: Transaction?,
        pricePerShare: Fraction?,
        picked: BigDecimal,
        startingPicks: Map<String, String>,
    ): SellEntryModel {
        val quantity = (Quantity.parse(form.quantityText) as? QuantityResult.Exact)?.value
        return SellEntryModel(
            banner = original?.let { Replacing.banner(it) },
            portfolio = portfolios.firstOrNull { it.id == form.portfolioId }?.name,
            portfolioId = form.portfolioId,
            portfolios = portfolios.map { PickOption(it.id, it.name) },
            ticker = form.ticker,
            tickers = tickers,
            originals =
            Replacing.hints(
                original,
                portfolios,
                form.portfolioId,
                form.ticker,
                form.dateText,
                form.quantityText,
                (sellTotal(form.totalText) as? AmountResult.Cents)?.value,
            ),
            pricePerShare = pricePerShare?.let { Money.formatPerShare(it) },
            picked =
            PickedLineModel(
                picked = Replacing.shares(Fraction.of(picked)),
                of = quantity?.let { Replacing.shares(Fraction.of(it)) },
                originals = original != null && form.picks.isNotEmpty() && form.picks == startingPicks,
            ),
        )
    }
}

/** One lot used on the sell review (11 wording 3): its date, its shares, and its cost, proceeds and gain by largest remainder (4.10). */
data class SoldPieceModel(
    val date: String,
    val shares: SharesModel,
    val cost: String,
    val proceeds: String,
    val gainCents: Long,
)

/**
 * Sell review (wireframe 10a frame E): the values as entered, the price per share, each lot used, the cost of the lots
 * used, the realized gain/loss in cents (colored and signed by the screen) and the cash effect in cents.
 */
data class SellReviewModel(
    val portfolio: String,
    val ticker: String,
    val date: String,
    val quantity: String,
    val total: String,
    val pricePerShare: String?,
    val pieces: List<SoldPieceModel>,
    val cost: String,
    val realizedCents: Long,
    val cashCents: Long,
)

object SellReview {
    fun of(
        sell: Transaction,
        pieces: List<SoldPiece>,
        portfolios: List<Portfolio>,
        pricePerShare: Fraction?,
    ): SellReviewModel {
        val sold = SoldPieces.of(pieces)
        return SellReviewModel(
            portfolio = portfolios.firstOrNull { it.id == sell.portfolioId }?.name.orEmpty(),
            ticker = sell.ticker,
            date = sell.date.toString(),
            quantity = Quantity.format(sell.quantity),
            total = Money.format(sell.totalCents),
            pricePerShare = pricePerShare?.let { Money.formatPerShare(it) },
            pieces = sold.pieces,
            cost = sold.cost,
            realizedCents = sold.realizedCents,
            cashCents = sell.totalCents,
        )
    }
}

/** A sell's lots used, the cost of the lots used and the realized gain/loss in cents (sell review; transaction detail, 13a decision 1). */
data class SoldPiecesModel(
    val pieces: List<SoldPieceModel>,
    val cost: String,
    val realizedCents: Long,
)

object SoldPieces {
    fun of(pieces: List<SoldPiece>): SoldPiecesModel {
        // Parts beside a total are shown by largest remainder, so they add up to it (4.10).
        val costs = Money.partsInCents(pieces.map { it.cost })
        val proceeds = Money.partsInCents(pieces.map { it.proceeds })
        val gains = Money.partsInCents(pieces.map { it.gain })
        return SoldPiecesModel(
            pieces =
            pieces.mapIndexed { index, piece ->
                SoldPieceModel(
                    date = piece.buyDate.toString(),
                    shares = Replacing.shares(piece.quantity),
                    cost = Money.format(costs[index]),
                    proceeds = Money.format(proceeds[index]),
                    gainCents = gains[index],
                )
            },
            cost = Money.format(costs.sum()),
            realizedCents = gains.sum(),
        )
    }
}
