package app.ghghorg.atsassets

import app.ghghorg.atsassets.core.BackupReminder
import org.junit.Assert.assertEquals
import org.junit.Test

// Spec section 9 and AC-65 (CR4; 38a decision 1): the release build, your daily app, keeps the 7 days (0 = core's
// BackupReminder.AFTER). It runs only with the release build's unit tests (app/src/testRelease); CI runs it.
class BackupDueReleaseTest {
    @Test
    fun theReleaseBuild_keepsThe7Days() {
        assertEquals(0L, BuildConfig.BACKUP_DUE_AFTER_MINUTES)
        assertEquals(BackupReminder.AFTER, BackupReminder.after(BuildConfig.BACKUP_DUE_AFTER_MINUTES))
    }
}
