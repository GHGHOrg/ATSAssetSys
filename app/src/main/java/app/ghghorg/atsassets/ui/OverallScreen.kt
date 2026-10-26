package app.ghghorg.atsassets.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.core.Money

/** Overall (view 1), cash only in iteration 02a: net worth, or the guided steps when the app holds no data. */
@Composable
fun OverallScreen(
    state: AppState,
    onSetOpeningBalance: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.overall_title),
            style = MaterialTheme.typography.titleLarge,
        )
        if (state.holdsData) {
            NetWorthCard(state.netWorthCents)
            Text(
                text = stringResource(R.string.no_portfolios_yet),
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            GuidedSteps(onSetOpeningBalance)
        }
    }
}

@Composable
private fun NetWorthCard(cents: Long) {
    val amount = Money.format(cents)
    val spoken = stringResource(R.string.net_worth_spoken, amount.replace("\$", ""))
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp).clearAndSetSemantics { contentDescription = spoken },
        ) {
            Text(
                text = stringResource(R.string.net_worth_label),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = amount,
                style = MaterialTheme.typography.headlineSmall,
            )
        }
    }
}

@Composable
private fun GuidedSteps(onSetOpeningBalance: () -> Unit) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.guided_step_1),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Button(onClick = onSetOpeningBalance) {
                Text(text = stringResource(R.string.guided_set_opening_balance))
            }
        }
    }
    LockedStep(stringResource(R.string.guided_step_2))
    LockedStep(stringResource(R.string.guided_step_3))
}

@Composable
private fun LockedStep(text: String) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.guided_locked),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
