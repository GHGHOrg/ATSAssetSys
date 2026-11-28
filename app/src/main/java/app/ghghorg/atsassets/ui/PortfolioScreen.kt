package app.ghghorg.atsassets.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.Quantity
import app.ghghorg.atsassets.ui.design.AtsCard
import app.ghghorg.atsassets.ui.design.AtsIconButton
import app.ghghorg.atsassets.ui.design.AtsMenu
import app.ghghorg.atsassets.ui.design.AtsMenuItem
import app.ghghorg.atsassets.ui.design.AtsRowDivider
import app.ghghorg.atsassets.ui.design.AtsScreen
import app.ghghorg.atsassets.ui.design.CardKind
import app.ghghorg.atsassets.ui.theme.AtsIcon
import app.ghghorg.atsassets.ui.theme.AtsTheme
import app.ghghorg.atsassets.ui.theme.LucideIcon
import app.ghghorg.atsassets.ui.theme.gainLossColor
import app.ghghorg.atsassets.ui.theme.tokenDp
import app.ghghorg.atsassets.ui.theme.typeStyle
import java.math.BigDecimal

/**
 * One portfolio (view 3, wireframe 3 frame B), in the design system (44a step 8 group (b); drawing
 * spec/wireframes/design/03-portfolios.html frames B and C, D-38): the title band with Back, the name and the ⋮ menu
 * with Rename and Delete (08 decision 1; 29), the summary card with its price age line and refresh lines (16b), the
 * holdings card with its heading and column head, and the links as a card of rows with chevrons. A holding opens Holding
 * detail (10). Pulling down refreshes the prices (16b decision 2). Back closes the menu first, then returns to the list.
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
        // While the menu is open, TalkBack reads only the menu. The Delete dialog and the blocked message are drawn by
        // AppContent over the bar, which hides this screen from TalkBack while they are open (29 step 10).
        val under = if (state.portfolioMenuOpen) Modifier.clearAndSetSemantics { } else Modifier
        AtsScreen(
            title = portfolio.name,
            modifier = Modifier.fillMaxSize().then(under),
            onBack = actions.onBack,
            backLabel = stringResource(R.string.back_button),
            actions = {
                AtsIconButton(LucideIcon.ELLIPSIS_VERTICAL, stringResource(R.string.portfolio_menu), actions.onOpenMenu, tint = AtsTheme.colors.iconBand)
            },
        ) {
            PricePullToRefresh(state.refreshing, actions.onRefresh, Modifier.weight(1f).fillMaxWidth()) {
                PortfolioContent(state, portfolio.id, actions, Modifier.fillMaxSize())
            }
        }
        // The catalog's menu inside the window, over a scrim, under ⋮ (drawing 03 frame C, D-34). No DropdownMenu or
        // Popup: a separate window would take the focus (CLAUDE.md).
        if (state.portfolioMenuOpen) {
            AtsMenu(
                items =
                listOf(
                    AtsMenuItem(stringResource(R.string.rename_button), actions.onRename),
                    AtsMenuItem(stringResource(R.string.portfolio_delete_button), actions.onDelete),
                ),
                onDismiss = actions.onCloseMenu,
                title = stringResource(R.string.portfolio_menu),
            )
        }
    }
}

@Composable
private fun PortfolioContent(
    state: AppState,
    portfolioId: String,
    actions: PortfolioScreenActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(tokenDp("spaceMargin")),
        verticalArrangement = Arrangement.spacedBy(tokenDp("spaceGap")),
    ) {
        val holdings = state.portfolioHoldingRows(portfolioId)
        SummaryCard(state.portfolioFigures(portfolioId), state.priceRefresh)
        HoldingsCard(holdings, actions)
        // View 3's links, in its order: Allocation (19), Transaction history (27), Hidden holdings.
        AtsCard(padded = false) {
            LinkRow(stringResource(R.string.allocation_title), stringResource(R.string.allocation_link_spoken), actions.onOpenAllocation)
            AtsRowDivider()
            LinkRow(stringResource(R.string.history_link), stringResource(R.string.history_link_spoken), actions.onOpenHistory)
            AtsRowDivider()
            LinkRow(stringResource(R.string.hidden_link), stringResource(R.string.hidden_link_spoken), actions.onOpenHidden)
        }
    }
}

/**
 * The holdings card (drawing 03 frame B, note 4): "Holdings" in medium 16 as its heading and, with holdings, "Unrealized
 * gain/loss" in Asap Condensed 14 medium, secondary, over the right column; then a row per holding, or one plain line.
 */
@Composable
private fun HoldingsCard(
    holdings: List<PortfolioHoldingRowModel>,
    actions: PortfolioScreenActions,
) {
    val colors = AtsTheme.colors
    AtsCard(padded = false) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = tokenDp("spaceInset"), end = tokenDp("spaceInset"), top = tokenDp("spaceInset")),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = stringResource(R.string.portfolio_holdings),
                style = typeStyle("sectionHeading"),
                color = colors.text,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            if (holdings.isNotEmpty()) {
                // Over the figures, not the chevron: the chevron's width and the row's gap to its right.
                Text(
                    text = stringResource(R.string.portfolio_unrealized_label),
                    style = typeStyle("columnHead"),
                    color = colors.textSecondary,
                    modifier = Modifier.padding(end = tokenDp("fieldMarkSize") + tokenDp("spaceInset")),
                )
            }
        }
        if (holdings.isEmpty()) {
            Text(
                text = stringResource(R.string.portfolio_no_holdings_yet),
                style = typeStyle("body"),
                color = colors.text,
                modifier = Modifier.padding(tokenDp("spaceInset")),
            )
        } else {
            holdings.forEachIndexed { index, holding ->
                if (index > 0) AtsRowDivider()
                HoldingRow(holding) { actions.onOpenHolding(holding.ticker) }
            }
        }
    }
}

/**
 * A link to another screen (wireframe 03; D-31): a 48 row with the name in 16 and a chevron, "Allocation" (19) and
 * "Hidden holdings", the latter always listed, even with none hidden (wireframe 09 marker 1; 15 wording 2). TalkBack
 * reads [spoken] and a button.
 */
@Composable
private fun LinkRow(
    label: String,
    spoken: String,
    onOpen: () -> Unit,
) {
    val colors = AtsTheme.colors
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
            }.padding(horizontal = tokenDp("spaceInset")),
        horizontalArrangement = Arrangement.spacedBy(tokenDp("spaceInset")),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = typeStyle("body"), color = colors.text, modifier = Modifier.weight(1f))
        AtsIcon(LucideIcon.CHEVRON_RIGHT, tint = colors.iconChevron, size = tokenDp("fieldMarkSize"))
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
    // 19: "Allocation >" (view 3, view 4).
    val onOpenAllocation: () -> Unit = {},
    // 27: "Transaction history >" (view 3, view 8).
    val onOpenHistory: () -> Unit = {},
    // 29: Delete in the menu (wireframe 10d); the dialog and the blocked message are AppContent's (step 10).
    val onDelete: () -> Unit = {},
)

/**
 * Wireframe 3 marker 4, drawn from its model (CR2, 16a decision 1), as the summary card (drawing 03 frame B, note 3;
 * D-38): "Portfolio value" with the 32 figure, Cost and the unrealized gain/loss as 16 lines, the latter in the summary
 * card's own gain or loss color with its sign, with "(all holdings at cost)" while no holding with shares has a price
 * (09 wording 4). Then the 1-day line, the price age line, the oldest price used with ", some stale" when any is (16b
 * decision 1), and "Updating prices" and the refresh-failed line (section 6; AC-42). Each line is read on its own.
 */
@Composable
private fun SummaryCard(
    figures: PortfolioFiguresModel,
    refresh: PriceRefreshModel,
) {
    val colors = AtsTheme.colors
    AtsCard(kind = CardKind.SUMMARY) {
        val valueLabel = stringResource(R.string.portfolio_value_label)
        val valueSpoken = stringResource(R.string.spoken_label_dollars, valueLabel, figures.spokenValue)
        Column(modifier = Modifier.clearAndSetSemantics { contentDescription = valueSpoken }) {
            Text(text = valueLabel, style = typeStyle("small"), color = colors.onSummaryMuted)
            Text(text = figures.value, style = typeStyle("bigFigure"), color = colors.onSummary)
        }
        SummaryLine(stringResource(R.string.portfolio_cost_label), figures.cost, figures.spokenCost)
        SummaryLine(
            label = stringResource(R.string.portfolio_unrealized_label),
            // CR6 (43): with its percentage, read in full.
            amount = gainFigures(figures.gain),
            spokenAmount = spoken(figures.gain.spoken),
            spokenPercent = figures.gain.percent?.let { spoken(it.spoken) },
            atCost = figures.allAtCost,
            amountColor = summaryGainColor(sign(figures.gain)),
        )
        // CR5 (41), wireframe 03 marker 4: the 1-day line, indented and smaller, under the gain/loss.
        figures.day?.let { DaySummaryLine(it, onSummary = true) }
        figures.prices?.let { PricesAsOfLine(it) }
        PriceRefreshLines(refresh.refreshing, refresh.failed, refresh.noKey, onSummary = true)
    }
}

/** A figure's color on the summary card: its own gain and loss colors, or its text color at $0.00 (D-38). */
@Composable
internal fun summaryGainColor(signum: Int): Color {
    val colors = AtsTheme.colors
    return when {
        signum > 0 -> colors.gainOnSummary
        signum < 0 -> colors.lossOnSummary
        else -> colors.onSummary
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
        style = typeStyle("oneDay"),
        color = AtsTheme.colors.onSummaryMuted,
        modifier = Modifier.clearAndSetSemantics { contentDescription = spoken },
    )
}

/** A 16 line of the summary card: its label on the left in the card's label color, its figure on the right. */
@Composable
private fun SummaryLine(
    label: String,
    amount: String,
    spokenAmount: String,
    atCost: Boolean = false,
    amountColor: Color = AtsTheme.colors.onSummary,
    spokenPercent: String? = null,
) {
    val colors = AtsTheme.colors
    val base =
        spokenPercent?.let { stringResource(R.string.spoken_label_dollars_percent, label, spokenAmount, it) }
            ?: stringResource(R.string.spoken_label_dollars, label, spokenAmount)
    val spoken = if (atCost) stringResource(R.string.portfolio_row_at_cost_spoken, base) else base
    Column(modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = spoken }) {
        // CR6 (43): the figure stays at the right edge, beside the label or under it (C26).
        StartEndRow(
            modifier = Modifier.fillMaxWidth(),
            start = { Text(text = label, style = typeStyle("body"), color = colors.onSummaryMuted) },
            end = { Text(text = amount, style = typeStyle("body"), color = amountColor, textAlign = TextAlign.End) },
        )
        if (atCost) {
            Text(
                text = stringResource(R.string.holdings_at_cost),
                style = typeStyle("small"),
                color = colors.onSummaryMuted,
                modifier = Modifier.align(Alignment.End),
            )
        }
    }
}

/**
 * A holding (wireframe 3 marker 5, format of marker 6), drawn from its model (CR2, 16a decision 1): the ticker in
 * medium 16 and the shares in secondary 14 under it, the value in 16 and the gain/loss in 14 in green or red with its
 * sign at its latest price, or "no price, at cost" (4.8); a sold-out holding shows 0 shares and $0.00 with no flag (11
 * wording 4). A stale price shows "stale" with its own time in condensed 12 under the gain (wireframe 3 marker 6; 16b).
 * Then a chevron (drawing 03 frame B, note 5). One tap target opening Holding detail (iteration 10), read by TalkBack as
 * one button.
 */
@Composable
private fun HoldingRow(
    holding: PortfolioHoldingRowModel,
    onOpen: () -> Unit,
) {
    val colors = AtsTheme.colors
    val shares = stringResource(if (holding.oneShare) R.string.holding_share_one else R.string.holding_shares, holding.shares)
    val soldOut = holding.soldOut
    val gain = holding.gain
    val staleAsOf = holding.staleAsOf
    val spoken =
        when {
            soldOut -> stringResource(R.string.holding_row_sold_out_spoken, holding.ticker, shares, holding.spokenValue)

            gain is GainModel.Figure && staleAsOf != null ->
                stringResource(R.string.holding_row_gain_stale_spoken, holding.ticker, shares, holding.spokenValue, gainSpoken(gain), staleAsOf)

            gain is GainModel.Figure -> stringResource(R.string.holding_row_gain_spoken, holding.ticker, shares, holding.spokenValue, gainSpoken(gain))

            else -> stringResource(R.string.holding_row_spoken, holding.ticker, shares, holding.spokenValue)
        }.let { daySpoken(it, holding.day) }
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
        // CR6 (43): the value and figures stay at the right edge, beside the ticker or under it (C26).
        StartEndRow(
            modifier = Modifier.weight(1f),
            start = {
                Column {
                    Text(text = holding.ticker, style = typeStyle("sectionHeading"), color = colors.text)
                    Text(text = shares, style = typeStyle("small"), color = colors.textSecondary)
                }
            },
            end = { HoldingFigures(holding) },
        )
        AtsIcon(LucideIcon.CHEVRON_RIGHT, tint = colors.iconChevron, size = tokenDp("fieldMarkSize"))
    }
}

/** The right part of a holding: its value, its gain/loss or "no price, at cost", the 1-day line, "stale, as of". */
@Composable
private fun HoldingFigures(holding: PortfolioHoldingRowModel) {
    val colors = AtsTheme.colors
    val gain = holding.gain
    Column(horizontalAlignment = Alignment.End) {
        Text(text = holding.value, style = typeStyle("body"), color = colors.text, textAlign = TextAlign.End)
        if (!holding.soldOut) {
            when (gain) {
                is GainModel.Figure ->
                    Text(
                        text = gainFigures(gain),
                        style = typeStyle("small"),
                        color = gainLossColor(sign(gain)),
                        textAlign = TextAlign.End,
                    )

                GainModel.NoPrice ->
                    Text(
                        text = stringResource(R.string.holding_no_price),
                        style = typeStyle("small"),
                        color = colors.textSecondary,
                        textAlign = TextAlign.End,
                    )
            }
        }
        // CR5 (41), wireframe 03 marker 5: the 1-day line under the gain/loss.
        holding.day?.let { DayRowText(it) }
        holding.staleAsOf?.let {
            Text(
                text = stringResource(R.string.holding_row_stale, it),
                style = typeStyle("oneDay"),
                color = colors.textSecondary,
                textAlign = TextAlign.End,
            )
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
