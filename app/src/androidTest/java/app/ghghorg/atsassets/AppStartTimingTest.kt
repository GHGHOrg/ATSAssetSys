package app.ghghorg.atsassets

import android.os.Debug
import android.os.Process
import android.os.SystemClock
import android.system.Os
import android.system.OsConstants
import android.util.Log
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.ghghorg.atsassets.core.ScaleHistories
import app.ghghorg.atsassets.core.ScaleShape
import app.ghghorg.atsassets.core.TransactionType
import app.ghghorg.atsassets.data.CashEntryRow
import app.ghghorg.atsassets.data.DatabaseKey
import app.ghghorg.atsassets.data.PortfolioRow
import app.ghghorg.atsassets.data.SqlCipherOpener
import app.ghghorg.atsassets.data.allocationRows
import app.ghghorg.atsassets.data.toRow
import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
import org.junit.Assert.fail
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.io.File

/**
 * Iteration 15c (section 9, part 2, item 4; decision 3): how long the app takes, on the emulator, from the lock prompt
 * taking the test PIN to Overall showing its net worth, and from a tap on Cash to the Cash list showing its rows, with
 * 300 transactions of test data (ScaleHistories, spread). Three runs, each a new MainActivity with a new ViewModel that
 * loads from the database. Times only go to logcat, under ScreenTestTiming: no amount, ticker or name. Not a cold start:
 * the process is already running (15c section 8). It fails only if a screen takes over 120 s.
 */
@RunWith(AndroidJUnit4::class)
class AppStartTimingTest {
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(DeviceUnlockedRule()).around(composeRule)

    @Test
    fun run1() = timeOneStart(1)

    @Test
    fun run2() = timeOneStart(2)

    @Test
    fun run3() = timeOneStart(3)

    private fun timeOneStart(run: Int) {
        val unlocked = composeRule.unlockAppTimed()
        val atUnlock = Usage.now()
        val overall =
            stamp("Overall's net worth") {
                composeRule.onAllNodes(hasContentDescription(string(R.string.net_worth_label), substring = true)).fetchSemanticsNodes()
                    .isNotEmpty()
            }
        val atOverall = Usage.now()
        val tapped = SystemClock.uptimeMillis()
        val atTap = Usage.now()
        composeRule.onNode(isSelectable() and hasText(string(R.string.tab_cash))).performClick()
        // Every trade row's spoken label names its ticker, and the recipe's tickers start with S0.
        val cash =
            stamp("the Cash list's rows") {
                composeRule.onAllNodes(hasContentDescription(TICKER_START, substring = true)).fetchSemanticsNodes().isNotEmpty()
            }
        val atCash = Usage.now()
        val result = (overall - unlocked) to (cash - tapped)
        results += result
        Log.i(TAG, "run $run of 3, $SIZE transactions: load and Overall ${result.first} ms, Cash ${result.second} ms")
        // 15c step 7 (your B): where each phase's time goes. Times only.
        Log.i(TAG, "run $run of 3, CPU in load and Overall: ${atOverall - atUnlock}")
        Log.i(TAG, "run $run of 3, CPU in Cash: ${atCash - atTap}")
        // 15c step 7: logged by the third run to finish, while Gradle still keeps its logcat, not after the class (section
        // 14, item 5). JUnit does not promise the methods' order.
        if (results.size == 3) {
            val load = results.map { it.first }.sorted()[1]
            val cash = results.map { it.second }.sorted()[1]
            Log.i(TAG, "median of 3, $SIZE transactions: load and Overall $load ms, Cash $cash ms")
        }
    }

    // The uptime at which [condition] first holds; fails after 120 s. Each check waits for the app to be idle.
    private fun stamp(
        what: String,
        condition: () -> Boolean,
    ): Long {
        val end = SystemClock.uptimeMillis() + TIMEOUT_MS
        while (SystemClock.uptimeMillis() < end) {
            if (condition()) return SystemClock.uptimeMillis()
            SystemClock.sleep(20)
        }
        fail("$what did not show within $TIMEOUT_MS ms (over 120 s)")
        return end
    }

    /**
     * 15c step 7: the CPU time this process (all its threads, this test's own included) and its main thread have used,
     * and the time spent collecting garbage, at one moment, in ms. A phase's main-thread CPU near its wall time means the
     * app's own work; process CPU well above main-thread CPU means other threads compete; both low means it waits.
     */
    private class Usage(
        val processCpu: Long,
        val mainCpu: Long,
        val gc: Long,
    ) {
        operator fun minus(start: Usage): String {
            val process = processCpu - start.processCpu
            val main = mainCpu - start.mainCpu
            return "process $process ms, main thread $main ms, garbage collection ${gc - start.gc} ms"
        }

        companion object {
            fun now(): Usage {
                val gc = Debug.getRuntimeStat("art.gc.gc-time")?.toLongOrNull() ?: -1L
                return Usage(Process.getElapsedCpuTime(), mainThreadCpu(), gc)
            }

            // The main thread's id is the process id; its user and system times are fields 14 and 15 of its stat line,
            // in clock ticks. The name in brackets can hold spaces, so the fields are counted after its closing bracket.
            private fun mainThreadCpu(): Long {
                val fields = File("/proc/self/task/${Process.myPid()}/stat").readText().substringAfterLast(") ").split(" ")
                val ticks = fields[11].toLong() + fields[12].toLong()
                return ticks * 1_000L / Os.sysconf(OsConstants._SC_CLK_TCK)
            }
        }
    }

    companion object {
        private const val TAG = "ScreenTestTiming"
        private const val SIZE = 300
        private const val TICKER_START = "S0"
        private const val TIMEOUT_MS = 120_000L
        private val results = mutableListOf<Pair<Long, Long>>()

        /** Clears the app's database and writes the test data, before any of the three runs opens the app. */
        @BeforeClass
        @JvmStatic
        fun seed() {
            clear()
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val key = DatabaseKey(context.noBackupFilesDir)
            val database = SqlCipherOpener(context).open(key.loadOrCreatePassphrase(), key.databaseFile)
            val history = ScaleHistories.build(SIZE, ScaleShape.SPREAD)
            try {
                runBlocking {
                    history.portfolios.forEach { database.portfolioDao().insert(PortfolioRow(it.id, it.name, it.createdSeq)) }
                    database.cashEntryDao().insertAll(
                        history.cashEntries.map {
                            CashEntryRow(
                                id = it.id,
                                date = it.date.toString(),
                                amountCents = it.amountCents,
                                type = it.type.name,
                                note = it.note,
                                enteredSeq = it.enteredSeq,
                                reversesId = it.reversesId,
                                transactionId = it.transactionId,
                            )
                        },
                    )
                    val transactions = database.transactionDao()
                    for (transaction in history.transactions) {
                        transactions.insertTransaction(transaction.toRow())
                        if (transaction.type == TransactionType.SELL) transactions.insertAllocations(transaction.allocationRows())
                    }
                }
            } finally {
                database.close()
            }
        }

        /** Clears the database, so the other screen tests see the app as before. */
        @AfterClass
        @JvmStatic
        fun clearAfter() {
            clear()
        }

        // The database and its passphrase file (DatabaseKey); the Keystore key stays, as on an installed app.
        private fun clear() {
            val directory = InstrumentationRegistry.getInstrumentation().targetContext.noBackupFilesDir
            directory.listFiles()
                ?.filter { it.name.startsWith(DatabaseKey.DATABASE_NAME) || it.name == DatabaseKey.BLOB_NAME }
                ?.forEach { if (!it.delete()) fail("Could not delete ${it.name}") }
        }
    }
}
