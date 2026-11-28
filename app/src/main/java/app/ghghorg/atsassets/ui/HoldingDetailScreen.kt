package app.ghghorg.atsassets.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.core.ClassList
import app.ghghorg.atsassets.core.ClassOrigin
import app.ghghorg.atsassets.core.EffectiveClass
import app.ghghorg.atsassets.core.Holding
import app.ghghorg.atsassets.ui.design.AtsButton
import app.ghghorg.atsassets.ui.design.AtsCard
import app.ghghorg.atsassets.ui.design.AtsNoticeBanner
import app.ghghorg.atsassets.ui.design.AtsRowDivider
import app.ghghorg.atsassets.ui.design.AtsTabs
import app.ghghorg.atsassets.ui.design.AtsTitleBand
import app.ghghorg.atsassets.ui.design.ButtonKind
import app.ghghorg.atsassets.ui.design.ButtonSize
import app.ghghorg.atsassets.ui.design.CardKind
import app.ghghorg.atsassets.ui.theme.AtsIcon
import app.ghghorg.atsassets.ui.theme.AtsTheme
import app.ghghorg.atsassets.ui.theme.LucideIcon
import app.ghghorg.atsassets.ui.theme.gainLossColor
import app.ghghorg.atsassets.ui.theme.tokenDp
import app.ghghorg.atsassets.ui.theme.typeStyle

/** Holding detail's callbacks, passed together. */
class HoldingActions(
    val onBack: () -> Unit,
    val onTab: (HoldingTab) -> Unit,
    val onBuy: () -> Unit,
    val onSell: () -> Unit,
    val onSplit: () -> Unit,
    val onOpenTransaction: (String) -> Unit,
    val onHide: () -> Unit,
    // 16a: pull-to-refresh (decision 2).
    val onRefresh: () -> Unit = {},
    // 24: Edit and Clear override on a Classification row (decision 2).
    val onEditClass: (ClassList) -> Unit = {},
    val onClearClass: (ClassList) -> Unit = {},
)

/**
 * Holding detail (view 6, wireframe 06 frames A to C; iteration 10), in the design system (44a step 8 group (b); drawing
 * spec/wireframes/design/06-holding-detail.html, D-31, D-38 to D-42): the title band with Back and the ticker, the header
 * as the summary card (portfolio, value, shares, cost, the unrealized gain/loss at the latest price, the 1-day line and
 * the price line, 16a), Hide at 0 shares, Buy, Sell and Split (10 decisions 2 and 3), then the Lots, Transactions and
 * Classification tabs (decision 1; CR5) whose content scrolls. Pulling down refreshes the prices (16a decision 2); the
 * pull's own indicator shows only for a pull, the header's "updating" line for any refresh (section 6: only the price part
 * shows it).
 */
@Composable
fun HoldingDetailScreen(
    state: AppState,
    actions: HoldingActions,
    modifier: Modifier = Modifier,
) {
    val holding = state.shownHolding ?: return
    val header = state.holdingHeader ?: return
    BackHandler(onBack = actions.onBack)
    PricePullToRefresh(state.refreshing, actions.onRefresh, modifier.fillMaxSize().background(AtsTheme.colors.page)) {
        Content(state, holding, header, actions)
    }
}

@Composable
private fun Content(
    state: AppState,
    holding: Holding,
    header: HoldingHeaderModel,
    actions: HoldingActions,
) {
    // The fixed part, the band included, stays in place while it takes at most half the height (view 6); taller, as at
    // the largest font, the whole screen scrolls as one so the list is not hidden (criterion 39; defect found in 10 phone
    // check 6a).
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val limit = constraints.maxHeight / 2
        var fixedHeight by remember { mutableIntStateOf(0) }
        val fixedPart: @Composable () -> Unit = {
            FixedPart(state, holding, header, actions, Modifier.onSizeChanged { fixedHeight = it.height })
        }
        val list: @Composable () -> Unit = { TabContent(state, actions, Modifier.fillMaxWidth().padding(tokenDp("spaceMargin"))) }
        if (fixedHeight > limit) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                fixedPart()
                list()
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                fixedPart()
                Box(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    list()
                }
            }
        }
    }
}

/** The band, the header, Hide, Buy, Sell and Split, and the tabs: the part that stays in place when it fits (view 6). */
@Composable
private fun FixedPart(
    state: AppState,
    holding: Holding,
    header: HoldingHeaderModel,
    actions: HoldingActions,
    modifier: Modifier,
) {
    val colors = AtsTheme.colors
    val portfolio = header.portfolio
    Column(modifier = modifier.fillMaxWidth()) {
        AtsTitleBand(holding.ticker, onBack = actions.onBack, backLabel = stringResource(R.string.back_button))
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = tokenDp("spaceMargin"), end = tokenDp("spaceMargin"), top = tokenDp("spaceMargin")),
            verticalArrangement = Arrangement.spacedBy(tokenDp("spaceGap")),
        ) {
            Header(header)
            // Hide, only at exactly 0 shares and not while hidden (wireframe 06 marker 9; 15 decision 1, wording 1): an
            // outline button with Lucide's eye-off under the header (drawing 06 frame D, note 8).
            if (state.hideOffered) {
                val hideSpoken = stringResource(R.string.hide_spoken, holding.ticker, portfolio)
                AtsButton(
                    stringResource(R.string.hide_button),
                    actions.onHide,
                    modifier = Modifier.semantics { contentDescription = hideSpoken },
                    enabled = !state.saving,
                    icon = LucideIcon.EYE_OFF,
                )
            }
            if (state.hideRefusal == SaveRefusal.SAVE_FAILED) {
                Text(
                    text = stringResource(R.string.save_failed),
                    style = typeStyle("small"),
                    color = colors.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            // Buy, Sell and Split (view 6, criterion 48; 12 wording 4): three equal outline buttons in one row (drawing 06
            // note 2); at the largest font a name wraps inside its button.
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(tokenDp("spaceGap"))) {
                TradeButton(stringResource(R.string.buy_button), stringResource(R.string.holding_buy_spoken, holding.ticker, portfolio), actions.onBuy)
                TradeButton(stringResource(R.string.sell_button), stringResource(R.string.holding_sell_spoken, holding.ticker, portfolio), actions.onSell)
                TradeButton(stringResource(R.string.split_button), stringResource(R.string.holding_split_spoken, holding.ticker, portfolio), actions.onSplit)
            }
            // Sell at 0 shares (11 decision 2, wording 4): read once, gone when the holding is left.
            if (state.buyNotice == BuyNotice.NOTHING_TO_SELL_IN_HOLDING) {
                AtsNoticeBanner(
                    stringResource(R.string.holding_nothing_to_sell, portfolio, holding.ticker),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }
        // CR5 (41): Lots first, then Transactions and Classification (view 6, wireframe 06 marker 3); the catalog's tabs
        // inside a screen (drawing 06 note 3).
        val tabs = listOf(HoldingTab.LOTS, HoldingTab.TRANSACTIONS, HoldingTab.CLASSIFICATION)
        AtsTabs(
            labels = tabs.map { stringResource(tabLabel(it)) },
            selected = tabs.indexOf(state.holdingTab),
            onSelect = { actions.onTab(tabs[it]) },
            modifier = Modifier.padding(start = tokenDp("spaceMargin"), end = tokenDp("spaceMargin"), top = tokenDp("spaceGap")),
        )
    }
}

/** One of Buy, Sell and Split: an outline button of a third of the row, read by TalkBack as [spoken]. */
@Composable
private fun androidx.compose.foundation.layout.RowScope.TradeButton(
    text: String,
    spoken: String,
    onClick: () -> Unit,
) {
    AtsButton(text, onClick, modifier = Modifier.weight(1f).semantics { contentDescription = spoken }, fillWidth = true)
}

/** The selected tab's rows. */
@Composable
private fun TabContent(
    state: AppState,
    actions: HoldingActions,
    modifier: Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(tokenDp("spaceGap"))) {
        when (state.holdingTab) {
            // Drawn only from the rows' models (CR2): the screen picks the approved string and puts the values in.
            HoldingTab.TRANSACTIONS ->
                if (state.holdingRows.isNotEmpty()) {
                    RowCard(state.holdingRows) {
                        val open = { actions.onOpenTransaction(it.transactionId) }
                        when (it) {
                            is HoldingRowModel.Reversal -> ReversalRow(it, open)
                            is HoldingRowModel.Split -> SplitRow(it, open)
                            is HoldingRowModel.Trade -> TransactionRow(it, open)
                        }
                    }
                }

            HoldingTab.LOTS -> {
                if (state.holdingLots.isEmpty()) {
                    AtsCard {
                        Text(text = stringResource(R.string.holding_no_lots), style = typeStyle("body"), color = AtsTheme.colors.textSecondary)
                    }
                } else {
                    RowCard(state.holdingLotRows) { LotRow(it) }
                }
            }

            HoldingTab.CLASSIFICATION -> state.holdingClassification?.let { ClassificationTab(it, actions, enabled = !state.saving) }
        }
    }
}

private fun tabLabel(tab: HoldingTab): Int = when (tab) {
    HoldingTab.TRANSACTIONS -> R.string.holding_tab_transactions
    HoldingTab.LOTS -> R.string.holding_tab_lots
    HoldingTab.CLASSIFICATION -> R.string.holding_tab_classification
}

/**
 * The Classification tab (view 6, wireframe 06 frame C and marker 7; 23a), drawn from its model (CR2): a card with Sector
 * and Geography, each with its value and "Override" or "From the source" under it, or "Unclassified" (4.9, C14), and from
 * 24 its own Edit and, while it has an override, Clear override as small text buttons (decision 2; drawing 06 note 6); the
 * shared "Not saved" line after a failed clear; then "Split history" as a card of its own, every split of the holding,
 * newest first, a reversed one marked (C9; 23a's B), or one plain line (note 7).
 */
@Composable
private fun ClassificationTab(
    model: ClassificationModel,
    actions: HoldingActions,
    enabled: Boolean,
) {
    val colors = AtsTheme.colors
    AtsCard {
        ClassRow(
            name = stringResource(R.string.class_sector),
            effective = model.sector,
            editSpoken = stringResource(R.string.class_edit_sector_spoken),
            clearSpoken = stringResource(R.string.class_clear_sector_spoken),
            enabled = enabled,
            onEdit = { actions.onEditClass(ClassList.SECTOR) },
            onClear = { actions.onClearClass(ClassList.SECTOR) },
        )
        ClassRow(
            name = stringResource(R.string.class_geography),
            effective = model.geography,
            editSpoken = stringResource(R.string.class_edit_geography_spoken),
            clearSpoken = stringResource(R.string.class_clear_geography_spoken),
            enabled = enabled,
            onEdit = { actions.onEditClass(ClassList.GEOGRAPHY) },
            onClear = { actions.onClearClass(ClassList.GEOGRAPHY) },
        )
    }
    if (model.notSaved) {
        Text(
            text = stringResource(R.string.save_failed),
            style = typeStyle("small"),
            color = colors.error,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
    AtsCard {
        Text(
            text = stringResource(R.string.split_history),
            style = typeStyle("sectionHeading"),
            color = colors.text,
            modifier = Modifier.semantics { heading() },
        )
        if (model.splits.isEmpty()) {
            Text(text = stringResource(R.string.split_history_none), style = typeStyle("body"), color = colors.textSecondary)
        } else {
            model.splits.forEach { SplitHistoryRow(it) }
        }
    }
}

/**
 * "Sector" in secondary 16 on the left, its value in 16 on the right with its origin in condensed 12 under it, read as
 * one line ("Sector, Technology, from the source"; "Sector, Diversified, override"); then Edit, and Clear override only
 * while the row has an override (decision 2), each naming its list for TalkBack ("Edit sector").
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClassRow(
    name: String,
    effective: EffectiveClass,
    editSpoken: String,
    clearSpoken: String,
    enabled: Boolean,
    onEdit: () -> Unit,
    onClear: () -> Unit,
) {
    val colors = AtsTheme.colors
    val value = effective.value
    val overridden = effective.origin == ClassOrigin.OVERRIDE
    val spoken =
        when {
            value == null -> stringResource(R.string.class_row_unclassified_spoken, name)
            overridden -> stringResource(R.string.class_row_override_spoken, name, value)
            else -> stringResource(R.string.class_row_spoken, name, value)
        }
    Column(modifier = Modifier.fillMaxWidth()) {
        StartEndRow(
            modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = spoken },
            start = { Text(text = name, style = typeStyle("body"), color = colors.textSecondary) },
            end = {
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = value ?: stringResource(R.string.class_unclassified), style = typeStyle("body"), color = colors.text, textAlign = TextAlign.End)
                    if (value != null) {
                        Text(
                            text = stringResource(if (overridden) R.string.class_override else R.string.class_from_source),
                            style = typeStyle("oneDay"),
                            color = colors.textSecondary,
                            textAlign = TextAlign.End,
                        )
                    }
                }
            },
        )
        FlowRow {
            AtsButton(
                stringResource(R.string.class_edit),
                onEdit,
                modifier = Modifier.semantics { contentDescription = editSpoken },
                kind = ButtonKind.TEXT,
                size = ButtonSize.SMALL,
                enabled = enabled,
            )
            if (overridden) {
                AtsButton(
                    stringResource(R.string.class_clear_override),
                    onClear,
                    modifier = Modifier.semantics { contentDescription = clearSpoken },
                    kind = ButtonKind.TEXT,
                    size = ButtonSize.SMALL,
                    enabled = enabled,
                )
            }
        }
    }
}

/** One split: its date with "ET" on the left, its ratio on the right, "(reversed)" after it when reversed; read as one line. */
@Composable
private fun SplitHistoryRow(row: SplitHistoryRowModel) {
    val colors = AtsTheme.colors
    val shown = ratioText(row.ratio)
    val spoken =
        if (row.reversed) {
            stringResource(R.string.split_history_reversed_spoken, row.date, shown)
        } else {
            stringResource(R.string.holding_transaction_split_spoken, row.date, shown)
        }
    StartEndRow(
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = spoken },
        start = { Text(text = stringResource(R.string.date_with_et, row.date), style = typeStyle("body"), color = colors.textSecondary) },
        end = {
            Column(horizontalAlignment = Alignment.End) {
                Text(text = shown, style = typeStyle("body"), color = colors.text, textAlign = TextAlign.End)
                if (row.reversed) {
                    Text(text = stringResource(R.string.split_history_reversed), style = typeStyle("small"), color = colors.textSecondary, textAlign = TextAlign.End)
                }
            }
        },
    )
}

/**
 * Wireframe 06 marker 1, drawn from its model (CR2, 16a), as the summary card (drawing 06 note 1; D-38): the portfolio as
 * its label, the value in 32, Shares, Cost and the unrealized gain/loss in 16, the latter in the summary card's own gain
 * or loss color with its sign, or "no price, at cost" (4.8, criterion 48), the 1-day line, and the price with its own
 * time and the stale flag (16a decision 4). Read by TalkBack as one sentence; "Updating prices" and the refresh-failed
 * line under it are read on their own.
 */
@Composable
private fun Header(header: HoldingHeaderModel) {
    val colors = AtsTheme.colors
    val shares = stringResource(if (header.oneShare) R.string.holding_share_one else R.string.holding_shares, header.shares)
    val gain = header.gain
    val price = header.price
    val spoken =
        when {
            gain is GainModel.Figure && price != null ->
                stringResource(
                    R.string.holding_header_gain_spoken,
                    header.ticker,
                    header.portfolio,
                    header.spokenValue,
                    shares,
                    header.spokenCost,
                    gainSpoken(gain),
                    priceSpoken(price),
                )

            // Sold out with no price: plain zeros, no "no price" flag (11 wording 4).
            gain is GainModel.Figure ->
                stringResource(
                    R.string.holding_header_sold_out_spoken,
                    header.ticker,
                    header.portfolio,
                    header.spokenValue,
                    shares,
                    header.spokenCost,
                    gainSpoken(gain),
                )

            else -> stringResource(R.string.holding_header_spoken, header.ticker, header.portfolio, header.spokenValue, shares, header.spokenCost)
        }.let { daySpoken(it, header.day) }
    AtsCard(kind = CardKind.SUMMARY) {
        Column(
            modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = spoken },
            verticalArrangement = Arrangement.spacedBy(tokenDp("cardGap")),
        ) {
            Text(text = header.portfolio, style = typeStyle("small"), color = colors.onSummaryMuted)
            Text(text = header.value, style = typeStyle("bigFigure"), color = colors.onSummary)
            HeaderLine(stringResource(R.string.holding_shares_label), header.shares)
            HeaderLine(stringResource(R.string.portfolio_cost_label), header.cost)
            // CR6 (43): "+$80.00 (+6.67%)", the figures moving under the label where they do not fit (C26).
            when (gain) {
                is GainModel.Figure -> HeaderLine(stringResource(R.string.portfolio_unrealized_label), gainFigures(gain), summaryGainColor(sign(gain)))
                GainModel.NoPrice -> HeaderLine(stringResource(R.string.portfolio_unrealized_label), stringResource(R.string.holding_no_price))
            }
            // CR5 (41), wireframe 06 marker 1: the 1-day line, indented and smaller, under the gain/loss.
            header.day?.let { DayLineRow(it, indented = true, onSummary = true) }
            if (price != null) {
                Text(
                    text =
                    stringResource(
                        if (price.stale) R.string.holding_price_line_stale else R.string.holding_price_line,
                        price.perShare,
                        price.asOf,
                    ),
                    style = typeStyle("oneDay"),
                    color = colors.onSummaryMuted,
                )
            }
        }
        PriceRefreshLines(header.refreshing, header.refreshFailed, header.refreshNoKey, onSummary = true)
    }
}

/** The price for TalkBack, with "stale" when it is (16a). */
@Composable
private fun priceSpoken(price: PriceLineModel): String = stringResource(
    if (price.stale) R.string.holding_price_stale_spoken else R.string.holding_price_spoken,
    price.spokenPerShare,
    price.asOf,
)

/** The sign of a shown gain, for its color: green above 0, red below, none at $0.00 (section 6). */
internal fun sign(gain: GainModel.Figure): Int = when (gain.spoken.sign) {
    FigureSign.PLUS -> 1
    FigureSign.MINUS -> -1
    FigureSign.NONE -> 0
}

/** A 16 line of the header: the label in the summary card's label color, the figure at the right edge. */
@Composable
private fun HeaderLine(
    label: String,
    value: String,
    valueColor: Color = AtsTheme.colors.onSummary,
) {
    StartEndRow(
        modifier = Modifier.fillMaxWidth(),
        start = { Text(text = label, style = typeStyle("body"), color = AtsTheme.colors.onSummaryMuted) },
        end = { Text(text = value, style = typeStyle("body"), color = valueColor, textAlign = TextAlign.End) },
    )
}

/** [items] as the rows of one card, with dividers between them (D-08). */
@Composable
private fun <T> RowCard(
    items: List<T>,
    row: @Composable (T) -> Unit,
) {
    AtsCard(padded = false) {
        items.forEachIndexed { index, item ->
            if (index > 0) AtsRowDivider()
            row(item)
        }
    }
}

/**
 * A row that opens something (13a wording 4): tappable, read by TalkBack as one sentence, [spoken], with the button
 * role Android announces, as the Cash tab's entry rows since 05.
 */
internal fun Modifier.opensWith(
    spoken: String,
    onOpen: () -> Unit,
): Modifier = clickable(role = Role.Button, onClick = onOpen)
    .clearAndSetSemantics {
        contentDescription = spoken
        role = Role.Button
        onClick {
            onOpen()
            true
        }
    }

/**
 * A row of the Transactions tab (drawing 06 frame B, note 5; D-44): [title] in 16 (the type, then the date with "ET"),
 * [sub] in secondary 14 under it, [amount] in 16 on the right, uncolored, and a chevron, as it opens the transaction (13a
 * wording 4). At the largest font the amount moves under the title (C26).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OpeningRow(
    spoken: String,
    onOpen: () -> Unit,
    title: List<String>,
    sub: String?,
    amount: String?,
) {
    val colors = AtsTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = tokenDp("spaceTouch"))
            .opensWith(spoken, onOpen)
            .padding(tokenDp("spaceInset")),
        horizontalArrangement = Arrangement.spacedBy(tokenDp("spaceInset")),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StartEndRow(
            modifier = Modifier.weight(1f),
            start = {
                Column {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(tokenDp("chipGap"))) {
                        title.forEach { Text(text = it, style = typeStyle("body"), color = colors.text) }
                    }
                    sub?.let { Text(text = it, style = typeStyle("small"), color = colors.textSecondary) }
                }
            },
            end = { amount?.let { Text(text = it, style = typeStyle("body"), color = colors.text, textAlign = TextAlign.End) } },
        )
        AtsIcon(LucideIcon.CHEVRON_RIGHT, tint = colors.iconChevron, size = tokenDp("fieldMarkSize"))
    }
}

/** A buy or a sell (wording 2, option B; 11 wording 4): "Buy" or "Sell" and the date, the shares as typed, the amount. */
@Composable
private fun TransactionRow(
    row: HoldingRowModel.Trade,
    onOpen: () -> Unit,
) {
    val date = row.date
    val shares = stringResource(if (row.oneShare) R.string.holding_share_one else R.string.holding_shares, row.shares)
    val amount = row.amount
    val sell = row.sell
    val spoken =
        stringResource(
            if (sell) R.string.holding_transaction_sell_spoken else R.string.holding_transaction_spoken,
            date,
            shares,
            amount.substringAfter('$'),
        )
    OpeningRow(
        spoken = spoken,
        onOpen = onOpen,
        title =
        listOf(
            stringResource(if (sell) R.string.holding_transaction_sell else R.string.holding_transaction_buy),
            stringResource(R.string.date_with_et, date),
        ),
        sub = shares,
        amount = amount,
    )
}

/**
 * A reversal (13b wording 5): its date, then "Reversal of the buy of 2026-01-02 ET"; no amount, which shows on the Cash
 * tab and in its detail.
 */
@Composable
private fun ReversalRow(
    row: HoldingRowModel.Reversal,
    onOpen: () -> Unit,
) {
    val type = transactionTypeLower(row.originalType)
    val spoken = stringResource(R.string.holding_reversal_row_spoken, row.date, type, row.originalDate)
    OpeningRow(
        spoken = spoken,
        onOpen = onOpen,
        title = listOf(stringResource(R.string.date_with_et, row.date)),
        sub = stringResource(R.string.reversal_of_link, type, row.originalDate),
        amount = null,
    )
}

/** A split (12 wording 4): "Split" and the date, the ratio under it; no amount, as a split has no cash. */
@Composable
private fun SplitRow(
    row: HoldingRowModel.Split,
    onOpen: () -> Unit,
) {
    val shown = ratioText(row.ratio)
    val spoken = stringResource(R.string.holding_transaction_split_spoken, row.date, shown)
    OpeningRow(
        spoken = spoken,
        onOpen = onOpen,
        title = listOf(stringResource(R.string.holding_transaction_split), stringResource(R.string.date_with_et, row.date)),
        sub = shown,
        amount = null,
    )
}

/**
 * A lot (wireframe 06 frame A, marker 5; CR5), drawn from its model (CR2, 16a): the purchase date with "ET" in medium
 * 16, "10 left, $150.00 per share" and "Cost $1,200.00" in secondary 14 on the left; its current value in 16 on the
 * right, with its unrealized gain/loss in 14 in green or red with its sign under it, "stale" beside it when the price is
 * stale, or "no price" (4.8). No chevron: a lot opens nothing (drawing 06 note 4).
 */
@Composable
private fun LotRow(lot: LotRowModel) {
    val colors = AtsTheme.colors
    val date = lot.date
    val left = lot.left
    val gain = lot.gain
    val spoken =
        when (gain) {
            is GainModel.Figure ->
                stringResource(
                    if (lot.stale) R.string.holding_lot_gain_stale_spoken else R.string.holding_lot_gain_spoken,
                    date,
                    left,
                    lot.spokenPerShare,
                    lot.spokenCost,
                    lot.spokenValue,
                    gainSpoken(gain),
                )

            GainModel.NoPrice ->
                stringResource(R.string.holding_lot_spoken, date, left, lot.spokenPerShare, lot.spokenCost, lot.spokenValue)
        }
    // CR6 (43): the value and figures stay at the right edge, beside the lot or under it (C26).
    StartEndRow(
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = spoken }.padding(tokenDp("spaceInset")),
        start = {
            Column {
                Text(text = stringResource(R.string.date_with_et, date), style = typeStyle("sectionHeading"), color = colors.text)
                Text(text = stringResource(R.string.holding_lot_left, left, lot.perShare), style = typeStyle("small"), color = colors.textSecondary)
                Text(text = stringResource(R.string.holding_lot_cost, lot.cost), style = typeStyle("small"), color = colors.textSecondary)
            }
        },
        end = { LotFigures(lot) },
    )
}

/** The right part of a lot: its value, and its gain/loss with "stale" beside it, or "no price". */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LotFigures(lot: LotRowModel) {
    val colors = AtsTheme.colors
    val gain = lot.gain
    Column(horizontalAlignment = Alignment.End) {
        Text(text = lot.value, style = typeStyle("body"), color = colors.text, textAlign = TextAlign.End)
        when (gain) {
            is GainModel.Figure -> {
                // The figure carries its color; "stale" beside it is plain text, so it never rests on color (section 6).
                // CR6 (43): a flow, so with the percentage the figures and "stale" wrap rather than squeeze (C26).
                FlowRow(horizontalArrangement = Arrangement.spacedBy(tokenDp("chipGap"), Alignment.End)) {
                    Text(
                        text = gainFigures(gain),
                        style = typeStyle("small"),
                        color = gainLossColor(sign(gain)),
                        textAlign = TextAlign.End,
                    )
                    if (lot.stale) {
                        Text(text = stringResource(R.string.price_stale), style = typeStyle("small"), color = colors.textSecondary)
                    }
                }
            }

            GainModel.NoPrice ->
                Text(
                    text = stringResource(R.string.holding_lot_no_price),
                    style = typeStyle("small"),
                    color = colors.textSecondary,
                    textAlign = TextAlign.End,
                )
        }
    }
}
