package app.ghghorg.atsassets.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

// 31 step 5: Backup and restore as the screen draws it, and the last backup line (section 9, C15; wireframe 10e; CR2).
class BackupModelTest {
    private val moment: Instant = Instant.parse("2026-10-09T16:30:00Z")
    private val utc: ZoneId = ZoneId.of("UTC")

    private fun screen(
        lastBackup: Instant? = null,
        zone: ZoneId = utc,
        interrupted: Boolean = false,
        holdsData: Boolean = true,
        stage: BackupStage? = null,
        outcome: BackupOutcome? = null,
        saving: Boolean = false,
    ): BackupModel = Backups.screen(lastBackup, zone, interrupted, holdsData, stage, outcome, saving)

    @Test
    fun noBackup_isNull_forNeverBackedUp() {
        assertNull(Backups.lastBackup(null, utc))
        assertNull(screen().lastBackup)
    }

    // C15: the date is the export moment's in the device's zone; the same moment is the next day in Tokyo.
    @Test
    fun theDate_isTheMomentsInTheDevicesZone() {
        assertEquals("2026-10-09", Backups.lastBackup(moment, utc)!!.date)
        assertEquals("2026-10-09", Backups.lastBackup(moment, ZoneId.of("America/New_York"))!!.date)
        assertEquals("2026-10-10", Backups.lastBackup(moment, ZoneId.of("Asia/Tokyo"))!!.date)
        assertEquals("2026-10-10", Backups.lastBackup(moment, ZoneId.of("Asia/Singapore"))!!.date)
    }

    // C15: the zone's abbreviation for that moment: daylight time in October, standard time in January.
    @Test
    fun theZone_isItsAbbreviationAtThatMoment() {
        val newYork = ZoneId.of("America/New_York")
        assertEquals("EDT", Backups.lastBackup(moment, newYork)!!.zone)
        assertEquals("EST", Backups.lastBackup(Instant.parse("2026-01-09T16:30:00Z"), newYork)!!.zone)
        assertEquals("UTC", Backups.lastBackup(moment, utc)!!.zone)
    }

    // C15: a zone the platform has no short name for shows what the platform gives; here a fixed offset.
    @Test
    fun aZoneWithoutAShortName_showsWhatThePlatformGives() {
        assertEquals(LastBackup("2026-10-10", "+08:00"), Backups.lastBackup(moment, ZoneId.of("+08:00")))
    }

    // Section 6, number format: the date's digits do not follow the phone's language.
    @Test
    fun theLine_isTheSameUnderALanguageWithOtherDigits() {
        val before = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ar-EG-u-nu-arab"))
            assertEquals(LastBackup("2026-10-09", "EDT"), Backups.lastBackup(moment, ZoneId.of("America/New_York")))
        } finally {
            Locale.setDefault(before)
        }
    }

    @Test
    fun withData_andNothingUnderWay_backUpNowIsActive() {
        val model = screen(lastBackup = moment)
        assertTrue(model.canBackUp)
        assertNull(model.blocked)
        assertFalse(model.writing)
        assertNull(model.outcome)
        assertTrue(model.backEnabled)
        assertEquals(LastBackup("2026-10-09", "UTC"), model.lastBackup)
    }

    // Decision 6: no data, so inactive, with the reason; nothing else changes.
    @Test
    fun withNoData_backUpNowIsInactive_withItsReason() {
        val model = screen(holdsData = false)
        assertFalse(model.canBackUp)
        assertEquals(BackupBlocked.NO_DATA, model.blocked)
        assertTrue(model.backEnabled)
    }

    // While Android's file screen is open the button is off and no line shows; while the file is written, the indicator.
    @Test
    fun whileAnExportIsUnderWay_theButtonIsOff_andBackToo() {
        val choosing = screen(stage = BackupStage.CHOOSING, outcome = BackupOutcome.NOT_SAVED)
        assertFalse(choosing.canBackUp)
        assertFalse(choosing.writing)
        assertNull(choosing.outcome)
        assertFalse(choosing.backEnabled)
        assertNull(choosing.blocked)
        val writing = screen(stage = BackupStage.WRITING, saving = true)
        assertFalse(writing.canBackUp)
        assertTrue(writing.writing)
        assertFalse(writing.backEnabled)
    }

    // Another save running (none can start from this screen, but the rule is the app's): the button and Back wait.
    @Test
    fun whileAnotherSaveRuns_theButtonWaits() {
        val model = screen(saving = true)
        assertFalse(model.canBackUp)
        assertFalse(model.backEnabled)
        assertFalse(model.writing)
    }

    @Test
    fun eachOutcome_reachesTheModel_onceTheExportHasEnded() {
        for (outcome in BackupOutcome.entries) assertEquals(outcome, screen(outcome = outcome).outcome)
    }

    @Test
    fun anInterruptedBackup_reachesTheModel() {
        assertTrue(screen(interrupted = true).interrupted)
        assertFalse(screen().interrupted)
    }
}
