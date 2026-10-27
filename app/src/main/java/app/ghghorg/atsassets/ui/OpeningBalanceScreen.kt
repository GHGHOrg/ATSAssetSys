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
import app.ghghorg.atsassets.core.MarketDate
import app.ghghorg.atsassets.core.Money

/** The opening balance form (wireframe 10b, frame C). The typed text lives in the ViewModel. */
@Composable
fun OpeningBalanceForm(
    state: AppState,
    onDateChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onReview: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    val refusals = state.refusals
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
            text = stringResource(R.string.opening_balance),
            style = MaterialTheme.typography.titleLarge,
        )
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = stringResource(R.string.type_label),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(R.string.opening_balance),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = stringResource(R.string.opening_balance_hint),
                    style = MaterialTheme.typography.bodyMedium,
                )
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
            supportingText = { if (amountError != null) Text(text = amountError) },
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
        saveMessage(refusals.save)?.let { message ->
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
fun OpeningBalanceReview(
    state: AppState,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = !state.saving, onBack = onBack)
    val dateText = MarketDate.parse(state.form.dateText)?.toString() ?: state.form.dateText.trim()
    val cents = (Money.parseCents(state.form.amountText) as? AmountResult.Cents)?.value
    val signed = cents?.let { "+${Money.format(it)}" }.orEmpty()
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
                ReviewRow(stringResource(R.string.type_label), stringResource(R.string.opening_balance))
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
