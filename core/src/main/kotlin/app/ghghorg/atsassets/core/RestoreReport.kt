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
