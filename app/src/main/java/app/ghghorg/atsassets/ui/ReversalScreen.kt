package app.ghghorg.atsassets.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.Money
import app.ghghorg.atsassets.core.Quantity
import app.ghghorg.atsassets.core.ReversalEffect
import app.ghghorg.atsassets.core.ReversalRefusal
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType

/** The reversal screen's callbacks, passed together. */
class ReversalActions(
    val onDateChange: (String) -> Unit,
    val onConfirm: () -> Unit,
    val onCancel: () -> Unit,
)

/**
 * The reversal of a buy, sell or split (wireframe 10b frame I; iteration 13b): the original fixed under "Reverses", the
 * reversal date with its note, and the effect at that date (13b wording 1 to 3). One screen with Confirm and save and
 * Cancel, no separate review (section 6). A refusal keeps the screen open with the date (frame J).
 */
@Composable
fun TransactionReversalScreen(
    state: AppState,
    actions: ReversalActions,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = !state.saving, onBack = actions.onCancel)
    val original = state.shownTransaction ?: return
    val refusals = state.transactionReversalRefusals
    Column(
        modifier = modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TitleRow(reverseTitle(original), actions.onCancel, enabled = !state.saving)
        Text(
            text = stringResource(R.string.reversal_reverses_label),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OriginalCard(state, original)
        OutlinedTextField(
            value = state.transactionReversalDate,
            onValueChange = actions.onDateChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(R.string.reversal_date_label)) },
            supportingText = {
                Column {
                    dateMessage(refusals.date)?.let { Text(text = it) }
                    Text(text = stringResource(R.string.reversal_date_note, original.date.toString()))
                }
            },
            isError = refusals.date != null,
            visualTransformation = DateDigitsTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
        )
        state.transactionReversalEffect?.let { effect ->
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    EffectRows(effect)
                }
            }
        }
        listOfNotNull(saveMessage(refusals.save), reversalRuleMessage(refusals.rule)).forEach { Message(it) }
        ReversalButtons(state.saving, actions.onConfirm, actions.onCancel)
    }
}

/** "Reverse buy AAPL", "Reverse sell AAPL" or "Reverse split AAPL" (frame I; 13b wording 1 to 3). */
@Composable
private fun reverseTitle(original: Transaction): String = stringResource(
    when (original.type) {
        TransactionType.SELL -> R.string.reverse_title_sell
        TransactionType.SPLIT -> R.string.reverse_title_split
        else -> R.string.reverse_title_buy
    },
    original.ticker,
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

/** The original, fixed (frame I marker 16): its type and date with its portfolio, then its quantity and total, or its ratio. */
@Composable
private fun OriginalCard(
    state: AppState,
    original: Transaction,
) {
    val portfolio = state.portfolios.firstOrNull { it.id == original.portfolioId }?.name.orEmpty()
    val date = original.date.toString()
    val type =
        stringResource(
            when (original.type) {
                TransactionType.SELL -> R.string.holding_transaction_sell
                TransactionType.SPLIT -> R.string.holding_transaction_split
                else -> R.string.holding_transaction_buy
            },
        )
    val ratio = original.split
    val spoken =
        if (ratio != null) {
            stringResource(R.string.reversal_reverses_split_spoken, date, portfolio, ratioText(ratio))
        } else {
            stringResource(
                R.string.reversal_reverses_spoken,
                transactionTypeLower(original.type),
                date,
                portfolio,
                sharesText(original.quantity), // Spike F (open item 47): defect TB brought back on purpose
                Money.format(original.totalCents).substringAfter('$'),
            )
        }
    OutlinedCard(modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = spoken }) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ReviewRow(stringResource(R.string.transaction_link, type, date), portfolio)
            if (ratio != null) {
                ReviewRow(stringResource(R.string.split_button), ratioText(ratio))
            } else {
                ReviewRow(stringResource(R.string.transaction_quantity_traded), Quantity.format(original.boughtQuantity))
                ReviewRow(stringResource(R.string.buy_total_row), Money.format(original.totalCents))
            }
        }
    }
}

/**
 * What a reversal does (frame I marker 26; 13b wording 1 to 3), on the reversal screen and on a saved reversal's detail:
 * the shares removed, the cash and a realized gain of $0.00 for a buy; the lots restored, the cash and the realized
 * gain removed, without color, for a sell; the ratio applied and the shares before and after for a split.
 */
@Composable
internal fun EffectRows(effect: ReversalEffect) {
    when (effect) {
        is ReversalEffect.OfBuy -> {
            val removed = Quantity.format(effect.sharesRemoved)
            ReviewRow(
                stringResource(R.string.reversal_shares_removed),
                removed,
                spoken = stringResource(R.string.reversal_shares_removed_spoken, removed),
            )
            CashEffectRow(effect.cashCents)
            Text(
                text = stringResource(R.string.reversal_realized_zero, Money.format(0L)),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semanticsReadAs(spokenRealizedZero()),
            )
        }

        is ReversalEffect.OfSell -> {
            Text(
                text = stringResource(R.string.reversal_lots_restored),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            effect.lots.forEach { lot ->
                val date = lot.buyDate.toString()
                val shares = sharesText(lot.quantity)
                Text(
                    text = stringResource(R.string.sell_piece_lot, stringResource(R.string.date_with_et, date), shares),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.semanticsReadAs(stringResource(R.string.reversal_lot_restored_spoken, date, shares)),
                )
            }
            CashEffectRow(effect.cashCents)
            val removed = centsOf(effect.realizedRemoved)
            ReviewRow(
                stringResource(R.string.reversal_realized_removed),
                signedAmount(removed),
                spoken = stringResource(R.string.reversal_realized_removed_spoken, spokenSigned(removed)),
            )
        }

        is ReversalEffect.OfSplit -> {
            ReviewRow(stringResource(R.string.reversal_ratio_applied), ratioText(effect.inverse))
            ReviewRow(stringResource(R.string.split_shares_at_point), Quantity.format(effect.before))
            ReviewRow(stringResource(R.string.reversal_shares_after), Quantity.format(effect.after), bold = true)
            Text(
                text = stringResource(R.string.split_no_cash),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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

/** Read by TalkBack as [spoken] alone. */
private fun Modifier.semanticsReadAs(spoken: String): Modifier = clearAndSetSemantics { contentDescription = spoken }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReversalButtons(
    saving: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(onClick = onConfirm, enabled = !saving) {
            Text(text = stringResource(R.string.confirm_button))
        }
        TextButton(onClick = onCancel, enabled = !saving) {
            Text(text = stringResource(R.string.cancel_button))
        }
    }
}

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
