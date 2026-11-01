package app.ghghorg.atsassets.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.LotChoice
import app.ghghorg.atsassets.core.LotUnavailable
import app.ghghorg.atsassets.core.Money
import app.ghghorg.atsassets.core.Quantity
import app.ghghorg.atsassets.core.QuantityError
import app.ghghorg.atsassets.core.QuantityResult
import app.ghghorg.atsassets.core.SellRefusal
import app.ghghorg.atsassets.ui.theme.gainLossColor
import java.math.BigDecimal

/** The sell entry's callbacks, passed together. */
class SellActions(
    val onPortfolioChange: (String) -> Unit,
    val onTickerChange: (String) -> Unit,
    val onDateChange: (String) -> Unit,
    val onQuantityChange: (String) -> Unit,
    val onTotalChange: (String) -> Unit,
    val onToggleLot: (String) -> Unit,
    val onPickChange: (String, String) -> Unit,
    val onSellAll: () -> Unit,
    val onReview: () -> Unit,
    val onBack: () -> Unit,
)

/**
 * Sell entry (wireframe 10a frames C and D; iteration 11): one scrolling screen with the portfolio and ticker chips
 * (decision 1), the date as digits, quantity and total (zero allowed), the price per share, Sell all, then the lot list
 * oldest first, the "Picked X of Y shares" line and Review. The whole screen scrolls as one, so nothing is hidden at
 * the largest font (criterion 39).
 */
@Composable
fun SellEntryScreen(
    state: AppState,
    actions: SellActions,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = !state.saving, onBack = actions.onBack)
    val form = state.sellForm
    val refusals = state.sellRefusals
    Column(
        modifier = modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TitleRow(stringResource(R.string.sell_button), actions.onBack)
        PortfolioChips(state.portfolios, form.portfolioId, actions.onPortfolioChange)
        TickerChips(state, actions.onTickerChange)
        OutlinedTextField(
            value = form.dateText,
            onValueChange = actions.onDateChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(R.string.date_label)) },
            supportingText = {
                Column {
                    dateMessage(refusals.date)?.let { Text(text = it) }
                    Text(text = stringResource(R.string.date_note))
                }
            },
            isError = refusals.date != null,
            visualTransformation = DateDigitsTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
        )
        NumberField(
            value = form.quantityText,
            onChange = actions.onQuantityChange,
            label = stringResource(R.string.buy_quantity_label),
            error = quantityMessage(refusals.quantity),
        )
        NumberField(
            value = form.totalText,
            onChange = actions.onTotalChange,
            label = stringResource(R.string.sell_total_label),
            error = totalMessage(refusals.total),
        )
        state.sellPricePerShare?.let { price ->
            val shown = Money.formatPerShare(price)
            val spoken = spokenLabel(stringResource(R.string.buy_price_per_share_label), shown)
            Text(
                text = stringResource(R.string.buy_price_per_share, shown),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.clearAndSetSemantics { contentDescription = spoken },
            )
        }
        val sellAllSpoken = stringResource(R.string.sell_all_spoken)
        OutlinedButton(
            onClick = actions.onSellAll,
            modifier = Modifier.semantics { contentDescription = sellAllSpoken },
        ) {
            Text(text = stringResource(R.string.sell_all_button))
        }
        LotList(state, actions)
        val messages = listOfNotNull(saveMessage(refusals.save), sellRuleMessage(state, refusals.rule))
        if (refusals.tickerMissing) Message(stringResource(R.string.sell_choose_ticker))
        messages.forEach { Message(it) }
        Button(
            onClick = actions.onReview,
            enabled = !state.saving,
            modifier = Modifier.align(Alignment.End),
        ) {
            Text(text = stringResource(R.string.review_button))
        }
    }
}

/** One chip per ticker held in the chosen portfolio, in ticker order (11 decision 1); a line when there is none. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TickerChips(
    state: AppState,
    onChange: (String) -> Unit,
) {
    Column {
        Text(
            text = stringResource(R.string.sell_ticker_label),
            style = MaterialTheme.typography.bodyMedium,
        )
        val tickers = state.sellTickers
        if (tickers.isEmpty()) {
            val name = state.portfolios.firstOrNull { it.id == state.sellForm.portfolioId }?.name.orEmpty()
            Text(
                text = stringResource(R.string.sell_nothing_held_in, name),
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            FlowRow(
                modifier = Modifier.selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (ticker in tickers) {
                    FilterChip(
                        selected = ticker == state.sellForm.ticker,
                        onClick = { onChange(ticker) },
                        label = { Text(text = ticker) },
                    )
                }
            }
        }
    }
}

/**
 * The lot list (wireframe 10a markers 10 to 13): "Lots, oldest first", one row per lot, then "Picked X of Y shares"
 * and, after a date change dropped picks, the line saying so.
 */
@Composable
private fun LotList(
    state: AppState,
    actions: SellActions,
) {
    val choices = state.sellLotChoices
    Text(
        text = stringResource(R.string.sell_lots_heading),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { heading() },
    )
    if (choices.isNotEmpty()) {
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                choices.forEachIndexed { index, choice ->
                    if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    if (choice.unavailable == null) {
                        LotPickRow(
                            choice = choice,
                            pick = state.sellForm.picks[choice.buyId],
                            error = state.sellRefusals.picks[choice.buyId],
                            actions = actions,
                        )
                    } else {
                        UnavailableLotRow(choice)
                    }
                }
            }
        }
    }
    val quantity = (Quantity.parse(state.sellForm.quantityText) as? QuantityResult.Exact)?.value
    Text(
        text =
        if (quantity == null) {
            stringResource(R.string.sell_picked, sharesText(state.sellPicked))
        } else {
            stringResource(R.string.sell_picked_of, Quantity.format(state.sellPicked), sharesText(quantity))
        },
        style = MaterialTheme.typography.bodyLarge,
    )
    if (state.sellPicksDropped) {
        Text(
            text = stringResource(R.string.sell_picks_dropped),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

/**
 * A lot that passes the lot rules (11 wording 1): a tap on the left part picks or unpicks it; a picked lot shows
 * "Sell:" and its quantity field, prefilled with the full remainder. "no price" stands for the gain/loss until 16.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LotPickRow(
    choice: LotChoice,
    pick: String?,
    error: QuantityError?,
    actions: SellActions,
) {
    val date = choice.buyDate.toString()
    val left = Quantity.format(choice.available)
    val perShare = Money.formatPerShare(choice.costPerShare)
    val state = stringResource(if (pick != null) R.string.sell_lot_picked else R.string.sell_lot_not_picked)
    val spoken = stringResource(R.string.sell_lot_spoken, date, left, perShare.substringAfter('$'), state)
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier =
            Modifier
                .toggleable(value = pick != null, role = Role.Checkbox, onValueChange = { actions.onToggleLot(choice.buyId) })
                .clearAndSetSemantics { contentDescription = spoken },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = pick != null, onCheckedChange = null)
            LotText(date, left, perShare)
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End,
        ) {
            if (pick != null) {
                val fieldSpoken = stringResource(R.string.sell_pick_spoken, date)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = stringResource(R.string.sell_pick_label), style = MaterialTheme.typography.bodyLarge)
                    OutlinedTextField(
                        value = pick,
                        onValueChange = { actions.onPickChange(choice.buyId, it) },
                        modifier = Modifier.width(140.dp).padding(start = 8.dp).semantics { contentDescription = fieldSpoken },
                        isError = error != null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                    )
                }
                pickMessage(error)?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.End,
                    )
                }
            }
            Text(
                text = stringResource(R.string.holding_lot_no_price),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
            )
        }
    }
}

/**
 * A lot that fails the lot rules for the date (wireframe 10a marker 13): greyed out with a dashed outline, its reason in
 * text, and no way to pick it. It keeps its place in the order.
 */
@Composable
private fun UnavailableLotRow(choice: LotChoice) {
    val date = choice.buyDate.toString()
    val left = Quantity.format(choice.available)
    val perShare = Money.formatPerShare(choice.costPerShare)
    val reason =
        stringResource(
            if (choice.unavailable == LotUnavailable.BOUGHT_AFTER_SELL_DATE) R.string.sell_lot_bought_after else R.string.sell_lot_entered_after,
        )
    val spoken = stringResource(R.string.sell_lot_unavailable_spoken, date, left, perShare.substringAfter('$'), reason)
    val outline = MaterialTheme.colorScheme.outline
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    color = outline,
                    cornerRadius = CornerRadius(8.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 4.dp.toPx()))),
                )
            }.clearAndSetSemantics { contentDescription = spoken }
            .padding(8.dp),
    ) {
        Row(
            modifier = Modifier.alpha(0.6f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = false, onCheckedChange = null, enabled = false)
            LotText(date, left, perShare)
        }
        Text(text = reason, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun LotText(
    date: String,
    left: String,
    perShare: String,
) {
    Column {
        Text(text = stringResource(R.string.date_with_et, date), style = MaterialTheme.typography.bodyLarge)
        Text(
            text = stringResource(R.string.holding_lot_left, left, perShare),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A refusal: plain text with "Not saved", read by TalkBack when it appears (wireframe 10a marker 16). */
@Composable
internal fun Message(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

/**
 * Sell review (wireframe 10a frame E; 11 wording 3): the entered values and the price per share, then each lot used
 * with its shares and cost, and its proceeds and gain on a second line; the cost of lots used, the realized gain/loss
 * in color with its sign, and the cash effect with its sign and no color. Parts beside a total are shown by largest
 * remainder (4.10). Nothing is saved until Confirm and save (criterion 30).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SellReviewScreen(
    state: AppState,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = !state.saving, onBack = onBack)
    val review = state.sellReview ?: return
    val sell = review.transaction
    val pieces = state.sellPieces
    val portfolio = state.portfolios.firstOrNull { it.id == sell.portfolioId }?.name.orEmpty()
    val costs = Money.partsInCents(pieces.map { it.cost })
    val proceeds = Money.partsInCents(pieces.map { it.proceeds })
    val gains = Money.partsInCents(pieces.map { it.gain })
    val realized = gains.sum()
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TitleRow(stringResource(R.string.sell_review_title), onBack, enabled = !state.saving)
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ReviewRow(stringResource(R.string.buy_portfolio_label), portfolio)
                ReviewRow(stringResource(R.string.buy_ticker_label), sell.ticker)
                ReviewRow(stringResource(R.string.date_label), stringResource(R.string.date_with_et, sell.date.toString()))
                ReviewRow(stringResource(R.string.buy_quantity_label), Quantity.format(sell.quantity))
                val total = stringResource(R.string.buy_total_row)
                ReviewRow(total, Money.format(sell.totalCents), spoken = spokenLabel(total, Money.format(sell.totalCents)))
                state.sellPricePerShare?.let { price ->
                    val label = stringResource(R.string.buy_price_per_share_label)
                    val shown = Money.formatPerShare(price)
                    ReviewRow(label, shown, spoken = spokenLabel(label, shown))
                }
                Text(
                    text = stringResource(R.string.sell_lots_used),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                pieces.forEachIndexed { index, piece ->
                    PieceRows(piece.buyDate.toString(), piece.quantity, costs[index], proceeds[index], gains[index])
                }
                val costLabel = stringResource(R.string.sell_cost_of_lots)
                val costShown = Money.format(costs.sum())
                ReviewRow(costLabel, costShown, spoken = spokenLabel(costLabel, costShown))
                ReviewRow(
                    stringResource(R.string.sell_realized_label),
                    signedAmount(realized),
                    spoken = stringResource(R.string.sell_realized_spoken, spokenSigned(realized)),
                    color = gainLossColor(realized.compareTo(0L)),
                )
                val effect = stringResource(R.string.cash_effect_label)
                ReviewRow(effect, signedAmount(sell.totalCents), bold = true, spoken = "$effect, ${spokenSigned(sell.totalCents)} dollars")
            }
        }
        Text(
            text = stringResource(R.string.review_hint),
            style = MaterialTheme.typography.bodyMedium,
        )
        listOfNotNull(saveMessage(state.sellRefusals.save), sellRuleMessage(state, state.sellRefusals.rule)).forEach { Message(it) }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = onConfirm,
                enabled = !state.saving,
            ) {
                Text(text = stringResource(R.string.confirm_button))
            }
            TextButton(
                onClick = onBack,
                enabled = !state.saving,
            ) {
                Text(text = stringResource(R.string.back_to_edit_button))
            }
        }
    }
}

/** One lot used (11 wording 3, option A): the lot and its cost, then its proceeds and its gain in color with its sign. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PieceRows(
    date: String,
    quantity: Fraction,
    costCents: Long,
    proceedsCents: Long,
    gainCents: Long,
) {
    val shares = sharesText(quantity)
    val spoken =
        stringResource(
            R.string.sell_piece_spoken,
            date,
            shares,
            Money.format(costCents).substringAfter('$'),
            Money.format(proceedsCents).substringAfter('$'),
            spokenSigned(gainCents),
        )
    val gain = signedAmount(gainCents)
    val second = stringResource(R.string.sell_piece_proceeds, Money.format(proceedsCents), gain)
    val gainColor = gainLossColor(gainCents.compareTo(0L))
    Column(modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = spoken }) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.sell_piece_lot, stringResource(R.string.date_with_et, date), shares),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(R.string.sell_piece_cost, Money.format(costCents)),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.End,
            )
        }
        val start = second.lastIndexOf(gain)
        Text(
            text =
            buildAnnotatedString {
                append(second.substring(0, start))
                withStyle(SpanStyle(color = gainColor)) { append(gain) }
                append(second.substring(start + gain.length))
            },
            modifier = Modifier.padding(start = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun pickMessage(error: QuantityError?): String? = when (error) {
    null -> null
    QuantityError.EMPTY -> stringResource(R.string.pick_empty)
    QuantityError.BAD_FORMAT -> stringResource(R.string.pick_bad_format)
    QuantityError.TOO_MANY_DECIMALS -> stringResource(R.string.pick_too_many_decimals)
    QuantityError.ZERO -> stringResource(R.string.pick_zero)
}

/** K1's sell refusals (11 wording 2). The field reasons cannot reach here: the field rules catch them first. */
@Composable
private fun sellRuleMessage(
    state: AppState,
    refusal: SellRefusal?,
): String? {
    val form = state.sellForm
    val lotDate: (String) -> String = { id -> state.transactions.firstOrNull { it.id == id }?.date?.toString().orEmpty() }
    return when (refusal) {
        is SellRefusal.MoreThanHeld -> {
            val portfolio = state.portfolios.firstOrNull { it.id == form.portfolioId }?.name.orEmpty()
            stringResource(
                R.string.sell_more_than_held,
                portfolio,
                sharesText(refusal.held),
                form.ticker.orEmpty(),
                refusal.date.toString(),
                Quantity.format(refusal.quantity),
            )
        }

        is SellRefusal.PicksDoNotAddUp ->
            stringResource(R.string.sell_picks_do_not_add_up, sharesText(refusal.picked), Quantity.format(refusal.quantity))

        is SellRefusal.PickOverRemainder -> {
            val pick = form.picks[refusal.lotId]?.let { (Quantity.parse(it) as? QuantityResult.Exact)?.value } ?: BigDecimal.ZERO
            stringResource(R.string.sell_pick_over_remainder, lotDate(refusal.lotId), sharesText(refusal.remainder), Quantity.format(pick))
        }

        is SellRefusal.PickNotAvailable ->
            if (refusal.reason == LotUnavailable.BOUGHT_AFTER_SELL_DATE) {
                stringResource(R.string.sell_pick_bought_after, lotDate(refusal.lotId))
            } else {
                stringResource(R.string.sell_pick_entered_after, lotDate(refusal.lotId))
            }

        is SellRefusal.PickNotALot -> stringResource(R.string.sell_pick_not_a_lot)

        is SellRefusal.BeforeOpeningBalance -> stringResource(R.string.sell_before_opening_balance, refusal.openingDate.toString())

        is SellRefusal.NegativeCash -> stringResource(R.string.cash_negative, refusal.date.toString(), Money.format(refusal.shortfallCents))

        SellRefusal.PortfolioNotFound -> stringResource(R.string.buy_choose_portfolio)

        SellRefusal.TooLarge -> stringResource(R.string.cash_too_large)

        else -> null
    }
}
