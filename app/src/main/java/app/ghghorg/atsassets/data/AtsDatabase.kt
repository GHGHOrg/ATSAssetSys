package app.ghghorg.atsassets.data

import androidx.room3.AutoMigration
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.RoomDatabase
import androidx.room3.Transaction
import androidx.room3.Upsert

/** One cash entry row. The date is text (YYYY-MM-DD), the amount is integer cents (4.10). */
@Entity(tableName = "cash_entries")
data class CashEntryRow(
    @PrimaryKey val id: String,
    val date: String,
    val amountCents: Long,
    val type: String,
    val note: String,
    val enteredSeq: Long,
    val reversesId: String?,
    val transactionId: String?,
)

/** One portfolio row (spec 3). createdSeq keeps the order of creation. */
@Entity(tableName = "portfolios")
data class PortfolioRow(
    @PrimaryKey val id: String,
    val name: String,
    val createdSeq: Long,
)

/**
 * One transaction row (spec 3). The date is text (YYYY-MM-DD), the quantity is the exact decimal as typed, as text
 * (4.10), the total is integer cents.
 */
@Entity(tableName = "transactions")
data class TransactionRow(
    @PrimaryKey val id: String,
    val portfolioId: String,
    val ticker: String,
    val type: String,
    val date: String,
    val enteredSeq: Long,
    val quantity: String,
    val totalCents: Long,
    val reversesId: String?,
)

/** One instrument row (spec 3): a verified ticker and its asset type. Prices and classification join later (16, 23). */
@Entity(tableName = "instruments")
data class InstrumentRow(
    @PrimaryKey val ticker: String,
    val assetType: String,
    val verified: Boolean,
)

@Dao
interface CashEntryDao {
    /** One transaction. The default conflict rule (abort) is kept: records are permanent. */
    @Insert
    suspend fun insertAll(rows: List<CashEntryRow>)

    @Query("SELECT * FROM cash_entries ORDER BY date, enteredSeq")
    suspend fun loadAll(): List<CashEntryRow>
}

@Dao
interface PortfolioDao {
    /** The default conflict rule (abort) is kept. */
    @Insert
    suspend fun insert(row: PortfolioRow)

    @Query("SELECT * FROM portfolios ORDER BY createdSeq")
    suspend fun loadAll(): List<PortfolioRow>

    /** Rename (4.7): the name of one row by id, nothing else. Returns the number of rows changed. */
    @Query("UPDATE portfolios SET name = :name WHERE id = :id")
    suspend fun rename(
        id: String,
        name: String,
    ): Int
}

@Dao
interface TransactionDao {
    /** The default conflict rule (abort) is kept: records are permanent. */
    @Insert
    suspend fun insertTransaction(row: TransactionRow)

    /** The default conflict rule (abort) is kept. */
    @Insert
    suspend fun insertCashEntry(row: CashEntryRow)

    /** A buy and its trade cash line in one database transaction: both or neither (open item 14). */
    @Transaction
    suspend fun insertBuy(
        transaction: TransactionRow,
        cashLine: CashEntryRow,
    ) {
        insertTransaction(transaction)
        insertCashEntry(cashLine)
    }

    @Query("SELECT * FROM transactions ORDER BY date, enteredSeq")
    suspend fun loadAll(): List<TransactionRow>
}

@Dao
interface InstrumentDao {
    /** Inserts, or replaces the row with the same ticker (K4 only). */
    @Upsert
    suspend fun upsert(row: InstrumentRow)

    @Query("SELECT * FROM instruments ORDER BY ticker")
    suspend fun loadAll(): List<InstrumentRow>
}

/**
 * Version 2 (iteration 07) adds the portfolios table, version 3 (09) the transactions and instruments tables; Room
 * generates each step out of the schemas in app/schemas.
 */
@Database(
    entities = [CashEntryRow::class, PortfolioRow::class, TransactionRow::class, InstrumentRow::class],
    version = 3,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
)
abstract class AtsDatabase : RoomDatabase() {
    abstract fun cashEntryDao(): CashEntryDao

    abstract fun portfolioDao(): PortfolioDao

    abstract fun transactionDao(): TransactionDao

    abstract fun instrumentDao(): InstrumentDao
}
