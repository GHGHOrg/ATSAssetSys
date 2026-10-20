package app.ghghorg.atsassets.lock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockTest {
    private fun lockWithScreenLock() = AppLock().also { it.updateScreenLockAvailable(true) }

    private fun unlocked() = lockWithScreenLock().also {
        it.onPromptShown()
        it.onAuthSucceeded()
    }

    @Test
    fun startsLocked() {
        assertEquals(LockState.Locked, AppLock().state)
    }

    @Test
    fun asksForThePromptAtStart() {
        assertTrue(lockWithScreenLock().shouldShowPrompt())
    }

    @Test
    fun unlocksOnlyOnSuccess() {
        assertEquals(LockState.Unlocked, unlocked().state)
    }

    @Test
    fun cancelledOrFailedPromptStaysLocked() {
        val lock = lockWithScreenLock()
        lock.onPromptShown()
        lock.onAuthCancelledOrFailed()
        assertEquals(LockState.Locked, lock.state)
    }

    @Test
    fun leavingTheAppLocksAgain() {
        val lock = unlocked()
        lock.onAppLeft()
        assertEquals(LockState.Locked, lock.state)
        assertTrue(lock.shouldShowPrompt())
    }

    @Test
    fun noSecondPromptWhileOneIsShowing() {
        val lock = lockWithScreenLock()
        lock.onPromptShown()
        assertFalse(lock.shouldShowPrompt())
        lock.onAuthCancelledOrFailed()
        assertTrue(lock.shouldShowPrompt())
    }

    @Test
    fun leavingTheAppEndsAShowingPrompt() {
        val lock = lockWithScreenLock()
        lock.onPromptShown()
        lock.onAppLeft()
        assertTrue(lock.shouldShowPrompt())
    }

    @Test
    fun noScreenLockNeverUnlocks() {
        val lock = AppLock()
        lock.updateScreenLockAvailable(false)
        lock.onAuthSucceeded()
        assertEquals(LockState.NoScreenLock, lock.state)
        assertFalse(lock.shouldShowPrompt())
    }

    @Test
    fun settingUpAScreenLockLaterLeadsToLocked() {
        val lock = AppLock()
        lock.updateScreenLockAvailable(false)
        lock.updateScreenLockAvailable(true)
        assertEquals(LockState.Locked, lock.state)
        assertTrue(lock.shouldShowPrompt())
    }

    @Test
    fun leavingWhileNoScreenLockStaysNoScreenLock() {
        val lock = AppLock()
        lock.updateScreenLockAvailable(false)
        lock.onAppLeft()
        assertEquals(LockState.NoScreenLock, lock.state)
    }

    @Test
    fun recheckingAvailabilityKeepsAnUnlockedAppUnlocked() {
        val lock = unlocked()
        lock.updateScreenLockAvailable(true)
        assertEquals(LockState.Unlocked, lock.state)
    }

    @Test
    fun removingTheScreenLockWhileUnlockedBlocksTheApp() {
        val lock = unlocked()
        lock.updateScreenLockAvailable(false)
        assertEquals(LockState.NoScreenLock, lock.state)
    }
}
