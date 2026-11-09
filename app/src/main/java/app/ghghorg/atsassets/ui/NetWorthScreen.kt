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
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.ghghorg.atsassets.R

/** Net Worth (Total)'s callbacks, passed together. */
class NetWorthActions(
    val onBack: () -> Unit,
    val onRefresh: () -> Unit,
)

/**
 * Net Worth (Total) (view 2, wireframe 02; iteration 17), drawn from its model (CR2): "<" and the title, net worth with
 * its age line, "Updating prices" and the failed line (marker 1), then the composition (marker 2), or with net worth of
 * $0.00 one plain line (marker 6; decision 4). One scrolling screen; pulling down refreshes the prices (decision 2).
 * Back returns to Overall or More. The breakdowns follow in 18 and 23.
 */
@Composable
fun NetWorthScreen(
    state: AppState,
    actions: NetWorthActions,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = actions.onBack)
    val model = state.netWorthTotal
    PricePullToRefresh(state.refreshing, actions.onRefresh, modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TitleRow(stringResource(R.string.net_worth_title), actions.onBack)
            NetWorthCard(
                netWorth = model.netWorth,
                spokenNetWorth = model.spokenNetWorth,
                prices = model.prices,
                refresh = model.refresh,
                failedLineHere = true,
            )
            val composition = model.composition
            if (composition == null) {
                Text(text = stringResource(R.string.net_worth_zero), style = MaterialTheme.typography.bodyLarge)
            } else {
                Composition(composition, model)
            }
        }
    }
}

/**
 * Marker 2: "Composition", a plain list of amounts (cash, then each portfolio in the order created), ending with net
 * worth, which they add up to (4.10). No donut. TalkBack reads each row as its name and amount.
 */
@Composable
private fun Composition(
    composition: CompositionModel,
    model: NetWorthModel,
) {
    Text(
        text = stringResource(R.string.net_worth_composition_heading),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { heading() },
    )
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CompositionRow(stringResource(R.string.net_worth_composition_cash), composition.cash, composition.spokenCash)
            composition.portfolios.forEach { CompositionRow(it.name, it.amount, it.spokenAmount) }
            HorizontalDivider()
            CompositionRow(stringResource(R.string.net_worth_label), model.netWorth, model.spokenNetWorth, total = true)
        }
    }
}

@Composable
private fun CompositionRow(
    name: String,
    amount: String,
    spokenAmount: String,
    total: Boolean = false,
) {
    val spoken = stringResource(R.string.net_worth_composition_row_spoken, name, spokenAmount)
    val weight = if (total) FontWeight.Medium else FontWeight.Normal
    Row(
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = spoken },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = name, style = MaterialTheme.typography.bodyLarge, fontWeight = weight, modifier = Modifier.weight(1f))
        Text(text = amount, style = MaterialTheme.typography.bodyLarge, fontWeight = weight, textAlign = TextAlign.End)
    }
}
