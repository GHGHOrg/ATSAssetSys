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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.core.Money
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.PortfolioRefusal

/**
 * Portfolios (view 3, wireframe 3 frame A): the rows in the order they were created, each opening its portfolio
 * (08 decision 3), and New portfolio. Before the opening balance exists, New portfolio asks for it instead of
 * opening the dialog (4.2).
 */
@Composable
fun PortfoliosScreen(
    state: AppState,
    onOpenPortfolio: (String) -> Unit,
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
            state.portfolios.forEach { portfolio -> PortfolioRow(portfolio) { onOpenPortfolio(portfolio.id) } }
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

// Holdings and values arrive with 09; until then every portfolio has no holdings and $0.00 (07 stubs). The whole
// row is one tap target without ">", read by TalkBack as one button (08 decision 3).
@Composable
private fun PortfolioRow(
    portfolio: Portfolio,
    onOpen: () -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onOpen).padding(12.dp),
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
