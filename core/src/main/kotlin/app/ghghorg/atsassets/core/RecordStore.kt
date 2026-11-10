package app.ghghorg.atsassets.core

/** K2: where records are kept. `core/` knows no database, key or Android class. */
interface RecordStore {
    /** Throws [UnreadableDataException] if stored data exists but cannot be read. */
    suspend fun loadCashEntries(): List<CashEntry>

    /** Saves the entries as one all-or-nothing batch. */
    suspend fun saveCashEntries(entries: List<CashEntry>)

    /** Throws [UnreadableDataException] if stored data exists but cannot be read. */
    suspend fun loadPortfolios(): List<Portfolio>

    /** Saves one new portfolio. */
    suspend fun savePortfolio(portfolio: Portfolio)

    /** Changes the name of portfolio [id] and nothing else (4.7). Throws if no portfolio, or more than one, changed. */
    suspend fun renamePortfolio(
        id: String,
        name: String,
    )

    /** Each sell with its allocations. Throws [UnreadableDataException] if stored data exists but cannot be read. */
    suspend fun loadTransactions(): List<Transaction>

    /**
     * Saves a buy and its trade cash line together: both or neither (open item 14). The hidden holdings it gives shares
     * again, [unhide], are unhidden in the same action (4.4; 15 decision 2).
     */
    suspend fun saveBuy(
        transaction: Transaction,
        cashLine: CashEntry,
        unhide: List<HiddenHolding> = emptyList(),
    )

    /** Saves a sell, its trade cash line and its allocations together: all or nothing (open item 14). */
    suspend fun saveSell(
        transaction: Transaction,
        cashLine: CashEntry,
    )

    /** Saves a split with its ratio; it has no cash line (4.5). */
    suspend fun saveSplit(transaction: Transaction)

    /**
     * Saves a reversal and, for a buy's or sell's reversal, its trade cash line together: both or neither (4.6). A sell's
     * reversal unhides [unhide] in the same action (4.4; 15 decision 2); a split's reversal never unhides.
     */
    suspend fun saveReversal(
        transaction: Transaction,
        cashLine: CashEntry?,
        unhide: List<HiddenHolding> = emptyList(),
    )

    /**
     * Saves a Reverse and re-enter pair together: the reversal, its trade cash line, the new buy or sell, its trade cash
     * line and a new sell's allocations, and the unhiding of [unhide]; all or nothing (4.4, 4.6, K2; 15 decision 2).
     */
    suspend fun savePair(
        reversal: Transaction,
        reversalCashLine: CashEntry,
        replacement: Transaction,
        replacementCashLine: CashEntry,
        unhide: List<HiddenHolding> = emptyList(),
    )

    /**
     * Saves a split's Reverse and re-enter pair together: the reversal and the new split, neither with a cash line; both
     * or neither (4.6, K2). A cash entry's pair is two cash entries, saved by [saveCashEntries] as one batch.
     */
    suspend fun saveSplitPair(
        reversal: Transaction,
        replacement: Transaction,
    )

    /** Throws [UnreadableDataException] if stored data exists but cannot be read. */
    suspend fun loadInstruments(): List<Instrument>

    /** Saves an instrument, replacing the one with the same ticker but keeping its stored price (K4 only). */
    suspend fun saveInstrument(instrument: Instrument)

    /**
     * After a price refresh (K4 only, 16a), as one all-or-nothing batch: each of [fresh] becomes its ticker's latest
     * price, not stale; the stored price of each of [failed] is kept and marked stale (4.8). A ticker that never had a
     * price keeps none, and a ticker with no stored instrument is skipped.
     */
    suspend fun savePrices(
        fresh: Map<String, Price>,
        failed: Collection<String>,
    )

    /** The hidden holdings, each at 0 shares. Throws [UnreadableDataException] if stored data cannot be read or checked. */
    suspend fun loadHidden(): List<HiddenHolding>

    /**
     * As [loadHidden], checked against [transactions] the caller has just loaded, so a store need not read them again
     * (iteration 15c). By default it is [loadHidden].
     */
    suspend fun loadHidden(transactions: List<Transaction>): List<HiddenHolding> = loadHidden()

    /** Hides [holding] (4.4). Not a transaction; no cash effect. Throws if it is already hidden. */
    suspend fun hide(holding: HiddenHolding)

    /** Unhides [holding] (view 9). Throws if it was not hidden. */
    suspend fun unhide(holding: HiddenHolding)

    /**
     * The auto-lock choice (spec section 9), [AutoLock.EVERY_LEAVE] when none was saved. Throws
     * [UnreadableDataException] if stored data exists but cannot be read.
     */
    suspend fun loadAutoLock(): AutoLock

    /** Saves the auto-lock choice in place of the one before; the other settings are kept (20 decision 3). */
    suspend fun saveAutoLock(choice: AutoLock)
}

class UnreadableDataException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
