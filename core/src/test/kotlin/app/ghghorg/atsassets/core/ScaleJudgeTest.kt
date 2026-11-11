package app.ghghorg.atsassets.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

// 24a decision 1 (your B): a failed check is measured again at once, up to 3 tries in all; any try that passes is enough.
// Made-up measurements; the figures of the red run of 2026-11-12 00:19 ("2 netWorth", SPREAD) stand in for a busy try.
class ScaleJudgeTest {
    private val netWorth = ScaleCheck("2 netWorth", ScaleShape.SPREAD, bound = 8.0, limit = 60.0)
    private val normal = ScaleTry(at1000 = 2.3, at3000 = 7.0)
    private val busy = ScaleTry(at1000 = 2.882, at3000 = 29.175)
    private val overItsTime = ScaleTry(at1000 = 9.0, at3000 = 61.0)

    /** The tries after the first, in order; counts how many were asked for. */
    private class Again(
        vararg tries: ScaleTry,
    ) : () -> ScaleTry {
        private val left = tries.toMutableList()
        var asked = 0

        override fun invoke(): ScaleTry {
            asked++
            if (left.isEmpty()) fail("measured again more often than the test planned")
            return left.removeAt(0)
        }
    }

    @Test
    fun aFirstTryThatPasses_isMeasuredOnce() {
        val again = Again()
        val verdict = ScaleJudge.judge(netWorth, normal, again)
        assertNull(verdict.problem)
        assertEquals(listOf(normal), verdict.tries)
        assertEquals(0, again.asked)
    }

    @Test
    fun aBusyFirstTry_thenANormalOne_passesOnTheSecond() {
        val again = Again(normal)
        val verdict = ScaleJudge.judge(netWorth, busy, again)
        assertNull(verdict.problem)
        assertEquals(listOf(busy, normal), verdict.tries)
    }

    @Test
    fun twoBusyTries_thenANormalOne_passesOnTheThird() {
        val again = Again(busy, normal)
        val verdict = ScaleJudge.judge(netWorth, busy, again)
        assertNull(verdict.problem)
        assertEquals(listOf(busy, busy, normal), verdict.tries)
    }

    // A real slowdown is slow on every try: the check fails after 3 tries and is not measured a fourth time.
    @Test
    fun slowOnEveryTry_failsAfterThreeTries() {
        val again = Again(busy, busy)
        val verdict = ScaleJudge.judge(netWorth, busy, again)
        assertEquals("2 netWorth | SPREAD: grew 10.1 times, bound 8", verdict.problem)
        assertEquals(3, verdict.tries.size)
        assertEquals(2, again.asked)
    }

    // The fixed time at 3,000 (15c decision 2) is a failed check too, and is measured again in the same way.
    @Test
    fun overItsFixedTime_isMeasuredAgain_andPassesOnANormalTry() {
        assertEquals("2 netWorth | SPREAD: 61.000 ms at 3,000, limit 60.0 ms", ScaleJudge.problem(netWorth, overItsTime))
        val verdict = ScaleJudge.judge(netWorth, overItsTime, Again(normal))
        assertNull(verdict.problem)
        assertEquals(2, verdict.tries.size)
    }

    // A size the timing skipped (15c's cap) is not timed by measuring it again, so it fails at once.
    @Test
    fun aCheckNotTimed_failsAtOnce() {
        val again = Again()
        val verdict = ScaleJudge.judge(netWorth, ScaleTry(at1000 = 2.3, at3000 = null), again)
        assertEquals("2 netWorth | SPREAD: not timed", verdict.problem)
        assertEquals(0, again.asked)
    }

    // Calls 6 and 8 on the spread shape have no growth bound (15c), only their fixed time; that stays so.
    @Test
    fun withNoGrowthBound_onlyTheFixedTimeCounts() {
        val lotChoices = ScaleCheck("6 lotChoices", ScaleShape.SPREAD, bound = null, limit = 7.0)
        val verdict = ScaleJudge.judge(lotChoices, ScaleTry(at1000 = 0.1, at3000 = 2.0), Again())
        assertNull(verdict.problem)
        assertTrue(verdict.tries.size == 1)
    }
}
