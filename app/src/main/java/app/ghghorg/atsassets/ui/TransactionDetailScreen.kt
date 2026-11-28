package app.ghghorg.atsassets.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.ui.design.AtsButton
import app.ghghorg.atsassets.ui.design.AtsReviewCard
import app.ghghorg.atsassets.ui.design.AtsReviewSubheading
import app.ghghorg.atsassets.ui.design.AtsRowDivider
import app.ghghorg.atsassets.ui.design.AtsScreen
import app.ghghorg.atsassets.ui.theme.gainLossColor
import app.ghghorg.atsassets.ui.theme.tokenDp

/** The transaction detail's callbacks, passed together. */
class TransactionActions(
    val onBack: () -> Unit,
    val onOpenCashLine: () -> Unit,
    val onReverse: () -> Unit,
    val onOpenLink: () -> Unit,
    val onReenter: () -> Unit,
)

/**
 * A buy, sell, split or reversal (wireframe 10b frame G; iterations 13a and 13b), drawn from its model (CR2, open item
 * 48) in the design system (44a step 8 group (d) part 3; D-31): under its band, the entered values in a card with the
 * quantity as originally traded, then what it did: a buy's lot as of today (13a decision 2), the lots a sell used with
 * their gains as on its review (13a decision 1), a split's shares before and after, a reversal's effect; a buy, sell or
 * their reversal links to its cash line as a row with a chevron inside the card. Then "Not reversed." with Reverse and
 * Reverse and re-enter as outline buttons, or a link row to "Reversed on …" or, for a reversal, "Reversal of the … of …"
 * (frame G marker 14; 13b wording 5). The whole screen scrolls as one (criterion 39).
 */
@Composable
fun TransactionDetailScreen(
    state: AppState,
    actions: TransactionActions,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = actions.onBack)
    val model = state.transactionDetail ?: return
    AtsScreen(
        title = tradeTitleText(model.title, model.ticker),
        modifier = modifier,
        onBack = actions.onBack,
        backLabel = stringResource(R.string.back_button),
    ) {
        EntryColumn {
            AtsReviewCard {
                ReviewRow(stringResource(R.string.buy_portfolio_label), model.portfolio)
                ReviewRow(stringResource(R.string.date_label), stringResource(R.string.date_with_et, model.date))
                when (val body = model.body) {
                    is TransactionBodyModel.Trade -> TradeRows(body)
                    is TransactionBodyModel.Split -> SplitRows(body)
                    is TransactionBodyModel.Reversal -> body.effect?.let { EffectRows(it) }
                }
                model.cashCents?.let { cents ->
                    AtsRowDivider()
                    DetailLinkLine(
                        stringResource(R.string.transaction_cash_link, signedAmount(cents)),
                        stringResource(R.string.transaction_cash_link_spoken, spokenSigned(cents)),
                        actions.onOpenCashLine,
                    )
                }
            }
            ReversalStatus(model, actions)
        }
    }
}

/**
 * The last part (frame G marker 14; 13b wording 5): a reversal links to its original; a reversed entry links to its
 * reversal; any other shows "Not reversed." and the buttons offered.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReversalStatus(
    model: TransactionDetailModel,
    actions: TransactionActions,
) {
    when (val status = model.status) {
        is TransactionStatusModel.ReversalOf -> {
            val text = stringResource(R.string.reversal_of_link, transactionTypeLower(status.type), status.date)
            DetailLinkRow(text, text, actions.onOpenLink)
        }

        is TransactionStatusModel.ReversedOn -> {
            val text = stringResource(R.string.detail_reversed_on, status.date)
            DetailLinkRow(text, text, actions.onOpenLink)
        }

        is TransactionStatusModel.NotReversed -> {
            StatusLine(stringResource(R.string.detail_not_reversed))
            if (status.reverseOffered || status.reenterOffered) {
                val type = transactionTypeLower(model.type)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(tokenDp("spaceGap"))) {
                    if (status.reverseOffered) {
                        val spoken = stringResource(R.string.reverse_button_spoken, type, model.ticker, model.date)
                        AtsButton(stringResource(R.string.reverse_button), actions.onReverse, modifier = Modifier.semantics { contentDescription = spoken })
                    }
                    // Reverse and re-enter on a buy or sell (14a; wireframe 10c marker 1) or a split (14b; marker 11).
                    if (status.reenterOffered) {
                        val spoken = stringResource(R.string.reenter_button_spoken, type, model.ticker, model.date)
                        AtsButton(stringResource(R.string.reenter_button), actions.onReenter, modifier = Modifier.semantics { contentDescription = spoken })
                    }
                }
            }
        }
    }
}

/** Quantity as traded, total amount and price per share (frame G), then a buy's lot or a sell's lots used. */
@Composable
private fun TradeRows(body: TransactionBodyModel.Trade) {
    ReviewRow(stringResource(R.string.transaction_quantity_traded), body.quantity)
    val total = stringResource(R.string.buy_total_row)
    ReviewRow(total, body.total, spoken = spokenLabel(total, body.total))
    val price = stringResource(R.string.buy_price_per_share_label)
    ReviewRow(price, body.pricePerShare, spoken = spokenLabel(price, body.pricePerShare))
    body.lot?.let { LotRow(it) }
    body.sold?.let { SellRows(it) }
}

/** What is left of a buy's lot as of today, split-adjusted; "0 left" once sold out; "from" a date after today (decision 2). */
@Composable
private fun LotRow(lot: LotLineModel) {
    val label = stringResource(R.string.transaction_lot_label)
    when (lot) {
        is LotLineModel.From ->
            ReviewRow(
                label,
                stringResource(R.string.transaction_lot_from, lot.quantity, lot.date),
                spoken = stringResource(R.string.transaction_lot_from_spoken, lot.quantity, lot.date),
            )

        LotLineModel.SoldOut ->
            ReviewRow(label, stringResource(R.string.transaction_lot_sold_out), spoken = stringResource(R.string.transaction_lot_sold_out_spoken))

        is LotLineModel.Left ->
            ReviewRow(
                label,
                stringResource(R.string.holding_lot_left, lot.left, lot.perShare),
                spoken = stringResource(R.string.transaction_lot_spoken, lot.left, lot.perShare.substringAfter('$')),
            )
    }
}

/** The lots a sell used, the cost of lots used and the realized gain/loss, as on the sell review (decision 1). */
@Composable
private fun SellRows(sold: SoldPiecesModel) {
    AtsReviewSubheading(stringResource(R.string.sell_lots_used))
    sold.pieces.forEach { SoldPieceRows(it) }
    val costLabel = stringResource(R.string.sell_cost_of_lots)
    ReviewRow(costLabel, sold.cost, spoken = spokenLabel(costLabel, sold.cost))
    ReviewRow(
        stringResource(R.string.sell_realized_label),
        signedAmount(sold.realizedCents),
        spoken = stringResource(R.string.sell_realized_spoken, spokenSigned(sold.realizedCents)),
        color = gainLossColor(sold.realizedCents.compareTo(0L)),
    )
}

/** A split's ratio and the shares at its place before and after (12 wording 2, option A). */
@Composable
private fun SplitRows(body: TransactionBodyModel.Split) {
    body.ratio?.let { ReviewRow(stringResource(R.string.split_button), ratioText(it)) }
    if (body.before != null && body.after != null) {
        ReviewRow(stringResource(R.string.split_shares_at_point), body.before)
        ReviewRow(stringResource(R.string.split_shares_after), body.after)
    }
}
