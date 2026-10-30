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
}

class UnreadableDataException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
