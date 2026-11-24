package app.ghghorg.atsassets.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

// 17, 19, 26, 31 and 41 (CR5): More as the screen draws it (section 6, More; wireframes 10f and 11; CR2).
class MoreModelTest {
    private val entries = listOf(MoreEntry.NET_WORTH, MoreEntry.ALLOCATION, MoreEntry.REALIZED_GAINS, MoreEntry.BACKUP, MoreEntry.ABOUT)

    // Wireframe 10f's order (AC-58): Net Worth (Total), Allocation, Realized gains, Backup and restore, and from CR5 About
    // as the fifth (wireframe 11 marker 1, AC-70); with no backup yet, "never backed up".
    @Test
    fun more_withNoBackup_isNeverBackedUp_withTheFiveEntriesInOrder() {
        assertEquals(MoreModel(lastBackup = null, backupInterrupted = false, entries = entries), More.of())
    }

    // Section 9 and C15: the export moment's date in the device's zone, with that zone's abbreviation.
    @Test
    fun more_withABackup_showsItsDateAndZone() {
        val moment = Instant.parse("2026-10-09T16:30:00Z")
        assertEquals(
            MoreModel(lastBackup = LastBackup("2026-10-09", "EDT"), backupInterrupted = false, entries = entries),
            More.of(moment, ZoneId.of("America/New_York")),
        )
    }

    // 31 decision 7: the interrupted backup's warning reaches More, with or without an earlier backup.
    @Test
    fun more_afterAnInterruptedBackup_saysSo() {
        assertEquals(MoreModel(null, backupInterrupted = true, entries = entries), More.of(backupInterrupted = true))
    }
}
