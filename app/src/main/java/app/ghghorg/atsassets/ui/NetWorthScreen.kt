package app.ghghorg.atsassets.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.ui.design.AtsCard
import app.ghghorg.atsassets.ui.design.AtsRowDivider
import app.ghghorg.atsassets.ui.design.AtsScreen
import app.ghghorg.atsassets.ui.design.AtsSectionHeading
import app.ghghorg.atsassets.ui.theme.AtsTheme
import app.ghghorg.atsassets.ui.theme.tokenDp
import app.ghghorg.atsassets.ui.theme.typeStyle

/** Net Worth (Total)'s callbacks, passed together. */
class NetWorthActions(
    val onBack: () -> Unit,
    val onRefresh: () -> Unit,
)

/**
 * Net Worth (Total) (view 2, wireframe 02; iteration 17), drawn from its model (CR2) in the design system (44a step 8;
 * drawing spec/wireframes/design/02-net-worth.html, D-39): the title band with Back, net worth as the summary card with
 * its age line, "Updating prices" and the failed line (marker 1), then the composition (marker 2), or with net worth of
 * $0.00 one plain line (marker 6; decision 4). One scrolling screen; pulling down refreshes the prices (decision 2). Back
 * returns to Overall or More. Under the composition the breakdowns by asset type, portfolio and holding (18), then sector
 * and geography (23b), each under its section heading.
 */
@Composable
fun NetWorthScreen(
    state: AppState,
    actions: NetWorthActions,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = actions.onBack)
    val model = state.netWorthTotal
    AtsScreen(stringResource(R.string.net_worth_title), modifier, onBack = actions.onBack, backLabel = stringResource(R.string.back_button)) {
        PricePullToRefresh(state.refreshing, actions.onRefresh, Modifier.weight(1f).fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(tokenDp("spaceMargin")),
                verticalArrangement = Arrangement.spacedBy(tokenDp("spaceGap")),
            ) {
                NetWorthCard(
                    netWorth = model.netWorth,
                    spokenNetWorth = model.spokenNetWorth,
                    investments = model.investments,
                    spokenInvestments = model.spokenInvestments,
                    prices = model.prices,
                    refresh = model.refresh,
                    failedLineHere = true,
                    day = model.day,
                )
                val composition = model.composition
                if (composition == null) {
                    Text(text = stringResource(R.string.net_worth_zero), style = typeStyle("body"), color = AtsTheme.colors.text)
                } else {
                    Composition(composition, model)
                    AllocationBy.entries.forEach { by -> state.allocation(by)?.let { BreakdownCard(it) } }
                }
            }
        }
    }
}

/**
 * Marker 2: "Composition" as a section heading, then a plain list of amounts (cash, then each portfolio in the order
 * created), ending with net worth under a divider in medium, which they add up to (4.10). No donut. TalkBack reads each row
 * as its name and amount.
 */
@Composable
private fun Composition(
    composition: CompositionModel,
    model: NetWorthModel,
) {
    AtsSectionHeading(stringResource(R.string.net_worth_composition_heading))
    AtsCard(padded = false) {
        CompositionRow(stringResource(R.string.net_worth_composition_cash), composition.cash, composition.spokenCash)
        composition.portfolios.forEach { CompositionRow(it.name, it.amount, it.spokenAmount) }
        AtsRowDivider()
        CompositionRow(stringResource(R.string.net_worth_label), model.netWorth, model.spokenNetWorth, total = true)
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
    val style = typeStyle(if (total) "sectionHeading" else "body")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = tokenDp("spaceTouch"))
            .clearAndSetSemantics { contentDescription = spoken }
            .padding(horizontal = tokenDp("spaceInset")),
        horizontalArrangement = Arrangement.spacedBy(tokenDp("rowControlGap")),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = name, style = style, color = AtsTheme.colors.text, modifier = Modifier.weight(1f))
        Text(text = amount, style = style, color = AtsTheme.colors.text, textAlign = TextAlign.End)
    }
}
