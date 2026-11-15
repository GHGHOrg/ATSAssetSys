package app.ghghorg.atsassets.core

import java.time.Duration
import java.time.Instant

/**
 * K6's reminder state (spec section 9, Backup reminder; AC-18; iteration 32): whether a backup is due. The app knows
 * only when it last exported, not whether the file still exists or was copied off the device (the spec's known limit).
 */
object BackupReminder {
    /** A last backup older than this is due: 7 days of elapsed time, 7 × 24 hours, with no zone (C15). */
    val AFTER: Duration = Duration.ofDays(7)

    /**
     * True when the last backup is more than 7 days before [now], or there is none while the app holds data
     * ([holdsData]; spec section 3). Exactly 7 days is not due. A last backup after [now], the phone's clock having been
     * moved back, is not due.
     */
    fun due(
        lastBackup: Instant?,
        now: Instant,
        holdsData: Boolean,
    ): Boolean = if (lastBackup == null) holdsData else Duration.between(lastBackup, now) > AFTER
}
