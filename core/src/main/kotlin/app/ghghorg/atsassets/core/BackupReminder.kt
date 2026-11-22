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
     * True when the last backup is more than [after] (7 days unless given) before [now], or there is none while the app
     * holds data ([holdsData]; spec section 3). Exactly [after] is not due. A last backup after [now], the phone's clock
     * having been moved back, is not due. Only the check build passes another time (iteration 36).
     */
    fun due(
        lastBackup: Instant?,
        now: Instant,
        holdsData: Boolean,
        after: Duration = AFTER,
    ): Boolean = if (lastBackup == null) holdsData else Duration.between(lastBackup, now) > after

    /** The time a build sets in minutes (iteration 36, decision 1): 0 means the 7 days; below 0 is refused. */
    fun after(minutes: Long): Duration {
        require(minutes >= 0) { "minutes must not be below 0" }
        return if (minutes == 0L) AFTER else Duration.ofMinutes(minutes)
    }
}
