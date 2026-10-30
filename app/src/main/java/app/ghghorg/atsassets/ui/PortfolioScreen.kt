package app.ghghorg.atsassets.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.core.Money

/**
 * One portfolio (view 3, wireframe 3 frame B) in iteration 08: "<", the name and the ⋮ menu with Rename
 * (08 decision 1), the summary at $0.00 and "No holdings yet" (08 decision 2). The values, holdings, price line and
 * links arrive with 09, 15 to 18 and 27. Back closes the menu first, then returns to the list.
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
            SummaryCard()
            Text(
                text = stringResource(R.string.portfolio_holdings),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.portfolio_no_holdings_yet),
                style = MaterialTheme.typography.bodyLarge,
            )
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

// Wireframe 3 marker 4 at $0.00: no holdings until 09, so value, cost and gain/loss are all zero, without color.
@Composable
private fun SummaryCard() {
    val zero = Money.format(0L)
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SummaryLine(stringResource(R.string.portfolio_value_label), zero, MaterialTheme.typography.headlineSmall)
            SummaryLine(stringResource(R.string.portfolio_cost_label), zero, MaterialTheme.typography.bodyLarge)
            SummaryLine(stringResource(R.string.portfolio_unrealized_label), zero, MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun SummaryLine(
    label: String,
    amount: String,
    amountStyle: TextStyle,
) {
    Column(modifier = Modifier.semantics(mergeDescendants = true) {}) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = amount, style = amountStyle)
    }
}

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
