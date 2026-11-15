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
}
