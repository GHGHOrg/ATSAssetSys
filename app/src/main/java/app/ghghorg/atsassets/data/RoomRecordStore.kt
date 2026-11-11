package app.ghghorg.atsassets.data

import app.ghghorg.atsassets.core.AssetType
import app.ghghorg.atsassets.core.AutoLock
import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.ClassList
import app.ghghorg.atsassets.core.ClassLists
import app.ghghorg.atsassets.core.Classification
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.HiddenHolding
import app.ghghorg.atsassets.core.Hiding
import app.ghghorg.atsassets.core.Instrument
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.Price
import app.ghghorg.atsassets.core.RecordStore
import app.ghghorg.atsassets.core.SellAllocation
import app.ghghorg.atsassets.core.SourceProfile
import app.ghghorg.atsassets.core.SplitRatio
import app.ghghorg.atsassets.core.Splits
import app.ghghorg.atsassets.core.TabOrder
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import app.ghghorg.atsassets.core.UnreadableDataException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlin.coroutines.cancellation.CancellationException

/** K2 on Room and SQLCipher. Every failure to open or read stored data is "unreadable". */
class RoomRecordStore(
    private val key: DatabaseKey,
    private val open: (passphrase: ByteArray, file: File) -> AtsDatabase,
) : RecordStore {
    private val database: AtsDatabase by lazy {
        open(key.loadOrCreatePassphrase(), key.databaseFile)
    }

    override suspend fun loadCashEntries(): List<CashEntry> = withContext(Dispatchers.IO) {
        try {
            database.cashEntryDao().loadAll().map { it.toEntry() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: UnreadableDataException) {
            throw e
        } catch (e: Exception) {
            throw UnreadableDataException("The stored data could not be read.", e)
        }
    }

    override suspend fun saveCashEntries(entries: List<CashEntry>) {
        val dao = withContext(Dispatchers.IO) {
            try {
                database.cashEntryDao()
            } catch (e: CancellationException) {
                throw e
            } catch (e: UnreadableDataException) {
                throw e
            } catch (e: Exception) {
                throw UnreadableDataException("The stored data could not be opened.", e)
            }
        }
        dao.insertAll(entries.map { it.toRow() })
    }

    override suspend fun loadPortfolios(): List<Portfolio> = withContext(Dispatchers.IO) {
        try {
            database.portfolioDao().loadAll().map { it.toPortfolio() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: UnreadableDataException) {
            throw e
        } catch (e: Exception) {
            throw UnreadableDataException("The stored data could not be read.", e)
        }
    }

    override suspend fun savePortfolio(portfolio: Portfolio) {
        val dao = withContext(Dispatchers.IO) {
            try {
                database.portfolioDao()
            } catch (e: CancellationException) {
                throw e
            } catch (e: UnreadableDataException) {
                throw e
            } catch (e: Exception) {
                throw UnreadableDataException("The stored data could not be opened.", e)
            }
        }
        dao.insert(portfolio.toRow())
    }

    override suspend fun renamePortfolio(
        id: String,
        name: String,
    ) {
        val dao = withContext(Dispatchers.IO) {
            try {
                database.portfolioDao()
            } catch (e: CancellationException) {
                throw e
            } catch (e: UnreadableDataException) {
                throw e
            } catch (e: Exception) {
                throw UnreadableDataException("The stored data could not be opened.", e)
            }
        }
        // The message names only the count: never a portfolio name (spec section 9).
        val changed = dao.rename(id, name)
        check(changed == 1) { "A rename changed $changed rows instead of 1." }
    }

    override suspend fun loadTransactions(): List<Transaction> = withContext(Dispatchers.IO) {
        try {
            val dao = database.transactionDao()
            checkReversals(withAllocations(dao.loadAll().map { it.toTransaction() }, dao.loadAllocations()))
        } catch (e: CancellationException) {
            throw e
        } catch (e: UnreadableDataException) {
            throw e
        } catch (e: Exception) {
            throw UnreadableDataException("The stored data could not be read.", e)
        }
    }

    override suspend fun saveReversal(
        transaction: Transaction,
        cashLine: CashEntry?,
        unhide: List<HiddenHolding>,
    ) {
        // A split's reversal has no cash line and never takes a holding from 0 to shares (4.4).
        require(cashLine != null || unhide.isEmpty()) { "A reversal without a cash line unhides nothing." }
        val dao = withContext(Dispatchers.IO) {
            try {
                database.transactionDao()
            } catch (e: CancellationException) {
                throw e
            } catch (e: UnreadableDataException) {
                throw e
            } catch (e: Exception) {
                throw UnreadableDataException("The stored data could not be opened.", e)
            }
        }
        if (cashLine == null) {
            dao.insertTransaction(transaction.toRow())
        } else {
            dao.insertReversal(transaction.toRow(), cashLine.toRow(), unhide.map { it.toRow() })
        }
    }

    override suspend fun saveBuy(
        transaction: Transaction,
        cashLine: CashEntry,
        unhide: List<HiddenHolding>,
    ) {
        val dao = withContext(Dispatchers.IO) {
            try {
                database.transactionDao()
            } catch (e: CancellationException) {
                throw e
            } catch (e: UnreadableDataException) {
                throw e
            } catch (e: Exception) {
                throw UnreadableDataException("The stored data could not be opened.", e)
            }
        }
        dao.insertBuy(transaction.toRow(), cashLine.toRow(), unhide.map { it.toRow() })
    }

    override suspend fun saveSell(
        transaction: Transaction,
        cashLine: CashEntry,
    ) {
        val dao = withContext(Dispatchers.IO) {
            try {
                database.transactionDao()
            } catch (e: CancellationException) {
                throw e
            } catch (e: UnreadableDataException) {
                throw e
            } catch (e: Exception) {
                throw UnreadableDataException("The stored data could not be opened.", e)
            }
        }
        dao.insertSell(transaction.toRow(), cashLine.toRow(), transaction.allocationRows())
    }

    override suspend fun saveSplit(transaction: Transaction) {
        val dao = withContext(Dispatchers.IO) {
            try {
                database.transactionDao()
            } catch (e: CancellationException) {
                throw e
            } catch (e: UnreadableDataException) {
                throw e
            } catch (e: Exception) {
                throw UnreadableDataException("The stored data could not be opened.", e)
            }
        }
        dao.insertTransaction(transaction.toRow())
    }

    override suspend fun savePair(
        reversal: Transaction,
        reversalCashLine: CashEntry,
        replacement: Transaction,
        replacementCashLine: CashEntry,
        unhide: List<HiddenHolding>,
    ) {
        val dao = withContext(Dispatchers.IO) {
            try {
                database.transactionDao()
            } catch (e: CancellationException) {
                throw e
            } catch (e: UnreadableDataException) {
                throw e
            } catch (e: Exception) {
                throw UnreadableDataException("The stored data could not be opened.", e)
            }
        }
        dao.insertPair(
            reversal = reversal.toRow(),
            reversalCashLine = reversalCashLine.toRow(),
            replacement = replacement.toRow(),
            replacementCashLine = replacementCashLine.toRow(),
            allocations = replacement.allocationRows(),
            unhide = unhide.map { it.toRow() },
        )
    }

    override suspend fun saveSplitPair(
        reversal: Transaction,
        replacement: Transaction,
    ) {
        val dao = withContext(Dispatchers.IO) {
            try {
                database.transactionDao()
            } catch (e: CancellationException) {
                throw e
            } catch (e: UnreadableDataException) {
                throw e
            } catch (e: Exception) {
                throw UnreadableDataException("The stored data could not be opened.", e)
            }
        }
        dao.insertSplitPair(reversal.toRow(), replacement.toRow())
    }

    override suspend fun loadInstruments(): List<Instrument> = withContext(Dispatchers.IO) {
        try {
            database.instrumentDao().loadAll().map { it.toInstrument() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: UnreadableDataException) {
            throw e
        } catch (e: Exception) {
            throw UnreadableDataException("The stored data could not be read.", e)
        }
    }

    override suspend fun saveInstrument(instrument: Instrument) {
        val dao = withContext(Dispatchers.IO) {
            try {
                database.instrumentDao()
            } catch (e: CancellationException) {
                throw e
            } catch (e: UnreadableDataException) {
                throw e
            } catch (e: Exception) {
                throw UnreadableDataException("The stored data could not be opened.", e)
            }
        }
        dao.upsertKeepingPrice(instrument.toRow())
    }

    override suspend fun savePrices(
        fresh: Map<String, Price>,
        failed: Collection<String>,
        profiles: Map<String, SourceProfile>,
    ) {
        val dao = withContext(Dispatchers.IO) {
            try {
                database.instrumentDao()
            } catch (e: CancellationException) {
                throw e
            } catch (e: UnreadableDataException) {
                throw e
            } catch (e: Exception) {
                throw UnreadableDataException("The stored data could not be opened.", e)
            }
        }
        dao.savePrices(fresh.mapValues { (_, price) -> price.perShare.toPlainString() to price.time.epochSecond }, failed, profiles)
    }

    override suspend fun loadHidden(): List<HiddenHolding> = loadHidden(loadTransactions())

    /** Checked against the transactions the caller has just loaded: they are not read a second time (iteration 15c). */
    override suspend fun loadHidden(transactions: List<Transaction>): List<HiddenHolding> = withContext(Dispatchers.IO) {
        try {
            checkHidden(transactions, database.transactionDao().loadHidden().map { it.toHidden() })
        } catch (e: CancellationException) {
            throw e
        } catch (e: UnreadableDataException) {
            throw e
        } catch (e: Exception) {
            throw UnreadableDataException("The stored data could not be read.", e)
        }
    }

    override suspend fun hide(holding: HiddenHolding) {
        hiddenDao().insertHidden(holding.toRow())
    }

    override suspend fun unhide(holding: HiddenHolding) {
        hiddenDao().unhide(holding.toRow())
    }

    override suspend fun loadAutoLock(): AutoLock = withContext(Dispatchers.IO) {
        try {
            database.settingsDao().load().toAutoLock()
        } catch (e: CancellationException) {
            throw e
        } catch (e: UnreadableDataException) {
            throw e
        } catch (e: Exception) {
            throw UnreadableDataException("The stored data could not be read.", e)
        }
    }

    override suspend fun saveAutoLock(choice: AutoLock) {
        val dao = withContext(Dispatchers.IO) {
            try {
                database.settingsDao()
            } catch (e: CancellationException) {
                throw e
            } catch (e: UnreadableDataException) {
                throw e
            } catch (e: Exception) {
                throw UnreadableDataException("The stored data could not be opened.", e)
            }
        }
        dao.saveAutoLock(choice.name)
    }

    override suspend fun loadTabOrder(): TabOrder = withContext(Dispatchers.IO) {
        try {
            database.settingsDao().load().toTabOrder()
        } catch (e: CancellationException) {
            throw e
        } catch (e: UnreadableDataException) {
            throw e
        } catch (e: Exception) {
            throw UnreadableDataException("The stored data could not be read.", e)
        }
    }

    override suspend fun saveTabOrder(order: TabOrder) {
        val dao = withContext(Dispatchers.IO) {
            try {
                database.settingsDao()
            } catch (e: CancellationException) {
                throw e
            } catch (e: UnreadableDataException) {
                throw e
            } catch (e: Exception) {
                throw UnreadableDataException("The stored data could not be opened.", e)
            }
        }
        dao.saveTabOrder(order.text)
    }

    override suspend fun loadClassLists(): ClassLists = withContext(Dispatchers.IO) {
        try {
            database.instrumentDao().loadListValues().toClassLists()
        } catch (e: CancellationException) {
            throw e
        } catch (e: UnreadableDataException) {
            throw e
        } catch (e: Exception) {
            throw UnreadableDataException("The stored data could not be read.", e)
        }
    }

    private suspend fun hiddenDao(): TransactionDao = withContext(Dispatchers.IO) {
        try {
            database.transactionDao()
        } catch (e: CancellationException) {
            throw e
        } catch (e: UnreadableDataException) {
            throw e
        } catch (e: Exception) {
            throw UnreadableDataException("The stored data could not be opened.", e)
        }
    }
}

/**
 * The quantity is stored as the exact text of the decimal as typed, so "2.50" stays "2.50" (4.10). A split's ratio and
 * a buy's quantity ratio share the two ratio columns (spec section 8, buy row; CR1).
 */
internal fun Transaction.toRow() = TransactionRow(
    id = id,
    portfolioId = portfolioId,
    ticker = ticker,
    type = type.name,
    date = date.toString(),
    enteredSeq = enteredSeq,
    quantity = quantity.toPlainString(),
    totalCents = totalCents,
    reversesId = reversesId,
    splitTo = (split ?: quantityRatio)?.to,
    splitFrom = (split ?: quantityRatio)?.from,
)

/**
 * Throws [UnreadableDataException] for a split without a ratio that 4.5 allows (whole numbers from 1 to
 * [Splits.MAX_RATIO], not equal), a buy with half a ratio or one that is not two unequal whole numbers above 0 (CR1),
 * a sell or reversal with a ratio, a reversal without its link, or a buy, sell or split with one (13b decision 1). The
 * messages name no value (spec section 9).
 */
internal fun TransactionRow.toTransaction(): Transaction {
    val transactionType = TransactionType.valueOf(type)
    if ((transactionType == TransactionType.REVERSAL) != (reversesId != null)) {
        throw UnreadableDataException("A stored transaction has a damaged reversal link.")
    }
    val ratio =
        if (transactionType == TransactionType.SPLIT) {
            val to = splitTo ?: throw UnreadableDataException("A stored split has no ratio.")
            val from = splitFrom ?: throw UnreadableDataException("A stored split has no ratio.")
            if (to !in 1..Splits.MAX_RATIO || from !in 1..Splits.MAX_RATIO || to == from) {
                throw UnreadableDataException("A stored split ratio could not be read.")
            }
            SplitRatio(to, from)
        } else {
            null
        }
    val quantityRatio =
        when {
            transactionType == TransactionType.SPLIT || (splitTo == null && splitFrom == null) -> null

            transactionType != TransactionType.BUY -> throw UnreadableDataException("A stored sell or reversal has a split ratio.")

            splitTo == null || splitFrom == null || splitTo < 1 || splitFrom < 1 || splitTo == splitFrom ->
                throw UnreadableDataException("A stored buy's quantity ratio could not be read.")

            else -> SplitRatio(splitTo, splitFrom)
        }
    return Transaction(
        id = id,
        portfolioId = portfolioId,
        ticker = ticker,
        type = transactionType,
        date = LocalDate.parse(date),
        enteredSeq = enteredSeq,
        quantity = BigDecimal(quantity),
        totalCents = totalCents,
        reversesId = reversesId,
        split = ratio,
        quantityRatio = quantityRatio,
    )
}

/**
 * Checks each stored reversal against its original (4.6; 13b decision 1): the original is there, is a buy, sell or
 * split, has no other reversal, is not dated after it, and is in the same portfolio and ticker. Throws
 * [UnreadableDataException] otherwise; nothing is guessed. The messages name no value (spec section 9).
 */
internal fun checkReversals(transactions: List<Transaction>): List<Transaction> {
    val byId = transactions.associateBy { it.id }
    val reversals = transactions.filter { it.type == TransactionType.REVERSAL }
    if (reversals.groupBy { it.reversesId }.any { it.value.size > 1 }) {
        throw UnreadableDataException("A stored transaction is reversed twice.")
    }
    for (reversal in reversals) {
        val original = byId[reversal.reversesId] ?: throw UnreadableDataException("A stored reversal has no original.")
        val matches =
            original.type != TransactionType.REVERSAL &&
                !reversal.date.isBefore(original.date) &&
                reversal.portfolioId == original.portfolioId &&
                reversal.ticker == original.ticker
        if (!matches) throw UnreadableDataException("A stored reversal does not match its original.")
    }
    return transactions
}

/** A sell's allocations as rows, each quantity in its stored form (11 decision 3); none for a buy. */
internal fun Transaction.allocationRows(): List<SellAllocationRow> = allocations.map {
    SellAllocationRow(sellId = id, lotId = it.lotId, quantity = it.quantity.stored())
}

/**
 * Puts each stored allocation back into its sell, oldest lot first (4.4). The messages name no value (spec section 9).
 * Throws [UnreadableDataException] for a quantity not in the stored form, an allocation of no sell, or a sell with none.
 */
internal fun withAllocations(
    transactions: List<Transaction>,
    rows: List<SellAllocationRow>,
): List<Transaction> {
    val sellIds = transactions.filter { it.type == TransactionType.SELL }.map { it.id }.toSet()
    if (rows.any { it.sellId !in sellIds }) throw UnreadableDataException("A stored sell allocation belongs to no sell.")
    val lotOrder = transactions.filter { it.type == TransactionType.BUY }.associate { it.id to (it.date to it.enteredSeq) }
    val bySell = rows.groupBy { it.sellId }
    return transactions.map { transaction ->
        if (transaction.type != TransactionType.SELL) return@map transaction
        val own = bySell[transaction.id] ?: throw UnreadableDataException("A stored sell has no allocations.")
        val allocations =
            own
                .sortedWith(compareBy({ lotOrder[it.lotId]?.first }, { lotOrder[it.lotId]?.second }))
                .map { row ->
                    val quantity =
                        Fraction.fromStored(row.quantity) ?: throw UnreadableDataException("A stored sell allocation could not be read.")
                    SellAllocation(row.lotId, quantity)
                }
        transaction.copy(allocations = allocations)
    }
}

internal fun Instrument.toRow() = InstrumentRow(
    ticker = ticker,
    assetType = assetType.name,
    verified = verified,
    price = price?.perShare?.toPlainString(),
    priceTime = price?.time?.epochSecond,
    priceStale = price?.stale ?: false,
    sourceSector = profile?.sector,
    sourceGeography = profile?.geography,
    profileAsked = profile != null,
)

/**
 * Throws [UnreadableDataException] for a stored price that is not a decimal above 0, a price without its time or the
 * other way round, a time not above 0, or a stale flag without a price (16a). The messages name no value (spec 9).
 */
internal fun InstrumentRow.toInstrument(): Instrument {
    val stored =
        when {
            price == null && priceTime == null -> {
                if (priceStale) throw UnreadableDataException("A stored instrument is stale without a price.")
                null
            }

            price == null || priceTime == null -> throw UnreadableDataException("A stored price has no time, or a time no price.")

            else -> {
                val perShare = price.toBigDecimalOrNull()
                if (perShare == null || perShare.signum() <= 0 || priceTime <= 0) {
                    throw UnreadableDataException("A stored price is damaged.")
                }
                Price(perShare, Instant.ofEpochSecond(priceTime), priceStale)
            }
        }
    return Instrument(ticker = ticker, assetType = AssetType.valueOf(assetType), verified = verified, price = stored, profile = toProfile())
}

/**
 * The source profile (23a, decision 3): none until asked; once asked, its two texts, each possibly null. Throws
 * [UnreadableDataException] for a text without the asked mark. The message names no value (spec 9).
 */
private fun InstrumentRow.toProfile(): SourceProfile? {
    if (profileAsked) return SourceProfile(sourceSector, sourceGeography)
    if (sourceSector != null || sourceGeography != null) throw UnreadableDataException("A stored profile is damaged.")
    return null
}

/**
 * This row with the price columns (16a) and the profile columns (23a) of [stored]: saving an instrument again keeps
 * both, since only a refresh sets them.
 */
internal fun InstrumentRow.withStoredOf(stored: InstrumentRow?): InstrumentRow = copy(
    price = stored?.price,
    priceTime = stored?.priceTime,
    priceStale = stored?.priceStale ?: false,
    sourceSector = stored?.sourceSector,
    sourceGeography = stored?.sourceGeography,
    profileAsked = stored?.profileAsked ?: false,
)

/**
 * The two lists from their rows, each in the order added (23a). Throws [UnreadableDataException] for an unknown list
 * or two values in one list that match (D7); nothing is guessed. The messages name no value (spec 9).
 */
internal fun List<ListValueRow>.toClassLists(): ClassLists {
    val byList = ClassList.entries.associateWith { mutableListOf<String>() }
    for (row in sortedBy { it.seq }) {
        val list = ClassList.entries.firstOrNull { it.name == row.list } ?: throw UnreadableDataException("A stored list could not be read.")
        val values = byList.getValue(list)
        if (Classification.valueFor(values, row.value) != null) throw UnreadableDataException("A stored list holds a value twice.")
        values += row.value
    }
    return ClassLists(sectors = byList.getValue(ClassList.SECTOR), geographies = byList.getValue(ClassList.GEOGRAPHY))
}

private fun Portfolio.toRow() = PortfolioRow(id = id, name = name, createdSeq = createdSeq)

private fun PortfolioRow.toPortfolio() = Portfolio(id = id, name = name, createdSeq = createdSeq)

private fun CashEntry.toRow() = CashEntryRow(
    id = id,
    date = date.toString(),
    amountCents = amountCents,
    type = type.name,
    note = note,
    enteredSeq = enteredSeq,
    reversesId = reversesId,
    transactionId = transactionId,
)

private fun CashEntryRow.toEntry() = CashEntry(
    id = id,
    date = LocalDate.parse(date),
    amountCents = amountCents,
    type = CashType.valueOf(type),
    note = note,
    enteredSeq = enteredSeq,
    reversesId = reversesId,
    transactionId = transactionId,
)

/**
 * No settings row is the default, every time you leave (spec section 9). Throws [UnreadableDataException] for a stored
 * choice that is not one of the four; nothing is guessed, not even the default. The message names no value (spec 9).
 */
internal fun SettingsRow?.toAutoLock(): AutoLock {
    if (this == null) return AutoLock.EVERY_LEAVE
    return AutoLock.entries.firstOrNull { it.name == autoLock } ?: throw UnreadableDataException("A stored setting could not be read.")
}

/**
 * No settings row, or none with an order, is the default order (spec 6; 22). Throws [UnreadableDataException] for a
 * stored order that is not each tab once (spec 8's rule); nothing is guessed, not even the default.
 */
internal fun SettingsRow?.toTabOrder(): TabOrder {
    val text = this?.tabOrder ?: return TabOrder.DEFAULT
    return TabOrder.parse(text) ?: throw UnreadableDataException("A stored setting could not be read.")
}

/** The settings row with [autoLock] in place of its choice, or a new row with only that choice. */
internal fun SettingsRow?.withAutoLock(autoLock: String): SettingsRow = this?.copy(autoLock = autoLock) ?: SettingsRow(autoLock = autoLock)

/** The settings row with [tabOrder] in place of its order, or a new row with it and the default auto-lock choice. */
internal fun SettingsRow?.withTabOrder(tabOrder: String): SettingsRow {
    if (this == null) return SettingsRow(autoLock = AutoLock.EVERY_LEAVE.name, tabOrder = tabOrder)
    return copy(tabOrder = tabOrder)
}

internal fun HiddenHolding.toRow() = HiddenHoldingRow(portfolioId = portfolioId, ticker = ticker)

internal fun HiddenHoldingRow.toHidden() = HiddenHolding(portfolioId = portfolioId, ticker = ticker)

/**
 * Checks the stored hidden holdings against the transactions (4.4, section 8; 15 decision 1): each holding exists and
 * has 0 shares counting every entry. Throws [UnreadableDataException] otherwise; nothing is guessed. The messages name
 * no value (spec section 9).
 */
internal fun checkHidden(
    transactions: List<Transaction>,
    hidden: List<HiddenHolding>,
): List<HiddenHolding> {
    // Only the hidden holdings' own lots are worked out (iteration 15c).
    val held = Hiding.sharesAfterEveryEntry(transactions, hidden)
    for (holding in hidden) {
        val shares = held[holding] ?: throw UnreadableDataException("A stored hidden holding has no transactions.")
        if (shares.signum() != 0) throw UnreadableDataException("A stored hidden holding has shares.")
    }
    return hidden
}
