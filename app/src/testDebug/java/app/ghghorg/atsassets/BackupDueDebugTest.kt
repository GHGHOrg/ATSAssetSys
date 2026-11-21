package app.ghghorg.atsassets

import app.ghghorg.atsassets.core.BackupReminder
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration

// AC-65 (CR4; 38a decision 1): the debug build is the development app, whose backup reminder is due after 10 minutes.
// It runs only with the debug build's unit tests (app/src/testDebug).
class BackupDueDebugTest {
    @Test
    fun theDevelopmentApp_remindsAfter10Minutes() {
        assertEquals(10L, BuildConfig.BACKUP_DUE_AFTER_MINUTES)
        assertEquals(Duration.ofMinutes(10), BackupReminder.after(BuildConfig.BACKUP_DUE_AFTER_MINUTES))
    }
}
