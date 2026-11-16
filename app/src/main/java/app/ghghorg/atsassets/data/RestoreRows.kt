package app.ghghorg.atsassets.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import app.ghghorg.atsassets.core.BackupRecords
import app.ghghorg.atsassets.core.ClassList
import app.ghghorg.atsassets.core.Instrument

/**
 * Every row a restore writes (section 8, restore step 7; 34): the whole store after it, nothing else (decision 4).
 */
class RestoreRows(
    val cash: List<CashEntryRow>,
    val portfolios: List<PortfolioRow>,
    val transactions: List<TransactionRow>,
    val allocations: List<SellAllocationRow>,
    val hidden: List<HiddenHoldingRow>,
    val instruments: List<InstrumentRow>,
    val listValues: List<ListValueRow>,
    val sourceTexts: List<SourceTextRow>,
    val overrides: List<ClassOverrideRow>,
    val settings: SettingsRow,
)

/**
 * The rows for [records], the file's records as K1 made them, and the held tickers' verified [instruments]. The list
 * values get their own row numbers, sectors first, each list in its order; each remembered text and override names the
 * row of its value (24 decision 3; 25). The settings are the file's lock choice and tab order, with no price key
 * (decision 1) and no backup moment or mark (section 8: "never backed up" after a restore). An instrument keeps its
 * ticker and type only: no price, no profile ("no price" until the first refresh). Throws for a text or an override
 * naming no value of its list; the check refuses such a file before this. The messages name no value (spec section 9).
 */
internal fun restoreRows(
    records: BackupRecords,
    instruments: List<Instrument>,
): RestoreRows {
    val values =
        ClassList.entries.flatMap { list -> records.lists[list].map { list to it } }.mapIndexed { index, (list, value) ->
            ListValueRow(seq = index + 1L, list = list.name, value = value)
        }
    val texts =
        records.lists.remembered.mapIndexed { index, text ->
            val value = values.firstOrNull { it.list == text.list.name && it.value == text.value }
            requireNotNull(value) { "A remembered text names no value of its list." }
            SourceTextRow(seq = index + 1L, list = text.list.name, valueSeq = value.seq, text = text.text)
        }
    val overrides = records.overrides.map { requireNotNull(it.toRow(values)) { "An override names no value of its list." } }
    return RestoreRows(
        cash = records.cashEntries.map { it.toRow() },
        portfolios = records.portfolios.map { it.toRow() },
        transactions = records.transactions.map { it.toRow() },
        allocations = records.transactions.flatMap { it.allocationRows() },
        hidden = records.hidden.map { it.toRow() },
        instruments = instruments.map { InstrumentRow(ticker = it.ticker, assetType = it.assetType.name, verified = true) },
        listValues = values,
        sourceTexts = texts,
        overrides = overrides,
        settings = SettingsRow(autoLock = records.autoLock.name, tabOrder = records.tabOrder.text),
    )
}

/** The restore's delete and load (K2; 34): every table cleared and the file's rows written, in one transaction. */
@Dao
interface RestoreDao {
    @Query("DELETE FROM cash_entries")
    suspend fun clearCashEntries()

    @Query("DELETE FROM portfolios")
    suspend fun clearPortfolios()

    @Query("DELETE FROM transactions")
    suspend fun clearTransactions()

    @Query("DELETE FROM sell_allocations")
    suspend fun clearAllocations()

    @Query("DELETE FROM hidden_holdings")
    suspend fun clearHidden()

    @Query("DELETE FROM instruments")
    suspend fun clearInstruments()

    @Query("DELETE FROM list_values")
    suspend fun clearListValues()

    @Query("DELETE FROM source_texts")
    suspend fun clearSourceTexts()

    @Query("DELETE FROM class_overrides")
    suspend fun clearOverrides()

    @Query("DELETE FROM settings")
    suspend fun clearSettings()

    @Insert
    suspend fun insertCashEntries(rows: List<CashEntryRow>)

    @Insert
    suspend fun insertPortfolios(rows: List<PortfolioRow>)

    @Insert
    suspend fun insertTransactions(rows: List<TransactionRow>)

    @Insert
    suspend fun insertAllocations(rows: List<SellAllocationRow>)

    @Insert
    suspend fun insertHidden(rows: List<HiddenHoldingRow>)

    @Insert
    suspend fun insertInstruments(rows: List<InstrumentRow>)

    @Insert
    suspend fun insertListValues(rows: List<ListValueRow>)

    @Insert
    suspend fun insertSourceTexts(rows: List<SourceTextRow>)

    @Insert
    suspend fun insertOverrides(rows: List<ClassOverrideRow>)

    @Insert
    suspend fun insertSettings(row: SettingsRow)

    /**
     * All or nothing (C3): a failure anywhere, such as two rows with one id, leaves every table as it was. Nothing of the
     * rows is logged or put in a message (spec section 9).
     */
    @Transaction
    suspend fun replaceAll(rows: RestoreRows) {
        clearAllocations()
        clearCashEntries()
        clearTransactions()
        clearHidden()
        clearOverrides()
        clearSourceTexts()
        clearListValues()
        clearInstruments()
        clearPortfolios()
        clearSettings()
        insertPortfolios(rows.portfolios)
        insertTransactions(rows.transactions)
        insertAllocations(rows.allocations)
        insertCashEntries(rows.cash)
        insertHidden(rows.hidden)
        insertInstruments(rows.instruments)
        insertListValues(rows.listValues)
        insertSourceTexts(rows.sourceTexts)
        insertOverrides(rows.overrides)
        insertSettings(rows.settings)
    }
}
