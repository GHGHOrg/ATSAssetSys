package app.ghghorg.atsassets.lock

import app.ghghorg.atsassets.core.AutoLock

enum class LockState { Locked, Unlocked, NoScreenLock }

/**
 * Lock rules for K8 (spec section 9): the four auto-lock choices (20).
 * Pure Kotlin, no Android classes. Not thread-safe: call it from the main thread only.
 * Every `now` is milliseconds on a clock that does not jump with the wall clock
 * (SystemClock.elapsedRealtime), so changing the phone's time cannot stretch the idle time.
 *
 * Fails closed: the app starts Locked whatever the choice (a restart always locks), a screen
 * lock is not assumed until updateScreenLockAvailable() has been called, and until the choice
 * is loaded the strictest rule holds, "every time you leave".
 *
 * Idle is no touch in the app, the time away counting (20 decision 1); it runs from the last
 * touch or the unlock, whichever is later.
 */
class AppLock {
    var state: LockState = LockState.Locked
        private set

    private var screenLockAvailable = false
    private var promptShowing = false
    private var autoLock: AutoLock? = null
    private var lastInteraction = 0L

    /** The choice applies from the next event on. */
    fun setAutoLock(choice: AutoLock) {
        autoLock = choice
    }

    /** Call on every entry to the app, before asking whether to show the prompt. */
    fun updateScreenLockAvailable(available: Boolean) {
        screenLockAvailable = available
        if (!available) {
            promptShowing = false
            state = LockState.NoScreenLock
        } else if (state == LockState.NoScreenLock) {
            state = LockState.Locked
        }
    }

    fun shouldShowPrompt(): Boolean = state == LockState.Locked && screenLockAvailable && !promptShowing

    fun onPromptShown() {
        promptShowing = true
    }

    /** Unlocks only for a prompt this object knows is showing. A late result is ignored. */
    fun onAuthSucceeded(now: Long) {
        if (promptShowing && state == LockState.Locked) {
            state = LockState.Unlocked
            lastInteraction = now
        }
        promptShowing = false
    }

    /** The prompt was dismissed or returned an error. The app stays as it was. */
    fun onAuthCancelledOrFailed() {
        promptShowing = false
    }

    /** A touch in the app. */
    fun onInteraction(now: Long) {
        lastInteraction = now
    }

    /** The app went out of view; [screenOff] when the phone was locked with the app in view. A showing prompt is forgotten. */
    fun onAppLeft(
        now: Long,
        screenOff: Boolean,
    ) {
        promptShowing = false
        when (autoLock) {
            null, AutoLock.EVERY_LEAVE -> lockIfUnlocked()
            AutoLock.IDLE_1_MIN, AutoLock.IDLE_5_MIN -> onIdleCheck(now)
            AutoLock.PHONE_LOCK_OR_RESTART -> if (screenOff) lockIfUnlocked()
        }
    }

    /** The app came back into view, before asking whether to show the prompt. */
    fun onAppReturned(now: Long) {
        onIdleCheck(now)
    }

    /** Locks an idle app under the idle choices; call it on a timer while the app is in view. */
    fun onIdleCheck(now: Long) {
        val limit = idleLimit(autoLock) ?: return
        if (now - lastInteraction >= limit) {
            lockIfUnlocked()
        }
    }

    /** The phone locked (the screen went off) while the process lived, the app in view or not. */
    fun onPhoneLocked() {
        when (autoLock) {
            null, AutoLock.EVERY_LEAVE, AutoLock.PHONE_LOCK_OR_RESTART -> lockIfUnlocked()
            AutoLock.IDLE_1_MIN, AutoLock.IDLE_5_MIN -> Unit
        }
    }

    private fun lockIfUnlocked() {
        if (state == LockState.Unlocked) {
            state = LockState.Locked
        }
    }

    private fun idleLimit(choice: AutoLock?): Long? = when (choice) {
        AutoLock.IDLE_1_MIN -> 60_000L
        AutoLock.IDLE_5_MIN -> 300_000L
        null, AutoLock.EVERY_LEAVE, AutoLock.PHONE_LOCK_OR_RESTART -> null
    }
}
