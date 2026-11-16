package app.ghghorg.atsassets.ui

import android.content.res.Resources
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.core.CashType
import java.time.LocalDate

/**
 * The wizard's callbacks, passed together. [onChooseFile] and [onCopyReport] are MainActivity's: Android's file screen
 * with the lock's exemption (AC-57), and the clipboard (C16). [onCheck] takes the note of 4.6 for a cash reversal the
 * file gives none, worded here (D9).
 */
class RestoreActions(
    val onBack: () -> Unit,
    val onChooseFile: () -> Unit,
    val onCheck: ((CashType, LocalDate) -> String) -> Unit,
    val onCancelCheck: () -> Unit,
    val onRetry: () -> Unit,
    val onCopyReport: (String) -> Unit,
    val onChooseAnother: () -> Unit,
    val onClose: () -> Unit,
)

/**
 * The restore wizard (section 6, Backup export and restore entry; section 8; wireframe 10e frames C to F; 33b), drawn
 * from its model (CR2): step 1, step 2, the report or the summary, one screen each. Everything is on the screen, which
 * scrolls; no part stays in place, and there is no layer and no text field. Back returns one step, and leaves from step
 * 1. Nothing is changed in the app on any of these screens (C3).
 */
@Composable
fun RestoreScreen(
    model: RestoreModel,
    sourceName: String,
    actions: RestoreActions,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = model.backEnabled, onBack = actions.onBack)
    // Configuration-aware, so the sentences follow a change of language or font while the screen shows (lint).
    val resources = LocalResources.current
    // The report as a lazy list, so a file with thousands of errors draws only what shows (the Deploy review's finding 2).
    val report = model.report
    if (model.step == RestoreStep.REPORT && report != null) {
        ReportList(report, model.backEnabled, actions, resources, modifier)
        return
    }
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TitleRow(stringResource(R.string.restore_title), actions.onBack, enabled = model.backEnabled)
        when (model.step) {
            RestoreStep.CHOOSE -> ChooseStep(model, actions, reversalNote(resources))
            RestoreStep.CHECKING -> CheckingStep(model, sourceName, actions, resources)
            RestoreStep.REPORT -> Unit
            RestoreStep.SUMMARY -> model.summary?.let { SummaryStep(it, actions) }
            RestoreStep.RESTORING, RestoreStep.RESULT, RestoreStep.FAILED -> Unit
        }
    }
}

// Frame C: Choose file, the file's name, Check file, and the date format help with section 6's date note.
@Composable
private fun ChooseStep(
    model: RestoreModel,
    actions: RestoreActions,
    note: (CashType, LocalDate) -> String,
) {
    BodyLine(stringResource(R.string.restore_step1))
    OutlinedButton(onClick = actions.onChooseFile, enabled = model.canChoose) {
        Text(text = stringResource(R.string.restore_choose_file))
    }
    BodyLine(model.fileName?.let { stringResource(R.string.restore_file_name, it) } ?: stringResource(R.string.restore_no_file))
    Button(onClick = { actions.onCheck(note) }, enabled = model.canCheck) {
        Text(text = stringResource(R.string.restore_check_file))
    }
    BodyLine(stringResource(R.string.restore_date_help))
    QuietLine(stringResource(R.string.date_note))
}

// Frame D: the two phases, the source line, a problem of the source with Try again (decision 3), and Cancel.
@Composable
private fun CheckingStep(
    model: RestoreModel,
    sourceName: String,
    actions: RestoreActions,
    resources: Resources,
) {
    BodyLine(stringResource(R.string.restore_step2))
    Text(
        text = stringResource(if (model.readingDone) R.string.restore_reading_done else R.string.restore_reading),
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
    Text(
        text = model.progress?.let { say(resources, it) } ?: stringResource(R.string.restore_verify_waiting),
        style = MaterialTheme.typography.bodyLarge,
    )
    QuietLine(stringResource(R.string.restore_source_line, sourceName))
    model.sourceProblem?.let { problem ->
        Text(
            text = say(resources, problem),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        Button(onClick = actions.onRetry) {
            Text(text = stringResource(R.string.restore_try_again))
        }
    }
    OutlinedButton(onClick = actions.onCancelCheck) {
        Text(text = stringResource(R.string.cancel_button))
    }
}

// Frame E: the title, "N errors in M lines", then each error as "<place>: <reason>", the text Copy report copies (C16),
// one lazy list that scrolls as one; only the errors in view are drawn.
@Composable
private fun ReportList(
    report: RestoreReportModel,
    backEnabled: Boolean,
    actions: RestoreActions,
    resources: Resources,
    modifier: Modifier,
) {
    val say: (RestoreWords) -> String = { say(resources, it) }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { TitleRow(stringResource(R.string.restore_title), actions.onBack, enabled = backEnabled) }
        item {
            Text(
                text = say(report.count),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics {
                    heading()
                    liveRegion = LiveRegionMode.Polite
                },
            )
        }
        items(report.lines) { line -> Text(text = Restores.lineText(line, say), style = MaterialTheme.typography.bodyLarge) }
        item { BodyLine(stringResource(R.string.restore_nothing_changed)) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { actions.onCopyReport(Restores.reportText(report, say)) }) {
                    Text(text = stringResource(R.string.restore_copy_report))
                }
                OutlinedButton(onClick = actions.onChooseAnother) {
                    Text(text = stringResource(R.string.restore_choose_another))
                }
            }
        }
    }
}

// Frame F, the file's column only (decision 1; frame F marker 15's fresh-install form), with no Continue until 34.
@Composable
private fun SummaryStep(
    summary: RestoreSummaryModel,
    actions: RestoreActions,
) {
    BodyLine(stringResource(R.string.restore_step3))
    Text(
        text = stringResource(R.string.restore_in_file),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { heading() },
    )
    SummaryRow(stringResource(R.string.restore_portfolios), summary.portfolios)
    SummaryRow(stringResource(R.string.restore_transactions), summary.transactions)
    SummaryRow(stringResource(R.string.restore_cash_entries), summary.cashEntries)
    SummaryRow(stringResource(R.string.restore_cash_after), summary.cashBalance)
    SummaryRow(stringResource(R.string.restore_holdings), summary.holdingsWithShares)
    BodyLine(stringResource(R.string.restore_summary_note))
    OutlinedButton(onClick = actions.onClose) {
        Text(text = stringResource(R.string.cancel_button))
    }
}

// A label and its figure, read by TalkBack as one: "Portfolios, 2" (AC-61 (d)).
@Composable
private fun SummaryRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f).padding(end = 12.dp))
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun BodyLine(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyLarge)
}

@Composable
private fun QuietLine(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

// The note of 4.6 for a cash reversal the file gives none, as the app words it on Cash entry detail (05).
@Composable
private fun reversalNote(resources: Resources): (CashType, LocalDate) -> String {
    val types = CashType.entries.associateWith { cashTypeLower(it) }
    return { type, date -> resources.getString(R.string.reversal_note_default, types.getValue(type), date.toString()) }
}

/** An approved sentence with its values put in (D9). */
internal fun say(
    resources: Resources,
    words: RestoreWords,
): String = resources.getString(stringOf(words.text), *words.args.toTypedArray())

/** Each sentence's approved string (33b section 12). */
internal fun stringOf(text: RestoreText): Int = when (text) {
    RestoreText.ERRORS_IN_LINES -> R.string.restore_errors_in_lines
    RestoreText.ERROR_IN_LINE -> R.string.restore_error_in_line
    RestoreText.ERRORS_IN_LINE -> R.string.restore_errors_in_line
    RestoreText.ERROR_IN_FILE -> R.string.restore_error_in_file
    RestoreText.ERRORS_IN_FILE -> R.string.restore_errors_in_file
    RestoreText.TICKER_ERROR -> R.string.restore_ticker_error
    RestoreText.TICKER_ERRORS -> R.string.restore_ticker_errors
    RestoreText.PLACE_FILE -> R.string.restore_place_file
    RestoreText.PLACE_LINE -> R.string.restore_place_line
    RestoreText.PLACE_LINE_COLUMN -> R.string.restore_place_line_column
    RestoreText.PLACE_TICKER -> R.string.restore_place_ticker
    RestoreText.PLACE_LINE_TICKER -> R.string.restore_place_line_ticker
    RestoreText.REPORT_LINE -> R.string.restore_report_line
    RestoreText.NOT_UTF8 -> R.string.restore_not_utf8
    RestoreText.NO_HEADER -> R.string.restore_no_header
    RestoreText.MISSING_COLUMN -> R.string.restore_missing_column
    RestoreText.UNKNOWN_COLUMN -> R.string.restore_unknown_column
    RestoreText.UNNAMED_COLUMN -> R.string.restore_unnamed_column
    RestoreText.REPEATED_COLUMN -> R.string.restore_repeated_column
    RestoreText.NO_FORMAT_ROW -> R.string.restore_no_format_row
    RestoreText.VERSION_NOT_WHOLE -> R.string.restore_version_not_whole
    RestoreText.VERSION_UNKNOWN -> R.string.restore_version_unknown
    RestoreText.CELL_COUNT -> R.string.restore_cell_count
    RestoreText.LINE_BREAK_IN_CELL -> R.string.restore_line_break_in_cell
    RestoreText.ROW_TYPE_MISSING -> R.string.restore_row_type_missing
    RestoreText.ROW_TYPE_UNKNOWN -> R.string.restore_row_type_unknown
    RestoreText.FORMAT_NOT_FIRST -> R.string.restore_format_not_first
    RestoreText.VALUE_MISSING -> R.string.restore_value_missing
    RestoreText.CELL_NOT_USED -> R.string.restore_cell_not_used
    RestoreText.DATE_FORM -> R.string.restore_date_form
    RestoreText.DATE_NOT_REAL -> R.string.restore_date_not_real
    RestoreText.NUMBER_FORM -> R.string.restore_number_form
    RestoreText.QUANTITY_NOT_POSITIVE -> R.string.restore_quantity_not_positive
    RestoreText.QUANTITY_TOO_MANY_DECIMALS -> R.string.restore_quantity_too_many_decimals
    RestoreText.AMOUNT_TOO_MANY_DECIMALS -> R.string.restore_amount_too_many_decimals
    RestoreText.AMOUNT_TOO_LARGE -> R.string.restore_amount_too_large
    RestoreText.WRONG_SIGN -> R.string.restore_wrong_sign
    RestoreText.CASH_AMOUNT_ZERO -> R.string.restore_cash_amount_zero
    RestoreText.BUY_TOTAL_ZERO -> R.string.restore_buy_total_zero
    RestoreText.ID_BADLY_FORMED -> R.string.restore_id_badly_formed
    RestoreText.ID_DUPLICATE -> R.string.restore_id_duplicate
    RestoreText.PORTFOLIO_NAME_TOO_SHORT -> R.string.restore_portfolio_name_too_short
    RestoreText.PORTFOLIO_NAME_NO_LETTER -> R.string.restore_portfolio_name_no_letter
    RestoreText.PORTFOLIO_NAME_DUPLICATE -> R.string.restore_portfolio_name_duplicate
    RestoreText.PORTFOLIO_NOT_FOUND -> R.string.restore_portfolio_not_found
    RestoreText.CASH_TYPE_UNKNOWN -> R.string.restore_cash_type_unknown
    RestoreText.CASH_TYPE_TRADE -> R.string.restore_cash_type_trade
    RestoreText.ADJUSTMENT_WITHOUT_NOTE -> R.string.restore_adjustment_without_note
    RestoreText.NO_OPENING_BALANCE -> R.string.restore_no_opening_balance
    RestoreText.SECOND_OPENING_BALANCE -> R.string.restore_second_opening_balance
    RestoreText.OPENING_BALANCE_NOT_EARLIEST -> R.string.restore_opening_balance_not_earliest
    RestoreText.TICKER_BAD_CHARACTERS -> R.string.restore_ticker_bad_characters
    RestoreText.SPLIT_NUMBER_NOT_WHOLE -> R.string.restore_split_number_not_whole
    RestoreText.SPLIT_NUMBERS_EQUAL -> R.string.restore_split_numbers_equal
    RestoreText.SPLIT_NUMBER_TOO_LARGE -> R.string.restore_split_number_too_large
    RestoreText.BUY_RATIO_TOO_LARGE -> R.string.restore_buy_ratio_too_large
    RestoreText.PICKS_BADLY_WRITTEN -> R.string.restore_picks_badly_written
    RestoreText.PICK_NOT_FOUND -> R.string.restore_pick_not_found
    RestoreText.PICK_NOT_A_BUY -> R.string.restore_pick_not_a_buy
    RestoreText.PICK_OTHER_PORTFOLIO -> R.string.restore_pick_other_portfolio
    RestoreText.PICK_OTHER_TICKER -> R.string.restore_pick_other_ticker
    RestoreText.PICK_TWICE -> R.string.restore_pick_twice
    RestoreText.LOT_BOUGHT_AFTER_SELL -> R.string.restore_lot_bought_after_sell
    RestoreText.LOT_ENTERED_AFTER_SELL -> R.string.restore_lot_entered_after_sell
    RestoreText.PICK_OVER_LOT -> R.string.restore_pick_over_lot
    RestoreText.PICKS_DO_NOT_ADD_UP -> R.string.restore_picks_do_not_add_up
    RestoreText.REVERSAL_TARGET_NOT_FOUND -> R.string.restore_reversal_target_not_found
    RestoreText.REVERSAL_OF_OPENING_BALANCE -> R.string.restore_reversal_of_opening_balance
    RestoreText.REVERSAL_TARGET_LISTED_AFTER -> R.string.restore_reversal_target_listed_after
    RestoreText.ALREADY_REVERSED -> R.string.restore_already_reversed
    RestoreText.REVERSAL_BEFORE_ORIGINAL -> R.string.restore_reversal_before_original
    RestoreText.HOLDING_NOT_FOUND -> R.string.restore_holding_not_found
    RestoreText.HIDDEN_HOLDING_HAS_SHARES -> R.string.restore_hidden_holding_has_shares
    RestoreText.OVERRIDE_BOTH_EMPTY -> R.string.restore_override_both_empty
    RestoreText.OVERRIDE_VALUE_NOT_ON_LIST -> R.string.restore_override_value_not_on_list
    RestoreText.LIST_UNKNOWN -> R.string.restore_list_unknown
    RestoreText.DUPLICATE_LIST_VALUE -> R.string.restore_duplicate_list_value
    RestoreText.DUPLICATE_REMEMBERED_TEXT -> R.string.restore_duplicate_remembered_text
    RestoreText.DUPLICATE_HIDDEN -> R.string.restore_duplicate_hidden
    RestoreText.DUPLICATE_OVERRIDE -> R.string.restore_duplicate_override
    RestoreText.LIST_VALUE_MATCHES_REMEMBERED_TEXT -> R.string.restore_list_value_matches_remembered_text
    RestoreText.REMEMBERED_TEXT_VALUE_NOT_ON_LIST -> R.string.restore_remembered_text_value_not_on_list
    RestoreText.SETTING_KEY_UNKNOWN -> R.string.restore_setting_key_unknown
    RestoreText.SETTING_VALUE_UNKNOWN -> R.string.restore_setting_value_unknown
    RestoreText.SETTING_KEY_TWICE -> R.string.restore_setting_key_twice
    RestoreText.TAB_ORDER_NOT_EACH_ONCE -> R.string.restore_tab_order_not_each_once
    RestoreText.CASH_NEGATIVE -> R.string.restore_cash_negative
    RestoreText.SHARES_NEGATIVE -> R.string.restore_shares_negative
    RestoreText.SELL_MORE_THAN_HELD -> R.string.restore_sell_more_than_held
    RestoreText.DATED_BEFORE_OPENING_BALANCE -> R.string.restore_dated_before_opening_balance
    RestoreText.SPLIT_TICKER_NOT_BOUGHT -> R.string.restore_split_ticker_not_bought
    RestoreText.BALANCE_TOO_LARGE -> R.string.restore_balance_too_large
    RestoreText.DEPENDS_ON -> R.string.restore_depends_on
    RestoreText.FILE_TOO_LARGE -> R.string.restore_file_too_large
    RestoreText.FILE_NOT_READ -> R.string.restore_file_not_read
    RestoreText.FILE_CHECK_FAILED -> R.string.restore_file_check_failed
    RestoreText.TICKER_NOT_FOUND -> R.string.restore_ticker_not_found
    RestoreText.TICKER_MUTUAL_FUND -> R.string.restore_ticker_mutual_fund
    RestoreText.TICKER_NOT_US_EXCHANGE -> R.string.restore_ticker_not_us_exchange
    RestoreText.TICKER_NOT_USD -> R.string.restore_ticker_not_usd
    RestoreText.VERIFY_PROGRESS -> R.string.restore_verify_progress
    RestoreText.SOURCE_OFFLINE -> R.string.restore_source_offline
    RestoreText.SOURCE_NO_KEY -> R.string.restore_source_no_key
    RestoreText.SOURCE_NO_ANSWER -> R.string.restore_source_no_answer
    RestoreText.SOURCE_KEY_REFUSED -> R.string.restore_source_key_refused
    RestoreText.SOURCE_TOO_MANY_CALLS -> R.string.restore_source_too_many_calls
    RestoreText.SOURCE_UNREADABLE -> R.string.restore_source_unreadable
}
