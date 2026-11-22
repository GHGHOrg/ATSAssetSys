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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.core.Quantity
import app.ghghorg.atsassets.core.SplitRatio
import app.ghghorg.atsassets.core.SplitRefusal
import app.ghghorg.atsassets.core.TransactionType

/** The split entry's callbacks, passed together. */
class SplitActions(
    val onPortfolioChange: (String) -> Unit,
    val onTickerChange: (String) -> Unit,
    val onDateChange: (String) -> Unit,
    val onToChange: (String) -> Unit,
    val onFromChange: (String) -> Unit,
    val onReview: () -> Unit,
    val onBack: () -> Unit,
)

/**
 * Split entry (wireframe 10b frame E; iteration 12): one scrolling screen with the portfolio and ticker chips (decision
 * 1), the date as digits, and the ratio as two whole numbers, "to for from" (12 wording 1). The whole screen scrolls as
 * one, so nothing is hidden at the largest font (criterion 39).
 */
@Composable
fun SplitEntryScreen(
    state: AppState,
    actions: SplitActions,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = !state.saving, onBack = actions.onBack)
    val form = state.splitForm
    val refusals = state.splitRefusals
    // A split replaced by Reverse and re-enter (14b; wording 1): the title, the banner, the hints and the pair's messages.
    val original = state.replacedTransaction?.takeIf { it.type == TransactionType.SPLIT }
    Column(
        modifier = modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TitleRow(stringResource(if (original != null) R.string.split_replacing_title else R.string.split_button), actions.onBack)
        original?.let { SplitReplacingBanner(it) }
        PortfolioChips(state.portfolios, form.portfolioId, actions.onPortfolioChange)
        PortfolioHint(state, form.portfolioId)
        TickerChips(state, actions.onTickerChange)
        TickerHint(original, form.ticker)
        OutlinedTextField(
            value = form.dateText,
            onValueChange = actions.onDateChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(R.string.date_label)) },
            supportingText = {
                Column {
                    dateMessage(refusals.date)?.let { Text(text = it) }
                    Text(text = stringResource(R.string.date_note))
                    DateHint(original, form.dateText)
                }
            },
            isError = refusals.date != null,
            visualTransformation = DateDigitsTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
        )
        RatioFields(form, refusals, actions)
        original?.split?.let { ratio ->
            if (ratioNumber(form.toText) != ratio.to || ratioNumber(form.fromText) != ratio.from) {
                val shown = ratioText(ratio)
                OriginalHint(shown, stringResource(R.string.original_hint_spoken, shown))
            }
        }
        if (refusals.tickerMissing) Message(stringResource(R.string.sell_choose_ticker))
        val messages =
            if (original != null) {
                listOfNotNull(
                    saveMessage(refusals.save),
                    replacingMessage(splitRuleMessage(refusals.rule)),
                    replaceRuleMessage(state, state.replaceRefusals.rule),
                )
            } else {
                listOfNotNull(saveMessage(refusals.save), splitRuleMessage(refusals.rule))
            }
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

/** One chip per ticker with a buy in the chosen portfolio, in ticker order (12 decision 1); a line when there is none. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TickerChips(
    state: AppState,
    onChange: (String) -> Unit,
) {
    Column {
        Text(
            text = stringResource(R.string.buy_ticker_label),
            style = MaterialTheme.typography.bodyMedium,
        )
        val tickers = state.splitTickers
        if (tickers.isEmpty()) {
            val name = state.portfolios.firstOrNull { it.id == state.splitForm.portfolioId }?.name.orEmpty()
            Text(
                text = stringResource(R.string.split_no_buys_in, name),
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            FlowRow(
                modifier = Modifier.selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (ticker in tickers) {
                    FilterChip(
                        selected = ticker == state.splitForm.ticker,
                        onClick = { onChange(ticker) },
                        label = { Text(text = ticker) },
                    )
                }
            }
        }
    }
}

/**
 * "Split", the two number fields with "for" between them, the hint, and one message line per kind of refusal (12
 * wording 1): the wireframe's line for an empty, not whole, 0 or equal number, and the limit line (decision 3).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RatioFields(
    form: SplitForm,
    refusals: SplitRefusals,
    actions: SplitActions,
) {
    val errors = listOfNotNull(refusals.to, refusals.from)
    val ruleBroken =
        errors.any { it != RatioError.TOO_LARGE } ||
            refusals.rule == SplitRefusal.RatioEqual ||
            refusals.rule == SplitRefusal.RatioOutOfRange
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = stringResource(R.string.split_button), style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RatioField(form.toText, actions.onToChange, stringResource(R.string.split_to_spoken), refusals.to != null || ruleBroken)
            Text(
                text = stringResource(R.string.split_for),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
            RatioField(form.fromText, actions.onFromChange, stringResource(R.string.split_from_spoken), refusals.from != null || ruleBroken)
        }
        Text(
            text = stringResource(R.string.split_ratio_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (ruleBroken) Message(stringResource(R.string.split_ratio_rule))
        if (RatioError.TOO_LARGE in errors) Message(stringResource(R.string.split_ratio_too_large))
    }
}

@Composable
private fun RatioField(
    value: String,
    onChange: (String) -> Unit,
    spoken: String,
    isError: Boolean,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.width(120.dp).semantics { contentDescription = spoken },
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
    )
}

/**
 * Split review (wireframe 10b frame F; 12 wording 2, option A): the entered values, the shares at this point in the
 * 4.1 order and the shares after the split on two lines, and the line that cost and cash do not change. Nothing is
 * saved until Confirm and save.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SplitReviewScreen(
    state: AppState,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = !state.saving, onBack = onBack)
    val split = state.splitReview?.transaction ?: return
    val ratio = split.split ?: return
    val effect = state.splitEffect ?: return
    val portfolio = state.portfolios.firstOrNull { it.id == split.portfolioId }?.name.orEmpty()
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TitleRow(stringResource(R.string.split_review_title), onBack, enabled = !state.saving)
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ReviewRow(stringResource(R.string.buy_portfolio_label), portfolio)
                ReviewRow(stringResource(R.string.buy_ticker_label), split.ticker)
                ReviewRow(stringResource(R.string.date_label), stringResource(R.string.date_with_et, split.date.toString()))
                ReviewRow(stringResource(R.string.split_button), ratioText(ratio))
                ReviewRow(stringResource(R.string.split_shares_at_point), Quantity.format(effect.before))
                ReviewRow(stringResource(R.string.split_shares_after), Quantity.format(effect.after), bold = true)
                Text(
                    text = stringResource(R.string.split_no_cash),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = stringResource(R.string.review_hint),
            style = MaterialTheme.typography.bodyMedium,
        )
        listOfNotNull(saveMessage(state.splitRefusals.save), splitRuleMessage(state.splitRefusals.rule)).forEach { Message(it) }
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

/** "2 for 1" (12 wording 2): the review's Split row and the split row on Holding detail. */
@Composable
internal fun ratioText(ratio: SplitRatio): String = stringResource(R.string.split_ratio, ratio.to.toString(), ratio.from.toString())

/** K1's split refusals (12 wording 3). The ratio rules show at the ratio fields instead. */
@Composable
private fun splitRuleMessage(refusal: SplitRefusal?): String? = when (refusal) {
    is SplitRefusal.SellShort -> stringResource(R.string.split_sell_short, refusal.date.toString())
    is SplitRefusal.BeforeOpeningBalance -> stringResource(R.string.split_before_opening_balance, refusal.openingDate.toString())
    SplitRefusal.NoOpeningBalance -> stringResource(R.string.opening_balance_first)
    SplitRefusal.PortfolioNotFound -> stringResource(R.string.buy_choose_portfolio)
    SplitRefusal.TickerNotBought -> stringResource(R.string.sell_choose_ticker)
    else -> null
}
