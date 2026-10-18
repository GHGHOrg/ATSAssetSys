package app.ghghorg.atsassets.lock

enum class LockState { Locked, Unlocked, NoScreenLock }

/**
 * Lock rules for K8 (spec section 9), iteration 01: the policy is fixed at
 * "lock every time the app is left". Pure Kotlin, no Android classes.
 * Not thread-safe: call it from the main thread only.
 *
 * Fails closed: the app starts Locked, and a screen lock is not assumed
 * until updateScreenLockAvailable() has been called.
 */
class AppLock {
    var state: LockState = LockState.Locked
        private set

    private var screenLockAvailable = false
    private var promptShowing = false

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

    fun shouldShowPrompt(): Boolean =
        state == LockState.Locked && screenLockAvailable && !promptShowing

    fun onPromptShown() {
        promptShowing = true
    }

    /** Unlocks only for a prompt this object knows is showing. A late result is ignored. */
    fun onAuthSucceeded() {
        if (promptShowing && state == LockState.Locked) {
            state = LockState.Unlocked
        }
        promptShowing = false
    }

    /** The prompt was dismissed or returned an error. The app stays as it was. */
    fun onAuthCancelledOrFailed() {
        promptShowing = false
    }

    /** The app went out of view. An unlocked app locks; a showing prompt is forgotten. */
    fun onAppLeft() {
        promptShowing = false
        if (state == LockState.Unlocked) {
            state = LockState.Locked
        }
    }
}
