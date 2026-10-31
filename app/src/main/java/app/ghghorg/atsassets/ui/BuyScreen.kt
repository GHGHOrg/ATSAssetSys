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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.core.AmountError
import app.ghghorg.atsassets.core.AmountResult
import app.ghghorg.atsassets.core.BuyRefusal
import app.ghghorg.atsassets.core.Money
import app.ghghorg.atsassets.core.Quantity
import app.ghghorg.atsassets.core.QuantityError
import app.ghghorg.atsassets.core.QuantityResult
import app.ghghorg.atsassets.core.TickerCheck
import app.ghghorg.atsassets.core.TickerRefusal
import app.ghghorg.atsassets.prices.CheckRefusal

/** The buy entry's callbacks, passed together. */
class BuyActions(
    val onPortfolioChange: (String) -> Unit,
    val onTickerChange: (String) -> Unit,
    val onDateChange: (String) -> Unit,
    val onQuantityChange: (String) -> Unit,
    val onTotalChange: (String) -> Unit,
    val onReview: () -> Unit,
    val onBack: () -> Unit,
)

/**
 * Buy entry (wireframe 10a frame A; 09 decisions 10 and 11): the portfolio as chips (no dropdown window), the ticker
 * in capitals, the date as digits like the Cash filters, quantity and total on a number keyboard, and the price per
 * share as the fields change. The refusals of the fields, of K1 and of the ticker check show here; the typed text
 * lives in the ViewModel.
 */
@Composable
fun BuyEntryScreen(
    state: AppState,
    actions: BuyActions,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = !state.checking, onBack = actions.onBack)
    val form = state.buyForm
    val refusals = state.buyRefusals
    val ticker = TickerCheck.normalize(form.tickerText).orEmpty()
    Column(
        modifier = modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TitleRow(stringResource(R.string.buy_button), actions.onBack)
        PortfolioChips(state, actions.onPortfolioChange)
        OutlinedTextField(
            value = form.tickerText,
            onValueChange = actions.onTickerChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(R.string.buy_ticker_label)) },
            supportingText = if (refusals.tickerEmpty) ({ Text(text = stringResource(R.string.buy_ticker_empty)) }) else null,
            isError = refusals.tickerEmpty,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false),
            singleLine = true,
        )
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
            label = stringResource(R.string.buy_total_label),
            error = totalMessage(refusals.total),
        )
        state.buyPricePerShare?.let { price ->
            val shown = Money.formatPerShare(price)
            val spoken = spokenLabel(stringResource(R.string.buy_price_per_share_label), shown)
            Text(
                text = stringResource(R.string.buy_price_per_share, shown),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.clearAndSetSemantics { contentDescription = spoken },
            )
        }
        val messages = listOfNotNull(saveMessage(refusals.save), ruleMessage(refusals.rule), checkMessage(refusals.check, ticker))
        messages.forEach { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        if (state.checking) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Text(
                    text = stringResource(R.string.buy_checking, ticker),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Button(
            onClick = actions.onReview,
            enabled = !state.checking,
            modifier = Modifier.align(Alignment.End),
        ) {
            Text(text = stringResource(R.string.review_button))
        }
    }
}

/**
 * Buy review (wireframe 10a frame B): the entered values, the price per share and the cash effect with its sign and
 * no color. Nothing is saved until Confirm and save (criterion 30).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BuyReviewScreen(
    state: AppState,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = !state.saving, onBack = onBack)
    val form = state.buyForm
    val portfolio = state.portfolios.firstOrNull { it.id == form.portfolioId }?.name.orEmpty()
    val date = DateDigits.toDate(form.dateText)?.toString() ?: DateDigits.format(form.dateText)
    val quantity = (Quantity.parse(form.quantityText) as? QuantityResult.Exact)?.value
    val cents = (Money.parseCents(form.totalText) as? AmountResult.Cents)?.value
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TitleRow(stringResource(R.string.buy_review_title), onBack, enabled = !state.saving)
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ReviewRow(stringResource(R.string.buy_portfolio_label), portfolio)
                ReviewRow(stringResource(R.string.buy_ticker_label), TickerCheck.normalize(form.tickerText).orEmpty())
                ReviewRow(stringResource(R.string.date_label), stringResource(R.string.date_with_et, date))
                ReviewRow(stringResource(R.string.buy_quantity_label), quantity?.let { Quantity.format(it) }.orEmpty())
                if (cents != null) {
                    val total = stringResource(R.string.buy_total_row)
                    ReviewRow(total, Money.format(cents), spoken = spokenLabel(total, Money.format(cents)))
                    state.buyPricePerShare?.let { price ->
                        val label = stringResource(R.string.buy_price_per_share_label)
                        val shown = Money.formatPerShare(price)
                        ReviewRow(label, shown, spoken = spokenLabel(label, shown))
                    }
                    val effect = stringResource(R.string.cash_effect_label)
                    ReviewRow(effect, signedAmount(-cents), bold = true, spoken = "$effect, ${spokenDollars(-cents)} dollars")
                }
            }
        }
        Text(
            text = stringResource(R.string.review_hint),
            style = MaterialTheme.typography.bodyMedium,
        )
        listOfNotNull(saveMessage(state.buyRefusals.save), ruleMessage(state.buyRefusals.rule)).forEach { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
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

/** "<" and the title, as on a portfolio's screen; TalkBack reads "<" as Back. */
@Composable
private fun TitleRow(
    title: String,
    onBack: () -> Unit,
    enabled: Boolean = true,
) {
    val back = stringResource(R.string.back_button)
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onBack, enabled = enabled, modifier = Modifier.semantics { contentDescription = back }) {
            Text(text = "<", style = MaterialTheme.typography.titleLarge)
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
    }
}

/** One chip per portfolio, in creation order, one selected (09 decision 11: no dropdown window). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PortfolioChips(
    state: AppState,
    onChange: (String) -> Unit,
) {
    Column {
        Text(
            text = stringResource(R.string.buy_portfolio_label),
            style = MaterialTheme.typography.bodyMedium,
        )
        FlowRow(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (portfolio in state.portfolios) {
                FilterChip(
                    selected = portfolio.id == state.buyForm.portfolioId,
                    onClick = { onChange(portfolio.id) },
                    label = { Text(text = portfolio.name) },
                )
            }
        }
    }
}

@Composable
private fun NumberField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    error: String?,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(text = label) },
        supportingText = error?.let { { Text(text = it) } },
        isError = error != null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
    )
}

/** "Label, 150.00 dollars" for TalkBack, from a shown dollar figure such as "$150.00". */
@Composable
private fun spokenLabel(
    label: String,
    shown: String,
): String = stringResource(R.string.spoken_label_dollars, label, shown.substringAfter('$'))

@Composable
private fun quantityMessage(error: QuantityError?): String? = when (error) {
    null -> null
    QuantityError.EMPTY -> stringResource(R.string.quantity_empty)
    QuantityError.BAD_FORMAT -> stringResource(R.string.quantity_bad_format)
    QuantityError.TOO_MANY_DECIMALS -> stringResource(R.string.quantity_too_many_decimals)
    QuantityError.ZERO -> stringResource(R.string.quantity_zero)
}

@Composable
private fun totalMessage(error: AmountError?): String? = when (error) {
    null -> null
    AmountError.EMPTY -> stringResource(R.string.total_empty)
    AmountError.BAD_FORMAT -> stringResource(R.string.total_bad_format)
    AmountError.TOO_MANY_DECIMALS -> stringResource(R.string.total_too_many_decimals)
    AmountError.ZERO -> stringResource(R.string.total_zero)
    AmountError.TOO_LARGE -> stringResource(R.string.total_too_large)
}

// The field reasons of K1 (no opening balance, ticker, quantity, total) cannot reach here: the menu and the field
// rules catch them first.
@Composable
private fun ruleMessage(refusal: BuyRefusal?): String? = when (refusal) {
    is BuyRefusal.NegativeCash ->
        stringResource(R.string.cash_negative, refusal.date.toString(), Money.format(refusal.shortfallCents))

    is BuyRefusal.BeforeOpeningBalance ->
        stringResource(R.string.buy_before_opening_balance, refusal.openingDate.toString())

    BuyRefusal.PortfolioNotFound -> stringResource(R.string.buy_choose_portfolio)

    BuyRefusal.TooLarge -> stringResource(R.string.cash_too_large)

    else -> null
}

/** The six check-failure messages and the ticker refusals (09 decisions 6 and 8, amended in step 6; wording 1). */
@Composable
private fun checkMessage(
    refusal: CheckRefusal?,
    ticker: String,
): String? = when (refusal) {
    null -> null
    CheckRefusal.Offline -> stringResource(R.string.check_offline, ticker)
    CheckRefusal.NoKey -> stringResource(R.string.check_no_key, ticker)
    is CheckRefusal.NoAnswer -> noAnswerMessage(refusal.status, ticker)
    is CheckRefusal.KeyRefused -> stringResource(R.string.check_key_refused, ticker, refusal.status)
    CheckRefusal.TooManyCalls -> stringResource(R.string.check_too_many_calls, ticker)
    CheckRefusal.Unreadable -> stringResource(R.string.check_unreadable, ticker)
    is CheckRefusal.Ticker -> tickerMessage(refusal.reason, ticker)
}

/** With the status in parentheses when there is one; without when no connection was made (09 decision 8). */
@Composable
private fun noAnswerMessage(
    status: Int?,
    ticker: String,
): String = if (status == null) {
    stringResource(R.string.check_no_answer, ticker)
} else {
    stringResource(R.string.check_no_answer_status, ticker, status)
}

@Composable
private fun tickerMessage(
    reason: TickerRefusal,
    ticker: String,
): String = when (reason) {
    TickerRefusal.NotFound -> stringResource(R.string.ticker_not_found, ticker)
    TickerRefusal.MutualFund -> stringResource(R.string.ticker_mutual_fund, ticker)
    is TickerRefusal.NotUsExchange -> stringResource(R.string.ticker_not_us_exchange, ticker, reason.reported)
    is TickerRefusal.NotUsd -> stringResource(R.string.ticker_not_usd, ticker, reason.reported)
}
