package app.ghghorg.atsassets.data

import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.RecordStore
import app.ghghorg.atsassets.core.UnreadableDataException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
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
