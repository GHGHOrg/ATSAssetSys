package app.ghghorg.atsassets.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** K6's reading half, step 2 (spec section 8, file format; iteration 33a): bytes to rows, and the file-level refusals. */
class BackupReaderTest {
    private val header = BackupFile.COLUMNS.joinToString(",")

    /** A row of the 19 columns: the named cells, every other one empty. */
    private fun row(vararg cells: Pair<String, String>): String {
        val values = cells.toMap()
        return BackupFile.COLUMNS.joinToString(",") { values[it] ?: "" }
    }

    private val format = row("row_type" to "format", "value" to "1")

    private fun read(text: String): FileRead = BackupReader.read(text.toByteArray(Charsets.UTF_8))

    private fun rows(text: String): List<FileRow> = (read(text) as FileRead.Read).rows

    private fun refusal(text: String): List<RestoreError> = (read(text) as FileRead.Refused).errors

    @Test
    fun theTemplate_readsAsTheFormatRowAlone() {
        val rows = rows(File("../spec/template-backup.csv").readText())
        assertEquals(1, rows.size)
        assertEquals(2, rows.single().line)
        assertEquals("format", rows.single()["row_type"])
        assertEquals("1", rows.single()["value"])
    }

    // Section 8: with or without a byte-order mark, LF or CRLF, the same rows on the same lines.
    @Test
    fun aByteOrderMark_andCrlf_readTheSame() {
        val lf = "$header\n$format\n${row("row_type" to "portfolio", "portfolio" to "Main")}\n"
        val expected = rows(lf).map { it.line to it.cells }
        assertEquals(expected, rows(Char(0xFEFF) + lf).map { it.line to it.cells })
        assertEquals(expected, rows(lf.replace("\n", "\r\n")).map { it.line to it.cells })
        val bytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + lf.toByteArray(Charsets.UTF_8)
        assertEquals(expected, (BackupReader.read(bytes) as FileRead.Read).rows.map { it.line to it.cells })
    }

    @Test
    fun aFileThatIsNotUtf8_isRefused_withNoLine() {
        val bytes = "$header\n$format\n".toByteArray(Charsets.UTF_8) + byteArrayOf(0xC3.toByte(), 0x28)
        assertEquals(listOf(RestoreError(null, null, RestoreReason.NotUtf8)), (BackupReader.read(bytes) as FileRead.Refused).errors)
        // Windows-1252's "é" is not UTF-8.
        val ansi = "$header\n$format\n${row("row_type" to "portfolio", "portfolio" to "Caf")}".toByteArray(Charsets.UTF_8) + byteArrayOf(0xE9.toByte())
        assertEquals(RestoreReason.NotUtf8, (BackupReader.read(ansi) as FileRead.Refused).errors.single().reason)
    }

    @Test
    fun anEmptyFile_orOnlyBlankLines_hasNoHeader() {
        assertEquals(listOf(RestoreError(null, null, RestoreReason.NoHeader)), refusal(""))
        assertEquals(listOf(RestoreError(null, null, RestoreReason.NoHeader)), refusal("\n\n,,,\n"))
    }

    // Columns are matched by name, so their order is free; the names are read trimmed and in any case.
    @Test
    fun columnsInAnotherOrder_areMatchedByName() {
        val names = BackupFile.COLUMNS.reversed()
        val cells = mapOf("row_type" to "format", "value" to "1")
        val text = names.joinToString(",") { " ${it.uppercase()} " } + "\n" + names.joinToString(",") { cells[it] ?: "" }
        val first = rows(text).single()
        assertEquals("format", first["row_type"])
        assertEquals("1", first["value"])
    }

    // Every header problem is listed, on the header's line, and the check stops there.
    @Test
    fun aMissingUnknownOrRepeatedColumn_refusesTheFile() {
        val names = BackupFile.COLUMNS.filter { it != "note" && it != "ticker" } + listOf("comment", "date")
        val errors = refusal(names.joinToString(",") + "\n" + names.joinToString(",") { "" })
        assertEquals(
            listOf(
                RestoreError(1, null, RestoreReason.UnknownColumn("comment")),
                RestoreError(1, null, RestoreReason.RepeatedColumn("date")),
                RestoreError(1, null, RestoreReason.MissingColumn("ticker")),
                RestoreError(1, null, RestoreReason.MissingColumn("note")),
            ),
            errors,
        )
    }

    // A column with an empty header whose cells are all empty is ignored; one that holds anything is an unknown column.
    @Test
    fun anUnnamedColumn_isIgnoredOnlyWhileItIsEmpty() {
        val empty = "$header,\n$format,\n${row("row_type" to "portfolio", "portfolio" to "Main")},  \n"
        assertEquals(listOf("format", "portfolio"), rows(empty).map { it["row_type"] })
        val used = "$header,\n$format,\n${row("row_type" to "portfolio", "portfolio" to "Main")},x\n"
        assertEquals(listOf(RestoreError(1, null, RestoreReason.UnknownColumn(""))), refusal(used))
    }

    @Test
    fun theFirstRowAfterTheHeader_mustBeTheFormatRow() {
        val portfolio = row("row_type" to "portfolio", "portfolio" to "Main")
        assertEquals(listOf(RestoreError(2, "row_type", RestoreReason.NoFormatRow)), refusal("$header\n$portfolio\n$format\n"))
        assertEquals(listOf(RestoreError(null, null, RestoreReason.NoFormatRow)), refusal("$header\n"))
        // A blank line does not count as a row, though it counts as a line.
        assertEquals(listOf(3 to "format", 4 to "portfolio"), rows("$header\n\n$format\n$portfolio\n").map { it.line to it["row_type"] })
    }

    // Section 8: a missing, non-numeric or unknown version rejects the file; the app reads version 1.
    @Test
    fun theVersion_mustBeAWholeNumberTheAppKnows() {
        fun version(value: String): List<RestoreError> = refusal("$header\n${row("row_type" to "format", "value" to value)}\n")
        assertEquals(listOf(RestoreError(2, "value", RestoreReason.VersionNotWhole)), version(""))
        assertEquals(listOf(RestoreError(2, "value", RestoreReason.VersionNotWhole)), version("1.0"))
        assertEquals(listOf(RestoreError(2, "value", RestoreReason.VersionNotWhole)), version("one"))
        assertEquals(listOf(RestoreError(2, "value", RestoreReason.VersionUnknown("2"))), version("2"))
        assertEquals(listOf(RestoreError(2, "value", RestoreReason.VersionUnknown("0"))), version("0"))
        assertEquals("1", rows("$header\n${row("row_type" to "format", "value" to " 01 ")}\n").single()["value"].trimStart('0'))
    }

    // An all-empty row is ignored but still counts in line numbers, as Notepad shows them (the header is line 1).
    @Test
    fun blankRows_areIgnored_butCountInLineNumbers() {
        val portfolio = row("row_type" to "portfolio", "portfolio" to "Main")
        val rows = rows("$header\n$format\n\n${row()}\n   ,  \n$portfolio\n")
        assertEquals(listOf(2, 6), rows.map { it.line })
    }

    // 31 decision 3: a cell holding a comma or a quote is quoted, a quote inside doubled. Cells are trimmed, `note` is not.
    @Test
    fun quotedCells_andTrimming() {
        val text = "$header\n$format\n" + row("row_type" to " cash ", "date" to " 2026-01-01 ", "note" to "\" Paid, \"\"in full\"\" \"", "amount" to "\"10.00\"")
        val cash = rows(text)[1]
        assertEquals("cash", cash["row_type"])
        assertEquals("2026-01-01", cash["date"])
        assertEquals(" Paid, \"in full\" ", cash["note"])
        assertEquals("10.00", cash["amount"])
        assertEquals(emptyList<RestoreError>(), cash.errors)
    }

    // Section 8: a line break inside a cell is not allowed. The record is one error on the line it starts on, and the
    // lines after it keep Notepad's numbers.
    @Test
    fun aLineBreakInsideACell_isAnError_onTheLineItStarts() {
        val note = row("row_type" to "cash", "note" to "\"two\nlines\"")
        val portfolio = row("row_type" to "portfolio", "portfolio" to "Main")
        val rows = rows("$header\n$format\n$note\n$portfolio\n")
        assertEquals(listOf(2, 3, 5), rows.map { it.line })
        assertEquals(listOf(RestoreError(3, "note", RestoreReason.LineBreakInCell)), rows[1].errors)
        assertEquals(emptyList<RestoreError>(), rows[2].errors)
    }

    @Test
    fun aRowWithMoreOrFewerCellsThanTheHeader_isAnError() {
        val short = "portfolio,,Main"
        val long = row("row_type" to "portfolio", "portfolio" to "Main") + ",extra"
        val rows = rows("$header\n$format\n$short\n$long\n")
        assertEquals(listOf(RestoreError(3, null, RestoreReason.CellCount(3, 19))), rows[1].errors)
        assertEquals(listOf(RestoreError(4, null, RestoreReason.CellCount(20, 19))), rows[2].errors)
        // The cells it has are still read by position.
        assertEquals("Main", rows[1]["portfolio"])
    }

    @Test
    fun theSampleFile_readsEveryRow() {
        val rows = (BackupReader.read(File("../spec/example-backup.csv").readBytes()) as FileRead.Read).rows
        assertEquals(24, rows.size)
        assertEquals((2..25).toList(), rows.map { it.line })
        assertTrue(rows.all { it.errors.isEmpty() })
        assertEquals("L1:10;L2:2", rows.first { it.line == 16 }["lot_picks"])
        assertEquals(BackupFile.COLUMNS.indexOf("note"), (read(File("../spec/example-backup.csv").readText()) as FileRead.Read).columnOrder["note"])
    }
}
