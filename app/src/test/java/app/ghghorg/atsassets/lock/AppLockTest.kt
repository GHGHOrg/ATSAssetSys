package app.ghghorg.atsassets.lock

import app.ghghorg.atsassets.core.AutoLock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockTest {
    private val oneMinute = 60_000L
    private val fiveMinutes = 300_000L

    // A monotonic time, as from SystemClock.elapsedRealtime(); not 0, as on a phone.
    private val start = 1_000_000L

    private fun lockWithScreenLock() = AppLock().also { it.updateScreenLockAvailable(true) }

    private fun unlocked(at: Long = start) = lockWithScreenLock().also {
        it.onPromptShown()
        it.onAuthSucceeded(at)
    }

    private fun unlockedWith(
        choice: AutoLock,
        at: Long = start,
    ) = unlocked(at).also { it.setAutoLock(choice) }

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
        lock.onAppLeft(start + 1, screenOff = false)
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
        lock.onAppLeft(start, screenOff = false)
        assertTrue(lock.shouldShowPrompt())
    }

    @Test
    fun noScreenLockNeverUnlocks() {
        val lock = AppLock()
        lock.updateScreenLockAvailable(false)
        lock.onAuthSucceeded(start)
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
        lock.onAppLeft(start, screenOff = false)
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

    // 20: the four choices (spec section 9; decisions 1 and 2).

    @Test
    fun aRestartLocksWhateverTheChoice() {
        for (choice in AutoLock.entries) {
            val lock = AppLock()
            lock.setAutoLock(choice)
            lock.updateScreenLockAvailable(true)
            assertEquals(choice.name, LockState.Locked, lock.state)
            assertTrue(choice.name, lock.shouldShowPrompt())
        }
    }

    @Test
    fun choiceNotLoadedLocksOnLeavingAndOnThePhoneLock() {
        val leaving = unlocked()
        leaving.onAppLeft(start + 1, screenOff = false)
        assertEquals(LockState.Locked, leaving.state)

        val phoneLock = unlocked()
        phoneLock.onPhoneLocked()
        assertEquals(LockState.Locked, phoneLock.state)
    }

    @Test
    fun choiceNotLoadedDoesNotLockWhileInView() {
        val lock = unlocked()
        lock.onIdleCheck(start + 10 * fiveMinutes)
        assertEquals(LockState.Unlocked, lock.state)
    }

    @Test
    fun everyLeaveLocksOnLeaving() {
        val lock = unlockedWith(AutoLock.EVERY_LEAVE)
        lock.onAppLeft(start + 1, screenOff = false)
        lock.onAppReturned(start + 2)
        assertEquals(LockState.Locked, lock.state)
        assertTrue(lock.shouldShowPrompt())
    }

    @Test
    fun everyLeaveLocksOnThePhoneLock() {
        val lock = unlockedWith(AutoLock.EVERY_LEAVE)
        lock.onPhoneLocked()
        assertEquals(LockState.Locked, lock.state)
    }

    @Test
    fun everyLeaveDoesNotLockWhileInView() {
        val lock = unlockedWith(AutoLock.EVERY_LEAVE)
        lock.onIdleCheck(start + 10 * fiveMinutes)
        assertEquals(LockState.Unlocked, lock.state)
    }

    @Test
    fun idleOneMinuteDoesNotLockOnLeaving() {
        val lock = unlockedWith(AutoLock.IDLE_1_MIN)
        lock.onAppLeft(start + 1, screenOff = false)
        assertEquals(LockState.Unlocked, lock.state)
    }

    @Test
    fun idleOneMinuteAwayJustUnderTheLimitStaysUnlocked() {
        val lock = unlockedWith(AutoLock.IDLE_1_MIN)
        lock.onAppLeft(start + 1, screenOff = false)
        lock.onAppReturned(start + oneMinute - 1)
        assertEquals(LockState.Unlocked, lock.state)
    }

    @Test
    fun idleOneMinuteAwayAtTheLimitLocks() {
        val lock = unlockedWith(AutoLock.IDLE_1_MIN)
        lock.onAppLeft(start + 1, screenOff = false)
        lock.onAppReturned(start + oneMinute)
        assertEquals(LockState.Locked, lock.state)
        assertTrue(lock.shouldShowPrompt())
    }

    @Test
    fun idleOneMinuteCountsFromTheLastTouchNotFromLeaving() {
        val lock = unlockedWith(AutoLock.IDLE_1_MIN)
        lock.onInteraction(start + 20_000)
        lock.onAppLeft(start + 50_000, screenOff = false)
        lock.onAppReturned(start + 20_000 + oneMinute)
        assertEquals(LockState.Locked, lock.state)
    }

    @Test
    fun idleOneMinuteInViewJustUnderTheLimitStaysUnlocked() {
        val lock = unlockedWith(AutoLock.IDLE_1_MIN)
        lock.onIdleCheck(start + oneMinute - 1)
        assertEquals(LockState.Unlocked, lock.state)
    }

    @Test
    fun idleOneMinuteInViewAtTheLimitLocksAndAsksForThePrompt() {
        val lock = unlockedWith(AutoLock.IDLE_1_MIN)
        lock.onIdleCheck(start + oneMinute)
        assertEquals(LockState.Locked, lock.state)
        assertTrue(lock.shouldShowPrompt())
    }

    @Test
    fun aTouchRestartsTheIdleTime() {
        val lock = unlockedWith(AutoLock.IDLE_1_MIN)
        lock.onInteraction(start + 50_000)
        lock.onIdleCheck(start + 50_000 + oneMinute - 1)
        assertEquals(LockState.Unlocked, lock.state)
        lock.onIdleCheck(start + 50_000 + oneMinute)
        assertEquals(LockState.Locked, lock.state)
    }

    @Test
    fun unlockingRestartsTheIdleTime() {
        val lock = lockWithScreenLock()
        lock.setAutoLock(AutoLock.IDLE_1_MIN)
        lock.onInteraction(start)
        lock.onPromptShown()
        lock.onAuthSucceeded(start + 10 * oneMinute)
        lock.onIdleCheck(start + 11 * oneMinute - 1)
        assertEquals(LockState.Unlocked, lock.state)
    }

    @Test
    fun aTouchWhileLockedDoesNotUnlock() {
        val lock = lockWithScreenLock()
        lock.setAutoLock(AutoLock.IDLE_1_MIN)
        lock.onInteraction(start)
        lock.onIdleCheck(start + 1)
        assertEquals(LockState.Locked, lock.state)
    }

    @Test
    fun idleFiveMinutesJustUnderAndAtTheLimit() {
        val away = unlockedWith(AutoLock.IDLE_5_MIN)
        away.onAppLeft(start + 1, screenOff = false)
        away.onAppReturned(start + fiveMinutes - 1)
        assertEquals(LockState.Unlocked, away.state)
        away.onAppLeft(start + fiveMinutes, screenOff = false)
        away.onAppReturned(start + fiveMinutes)
        assertEquals(LockState.Locked, away.state)

        val inView = unlockedWith(AutoLock.IDLE_5_MIN)
        inView.onIdleCheck(start + fiveMinutes - 1)
        assertEquals(LockState.Unlocked, inView.state)
        inView.onIdleCheck(start + fiveMinutes)
        assertEquals(LockState.Locked, inView.state)
    }

    @Test
    fun idleChoicesGoByIdleTimeNotByThePhoneLock() {
        for (choice in listOf(AutoLock.IDLE_1_MIN, AutoLock.IDLE_5_MIN)) {
            val lock = unlockedWith(choice)
            lock.onAppLeft(start + 1, screenOff = true)
            lock.onPhoneLocked()
            lock.onAppReturned(start + oneMinute - 1)
            assertEquals(choice.name, LockState.Unlocked, lock.state)
        }
    }

    @Test
    fun phoneLockDoesNotLockOnLeavingOrIdle() {
        val lock = unlockedWith(AutoLock.PHONE_LOCK_OR_RESTART)
        lock.onIdleCheck(start + 10 * fiveMinutes)
        lock.onAppLeft(start + 10 * fiveMinutes, screenOff = false)
        lock.onAppReturned(start + 20 * fiveMinutes)
        assertEquals(LockState.Unlocked, lock.state)
    }

    @Test
    fun phoneLockLocksWhenLeftWithTheScreenOff() {
        val lock = unlockedWith(AutoLock.PHONE_LOCK_OR_RESTART)
        lock.onAppLeft(start + 1, screenOff = true)
        assertEquals(LockState.Locked, lock.state)
        assertTrue(lock.shouldShowPrompt())
    }

    @Test
    fun phoneLockLocksWhenThePhoneLocksWhileAway() {
        val lock = unlockedWith(AutoLock.PHONE_LOCK_OR_RESTART)
        lock.onAppLeft(start + 1, screenOff = false)
        lock.onPhoneLocked()
        assertEquals(LockState.Locked, lock.state)
    }

    @Test
    fun thePhoneLockKeepsNoScreenLock() {
        val lock = AppLock()
        lock.setAutoLock(AutoLock.PHONE_LOCK_OR_RESTART)
        lock.updateScreenLockAvailable(false)
        lock.onPhoneLocked()
        lock.onAppLeft(start, screenOff = true)
        assertEquals(LockState.NoScreenLock, lock.state)
    }

    @Test
    fun idleKeepsNoScreenLock() {
        val lock = AppLock()
        lock.setAutoLock(AutoLock.IDLE_1_MIN)
        lock.updateScreenLockAvailable(false)
        lock.onIdleCheck(start + fiveMinutes)
        assertEquals(LockState.NoScreenLock, lock.state)
    }

    @Test
    fun aChangedChoiceAppliesAtOnce() {
        val lock = unlockedWith(AutoLock.PHONE_LOCK_OR_RESTART)
        lock.setAutoLock(AutoLock.EVERY_LEAVE)
        lock.onAppLeft(start + 1, screenOff = false)
        assertEquals(LockState.Locked, lock.state)

        val relaxed = unlockedWith(AutoLock.EVERY_LEAVE)
        relaxed.setAutoLock(AutoLock.PHONE_LOCK_OR_RESTART)
        relaxed.onAppLeft(start + 1, screenOff = false)
        assertEquals(LockState.Unlocked, relaxed.state)
    }

    @Test
    fun aLatePromptResultIsIgnoredWhateverTheChoice() {
        for (choice in AutoLock.entries) {
            val lock = lockWithScreenLock()
            lock.setAutoLock(choice)
            lock.onPromptShown()
            lock.onAppLeft(start, screenOff = false)
            lock.onAuthSucceeded(start + 1)
            assertEquals(choice.name, LockState.Locked, lock.state)
        }
    }
}
