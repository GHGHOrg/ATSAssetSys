package app.ghghorg.atsassets.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.core.ClassList
import app.ghghorg.atsassets.ui.design.AtsBlockedMessage
import app.ghghorg.atsassets.ui.design.AtsButton
import app.ghghorg.atsassets.ui.design.AtsCard
import app.ghghorg.atsassets.ui.design.AtsDialog
import app.ghghorg.atsassets.ui.design.AtsEntryFoot
import app.ghghorg.atsassets.ui.design.AtsNoticeBanner
import app.ghghorg.atsassets.ui.design.AtsRowDivider
import app.ghghorg.atsassets.ui.design.AtsScreen
import app.ghghorg.atsassets.ui.design.AtsTextField
import app.ghghorg.atsassets.ui.design.ButtonKind
import app.ghghorg.atsassets.ui.design.ButtonSize
import app.ghghorg.atsassets.ui.theme.AtsTheme
import app.ghghorg.atsassets.ui.theme.tokenDp
import app.ghghorg.atsassets.ui.theme.typeStyle

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
 * drawn from its model (CR2), in the design system (44a step 8 group (e); drawing
 * spec/wireframes/design/10f-more-settings.html frames H and I): under the title band, the line of the last add, rename
 * or delete as D-33's notice banner, a refused delete as D-31's blocked card (marker 15, note 8); one card with a row per
 * value, "Used by N holdings." and, after a rename, "Also matches: ..." in secondary text, Rename and Delete small text
 * buttons under them (note 9); an empty list's line in a card; Add value the gold foot. Add value and Rename open the
 * catalog's dialog with the Name field, a refusal on the field (frame I, marker 14), drawn inside the activity window over
 * the bar (CLAUDE.md, 07 and 08). Back closes the dialog first, then returns to Settings.
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
        // While the Name dialog is open, TalkBack reads only the dialog.
        val under = if (edit != null) Modifier.clearAndSetSemantics { } else Modifier
        AtsScreen(
            title = stringResource(if (sector) R.string.settings_sectors else R.string.settings_geographies),
            modifier = under,
            onBack = actions.onBack,
            backLabel = stringResource(R.string.back_button),
            backEnabled = model.enabled,
        ) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(tokenDp("spaceMargin")),
                verticalArrangement = Arrangement.spacedBy(tokenDp("spaceGap")),
            ) {
                model.line?.let { ListLineText(it) }
                if (model.rows.isEmpty()) {
                    PlainLineCard(stringResource(if (sector) R.string.list_no_sectors else R.string.list_no_geographies))
                } else {
                    AtsCard(padded = false) {
                        model.rows.forEachIndexed { index, row ->
                            key(row.value) {
                                if (index > 0) AtsRowDivider()
                                ValueRow(row, enabled = model.enabled && edit == null, actions = actions)
                            }
                        }
                    }
                }
                AtsEntryFoot(stringResource(R.string.list_add_value), actions.onAdd, enabled = model.enabled && edit == null)
            }
        }
        if (edit != null) {
            NameLayer(edit, enabled = model.enabled, actions = actions)
        }
    }
}

/**
 * One value: its name, "Used by N holdings." and "Also matches: ..." read together by TalkBack as one line, then Rename
 * and Delete as small text buttons, read as "Rename Technology" and "Delete Technology".
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ValueRow(
    row: ClassValueRowModel,
    enabled: Boolean,
    actions: ClassListActions,
) {
    val colors = AtsTheme.colors
    val usedBy = pluralStringResource(R.plurals.list_used_by, row.holdings, row.holdings)
    val spoken =
        if (row.alsoMatches == null) {
            stringResource(R.string.list_row_spoken, row.value, usedBy)
        } else {
            stringResource(R.string.list_row_also_spoken, row.value, usedBy, row.alsoMatches)
        }
    Column(modifier = Modifier.fillMaxWidth().padding(top = tokenDp("spaceInset"), start = tokenDp("spaceInset"), end = tokenDp("spaceInset"))) {
        Column(
            modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = spoken },
            verticalArrangement = Arrangement.spacedBy(tokenDp("cardGap")),
        ) {
            Text(text = row.value, style = typeStyle("body"), color = colors.text)
            Text(text = usedBy, style = typeStyle("small"), color = colors.textSecondary)
            row.alsoMatches?.let { Text(text = stringResource(R.string.list_also_matches, it), style = typeStyle("small"), color = colors.textSecondary) }
        }
        FlowRow {
            val rename = stringResource(R.string.list_rename_spoken, row.value)
            val delete = stringResource(R.string.list_delete_spoken, row.value)
            AtsButton(
                stringResource(R.string.list_rename),
                { actions.onRename(row.value) },
                modifier = Modifier.semantics { contentDescription = rename },
                kind = ButtonKind.TEXT,
                size = ButtonSize.SMALL,
                enabled = enabled,
            )
            AtsButton(
                stringResource(R.string.list_delete),
                { actions.onDelete(row.value) },
                modifier = Modifier.semantics { contentDescription = delete },
                kind = ButtonKind.TEXT,
                size = ButtonSize.SMALL,
                enabled = enabled,
            )
        }
    }
}

/**
 * The line of the last change (frame H, marker 15), read when it appears: a refused or failed delete as D-31's blocked
 * card, starting "Not deleted"; an add, rename or delete as D-33's notice banner.
 */
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
    if (line is ClassListLine.InUse || line == ClassListLine.NotDeleted) {
        AtsBlockedMessage(text, prefix = null)
    } else {
        AtsNoticeBanner(text, modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite })
    }
}

/**
 * The Name dialog (frame I, marker 14) for Add value or Rename: the catalog's dialog with one Name field, a refusal on
 * the field (D-34), Cancel a text button and Save gold. A tap outside keeps the typed text; Back closes it like Cancel.
 */
@Composable
private fun NameLayer(
    edit: ClassNameEditModel,
    enabled: Boolean,
    actions: ClassListActions,
) {
    BackHandler(enabled = enabled, onBack = actions.onNameCancel)
    val renaming = edit.renaming
    AtsDialog(
        title = if (renaming == null) stringResource(R.string.list_add_value) else stringResource(R.string.list_rename_title, renaming),
        actions = {
            AtsButton(stringResource(R.string.cancel_button), actions.onNameCancel, kind = ButtonKind.TEXT, enabled = enabled)
            AtsButton(stringResource(R.string.save_button), actions.onNameSave, kind = ButtonKind.FILLED, enabled = enabled)
        },
    ) {
        AtsTextField(
            value = edit.text,
            onValueChange = actions.onNameChange,
            label = stringResource(R.string.list_name_label),
            modifier = Modifier.fillMaxWidth(),
            refusal = edit.line?.let { nameLineText(it) },
            enabled = enabled,
            raised = true,
            announce = true,
        )
    }
}

@Composable
private fun nameLineText(line: ClassNameLine): String = when (line) {
    ClassNameLine.Blank -> stringResource(R.string.list_name_blank)
    is ClassNameLine.Duplicate -> stringResource(R.string.list_name_duplicate, line.value)
    is ClassNameLine.Remembered -> stringResource(R.string.list_name_remembered, line.text, line.value)
    ClassNameLine.NotSaved -> stringResource(R.string.save_failed)
}
