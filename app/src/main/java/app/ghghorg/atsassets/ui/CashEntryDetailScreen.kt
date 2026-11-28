package app.ghghorg.atsassets.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.ui.design.AtsButton
import app.ghghorg.atsassets.ui.design.AtsCard
import app.ghghorg.atsassets.ui.design.AtsEntryFoot
import app.ghghorg.atsassets.ui.design.AtsReviewCard
import app.ghghorg.atsassets.ui.design.AtsScreen
import app.ghghorg.atsassets.ui.design.AtsTextField
import app.ghghorg.atsassets.ui.theme.AtsIcon
import app.ghghorg.atsassets.ui.theme.AtsTheme
import app.ghghorg.atsassets.ui.theme.LucideIcon
import app.ghghorg.atsassets.ui.theme.tokenDp
import app.ghghorg.atsassets.ui.theme.typeStyle

/**
 * The cash entry detail (wireframe 10b, frame H; iteration 05), drawn from its model (CR2, open item 48) in the design
 * system (44a step 8 group (d) part 2; D-31): under its band, the type, date with ET, signed amount and note as
 * label-and-value rows in a card, then one status line or a link to the other entry as a tappable row with a chevron
 * (D-31 (5)), and Reverse and Reverse and re-enter as outline buttons when they are offered (spec 4.6). A trade line is
 * titled with its buy or sell and portfolio, and links to that transaction instead (13a wording 3).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CashEntryDetail(
    state: AppState,
    onOpenEntry: (String) -> Unit,
    onOpenTransaction: () -> Unit,
    onReverse: (String) -> Unit,
    onReenter: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    val model = state.cashDetailModel ?: return
    val entry = model.entry
    val defaultNote = stringResource(R.string.reversal_note_default, cashTypeLower(entry.type), entry.date)
    val title = model.title
    AtsScreen(
        title = if (title is CashDetailTitle.Trade) tradeTitleText(title.title, title.ticker) else cashTypeLabel(entry.type),
        modifier = modifier,
        onBack = onBack,
        backLabel = stringResource(R.string.back_button),
    ) {
        EntryColumn {
            if (title is CashDetailTitle.Trade) {
                Text(text = stringResource(R.string.trade_for_portfolio, title.portfolio), style = typeStyle("body"), color = AtsTheme.colors.text)
            }
            AtsReviewCard {
                ReviewRow(stringResource(R.string.type_label), cashTypeLabel(entry.type))
                ReviewRow(stringResource(R.string.date_label), stringResource(R.string.date_with_et, entry.date))
                ReviewRow(stringResource(R.string.amount_label), signedAmount(entry.amountCents))
                NoteRow(model.note)
            }
            when (val status = model.status) {
                CashDetailStatus.OpeningNotReversible -> StatusLine(stringResource(R.string.detail_opening_not_reversible))

                is CashDetailStatus.TradeLine -> {
                    if (status.hint) StatusLine(stringResource(R.string.trade_line_reverse_hint))
                    val trade = tradeTitleText(status.title, status.ticker)
                    DetailLinkRow(stringResource(R.string.transaction_link, trade, status.date), stringResource(R.string.transaction_link_spoken, trade, status.date), onOpenTransaction)
                }

                is CashDetailStatus.ReversedBy -> {
                    val text = stringResource(R.string.detail_reversed_on, status.date)
                    DetailLinkRow(text, text) { onOpenEntry(status.id) }
                }

                is CashDetailStatus.Reverses -> {
                    val text = stringResource(R.string.detail_reverses, cashTypeLabel(status.type), status.date)
                    DetailLinkRow(text, text) { onOpenEntry(status.id) }
                }

                CashDetailStatus.NotReversed -> StatusLine(stringResource(R.string.detail_not_reversed))
            }
            if (model.reverseOffered || model.reenterOffered) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(tokenDp("spaceGap"))) {
                    if (model.reverseOffered) AtsButton(stringResource(R.string.reverse_button), { onReverse(defaultNote) })
                    // Reverse and re-enter on a direct cash entry (14b; 10b marker 19), its reversal with the same fixed note.
                    if (model.reenterOffered) {
                        val spoken = stringResource(R.string.reenter_cash_spoken, cashTypeLower(entry.type), entry.date)
                        AtsButton(stringResource(R.string.reenter_button), { onReenter(defaultNote) }, modifier = Modifier.semantics { contentDescription = spoken })
                    }
                }
            }
        }
    }
}

/**
 * The reversal of a cash entry (wireframe 10b, frame I marker 19), in the design system (44a step 8 group (d) part 2):
 * the original fixed in a card, an Adjustment with the opposite amount ending in its cash effect, the date and the
 * pre-filled note (05 decision 1), a refusal as the blocked card at the top, and Confirm and save with Cancel as a text
 * button (D-31 (2)). One screen, no separate review (spec 4.6).
 */
@Composable
fun CashReversal(
    state: AppState,
    onDateChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = !state.saving, onBack = onCancel)
    val original = state.cashDetailModel?.entry ?: return
    val refusals = state.refusals
    val opposite = signedAmount(-original.amountCents)
    AtsScreen(
        title = stringResource(R.string.reversal_title, cashTypeLower(original.type)),
        modifier = modifier,
        onBack = onCancel,
        backLabel = stringResource(R.string.back_button),
        backEnabled = !state.saving,
    ) {
        EntryColumn {
            listOfNotNull(saveMessage(refusals.save), cashMessage(refusals.cash)).forEach { Message(it) }
            AtsReviewCard {
                Text(text = stringResource(R.string.reversal_reverses_label), style = typeStyle("small"), color = AtsTheme.colors.textSecondary)
                ReviewRow(cashTypeLabel(original.type), stringResource(R.string.date_with_et, original.date))
                ReviewRow(stringResource(R.string.amount_label), signedAmount(original.amountCents))
            }
            AtsReviewCard {
                Text(text = stringResource(R.string.reversal_type_fixed), style = typeStyle("body"), color = AtsTheme.colors.text)
                ReviewRow(stringResource(R.string.amount_label), opposite)
                ReviewRow(stringResource(R.string.cash_effect_label), opposite, bold = true)
            }
            AtsTextField(
                value = state.reversal.dateText,
                onValueChange = onDateChange,
                label = stringResource(R.string.reversal_date_label),
                modifier = Modifier.fillMaxWidth(),
                refusal = dateMessage(refusals.date),
                note = stringResource(R.string.reversal_date_note, original.date),
                trailing = LucideIcon.CALENDAR,
                trailingLabel = stringResource(R.string.date_pick_open),
                onTrailing = datePickerOpener(state.reversal.dateText, onDateChange, dense = false, digitsOnly = false),
            )
            AtsTextField(
                value = state.reversal.noteText,
                onValueChange = onNoteChange,
                label = stringResource(R.string.note_label_required),
                modifier = Modifier.fillMaxWidth(),
                refusal = noteMessage(refusals.noteHasLineBreak, refusals.noteRequired),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                singleLine = false,
            )
            AtsEntryFoot(
                primary = stringResource(R.string.confirm_button),
                onPrimary = onConfirm,
                enabled = !state.saving,
                secondary = stringResource(R.string.cancel_button),
                onSecondary = onCancel,
                secondaryText = true,
            )
        }
    }
}

/** A plain status line in secondary 14 (10b frame H: "Not reversed.", why there is no button). */
@Composable
internal fun StatusLine(text: String) {
    Text(text = text, style = typeStyle("small"), color = AtsTheme.colors.textSecondary)
}

/**
 * A link to the other entry of a reversal pair or to the trade (spec 4.6: the chain is traceable): a tappable 48 row in a
 * card with a chevron (D-31 (5)), read by TalkBack as [spoken] and a button.
 */
@Composable
internal fun DetailLinkRow(
    text: String,
    spoken: String,
    onOpen: () -> Unit,
) {
    AtsCard(padded = false) { DetailLinkLine(text, spoken, onOpen, Modifier.padding(horizontal = tokenDp("spaceInset"))) }
}

/** The link's row alone, for inside a card that already has its padding (a transaction's cash line, 10b frame G). */
@Composable
internal fun DetailLinkLine(
    text: String,
    spoken: String,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AtsTheme.colors
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .heightIn(min = tokenDp("spaceTouch"))
            .clickable(role = Role.Button, onClick = onOpen)
            .clearAndSetSemantics {
                contentDescription = spoken
                role = Role.Button
                onClick {
                    onOpen()
                    true
                }
            }.then(modifier),
        horizontalArrangement = Arrangement.spacedBy(tokenDp("spaceInset")),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = text, style = typeStyle("body"), color = colors.text, modifier = Modifier.weight(1f))
        AtsIcon(LucideIcon.CHEVRON_RIGHT, tint = colors.iconChevron, size = tokenDp("fieldMarkSize"))
    }
}

/** The type in lower case, for the reversal title and the pre-filled note (wireframe 10b, marker 19); also 14b's banner. */
@Composable
internal fun cashTypeLower(type: CashType): String = when (type) {
    CashType.DEPOSIT -> stringResource(R.string.cash_type_deposit_lower)
    CashType.WITHDRAWAL -> stringResource(R.string.cash_type_withdrawal_lower)
    CashType.INTEREST_DIVIDEND -> stringResource(R.string.cash_type_interest_dividend_lower)
    CashType.ADJUSTMENT -> stringResource(R.string.cash_type_adjustment_lower)
    CashType.OPENING_BALANCE, CashType.TRADE -> cashTypeLabel(type)
}
