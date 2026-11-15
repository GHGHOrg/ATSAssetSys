package app.ghghorg.atsassets.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/*
 * Backup and restore as the screen draws it (section 6, Backup export and restore entry; wireframe 10e frames A and B;
 * CR2, 31), and the last backup as More and that screen show it (section 9, C15). Plain Kotlin: the screens pick the
 * approved string for each and put these values in.
 */

/**
 * The last backup as shown: the export moment's [date] (YYYY-MM-DD) in the device's zone, and that zone's abbreviation
 * [zone] as the platform gives it for that moment, a short name or an offset (C15). The only shown date that is not in
 * New York time, so it carries no "ET" (AC-09).
 */
data class LastBackup(
    val date: String,
    val zone: String,
)

/** Why Back up now is inactive, when a line says so. [NO_DATA]: the app holds no data (31 decision 6). */
enum class BackupBlocked {
    NO_DATA,
}

/**
 * [lastBackup] is null for "never backed up". [interrupted]: a backup's write never ended, and its file may be
 * unfinished (decision 7). [canBackUp]: Back up now is active. [blocked]: why it is not, when a line says why.
 * [writing]: the file is being written (the indicator). [outcome]: how the last export on this screen ended, with none
 * under way. [backEnabled]: "<" and Android Back leave the screen.
 */
data class BackupModel(
    val lastBackup: LastBackup?,
    val interrupted: Boolean,
    val canBackUp: Boolean,
    val blocked: BackupBlocked?,
    val writing: Boolean,
    val outcome: BackupOutcome?,
    val backEnabled: Boolean,
)

/** Why a backup is due (spec section 9; 32): there is none yet ([NEVER]), or the last one is more than 7 days old ([OLD]). */
enum class BackupDue {
    NEVER,
    OLD,
}

/**
 * Overall's backup reminder as the screen draws it (view 1, wireframe 01 marker 1; CR2, 32). [due] is why the banner
 * shows, null when it does not: no backup is due, or it was dismissed. [lastBackup] is for the [BackupDue.OLD] banner
 * and the saved line. [canAct]: Back up now and Dismiss are active. [writing] and [outcome] are the line of an export
 * started from the banner (decision 1): under the banner, or alone in its place once a backup is no longer due.
 */
data class BackupReminderModel(
    val due: BackupDue?,
    val lastBackup: LastBackup?,
    val canAct: Boolean,
    val writing: Boolean,
    val outcome: BackupOutcome?,
)

object Backups {
    // The zone's short name, or its offset where it has none; fixed English, whatever the phone's language.
    private val ZONE_NAME: DateTimeFormatter = DateTimeFormatter.ofPattern("zzz", Locale.US)

    /**
     * [moment] as shown in [zone]: no zone is stored with a backup, so the same moment shows another date or
     * abbreviation after the phone's zone changes (C15). Null for no backup.
     */
    fun lastBackup(
        moment: Instant?,
        zone: ZoneId,
    ): LastBackup? = moment?.atZone(zone)?.let { LastBackup(date = it.toLocalDate().toString(), zone = ZONE_NAME.format(it)) }

    /**
     * The screen. Back up now is active only with data in the app, no export under way and no other save running; the
     * line saying why not is for no data only. A result shows once the export has ended.
     */
    fun screen(
        lastBackup: Instant?,
        zone: ZoneId,
        interrupted: Boolean,
        holdsData: Boolean,
        stage: BackupStage?,
        outcome: BackupOutcome?,
        saving: Boolean,
    ): BackupModel = BackupModel(
        lastBackup = lastBackup(lastBackup, zone),
        interrupted = interrupted,
        canBackUp = holdsData && stage == null && !saving,
        blocked = if (holdsData) null else BackupBlocked.NO_DATA,
        writing = stage == BackupStage.WRITING,
        outcome = if (stage == null) outcome else null,
        backEnabled = stage == null && !saving,
    )

    /**
     * Overall's reminder (32), null when Overall shows nothing of it. [due] is K6's answer and [dismissed] the
     * dismissal since the app was last opened. The buttons are active with data in the app, no export under way and no
     * other save running. A result shows once the export has ended.
     */
    fun reminder(
        lastBackup: Instant?,
        zone: ZoneId,
        due: Boolean,
        dismissed: Boolean,
        holdsData: Boolean,
        stage: BackupStage?,
        outcome: BackupOutcome?,
        saving: Boolean,
    ): BackupReminderModel? {
        val reason =
            when {
                !due || dismissed -> null
                lastBackup == null -> BackupDue.NEVER
                else -> BackupDue.OLD
            }
        val writing = stage == BackupStage.WRITING
        val ended = if (stage == null) outcome else null
        if (reason == null && !writing && ended == null) return null
        return BackupReminderModel(
            due = reason,
            lastBackup = lastBackup(lastBackup, zone),
            canAct = holdsData && stage == null && !saving,
            writing = writing,
            outcome = ended,
        )
    }
}
