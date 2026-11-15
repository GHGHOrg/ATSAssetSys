package app.ghghorg.atsassets.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/** K6's export half (spec section 8; iteration 31): the backup file's text, from records made by K1 where a rule matters. */
class BackupFileTest {
    /**
     * A small app state built through K1, so entry numbers, allocations and ratios are the real ones. Its ids ("app-1",
     * "app-2", …) are never file ids: the file has its own (decision 5b).
     */
    private class Book {
        var cash: List<CashEntry> = emptyList()
        var transactions: List<Transaction> = emptyList()
        var portfolios: List<Portfolio> = emptyList()
        private var made = 0

        private fun id(): String = "app-${++made}"

        fun open(
            date: String,
            cents: Long,
            note: String = "",
        ): CashEntry {
            val created = OpeningBalance.create(cash, id(), LocalDate.parse(date), cents, note) as OpeningBalanceResult.Created
            cash = cash + created.entry
            return created.entry
        }

        fun portfolio(name: String): String {
            val created = (Portfolios.create(portfolios, cash, id(), name) as PortfolioResult.Created).portfolio
            portfolios = portfolios + created
            return created.id
        }

        fun cash(
            type: CashType,
            date: String,
            cents: Long,
            note: String = "",
            direction: CashDirection = CashDirection.IN,
        ): CashEntry {
            val created = CashEntries.create(cash, id(), type, LocalDate.parse(date), cents, note, direction) as CashEntryResult.Created
            cash = cash + created.entry
            return created.entry
        }

        fun reverseCash(
            original: CashEntry,
            date: String,
            note: String,
        ): CashEntry {
            val created = CashEntries.reverse(cash, id(), original.id, LocalDate.parse(date), note) as CashEntryResult.Created
            cash = cash + created.entry
            return created.entry
        }

        fun buy(
            portfolioId: String,
            ticker: String,
            date: String,
            quantity: String,
            cents: Long,
            ratio: SplitRatio? = null,
        ): Transaction {
            val created =
                Transactions.buy(
                    cash,
                    transactions,
                    portfolios,
                    id(),
                    id(),
                    portfolioId,
                    ticker,
                    LocalDate.parse(date),
                    BigDecimal(quantity),
                    cents,
                    ratio,
                ) as BuyResult.Created
            transactions = transactions + created.transaction
            cash = cash + created.cashLine
            return created.transaction
        }

        fun split(
            portfolioId: String,
            ticker: String,
            date: String,
            to: Int,
            from: Int,
        ): Transaction {
            val created =
                Splits.split(cash, transactions, portfolios, id(), portfolioId, ticker, LocalDate.parse(date), SplitRatio(to, from))
                    as SplitResult.Created
            transactions = transactions + created.transaction
            return created.transaction
        }

        fun sell(
            portfolioId: String,
            ticker: String,
            date: String,
            cents: Long,
            picks: List<Pick>,
        ): Transaction {
            val quantity = picks.fold(BigDecimal.ZERO) { sum, pick -> sum + pick.quantity }
            val created =
                Sells.sell(cash, transactions, portfolios, id(), id(), portfolioId, ticker, LocalDate.parse(date), quantity, cents, picks)
                    as SellResult.Created
            transactions = transactions + created.transaction
            cash = cash + created.cashLine
            return created.transaction
        }

        /** Sell all (4.4): every lot at its remainder as the picker shows it. */
        fun sellAll(
            portfolioId: String,
            ticker: String,
            date: String,
            cents: Long,
        ): Transaction {
            val seq = Transactions.nextSeq(cash, transactions)
            val choices = Sells.lotChoices(transactions, portfolioId, ticker, LocalDate.parse(date), seq)
            return sell(portfolioId, ticker, date, cents, Sells.sellAll(choices))
        }

        /** A reversal of a buy, sell or split as the store holds it: only the link, the date and the entry number (4.6). */
        fun reverse(
            original: Transaction,
            date: String,
        ): Transaction {
            val seq = Transactions.nextSeq(cash, transactions)
            val reversal =
                Transaction(
                    id = id(),
                    portfolioId = original.portfolioId,
                    ticker = original.ticker,
                    type = TransactionType.REVERSAL,
                    date = LocalDate.parse(date),
                    enteredSeq = seq,
                    quantity = BigDecimal.ZERO,
                    totalCents = 0L,
                    reversesId = original.id,
                )
            transactions = transactions + reversal
            if (original.type != TransactionType.SPLIT) {
                val back = if (original.type == TransactionType.BUY) original.totalCents else -original.totalCents
                cash = cash + CashEntry(id(), reversal.date, back, CashType.TRADE, "", seq, transactionId = reversal.id)
            }
            return reversal
        }

        fun records(
            hidden: List<HiddenHolding> = emptyList(),
            overrides: List<ClassOverride> = emptyList(),
            lists: ClassLists = ClassLists(),
            autoLock: AutoLock = AutoLock.EVERY_LEAVE,
            tabOrder: TabOrder = TabOrder.DEFAULT,
        ): BackupRecords = BackupRecords(cash, portfolios, transactions, hidden, overrides, lists, autoLock, tabOrder, "finnhub")
    }

    private fun textOf(records: BackupRecords): String = (BackupFile.write(records) as BackupResult.Written).text

    private fun linesOf(records: BackupRecords): List<String> = textOf(records).removeSuffix("\n").split("\n")

    private fun rowsOf(
        records: BackupRecords,
        type: String,
    ): List<String> = linesOf(records).filter { it.startsWith("$type,") }

    /** The records of spec/example-backup.csv as the app would hold them after entering them by hand in the file's order. */
    private fun sample(): BackupRecords {
        val book = Book()
        book.open("2026-01-01", 1_000_000L)
        val main = book.portfolio("Main")
        book.portfolio("Retirement")
        val l1 = book.buy(main, "AAPL", "2026-01-02", "10", 100_000L)
        val l2 = book.buy(main, "AAPL", "2026-02-01", "10", 150_000L)
        val o1 = book.buy(main, "OLDCO", "2026-02-10", "100", 20_000L)
        book.sell(main, "AAPL", "2026-03-01", 210_000L, listOf(Pick(l1.id, BigDecimal("10")), Pick(l2.id, BigDecimal("2"))))
        book.sell(main, "OLDCO", "2026-03-10", 0L, listOf(Pick(o1.id, BigDecimal("100"))))
        val l3 = book.buy(main, "AAPL", "2026-03-20", "2", 36_000L)
        book.split(main, "AAPL", "2026-04-01", 2, 1)
        book.reverse(l3, "2026-04-05")
        book.cash(CashType.INTEREST_DIVIDEND, "2026-04-15", 2_500L, "Dividend")
        val deposit = book.cash(CashType.DEPOSIT, "2026-04-20", 50_000L, "Deposit entered by mistake")
        book.reverseCash(deposit, "2026-04-21", "Reversal of the deposit of 2026-04-20 ET")
        return book.records(
            hidden = listOf(HiddenHolding(main, "OLDCO")),
            overrides =
            listOf(
                ClassOverride(main, "AAPL", ClassList.GEOGRAPHY, "United States"),
                ClassOverride(main, "AAPL", ClassList.SECTOR, "Technology"),
            ),
            lists =
            ClassLists(
                sectors = listOf("Technology", "Diversified"),
                geographies = listOf("United States", "Global"),
                remembered = listOf(RememberedText(ClassList.SECTOR, "Information Technology", "Technology")),
            ),
            autoLock = AutoLock.IDLE_5_MIN,
            tabOrder = TabOrder.parse("overall;portfolios;cash;more")!!,
        )
    }

    private val header =
        "row_type,id,portfolio,date,ticker,quantity,amount,lot_picks,split_to,split_from,cash_type,note,reverses,sector,geography," +
            "list,key,value,source_text"

    // The sample file's records, written in full: the format row first, settings, portfolios in creation order, list
    // values alphabetically, the entries in entry order with an id each, a reversal with only its link, date and note,
    // then the hidden and override rows; LF line endings and no byte-order mark (section 8).
    @Test
    fun sampleRecords_theWholeFile() {
        val expected =
            listOf(
                header,
                "format,,,,,,,,,,,,,,,,,1,",
                "setting,,,,,,,,,,,,,,,,auto_lock,5_min,",
                "setting,,,,,,,,,,,,,,,,price_source,finnhub,",
                "setting,,,,,,,,,,,,,,,,tab_order,overall;portfolios;cash;more,",
                "portfolio,,Main,,,,,,,,,,,,,,,,",
                "portfolio,,Retirement,,,,,,,,,,,,,,,,",
                "list_value,,,,,,,,,,,,,,,sector,,Diversified,",
                "list_value,,,,,,,,,,,,,,,sector,,Technology,",
                "list_value,,,,,,,,,,,,,,,geography,,Global,",
                "list_value,,,,,,,,,,,,,,,geography,,United States,",
                "source_text,,,,,,,,,,,,,,,sector,,Technology,Information Technology",
                "cash,C1,,2026-01-01,,,10000.00,,,,opening_balance,,,,,,,,",
                "buy,L1,Main,2026-01-02,AAPL,10,1000.00,,,,,,,,,,,,",
                "buy,L2,Main,2026-02-01,AAPL,10,1500.00,,,,,,,,,,,,",
                "buy,L3,Main,2026-02-10,OLDCO,100,200.00,,,,,,,,,,,,",
                "sell,S1,Main,2026-03-01,AAPL,12,2100.00,L1:10;L2:2,,,,,,,,,,,",
                "sell,S2,Main,2026-03-10,OLDCO,100,0.00,L3:100,,,,,,,,,,,",
                "buy,L4,Main,2026-03-20,AAPL,2,360.00,,,,,,,,,,,,",
                "split,X1,Main,2026-04-01,AAPL,,,,2,1,,,,,,,,,",
                "reversal,,,2026-04-05,,,,,,,,,L4,,,,,,",
                "cash,C2,,2026-04-15,,,25.00,,,,interest_dividend,Dividend,,,,,,,",
                "cash,C3,,2026-04-20,,,500.00,,,,deposit,Deposit entered by mistake,,,,,,,",
                "reversal,,,2026-04-21,,,,,,,,Reversal of the deposit of 2026-04-20 ET,C3,,,,,,",
                "hidden,,Main,,OLDCO,,,,,,,,,,,,,,",
                "override,,Main,,AAPL,,,,,,,,,Technology,United States,,,,",
            )
        assertEquals(expected.joinToString("\n", postfix = "\n"), textOf(sample()))
    }

    // Every row of spec/example-backup.csv that carries no id (the header, the format row, the settings, portfolios,
    // list values, the remembered text, the hidden row and the override) is written exactly as the sample has it.
    @Test
    fun sampleRecords_theSpecSamplesRowsWithoutIds_areWrittenTheSame() {
        val written = linesOf(sample())
        val sampleFile = File("../spec/example-backup.csv").readLines().filter { it.isNotBlank() }
        val kinds = listOf("row_type,", "format,", "setting,", "portfolio,", "list_value,", "source_text,", "hidden,", "override,")
        val expected = sampleFile.filter { line -> kinds.any { line.startsWith(it) } }
        assertEquals(13, expected.size)
        for (line in expected) assertTrue("missing: $line", line in written)
    }

    @Test
    fun sampleRecords_everyRowHas19Cells_andTheHeaderNamesThem() {
        val written = linesOf(sample())
        assertEquals(BackupFile.COLUMNS.joinToString(","), written.first())
        assertEquals(19, BackupFile.COLUMNS.size)
        for (line in written) assertEquals(line, 19, line.split(",").size)
    }

    @Test
    fun text_endsEachLineWithLf_andHasNoCarriageReturnOrByteOrderMark() {
        val text = textOf(sample())
        assertTrue(text.endsWith("\n"))
        assertFalse(text.contains('\r'))
        assertFalse(text.startsWith("﻿"))
    }

    // The entry-order question (31 file, section 4): a split has no cash line and a cash entry is numbered from the cash
    // lines only, so the deposit entered after a split gets the split's number. The file puts the split first, and the
    // two cannot change each other's result (a split moves no cash).
    @Test
    fun entryOrder_aCashEntryEnteredAfterASplit_sharesItsNumber_andIsWrittenAfterIt() {
        val book = Book()
        book.open("2026-01-01", 1_000_000L)
        val main = book.portfolio("Main")
        book.buy(main, "AAPL", "2026-01-02", "10", 100_000L)
        val split = book.split(main, "AAPL", "2026-01-05", 2, 1)
        val deposit = book.cash(CashType.DEPOSIT, "2026-01-05", 5_000L)
        assertEquals(split.enteredSeq, deposit.enteredSeq)
        val entries = linesOf(book.records()).filter { it.startsWith("split,") || it.startsWith("cash,") }
        assertEquals(
            listOf(
                "cash,C1,,2026-01-01,,,10000.00,,,,opening_balance,,,,,,,,",
                "split,X1,Main,2026-01-05,AAPL,,,,2,1,,,,,,,,,",
                "cash,C2,,2026-01-05,,,50.00,,,,deposit,,,,,,,,",
            ),
            entries,
        )
    }

    // Two splits in a row, then a deposit: the deposit's number is the first split's, lower than the second's, so it is
    // written between them. Entry order by number, a transaction before a cash entry on a tie.
    @Test
    fun entryOrder_aCashEntryNumberedBelowALaterSplit_isWrittenByItsNumber() {
        val book = Book()
        book.open("2026-01-01", 1_000_000L)
        val main = book.portfolio("Main")
        book.buy(main, "AAPL", "2026-01-02", "10", 100_000L)
        book.split(main, "AAPL", "2026-01-05", 2, 1)
        book.split(main, "AAPL", "2026-01-06", 3, 1)
        book.cash(CashType.DEPOSIT, "2026-01-07", 5_000L)
        val kinds = linesOf(book.records()).map { it.substringBefore(",") }.filter { it in setOf("cash", "buy", "split") }
        assertEquals(listOf("cash", "buy", "split", "cash", "split"), kinds)
    }

    // Decision 5b: a letter for the kind and a running number per kind, in entry order; a reversal has no id; a trade
    // cash line is not written at all.
    @Test
    fun ids_aLetterPerKindAndARunningNumber_inEntryOrder() {
        val book = Book()
        book.open("2026-01-01", 1_000_000L)
        val main = book.portfolio("Main")
        val first = book.buy(main, "AAPL", "2026-01-02", "10", 100_000L)
        book.cash(CashType.WITHDRAWAL, "2026-01-03", 20_000L)
        val second = book.buy(main, "MSFT", "2026-01-04", "1", 30_000L)
        book.sell(main, "AAPL", "2026-01-05", 60_000L, listOf(Pick(first.id, BigDecimal("4"))))
        book.reverse(second, "2026-01-06")
        book.sell(main, "AAPL", "2026-01-07", 70_000L, listOf(Pick(first.id, BigDecimal("6"))))
        val ids = linesOf(book.records()).drop(1).map { it.split(",") }.filter { it[0] in setOf("cash", "buy", "sell", "split", "reversal") }
        assertEquals(listOf("C1", "L1", "C2", "L2", "S1", "", "S2"), ids.map { it[1] })
        assertEquals(7, ids.size)
        assertEquals("L2", ids[5][12])
        assertFalse(textOf(book.records()).contains("app-"))
        assertFalse(textOf(book.records()).contains("trade"))
    }

    // Signs and decimals (4.2, section 8): a withdrawal is negative, an adjustment out is negative, cents keep two places.
    @Test
    fun cashRows_theAmountIsSigned_withTwoDecimals() {
        val book = Book()
        book.open("2026-01-01", 1_000_005L)
        book.cash(CashType.WITHDRAWAL, "2026-01-02", 20_000L)
        book.cash(CashType.ADJUSTMENT, "2026-01-03", 7L, "Bank fee", CashDirection.OUT)
        book.cash(CashType.ADJUSTMENT, "2026-01-04", 150L, "Found", CashDirection.IN)
        val rows = rowsOf(book.records(), "cash").map { it.split(",") }
        assertEquals(listOf("10000.05", "-200.00", "-0.07", "1.50"), rows.map { it[6] })
        assertEquals(listOf("opening_balance", "withdrawal", "adjustment", "adjustment"), rows.map { it[10] })
        assertEquals(listOf("", "", "Bank fee", "Found"), rows.map { it[11] })
    }

    // AC-59 (c): 10 shares, a 1 for 3 split, then the whole lot sold as shown: the pick is written as the picker shows
    // it, 3.33333333, which a restore reads as the lot's exact remainder (4.4).
    @Test
    fun picks_aWholeLotOfTenThirds_isWrittenAsShown() {
        val book = Book()
        book.open("2026-01-01", 1_000_000L)
        val main = book.portfolio("Main")
        val lot = book.buy(main, "AAPL", "2026-01-02", "10", 100_000L)
        book.split(main, "AAPL", "2026-01-05", 1, 3)
        val sell = book.sell(main, "AAPL", "2026-01-06", 40_000L, listOf(Pick(lot.id, BigDecimal("3.33333333"))))
        assertEquals(Fraction.of(BigDecimal("10")) / Fraction.of(BigDecimal("3")), sell.allocations.single().quantity)
        assertEquals(listOf("sell,S1,Main,2026-01-06,AAPL,3.33333333,400.00,L1:3.33333333,,,,,,,,,,,"), rowsOf(book.records(), "sell"))
    }

    // AC-59 (e): a leftover too small to show is shown, picked and written as 0.00000001 (CR1).
    @Test
    fun picks_aLeftoverBelowTheSmallestQuantity_isWrittenAsTheSmallest() {
        val book = Book()
        book.open("2026-01-01", 1_000_000L)
        val main = book.portfolio("Main")
        val lot = book.buy(main, "V", "2026-01-02", "0.00000003", 100L)
        book.split(main, "V", "2026-01-05", 1, 7)
        val sell = book.sellAll(main, "V", "2026-01-06", 100L)
        assertTrue(sell.allocations.single().quantity < Fraction.of(BigDecimal("0.00000001")))
        assertEquals(lot.id, sell.allocations.single().lotId)
        assertEquals(listOf("sell,S1,Main,2026-01-06,V,0.00000001,1.00,L1:0.00000001,,,,,,,,,,,"), rowsOf(book.records(), "sell"))
    }

    // AC-59 (f): a buy re-entered with its quantity left as filled in keeps the original's quantity and the ratio of the
    // splits it does not get; the ratio is written on that buy only (section 8).
    @Test
    fun buyRows_aReenteredBuysRatio_isWrittenInTheSplitColumns() {
        val book = Book()
        book.open("2026-01-01", 1_000_000L)
        val main = book.portfolio("Main")
        book.buy(main, "MA", "2026-01-02", "1", 10_000L)
        book.buy(main, "MA", "2026-01-03", "1", 10_000L, SplitRatio(1, 3))
        assertEquals(
            listOf(
                "buy,L1,Main,2026-01-02,MA,1,100.00,,,,,,,,,,,,",
                "buy,L2,Main,2026-01-03,MA,1,100.00,,1,3,,,,,,,,,",
            ),
            rowsOf(book.records(), "buy"),
        )
    }

    // Remainders that round up as well as down, over several lots (CLAUDE.md, 12's pitfall): 2/3 and 5/3 show
    // 0.66666667 and 1.66666667; 3/7 and 1/7 show 0.42857143 and 0.14285714. Sell all writes each pick as shown, and the
    // picks add up to the written sell quantity, as section 8 requires.
    @Test
    fun picks_remaindersThatRoundUpAndDown_addUpToTheSellQuantity() {
        val book = Book()
        book.open("2026-01-01", 1_000_000L)
        val main = book.portfolio("Main")
        book.buy(main, "AAA", "2026-01-02", "2", 10_000L)
        book.buy(main, "AAA", "2026-01-03", "5", 10_000L)
        book.split(main, "AAA", "2026-01-05", 1, 3)
        book.buy(main, "BBB", "2026-01-02", "3", 10_000L)
        book.buy(main, "BBB", "2026-01-03", "1", 10_000L)
        book.split(main, "BBB", "2026-01-05", 1, 7)
        book.sellAll(main, "AAA", "2026-01-06", 10_000L)
        book.sellAll(main, "BBB", "2026-01-06", 10_000L)
        val sells = rowsOf(book.records(), "sell").map { it.split(",") }
        assertEquals(listOf("2.33333334", "0.57142857"), sells.map { it[5] })
        assertEquals(listOf("L1:0.66666667;L2:1.66666667", "L3:0.42857143;L4:0.14285714"), sells.map { it[7] })
        for (sell in sells) {
            val sum = sell[7].split(";").fold(BigDecimal.ZERO) { total, pick -> total + BigDecimal(pick.substringAfter(":")) }
            assertEquals(0, sum.compareTo(BigDecimal(sell[5])))
        }
    }

    // A quantity is written plain: no thousands separator, no exponent, trailing zeros dropped.
    @Test
    fun quantities_areWrittenPlain() {
        val book = Book()
        book.open("2026-01-01", 100_000_000_000L)
        val main = book.portfolio("Main")
        book.buy(main, "AAA", "2026-01-02", "1000000", 10_000L)
        book.buy(main, "BBB", "2026-01-02", "2.50", 10_000L)
        book.buy(main, "CCC", "2026-01-02", "0.00000001", 10_000L)
        assertEquals(listOf("1000000", "2.5", "0.00000001"), rowsOf(book.records(), "buy").map { it.split(",")[5] })
    }

    // Decision 3: a cell with a comma or a double quote is wrapped in double quotes and a quote inside it is doubled;
    // every other cell is written bare. A portfolio name, a note, a list value and a remembered text can all hold them.
    @Test
    fun cells_withACommaOrAQuote_areQuoted_theOthersBare() {
        val book = Book()
        book.open("2026-01-01", 1_000_000L, "Start, from the \"old\" broker")
        val main = book.portfolio("Main, \"old\"")
        book.buy(main, "AAPL", "2026-01-02", "10", 100_000L)
        book.cash(CashType.DEPOSIT, "2026-01-03", 5_000L, " a note with spaces kept ")
        val records =
            book.records(
                overrides = listOf(ClassOverride(main, "AAPL", ClassList.SECTOR, "Banks, diversified")),
                lists =
                ClassLists(
                    sectors = listOf("Banks, diversified"),
                    remembered = listOf(RememberedText(ClassList.SECTOR, "Banks, \"big\"", "Banks, diversified")),
                ),
            )
        val written = linesOf(records)
        assertTrue("portfolio,,\"Main, \"\"old\"\"\",,,,,,,,,,,,,,,," in written)
        assertTrue("cash,C1,,2026-01-01,,,10000.00,,,,opening_balance,\"Start, from the \"\"old\"\" broker\",,,,,,," in written)
        assertTrue("cash,C2,,2026-01-03,,,50.00,,,,deposit, a note with spaces kept ,,,,,,," in written)
        assertTrue("buy,L1,\"Main, \"\"old\"\"\",2026-01-02,AAPL,10,1000.00,,,,,,,,,,,," in written)
        assertTrue("list_value,,,,,,,,,,,,,,,sector,,\"Banks, diversified\"," in written)
        assertTrue("source_text,,,,,,,,,,,,,,,sector,,\"Banks, diversified\",\"Banks, \"\"big\"\"\"" in written)
        assertTrue("override,,\"Main, \"\"old\"\"\",,AAPL,,,,,,,,,\"Banks, diversified\",,,,," in written)
    }

    // 4.6: a cash reversal is an adjustment linked to its original; the file's reversal row holds its link, date and note.
    @Test
    fun reversalRows_aCashReversal_carriesItsLinkDateAndNote_andNoId() {
        val book = Book()
        book.open("2026-01-01", 1_000_000L)
        val deposit = book.cash(CashType.DEPOSIT, "2026-01-02", 5_000L)
        book.reverseCash(deposit, "2026-01-04", "Entered twice")
        assertEquals(listOf("reversal,,,2026-01-04,,,,,,,,Entered twice,C2,,,,,,"), rowsOf(book.records(), "reversal"))
        assertEquals(2, rowsOf(book.records(), "cash").size)
    }

    // 4.6: a reversal of a sell and of a split, each naming its original's file id.
    @Test
    fun reversalRows_ofASellAndASplit_nameTheirOriginals() {
        val book = Book()
        book.open("2026-01-01", 1_000_000L)
        val main = book.portfolio("Main")
        val lot = book.buy(main, "AAPL", "2026-01-02", "10", 100_000L)
        val sell = book.sell(main, "AAPL", "2026-01-03", 50_000L, listOf(Pick(lot.id, BigDecimal("4"))))
        val split = book.split(main, "AAPL", "2026-01-04", 2, 1)
        book.reverse(split, "2026-01-05")
        book.reverse(sell, "2026-01-06")
        assertEquals(
            listOf("reversal,,,2026-01-05,,,,,,,,,X1,,,,,,", "reversal,,,2026-01-06,,,,,,,,,S1,,,,,,"),
            rowsOf(book.records(), "reversal"),
        )
    }

    // AC-60 (a), the export half: a holding that was hidden and then unhidden by the reversal of its sell is no longer in
    // the hidden list, so the file has no hidden row for it.
    @Test
    fun hiddenRows_noneForAHoldingThatIsNotHidden() {
        val book = Book()
        book.open("2026-01-01", 100_000L)
        val main = book.portfolio("Main")
        val lot = book.buy(main, "AAPL", "2026-01-02", "10", 10_000L)
        val sell = book.sell(main, "AAPL", "2026-01-03", 12_000L, listOf(Pick(lot.id, BigDecimal("10"))))
        val whileHidden = book.records(hidden = listOf(HiddenHolding(main, "AAPL")))
        assertEquals(listOf("hidden,,Main,,AAPL,,,,,,,,,,,,,,"), rowsOf(whileHidden, "hidden"))
        book.reverse(sell, "2026-01-03")
        assertTrue(rowsOf(book.records(), "hidden").isEmpty())
    }

    // Open item 58's safeguard: a hidden or override row is written only for a holding with a buy or sell row in the
    // file, as section 8 refuses any other on restore.
    @Test
    fun stateRows_onlyForAHoldingWithABuyOrSellInTheFile() {
        val book = Book()
        book.open("2026-01-01", 1_000_000L)
        val main = book.portfolio("Main")
        book.buy(main, "AAPL", "2026-01-02", "10", 100_000L)
        val records =
            book.records(
                hidden = listOf(HiddenHolding(main, "GONE"), HiddenHolding("no-such-portfolio", "AAPL")),
                overrides =
                listOf(
                    ClassOverride(main, "GONE", ClassList.SECTOR, "Technology"),
                    ClassOverride("no-such-portfolio", "AAPL", ClassList.SECTOR, "Technology"),
                    ClassOverride(main, "AAPL", ClassList.GEOGRAPHY, "Global"),
                ),
                lists = ClassLists(sectors = listOf("Technology"), geographies = listOf("Global")),
            )
        assertTrue(rowsOf(records, "hidden").isEmpty())
        assertEquals(listOf("override,,Main,,AAPL,,,,,,,,,,Global,,,,"), rowsOf(records, "override"))
    }

    // One row per holding with an override, both values on it; portfolios in creation order, then the ticker.
    @Test
    fun overrideRows_oneRowPerHolding_inPortfolioThenTickerOrder() {
        val book = Book()
        book.open("2026-01-01", 1_000_000L)
        val first = book.portfolio("Zeta")
        val second = book.portfolio("Alpha")
        book.buy(second, "MSFT", "2026-01-02", "1", 10_000L)
        book.buy(first, "MSFT", "2026-01-02", "1", 10_000L)
        book.buy(first, "AAPL", "2026-01-02", "1", 10_000L)
        val records =
            book.records(
                overrides =
                listOf(
                    ClassOverride(second, "MSFT", ClassList.SECTOR, "Technology"),
                    ClassOverride(first, "MSFT", ClassList.GEOGRAPHY, "Global"),
                    ClassOverride(first, "AAPL", ClassList.SECTOR, "Technology"),
                    ClassOverride(first, "AAPL", ClassList.GEOGRAPHY, "Global"),
                ),
                lists = ClassLists(sectors = listOf("Technology"), geographies = listOf("Global")),
            )
        assertEquals(
            listOf(
                "override,,Zeta,,AAPL,,,,,,,,,Technology,Global,,,,",
                "override,,Zeta,,MSFT,,,,,,,,,,Global,,,,",
                "override,,Alpha,,MSFT,,,,,,,,,Technology,,,,,",
            ),
            rowsOf(records, "override"),
        )
        assertEquals(listOf("portfolio,,Zeta,,,,,,,,,,,,,,,,", "portfolio,,Alpha,,,,,,,,,,,,,,,,"), rowsOf(records, "portfolio"))
    }

    // List values alphabetically, case ignored, the sector list first; a list may be empty.
    @Test
    fun listRows_alphabeticallyWithoutCase_andAnEmptyListWritesNone() {
        val records = Book().records(lists = ClassLists(sectors = listOf("utilities", "Energy", "Technology")))
        assertEquals(
            listOf(
                "list_value,,,,,,,,,,,,,,,sector,,Energy,",
                "list_value,,,,,,,,,,,,,,,sector,,Technology,",
                "list_value,,,,,,,,,,,,,,,sector,,utilities,",
            ),
            rowsOf(records, "list_value"),
        )
    }

    // The settings in force (section 8's keys): each auto-lock choice by its word, the tab order as stored, the source.
    @Test
    fun settingRows_eachAutoLockChoiceByItsWord_andTheDefaults() {
        val words =
            AutoLock.entries.associateWith { choice ->
                rowsOf(Book().records(autoLock = choice), "setting").first().split(",")[17]
            }
        assertEquals(
            mapOf(
                AutoLock.EVERY_LEAVE to "every_exit",
                AutoLock.IDLE_1_MIN to "1_min",
                AutoLock.IDLE_5_MIN to "5_min",
                AutoLock.PHONE_LOCK_OR_RESTART to "phone_lock_only",
            ),
            words,
        )
        assertEquals(
            listOf(
                "setting,,,,,,,,,,,,,,,,auto_lock,every_exit,",
                "setting,,,,,,,,,,,,,,,,price_source,finnhub,",
                "setting,,,,,,,,,,,,,,,,tab_order,more;cash;portfolios;overall,",
            ),
            rowsOf(Book().records(), "setting"),
        )
    }

    // An app with no data still gives a well-formed text (the screen does not offer the export then, decision 6).
    @Test
    fun noData_theHeaderTheFormatRowAndTheSettingsOnly() {
        assertEquals(5, linesOf(Book().records()).size)
    }

    // Section 6, number format: the file's digits, dots and dates do not follow the phone's language.
    @Test
    fun text_isTheSameUnderALanguageWithOtherDigits() {
        val usual = textOf(sample())
        val before = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ar-EG-u-nu-arab"))
            assertEquals(usual, textOf(sample()))
            assertEquals("backup-2026-10-09.csv", BackupFile.fileName(Instant.parse("2026-10-09T03:00:00Z"), ZoneId.of("Asia/Tokyo")))
        } finally {
            Locale.setDefault(before)
        }
    }

    // A stored line break cannot be written into a cell (section 8); the writer refuses and writes nothing.
    @Test
    fun aLineBreakInAStoredNote_isRefused() {
        val book = Book()
        val opening = book.open("2026-01-01", 1_000_000L)
        book.cash = listOf(opening.copy(note = "two\nlines"))
        assertEquals(BackupResult.Refused(BackupRefusal.LINE_BREAK), BackupFile.write(book.records()))
    }

    // Records that point to something missing are refused, not written as a file a restore would reject.
    @Test
    fun brokenLinks_areRefused() {
        val book = Book()
        book.open("2026-01-01", 1_000_000L)
        val main = book.portfolio("Main")
        val lot = book.buy(main, "AAPL", "2026-01-02", "10", 100_000L)
        val sell = book.sell(main, "AAPL", "2026-01-03", 50_000L, listOf(Pick(lot.id, BigDecimal("4"))))
        val whole = book.transactions
        val refused = BackupResult.Refused(BackupRefusal.BROKEN_LINK)

        book.transactions = whole.map { if (it.id == lot.id) it.copy(portfolioId = "gone") else it }
        assertEquals(refused, BackupFile.write(book.records()))

        book.transactions = whole.filter { it.id != lot.id }
        assertEquals(refused, BackupFile.write(book.records()))

        book.transactions = whole + sell.copy(id = "r", type = TransactionType.REVERSAL, reversesId = "gone", allocations = emptyList())
        assertEquals(refused, BackupFile.write(book.records()))

        book.transactions = whole + lot.copy(id = "s", type = TransactionType.SPLIT, split = null)
        assertEquals(refused, BackupFile.write(book.records()))
    }

    // C15 and section 8: the suggested name carries the export moment's date in the device's zone.
    @Test
    fun fileName_theDateInTheDevicesZone() {
        val moment = Instant.parse("2026-10-09T16:30:00Z")
        assertEquals("backup-2026-10-10.csv", BackupFile.fileName(moment, ZoneId.of("Asia/Tokyo")))
        assertEquals("backup-2026-10-09.csv", BackupFile.fileName(moment, ZoneId.of("America/New_York")))
        assertEquals("backup-2026-10-09.csv", BackupFile.fileName(moment, ZoneId.of("UTC")))
    }

    // Timing (31 file, section 4): 3,000 transactions are written in well under a second on this PC; the bound is wide
    // on purpose, so a busy PC does not fail it. The writer does one pass: picks come from the stored allocations.
    @Test
    fun threeThousandTransactions_areWrittenQuickly() {
        val history = ScaleHistories.build(3_000, ScaleShape.SPREAD)
        val records =
            BackupRecords(
                history.cashEntries,
                history.portfolios,
                history.transactions,
                emptyList(),
                emptyList(),
                ClassLists(),
                AutoLock.EVERY_LEAVE,
                TabOrder.DEFAULT,
                "finnhub",
            )
        textOf(records)
        val started = System.nanoTime()
        val text = textOf(records)
        val millis = (System.nanoTime() - started) / 1_000_000
        println("BackupFile.write, 3,000 transactions: $millis ms, ${text.length} characters")
        assertEquals(3_000, text.lines().count { line -> listOf("buy,", "sell,", "split,", "reversal,").any { line.startsWith(it) } })
        assertTrue("took $millis ms", millis < 5_000)
    }
}
