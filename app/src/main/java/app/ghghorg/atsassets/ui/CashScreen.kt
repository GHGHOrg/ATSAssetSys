package app.ghghorg.atsassets.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.core.CashRow
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.Money

/**
 * The Cash account (view 7, wireframe 07 frame A): the balance, the Cash entry button (the opening balance until
 * it exists, then a deposit or withdrawal, iteration 03), and the rows. A row opens its detail and shows its note
 * (05 decision 3). Filters arrive in 06 and 28, trade lines in 09. Cash is not colored.
 */
@Composable
fun CashScreen(
    state: AppState,
    onCashEntry: () -> Unit,
    onOpenEntry: (String) -> Unit,
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
        val rows = state.cashRows
        if (rows.isEmpty()) {
            Text(
                text = stringResource(R.string.cash_no_entries),
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    rows.forEachIndexed { index, row ->
                        if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        EntryRow(row, onOpen = { onOpenEntry(row.entry.id) })
                    }
                }
            }
        }
    }
}

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

// Shared with the cash entry form and review. Trade lines (09) show buy or sell, ticker and portfolio instead.
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
