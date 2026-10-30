package app.ghghorg.atsassets.data

import androidx.room3.AutoMigration
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.RoomDatabase

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

/** Version 2 (iteration 07) adds the portfolios table; Room generates the step from 1 out of schemas/1.json and 2.json. */
@Database(
    entities = [CashEntryRow::class, PortfolioRow::class],
    version = 2,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class AtsDatabase : RoomDatabase() {
    abstract fun cashEntryDao(): CashEntryDao

    abstract fun portfolioDao(): PortfolioDao
}
