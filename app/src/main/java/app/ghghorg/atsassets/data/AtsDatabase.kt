package app.ghghorg.atsassets.data

import androidx.room3.AutoMigration
import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.RoomDatabase
import androidx.room3.Transaction
import androidx.room3.Update
import androidx.room3.Upsert
import app.ghghorg.atsassets.core.ClassList
import app.ghghorg.atsassets.core.ClassOverride
import app.ghghorg.atsassets.core.Classification
import app.ghghorg.atsassets.core.SourceProfile

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
 * (4.10), the total is integer cents. A split keeps its ratio in splitTo and splitFrom (section 8's columns); every
 * other row leaves them empty.
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
    val splitTo: Int? = null,
    val splitFrom: Int? = null,
)

/**
 * One sell allocation row (spec 3): the exact quantity sell [sellId] took from the lot of buy [lotId], stored as a
 * reduced fraction in text, such as "5/2" (4.10, 11 decision 3). A sell takes from a lot at most once.
 */
@Entity(tableName = "sell_allocations", primaryKeys = ["sellId", "lotId"])
data class SellAllocationRow(
    val sellId: String,
    val lotId: String,
    val quantity: String,
)

/**
 * One hidden holding (spec 3, 4.4; section 8's `hidden` row): a ticker within a portfolio, hidden by you. A holding is
 * not stored otherwise; it is worked out from its transactions.
 */
@Entity(tableName = "hidden_holdings", primaryKeys = ["portfolioId", "ticker"])
data class HiddenHoldingRow(
    val portfolioId: String,
    val ticker: String,
)

/**
 * One instrument row (spec 3): a verified ticker, its asset type and, from 16a, its latest price: the exact decimal as
 * text (4.10), its time at the source in Unix seconds and the stale flag; empty and false when none was ever fetched.
 * Classification joins later (23).
 */
@Entity(tableName = "instruments")
data class InstrumentRow(
    @PrimaryKey val ticker: String,
    val assetType: String,
    val verified: Boolean,
    val price: String? = null,
    val priceTime: Long? = null,
    @ColumnInfo(defaultValue = "0") val priceStale: Boolean = false,
    // 23a: the source profile, asked once (decision 3); the two texts are null when the source gave none.
    val sourceSector: String? = null,
    val sourceGeography: String? = null,
    @ColumnInfo(defaultValue = "0") val profileAsked: Boolean = false,
)

/**
 * One value of the sector or geography list (spec 3, 4.9; 23a): [list] is a [app.ghghorg.atsassets.core.ClassList] by
 * name, and [seq] keeps the order values were added in. K10 adds a value only when none matches (D7), and reading
 * refuses two that match.
 */
@Entity(tableName = "list_values")
data class ListValueRow(
    @PrimaryKey(autoGenerate = true) val seq: Long = 0,
    val list: String,
    val value: String,
)

/**
 * A holding's override on one list (4.9; 24): [list] is a [app.ghghorg.atsassets.core.ClassList] by name and [valueSeq]
 * the [ListValueRow] it names (decision 3), so a rename (25) carries over. An override naming no row of its list is
 * unreadable.
 */
@Entity(tableName = "class_overrides", primaryKeys = ["portfolioId", "ticker", "list"])
data class ClassOverrideRow(
    val portfolioId: String,
    val ticker: String,
    val list: String,
    val valueSeq: Long,
)

/**
 * A remembered source text (spec 3, 4.9; 25): on [list], a [app.ghghorg.atsassets.core.ClassList] by name, the source's
 * [text] maps to the [ListValueRow] [valueSeq] names, so a rename of that value carries it over. A text naming no row of
 * its list, two texts of a list that match (D7), or a text that matches a value of its list is unreadable.
 */
@Entity(tableName = "source_texts")
data class SourceTextRow(
    @PrimaryKey(autoGenerate = true) val seq: Long = 0,
    val list: String,
    val valueSeq: Long,
    val text: String,
)

/**
 * The settings (spec section 3), one row with a column per setting (20 decision 3): the row's [id] is always
 * [ONLY_ID]. [autoLock] is an [app.ghghorg.atsassets.core.AutoLock] by name. [tabOrder] is spec 8's `tab_order` text
 * (22), null until an order is saved. Each later setting adds a column.
 */
@Entity(tableName = "settings")
data class SettingsRow(
    @PrimaryKey val id: Int = ONLY_ID,
    val autoLock: String,
    val tabOrder: String? = null,
) {
    companion object {
        const val ONLY_ID = 1
    }
}

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

    /** The default conflict rule (abort) is kept: a holding is hidden once. */
    @Insert
    suspend fun insertHidden(row: HiddenHoldingRow)

    /** Returns the number of rows removed. */
    @Query("DELETE FROM hidden_holdings WHERE portfolioId = :portfolioId AND ticker = :ticker")
    suspend fun deleteHidden(
        portfolioId: String,
        ticker: String,
    ): Int

    @Query("SELECT * FROM hidden_holdings ORDER BY portfolioId, ticker")
    suspend fun loadHidden(): List<HiddenHoldingRow>

    /** Removes each of [rows]; one that is not there fails the whole database transaction it runs in (15 decision 2). */
    suspend fun unhideAll(rows: List<HiddenHoldingRow>) {
        for (row in rows) {
            val removed = deleteHidden(row.portfolioId, row.ticker)
            check(removed == 1) { "An unhide removed $removed rows instead of 1." }
        }
    }

    /** One unhide in its own database transaction (view 9). */
    @Transaction
    suspend fun unhide(row: HiddenHoldingRow) {
        unhideAll(listOf(row))
    }

    /**
     * A buy, its trade cash line and the unhiding of [unhide] in one database transaction: all or nothing (open item
     * 14; 4.4; 15 decision 2).
     */
    @Transaction
    suspend fun insertBuy(
        transaction: TransactionRow,
        cashLine: CashEntryRow,
        unhide: List<HiddenHoldingRow>,
    ) {
        insertTransaction(transaction)
        insertCashEntry(cashLine)
        unhideAll(unhide)
    }

    /**
     * A reversal of a buy or sell, its trade cash line and the unhiding of [unhide] in one database transaction: all or
     * nothing (4.4, 4.6; 15 decision 2).
     */
    @Transaction
    suspend fun insertReversal(
        transaction: TransactionRow,
        cashLine: CashEntryRow,
        unhide: List<HiddenHoldingRow>,
    ) {
        insertTransaction(transaction)
        insertCashEntry(cashLine)
        unhideAll(unhide)
    }

    /** The default conflict rule (abort) is kept. */
    @Insert
    suspend fun insertAllocations(rows: List<SellAllocationRow>)

    /** A sell, its trade cash line and its allocations in one database transaction: all or nothing (open item 14). */
    @Transaction
    suspend fun insertSell(
        transaction: TransactionRow,
        cashLine: CashEntryRow,
        allocations: List<SellAllocationRow>,
    ) {
        insertTransaction(transaction)
        insertCashEntry(cashLine)
        insertAllocations(allocations)
    }

    /**
     * A Reverse and re-enter pair in one database transaction: the reversal and its trade cash line, then the new buy
     * or sell, its trade cash line and a new sell's allocations, and the unhiding of [unhide]; all or nothing (4.4,
     * 4.6, K2; 15 decision 2).
     */
    @Transaction
    suspend fun insertPair(
        reversal: TransactionRow,
        reversalCashLine: CashEntryRow,
        replacement: TransactionRow,
        replacementCashLine: CashEntryRow,
        allocations: List<SellAllocationRow>,
        unhide: List<HiddenHoldingRow>,
    ) {
        insertTransaction(reversal)
        insertCashEntry(reversalCashLine)
        insertTransaction(replacement)
        insertCashEntry(replacementCashLine)
        if (allocations.isNotEmpty()) insertAllocations(allocations)
        unhideAll(unhide)
    }

    /** A split's Reverse and re-enter pair in one database transaction: the reversal and the new split; both or neither (14b). */
    @Transaction
    suspend fun insertSplitPair(
        reversal: TransactionRow,
        replacement: TransactionRow,
    ) {
        insertTransaction(reversal)
        insertTransaction(replacement)
    }

    // 29: a portfolio's deletion (4.7), by the ids K1 decided (PortfolioRemoval). Each returns the rows removed.
    @Query("DELETE FROM sell_allocations WHERE sellId IN (:sellIds)")
    suspend fun deleteAllocationsOf(sellIds: List<String>): Int

    @Query("DELETE FROM cash_entries WHERE id IN (:ids)")
    suspend fun deleteCashEntries(ids: List<String>): Int

    @Query("DELETE FROM transactions WHERE id IN (:ids)")
    suspend fun deleteTransactions(ids: List<String>): Int

    @Query("DELETE FROM hidden_holdings WHERE portfolioId = :portfolioId")
    suspend fun deleteHiddenOf(portfolioId: String): Int

    @Query("DELETE FROM class_overrides WHERE portfolioId = :portfolioId")
    suspend fun deleteOverridesOfPortfolio(portfolioId: String): Int

    @Query("DELETE FROM portfolios WHERE id = :portfolioId")
    suspend fun deletePortfolioRow(portfolioId: String): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE portfolioId = :portfolioId")
    suspend fun countTransactionsOf(portfolioId: String): Int

    // 29 step 7 (the review's finding 2): what still names a deleted transaction, after the deletes.
    @Query("SELECT COUNT(*) FROM cash_entries WHERE type = 'TRADE' AND transactionId IN (:ids)")
    suspend fun countTradeLinesOf(ids: List<String>): Int

    @Query("SELECT COUNT(*) FROM sell_allocations WHERE sellId IN (:ids)")
    suspend fun countAllocationsOfSells(ids: List<String>): Int

    @Query("SELECT COUNT(*) FROM sell_allocations WHERE lotId IN (:ids)")
    suspend fun countAllocationsOfLots(ids: List<String>): Int

    /**
     * A portfolio's deletion in one database transaction (4.7, K2; 29): its sells' allocations, its trade cash lines,
     * its transactions, its own hidden rows and overrides (decision 1), then the portfolio. Every count must be the one
     * K1 expected and no transaction of the portfolio may be left, and (step 7) no trade cash line or allocation may still
     * name a deleted transaction, as its sell or as its lot; otherwise it throws and nothing is removed. The messages name
     * counts only, never a portfolio name (spec section 9).
     */
    @Transaction
    suspend fun deletePortfolio(
        portfolioId: String,
        transactionIds: List<String>,
        cashEntryIds: List<String>,
        hiddenCount: Int,
        overrideCount: Int,
    ) {
        for (chunk in transactionIds.chunked(DELETE_CHUNK)) deleteAllocationsOf(chunk)
        val lines = cashEntryIds.chunked(DELETE_CHUNK).sumOf { deleteCashEntries(it) }
        check(lines == cashEntryIds.size) { "A deletion removed $lines cash lines instead of ${cashEntryIds.size}." }
        val trades = transactionIds.chunked(DELETE_CHUNK).sumOf { deleteTransactions(it) }
        check(trades == transactionIds.size) { "A deletion removed $trades transactions instead of ${transactionIds.size}." }
        check(countTransactionsOf(portfolioId) == 0) { "A deletion left transactions of the portfolio." }
        val chunks = transactionIds.chunked(DELETE_CHUNK)
        val linesLeft = chunks.sumOf { countTradeLinesOf(it) }
        check(linesLeft == 0) { "A deletion left $linesLeft trade cash lines of deleted transactions." }
        val allocationsLeft = chunks.sumOf { countAllocationsOfSells(it) + countAllocationsOfLots(it) }
        check(allocationsLeft == 0) { "A deletion left $allocationsLeft allocations naming deleted transactions." }
        val hidden = deleteHiddenOf(portfolioId)
        check(hidden == hiddenCount) { "A deletion removed $hidden hidden rows instead of $hiddenCount." }
        val overrides = deleteOverridesOfPortfolio(portfolioId)
        check(overrides == overrideCount) { "A deletion removed $overrides overrides instead of $overrideCount." }
        val portfolios = deletePortfolioRow(portfolioId)
        check(portfolios == 1) { "A deletion removed $portfolios portfolios instead of 1." }
    }

    @Query("SELECT * FROM transactions ORDER BY date, enteredSeq")
    suspend fun loadAll(): List<TransactionRow>

    @Query("SELECT * FROM sell_allocations")
    suspend fun loadAllocations(): List<SellAllocationRow>
}

@Dao
interface InstrumentDao {
    /** Inserts, or replaces the row with the same ticker (K4 only). */
    @Upsert
    suspend fun upsert(row: InstrumentRow)

    @Query("SELECT * FROM instruments WHERE ticker = :ticker")
    suspend fun find(ticker: String): InstrumentRow?

    /**
     * As [upsert], keeping the stored price (16a) and the source profile (23a) of the row it replaces: only a refresh sets
     * them, so a buy that saves its instrument again loses neither.
     */
    @Transaction
    suspend fun upsertKeepingPrice(row: InstrumentRow) {
        upsert(row.withStoredOf(find(row.ticker)))
    }

    @Query("UPDATE instruments SET price = :price, priceTime = :time, priceStale = 0 WHERE ticker = :ticker")
    suspend fun setPrice(
        ticker: String,
        price: String,
        time: Long,
    )

    /** A ticker without a stored price keeps none: there is nothing to mark stale (4.8). */
    @Query("UPDATE instruments SET priceStale = 1 WHERE ticker = :ticker AND price IS NOT NULL")
    suspend fun markStale(ticker: String)

    /** Sets an instrument's source profile and marks it asked (23a); returns the number of rows changed. */
    @Query("UPDATE instruments SET sourceSector = :sector, sourceGeography = :geography, profileAsked = 1 WHERE ticker = :ticker")
    suspend fun setProfile(
        ticker: String,
        sector: String?,
        geography: String?,
    ): Int

    /** Both lists, each value in the order it was added (23a); also read by RoomRecordStore.loadClassLists. */
    @Query("SELECT * FROM list_values ORDER BY seq")
    suspend fun loadListValues(): List<ListValueRow>

    @Insert
    suspend fun insertListValue(row: ListValueRow)

    /** The remembered source texts (25), in the order remembered; also read by RoomRecordStore.loadClassLists. */
    @Query("SELECT * FROM source_texts ORDER BY seq")
    suspend fun loadSourceTexts(): List<SourceTextRow>

    @Insert
    suspend fun insertSourceText(row: SourceTextRow)

    @Update
    suspend fun updateListValue(row: ListValueRow): Int

    @Query("DELETE FROM list_values WHERE seq = :seq")
    suspend fun deleteListValueRow(seq: Long): Int

    @Query("DELETE FROM source_texts WHERE valueSeq = :seq")
    suspend fun deleteSourceTextsOf(seq: Long): Int

    @Query("DELETE FROM class_overrides WHERE valueSeq = :seq")
    suspend fun deleteOverridesOf(seq: Long): Int

    /**
     * One refresh's prices and profiles in one database transaction (K4 only, 16a and 23a): each fresh price as text
     * and Unix seconds; each profile on its instrument; then the texts of those profiles and (25 decision 4) of the
     * [held] tickers' stored profiles that map to no value are added (4.9, K10). A profile for a ticker with no stored
     * instrument adds nothing.
     */
    @Transaction
    suspend fun savePrices(
        fresh: Map<String, Pair<String, Long>>,
        failed: Collection<String>,
        profiles: Map<String, SourceProfile>,
        held: Collection<String>,
    ) {
        for ((ticker, price) in fresh) setPrice(ticker, price.first, price.second)
        for (ticker in failed) markStale(ticker)
        if (profiles.isEmpty() && held.isEmpty()) return
        val saved = profiles.filter { (ticker, profile) -> setProfile(ticker, profile.sector, profile.geography) > 0 }
        val lists = loadListValues().toClassLists(loadSourceTexts())
        for ((list, value) in Classification.additions(lists, profilesToAdd(saved, loadAll(), held))) {
            insertListValue(ListValueRow(list = list.name, value = value))
        }
    }

    /** Adds [name] to [list] in one database transaction (25), by K10's rule against the stored lists; throws if refused. */
    @Transaction
    suspend fun addListValue(
        list: ClassList,
        name: String,
    ) {
        insertListValue(addedRow(loadListValues(), loadSourceTexts(), list, name))
    }

    /**
     * Renames [from] on [list] to [to] in one database transaction (25): its row changes, so its overrides and texts
     * follow by seq, and the text K10 remembers (decision 1) is added. Throws if K10 refuses it.
     */
    @Transaction
    suspend fun renameListValue(
        list: ClassList,
        from: String,
        to: String,
    ) {
        val profiles = loadAll().mapNotNull { it.toProfile() }
        val (row, text) = renamedRows(loadListValues(), loadSourceTexts(), profiles, list, from, to)
        if (updateListValue(row) != 1) throw IllegalStateException("The value was not renamed.")
        text?.let { insertSourceText(it) }
    }

    /**
     * Deletes [value] from [list] in one database transaction (25): its texts and the overrides naming it, each of them
     * one of [removed] (decision 3), then its row. Throws if another override names it.
     */
    @Transaction
    suspend fun deleteListValue(
        list: ClassList,
        value: String,
        removed: List<ClassOverride>,
    ) {
        val seq = deletedSeq(loadListValues(), loadOverrides(), list, value, removed)
        deleteOverridesOf(seq)
        deleteSourceTextsOf(seq)
        deleteListValueRow(seq)
    }

    @Query("SELECT * FROM instruments ORDER BY ticker")
    suspend fun loadAll(): List<InstrumentRow>

    // 24: the holdings' overrides, here with the list values they name, so a save reads the list in its own transaction.
    @Query("SELECT * FROM class_overrides ORDER BY portfolioId, ticker, list")
    suspend fun loadOverrides(): List<ClassOverrideRow>

    @Upsert
    suspend fun upsertOverride(row: ClassOverrideRow)

    /** Returns the number of rows removed. */
    @Query("DELETE FROM class_overrides WHERE portfolioId = :portfolioId AND ticker = :ticker AND list = :list")
    suspend fun deleteOverride(
        portfolioId: String,
        ticker: String,
        list: String,
    ): Int

    /** Sets [override] naming its list's matching value (decision 3), in one database transaction; throws if none matches. */
    @Transaction
    suspend fun saveOverride(override: ClassOverride) {
        upsertOverride(override.toRow(loadListValues()) ?: throw IllegalArgumentException("The value is not on its list."))
    }
}

@Dao
interface SettingsDao {
    @Upsert
    suspend fun upsert(row: SettingsRow)

    /** The settings row, or null when no setting was ever saved. */
    @Query("SELECT * FROM settings WHERE id = ${SettingsRow.ONLY_ID}")
    suspend fun load(): SettingsRow?

    /** Sets the auto-lock column of the settings row, making the row if there is none; the other columns are kept. */
    @Transaction
    suspend fun saveAutoLock(autoLock: String) {
        upsert(load().withAutoLock(autoLock))
    }

    /** Sets the tab order column of the settings row, making the row if there is none; the other columns are kept. */
    @Transaction
    suspend fun saveTabOrder(tabOrder: String) {
        upsert(load().withTabOrder(tabOrder))
    }
}

/**
 * Version 2 (iteration 07) adds the portfolios table, version 3 (09) the transactions and instruments tables, version 4
 * (11) the sell_allocations table, version 5 (12) the two split columns of transactions, version 6 (15) the
 * hidden_holdings table, version 7 (16a) the three price columns of instruments, version 8 (20) the settings table,
 * version 9 (22) its tab order column, version 10 (23a) the three profile columns of instruments and the list_values
 * table, version 11 (24) the class_overrides table, version 12 (25) the source_texts table; Room generates each step out
 * of the schemas in app/schemas.
 */
// 29: ids per delete query, well under SQLite's limit on bound variables.
internal const val DELETE_CHUNK = 500

@Database(
    entities = [
        CashEntryRow::class,
        PortfolioRow::class,
        TransactionRow::class,
        InstrumentRow::class,
        SellAllocationRow::class,
        HiddenHoldingRow::class,
        SettingsRow::class,
        ListValueRow::class,
        ClassOverrideRow::class,
        SourceTextRow::class,
    ],
    version = 12,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 8, to = 9),
        AutoMigration(from = 9, to = 10),
        AutoMigration(from = 10, to = 11),
        AutoMigration(from = 11, to = 12),
    ],
)
abstract class AtsDatabase : RoomDatabase() {
    abstract fun cashEntryDao(): CashEntryDao

    abstract fun portfolioDao(): PortfolioDao

    abstract fun transactionDao(): TransactionDao

    abstract fun instrumentDao(): InstrumentDao

    abstract fun settingsDao(): SettingsDao
}
