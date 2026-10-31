package app.ghghorg.atsassets.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.Holding
import app.ghghorg.atsassets.core.Money
import app.ghghorg.atsassets.core.Quantity
import java.math.BigInteger

/**
 * One portfolio (view 3, wireframe 3 frame B): "<", the name and the ⋮ menu with Rename (08 decision 1), the summary
 * and the holdings at cost (09 decision 3, wording 4). The price line and links arrive with 15 to 18 and 27, and a
 * holding opens Holding detail from 10. Back closes the menu first, then returns to the list.
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
        Column(
            modifier = Modifier.fillMaxSize().then(under).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TopBar(portfolio.name, actions)
            val holdings = state.holdingsOf(portfolio.id)
            SummaryCard(holdings)
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
                            HoldingRow(holding)
                        }
                    }
                }
            }
        }
        if (state.portfolioMenuOpen) PortfolioMenu(actions)
    }
}

/** The screen's callbacks, passed together. */
class PortfolioScreenActions(
    val onBack: () -> Unit,
    val onOpenMenu: () -> Unit,
    val onCloseMenu: () -> Unit,
    val onRename: () -> Unit,
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
 * Wireframe 3 marker 4. No holding has a price before 16, so each is valued at cost (4.8): the value equals the cost
 * and the gain/loss is $0.00 without color, with "(all holdings at cost)" while there are holdings (wording 4).
 */
@Composable
private fun SummaryCard(holdings: List<Holding>) {
    val cost = holdings.fold(Fraction.ZERO) { sum, holding -> sum + holding.cost }
    val shown = Money.format(cost)
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SummaryLine(stringResource(R.string.portfolio_value_label), shown, MaterialTheme.typography.headlineSmall)
            SummaryLine(stringResource(R.string.portfolio_cost_label), shown, MaterialTheme.typography.bodyLarge)
            SummaryLine(
                label = stringResource(R.string.portfolio_unrealized_label),
                amount = Money.format(Fraction.ZERO),
                amountStyle = MaterialTheme.typography.bodyLarge,
                atCost = allAtCost(holdings),
            )
        }
    }
}

@Composable
private fun SummaryLine(
    label: String,
    amount: String,
    amountStyle: TextStyle,
    atCost: Boolean = false,
) {
    val base = stringResource(R.string.spoken_label_dollars, label, amount.substringAfter('$'))
    val spoken = if (atCost) stringResource(R.string.portfolio_row_at_cost_spoken, base) else base
    Column(modifier = Modifier.clearAndSetSemantics { contentDescription = spoken }) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = amount, style = amountStyle)
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
 * A holding (wireframe 3 marker 5, format of marker 6): ticker and shares, the value at cost and "no price, at cost"
 * in place of a gain/loss (4.8). Read by TalkBack as one sentence. Not tappable until Holding detail (10).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HoldingRow(holding: Holding) {
    val shares = sharesText(holding.shares)
    val value = Money.format(holding.cost)
    val spoken = stringResource(R.string.holding_row_spoken, holding.ticker, shares, value.substringAfter('$'))
    FlowRow(
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = spoken },
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
            Text(
                text = stringResource(R.string.holding_no_price),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
            )
        }
    }
}

/** "1 share" or "10 shares", "2.5 shares" (section 6 quantity format). */
@Composable
internal fun sharesText(shares: Fraction): String {
    val shown = Quantity.format(shares)
    val one = shares.compareTo(Fraction.of(BigInteger.ONE, BigInteger.ONE)) == 0
    return stringResource(if (one) R.string.holding_share_one else R.string.holding_shares, shown)
}

/** No holding has a price before iteration 16, so every portfolio with holdings is all at cost (wording 4). */
internal fun allAtCost(holdings: List<Holding>): Boolean = holdings.isNotEmpty()

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
