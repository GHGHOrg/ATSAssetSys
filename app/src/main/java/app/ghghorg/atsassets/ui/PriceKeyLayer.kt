package app.ghghorg.atsassets.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.ui.design.AtsButton
import app.ghghorg.atsassets.ui.design.AtsDialog
import app.ghghorg.atsassets.ui.design.AtsProgressRow
import app.ghghorg.atsassets.ui.design.AtsTextField
import app.ghghorg.atsassets.ui.design.ButtonKind
import app.ghghorg.atsassets.ui.theme.AtsTheme
import app.ghghorg.atsassets.ui.theme.typeStyle

/** The key entry's callbacks, passed together (30). */
class KeyEntryActions(
    val onTextChange: (String) -> Unit,
    val onSave: () -> Unit,
    val onCancel: () -> Unit,
    val onAskClear: () -> Unit,
    val onCancelClear: () -> Unit,
    val onConfirmClear: () -> Unit,
)

/**
 * The price source's key entry (30), drawn from its model (CR2) as the catalog's dialog inside the activity window (44a
 * step 8 group (e); D-34; CLAUDE.md: a separate window loops with the cover screen), which AppContent draws over the
 * bottom bar and which gives way to the keyboard. The title names the source; then the saved key's last characters or
 * that none is saved, the line that the key is checked before it is saved, and the field, a refusal on it (D-34). The
 * field hides what is typed until Show key is tapped, and asks the keyboard for a password field: no suggestions, no
 * learning (30, risk 2). Save, gold, is active once something is typed; while the key is checked, loader-circle and the
 * line (D-35). Clear key, offered while a key is saved, asks first in this same dialog. Cancel and Back close it, or go
 * back from the question.
 */
@Composable
internal fun KeyEntryLayer(
    model: KeyEntryModel?,
    actions: KeyEntryActions,
) {
    if (model == null) return
    BackHandler(enabled = !model.busy, onBack = if (model.confirmingClear) actions.onCancelClear else actions.onCancel)
    AtsDialog(
        title = stringResource(R.string.key_entry_title, model.source),
        actions = {
            if (model.confirmingClear) {
                AtsButton(stringResource(R.string.cancel_button), actions.onCancelClear, kind = ButtonKind.TEXT, enabled = !model.busy)
                AtsButton(stringResource(R.string.key_entry_clear), actions.onConfirmClear, kind = ButtonKind.FILLED, enabled = !model.busy)
            } else {
                AtsButton(stringResource(R.string.cancel_button), actions.onCancel, kind = ButtonKind.TEXT, enabled = !model.busy)
                AtsButton(stringResource(R.string.save_button), actions.onSave, kind = ButtonKind.FILLED, enabled = model.saveEnabled)
            }
        },
    ) {
        if (model.confirmingClear) ClearQuestion() else KeyFields(model, actions)
    }
}

@Composable
private fun KeyFields(
    model: KeyEntryModel,
    actions: KeyEntryActions,
) {
    val colors = AtsTheme.colors
    CurrentKeyLine(model)
    Text(text = stringResource(R.string.key_entry_note, model.source), style = typeStyle("small"), color = colors.textSecondary)
    var shown by rememberSaveable { mutableStateOf(false) }
    AtsTextField(
        value = model.text,
        onValueChange = actions.onTextChange,
        label = stringResource(R.string.key_entry_field),
        modifier = Modifier.fillMaxWidth(),
        refusal = keyMessage(model.refusal, model.source),
        enabled = !model.busy,
        keyboardOptions =
        KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { if (model.saveEnabled) actions.onSave() }),
        visualTransformation = if (shown) VisualTransformation.None else PasswordVisualTransformation(),
        raised = true,
        announce = true,
    )
    AtsButton(stringResource(if (shown) R.string.key_entry_hide else R.string.key_entry_show), { shown = !shown }, kind = ButtonKind.TEXT, enabled = !model.busy)
    if (model.busy) {
        AtsProgressRow(
            stringResource(R.string.key_entry_checking, model.source),
            done = false,
            modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        )
    }
    // Clear key on its own line, so the dialog's buttons fit at the largest font.
    if (model.keySet) AtsButton(stringResource(R.string.key_entry_clear), actions.onAskClear, kind = ButtonKind.TEXT, enabled = !model.busy)
}

/** The saved key's state: its last characters, read by TalkBack one by one, or that a key, or none, is saved. */
@Composable
private fun CurrentKeyLine(model: KeyEntryModel) {
    val shown =
        when {
            !model.keySet -> stringResource(R.string.key_entry_none)
            model.keyEnding.isEmpty() -> stringResource(R.string.key_entry_current_set)
            else -> stringResource(R.string.key_entry_current, model.keyEnding)
        }
    val spoken = if (model.keySet && model.keyEnding.isNotEmpty()) stringResource(R.string.key_entry_current, model.spokenEnding) else shown
    Text(text = shown, style = typeStyle("body"), color = AtsTheme.colors.text, modifier = Modifier.semantics { contentDescription = spoken })
}

/** The clear confirmation, in place of the field: the question, read when it appears; Cancel and Clear key below. */
@Composable
private fun ClearQuestion() {
    Text(
        text = stringResource(R.string.key_clear_question),
        style = typeStyle("body"),
        color = AtsTheme.colors.text,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

/** Why the last Save or Clear did not go through, in the approved words; null when it did or none was tried. */
@Composable
private fun keyMessage(
    refusal: KeyRefusal?,
    source: String,
): String? = when (refusal) {
    null -> null

    KeyRefusal.NotAKey -> stringResource(R.string.key_not_a_key)

    KeyRefusal.Offline -> stringResource(R.string.key_offline)

    is KeyRefusal.Refused -> stringResource(R.string.key_refused, source, refusal.status)

    KeyRefusal.TooManyCalls -> stringResource(R.string.key_too_many)

    is KeyRefusal.NoAnswer -> {
        val status = refusal.status
        if (status == null) stringResource(R.string.key_no_answer, source) else stringResource(R.string.key_no_answer_status, source, status)
    }

    KeyRefusal.SaveFailed -> stringResource(R.string.save_failed)
}
