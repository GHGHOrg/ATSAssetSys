package app.ghghorg.atsassets.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.Holding
import app.ghghorg.atsassets.core.Lot
import app.ghghorg.atsassets.core.Money
import app.ghghorg.atsassets.core.Quantity
import app.ghghorg.atsassets.core.Transaction

/** Holding detail's callbacks, passed together. */
class HoldingActions(
    val onBack: () -> Unit,
    val onTab: (HoldingTab) -> Unit,
    val onBuy: () -> Unit,
)

/**
 * Holding detail (view 6, wireframe 06 frames A and B; iteration 10): "<" and the ticker, a header that stays in place
 * (portfolio, value, shares, cost, and "no price, at cost" as the gain/loss until 16), Buy (10 decisions 2 and 3),
 * then the Transactions and Lots tabs (decision 1) whose content scrolls. Rows open nothing until 13 (decision 4).
 */
@Composable
fun HoldingDetailScreen(
    state: AppState,
    actions: HoldingActions,
    modifier: Modifier = Modifier,
) {
    val holding = state.shownHolding ?: return
    val portfolio = state.shownPortfolio?.name.orEmpty()
    BackHandler(onBack = actions.onBack)
    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TitleRow(holding.ticker, actions.onBack)
            Header(holding, portfolio)
            val buySpoken = stringResource(R.string.holding_buy_spoken, holding.ticker, portfolio)
            OutlinedButton(onClick = actions.onBuy, modifier = Modifier.semantics { contentDescription = buySpoken }) {
                Text(text = stringResource(R.string.buy_button))
            }
        }
        val tabs = listOf(HoldingTab.TRANSACTIONS, HoldingTab.LOTS)
        PrimaryTabRow(selectedTabIndex = tabs.indexOf(state.holdingTab)) {
            tabs.forEach { tab ->
                Tab(
                    selected = tab == state.holdingTab,
                    onClick = { actions.onTab(tab) },
                    text = { Text(text = stringResource(tabLabel(tab))) },
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        ) {
            when (state.holdingTab) {
                HoldingTab.TRANSACTIONS -> RowList(state.holdingTransactions) { TransactionRow(it) }

                HoldingTab.LOTS -> {
                    if (state.holdingLots.isEmpty()) {
                        Text(text = stringResource(R.string.holding_no_lots), style = MaterialTheme.typography.bodyLarge)
                    } else {
                        RowList(state.holdingLots) { LotRow(it) }
                    }
                }
            }
        }
    }
}

private fun tabLabel(tab: HoldingTab): Int = when (tab) {
    HoldingTab.TRANSACTIONS -> R.string.holding_tab_transactions
    HoldingTab.LOTS -> R.string.holding_tab_lots
}

/** "<" and the ticker; TalkBack reads "<" as Back. */
@Composable
private fun TitleRow(
    ticker: String,
    onBack: () -> Unit,
) {
    val back = stringResource(R.string.back_button)
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onBack, modifier = Modifier.semantics { contentDescription = back }) {
            Text(text = "<", style = MaterialTheme.typography.titleLarge)
        }
        Text(
            text = ticker,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
    }
}

/**
 * Wireframe 06 marker 1 without prices (wording 1, option A): the portfolio, the value at cost, Shares, Cost, and
 * "no price, at cost" in the gain/loss row (4.8, criterion 48). Read by TalkBack as one sentence.
 */
@Composable
private fun Header(
    holding: Holding,
    portfolio: String,
) {
    val value = Money.format(holding.cost)
    val spoken =
        stringResource(
            R.string.holding_header_spoken,
            holding.ticker,
            portfolio,
            value.substringAfter('$'),
            sharesText(holding.shares),
            value.substringAfter('$'),
        )
    OutlinedCard(modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = spoken }) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = portfolio,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = value, style = MaterialTheme.typography.headlineSmall)
            HeaderLine(stringResource(R.string.holding_shares_label), Quantity.format(holding.shares))
            HeaderLine(stringResource(R.string.portfolio_cost_label), value)
            HeaderLine(stringResource(R.string.portfolio_unrealized_label), stringResource(R.string.holding_no_price))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeaderLine(
    label: String,
    value: String,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun <T> RowList(
    items: List<T>,
    row: @Composable (T) -> Unit,
) {
    items.forEachIndexed { index, item ->
        if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        row(item)
    }
}

/**
 * A transaction (wording 2, option B): one line, the date first, then "Buy", the shares and the amount, separated by
 * space only. At the largest font the amount wraps below. Not tappable until 13 (decision 4).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TransactionRow(transaction: Transaction) {
    val date = transaction.date.toString()
    val shares = sharesText(Fraction.of(transaction.quantity))
    val amount = Money.format(transaction.totalCents)
    val spoken = stringResource(R.string.holding_transaction_spoken, date, shares, amount.substringAfter('$'))
    FlowRow(
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = spoken },
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = stringResource(R.string.date_with_et, date), style = MaterialTheme.typography.bodyLarge)
        Text(
            text = stringResource(R.string.holding_transaction_buy),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
        Text(text = shares, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = amount,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.End,
        )
    }
}

/**
 * A lot (wireframe 06 frame B, wording 2): the purchase date with "ET" and "10 left, $150.00 per share", the lot cost
 * and "no price" in place of the gain/loss (4.8). Cost per share is exact and rounded only here (4.10).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LotRow(lot: Lot) {
    val date = lot.buyDate.toString()
    val left = Quantity.format(lot.remainingQuantity)
    val perShare = Money.formatPerShare(lot.costPerShare)
    val cost = Money.format(lot.cost)
    val spoken = stringResource(R.string.holding_lot_spoken, date, left, perShare.substringAfter('$'), cost.substringAfter('$'))
    FlowRow(
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = spoken },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Column {
            Text(text = stringResource(R.string.date_with_et, date), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(R.string.holding_lot_left, left, perShare),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End,
        ) {
            Text(text = cost, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.End)
            Text(
                text = stringResource(R.string.holding_lot_no_price),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
            )
        }
    }
}
