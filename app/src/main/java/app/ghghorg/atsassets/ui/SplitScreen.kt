package app.ghghorg.atsassets.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
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
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.core.SplitRatio
import app.ghghorg.atsassets.core.SplitRefusal
import app.ghghorg.atsassets.ui.design.AtsDenseField
import app.ghghorg.atsassets.ui.design.AtsEntryFoot
import app.ghghorg.atsassets.ui.design.AtsReviewCard
import app.ghghorg.atsassets.ui.design.AtsScreen
import app.ghghorg.atsassets.ui.theme.AtsTheme
import app.ghghorg.atsassets.ui.theme.tokenDp
import app.ghghorg.atsassets.ui.theme.typeStyle

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

// Which pick list is open over the split screen.
private enum class SplitPicking { NONE, PORTFOLIO, TICKER }

/**
 * Split entry (wireframe 10b frame E; iteration 12), drawn from its model (CR2, open item 48) in the design system (44a
 * step 8 group (d) part 3; D-31 (6), (7)): under its band, a refusal as the blocked card at the top, the portfolio and the
 * ticker with a buy there as standard pick fields side by side (their lists inside the window), the date with its
 * calendar, and the ratio as two small dense fields, "to for from" (12 wording 1), then Review at the foot. The whole
 * screen scrolls as one, so nothing is hidden at the largest font (criterion 39).
 */
@Composable
fun SplitEntryScreen(
    state: AppState,
    actions: SplitActions,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = !state.saving, onBack = actions.onBack)
    val model = state.splitEntry
    val form = state.splitForm
    val refusals = state.splitRefusals
    var picking by rememberSaveable { mutableStateOf(SplitPicking.NONE) }
    // A split replaced by Reverse and re-enter (14b; wording 1): the title, the banner, the hints and the pair's messages.
    val messages =
        listOfNotNull(if (refusals.tickerMissing) stringResource(R.string.sell_choose_ticker) else null) +
            if (model.replacing) {
                listOfNotNull(
                    saveMessage(refusals.save),
                    replacingMessage(splitRuleMessage(refusals.rule)),
                    replaceRuleMessage(state, state.replaceRefusals.rule),
                )
            } else {
                listOfNotNull(saveMessage(refusals.save), splitRuleMessage(refusals.rule))
            }
    Box(modifier = modifier.fillMaxSize()) {
        val under = if (picking != SplitPicking.NONE) Modifier.clearAndSetSemantics { } else Modifier
        AtsScreen(
            title = stringResource(if (model.replacing) R.string.split_replacing_title else R.string.split_button),
            modifier = Modifier.fillMaxSize().then(under),
            onBack = actions.onBack,
            backLabel = stringResource(R.string.back_button),
            backEnabled = !state.saving,
        ) {
            EntryColumn {
                model.banner?.let { SplitReplacingBanner(it) }
                messages.forEach { Message(it) }
                Row(horizontalArrangement = Arrangement.spacedBy(tokenDp("spaceGap"))) {
                    PortfolioPick(model.portfolio, model.originals.portfolio, Modifier.weight(1f)) { picking = SplitPicking.PORTFOLIO }
                    TickerPick(stringResource(R.string.buy_ticker_label), model.ticker, model.tickers, model.originals.ticker, Modifier.weight(1f)) {
                        picking = SplitPicking.TICKER
                    }
                }
                if (model.tickers.isEmpty()) {
                    Text(text = stringResource(R.string.split_no_buys_in, model.portfolio.orEmpty()), style = typeStyle("body"), color = AtsTheme.colors.text)
                }
                EntryDateField(form.dateText, dateMessage(refusals.date), actions.onDateChange, model.originals.date)
                RatioFields(form, refusals, actions, model.originalRatio)
                AtsEntryFoot(stringResource(R.string.review_button), actions.onReview, enabled = !state.saving)
            }
        }
        when (picking) {
            SplitPicking.PORTFOLIO ->
                PortfolioChoice(model.portfolios, model.portfolioId, onChoose = { actions.onPortfolioChange(it) }, onClose = { picking = SplitPicking.NONE })

            SplitPicking.TICKER ->
                TickerChoice(stringResource(R.string.buy_ticker_label), model.tickers, model.ticker, actions.onTickerChange) { picking = SplitPicking.NONE }

            SplitPicking.NONE -> Unit
        }
    }
}

/**
 * "Split" in secondary 14, the two small dense fields with "for" between them (D-31 (7)), the hint in the note's style,
 * and one line per kind of refusal in the refused field's message style, read when it appears (12 wording 1): the
 * wireframe's line for an empty, not whole, 0 or equal number, and the limit line (decision 3). Then the original ratio
 * while replacing and it differs.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RatioFields(
    form: SplitForm,
    refusals: SplitRefusals,
    actions: SplitActions,
    original: SplitRatio?,
) {
    val colors = AtsTheme.colors
    val errors = listOfNotNull(refusals.to, refusals.from)
    val ruleBroken =
        errors.any { it != RatioError.TOO_LARGE } ||
            refusals.rule == SplitRefusal.RatioEqual ||
            refusals.rule == SplitRefusal.RatioOutOfRange
    Column {
        Text(text = stringResource(R.string.split_button), style = typeStyle("small"), color = colors.textSecondary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(tokenDp("fieldDensePadding")), itemVerticalAlignment = Alignment.CenterVertically) {
            RatioField(form.toText, actions.onToChange, stringResource(R.string.split_to_spoken), refusals.to != null || ruleBroken)
            Box(modifier = Modifier.heightIn(min = tokenDp("spaceTouch")), contentAlignment = Alignment.Center) {
                Text(text = stringResource(R.string.split_for), style = typeStyle("body"), color = colors.text)
            }
            RatioField(form.fromText, actions.onFromChange, stringResource(R.string.split_from_spoken), refusals.from != null || ruleBroken)
        }
        Text(text = stringResource(R.string.split_ratio_hint), style = typeStyle("note"), color = colors.textSecondary)
        val lines =
            listOfNotNull(
                if (ruleBroken) stringResource(R.string.split_ratio_rule) else null,
                if (RatioError.TOO_LARGE in errors) stringResource(R.string.split_ratio_too_large) else null,
            )
        lines.forEach {
            Text(text = it, style = typeStyle("message"), color = colors.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        original?.let {
            val shown = ratioText(it)
            OriginalHint(shown, stringResource(R.string.original_hint_spoken, shown))
        }
    }
}

/** One number of the ratio: a small dense field on a number keyboard, at least 64 wide, its refused look when [refused]. */
@Composable
private fun RatioField(
    value: String,
    onChange: (String) -> Unit,
    spoken: String,
    refused: Boolean,
) {
    AtsDenseField(
        value = value,
        onValueChange = onChange,
        // The fields share the lines under them, so a refused one shows its outline and mark with no text of its own.
        refusal = if (refused) "" else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        label = spoken,
        minWidth = tokenDp("fieldRatioWidth"),
    )
}

/**
 * Split review (wireframe 10b frame F; 12 wording 2, option A), drawn from its model (CR2, open item 48): the entered
 * values, the shares at this point in the 4.1 order and the shares after the split as the total line, and the line that
 * cost and cash do not change; "Nothing is saved until you confirm." and the foot. Nothing is saved until Confirm and
 * save.
 */
@Composable
fun SplitReviewScreen(
    state: AppState,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = !state.saving, onBack = onBack)
    val model = state.splitReviewModel ?: return
    AtsScreen(
        title = stringResource(R.string.split_review_title),
        modifier = modifier,
        onBack = onBack,
        backLabel = stringResource(R.string.back_button),
        backEnabled = !state.saving,
    ) {
        EntryColumn {
            listOfNotNull(saveMessage(state.splitRefusals.save), splitRuleMessage(state.splitRefusals.rule)).forEach { Message(it) }
            AtsReviewCard {
                ReviewRow(stringResource(R.string.buy_portfolio_label), model.portfolio)
                ReviewRow(stringResource(R.string.buy_ticker_label), model.ticker)
                ReviewRow(stringResource(R.string.date_label), stringResource(R.string.date_with_et, model.date))
                ReviewRow(stringResource(R.string.split_button), ratioText(model.ratio))
                ReviewRow(stringResource(R.string.split_shares_at_point), model.before)
                ReviewRow(stringResource(R.string.split_shares_after), model.after, bold = true)
                Text(text = stringResource(R.string.split_no_cash), style = typeStyle("small"), color = AtsTheme.colors.textSecondary)
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
