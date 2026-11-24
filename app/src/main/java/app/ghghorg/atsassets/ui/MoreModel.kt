package app.ghghorg.atsassets.ui

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/*
 * More as the screen draws it (section 6, More; wireframe 10f; CR2, 17): the last backup line and the five entries
 * (About from CR5, 41).
 * Plain Kotlin: the screen picks the approved string for each.
 */

/** The entries of More, in the order of wireframe 10f. */
enum class MoreEntry {
    NET_WORTH,

    // 19 decision 1: on the first portfolio.
    ALLOCATION,

    // 26: view 5.
    REALIZED_GAINS,

    // 31: Backup and restore (wireframe 10e).
    BACKUP,

    // CR5 (41): About (wireframe 11 marker 1), the fifth entry.
    ABOUT,
}

/**
 * [lastBackup] is the last backup as shown, null for "never backed up" (section 9; 31); the line is information only
 * (wireframe 10f marker 1). [backupInterrupted]: a backup's write never ended (31 decision 7).
 */
data class MoreModel(
    val lastBackup: LastBackup?,
    val backupInterrupted: Boolean,
    val entries: List<MoreEntry>,
)

object More {
    /** More for a last backup at [lastBackup], shown in [zone] (C15: the device's zone when the line is shown). */
    fun of(
        lastBackup: Instant? = null,
        zone: ZoneId = ZoneOffset.UTC,
        backupInterrupted: Boolean = false,
    ): MoreModel = MoreModel(
        lastBackup = Backups.lastBackup(lastBackup, zone),
        backupInterrupted = backupInterrupted,
        entries = listOf(MoreEntry.NET_WORTH, MoreEntry.ALLOCATION, MoreEntry.REALIZED_GAINS, MoreEntry.BACKUP, MoreEntry.ABOUT),
    )
}
