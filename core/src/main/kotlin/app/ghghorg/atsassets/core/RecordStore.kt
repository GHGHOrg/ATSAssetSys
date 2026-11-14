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

    /**
     * Deletes a portfolio as one all-or-nothing action (4.7; 29): what K1's [removal] names, the portfolio's sells'
     * allocations with them. Throws if any count differs from the removal's, or if a transaction of the portfolio is
     * left; nothing is removed then.
     */
    suspend fun deletePortfolio(removal: PortfolioRemoval)

    /** Each sell with its allocations. Throws [UnreadableDataException] if stored data exists but cannot be read. */
    suspend fun loadTransactions(): List<Transaction>

    /**
     * Saves a buy and its trade cash line together: both or neither (open item 14). The hidden holdings it gives shares
     * again, [unhide], are unhidden in the same action (4.4; 15 decision 2).
     */
    suspend fun saveBuy(
        transaction: Transaction,
        cashLine: CashEntry,
        unhide: List<HiddenHolding> = emptyList(),
    )

    /** Saves a sell, its trade cash line and its allocations together: all or nothing (open item 14). */
    suspend fun saveSell(
        transaction: Transaction,
        cashLine: CashEntry,
    )

    /** Saves a split with its ratio; it has no cash line (4.5). */
    suspend fun saveSplit(transaction: Transaction)

    /**
     * Saves a reversal and, for a buy's or sell's reversal, its trade cash line together: both or neither (4.6). A sell's
     * reversal unhides [unhide] in the same action (4.4; 15 decision 2); a split's reversal never unhides.
     */
    suspend fun saveReversal(
        transaction: Transaction,
        cashLine: CashEntry?,
        unhide: List<HiddenHolding> = emptyList(),
    )

    /**
     * Saves a Reverse and re-enter pair together: the reversal, its trade cash line, the new buy or sell, its trade cash
     * line and a new sell's allocations, and the unhiding of [unhide]; all or nothing (4.4, 4.6, K2; 15 decision 2).
     */
    suspend fun savePair(
        reversal: Transaction,
        reversalCashLine: CashEntry,
        replacement: Transaction,
        replacementCashLine: CashEntry,
        unhide: List<HiddenHolding> = emptyList(),
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

    /** Saves an instrument, replacing the one with the same ticker but keeping its stored price (K4 only). */
    suspend fun saveInstrument(instrument: Instrument)

    /**
     * After a price refresh (K4 only, 16a), as one all-or-nothing batch: each of [fresh] becomes its ticker's latest
     * price, not stale; the stored price of each of [failed] is kept and marked stale (4.8). A ticker that never had a
     * price keeps none, and a ticker with no stored instrument is skipped. From 23a, each of [profiles] becomes its
     * instrument's source profile, marking it asked, and each of its value texts that maps to no value is added (4.9).
     * From 25 (decision 4), so is each such text of the [held] tickers' stored profiles.
     */
    suspend fun savePrices(
        fresh: Map<String, Price>,
        failed: Collection<String>,
        profiles: Map<String, SourceProfile> = emptyMap(),
        held: Collection<String> = emptyList(),
    )

    /** The hidden holdings, each at 0 shares. Throws [UnreadableDataException] if stored data cannot be read or checked. */
    suspend fun loadHidden(): List<HiddenHolding>

    /**
     * As [loadHidden], checked against [transactions] the caller has just loaded, so a store need not read them again
     * (iteration 15c). By default it is [loadHidden].
     */
    suspend fun loadHidden(transactions: List<Transaction>): List<HiddenHolding> = loadHidden()

    /** Hides [holding] (4.4). Not a transaction; no cash effect. Throws if it is already hidden. */
    suspend fun hide(holding: HiddenHolding)

    /** Unhides [holding] (view 9). Throws if it was not hidden. */
    suspend fun unhide(holding: HiddenHolding)

    /**
     * The auto-lock choice (spec section 9), [AutoLock.EVERY_LEAVE] when none was saved. Throws
     * [UnreadableDataException] if stored data exists but cannot be read.
     */
    suspend fun loadAutoLock(): AutoLock

    /** Saves the auto-lock choice in place of the one before; the other settings are kept (20 decision 3). */
    suspend fun saveAutoLock(choice: AutoLock)

    /**
     * The bottom bar's order (spec 6, a setting), [TabOrder.DEFAULT] when none was saved. Throws
     * [UnreadableDataException] if a stored order exists but is not each tab once (22).
     */
    suspend fun loadTabOrder(): TabOrder

    /** Saves the tab order in place of the one before; the other settings are kept (22). */
    suspend fun saveTabOrder(order: TabOrder)

    /**
     * The sector and geography lists (spec 3, 4.9; 23a), each in the order its values were added, with their remembered
     * texts (25); empty lists when none was saved. Throws [UnreadableDataException] for an unknown list, two values in a
     * list that match (D7), or a remembered text naming no value or matching a value or another text of its list.
     */
    suspend fun loadClassLists(): ClassLists

    /** Adds [name] to [list] by K10's rule (4.9; 25), against the stored lists. Throws if K10 refuses it; nothing is saved then. */
    suspend fun addListValue(
        list: ClassList,
        name: String,
    )

    /**
     * Renames [from] on [list] to [to] by K10's rule (4.9; 25, decisions 1 and 2), against the stored lists and every
     * stored instrument's source texts: its overrides and remembered texts follow, and the text K10 remembers is added.
     * Throws if K10 refuses it; nothing is saved then.
     */
    suspend fun renameListValue(
        list: ClassList,
        from: String,
        to: String,
    )

    /**
     * Deletes [value] from [list] with its remembered texts and the overrides in [removed] (4.9; 25 decision 3: K10's
     * [ListResult.Deleted.overridesRemoved]). Throws if another override names it; nothing is saved then.
     */
    suspend fun deleteListValue(
        list: ClassList,
        value: String,
        removed: List<ClassOverride>,
    )

    /**
     * Every holding's override (4.9; 24), each value as its list spells it. Throws [UnreadableDataException] for an
     * override naming no value of its list (decision 3) or an unknown list.
     */
    suspend fun loadOverrides(): List<ClassOverride>

    /**
     * Sets [override] in place of the holding's override on that list, naming the list's value that matches (D7). Throws
     * when no value of the list matches; nothing is saved then.
     */
    suspend fun saveOverride(override: ClassOverride)

    /** Clears the holding's override on [list], if it has one (4.9; 24). */
    suspend fun clearOverride(
        portfolioId: String,
        ticker: String,
        list: ClassList,
    )
}

class UnreadableDataException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
