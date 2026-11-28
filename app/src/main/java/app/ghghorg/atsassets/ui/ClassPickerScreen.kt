package app.ghghorg.atsassets.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.core.ClassList
import app.ghghorg.atsassets.ui.design.AtsBlockedMessage
import app.ghghorg.atsassets.ui.design.AtsButton
import app.ghghorg.atsassets.ui.design.AtsCard
import app.ghghorg.atsassets.ui.design.AtsRadioGroup
import app.ghghorg.atsassets.ui.design.AtsRadioRow
import app.ghghorg.atsassets.ui.design.AtsScreen
import app.ghghorg.atsassets.ui.design.ButtonKind
import app.ghghorg.atsassets.ui.design.ButtonSize
import app.ghghorg.atsassets.ui.theme.AtsTheme
import app.ghghorg.atsassets.ui.theme.tokenDp
import app.ghghorg.atsassets.ui.theme.typeStyle

/** The Edit picker's callbacks, passed together. */
class ClassPickerActions(
    val onCancel: () -> Unit,
    val onChoose: (String) -> Unit,
    val onSave: () -> Unit,
)

/**
 * The Edit picker (section 6, More and Settings entry; wireframe 10f frame H, markers 16 and 17; 24), drawn from its model
 * (CR2), in the design system (44a step 8 group (e); drawing spec/wireframes/design/10f-more-settings.html frame J): under
 * the title band ("Sector, AAPL"), the catalog's radio rows in a card, one per value of the list in its order, the pick
 * selected; the line "Choose from the list." in secondary text; Save the gold foot and Cancel a text button under it. An
 * empty list shows its line in a card and no Save. A failed save as the blocked card. Back cancels.
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
    AtsScreen(
        title = stringResource(R.string.class_picker_title, listName, model.ticker),
        modifier = modifier,
        onBack = actions.onCancel,
        backLabel = stringResource(R.string.back_button),
        backEnabled = !state.saving,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(tokenDp("spaceMargin")),
            verticalArrangement = Arrangement.spacedBy(tokenDp("spaceGap")),
        ) {
            if (model.notSaved) AtsBlockedMessage(stringResource(R.string.save_failed), prefix = null)
            if (model.rows.isEmpty()) {
                val empty = if (model.list == ClassList.SECTOR) R.string.class_picker_no_sectors else R.string.class_picker_no_geographies
                PlainLineCard(stringResource(empty))
                AtsButton(stringResource(R.string.cancel_button), actions.onCancel, kind = ButtonKind.TEXT, size = ButtonSize.LARGE, enabled = !state.saving, fillWidth = true)
            } else {
                AtsCard(padded = false) {
                    AtsRadioGroup {
                        model.rows.forEach { row ->
                            AtsRadioRow(row.value, selected = row.selected, onClick = { actions.onChoose(row.value) }, enabled = !state.saving)
                        }
                    }
                }
                Text(text = stringResource(R.string.class_picker_note), style = typeStyle("small"), color = AtsTheme.colors.textSecondary)
                // Save waits for a pick; Cancel does not (AtsEntryFoot's one flag would hold both).
                AtsButton(stringResource(R.string.save_button), actions.onSave, kind = ButtonKind.FILLED, size = ButtonSize.LARGE, enabled = model.canSave, fillWidth = true)
                AtsButton(stringResource(R.string.cancel_button), actions.onCancel, kind = ButtonKind.TEXT, size = ButtonSize.LARGE, enabled = !state.saving, fillWidth = true)
            }
        }
    }
}
