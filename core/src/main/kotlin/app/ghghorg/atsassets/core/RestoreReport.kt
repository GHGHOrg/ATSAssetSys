package app.ghghorg.atsassets.core

import java.math.BigDecimal
import java.time.LocalDate

/**
 * Why a backup file, or one of its lines, cannot be restored (spec section 8, rejection reasons and the report; 33a).
 * Reasons only, with the figures the message needs; the wording is D9 and lives in the app (33b).
 */
sealed interface RestoreReason {
    // File level: the check stops here (section 8, restore step 2).

    data object NotUtf8 : RestoreReason

    data object NoHeader : RestoreReason

    data class MissingColumn(
        val column: String,
    ) : RestoreReason

    /** A header cell that names no column, as written; empty for a column with no name whose cells are not all empty. */
    data class UnknownColumn(
        val column: String,
    ) : RestoreReason

    data class RepeatedColumn(
        val column: String,
    ) : RestoreReason

    /** The first row after the header is not a `format` row, or there is none. */
    data object NoFormatRow : RestoreReason

    data object VersionNotWhole : RestoreReason

    /** A whole number other than the one version this app reads ([BackupFile.FORMAT_VERSION]), as written. */
    data class VersionUnknown(
        val version: String,
    ) : RestoreReason

    // One line.

    data class CellCount(
        val found: Int,
        val expected: Int,
    ) : RestoreReason

    data object LineBreakInCell : RestoreReason

    // Every row (33a step 3). The error's column names the cell.

    data object RowTypeMissing : RestoreReason

    data class RowTypeUnknown(
        val rowType: String,
    ) : RestoreReason

    /** A `format` row that is not the first row after the header. */
    data object FormatNotFirst : RestoreReason

    /** A value the row type needs is empty. */
    data object ValueMissing : RestoreReason

    /** A cell in a column the row type does not use is not empty (it catches a comma that shifted cells). */
    data object CellNotUsed : RestoreReason

    // Dates and numbers.

    /** Not YYYY-MM-DD. */
    data object DateForm : RestoreReason

    /** YYYY-MM-DD, but no real date. */
    data object DateNotReal : RestoreReason

    /** Not digits with an optional leading minus and an optional dot: a separator, a sign, a symbol or an exponent. */
    data object NumberForm : RestoreReason

    data object QuantityNotPositive : RestoreReason

    data object QuantityTooManyDecimals : RestoreReason

    data object AmountTooManyDecimals : RestoreReason

    /** Beyond the exact cents the app can hold (33a decision 3; not a reason of section 8's list). */
    data object AmountTooLarge : RestoreReason

    data object WrongSign : RestoreReason

    data object CashAmountZero : RestoreReason

    data object BuyTotalZero : RestoreReason

    // Ids and portfolios.

    data object IdBadlyFormed : RestoreReason

    data object PortfolioNameTooShort : RestoreReason

    data object PortfolioNameNoLetter : RestoreReason

    // Cash, tickers and splits.

    data object CashTypeUnknown : RestoreReason

    data object CashTypeTrade : RestoreReason

    data object AdjustmentWithoutNote : RestoreReason

    data object TickerBadCharacters : RestoreReason

    data object SplitNumberNotWhole : RestoreReason

    data object SplitNumbersEqual : RestoreReason

    /** Above [Splits.MAX_RATIO] (33a decision 3; not a reason of section 8's list). */
    data object SplitNumberTooLarge : RestoreReason

    /** A re-entered buy's ratio too large to keep (14c decision 1; 33a decision 3; not a reason of section 8's list). */
    data object BuyRatioTooLarge : RestoreReason

    // Sells and picks.

    data object PicksBadlyWritten : RestoreReason

    // State rows and settings.

    data object OverrideBothEmpty : RestoreReason

    data object ListUnknown : RestoreReason

    data object SettingKeyUnknown : RestoreReason

    data object SettingValueUnknown : RestoreReason

    data object TabOrderNotEachOnce : RestoreReason

    // Between rows and in the replay (33a step 4). A [line] names the other row.

    /** The id is already used on [line], compared ignoring case (one id space, section 8). */
    data class IdDuplicate(
        val line: Int,
    ) : RestoreReason

    /** The name matches the portfolio on [line] (4.7, D7). */
    data class PortfolioNameDuplicate(
        val line: Int,
    ) : RestoreReason

    data object PortfolioNotFound : RestoreReason

    /** The file as a whole: no opening balance row (4.2). Nothing is replayed without one. */
    data object NoOpeningBalance : RestoreReason

    /** The opening balance is already on [line]. */
    data class SecondOpeningBalance(
        val line: Int,
    ) : RestoreReason

    data object OpeningBalanceNotEarliest : RestoreReason

    /** The pick's id, as written, is no row of the file. */
    data class PickNotFound(
        val id: String,
    ) : RestoreReason

    data class PickNotABuy(
        val id: String,
    ) : RestoreReason

    data class PickOtherPortfolio(
        val id: String,
    ) : RestoreReason

    /** A lot of another ticker (4.4's lot rules; not a reason of section 8's list). */
    data class PickOtherTicker(
        val id: String,
    ) : RestoreReason

    /** The lot is picked twice on one sell (4.4; not a reason of section 8's list). */
    data class PickTwice(
        val id: String,
    ) : RestoreReason

    data class LotBoughtAfterSell(
        val id: String,
    ) : RestoreReason

    data class LotEnteredAfterSell(
        val id: String,
    ) : RestoreReason

    /** The pick is above what the lot holds, [remainder] as the lot picker shows it (4.4, 4.10). */
    data class PickOverLot(
        val id: String,
        val remainder: BigDecimal,
    ) : RestoreReason

    data class PicksDoNotAddUp(
        val picked: BigDecimal,
        val quantity: BigDecimal,
    ) : RestoreReason

    /** The reversal's target, as written, is no row of the file. */
    data class ReversalTargetNotFound(
        val id: String,
    ) : RestoreReason

    data object ReversalOfOpeningBalance : RestoreReason

    /** The target is on [line], after the reversal (section 8: a reversal comes after the entry it reverses). */
    data class ReversalTargetListedAfter(
        val line: Int,
    ) : RestoreReason

    /** The target is already reversed on [line] (4.6: at most once). */
    data class AlreadyReversed(
        val line: Int,
    ) : RestoreReason

    data class ReversalBeforeOriginal(
        val originalDate: LocalDate,
    ) : RestoreReason

    // State rows.

    data object HiddenHoldingNotFound : RestoreReason

    data object HiddenHoldingHasShares : RestoreReason

    data object OverrideHoldingNotFound : RestoreReason

    data object OverrideValueNotOnList : RestoreReason

    data class DuplicateListValue(
        val line: Int,
    ) : RestoreReason

    data class DuplicateRememberedText(
        val line: Int,
    ) : RestoreReason

    data class DuplicateHidden(
        val line: Int,
    ) : RestoreReason

    data class DuplicateOverride(
        val line: Int,
    ) : RestoreReason

    /** The value matches the remembered text on [line] of its list (4.9). */
    data class ListValueMatchesRememberedText(
        val line: Int,
    ) : RestoreReason

    data object RememberedTextValueNotOnList : RestoreReason

    data class SettingKeyTwice(
        val line: Int,
    ) : RestoreReason

    // Rules (4.1 to 4.6), from K1.

    /** Cash would be below 0 on [date] by [shortfallCents]. */
    data class CashNegative(
        val date: LocalDate,
        val shortfallCents: Long,
    ) : RestoreReason

    /** A lot or a holding would be below 0 on [date]. */
    data class SharesNegative(
        val date: LocalDate,
    ) : RestoreReason

    /** The portfolio holds only [held] of the ticker on [date], as the lot picker shows it. */
    data class SellMoreThanHeld(
        val date: LocalDate,
        val held: BigDecimal,
    ) : RestoreReason

    data class DatedBeforeOpeningBalance(
        val openingDate: LocalDate,
    ) : RestoreReason

    /** A split of a ticker with no buy before it in that portfolio (4.5; not a reason of section 8's list). */
    data object SplitTickerNotBought : RestoreReason

    /** The cash balance would leave the exact cents the app can hold (33a decision 3; not a reason of section 8's list). */
    data object BalanceTooLarge : RestoreReason

    // Dependent (C7; 33a decision 2).

    /** The row points to a failed row; [line] is the root cause's. It is not checked further. */
    data class DependsOn(
        val line: Int,
    ) : RestoreReason
}

/**
 * The step-5 summary of section 8 for a clean file: the file's portfolios, transactions (buys, sells, splits and their
 * reversals) and cash entries (cash entries and their reversals; the rebuilt trade lines are not counted), the cash
 * balance after the restore (every entry, as the Cash tab shows it) and the holdings with shares at the end of the file.
 */
data class RestoreSummary(
    val portfolios: Int,
    val transactions: Int,
    val cashEntries: Int,
    val cashBalanceCents: Long,
    val holdingsWithShares: Int,
)

/** What a check gives (section 8, restore steps 2 and 5; 33a decision 1). Nothing is stored either way (C3). */
sealed interface RestoreCheck {
    /**
     * Every error found, in line order, the file as a whole first; within a line, the form errors in the file's own
     * column order, then the rule error or "depends on line N" (33a decision 4).
     */
    data class Failed(
        val errors: List<RestoreError>,
    ) : RestoreCheck {
        /** N of "N errors in M lines" (section 8). */
        val errorCount: Int get() = errors.size

        /** M of "N errors in M lines": the lines with an error; an error of the file as a whole is no line. */
        val lineCount: Int get() = errors.mapNotNull { it.line }.distinct().size
    }

    /**
     * A clean file: the [records] K1 made in the replay with the app's own ids, ready to load (34); the tickers with
     * shares at the end of the file, for the ticker check (33b); and the summary. [tickerLines]: each held ticker's
     * first buy row in the file, the line the second round's report names (section 8: every error with its line; 33b
     * departure 3, your B).
     */
    data class Passed(
        val records: BackupRecords,
        val heldTickers: List<String>,
        val summary: RestoreSummary,
        val tickerLines: Map<String, Int> = emptyMap(),
    ) : RestoreCheck
}

/**
 * What the check needs from the app: the ids K1 gives the records ([newId]); the price sources' words a file may name
 * ([priceSources]) and the one a file without a `price_source` row gets ([defaultSource]); and the note of 4.6 for a cash
 * reversal the file gives none ([reversalNote], the original's type and date; its wording is the app's, D9).
 */
class RestoreContext(
    val newId: () -> String,
    val priceSources: Set<String>,
    val defaultSource: String,
    val reversalNote: (CashType, LocalDate) -> String,
)

/**
 * One error of the report (section 8): its [line] as Notepad shows it (the header is line 1), null for the file as a whole;
 * its [column] when one applies; and its [reason].
 */
data class RestoreError(
    val line: Int?,
    val column: String?,
    val reason: RestoreReason,
)
