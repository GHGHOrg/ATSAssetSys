package app.ghghorg.atsassets.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.core.HistoryChip
import app.ghghorg.atsassets.core.SplitRatio

/** Transaction history's callbacks, passed together. */
class TransactionHistoryActions(
    val onBack: () -> Unit,
    val onFromChange: (String) -> Unit,
    val onToChange: (String) -> Unit,
    val onChip: (HistoryChip) -> Unit,
    val onOpenTickerMenu: () -> Unit,
    val onCloseTickerMenu: () -> Unit,
    val onPickTicker: (String?) -> Unit,
    val onOpenTransaction: (String) -> Unit,
    val onOpenCashLine: (String) -> Unit,
)

/**
 * Transaction history (view 8, wireframe 08; 27), drawn from its model (CR2): "<" and the title; the portfolio named at
 * the top, From and To with the date note, the Ticker field and the type chips (markers 1 and 2); the rows, newest first,
 * each opening its transaction, with its cash-effect line and its "Reversed on" link (markers 3 and 4); under Cash the
 * cash lines (markers 5 and 7); with no match one plain line (marker 6). One scrolling screen; the Ticker menu is drawn
 * in the activity window (decision 3), and Back closes it first.
 */
@Composable
fun TransactionHistoryScreen(
    model: TransactionHistoryModel,
    actions: TransactionHistoryActions,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = actions.onBack)
    Box(modifier = modifier.fillMaxSize()) {
        // While the menu is open, TalkBack reads only the menu.
        val under = if (model.menuOpen) Modifier.clearAndSetSemantics { } else Modifier
        Column(
            modifier = Modifier.fillMaxSize().then(under).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TitleRow(stringResource(R.string.history_title), actions.onBack)
            FiltersCard(model, actions)
            if (model.rows.isEmpty()) {
                Text(text = stringResource(R.string.history_none), style = MaterialTheme.typography.bodyLarge)
            } else {
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    model.rows.forEachIndexed { index, row ->
                        key(row.key) {
                            if (index > 0) HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                            HistoryRowView(row, actions)
                        }
                    }
                }
            }
        }
        if (model.menuOpen) TickerMenu(model, actions)
    }
}

/** The portfolio, From and To with the date note, the Ticker field and the chips (markers 1 and 2). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FiltersCard(
    model: TransactionHistoryModel,
    actions: TransactionHistoryActions,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val portfolio = stringResource(R.string.allocation_portfolio_spoken, model.portfolioName)
            Column(modifier = Modifier.clearAndSetSemantics { contentDescription = portfolio }) {
                Text(
                    text = stringResource(R.string.history_portfolio_label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(text = model.portfolioName, style = MaterialTheme.typography.titleMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterDateField(
                    label = stringResource(R.string.filter_from),
                    text = model.fromText,
                    error = dateMessage(model.fromRefusal),
                    onChange = actions.onFromChange,
                    modifier = Modifier.weight(1f),
                )
                FilterDateField(
                    label = stringResource(R.string.filter_to),
                    text = model.toText,
                    error = dateMessage(model.toRefusal),
                    onChange = actions.onToChange,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = stringResource(R.string.date_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TickerField(model.ticker, actions.onOpenTickerMenu)
            FlowRow(modifier = Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (chip in HistoryChip.entries) {
                    FilterChip(
                        selected = model.chip == chip,
                        onClick = { actions.onChip(chip) },
                        label = { Text(text = chipLabel(chip)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun chipLabel(chip: HistoryChip): String = stringResource(
    when (chip) {
        HistoryChip.ALL -> R.string.filter_all
        HistoryChip.BUY -> R.string.history_chip_buy
        HistoryChip.SELL -> R.string.history_chip_sell
        HistoryChip.SPLIT -> R.string.history_chip_split
        HistoryChip.CASH -> R.string.history_chip_cash
    },
)

/** "Ticker" with "All tickers v" or the ticker picked (wireframe 08); one button opening the menu (decision 3). */
@Composable
private fun TickerField(
    ticker: String?,
    onOpen: () -> Unit,
) {
    val shown = ticker ?: stringResource(R.string.history_all_tickers)
    val spoken = stringResource(R.string.history_ticker_spoken, shown)
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onOpen)
            .clearAndSetSemantics {
                contentDescription = spoken
                role = Role.Button
                onClick {
                    onOpen()
                    true
                }
            },
    ) {
        Text(
            text = stringResource(R.string.history_ticker_label),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = shown, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text(text = "v", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun HistoryRowView(
    row: HistoryRowModel,
    actions: TransactionHistoryActions,
) {
    when (row) {
        is HistoryRowModel.Trade -> TradeRow(row, actions)
        is HistoryRowModel.Split -> SplitRow(row, actions)
        is HistoryRowModel.Reversal -> ReversalRow(row, actions)
        is HistoryRowModel.CashLine -> CashLineRow(row, actions)
    }
}

@Composable
private fun historyShares(shares: HistoryShares): String {
    val id = if (shares.one) R.string.holding_share_one else R.string.holding_shares
    return stringResource(id, shares.text)
}

@Composable
private fun historyRatio(ratio: SplitRatio): String = stringResource(R.string.split_ratio, ratio.to.toString(), ratio.from.toString())

/** A buy or sell (marker 3): one button with each figure, then "Reversed on" and the cash-effect line. */
@Composable
private fun TradeRow(
    row: HistoryRowModel.Trade,
    actions: TransactionHistoryActions,
) {
    val title = stringResource(if (row.sell) R.string.trade_sell_title else R.string.trade_buy_title, row.ticker)
    val shares = historyShares(row.shares)
    val cash = row.cash
    val spoken =
        if (cash != null) {
            stringResource(R.string.history_trade_cash_spoken, title, row.date, shares, row.spokenTotal, spoken(cash.spoken))
        } else {
            stringResource(R.string.history_trade_spoken, title, row.date, shares, row.spokenTotal)
        }
    Column {
        MainLine(title, stringResource(R.string.history_date_shares, row.date, shares), row.total, spoken) { actions.onOpenTransaction(row.transactionId) }
        row.reversedOn?.let { ReversedOnLine(it, actions) }
        cash?.let { CashEffectLine(it, actions) }
    }
}

/** A split (marker 4): its date and ratio, no cash line. */
@Composable
private fun SplitRow(
    row: HistoryRowModel.Split,
    actions: TransactionHistoryActions,
) {
    val title = stringResource(R.string.transaction_split_title, row.ticker)
    val ratio = historyRatio(row.ratio)
    Column {
        MainLine(title, stringResource(R.string.history_date, row.date), ratio, stringResource(R.string.history_split_spoken, title, row.date, ratio)) {
            actions.onOpenTransaction(row.transactionId)
        }
        row.reversedOn?.let { ReversedOnLine(it, actions) }
    }
}

/** A reversal (marker 4): "Reversal of the buy of … ET", its ticker, date and the original's figures; opens the reversal itself. */
@Composable
private fun ReversalRow(
    row: HistoryRowModel.Reversal,
    actions: TransactionHistoryActions,
) {
    val type = transactionTypeLower(row.originalType)
    val title = stringResource(R.string.reversal_of_link, type, row.originalDate)
    val shares = row.shares?.let { historyShares(it) }
    val ratio = row.ratio
    val cash = row.cash
    val line: String
    val amount: String
    val spoken: String
    if (shares != null) {
        line = stringResource(R.string.history_reversal_line, row.ticker, row.date, shares)
        amount = row.total.orEmpty()
        spoken =
            stringResource(
                R.string.history_reversal_spoken,
                type,
                row.originalDate,
                row.ticker,
                row.date,
                shares,
                row.spokenTotal.orEmpty(),
                cash?.let { spoken(it.spoken) }.orEmpty(),
            )
    } else {
        val ratioShown = ratio?.let { historyRatio(it) }.orEmpty()
        line = stringResource(R.string.history_reversal_split_line, row.ticker, row.date)
        amount = ratioShown
        spoken = stringResource(R.string.history_reversal_split_spoken, row.originalDate, row.ticker, row.date, ratioShown)
    }
    Column {
        MainLine(title, line, amount, spoken) { actions.onOpenTransaction(row.transactionId) }
        cash?.let { CashEffectLine(it, actions) }
    }
}

/** Under Cash (marker 5): "Cash line, buy AAPL" or "Reversal of buy AAPL", its date and signed amount; opens its transaction. */
@Composable
private fun CashLineRow(
    row: HistoryRowModel.CashLine,
    actions: TransactionHistoryActions,
) {
    val title =
        stringResource(
            when (row.kind) {
                HistoryCashKind.BUY -> R.string.history_cash_line_buy
                HistoryCashKind.SELL -> R.string.history_cash_line_sell
                HistoryCashKind.REVERSAL_OF_BUY -> R.string.reversal_of_buy_title
                HistoryCashKind.REVERSAL_OF_SELL -> R.string.reversal_of_sell_title
            },
            row.ticker,
        )
    val spokenRow = stringResource(R.string.history_cash_row_spoken, title, row.date, spoken(row.spoken))
    MainLine(title, stringResource(R.string.history_date, row.date), row.amount, spokenRow) { actions.onOpenTransaction(row.transactionId) }
}

/** A row's own button: title and line on the left, the amount and ">" on the right; read by TalkBack as [spoken]. */
@Composable
private fun MainLine(
    title: String,
    line: String,
    amount: String,
    spoken: String,
    onOpen: () -> Unit,
) {
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onOpen)
            .clearAndSetSemantics {
                contentDescription = spoken
                role = Role.Button
                onClick {
                    onOpen()
                    true
                }
            }.padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(text = line, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = amount, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.End)
            Text(text = ">", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/** "Reversed on 2026-04-05 ET >" under the original (marker 4): a button opening the reversal. */
@Composable
private fun ReversedOnLine(
    reversedOn: HistoryReversedOn,
    actions: TransactionHistoryActions,
) {
    val spoken = stringResource(R.string.history_reversed_on_spoken, reversedOn.date)
    LinkLine(stringResource(R.string.detail_reversed_on, reversedOn.date) + " >", spoken) { actions.onOpenTransaction(reversedOn.reversalId) }
}

/** "Cash −$1,500.00 >" under a buy, sell or reversal (marker 3): signed, no color; a button opening the trade's line. */
@Composable
private fun CashEffectLine(
    cash: HistoryCashEffect,
    actions: TransactionHistoryActions,
) {
    val spoken = stringResource(R.string.history_cash_effect_spoken, spoken(cash.spoken))
    LinkLine(stringResource(R.string.history_cash_effect, cash.text) + " >", spoken) { actions.onOpenCashLine(cash.transactionId) }
}

@Composable
private fun LinkLine(
    text: String,
    spoken: String,
    onOpen: () -> Unit,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        modifier =
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onOpen)
            .clearAndSetSemantics {
                contentDescription = spoken
                role = Role.Button
                onClick {
                    onOpen()
                    true
                }
            }.padding(start = 28.dp, end = 12.dp, top = 6.dp, bottom = 10.dp),
    )
}

/**
 * The Ticker menu (decision 3), drawn inside the activity window as the ⋮ menu: a clear layer that closes it on a tap
 * outside, and a card with "All tickers" and this portfolio's tickers, scrolling when they do not fit. No DropdownMenu or
 * Popup: a separate window would take the focus (CLAUDE.md).
 */
@Composable
private fun TickerMenu(
    model: TransactionHistoryModel,
    actions: TransactionHistoryActions,
) {
    val title = stringResource(R.string.history_ticker_menu)
    Box(
        modifier = Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { actions.onCloseTickerMenu() } }.padding(16.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().semantics { paneTitle = title },
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 3.dp,
            shadowElevation = 3.dp,
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
                MenuItem(stringResource(R.string.history_all_tickers), model.ticker == null) { actions.onPickTicker(null) }
                model.tickers.forEach { ticker -> MenuItem(ticker, model.ticker == ticker) { actions.onPickTicker(ticker) } }
            }
        }
    }
}

@Composable
private fun MenuItem(
    label: String,
    chosen: Boolean,
    onPick: () -> Unit,
) {
    TextButton(
        onClick = onPick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).semantics { selected = chosen },
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (chosen) FontWeight.Medium else FontWeight.Normal,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
