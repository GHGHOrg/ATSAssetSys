package app.ghghorg.atsassets.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.input.KeyboardType
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.Money
import app.ghghorg.atsassets.core.Quantity
import app.ghghorg.atsassets.core.ReversalRefusal
import app.ghghorg.atsassets.core.TransactionType
import app.ghghorg.atsassets.ui.design.AtsEntryFoot
import app.ghghorg.atsassets.ui.design.AtsReviewCard
import app.ghghorg.atsassets.ui.design.AtsReviewSubheading
import app.ghghorg.atsassets.ui.design.AtsScreen
import app.ghghorg.atsassets.ui.design.AtsTextField
import app.ghghorg.atsassets.ui.theme.AtsTheme
import app.ghghorg.atsassets.ui.theme.LucideIcon
import app.ghghorg.atsassets.ui.theme.typeStyle

/** The reversal screen's callbacks, passed together. */
class ReversalActions(
    val onDateChange: (String) -> Unit,
    val onConfirm: () -> Unit,
    val onCancel: () -> Unit,
)

/**
 * The reversal of a buy, sell or split (wireframe 10b frame I; iteration 13b), drawn from its model (CR2, open item 48)
 * in the design system (44a step 8 group (d) part 3; D-31): under its band, a refusal as the blocked card at the top (frame
 * J), the original fixed in a card under "Reverses", the reversal date with its calendar and note, the effect at that date
 * in a card ending in its cash effect (13b wording 1 to 3), and Confirm and save with Cancel as a text button. One
 * screen, no separate review (section 6). A refusal keeps the screen open with the date.
 */
@Composable
fun TransactionReversalScreen(
    state: AppState,
    actions: ReversalActions,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = !state.saving, onBack = actions.onCancel)
    val model = state.reversalScreen ?: return
    val refusals = state.transactionReversalRefusals
    AtsScreen(
        title = reverseTitle(model.type, model.ticker),
        modifier = modifier,
        onBack = actions.onCancel,
        backLabel = stringResource(R.string.back_button),
        backEnabled = !state.saving,
    ) {
        EntryColumn {
            listOfNotNull(saveMessage(refusals.save), reversalRuleMessage(refusals.rule)).forEach { Message(it) }
            OriginalCard(model)
            AtsTextField(
                value = state.transactionReversalDate,
                onValueChange = actions.onDateChange,
                label = stringResource(R.string.reversal_date_label),
                modifier = Modifier.fillMaxWidth(),
                refusal = dateMessage(refusals.date),
                note = stringResource(R.string.reversal_date_note, model.date),
                trailing = LucideIcon.CALENDAR,
                trailingLabel = stringResource(R.string.date_pick_open),
                onTrailing = datePickerOpener(state.transactionReversalDate, actions.onDateChange, dense = false),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = DateDigitsTransformation,
            )
            model.effect?.let { effect -> AtsReviewCard { EffectRows(effect) } }
            AtsEntryFoot(
                primary = stringResource(R.string.confirm_button),
                onPrimary = actions.onConfirm,
                enabled = !state.saving,
                secondary = stringResource(R.string.cancel_button),
                onSecondary = actions.onCancel,
                secondaryText = true,
            )
        }
    }
}

/** "Reverse buy AAPL", "Reverse sell AAPL" or "Reverse split AAPL" (frame I; 13b wording 1 to 3). */
@Composable
private fun reverseTitle(
    type: TransactionType,
    ticker: String,
): String = stringResource(
    when (type) {
        TransactionType.SELL -> R.string.reverse_title_sell
        TransactionType.SPLIT -> R.string.reverse_title_split
        else -> R.string.reverse_title_buy
    },
    ticker,
)

/** "buy", "sell" or "split", inside a sentence. */
@Composable
internal fun transactionTypeLower(type: TransactionType): String = stringResource(
    when (type) {
        TransactionType.SELL -> R.string.transaction_type_sell_lower
        TransactionType.SPLIT -> R.string.transaction_type_split_lower
        else -> R.string.transaction_type_buy_lower
    },
)

/**
 * The original, fixed (frame I marker 16): "Reverses" in secondary 14, its type and date with its portfolio, then its
 * quantity and total, or its ratio; read by TalkBack as one sentence.
 */
@Composable
private fun OriginalCard(model: ReversalScreenModel) {
    val type =
        stringResource(
            when (model.type) {
                TransactionType.SELL -> R.string.holding_transaction_sell
                TransactionType.SPLIT -> R.string.holding_transaction_split
                else -> R.string.holding_transaction_buy
            },
        )
    val ratio = model.ratio
    val shares = stringResource(if (model.shares.one) R.string.holding_share_one else R.string.holding_shares, model.shares.shown)
    val spoken =
        if (ratio != null) {
            stringResource(R.string.reversal_reverses_split_spoken, model.date, model.portfolio, ratioText(ratio))
        } else {
            stringResource(R.string.reversal_reverses_spoken, transactionTypeLower(model.type), model.date, model.portfolio, shares, model.total.substringAfter('$'))
        }
    AtsReviewCard {
        Column(modifier = Modifier.clearAndSetSemantics { contentDescription = spoken }) {
            Text(text = stringResource(R.string.reversal_reverses_label), style = typeStyle("small"), color = AtsTheme.colors.textSecondary)
            ReviewRow(stringResource(R.string.transaction_link, type, model.date), model.portfolio)
            if (ratio != null) {
                ReviewRow(stringResource(R.string.split_button), ratioText(ratio))
            } else {
                ReviewRow(stringResource(R.string.transaction_quantity_traded), model.shares.shown)
                ReviewRow(stringResource(R.string.buy_total_row), model.total)
            }
        }
    }
}

/**
 * What a reversal does (frame I marker 26; 13b wording 1 to 3), drawn from its model, on the reversal screen and on a
 * saved reversal's detail: the shares removed, the cash and a realized gain of $0.00 for a buy; the lots restored, the
 * cash and the realized gain removed, without color, for a sell; the ratio applied and the shares before and after for a
 * split.
 */
@Composable
internal fun EffectRows(effect: ReversalEffectModel) {
    val colors = AtsTheme.colors
    when (effect) {
        is ReversalEffectModel.OfBuy -> {
            ReviewRow(
                stringResource(R.string.reversal_shares_removed),
                effect.sharesRemoved,
                spoken = stringResource(R.string.reversal_shares_removed_spoken, effect.sharesRemoved),
            )
            CashEffectRow(effect.cashCents)
            val zero = spokenRealizedZero()
            Text(
                text = stringResource(R.string.reversal_realized_zero, Money.format(0L)),
                style = typeStyle("small"),
                color = colors.textSecondary,
                modifier = Modifier.clearAndSetSemantics { contentDescription = zero },
            )
        }

        is ReversalEffectModel.OfSell -> {
            AtsReviewSubheading(stringResource(R.string.reversal_lots_restored))
            effect.lots.forEach { lot ->
                val shares = stringResource(if (lot.shares.one) R.string.holding_share_one else R.string.holding_shares, lot.shares.shown)
                val spoken = stringResource(R.string.reversal_lot_restored_spoken, lot.date, shares)
                Text(
                    text = stringResource(R.string.sell_piece_lot, stringResource(R.string.date_with_et, lot.date), shares),
                    style = typeStyle("body"),
                    color = colors.text,
                    modifier = Modifier.clearAndSetSemantics { contentDescription = spoken },
                )
            }
            CashEffectRow(effect.cashCents)
            ReviewRow(
                stringResource(R.string.reversal_realized_removed),
                signedAmount(effect.realizedRemovedCents),
                spoken = stringResource(R.string.reversal_realized_removed_spoken, spokenSigned(effect.realizedRemovedCents)),
            )
        }

        is ReversalEffectModel.OfSplit -> {
            ReviewRow(stringResource(R.string.reversal_ratio_applied), ratioText(effect.inverse))
            ReviewRow(stringResource(R.string.split_shares_at_point), effect.before)
            ReviewRow(stringResource(R.string.reversal_shares_after), effect.after, bold = true)
            Text(text = stringResource(R.string.split_no_cash), style = typeStyle("small"), color = colors.textSecondary)
        }
    }
}

@Composable
internal fun CashEffectRow(cents: Long) {
    ReviewRow(
        stringResource(R.string.cash_effect_label),
        signedAmount(cents),
        bold = true,
        spoken = stringResource(R.string.reversal_cash_effect_spoken, spokenSigned(cents)),
    )
}

@Composable
private fun spokenRealizedZero(): String = stringResource(R.string.sell_realized_spoken, Money.format(0L).substringAfter('$'))

/** A value shown alone, rounded half away from zero to cents (4.10). */
internal fun centsOf(dollars: Fraction): Long = dollars.rounded(2).movePointRight(2).longValueExact()

/** K1's reversal refusals (13b wording 4; frame J). The date field's reasons show at the field. */
@Composable
private fun reversalRuleMessage(refusal: ReversalRefusal?): String? = when (refusal) {
    is ReversalRefusal.SellNotReversed ->
        stringResource(R.string.reversal_sell_not_reversed, signedShares(refusal.held), refusal.date.toString(), refusal.sellDate.toString())

    is ReversalRefusal.TooEarly ->
        stringResource(
            R.string.reversal_too_early,
            signedShares(refusal.held),
            refusal.date.toString(),
            refusal.reversalDate.toString(),
            refusal.earliest.toString(),
        )

    is ReversalRefusal.SellShort -> stringResource(R.string.reversal_sell_short, refusal.date.toString())

    is ReversalRefusal.BeforeOriginal -> stringResource(R.string.reversal_before_original, refusal.originalDate.toString())

    is ReversalRefusal.NegativeCash -> stringResource(R.string.cash_negative, refusal.date.toString(), Money.format(refusal.shortfallCents))

    ReversalRefusal.TooLarge -> stringResource(R.string.cash_too_large)

    ReversalRefusal.NotFound, ReversalRefusal.NotReversible -> stringResource(R.string.reversal_not_reversible)

    null -> null
}

/** A lot's figure below 0 with a real minus sign, up to 8 decimals (13b wording 4). */
private fun signedShares(held: Fraction): String = if (held.signum() < 0) "−${Quantity.format(held.abs())}" else Quantity.format(held)
