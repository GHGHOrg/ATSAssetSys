package app.ghghorg.atsassets.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.Locale
import kotlin.math.pow

/**
 * Iteration 15c (section 9, part 2): times the calculations the app makes on every load, save and redraw, on
 * ScaleHistories' recipe, in two shapes and at growing sizes. The table goes to standard output and to
 * core/build/scale/. Step 2 times the code before 15c (HoldingsReference) once, with ATS_SCALE_OLD=1; step 7 adds the
 * new code's run, with its ratio and time bounds.
 */
class HoldingsScaleTest {
    @Test
    fun theRecipeMakesValidHistories() {
        for (shape in ScaleShape.entries) {
            val history = ScaleHistories.build(100, shape)
            val transactions = history.transactions
            for ((portfolio, ticker) in transactions.map { it.portfolioId to it.ticker }.distinct()) {
                assertNull("$shape: $portfolio $ticker goes below 0", Holdings.firstNegative(transactions, portfolio, ticker))
            }
            val byId = transactions.associateBy { it.id }
            for (reversal in transactions.filter { it.type == TransactionType.REVERSAL }) {
                val original = byId.getValue(reversal.reversesId!!)
                assertEquals("$shape: ${reversal.id} is in another holding", original.ticker, reversal.ticker)
                assertEquals("$shape: ${original.id} is reversed twice", 1, transactions.count { it.reversesId == original.id })
            }
            assertNull("$shape: the cash goes below 0", CashRules.replay(history.cashEntries, history.cashEntries.minOf { it.date }))
            assertEquals("$shape: every transaction counted", 100, transactions.size)
        }
    }

    /** Step 2, once: the code before 15c. Some sizes take minutes, so it runs only when asked (15c section 9, part 2, item 3). */
    @Test
    fun timesTheCodeBefore15c() {
        assumeTrue("Set ATS_SCALE_OLD=1 to time the code before 15c (15c step 2)", System.getenv("ATS_SCALE_OLD") == "1")
        val table = ScaleTiming.run(REFERENCE_CALLS, listOf(100, 300, 1_000), capSeconds = 60.0)
        ScaleTiming.write("holdings-scale-before.txt", "The code before 15c (HoldingsReference)", table)
    }

    /**
     * Step 7, every run: the code after 15c at four sizes. The growth from 1,000 to 3,000 transactions stays under its
     * bound (15c section 9, part 2, item 2, step 6), and each call at 3,000 under its fixed time (decision 2). From 24a
     * (decision 1, your B), a check that fails is measured again at 1,000 and 3,000 at once, up to 3 tries in all, and
     * passes when any try passes; every try is in the table.
     */
    @Test
    fun theCodeAfter15c_growsWithinItsBounds() {
        val sizes = listOf(100, 300, 1_000, 3_000)
        val table = ScaleTiming.run(NEW_CALLS, sizes, capSeconds = 60.0)
        val histories = mutableMapOf<Pair<ScaleShape, Int>, ScaleHistory>()
        val retried = mutableListOf<String>()
        val problems = mutableListOf<String>()
        for (call in NEW_CALLS) {
            for (shape in ScaleShape.entries) {
                fun timed(size: Int) = table.single { it.call == call.name && it.shape == shape && it.size == size }.millis

                fun measured(size: Int) = ScaleTiming.measure(call, histories.getOrPut(shape to size) { ScaleHistories.build(size, shape) })
                val check = ScaleCheck(call.name, shape, ratioBound(call.name, shape), LIMITS_AT_3000.getValue(call.name to shape))
                val verdict = ScaleJudge.judge(check, ScaleTry(timed(1_000), timed(3_000))) { ScaleTry(measured(1_000), measured(3_000)) }
                if (verdict.tries.size > 1) {
                    verdict.tries.forEachIndexed { index, attempt -> retried += ScaleTiming.describe(check, index + 1, attempt) }
                }
                verdict.problem?.let { problems += "$it (${verdict.tries.size} of ${ScaleJudge.TRIES} tries)" }
            }
        }
        ScaleTiming.write("holdings-scale-after.txt", "The code after 15c", table, retried)
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    // Calls 6 and 8 are O(n²) on one holding; every other bounded growth is about n log n or less. On the spread
    // shape, calls 6 and 8 have no ratio bound in section 9, only the fixed time.
    private fun ratioBound(
        call: String,
        shape: ScaleShape,
    ): Double? = when {
        call.startsWith("6 ") || call.startsWith("8 ") -> if (shape == ScaleShape.ONE_HOLDING) 20.0 else null
        else -> 8.0
    }

    private companion object {
        // Decision 2: ms per call at 3,000 transactions, about 10 times step 7's measurement on this PC (2026-11-07), rounded up.
        val LIMITS_AT_3000 =
            mapOf(
                ("1 holdings" to ScaleShape.SPREAD) to 65.0,
                ("2 netWorth" to ScaleShape.SPREAD) to 60.0,
                ("3 lots" to ScaleShape.SPREAD) to 55.0,
                ("4 lotsOf" to ScaleShape.SPREAD) to 7.0,
                ("5 firstNegative" to ScaleShape.SPREAD) to 7.0,
                ("6 lotChoices" to ScaleShape.SPREAD) to 7.0,
                ("7 Hide" to ScaleShape.SPREAD) to 15.0,
                ("8 effects" to ScaleShape.SPREAD) to 14.0,
                ("9 pieces" to ScaleShape.SPREAD) to 1.6,
                ("1 holdings" to ScaleShape.ONE_HOLDING) to 75.0,
                ("2 netWorth" to ScaleShape.ONE_HOLDING) to 75.0,
                ("3 lots" to ScaleShape.ONE_HOLDING) to 65.0,
                ("4 lotsOf" to ScaleShape.ONE_HOLDING) to 65.0,
                ("5 firstNegative" to ScaleShape.ONE_HOLDING) to 75.0,
                ("6 lotChoices" to ScaleShape.ONE_HOLDING) to 65.0,
                ("7 Hide" to ScaleShape.ONE_HOLDING) to 135.0,
                ("8 effects" to ScaleShape.ONE_HOLDING) to 85.0,
                ("9 pieces" to ScaleShape.ONE_HOLDING) to 3.5,
            )

        // The same calls on the code after 15c; the exponent is the expected growth, for the cap.
        val NEW_CALLS =
            listOf(
                ScaleCall("1 holdings", 1) { Holdings.holdings(it.transactions, it.lastDate) },
                ScaleCall("2 netWorth", 1) { Valuation.netWorth(it.cashEntries, it.transactions, it.lastDate) },
                ScaleCall("3 lots", 1) { Holdings.lots(it.transactions, it.lastDate) },
                ScaleCall("4 lotsOf", 1) { Holdings.lotsOf(it.transactions, it.largestPortfolioId, it.largestTicker, it.lastDate) },
                ScaleCall("5 firstNegative", 1) { Holdings.firstNegative(it.transactions, it.largestPortfolioId, it.largestTicker) },
                ScaleCall("6 lotChoices", 2) {
                    Sells.lotChoices(it.transactions, it.largestPortfolioId, it.largestTicker, it.lastDate, it.nextSeq)
                },
                ScaleCall("7 Hide", 1) {
                    Hiding.offered(it.transactions, emptySet(), it.largestPortfolioId, it.largestTicker) to
                        Hiding.unhiddenBy(it.transactions, it.fiveHidden)
                },
                ScaleCall("8 effects", 2) {
                    val split = it.lastSplit
                    val reversal = it.draftReversalOfLastSplit
                    Splits.effect(it.transactions, split) to Reversals.effect(it.transactions + reversal, reversal)
                },
                ScaleCall("9 pieces", 1) { Sells.pieces(it.transactions, it.lastSell) },
            )

        // The calls of 15c section 9, part 2, item 1, on the code before 15c; the exponent is today's growth, for the cap.
        val REFERENCE_CALLS =
            listOf(
                ScaleCall("1 holdings", 3) { HoldingsReference.holdings(it.transactions, it.lastDate) },
                ScaleCall("2 netWorth", 3) { HoldingsReference.netWorth(it.cashEntries, it.transactions, it.lastDate) },
                ScaleCall("3 lots", 3) { HoldingsReference.lots(it.transactions, it.lastDate) },
                ScaleCall("4 lotsOf", 3) {
                    HoldingsReference.lotsOf(it.transactions, it.largestPortfolioId, it.largestTicker, it.lastDate)
                },
                ScaleCall("5 firstNegative", 4) {
                    HoldingsReference.firstNegative(it.transactions, it.largestPortfolioId, it.largestTicker)
                },
                ScaleCall("6 lotChoices", 4) {
                    HoldingsReference.lotChoices(it.transactions, it.largestPortfolioId, it.largestTicker, it.lastDate, it.nextSeq)
                },
                ScaleCall("7 Hide", 3) {
                    HoldingsReference.hidingOffered(it.transactions, emptySet(), it.largestPortfolioId, it.largestTicker) to
                        HoldingsReference.unhiddenBy(it.transactions, it.fiveHidden)
                },
                ScaleCall("8 effects", 3) {
                    val split = it.lastSplit
                    val reversal = it.draftReversalOfLastSplit
                    HoldingsReference.splitEffect(it.transactions, split) to
                        HoldingsReference.reversalEffect(it.transactions + reversal, reversal)
                },
                ScaleCall("9 pieces", 2) { HoldingsReference.pieces(it.transactions, it.lastSell) },
            )
    }
}

/** One timed call: [exponent] is its expected growth, used to skip a size that would take too long. */
internal class ScaleCall(
    val name: String,
    val exponent: Int,
    val run: (ScaleHistory) -> Any?,
)

internal val ScaleHistory.nextSeq: Long get() = Transactions.nextSeq(cashEntries, transactions)

private val ScaleHistory.ofLargest: List<Transaction>
    get() = transactions.filter { it.portfolioId == largestPortfolioId && it.ticker == largestTicker }

internal val ScaleHistory.lastSplit: Transaction get() = ofLargest.last { it.type == TransactionType.SPLIT }

internal val ScaleHistory.lastSell: Transaction get() = ofLargest.last { it.type == TransactionType.SELL }

internal val ScaleHistory.draftReversalOfLastSplit: Transaction
    get() = Reversals.draft(cashEntries, transactions, lastSplit, "draft", lastDate)

internal val ScaleHistory.fiveHidden: List<HiddenHolding>
    get() = transactions.map { HiddenHolding(it.portfolioId, it.ticker) }.distinct().take(5)

/** One row of the table: a call, a shape and a size, with the median time per call, or why it was not run. */
internal data class ScaleRow(
    val call: String,
    val shape: ScaleShape,
    val size: Int,
    val millis: Double?,
    val note: String = "",
)

/**
 * The timing of 15c section 9, part 2, item 2: each call warmed up, then timed in blocks of at least 50 ms, five
 * blocks, the median kept. A call that takes over 2 s alone is warmed up once and timed three times, one call each, or
 * only once when three runs would pass [capSeconds]. A size whose whole measurement is estimated (from the size before,
 * times the growth) at over [capSeconds] is skipped and noted.
 */
internal object ScaleTiming {
    private const val BLOCK_NANOS = 50_000_000L
    private const val SLOW_NANOS = 2_000_000_000L

    // Read by no one, but written each call, so the compiler cannot leave a call out as unused.
    @Volatile
    private var sink: Any? = null

    fun run(
        calls: List<ScaleCall>,
        sizes: List<Int>,
        capSeconds: Double,
    ): List<ScaleRow> {
        val rows = mutableListOf<ScaleRow>()
        PROGRESS.parentFile.mkdirs()
        PROGRESS.writeText("")
        for (shape in ScaleShape.entries) {
            val histories = sizes.associateWith { ScaleHistories.build(it, shape) }
            for (call in calls) {
                var previous: Pair<Int, Double>? = null
                var skipped = false
                for (size in sizes) {
                    if (skipped) {
                        rows += ScaleRow(call.name, shape, size, null, "skipped, a smaller size was skipped")
                        continue
                    }
                    // The whole measurement: a slow call runs four times (a warm-up and three runs), a faster one at least
                    // eight times (three warm-ups and five blocks of at least 50 ms). 15c step 2: the cap first counted one
                    // call only, and a run took too long.
                    val perCall = previous?.let { (n, ms) -> ms * (size.toDouble() / n).pow(call.exponent) / 1_000.0 }
                    val whole = perCall?.let { if (it * 1e9 > SLOW_NANOS) it * 4 else it * 8 + 0.3 }
                    if (whole != null && whole > capSeconds) {
                        val note = String.format(Locale.ROOT, "skipped, estimated about %.0f s per call", perCall)
                        rows += ScaleRow(call.name, shape, size, null, note)
                        skipped = true
                        progress("${call.name} | $shape | $size | $note")
                        continue
                    }
                    val started = System.nanoTime()
                    val millis = median(call, histories.getValue(size), capSeconds) / 1_000_000.0
                    rows += ScaleRow(call.name, shape, size, millis)
                    previous = size to millis
                    val took = (System.nanoTime() - started) / 1e9
                    progress(
                        String.format(
                            Locale.ROOT,
                            "%s | %s | %d | %.3f ms per call, measured in %.1f s",
                            call.name,
                            shape,
                            size,
                            millis,
                            took,
                        ),
                    )
                }
            }
        }
        return rows
    }

    // One line per call and size, so a long run can be followed in core/build/scale/progress.txt.
    private val PROGRESS = File("build/scale/progress.txt")

    private fun progress(line: String) = PROGRESS.appendText(line + "\n")

    /** [call]'s median ms per call on [history], measured as in [run]; for a check measured again (24a decision 1). */
    fun measure(
        call: ScaleCall,
        history: ScaleHistory,
        capSeconds: Double = 60.0,
    ): Double = median(call, history, capSeconds) / 1_000_000.0

    /** One try of a check measured again, as a line of the table's last part. */
    fun describe(
        check: ScaleCheck,
        number: Int,
        attempt: ScaleTry,
    ): String {
        val growth = if (attempt.at1000 != null && attempt.at3000 != null) attempt.at3000 / attempt.at1000 else null
        val result = if (ScaleJudge.problem(check, attempt) == null) "passes" else "fails"
        return String.format(
            Locale.ROOT,
            "%s | %s | try %d | %s ms at 1,000 | %s ms at 3,000 | growth %s | %s",
            check.call,
            check.shape,
            number,
            attempt.at1000?.let { String.format(Locale.ROOT, "%.3f", it) } ?: "not timed",
            attempt.at3000?.let { String.format(Locale.ROOT, "%.3f", it) } ?: "not timed",
            growth?.let { String.format(Locale.ROOT, "%.1f", it) } ?: "-",
            result,
        )
    }

    private fun median(
        call: ScaleCall,
        history: ScaleHistory,
        capSeconds: Double,
    ): Double {
        val first = once(call, history)
        if (first > SLOW_NANOS) {
            // A warm-up already taken; three runs only while they fit the cap, else the warm-up's time alone.
            if (first * 3 / 1e9 > capSeconds) return first.toDouble()
            return List(3) { once(call, history).toDouble() }.sorted()[1]
        }
        repeat(2) { once(call, history) }
        val blocks =
            List(5) {
                var count = 0
                val start = System.nanoTime()
                var elapsed: Long
                do {
                    sink = call.run(history)
                    count++
                    elapsed = System.nanoTime() - start
                } while (elapsed < BLOCK_NANOS)
                elapsed.toDouble() / count
            }
        return blocks.sorted()[2]
    }

    private fun once(
        call: ScaleCall,
        history: ScaleHistory,
    ): Long {
        val start = System.nanoTime()
        sink = call.run(history)
        return System.nanoTime() - start
    }

    /**
     * The table as text, with each size's growth over the size before, then each check measured again with every try
     * (24a decision 1); written to core/build/scale/[name] and printed.
     */
    fun write(
        name: String,
        title: String,
        rows: List<ScaleRow>,
        retried: List<String>? = null,
    ) {
        val text =
            buildString {
                appendLine(title)
                appendLine("call | shape | transactions | median ms per call | growth over the size before")
                for (row in rows) {
                    val before = rows.lastOrNull { it.call == row.call && it.shape == row.shape && it.size < row.size }
                    val growth =
                        if (row.millis != null && before?.millis != null) {
                            String.format(Locale.ROOT, "%.1f", row.millis / before.millis)
                        } else {
                            ""
                        }
                    val millis = row.millis?.let { String.format(Locale.ROOT, "%.3f", it) } ?: row.note
                    appendLine("${row.call} | ${row.shape} | ${row.size} | $millis | $growth")
                }
                if (retried != null) {
                    appendLine()
                    appendLine("Checks measured again (24a decision 1), every try:")
                    if (retried.isEmpty()) appendLine("none")
                    retried.forEach { appendLine(it) }
                }
            }
        print(text)
        File("build/scale").mkdirs()
        File("build/scale", name).writeText(text)
    }
}
