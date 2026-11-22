package app.ghghorg.atsassets.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.core.ClassList

/** The list screen's callbacks, passed together. */
class ClassListActions(
    val onBack: () -> Unit,
    val onAdd: () -> Unit,
    val onRename: (String) -> Unit,
    val onDelete: (String) -> Unit,
    val onNameChange: (String) -> Unit,
    val onNameSave: () -> Unit,
    val onNameCancel: () -> Unit,
)

/**
 * A sector or geography list (section 6, More and Settings entry; wireframe 10f frames F and G, markers 12 to 15; 25),
 * drawn from its model (CR2): "<" and the title, one row per value with "Used by N holdings." and, after a rename, "Also
 * matches: ..." (read only), each with Rename and Delete; an empty list's line; the line under the list after an add,
 * rename or delete; Add value. Add value and Rename open the Name layer (frame G, marker 14), drawn in the activity
 * window over a dimmed layer with imePadding, so the keyboard does not cover it and no separate window takes MainActivity's
 * focus (CLAUDE.md, 07 and 08). Back closes the layer first, then returns to Settings.
 */
@Composable
fun ClassListScreen(
    state: AppState,
    actions: ClassListActions,
    modifier: Modifier = Modifier,
) {
    val model = state.classListScreen ?: return
    val edit = model.edit
    BackHandler(enabled = model.enabled && edit == null, onBack = actions.onBack)
    val sector = model.list == ClassList.SECTOR
    Box(modifier = modifier.fillMaxSize()) {
        // While the Name layer is open, TalkBack reads only the layer.
        val under = if (edit != null) Modifier.clearAndSetSemantics { } else Modifier
        Column(
            modifier = Modifier.fillMaxSize().then(under).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val title = stringResource(if (sector) R.string.settings_sectors else R.string.settings_geographies)
            TitleRow(title, actions.onBack, enabled = model.enabled)
            if (model.rows.isEmpty()) {
                Text(
                    text = stringResource(if (sector) R.string.list_no_sectors else R.string.list_no_geographies),
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    model.rows.forEachIndexed { index, row ->
                        key(row.value) {
                            if (index > 0) HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                            ValueRow(row, enabled = model.enabled && edit == null, actions = actions)
                        }
                    }
                }
            }
            model.line?.let { ListLineText(it) }
            Button(onClick = actions.onAdd, enabled = model.enabled && edit == null) {
                Text(text = stringResource(R.string.list_add_value))
            }
        }
        if (edit != null) {
            NameLayer(edit, enabled = model.enabled, actions = actions)
        }
    }
}

/**
 * One value: its name, "Used by N holdings." and "Also matches: ..." read together by TalkBack as one line, then Rename
 * and Delete, read as "Rename Technology" and "Delete Technology"; the buttons wrap under the text at the largest font.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ValueRow(
    row: ClassValueRowModel,
    enabled: Boolean,
    actions: ClassListActions,
) {
    val usedBy = pluralStringResource(R.plurals.list_used_by, row.holdings, row.holdings)
    val spoken =
        if (row.alsoMatches == null) {
            stringResource(R.string.list_row_spoken, row.value, usedBy)
        } else {
            stringResource(R.string.list_row_also_spoken, row.value, usedBy, row.alsoMatches)
        }
    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = spoken },
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = row.value, style = MaterialTheme.typography.bodyLarge)
            Text(text = usedBy, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            row.alsoMatches?.let {
                Text(
                    text = stringResource(R.string.list_also_matches, it),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val rename = stringResource(R.string.list_rename_spoken, row.value)
            val delete = stringResource(R.string.list_delete_spoken, row.value)
            OutlinedButton(
                onClick = { actions.onRename(row.value) },
                enabled = enabled,
                modifier = Modifier.semantics { contentDescription = rename },
            ) {
                Text(text = stringResource(R.string.list_rename))
            }
            OutlinedButton(
                onClick = { actions.onDelete(row.value) },
                enabled = enabled,
                modifier = Modifier.semantics { contentDescription = delete },
            ) {
                Text(text = stringResource(R.string.list_delete))
            }
        }
    }
}

/** The line under the list (frame G, marker 15): plain text, a refusal or failure starting "Not deleted" (no color alone). */
@Composable
private fun ListLineText(line: ClassListLine) {
    val text =
        when (line) {
            is ClassListLine.Added -> stringResource(R.string.list_added, line.value)
            is ClassListLine.Renamed -> stringResource(R.string.list_renamed, line.from, line.to)
            is ClassListLine.RenamedRemembering -> stringResource(R.string.list_renamed_remembering, line.text, line.value)
            is ClassListLine.Deleted -> stringResource(R.string.list_deleted, line.value)
            is ClassListLine.InUse -> pluralStringResource(R.plurals.list_in_use, line.holdings, line.value, line.holdings)
            ClassListLine.NotDeleted -> stringResource(R.string.list_not_deleted)
        }
    val refused = line is ClassListLine.InUse || line == ClassListLine.NotDeleted
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = if (refused) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

/**
 * The Name layer (frame G, marker 14) for Add value or Rename: one Name field with its line under it, Cancel and Save.
 * Drawn in the activity window over a dimmed layer that takes every tap, so a tap outside keeps the typed text; the
 * layer sits above the keyboard (imePadding) and scrolls there. Back closes it like Cancel.
 */
@Composable
private fun NameLayer(
    edit: ClassNameEditModel,
    enabled: Boolean,
    actions: ClassListActions,
) {
    BackHandler(enabled = enabled, onBack = actions.onNameCancel)
    val renaming = edit.renaming
    val title = if (renaming == null) stringResource(R.string.list_add_value) else stringResource(R.string.list_rename_title, renaming)
    val error = edit.line?.let { nameLineText(it) }
    Box(
        modifier =
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = NAME_SCRIM_ALPHA))
            .pointerInput(Unit) { detectTapGestures { } }
            .imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.padding(24.dp).widthIn(max = 560.dp).semantics { paneTitle = title },
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(text = title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                OutlinedTextField(
                    value = edit.text,
                    onValueChange = actions.onNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = stringResource(R.string.list_name_label)) },
                    supportingText =
                    error?.let {
                        { Text(text = it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
                    },
                    isError = error != null,
                    singleLine = true,
                    enabled = enabled,
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    TextButton(onClick = actions.onNameCancel, enabled = enabled) {
                        Text(text = stringResource(R.string.cancel_button))
                    }
                    TextButton(onClick = actions.onNameSave, enabled = enabled) {
                        Text(text = stringResource(R.string.save_button))
                    }
                }
            }
        }
    }
}

@Composable
private fun nameLineText(line: ClassNameLine): String = when (line) {
    ClassNameLine.Blank -> stringResource(R.string.list_name_blank)
    is ClassNameLine.Duplicate -> stringResource(R.string.list_name_duplicate, line.value)
    is ClassNameLine.Remembered -> stringResource(R.string.list_name_remembered, line.text, line.value)
    ClassNameLine.NotSaved -> stringResource(R.string.save_failed)
}

// The dim behind the Name layer, as PortfolioNameDialog's.
private const val NAME_SCRIM_ALPHA = 0.32f
