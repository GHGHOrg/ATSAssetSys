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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.core.AmountResult
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.Money
import app.ghghorg.atsassets.core.Quantity
import app.ghghorg.atsassets.core.QuantityResult
import app.ghghorg.atsassets.core.ReentryRefusal
import app.ghghorg.atsassets.core.ReversalEffect
import app.ghghorg.atsassets.core.ReversalRefusal
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import app.ghghorg.atsassets.core.Transactions
import app.ghghorg.atsassets.ui.theme.gainLossColor

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
 * reversal date shows under part 1 (decision 3); the whole screen scrolls as one (criterion 39).
 */
@Composable
fun ReplaceReviewScreen(
    state: AppState,
    actions: ReplaceReviewActions,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = !state.saving, onBack = actions.onBack)
    val original = state.replacedTransaction ?: return
    Column(
        modifier = modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TitleRow(stringResource(R.string.replace_review_title), actions.onBack, enabled = !state.saving)
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ReversalPart(state, original, actions)
            }
        }
        state.replaceReview?.let { review ->
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ReplacementPart(state, review.replacement, review.replacementCashLine.amountCents)
                }
            }
        }
        state.replaceCombinedCents?.let { cents ->
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    ReviewRow(
                        stringResource(R.string.replace_combined_effect),
                        signedAmount(cents),
                        bold = true,
                        spoken = stringResource(R.string.replace_combined_effect_spoken, spokenSigned(cents)),
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.replace_both_or_neither),
            style = MaterialTheme.typography.bodyMedium,
        )
        saveMessage(state.replaceRefusals.save)?.let { Message(it) }
        ReplaceButtons(state.saving, actions)
    }
}

/** Part 1 (10c markers 4 and 5): what is reversed, the reversal date with Change, and the effect at that date. */
@Composable
private fun ReversalPart(
    state: AppState,
    original: Transaction,
    actions: ReplaceReviewActions,
) {
    PartHeading(stringResource(R.string.replace_part_reversal))
    val type = transactionTypeLower(original.type)
    val date = original.date.toString()
    ReviewRow(
        stringResource(R.string.reversal_reverses_label),
        stringResource(R.string.transaction_link, entryTypeLabel(original.type), date),
        spoken = stringResource(R.string.replace_reverses_spoken, type, date),
    )
    val refusals = state.replaceRefusals
    val beforeOriginal = (refusals.rule as? ReentryRefusal.OfReversal)?.reason as? ReversalRefusal.BeforeOriginal
    if (state.replaceDateEditing) {
        OutlinedTextField(
            value = state.replaceReversalDate,
            onValueChange = actions.onDateChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(R.string.reversal_date_label)) },
            supportingText = {
                Column {
                    dateMessage(refusals.date)?.let { Text(text = it) }
                    beforeOriginal?.let { Text(text = stringResource(R.string.reversal_before_original, it.originalDate.toString())) }
                    Text(text = stringResource(R.string.reversal_date_note, date))
                }
            },
            isError = refusals.date != null || beforeOriginal != null,
            visualTransformation = DateDigitsTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
        )
    } else {
        ReversalDateRow(state.replaceReversalDate, state.saving, actions.onChangeDate)
    }
    state.replaceReversalEffect?.let { PartOneEffect(it) }
    if (beforeOriginal == null) replaceRuleMessage(state, refusals.rule)?.let { Message(it) }
}

/** "Reversal date 2026-01-02 ET" with Change beside it (10c marker 5). */
@Composable
private fun ReversalDateRow(
    digits: String,
    saving: Boolean,
    onChange: () -> Unit,
) {
    val shown = DateDigits.toDate(digits)?.toString() ?: DateDigits.format(digits)
    val changeSpoken = stringResource(R.string.change_reversal_date_spoken)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            ReviewRow(
                stringResource(R.string.reversal_date_label),
                stringResource(R.string.date_with_et, shown),
                spoken = stringResource(R.string.replace_reversal_date_spoken, shown),
            )
        }
        TextButton(onClick = onChange, enabled = !saving, modifier = Modifier.semantics { contentDescription = changeSpoken }) {
            Text(text = stringResource(R.string.change_button))
        }
    }
}

/** The shares removed and the cash for a buy; the lots put back, the gain removed and the cash for a sell (10c frames B, D). */
@Composable
private fun PartOneEffect(effect: ReversalEffect) {
    when (effect) {
        is ReversalEffect.OfBuy -> {
            val removed = Quantity.format(effect.sharesRemoved)
            ReviewRow(
                stringResource(R.string.replace_shares_removed),
                removed,
                spoken = stringResource(R.string.reversal_shares_removed_spoken, removed),
            )
            CashEffectRow(effect.cashCents)
        }

        is ReversalEffect.OfSell -> {
            val quantities = effect.lots.map { it.quantity }
            ReviewRow(
                stringResource(R.string.replace_lots_put_back),
                joinedShares(quantities, " + "),
                spoken = stringResource(R.string.replace_lots_put_back_spoken, joinedShares(quantities, " plus ")),
            )
            val removed = -centsOf(effect.realizedRemoved)
            ReviewRow(
                stringResource(R.string.replace_realized_removed),
                signedAmount(removed),
                spoken = stringResource(R.string.replace_realized_removed_spoken, spokenSigned(removed)),
            )
            CashEffectRow(effect.cashCents)
        }

        is ReversalEffect.OfSplit -> EffectRows(effect)
    }
}

/** Part 2 (10c markers 6 and 10): the new buy or sell as on its own review, with its cash effect. */
@Composable
private fun ReplacementPart(
    state: AppState,
    replacement: Transaction,
    cashCents: Long,
) {
    PartHeading(stringResource(R.string.replace_part_replacement))
    val portfolio = state.portfolios.firstOrNull { it.id == replacement.portfolioId }?.name.orEmpty()
    val kind = entryTypeLabel(replacement.type)
    val date = replacement.date.toString()
    ReviewRow(
        stringResource(R.string.replace_new_entry, kind, replacement.ticker, portfolio),
        stringResource(R.string.date_with_et, date),
        spoken = stringResource(R.string.replace_new_entry_spoken, kind, replacement.ticker, portfolio, date),
    )
    val totalShown = Money.format(replacement.totalCents)
    if (replacement.type == TransactionType.SELL) {
        ReviewRow(
            stringResource(R.string.replace_quantity_total),
            stringResource(R.string.replace_quantity_total_value, Quantity.format(replacement.quantity), totalShown),
            spoken = stringResource(R.string.replace_quantity_total_spoken, Quantity.format(replacement.quantity), totalShown.substringAfter('$')),
        )
        val pieces = state.replacePieces
        ReviewRow(stringResource(R.string.sell_lots_used), joinedShares(pieces.map { it.quantity }, " + "))
        val costs = Money.partsInCents(pieces.map { it.cost })
        val realized = Money.partsInCents(pieces.map { it.gain }).sum()
        val costLabel = stringResource(R.string.sell_cost_of_lots)
        val costShown = Money.format(costs.sum())
        ReviewRow(costLabel, costShown, spoken = spokenLabel(costLabel, costShown))
        ReviewRow(
            stringResource(R.string.sell_realized_label),
            signedAmount(realized),
            spoken = stringResource(R.string.sell_realized_spoken, spokenSigned(realized)),
            color = gainLossColor(realized.compareTo(0L)),
        )
    } else {
        ReviewRow(stringResource(R.string.buy_quantity_label), Quantity.format(replacement.quantity))
        val total = stringResource(R.string.buy_total_row)
        ReviewRow(total, totalShown, spoken = spokenLabel(total, totalShown))
        val price = stringResource(R.string.buy_price_per_share_label)
        val priceShown = Money.formatPerShare(Transactions.pricePerShare(replacement.totalCents, replacement.quantity))
        ReviewRow(price, priceShown, spoken = spokenLabel(price, priceShown))
    }
    CashEffectRow(cashCents)
}

@Composable
private fun PartHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.semantics { heading() },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReplaceButtons(
    saving: Boolean,
    actions: ReplaceReviewActions,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(onClick = actions.onConfirm, enabled = !saving) {
            Text(text = stringResource(R.string.replace_confirm_button))
        }
        TextButton(onClick = actions.onBack, enabled = !saving) {
            Text(text = stringResource(R.string.back_to_edit_button))
        }
    }
}

/** "Buy" or "Sell", capitalized, as in the review's rows. */
@Composable
private fun entryTypeLabel(type: TransactionType): String = stringResource(
    if (type == TransactionType.SELL) R.string.holding_transaction_sell else R.string.holding_transaction_buy,
)

/** "10 + 2 shares" (10c): the quantities joined, with "share" or "shares" after the last as [sharesText] gives it. */
@Composable
private fun joinedShares(
    quantities: List<Fraction>,
    separator: String,
): String {
    val last = quantities.lastOrNull() ?: return sharesText(Fraction.ZERO)
    val firsts = quantities.dropLast(1).joinToString(separator) { Quantity.format(it) }
    val noun = if (quantities.size > 1) stringResource(R.string.holding_shares, Quantity.format(last)) else sharesText(last)
    return if (firsts.isEmpty()) noun else firsts + separator + noun
}

/**
 * The banner over a replacing buy or sell (10c marker 1): what is replaced, and that nothing changes until confirmed.
 * Read by TalkBack as one sentence (wording 3).
 */
@Composable
internal fun ReplacingBanner(original: Transaction) {
    val type = transactionTypeLower(original.type)
    val date = original.date.toString()
    val quantity = Quantity.format(original.quantity)
    val total = Money.format(original.totalCents)
    val spoken = stringResource(R.string.replacing_banner_spoken, type, date, quantity, original.ticker, total.substringAfter('$'))
    Card(
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = spoken },
        colors =
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Text(
            text = stringResource(R.string.replacing_banner, type, date, quantity, original.ticker, total),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(12.dp),
        )
    }
}

/** "Original: 10" under a changed field (10c marker 2); [spoken] is what TalkBack reads (wording 3). */
@Composable
internal fun OriginalHint(
    shown: String,
    spoken: String,
) {
    Text(
        text = stringResource(R.string.original_hint, shown),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.clearAndSetSemantics { contentDescription = spoken },
    )
}

/** "Original: Main" under the portfolio chips while the portfolio differs from the replaced entry's. */
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
    if (typed != null && typed.compareTo(original.quantity) == 0) return
    val shown = Quantity.format(original.quantity)
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

/** "−10 shares": a lot's figure below 0 with a real minus sign and its noun (frame F). */
@Composable
private fun heldShares(held: Fraction): String = if (held.signum() < 0) "−" + sharesText(held.abs()) else sharesText(held)
