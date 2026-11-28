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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.ui.design.AtsButton
import app.ghghorg.atsassets.ui.design.AtsCard
import app.ghghorg.atsassets.ui.design.AtsScreen
import app.ghghorg.atsassets.ui.design.ButtonKind
import app.ghghorg.atsassets.ui.design.CardKind
import app.ghghorg.atsassets.ui.theme.AtsTheme
import app.ghghorg.atsassets.ui.theme.tokenDp
import app.ghghorg.atsassets.ui.theme.typeStyle

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
 * Allocation, one portfolio at a time (view 4, wireframe 04; iteration 19), drawn from its model (CR2) in the design system
 * (44a step 8; drawing spec/wireframes/design/04-allocation.html, D-41): the title band with Back, the chooser as the
 * standard pick field labelled "Portfolio", at the top outside any card (marker 1; decision 2), the portfolio's value as the
 * summary card with its age line, "Updating prices" and the failed line (marker 2), then the breakdowns by asset type,
 * holding, sector and geography (marker 3; 23b), or one plain line at $0.00 (marker 5). With no portfolio, "No portfolios
 * yet" and Create a portfolio (marker 4). One scrolling screen; pulling down refreshes the prices (decision 3). Back returns
 * to the portfolio or to More.
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
    val colors = AtsTheme.colors
    Box(modifier = modifier.fillMaxSize()) {
        // While the chooser is open, TalkBack reads only the chooser.
        val under = if (state.portfolioChoiceOpen) Modifier.clearAndSetSemantics { } else Modifier
        AtsScreen(
            stringResource(R.string.allocation_title),
            Modifier.then(under),
            onBack = actions.onBack,
            backLabel = stringResource(R.string.back_button),
            backEnabled = !state.saving,
        ) {
            PricePullToRefresh(state.refreshing, actions.onRefresh, Modifier.weight(1f).fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(tokenDp("spaceMargin")),
                    verticalArrangement = Arrangement.spacedBy(tokenDp("spaceGap")),
                ) {
                    if (shown == null) {
                        NoPortfoliosYet(actions.onCreatePortfolio)
                        if (state.portfolioNeedsOpeningBalance) {
                            Text(
                                text = stringResource(R.string.opening_balance_first),
                                style = typeStyle("body"),
                                color = colors.text,
                                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                            )
                            AtsButton(stringResource(R.string.guided_set_opening_balance), actions.onSetOpeningBalance, kind = ButtonKind.FILLED)
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
                            Text(text = stringResource(R.string.allocation_portfolio_zero), style = typeStyle("body"), color = colors.text)
                        } else {
                            breakdowns.forEach { BreakdownCard(it) }
                        }
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
 * Marker 2, as the summary card (D-41): "Portfolio value", the 32 figure, and from the oldest price used in this portfolio
 * "Prices as of … ET" with ", some stale" (16b) in condensed 12, then "Updating prices" and the failed line. TalkBack reads
 * the value and the age line as one.
 */
@Composable
private fun ValueCard(model: PortfolioAllocationModel) {
    val colors = AtsTheme.colors
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
    AtsCard(kind = CardKind.SUMMARY) {
        Column(modifier = Modifier.clearAndSetSemantics { contentDescription = spoken }, verticalArrangement = Arrangement.spacedBy(tokenDp("cardGap"))) {
            Text(text = stringResource(R.string.portfolio_value_label), style = typeStyle("small"), color = colors.onSummaryMuted)
            Text(text = model.value, style = typeStyle("bigFigure"), color = colors.onSummary)
            if (prices != null) {
                Text(
                    text =
                    stringResource(
                        if (prices.someStale) R.string.portfolio_prices_as_of_some_stale else R.string.portfolio_prices_as_of,
                        prices.asOf,
                    ),
                    style = typeStyle("oneDay"),
                    color = colors.onSummaryMuted,
                )
            }
        }
        PriceRefreshLines(refreshing = model.refresh.refreshing, failed = model.refresh.failed, noKey = model.refresh.noKey, onSummary = true)
    }
}
