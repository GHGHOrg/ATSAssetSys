package app.ghghorg.atsassets.lock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 21: the 3-minute screenshot window (spec section 9; decisions 1 and 2). */
class ScreenshotWindowTest {
    // A monotonic time, as from SystemClock.elapsedRealtime(); not 0, as on a phone.
    private val start = 1_000_000L

    private fun on(at: Long = start) = ScreenshotWindow().also {
        assertTrue(it.requestCheck())
        assertTrue(it.onCheckPassed(at, appUnlocked = true))
    }

    @Test
    fun offAtStart() {
        assertNull(ScreenshotWindow().timeLeft(start))
    }

    @Test
    fun aPassedCheck_startsThreeMinutes() {
        assertEquals(180_000L, on().timeLeft(start))
    }

    @Test
    fun theTimeLeft_runsDown_andTheWindowEndsAtExactlyThreeMinutes() {
        val window = on()
        assertEquals(60_000L, window.timeLeft(start + 120_000))
        assertEquals(1L, window.timeLeft(start + 179_999))
        assertNull(window.timeLeft(start + 180_000))
    }

    @Test
    fun anEndedWindow_staysEnded() {
        val window = on()
        assertNull(window.timeLeft(start + 180_000))
        assertNull(window.timeLeft(start + 1))
    }

    @Test
    fun neverOn_withoutACheckAskedFor() {
        val window = ScreenshotWindow()
        assertFalse("the result is not the window's", window.onCheckPassed(start, appUnlocked = true))
        assertNull(window.timeLeft(start))
    }

    @Test
    fun aCheckCancelledOrFailed_leavesItOff_andALateResultIsNotTheWindows() {
        val window = ScreenshotWindow()
        assertTrue(window.requestCheck())
        window.onCheckEnded()
        assertFalse(window.onCheckPassed(start, appUnlocked = true))
        assertNull(window.timeLeft(start))
    }

    @Test
    fun aCheckPassedWhileLocked_isTheWindows_butDoesNotStartIt() {
        val window = ScreenshotWindow()
        assertTrue(window.requestCheck())
        assertTrue("taken, so the lock does not get it", window.onCheckPassed(start, appUnlocked = false))
        assertNull(window.timeLeft(start))
    }

    @Test
    fun oneCheckStartsTheWindowOnce() {
        val window = on()
        assertFalse("a second result is not the window's", window.onCheckPassed(start + 1, appUnlocked = true))
        assertEquals(179_999L, window.timeLeft(start + 1))
    }

    @Test
    fun noSecondCheck_whileOneIsAskedFor_orWhileOn() {
        val asked = ScreenshotWindow()
        assertTrue(asked.requestCheck())
        assertFalse(asked.requestCheck())
        assertFalse(on().requestCheck())
    }

    @Test
    fun turnOff_leavingOrALock_endsIt() {
        val window = on()
        window.end()
        assertNull(window.timeLeft(start + 1))
    }

    @Test
    fun endingClearsACheckAskedFor() {
        val window = ScreenshotWindow()
        assertTrue(window.requestCheck())
        window.end()
        assertFalse(window.onCheckPassed(start, appUnlocked = true))
        assertNull(window.timeLeft(start))
    }

    @Test
    fun afterItEnds_aNewCheckStartsItAgain() {
        val window = on()
        window.end()
        assertTrue(window.requestCheck())
        assertTrue(window.onCheckPassed(start + 10_000, appUnlocked = true))
        assertEquals(180_000L, window.timeLeft(start + 10_000))
    }

    @Test
    fun aRestart_isANewWindow_off() {
        on()
        assertNull(ScreenshotWindow().timeLeft(start + 1))
    }
}
