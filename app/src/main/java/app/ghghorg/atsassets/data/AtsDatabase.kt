package app.ghghorg.atsassets.data

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

@Dao
interface CashEntryDao {
    /** One transaction. The default conflict rule (abort) is kept: records are permanent. */
    @Insert
    suspend fun insertAll(rows: List<CashEntryRow>)

    @Query("SELECT * FROM cash_entries ORDER BY date, enteredSeq")
    suspend fun loadAll(): List<CashEntryRow>
}

@Database(entities = [CashEntryRow::class], version = 1)
abstract class AtsDatabase : RoomDatabase() {
    abstract fun cashEntryDao(): CashEntryDao
}
