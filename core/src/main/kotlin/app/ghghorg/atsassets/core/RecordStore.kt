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

    /** Saves a buy and its trade cash line together: both or neither (open item 14). */
    suspend fun saveBuy(
        transaction: Transaction,
        cashLine: CashEntry,
    )

    /** Saves a sell, its trade cash line and its allocations together: all or nothing (open item 14). */
    suspend fun saveSell(
        transaction: Transaction,
        cashLine: CashEntry,
    )

    /** Saves a split with its ratio; it has no cash line (4.5). */
    suspend fun saveSplit(transaction: Transaction)

    /** Saves a reversal and, for a buy's or sell's reversal, its trade cash line together: both or neither (4.6). */
    suspend fun saveReversal(
        transaction: Transaction,
        cashLine: CashEntry?,
    )

    /**
     * Saves a Reverse and re-enter pair together: the reversal, its trade cash line, the new buy or sell, its trade cash
     * line and a new sell's allocations; all or nothing (4.6, K2).
     */
    suspend fun savePair(
        reversal: Transaction,
        reversalCashLine: CashEntry,
        replacement: Transaction,
        replacementCashLine: CashEntry,
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

    /** Saves an instrument, replacing the one with the same ticker (K4 only). */
    suspend fun saveInstrument(instrument: Instrument)
}

class UnreadableDataException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
