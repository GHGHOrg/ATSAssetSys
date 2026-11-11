package app.ghghorg.atsassets.core

import java.util.Locale

/**
 * One check of HoldingsScaleTest (15c section 9, part 2 and decision 2): a call on a shape, its growth bound from 1,000 to
 * 3,000 transactions (null: none) and its fixed time at 3,000.
 */
internal data class ScaleCheck(
    val call: String,
    val shape: ScaleShape,
    val bound: Double?,
    val limit: Double,
)

/** One try of a check: the median ms per call at 1,000 and at 3,000 transactions, null when not timed. */
internal data class ScaleTry(
    val at1000: Double?,
    val at3000: Double?,
)

/** A check's outcome: every try in order, and why the last one failed, or null when a try passed. */
internal data class ScaleVerdict(
    val tries: List<ScaleTry>,
    val problem: String?,
)

/**
 * 24a decision 1 (your B): a check that fails is measured again at once, up to [TRIES] tries in all, and passes when any
 * try passes. The limits and bounds are 15c's, unchanged. A check that was not timed fails at once: measuring it again
 * would not time it.
 */
internal object ScaleJudge {
    const val TRIES = 3

    /** Judges [first]; while the check fails and was timed, asks [again] for another try, up to [TRIES] in all. */
    fun judge(
        check: ScaleCheck,
        first: ScaleTry,
        again: () -> ScaleTry,
    ): ScaleVerdict {
        val tries = mutableListOf(first)
        var problem = problem(check, first)
        val timed = first.at1000 != null && first.at3000 != null
        while (problem != null && timed && tries.size < TRIES) {
            val next = again()
            tries += next
            problem = problem(check, next)
        }
        return ScaleVerdict(tries, problem)
    }

    /**
     * The test's message for [verdict], or null when it passed: the problem, with its tries named once it was measured
     * again ("(3 of 3 tries)"); a check not timed had no retries, so it names none (24a's Deploy review).
     */
    fun failure(verdict: ScaleVerdict): String? = verdict.problem?.let {
        if (verdict.tries.size > 1) "$it (${verdict.tries.size} of $TRIES tries)" else it
    }

    /** Why [attempt] fails [check], each failed limit on its own line, or null when it passes. */
    fun problem(
        check: ScaleCheck,
        attempt: ScaleTry,
    ): String? {
        val at1000 = attempt.at1000
        val at3000 = attempt.at3000
        if (at1000 == null || at3000 == null) return "${check.call} | ${check.shape}: not timed"
        val found = mutableListOf<String>()
        val ratio = at3000 / at1000
        if (check.bound != null && ratio >= check.bound) {
            found += String.format(Locale.ROOT, "%s | %s: grew %.1f times, bound %.0f", check.call, check.shape, ratio, check.bound)
        }
        if (at3000 >= check.limit) {
            found += String.format(Locale.ROOT, "%s | %s: %.3f ms at 3,000, limit %.1f ms", check.call, check.shape, at3000, check.limit)
        }
        return if (found.isEmpty()) null else found.joinToString("\n")
    }
}
