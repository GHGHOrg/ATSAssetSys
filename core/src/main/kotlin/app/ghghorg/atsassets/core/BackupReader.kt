package app.ghghorg.atsassets.core

import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.util.Locale

/**
 * One record of a backup file (spec section 8): its [line] as Notepad shows it, the line it starts on; its [cells] by
 * column name, each trimmed except `note`, empty when the record is short; and the errors of its form ([errors]: too
 * many or too few cells, a line break inside a cell).
 */
internal class FileRow(
    val line: Int,
    val cells: Map<String, String>,
    val errors: List<RestoreError>,
) {
    operator fun get(column: String): String = cells[column].orEmpty()
}

internal sealed interface FileRead {
    /** A file-level failure: the check stops (section 8, restore step 2). */
    data class Refused(
        val errors: List<RestoreError>,
    ) : FileRead

    /**
     * The rows after the header, the `format` row first, all-empty rows left out; [columnOrder] is each column's place in
     * the file's header, for the report's order within a line (33a decision 4).
     */
    data class Read(
        val rows: List<FileRow>,
        val columnOrder: Map<String, Int>,
    ) : FileRead
}

/**
 * K6's reading half, the file level (spec section 8, file format; 33a step 2): bytes to rows. UTF-8 or refused, with or
 * without a byte-order mark, LF, CRLF or CR; the quoting the export writes (31 decision 3); the header's 19 columns matched
 * by name; the `format` row first, with a version this app reads.
 */
internal object BackupReader {
    private const val ROW_TYPE = "row_type"
    private const val VALUE = "value"
    private const val NOTE = "note"
    private val WHOLE = Regex("-?[0-9]+")
    private val BYTE_ORDER_MARK = Char(0xFEFF).toString()

    fun read(bytes: ByteArray): FileRead {
        val text = decoded(bytes) ?: return refused(RestoreError(null, null, RestoreReason.NotUtf8))
        val records = records(text.removePrefix(BYTE_ORDER_MARK)).filterNot { record -> record.cells.all { it.isBlank() } }
        val header = records.firstOrNull() ?: return refused(RestoreError(null, null, RestoreReason.NoHeader))
        val data = records.drop(1)
        val (positions, headerErrors) = columns(header, data)
        if (headerErrors.isNotEmpty()) return FileRead.Refused(headerErrors)
        val rows = data.map { row(it, positions, header.cells.size) }
        formatRefusal(rows.firstOrNull())?.let { return refused(it) }
        return FileRead.Read(rows, positions)
    }

    private fun refused(error: RestoreError): FileRead = FileRead.Refused(listOf(error))

    private fun decoded(bytes: ByteArray): String? = try {
        Charsets.UTF_8
            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    } catch (e: CharacterCodingException) {
        null
    }

    /** One record as written: the line it starts on, its cells untrimmed, and the cell that holds a line break, if one does. */
    private class RawRecord(
        val line: Int,
        val cells: List<String>,
        val lineBreakCell: Int?,
    )

    // Records split at line ends outside quotes; a quoted cell may run over a line end, which is then an error of its
    // record (section 8). LF, CRLF and a lone CR each end a line, as Notepad counts them.
    private fun records(text: String): List<RawRecord> {
        val out = ArrayList<RawRecord>()
        val cells = ArrayList<String>()
        val cell = StringBuilder()
        var line = 1
        var start = 1
        var quoted = false
        var cellQuoted = false
        var lineBreakCell: Int? = null

        fun endCell() {
            cells.add(cell.toString())
            cell.setLength(0)
            cellQuoted = false
        }

        fun endRecord() {
            endCell()
            out.add(RawRecord(start, cells.toList(), lineBreakCell))
            cells.clear()
            lineBreakCell = null
        }
        var i = 0
        while (i < text.length) {
            val c = text[i]
            val lineEnd = c == '\n' || c == '\r'
            if (lineEnd && c == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
            when {
                quoted && c == '"' && i + 1 < text.length && text[i + 1] == '"' -> {
                    cell.append('"')
                    i++
                }

                quoted && c == '"' -> quoted = false

                quoted && lineEnd -> {
                    cell.append('\n')
                    if (lineBreakCell == null) lineBreakCell = cells.size
                    line++
                }

                quoted -> cell.append(c)

                c == '"' && !cellQuoted && cell.isBlank() -> {
                    cell.setLength(0)
                    quoted = true
                    cellQuoted = true
                }

                c == ',' -> endCell()

                lineEnd -> {
                    endRecord()
                    line++
                    start = line
                }

                else -> cell.append(c)
            }
            i++
        }
        endRecord()
        return out
    }

    // The header's columns by name, read trimmed and in any case (section 8: matched by name, order free), and its errors
    // in the header's order, then each missing column in the export's order. A column with no name is ignored while its
    // cells are all empty.
    private fun columns(
        header: RawRecord,
        data: List<RawRecord>,
    ): Pair<Map<String, Int>, List<RestoreError>> {
        val positions = LinkedHashMap<String, Int>()
        val errors = ArrayList<RestoreError>()
        header.cells.forEachIndexed { index, raw ->
            val name = raw.trim().lowercase(Locale.ROOT)
            when {
                name.isEmpty() ->
                    if (data.any { index < it.cells.size && it.cells[index].isNotBlank() }) {
                        errors.add(RestoreError(header.line, null, RestoreReason.UnknownColumn("")))
                    }

                name !in BackupFile.COLUMNS -> errors.add(RestoreError(header.line, null, RestoreReason.UnknownColumn(raw.trim())))

                name in positions -> errors.add(RestoreError(header.line, null, RestoreReason.RepeatedColumn(name)))

                else -> positions[name] = index
            }
        }
        BackupFile.COLUMNS.filter { it !in positions }.forEach { errors.add(RestoreError(header.line, null, RestoreReason.MissingColumn(it))) }
        return positions to errors
    }

    // A record's cells by column, trimmed except `note`; read by position even when it has more or fewer cells than the
    // header, which is its error.
    private fun row(
        record: RawRecord,
        positions: Map<String, Int>,
        width: Int,
    ): FileRow {
        val cells = positions.mapValues { (name, index) -> record.cells.getOrNull(index).orEmpty().let { if (name == NOTE) it else it.trim() } }
        val errors = ArrayList<RestoreError>()
        if (record.cells.size != width) errors.add(RestoreError(record.line, null, RestoreReason.CellCount(record.cells.size, width)))
        record.lineBreakCell?.let { index ->
            val column = positions.entries.firstOrNull { it.value == index }?.key
            errors.add(RestoreError(record.line, column, RestoreReason.LineBreakInCell))
        }
        return FileRow(record.line, cells, errors)
    }

    // Section 8, format version: the first row after the header is the `format` row, its version a whole number this app
    // reads; a leading zero is the same number.
    private fun formatRefusal(first: FileRow?): RestoreError? {
        if (first == null) return RestoreError(null, null, RestoreReason.NoFormatRow)
        if (first[ROW_TYPE].lowercase(Locale.ROOT) != "format") return RestoreError(first.line, ROW_TYPE, RestoreReason.NoFormatRow)
        val version = first[VALUE]
        if (!WHOLE.matches(version)) return RestoreError(first.line, VALUE, RestoreReason.VersionNotWhole)
        if (BigInteger(version) != BigInteger.valueOf(BackupFile.FORMAT_VERSION.toLong())) {
            return RestoreError(first.line, VALUE, RestoreReason.VersionUnknown(version))
        }
        return null
    }
}
