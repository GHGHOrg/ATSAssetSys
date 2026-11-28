package app.ghghorg.atsassets.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.core.AmountError
import app.ghghorg.atsassets.core.CashDirection
import app.ghghorg.atsassets.core.CashEntryRefusal
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.Money
import app.ghghorg.atsassets.ui.design.AtsChip
import app.ghghorg.atsassets.ui.design.AtsEntryFoot
import app.ghghorg.atsassets.ui.design.AtsReviewCard
import app.ghghorg.atsassets.ui.design.AtsRowDivider
import app.ghghorg.atsassets.ui.design.AtsScreen
import app.ghghorg.atsassets.ui.design.AtsTextField
import app.ghghorg.atsassets.ui.theme.AtsTheme
import app.ghghorg.atsassets.ui.theme.LucideIcon
import app.ghghorg.atsassets.ui.theme.tokenDp
import app.ghghorg.atsassets.ui.theme.typeStyle

/**
 * The cash entry form (wireframe 10b): the opening balance until it exists (frame C), then a deposit,
 * withdrawal (frame A, 03), interest/dividend or adjustment with a direction and a required note (frame B, 04).
 * The typed text lives in the ViewModel.
 */
@Composable
fun CashEntryForm(
    state: AppState,
    onTypeChange: (CashType) -> Unit,
    onDirectionChange: (CashDirection) -> Unit,
    onDateChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onReview: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    // The messages come only from their model (CR2): the screen picks the approved string for each.
    val shown = state.cashFormMessages
    val model = state.cashFormModel
    val originals = model.originals
    val type = state.form.type
    val adjustment = type == CashType.ADJUSTMENT
    val messages =
        shown.below.mapNotNull { message ->
            when (message) {
                is CashFormMessage.Save -> saveMessage(message.refusal)

                is CashFormMessage.Cash ->
                    if (message.replacing) {
                        replacingMessage(cashMessage(message.refusal, replacing = true))
                    } else {
                        cashMessage(message.refusal)
                    }

                is CashFormMessage.Replace -> replaceRuleMessage(state, message.refusal)
            }
        }
    AtsScreen(
        title =
        stringResource(
            when (model.title) {
                CashFormTitle.REPLACING -> R.string.cash_replacing_title
                CashFormTitle.OPENING -> R.string.opening_balance
                CashFormTitle.ENTRY -> R.string.cash_entry_button
            },
        ),
        modifier = modifier,
        onBack = onBack,
        backLabel = stringResource(R.string.back_button),
    ) {
        EntryColumn {
            model.banner?.let { CashReplacingBanner(it) }
            // A rule that stops the save is the blocked card at the top; a field's own rule stays on it (10b frame J).
            messages.forEach { Message(it) }
            ChoiceGroup(stringResource(R.string.type_label)) {
                if (model.title == CashFormTitle.OPENING) {
                    // The only type offered until it exists, and one line saying why (10b frame C).
                    AtsChip(stringResource(R.string.opening_balance), selected = true, onClick = {})
                } else {
                    TypeChips(type, onTypeChange)
                }
            }
            if (model.title == CashFormTitle.OPENING) {
                Text(text = stringResource(R.string.opening_balance_hint), style = typeStyle("note"), color = AtsTheme.colors.textSecondary)
            }
            originals.type?.let { was ->
                val label = cashTypeLabel(was)
                OriginalHint(label, stringResource(R.string.original_hint_spoken, label))
            }
            Column {
                AtsTextField(
                    value = state.form.dateText,
                    onValueChange = onDateChange,
                    label = stringResource(R.string.date_label),
                    modifier = Modifier.fillMaxWidth(),
                    refusal = dateMessage(shown.date),
                    note = stringResource(R.string.date_note),
                    trailing = LucideIcon.CALENDAR,
                    trailingLabel = stringResource(R.string.date_pick_open),
                    onTrailing = datePickerOpener(state.form.dateText, onDateChange, dense = false, digitsOnly = false),
                )
                OriginalLine(originals.date)
            }
            if (adjustment) {
                // Adjustment only: the direction sets the sign (wireframe 10b, marker 6; 04 decision 2).
                ChoiceGroup(stringResource(R.string.direction_label)) {
                    DirectionChips(state.form.direction, onDirectionChange)
                }
                originals.direction?.let { was ->
                    val label = stringResource(if (was == CashDirection.OUT) R.string.direction_out else R.string.direction_in)
                    OriginalHint(label, stringResource(R.string.original_hint_spoken, label))
                }
            }
            Column {
                AtsTextField(
                    value = state.form.amountText,
                    onValueChange = onAmountChange,
                    label = stringResource(R.string.amount_label),
                    modifier = Modifier.fillMaxWidth(),
                    refusal = amountMessage(shown.amount),
                    note = amountLine(type),
                    prefix = "$",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OriginalLine(originals.amount)
            }
            Column {
                AtsTextField(
                    value = state.form.noteText,
                    onValueChange = onNoteChange,
                    label = stringResource(if (adjustment) R.string.note_label_required else R.string.note_label),
                    modifier = Modifier.fillMaxWidth(),
                    refusal = noteMessage(shown.note == NoteMessage.LINE_BREAK, shown.note == NoteMessage.REQUIRED),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    singleLine = false,
                )
                originals.note?.let { was ->
                    val label = was.ifEmpty { stringResource(R.string.note_none) }
                    OriginalHint(label, stringResource(R.string.original_hint_spoken, label))
                }
            }
            AtsEntryFoot(stringResource(R.string.review_button), onReview)
        }
    }
}

/** A one-at-a-time choice (D-31 (1)): its label in secondary 14 over the catalog's chips. */
@Composable
private fun ChoiceGroup(
    label: String,
    chips: @Composable () -> Unit,
) {
    Column {
        Text(text = label, style = typeStyle("small"), color = AtsTheme.colors.textSecondary)
        chips()
    }
}

/**
 * The review screen (wireframe 10b, frame D), drawn from its model (CR2, open item 48): the values as label-and-value
 * rows, the cash effect as the total line, signed and uncolored, "Nothing is saved until you confirm." and the foot.
 * Nothing is saved until Confirm and save.
 */
@Composable
fun CashEntryReview(
    state: AppState,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = !state.saving, onBack = onBack)
    val model = state.cashReviewModel
    val signed = model.cashCents?.let { signedAmount(it) }.orEmpty()
    AtsScreen(
        title = stringResource(R.string.review_title),
        modifier = modifier,
        onBack = onBack,
        backLabel = stringResource(R.string.back_button),
        backEnabled = !state.saving,
    ) {
        EntryColumn {
            saveMessage(state.refusals.save)?.let { Message(it) }
            AtsReviewCard {
                ReviewRow(stringResource(R.string.type_label), cashTypeLabel(model.type))
                ReviewRow(stringResource(R.string.date_label), stringResource(R.string.date_with_et, model.date))
                ReviewRow(stringResource(R.string.amount_label), signed)
                NoteRow(model.note)
                ReviewRow(stringResource(R.string.cash_effect_label), signed, bold = true)
            }
            ReviewHint()
            AtsEntryFoot(
                primary = stringResource(R.string.confirm_button),
                onPrimary = onConfirm,
                enabled = !state.saving,
                secondary = stringResource(R.string.back_to_edit_button),
                onSecondary = onBack,
            )
        }
    }
}

/** The note as a review or detail row under its label, or "none" in the secondary color (10b frames D and H). */
@Composable
internal fun NoteRow(note: String) {
    if (note.isEmpty()) {
        ReviewRow(stringResource(R.string.note_row_label), stringResource(R.string.note_none), muted = true, alignStart = true)
    } else {
        ReviewRow(stringResource(R.string.note_row_label), note, alignStart = true)
    }
}

/**
 * A review or detail row (D-31 (4); 44a step 8 group (d)): the label in secondary 16 on the left, the value in 16 at the
 * right edge, beside it or under it (C26). [bold]: the result's total line, under a divider, in medium with the label in
 * the text color. [alignStart]: a long value under its label, full width (10c note 7). [spoken], when given, is what
 * TalkBack reads for the whole row, such as "Cash effect, minus 1,500.00 dollars" (09). [emphasis]: the total line alone in
 * its card, in medium with no divider (the combined cash effect; 10c note 4).
 */
@Composable
internal fun ReviewRow(
    label: String,
    value: String,
    muted: Boolean = false,
    bold: Boolean = false,
    alignStart: Boolean = false,
    spoken: String? = null,
    color: Color? = null,
    emphasis: Boolean = false,
) {
    val colors = AtsTheme.colors
    val valueColor = color ?: if (muted) colors.textSecondary else colors.text
    val strong = bold || emphasis
    val style = if (strong) typeStyle("sectionHeading") else typeStyle("body")
    val reading = if (spoken == null) Modifier else Modifier.clearAndSetSemantics { contentDescription = spoken }
    Column(modifier = Modifier.fillMaxWidth()) {
        if (bold) AtsRowDivider()
        val rowModifier = Modifier.fillMaxWidth().then(reading).padding(top = if (bold) tokenDp("cardGap") else 0.dp)
        if (alignStart) {
            Column(modifier = rowModifier) {
                Text(text = label, style = typeStyle("body"), color = colors.textSecondary)
                Text(text = value, style = style, color = valueColor)
            }
        } else {
            StartEndRow(
                modifier = rowModifier,
                start = { Text(text = label, style = style, color = if (strong) colors.text else colors.textSecondary) },
                end = { Text(text = value, style = style, color = valueColor, textAlign = TextAlign.End) },
            )
        }
    }
}

/** The four entry types, one at a time, as the catalog's chips (wireframe 10b, frame A; D-31 (1); 04). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TypeChips(
    selected: CashType,
    onTypeChange: (CashType) -> Unit,
) {
    val types = listOf(CashType.DEPOSIT, CashType.WITHDRAWAL, CashType.INTEREST_DIVIDEND, CashType.ADJUSTMENT)
    FlowRow(modifier = Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(tokenDp("fieldDensePadding"))) {
        for (type in types) {
            AtsChip(cashTypeLabel(type), selected = type == selected, onClick = { onTypeChange(type) })
        }
    }
}

/** An adjustment's direction, one at a time, as the catalog's chips (wireframe 10b, frame B). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DirectionChips(
    selected: CashDirection,
    onDirectionChange: (CashDirection) -> Unit,
) {
    FlowRow(modifier = Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(tokenDp("fieldDensePadding"))) {
        for (direction in listOf(CashDirection.IN, CashDirection.OUT)) {
            val label = if (direction == CashDirection.IN) R.string.direction_in else R.string.direction_out
            AtsChip(stringResource(label), selected = direction == selected, onClick = { onDirectionChange(direction) })
        }
    }
}

/** The line under the amount (wireframe 10b, frame A). None for the opening balance or an adjustment (04 decision 3). */
@Composable
private fun amountLine(type: CashType): String? = when (type) {
    CashType.DEPOSIT, CashType.INTEREST_DIVIDEND -> stringResource(R.string.amount_line_deposit)
    CashType.WITHDRAWAL -> stringResource(R.string.amount_line_withdrawal)
    else -> null
}

// Shared with the reversal screen. The other reasons cannot reach here: the form and the detail only offer
// what K1 allows, and the amount and note rules are shown on their own fields. While [replacing], the cash rule names
// the replacement (14a wording 1).
@Composable
internal fun cashMessage(
    refusal: CashEntryRefusal?,
    replacing: Boolean = false,
): String? = when (refusal) {
    is CashEntryRefusal.NegativeCash ->
        stringResource(
            if (replacing) R.string.replace_cash_negative else R.string.cash_negative,
            refusal.date.toString(),
            Money.format(refusal.shortfallCents),
        )

    is CashEntryRefusal.BeforeOpeningBalance ->
        stringResource(R.string.cash_before_opening_balance, refusal.openingDate.toString())

    is CashEntryRefusal.BeforeOriginal ->
        stringResource(R.string.reversal_before_original, refusal.originalDate.toString())

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
internal fun dateMessage(refusal: DateRefusal?): String? = when (refusal) {
    null -> null
    DateRefusal.EMPTY -> stringResource(R.string.date_empty)
    DateRefusal.NOT_A_DATE -> stringResource(R.string.date_not_a_date)
    DateRefusal.NOT_DIGITS -> stringResource(R.string.date_not_digits)
    DateRefusal.BEFORE_FROM -> stringResource(R.string.date_before_from)
}

@Composable
internal fun noteMessage(
    lineBreak: Boolean,
    required: Boolean,
): String? = when {
    lineBreak -> stringResource(R.string.note_line_break)
    required -> stringResource(R.string.note_required)
    else -> null
}

@Composable
internal fun saveMessage(refusal: SaveRefusal?): String? = when (refusal) {
    null -> null
    SaveRefusal.ALREADY_EXISTS -> stringResource(R.string.save_already_exists)
    SaveRefusal.SAVE_FAILED -> stringResource(R.string.save_failed)
}
