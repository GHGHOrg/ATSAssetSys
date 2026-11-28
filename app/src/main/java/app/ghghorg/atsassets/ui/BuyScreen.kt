package app.ghghorg.atsassets.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.core.AmountError
import app.ghghorg.atsassets.core.BuyRefusal
import app.ghghorg.atsassets.core.Money
import app.ghghorg.atsassets.core.QuantityError
import app.ghghorg.atsassets.core.TickerRefusal
import app.ghghorg.atsassets.prices.CheckRefusal
import app.ghghorg.atsassets.ui.design.AtsEntryFoot
import app.ghghorg.atsassets.ui.design.AtsPickField
import app.ghghorg.atsassets.ui.design.AtsProgressRow
import app.ghghorg.atsassets.ui.design.AtsReviewCard
import app.ghghorg.atsassets.ui.design.AtsScreen
import app.ghghorg.atsassets.ui.design.AtsTextField
import app.ghghorg.atsassets.ui.theme.AtsTheme
import app.ghghorg.atsassets.ui.theme.LucideIcon
import app.ghghorg.atsassets.ui.theme.tokenDp
import app.ghghorg.atsassets.ui.theme.typeStyle

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
 * Buy entry (wireframe 10a frame A; 09 decision 10), drawn from its model (CR2, open item 48) in the design system (44a
 * step 8 group (d) part 1; drawings 10a and 10c, D-30, D-31): under the title band, a refusal that stops the save as the
 * blocked card at the top (frame F); the portfolio as the standard pick field, its list inside the window (D-31 (6)); the
 * ticker in capitals; the date as digits with its calendar; quantity and total side by side, the total with "$" before it;
 * the price per share as a plain line; Review the large gold button at the foot. A field's own rule stays on the field.
 * The typed text lives in the ViewModel. While a buy is replaced (14a), the title says so, the banner names the original,
 * a changed field shows the original under it, and refusals say nothing was changed (wireframe 10c frame A).
 */
@Composable
fun BuyEntryScreen(
    state: AppState,
    actions: BuyActions,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = !state.checking, onBack = actions.onBack)
    val model = state.buyEntry
    val form = state.buyForm
    val refusals = state.buyRefusals
    val replacing = model.replacing
    var picking by rememberSaveable { mutableStateOf(false) }
    val messages =
        if (replacing) {
            listOfNotNull(
                saveMessage(refusals.save),
                replacingMessage(ruleMessage(refusals.rule, replacing = true)),
                replacingMessage(checkMessage(refusals.check, model.ticker)),
                replaceRuleMessage(state, state.replaceRefusals.rule),
            )
        } else {
            listOfNotNull(saveMessage(refusals.save), ruleMessage(refusals.rule), checkMessage(refusals.check, model.ticker))
        }
    Box(modifier = modifier.fillMaxSize()) {
        val under = if (picking) Modifier.clearAndSetSemantics { } else Modifier
        AtsScreen(
            title = stringResource(if (replacing) R.string.buy_replacing_title else R.string.buy_button),
            modifier = Modifier.fillMaxSize().then(under),
            onBack = actions.onBack,
            backLabel = stringResource(R.string.back_button),
            backEnabled = !state.checking,
        ) {
            EntryColumn {
                model.banner?.let { ReplacingBanner(it) }
                messages.forEach { Message(it) }
                PortfolioPick(model.portfolio, model.originals.portfolio) { picking = true }
                Column {
                    AtsTextField(
                        value = form.tickerText,
                        onValueChange = actions.onTickerChange,
                        label = stringResource(R.string.buy_ticker_label),
                        modifier = Modifier.fillMaxWidth(),
                        refusal = if (refusals.tickerEmpty) stringResource(R.string.buy_ticker_empty) else null,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false),
                    )
                    OriginalLine(model.originals.ticker)
                }
                EntryDateField(form.dateText, dateMessage(refusals.date), actions.onDateChange, model.originals.date)
                QuantityAndTotal(
                    quantity = form.quantityText,
                    onQuantity = actions.onQuantityChange,
                    quantityError = quantityMessage(refusals.quantity),
                    total = form.totalText,
                    onTotal = actions.onTotalChange,
                    totalLabel = stringResource(R.string.buy_total_label),
                    totalError = totalMessage(refusals.total),
                    originals = model.originals,
                )
                model.pricePerShare?.let { PricePerShareLine(it) }
                if (state.checking) {
                    AtsProgressRow(
                        stringResource(R.string.buy_checking, model.ticker),
                        done = false,
                        modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                    )
                }
                AtsEntryFoot(stringResource(R.string.review_button), actions.onReview, enabled = !state.checking)
            }
        }
        if (picking) {
            PortfolioChoice(model.portfolios, model.portfolioId, onChoose = { actions.onPortfolioChange(it) }, onClose = { picking = false })
        }
    }
}

/**
 * Buy review (wireframe 10a frame B), drawn from its model (CR2, open item 48): the entered values and the price per share
 * as label-and-value rows in a card, the cash effect as the total line with its sign and no color (D-31 (4)); "Nothing is
 * saved until you confirm." and the foot's Confirm and save and Back to edit (D-31 (2)). Nothing is saved until Confirm
 * and save (criterion 30).
 */
@Composable
fun BuyReviewScreen(
    state: AppState,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = !state.saving, onBack = onBack)
    val model = state.buyReviewModel
    AtsScreen(
        title = stringResource(R.string.buy_review_title),
        modifier = modifier,
        onBack = onBack,
        backLabel = stringResource(R.string.back_button),
        backEnabled = !state.saving,
    ) {
        EntryColumn {
            listOfNotNull(saveMessage(state.buyRefusals.save), ruleMessage(state.buyRefusals.rule)).forEach { Message(it) }
            AtsReviewCard {
                ReviewRow(stringResource(R.string.buy_portfolio_label), model.portfolio)
                ReviewRow(stringResource(R.string.buy_ticker_label), model.ticker)
                ReviewRow(stringResource(R.string.date_label), stringResource(R.string.date_with_et, model.date))
                ReviewRow(stringResource(R.string.buy_quantity_label), model.quantity)
                model.total?.let { total ->
                    val label = stringResource(R.string.buy_total_row)
                    ReviewRow(label, total, spoken = spokenLabel(label, total))
                }
                model.pricePerShare?.let { price ->
                    val label = stringResource(R.string.buy_price_per_share_label)
                    ReviewRow(label, price, spoken = spokenLabel(label, price))
                }
                model.cashCents?.let { CashEffectRow(it) }
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

/** An entry or review's scrolling column under its band: the side margin, the gap between parts, room for the keyboard. */
@Composable
internal fun EntryColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(tokenDp("spaceMargin")),
        verticalArrangement = Arrangement.spacedBy(tokenDp("spaceGap")),
        content = content,
    )
}

/** "Nothing is saved until you confirm." under a review card, in secondary 14 (10a frames B and E). */
@Composable
internal fun ReviewHint() {
    Text(text = stringResource(R.string.review_hint), style = typeStyle("small"), color = AtsTheme.colors.textSecondary)
}

/**
 * The portfolio as the standard pick field (D-31 (6)): its name, read by TalkBack as "Portfolio, Main"; a tap opens its
 * list inside the window. The original's name under it while replacing and it differs (10c marker 2).
 */
@Composable
internal fun PortfolioPick(
    name: String?,
    original: OriginalValue?,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit,
) {
    Column(modifier = modifier) {
        val label = stringResource(R.string.buy_portfolio_label)
        AtsPickField(label, name.orEmpty(), onOpen, modifier = Modifier.fillMaxWidth(), spoken = stringResource(R.string.allocation_portfolio_spoken, name.orEmpty()))
        OriginalLine(original)
    }
}

/** The portfolio's list (D-30, D-31): the shared choice layer inside the window, one portfolio per radio row. */
@Composable
internal fun PortfolioChoice(
    portfolios: List<PickOption>,
    selected: String?,
    onChoose: (String) -> Unit,
    onClose: () -> Unit,
) {
    ChoiceLayer<String?>(
        title = stringResource(R.string.buy_portfolio_label),
        options = portfolios.map { it.id to it.label },
        selected = selected,
        onChoose = { id ->
            id?.let(onChoose)
            onClose()
        },
        onClose = onClose,
    )
}

/** The entry's date (D-28): digits on a number keyboard, the calendar, the New York note, and the original under it. */
@Composable
internal fun EntryDateField(
    text: String,
    error: String?,
    onChange: (String) -> Unit,
    original: OriginalValue?,
) {
    Column {
        AtsTextField(
            value = text,
            onValueChange = onChange,
            label = stringResource(R.string.date_label),
            modifier = Modifier.fillMaxWidth(),
            refusal = error,
            note = stringResource(R.string.date_note),
            trailing = LucideIcon.CALENDAR,
            trailingLabel = stringResource(R.string.date_pick_open),
            onTrailing = datePickerOpener(text, onChange, dense = false),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            visualTransformation = DateDigitsTransformation,
        )
        OriginalLine(original)
    }
}

/**
 * Quantity and total side by side (10a note 2), each with its own refusal and its original under it; the total with "$"
 * before the number in the secondary color, typed without it (D-31 (3)).
 */
@Composable
internal fun QuantityAndTotal(
    quantity: String,
    onQuantity: (String) -> Unit,
    quantityError: String?,
    total: String,
    onTotal: (String) -> Unit,
    totalLabel: String,
    totalError: String?,
    originals: OriginalHints,
) {
    val decimal = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    Row(horizontalArrangement = Arrangement.spacedBy(tokenDp("spaceGap"))) {
        Column(modifier = Modifier.weight(1f)) {
            AtsTextField(quantity, onQuantity, stringResource(R.string.buy_quantity_label), refusal = quantityError, keyboardOptions = decimal)
            OriginalLine(originals.quantity)
        }
        Column(modifier = Modifier.weight(1f)) {
            AtsTextField(total, onTotal, totalLabel, refusal = totalError, prefix = "$", keyboardOptions = decimal)
            OriginalLine(originals.total)
        }
    }
}

/** "Price per share: $150.00" (10a note 1): a plain line in secondary 14, not a field; read as dollars. */
@Composable
internal fun PricePerShareLine(
    shown: String,
    modifier: Modifier = Modifier,
) {
    val spoken = spokenLabel(stringResource(R.string.buy_price_per_share_label), shown)
    Text(
        text = stringResource(R.string.buy_price_per_share, shown),
        style = typeStyle("small"),
        color = AtsTheme.colors.textSecondary,
        modifier = modifier.clearAndSetSemantics { contentDescription = spoken },
    )
}

/** "<" and the title, as on a portfolio's screen; TalkBack reads "<" as Back. Shared with Sell (11). */
@Composable
internal fun TitleRow(
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

/** "Label, 150.00 dollars" for TalkBack, from a shown dollar figure such as "$150.00". */
@Composable
internal fun spokenLabel(
    label: String,
    shown: String,
): String = stringResource(R.string.spoken_label_dollars, label, shown.substringAfter('$'))

@Composable
internal fun quantityMessage(error: QuantityError?): String? = when (error) {
    null -> null
    QuantityError.EMPTY -> stringResource(R.string.quantity_empty)
    QuantityError.BAD_FORMAT -> stringResource(R.string.quantity_bad_format)
    QuantityError.TOO_MANY_DECIMALS -> stringResource(R.string.quantity_too_many_decimals)
    QuantityError.ZERO -> stringResource(R.string.quantity_zero)
}

@Composable
internal fun totalMessage(error: AmountError?): String? = when (error) {
    null -> null
    AmountError.EMPTY -> stringResource(R.string.total_empty)
    AmountError.BAD_FORMAT -> stringResource(R.string.total_bad_format)
    AmountError.TOO_MANY_DECIMALS -> stringResource(R.string.total_too_many_decimals)
    AmountError.ZERO -> stringResource(R.string.total_zero)
    AmountError.TOO_LARGE -> stringResource(R.string.total_too_large)
}

// The field reasons of K1 (no opening balance, ticker, quantity, total) cannot reach here: the menu and the field
// rules catch them first. While [replacing], the cash rule names the replacement (frame F).
@Composable
private fun ruleMessage(
    refusal: BuyRefusal?,
    replacing: Boolean = false,
): String? = when (refusal) {
    is BuyRefusal.NegativeCash ->
        stringResource(
            if (replacing) R.string.replace_cash_negative else R.string.cash_negative,
            refusal.date.toString(),
            Money.format(refusal.shortfallCents),
        )

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
