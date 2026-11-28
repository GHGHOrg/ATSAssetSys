package app.ghghorg.atsassets.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextAlign
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.core.HistoryChip
import app.ghghorg.atsassets.core.SplitRatio
import app.ghghorg.atsassets.ui.design.AtsCard
import app.ghghorg.atsassets.ui.design.AtsChip
import app.ghghorg.atsassets.ui.design.AtsRowDivider
import app.ghghorg.atsassets.ui.design.AtsScreen
import app.ghghorg.atsassets.ui.theme.AtsIcon
import app.ghghorg.atsassets.ui.theme.AtsTheme
import app.ghghorg.atsassets.ui.theme.LucideIcon
import app.ghghorg.atsassets.ui.theme.tokenDp
import app.ghghorg.atsassets.ui.theme.typeStyle

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
 * Transaction history (view 8, wireframe 08; 27), drawn from its model (CR2): the title band with Back; the portfolio
 * named at the top, From and To with the date note, the Ticker field and the type chips (markers 1 and 2); the rows, newest
 * first, each opening its transaction, with its cash-effect line and its "Reversed on" link (markers 3 and 4); under Cash
 * the cash lines (markers 5 and 7); with no match one plain line (marker 6). One scrolling screen; the Ticker menu is
 * drawn in the activity window (decision 3), and Back closes it first. In the design system (44a step 8 group (c);
 * drawing spec/wireframes/design/08-transaction-history.html; D-30, D-31): Cash's filters, chips and rows; a row's links
 * as indented rows with chevrons.
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
        AtsScreen(
            title = stringResource(R.string.history_title),
            modifier = Modifier.fillMaxSize().then(under),
            onBack = actions.onBack,
            backLabel = stringResource(R.string.back_button),
        ) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(tokenDp("spaceMargin")),
                verticalArrangement = Arrangement.spacedBy(tokenDp("spaceGap")),
            ) {
                FiltersCard(model, actions)
                if (model.rows.isEmpty()) {
                    PlainLineCard(stringResource(R.string.history_none))
                } else {
                    AtsCard(padded = false) {
                        model.rows.forEachIndexed { index, row ->
                            key(row.key) {
                                if (index > 0) AtsRowDivider()
                                HistoryRowView(row, actions)
                            }
                        }
                    }
                }
            }
        }
        if (model.menuOpen) TickerMenu(model.ticker, model.tickers, actions.onPickTicker, actions.onCloseTickerMenu)
    }
}

/** The portfolio as a read-only line, From and To with the date note, the Ticker field and the chips (markers 1 and 2). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FiltersCard(
    model: TransactionHistoryModel,
    actions: TransactionHistoryActions,
) {
    val colors = AtsTheme.colors
    AtsCard {
        val portfolio = stringResource(R.string.allocation_portfolio_spoken, model.portfolioName)
        // Drawing 08 note 1: "Portfolio" in secondary 14, the name in medium 16, since there is no chooser.
        FlowRow(
            modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = portfolio },
            horizontalArrangement = Arrangement.spacedBy(tokenDp("spaceInset")),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = stringResource(R.string.history_portfolio_label), style = typeStyle("small"), color = colors.textSecondary)
            Text(text = model.portfolioName, style = typeStyle("sectionHeading"), color = colors.text)
        }
        DenseDateFields(model.fromText, model.toText, dateMessage(model.fromRefusal), dateMessage(model.toRefusal), actions.onFromChange, actions.onToChange)
        TickerField(model.ticker, actions.onOpenTickerMenu)
        // No chip is ever disabled here: every combination can match (marker 2; drawing 08 note 4).
        FlowRow(modifier = Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(tokenDp("fieldDensePadding"))) {
            for (chip in HistoryChip.entries) {
                AtsChip(chipLabel(chip), selected = model.chip == chip, onClick = { actions.onChip(chip) })
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

/**
 * A row's own button (drawing 08 note 3): the title in regular 16 and the line in secondary 14 on the left, the amount
 * uncolored (or a split's ratio) in 16 on the right, and a chevron; read by TalkBack as [spoken]. At the largest font the
 * amount moves under the title (C26).
 */
@Composable
private fun MainLine(
    title: String,
    line: String,
    amount: String,
    spoken: String,
    onOpen: () -> Unit,
) {
    val colors = AtsTheme.colors
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .heightIn(min = tokenDp("spaceTouch"))
            .clickable(role = Role.Button, onClick = onOpen)
            .clearAndSetSemantics {
                contentDescription = spoken
                role = Role.Button
                onClick {
                    onOpen()
                    true
                }
            }.padding(tokenDp("spaceInset")),
        horizontalArrangement = Arrangement.spacedBy(tokenDp("spaceInset")),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StartEndRow(
            modifier = Modifier.weight(1f),
            start = {
                Column {
                    Text(text = title, style = typeStyle("body"), color = colors.text)
                    Text(text = line, style = typeStyle("small"), color = colors.textSecondary)
                }
            },
            end = { Text(text = amount, style = typeStyle("body"), color = colors.text, textAlign = TextAlign.End) },
        )
        AtsIcon(LucideIcon.CHEVRON_RIGHT, tint = colors.iconChevron, size = tokenDp("fieldMarkSize"))
    }
}

/** "Reversed on 2026-04-05 ET" under the original (marker 4): a link row opening the reversal. */
@Composable
private fun ReversedOnLine(
    reversedOn: HistoryReversedOn,
    actions: TransactionHistoryActions,
) {
    val spoken = stringResource(R.string.history_reversed_on_spoken, reversedOn.date)
    LinkLine(stringResource(R.string.detail_reversed_on, reversedOn.date), spoken) { actions.onOpenTransaction(reversedOn.reversalId) }
}

/** "Cash −$1,500.00" under a buy, sell or reversal (marker 3): signed, no color; a link row opening the trade's line. */
@Composable
private fun CashEffectLine(
    cash: HistoryCashEffect,
    actions: TransactionHistoryActions,
) {
    val spoken = stringResource(R.string.history_cash_effect_spoken, spoken(cash.spoken))
    LinkLine(stringResource(R.string.history_cash_effect, cash.text), spoken) { actions.onOpenCashLine(cash.transactionId) }
}

/**
 * A link row under a row (drawing 08 note 2): indented, in secondary 14 with its sign and no color, and a chevron, 48
 * tall; read by TalkBack as [spoken].
 */
@Composable
private fun LinkLine(
    text: String,
    spoken: String,
    onOpen: () -> Unit,
) {
    val colors = AtsTheme.colors
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .heightIn(min = tokenDp("spaceTouch"))
            .clickable(role = Role.Button, onClick = onOpen)
            .clearAndSetSemantics {
                contentDescription = spoken
                role = Role.Button
                onClick {
                    onOpen()
                    true
                }
            }.padding(start = tokenDp("spaceInset") + tokenDp("spaceMargin"), end = tokenDp("spaceInset")),
        horizontalArrangement = Arrangement.spacedBy(tokenDp("spaceInset")),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = text, style = typeStyle("small"), color = colors.textSecondary, modifier = Modifier.weight(1f))
        AtsIcon(LucideIcon.CHEVRON_RIGHT, tint = colors.iconChevron, size = tokenDp("fieldMarkSize"))
    }
}
