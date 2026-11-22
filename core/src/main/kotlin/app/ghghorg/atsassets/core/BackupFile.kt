package app.ghghorg.atsassets.core

import java.math.BigInteger
import java.time.Instant
import java.time.ZoneId

/**
 * Everything the backup file holds (spec section 8, file contents), as the store loads it. Trade cash lines may be in
 * [cashEntries]; the writer leaves them out. [priceSource] is the selected source's word for the file (31 decision 5a).
 */
data class BackupRecords(
    val cashEntries: List<CashEntry>,
    val portfolios: List<Portfolio>,
    val transactions: List<Transaction>,
    val hidden: List<HiddenHolding>,
    val overrides: List<ClassOverride>,
    val lists: ClassLists,
    val autoLock: AutoLock,
    val tabOrder: TabOrder,
    val priceSource: String,
)

/** Why the records cannot be written as a backup file. Reasons only; the wording is D9. */
enum class BackupRefusal {
    /** A stored text holds a line break, which a cell cannot hold (section 8). */
    LINE_BREAK,

    /** A record points to one that is not there: a portfolio, a reversal's original, a sell's lot, a split's ratio. */
    BROKEN_LINK,
}

sealed interface BackupResult {
    /** The whole file's text: LF line endings, no byte-order mark (section 8). */
    data class Written(
        val text: String,
    ) : BackupResult

    data class Refused(
        val reason: BackupRefusal,
    ) : BackupResult
}

/** K6, the export half (spec section 8; iteration 31): records to the backup file's text. It reads nothing and writes no file. */
object BackupFile {
    const val FORMAT_VERSION = 1

    /** The 19 columns, in the order of spec/template-backup.csv. */
    val COLUMNS: List<String> =
        listOf(
            "row_type",
            "id",
            "portfolio",
            "date",
            "ticker",
            "quantity",
            "amount",
            "lot_picks",
            "split_to",
            "split_from",
            "cash_type",
            "note",
            "reverses",
            "sector",
            "geography",
            "list",
            "key",
            "value",
            "source_text",
        )

    /** The suggested name of a backup made at [moment]: backup-YYYY-MM-DD.csv, the date in [zone] (section 8, C15). */
    fun fileName(
        moment: Instant,
        zone: ZoneId,
    ): String = "backup-${moment.atZone(zone).toLocalDate()}.csv"

    /**
     * The file's text, in the order of spec/example-backup.csv: the header, the `format` row, the settings, the
     * portfolios in creation order, the list values alphabetically with their remembered texts, the entries in entry
     * order, then the hidden holdings and the overrides (section 8).
     *
     * Entry order is the entry number; a cash entry can share a split's number, as a split has no cash line and a cash
     * entry is numbered from the cash lines only, and then the transaction comes first. The two cannot change each
     * other's result: a split moves no cash (31 file, section 4).
     *
     * A cash entry, a buy, a sell and a split each get an id of the file's own (decision 5b); the app's ids are not
     * written. A trade cash line is not written: a restore rebuilds it. A hidden or override row is written only for a
     * holding with a buy or sell row, as section 8 refuses any other (open item 58).
     */
    fun write(records: BackupRecords): BackupResult = try {
        BackupResult.Written(Writer(records).text())
    } catch (unwritable: Unwritable) {
        BackupResult.Refused(unwritable.reason)
    }

    private class Unwritable(
        val reason: BackupRefusal,
    ) : Exception()

    private fun broken(): Nothing = throw Unwritable(BackupRefusal.BROKEN_LINK)

    /** One entry of the shared order: a cash entry or a transaction, by its entry number, a transaction first on a tie. */
    private class Entry(
        val seq: Long,
        val cash: CashEntry? = null,
        val transaction: Transaction? = null,
    )

    private class Writer(
        private val records: BackupRecords,
    ) {
        private val out = StringBuilder()
        private val portfolioNames = records.portfolios.associate { it.id to it.name }
        private val portfolioOrder = records.portfolios.sortedBy { it.createdSeq }.mapIndexed { index, it -> it.id to index }.toMap()
        private val cashIds = HashMap<String, String>()
        private val transactionIds = HashMap<String, String>()

        fun text(): String {
            out.append(COLUMNS.joinToString(",")).append('\n')
            row("row_type" to "format", "value" to FORMAT_VERSION.toString())
            settings()
            for (portfolio in records.portfolios.sortedBy { it.createdSeq }) row("row_type" to "portfolio", "portfolio" to portfolio.name)
            lists()
            entries()
            stateRows()
            return out.toString()
        }

        private fun settings() {
            setting("auto_lock", autoLockWord(records.autoLock))
            setting("price_source", records.priceSource)
            setting("tab_order", records.tabOrder.text)
        }

        private fun setting(
            key: String,
            value: String,
        ) = row("row_type" to "setting", "key" to key, "value" to value)

        private fun lists() {
            for (list in ClassList.entries) {
                for (value in records.lists[list].sortedWith(ALPHABETICAL)) {
                    row("row_type" to "list_value", "list" to listWord(list), "value" to value)
                }
            }
            for (list in ClassList.entries) {
                val byValueThenText = compareBy<RememberedText, String>(ALPHABETICAL) { it.value }.thenBy(ALPHABETICAL) { it.text }
                for (text in records.lists.rememberedOn(list).sortedWith(byValueThenText)) {
                    row("row_type" to "source_text", "list" to listWord(list), "value" to text.value, "source_text" to text.text)
                }
            }
        }

        private fun entries() {
            val ordered =
                (
                    records.transactions.map { Entry(it.enteredSeq, transaction = it) } +
                        records.cashEntries.filter { it.type != CashType.TRADE }.map { Entry(it.enteredSeq, cash = it) }
                    ).sortedWith(compareBy<Entry>({ it.seq }, { if (it.transaction != null) 0 else 1 }))
            giveIds(ordered)
            for (entry in ordered) {
                entry.cash?.let { cashRow(it) }
                entry.transaction?.let { transactionRow(it) }
            }
        }

        // A letter for the kind and a running number per kind, in entry order (decision 5b). A reversal gets none.
        private fun giveIds(ordered: List<Entry>) {
            val counts = HashMap<Char, Int>()

            fun next(letter: Char): String = "$letter${counts.merge(letter, 1, Int::plus)}"
            for (entry in ordered) {
                val cash = entry.cash
                val transaction = entry.transaction
                if (cash != null && cash.reversesId == null) cashIds[cash.id] = next('C')
                when (transaction?.type) {
                    TransactionType.BUY -> transactionIds[transaction.id] = next('L')
                    TransactionType.SELL -> transactionIds[transaction.id] = next('S')
                    TransactionType.SPLIT -> transactionIds[transaction.id] = next('X')
                    else -> Unit
                }
            }
        }

        private fun cashRow(entry: CashEntry) {
            val reverses = entry.reversesId
            if (reverses != null) {
                val original = cashIds[reverses] ?: broken()
                row("row_type" to "reversal", "date" to entry.date.toString(), "note" to entry.note, "reverses" to original)
                return
            }
            row(
                "row_type" to "cash",
                "id" to cashIds.getValue(entry.id),
                "date" to entry.date.toString(),
                "amount" to amount(entry.amountCents),
                "cash_type" to cashWord(entry.type),
                "note" to entry.note,
            )
        }

        private fun transactionRow(transaction: Transaction) {
            val portfolio = portfolioNames[transaction.portfolioId] ?: broken()
            val date = transaction.date.toString()
            when (transaction.type) {
                TransactionType.BUY -> {
                    val ratio = transaction.quantityRatio
                    row(
                        "row_type" to "buy",
                        "id" to transactionIds.getValue(transaction.id),
                        "portfolio" to portfolio,
                        "date" to date,
                        "ticker" to transaction.ticker,
                        "quantity" to quantity(Fraction.of(transaction.quantity)),
                        "amount" to amount(transaction.totalCents),
                        "split_to" to (ratio?.to?.toString() ?: ""),
                        "split_from" to (ratio?.from?.toString() ?: ""),
                    )
                }

                TransactionType.SELL ->
                    row(
                        "row_type" to "sell",
                        "id" to transactionIds.getValue(transaction.id),
                        "portfolio" to portfolio,
                        "date" to date,
                        "ticker" to transaction.ticker,
                        "quantity" to quantity(Fraction.of(transaction.quantity)),
                        "amount" to amount(transaction.totalCents),
                        "lot_picks" to picks(transaction),
                    )

                TransactionType.SPLIT -> {
                    val ratio = transaction.split ?: broken()
                    row(
                        "row_type" to "split",
                        "id" to transactionIds.getValue(transaction.id),
                        "portfolio" to portfolio,
                        "date" to date,
                        "ticker" to transaction.ticker,
                        "split_to" to ratio.to.toString(),
                        "split_from" to ratio.from.toString(),
                    )
                }

                TransactionType.REVERSAL -> {
                    val original = transaction.reversesId?.let { transactionIds[it] } ?: broken()
                    row("row_type" to "reversal", "date" to date, "reverses" to original)
                }
            }
        }

        // Each pick as the lot picker shows it on the sell's date (4.4): an allocation is kept in the sell's own units, and
        // a whole remaining lot, kept exact, is written as its shown remainder, which a restore reads as the exact one.
        private fun picks(sell: Transaction): String = sell.allocations.joinToString(";") { allocation ->
            val lot = transactionIds[allocation.lotId]?.takeIf { it.startsWith('L') } ?: broken()
            "$lot:${quantity(allocation.quantity)}"
        }

        private fun stateRows() {
            val inFile =
                records.transactions
                    .filter { (it.type == TransactionType.BUY || it.type == TransactionType.SELL) && it.portfolioId in portfolioNames }
                    .mapTo(HashSet()) { it.portfolioId to it.ticker }
            val order = compareBy<Pair<String, String>>({ portfolioOrder.getValue(it.first) }, { it.second })
            val hidden = records.hidden.map { it.portfolioId to it.ticker }.filter { it in inFile }.distinct()
            for ((portfolioId, ticker) in hidden.sortedWith(order)) {
                row("row_type" to "hidden", "portfolio" to portfolioNames.getValue(portfolioId), "ticker" to ticker)
            }
            val overrides = records.overrides.filter { (it.portfolioId to it.ticker) in inFile }.groupBy { it.portfolioId to it.ticker }
            for (holding in overrides.keys.sortedWith(order)) {
                val values = overrides.getValue(holding)
                row(
                    "row_type" to "override",
                    "portfolio" to portfolioNames.getValue(holding.first),
                    "ticker" to holding.second,
                    "sector" to (values.firstOrNull { it.list == ClassList.SECTOR }?.value ?: ""),
                    "geography" to (values.firstOrNull { it.list == ClassList.GEOGRAPHY }?.value ?: ""),
                )
            }
        }

        // One record per line: the named cells, every other column empty (section 8).
        private fun row(vararg cells: Pair<String, String>) {
            val values = cells.toMap()
            out.append(COLUMNS.joinToString(",") { cell(values[it] ?: "") }).append('\n')
        }
    }

    private val ALPHABETICAL: Comparator<String> = String.CASE_INSENSITIVE_ORDER.thenComparing(Comparator.naturalOrder())

    // Decision 3: a cell with a comma or a double quote is wrapped in double quotes, a quote inside it doubled; the
    // others are written bare. A line break cannot be written (section 8).
    private fun cell(text: String): String {
        if (text.any { it == '\n' || it == '\r' }) throw Unwritable(BackupRefusal.LINE_BREAK)
        return if (text.any { it == ',' || it == '"' }) "\"" + text.replace("\"", "\"\"") + "\"" else text
    }

    // Signed, a dot and two decimals, digits only: never the phone's number format (section 6, section 8).
    private fun amount(cents: Long): String {
        val magnitude = BigInteger.valueOf(cents).abs()
        val hundredths = magnitude.remainder(HUNDRED).toString().padStart(2, '0')
        return (if (cents < 0) "-" else "") + magnitude.divide(HUNDRED).toString() + "." + hundredths
    }

    // Plain digits, up to 8 decimals as shown (4.10), trailing zeros dropped, no separator and no exponent.
    private fun quantity(value: Fraction): String = Sells.shown(value).toPlainString()

    private val HUNDRED = BigInteger.valueOf(100)

    private fun autoLockWord(choice: AutoLock): String = when (choice) {
        AutoLock.EVERY_LEAVE -> "every_exit"
        AutoLock.IDLE_1_MIN -> "1_min"
        AutoLock.IDLE_5_MIN -> "5_min"
        AutoLock.PHONE_LOCK_OR_RESTART -> "phone_lock_only"
    }

    private fun listWord(list: ClassList): String = if (list == ClassList.SECTOR) "sector" else "geography"

    private fun cashWord(type: CashType): String = when (type) {
        CashType.OPENING_BALANCE -> "opening_balance"
        CashType.DEPOSIT -> "deposit"
        CashType.WITHDRAWAL -> "withdrawal"
        CashType.INTEREST_DIVIDEND -> "interest_dividend"
        CashType.ADJUSTMENT -> "adjustment"
        CashType.TRADE -> broken()
    }
}
