package app.ghghorg.atsassets.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

/** K6's reminder state (spec section 9, Backup reminder; C15; AC-18; iteration 32). */
class BackupReminderTest {
    private val last = Instant.parse("2026-11-15T03:20:00Z")

    @Test
    fun `a backup exactly 7 days old is not due`() {
        assertFalse(BackupReminder.due(last, last.plus(Duration.ofDays(7)), holdsData = true))
    }

    @Test
    fun `a backup one millisecond older than 7 days is due`() {
        assertTrue(BackupReminder.due(last, last.plus(Duration.ofDays(7)).plusMillis(1), holdsData = true))
    }

    @Test
    fun `a backup made a moment ago is not due`() {
        assertFalse(BackupReminder.due(last, last.plusMillis(1), holdsData = true))
        assertFalse(BackupReminder.due(last, last, holdsData = true))
    }

    @Test
    fun `no backup while the app holds data is due`() {
        assertTrue(BackupReminder.due(null, last, holdsData = true))
    }

    @Test
    fun `no backup while the app holds no data is not due`() {
        assertFalse(BackupReminder.due(null, last, holdsData = false))
    }

    @Test
    fun `a backup dated after now is not due`() {
        // The phone's clock was moved back: no elapsed time can be told, so nothing is due until the time passes again.
        assertFalse(BackupReminder.due(last, last.minus(Duration.ofDays(30)), holdsData = true))
    }

    @Test
    fun `an old backup is due whether or not the app holds data`() {
        // Section 9's first case does not ask for data; a backup can only have been made with data in the app (31 decision 6).
        val now = last.plus(Duration.ofDays(8))
        assertTrue(BackupReminder.due(last, now, holdsData = true))
        assertTrue(BackupReminder.due(last, now, holdsData = false))
    }

    @Test
    fun `the 7 days are elapsed time, whatever the calendar says`() {
        // 7 days minus one second, across a date change in any zone: still not due (C15: no zone is used).
        assertFalse(BackupReminder.due(last, last.plus(Duration.ofDays(7)).minusSeconds(1), holdsData = true))
        assertEquals(Duration.ofHours(168), BackupReminder.AFTER)
    }
}
