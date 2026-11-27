package app.ghghorg.atsassets.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextAlign
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.ui.design.AtsButton
import app.ghghorg.atsassets.ui.design.AtsCard
import app.ghghorg.atsassets.ui.design.AtsRowDivider
import app.ghghorg.atsassets.ui.design.AtsScreen
import app.ghghorg.atsassets.ui.design.AtsSectionHeading
import app.ghghorg.atsassets.ui.design.ButtonKind
import app.ghghorg.atsassets.ui.design.CardKind
import app.ghghorg.atsassets.ui.design.unavailable
import app.ghghorg.atsassets.ui.theme.AtsIcon
import app.ghghorg.atsassets.ui.theme.AtsTheme
import app.ghghorg.atsassets.ui.theme.LucideIcon
import app.ghghorg.atsassets.ui.theme.gainLossColor
import app.ghghorg.atsassets.ui.theme.tokenDp
import app.ghghorg.atsassets.ui.theme.typeStyle

/**
 * Overall (view 1, wireframe 01 state A), drawn from its model (CR2, 16c) in the design system (44a step 8; drawing
 * spec/wireframes/design/01-overall.html, D-38): net worth as the summary card with its price age line and the "updating"
 * line (marker 2; decision 1), each portfolio with its value and gain/loss (marker 4), and the refresh-failed line under
 * them (marker 5); or the guided steps when the app holds no data. With cash and no portfolio it offers Create a
 * portfolio (criterion 24, part; 07 decision 2). Pulling down refreshes the prices (decision 2). Tapping net worth opens
 * Net Worth (Total) (navigation outline; 17), and so does the allocation under it (marker 3; 18). Under the title, the
 * backup reminder when a backup is due (marker 1; 32).
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
    AtsScreen(stringResource(R.string.overall_title), modifier) {
        PricePullToRefresh(state.refreshing, onRefresh, Modifier.weight(1f).fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(tokenDp("spaceMargin")),
                verticalArrangement = Arrangement.spacedBy(tokenDp("spaceGap")),
            ) {
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
                        day = overall.day,
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
}

/**
 * The backup reminder (view 1, wireframe 01 marker 1; spec section 9; 32), drawn from its model (CR2), under the title
 * and scrolling with the screen: the catalog's card with actions (D-38), the message in 16 saying in words why a backup is
 * due, Back up now gold, which opens Android's file screen as on Backup and restore, and Dismiss outline, which hides the
 * banner until the app is next opened. The buttons wrap at a large font. Under it, or alone in its place once the backup
 * is made, the line of an export started here (decision 1), read by TalkBack when it appears.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BackupReminderBanner(
    model: BackupReminderModel,
    onBackUpNow: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = AtsTheme.colors
    AtsCard {
        val last = model.lastBackup
        when {
            model.due == BackupDue.NEVER ->
                Text(text = stringResource(R.string.backup_due_never), style = typeStyle("body"), color = colors.text)

            model.due == BackupDue.OLD && last != null ->
                Text(text = stringResource(R.string.backup_due_old, last.date, last.zone), style = typeStyle("body"), color = colors.text)
        }
        if (model.due != null) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(tokenDp("spaceGap"))) {
                AtsButton(stringResource(R.string.backup_now), onBackUpNow, kind = ButtonKind.FILLED, enabled = model.canAct)
                AtsButton(stringResource(R.string.backup_due_dismiss), onDismiss, enabled = model.canDismiss)
            }
        }
        BackupStatusLine(model.writing, noData = false, model.outcome, last)
    }
}

/**
 * Net worth on Overall (wireframe 01 marker 2) and on Net Worth (Total) (wireframe 02 marker 1), as the summary card (D-38):
 * the label, the 32 figure, Investments in 16, the 1-day line indented in condensed 12 in the summary card's own gain
 * color, and from the oldest price used "Prices as of … ET" with ", some stale" when any is (16b decision 1) in condensed
 * 12, then "Updating prices" while a refresh runs (16c decision 1) and, with [failedLineHere], the failed line (Overall has
 * it under its rows, marker 5; Net Worth (Total) here, marker 1). TalkBack reads net worth and the age line as one sentence
 * that starts "Net worth" (AppStartTimingTest finds Overall by it). With [onOpen] the figure is a button that opens Net
 * Worth (Total) (navigation outline; 17), with a chevron at its top right.
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
    day: DayLineModel? = null,
) {
    val colors = AtsTheme.colors
    // CR5 (41), AC-68: the sentence still starts "Net worth" (AppStartTimingTest), then reads Investments, then the 1-day
    // line when there is one (AC-67).
    val base =
        daySpoken(
            stringResource(
                R.string.net_worth_investments_spoken,
                stringResource(R.string.net_worth_spoken, spokenNetWorth),
                spokenInvestments,
            ),
            day,
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
    AtsCard(kind = CardKind.SUMMARY) {
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
        Column(modifier = figure, verticalArrangement = Arrangement.spacedBy(tokenDp("cardGap"))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.net_worth_label),
                    style = typeStyle("small"),
                    color = colors.onSummaryMuted,
                    modifier = Modifier.weight(1f),
                )
                if (onOpen != null) AtsIcon(LucideIcon.CHEVRON_RIGHT, tint = colors.onSummaryMuted, size = tokenDp("fieldMarkSize"))
            }
            Text(text = netWorth, style = typeStyle("bigFigure"), color = colors.onSummary)
            // CR5 (41), wireframe 01 marker 2: Investments under net worth, not colored.
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = stringResource(R.string.net_worth_investments), style = typeStyle("body"), color = colors.onSummaryMuted)
                Text(text = investments, style = typeStyle("body"), color = colors.onSummary)
            }
            // CR5 (41), wireframe 01 marker 2: the 1-day line, indented and smaller, under Investments.
            day?.let { DayLineRow(it, indented = true, onSummary = true) }
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
        PriceRefreshLines(refreshing = refresh.refreshing, failed = failedLineHere && refresh.failed, noKey = refresh.noKey, onSummary = true)
    }
}

/**
 * Marker 3 (18): "Allocation by" as the dense pick field in the card's head (D-38), then that breakdown's donut and rows;
 * a tap on them opens Net Worth (Total), the choice does not. With net worth of $0.00 one plain line stands in (4.8; 17
 * decision 4).
 */
@Composable
private fun AllocationSummary(
    state: AppState,
    onOpenChoice: () -> Unit,
    onOpenNetWorth: () -> Unit,
) {
    AtsCard {
        AllocationByButton(state.allocationBy, onOpenChoice)
        val model = state.allocation(state.allocationBy)
        if (model == null) {
            Text(text = stringResource(R.string.net_worth_zero), style = typeStyle("body"), color = AtsTheme.colors.text)
        } else {
            AllocationBreakdown(model, onOpen = onOpenNetWorth)
        }
    }
}

/**
 * Marker 4: under a section heading (D-38), each portfolio in the order created, with its value in 16 and its gain/loss
 * with its percentage in 14 in green or red with its sign, the 1-day line in condensed 12, and a chevron; "(all holdings at
 * cost)" while none of its holdings has a price (09 wording 4). A row opens that portfolio; Back returns to Overall. Read by
 * TalkBack as one button with its figures.
 */
@Composable
private fun PortfolioRows(
    rows: List<OverallPortfolioRowModel>,
    onOpen: (String) -> Unit,
) {
    AtsSectionHeading(stringResource(R.string.overall_portfolios_heading))
    AtsCard(padded = false) {
        rows.forEachIndexed { index, row ->
            if (index > 0) AtsRowDivider()
            PortfolioRow(row) { onOpen(row.id) }
        }
    }
}

@Composable
private fun PortfolioRow(
    row: OverallPortfolioRowModel,
    onOpen: () -> Unit,
) {
    val colors = AtsTheme.colors
    val figures = row.figures
    val base = stringResource(R.string.overall_portfolio_row_spoken, row.name, figures.spokenValue, gainSpoken(figures.gain))
    // CR5 (41), AC-67: the row reads its 1-day line after its gain/loss.
    val spoken = daySpoken(if (figures.allAtCost) stringResource(R.string.portfolio_row_at_cost_spoken, base) else base, figures.day)
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .heightIn(min = tokenDp("spaceTouch"))
            .clickable(role = Role.Button, onClick = onOpen)
            .clearAndSetSemantics {
                contentDescription = spoken
                role = Role.Button
                onClick {
                    onOpen()
                    true
                }
            }.padding(tokenDp("spaceInset")),
        horizontalArrangement = Arrangement.spacedBy(tokenDp("spaceInset")),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = row.name, style = typeStyle("body"), color = colors.text, modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text(text = figures.value, style = typeStyle("body"), color = colors.text, textAlign = TextAlign.End)
            Text(
                // CR6 (43): with its percentage; all at cost has none (no priced cost, 4.8).
                text = if (figures.allAtCost) "${gainFigures(figures.gain)} ${stringResource(R.string.holdings_at_cost)}" else gainFigures(figures.gain),
                style = typeStyle("small"),
                color = gainLossColor(sign(figures.gain)),
                textAlign = TextAlign.End,
            )
            figures.day?.let { DayRowText(it) }
        }
        AtsIcon(LucideIcon.CHEVRON_RIGHT, tint = colors.iconChevron, size = tokenDp("fieldMarkSize"))
    }
}

/**
 * With no data (wireframe 01 state B, markers 7 and 8; D-38): step 1 a card with its gold action; steps 2 and 3 in the
 * catalog's disabled state with Lucide's lock and the word "locked", so the meaning is never the fading alone.
 */
@Composable
private fun GuidedSteps(onSetOpeningBalance: () -> Unit) {
    AtsCard {
        Text(text = stringResource(R.string.guided_step_1), style = typeStyle("sectionHeading"), color = AtsTheme.colors.text)
        AtsButton(stringResource(R.string.guided_set_opening_balance), onSetOpeningBalance, kind = ButtonKind.FILLED)
    }
    LockedStep(stringResource(R.string.guided_step_2))
    LockedStep(stringResource(R.string.guided_step_3))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LockedStep(text: String) {
    val colors = AtsTheme.colors
    AtsCard(modifier = Modifier.unavailable(enabled = false)) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(tokenDp("fieldDensePadding")),
            verticalArrangement = Arrangement.spacedBy(tokenDp("cardGap")),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            AtsIcon(LucideIcon.LOCK, tint = colors.iconChevron, size = tokenDp("fieldMarkSize"))
            Text(text = text, style = typeStyle("body"), color = colors.text)
            Text(
                text = stringResource(R.string.guided_locked),
                modifier = Modifier.weight(1f),
                style = typeStyle("small"),
                color = colors.textSecondary,
                textAlign = TextAlign.End,
            )
        }
    }
}
