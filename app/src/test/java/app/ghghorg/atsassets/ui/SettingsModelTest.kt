package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.AutoLock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 20: Settings' model (wireframe 10f frames B and C; CR2). */
class SettingsModelTest {
    @Test
    fun theAutoLockRow_showsTheChoiceInForce() {
        for (choice in AutoLock.entries) {
            assertEquals(choice, Settings.of(choice, refusal = null).autoLock)
        }
    }

    @Test
    fun aChoiceNotYetLoaded_showsEveryLeave_theRuleTheLockKeeps() {
        assertEquals(AutoLock.EVERY_LEAVE, Settings.of(null, refusal = null).autoLock)
    }

    @Test
    fun theFourChoices_inTheOrderOfSection9_onlyEveryLeaveMarkedDefault() {
        val options = Settings.of(AutoLock.IDLE_5_MIN, refusal = null).options
        assertEquals(
            listOf(
                AutoLockOption(AutoLock.EVERY_LEAVE, isDefault = true),
                AutoLockOption(AutoLock.IDLE_1_MIN, isDefault = false),
                AutoLockOption(AutoLock.IDLE_5_MIN, isDefault = false),
                AutoLockOption(AutoLock.PHONE_LOCK_OR_RESTART, isDefault = false),
            ),
            options,
        )
    }

    @Test
    fun aFailedSave_isShown_andNoRefusalShowsNothing() {
        assertTrue(Settings.of(AutoLock.EVERY_LEAVE, SaveRefusal.SAVE_FAILED).notSaved)
        assertFalse(Settings.of(AutoLock.EVERY_LEAVE, refusal = null).notSaved)
    }
}
