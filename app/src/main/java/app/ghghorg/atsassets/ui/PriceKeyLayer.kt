package app.ghghorg.atsassets.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import app.ghghorg.atsassets.R

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
 * The price source's key entry (30), drawn from its model (CR2) inside the activity window over the dimmed layer of the
 * Delete dialog (CLAUDE.md: a separate window loops with the cover screen), which AppContent draws over the bottom bar
 * and which gives way to the keyboard. The title names the source; then the saved key's last characters or that none is
 * saved, the line that the key is checked before it is saved, and the field. The field hides what is typed until Show
 * key is tapped, and asks the keyboard for a password field: no suggestions, no learning (30, risk 2). Save is active
 * once something is typed; Clear key, offered while a key is saved, asks first in this same layer. Cancel and Back close
 * it, or go back from the question.
 */
@Composable
internal fun KeyEntryLayer(
    model: KeyEntryModel?,
    actions: KeyEntryActions,
) {
    if (model == null) return
    BackHandler(enabled = !model.busy, onBack = if (model.confirmingClear) actions.onCancelClear else actions.onCancel)
    val title = stringResource(R.string.key_entry_title, model.source)
    DialogScrim {
        Surface(
            modifier = Modifier.padding(24.dp).widthIn(max = 560.dp).semantics { paneTitle = title },
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(text = title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                if (model.confirmingClear) ClearQuestion(model, actions) else KeyFields(model, actions)
            }
        }
    }
}

@Composable
private fun KeyFields(
    model: KeyEntryModel,
    actions: KeyEntryActions,
) {
    CurrentKeyLine(model)
    Text(
        text = stringResource(R.string.key_entry_note, model.source),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    var shown by rememberSaveable { mutableStateOf(false) }
    val message = keyMessage(model.refusal, model.source)
    OutlinedTextField(
        value = model.text,
        onValueChange = actions.onTextChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(text = stringResource(R.string.key_entry_field)) },
        supportingText =
        if (message != null) {
            { Text(text = message, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
        } else {
            null
        },
        isError = message != null,
        enabled = !model.busy,
        singleLine = true,
        visualTransformation = if (shown) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions =
        KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { if (model.saveEnabled) actions.onSave() }),
    )
    TextButton(onClick = { shown = !shown }, enabled = !model.busy) {
        Text(text = stringResource(if (shown) R.string.key_entry_hide else R.string.key_entry_show))
    }
    if (model.busy) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        ) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Text(
                text = stringResource(R.string.key_entry_checking, model.source),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    // Clear key on its own line, so the three buttons fit at the largest font.
    if (model.keySet) {
        TextButton(onClick = actions.onAskClear, enabled = !model.busy) {
            Text(text = stringResource(R.string.key_entry_clear))
        }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
        TextButton(onClick = actions.onCancel, enabled = !model.busy) {
            Text(text = stringResource(R.string.cancel_button))
        }
        TextButton(onClick = actions.onSave, enabled = model.saveEnabled) {
            Text(text = stringResource(R.string.save_button))
        }
    }
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
    Text(text = shown, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.semantics { contentDescription = spoken })
}

/** The clear confirmation, in place of the field: the question, read when it appears, then Cancel and Clear key. */
@Composable
private fun ClearQuestion(
    model: KeyEntryModel,
    actions: KeyEntryActions,
) {
    Text(
        text = stringResource(R.string.key_clear_question),
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
        TextButton(onClick = actions.onCancelClear, enabled = !model.busy) {
            Text(text = stringResource(R.string.cancel_button))
        }
        TextButton(onClick = actions.onConfirmClear, enabled = !model.busy) {
            Text(text = stringResource(R.string.key_entry_clear))
        }
    }
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
