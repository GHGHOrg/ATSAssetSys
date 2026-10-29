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
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.core.AmountError
import app.ghghorg.atsassets.core.AmountResult
import app.ghghorg.atsassets.core.CashEntryRefusal
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.MarketDate
import app.ghghorg.atsassets.core.Money

/**
 * The cash entry form (wireframe 10b): the opening balance until it exists (frame C), then a deposit or
 * withdrawal (frame A, iteration 03). The typed text lives in the ViewModel.
 */
@Composable
fun CashEntryForm(
    state: AppState,
    onTypeChange: (CashType) -> Unit,
    onDateChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onReview: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    val refusals = state.refusals
    val type = state.form.type
    val opening = type == CashType.OPENING_BALANCE
    val dateError = dateMessage(refusals.date)
    val amountError = amountMessage(refusals.amount)
    val noteError = noteMessage(refusals.noteHasLineBreak)
    Column(
        modifier = modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onBack) {
            Text(text = stringResource(R.string.back_button))
        }
        Text(
            text = stringResource(if (opening) R.string.opening_balance else R.string.cash_entry_button),
            style = MaterialTheme.typography.titleLarge,
        )
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = stringResource(R.string.type_label),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (opening) {
                    Text(
                        text = stringResource(R.string.opening_balance),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = stringResource(R.string.opening_balance_hint),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    TypeChips(type, onTypeChange)
                }
            }
        }
        OutlinedTextField(
            value = state.form.dateText,
            onValueChange = onDateChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(R.string.date_label)) },
            supportingText = {
                Column {
                    if (dateError != null) Text(text = dateError)
                    Text(text = stringResource(R.string.date_note))
                }
            },
            isError = refusals.date != null,
            singleLine = true,
        )
        OutlinedTextField(
            value = state.form.amountText,
            onValueChange = onAmountChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(R.string.amount_label)) },
            supportingText = {
                Column {
                    if (amountError != null) Text(text = amountError)
                    amountLine(type)?.let { Text(text = it) }
                }
            },
            isError = refusals.amount != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
        )
        OutlinedTextField(
            value = state.form.noteText,
            onValueChange = onNoteChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(R.string.note_label)) },
            supportingText = { if (noteError != null) Text(text = noteError) },
            isError = refusals.noteHasLineBreak,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        )
        listOfNotNull(saveMessage(refusals.save), cashMessage(refusals.cash)).forEach { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Button(
            onClick = onReview,
            modifier = Modifier.align(Alignment.End),
        ) {
            Text(text = stringResource(R.string.review_button))
        }
    }
}

/** The review screen (wireframe 10b, frame D). Nothing is saved until Confirm and save. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CashEntryReview(
    state: AppState,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = !state.saving, onBack = onBack)
    val type = state.form.type
    val dateText = MarketDate.parse(state.form.dateText)?.toString() ?: state.form.dateText.trim()
    val cents = (Money.parseCents(state.form.amountText) as? AmountResult.Cents)?.value
    // The amount is typed positive; the sign comes from the type (wireframe 10b, frame D).
    val signed = cents?.let { signedAmount(if (type == CashType.WITHDRAWAL) -it else it) }.orEmpty()
    val note = state.form.noteText
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.review_title),
            style = MaterialTheme.typography.titleLarge,
        )
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ReviewRow(stringResource(R.string.type_label), cashTypeLabel(type))
                ReviewRow(stringResource(R.string.date_label), stringResource(R.string.date_with_et, dateText))
                ReviewRow(stringResource(R.string.amount_label), signed)
                if (note.isEmpty()) {
                    ReviewRow(
                        stringResource(R.string.note_row_label),
                        stringResource(R.string.note_none),
                        muted = true,
                        alignStart = true,
                    )
                } else {
                    ReviewRow(stringResource(R.string.note_row_label), note, alignStart = true)
                }
                ReviewRow(stringResource(R.string.cash_effect_label), signed, bold = true)
            }
        }
        Text(
            text = stringResource(R.string.review_hint),
            style = MaterialTheme.typography.bodyMedium,
        )
        saveMessage(state.refusals.save)?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = onConfirm,
                enabled = !state.saving,
            ) {
                Text(text = stringResource(R.string.confirm_button))
            }
            TextButton(
                onClick = onBack,
                enabled = !state.saving,
            ) {
                Text(text = stringResource(R.string.back_to_edit_button))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReviewRow(
    label: String,
    value: String,
    muted: Boolean = false,
    bold: Boolean = false,
    alignStart: Boolean = false,
) {
    val valueColor =
        if (muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = valueColor,
            fontWeight = if (bold) FontWeight.Medium else FontWeight.Normal,
            textAlign = if (alignStart) TextAlign.Start else TextAlign.End,
        )
    }
}

/** Deposit and Withdrawal, one at a time (03 decision 2: the other two types arrive in 04). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TypeChips(
    selected: CashType,
    onTypeChange: (CashType) -> Unit,
) {
    FlowRow(
        modifier = Modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (type in listOf(CashType.DEPOSIT, CashType.WITHDRAWAL)) {
            FilterChip(
                selected = type == selected,
                onClick = { onTypeChange(type) },
                label = { Text(text = cashTypeLabel(type)) },
            )
        }
    }
}

/** The line under the amount (wireframe 10b, frame A). None for the opening balance. */
@Composable
private fun amountLine(type: CashType): String? = when (type) {
    CashType.DEPOSIT -> stringResource(R.string.amount_line_deposit)
    CashType.WITHDRAWAL -> stringResource(R.string.amount_line_withdrawal)
    else -> null
}

// NoOpeningBalance, TypeNotAllowed, AmountNotPositive and NoteHasLineBreak cannot reach here: the form only
// offers Deposit and Withdrawal after the opening balance, the amount rules and the note come first.
@Composable
private fun cashMessage(refusal: CashEntryRefusal?): String? = when (refusal) {
    is CashEntryRefusal.NegativeCash ->
        stringResource(R.string.cash_negative, refusal.date.toString(), Money.format(refusal.shortfallCents))

    is CashEntryRefusal.BeforeOpeningBalance ->
        stringResource(R.string.cash_before_opening_balance, refusal.openingDate.toString())

    CashEntryRefusal.TooLarge -> stringResource(R.string.cash_too_large)

    else -> null
}

@Composable
private fun amountMessage(error: AmountError?): String? = when (error) {
    null -> null
    AmountError.EMPTY -> stringResource(R.string.amount_empty)
    AmountError.BAD_FORMAT -> stringResource(R.string.amount_bad_format)
    AmountError.TOO_MANY_DECIMALS -> stringResource(R.string.amount_too_many_decimals)
    AmountError.ZERO -> stringResource(R.string.amount_zero)
    AmountError.TOO_LARGE -> stringResource(R.string.amount_too_large)
}

@Composable
private fun dateMessage(refusal: DateRefusal?): String? = when (refusal) {
    null -> null
    DateRefusal.EMPTY -> stringResource(R.string.date_empty)
    DateRefusal.NOT_A_DATE -> stringResource(R.string.date_not_a_date)
}

@Composable
private fun noteMessage(lineBreak: Boolean): String? = if (lineBreak) stringResource(R.string.note_line_break) else null

@Composable
private fun saveMessage(refusal: SaveRefusal?): String? = when (refusal) {
    null -> null
    SaveRefusal.ALREADY_EXISTS -> stringResource(R.string.save_already_exists)
    SaveRefusal.SAVE_FAILED -> stringResource(R.string.save_failed)
}
