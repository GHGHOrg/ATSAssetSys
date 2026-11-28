package app.ghghorg.atsassets.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.LotUnavailable
import app.ghghorg.atsassets.core.Money
import app.ghghorg.atsassets.core.Quantity
import app.ghghorg.atsassets.core.QuantityError
import app.ghghorg.atsassets.core.QuantityResult
import app.ghghorg.atsassets.core.SellRefusal
import app.ghghorg.atsassets.ui.design.AtsBlockedMessage
import app.ghghorg.atsassets.ui.design.AtsButton
import app.ghghorg.atsassets.ui.design.AtsCard
import app.ghghorg.atsassets.ui.design.AtsCheckBox
import app.ghghorg.atsassets.ui.design.AtsDenseField
import app.ghghorg.atsassets.ui.design.AtsEntryFoot
import app.ghghorg.atsassets.ui.design.AtsPickField
import app.ghghorg.atsassets.ui.design.AtsReviewCard
import app.ghghorg.atsassets.ui.design.AtsReviewSubheading
import app.ghghorg.atsassets.ui.design.AtsRowDivider
import app.ghghorg.atsassets.ui.design.AtsScreen
import app.ghghorg.atsassets.ui.design.ButtonSize
import app.ghghorg.atsassets.ui.theme.AtsTheme
import app.ghghorg.atsassets.ui.theme.gainLossColor
import app.ghghorg.atsassets.ui.theme.tokenDp
import app.ghghorg.atsassets.ui.theme.tokenNumber
import app.ghghorg.atsassets.ui.theme.typeStyle
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

// Which pick list is open over the sell screen.
private enum class SellPicking { NONE, PORTFOLIO, TICKER }

/**
 * Sell entry (wireframe 10a frames C and D; iteration 11), drawn from its model (CR2, open item 48; the lot rows from
 * theirs, 16c) in the design system (44a step 8 group (d) part 1; drawings 10a and 10c): one scrolling screen under its
 * band with a refusal as the blocked card at the top, the portfolio and the held ticker as standard pick fields side by
 * side (their lists inside the window), the date, quantity and total (zero allowed), the price per share with Sell all as a
 * small outline button, then the lot list oldest first, the "Picked X of Y shares" line and Review at the foot. The whole
 * screen scrolls as one, so nothing is hidden at the largest font (criterion 39). While a sell is replaced (14a), the
 * title says so, the banner names the original, a changed field shows the original under it, the lots are as after the
 * reversal, and refusals say nothing was changed (wireframe 10c frame C).
 */
@Composable
fun SellEntryScreen(
    state: AppState,
    actions: SellActions,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = !state.saving, onBack = actions.onBack)
    val model = state.sellEntry
    val form = state.sellForm
    val refusals = state.sellRefusals
    var picking by rememberSaveable { mutableStateOf(SellPicking.NONE) }
    val messages =
        listOfNotNull(if (refusals.tickerMissing) stringResource(R.string.sell_choose_ticker) else null) +
            if (model.replacing) {
                listOfNotNull(
                    saveMessage(refusals.save),
                    replacingMessage(sellRuleMessage(state, refusals.rule, replacing = true)),
                    replaceRuleMessage(state, state.replaceRefusals.rule),
                )
            } else {
                listOfNotNull(saveMessage(refusals.save), sellRuleMessage(state, refusals.rule))
            }
    Box(modifier = modifier.fillMaxSize()) {
        val under = if (picking != SellPicking.NONE) Modifier.clearAndSetSemantics { } else Modifier
        AtsScreen(
            title = stringResource(if (model.replacing) R.string.sell_replacing_title else R.string.sell_button),
            modifier = Modifier.fillMaxSize().then(under),
            onBack = actions.onBack,
            backLabel = stringResource(R.string.back_button),
            backEnabled = !state.saving,
        ) {
            EntryColumn {
                model.banner?.let { ReplacingBanner(it) }
                messages.forEach { Message(it) }
                Row(horizontalArrangement = Arrangement.spacedBy(tokenDp("spaceGap"))) {
                    PortfolioPick(model.portfolio, model.originals.portfolio, Modifier.weight(1f)) { picking = SellPicking.PORTFOLIO }
                    TickerPick(model, Modifier.weight(1f)) { picking = SellPicking.TICKER }
                }
                if (model.tickers.isEmpty()) {
                    Text(text = stringResource(R.string.sell_nothing_held_in, model.portfolio.orEmpty()), style = typeStyle("body"), color = AtsTheme.colors.text)
                }
                EntryDateField(form.dateText, dateMessage(refusals.date), actions.onDateChange, model.originals.date)
                QuantityAndTotal(
                    quantity = form.quantityText,
                    onQuantity = actions.onQuantityChange,
                    quantityError = quantityMessage(refusals.quantity),
                    total = form.totalText,
                    onTotal = actions.onTotalChange,
                    totalLabel = stringResource(R.string.sell_total_label),
                    totalError = totalMessage(refusals.total),
                    originals = model.originals,
                )
                // The price per share and Sell all, a small outline button, on one line (10a note 3).
                Row(modifier = Modifier.fillMaxWidth().heightIn(min = tokenDp("spaceTouch")), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(1f)) { model.pricePerShare?.let { PricePerShareLine(it) } }
                    val sellAllSpoken = stringResource(R.string.sell_all_spoken)
                    AtsButton(
                        stringResource(R.string.sell_all_button),
                        actions.onSellAll,
                        modifier = Modifier.semantics { contentDescription = sellAllSpoken },
                        size = ButtonSize.SMALL,
                    )
                }
                LotList(state, model.picked, actions)
                AtsEntryFoot(stringResource(R.string.review_button), actions.onReview, enabled = !state.saving)
            }
        }
        when (picking) {
            SellPicking.PORTFOLIO ->
                PortfolioChoice(model.portfolios, model.portfolioId, onChoose = { actions.onPortfolioChange(it) }, onClose = { picking = SellPicking.NONE })

            SellPicking.TICKER ->
                ChoiceLayer<String?>(
                    title = stringResource(R.string.sell_ticker_label),
                    options = model.tickers.map { it to it },
                    selected = model.ticker,
                    onChoose = { ticker ->
                        ticker?.let(actions.onTickerChange)
                        picking = SellPicking.NONE
                    },
                    onClose = { picking = SellPicking.NONE },
                )

            SellPicking.NONE -> Unit
        }
    }
}

/**
 * The held ticker as the standard pick field (wireframe 10a marker 8): read as "Ticker, AAPL"; a tap opens the tickers the
 * portfolio holds, in ticker order (11 decision 1). The original under it while replacing and it differs.
 */
@Composable
private fun TickerPick(
    model: SellEntryModel,
    modifier: Modifier,
    onOpen: () -> Unit,
) {
    Column(modifier = modifier) {
        val shown = model.ticker.orEmpty()
        AtsPickField(
            stringResource(R.string.sell_ticker_label),
            shown,
            onOpen,
            modifier = Modifier.fillMaxWidth(),
            enabled = model.tickers.isNotEmpty(),
            spoken = stringResource(R.string.history_ticker_spoken, shown),
        )
        OriginalLine(model.originals.ticker)
    }
}

/**
 * The lot list (wireframe 10a markers 10 to 14), drawn from its model (CR2, 16c decision 3): "Lots, oldest first" in
 * medium 16, the "updating" and refresh-failed lines once it shows prices (section 6, states (2) and (3); 16c), one row per
 * lot in a card, then "Picked X of Y shares" in the card and, after a date change dropped picks, the line saying so.
 */
@Composable
private fun LotList(
    state: AppState,
    picked: PickedLineModel,
    actions: SellActions,
) {
    val colors = AtsTheme.colors
    val rows = state.sellLotRows
    val replacing = state.replacingId != null
    Text(
        text = stringResource(if (replacing) R.string.replace_lots_heading else R.string.sell_lots_heading),
        style = typeStyle("sectionHeading"),
        color = colors.text,
        modifier = Modifier.semantics { heading() },
    )
    if (rows.isNotEmpty()) PriceRefreshLines(state.priceRefresh.refreshing, state.priceRefresh.failed, state.priceRefresh.noKey)
    AtsCard(padded = false) {
        rows.forEachIndexed { index, row ->
            if (row.unavailable == null) {
                if (index > 0) AtsRowDivider()
                LotPickRow(row, state.sellForm.picks[row.key], state.sellRefusals.picks[row.key], actions)
            } else {
                UnavailableLotRow(row)
            }
        }
        if (rows.isNotEmpty()) AtsRowDivider()
        PickedLine(picked)
    }
    if (state.sellPicksDropped) {
        Text(
            text = stringResource(R.string.sell_picks_dropped),
            style = typeStyle("small"),
            color = colors.text,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

/** "Picked 12 of 12 shares", with "(the original's picks)" while they are (10c marker 9). */
@Composable
private fun PickedLine(model: PickedLineModel) {
    val pickedShares = stringResource(if (model.picked.one) R.string.holding_share_one else R.string.holding_shares, model.picked.shown)
    val line =
        model.of?.let { of ->
            stringResource(R.string.sell_picked_of, model.picked.shown, stringResource(if (of.one) R.string.holding_share_one else R.string.holding_shares, of.shown))
        } ?: stringResource(R.string.sell_picked, pickedShares)
    Text(
        text = if (model.originals) line + " " + stringResource(R.string.replace_original_picks) else line,
        style = typeStyle("body"),
        color = AtsTheme.colors.text,
        modifier = Modifier.padding(tokenDp("spaceInset")),
    )
}

/**
 * A lot that passes the lot rules (11 wording 1; drawing 10a frame C): the catalog's checkbox, the date in 16 and "10
 * left, $100.00 per share" in secondary 14; a tap on that part picks or unpicks it. A picked lot shows "Sell:" and its
 * dense quantity field, prefilled with the full remainder. Its unrealized gain/loss in 14 in green or red with its sign,
 * at the latest price in today's shares (16c decision 4), with "stale, as of … ET" in condensed 12 under it, or "no price"
 * (wireframe 10a markers 12 and 14; 10a note 4). TalkBack reads them with the lot.
 */
@Composable
private fun LotPickRow(
    row: SellLotRowModel,
    pick: String?,
    error: QuantityError?,
    actions: SellActions,
) {
    val gain = row.gain
    val staleAsOf = row.staleAsOf
    val state = stringResource(if (pick != null) R.string.sell_lot_picked else R.string.sell_lot_not_picked)
    val spoken =
        when {
            gain is GainModel.Figure && staleAsOf != null ->
                stringResource(R.string.sell_lot_gain_stale_spoken, row.date, row.left, row.spokenPerShare, gainSpoken(gain), staleAsOf, state)

            gain is GainModel.Figure -> stringResource(R.string.sell_lot_gain_spoken, row.date, row.left, row.spokenPerShare, gainSpoken(gain), state)

            else -> stringResource(R.string.sell_lot_spoken, row.date, row.left, row.spokenPerShare, state)
        }
    // CR6 (43): the figures stay at the right edge, beside the lot or under it (C26).
    StartEndRow(
        modifier = Modifier.fillMaxWidth().padding(tokenDp("spaceInset")),
        lineGap = tokenDp("cardGap"),
        start = {
            Row(
                modifier =
                Modifier
                    .heightIn(min = tokenDp("spaceTouch"))
                    .toggleable(value = pick != null, role = Role.Checkbox, onValueChange = { actions.onToggleLot(row.key) })
                    .clearAndSetSemantics { contentDescription = spoken },
                horizontalArrangement = Arrangement.spacedBy(tokenDp("spaceInset")),
            ) {
                Box(modifier = Modifier.padding(top = tokenDp("cardGap"))) { AtsCheckBox(pick != null) }
                LotText(row.date, row.left, row.perShare)
            }
        },
        end = { LotPickFigures(row, pick, error, actions) },
    )
}

/** The right part of a lot that passes the lot rules: "Sell:" and its field when picked, the gain/loss, "stale, as of". */
@Composable
private fun LotPickFigures(
    row: SellLotRowModel,
    pick: String?,
    error: QuantityError?,
    actions: SellActions,
) {
    val colors = AtsTheme.colors
    val gain = row.gain
    Column(horizontalAlignment = Alignment.End) {
        if (pick != null) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(tokenDp("fieldDensePadding"))) {
                Box(modifier = Modifier.heightIn(min = tokenDp("spaceTouch")).clearAndSetSemantics { }, contentAlignment = Alignment.CenterStart) {
                    Text(text = stringResource(R.string.sell_pick_label), style = typeStyle("small"), color = colors.text)
                }
                AtsDenseField(
                    value = pick,
                    onValueChange = { actions.onPickChange(row.key, it) },
                    refusal = pickMessage(error),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    label = stringResource(R.string.sell_pick_spoken, row.date),
                )
            }
        }
        when (gain) {
            is GainModel.Figure ->
                Text(text = gainFigures(gain), style = typeStyle("small"), color = gainLossColor(sign(gain)), textAlign = TextAlign.End)

            else ->
                Text(text = stringResource(R.string.holding_lot_no_price), style = typeStyle("small"), color = colors.textSecondary, textAlign = TextAlign.End)
        }
        row.staleAsOf?.let {
            Text(text = stringResource(R.string.holding_row_stale, it), style = typeStyle("oneDay"), color = colors.textSecondary, textAlign = TextAlign.End)
        }
    }
}

/**
 * A lot that fails the lot rules for the date (wireframe 10a marker 13; D-18): it keeps its place in the order, inside a
 * dashed outline, faded to the unavailable lot's opacity, with its reason in text and no way to pick it.
 */
@Composable
private fun UnavailableLotRow(row: SellLotRowModel) {
    val colors = AtsTheme.colors
    val reason =
        stringResource(
            if (row.unavailable == LotUnavailable.BOUGHT_AFTER_SELL_DATE) R.string.sell_lot_bought_after else R.string.sell_lot_entered_after,
        )
    val spoken = stringResource(R.string.sell_lot_unavailable_spoken, row.date, row.left, row.spokenPerShare, reason)
    val outline = colors.textSecondary
    val radius = tokenDp("radiusDefault")
    val width = tokenDp("buttonOutline")
    val dash = tokenDp("chipPadding")
    val gap = tokenDp("cardGap")
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(tokenDp("spaceInset"))
            .drawBehind {
                drawRoundRect(
                    color = outline,
                    cornerRadius = CornerRadius(radius.toPx()),
                    style = Stroke(width = width.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash.toPx(), gap.toPx()))),
                )
            }.clearAndSetSemantics { contentDescription = spoken }
            .padding(tokenDp("spaceInset")),
    ) {
        // Faded to the unavailable lot's own opacity (4.4).
        Row(
            modifier = Modifier.alpha(tokenNumber("opacityUnavailableLot")),
            horizontalArrangement = Arrangement.spacedBy(tokenDp("spaceInset")),
        ) {
            Box(modifier = Modifier.padding(top = tokenDp("cardGap"))) { AtsCheckBox(false) }
            Column {
                LotText(row.date, row.left, row.perShare)
                Text(text = reason, style = typeStyle("small"), color = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun LotText(
    date: String,
    left: String,
    perShare: String,
) {
    Column {
        Text(text = stringResource(R.string.date_with_et, date), style = typeStyle("body"), color = AtsTheme.colors.text)
        Text(text = stringResource(R.string.holding_lot_left, left, perShare), style = typeStyle("small"), color = AtsTheme.colors.textSecondary)
    }
}

/**
 * A refusal that stops the save (wireframe 10a marker 16): the catalog's blocked card with "Not saved." (or "Not saved.
 * Nothing was changed." while replacing) in medium bronze, read by TalkBack when it appears (D-31 (8)).
 */
@Composable
internal fun Message(text: String) {
    val replacing = stringResource(R.string.replace_not_saved)
    val plain = stringResource(R.string.not_saved)
    AtsBlockedMessage(text, prefix = if (text.startsWith(replacing)) replacing else plain)
}

/**
 * Sell review (wireframe 10a frame E; 11 wording 3), drawn from its model (CR2, open item 48): the entered values and the
 * price per share, then "Lots used" as a sub-heading with each lot used, its shares and cost, and its proceeds and gain on
 * a second line; the cost of lots used, the realized gain/loss in color with its sign (the one colored figure), and the
 * cash effect as the total line with its sign and no color (10a note 6). Parts beside a total are shown by largest
 * remainder (4.10). Nothing is saved until Confirm and save (criterion 30).
 */
@Composable
fun SellReviewScreen(
    state: AppState,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = !state.saving, onBack = onBack)
    val model = state.sellReviewModel ?: return
    AtsScreen(
        title = stringResource(R.string.sell_review_title),
        modifier = modifier,
        onBack = onBack,
        backLabel = stringResource(R.string.back_button),
        backEnabled = !state.saving,
    ) {
        EntryColumn {
            listOfNotNull(saveMessage(state.sellRefusals.save), sellRuleMessage(state, state.sellRefusals.rule)).forEach { Message(it) }
            AtsReviewCard {
                ReviewRow(stringResource(R.string.buy_portfolio_label), model.portfolio)
                ReviewRow(stringResource(R.string.buy_ticker_label), model.ticker)
                ReviewRow(stringResource(R.string.date_label), stringResource(R.string.date_with_et, model.date))
                ReviewRow(stringResource(R.string.buy_quantity_label), model.quantity)
                val total = stringResource(R.string.buy_total_row)
                ReviewRow(total, model.total, spoken = spokenLabel(total, model.total))
                model.pricePerShare?.let { price ->
                    val label = stringResource(R.string.buy_price_per_share_label)
                    ReviewRow(label, price, spoken = spokenLabel(label, price))
                }
                AtsReviewSubheading(stringResource(R.string.sell_lots_used))
                model.pieces.forEach { SoldPieceRows(it) }
                val costLabel = stringResource(R.string.sell_cost_of_lots)
                ReviewRow(costLabel, model.cost, spoken = spokenLabel(costLabel, model.cost))
                ReviewRow(
                    stringResource(R.string.sell_realized_label),
                    signedAmount(model.realizedCents),
                    spoken = stringResource(R.string.sell_realized_spoken, spokenSigned(model.realizedCents)),
                    color = gainLossColor(model.realizedCents.compareTo(0L)),
                )
                CashEffectRow(model.cashCents)
            }
            ReviewHint()
            AtsEntryFoot(
                primary = stringResource(R.string.confirm_button),
                onPrimary = onConfirm,
                enabled = !state.saving,
                secondary = stringResource(R.string.back_to_edit_button),
                onSecondary = onBack,
            )
        }
    }
}

/** One lot used on the sell review, from its model (11 wording 3, option A). */
@Composable
private fun SoldPieceRows(piece: SoldPieceModel) {
    val shares = stringResource(if (piece.shares.one) R.string.holding_share_one else R.string.holding_shares, piece.shares.shown)
    PieceLines(piece.date, shares, piece.cost, piece.proceeds, piece.gainCents)
}

/** One lot used (11 wording 3, option A), for transaction detail: from the piece's exact figures, by largest remainder. */
@Composable
internal fun PieceRows(
    date: String,
    quantity: Fraction,
    costCents: Long,
    proceedsCents: Long,
    gainCents: Long,
) {
    PieceLines(date, sharesText(quantity), Money.format(costCents), Money.format(proceedsCents), gainCents)
}

/**
 * The lot and its cost as a label-and-value row, then its proceeds and its gain in color with its sign in secondary 14
 * under it (D-31 (4)); read by TalkBack as one sentence.
 */
@Composable
private fun PieceLines(
    date: String,
    shares: String,
    cost: String,
    proceeds: String,
    gainCents: Long,
) {
    val colors = AtsTheme.colors
    val spoken = stringResource(R.string.sell_piece_spoken, date, shares, cost.substringAfter('$'), proceeds.substringAfter('$'), spokenSigned(gainCents))
    val gain = signedAmount(gainCents)
    val second = stringResource(R.string.sell_piece_proceeds, proceeds, gain)
    val gainColor = gainLossColor(gainCents.compareTo(0L))
    Column(modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = spoken }) {
        StartEndRow(
            modifier = Modifier.fillMaxWidth(),
            start = { Text(text = stringResource(R.string.sell_piece_lot, stringResource(R.string.date_with_et, date), shares), style = typeStyle("body"), color = colors.textSecondary) },
            end = { Text(text = stringResource(R.string.sell_piece_cost, cost), style = typeStyle("body"), color = colors.text, textAlign = TextAlign.End) },
        )
        val start = second.lastIndexOf(gain)
        Text(
            text =
            buildAnnotatedString {
                append(second.substring(0, start))
                withStyle(SpanStyle(color = gainColor)) { append(gain) }
                append(second.substring(start + gain.length))
            },
            modifier = Modifier.padding(start = tokenDp("spaceMargin")),
            style = typeStyle("small"),
            color = colors.textSecondary,
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

/**
 * K1's sell refusals (11 wording 2). The field reasons cannot reach here: the field rules catch them first. While
 * [replacing], the cash rule names the replacement (frame F).
 */
@Composable
private fun sellRuleMessage(
    state: AppState,
    refusal: SellRefusal?,
    replacing: Boolean = false,
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

        is SellRefusal.NegativeCash ->
            stringResource(
                if (replacing) R.string.replace_cash_negative else R.string.cash_negative,
                refusal.date.toString(),
                Money.format(refusal.shortfallCents),
            )

        SellRefusal.PortfolioNotFound -> stringResource(R.string.buy_choose_portfolio)

        SellRefusal.TooLarge -> stringResource(R.string.cash_too_large)

        else -> null
    }
}
