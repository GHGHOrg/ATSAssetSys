package app.ghghorg.atsassets

import app.ghghorg.atsassets.core.BackupReminder
import org.junit.Assert.assertEquals
import org.junit.Test

// 36 decision 1: only the check build ("ATS Check") sets a shorter "backup due" time; every other build, the debug build
// these tests run on included, sets 0, which means spec section 9's 7 days (risk 1).
class BackupDueBuildTest {
    @Test
    fun theDebugBuild_keepsThe7Days() {
        assertEquals(0L, BuildConfig.BACKUP_DUE_AFTER_MINUTES)
        assertEquals(BackupReminder.AFTER, BackupReminder.after(BuildConfig.BACKUP_DUE_AFTER_MINUTES))
    }
}
