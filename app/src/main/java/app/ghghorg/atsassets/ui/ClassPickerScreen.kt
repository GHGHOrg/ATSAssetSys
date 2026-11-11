package app.ghghorg.atsassets.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.core.ClassList

/** The Edit picker's callbacks, passed together. */
class ClassPickerActions(
    val onCancel: () -> Unit,
    val onChoose: (String) -> Unit,
    val onSave: () -> Unit,
)

/**
 * The Edit picker (section 6, More and Settings entry; wireframe 10f frame H, markers 16 and 17; 24), drawn from its model
 * (CR2): "<" and the title ("Sector, AAPL"), one radio row per value of the list in its order, the pick checked; the line
 * "Choose from the list." (no pointer to Settings, Lists until 25, decision 1); Save and Cancel. An empty list shows one
 * plain line and no Save. A failed save shows the shared "Not saved" line. One scrolling screen, drawn in the activity
 * window (CLAUDE.md); Back cancels.
 */
@Composable
fun ClassPickerScreen(
    state: AppState,
    actions: ClassPickerActions,
    modifier: Modifier = Modifier,
) {
    val model = state.classPickerModel ?: return
    BackHandler(enabled = !state.saving, onBack = actions.onCancel)
    val listName = stringResource(if (model.list == ClassList.SECTOR) R.string.class_sector else R.string.class_geography)
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TitleRow(stringResource(R.string.class_picker_title, listName, model.ticker), actions.onCancel, enabled = !state.saving)
        if (model.rows.isEmpty()) {
            val empty = if (model.list == ClassList.SECTOR) R.string.class_picker_no_sectors else R.string.class_picker_no_geographies
            Text(text = stringResource(empty), style = MaterialTheme.typography.bodyLarge)
        } else {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.selectableGroup()) {
                    model.rows.forEach { row -> PickerRow(row, enabled = !state.saving, onChoose = actions.onChoose) }
                }
            }
            Text(
                text = stringResource(R.string.class_picker_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Buttons(showSave = model.rows.isNotEmpty(), canSave = model.canSave, saving = state.saving, actions = actions)
        if (model.notSaved) {
            Text(
                text = stringResource(R.string.save_failed),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

/** One value: a radio button and its name; TalkBack reads the name and whether it is selected. */
@Composable
private fun PickerRow(
    row: ClassPickerRowModel,
    enabled: Boolean,
    onChoose: (String) -> Unit,
) {
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .selectable(selected = row.selected, enabled = enabled, role = Role.RadioButton, onClick = { onChoose(row.value) })
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = row.selected, onClick = null, enabled = enabled)
        Text(text = row.value, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Save (only with a list to pick from, marker 17) and Cancel; they wrap at the largest font. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Buttons(
    showSave: Boolean,
    canSave: Boolean,
    saving: Boolean,
    actions: ClassPickerActions,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (showSave) {
            Button(onClick = actions.onSave, enabled = canSave) {
                Text(text = stringResource(R.string.save_button))
            }
        }
        OutlinedButton(onClick = actions.onCancel, enabled = !saving) {
            Text(text = stringResource(R.string.cancel_button))
        }
    }
}
