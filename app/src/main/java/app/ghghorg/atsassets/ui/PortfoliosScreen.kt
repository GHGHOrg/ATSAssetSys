package app.ghghorg.atsassets.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.core.Money
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.PortfolioRefusal

/**
 * Portfolios (view 3, wireframe 3 frame A), create only in iteration 07: the rows in the order they were created,
 * display-only until 08 (07 decision 1), and New portfolio. Before the opening balance exists, New portfolio asks
 * for it instead of opening the dialog (4.2).
 */
@Composable
fun PortfoliosScreen(
    state: AppState,
    onNewPortfolio: () -> Unit,
    onSetOpeningBalance: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.tab_portfolios),
            style = MaterialTheme.typography.titleLarge,
        )
        if (state.portfolios.isEmpty()) {
            NoPortfoliosYet(onNewPortfolio)
        } else {
            state.portfolios.forEach { PortfolioRow(it) }
            OutlinedButton(onClick = onNewPortfolio) {
                Text(text = stringResource(R.string.new_portfolio))
            }
        }
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

// Holdings and values arrive with 09; until then every portfolio has no holdings and $0.00 (07 stubs).
@Composable
private fun PortfolioRow(portfolio: Portfolio) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp).semantics(mergeDescendants = true) {},
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = portfolio.name,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.portfolio_no_holdings),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = Money.format(0L),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

/**
 * The New portfolio dialog (wireframe 10d frames A and C): one Name field, the helper line, the first refusal under
 * the field, Create and Cancel. Create is always tappable. A tap outside does not close it, so the typed text is
 * not lost by accident; Back closes it like Cancel.
 */
@Composable
internal fun NewPortfolioDialog(
    dialog: PortfolioDialog,
    saving: Boolean,
    onNameChange: (String) -> Unit,
    onCreate: () -> Unit,
    onCancel: () -> Unit,
) {
    val error = portfolioMessage(dialog)
    AlertDialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(dismissOnClickOutside = false),
        title = { Text(text = stringResource(R.string.new_portfolio)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = dialog.nameText,
                    onValueChange = onNameChange,
                    label = { Text(text = stringResource(R.string.portfolio_name_label)) },
                    supportingText = {
                        Column {
                            if (error != null) {
                                Text(
                                    text = error,
                                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                                )
                            }
                            Text(text = stringResource(R.string.portfolio_name_helper))
                        }
                    },
                    isError = error != null,
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onCreate, enabled = !saving) {
                Text(text = stringResource(R.string.create_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel, enabled = !saving) {
                Text(text = stringResource(R.string.cancel_button))
            }
        },
    )
}

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
