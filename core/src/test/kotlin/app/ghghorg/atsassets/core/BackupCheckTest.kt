package app.ghghorg.atsassets.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate

/**
 * K6's reading half, step 4 (spec section 8, restore steps 2 and 5; 4.1 to 4.6; C7; 33a decisions 1 to 3): the replay
 * through K1, the rules between rows, the dependent errors, and what a clean file gives.
 */
class BackupCheckTest {
    private val header = BackupFile.COLUMNS.joinToString(",")

    private fun row(vararg cells: Pair<String, String>): String {
        val values = cells.toMap()
        return BackupFile.COLUMNS.joinToString(",") { values[it] ?: "" }
    }

    private fun context(): RestoreContext {
        var made = 0
        return RestoreContext(
            newId = { "app-${++made}" },
            priceSources = setOf("finnhub"),
            defaultSource = "finnhub",
            reversalNote = { type, date -> "Reversal of the ${type.name.lowercase()} of $date ET" },
        )
    }

    private fun check(bytes: ByteArray): RestoreCheck = BackupCheck.check(bytes, context())

    /** The header, the `format` row (line 2) and [lines] from line 3 on. */
    private fun check(vararg lines: String): RestoreCheck = check(("$header\n" + row("row_type" to "format", "value" to "1") + "\n" + lines.joinToString("\n")).toByteArray())

    private fun errors(vararg lines: String): List<RestoreError> = (check(*lines) as RestoreCheck.Failed).errors

    private fun passed(vararg lines: String): RestoreCheck.Passed = check(*lines).let { it as? RestoreCheck.Passed ?: error("not passed: $it") }

    // Building blocks: line 3 is the opening balance and line 4 the portfolio Main, unless a test says otherwise.
    private val opening = cash("opening_balance", "2026-01-01", "10000.00")
    private val main = portfolio("Main")

    private fun portfolio(name: String): String = row("row_type" to "portfolio", "portfolio" to name)

    private fun cash(
        type: String,
        date: String,
        amount: String,
        id: String = "",
        note: String = "",
    ): String = row("row_type" to "cash", "id" to id, "date" to date, "cash_type" to type, "amount" to amount, "note" to note)

    private fun buy(
        id: String,
        date: String,
        quantity: String,
        amount: String,
        ticker: String = "AAPL",
        portfolio: String = "Main",
    ): String = row(
        "row_type" to "buy",
        "id" to id,
        "portfolio" to portfolio,
        "date" to date,
        "ticker" to ticker,
        "quantity" to quantity,
        "amount" to amount,
    )

    private fun sell(
        date: String,
        quantity: String,
        amount: String,
        picks: String,
        ticker: String = "AAPL",
        portfolio: String = "Main",
        id: String = "",
    ): String = row(
        "row_type" to "sell",
        "id" to id,
        "portfolio" to portfolio,
        "date" to date,
        "ticker" to ticker,
        "quantity" to quantity,
        "amount" to amount,
        "lot_picks" to picks,
    )

    private fun split(
        date: String,
        to: String,
        from: String,
        ticker: String = "AAPL",
        id: String = "",
    ): String = row("row_type" to "split", "id" to id, "portfolio" to "Main", "date" to date, "ticker" to ticker, "split_to" to to, "split_from" to from)

    private fun reversal(
        reverses: String,
        date: String,
        note: String = "",
    ): String = row("row_type" to "reversal", "reverses" to reverses, "date" to date, "note" to note)

    private fun hidden(ticker: String): String = row("row_type" to "hidden", "portfolio" to "Main", "ticker" to ticker)

    private fun override(
        ticker: String,
        sector: String = "",
        geography: String = "",
    ): String = row("row_type" to "override", "portfolio" to "Main", "ticker" to ticker, "sector" to sector, "geography" to geography)

    private fun listValue(
        list: String,
        value: String,
    ): String = row("row_type" to "list_value", "list" to list, "value" to value)

    private fun sourceText(
        list: String,
        value: String,
        text: String,
    ): String = row("row_type" to "source_text", "list" to list, "value" to value, "source_text" to text)

    private fun setting(
        key: String,
        value: String,
    ): String = row("row_type" to "setting", "key" to key, "value" to value)

    private fun error(
        line: Int?,
        column: String?,
        reason: RestoreReason,
    ): RestoreError = RestoreError(line, column, reason)

    private fun dollars(cents: Long): Fraction = Fraction.ofCents(cents)

    // spec/example-backup.csv against spec/example-backup-expected.md (section 8, step 5; AC-25's reading part).
    @Test
    fun theSampleFile_passes_withTheExpectedFigures() {
        val passed = check(File("../spec/example-backup.csv").readBytes()) as RestoreCheck.Passed
        assertEquals(RestoreSummary(portfolios = 2, transactions = 8, cashEntries = 4, cashBalanceCents = 942_500L, holdingsWithShares = 1), passed.summary)
        assertEquals(listOf("AAPL"), passed.heldTickers)
        val records = passed.records
        assertEquals(listOf("Main", "Retirement"), records.portfolios.map { it.name })
        // 7 trade cash lines rebuilt: 4 buys, 2 sells, the reversal of a buy.
        assertEquals(7, records.cashEntries.count { it.type == CashType.TRADE })
        val holdings = Holdings.holdings(records.transactions, LocalDate.of(2026, 6, 1))
        val aapl = holdings.single { it.ticker == "AAPL" }
        assertEquals(Fraction.of(BigDecimal("16")), aapl.shares)
        assertEquals(dollars(120_000L), aapl.cost)
        assertEquals(Fraction.ZERO, holdings.single { it.ticker == "OLDCO" }.shares)
        assertEquals(dollars(60_000L), RealizedGains.report(records.transactions, records.portfolios, null, null).total)
        assertEquals(dollars(80_000L), RealizedGains.report(records.transactions, records.portfolios, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 1)).total)
        val mainId = records.portfolios.first().id
        assertEquals(listOf(HiddenHolding(mainId, "OLDCO")), records.hidden)
        assertEquals(
            listOf(ClassOverride(mainId, "AAPL", ClassList.SECTOR, "Technology"), ClassOverride(mainId, "AAPL", ClassList.GEOGRAPHY, "United States")),
            records.overrides,
        )
        assertEquals(
            ClassLists(
                sectors = listOf("Technology", "Diversified"),
                geographies = listOf("United States", "Global"),
                remembered = listOf(RememberedText(ClassList.SECTOR, "Information Technology", "Technology")),
            ),
            records.lists,
        )
        assertEquals(AutoLock.IDLE_5_MIN, records.autoLock)
        assertEquals(TabOrder.parse("overall;portfolios;cash;more"), records.tabOrder)
        // No price_source row: the app's default.
        assertEquals("finnhub", records.priceSource)
        // The reversal of D1 has no note in the file: it gets the note of 4.6.
        val reversalOfDeposit = records.cashEntries.single { it.reversesId != null }
        assertEquals("Reversal of the deposit of 2026-04-20 ET", reversalOfDeposit.note)
        assertEquals(-50_000L, reversalOfDeposit.amountCents)
        // The app's own ids, never the file's (section 8).
        assertTrue(records.transactions.none { it.id in setOf("L1", "L2", "L3", "O1") })
    }

    @Test
    fun theSampleFile_withAByteOrderMarkAndCrlf_givesTheSame() {
        val bytes = File("../spec/example-backup.csv").readBytes()
        val plain = (check(bytes) as RestoreCheck.Passed).summary
        val text = String(bytes, Charsets.UTF_8).replace("\r\n", "\n").replace("\n", "\r\n")
        val marked = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + text.toByteArray(Charsets.UTF_8)
        assertEquals(plain, (check(marked) as RestoreCheck.Passed).summary)
    }

    // The export (31) and the check read one format: a file the app wrote checks clean and gives the same records back.
    @Test
    fun aFileTheAppWrote_checksClean_andGivesItsRecordsBack() {
        val first = (check(File("../spec/example-backup.csv").readBytes()) as RestoreCheck.Passed).records
        val written = (BackupFile.write(first) as BackupResult.Written).text
        val second = check(written.toByteArray()) as RestoreCheck.Passed
        assertEquals(
            first.transactions.map { listOf(it.type, it.date, it.ticker, it.quantity, it.totalCents, it.split, it.quantityRatio) },
            second.records.transactions.map { listOf(it.type, it.date, it.ticker, it.quantity, it.totalCents, it.split, it.quantityRatio) },
        )
        assertEquals(first.cashEntries.map { Triple(it.date, it.amountCents, it.note) }, second.records.cashEntries.map { Triple(it.date, it.amountCents, it.note) })
        // The export writes list values alphabetically (31); the lists hold the same values and texts.
        assertEquals(first.lists.sectors.sorted(), second.records.lists.sectors.sorted())
        assertEquals(first.lists.geographies.sorted(), second.records.lists.geographies.sorted())
        assertEquals(first.lists.remembered.toSet(), second.records.lists.remembered.toSet())
        assertEquals(first.hidden.size, second.records.hidden.size)
        assertEquals(first.overrides.map { it.list to it.value }, second.records.overrides.map { it.list to it.value })
        assertEquals("finnhub", second.records.priceSource)
    }

    // AC-59 (a) and (c) through a file: a 1 for 3 split leaves 3.33333333 shown, and that pick takes the exact remainder.
    @Test
    fun aPickAsTheLotPickerShowsIt_takesTheExactRemainder() {
        val passed =
            passed(
                opening,
                main,
                buy("L1", "2026-01-02", "10", "1000.00"),
                split("2026-01-03", "1", "3"),
                sell("2026-01-04", "3.33333333", "1200.00", "L1:3.33333333"),
            )
        assertEquals(0, passed.summary.holdingsWithShares)
        assertEquals(emptyList<String>(), passed.heldTickers)
        assertEquals(Fraction.ZERO, Holdings.holdings(passed.records.transactions, LocalDate.of(2026, 6, 1)).single().shares)
    }

    // Ids: one id space, compared ignoring case; a duplicate is refused (section 8).
    @Test
    fun aDuplicateId_ignoringCase() {
        val errors = errors(opening, main, buy("L1", "2026-01-02", "1", "10.00"), cash("deposit", "2026-01-03", "5.00", id = "l1"))
        assertEquals(listOf(error(6, "id", RestoreReason.IdDuplicate(5))), errors)
    }

    // Portfolios: a duplicate name (4.7, D7: case and spaces ignored); a portfolio not found.
    @Test
    fun aDuplicatePortfolioName_andAPortfolioNotFound() {
        assertEquals(listOf(error(5, "portfolio", RestoreReason.PortfolioNameDuplicate(4))), errors(opening, main, portfolio(" MAIN ")))
        assertEquals(listOf(error(5, "portfolio", RestoreReason.PortfolioNotFound)), errors(opening, main, buy("L1", "2026-01-02", "1", "10.00", portfolio = "Other")))
        // Named in any case and spacing.
        assertEquals(1, passed(opening, main, buy("L1", "2026-01-02", "1", "10.00", portfolio = "main")).summary.transactions)
    }

    // Cash: no opening balance (the file as a whole: no line), more than one, or one that is not the earliest cash entry.
    @Test
    fun noOpeningBalance() {
        assertEquals(listOf(error(null, null, RestoreReason.NoOpeningBalance)), errors(main, cash("deposit", "2026-01-02", "5.00"), buy("L1", "2026-01-02", "1", "1.00")))
        assertEquals(listOf(error(null, null, RestoreReason.NoOpeningBalance)), errors(main))
    }

    @Test
    fun aSecondOpeningBalance() {
        assertEquals(listOf(error(5, "cash_type", RestoreReason.SecondOpeningBalance(3))), errors(opening, main, cash("opening_balance", "2026-02-01", "5.00")))
    }

    @Test
    fun anOpeningBalanceThatIsNotTheEarliestCashEntry() {
        val errors = errors(cash("deposit", "2025-12-31", "5.00"), main, opening)
        assertTrue(errors.toString(), error(5, "date", RestoreReason.OpeningBalanceNotEarliest) in errors)
        assertTrue(errors.toString(), error(3, "date", RestoreReason.DatedBeforeOpeningBalance(LocalDate.of(2026, 1, 1))) in errors)
        // Same date but listed first: not the earliest either.
        assertTrue(error(5, "date", RestoreReason.OpeningBalanceNotEarliest) in errors(cash("deposit", "2026-01-01", "5.00"), main, opening))
    }

    // Rules (4.1, 4.2): cash would go negative, with the date and the shortfall; a record dated before the opening balance.
    @Test
    fun cashWouldGoNegative_withTheDateAndTheShortfall() {
        assertEquals(
            listOf(error(5, null, RestoreReason.CashNegative(LocalDate.of(2026, 2, 10), 20_000L))),
            errors(opening, main, cash("withdrawal", "2026-02-10", "-10200.00")),
        )
        // A later entry is replayed after, so the error is on the row that makes it negative.
        assertEquals(
            listOf(error(6, null, RestoreReason.CashNegative(LocalDate.of(2026, 3, 1), 50_000L))),
            errors(opening, main, buy("L1", "2026-02-01", "1", "9000.00"), buy("L2", "2026-03-01", "1", "1500.00")),
        )
    }

    @Test
    fun aRecordDatedBeforeTheOpeningBalance() {
        assertEquals(
            listOf(error(5, "date", RestoreReason.DatedBeforeOpeningBalance(LocalDate.of(2026, 1, 1)))),
            errors(opening, main, buy("L1", "2025-12-01", "1", "10.00")),
        )
    }

    // 33a decision 3: a balance beyond exact cents.
    @Test
    fun aBalanceBeyondExactCents() {
        val errors = errors(cash("opening_balance", "2026-01-01", "92233720368547758.07"), main, cash("deposit", "2026-01-02", "0.01"))
        assertEquals(listOf(error(5, null, RestoreReason.BalanceTooLarge)), errors)
    }

    // Sells and picks (section 8; 4.4): a pick of an id not found, not a buy or in another portfolio; and two that
    // section 8 does not name but 4.4's lot rules refuse: a lot of another ticker, and a lot picked twice.
    @Test
    fun aPickOfAnIdNotFound_notABuy_orInAnotherPortfolio() {
        fun firstPickError(vararg extra: String, picks: String): List<RestoreError> = errors(
            opening,
            main,
            portfolio("Second"),
            buy("L1", "2026-01-02", "10", "100.00"),
            buy("P1", "2026-01-02", "10", "100.00", portfolio = "Second"),
            buy("M1", "2026-01-02", "10", "100.00", ticker = "MSFT"),
            cash("deposit", "2026-01-02", "5.00", id = "C1"),
            *extra,
            sell("2026-02-01", "1", "10.00", picks),
        )
        assertEquals(listOf(error(10, "lot_picks", RestoreReason.PickNotFound("X9"))), firstPickError(picks = "X9:1"))
        assertEquals(listOf(error(10, "lot_picks", RestoreReason.PickNotABuy("C1"))), firstPickError(picks = "C1:1"))
        assertEquals(listOf(error(10, "lot_picks", RestoreReason.PickOtherPortfolio("P1"))), firstPickError(picks = "P1:1"))
        assertEquals(listOf(error(10, "lot_picks", RestoreReason.PickOtherTicker("M1"))), firstPickError(picks = "M1:1"))
        assertEquals(listOf(error(10, "lot_picks", RestoreReason.PickTwice("l1"))), firstPickError(picks = "L1:0.5;l1:0.5"))
    }

    // 4.4's lot rules: a lot bought after the sell date, or entered after the sell on the same date.
    @Test
    fun aLotBoughtAfterTheSellDate_orEnteredAfterTheSellOnTheSameDate() {
        assertEquals(
            listOf(error(5, "lot_picks", RestoreReason.LotBoughtAfterSell("L1"))),
            errors(opening, main, sell("2026-01-02", "1", "10.00", "L1:1"), buy("L1", "2026-01-03", "1", "10.00")),
        )
        assertEquals(
            listOf(error(5, "lot_picks", RestoreReason.LotEnteredAfterSell("L1"))),
            errors(opening, main, sell("2026-01-03", "1", "10.00", "L1:1"), buy("L1", "2026-01-03", "1", "10.00")),
        )
    }

    // A pick of more than the lot holds (compared to 8 places); picks that do not add up; a sell of more than is held.
    @Test
    fun aPickOfMoreThanTheLotHolds_picksThatDoNotAddUp_andASellOfMoreThanIsHeld() {
        val two = arrayOf(opening, main, buy("L1", "2026-01-02", "10", "100.00"), buy("L2", "2026-01-02", "10", "100.00"))
        assertEquals(
            listOf(error(7, "lot_picks", RestoreReason.PickOverLot("L1", BigDecimal("10")))),
            errors(*two, sell("2026-02-01", "11", "110.00", "L1:11")),
        )
        assertEquals(
            listOf(error(7, "lot_picks", RestoreReason.PicksDoNotAddUp(BigDecimal("5"), BigDecimal("6")))),
            errors(*two, sell("2026-02-01", "6", "60.00", "L1:2;L2:3")),
        )
        assertEquals(
            listOf(error(7, "quantity", RestoreReason.SellMoreThanHeld(LocalDate.of(2026, 2, 1), BigDecimal("20")))),
            errors(*two, sell("2026-02-01", "21", "60.00", "L1:10;L2:11")),
        )
    }

    // Rules: shares would go negative (4.1), here a split after a sell that took all of a lot, then reversed (4.6).
    @Test
    fun sharesWouldGoNegative() {
        val errors =
            errors(
                opening,
                main,
                buy("L1", "2026-01-02", "10", "100.00"),
                sell("2026-01-03", "10", "100.00", "L1:10"),
                reversal("L1", "2026-01-04"),
            )
        assertEquals(listOf(error(7, null, RestoreReason.SharesNegative(LocalDate.of(2026, 1, 4)))), errors)
    }

    // A split of a ticker with no buy before it in that portfolio: 4.5's rule, not one of section 8's reasons.
    @Test
    fun aSplitOfATickerWithNoBuy() {
        assertEquals(listOf(error(5, "ticker", RestoreReason.SplitTickerNotBought)), errors(opening, main, split("2026-01-02", "2", "1")))
    }

    // Reversals: the target not found, the opening balance, listed after the reversal, already reversed; a reversal dated
    // before its original.
    @Test
    fun aReversalsTarget() {
        val buy = buy("L1", "2026-01-02", "1", "10.00")
        assertEquals(listOf(error(5, "reverses", RestoreReason.ReversalTargetNotFound("X1"))), errors(opening, main, reversal("X1", "2026-01-02")))
        assertEquals(
            listOf(error(5, "reverses", RestoreReason.ReversalOfOpeningBalance)),
            errors(cash("opening_balance", "2026-01-01", "100.00", id = "OB"), main, reversal("OB", "2026-01-02")),
        )
        assertEquals(listOf(error(5, "reverses", RestoreReason.ReversalTargetListedAfter(6))), errors(opening, main, reversal("L1", "2026-01-03"), buy))
        assertEquals(
            listOf(error(7, "reverses", RestoreReason.AlreadyReversed(6))),
            errors(opening, main, buy, reversal("L1", "2026-01-03"), reversal("L1", "2026-01-04")),
        )
        assertEquals(listOf(error(6, "date", RestoreReason.ReversalBeforeOriginal(LocalDate.of(2026, 1, 2)))), errors(opening, main, buy, reversal("L1", "2026-01-01")))
    }

    // Only a cash reversal may have a note (section 8); without one it gets the note of 4.6.
    @Test
    fun aNoteOnlyOnACashReversal() {
        assertEquals(listOf(error(6, "note", RestoreReason.CellNotUsed)), errors(opening, main, buy("L1", "2026-01-02", "1", "10.00"), reversal("L1", "2026-01-02", "Oops")))
        val passed = passed(opening, main, cash("deposit", "2026-01-02", "5.00", id = "D1"), reversal("D1", "2026-01-03", "Typo"))
        assertEquals("Typo", passed.records.cashEntries.single { it.reversesId != null }.note)
    }

    // State rows: a hidden holding not found or with shares left; a duplicate hidden row.
    @Test
    fun aHiddenHoldingNotFound_withSharesLeft_orTwice() {
        val sold = arrayOf(opening, main, buy("L1", "2026-01-02", "1", "10.00"), sell("2026-01-03", "1", "10.00", "L1:1"))
        assertEquals(listOf(error(7, "ticker", RestoreReason.HiddenHoldingNotFound)), errors(*sold, hidden("MSFT")))
        assertEquals(listOf(error(5, "ticker", RestoreReason.HiddenHoldingHasShares)), errors(opening, main, hidden("AAPL"), buy("L1", "2026-01-02", "1", "10.00")))
        assertEquals(listOf(error(8, "ticker", RestoreReason.DuplicateHidden(7))), errors(*sold, hidden("AAPL"), hidden("aapl")))
        assertEquals(1, passed(*sold, hidden("AAPL")).records.hidden.size)
    }

    // An override for a holding not found, with a value not on its list, or twice.
    @Test
    fun anOverrideForAHoldingNotFound_aValueNotOnItsList_orTwice() {
        val held = arrayOf(opening, main, buy("L1", "2026-01-02", "1", "10.00"), listValue("sector", "Technology"))
        assertEquals(listOf(error(7, "ticker", RestoreReason.OverrideHoldingNotFound)), errors(*held, override("MSFT", sector = "Technology")))
        assertEquals(listOf(error(7, "geography", RestoreReason.OverrideValueNotOnList)), errors(*held, override("AAPL", geography = "Mars")))
        assertEquals(listOf(error(8, "ticker", RestoreReason.DuplicateOverride(7))), errors(*held, override("AAPL", sector = "Technology"), override("AAPL", sector = "Technology")))
        // Matched as in 4.9; the list's own spelling is kept.
        val passed = passed(*held, override("AAPL", sector = " technology "))
        assertEquals("Technology", passed.records.overrides.single().value)
    }

    // Lists (4.9): a duplicate value or remembered text, a value matching a remembered text, a text pointing to no value.
    @Test
    fun theListRows() {
        assertEquals(listOf(error(5, "value", RestoreReason.DuplicateListValue(4))), errors(opening, listValue("sector", "Tech"), listValue("Sector", " TECH ")))
        assertEquals(
            listOf(error(6, "source_text", RestoreReason.DuplicateRememberedText(5))),
            errors(opening, listValue("sector", "Technology"), sourceText("sector", "Technology", "IT"), sourceText("sector", "technology", "it")),
        )
        assertEquals(
            listOf(error(5, "value", RestoreReason.ListValueMatchesRememberedText(6))),
            errors(opening, listValue("sector", "Technology"), listValue("sector", "Info Tech"), sourceText("sector", "Technology", "Info  tech")),
        )
        assertEquals(listOf(error(4, "value", RestoreReason.RememberedTextValueNotOnList)), errors(opening, sourceText("geography", "Asia", "APAC")))
        // The same text on the other list is fine.
        assertEquals(
            2,
            passed(opening, listValue("sector", "Asia"), listValue("geography", "Asia"), sourceText("sector", "Asia", "APAC"), sourceText("geography", "Asia", "APAC"))
                .records.lists.remembered.size,
        )
    }

    // Settings: a key twice; a missing row is never an error, and its default applies.
    @Test
    fun aSettingKeyTwice_andTheDefaults() {
        assertEquals(listOf(error(5, "key", RestoreReason.SettingKeyTwice(4))), errors(opening, setting("auto_lock", "1_min"), setting("AUTO_LOCK", "5_min")))
        val records = passed(opening).records
        assertEquals(AutoLock.EVERY_LEAVE, records.autoLock)
        assertEquals(TabOrder.DEFAULT, records.tabOrder)
        assertEquals("finnhub", records.priceSource)
        assertEquals(RestoreSummary(0, 0, 1, 1_000_000L, 0), passed(opening).summary)
    }

    // C7 and 33a decision 2: a row that depends on a failed row names the root cause's line, and is not checked further.
    @Test
    fun dependentRows_nameTheRootCause() {
        val errors =
            errors(
                opening,
                main,
                buy("L1", "2026-01-02", "0", "100.00"),
                sell("2026-01-03", "1", "10.00", "L1:1", id = "S1"),
                reversal("S1", "2026-01-04"),
                hidden("AAPL"),
                override("AAPL", sector = "Tech"),
                reversal("L1", "2026-01-05"),
            )
        assertEquals(
            listOf(
                error(5, "quantity", RestoreReason.QuantityNotPositive),
                error(6, "lot_picks", RestoreReason.DependsOn(5)),
                error(7, "reverses", RestoreReason.DependsOn(5)),
                error(8, "ticker", RestoreReason.DependsOn(5)),
                error(9, "ticker", RestoreReason.DependsOn(5)),
                error(10, "reverses", RestoreReason.DependsOn(5)),
            ),
            errors,
        )
    }

    @Test
    fun aFailedPortfolio_makesItsRowsDependOnIt() {
        val errors = errors(opening, portfolio("AB"), buy("L1", "2026-01-02", "1", "10.00", portfolio = "AB"), split("2026-01-03", "2", "1"))
        assertEquals(
            listOf(
                error(4, "portfolio", RestoreReason.PortfolioNameTooShort),
                error(5, "portfolio", RestoreReason.DependsOn(4)),
                error(6, "portfolio", RestoreReason.PortfolioNotFound),
            ),
            errors,
        )
    }

    // A split of a holding whose buys failed depends on them; a buy that fails a rule (here cash) fails its holding too.
    @Test
    fun aSplitOrHiddenRow_dependsOnTheHoldingsFailedBuy() {
        val errors = errors(opening, main, buy("L1", "2026-01-02", "1", "20000.00"), split("2026-01-03", "2", "1"))
        assertEquals(
            listOf(
                error(5, null, RestoreReason.CashNegative(LocalDate.of(2026, 1, 2), 1_000_000L)),
                error(6, "ticker", RestoreReason.DependsOn(5)),
            ),
            errors,
        )
    }

    // A buy that fails as a duplicate id fails its holding too (decision 2; 33a Deploy finding 1).
    @Test
    fun aBuyWithADuplicateId_failsItsHolding() {
        val errors = errors(opening, main, buy("L1", "2026-01-02", "1", "10.00", ticker = "MSFT"), buy("l1", "2026-01-02", "1", "10.00"), hidden("AAPL"))
        assertEquals(
            listOf(
                error(6, "id", RestoreReason.IdDuplicate(5)),
                error(7, "ticker", RestoreReason.DependsOn(6)),
            ),
            errors,
        )
    }

    // The replay is in date order, then file order (4.1): a file whose rows are out of date order replays the same.
    @Test
    fun theReplay_isInDateOrder_thenFileOrder() {
        val passed =
            passed(
                opening,
                main,
                sell("2026-03-01", "5", "60.00", "L1:5"),
                buy("L1", "2026-02-01", "10", "100.00"),
            )
        assertEquals(listOf(TransactionType.BUY, TransactionType.SELL), passed.records.transactions.sortedBy { it.enteredSeq }.map { it.type })
        assertEquals(1_000_000L - 10_000L + 6_000L, passed.summary.cashBalanceCents)
    }
}
