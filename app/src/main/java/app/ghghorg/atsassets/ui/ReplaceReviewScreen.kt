package app.ghghorg.atsassets.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.core.AmountResult
import app.ghghorg.atsassets.core.CashEntryRefusal
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.Money
import app.ghghorg.atsassets.core.Quantity
import app.ghghorg.atsassets.core.QuantityResult
import app.ghghorg.atsassets.core.ReentryRefusal
import app.ghghorg.atsassets.core.ReversalRefusal
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import app.ghghorg.atsassets.ui.design.AtsButton
import app.ghghorg.atsassets.ui.design.AtsCard
import app.ghghorg.atsassets.ui.design.AtsEntryFoot
import app.ghghorg.atsassets.ui.design.AtsNoticeBanner
import app.ghghorg.atsassets.ui.design.AtsPartHeading
import app.ghghorg.atsassets.ui.design.AtsScreen
import app.ghghorg.atsassets.ui.design.AtsTextField
import app.ghghorg.atsassets.ui.design.ButtonSize
import app.ghghorg.atsassets.ui.theme.AtsTheme
import app.ghghorg.atsassets.ui.theme.LucideIcon
import app.ghghorg.atsassets.ui.theme.gainLossColor
import app.ghghorg.atsassets.ui.theme.tokenDp
import app.ghghorg.atsassets.ui.theme.typeStyle

/** The combined review's callbacks, passed together. */
class ReplaceReviewActions(
    val onChangeDate: () -> Unit,
    val onDateChange: (String) -> Unit,
    val onConfirm: () -> Unit,
    val onBack: () -> Unit,
)

/**
 * Reverse and re-enter's combined review (wireframe 10c frames B and D; iteration 14a): part 1, the reversal, with its
 * date and Change (decision 4) and what it does at that date; part 2, the new buy or sell as on its own review; the
 * combined cash effect, signed and without color. Confirm and save both is the only way to save. A refusal about the
 * reversal date shows under part 1 (decision 3); the whole screen scrolls as one (criterion 39). In the design system (44a
 * step 8 group (d) part 1; drawing spec/wireframes/design/10c-reverse-reenter.html, D-31, D-32): two cards each ending in
 * its cash effect, the combined cash effect a third card, a refusal as the blocked card, the foot's large buttons.
 */
@Composable
fun ReplaceReviewScreen(
    state: AppState,
    actions: ReplaceReviewActions,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = !state.saving, onBack = actions.onBack)
    // Drawn only from the review's model (CR2): the screen picks the approved string and puts the values in.
    val model = state.replaceReviewModel
    AtsScreen(
        title = stringResource(R.string.replace_review_title),
        modifier = modifier,
        onBack = actions.onBack,
        backLabel = stringResource(R.string.back_button),
        backEnabled = !state.saving,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(tokenDp("spaceMargin")),
            verticalArrangement = Arrangement.spacedBy(tokenDp("spaceGap")),
        ) {
            saveMessage(state.replaceRefusals.save)?.let { Message(it) }
            model?.let { review ->
                AtsCard {
                    when (val reverses = review.reverses) {
                        // A cash entry's reversal (14b; frame E): an Adjustment of the opposite amount with its fixed note.
                        is ReversedEntry.Cash ->
                            ReversalPart(state, cashTypeLabel(reverses.type), cashTypeLower(reverses.type), reverses.date, actions) {
                                review.partOne?.let { PartOneEffect(it) }
                            }

                        is ReversedEntry.Trade ->
                            ReversalPart(state, entryTypeLabel(reverses.type), transactionTypeLower(reverses.type), reverses.date, actions) {
                                review.partOne?.let { PartOneEffect(it) }
                            }
                    }
                }
                when (val part = review.partTwo) {
                    is ReplacementPartModel.Buy -> AtsCard { BuyReplacementPart(part) }
                    is ReplacementPartModel.Sell -> AtsCard { SellReplacementPart(part) }
                    is ReplacementPartModel.Split -> AtsCard { SplitReplacementPart(part) }
                    is ReplacementPartModel.Cash -> AtsCard { CashReplacementPart(part) }
                    null -> Unit
                }
            }
            // The combined cash effect, a third card with one total line, signed and uncolored (10c note 4).
            model?.combinedCents?.let { cents ->
                AtsCard {
                    ReviewRow(
                        stringResource(R.string.replace_combined_effect),
                        signedAmount(cents),
                        spoken = stringResource(R.string.replace_combined_effect_spoken, spokenSigned(cents)),
                        emphasis = true,
                    )
                }
            }
            Text(text = stringResource(R.string.replace_both_or_neither), style = typeStyle("small"), color = AtsTheme.colors.textSecondary)
            AtsEntryFoot(
                primary = stringResource(R.string.replace_confirm_button),
                onPrimary = actions.onConfirm,
                enabled = !state.saving,
                secondary = stringResource(R.string.back_to_edit_button),
                onSecondary = actions.onBack,
            )
        }
    }
}

/**
 * Part 1 (10c markers 4 and 5): what is reversed ([kind] and its date), the reversal date with Change, then [effect] at
 * that date and a refusal of this part. A date before the original shows at the date field.
 */
@Composable
private fun ReversalPart(
    state: AppState,
    kind: String,
    kindLower: String,
    date: String,
    actions: ReplaceReviewActions,
    effect: @Composable () -> Unit,
) {
    AtsPartHeading(stringResource(R.string.replace_part_reversal))
    ReviewRow(
        stringResource(R.string.reversal_reverses_label),
        stringResource(R.string.transaction_link, kind, date),
        spoken = stringResource(R.string.replace_reverses_spoken, kindLower, date),
    )
    val refusals = state.replaceRefusals
    val beforeOriginal =
        when (val rule = refusals.rule) {
            is ReentryRefusal.OfReversal -> (rule.reason as? ReversalRefusal.BeforeOriginal)?.originalDate
            is ReentryRefusal.OfCashReversal -> (rule.reason as? CashEntryRefusal.BeforeOriginal)?.originalDate
            else -> null
        }
    if (state.replaceDateEditing) {
        AtsTextField(
            value = state.replaceReversalDate,
            onValueChange = actions.onDateChange,
            label = stringResource(R.string.reversal_date_label),
            modifier = Modifier.fillMaxWidth(),
            refusal = dateMessage(refusals.date) ?: beforeOriginal?.let { stringResource(R.string.reversal_before_original, it.toString()) },
            note = stringResource(R.string.reversal_date_note, date),
            trailing = LucideIcon.CALENDAR,
            trailingLabel = stringResource(R.string.date_pick_open),
            onTrailing = datePickerOpener(state.replaceReversalDate, actions.onDateChange, dense = false),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            visualTransformation = DateDigitsTransformation,
        )
    } else {
        ReversalDateRow(state.replaceReversalDate, state.saving, actions.onChangeDate)
    }
    effect()
    if (beforeOriginal == null) replaceRuleMessage(state, refusals.rule)?.let { Message(it) }
}

/** The new entry's heading row: "Buy MA, Main" (or Sell, Split) with its date (10c marker 6). */
@Composable
private fun NewEntryRow(
    type: TransactionType,
    ticker: String,
    portfolio: String,
    date: String,
) {
    val kind = entryTypeLabel(type)
    ReviewRow(
        stringResource(R.string.replace_new_entry, kind, ticker, portfolio),
        stringResource(R.string.date_with_et, date),
        spoken = stringResource(R.string.replace_new_entry_spoken, kind, ticker, portfolio, date),
    )
}

/** Part 2 for a split (10c marker 11): the new split, its ratio, and the shares at its place before and after. */
@Composable
private fun SplitReplacementPart(part: ReplacementPartModel.Split) {
    AtsPartHeading(stringResource(R.string.replace_part_replacement))
    NewEntryRow(TransactionType.SPLIT, part.ticker, part.portfolio, part.date)
    part.ratio?.let { ReviewRow(stringResource(R.string.split_button), ratioText(it)) }
    if (part.before != null && part.after != null) {
        ReviewRow(stringResource(R.string.split_shares_at_point), part.before)
        ReviewRow(stringResource(R.string.split_shares_after), part.after, bold = true)
    }
}

/** Part 2 for a cash entry (frame E): its type, date and signed amount, its note, and its cash effect. */
@Composable
private fun CashReplacementPart(part: ReplacementPartModel.Cash) {
    AtsPartHeading(stringResource(R.string.replace_part_replacement))
    val kind = cashTypeLabel(part.type)
    ReviewRow(
        stringResource(R.string.transaction_link, kind, part.date),
        signedAmount(part.amountCents),
        spoken = stringResource(R.string.replace_new_cash_spoken, kind, part.date, spokenSigned(part.amountCents)),
    )
    if (part.note.isEmpty()) {
        ReviewRow(stringResource(R.string.note_row_label), stringResource(R.string.note_none), muted = true, alignStart = true)
    } else {
        ReviewRow(stringResource(R.string.note_row_label), part.note, alignStart = true)
    }
    CashEffectRow(part.amountCents)
}

/** "Reversal date 2026-01-02 ET" with Change, a small outline button, beside it; the row is 48 tall (10c note 5). */
@Composable
private fun ReversalDateRow(
    digits: String,
    saving: Boolean,
    onChange: () -> Unit,
) {
    val shown = DateDigits.toDate(digits)?.toString() ?: DateDigits.format(digits)
    val changeSpoken = stringResource(R.string.change_reversal_date_spoken)
    Row(modifier = Modifier.heightIn(min = tokenDp("spaceTouch")), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            ReviewRow(
                stringResource(R.string.reversal_date_label),
                stringResource(R.string.date_with_et, shown),
                spoken = stringResource(R.string.replace_reversal_date_spoken, shown),
            )
        }
        AtsButton(
            stringResource(R.string.change_button),
            onChange,
            modifier = Modifier.padding(start = tokenDp("fieldDensePadding")).semantics { contentDescription = changeSpoken },
            size = ButtonSize.SMALL,
            enabled = !saving,
        )
    }
}

/**
 * Part 1's effect (10c frames B, D, E): the shares removed and the cash for a buy; the lots put back, the gain removed
 * (uncolored, 10c note 6) and the cash for a sell; the ratio applied and the shares for a split, with no cash; the
 * Adjustment, its fixed note (14b decision 1) under its label, and the opposite amount for a cash entry.
 */
@Composable
private fun PartOneEffect(part: ReversalPartModel) {
    when (part) {
        is ReversalPartModel.OfBuy -> {
            ReviewRow(
                stringResource(R.string.replace_shares_removed),
                part.sharesRemoved,
                spoken = stringResource(R.string.reversal_shares_removed_spoken, part.sharesRemoved),
            )
            CashEffectRow(part.cashCents)
        }

        is ReversalPartModel.OfSell -> {
            ReviewRow(
                stringResource(R.string.replace_lots_put_back),
                joinedShares(part.lotsPutBack, " + "),
                spoken = stringResource(R.string.replace_lots_put_back_spoken, joinedShares(part.lotsPutBack, " plus ")),
            )
            ReviewRow(
                stringResource(R.string.replace_realized_removed),
                signedAmount(part.realizedRemovedCents),
                spoken = stringResource(R.string.replace_realized_removed_spoken, spokenSigned(part.realizedRemovedCents)),
            )
            CashEffectRow(part.cashCents)
        }

        is ReversalPartModel.OfSplit -> {
            ReviewRow(stringResource(R.string.reversal_ratio_applied), ratioText(part.inverse))
            ReviewRow(stringResource(R.string.split_shares_at_point), part.before)
            ReviewRow(stringResource(R.string.reversal_shares_after), part.after, bold = true)
            Text(text = stringResource(R.string.split_no_cash), style = typeStyle("small"), color = AtsTheme.colors.textSecondary)
        }

        is ReversalPartModel.OfCash -> {
            ReviewRow(stringResource(R.string.type_label), cashTypeLabel(CashType.ADJUSTMENT))
            ReviewRow(stringResource(R.string.note_row_label), part.note, alignStart = true)
            CashEffectRow(part.cashCents)
        }
    }
}

/** Part 2 for a buy (10c marker 6): as on its own review, with its exact shares and price (CR1), and its cash effect. */
@Composable
private fun BuyReplacementPart(part: ReplacementPartModel.Buy) {
    AtsPartHeading(stringResource(R.string.replace_part_replacement))
    NewEntryRow(TransactionType.BUY, part.ticker, part.portfolio, part.date)
    ReviewRow(stringResource(R.string.buy_quantity_label), part.quantity)
    val total = stringResource(R.string.buy_total_row)
    ReviewRow(total, part.total, spoken = spokenLabel(total, part.total))
    val price = stringResource(R.string.buy_price_per_share_label)
    ReviewRow(price, part.pricePerShare, spoken = spokenLabel(price, part.pricePerShare))
    CashEffectRow(part.cashCents)
}

/** Part 2 for a sell (10c marker 10): as on its own review, with the lots used, their cost and the realized gain. */
@Composable
private fun SellReplacementPart(part: ReplacementPartModel.Sell) {
    AtsPartHeading(stringResource(R.string.replace_part_replacement))
    NewEntryRow(TransactionType.SELL, part.ticker, part.portfolio, part.date)
    ReviewRow(
        stringResource(R.string.replace_quantity_total),
        stringResource(R.string.replace_quantity_total_value, part.quantity, part.total),
        spoken = stringResource(R.string.replace_quantity_total_spoken, part.quantity, part.total.substringAfter('$')),
    )
    ReviewRow(stringResource(R.string.sell_lots_used), joinedShares(part.lotsUsed, " + "))
    val costLabel = stringResource(R.string.sell_cost_of_lots)
    ReviewRow(costLabel, part.cost, spoken = spokenLabel(costLabel, part.cost))
    ReviewRow(
        stringResource(R.string.sell_realized_label),
        signedAmount(part.realizedCents),
        spoken = stringResource(R.string.sell_realized_spoken, spokenSigned(part.realizedCents)),
        color = gainLossColor(part.realizedCents.compareTo(0L)),
    )
    CashEffectRow(part.cashCents)
}

/** "Buy", "Sell" or "Split", capitalized, as in the review's rows. */
@Composable
private fun entryTypeLabel(type: TransactionType): String = stringResource(
    when (type) {
        TransactionType.SELL -> R.string.holding_transaction_sell
        TransactionType.SPLIT -> R.string.holding_transaction_split
        else -> R.string.holding_transaction_buy
    },
)

/**
 * "10 + 2 shares" (10c): the model's quantities joined by [separator], the last with the noun the model chose (15a
 * Deploy finding 1).
 */
@Composable
private fun joinedShares(
    shares: ShareList,
    separator: String,
): String {
    val firsts = shares.quantities.dropLast(1)
    val noun = stringResource(if (shares.oneShare) R.string.holding_share_one else R.string.holding_shares, shares.quantities.last())
    return (firsts + noun).joinToString(separator)
}

/**
 * The banner over a replacing buy or sell (10c marker 1), drawn from its model: what is replaced, and that nothing
 * changes until confirmed, as the catalog's notice card with Lucide's info (10c note 1). Read by TalkBack as one sentence
 * (wording 3).
 */
@Composable
internal fun ReplacingBanner(model: ReplacingBannerModel) {
    val type = transactionTypeLower(model.type)
    val spoken = stringResource(R.string.replacing_banner_spoken, type, model.date, model.quantity, model.ticker, model.total.substringAfter('$'))
    BannerCard(stringResource(R.string.replacing_banner, type, model.date, model.quantity, model.ticker, model.total), spoken)
}

/** The banner over a replacing split (14b wording 1): its date, ratio and ticker. */
@Composable
internal fun SplitReplacingBanner(original: Transaction) {
    val date = original.date.toString()
    val ratio = original.split?.let { ratioText(it) }.orEmpty()
    BannerCard(
        stringResource(R.string.replacing_banner_split, date, ratio, original.ticker),
        stringResource(R.string.replacing_banner_split_spoken, date, ratio, original.ticker),
    )
}

/** The banner over a replacing cash entry (14b wording 1): its type, date and signed amount, as on the Cash tab. */
@Composable
internal fun CashReplacingBanner(original: CashEntryShown) {
    val type = cashTypeLower(original.type)
    BannerCard(
        stringResource(R.string.replacing_banner_cash, type, original.date, signedAmount(original.amountCents)),
        stringResource(R.string.replacing_banner_cash_spoken, type, original.date, spokenSigned(original.amountCents)),
    )
}

@Composable
private fun BannerCard(
    text: String,
    spoken: String,
) {
    AtsNoticeBanner(text, modifier = Modifier.clearAndSetSemantics { contentDescription = spoken })
}

/** "Original: 10" under a changed field, as the field's note line (10c note 2); [spoken] is what TalkBack reads (wording 3). */
@Composable
internal fun OriginalHint(
    shown: String,
    spoken: String,
) {
    Text(
        text = stringResource(R.string.original_hint, shown),
        style = typeStyle("note"),
        color = AtsTheme.colors.textSecondary,
        modifier = Modifier.padding(horizontal = tokenDp("fieldPadding")).clearAndSetSemantics { contentDescription = spoken },
    )
}

/** The model's "Original: …" under a changed field, when it differs (10c marker 2); nothing otherwise. */
@Composable
internal fun OriginalLine(value: OriginalValue?) {
    if (value == null) return
    val spoken =
        if (value.dollars) {
            stringResource(R.string.original_hint_dollars_spoken, value.shown.substringAfter('$'))
        } else {
            stringResource(R.string.original_hint_spoken, value.shown)
        }
    OriginalHint(value.shown, spoken)
}

/** "Original: Main" under the portfolio while it differs from the replaced entry's (Split; the cash form). */
@Composable
internal fun PortfolioHint(
    state: AppState,
    selectedId: String?,
) {
    val original = state.replacedTransaction ?: return
    if (selectedId == original.portfolioId) return
    val name = state.portfolios.firstOrNull { it.id == original.portfolioId }?.name.orEmpty()
    OriginalHint(name, stringResource(R.string.original_hint_spoken, name))
}

/** "Original: AAPL" while the ticker differs. */
@Composable
internal fun TickerHint(
    original: Transaction?,
    ticker: String?,
) {
    if (original == null || ticker == original.ticker) return
    OriginalHint(original.ticker, stringResource(R.string.original_hint_spoken, original.ticker))
}

/** "Original: 2026-01-02" while the date differs. */
@Composable
internal fun DateHint(
    original: Transaction?,
    digits: String,
) {
    if (original == null || DateDigits.toDate(digits) == original.date) return
    val shown = original.date.toString()
    OriginalHint(shown, stringResource(R.string.original_hint_spoken, shown))
}

/** "Original: 10" while the quantity is not the same number. */
@Composable
internal fun QuantityHint(
    original: Transaction?,
    text: String,
) {
    if (original == null) return
    val typed = (Quantity.parse(text) as? QuantityResult.Exact)?.value
    if (typed != null && typed.compareTo(Quantity.shown(original.boughtQuantity)) == 0) return
    val shown = Quantity.format(original.boughtQuantity)
    OriginalHint(shown, stringResource(R.string.original_hint_spoken, shown))
}

/** "Original: $1,000.00" while the total, read as [parse] reads it, is not the same number of cents. */
@Composable
internal fun TotalHint(
    original: Transaction?,
    text: String,
    parse: (String) -> AmountResult,
) {
    if (original == null || (parse(text) as? AmountResult.Cents)?.value == original.totalCents) return
    val shown = Money.format(original.totalCents)
    OriginalHint(shown, stringResource(R.string.original_hint_dollars_spoken, shown.substringAfter('$')))
}

/** "Not saved." becomes "Not saved. Nothing was changed." while replacing (wording 1); other text is left as it is. */
@Composable
internal fun replacingMessage(message: String?): String? {
    val plain = stringResource(R.string.not_saved)
    val replacing = stringResource(R.string.replace_not_saved)
    return message?.let { if (it.startsWith(plain) && !it.startsWith(replacing)) replacing + it.removePrefix(plain) else it }
}

/**
 * The reversal part's refusals (frame F; wording 2). The lot is the original buy's, whose reversal is refused. A date
 * before the original shows at the date field instead.
 */
@Composable
internal fun replaceRuleMessage(
    state: AppState,
    refusal: ReentryRefusal?,
): String? {
    if (refusal is ReentryRefusal.OfCashReversal) return cashReversalMessage(refusal.reason)
    if (refusal == ReentryRefusal.QuantityRatioTooLarge) return stringResource(R.string.replace_ratio_too_large)
    val reason = (refusal as? ReentryRefusal.OfReversal)?.reason ?: return null
    val lotDate = state.replacedTransaction?.date?.toString().orEmpty()
    return when (reason) {
        is ReversalRefusal.SellNotReversed ->
            stringResource(R.string.replace_sell_not_reversed, lotDate, heldShares(reason.held), reason.date.toString(), reason.sellDate.toString())

        is ReversalRefusal.TooEarly ->
            stringResource(
                R.string.replace_too_early,
                reason.reversalDate.toString(),
                reason.date.toString(),
                lotDate,
                heldShares(reason.held),
                reason.earliest.toString(),
            )

        is ReversalRefusal.NegativeCash -> stringResource(R.string.replace_reversal_cash, reason.date.toString(), Money.format(reason.shortfallCents))

        is ReversalRefusal.SellShort -> replacingMessage(stringResource(R.string.reversal_sell_short, reason.date.toString()))

        ReversalRefusal.TooLarge -> replacingMessage(stringResource(R.string.cash_too_large))

        ReversalRefusal.NotFound, ReversalRefusal.NotReversible -> stringResource(R.string.replace_not_reversible)

        is ReversalRefusal.BeforeOriginal -> stringResource(R.string.reversal_before_original, reason.originalDate.toString())
    }
}

/** A cash entry's reversal refused in the pair (14b; 14a wording 2): cash, an entry that cannot be reversed, the date. */
@Composable
private fun cashReversalMessage(reason: CashEntryRefusal): String? = when (reason) {
    is CashEntryRefusal.NegativeCash -> stringResource(R.string.replace_reversal_cash, reason.date.toString(), Money.format(reason.shortfallCents))
    is CashEntryRefusal.NotReversible, CashEntryRefusal.OriginalNotFound -> stringResource(R.string.replace_not_reversible)
    is CashEntryRefusal.BeforeOriginal -> stringResource(R.string.reversal_before_original, reason.originalDate.toString())
    else -> replacingMessage(cashMessage(reason))
}

/** "−10 shares": a lot's figure below 0 with a real minus sign and its noun (frame F). */
@Composable
private fun heldShares(held: Fraction): String = if (held.signum() < 0) "−" + sharesText(held.abs()) else sharesText(held)
