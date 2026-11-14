package app.ghghorg.atsassets.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.core.Holding
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.PortfolioRefusal
import app.ghghorg.atsassets.ui.theme.gainLossColor

/**
 * Portfolios (view 3, wireframe 3 frame A): the rows in the order they were created, each opening its portfolio
 * (08 decision 3), and New portfolio. Before the opening balance exists, New portfolio asks for it instead of
 * opening the dialog (4.2). Under the list, "Updating prices" and the refresh-failed line (marker 1; 16b); pulling
 * down refreshes the prices (16b decision 2).
 */
@Composable
fun PortfoliosScreen(
    state: AppState,
    onOpenPortfolio: (String) -> Unit,
    onNewPortfolio: () -> Unit,
    onSetOpeningBalance: () -> Unit,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit = {},
) {
    PricePullToRefresh(state.refreshing, onRefresh, modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.tab_portfolios),
                style = MaterialTheme.typography.titleLarge,
            )
            if (state.portfolios.isEmpty()) {
                NoPortfoliosYet(onNewPortfolio)
            } else {
                state.portfolios.forEach { portfolio ->
                    PortfolioRow(portfolio, state.holdingsOf(portfolio.id), state.portfolioFigures(portfolio.id)) { onOpenPortfolio(portfolio.id) }
                }
                OutlinedButton(onClick = onNewPortfolio) {
                    Text(text = stringResource(R.string.new_portfolio))
                }
            }
            PriceRefreshLines(state.priceRefresh.refreshing, state.priceRefresh.failed, state.priceRefresh.noKey)
            if (state.portfolioNeedsOpeningBalance) {
                Text(
                    text = stringResource(R.string.opening_balance_first),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
                Button(onClick = onSetOpeningBalance) {
                    Text(text = stringResource(R.string.guided_set_opening_balance))
                }
            }
        }
    }
}

/** "No portfolios yet" with a Create a portfolio button (wireframe 3; Overall, 07 decision 2). */
@Composable
internal fun NoPortfoliosYet(onCreate: () -> Unit) {
    Text(
        text = stringResource(R.string.no_portfolios_yet),
        style = MaterialTheme.typography.bodyLarge,
    )
    Button(onClick = onCreate) {
        Text(text = stringResource(R.string.create_portfolio))
    }
}

// Wireframe 3 marker 1, drawn from its model (CR2, 16a decision 1): name, number of holdings, the value with each
// holding at its latest price or at cost (4.8), and the gain/loss in green or red with its sign, with "(all holdings at
// cost)" while no holding with shares has a price (wording 4). No age line on a row (view 3); a portfolio's screen has
// it. The whole row is one tap target without ">", read by TalkBack as one button with the figures (08 decision 3,
// wording 5).
@Composable
private fun PortfolioRow(
    portfolio: Portfolio,
    holdings: List<Holding>,
    figures: PortfolioFiguresModel,
    onOpen: () -> Unit,
) {
    val count =
        if (holdings.isEmpty()) {
            stringResource(R.string.portfolio_no_holdings)
        } else {
            pluralStringResource(R.plurals.portfolio_holding_count, holdings.size, holdings.size)
        }
    val value = figures.value
    val gain = figures.gain.text
    val atCost = figures.allAtCost
    val dollars = figures.spokenValue
    // Wireframe 3 shows no gain/loss line on a portfolio with no holdings ("Retirement").
    val spoken =
        if (holdings.isEmpty()) {
            stringResource(R.string.portfolio_row_empty_spoken, portfolio.name, count, dollars)
        } else {
            val base = stringResource(R.string.portfolio_row_spoken, portfolio.name, count, dollars, spoken(figures.gain.spoken))
            if (atCost) stringResource(R.string.portfolio_row_at_cost_spoken, base) else base
        }
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
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
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = portfolio.name,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = count,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (holdings.isNotEmpty()) {
                Text(
                    text = if (atCost) "$gain ${stringResource(R.string.holdings_at_cost)}" else gain,
                    style = MaterialTheme.typography.bodyMedium,
                    color = gainLossColor(sign(figures.gain)),
                )
            }
        }
    }
}

/** The wording that differs between New portfolio and Rename portfolio (wireframe 10d frames A and B). */
internal class PortfolioNameWording(
    val title: String,
    val helper: String,
    val confirm: String,
)

/**
 * The New portfolio and Rename portfolio dialog (wireframe 10d frames A to C): one Name field, the helper line, the
 * first refusal under the field, Cancel and the confirm button. The confirm button is always tappable. Drawn inside
 * the activity's window over a dimmed layer, not as a separate dialog window: a separate window takes the focus, and
 * MainActivity covers the app whenever its window loses focus (defect found in 07 phone check 2). The layer takes
 * every tap, so a tap outside the card does nothing and the typed text is not lost by accident; Back closes it like
 * Cancel.
 */
@Composable
internal fun PortfolioNameDialog(
    wording: PortfolioNameWording,
    dialog: PortfolioDialog,
    saving: Boolean,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    BackHandler(enabled = !saving, onBack = onCancel)
    val error = portfolioMessage(dialog)
    val title = wording.title
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = SCRIM_ALPHA))
            .pointerInput(Unit) { detectTapGestures { } }
            // The app draws edge to edge, so the window does not shrink for the keyboard: the card is centred in
            // the space above it and scrolls there (defect found in 08 phone check 9a).
            .imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.padding(24.dp).widthIn(max = 560.dp).semantics { paneTitle = title },
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() },
                )
                OutlinedTextField(
                    value = dialog.nameText,
                    onValueChange = onNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = stringResource(R.string.portfolio_name_label)) },
                    supportingText = {
                        Column {
                            if (error != null) {
                                Text(
                                    text = error,
                                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                                )
                            }
                            Text(text = wording.helper)
                        }
                    },
                    isError = error != null,
                    singleLine = true,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    TextButton(onClick = onCancel, enabled = !saving) {
                        Text(text = stringResource(R.string.cancel_button))
                    }
                    TextButton(onClick = onConfirm, enabled = !saving) {
                        Text(text = wording.confirm)
                    }
                }
            }
        }
    }
}

// The dim behind the dialog, as Material's own dialogs use.
private const val SCRIM_ALPHA = 0.32f

@Composable
private fun portfolioMessage(dialog: PortfolioDialog): String? {
    if (dialog.save == SaveRefusal.SAVE_FAILED) return stringResource(R.string.save_failed)
    return when (val refusal = dialog.refusal) {
        null -> null

        PortfolioRefusal.TooShort -> stringResource(R.string.portfolio_name_too_short)

        PortfolioRefusal.NoLetter -> stringResource(R.string.portfolio_name_no_letter)

        is PortfolioRefusal.Duplicate -> stringResource(R.string.portfolio_name_duplicate, refusal.otherName)

        // The dialog opens only once the opening balance exists; the same words as the tab's line, should it happen.
        PortfolioRefusal.NoOpeningBalance -> stringResource(R.string.opening_balance_first)
    }
}
