package app.ghghorg.atsassets.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

// 32 step 4: Overall's backup reminder as the screen draws it (view 1, wireframe 01 marker 1; spec section 9; CR2).
class BackupReminderModelTest {
    private val moment: Instant = Instant.parse("2026-11-01T16:30:00Z")
    private val zone: ZoneId = ZoneId.of("America/New_York")

    private fun reminder(
        lastBackup: Instant? = null,
        due: Boolean = true,
        dismissed: Boolean = false,
        holdsData: Boolean = true,
        stage: BackupStage? = null,
        outcome: BackupOutcome? = null,
        saving: Boolean = false,
    ): BackupReminderModel? = Backups.reminder(lastBackup, zone, due, dismissed, holdsData, stage, outcome, saving)

    @Test
    fun noBackupYet_isTheNeverBanner_withBothButtonsActive() {
        assertEquals(
            BackupReminderModel(due = BackupDue.NEVER, lastBackup = null, canAct = true, writing = false, outcome = null),
            reminder(),
        )
    }

    // The old banner carries the last backup as More shows it: the date in the device's zone, with its abbreviation (C15).
    @Test
    fun anOldBackup_isTheOldBanner_withItsDateInTheDevicesZone() {
        assertEquals(
            BackupReminderModel(
                due = BackupDue.OLD,
                lastBackup = LastBackup("2026-11-01", "EST"),
                canAct = true,
                writing = false,
                outcome = null,
            ),
            reminder(lastBackup = moment),
        )
        assertEquals("2026-11-02", Backups.reminder(moment, ZoneId.of("Asia/Tokyo"), true, false, true, null, null, false)!!.lastBackup!!.date)
    }

    @Test
    fun notDue_orDismissed_withNoExportLine_showsNothing() {
        assertNull(reminder(lastBackup = moment, due = false))
        assertNull(reminder(dismissed = true))
        assertNull(reminder(lastBackup = moment, dismissed = true))
    }

    // Decision 1: once the backup is made the banner is gone, and the saved line stands alone in its place.
    @Test
    fun aCompletedExport_leavesTheSavedLineAlone() {
        assertEquals(
            BackupReminderModel(
                due = null,
                lastBackup = LastBackup("2026-11-01", "EST"),
                canAct = true,
                writing = false,
                outcome = BackupOutcome.SAVED,
            ),
            reminder(lastBackup = moment, due = false, outcome = BackupOutcome.SAVED),
        )
    }

    // Decision 1: a cancelled or failed export keeps the banner, with its line under it.
    @Test
    fun anExportThatDidNotSave_keepsTheBanner_withItsLine() {
        for (outcome in listOf(BackupOutcome.NOT_SAVED, BackupOutcome.NOT_SAVED_FILE_MAY_REMAIN, BackupOutcome.SAVED_NOT_RECORDED)) {
            val model = reminder(outcome = outcome)!!
            assertEquals(BackupDue.NEVER, model.due)
            assertEquals(outcome, model.outcome)
            assertTrue(model.canAct)
        }
    }

    @Test
    fun whileAnExportIsUnderWay_theButtonsAreOff_andOnlyTheWritingShows() {
        val choosing = reminder(stage = BackupStage.CHOOSING, outcome = BackupOutcome.NOT_SAVED)!!
        assertFalse(choosing.canAct)
        assertFalse(choosing.writing)
        assertNull(choosing.outcome)
        val writing = reminder(stage = BackupStage.WRITING, saving = true)!!
        assertFalse(writing.canAct)
        assertTrue(writing.writing)
        assertEquals(BackupDue.NEVER, writing.due)
    }

    @Test
    fun whileAnotherSaveRuns_orWithNoData_theButtonsWait() {
        assertFalse(reminder(saving = true)!!.canAct)
        assertFalse(reminder(lastBackup = moment, holdsData = false)!!.canAct)
    }
}
