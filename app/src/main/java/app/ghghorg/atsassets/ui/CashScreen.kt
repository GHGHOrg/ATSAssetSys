package app.ghghorg.atsassets.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.Money
import app.ghghorg.atsassets.core.TradeSide
import app.ghghorg.atsassets.core.TransactionType

/**
 * The Cash account (view 7, wireframe 07 frame A): the balance, the Cash entry button (the opening balance until
 * it exists, then a deposit or withdrawal, iteration 03), and the rows. A row opens its detail and shows its note
 * (05 decision 3). The date range and type filters (06) sit between the balance and the list, with the Ticker field and
 * the Buy and Sell chips (28); trade lines from 09. Cash is not colored.
 *
 * 28: the Ticker menu is drawn in the activity window over the list (decision 2; the shared TickerMenu), and Back
 * closes it first.
 *
 * 15d: drawn only from the screen's model (state.cashList; CR2), in one lazy list, so only the rows on the screen are
 * laid out (decision 2). The rows' card is drawn in slices, one per row, so it still looks like one card.
 */
@Composable
fun CashScreen(
    state: AppState,
    onCashEntry: () -> Unit,
    onOpenEntry: (String) -> Unit,
    onOpenTransaction: (String) -> Unit,
    filterActions: CashFilterActions,
    modifier: Modifier = Modifier,
) {
    val model = state.cashList
    val menuOpen = state.cashTickerMenuOpen
    // 28: Back closes the Ticker menu first (27 decision 3); otherwise it goes to Overall (02b decision 3).
    BackHandler(enabled = menuOpen, onBack = filterActions.onCloseTickerMenu)
    Box(modifier = modifier.fillMaxSize()) {
        // While the menu is open, TalkBack reads only the menu.
        val under = if (menuOpen) Modifier.clearAndSetSemantics { } else Modifier
        CashList(state, model, onCashEntry, onOpenEntry, onOpenTransaction, filterActions, Modifier.fillMaxSize().then(under))
        if (menuOpen) TickerMenu(model.ticker, model.tickers, filterActions.onPickTicker, filterActions.onCloseTickerMenu)
    }
}

/** The balance, the filters and the rows, in one lazy list (15d decision 2). */
@Composable
private fun CashList(
    state: AppState,
    model: CashListModel,
    onCashEntry: () -> Unit,
    onOpenEntry: (String) -> Unit,
    onOpenTransaction: (String) -> Unit,
    filterActions: CashFilterActions,
    modifier: Modifier = Modifier,
) {
    val shape = CardDefaults.outlinedShape
    val border = CardDefaults.outlinedCardBorder()
    val container = CardDefaults.outlinedCardColors().containerColor
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
    ) {
        item(key = "title") {
            Text(
                text = stringResource(R.string.tab_cash),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }
        item(key = "balance") {
            BalanceCard(model.balance, spoken(model.spokenBalance), onCashEntry, Modifier.padding(bottom = 12.dp))
        }
        if (!model.hasEntries) {
            item(key = "no entries") {
                Text(
                    text = stringResource(R.string.cash_no_entries),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            return@LazyColumn
        }
        item(key = "filters") {
            FilterCard(state.cashFilterForm, model, filterActions, Modifier.padding(bottom = 12.dp))
        }
        if (model.rows.isEmpty()) {
            item(key = "no match") {
                Text(
                    text = stringResource(R.string.cash_no_match),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            return@LazyColumn
        }
        val last = model.rows.lastIndex
        itemsIndexed(model.rows, key = { _, row -> row.key }) { index, row ->
            Column(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .cardSlice(top = index == 0, bottom = index == last, shape, border, container)
                    .padding(
                        start = 12.dp,
                        end = 12.dp,
                        top = if (index == 0) 12.dp else 0.dp,
                        bottom = if (index == last) 12.dp else 0.dp,
                    ),
            ) {
                if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                when (row) {
                    is CashRowModel.Trade -> TradeRow(row, onOpen = { onOpenTransaction(row.transactionId) })
                    is CashRowModel.Entry -> EntryRow(row, onOpen = { onOpenEntry(row.key) })
                }
            }
        }
    }
}

/**
 * One row's slice of an outlined card (15d): the card's fill and outline, with the top edge and its rounded corners
 * only on the first row and the bottom edge only on the last; the shape, outline and fill are the card's own.
 */
private fun Modifier.cardSlice(
    top: Boolean,
    bottom: Boolean,
    shape: Shape,
    border: BorderStroke,
    container: Color,
): Modifier = drawBehind {
    val stroke = border.width.toPx()
    // A missing edge is drawn beyond the row, where the clip below cuts it off with its corners.
    val beyond = stroke + size.height
    val from = if (top) 0f else -beyond
    val to = if (bottom) size.height else size.height + beyond
    val outline = shape.createOutline(Size(size.width - stroke, to - from - stroke), layoutDirection, this)
    clipRect {
        translate(stroke / 2, from + stroke / 2) {
            drawOutline(outline, container)
            drawOutline(outline, border.brush, style = Stroke(stroke))
        }
    }
}

/** A figure for TalkBack: its digits, after "plus" or "minus" when its sign says so. */
@Composable
internal fun spoken(figure: SpokenFigure): String = when (figure.sign) {
    FigureSign.PLUS -> stringResource(R.string.spoken_plus, figure.digits)
    FigureSign.MINUS -> stringResource(R.string.spoken_minus, figure.digits)
    FigureSign.NONE -> figure.digits
}

/** A trade line's title (09 wording 3, 11 wording 4, 13b wording 5), the same strings as transaction detail's. */
@Composable
private fun tradeTitle(row: CashRowModel.Trade): String = stringResource(
    when (row.title) {
        TradeTitle.BUY -> R.string.trade_buy_title
        TradeTitle.SELL -> R.string.trade_sell_title
        TradeTitle.SPLIT -> R.string.transaction_split_title
        TradeTitle.REVERSAL_OF_BUY -> R.string.reversal_of_buy_title
        TradeTitle.REVERSAL_OF_SELL -> R.string.reversal_of_sell_title
        TradeTitle.REVERSAL_OF_SPLIT -> R.string.reversal_of_split_title
    },
    row.ticker,
)

/**
 * The filter actions of the Cash list (iteration 06; the Buy and Sell chips and the Ticker field and menu, 28), passed
 * together to keep the screen's parameters short.
 */
data class CashFilterActions(
    val onFromChange: (String) -> Unit,
    val onToChange: (String) -> Unit,
    val onTypeChange: (CashType?) -> Unit,
    val onClear: () -> Unit,
    val onTradeChange: (TradeSide) -> Unit = {},
    val onOpenTickerMenu: () -> Unit = {},
    val onPickTicker: (String?) -> Unit = {},
    val onCloseTickerMenu: () -> Unit = {},
)

/**
 * From and To with the date note, the type chips, and, while a filter is active, Clear filters and the count line
 * (wireframe 07 markers 2, 3, 5 and 6; 06 decisions 1 and 3).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterCard(
    form: CashFilterForm,
    model: CashListModel,
    actions: CashFilterActions,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterDateField(
                    label = stringResource(R.string.filter_from),
                    text = form.fromText,
                    error = dateMessage(model.fromRefusal),
                    onChange = actions.onFromChange,
                    modifier = Modifier.weight(1f),
                )
                FilterDateField(
                    label = stringResource(R.string.filter_to),
                    text = form.toText,
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
            FlowRow(
                modifier = Modifier.selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = form.type == null && form.trade == null,
                    onClick = { actions.onTypeChange(null) },
                    label = { Text(text = stringResource(R.string.filter_all)) },
                )
                for (type in FILTER_TYPES) {
                    DirectChip(
                        label = cashTypeLabel(type),
                        selected = form.type == type,
                        enabled = model.directChipsEnabled,
                        onClick = { actions.onTypeChange(type) },
                    )
                }
                for (side in TradeSide.entries) {
                    FilterChip(
                        selected = form.trade == side,
                        onClick = { actions.onTradeChange(side) },
                        label = { Text(text = tradeSideLabel(side)) },
                    )
                }
            }
            if (!model.directChipsEnabled) {
                Text(
                    text = stringResource(R.string.cash_chips_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (model.filterActive) {
                val count = model.rows.size
                Text(
                    text = pluralStringResource(R.plurals.cash_entries_shown, count, count),
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(onClick = actions.onClear) {
                    Text(text = stringResource(R.string.filter_clear))
                }
            }
        }
    }
}

/** A From or To field (06), shared with Realized gains (26): digits on a number keyboard, the dashes shown, not typed. */
@Composable
internal fun FilterDateField(
    label: String,
    text: String,
    error: String?,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Digits only on a number keyboard; the dashes are shown, not typed (06, after phone check 12).
    OutlinedTextField(
        value = text,
        onValueChange = onChange,
        modifier = modifier,
        label = { Text(text = label) },
        placeholder = { Text(text = stringResource(R.string.filter_date_placeholder)) },
        supportingText = error?.let { { Text(text = it) } },
        isError = error != null,
        visualTransformation = DateDigitsTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
    )
}

/** Shows the typed digits with the dashes of YYYY-MM-DD; the cursor follows (rules in DateDigits). Also the buy date (09). */
internal object DateDigitsTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val typed = text.text
        val mapping =
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int = DateDigits.toShownOffset(offset, typed)

                override fun transformedToOriginal(offset: Int): Int = DateDigits.toTypedOffset(offset, typed)
            }
        return TransformedText(AnnotatedString(DateDigits.format(typed)), mapping)
    }
}

// The chips after All, in the order of view 7; Buy and Sell follow them (28).
private val FILTER_TYPES = listOf(CashType.DEPOSIT, CashType.WITHDRAWAL, CashType.INTEREST_DIVIDEND, CashType.ADJUSTMENT)

/** "Buy" or "Sell", Transaction history's chip labels (27). */
@Composable
private fun tradeSideLabel(side: TradeSide): String = stringResource(
    when (side) {
        TradeSide.BUY -> R.string.history_chip_buy
        TradeSide.SELL -> R.string.history_chip_sell
    },
)

/**
 * A direct-entry chip (view 7). While a ticker is picked it is disabled: not tappable, read as disabled by the
 * platform, greyed by Material, and drawn with a dashed outline in the theme's outline color instead of its own
 * (wireframe 07 marker 3; 28 decision 3).
 */
@Composable
private fun DirectChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val dashed = MaterialTheme.colorScheme.outline
    val shape = FilterChipDefaults.shape
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = label) },
        enabled = enabled,
        border = if (enabled) FilterChipDefaults.filterChipBorder(enabled = true, selected = selected) else null,
        modifier =
        if (enabled) {
            Modifier
        } else {
            Modifier.drawBehind {
                val width = 1.dp.toPx()
                val dash = 4.dp.toPx()
                val outline = shape.createOutline(Size(size.width - width, size.height - width), layoutDirection, this)
                translate(width / 2, width / 2) {
                    drawOutline(outline, dashed, style = Stroke(width, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash))))
                }
            }
        },
    )
}

@Composable
private fun BalanceCard(
    balance: String,
    spokenBalance: String,
    onCashEntry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spoken = stringResource(R.string.cash_balance_spoken, spokenBalance)
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.clearAndSetSemantics { contentDescription = spoken }) {
                Text(
                    text = stringResource(R.string.cash_balance_label),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = balance,
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
            Button(onClick = onCashEntry) {
                Text(text = stringResource(R.string.cash_entry_button))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EntryRow(
    row: CashRowModel.Entry,
    onOpen: () -> Unit,
) {
    val type = cashTypeLabel(row.type)
    val base =
        stringResource(
            R.string.cash_row_spoken,
            type,
            row.date,
            spoken(row.spokenAmount),
            spoken(row.spokenBalanceAfter),
        )
    // The note is read last, after the figures (05 decision 3).
    val spoken = if (row.note.isEmpty()) base else "$base, ${row.note}"
    // A wrapping two-part row, so nothing is cut off at the largest font (criterion 39). Tapping opens the detail.
    FlowRow(
        modifier = Modifier.fillMaxWidth().opensWith(spoken, onOpen),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Column {
            Text(
                text = type,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = stringResource(R.string.date_with_et, row.date),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (row.note.isNotEmpty()) {
                Text(
                    text = row.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Amounts(row)
    }
}

/**
 * A trade line (view 7; 09 wording 3, 11 wording 4, 13b wording 5): "Buy AAPL", "Sell AAPL" or "Reversal of buy AAPL",
 * the date with "ET" and the balance after it, and "For portfolio Main" where a direct entry shows its note. Read by
 * TalkBack with the buy, sell or reversal sentence; a reversal's starts with its title, the others with the ticker.
 * Tapping opens its transaction (criterion 29; 13a wording 4).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TradeRow(
    row: CashRowModel.Trade,
    onOpen: () -> Unit,
) {
    val title = tradeTitle(row)
    val spoken =
        stringResource(
            when (row.kind) {
                TransactionType.SELL -> R.string.trade_sell_spoken
                TransactionType.BUY -> R.string.trade_buy_spoken
                else -> R.string.trade_reversal_spoken
            },
            if (row.kind == TransactionType.REVERSAL) title else row.ticker,
            row.portfolio,
            row.date,
            spoken(row.spokenAmount),
            spoken(row.spokenBalanceAfter),
        )
    FlowRow(
        modifier = Modifier.fillMaxWidth().opensWith(spoken, onOpen),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = stringResource(R.string.date_with_et, row.date),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.trade_for_portfolio, row.portfolio),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Amounts(row)
    }
}

/** A row's right-hand part: the signed amount, then the balance after it; it takes the rest of the row, or wraps below. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowScope.Amounts(row: CashRowModel) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.End,
    ) {
        Text(
            text = row.amount,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.End,
        )
        Text(
            text = stringResource(R.string.cash_balance_after, row.balanceAfter),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
        )
    }
}

// Shared with the cash entry form and review. Trade lines (09) show buy, ticker and portfolio instead (TradeRow).
@Composable
internal fun cashTypeLabel(type: CashType): String = when (type) {
    CashType.OPENING_BALANCE -> stringResource(R.string.opening_balance)
    CashType.DEPOSIT -> stringResource(R.string.cash_type_deposit)
    CashType.WITHDRAWAL -> stringResource(R.string.cash_type_withdrawal)
    CashType.INTEREST_DIVIDEND -> stringResource(R.string.cash_type_interest_dividend)
    CashType.ADJUSTMENT -> stringResource(R.string.cash_type_adjustment)
    CashType.TRADE -> stringResource(R.string.cash_type_trade)
}

/** A cash amount with its sign and no color (section 6): +$10,000.00 or −$200.00. */
internal fun signedAmount(cents: Long): String = if (cents > 0L) "+${Money.format(cents)}" else Money.format(cents)

/** "10,000.00" or "minus 200.00", for TalkBack. */
@Composable
internal fun spokenDollars(cents: Long): String {
    val digits = Money.format(cents).substringAfter('$')
    return if (cents < 0L) stringResource(R.string.spoken_minus, digits) else digits
}

/** "plus 10,000.00" or "minus 200.00", for TalkBack. */
@Composable
internal fun spokenSigned(cents: Long): String {
    if (cents <= 0L) return spokenDollars(cents)
    return stringResource(R.string.spoken_plus, Money.format(cents).substringAfter('$'))
}
