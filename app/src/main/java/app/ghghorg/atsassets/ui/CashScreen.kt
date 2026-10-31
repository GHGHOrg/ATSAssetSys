package app.ghghorg.atsassets.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.core.CashRow
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.Money

/**
 * The Cash account (view 7, wireframe 07 frame A): the balance, the Cash entry button (the opening balance until
 * it exists, then a deposit or withdrawal, iteration 03), and the rows. A row opens its detail and shows its note
 * (05 decision 3). The date range and type filters (06) sit between the balance and the list; the ticker and the
 * Buy and Sell chips arrive in 28, trade lines in 09. Cash is not colored.
 */
@Composable
fun CashScreen(
    state: AppState,
    onCashEntry: () -> Unit,
    onOpenEntry: (String) -> Unit,
    filterActions: CashFilterActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.tab_cash),
            style = MaterialTheme.typography.titleLarge,
        )
        BalanceCard(state.cashBalanceCents, onCashEntry)
        if (state.cashRows.isEmpty()) {
            Text(
                text = stringResource(R.string.cash_no_entries),
                style = MaterialTheme.typography.bodyLarge,
            )
            return@Column
        }
        FilterCard(state, filterActions)
        val rows = state.cashFilteredRows
        if (rows.isEmpty()) {
            Text(
                text = stringResource(R.string.cash_no_match),
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    rows.forEachIndexed { index, row ->
                        if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        val trade = state.transactions.firstOrNull { it.id == row.entry.transactionId }
                        if (trade != null) {
                            val portfolio = state.portfolios.firstOrNull { it.id == trade.portfolioId }?.name.orEmpty()
                            TradeRow(row, trade.ticker, portfolio)
                        } else {
                            EntryRow(row, onOpen = { onOpenEntry(row.entry.id) })
                        }
                    }
                }
            }
        }
    }
}

/** The filter actions of the Cash list (iteration 06), passed together to keep the screen's parameters short. */
data class CashFilterActions(
    val onFromChange: (String) -> Unit,
    val onToChange: (String) -> Unit,
    val onTypeChange: (CashType?) -> Unit,
    val onClear: () -> Unit,
)

/**
 * From and To with the date note, the type chips, and, while a filter is active, Clear filters and the count line
 * (wireframe 07 markers 2, 3, 5 and 6; 06 decisions 1 and 3).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterCard(
    state: AppState,
    actions: CashFilterActions,
) {
    val form = state.cashFilterForm
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterDateField(
                    label = stringResource(R.string.filter_from),
                    text = form.fromText,
                    error = dateMessage(state.filterFromRefusal),
                    onChange = actions.onFromChange,
                    modifier = Modifier.weight(1f),
                )
                FilterDateField(
                    label = stringResource(R.string.filter_to),
                    text = form.toText,
                    error = dateMessage(state.filterToRefusal),
                    onChange = actions.onToChange,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = stringResource(R.string.date_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                modifier = Modifier.selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = form.type == null,
                    onClick = { actions.onTypeChange(null) },
                    label = { Text(text = stringResource(R.string.filter_all)) },
                )
                for (type in FILTER_TYPES) {
                    FilterChip(
                        selected = form.type == type,
                        onClick = { actions.onTypeChange(type) },
                        label = { Text(text = cashTypeLabel(type)) },
                    )
                }
            }
            if (state.filterActive) {
                val count = state.cashFilteredRows.size
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

@Composable
private fun FilterDateField(
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

// The chips after All, in the order of view 7; Buy and Sell arrive in 28.
private val FILTER_TYPES = listOf(CashType.DEPOSIT, CashType.WITHDRAWAL, CashType.INTEREST_DIVIDEND, CashType.ADJUSTMENT)

@Composable
private fun BalanceCard(
    cents: Long,
    onCashEntry: () -> Unit,
) {
    val spoken = stringResource(R.string.cash_balance_spoken, spokenDollars(cents))
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
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
                    text = Money.format(cents),
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
    row: CashRow,
    onOpen: () -> Unit,
) {
    val entry = row.entry
    val type = cashTypeLabel(entry.type)
    val date = entry.date.toString()
    val base =
        stringResource(
            R.string.cash_row_spoken,
            type,
            date,
            spokenSigned(entry.amountCents),
            spokenDollars(row.balanceAfterCents),
        )
    // The note is read last, after the figures (05 decision 3).
    val spoken = if (entry.note.isEmpty()) base else "$base, ${entry.note}"
    // A wrapping two-part row, so nothing is cut off at the largest font (criterion 39). Tapping opens the detail.
    FlowRow(
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
                text = stringResource(R.string.date_with_et, date),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (entry.note.isNotEmpty()) {
                Text(
                    text = entry.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                text = signedAmount(entry.amountCents),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.End,
            )
            Text(
                text = stringResource(R.string.cash_balance_after, Money.format(row.balanceAfterCents)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
            )
        }
    }
}

/**
 * A buy's trade line (view 7; 09 wording 3): "Buy AAPL", the date with "ET" and the balance after it, and
 * "For portfolio Main" where a direct entry shows its note. Not tappable until it opens its transaction (10 or 27).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TradeRow(
    row: CashRow,
    ticker: String,
    portfolio: String,
) {
    val entry = row.entry
    val date = entry.date.toString()
    val spoken =
        stringResource(
            R.string.trade_buy_spoken,
            ticker,
            portfolio,
            date,
            spokenSigned(entry.amountCents),
            spokenDollars(row.balanceAfterCents),
        )
    FlowRow(
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = spoken },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Column {
            Text(
                text = stringResource(R.string.trade_buy_title, ticker),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = stringResource(R.string.date_with_et, date),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.trade_for_portfolio, portfolio),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                text = signedAmount(entry.amountCents),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.End,
            )
            Text(
                text = stringResource(R.string.cash_balance_after, Money.format(row.balanceAfterCents)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
            )
        }
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
private fun spokenSigned(cents: Long): String {
    if (cents <= 0L) return spokenDollars(cents)
    return stringResource(R.string.spoken_plus, Money.format(cents).substringAfter('$'))
}
