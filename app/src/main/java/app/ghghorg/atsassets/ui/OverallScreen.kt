package app.ghghorg.atsassets.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.ui.theme.gainLossColor

/**
 * Overall (view 1, wireframe 01 state A), drawn from its model (CR2, 16c): net worth with its price age line and the
 * "updating" line (marker 2; decision 1), each portfolio with its value and gain/loss (marker 4), and the refresh-failed
 * line under them (marker 5); or the guided steps when the app holds no data. With cash and no portfolio it offers
 * Create a portfolio (criterion 24, part; 07 decision 2). Pulling down refreshes the prices (decision 2). Tapping net
 * worth opens Net Worth (Total) (navigation outline; 17), and so does the allocation under it (marker 3; 18). Under the
 * title, the backup reminder when a backup is due (marker 1; 32).
 */
@Composable
fun OverallScreen(
    state: AppState,
    onSetOpeningBalance: () -> Unit,
    onCreatePortfolio: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenPortfolio: (String) -> Unit = {},
    onRefresh: () -> Unit = {},
    onOpenNetWorth: () -> Unit = {},
    onOpenAllocationChoice: () -> Unit = {},
    onBackUpNow: () -> Unit = {},
    onDismissReminder: () -> Unit = {},
) {
    PricePullToRefresh(state.refreshing, onRefresh, modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.overall_title),
                style = MaterialTheme.typography.titleLarge,
            )
            state.backupReminder?.let { BackupReminderBanner(it, onBackUpNow, onDismissReminder) }
            if (state.holdsData) {
                val overall = state.overall
                NetWorthCard(
                    netWorth = overall.netWorth,
                    spokenNetWorth = overall.spokenNetWorth,
                    investments = overall.investments,
                    spokenInvestments = overall.spokenInvestments,
                    prices = overall.prices,
                    refresh = overall.refresh,
                    failedLineHere = false,
                    onOpen = onOpenNetWorth,
                )
                AllocationSummary(state, onOpenAllocationChoice, onOpenNetWorth)
                if (state.portfolios.isEmpty()) {
                    NoPortfoliosYet(onCreatePortfolio)
                } else {
                    PortfolioRows(overall.portfolios, onOpenPortfolio)
                }
                PriceRefreshLines(refreshing = false, failed = overall.refresh.failed, noKey = overall.refresh.noKey)
            } else {
                GuidedSteps(onSetOpeningBalance)
            }
        }
    }
}

/**
 * The backup reminder (view 1, wireframe 01 marker 1; spec section 9; 32), drawn from its model (CR2), under the title
 * and scrolling with the screen: a banner that says in words why a backup is due, with Back up now, which opens
 * Android's file screen as on Backup and restore, and Dismiss, which hides the banner until the app is next opened. The
 * buttons wrap at a large font. Under it, or alone in its place once the backup is made, the line of an export started
 * here (decision 1), read by TalkBack when it appears. An in-app banner, not an Android notification.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BackupReminderBanner(
    model: BackupReminderModel,
    onBackUpNow: () -> Unit,
    onDismiss: () -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val last = model.lastBackup
            when {
                model.due == BackupDue.NEVER ->
                    Text(text = stringResource(R.string.backup_due_never), style = MaterialTheme.typography.bodyLarge)

                model.due == BackupDue.OLD && last != null ->
                    Text(text = stringResource(R.string.backup_due_old, last.date, last.zone), style = MaterialTheme.typography.bodyLarge)
            }
            if (model.due != null) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onBackUpNow, enabled = model.canAct) {
                        Text(text = stringResource(R.string.backup_now))
                    }
                    OutlinedButton(onClick = onDismiss, enabled = model.canDismiss) {
                        Text(text = stringResource(R.string.backup_due_dismiss))
                    }
                }
            }
            BackupStatusLine(model.writing, noData = false, model.outcome, last)
        }
    }
}

/**
 * Net worth on Overall (wireframe 01 marker 2) and on Net Worth (Total) (wireframe 02 marker 1): net worth and, from the
 * oldest price used, "Prices as of … ET" with ", some stale" when any is (16b decision 1), then "Updating prices" while
 * a refresh runs (16c decision 1) and, with [failedLineHere], the failed line (Overall has it under its rows, marker 5;
 * Net Worth (Total) here, marker 1). TalkBack reads net worth and the age line
 * as one sentence that starts "Net worth" (AppStartTimingTest finds Overall by it). With [onOpen] the figure is a button
 * that opens Net Worth (Total) (navigation outline; 17).
 */
@Composable
internal fun NetWorthCard(
    netWorth: String,
    spokenNetWorth: String,
    investments: String,
    spokenInvestments: String,
    prices: PricesAsOfModel?,
    refresh: PriceRefreshModel,
    failedLineHere: Boolean,
    onOpen: (() -> Unit)? = null,
) {
    // CR5 (41), AC-68: the sentence still starts "Net worth" (AppStartTimingTest), then reads Investments.
    val base =
        stringResource(
            R.string.net_worth_investments_spoken,
            stringResource(R.string.net_worth_spoken, spokenNetWorth),
            spokenInvestments,
        )
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
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val figure =
                if (onOpen == null) {
                    Modifier.clearAndSetSemantics { contentDescription = spoken }
                } else {
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
                        }
                }
            Column(modifier = figure) {
                Text(
                    text = stringResource(R.string.net_worth_label),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = netWorth,
                    style = MaterialTheme.typography.headlineSmall,
                )
                // CR5 (41), wireframe 01 marker 2: Investments under net worth, not colored.
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = stringResource(R.string.net_worth_investments), style = MaterialTheme.typography.bodyMedium)
                    Text(text = investments, style = MaterialTheme.typography.bodyMedium)
                }
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
            PriceRefreshLines(refreshing = refresh.refreshing, failed = failedLineHere && refresh.failed, noKey = refresh.noKey)
        }
    }
}

/**
 * Marker 3 (18): "Allocation by" with the shown option, then that breakdown's donut and rows; a tap on them opens Net
 * Worth (Total), the choice does not. With net worth of $0.00 one plain line stands in (4.8; 17 decision 4).
 */
@Composable
private fun AllocationSummary(
    state: AppState,
    onOpenChoice: () -> Unit,
    onOpenNetWorth: () -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AllocationByButton(state.allocationBy, onOpenChoice)
            val model = state.allocation(state.allocationBy)
            if (model == null) {
                Text(text = stringResource(R.string.net_worth_zero), style = MaterialTheme.typography.bodyLarge)
            } else {
                AllocationBreakdown(model, onOpen = onOpenNetWorth)
            }
        }
    }
}

/**
 * Marker 4: each portfolio in the order created, with its value and its gain/loss in green or red with its sign, and
 * "(all holdings at cost)" while none of its holdings has a price (09 wording 4). A row opens that portfolio; Back
 * returns to Overall. Read by TalkBack as one button with its figures.
 */
@Composable
private fun PortfolioRows(
    rows: List<OverallPortfolioRowModel>,
    onOpen: (String) -> Unit,
) {
    Text(
        text = stringResource(R.string.overall_portfolios_heading),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { heading() },
    )
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            rows.forEachIndexed { index, row ->
                if (index > 0) HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                PortfolioRow(row) { onOpen(row.id) }
            }
        }
    }
}

@Composable
private fun PortfolioRow(
    row: OverallPortfolioRowModel,
    onOpen: () -> Unit,
) {
    val figures = row.figures
    val base = stringResource(R.string.overall_portfolio_row_spoken, row.name, figures.spokenValue, spoken(figures.gain.spoken))
    val spoken = if (figures.allAtCost) stringResource(R.string.portfolio_row_at_cost_spoken, base) else base
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
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = row.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(text = figures.value, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.End)
            Text(
                text = if (figures.allAtCost) "${figures.gain.text} ${stringResource(R.string.holdings_at_cost)}" else figures.gain.text,
                style = MaterialTheme.typography.bodyMedium,
                color = gainLossColor(sign(figures.gain)),
                textAlign = TextAlign.End,
            )
        }
        Text(text = ">", style = MaterialTheme.typography.bodyLarge)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LockedStep(text: String) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.guided_locked),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
            )
        }
    }
}
