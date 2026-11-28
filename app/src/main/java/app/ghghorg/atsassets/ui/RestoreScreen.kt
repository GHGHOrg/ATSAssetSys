package app.ghghorg.atsassets.ui

import android.content.res.Resources
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.ui.design.AtsBlockedMessage
import app.ghghorg.atsassets.ui.design.AtsButton
import app.ghghorg.atsassets.ui.design.AtsCard
import app.ghghorg.atsassets.ui.design.AtsDoneHeading
import app.ghghorg.atsassets.ui.design.AtsEntryFoot
import app.ghghorg.atsassets.ui.design.AtsProgressRow
import app.ghghorg.atsassets.ui.design.AtsRowDivider
import app.ghghorg.atsassets.ui.design.AtsScreen
import app.ghghorg.atsassets.ui.design.AtsTextField
import app.ghghorg.atsassets.ui.design.AtsValueRow
import app.ghghorg.atsassets.ui.design.AtsWarningLine
import app.ghghorg.atsassets.ui.design.ButtonKind
import app.ghghorg.atsassets.ui.design.ButtonSize
import app.ghghorg.atsassets.ui.theme.AtsIcon
import app.ghghorg.atsassets.ui.theme.AtsTheme
import app.ghghorg.atsassets.ui.theme.LucideIcon
import app.ghghorg.atsassets.ui.theme.tokenDp
import app.ghghorg.atsassets.ui.theme.typeStyle
import java.time.LocalDate

/**
 * The wizard's callbacks, passed together. [onChooseFile] and [onCopyReport] are MainActivity's: Android's file screen
 * with the lock's exemption (AC-57), and the clipboard (C16). [onCheck] takes the note of 4.6 for a cash reversal the
 * file gives none, worded here (D9). 34: [onRestore] is the summary's Restore on an app with no data, and [onGoToOverall]
 * the result's button. 35: [onContinue] is the summary's Continue on an app that holds data; on step 4, [onType] takes
 * the word as typed, [onBackUpNow] is MainActivity's export (Android's file screen with the lock's exemption, AC-57), and
 * [onRestore] is Replace all data.
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
    val onRestore: () -> Unit = {},
    val onGoToOverall: () -> Unit = {},
    val onContinue: () -> Unit = {},
    val onType: (String) -> Unit = {},
    val onBackUpNow: () -> Unit = {},
)

/**
 * The restore wizard (section 6, Backup export and restore entry; section 8; wireframe 10e frames C to I; 33b), drawn
 * from its model (CR2), in the design system (44a step 8 group (e); drawing spec/wireframes/design/10e-backup-restore.html):
 * under the title band, one screen per step, its step line first in condensed 14 medium (note 1); the step's main action
 * the large gold foot, Cancel a large text button under it (note 8). Everything under the band scrolls; there is no
 * layer. 35: step 4 (frame G) has the one text field, the typed word; the screen keeps clear of the keyboard and scrolls
 * the field into view. It lies above the bottom bar and the navigation strip, which the keyboard covers too, so it first
 * consumes that height and pads only by the rest of the keyboard: its room ends at the keyboard's top (phone check 12,
 * decision 4; as 29 step 10's Delete dialog). Back returns one step, and leaves from step 1. Nothing is changed in the
 * app before Restore or Replace all data (C3).
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
    AtsScreen(
        title = stringResource(R.string.restore_title),
        modifier = modifier,
        onBack = actions.onBack,
        backLabel = stringResource(R.string.back_button),
        backEnabled = model.backEnabled,
    ) {
        // The report as a lazy list, so a file with thousands of errors draws only what shows (the Deploy review's finding 2).
        val report = model.report
        if (model.step == RestoreStep.REPORT && report != null) {
            ReportList(report, actions, resources)
            return@AtsScreen
        }
        // How far the screen's bottom lies above the window's: the bottom bar and the navigation strip (decision 4).
        val window = LocalView.current.rootView
        val density = LocalDensity.current
        var below by remember { mutableStateOf(0.dp) }
        Column(
            modifier =
            Modifier
                .fillMaxSize()
                .onGloballyPositioned { placed ->
                    below = with(density) { (window.height - placed.boundsInWindow().bottom).coerceAtLeast(0f).toDp() }
                }.consumeWindowInsets(PaddingValues(bottom = below))
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(tokenDp("spaceMargin")),
            verticalArrangement = Arrangement.spacedBy(tokenDp("spaceGap")),
        ) {
            when (model.step) {
                RestoreStep.CHOOSE -> ChooseStep(model, actions, reversalNote(resources))
                RestoreStep.CHECKING -> CheckingStep(model, sourceName, actions, resources)
                RestoreStep.REPORT -> Unit
                RestoreStep.SUMMARY -> model.summary?.let { SummaryStep(it, model, actions) }
                RestoreStep.CONFIRM -> model.confirm?.let { ConfirmStep(it, model, actions) }
                RestoreStep.RESTORING -> RestoringStep(model.totalSteps)
                RestoreStep.RESULT -> model.loaded?.let { ResultStep(it, actions) }
                RestoreStep.FAILED -> FailedStep(actions)
            }
        }
    }
}

// The step line under the band (note 1): "Step 1 of 5. Nothing is changed until step 5." in condensed 14 medium.
@Composable
private fun StepLine(text: String) {
    Text(text = text, style = typeStyle("columnHead"), color = AtsTheme.colors.textSecondary)
}

// Frame C: Choose file an outline button and the file's name as a row, the date format help with section 6's date note,
// then Check file the large gold foot.
@Composable
private fun ChooseStep(
    model: RestoreModel,
    actions: RestoreActions,
    note: (CashType, LocalDate) -> String,
) {
    StepLine(stringResource(R.string.restore_step1, model.totalSteps))
    AtsCard {
        AtsButton(stringResource(R.string.restore_choose_file), actions.onChooseFile, enabled = model.canChoose)
        BodyLine(model.fileName?.let { stringResource(R.string.restore_file_name, it) } ?: stringResource(R.string.restore_no_file))
        QuietLine(stringResource(R.string.restore_date_help))
        QuietLine(stringResource(R.string.date_note))
    }
    AtsEntryFoot(stringResource(R.string.restore_check_file), { actions.onCheck(note) }, enabled = model.canCheck)
}

// Frame D: each phase a 48 row with a check when done and loader-circle while it runs (D-35), the source line, a
// problem of the source as the blocked card with Try again (decision 3), and Cancel.
@Composable
private fun CheckingStep(
    model: RestoreModel,
    sourceName: String,
    actions: RestoreActions,
    resources: Resources,
) {
    StepLine(stringResource(R.string.restore_step2, model.totalSteps))
    AtsCard {
        AtsProgressRow(
            stringResource(if (model.readingDone) R.string.restore_reading_done else R.string.restore_reading),
            done = model.readingDone,
            modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        )
        AtsProgressRow(model.progress?.let { say(resources, it) } ?: stringResource(R.string.restore_verify_waiting), done = false)
        QuietLine(stringResource(R.string.restore_source_line, sourceName))
    }
    model.sourceProblem?.let { problem ->
        AtsBlockedMessage(say(resources, problem), prefix = null)
        AtsButton(stringResource(R.string.restore_try_again), actions.onRetry, kind = ButtonKind.FILLED, size = ButtonSize.LARGE, fillWidth = true)
    }
    AtsButton(stringResource(R.string.cancel_button), actions.onCancelCheck, kind = ButtonKind.TEXT, size = ButtonSize.LARGE, fillWidth = true)
}

// Frame E: the count as D-31's blocked card on top, read as a heading when it appears, with "Nothing was changed"; then
// each error as "<place>: <reason>", the text Copy report copies (C16), one row each in a card with dividers; Choose
// another file the gold foot, Copy report an outline button. One lazy list that scrolls as one; only the errors in view
// are drawn.
@Composable
private fun ReportList(
    report: RestoreReportModel,
    actions: RestoreActions,
    resources: Resources,
) {
    val say: (RestoreWords) -> String = { say(resources, it) }
    val colors = AtsTheme.colors
    val shape = RoundedCornerShape(tokenDp("radiusDefault"))
    val last = report.lines.lastIndex
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(tokenDp("spaceMargin")),
    ) {
        item { ReportHead(say(report.count)) }
        itemsIndexed(report.lines) { index, line ->
            Column(
                modifier = Modifier
                    .padding(top = if (index == 0) tokenDp("spaceGap") else 0.dp)
                    .fillMaxWidth()
                    .cardSlice(top = index == 0, bottom = index == last, shape, colors.card),
            ) {
                if (index > 0) AtsRowDivider()
                Text(
                    text = Restores.lineText(line, say),
                    style = typeStyle("small"),
                    color = colors.text,
                    modifier = Modifier.padding(tokenDp("spaceInset")),
                )
            }
        }
        item {
            Column(modifier = Modifier.padding(top = tokenDp("spaceGap")), verticalArrangement = Arrangement.spacedBy(tokenDp("spaceGap"))) {
                AtsButton(
                    stringResource(R.string.restore_choose_another),
                    actions.onChooseAnother,
                    kind = ButtonKind.FILLED,
                    size = ButtonSize.LARGE,
                    fillWidth = true,
                )
                AtsButton(
                    stringResource(R.string.restore_copy_report),
                    { actions.onCopyReport(Restores.reportText(report, say)) },
                    size = ButtonSize.LARGE,
                    fillWidth = true,
                )
            }
        }
    }
}

// The report's count in the blocked card (D-31): circle-alert, the count in medium bronze as a heading, read when it
// appears, and "Nothing was changed" under it.
@Composable
private fun ReportHead(count: String) {
    val colors = AtsTheme.colors
    BlockedFrame {
        Text(
            text = count,
            style = typeStyle("button"),
            color = colors.error,
            modifier = Modifier.semantics {
                heading()
                liveRegion = LiveRegionMode.Polite
            },
        )
        Text(text = stringResource(R.string.restore_nothing_changed), style = typeStyle("small"), color = colors.text)
    }
}

// D-31's blocked card around [content], as AtsBlockedMessage draws it: the 2 dp bronze outline and circle-alert.
@Composable
private fun BlockedFrame(content: @Composable () -> Unit) {
    val colors = AtsTheme.colors
    val shape = RoundedCornerShape(tokenDp("radiusDefault"))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.card)
            .border(tokenDp("layerBlockedOutline"), colors.error, shape)
            .padding(tokenDp("spaceInset")),
        horizontalArrangement = Arrangement.spacedBy(tokenDp("fieldDensePadding")),
    ) {
        AtsIcon(LucideIcon.CIRCLE_EXCLAMATION_POINT, tint = colors.error, size = tokenDp("fieldMarkSize"))
        Column(verticalArrangement = Arrangement.spacedBy(tokenDp("cardGap"))) { content() }
    }
}

// Frame F. On an app with no data, the file's column only (marker 15's fresh-install form), Restore and what it does
// (34). 35: on an app that holds data, the counts now beside the file's as two right-aligned columns under condensed
// heads (marker 13), the balance and holdings after the restore as label-and-value rows under a divider (marker 14), and
// Continue to step 4.
@Composable
private fun SummaryStep(
    summary: RestoreSummaryModel,
    model: RestoreModel,
    actions: RestoreActions,
) {
    StepLine(stringResource(R.string.restore_step3, model.totalSteps))
    val now = model.now
    AtsCard {
        if (now == null) {
            Text(
                text = stringResource(R.string.restore_in_file),
                style = typeStyle("columnHead"),
                color = AtsTheme.colors.textSecondary,
                modifier = Modifier.semantics { heading() },
            )
            AtsValueRow(stringResource(R.string.restore_portfolios), summary.portfolios)
            AtsValueRow(stringResource(R.string.restore_transactions), summary.transactions)
            AtsValueRow(stringResource(R.string.restore_cash_entries), summary.cashEntries)
        } else {
            CountsHeader()
            CountsRow(stringResource(R.string.restore_portfolios), now.portfolios, summary.portfolios)
            CountsRow(stringResource(R.string.restore_transactions), now.transactions, summary.transactions)
            CountsRow(stringResource(R.string.restore_cash_entries), now.cashEntries, summary.cashEntries)
        }
        AtsRowDivider()
        AtsValueRow(stringResource(R.string.restore_cash_after), summary.cashBalance)
        AtsValueRow(stringResource(R.string.restore_holdings), summary.holdingsWithShares)
    }
    if (model.canRestore) QuietLine(stringResource(R.string.restore_empty_note))
    if (model.canContinue) QuietLine(stringResource(R.string.restore_data_note))
    when {
        model.canRestore -> AtsEntryFoot(stringResource(R.string.restore_now), actions.onRestore, secondary = stringResource(R.string.cancel_button), onSecondary = actions.onClose, secondaryText = true)
        model.canContinue -> AtsEntryFoot(stringResource(R.string.restore_continue), actions.onContinue, secondary = stringResource(R.string.cancel_button), onSecondary = actions.onClose, secondaryText = true)
        else -> AtsButton(stringResource(R.string.cancel_button), actions.onClose, kind = ButtonKind.TEXT, size = ButtonSize.LARGE, fillWidth = true)
    }
}

// Marker 13's column heads, "Now" and "In the file", over the three counts, condensed; TalkBack reads each row whole instead.
@Composable
private fun CountsHeader() {
    Row(modifier = Modifier.fillMaxWidth().clearAndSetSemantics { }, horizontalArrangement = Arrangement.spacedBy(tokenDp("fieldDensePadding"))) {
        Spacer(modifier = Modifier.weight(COUNT_LABEL_WEIGHT))
        CountCell(stringResource(R.string.restore_now_column), typeStyle("columnHead"), head = true)
        CountCell(stringResource(R.string.restore_in_file), typeStyle("columnHead"), head = true)
    }
}

// A count now and in the file, read by TalkBack as one: "Portfolios: 1 now, 2 in the file" (AC-61 (d)).
@Composable
private fun CountsRow(
    label: String,
    now: String,
    inFile: String,
) {
    val spoken = stringResource(R.string.restore_count_row, label, now, inFile)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = spoken }
            .padding(vertical = tokenDp("cardGap")),
        horizontalArrangement = Arrangement.spacedBy(tokenDp("fieldDensePadding")),
    ) {
        Text(text = label, style = typeStyle("body"), color = AtsTheme.colors.textSecondary, modifier = Modifier.weight(COUNT_LABEL_WEIGHT))
        CountCell(now, typeStyle("body"))
        CountCell(inFile, typeStyle("body"))
    }
}

@Composable
private fun RowScope.CountCell(
    text: String,
    style: TextStyle,
    head: Boolean = false,
) {
    val colors = AtsTheme.colors
    Text(text = text, style = style, color = if (head) colors.textSecondary else colors.text, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
}

// The label column takes more room than each count, so a long label wraps before a count does.
private const val COUNT_LABEL_WEIGHT = 1.6f

// Frame G (step 4, an app that holds data): marker 16, the export offer as a card with an outline Back up now and its
// result line (31's sentences); marker 17, what Replace all data does as D-34's warning line, the field for the word,
// then Replace all data the gold foot (the catalog's disabled button until the word matches; no dashed outline) and
// Cancel a text button.
@Composable
private fun ConfirmStep(
    confirm: RestoreConfirmModel,
    model: RestoreModel,
    actions: RestoreActions,
) {
    StepLine(stringResource(R.string.restore_step4, model.totalSteps))
    AtsCard {
        Text(
            text = stringResource(R.string.restore_backup_first),
            style = typeStyle("sectionHeading"),
            color = AtsTheme.colors.text,
            modifier = Modifier.semantics { heading() },
        )
        QuietLine(stringResource(R.string.restore_backup_first_note))
        AtsButton(stringResource(R.string.backup_now), actions.onBackUpNow, enabled = confirm.canBackUp)
        BackupStatusLine(confirm.writing, noData = false, confirm.outcome, confirm.lastBackup)
    }
    AtsWarningLine(stringResource(R.string.restore_replace_note))
    AtsTextField(
        value = confirm.typed,
        onValueChange = actions.onType,
        label = stringResource(R.string.restore_type_word),
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions =
        KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false, imeAction = ImeAction.Done),
    )
    Column(verticalArrangement = Arrangement.spacedBy(tokenDp("spaceGap"))) {
        AtsButton(
            stringResource(R.string.restore_replace),
            actions.onRestore,
            kind = ButtonKind.FILLED,
            size = ButtonSize.LARGE,
            enabled = confirm.canReplace,
            fillWidth = true,
        )
        AtsButton(
            stringResource(R.string.cancel_button),
            actions.onClose,
            kind = ButtonKind.TEXT,
            size = ButtonSize.LARGE,
            enabled = model.backEnabled,
            fillWidth = true,
        )
    }
}

// The delete and load under way (section 8, step 7): loader-circle and one line, read by TalkBack when it appears;
// nothing to tap. 35: the last of four steps onto an empty app, of five over data.
@Composable
private fun RestoringStep(totalSteps: String) {
    AtsProgressRow(
        stringResource(R.string.restore_restoring, totalSteps),
        done = false,
        modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
    )
}

// Frame H, marker 18: circle-check beside the heading (D-35, never green), what was loaded as three rows, the "no price"
// and "never backed up" notes and the key (34 decision 1) in secondary text, Go to Overall the gold foot.
@Composable
private fun ResultStep(
    loaded: RestoreSummaryModel,
    actions: RestoreActions,
) {
    AtsDoneHeading(
        stringResource(R.string.restore_complete),
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
    BodyLine(stringResource(R.string.restore_loaded))
    AtsCard {
        AtsValueRow(stringResource(R.string.restore_portfolios), loaded.portfolios)
        AtsValueRow(stringResource(R.string.restore_transactions), loaded.transactions)
        AtsValueRow(stringResource(R.string.restore_cash_entries), loaded.cashEntries)
    }
    QuietLine(stringResource(R.string.restore_no_price_note))
    QuietLine(stringResource(R.string.restore_never_backed_up_note))
    QuietLine(stringResource(R.string.restore_key_note))
    AtsEntryFoot(stringResource(R.string.restore_go_overall), actions.onGoToOverall)
}

// Frame I, marker 19: the write failed and nothing was changed, as the blocked card; Close the one button at the foot.
@Composable
private fun FailedStep(actions: RestoreActions) {
    AtsBlockedMessage(stringResource(R.string.restore_failed), prefix = null)
    AtsEntryFoot(stringResource(R.string.restore_close), actions.onClose)
}

@Composable
private fun BodyLine(text: String) {
    Text(text = text, style = typeStyle("body"), color = AtsTheme.colors.text)
}

@Composable
private fun QuietLine(text: String) {
    Text(text = text, style = typeStyle("small"), color = AtsTheme.colors.textSecondary)
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
