package app.ghghorg.atsassets

import android.os.SystemClock
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
        val overall =
            stamp("Overall's net worth") {
                composeRule.onAllNodes(hasContentDescription(string(R.string.net_worth_label), substring = true)).fetchSemanticsNodes()
                    .isNotEmpty()
            }
        val tapped = SystemClock.uptimeMillis()
        composeRule.onNode(isSelectable() and hasText(string(R.string.tab_cash))).performClick()
        // Every trade row's spoken label names its ticker, and the recipe's tickers start with S0.
        val cash =
            stamp("the Cash list's rows") {
                composeRule.onAllNodes(hasContentDescription(TICKER_START, substring = true)).fetchSemanticsNodes().isNotEmpty()
            }
        val result = (overall - unlocked) to (cash - tapped)
        results += result
        Log.i(TAG, "run $run of 3, $SIZE transactions: load and Overall ${result.first} ms, Cash ${result.second} ms")
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

        /** Logs the medians of the three runs, then clears the database, so the other screen tests see the app as before. */
        @AfterClass
        @JvmStatic
        fun clearAfter() {
            if (results.size == 3) {
                val load = results.map { it.first }.sorted()[1]
                val cash = results.map { it.second }.sorted()[1]
                Log.i(TAG, "median of 3, $SIZE transactions: load and Overall $load ms, Cash $cash ms")
            }
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
