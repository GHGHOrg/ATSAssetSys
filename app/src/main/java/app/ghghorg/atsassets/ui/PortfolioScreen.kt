package app.ghghorg.atsassets.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.Quantity
import app.ghghorg.atsassets.ui.theme.gainLossColor
import java.math.BigDecimal

/**
 * One portfolio (view 3, wireframe 3 frame B): "<", the name and the ⋮ menu with Rename (08 decision 1), the summary
 * with its price age line and refresh lines (16b), and the holdings with their flags. The links arrive with 18 and 27,
 * and a holding opens Holding detail from 10. Pulling down refreshes the prices (16b decision 2). Back closes the menu
 * first, then returns to the list.
 */
@Composable
fun PortfolioScreen(
    state: AppState,
    actions: PortfolioScreenActions,
    modifier: Modifier = Modifier,
) {
    val portfolio = state.shownPortfolio ?: return
    BackHandler(onBack = if (state.portfolioMenuOpen) actions.onCloseMenu else actions.onBack)
    Box(modifier = modifier.fillMaxSize()) {
        // While the menu is open, TalkBack reads only the menu.
        val under = if (state.portfolioMenuOpen) Modifier.clearAndSetSemantics { } else Modifier
        PricePullToRefresh(state.refreshing, actions.onRefresh, Modifier.fillMaxSize()) {
            PortfolioContent(state, portfolio.id, portfolio.name, actions, Modifier.fillMaxSize().then(under))
        }
        if (state.portfolioMenuOpen) PortfolioMenu(actions)
    }
}

@Composable
private fun PortfolioContent(
    state: AppState,
    portfolioId: String,
    name: String,
    actions: PortfolioScreenActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TopBar(name, actions)
        val holdings = state.portfolioHoldingRows(portfolioId)
        SummaryCard(state.portfolioFigures(portfolioId), state.priceRefresh)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = stringResource(R.string.portfolio_holdings),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            if (holdings.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.portfolio_unrealized_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (holdings.isEmpty()) {
            Text(
                text = stringResource(R.string.portfolio_no_holdings_yet),
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    holdings.forEachIndexed { index, holding ->
                        if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        HoldingRow(holding) { actions.onOpenHolding(holding.ticker) }
                    }
                }
            }
        }
        HiddenLink(actions.onOpenHidden)
    }
}

/** "Hidden holdings >", always listed, even with none hidden (wireframe 03 marker 7, wireframe 09 marker 1; 15 wording 2). */
@Composable
private fun HiddenLink(onOpen: () -> Unit) {
    val spoken = stringResource(R.string.hidden_link_spoken)
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onOpen)
                .clearAndSetSemantics {
                    contentDescription = spoken
                    role = Role.Button
                    onClick {
                        onOpen()
                        true
                    }
                }.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = stringResource(R.string.hidden_link), style = MaterialTheme.typography.bodyLarge)
            Text(text = ">", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/** The screen's callbacks, passed together. */
class PortfolioScreenActions(
    val onBack: () -> Unit,
    val onOpenMenu: () -> Unit,
    val onCloseMenu: () -> Unit,
    val onRename: () -> Unit,
    val onOpenHolding: (String) -> Unit,
    val onOpenHidden: () -> Unit,
    // 16b: pull-to-refresh (decision 2).
    val onRefresh: () -> Unit = {},
)

@Composable
private fun TopBar(
    name: String,
    actions: PortfolioScreenActions,
) {
    val back = stringResource(R.string.back_button)
    val menu = stringResource(R.string.portfolio_menu)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = actions.onBack, modifier = Modifier.semantics { contentDescription = back }) {
            Text(text = "<", style = MaterialTheme.typography.titleLarge)
        }
        Text(
            text = name,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        TextButton(onClick = actions.onOpenMenu, modifier = Modifier.semantics { contentDescription = menu }) {
            Text(text = "⋮", style = MaterialTheme.typography.titleLarge)
        }
    }
}

/**
 * Wireframe 3 marker 4, drawn from its model (CR2, 16a decision 1): the value with each holding at its latest price or
 * at cost (4.8), the cost, and the gain/loss in green or red with its sign, with "(all holdings at cost)" while no
 * holding with shares has a price (09 wording 4). Then the price age line, the oldest price used with ", some stale"
 * when any is (16b decision 1), and under it "Updating prices" and the refresh-failed line (section 6; AC-42).
 */
@Composable
private fun SummaryCard(
    figures: PortfolioFiguresModel,
    refresh: PriceRefreshModel,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SummaryLine(stringResource(R.string.portfolio_value_label), figures.value, figures.spokenValue, MaterialTheme.typography.headlineSmall)
            SummaryLine(stringResource(R.string.portfolio_cost_label), figures.cost, figures.spokenCost, MaterialTheme.typography.bodyLarge)
            SummaryLine(
                label = stringResource(R.string.portfolio_unrealized_label),
                amount = figures.gain.text,
                spokenAmount = spoken(figures.gain.spoken),
                amountStyle = MaterialTheme.typography.bodyLarge,
                atCost = figures.allAtCost,
                amountColor = gainLossColor(sign(figures.gain)),
            )
            figures.prices?.let { PricesAsOfLine(it) }
            PriceRefreshLines(refresh.refreshing, refresh.failed)
        }
    }
}

/** "Prices as of 2026-11-06 16:00 ET", with ", some stale" when any price used is stale (16b decision 1). */
@Composable
private fun PricesAsOfLine(prices: PricesAsOfModel) {
    val text = stringResource(if (prices.someStale) R.string.portfolio_prices_as_of_some_stale else R.string.portfolio_prices_as_of, prices.asOf)
    val spoken =
        stringResource(
            if (prices.someStale) R.string.portfolio_prices_as_of_some_stale_spoken else R.string.portfolio_prices_as_of_spoken,
            prices.asOf,
        )
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.clearAndSetSemantics { contentDescription = spoken },
    )
}

@Composable
private fun SummaryLine(
    label: String,
    amount: String,
    spokenAmount: String,
    amountStyle: TextStyle,
    atCost: Boolean = false,
    amountColor: Color = Color.Unspecified,
) {
    val base = stringResource(R.string.spoken_label_dollars, label, spokenAmount)
    val spoken = if (atCost) stringResource(R.string.portfolio_row_at_cost_spoken, base) else base
    Column(modifier = Modifier.clearAndSetSemantics { contentDescription = spoken }) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = amount, style = amountStyle, color = amountColor)
        if (atCost) {
            Text(
                text = stringResource(R.string.holdings_at_cost),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * A holding (wireframe 3 marker 5, format of marker 6), drawn from its model (CR2, 16a decision 1): ticker and shares,
 * the value and the gain/loss in green or red with its sign at its latest price, or "no price, at cost" (4.8); a
 * sold-out holding shows 0 shares and $0.00 with no flag (11 wording 4). A stale price shows "stale" with its own time
 * under the gain (wireframe 3 marker 6; 16b). One tap target opening Holding detail (iteration 10), read by TalkBack
 * as one button.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HoldingRow(
    holding: PortfolioHoldingRowModel,
    onOpen: () -> Unit,
) {
    val shares = stringResource(if (holding.oneShare) R.string.holding_share_one else R.string.holding_shares, holding.shares)
    val value = holding.value
    val soldOut = holding.soldOut
    val gain = holding.gain
    val staleAsOf = holding.staleAsOf
    val spoken =
        when {
            soldOut -> stringResource(R.string.holding_row_sold_out_spoken, holding.ticker, shares, holding.spokenValue)

            gain is GainModel.Figure && staleAsOf != null ->
                stringResource(R.string.holding_row_gain_stale_spoken, holding.ticker, shares, holding.spokenValue, spoken(gain.spoken), staleAsOf)

            gain is GainModel.Figure -> stringResource(R.string.holding_row_gain_spoken, holding.ticker, shares, holding.spokenValue, spoken(gain.spoken))

            else -> stringResource(R.string.holding_row_spoken, holding.ticker, shares, holding.spokenValue)
        }
    FlowRow(
        modifier =
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onOpen)
            .clearAndSetSemantics {
                contentDescription = spoken
                role = Role.Button
                onClick {
                    onOpen()
                    true
                }
            },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Column {
            Text(
                text = holding.ticker,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = shares,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.End,
            )
            if (!soldOut) {
                when (gain) {
                    is GainModel.Figure ->
                        Text(
                            text = gain.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = gainLossColor(sign(gain)),
                            textAlign = TextAlign.End,
                        )

                    GainModel.NoPrice ->
                        Text(
                            text = stringResource(R.string.holding_no_price),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.End,
                        )
                }
            }
            if (staleAsOf != null) {
                Text(
                    text = stringResource(R.string.holding_row_stale, staleAsOf),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

/**
 * "1 share" only when the quantity as shown is exactly "1", otherwise "0 shares", "10 shares", "2.5 shares" (section 6
 * quantity format; 12 decision 4): Android's plural strings take whole numbers only, so the shown text decides.
 */
@Composable
internal fun sharesText(shares: Fraction): String {
    val shown = Quantity.format(shares)
    return stringResource(if (shown == "1") R.string.holding_share_one else R.string.holding_shares, shown)
}

/** As [sharesText], for a quantity as typed. */
@Composable
internal fun sharesText(shares: BigDecimal): String = sharesText(Fraction.of(shares))

/**
 * The ⋮ menu (08 decision 1), drawn inside the activity window: a clear layer that closes the menu on a tap outside,
 * and a card under ⋮ with Rename. No DropdownMenu or Popup: a separate window would take the focus (CLAUDE.md).
 */
@Composable
private fun PortfolioMenu(actions: PortfolioScreenActions) {
    val title = stringResource(R.string.portfolio_menu)
    Box(
        modifier = Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { actions.onCloseMenu() } },
        contentAlignment = Alignment.TopEnd,
    ) {
        Surface(
            modifier = Modifier.padding(top = 56.dp, end = 16.dp).semantics { paneTitle = title },
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 3.dp,
            shadowElevation = 3.dp,
        ) {
            TextButton(onClick = actions.onRename, modifier = Modifier.padding(horizontal = 8.dp)) {
                Text(text = stringResource(R.string.rename_button), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
