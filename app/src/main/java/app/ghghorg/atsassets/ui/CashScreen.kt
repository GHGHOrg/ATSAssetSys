package app.ghghorg.atsassets.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.Money
import app.ghghorg.atsassets.core.TradeSide
import app.ghghorg.atsassets.core.TransactionType
import app.ghghorg.atsassets.ui.design.AtsButton
import app.ghghorg.atsassets.ui.design.AtsCard
import app.ghghorg.atsassets.ui.design.AtsChip
import app.ghghorg.atsassets.ui.design.AtsDenseField
import app.ghghorg.atsassets.ui.design.AtsRowDivider
import app.ghghorg.atsassets.ui.design.AtsScreen
import app.ghghorg.atsassets.ui.design.AtsTextField
import app.ghghorg.atsassets.ui.design.ButtonKind
import app.ghghorg.atsassets.ui.design.ButtonSize
import app.ghghorg.atsassets.ui.design.CardKind
import app.ghghorg.atsassets.ui.theme.AtsIcon
import app.ghghorg.atsassets.ui.theme.AtsTheme
import app.ghghorg.atsassets.ui.theme.LucideIcon
import app.ghghorg.atsassets.ui.theme.tokenDp
import app.ghghorg.atsassets.ui.theme.typeStyle

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
 *
 * 44a step 8 group (c) (drawing spec/wireframes/design/07-cash.html; D-29, D-30, D-38): under the title band; the balance
 * as the summary card with Cash entry in its own outline; the filters in a card (dense From and To with their calendar,
 * the Ticker pick field, the catalog's chips, a disabled one at 38%); each row with its chevron.
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
        AtsScreen(stringResource(R.string.tab_cash), Modifier.fillMaxSize().then(under)) {
            CashList(state, model, onCashEntry, onOpenEntry, onOpenTransaction, filterActions, Modifier.weight(1f).fillMaxWidth())
        }
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
    val colors = AtsTheme.colors
    val shape = RoundedCornerShape(tokenDp("radiusDefault"))
    val gap = tokenDp("spaceGap")
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(tokenDp("spaceMargin")),
    ) {
        item(key = "balance") {
            BalanceCard(model.balance, spoken(model.spokenBalance), onCashEntry, Modifier.padding(bottom = gap))
        }
        if (!model.hasEntries) {
            item(key = "no entries") { PlainLineCard(stringResource(R.string.cash_no_entries)) }
            return@LazyColumn
        }
        item(key = "filters") {
            FilterCard(state.cashFilterForm, model, filterActions, Modifier.padding(bottom = gap))
        }
        if (model.rows.isEmpty()) {
            item(key = "no match") { PlainLineCard(stringResource(R.string.cash_no_match)) }
            return@LazyColumn
        }
        val last = model.rows.lastIndex
        itemsIndexed(model.rows, key = { _, row -> row.key }) { index, row ->
            Column(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .cardSlice(top = index == 0, bottom = index == last, shape, colors.card),
            ) {
                if (index > 0) AtsRowDivider()
                when (row) {
                    is CashRowModel.Trade -> TradeRow(row, onOpen = { onOpenTransaction(row.transactionId) })
                    is CashRowModel.Entry -> EntryRow(row, onOpen = { onOpenEntry(row.key) })
                }
            }
        }
    }
}

/** One plain line in a card in place of a list (D-39). */
@Composable
internal fun PlainLineCard(text: String) {
    AtsCard {
        Text(text = text, style = typeStyle("body"), color = AtsTheme.colors.textSecondary)
    }
}

/**
 * One row's slice of a card (15d): the card's fill, with the top rounded corners only on the first row and the bottom
 * ones only on the last. The card's shadow is not drawn here: a shadow per slice would show a seam at each row.
 */
private fun Modifier.cardSlice(
    top: Boolean,
    bottom: Boolean,
    shape: Shape,
    fill: Color,
): Modifier = drawBehind {
    // A missing edge is drawn beyond the row, where the clip below cuts it off with its corners.
    val beyond = size.height
    val from = if (top) 0f else -beyond
    val to = if (bottom) size.height else size.height + beyond
    val outline = shape.createOutline(Size(size.width, to - from), layoutDirection, this)
    clipRect {
        translate(0f, from) { drawOutline(outline, fill) }
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
 * From and To with the date note, the Ticker field, the type chips, and, while a filter is active, the count line and
 * Clear filters (wireframe 07 markers 2, 3, 5 and 6; 06 decisions 1 and 3).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterCard(
    form: CashFilterForm,
    model: CashListModel,
    actions: CashFilterActions,
    modifier: Modifier = Modifier,
) {
    val colors = AtsTheme.colors
    AtsCard(modifier = modifier) {
        DenseDateFields(form.fromText, form.toText, dateMessage(model.fromRefusal), dateMessage(model.toRefusal), actions.onFromChange, actions.onToChange)
        TickerField(model.ticker, actions.onOpenTickerMenu)
        FlowRow(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(tokenDp("fieldDensePadding")),
        ) {
            AtsChip(stringResource(R.string.filter_all), selected = form.type == null && form.trade == null, onClick = { actions.onTypeChange(null) })
            for (type in FILTER_TYPES) {
                // While a ticker is picked a direct-entry chip is the catalog's disabled chip: not tappable, at 38%, read as
                // disabled (wireframe 07 marker 3; 28 decision 3; D-29), and the line under the chips says why.
                AtsChip(cashTypeLabel(type), selected = form.type == type, onClick = { actions.onTypeChange(type) }, enabled = model.directChipsEnabled)
            }
            for (side in TradeSide.entries) {
                AtsChip(tradeSideLabel(side), selected = form.trade == side, onClick = { actions.onTradeChange(side) })
            }
        }
        if (!model.directChipsEnabled) {
            Text(text = stringResource(R.string.cash_chips_unavailable), style = typeStyle("note"), color = colors.textSecondary)
        }
        if (model.filterActive) {
            val count = model.rows.size
            Text(text = pluralStringResource(R.plurals.cash_entries_shown, count, count), style = typeStyle("small"), color = colors.textSecondary)
            AtsButton(stringResource(R.string.filter_clear), actions.onClear, kind = ButtonKind.TEXT, size = ButtonSize.SMALL)
        }
    }
}

/**
 * From and To as dense date fields with their calendar, side by side while they fit and To under From when they do not,
 * then the New York date note (drawing 07 frame E; D-18, D-28). Shared with Transaction history (27).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DenseDateFields(
    fromText: String,
    toText: String,
    fromError: String?,
    toError: String?,
    onFromChange: (String) -> Unit,
    onToChange: (String) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(tokenDp("spaceMargin"))) {
        FilterDateField(stringResource(R.string.filter_from), fromText, fromError, onFromChange)
        FilterDateField(stringResource(R.string.filter_to), toText, toError, onToChange)
    }
    Text(text = stringResource(R.string.date_note), style = typeStyle("note"), color = AtsTheme.colors.textSecondary)
}

/**
 * A From or To field (06), shared with Realized gains (26) and Transaction history (27): digits on a number keyboard, the
 * dashes shown, not typed; the calendar icon opens the dense picker (spec 6, Date fields: the filters' From and To). With
 * [dense] the catalog's dense field with its label beside it (drawings 07 and 08); otherwise the standard field with its
 * label on the outline (drawing 05). Either way the field itself carries the label, for TalkBack and the screen tests.
 */
@Composable
internal fun FilterDateField(
    label: String,
    text: String,
    error: String?,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    dense: Boolean = true,
) {
    val open = datePickerOpener(text, onChange, dense = true)
    val pick = stringResource(R.string.date_pick_open)
    val placeholder = stringResource(R.string.filter_date_placeholder)
    val keyboard = KeyboardOptions(keyboardType = KeyboardType.Number)
    if (dense) {
        Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(tokenDp("fieldDensePadding"))) {
            // Beside the field, at its middle; TalkBack reads the label with the field, not twice.
            Box(modifier = Modifier.heightIn(min = tokenDp("spaceTouch")).clearAndSetSemantics { }, contentAlignment = Alignment.CenterStart) {
                Text(text = label, style = typeStyle("small"), color = AtsTheme.colors.text)
            }
            AtsDenseField(
                value = text,
                onValueChange = onChange,
                refusal = error,
                trailing = LucideIcon.CALENDAR,
                trailingLabel = pick,
                onTrailing = open,
                keyboardOptions = keyboard,
                label = label,
                placeholder = placeholder,
                visualTransformation = DateDigitsTransformation,
            )
        }
    } else {
        AtsTextField(
            value = text,
            onValueChange = onChange,
            label = label,
            modifier = modifier,
            refusal = error,
            trailing = LucideIcon.CALENDAR,
            trailingLabel = pick,
            onTrailing = open,
            keyboardOptions = keyboard,
            placeholder = placeholder,
            visualTransformation = DateDigitsTransformation,
        )
    }
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
 * The balance as the summary card (drawing 07; D-38): "Cash balance" and the 32 figure, read by TalkBack as one line, and
 * Cash entry as an outline button in the summary card's own colors; it moves under the figure where they do not fit.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BalanceCard(
    balance: String,
    spokenBalance: String,
    onCashEntry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AtsTheme.colors
    val spoken = stringResource(R.string.cash_balance_spoken, spokenBalance)
    AtsCard(modifier = modifier, kind = CardKind.SUMMARY) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(tokenDp("cardGap")),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.clearAndSetSemantics { contentDescription = spoken }) {
                Text(text = stringResource(R.string.cash_balance_label), style = typeStyle("small"), color = colors.onSummaryMuted)
                Text(text = balance, style = typeStyle("bigFigure"), color = colors.onSummary)
            }
            AtsButton(stringResource(R.string.cash_entry_button), onCashEntry, kind = ButtonKind.OUTLINE_ON_SUMMARY)
        }
    }
}

/**
 * A row of the list (drawing 07): [title] in 16, [lines] in secondary 14 under it, the signed amount in 16 and the balance
 * after it in condensed 12 on the right (uncolored, section 6), and a chevron, as it opens its detail or transaction. Read
 * by TalkBack as [spoken]. At the largest font the figures move under the title (C26).
 */
@Composable
private fun CashRowLayout(
    spoken: String,
    onOpen: () -> Unit,
    title: String,
    lines: List<String>,
    row: CashRowModel,
) {
    val colors = AtsTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = tokenDp("spaceTouch"))
            .opensWith(spoken, onOpen)
            .padding(tokenDp("spaceInset")),
        horizontalArrangement = Arrangement.spacedBy(tokenDp("spaceInset")),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StartEndRow(
            modifier = Modifier.weight(1f),
            start = {
                Column {
                    Text(text = title, style = typeStyle("body"), color = colors.text)
                    lines.forEach { Text(text = it, style = typeStyle("small"), color = colors.textSecondary) }
                }
            },
            end = {
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = row.amount, style = typeStyle("body"), color = colors.text, textAlign = TextAlign.End)
                    Text(
                        text = stringResource(R.string.cash_balance_after, row.balanceAfter),
                        style = typeStyle("oneDay"),
                        color = colors.textSecondary,
                        textAlign = TextAlign.End,
                    )
                }
            },
        )
        AtsIcon(LucideIcon.CHEVRON_RIGHT, tint = colors.iconChevron, size = tokenDp("fieldMarkSize"))
    }
}

/** A direct entry: its type, its date with "ET" and its note; read with the note last (05 decision 3). */
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
    val date = stringResource(R.string.date_with_et, row.date)
    CashRowLayout(spoken, onOpen, type, if (row.note.isEmpty()) listOf(date) else listOf(date, row.note), row)
}

/**
 * A trade line (view 7; 09 wording 3, 11 wording 4, 13b wording 5): "Buy AAPL", "Sell AAPL" or "Reversal of buy AAPL",
 * the date with "ET" and the balance after it, and "For portfolio Main" where a direct entry shows its note. Read by
 * TalkBack with the buy, sell or reversal sentence; a reversal's starts with its title, the others with the ticker.
 * Tapping opens its transaction (criterion 29; 13a wording 4).
 */
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
    val lines = listOf(stringResource(R.string.date_with_et, row.date), stringResource(R.string.trade_for_portfolio, row.portfolio))
    CashRowLayout(spoken, onOpen, title, lines, row)
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
