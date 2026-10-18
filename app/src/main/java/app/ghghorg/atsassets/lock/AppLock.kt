package app.ghghorg.atsassets.lock

enum class LockState { Locked, Unlocked, NoScreenLock }

/** STUB (iteration 01, step 3). Replaced by the real logic in step 4. */
class AppLock {
    var state: LockState = LockState.Locked
        private set

    fun updateScreenLockAvailable(available: Boolean) {}
    fun shouldShowPrompt(): Boolean = false
    fun onPromptShown() {}
    fun onAuthSucceeded() {}
    fun onAuthCancelledOrFailed() {}
    fun onAppLeft() {}
}
