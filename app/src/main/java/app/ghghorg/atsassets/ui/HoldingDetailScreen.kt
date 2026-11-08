package app.ghghorg.atsassets.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.core.Holding
import app.ghghorg.atsassets.core.Reversals
import app.ghghorg.atsassets.core.SplitRatio
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import app.ghghorg.atsassets.ui.theme.gainLossColor

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
)

/**
 * Holding detail (view 6, wireframe 06 frames A and B; iteration 10): "<" and the ticker, a header that stays in place
 * (portfolio, value, shares, cost, the unrealized gain/loss at the latest price and the price line, 16a), Buy (10
 * decisions 2 and 3), then the Transactions and Lots tabs (decision 1) whose content scrolls. Pulling down refreshes
 * the prices (16a decision 2); the pull's own indicator shows only for a pull, the header's "updating" line for any
 * refresh (section 6: only the price part shows it).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HoldingDetailScreen(
    state: AppState,
    actions: HoldingActions,
    modifier: Modifier = Modifier,
) {
    val holding = state.shownHolding ?: return
    val header = state.holdingHeader ?: return
    BackHandler(onBack = actions.onBack)
    var pulled by remember { mutableStateOf(false) }
    LaunchedEffect(state.refreshing) { if (!state.refreshing) pulled = false }
    PullToRefreshBox(
        isRefreshing = pulled && state.refreshing,
        onRefresh = {
            pulled = true
            actions.onRefresh()
        },
        modifier = modifier.fillMaxSize(),
    ) {
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
    // The fixed part stays in place while it takes at most half the height (view 6); taller, as at the largest font,
    // the whole screen scrolls as one so the list is not hidden (criterion 39; defect found in 10 phone check 6a).
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val limit = constraints.maxHeight / 2
        var fixedHeight by remember { mutableIntStateOf(0) }
        val fixedPart: @Composable () -> Unit = {
            FixedPart(state, holding, header, actions, Modifier.onSizeChanged { fixedHeight = it.height })
        }
        val list: @Composable () -> Unit = { TabContent(state, actions.onOpenTransaction, Modifier.fillMaxWidth().padding(16.dp)) }
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

/** "<" and the ticker, the header, Buy and the tabs: the part that stays in place when it fits (view 6). */
@Composable
private fun FixedPart(
    state: AppState,
    holding: Holding,
    header: HoldingHeaderModel,
    actions: HoldingActions,
    modifier: Modifier,
) {
    val portfolio = header.portfolio
    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TitleRow(holding.ticker, actions.onBack)
            Header(header)
            RefreshState(header)
            val buySpoken = stringResource(R.string.holding_buy_spoken, holding.ticker, portfolio)
            val sellSpoken = stringResource(R.string.holding_sell_spoken, holding.ticker, portfolio)
            val splitSpoken = stringResource(R.string.holding_split_spoken, holding.ticker, portfolio)
            // Buy, Sell and Split (view 6, criterion 48; 12 wording 4); they wrap when there is no room.
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = actions.onBuy, modifier = Modifier.semantics { contentDescription = buySpoken }) {
                    Text(text = stringResource(R.string.buy_button))
                }
                OutlinedButton(onClick = actions.onSell, modifier = Modifier.semantics { contentDescription = sellSpoken }) {
                    Text(text = stringResource(R.string.sell_button))
                }
                OutlinedButton(onClick = actions.onSplit, modifier = Modifier.semantics { contentDescription = splitSpoken }) {
                    Text(text = stringResource(R.string.split_button))
                }
            }
            // Hide, only at exactly 0 shares and not while hidden (wireframe 06 marker 9; 15 decision 1, wording 1).
            if (state.hideOffered) {
                val hideSpoken = stringResource(R.string.hide_spoken, holding.ticker, portfolio)
                OutlinedButton(
                    onClick = actions.onHide,
                    enabled = !state.saving,
                    modifier = Modifier.semantics { contentDescription = hideSpoken },
                ) {
                    Text(text = stringResource(R.string.hide_button))
                }
            }
            if (state.hideRefusal == SaveRefusal.SAVE_FAILED) {
                Text(
                    text = stringResource(R.string.save_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            // Sell at 0 shares (11 decision 2, wording 4): read once, gone when the holding is left.
            if (state.buyNotice == BuyNotice.NOTHING_TO_SELL_IN_HOLDING) {
                Text(
                    text = stringResource(R.string.holding_nothing_to_sell, portfolio, holding.ticker),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }
        val tabs = listOf(HoldingTab.TRANSACTIONS, HoldingTab.LOTS)
        PrimaryTabRow(selectedTabIndex = tabs.indexOf(state.holdingTab)) {
            tabs.forEach { tab ->
                Tab(
                    selected = tab == state.holdingTab,
                    onClick = { actions.onTab(tab) },
                    text = { Text(text = stringResource(tabLabel(tab))) },
                )
            }
        }
    }
}

/** The selected tab's rows. */
@Composable
private fun TabContent(
    state: AppState,
    onOpenTransaction: (String) -> Unit,
    modifier: Modifier,
) {
    Column(modifier = modifier) {
        when (state.holdingTab) {
            // Drawn only from the rows' models (CR2): the screen picks the approved string and puts the values in.
            HoldingTab.TRANSACTIONS ->
                RowList(state.holdingRows) {
                    val open = { onOpenTransaction(it.transactionId) }
                    when (it) {
                        is HoldingRowModel.Reversal -> ReversalRow(it, open)
                        is HoldingRowModel.Split -> SplitRow(it, open)
                        is HoldingRowModel.Trade -> TransactionRow(it, open)
                    }
                }

            HoldingTab.LOTS -> {
                if (state.holdingLots.isEmpty()) {
                    Text(text = stringResource(R.string.holding_no_lots), style = MaterialTheme.typography.bodyLarge)
                } else {
                    RowList(state.holdingLotRows) { LotRow(it) }
                }
            }
        }
    }
}

private fun tabLabel(tab: HoldingTab): Int = when (tab) {
    HoldingTab.TRANSACTIONS -> R.string.holding_tab_transactions
    HoldingTab.LOTS -> R.string.holding_tab_lots
}

/** "<" and the ticker; TalkBack reads "<" as Back. */
@Composable
private fun TitleRow(
    ticker: String,
    onBack: () -> Unit,
) {
    val back = stringResource(R.string.back_button)
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onBack, modifier = Modifier.semantics { contentDescription = back }) {
            Text(text = "<", style = MaterialTheme.typography.titleLarge)
        }
        Text(
            text = ticker,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
    }
}

/**
 * Wireframe 06 marker 1, drawn from its model (CR2, 16a): the portfolio, the value, Shares, Cost, the unrealized
 * gain/loss in green or red with its sign, or "no price, at cost" (4.8, criterion 48), and the price with its own time
 * and the stale flag (16a decision 4). Read by TalkBack as one sentence.
 */
@Composable
private fun Header(header: HoldingHeaderModel) {
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
                    spoken(gain.spoken),
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
                    spoken(gain.spoken),
                )

            else -> stringResource(R.string.holding_header_spoken, header.ticker, header.portfolio, header.spokenValue, shares, header.spokenCost)
        }
    OutlinedCard(modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = spoken }) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = header.portfolio,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = header.value, style = MaterialTheme.typography.headlineSmall)
            HeaderLine(stringResource(R.string.holding_shares_label), header.shares)
            HeaderLine(stringResource(R.string.portfolio_cost_label), header.cost)
            when (gain) {
                is GainModel.Figure -> HeaderLine(stringResource(R.string.portfolio_unrealized_label), gain.text, gainLossColor(sign(gain)))
                GainModel.NoPrice -> HeaderLine(stringResource(R.string.portfolio_unrealized_label), stringResource(R.string.holding_no_price))
            }
            if (price != null) {
                Text(
                    text =
                    stringResource(
                        if (price.stale) R.string.holding_price_line_stale else R.string.holding_price_line,
                        price.perShare,
                        price.asOf,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
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

/**
 * Under the header (section 6, error and loading states; AC-42): "Updating prices" while a refresh runs, and one quiet
 * line when the last refresh failed, with no pop-up. TalkBack reads each as it appears.
 */
@Composable
private fun RefreshState(header: HoldingHeaderModel) {
    if (header.refreshing) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        ) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Text(
                text = stringResource(R.string.prices_updating),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (header.refreshFailed) {
        Text(
            text = stringResource(R.string.price_refresh_failed),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeaderLine(
    label: String,
    value: String,
    valueColor: Color = Color.Unspecified,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = valueColor,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun <T> RowList(
    items: List<T>,
    row: @Composable (T) -> Unit,
) {
    items.forEachIndexed { index, item ->
        if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        row(item)
    }
}

/**
 * A transaction (wording 2, option B): one line, the date first, then "Buy" or "Sell" (11 wording 4), the shares as
 * typed and the amount, separated by space only. At the largest font the amount wraps below. Tapping opens the
 * transaction (13a wording 4).
 */
@OptIn(ExperimentalLayoutApi::class)
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
    FlowRow(
        modifier = Modifier.fillMaxWidth().opensWith(spoken, onOpen),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = stringResource(R.string.date_with_et, date), style = MaterialTheme.typography.bodyLarge)
        Text(
            text = stringResource(if (sell) R.string.holding_transaction_sell else R.string.holding_transaction_buy),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
        Text(text = shares, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = amount,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.End,
        )
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
 * A reversal (13b wording 5): one line, its date, then "Reversal of the buy of 2026-01-02 ET"; no amount, which shows
 * on the Cash tab and in its detail. It wraps at the largest font.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReversalRow(
    row: HoldingRowModel.Reversal,
    onOpen: () -> Unit,
) {
    val date = row.date
    val type = transactionTypeLower(row.originalType)
    val spoken = stringResource(R.string.holding_reversal_row_spoken, date, type, row.originalDate)
    FlowRow(
        modifier = Modifier.fillMaxWidth().opensWith(spoken, onOpen),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = stringResource(R.string.date_with_et, date), style = MaterialTheme.typography.bodyLarge)
        Text(
            text = stringResource(R.string.reversal_of_link, type, row.originalDate),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** A split (12 wording 4): one line, the date first, then "Split" and the ratio; no amount, as a split has no cash. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SplitRow(
    row: HoldingRowModel.Split,
    onOpen: () -> Unit,
) {
    val date = row.date
    val shown = ratioText(row.ratio)
    val spoken = stringResource(R.string.holding_transaction_split_spoken, date, shown)
    FlowRow(
        modifier = Modifier.fillMaxWidth().opensWith(spoken, onOpen),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = stringResource(R.string.date_with_et, date), style = MaterialTheme.typography.bodyLarge)
        Text(
            text = stringResource(R.string.holding_transaction_split),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
        Text(text = shown, style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * A lot (wireframe 06 frame B, wording 2), drawn from its model (CR2, 16a): the purchase date with "ET" and "10 left,
 * $150.00 per share", the lot cost and its unrealized gain/loss in green or red with its sign, "stale" beside it when
 * the price is stale, or "no price" (4.8).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LotRow(lot: LotRowModel) {
    val date = lot.date
    val left = lot.left
    val perShare = lot.perShare
    val cost = lot.cost
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
                    spoken(gain.spoken),
                )

            GainModel.NoPrice -> stringResource(R.string.holding_lot_spoken, date, left, lot.spokenPerShare, lot.spokenCost)
        }
    FlowRow(
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = spoken },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Column {
            Text(text = stringResource(R.string.date_with_et, date), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(R.string.holding_lot_left, left, perShare),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End,
        ) {
            Text(text = cost, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.End)
            when (gain) {
                is GainModel.Figure -> {
                    // The figure carries its color; "stale" beside it is plain text, so it never rests on color (section 6).
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = gain.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = gainLossColor(sign(gain)),
                            textAlign = TextAlign.End,
                        )
                        if (lot.stale) {
                            Text(
                                text = stringResource(R.string.price_stale),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                GainModel.NoPrice ->
                    Text(
                        text = stringResource(R.string.holding_lot_no_price),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                    )
            }
        }
    }
}
