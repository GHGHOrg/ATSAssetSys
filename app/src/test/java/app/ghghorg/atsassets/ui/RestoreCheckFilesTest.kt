package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.BackupCheck
import app.ghghorg.atsassets.core.RestoreCheck
import app.ghghorg.atsassets.core.RestoreContext
import app.ghghorg.atsassets.core.RestoreReason
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

// 33b decision 6: the synthetic files for the phone check (app/src/test/resources/restore-check; made-up data only, not
// packaged in the app). Each file gives the errors written in expected-reports.txt beside it, which the phone check
// compares with the report on the screen; together the files give every one of section 8's reasons and 33a's additions,
// so every sentence can be seen on the phone. The 10 MB file of decision 4 is made for the phone only (too large to keep).
class RestoreCheckFilesTest {
    private val dir: File =
        requireNotNull(File(requireNotNull(javaClass.classLoader?.getResource("restore-check/expected-reports.txt")).toURI()).parentFile)

    // Each file's errors, one line each: "<line> <column> <reason>", "null" where none applies; a file that passes says so.
    private fun reports(): String = buildString {
        for (file in dir.listFiles { f -> f.name.endsWith(".csv") }.orEmpty().sortedBy { it.name }) {
            var made = 0
            val context = RestoreContext({ "app-${++made}" }, setOf("finnhub"), "finnhub", { type, date -> "Reversal of the $type of $date ET" })
            append("== ${file.name}\n")
            when (val result = BackupCheck.check(file.readBytes(), context)) {
                is RestoreCheck.Failed -> result.errors.forEach { append("${it.line} ${it.column} ${it.reason}\n") }
                is RestoreCheck.Passed -> append("PASSED ${result.summary}\n")
            }
        }
    }

    @Test
    fun eachFile_givesItsExpectedErrors() {
        assertEquals(File(dir, "expected-reports.txt").readText().replace("\r\n", "\n"), reports())
    }

    @Test
    fun theFiles_giveEveryKindOfReason() {
        val type = RestoreReason::class.java
        val kinds = type.declaredClasses.filter { type.isAssignableFrom(it) }.map { it.simpleName }.toSet()
        val given =
            reports()
                .lines()
                .filter { it.isNotEmpty() && !it.startsWith("==") && !it.startsWith("PASSED") }
                .map { it.split(' ', limit = 3)[2].substringBefore('(') }
                .toSet()
        assertEquals(kinds, given)
    }
}
