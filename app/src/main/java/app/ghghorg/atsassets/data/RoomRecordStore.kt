package app.ghghorg.atsassets.data

import app.ghghorg.atsassets.core.AssetType
import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.Instrument
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.RecordStore
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import app.ghghorg.atsassets.core.UnreadableDataException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.math.BigDecimal
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
            database.transactionDao().loadAll().map { it.toTransaction() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: UnreadableDataException) {
            throw e
        } catch (e: Exception) {
            throw UnreadableDataException("The stored data could not be read.", e)
        }
    }

    override suspend fun saveBuy(
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
        dao.insertBuy(transaction.toRow(), cashLine.toRow())
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
        dao.upsert(instrument.toRow())
    }
}

/** The quantity is stored as the exact text of the decimal as typed, so "2.50" stays "2.50" (4.10). */
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
)

internal fun TransactionRow.toTransaction() = Transaction(
    id = id,
    portfolioId = portfolioId,
    ticker = ticker,
    type = TransactionType.valueOf(type),
    date = LocalDate.parse(date),
    enteredSeq = enteredSeq,
    quantity = BigDecimal(quantity),
    totalCents = totalCents,
    reversesId = reversesId,
)

internal fun Instrument.toRow() = InstrumentRow(ticker = ticker, assetType = assetType.name, verified = verified)

internal fun InstrumentRow.toInstrument() = Instrument(ticker = ticker, assetType = AssetType.valueOf(assetType), verified = verified)

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
