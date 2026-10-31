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

    /** Throws [UnreadableDataException] if stored data exists but cannot be read. */
    suspend fun loadTransactions(): List<Transaction>

    /** Saves a buy and its trade cash line together: both or neither (open item 14). */
    suspend fun saveBuy(
        transaction: Transaction,
        cashLine: CashEntry,
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
