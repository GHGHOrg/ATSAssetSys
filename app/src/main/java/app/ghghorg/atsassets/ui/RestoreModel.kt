package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.BackupFile
import app.ghghorg.atsassets.core.Money
import app.ghghorg.atsassets.core.Quantity
import app.ghghorg.atsassets.core.RestoreCounts
import app.ghghorg.atsassets.core.RestoreError
import app.ghghorg.atsassets.core.RestoreReason
import app.ghghorg.atsassets.core.RestoreSummary
import app.ghghorg.atsassets.core.Splits
import app.ghghorg.atsassets.core.TickerRefusal
import app.ghghorg.atsassets.prices.CheckRefusal
import java.math.BigDecimal
import java.util.Locale

/*
 * The restore wizard as the screens draw it (section 6, Backup export and restore entry; section 8, rejection reasons and
 * the report; wireframe 10e frames C to F; CR2, 33b). Plain Kotlin: each sentence is a key for the approved string (D9)
 * and the values put into it, worked out here; the screen only picks the string. The report's text, shown and copied, is
 * made in one place ([Restores.reportText]), so Copy report copies exactly what is shown (C16).
 */

/** Every sentence the wizard shows, one approved string each (D9; 33b section 12). */
enum class RestoreText {
    // The report's count line (section 8: "N errors in M lines") and the second round's.
    ERRORS_IN_LINES,
    ERROR_IN_LINE,
    ERRORS_IN_LINE,
    ERROR_IN_FILE,
    ERRORS_IN_FILE,
    TICKER_ERROR,
    TICKER_ERRORS,

    // Where an error is: the file as a whole, a line, a line and a column, a held ticker; and "<place>: <reason>".
    PLACE_FILE,
    PLACE_LINE,
    PLACE_LINE_COLUMN,
    PLACE_TICKER,
    PLACE_LINE_TICKER,
    REPORT_LINE,

    // The file as a whole (section 8, File).
    NOT_UTF8,
    NO_HEADER,
    MISSING_COLUMN,
    UNKNOWN_COLUMN,
    UNNAMED_COLUMN,
    REPEATED_COLUMN,
    NO_FORMAT_ROW,
    VERSION_NOT_WHOLE,
    VERSION_UNKNOWN,
    CELL_COUNT,
    LINE_BREAK_IN_CELL,

    // Every row, dates and numbers.
    ROW_TYPE_MISSING,
    ROW_TYPE_UNKNOWN,
    FORMAT_NOT_FIRST,
    VALUE_MISSING,
    CELL_NOT_USED,
    DATE_FORM,
    DATE_NOT_REAL,
    NUMBER_FORM,
    QUANTITY_NOT_POSITIVE,
    QUANTITY_TOO_MANY_DECIMALS,
    AMOUNT_TOO_MANY_DECIMALS,
    AMOUNT_TOO_LARGE,
    WRONG_SIGN,
    CASH_AMOUNT_ZERO,
    BUY_TOTAL_ZERO,

    // Ids and portfolios.
    ID_BADLY_FORMED,
    ID_DUPLICATE,
    PORTFOLIO_NAME_TOO_SHORT,
    PORTFOLIO_NAME_NO_LETTER,
    PORTFOLIO_NAME_DUPLICATE,
    PORTFOLIO_NOT_FOUND,

    // Cash, tickers and splits.
    CASH_TYPE_UNKNOWN,
    CASH_TYPE_TRADE,
    ADJUSTMENT_WITHOUT_NOTE,
    NO_OPENING_BALANCE,
    SECOND_OPENING_BALANCE,
    OPENING_BALANCE_NOT_EARLIEST,
    TICKER_BAD_CHARACTERS,
    SPLIT_NUMBER_NOT_WHOLE,
    SPLIT_NUMBERS_EQUAL,
    SPLIT_NUMBER_TOO_LARGE,
    BUY_RATIO_TOO_LARGE,

    // Sells and picks.
    PICKS_BADLY_WRITTEN,
    PICK_NOT_FOUND,
    PICK_NOT_A_BUY,
    PICK_OTHER_PORTFOLIO,
    PICK_OTHER_TICKER,
    PICK_TWICE,
    LOT_BOUGHT_AFTER_SELL,
    LOT_ENTERED_AFTER_SELL,
    PICK_OVER_LOT,
    PICKS_DO_NOT_ADD_UP,

    // Reversals.
    REVERSAL_TARGET_NOT_FOUND,
    REVERSAL_OF_OPENING_BALANCE,
    REVERSAL_TARGET_LISTED_AFTER,
    ALREADY_REVERSED,
    REVERSAL_BEFORE_ORIGINAL,

    // State rows, lists and settings.
    HOLDING_NOT_FOUND,
    HIDDEN_HOLDING_HAS_SHARES,
    OVERRIDE_BOTH_EMPTY,
    OVERRIDE_VALUE_NOT_ON_LIST,
    LIST_UNKNOWN,
    DUPLICATE_LIST_VALUE,
    DUPLICATE_REMEMBERED_TEXT,
    DUPLICATE_HIDDEN,
    DUPLICATE_OVERRIDE,
    LIST_VALUE_MATCHES_REMEMBERED_TEXT,
    REMEMBERED_TEXT_VALUE_NOT_ON_LIST,
    SETTING_KEY_UNKNOWN,
    SETTING_VALUE_UNKNOWN,
    SETTING_KEY_TWICE,
    TAB_ORDER_NOT_EACH_ONCE,

    // Rules (4.1 to 4.6) and the dependent rows.
    CASH_NEGATIVE,
    SHARES_NEGATIVE,
    SELL_MORE_THAN_HELD,
    DATED_BEFORE_OPENING_BALANCE,
    SPLIT_TICKER_NOT_BOUGHT,
    BALANCE_TOO_LARGE,
    DEPENDS_ON,

    // The file that gave no check (decision 4).
    FILE_TOO_LARGE,
    FILE_NOT_READ,
    FILE_CHECK_FAILED,

    // The second round: a held ticker the source refused (4.3), with the source's name.
    TICKER_NOT_FOUND,
    TICKER_MUTUAL_FUND,
    TICKER_NOT_US_EXCHANGE,
    TICKER_NOT_USD,

    // Step 2: the ticker round's progress, and a problem of the source that stops it (decision 3).
    VERIFY_PROGRESS,
    SOURCE_OFFLINE,
    SOURCE_NO_KEY,
    SOURCE_NO_ANSWER,
    SOURCE_KEY_REFUSED,
    SOURCE_TOO_MANY_CALLS,
    SOURCE_UNREADABLE,
}

/** One sentence: the approved string's key and the values put into it, in order. */
data class RestoreWords(
    val text: RestoreText,
    val args: List<String> = emptyList(),
)

/** One error of the report: where it is, and why. */
data class RestoreReportLine(
    val place: RestoreWords,
    val reason: RestoreWords,
)

/** The report (wireframe 10e frame E): the count line, then every error in report order. */
data class RestoreReportModel(
    val count: RestoreWords,
    val lines: List<RestoreReportLine>,
)

/** The summary's figures (section 8, restore step 5; wireframe 10e frame F, the file's column only; decision 1). */
data class RestoreSummaryModel(
    val portfolios: String,
    val transactions: String,
    val cashEntries: String,
    val cashBalance: String,
    val holdingsWithShares: String,
)

/** 35: the summary's Now column (wireframe 10e frame F, marker 13): what the app holds now, counted as the file is. */
data class RestoreNowModel(
    val portfolios: String,
    val transactions: String,
    val cashEntries: String,
)

/**
 * 35: step 4 (wireframe 10e frame G). [typed]: the word as typed. [canReplace]: Replace all data is active (RESTORE
 * typed, decision 2; no export under way, no save running). Marker 16's export offer: [canBackUp], Back up now active;
 * [writing], the file is being written; [outcome], how the export ended, with [lastBackup] for "Backup saved".
 */
data class RestoreConfirmModel(
    val typed: String,
    val canReplace: Boolean,
    val canBackUp: Boolean,
    val writing: Boolean,
    val outcome: BackupOutcome?,
    val lastBackup: LastBackup?,
)

/**
 * The wizard's screen. [step] picks the frame. Step 1: [fileName], null with none chosen; [canChoose] and [canCheck].
 * Step 2: [readingDone] once the first phase passed; [progress] during the ticker round; [sourceProblem] when the source
 * could not answer (decision 3), with Try again. [report] on the report, [summary] on the summary. [backEnabled]: "<" and
 * Android Back act; not while Android's file screen is open. 34: [totalSteps] is "4" on an app with no data (wireframe 10e
 * marker 15: no step 4) and "5" on one that holds data, to the restore's end; [canRestore]: the summary offers Restore
 * (an app with no data); [loaded] on the result, the file's counts as loaded. Back does nothing while the restore is
 * written or on its result. 35: on the summary of an app that holds data, [now] (the Now column) and [canContinue]
 * (Continue to step 4); on step 4, [confirm].
 */
data class RestoreModel(
    val step: RestoreStep,
    val fileName: String?,
    val canChoose: Boolean,
    val canCheck: Boolean,
    val readingDone: Boolean,
    val progress: RestoreWords?,
    val sourceProblem: RestoreWords?,
    val report: RestoreReportModel?,
    val summary: RestoreSummaryModel?,
    val backEnabled: Boolean,
    val totalSteps: String,
    val canRestore: Boolean,
    val loaded: RestoreSummaryModel?,
    val now: RestoreNowModel? = null,
    val canContinue: Boolean = false,
    val confirm: RestoreConfirmModel? = null,
)

object Restores {
    /**
     * The wizard as drawn; [sourceName] is the selected source's own name (K4), for the second round's sentences;
     * [holdsData] is section 3's "app holds data" (34: an app with none restores in four steps; 35: one with data in
     * five). For step 4's export offer (35): the export's [stage] and [outcome] (31), the [lastBackup] as shown, and
     * whether another save is running ([saving]).
     */
    fun screen(
        wizard: RestoreWizard,
        sourceName: String,
        holdsData: Boolean = true,
        stage: BackupStage? = null,
        outcome: BackupOutcome? = null,
        lastBackup: LastBackup? = null,
        saving: Boolean = false,
    ): RestoreModel = RestoreModel(
        step = wizard.step,
        fileName = wizard.fileName,
        canChoose = !wizard.choosing,
        canCheck = wizard.fileName != null && !wizard.choosing,
        readingDone = wizard.phase == CheckPhase.VERIFYING,
        progress =
        if (wizard.phase == CheckPhase.VERIFYING && wizard.toVerify > 0) {
            val current = minOf(wizard.verified + 1, wizard.toVerify)
            RestoreWords(RestoreText.VERIFY_PROGRESS, listOf(count(current), count(wizard.toVerify)))
        } else {
            null
        },
        sourceProblem = wizard.sourceProblem?.let(::sourceProblem),
        report = if (wizard.step == RestoreStep.REPORT) report(wizard, sourceName) else null,
        summary = if (wizard.step == RestoreStep.SUMMARY) wizard.summary?.let(::summary) else null,
        backEnabled =
        !wizard.choosing &&
            wizard.step != RestoreStep.RESTORING &&
            wizard.step != RestoreStep.RESULT &&
            !(wizard.step == RestoreStep.CONFIRM && stage != null),
        totalSteps = totalSteps(wizard, holdsData),
        canRestore = wizard.step == RestoreStep.SUMMARY && !holdsData,
        loaded = if (wizard.step == RestoreStep.RESULT) wizard.summary?.let(::summary) else null,
        now = if (wizard.step == RestoreStep.SUMMARY && holdsData) wizard.now?.let(::now) else null,
        canContinue = wizard.step == RestoreStep.SUMMARY && holdsData && wizard.now != null,
        confirm =
        if (wizard.step == RestoreStep.CONFIRM) {
            RestoreConfirmModel(
                typed = wizard.typed,
                canReplace = wizard.now != null && RestoreWord.matches(wizard.typed) && stage == null && !saving,
                canBackUp = stage == null && !saving,
                writing = stage == BackupStage.WRITING,
                outcome = if (stage == null) outcome else null,
                lastBackup = lastBackup,
            )
        } else {
            null
        },
    )

    // From the restore's write on, the app holds the file's data, but the count stays that of the restore that led here:
    // five over data (the wizard counted what the app held), four onto an empty app.
    private fun totalSteps(
        wizard: RestoreWizard,
        holdsData: Boolean,
    ): String {
        val overData = if (wizard.step in AFTER_RESTORE) wizard.now != null else holdsData
        return if (overData) "5" else "4"
    }

    /** The Now column (35): each count as the app shows whole numbers. */
    fun now(counts: RestoreCounts): RestoreNowModel = RestoreNowModel(
        portfolios = count(counts.portfolios),
        transactions = count(counts.transactions),
        cashEntries = count(counts.cashEntries),
    )

    private val AFTER_RESTORE = setOf(RestoreStep.RESTORING, RestoreStep.RESULT, RestoreStep.FAILED)

    /** The report: a file problem alone, or the second round's tickers, or the first round's errors. */
    fun report(
        wizard: RestoreWizard,
        sourceName: String,
    ): RestoreReportModel {
        wizard.fileProblem?.let { problem ->
            return RestoreReportModel(RestoreWords(RestoreText.ERROR_IN_FILE), listOf(RestoreReportLine(file(), fileProblem(problem))))
        }
        if (wizard.tickerErrors.isNotEmpty()) {
            val n = wizard.tickerErrors.size
            val countLine = if (n == 1) words(RestoreText.TICKER_ERROR) else words(RestoreText.TICKER_ERRORS, count(n))
            val lines = wizard.tickerErrors.map { RestoreReportLine(tickerPlace(it), tickerReason(it.reason, sourceName)) }
            return RestoreReportModel(countLine, lines)
        }
        val errors = wizard.errors
        return RestoreReportModel(countLine(errors.size, errors.mapNotNull { it.line }.distinct().size), errors.map(::line))
    }

    /** "N errors in M lines" (section 8), in its singular forms where they apply; the file as a whole is no line. */
    fun countLine(
        errors: Int,
        lines: Int,
    ): RestoreWords = when {
        lines == 0 && errors == 1 -> RestoreWords(RestoreText.ERROR_IN_FILE)
        lines == 0 -> RestoreWords(RestoreText.ERRORS_IN_FILE, listOf(count(errors)))
        lines == 1 && errors == 1 -> RestoreWords(RestoreText.ERROR_IN_LINE)
        lines == 1 -> RestoreWords(RestoreText.ERRORS_IN_LINE, listOf(count(errors)))
        else -> RestoreWords(RestoreText.ERRORS_IN_LINES, listOf(count(errors), count(lines)))
    }

    /** One error's place (the header is line 1; its column when one applies) and reason. */
    fun line(error: RestoreError): RestoreReportLine {
        val line = error.line
        val column = error.column
        val place =
            when {
                line == null -> file()
                column == null -> RestoreWords(RestoreText.PLACE_LINE, listOf(count(line)))
                else -> RestoreWords(RestoreText.PLACE_LINE_COLUMN, listOf(count(line), column))
            }
        return RestoreReportLine(place, reason(error.reason))
    }

    /** Each reason of section 8, and 33a's additions, as its sentence and values (risk 1: one per reason). */
    fun reason(reason: RestoreReason): RestoreWords = when (reason) {
        RestoreReason.NotUtf8 -> words(RestoreText.NOT_UTF8)
        RestoreReason.NoHeader -> words(RestoreText.NO_HEADER)
        is RestoreReason.MissingColumn -> words(RestoreText.MISSING_COLUMN, reason.column)
        is RestoreReason.UnknownColumn -> unknownColumn(reason.column)
        is RestoreReason.RepeatedColumn -> words(RestoreText.REPEATED_COLUMN, reason.column)
        RestoreReason.NoFormatRow -> words(RestoreText.NO_FORMAT_ROW)
        RestoreReason.VersionNotWhole -> words(RestoreText.VERSION_NOT_WHOLE)
        is RestoreReason.VersionUnknown -> words(RestoreText.VERSION_UNKNOWN, reason.version, BackupFile.FORMAT_VERSION.toString())
        is RestoreReason.CellCount -> words(RestoreText.CELL_COUNT, count(reason.found), count(reason.expected))
        RestoreReason.LineBreakInCell -> words(RestoreText.LINE_BREAK_IN_CELL)
        RestoreReason.RowTypeMissing -> words(RestoreText.ROW_TYPE_MISSING)
        is RestoreReason.RowTypeUnknown -> words(RestoreText.ROW_TYPE_UNKNOWN, reason.rowType)
        RestoreReason.FormatNotFirst -> words(RestoreText.FORMAT_NOT_FIRST)
        RestoreReason.ValueMissing -> words(RestoreText.VALUE_MISSING)
        RestoreReason.CellNotUsed -> words(RestoreText.CELL_NOT_USED)
        RestoreReason.DateForm -> words(RestoreText.DATE_FORM)
        RestoreReason.DateNotReal -> words(RestoreText.DATE_NOT_REAL)
        RestoreReason.NumberForm -> words(RestoreText.NUMBER_FORM)
        RestoreReason.QuantityNotPositive -> words(RestoreText.QUANTITY_NOT_POSITIVE)
        RestoreReason.QuantityTooManyDecimals -> words(RestoreText.QUANTITY_TOO_MANY_DECIMALS)
        RestoreReason.AmountTooManyDecimals -> words(RestoreText.AMOUNT_TOO_MANY_DECIMALS)
        RestoreReason.AmountTooLarge -> words(RestoreText.AMOUNT_TOO_LARGE)
        RestoreReason.WrongSign -> words(RestoreText.WRONG_SIGN)
        RestoreReason.CashAmountZero -> words(RestoreText.CASH_AMOUNT_ZERO)
        RestoreReason.BuyTotalZero -> words(RestoreText.BUY_TOTAL_ZERO)
        RestoreReason.IdBadlyFormed -> words(RestoreText.ID_BADLY_FORMED)
        RestoreReason.PortfolioNameTooShort -> words(RestoreText.PORTFOLIO_NAME_TOO_SHORT)
        RestoreReason.PortfolioNameNoLetter -> words(RestoreText.PORTFOLIO_NAME_NO_LETTER)
        RestoreReason.CashTypeUnknown -> words(RestoreText.CASH_TYPE_UNKNOWN)
        RestoreReason.CashTypeTrade -> words(RestoreText.CASH_TYPE_TRADE)
        RestoreReason.AdjustmentWithoutNote -> words(RestoreText.ADJUSTMENT_WITHOUT_NOTE)
        RestoreReason.TickerBadCharacters -> words(RestoreText.TICKER_BAD_CHARACTERS)
        RestoreReason.SplitNumberNotWhole -> words(RestoreText.SPLIT_NUMBER_NOT_WHOLE)
        RestoreReason.SplitNumbersEqual -> words(RestoreText.SPLIT_NUMBERS_EQUAL)
        RestoreReason.SplitNumberTooLarge -> words(RestoreText.SPLIT_NUMBER_TOO_LARGE, count(Splits.MAX_RATIO))
        RestoreReason.BuyRatioTooLarge -> words(RestoreText.BUY_RATIO_TOO_LARGE)
        RestoreReason.PicksBadlyWritten -> words(RestoreText.PICKS_BADLY_WRITTEN)
        RestoreReason.OverrideBothEmpty -> words(RestoreText.OVERRIDE_BOTH_EMPTY)
        RestoreReason.ListUnknown -> words(RestoreText.LIST_UNKNOWN)
        RestoreReason.SettingKeyUnknown -> words(RestoreText.SETTING_KEY_UNKNOWN)
        RestoreReason.SettingValueUnknown -> words(RestoreText.SETTING_VALUE_UNKNOWN)
        RestoreReason.TabOrderNotEachOnce -> words(RestoreText.TAB_ORDER_NOT_EACH_ONCE)
        is RestoreReason.IdDuplicate -> words(RestoreText.ID_DUPLICATE, count(reason.line))
        is RestoreReason.PortfolioNameDuplicate -> words(RestoreText.PORTFOLIO_NAME_DUPLICATE, count(reason.line))
        RestoreReason.PortfolioNotFound -> words(RestoreText.PORTFOLIO_NOT_FOUND)
        RestoreReason.NoOpeningBalance -> words(RestoreText.NO_OPENING_BALANCE)
        is RestoreReason.SecondOpeningBalance -> words(RestoreText.SECOND_OPENING_BALANCE, count(reason.line))
        RestoreReason.OpeningBalanceNotEarliest -> words(RestoreText.OPENING_BALANCE_NOT_EARLIEST)
        is RestoreReason.PickNotFound -> words(RestoreText.PICK_NOT_FOUND, reason.id)
        is RestoreReason.PickNotABuy -> words(RestoreText.PICK_NOT_A_BUY, reason.id)
        is RestoreReason.PickOtherPortfolio -> words(RestoreText.PICK_OTHER_PORTFOLIO, reason.id)
        is RestoreReason.PickOtherTicker -> words(RestoreText.PICK_OTHER_TICKER, reason.id)
        is RestoreReason.PickTwice -> words(RestoreText.PICK_TWICE, reason.id)
        is RestoreReason.LotBoughtAfterSell -> words(RestoreText.LOT_BOUGHT_AFTER_SELL, reason.id)
        is RestoreReason.LotEnteredAfterSell -> words(RestoreText.LOT_ENTERED_AFTER_SELL, reason.id)
        is RestoreReason.PickOverLot -> words(RestoreText.PICK_OVER_LOT, reason.id, shares(reason.remainder))
        is RestoreReason.PicksDoNotAddUp -> words(RestoreText.PICKS_DO_NOT_ADD_UP, shares(reason.picked), shares(reason.quantity))
        is RestoreReason.ReversalTargetNotFound -> words(RestoreText.REVERSAL_TARGET_NOT_FOUND, reason.id)
        RestoreReason.ReversalOfOpeningBalance -> words(RestoreText.REVERSAL_OF_OPENING_BALANCE)
        is RestoreReason.ReversalTargetListedAfter -> words(RestoreText.REVERSAL_TARGET_LISTED_AFTER, count(reason.line))
        is RestoreReason.AlreadyReversed -> words(RestoreText.ALREADY_REVERSED, count(reason.line))
        is RestoreReason.ReversalBeforeOriginal -> words(RestoreText.REVERSAL_BEFORE_ORIGINAL, reason.originalDate.toString())
        RestoreReason.HiddenHoldingNotFound -> words(RestoreText.HOLDING_NOT_FOUND)
        RestoreReason.HiddenHoldingHasShares -> words(RestoreText.HIDDEN_HOLDING_HAS_SHARES)
        RestoreReason.OverrideHoldingNotFound -> words(RestoreText.HOLDING_NOT_FOUND)
        RestoreReason.OverrideValueNotOnList -> words(RestoreText.OVERRIDE_VALUE_NOT_ON_LIST)
        is RestoreReason.DuplicateListValue -> words(RestoreText.DUPLICATE_LIST_VALUE, count(reason.line))
        is RestoreReason.DuplicateRememberedText -> words(RestoreText.DUPLICATE_REMEMBERED_TEXT, count(reason.line))
        is RestoreReason.DuplicateHidden -> words(RestoreText.DUPLICATE_HIDDEN, count(reason.line))
        is RestoreReason.DuplicateOverride -> words(RestoreText.DUPLICATE_OVERRIDE, count(reason.line))
        is RestoreReason.ListValueMatchesRememberedText -> words(RestoreText.LIST_VALUE_MATCHES_REMEMBERED_TEXT, count(reason.line))
        RestoreReason.RememberedTextValueNotOnList -> words(RestoreText.REMEMBERED_TEXT_VALUE_NOT_ON_LIST)
        is RestoreReason.SettingKeyTwice -> words(RestoreText.SETTING_KEY_TWICE, count(reason.line))
        is RestoreReason.CashNegative -> words(RestoreText.CASH_NEGATIVE, reason.date.toString(), Money.format(reason.shortfallCents))
        is RestoreReason.SharesNegative -> words(RestoreText.SHARES_NEGATIVE, reason.date.toString())
        is RestoreReason.SellMoreThanHeld -> words(RestoreText.SELL_MORE_THAN_HELD, reason.date.toString(), shares(reason.held))
        is RestoreReason.DatedBeforeOpeningBalance -> words(RestoreText.DATED_BEFORE_OPENING_BALANCE, reason.openingDate.toString())
        RestoreReason.SplitTickerNotBought -> words(RestoreText.SPLIT_TICKER_NOT_BOUGHT)
        RestoreReason.BalanceTooLarge -> words(RestoreText.BALANCE_TOO_LARGE)
        is RestoreReason.DependsOn -> words(RestoreText.DEPENDS_ON, count(reason.line))
    }

    /** A held ticker the source refused (4.3), naming the source (wireframe 10e marker 12). */
    fun tickerReason(
        reason: TickerRefusal,
        sourceName: String,
    ): RestoreWords = when (reason) {
        TickerRefusal.NotFound -> words(RestoreText.TICKER_NOT_FOUND, sourceName)
        TickerRefusal.MutualFund -> words(RestoreText.TICKER_MUTUAL_FUND, sourceName)
        is TickerRefusal.NotUsExchange -> words(RestoreText.TICKER_NOT_US_EXCHANGE, sourceName, reason.reported)
        is TickerRefusal.NotUsd -> words(RestoreText.TICKER_NOT_USD, sourceName, reason.reported)
    }

    /**
     * A problem of the source that stops the ticker round (decision 3). A ticker's own refusal is no such problem: the
     * round reports that ticker instead and never stops on it, so its line here is never shown.
     */
    fun sourceProblem(problem: CheckRefusal): RestoreWords = when (problem) {
        CheckRefusal.Offline -> words(RestoreText.SOURCE_OFFLINE)
        CheckRefusal.NoKey -> words(RestoreText.SOURCE_NO_KEY)
        is CheckRefusal.NoAnswer -> words(RestoreText.SOURCE_NO_ANSWER)
        is CheckRefusal.KeyRefused -> words(RestoreText.SOURCE_KEY_REFUSED)
        CheckRefusal.TooManyCalls -> words(RestoreText.SOURCE_TOO_MANY_CALLS)
        CheckRefusal.Unreadable -> words(RestoreText.SOURCE_UNREADABLE)
        is CheckRefusal.Ticker -> words(RestoreText.SOURCE_UNREADABLE)
    }

    fun fileProblem(problem: RestoreFileProblem): RestoreWords = when (problem) {
        RestoreFileProblem.TOO_LARGE -> words(RestoreText.FILE_TOO_LARGE)
        RestoreFileProblem.NOT_READ -> words(RestoreText.FILE_NOT_READ)
        RestoreFileProblem.CHECK_FAILED -> words(RestoreText.FILE_CHECK_FAILED)
    }

    fun summary(summary: RestoreSummary): RestoreSummaryModel = RestoreSummaryModel(
        portfolios = count(summary.portfolios),
        transactions = count(summary.transactions),
        cashEntries = count(summary.cashEntries),
        cashBalance = Money.format(summary.cashBalanceCents),
        holdingsWithShares = count(summary.holdingsWithShares),
    )

    /**
     * The report as text, as shown and as Copy report copies it (C16): the count line, then each error as "<place>:
     * <reason>", one per line. [say] gives an approved string with its values put in (the screen's resources).
     */
    fun reportText(
        report: RestoreReportModel,
        say: (RestoreWords) -> String,
    ): String = (listOf(say(report.count)) + report.lines.map { lineText(it, say) }).joinToString("\n")

    /** One error as shown: "<place>: <reason>". */
    fun lineText(
        line: RestoreReportLine,
        say: (RestoreWords) -> String,
    ): String = say(RestoreWords(RestoreText.REPORT_LINE, listOf(say(line.place), say(line.reason))))

    private fun file() = RestoreWords(RestoreText.PLACE_FILE)

    // "Line 9, ticker XYZ" (wireframe 10e marker 12; departure 3, your B), or "Ticker XYZ" with no line known.
    private fun tickerPlace(error: TickerError): RestoreWords {
        val line = error.line ?: return words(RestoreText.PLACE_TICKER, error.ticker)
        return words(RestoreText.PLACE_LINE_TICKER, count(line), error.ticker)
    }

    // A column with no name is one whose cells are not all empty (section 8); one with a name the format does not have.
    private fun unknownColumn(column: String): RestoreWords {
        if (column.isEmpty()) return words(RestoreText.UNNAMED_COLUMN)
        return words(RestoreText.UNKNOWN_COLUMN, column)
    }

    private fun words(
        text: RestoreText,
        vararg args: String,
    ) = RestoreWords(text, args.toList())

    // A quantity as the lot picker shows it (4.4, 4.10; Quantity.shown through Quantity.format).
    private fun shares(quantity: BigDecimal): String = Quantity.format(quantity)

    // Whole numbers as the app shows them: digits with a comma for thousands, whatever the phone's language.
    private fun count(n: Int): String = String.format(Locale.US, "%,d", n)
}
