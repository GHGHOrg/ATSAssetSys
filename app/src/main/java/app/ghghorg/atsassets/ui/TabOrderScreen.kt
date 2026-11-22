package app.ghghorg.atsassets.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.core.Tab

/** The Tab order screen's callbacks, passed together. */
class TabOrderActions(
    val onBack: () -> Unit,
    val onUp: (Tab) -> Unit,
    val onDown: (Tab) -> Unit,
)

/**
 * The Tab order screen (section 6, More and Settings entry; wireframe 10f frame E, marker 11; 22), drawn from its model
 * (CR2): "<" and the title, one row per tab left to right with its number, Up and Down, and the line "Left to right in
 * the bottom bar. Takes effect at once.". Up on the first row and Down on the last are shown inactive (decision 1). Each
 * row is keyed by its tab, so after a move TalkBack's focus stays on the moved tab's button and the row reads its new
 * place (decision 2). The bar under the screen follows each saved move. Back returns to Settings.
 */
@Composable
fun TabOrderScreen(
    state: AppState,
    actions: TabOrderActions,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = !state.saving, onBack = actions.onBack)
    val model = state.tabOrderScreen
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TitleRow(stringResource(R.string.settings_tab_order), actions.onBack, enabled = !state.saving)
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            model.rows.forEachIndexed { index, row ->
                key(row.tab) {
                    if (index > 0) HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                    TabOrderRowView(row, actions)
                }
            }
        }
        Text(
            text = stringResource(R.string.tab_order_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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

/** One tab: "1. More", read as "More, position 1 of 4", then Up and Down, read as "Move More up" and "Move More down". */
@Composable
private fun TabOrderRowView(
    row: TabOrderRow,
    actions: TabOrderActions,
) {
    val name = stringResource(row.tab.labelRes)
    val spoken = stringResource(R.string.tab_order_row_spoken, name, row.position, row.count)
    val up = stringResource(R.string.tab_order_up_spoken, name)
    val down = stringResource(R.string.tab_order_down_spoken, name)
    // The name takes the space left, so at the largest font it wraps and the buttons stay on the row.
    Row(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.tab_order_row, row.position, name),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f).semantics { contentDescription = spoken },
        )
        OutlinedButton(
            onClick = { actions.onUp(row.tab) },
            enabled = row.canMoveUp,
            modifier = Modifier.semantics { contentDescription = up },
        ) {
            Text(text = stringResource(R.string.tab_order_up))
        }
        OutlinedButton(
            onClick = { actions.onDown(row.tab) },
            enabled = row.canMoveDown,
            modifier = Modifier.semantics { contentDescription = down },
        ) {
            Text(text = stringResource(R.string.tab_order_down))
        }
    }
}
