package app.ghghorg.atsassets.core

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
}

/**
 * One error of the report (section 8): its [line] as Notepad shows it (the header is line 1), null for the file as a whole;
 * its [column] when one applies; and its [reason].
 */
data class RestoreError(
    val line: Int?,
    val column: String?,
    val reason: RestoreReason,
)
