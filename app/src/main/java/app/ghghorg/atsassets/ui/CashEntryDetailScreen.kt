package app.ghghorg.atsassets.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashType

/**
 * The cash entry detail (wireframe 10b, frame H; iteration 05): type, date with ET, signed amount and note, then
 * one status line and Reverse when it is offered (spec 4.6). The links open the other entry's detail.
 */
@Composable
fun CashEntryDetail(
    state: AppState,
    onOpenEntry: (String) -> Unit,
    onReverse: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    val entry = state.detailEntry ?: return
    val reversedBy = state.detailReversedBy
    val reverses = state.detailReverses
    val defaultNote = stringResource(R.string.reversal_note_default, cashTypeLower(entry.type), entry.date.toString())
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onBack) {
            Text(text = stringResource(R.string.back_button))
        }
        Text(
            text = cashTypeLabel(entry.type),
            style = MaterialTheme.typography.titleLarge,
        )
        EntryCard(entry)
        when {
            entry.type == CashType.OPENING_BALANCE -> StatusLine(stringResource(R.string.detail_opening_not_reversible))

            reversedBy != null ->
                LinkButton(stringResource(R.string.detail_reversed_on, reversedBy.date.toString())) { onOpenEntry(reversedBy.id) }

            reverses != null -> LinkButton(
                stringResource(R.string.detail_reverses, cashTypeLabel(reverses.type), reverses.date.toString()),
            ) { onOpenEntry(reverses.id) }

            else -> StatusLine(stringResource(R.string.detail_not_reversed))
        }
        if (state.reverseOffered) {
            Button(onClick = { onReverse(defaultNote) }) {
                Text(text = stringResource(R.string.reverse_button))
            }
        }
    }
}

/**
 * The reversal of a cash entry (wireframe 10b, frame I marker 19): the original fixed, an Adjustment with the
 * opposite amount, the date and the pre-filled note (05 decision 1). One screen, no separate review (spec 4.6).
 */
@Composable
fun CashReversal(
    state: AppState,
    onDateChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = !state.saving, onBack = onCancel)
    val original = state.detailEntry ?: return
    val refusals = state.refusals
    val opposite = signedAmount(-original.amountCents)
    val dateError = dateMessage(refusals.date)
    val noteError = noteMessage(refusals.noteHasLineBreak, refusals.noteRequired)
    Column(
        modifier = modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.reversal_title, cashTypeLower(original.type)),
            style = MaterialTheme.typography.titleLarge,
        )
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(R.string.reversal_reverses_label),
                    style = MaterialTheme.typography.bodyMedium,
                )
                ReviewRow(cashTypeLabel(original.type), stringResource(R.string.date_with_et, original.date.toString()))
                ReviewRow(stringResource(R.string.amount_label), signedAmount(original.amountCents))
            }
        }
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(R.string.reversal_type_fixed),
                    style = MaterialTheme.typography.bodyLarge,
                )
                ReviewRow(stringResource(R.string.amount_label), opposite)
                ReviewRow(stringResource(R.string.cash_effect_label), opposite, bold = true)
            }
        }
        OutlinedTextField(
            value = state.reversal.dateText,
            onValueChange = onDateChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(R.string.reversal_date_label)) },
            supportingText = {
                Column {
                    if (dateError != null) Text(text = dateError)
                    Text(text = stringResource(R.string.reversal_date_note, original.date.toString()))
                }
            },
            isError = refusals.date != null,
            singleLine = true,
        )
        OutlinedTextField(
            value = state.reversal.noteText,
            onValueChange = onNoteChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(R.string.note_label_required)) },
            supportingText = { if (noteError != null) Text(text = noteError) },
            isError = noteError != null,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        )
        listOfNotNull(saveMessage(refusals.save), cashMessage(refusals.cash)).forEach { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        ReversalButtons(state.saving, onConfirm, onCancel)
    }
}

@Composable
private fun EntryCard(entry: CashEntry) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ReviewRow(stringResource(R.string.type_label), cashTypeLabel(entry.type))
            ReviewRow(stringResource(R.string.date_label), stringResource(R.string.date_with_et, entry.date.toString()))
            ReviewRow(stringResource(R.string.amount_label), signedAmount(entry.amountCents))
            if (entry.note.isEmpty()) {
                ReviewRow(stringResource(R.string.note_row_label), stringResource(R.string.note_none), muted = true, alignStart = true)
            } else {
                ReviewRow(stringResource(R.string.note_row_label), entry.note, alignStart = true)
            }
        }
    }
}

@Composable
private fun StatusLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** A link to the other entry of a reversal pair (spec 4.6: the chain is traceable). */
@Composable
private fun LinkButton(
    text: String,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick) {
        Text(text = text)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReversalButtons(
    saving: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(onClick = onConfirm, enabled = !saving) {
            Text(text = stringResource(R.string.confirm_button))
        }
        TextButton(onClick = onCancel, enabled = !saving) {
            Text(text = stringResource(R.string.cancel_button))
        }
    }
}

/** The type in lower case, for the reversal title and the pre-filled note (wireframe 10b, marker 19). */
@Composable
private fun cashTypeLower(type: CashType): String = when (type) {
    CashType.DEPOSIT -> stringResource(R.string.cash_type_deposit_lower)
    CashType.WITHDRAWAL -> stringResource(R.string.cash_type_withdrawal_lower)
    CashType.INTEREST_DIVIDEND -> stringResource(R.string.cash_type_interest_dividend_lower)
    CashType.ADJUSTMENT -> stringResource(R.string.cash_type_adjustment_lower)
    CashType.OPENING_BALANCE, CashType.TRADE -> cashTypeLabel(type)
}
