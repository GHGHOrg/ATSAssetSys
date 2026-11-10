package app.ghghorg.atsassets.lock

/**
 * K8's screenshot window (spec section 9; 21): screenshots and recording allowed for a fixed 3 minutes after a
 * fingerprint or phone PIN check asked for each time. Pure Kotlin, no Android classes. Not thread-safe: call it from the
 * main thread only. Every `now` is milliseconds on a clock that does not jump with the wall clock
 * (SystemClock.elapsedRealtime), as for the lock's idle time.
 *
 * Off at start; a new object after a restart, so it never survives one. MainActivity's one prompt serves the lock and
 * the window (decision 2): a passed check is the window's only if the window asked for it, and starts it only while the
 * app is unlocked; every other result is the lock's. Every doubt ends it: Turn off, leaving the app and any lock
 * (decision 1).
 */
class ScreenshotWindow {
    private var checkAsked = false
    private var endsAt: Long? = null

    /** True if a check may be shown for the window: it is off and none is asked for already. Marks the check as the window's. */
    fun requestCheck(): Boolean {
        if (checkAsked || endsAt != null) return false
        checkAsked = true
        return true
    }

    /**
     * A check passed. Returns true if it was the window's, so the lock does not get it; the window then starts only if
     * [appUnlocked]. Returns false for a result the window did not ask for.
     */
    fun onCheckPassed(
        now: Long,
        appUnlocked: Boolean,
    ): Boolean {
        if (!checkAsked) return false
        checkAsked = false
        if (appUnlocked) endsAt = now + LENGTH_MS
        return true
    }

    /** The window's check was cancelled or failed: it stays off, and a late result is not the window's. */
    fun onCheckEnded() {
        checkAsked = false
    }

    /** Turn off, leaving the app or a lock (decision 1): off at once, and a check asked for is dropped. */
    fun end() {
        checkAsked = false
        endsAt = null
    }

    /** The milliseconds left, or null when off; at 3 minutes it ends. */
    fun timeLeft(now: Long): Long? {
        val end = endsAt ?: return null
        if (now >= end) {
            endsAt = null
            return null
        }
        return end - now
    }

    companion object {
        const val LENGTH_MS = 180_000L
    }
}
