package app.ghghorg.atsassets.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.core.Fraction

/** Hidden holdings' callbacks, passed together. */
class HiddenHoldingsActions(
    val onBack: () -> Unit,
    val onOpenHolding: (String) -> Unit,
    val onUnhide: (String) -> Unit,
)

/**
 * Hidden holdings (view 9, wireframe 09; iteration 15): "<" and the title, the portfolio named at the top with no
 * chooser, then one row per hidden holding in ticker order (ticker, "0 shares", the last transaction date with "ET"
 * and Unhide), or one plain line with none hidden. A row opens Holding detail; Back returns here (15 decision 3).
 */
@Composable
fun HiddenHoldingsScreen(
    state: AppState,
    actions: HiddenHoldingsActions,
    modifier: Modifier = Modifier,
) {
    val portfolio = state.shownPortfolio ?: return
    BackHandler(onBack = actions.onBack)
    val rows = state.hiddenRows(portfolio.id)
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TitleRow(stringResource(R.string.hidden_title), actions.onBack, enabled = !state.saving)
        val portfolioSpoken = stringResource(R.string.hidden_title_spoken, portfolio.name)
        OutlinedCard(modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = portfolioSpoken }) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = stringResource(R.string.hidden_portfolio_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(text = portfolio.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            }
        }
        if (state.hideRefusal == SaveRefusal.SAVE_FAILED) {
            Text(
                text = stringResource(R.string.save_failed),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (rows.isEmpty()) {
                    Text(text = stringResource(R.string.hidden_none), style = MaterialTheme.typography.bodyLarge)
                } else {
                    rows.forEachIndexed { index, row ->
                        if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        HiddenRowLine(row, enabled = !state.saving, actions)
                    }
                }
            }
        }
    }
}

/** One hidden holding: its ticker, "0 shares" and last transaction date open Holding detail; Unhide beside them. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HiddenRowLine(
    row: HiddenRow,
    enabled: Boolean,
    actions: HiddenHoldingsActions,
) {
    val shares = sharesText(Fraction.ZERO)
    val date = row.lastTransaction?.toString().orEmpty()
    val spoken = stringResource(R.string.hidden_row_spoken, row.ticker, shares, date)
    val unhideSpoken = stringResource(R.string.unhide_spoken, row.ticker)
    val open = { actions.onOpenHolding(row.ticker) }
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier =
            Modifier
                .clickable(enabled = enabled, role = Role.Button, onClick = open)
                .clearAndSetSemantics {
                    contentDescription = spoken
                    role = Role.Button
                    onClick {
                        open()
                        true
                    }
                },
        ) {
            Text(text = row.ticker, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(text = shares, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (date.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.hidden_last, date),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        OutlinedButton(
            onClick = { actions.onUnhide(row.ticker) },
            enabled = enabled,
            modifier = Modifier.semantics { contentDescription = unhideSpoken },
        ) {
            Text(text = stringResource(R.string.unhide_button))
        }
    }
}
