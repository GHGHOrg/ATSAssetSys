package app.ghghorg.atsassets.core

import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.Locale

/** The row types of spec section 8, each by the word the file uses. */
internal enum class RowKind(
    val word: String,
) {
    FORMAT("format"),
    PORTFOLIO("portfolio"),
    CASH("cash"),
    BUY("buy"),
    SELL("sell"),
    SPLIT("split"),
    REVERSAL("reversal"),
    HIDDEN("hidden"),
    OVERRIDE("override"),
    LIST_VALUE("list_value"),
    SOURCE_TEXT("source_text"),
    SETTING("setting"),
}

/** One row that passed its own rules (33a step 3), by its [line]. Portfolios are named as written; ids as written. */
internal sealed interface FileRecord {
    val line: Int
}

/** A cash entry, a buy, a sell or a split, or a reversal: the records replayed in date order, then file order (4.1). */
internal sealed interface EntryRecord : FileRecord {
    val date: LocalDate
}

internal data class PortfolioRecord(
    override val line: Int,
    val name: String,
) : FileRecord

/** [cents] signed as in the file (4.2). */
internal data class CashRecord(
    override val line: Int,
    val id: String?,
    override val date: LocalDate,
    val type: CashType,
    val cents: Long,
    val note: String,
) : EntryRecord

internal data class BuyRecord(
    override val line: Int,
    val id: String?,
    val portfolio: String,
    override val date: LocalDate,
    val ticker: String,
    val quantity: BigDecimal,
    val cents: Long,
    val ratio: SplitRatio?,
) : EntryRecord

/** Each pick names its lot by the file's id, as written. */
internal data class SellRecord(
    override val line: Int,
    val id: String?,
    val portfolio: String,
    override val date: LocalDate,
    val ticker: String,
    val quantity: BigDecimal,
    val cents: Long,
    val picks: List<Pick>,
) : EntryRecord

internal data class SplitRecord(
    override val line: Int,
    val id: String?,
    val portfolio: String,
    override val date: LocalDate,
    val ticker: String,
    val ratio: SplitRatio,
) : EntryRecord

/** [note] as written, empty when the file gives none (only a cash reversal may have one). */
internal data class ReversalRecord(
    override val line: Int,
    val reverses: String,
    override val date: LocalDate,
    val note: String,
) : EntryRecord

internal data class HiddenRecord(
    override val line: Int,
    val portfolio: String,
    val ticker: String,
) : FileRecord

internal data class OverrideRecord(
    override val line: Int,
    val portfolio: String,
    val ticker: String,
    val sector: String?,
    val geography: String?,
) : FileRecord

internal data class ListValueRecord(
    override val line: Int,
    val list: ClassList,
    val value: String,
) : FileRecord

internal data class SourceTextRecord(
    override val line: Int,
    val list: ClassList,
    val value: String,
    val text: String,
) : FileRecord

/** [key] and [value] in the file's own words, in small letters (case is ignored, section 8). */
internal data class SettingRecord(
    override val line: Int,
    val key: String,
    val value: String,
) : FileRecord

/**
 * A row that failed its own rules, with what could still be read of it, so a row that points to it says "depends on
 * line N" (33a decision 2): its kind, a well-formed id, the portfolio named, a well-formed ticker, a known cash type.
 */
internal data class FailedRow(
    val line: Int,
    val kind: RowKind?,
    val id: String?,
    val portfolio: String?,
    val ticker: String?,
    val cashType: CashType?,
)

internal data class ParsedRows(
    val records: List<FileRecord>,
    val failed: List<FailedRow>,
    val errors: List<RestoreError>,
)

/**
 * K6's reading half, each row's own rules (spec section 8, records, settings keys and rejection reasons; 33a step 3):
 * the row type, the cells it needs and the ones it does not use, then each cell's form. Every error of a row is listed;
 * the rules between rows and the replay are [BackupCheck]'s. The `format` row was checked by [BackupReader].
 */
internal object BackupRows {
    const val SETTING_AUTO_LOCK = "auto_lock"
    const val SETTING_PRICE_SOURCE = "price_source"
    const val SETTING_TAB_ORDER = "tab_order"

    /** The `auto_lock` words of section 8, each with its choice. */
    val AUTO_LOCK_WORDS: Map<String, AutoLock> =
        mapOf(
            "every_exit" to AutoLock.EVERY_LEAVE,
            "1_min" to AutoLock.IDLE_1_MIN,
            "5_min" to AutoLock.IDLE_5_MIN,
            "phone_lock_only" to AutoLock.PHONE_LOCK_OR_RESTART,
        )

    private val CASH_WORDS: Map<String, CashType> =
        mapOf(
            "opening_balance" to CashType.OPENING_BALANCE,
            "deposit" to CashType.DEPOSIT,
            "withdrawal" to CashType.WITHDRAWAL,
            "interest_dividend" to CashType.INTEREST_DIVIDEND,
            "adjustment" to CashType.ADJUSTMENT,
        )

    // Each row type's columns, besides `row_type`: those it needs, then those it may have (section 8, records).
    private val NEEDS: Map<RowKind, List<String>> =
        mapOf(
            RowKind.FORMAT to listOf("value"),
            RowKind.PORTFOLIO to listOf("portfolio"),
            RowKind.CASH to listOf("date", "cash_type", "amount"),
            RowKind.BUY to listOf("portfolio", "date", "ticker", "quantity", "amount"),
            RowKind.SELL to listOf("portfolio", "date", "ticker", "quantity", "amount", "lot_picks"),
            RowKind.SPLIT to listOf("portfolio", "date", "ticker", "split_to", "split_from"),
            RowKind.REVERSAL to listOf("reverses", "date"),
            RowKind.HIDDEN to listOf("portfolio", "ticker"),
            RowKind.OVERRIDE to listOf("portfolio", "ticker"),
            RowKind.LIST_VALUE to listOf("list", "value"),
            RowKind.SOURCE_TEXT to listOf("list", "value", "source_text"),
            RowKind.SETTING to listOf("key", "value"),
        )

    private val MAY: Map<RowKind, List<String>> =
        mapOf(
            RowKind.CASH to listOf("id", "note"),
            RowKind.BUY to listOf("id", "split_to", "split_from"),
            RowKind.SELL to listOf("id"),
            RowKind.SPLIT to listOf("id"),
            RowKind.REVERSAL to listOf("note"),
            RowKind.OVERRIDE to listOf("sector", "geography"),
        )

    private val ID = Regex("[A-Za-z][A-Za-z0-9_-]{0,31}")
    private val DATE = Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}")
    private val NUMBER = Regex("(-?)([0-9]+)(?:\\.([0-9]+))?")
    private val WHOLE = Regex("-?[0-9]+")
    private val TICKER = Regex("[A-Z0-9.-]{1,12}")
    private const val AMOUNT_DECIMALS = 2

    fun parse(
        rows: List<FileRow>,
        priceSources: Set<String>,
    ): ParsedRows {
        val records = ArrayList<FileRecord>()
        val failed = ArrayList<FailedRow>()
        val errors = ArrayList<RestoreError>()
        rows.forEachIndexed { index, row ->
            val checked = Row(row, first = index == 0, priceSources)
            val record = checked.record()
            if (checked.errors.isEmpty() && record != null) {
                records.add(record)
            } else if (checked.errors.isNotEmpty()) {
                errors.addAll(checked.errors)
                failed.add(checked.facts())
            }
        }
        return ParsedRows(records, failed, errors)
    }

    /** One row's rules; [errors] fills as its cells are read. */
    private class Row(
        val row: FileRow,
        val first: Boolean,
        val priceSources: Set<String>,
    ) {
        val errors = ArrayList<RestoreError>(row.errors)
        val kind: RowKind? = RowKind.entries.firstOrNull { it.word == row["row_type"].lowercase(Locale.ROOT) }

        fun error(
            column: String?,
            reason: RestoreReason,
        ) {
            errors.add(RestoreError(row.line, column, reason))
        }

        // Null when the row has errors, or for the format row, which is no record.
        fun record(): FileRecord? {
            if (errors.isNotEmpty()) return null
            val kind = kind ?: return null.also { rowTypeError() }
            if (kind == RowKind.FORMAT && !first) return null.also { error("row_type", RestoreReason.FormatNotFirst) }
            cellsError(kind)
            val record = typed(kind)
            return if (errors.isEmpty()) record else null
        }

        private fun rowTypeError() {
            val written = row["row_type"]
            if (written.isEmpty()) error("row_type", RestoreReason.RowTypeMissing) else error("row_type", RestoreReason.RowTypeUnknown(written))
        }

        // Section 8: the cells a row type needs, and an empty cell in every column it does not use. A buy's ratio is both
        // numbers or neither.
        private fun cellsError(kind: RowKind) {
            val needs = NEEDS.getValue(kind)
            val may = MAY[kind].orEmpty()
            for (column in BackupFile.COLUMNS) {
                if (column == "row_type") continue
                val empty = row[column].isEmpty()
                when {
                    column in needs && empty -> error(column, RestoreReason.ValueMissing)
                    column !in needs && column !in may && !empty -> error(column, RestoreReason.CellNotUsed)
                }
            }
            if (kind == RowKind.BUY && row["split_to"].isEmpty() != row["split_from"].isEmpty()) {
                error(if (row["split_to"].isEmpty()) "split_to" else "split_from", RestoreReason.ValueMissing)
            }
        }

        private fun typed(kind: RowKind): FileRecord? = when (kind) {
            RowKind.FORMAT -> null
            RowKind.PORTFOLIO -> portfolio()
            RowKind.CASH -> cash()
            RowKind.BUY -> buy()
            RowKind.SELL -> sell()
            RowKind.SPLIT -> split()
            RowKind.REVERSAL -> reversal()
            RowKind.HIDDEN -> holding("ticker")?.let { HiddenRecord(row.line, row["portfolio"], it) }
            RowKind.OVERRIDE -> override()
            RowKind.LIST_VALUE -> list()?.let { ListValueRecord(row.line, it, row["value"]) }
            RowKind.SOURCE_TEXT -> list()?.let { SourceTextRecord(row.line, it, row["value"], row["source_text"]) }
            RowKind.SETTING -> setting()
        }

        private fun portfolio(): FileRecord? {
            val name = row["portfolio"]
            if (name.isEmpty()) return null
            when (Portfolios.nameRefusal(name, emptyList())) {
                PortfolioRefusal.TooShort -> error("portfolio", RestoreReason.PortfolioNameTooShort)
                PortfolioRefusal.NoLetter -> error("portfolio", RestoreReason.PortfolioNameNoLetter)
                else -> return PortfolioRecord(row.line, name)
            }
            return null
        }

        private fun cash(): FileRecord? {
            val id = id("id")
            val date = date()
            val type = cashType()
            val cents = amount()
            val note = row["note"]
            if (type == CashType.ADJUSTMENT && note.isBlank()) error("note", RestoreReason.AdjustmentWithoutNote)
            if (type != null && cents != null) {
                val sign = cents.compareTo(0L)
                when {
                    sign == 0 -> error("amount", RestoreReason.CashAmountZero)
                    type == CashType.WITHDRAWAL && sign > 0 -> error("amount", RestoreReason.WrongSign)
                    type != CashType.WITHDRAWAL && type != CashType.ADJUSTMENT && sign < 0 -> error("amount", RestoreReason.WrongSign)
                }
            }
            if (date == null || type == null || cents == null) return null
            return CashRecord(row.line, id, date, type, cents, note)
        }

        private fun buy(): FileRecord? {
            val id = id("id")
            val date = date()
            val ticker = holding("ticker")
            val quantity = quantity("quantity", row["quantity"])
            val cents = amount()
            when {
                cents == 0L -> error("amount", RestoreReason.BuyTotalZero)
                cents != null && cents < 0L -> error("amount", RestoreReason.WrongSign)
            }
            val ratio = if (row["split_to"].isEmpty() || row["split_from"].isEmpty()) null else ratio(limit = Int.MAX_VALUE)
            val written = row["split_to"].isNotEmpty() && row["split_from"].isNotEmpty()
            if (date == null || ticker == null || quantity == null || cents == null || (written && ratio == null)) return null
            return BuyRecord(row.line, id, row["portfolio"], date, ticker, quantity, cents, ratio)
        }

        private fun sell(): FileRecord? {
            val id = id("id")
            val date = date()
            val ticker = holding("ticker")
            val quantity = quantity("quantity", row["quantity"])
            val cents = amount()
            if (cents != null && cents < 0L) error("amount", RestoreReason.WrongSign)
            val picks = picks()
            if (date == null || ticker == null || quantity == null || cents == null || picks == null) return null
            return SellRecord(row.line, id, row["portfolio"], date, ticker, quantity, cents, picks)
        }

        private fun split(): FileRecord? {
            val id = id("id")
            val date = date()
            val ticker = holding("ticker")
            val ratio = ratio(limit = Splits.MAX_RATIO)
            if (ratio != null && ratio.to == ratio.from) error("split_from", RestoreReason.SplitNumbersEqual)
            if (date == null || ticker == null || ratio == null) return null
            return SplitRecord(row.line, id, row["portfolio"], date, ticker, ratio)
        }

        private fun reversal(): FileRecord? {
            val reverses = id("reverses")
            val date = date()
            if (reverses == null || date == null) return null
            return ReversalRecord(row.line, reverses, date, row["note"])
        }

        private fun override(): FileRecord? {
            val ticker = holding("ticker")
            val sector = row["sector"].ifEmpty { null }
            val geography = row["geography"].ifEmpty { null }
            if (sector == null && geography == null) error(null, RestoreReason.OverrideBothEmpty)
            if (ticker == null || (sector == null && geography == null)) return null
            return OverrideRecord(row.line, row["portfolio"], ticker, sector, geography)
        }

        private fun list(): ClassList? = when (row["list"].lowercase(Locale.ROOT)) {
            "" -> null
            "sector" -> ClassList.SECTOR
            "geography" -> ClassList.GEOGRAPHY
            else -> null.also { error("list", RestoreReason.ListUnknown) }
        }

        // Section 8, settings keys: each value as the file's own word, case ignored; a tab order is each tab once.
        private fun setting(): FileRecord? {
            val key = row["key"].lowercase(Locale.ROOT)
            val value = row["value"].lowercase(Locale.ROOT)
            if (key.isEmpty() || value.isEmpty()) return null
            val known =
                when (key) {
                    SETTING_AUTO_LOCK -> value.takeIf { it in AUTO_LOCK_WORDS }
                    SETTING_PRICE_SOURCE -> value.takeIf { it in priceSources }
                    SETTING_TAB_ORDER -> tabOrder(value)?.text ?: return null.also { error("value", RestoreReason.TabOrderNotEachOnce) }
                    else -> return null.also { error("key", RestoreReason.SettingKeyUnknown) }
                }
            if (known == null) return null.also { error("value", RestoreReason.SettingValueUnknown) }
            return SettingRecord(row.line, key, known)
        }

        private fun tabOrder(value: String): TabOrder? = TabOrder.parse(value.split(';').joinToString(";") { it.trim() })

        // An id is 1 to 32 letters, digits, hyphens or underscores and starts with a letter (section 8); as written.
        private fun id(column: String): String? {
            val text = row[column]
            if (text.isEmpty()) return null
            if (!ID.matches(text)) return null.also { error(column, RestoreReason.IdBadlyFormed) }
            return text
        }

        private fun date(): LocalDate? {
            val text = row["date"]
            if (text.isEmpty()) return null
            if (!DATE.matches(text)) return null.also { error("date", RestoreReason.DateForm) }
            return try {
                LocalDate.parse(text)
            } catch (e: DateTimeParseException) {
                null.also { error("date", RestoreReason.DateNotReal) }
            }
        }

        // Read trimmed and in capitals: letters, digits, dot and hyphen, 1 to 12 characters (section 8).
        fun holding(column: String): String? {
            val text = row[column]
            if (text.isEmpty()) return null
            val ticker = text.uppercase(Locale.ROOT)
            if (!TICKER.matches(ticker)) return null.also { error(column, RestoreReason.TickerBadCharacters) }
            return ticker
        }

        private fun cashType(): CashType? {
            val text = row["cash_type"].lowercase(Locale.ROOT)
            if (text.isEmpty()) return null
            if (text == "trade") return null.also { error("cash_type", RestoreReason.CashTypeTrade) }
            return CASH_WORDS[text] ?: null.also { error("cash_type", RestoreReason.CashTypeUnknown) }
        }

        // Signed cents: digits, an optional leading minus and at most 2 decimals; beyond exact cents is its own reason.
        private fun amount(): Long? {
            val text = row["amount"]
            if (text.isEmpty()) return null
            val match = NUMBER.matchEntire(text) ?: return null.also { error("amount", RestoreReason.NumberForm) }
            if (match.groupValues[3].length > AMOUNT_DECIMALS) return null.also { error("amount", RestoreReason.AmountTooManyDecimals) }
            val magnitude = BigDecimal(text.removePrefix("-")).movePointRight(AMOUNT_DECIMALS).toBigIntegerExact()
            if (magnitude > BigInteger.valueOf(Long.MAX_VALUE)) return null.also { error("amount", RestoreReason.AmountTooLarge) }
            return if (match.groupValues[1] == "-") -magnitude.toLong() else magnitude.toLong()
        }

        // Above 0, with up to 8 decimals (section 8, 4.3).
        fun quantity(
            column: String,
            text: String,
            badForm: RestoreReason = RestoreReason.NumberForm,
        ): BigDecimal? {
            if (text.isEmpty()) return null
            val match = NUMBER.matchEntire(text) ?: return null.also { error(column, badForm) }
            val value = BigDecimal(text)
            if (match.groupValues[1] == "-" || value.signum() == 0) return null.also { error(column, RestoreReason.QuantityNotPositive) }
            if (match.groupValues[3].length > Quantity.MAX_DECIMALS) return null.also { error(column, RestoreReason.QuantityTooManyDecimals) }
            return value
        }

        // `split_to` and `split_from`: whole numbers above 0, each up to [limit].
        private fun ratio(limit: Int): SplitRatio? {
            val to = whole("split_to", limit)
            val from = whole("split_from", limit)
            return if (to != null && from != null) SplitRatio(to, from) else null
        }

        private fun whole(
            column: String,
            limit: Int,
        ): Int? {
            val text = row[column]
            if (text.isEmpty()) return null
            if (!WHOLE.matches(text) || BigInteger(text).signum() <= 0) return null.also { error(column, RestoreReason.SplitNumberNotWhole) }
            if (BigInteger(text) > BigInteger.valueOf(limit.toLong())) {
                val reason = if (limit == Splits.MAX_RATIO) RestoreReason.SplitNumberTooLarge else RestoreReason.BuyRatioTooLarge
                return null.also { error(column, reason) }
            }
            return text.toInt()
        }

        // `id:quantity` pieces separated by semicolons (section 8); one error for the cell, the first found.
        private fun picks(): List<Pick>? {
            val text = row["lot_picks"]
            if (text.isEmpty()) return null
            val picks = ArrayList<Pick>()
            for (piece in text.split(';')) {
                val parts = piece.split(':')
                val id = parts.first().trim()
                if (parts.size != 2 || !ID.matches(id) || parts[1].isBlank()) return null.also { error("lot_picks", RestoreReason.PicksBadlyWritten) }
                val quantity = quantity("lot_picks", parts[1].trim(), badForm = RestoreReason.PicksBadlyWritten) ?: return null
                picks.add(Pick(id, quantity))
            }
            return picks
        }

        // What a row that failed still tells, read leniently: its kind, a well-formed id, its portfolio, its ticker.
        fun facts(): FailedRow {
            val id = (if (kind == RowKind.REVERSAL) null else row["id"]).takeIf { it != null && ID.matches(it) }
            val ticker = row["ticker"].uppercase(Locale.ROOT).takeIf { TICKER.matches(it) }
            return FailedRow(
                line = row.line,
                kind = kind,
                id = id,
                portfolio = row["portfolio"].ifEmpty { null },
                ticker = ticker,
                cashType = if (kind == RowKind.CASH) CASH_WORDS[row["cash_type"].lowercase(Locale.ROOT)] else null,
            )
        }
    }
}
