package app.ghghorg.atsassets.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.ghghorg.atsassets.R

/** Allocation's callbacks, passed together. */
class PortfolioAllocationActions(
    val onBack: () -> Unit,
    val onRefresh: () -> Unit,
    val onOpenChoice: () -> Unit,
    val onCloseChoice: () -> Unit,
    val onChoose: (String) -> Unit,
    val onCreatePortfolio: () -> Unit,
    val onSetOpeningBalance: () -> Unit,
)

/**
 * Allocation, one portfolio at a time (view 4, wireframe 04; iteration 19), drawn from its model (CR2): "<" and the
 * title, the chooser (marker 1; decision 2), the portfolio value with its age line, "Updating prices" and the failed line
 * (marker 2), then the breakdowns by asset type, holding, sector and geography (marker 3; 23b), or one plain line at
 * $0.00 (marker 5). With no portfolio, "No portfolios yet" and Create a portfolio (marker 4). One scrolling screen;
 * pulling down refreshes the prices (decision 3). Back returns to the portfolio or to More.
 */
@Composable
fun PortfolioAllocationScreen(
    state: AppState,
    actions: PortfolioAllocationActions,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = !state.saving, onBack = actions.onBack)
    val model = state.portfolioAllocation
    val shown = model.shown
    Box(modifier = modifier.fillMaxSize()) {
        // While the chooser is open, TalkBack reads only the chooser.
        val under = if (state.portfolioChoiceOpen) Modifier.clearAndSetSemantics { } else Modifier
        PricePullToRefresh(state.refreshing, actions.onRefresh, Modifier.fillMaxSize().then(under)) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TitleRow(stringResource(R.string.allocation_title), actions.onBack, enabled = !state.saving)
                if (shown == null) {
                    NoPortfoliosYet(actions.onCreatePortfolio)
                    if (state.portfolioNeedsOpeningBalance) {
                        Text(
                            text = stringResource(R.string.opening_balance_first),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                        Button(onClick = actions.onSetOpeningBalance) {
                            Text(text = stringResource(R.string.guided_set_opening_balance))
                        }
                    }
                } else {
                    ChoiceButton(
                        label = stringResource(R.string.allocation_portfolio),
                        shown = shown.name,
                        spoken = stringResource(R.string.allocation_portfolio_spoken, shown.name),
                        onOpenChoice = actions.onOpenChoice,
                    )
                    ValueCard(model)
                    val breakdowns = model.breakdowns
                    if (breakdowns == null) {
                        Text(text = stringResource(R.string.allocation_portfolio_zero), style = MaterialTheme.typography.bodyLarge)
                    } else {
                        breakdowns.forEach { Breakdown(it) }
                    }
                }
            }
        }
        if (state.portfolioChoiceOpen && shown != null) {
            ChoiceLayer(
                title = stringResource(R.string.allocation_portfolio),
                options = model.portfolios.map { it.id to it.name },
                selected = shown.id,
                onChoose = actions.onChoose,
                onClose = actions.onCloseChoice,
            )
        }
    }
}

/**
 * Marker 2: "Portfolio value", the value, and from the oldest price used in this portfolio "Prices as of … ET" with
 * ", some stale" (16b), then "Updating prices" and the failed line. TalkBack reads the value and the age line as one.
 */
@Composable
private fun ValueCard(model: PortfolioAllocationModel) {
    val prices = model.prices
    val base = stringResource(R.string.allocation_value_spoken, model.spokenValue)
    val spoken =
        if (prices == null) {
            base
        } else {
            val age =
                stringResource(
                    if (prices.someStale) R.string.portfolio_prices_as_of_some_stale_spoken else R.string.portfolio_prices_as_of_spoken,
                    prices.asOf,
                )
            stringResource(R.string.net_worth_prices_spoken, base, age)
        }
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Column(modifier = Modifier.clearAndSetSemantics { contentDescription = spoken }) {
                Text(text = stringResource(R.string.portfolio_value_label), style = MaterialTheme.typography.bodyMedium)
                Text(text = model.value, style = MaterialTheme.typography.headlineSmall)
                if (prices != null) {
                    Text(
                        text =
                        stringResource(
                            if (prices.someStale) R.string.portfolio_prices_as_of_some_stale else R.string.portfolio_prices_as_of,
                            prices.asOf,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            PriceRefreshLines(refreshing = model.refresh.refreshing, failed = model.refresh.failed, noKey = model.refresh.noKey)
        }
    }
}

/** Marker 3: a breakdown's heading, its donut and its rows (18's parts; no tap). */
@Composable
private fun Breakdown(model: AllocationModel) {
    Text(
        text = allocationHeading(model.by),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { heading() },
    )
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        AllocationBreakdown(model, modifier = Modifier.padding(12.dp))
    }
}
