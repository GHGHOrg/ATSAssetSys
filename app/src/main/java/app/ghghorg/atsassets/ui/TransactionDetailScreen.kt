package app.ghghorg.atsassets.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.Money
import app.ghghorg.atsassets.core.Quantity
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import app.ghghorg.atsassets.core.Transactions
import app.ghghorg.atsassets.ui.theme.gainLossColor

/** The transaction detail's callbacks, passed together. */
class TransactionActions(
    val onBack: () -> Unit,
    val onOpenCashLine: () -> Unit,
)

/**
 * A buy, sell or split (wireframe 10b frame G; iteration 13a): "<" and the title, the entered values with the quantity
 * as originally traded, then what it did: a buy's lot as of today (decision 2), the lots a sell used with their gains
 * as on its review (decision 1), a split's shares before and after. A buy or sell links to its cash line. Every
 * transaction is "Not reversed." until 13b. The whole screen scrolls as one (criterion 39).
 */
@Composable
fun TransactionDetailScreen(
    state: AppState,
    actions: TransactionActions,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = actions.onBack)
    val transaction = state.shownTransaction ?: return
    val portfolio = state.portfolios.firstOrNull { it.id == transaction.portfolioId }?.name.orEmpty()
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TitleRow(transactionTitle(transaction), actions.onBack)
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ReviewRow(stringResource(R.string.buy_portfolio_label), portfolio)
                ReviewRow(stringResource(R.string.date_label), stringResource(R.string.date_with_et, transaction.date.toString()))
                when (transaction.type) {
                    TransactionType.BUY -> {
                        TradeRows(transaction)
                        LotRow(state, transaction)
                    }

                    TransactionType.SELL -> {
                        TradeRows(transaction)
                        SellRows(state)
                    }

                    TransactionType.SPLIT -> SplitRows(state, transaction)
                }
            }
        }
        state.transactionCashLine?.let { CashLink(it, actions.onOpenCashLine) }
        Text(
            text = stringResource(R.string.detail_not_reversed),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** "Buy AAPL", "Sell AAPL" (the trade lines' titles) or "Split AAPL" (13a wording 2). */
@Composable
internal fun transactionTitle(transaction: Transaction): String = stringResource(
    when (transaction.type) {
        TransactionType.BUY -> R.string.trade_buy_title
        TransactionType.SELL -> R.string.trade_sell_title
        TransactionType.SPLIT -> R.string.transaction_split_title
    },
    transaction.ticker,
)

/** Quantity as traded, total amount and price per share (frame G). */
@Composable
private fun TradeRows(transaction: Transaction) {
    ReviewRow(stringResource(R.string.transaction_quantity_traded), Quantity.format(transaction.quantity))
    val total = stringResource(R.string.buy_total_row)
    val totalShown = Money.format(transaction.totalCents)
    ReviewRow(total, totalShown, spoken = spokenLabel(total, totalShown))
    val price = stringResource(R.string.buy_price_per_share_label)
    val priceShown = Money.formatPerShare(Transactions.pricePerShare(transaction.totalCents, transaction.quantity))
    ReviewRow(price, priceShown, spoken = spokenLabel(price, priceShown))
}

/** What is left of a buy's lot as of today, split-adjusted; "0 left" once sold out; "from" a date after today (decision 2). */
@Composable
private fun LotRow(
    state: AppState,
    buy: Transaction,
) {
    val label = stringResource(R.string.transaction_lot_label)
    val lot = state.transactionLot
    when {
        lot == null -> {
            val quantity = Quantity.format(buy.quantity)
            val date = buy.date.toString()
            ReviewRow(
                label,
                stringResource(R.string.transaction_lot_from, quantity, date),
                spoken = stringResource(R.string.transaction_lot_from_spoken, quantity, date),
            )
        }

        lot.remainingQuantity.signum() == 0 ->
            ReviewRow(label, stringResource(R.string.transaction_lot_sold_out), spoken = stringResource(R.string.transaction_lot_sold_out_spoken))

        else -> {
            val left = Quantity.format(lot.remainingQuantity)
            val perShare = Money.formatPerShare(lot.costPerShare)
            ReviewRow(
                label,
                stringResource(R.string.holding_lot_left, left, perShare),
                spoken = stringResource(R.string.transaction_lot_spoken, left, perShare.substringAfter('$')),
            )
        }
    }
}

/** The lots a sell used, the cost of lots used and the realized gain/loss, as on the sell review (decision 1). */
@Composable
private fun SellRows(state: AppState) {
    val pieces = state.transactionPieces
    val costs = Money.partsInCents(pieces.map { it.cost })
    val proceeds = Money.partsInCents(pieces.map { it.proceeds })
    val gains = Money.partsInCents(pieces.map { it.gain })
    val realized = gains.sum()
    Text(
        text = stringResource(R.string.sell_lots_used),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
    pieces.forEachIndexed { index, piece ->
        PieceRows(piece.buyDate.toString(), piece.quantity, costs[index], proceeds[index], gains[index])
    }
    val costLabel = stringResource(R.string.sell_cost_of_lots)
    val costShown = Money.format(costs.sum())
    ReviewRow(costLabel, costShown, spoken = spokenLabel(costLabel, costShown))
    ReviewRow(
        stringResource(R.string.sell_realized_label),
        signedAmount(realized),
        spoken = stringResource(R.string.sell_realized_spoken, spokenSigned(realized)),
        color = gainLossColor(realized.compareTo(0L)),
    )
}

/** A split's ratio and the shares at its place before and after (12 wording 2, option A). */
@Composable
private fun SplitRows(
    state: AppState,
    split: Transaction,
) {
    split.split?.let { ReviewRow(stringResource(R.string.split_button), ratioText(it)) }
    state.transactionSplitEffect?.let { effect ->
        ReviewRow(stringResource(R.string.split_shares_at_point), Quantity.format(effect.before))
        ReviewRow(stringResource(R.string.split_shares_after), Quantity.format(effect.after))
    }
}

/** "Cash −$1,500.00 >" (frame G): opens the trade line's detail (13a wording 3). */
@Composable
private fun CashLink(
    line: CashEntry,
    onOpen: () -> Unit,
) {
    val spoken = stringResource(R.string.transaction_cash_link_spoken, spokenSigned(line.amountCents))
    Row(
        modifier = Modifier.fillMaxWidth().opensWith(spoken, onOpen).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.transaction_cash_link, signedAmount(line.amountCents)),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Text(text = ">", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.End)
    }
}
