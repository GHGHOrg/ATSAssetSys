package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.BackupRecords
import app.ghghorg.atsassets.core.Instrument
import app.ghghorg.atsassets.core.RestoreError
import app.ghghorg.atsassets.core.RestoreSummary
import app.ghghorg.atsassets.core.TickerRefusal
import app.ghghorg.atsassets.prices.CheckRefusal

/** 33b: the wizard's steps built so far (wireframe 10e frames C to F): pick the file, the check, the report, the summary. */
enum class RestoreStep {
    CHOOSE,
    CHECKING,
    REPORT,
    SUMMARY,

    // 34 (wireframe 10e frame H): the delete and load under way; the result (marker 18); a failed write (marker 19).
    RESTORING,
    RESULT,
    FAILED,
}

/** 33b: step 2's two phases (wireframe 10e frame D, marker 9): reading and the rules, then the held tickers. */
enum class CheckPhase {
    READING,
    VERIFYING,
}

/**
 * 33b: a file that gave no check at all, reported as the file as a whole (wireframe 10e marker 12's layout): above the
 * limit (decision 4), not read, or a check that failed in the app itself. Reasons only; the wording is D9. Additions to
 * section 8's list, as 33a decision 3's.
 */
enum class RestoreFileProblem {
    TOO_LARGE,
    NOT_READ,
    CHECK_FAILED,
}

/**
 * 33b: a held ticker the source refused in the second round (section 8, restore step 3): which, why (4.3), and the line
 * of its first buy row in the file (section 8: every error with its line; departure 3, your B), null if none is known.
 */
data class TickerError(
    val ticker: String,
    val reason: TickerRefusal,
    val line: Int? = null,
)

/**
 * 33b: where the restore wizard stands, while it is open. [fileName] is the chosen file's name, null before one is chosen;
 * [choosing] while Android's file screen is open for it. On CHECKING: the [phase], and in the second [verified] of
 * [toVerify] held tickers done; [sourceProblem] when the source could not answer, which stops the round (decision 3). On
 * REPORT: the first round's [errors] in report order, or the second round's [tickerErrors], or a [fileProblem]. On
 * SUMMARY: the file's [summary].
 */
data class RestoreWizard(
    val step: RestoreStep = RestoreStep.CHOOSE,
    val fileName: String? = null,
    val choosing: Boolean = false,
    val phase: CheckPhase = CheckPhase.READING,
    val verified: Int = 0,
    val toVerify: Int = 0,
    val sourceProblem: CheckRefusal? = null,
    val errors: List<RestoreError> = emptyList(),
    val tickerErrors: List<TickerError> = emptyList(),
    val fileProblem: RestoreFileProblem? = null,
    val summary: RestoreSummary? = null,
)

/**
 * 33b decision 2: a file that passed both rounds, held in memory only, for 34 to store with the load: the records K1 made
 * and the held tickers' instruments, those newly verified included. Nothing of it is stored by the check (C3).
 */
class RestoreReady(
    val records: BackupRecords,
    val instruments: List<Instrument>,
)

/** 33b decision 4: a file above 10 MB is refused before it is read (about 150,000 rows, far beyond a real file). */
const val RESTORE_FILE_LIMIT_BYTES = 10 * 1024 * 1024
