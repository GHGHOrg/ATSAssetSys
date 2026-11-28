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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.core.Tab
import app.ghghorg.atsassets.ui.design.AtsBlockedMessage
import app.ghghorg.atsassets.ui.design.AtsCard
import app.ghghorg.atsassets.ui.design.AtsIconButton
import app.ghghorg.atsassets.ui.design.AtsRowDivider
import app.ghghorg.atsassets.ui.design.AtsScreen
import app.ghghorg.atsassets.ui.theme.AtsTheme
import app.ghghorg.atsassets.ui.theme.LucideIcon
import app.ghghorg.atsassets.ui.theme.tokenDp
import app.ghghorg.atsassets.ui.theme.typeStyle

/** The Tab order screen's callbacks, passed together. */
class TabOrderActions(
    val onBack: () -> Unit,
    val onUp: (Tab) -> Unit,
    val onDown: (Tab) -> Unit,
)

/**
 * The Tab order screen (section 6, More and Settings entry; wireframe 10f frame E, marker 11; 22), drawn from its model
 * (CR2), in the design system (44a step 8 group (e); drawing spec/wireframes/design/10f-more-settings.html frame G):
 * under the title band, one card with a row per tab left to right with its number, then Lucide's arrow-up and arrow-down
 * as 48 icon buttons, read as "Move More up" and "Move More down" (note 7); up on the first row and down on the last are
 * the catalog's disabled ones (decision 1). Then the line "Left to right in the bottom bar. Takes effect at once.", and a
 * failed save as the blocked card. Each row is keyed by its tab, so after a move TalkBack's focus stays on the moved tab's
 * button and the row reads its new place (decision 2). The bar under the screen follows each saved move. Back returns to
 * Settings.
 */
@Composable
fun TabOrderScreen(
    state: AppState,
    actions: TabOrderActions,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = !state.saving, onBack = actions.onBack)
    val model = state.tabOrderScreen
    AtsScreen(
        title = stringResource(R.string.settings_tab_order),
        modifier = modifier,
        onBack = actions.onBack,
        backLabel = stringResource(R.string.back_button),
        backEnabled = !state.saving,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(tokenDp("spaceMargin")),
            verticalArrangement = Arrangement.spacedBy(tokenDp("spaceGap")),
        ) {
            if (model.notSaved) AtsBlockedMessage(stringResource(R.string.save_failed), prefix = null)
            AtsCard(padded = false) {
                model.rows.forEachIndexed { index, row ->
                    key(row.tab) {
                        if (index > 0) AtsRowDivider()
                        TabOrderRowView(row, actions)
                    }
                }
            }
            Text(text = stringResource(R.string.tab_order_note), style = typeStyle("small"), color = AtsTheme.colors.textSecondary)
        }
    }
}

/** One tab: "1. More", read as "More, position 1 of 4", then up and down, read as "Move More up" and "Move More down". */
@Composable
private fun TabOrderRowView(
    row: TabOrderRow,
    actions: TabOrderActions,
) {
    val name = stringResource(row.tab.labelRes)
    val spoken = stringResource(R.string.tab_order_row_spoken, name, row.position, row.count)
    // The name takes the space left, so at the largest font it wraps and the buttons stay on the row.
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = tokenDp("spaceInset")),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.tab_order_row, row.position, name),
            style = typeStyle("body"),
            color = AtsTheme.colors.text,
            modifier = Modifier.weight(1f).semantics { contentDescription = spoken },
        )
        AtsIconButton(LucideIcon.ARROW_UP, stringResource(R.string.tab_order_up_spoken, name), { actions.onUp(row.tab) }, enabled = row.canMoveUp)
        AtsIconButton(LucideIcon.ARROW_DOWN, stringResource(R.string.tab_order_down_spoken, name), { actions.onDown(row.tab) }, enabled = row.canMoveDown)
    }
}
