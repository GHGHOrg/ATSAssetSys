package app.ghghorg.atsassets.core

import java.time.LocalDate
import java.util.Locale
import kotlin.math.abs

/**
 * K6's reading half (spec section 8, restore steps 2 and 5; 33a): a backup file's bytes checked as a restore would
 * load them, with nothing stored (C3). The file level and each row's own rules first ([BackupReader], [BackupRows]), then
 * the rules between rows, and the replay of the entries through K1 in date order, then file order, each as if entered
 * by hand (4.1 to 4.6); then the state rows, the lists and the settings. Every error is collected; a row that points to a
 * failed row names the root cause's line and is not checked further (C7; 33a decision 2).
 */
object BackupCheck {
    fun check(
        bytes: ByteArray,
        context: RestoreContext,
    ): RestoreCheck {
        val read = BackupReader.read(bytes)
        if (read is FileRead.Refused) return RestoreCheck.Failed(read.errors)
        val parsed = BackupRows.parse((read as FileRead.Read).rows, context.priceSources)
        val checked = Replay(parsed, context).run()
        if (checked !is RestoreCheck.Failed) return checked
        return RestoreCheck.Failed(inReportOrder(checked.errors, formErrors = parsed.errors.size, read.columnOrder))
    }

    // Decision 4: line order, the file as a whole first; within a line, the form errors (the first [formErrors] found,
    // each row's own rules) in the file's column order, an error with no column first, then the rest in the order found.
    private fun inReportOrder(
        errors: List<RestoreError>,
        formErrors: Int,
        columnOrder: Map<String, Int>,
    ): List<RestoreError> = errors
        .withIndex()
        .sortedWith(
            compareBy<IndexedValue<RestoreError>>(
                { it.value.line ?: -1 },
                { if (it.index < formErrors) 0 else 1 },
                { it.value.column?.let { column -> columnOrder[column] } ?: -1 },
            ),
        ).map { it.value }
}

/** One check's replay: what K1 has made so far, and every error found. */
private class Replay(
    parsed: ParsedRows,
    private val context: RestoreContext,
) {
    private val records = parsed.records
    private val failedRows = parsed.failed
    private val errors = ArrayList(parsed.errors)

    // Each failed line and the line of its root cause: itself, or the failed row it depends on (decision 2).
    private val roots = HashMap<Int, Int>()

    private val portfolios = ArrayList<Portfolio>()
    private val portfolioByKey = HashMap<String, Portfolio>()
    private val portfolioLines = HashMap<String, Int>()
    private val failedPortfolios = HashMap<String, Int>()
    private var cash: List<CashEntry> = emptyList()
    private var transactions: List<Transaction> = emptyList()

    // The file's ids, ignoring case, each to the first line that has it; each line's row type; each entry by its line;
    // each replayed entry's app id; each reversed line and the line of its reversal.
    private val ids = HashMap<String, Int>()
    private val kinds = HashMap<Int, RowKind?>()
    private val entryByLine = HashMap<Int, EntryRecord>()
    private val appIds = HashMap<Int, String>()
    private val reversedBy = HashMap<Int, Int>()

    // Each holding, by its portfolio's name key and ticker, whose buy or sell rows failed, to the first root cause.
    private val failedHoldings = HashMap<Pair<String, String>, Int>()

    init {
        for (row in failedRows) {
            roots[row.line] = row.line
            if (row.kind == RowKind.PORTFOLIO && row.portfolio != null) failedPortfolios.putIfAbsent(key(row.portfolio), row.line)
            if ((row.kind == RowKind.BUY || row.kind == RowKind.SELL) && row.portfolio != null && row.ticker != null) {
                failedHoldings.putIfAbsent(key(row.portfolio) to row.ticker, row.line)
            }
        }
        records.filterIsInstance<EntryRecord>().forEach { entryByLine[it.line] = it }
    }

    fun run(): RestoreCheck {
        portfolios()
        ids()
        val replayed = entries()
        val lists = lists()
        val hidden = if (replayed) hidden() else emptyList()
        val overrides = if (replayed) overrides(lists) else emptyList()
        val settings = settings()
        if (errors.isNotEmpty()) return RestoreCheck.Failed(errors)
        val held = Holdings.holdings(transactions, LocalDate.MAX).filter { it.shares.signum() > 0 }
        val summary =
            RestoreSummary(
                portfolios = portfolios.size,
                transactions = transactions.size,
                cashEntries = cash.count { it.type != CashType.TRADE },
                cashBalanceCents = CashAccount.balanceCents(cash),
                holdingsWithShares = held.size,
            )
        val loaded =
            BackupRecords(
                cashEntries = cash,
                portfolios = portfolios,
                transactions = transactions,
                hidden = hidden,
                overrides = overrides,
                lists = lists,
                autoLock = settings.autoLock,
                tabOrder = settings.tabOrder,
                priceSource = settings.priceSource,
            )
        return RestoreCheck.Passed(loaded, held.map { it.ticker }.distinct().sorted(), summary)
    }

    private fun key(name: String): String = Portfolios.nameKey(name)

    private fun fail(
        line: Int,
        column: String?,
        reason: RestoreReason,
    ) {
        errors.add(RestoreError(line, column, reason))
        roots.putIfAbsent(line, line)
    }

    // The row on [line] points to the failed row on [target]: it names the root cause and becomes failed itself.
    private fun dependsOn(
        line: Int,
        column: String?,
        target: Int,
    ) {
        val root = roots[target] ?: target
        errors.add(RestoreError(line, column, RestoreReason.DependsOn(root)))
        roots[line] = root
    }

    // A buy or sell that failed fails its holding, for the rows that name the holding (decision 2).
    private fun holdingFailed(
        line: Int,
        portfolio: String,
        ticker: String,
    ) {
        failedHoldings.putIfAbsent(key(portfolio) to ticker, roots[line] ?: line)
    }

    // Portfolio rows have no date and may be anywhere: made first, in file order, with the name rules of 4.7 only.
    private fun portfolios() {
        for (row in records.filterIsInstance<PortfolioRecord>()) {
            val key = key(row.name)
            val first = portfolioLines[key]
            if (first != null) {
                fail(row.line, "portfolio", RestoreReason.PortfolioNameDuplicate(first))
                continue
            }
            portfolioLines[key] = row.line
            val portfolio = Portfolio(id = context.newId(), name = row.name, createdSeq = portfolios.size + 1L)
            portfolios.add(portfolio)
            portfolioByKey[key] = portfolio
        }
    }

    // The portfolio a row names (D7), or null after its error: dependent on a failed portfolio row, or not found.
    private fun portfolio(
        line: Int,
        name: String,
    ): Portfolio? {
        val key = key(name)
        portfolioByKey[key]?.let { return it }
        val failed = failedPortfolios[key]
        if (failed != null) dependsOn(line, "portfolio", failed) else fail(line, "portfolio", RestoreReason.PortfolioNotFound)
        return null
    }

    // One id space for cash entries, buys, sells and splits, ignoring case; a later duplicate fails (section 8).
    private fun ids() {
        val named = ArrayList<Triple<Int, String, RowKind?>>()
        for (record in records) {
            val id =
                when (record) {
                    is CashRecord -> record.id
                    is BuyRecord -> record.id
                    is SellRecord -> record.id
                    is SplitRecord -> record.id
                    else -> null
                }
            val kind =
                when (record) {
                    is CashRecord -> RowKind.CASH
                    is BuyRecord -> RowKind.BUY
                    is SellRecord -> RowKind.SELL
                    is SplitRecord -> RowKind.SPLIT
                    else -> null
                }
            if (id != null) named.add(Triple(record.line, id, kind))
        }
        for (row in failedRows) {
            if (row.id != null && row.kind in ID_KINDS) named.add(Triple(row.line, row.id, row.kind))
        }
        for ((line, id, kind) in named.sortedBy { it.first }) {
            kinds[line] = kind
            val key = id.lowercase(Locale.ROOT)
            val first = ids[key]
            if (first != null) fail(line, "id", RestoreReason.IdDuplicate(first)) else ids[key] = line
        }
    }

    // The opening balance first (4.2), then every other entry in date order, then file order (4.1). False when there is
    // no opening balance to replay from: then nothing is replayed, and with no opening balance row at all the file has
    // that one error.
    private fun entries(): Boolean {
        val valid = records.filterIsInstance<EntryRecord>().filter { it.line !in roots }
        val openings = valid.filterIsInstance<CashRecord>().filter { it.type == CashType.OPENING_BALANCE }
        val opening = openings.firstOrNull()
        if (opening == null) {
            if (failedRows.none { it.cashType == CashType.OPENING_BALANCE }) errors.add(RestoreError(null, null, RestoreReason.NoOpeningBalance))
            return false
        }
        openings.drop(1).forEach { fail(it.line, "cash_type", RestoreReason.SecondOpeningBalance(opening.line)) }
        val earlier =
            valid.filterIsInstance<CashRecord>().any {
                it.type != CashType.OPENING_BALANCE && (it.date < opening.date || (it.date == opening.date && it.line < opening.line))
            }
        if (earlier) errors.add(RestoreError(opening.line, "date", RestoreReason.OpeningBalanceNotEarliest))
        when (val made = OpeningBalance.create(emptyList(), context.newId(), opening.date, opening.cents, opening.note)) {
            is OpeningBalanceResult.Created -> {
                cash = listOf(made.entry)
                appIds[opening.line] = made.entry.id
            }

            is OpeningBalanceResult.Refused -> {
                fail(opening.line, "amount", RestoreReason.WrongSign)
                return false
            }
        }
        valid
            .filter { it.line !in roots && it !== opening }
            .sortedWith(compareBy<EntryRecord>({ it.date }, { it.line }))
            .forEach { entry ->
                when (entry) {
                    is CashRecord -> cashEntry(entry)
                    is BuyRecord -> buy(entry)
                    is SellRecord -> sell(entry)
                    is SplitRecord -> split(entry)
                    is ReversalRecord -> reversal(entry)
                }
            }
        return true
    }

    private fun cashEntry(entry: CashRecord) {
        val direction = if (entry.cents < 0L) CashDirection.OUT else CashDirection.IN
        when (val made = CashEntries.create(cash, context.newId(), entry.type, entry.date, abs(entry.cents), entry.note, direction)) {
            is CashEntryResult.Created -> {
                cash = cash + made.entry
                appIds[entry.line] = made.entry.id
            }

            is CashEntryResult.Refused -> cashRefused(entry.line, made.reason)
        }
    }

    private fun cashRefused(
        line: Int,
        reason: CashEntryRefusal,
    ) {
        when (reason) {
            is CashEntryRefusal.BeforeOpeningBalance -> fail(line, "date", RestoreReason.DatedBeforeOpeningBalance(reason.openingDate))
            is CashEntryRefusal.NegativeCash -> fail(line, null, RestoreReason.CashNegative(reason.date, reason.shortfallCents))
            is CashEntryRefusal.BeforeOriginal -> fail(line, "date", RestoreReason.ReversalBeforeOriginal(reason.originalDate))
            is CashEntryRefusal.NotReversible -> fail(line, "reverses", RestoreReason.ReversalOfOpeningBalance)
            CashEntryRefusal.NoteRequired -> fail(line, "note", RestoreReason.AdjustmentWithoutNote)
            CashEntryRefusal.AmountNotPositive -> fail(line, "amount", RestoreReason.CashAmountZero)
            CashEntryRefusal.TypeNotAllowed -> fail(line, "cash_type", RestoreReason.CashTypeUnknown)
            CashEntryRefusal.OriginalNotFound -> fail(line, "reverses", RestoreReason.ReversalTargetNotFound(""))
            CashEntryRefusal.NoteHasLineBreak -> fail(line, "note", RestoreReason.LineBreakInCell)
            CashEntryRefusal.NoOpeningBalance, CashEntryRefusal.TooLarge -> fail(line, null, RestoreReason.BalanceTooLarge)
        }
    }

    private fun buy(entry: BuyRecord) {
        val portfolio = portfolio(entry.line, entry.portfolio) ?: return holdingFailed(entry.line, entry.portfolio, entry.ticker)
        val transactionId = context.newId()
        val made =
            Transactions.buy(
                cash,
                transactions,
                portfolios,
                transactionId,
                context.newId(),
                portfolio.id,
                entry.ticker,
                entry.date,
                entry.quantity,
                entry.cents,
                entry.ratio,
            )
        when (made) {
            is BuyResult.Created -> {
                transactions = transactions + made.transaction
                cash = cash + made.cashLine
                appIds[entry.line] = transactionId
            }

            is BuyResult.Refused -> {
                when (val reason = made.reason) {
                    is BuyRefusal.BeforeOpeningBalance -> fail(entry.line, "date", RestoreReason.DatedBeforeOpeningBalance(reason.openingDate))
                    is BuyRefusal.NegativeCash -> fail(entry.line, null, RestoreReason.CashNegative(reason.date, reason.shortfallCents))
                    BuyRefusal.TotalNotPositive -> fail(entry.line, "amount", RestoreReason.BuyTotalZero)
                    BuyRefusal.QuantityNotPositive -> fail(entry.line, "quantity", RestoreReason.QuantityNotPositive)
                    BuyRefusal.PortfolioNotFound -> fail(entry.line, "portfolio", RestoreReason.PortfolioNotFound)
                    BuyRefusal.TickerEmpty -> fail(entry.line, "ticker", RestoreReason.ValueMissing)
                    BuyRefusal.NoOpeningBalance, BuyRefusal.TooLarge -> fail(entry.line, null, RestoreReason.BalanceTooLarge)
                }
                holdingFailed(entry.line, entry.portfolio, entry.ticker)
            }
        }
    }

    private fun sell(entry: SellRecord) {
        val portfolio = portfolio(entry.line, entry.portfolio) ?: return holdingFailed(entry.line, entry.portfolio, entry.ticker)
        val picks = picks(entry) ?: return holdingFailed(entry.line, entry.portfolio, entry.ticker)
        val written = picks.associate { (pick, asWritten) -> pick.lotId to asWritten }
        val transactionId = context.newId()
        val made =
            Sells.sell(
                cash,
                transactions,
                portfolios,
                transactionId,
                context.newId(),
                portfolio.id,
                entry.ticker,
                entry.date,
                entry.quantity,
                entry.cents,
                picks.map { it.first },
            )
        when (made) {
            is SellResult.Created -> {
                transactions = transactions + made.transaction
                cash = cash + made.cashLine
                appIds[entry.line] = transactionId
            }

            is SellResult.Refused -> {
                sellRefused(entry, made.reason) { written[it] ?: it }
                holdingFailed(entry.line, entry.portfolio, entry.ticker)
            }
        }
    }

    private fun sellRefused(
        entry: SellRecord,
        reason: SellRefusal,
        fileId: (String) -> String,
    ) {
        val line = entry.line
        when (reason) {
            is SellRefusal.MoreThanHeld -> fail(line, "quantity", RestoreReason.SellMoreThanHeld(reason.date, Sells.shown(reason.held)))
            is SellRefusal.PickNotALot -> fail(line, "lot_picks", RestoreReason.PickNotFound(fileId(reason.lotId)))
            is SellRefusal.PickNotAvailable -> fail(line, "lot_picks", unavailable(reason.reason, fileId(reason.lotId)))
            is SellRefusal.PickNotPositive -> fail(line, "lot_picks", RestoreReason.QuantityNotPositive)
            is SellRefusal.PickOverRemainder -> fail(line, "lot_picks", RestoreReason.PickOverLot(fileId(reason.lotId), Sells.shown(reason.remainder)))
            is SellRefusal.PicksDoNotAddUp -> fail(line, "lot_picks", RestoreReason.PicksDoNotAddUp(reason.picked, reason.quantity))
            is SellRefusal.NegativeCash -> fail(line, null, RestoreReason.CashNegative(reason.date, reason.shortfallCents))
            is SellRefusal.BeforeOpeningBalance -> fail(line, "date", RestoreReason.DatedBeforeOpeningBalance(reason.openingDate))
            SellRefusal.QuantityNotPositive -> fail(line, "quantity", RestoreReason.QuantityNotPositive)
            SellRefusal.TotalNegative -> fail(line, "amount", RestoreReason.WrongSign)
            SellRefusal.PortfolioNotFound -> fail(line, "portfolio", RestoreReason.PortfolioNotFound)
            SellRefusal.TickerEmpty -> fail(line, "ticker", RestoreReason.ValueMissing)
            SellRefusal.NoOpeningBalance, SellRefusal.TooLarge -> fail(line, null, RestoreReason.BalanceTooLarge)
        }
    }

    private fun unavailable(
        reason: LotUnavailable,
        id: String,
    ): RestoreReason = if (reason == LotUnavailable.BOUGHT_AFTER_SELL_DATE) RestoreReason.LotBoughtAfterSell(id) else RestoreReason.LotEnteredAfterSell(id)

    // Each pick checked against the file before K1 sees it (section 8, sells and picks), each with the app id of its lot
    // and its id as written; null after the first error, or when a pick names a failed row (decision 2).
    private fun picks(entry: SellRecord): List<Pair<Pick, String>>? {
        val out = ArrayList<Pair<Pick, String>>()
        val seen = HashSet<String>()
        for (pick in entry.picks) {
            val key = pick.lotId.lowercase(Locale.ROOT)
            val reason =
                pickRefusal(entry, pick.lotId, key, seen) ?: run {
                    val line = ids.getValue(key)
                    if (line in roots) {
                        dependsOn(entry.line, "lot_picks", line)
                        return null
                    }
                    out.add(Pick(appIds.getValue(line), pick.quantity) to pick.lotId)
                    null
                }
            if (reason != null) {
                fail(entry.line, "lot_picks", reason)
                return null
            }
        }
        return out
    }

    private fun pickRefusal(
        entry: SellRecord,
        id: String,
        key: String,
        seen: MutableSet<String>,
    ): RestoreReason? {
        if (!seen.add(key)) return RestoreReason.PickTwice(id)
        val line = ids[key] ?: return RestoreReason.PickNotFound(id)
        if (line in roots) return null
        if (kinds[line] != RowKind.BUY) return RestoreReason.PickNotABuy(id)
        val lot = entryByLine.getValue(line) as BuyRecord
        return when {
            key(lot.portfolio) != key(entry.portfolio) -> RestoreReason.PickOtherPortfolio(id)
            lot.ticker != entry.ticker -> RestoreReason.PickOtherTicker(id)
            lot.date > entry.date -> RestoreReason.LotBoughtAfterSell(id)
            lot.date == entry.date && lot.line > entry.line -> RestoreReason.LotEnteredAfterSell(id)
            else -> null
        }
    }

    private fun split(entry: SplitRecord) {
        val portfolio = portfolio(entry.line, entry.portfolio) ?: return
        val transactionId = context.newId()
        when (val made = Splits.split(cash, transactions, portfolios, transactionId, portfolio.id, entry.ticker, entry.date, entry.ratio)) {
            is SplitResult.Created -> {
                transactions = transactions + made.transaction
                appIds[entry.line] = transactionId
            }

            is SplitResult.Refused ->
                when (val reason = made.reason) {
                    SplitRefusal.TickerNotBought -> splitWithoutBuy(entry)
                    is SplitRefusal.SellShort -> fail(entry.line, null, RestoreReason.SharesNegative(reason.date))
                    is SplitRefusal.BeforeOpeningBalance -> fail(entry.line, "date", RestoreReason.DatedBeforeOpeningBalance(reason.openingDate))
                    SplitRefusal.RatioOutOfRange -> fail(entry.line, "split_to", RestoreReason.SplitNumberTooLarge)
                    SplitRefusal.RatioEqual -> fail(entry.line, "split_from", RestoreReason.SplitNumbersEqual)
                    SplitRefusal.PortfolioNotFound -> fail(entry.line, "portfolio", RestoreReason.PortfolioNotFound)
                    SplitRefusal.NoOpeningBalance -> fail(entry.line, null, RestoreReason.NoOpeningBalance)
                }
        }
    }

    // A split of a ticker with no buy before it: dependent when the holding's buy or sell rows failed (decision 2).
    private fun splitWithoutBuy(entry: SplitRecord) {
        val failed = failedHoldings[key(entry.portfolio) to entry.ticker]
        if (failed != null) dependsOn(entry.line, "ticker", failed) else fail(entry.line, "ticker", RestoreReason.SplitTickerNotBought)
    }

    // Section 8, reversals: the target found and not failed, listed before, not the opening balance, not reversed yet,
    // not dated after the reversal; a note only on a cash reversal, the note of 4.6 when it has none.
    private fun reversal(entry: ReversalRecord) {
        val line = ids[entry.reverses.lowercase(Locale.ROOT)] ?: return fail(entry.line, "reverses", RestoreReason.ReversalTargetNotFound(entry.reverses))
        if (line in roots) return dependsOn(entry.line, "reverses", line)
        val target = entryByLine[line]
        val earlier = reversedBy[line]
        val refusal =
            when {
                target == null || line > entry.line -> RestoreReason.ReversalTargetListedAfter(line)
                target is CashRecord && target.type == CashType.OPENING_BALANCE -> RestoreReason.ReversalOfOpeningBalance
                earlier != null -> RestoreReason.AlreadyReversed(earlier)
                else -> null
            }
        if (refusal != null || target == null) return fail(entry.line, "reverses", refusal ?: RestoreReason.ReversalTargetListedAfter(line))
        if (target.date > entry.date) return fail(entry.line, "date", RestoreReason.ReversalBeforeOriginal(target.date))
        val original = appIds.getValue(line)
        if (target is CashRecord) {
            val note = entry.note.ifBlank { context.reversalNote(target.type, target.date) }
            when (val made = CashEntries.reverse(cash, context.newId(), original, entry.date, note)) {
                is CashEntryResult.Created -> cash = cash + made.entry
                is CashEntryResult.Refused -> return cashRefused(entry.line, made.reason)
            }
        } else {
            if (entry.note.isNotEmpty()) return fail(entry.line, "note", RestoreReason.CellNotUsed)
            when (val made = Reversals.reverse(cash, transactions, context.newId(), context.newId(), original, entry.date)) {
                is ReversalResult.Created -> {
                    transactions = transactions + made.transaction
                    made.cashLine?.let { cash = cash + it }
                }

                is ReversalResult.Refused -> return reversalRefused(entry.line, made.reason)
            }
        }
        reversedBy[line] = entry.line
    }

    private fun reversalRefused(
        line: Int,
        reason: ReversalRefusal,
    ) {
        when (reason) {
            is ReversalRefusal.BeforeOriginal -> fail(line, "date", RestoreReason.ReversalBeforeOriginal(reason.originalDate))
            is ReversalRefusal.SellNotReversed -> fail(line, null, RestoreReason.SharesNegative(reason.date))
            is ReversalRefusal.TooEarly -> fail(line, null, RestoreReason.SharesNegative(reason.date))
            is ReversalRefusal.SellShort -> fail(line, null, RestoreReason.SharesNegative(reason.date))
            is ReversalRefusal.NegativeCash -> fail(line, null, RestoreReason.CashNegative(reason.date, reason.shortfallCents))
            ReversalRefusal.NotFound -> fail(line, "reverses", RestoreReason.ReversalTargetNotFound(""))
            ReversalRefusal.NotReversible -> fail(line, "reverses", RestoreReason.ReversalOfOpeningBalance)
            ReversalRefusal.TooLarge -> fail(line, null, RestoreReason.BalanceTooLarge)
        }
    }

    // Section 8, `list_value` and `source_text` (4.9): values matched with D7's key within a list, each text mapped to
    // a value of its list, no text matching another, and no value matching a text.
    private fun lists(): ClassLists {
        val values = mapOf(ClassList.SECTOR to ArrayList<String>(), ClassList.GEOGRAPHY to ArrayList())
        val valueLines = HashMap<Pair<ClassList, String>, Int>()
        val valueRows = records.filterIsInstance<ListValueRecord>()
        for (row in valueRows) {
            val key = row.list to key(row.value)
            val first = valueLines[key]
            if (first != null) {
                fail(row.line, "value", RestoreReason.DuplicateListValue(first))
                continue
            }
            valueLines[key] = row.line
            values.getValue(row.list).add(row.value)
        }
        val remembered = ArrayList<RememberedText>()
        val textLines = HashMap<Pair<ClassList, String>, Int>()
        for (row in records.filterIsInstance<SourceTextRecord>()) {
            val spelled = values.getValue(row.list).firstOrNull { key(it) == key(row.value) }
            if (spelled == null) {
                fail(row.line, "value", RestoreReason.RememberedTextValueNotOnList)
                continue
            }
            val key = row.list to key(row.text)
            val first = textLines[key]
            if (first != null) {
                fail(row.line, "source_text", RestoreReason.DuplicateRememberedText(first))
                continue
            }
            textLines[key] = row.line
            remembered.add(RememberedText(row.list, row.text, spelled))
        }
        for (row in valueRows) {
            if (valueLines[row.list to key(row.value)] != row.line) continue
            textLines[row.list to key(row.value)]?.let { fail(row.line, "value", RestoreReason.ListValueMatchesRememberedText(it)) }
        }
        return ClassLists(values.getValue(ClassList.SECTOR), values.getValue(ClassList.GEOGRAPHY), remembered)
    }

    // The holding a state row names: its portfolio and ticker, or null after its error (dependent, a duplicate, not found).
    private fun holding(
        line: Int,
        portfolioName: String,
        ticker: String,
        seen: MutableMap<Pair<String, String>, Int>,
        duplicate: (Int) -> RestoreReason,
        notFound: RestoreReason,
    ): Portfolio? {
        val portfolio = portfolio(line, portfolioName) ?: return null
        failedHoldings[key(portfolioName) to ticker]?.let {
            dependsOn(line, "ticker", it)
            return null
        }
        seen[portfolio.id to ticker]?.let {
            fail(line, "ticker", duplicate(it))
            return null
        }
        seen[portfolio.id to ticker] = line
        val exists =
            transactions.any {
                (it.type == TransactionType.BUY || it.type == TransactionType.SELL) && it.portfolioId == portfolio.id && it.ticker == ticker
            }
        if (!exists) {
            fail(line, "ticker", notFound)
            return null
        }
        return portfolio
    }

    // Section 8, `hidden`: after the replay the holding exists and has 0 shares, counting every entry (4.4).
    private fun hidden(): List<HiddenHolding> {
        val seen = HashMap<Pair<String, String>, Int>()
        val out = ArrayList<HiddenHolding>()
        for (row in records.filterIsInstance<HiddenRecord>()) {
            val portfolio =
                holding(row.line, row.portfolio, row.ticker, seen, { RestoreReason.DuplicateHidden(it) }, RestoreReason.HiddenHoldingNotFound)
                    ?: continue
            if (!Hiding.offered(transactions, emptySet(), portfolio.id, row.ticker)) {
                fail(row.line, "ticker", RestoreReason.HiddenHoldingHasShares)
                continue
            }
            out.add(HiddenHolding(portfolio.id, row.ticker))
        }
        return out
    }

    // Section 8, `override`: the holding exists, and each value is on its list, kept as the list spells it (4.9).
    private fun overrides(lists: ClassLists): List<ClassOverride> {
        val seen = HashMap<Pair<String, String>, Int>()
        val out = ArrayList<ClassOverride>()
        for (row in records.filterIsInstance<OverrideRecord>()) {
            val portfolio =
                holding(row.line, row.portfolio, row.ticker, seen, { RestoreReason.DuplicateOverride(it) }, RestoreReason.OverrideHoldingNotFound)
                    ?: continue
            val made = ArrayList<ClassOverride>()
            for ((list, column, value) in listOf(Triple(ClassList.SECTOR, "sector", row.sector), Triple(ClassList.GEOGRAPHY, "geography", row.geography))) {
                if (value == null) continue
                val spelled = lists[list].firstOrNull { key(it) == key(value) }
                if (spelled == null) fail(row.line, column, RestoreReason.OverrideValueNotOnList) else made.add(ClassOverride(portfolio.id, row.ticker, list, spelled))
            }
            if (made.size == listOfNotNull(row.sector, row.geography).size) out.addAll(made)
        }
        return out
    }

    private class Settings(
        val autoLock: AutoLock,
        val tabOrder: TabOrder,
        val priceSource: String,
    )

    // Section 8, settings keys: one row per key; a missing row is never an error and its default applies.
    private fun settings(): Settings {
        val byKey = HashMap<String, SettingRecord>()
        for (row in records.filterIsInstance<SettingRecord>()) {
            val first = byKey[row.key]
            if (first != null) fail(row.line, "key", RestoreReason.SettingKeyTwice(first.line)) else byKey[row.key] = row
        }
        return Settings(
            autoLock = byKey[BackupRows.SETTING_AUTO_LOCK]?.let { BackupRows.AUTO_LOCK_WORDS[it.value] } ?: AutoLock.EVERY_LEAVE,
            tabOrder = byKey[BackupRows.SETTING_TAB_ORDER]?.let { TabOrder.parse(it.value) } ?: TabOrder.DEFAULT,
            priceSource = byKey[BackupRows.SETTING_PRICE_SOURCE]?.value ?: context.defaultSource,
        )
    }

    private companion object {
        val ID_KINDS = setOf(RowKind.CASH, RowKind.BUY, RowKind.SELL, RowKind.SPLIT)
    }
}
