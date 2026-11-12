package app.ghghorg.atsassets.data

import app.ghghorg.atsassets.core.AssetType
import app.ghghorg.atsassets.core.AutoLock
import app.ghghorg.atsassets.core.ClassList
import app.ghghorg.atsassets.core.ClassLists
import app.ghghorg.atsassets.core.ClassOverride
import app.ghghorg.atsassets.core.Fraction
import app.ghghorg.atsassets.core.HiddenHolding
import app.ghghorg.atsassets.core.Instrument
import app.ghghorg.atsassets.core.Price
import app.ghghorg.atsassets.core.RememberedText
import app.ghghorg.atsassets.core.SellAllocation
import app.ghghorg.atsassets.core.SourceProfile
import app.ghghorg.atsassets.core.SplitRatio
import app.ghghorg.atsassets.core.Tab
import app.ghghorg.atsassets.core.TabOrder
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import app.ghghorg.atsassets.core.UnreadableDataException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.LocalDate

class RowMappingTest {
    private val buy =
        Transaction(
            id = "t1",
            portfolioId = "p1",
            ticker = "AAPL",
            type = TransactionType.BUY,
            date = LocalDate.of(2026, 2, 1),
            enteredSeq = 4L,
            quantity = BigDecimal("2.50"),
            totalCents = 150_000L,
        )

    @Test
    fun transaction_isStoredAsText_andReadBackUnchanged() {
        val row = buy.toRow()
        assertEquals("2026-02-01", row.date)
        assertEquals("BUY", row.type)
        assertEquals(null, row.reversesId)
        assertEquals(buy, row.toTransaction())
    }

    @Test
    fun quantity_keepsItsExactTextAndScale() {
        assertEquals("2.50", buy.toRow().quantity)
        assertEquals(2, buy.toRow().toTransaction().quantity.scale())
        assertEquals("0.00000001", buy.copy(quantity = BigDecimal("0.00000001")).toRow().quantity)
        assertEquals("1234567.12345678", buy.copy(quantity = BigDecimal("1234567.12345678")).toRow().quantity)
    }

    @Test
    fun instrument_isStoredAndReadBackUnchanged() {
        val instrument = Instrument(ticker = "SPY", assetType = AssetType.ETF, verified = true)
        assertEquals("ETF", instrument.toRow().assetType)
        assertEquals(instrument, instrument.toRow().toInstrument())
    }

    private val spy = Instrument(ticker = "SPY", assetType = AssetType.ETF, verified = true)
    private val stalePrice = Price(BigDecimal("348.9100"), Instant.ofEpochSecond(1_793_304_000), stale = true)

    @Test
    fun instrument_withAPrice_storesItAsExactText_andReadsItBackUnchanged() {
        val row = spy.copy(price = stalePrice).toRow()
        assertEquals("348.9100", row.price)
        assertEquals(1_793_304_000L, row.priceTime)
        assertEquals(true, row.priceStale)
        assertEquals(spy.copy(price = stalePrice), row.toInstrument())
        assertEquals(4, row.toInstrument().price!!.perShare.scale())
        val fresh = Price(BigDecimal("0.0089"), Instant.ofEpochSecond(1_793_232_000))
        assertEquals(spy.copy(price = fresh), spy.copy(price = fresh).toRow().toInstrument())
    }

    @Test
    fun instrument_withoutAPrice_leavesThePriceColumnsEmpty() {
        val row = spy.toRow()
        assertEquals(null, row.price)
        assertEquals(null, row.priceTime)
        assertEquals(false, row.priceStale)
        assertEquals(spy, row.toInstrument())
    }

    @Test
    fun aDamagedStoredPrice_isUnreadable_withNoValueInTheMessage() {
        val damaged =
            listOf(
                spy.toRow().copy(price = "abc", priceTime = 1L),
                spy.toRow().copy(price = "0", priceTime = 1L),
                spy.toRow().copy(price = "-1.5", priceTime = 1L),
                spy.toRow().copy(price = "348.91", priceTime = 0L),
                spy.toRow().copy(price = "348.91", priceTime = null),
                spy.toRow().copy(price = null, priceTime = 1L),
                spy.toRow().copy(priceStale = true),
            )
        for (row in damaged) {
            val error = assertThrows(row.toString(), UnreadableDataException::class.java) { row.toInstrument() }
            assertFalse(row.toString(), error.message!!.contains("SPY") || error.message!!.contains("348"))
        }
    }

    @Test
    fun savingAnInstrumentAgain_keepsItsStoredPrice() {
        val stored = spy.copy(price = stalePrice).toRow()
        val again = spy.copy(assetType = AssetType.STOCK).toRow()
        assertEquals(again.copy(price = "348.9100", priceTime = 1_793_304_000L, priceStale = true), again.withStoredOf(stored))
        assertEquals(again, again.withStoredOf(null))
        assertEquals(again, again.withStoredOf(spy.toRow()))
    }

    // 23a: the source profile on the instrument (decision 3) and the two lists.
    private val aapl = Instrument(ticker = "AAPL", assetType = AssetType.STOCK, verified = true)

    @Test
    fun instrument_withAProfile_storesItsTexts_andReadsThemBack() {
        val row = aapl.copy(profile = SourceProfile("Technology", "US")).toRow()
        assertEquals("Technology", row.sourceSector)
        assertEquals("US", row.sourceGeography)
        assertEquals(true, row.profileAsked)
        assertEquals(aapl.copy(profile = SourceProfile("Technology", "US")), row.toInstrument())
    }

    @Test
    fun instrument_askedWithNoValues_isReadBackAsAsked() {
        // An ETF's empty answer: asked, nothing given, so not asked again (decision 3).
        val row = spy.copy(profile = SourceProfile(null, null)).toRow()
        assertEquals(true, row.profileAsked)
        assertEquals(spy.copy(profile = SourceProfile(null, null)), row.toInstrument())
    }

    @Test
    fun instrument_neverAsked_hasNoProfile() {
        val row = aapl.toRow()
        assertEquals(false, row.profileAsked)
        assertEquals(null, row.sourceSector)
        assertEquals(null, row.toInstrument().profile)
    }

    @Test
    fun aSourceTextWithoutTheAskedMark_isUnreadable_withNoValueInTheMessage() {
        for (row in listOf(aapl.toRow().copy(sourceSector = "Technology"), aapl.toRow().copy(sourceGeography = "US"))) {
            val error = assertThrows(row.toString(), UnreadableDataException::class.java) { row.toInstrument() }
            assertFalse(row.toString(), error.message!!.contains("Technology") || error.message!!.contains("US"))
        }
    }

    @Test
    fun savingAnInstrumentAgain_keepsItsStoredProfile() {
        // A buy saves its instrument again (09); only a refresh sets the profile (23a).
        val stored = aapl.copy(price = stalePrice, profile = SourceProfile("Technology", null)).toRow()
        val again = aapl.toRow()
        assertEquals(aapl.copy(price = stalePrice, profile = SourceProfile("Technology", null)), again.withStoredOf(stored).toInstrument())
    }

    @Test
    fun listRows_readBackAsTheTwoLists_eachInTheOrderAdded() {
        val rows =
            listOf(
                ListValueRow(seq = 1, list = "SECTOR", value = "Technology"),
                ListValueRow(seq = 2, list = "GEOGRAPHY", value = "US"),
                ListValueRow(seq = 3, list = "SECTOR", value = "Energy"),
            )
        assertEquals(ClassLists(sectors = listOf("Technology", "Energy"), geographies = listOf("US")), rows.toClassLists())
        assertEquals(ClassLists(), emptyList<ListValueRow>().toClassLists())
    }

    @Test
    fun anUnknownList_orTwoMatchingValuesInAList_isUnreadable() {
        val bad =
            listOf(
                listOf(ListValueRow(seq = 1, list = "INDUSTRY", value = "Technology")),
                listOf(ListValueRow(seq = 1, list = "SECTOR", value = "Technology"), ListValueRow(seq = 2, list = "SECTOR", value = "technology ")),
            )
        for (rows in bad) {
            val error = assertThrows(rows.toString(), UnreadableDataException::class.java) { rows.toClassLists() }
            assertFalse(rows.toString(), error.message!!.contains("Technology"))
        }
        // The same text in the two lists is allowed: they are separate (4.9).
        val both = listOf(ListValueRow(seq = 1, list = "SECTOR", value = "Global"), ListValueRow(seq = 2, list = "GEOGRAPHY", value = "Global"))
        assertEquals(ClassLists(sectors = listOf("Global"), geographies = listOf("Global")), both.toClassLists())
    }

    // 24: the list values an override can name; "Global" is on both lists, as separate rows.
    private val listValues =
        listOf(
            ListValueRow(seq = 1, list = "SECTOR", value = "Technology"),
            ListValueRow(seq = 2, list = "GEOGRAPHY", value = "US"),
            ListValueRow(seq = 3, list = "SECTOR", value = "Global"),
            ListValueRow(seq = 4, list = "GEOGRAPHY", value = "Global"),
        )

    // 24 decision 3: an override row names its value's list row; read back as the value, as the list spells it.
    @Test
    fun overrideRows_readBackAsTheValuesTheyName() {
        val rows = listOf(ClassOverrideRow("p1", "VT", "SECTOR", 3), ClassOverrideRow("p1", "VT", "GEOGRAPHY", 4), ClassOverrideRow("p2", "AAPL", "GEOGRAPHY", 2))
        assertEquals(
            listOf(
                ClassOverride("p1", "VT", ClassList.SECTOR, "Global"),
                ClassOverride("p1", "VT", ClassList.GEOGRAPHY, "Global"),
                ClassOverride("p2", "AAPL", ClassList.GEOGRAPHY, "US"),
            ),
            rows.toOverrides(listValues),
        )
    }

    // 24 decision 3: an override naming no row, a row of the other list, or an unknown list is unreadable; nothing guessed.
    @Test
    fun anOverrideNamingNoValueOfItsList_isUnreadable() {
        val bad = listOf(ClassOverrideRow("p1", "VT", "SECTOR", 9), ClassOverrideRow("p1", "VT", "SECTOR", 2), ClassOverrideRow("p1", "VT", "INDUSTRY", 1))
        for (row in bad) {
            val error = assertThrows(row.toString(), UnreadableDataException::class.java) { listOf(row).toOverrides(listValues) }
            assertFalse(row.toString(), error.message!!.contains("VT"))
        }
    }

    // 24 decision 3 and 4.9 (D7): a save names the row of its own list whose value matches, ignoring case and spaces.
    @Test
    fun anOverride_namesTheMatchingRowOfItsOwnList_orNone() {
        assertEquals(ClassOverrideRow("p1", "VT", "GEOGRAPHY", 4), ClassOverride("p1", "VT", ClassList.GEOGRAPHY, " global ").toRow(listValues))
        assertEquals(ClassOverrideRow("p1", "VT", "SECTOR", 1), ClassOverride("p1", "VT", ClassList.SECTOR, "TECHNOLOGY").toRow(listValues))
        assertEquals(null, ClassOverride("p1", "VT", ClassList.SECTOR, "US").toRow(listValues))
        assertEquals(null, ClassOverride("p1", "VT", ClassList.SECTOR, "Energy").toRow(listValues))
    }

    // 25: the lists with remembered texts; each text names its value's row, as an override does (24 decision 3).
    private val namedValues =
        listOf(
            ListValueRow(seq = 1, list = "SECTOR", value = "Technology"),
            ListValueRow(seq = 2, list = "GEOGRAPHY", value = "USA"),
            ListValueRow(seq = 3, list = "SECTOR", value = "Diversified"),
        )
    private val texts =
        listOf(
            SourceTextRow(seq = 1, list = "SECTOR", valueSeq = 1, text = "Information Technology"),
            SourceTextRow(seq = 2, list = "GEOGRAPHY", valueSeq = 2, text = "United States"),
        )

    @Test
    fun textRows_readBackAsRememberedTexts_ofTheValuesTheyName() {
        assertEquals(
            ClassLists(
                sectors = listOf("Technology", "Diversified"),
                geographies = listOf("USA"),
                remembered =
                listOf(
                    RememberedText(ClassList.SECTOR, "Information Technology", "Technology"),
                    RememberedText(ClassList.GEOGRAPHY, "United States", "USA"),
                ),
            ),
            namedValues.toClassLists(texts),
        )
    }

    // 4.9: a text naming no row of its list, of an unknown list, matching another text or matching a value is unreadable.
    @Test
    fun aTextNamingNoValue_orMatchingATextOrAValue_isUnreadable() {
        val bad =
            listOf(
                listOf(SourceTextRow(seq = 1, list = "SECTOR", valueSeq = 9, text = "Info Tech")),
                listOf(SourceTextRow(seq = 1, list = "SECTOR", valueSeq = 2, text = "Info Tech")),
                listOf(SourceTextRow(seq = 1, list = "INDUSTRY", valueSeq = 1, text = "Info Tech")),
                listOf(
                    SourceTextRow(seq = 1, list = "SECTOR", valueSeq = 1, text = "Info Tech"),
                    SourceTextRow(seq = 2, list = "SECTOR", valueSeq = 3, text = "info  tech"),
                ),
                listOf(SourceTextRow(seq = 1, list = "SECTOR", valueSeq = 3, text = " DIVERSIFIED")),
            )
        for (rows in bad) {
            val error = assertThrows(rows.toString(), UnreadableDataException::class.java) { namedValues.toClassLists(rows) }
            assertFalse(rows.toString(), error.message!!.contains("Info"))
            assertFalse(rows.toString(), error.message!!.contains("DIVERSIFIED"))
        }
    }

    @Test
    fun anAddedValue_isItsTrimmedName_onItsList_andARefusedOneThrows() {
        assertEquals(ListValueRow(list = "SECTOR", value = "Energy"), addedRow(namedValues, texts, ClassList.SECTOR, " Energy "))
        assertEquals(ListValueRow(list = "GEOGRAPHY", value = "Global"), addedRow(namedValues, texts, ClassList.GEOGRAPHY, "Global"))
        for (name in listOf("technology", "information  technology", "  ")) {
            assertThrows(name, IllegalArgumentException::class.java) { addedRow(namedValues, texts, ClassList.SECTOR, name) }
        }
    }

    // Decision 1: the old name is remembered only when a stored source text matches it.
    @Test
    fun aRename_changesItsRow_andRemembersTheSourcesText() {
        val profiles = listOf(SourceProfile(null, "usa"), SourceProfile("Information Technology", null))
        assertEquals(
            ListValueRow(seq = 2, list = "GEOGRAPHY", value = "America") to SourceTextRow(list = "GEOGRAPHY", valueSeq = 2, text = "USA"),
            renamedRows(namedValues, texts, profiles, ClassList.GEOGRAPHY, "USA", "America"),
        )
        assertEquals(
            ListValueRow(seq = 3, list = "SECTOR", value = "Multi") to null,
            renamedRows(namedValues, texts, profiles, ClassList.SECTOR, "Diversified", " Multi "),
        )
    }

    @Test
    fun aRefusedRename_orOneOfAValueNotOnTheList_throws() {
        val cases = listOf(ClassList.SECTOR to "Information Technology", ClassList.SECTOR to "technology", ClassList.GEOGRAPHY to "Multi")
        for ((list, to) in cases) {
            assertThrows("$list $to", IllegalArgumentException::class.java) {
                renamedRows(namedValues, texts, emptyList(), list, "Diversified", to)
            }
        }
    }

    // Decision 3: a delete takes the overrides naming the value only when K10 listed them as of holdings no longer there.
    @Test
    fun aDelete_namesItsRow_andOnlyListedOverridesGoWithIt() {
        assertEquals(3L, deletedSeq(namedValues, emptyList(), ClassList.SECTOR, "Diversified", emptyList()))
        val gone = ClassOverride("p9", "GONE", ClassList.SECTOR, "Diversified")
        val rows = listOf(ClassOverrideRow("p9", "GONE", "SECTOR", 3), ClassOverrideRow("p1", "AAPL", "SECTOR", 1))
        assertEquals(3L, deletedSeq(namedValues, rows, ClassList.SECTOR, "Diversified", listOf(gone)))
        val live = rows + ClassOverrideRow("p1", "VT", "SECTOR", 3)
        assertThrows(IllegalArgumentException::class.java) { deletedSeq(namedValues, live, ClassList.SECTOR, "Diversified", listOf(gone)) }
        assertThrows(IllegalArgumentException::class.java) { deletedSeq(namedValues, emptyList(), ClassList.GEOGRAPHY, "Diversified", emptyList()) }
    }

    // Decision 4: a price save adds the fresh profiles' texts, then the stored texts of held instruments.
    @Test
    fun aPriceSave_handsK10TheFreshProfiles_thenTheHeldInstrumentsStoredOnes() {
        val fresh = SourceProfile("Technology", "US")
        val msft = SourceProfile("Software", "US")
        val stored =
            listOf(
                InstrumentRow("AAPL", "STOCK", true, sourceSector = "Old", sourceGeography = null, profileAsked = true),
                InstrumentRow("MSFT", "STOCK", true, sourceSector = "Software", sourceGeography = "US", profileAsked = true),
                InstrumentRow("VT", "ETF", true),
                InstrumentRow("IBM", "STOCK", true, sourceSector = "Hardware", sourceGeography = "US", profileAsked = true),
            )
        assertEquals(listOf(fresh, msft), profilesToAdd(mapOf("AAPL" to fresh), stored, listOf("AAPL", "MSFT", "VT", "XOM")))
        assertEquals(listOf(fresh), profilesToAdd(mapOf("AAPL" to fresh), stored, emptyList()))
    }

    private val laterBuy = buy.copy(id = "t2", date = LocalDate.of(2026, 2, 3), enteredSeq = 5L, quantity = BigDecimal("10"))

    private val sell =
        buy.copy(
            id = "s1",
            type = TransactionType.SELL,
            date = LocalDate.of(2026, 3, 1),
            enteredSeq = 6L,
            quantity = BigDecimal("3.5"),
            totalCents = 70_000L,
            allocations =
            listOf(
                SellAllocation("t1", Fraction.of(BigDecimal("2.5"))),
                SellAllocation("t2", Fraction.of(BigInteger.ONE, BigInteger.ONE)),
            ),
        )

    @Test
    fun sellAllocations_areStoredAsReducedFractions_andReadBackIntoTheirSell() {
        val rows = sell.allocationRows()
        assertEquals(listOf("s1", "s1"), rows.map { it.sellId })
        assertEquals(listOf("t1", "t2"), rows.map { it.lotId })
        assertEquals(listOf("5/2", "1/1"), rows.map { it.quantity })
        assertEquals("SELL", sell.toRow().type)
        val read = withAllocations(listOf(buy.toRow(), laterBuy.toRow(), sell.toRow()).map { it.toTransaction() }, rows)
        assertEquals(listOf(buy, laterBuy, sell), read)
        assertEquals(emptyList<SellAllocationRow>(), buy.allocationRows())
    }

    @Test
    fun sellAllocations_areReadBackOldestLotFirst_whateverTheRowOrder() {
        val read = withAllocations(listOf(buy, laterBuy, sell.copy(allocations = emptyList())), sell.allocationRows().reversed())
        assertEquals(listOf("t1", "t2"), read.last().allocations.map { it.lotId })
    }

    @Test
    fun aDamagedAllocation_aSellWithoutAllocations_orAnAllocationOfNoSell_isUnreadable() {
        val bare = listOf(buy, laterBuy, sell.copy(allocations = emptyList()))
        val damaged = sell.allocationRows().map { it.copy(quantity = "10/2") }
        assertThrows(UnreadableDataException::class.java) { withAllocations(bare, damaged) }
        assertThrows(UnreadableDataException::class.java) { withAllocations(bare, emptyList()) }
        val stray = sell.allocationRows() + SellAllocationRow(sellId = "s9", lotId = "t1", quantity = "1/1")
        assertThrows(UnreadableDataException::class.java) { withAllocations(bare, stray) }
    }

    private val split =
        buy.copy(
            id = "x1",
            type = TransactionType.SPLIT,
            date = LocalDate.of(2026, 3, 1),
            enteredSeq = 7L,
            quantity = BigDecimal.ZERO,
            totalCents = 0L,
            split = SplitRatio(1, 3),
        )

    @Test
    fun aSplit_keepsItsRatioInTwoColumns_andEveryOtherRowLeavesThemEmpty() {
        val row = split.toRow()
        assertEquals("SPLIT", row.type)
        assertEquals(1, row.splitTo)
        assertEquals(3, row.splitFrom)
        assertEquals(split, row.toTransaction())
        assertEquals(null, buy.toRow().splitTo)
        assertEquals(null, sell.toRow().splitFrom)
        assertEquals(1_000_000, split.copy(split = SplitRatio(1_000_000, 1)).toRow().toTransaction().split?.to)
    }

    @Test
    fun aSplitWithoutAnAllowedRatio_orABuyOrSellWithOne_isUnreadable() {
        val row = split.toRow()
        val damaged =
            listOf(
                row.copy(splitTo = null),
                row.copy(splitFrom = null),
                row.copy(splitTo = 0),
                row.copy(splitFrom = -1),
                row.copy(splitTo = 1_000_001),
                row.copy(splitTo = 3),
                buy.toRow().copy(splitTo = 2),
                buy.toRow().copy(splitFrom = 3),
                buy.toRow().copy(splitTo = 0, splitFrom = 3),
                buy.toRow().copy(splitTo = 1, splitFrom = -3),
                buy.toRow().copy(splitTo = 3, splitFrom = 3),
                sell.toRow().copy(splitFrom = 1),
                sell.toRow().copy(splitTo = 1, splitFrom = 3),
            )
        damaged.forEach { assertThrows(UnreadableDataException::class.java) { it.toTransaction() } }
    }

    // 14c (CR1): a buy re-entered with its quantity left as filled in keeps its quantity ratio in the split columns.
    @Test
    fun aBuyWithAQuantityRatio_keepsItInTheSplitColumns_andIsReadBackUnchanged() {
        val kept = buy.copy(quantity = BigDecimal("1"), quantityRatio = SplitRatio(1, 3))
        val row = kept.toRow()
        assertEquals(1, row.splitTo)
        assertEquals(3, row.splitFrom)
        assertEquals(kept, row.toTransaction())
        assertEquals(null, row.toTransaction().split)
        val large = kept.copy(quantityRatio = SplitRatio(1, Int.MAX_VALUE))
        assertEquals(large, large.toRow().toTransaction())
    }

    private val reversal =
        buy.copy(
            id = "r1",
            type = TransactionType.REVERSAL,
            date = LocalDate.of(2026, 3, 2),
            enteredSeq = 8L,
            quantity = BigDecimal.ZERO,
            totalCents = 0L,
            reversesId = "t1",
        )

    @Test
    fun aReversal_keepsOnlyItsLink_andIsReadBackUnchanged() {
        val row = reversal.toRow()
        assertEquals("REVERSAL", row.type)
        assertEquals("t1", row.reversesId)
        assertEquals("0", row.quantity)
        assertEquals(0L, row.totalCents)
        assertEquals(null, row.splitTo)
        assertEquals(reversal, row.toTransaction())
        assertEquals(listOf(buy, reversal), checkReversals(listOf(buy, reversal)))
    }

    @Test
    fun aReversalWithoutItsLink_orABuyWithOne_isUnreadable() {
        assertThrows(UnreadableDataException::class.java) { reversal.toRow().copy(reversesId = null).toTransaction() }
        assertThrows(UnreadableDataException::class.java) { buy.toRow().copy(reversesId = "t9").toTransaction() }
    }

    @Test
    fun aReversalOfNothing_ofAReversal_aSecondOne_oneDatedBeforeItsOriginal_orInAnotherHolding_isUnreadable() {
        val damaged =
            listOf(
                listOf(reversal),
                listOf(buy, reversal, reversal.copy(id = "r2", reversesId = "r1")),
                listOf(buy, reversal, reversal.copy(id = "r2", enteredSeq = 9L)),
                listOf(buy, reversal.copy(date = LocalDate.of(2026, 1, 31))),
                listOf(buy, reversal.copy(ticker = "SPY")),
                listOf(buy, reversal.copy(portfolioId = "p2")),
            )
        damaged.forEach { assertThrows(UnreadableDataException::class.java) { checkReversals(it) } }
    }

    // 14a: a Reverse and re-enter pair is stored as a reversal and a normal entry; the new entry has no link (4.6).
    @Test
    fun aBuyPair_withTheNewBuyDatedBeforeItsReversal_isReadBackUnchanged() {
        val later = reversal.copy(date = LocalDate.of(2026, 3, 3))
        val newBuy = buy.copy(id = "n1", enteredSeq = 9L, quantity = BigDecimal("100"))
        val rows = listOf(buy, later, newBuy).map { it.toRow() }
        assertEquals(null, rows.last().reversesId)
        assertEquals(listOf(buy, later, newBuy), checkReversals(withAllocations(rows.map { it.toTransaction() }, emptyList())))
        assertEquals(emptyList<SellAllocationRow>(), later.allocationRows())
    }

    // 14b: a split's pair is a reversal of the split and a new split, neither with a cash line or allocations.
    @Test
    fun aSplitPair_isReadBackUnchanged_throughTheStoredChecks() {
        val splitReversal = reversal.copy(id = "r3", reversesId = "x1", date = LocalDate.of(2026, 3, 1), enteredSeq = 9L)
        val newSplit = split.copy(id = "n3", enteredSeq = 10L, split = SplitRatio(1, 2))
        val transactions = listOf(buy, split, splitReversal, newSplit)
        assertEquals(emptyList<SellAllocationRow>(), newSplit.allocationRows())
        assertEquals(null, newSplit.toRow().reversesId)
        val read = checkReversals(withAllocations(transactions.map { it.toRow().toTransaction() }, emptyList()))
        assertEquals(transactions, read)
    }

    @Test
    fun aSellPair_isReadBackWithTheOriginalsAndTheNewSellsAllocations_eachInItsOwnSell() {
        val sellReversal = reversal.copy(id = "r2", reversesId = "s1", date = LocalDate.of(2026, 3, 1), enteredSeq = 9L)
        val newSell =
            sell.copy(
                id = "n2",
                enteredSeq = 10L,
                totalCents = 80_000L,
                allocations = listOf(SellAllocation("t1", Fraction.of(BigInteger.valueOf(5), BigInteger.valueOf(3)))),
            )
        val transactions = listOf(buy, laterBuy, sell, sellReversal, newSell)
        val rows = sell.allocationRows() + newSell.allocationRows()
        assertEquals(listOf("5/3"), newSell.allocationRows().map { it.quantity })
        val read = checkReversals(withAllocations(transactions.map { it.toRow().toTransaction() }, rows))
        assertEquals(transactions, read)
    }

    // 15: a hidden holding is a portfolio id and a ticker (section 8's hidden row).
    @Test
    fun aHiddenHolding_isStoredAsItsPortfolioAndTicker_andReadBackUnchanged() {
        val hidden = HiddenHolding("p1", "AAPL")
        val row = hidden.toRow()
        assertEquals("p1", row.portfolioId)
        assertEquals("AAPL", row.ticker)
        assertEquals(hidden, row.toHidden())
    }

    private val soldOut =
        sell.copy(
            id = "s2",
            quantity = BigDecimal("2.50"),
            allocations = listOf(SellAllocation("t1", Fraction.of(BigDecimal("2.50")))),
        )

    // A hidden holding always has 0 shares, counting every entry (4.4, section 8; 15 decision 1): one with shares,
    // or with no transaction at all, is damaged data and is not guessed at.
    @Test
    fun aHiddenHoldingAtZero_isReadable_andOneWithShares_orWithNoHolding_isUnreadable() {
        val hidden = listOf(HiddenHolding("p1", "AAPL"))
        assertEquals(hidden, checkHidden(listOf(buy, soldOut), hidden))
        assertThrows(UnreadableDataException::class.java) { checkHidden(listOf(buy), hidden) }
        assertThrows(UnreadableDataException::class.java) { checkHidden(listOf(buy, soldOut), listOf(HiddenHolding("p1", "MSFT"))) }
    }

    // 20: the auto-lock choice in the settings row (decision 3).
    @Test
    fun noSettingsRow_isTheDefault_everyLeave() {
        val none: SettingsRow? = null
        assertEquals(AutoLock.EVERY_LEAVE, none.toAutoLock())
    }

    @Test
    fun eachAutoLockChoice_isStoredByName_andReadBack() {
        for (choice in AutoLock.entries) {
            assertEquals(choice, SettingsRow(autoLock = choice.name).toAutoLock())
        }
    }

    @Test
    fun anUnknownAutoLockChoice_isUnreadable_notTheDefault() {
        for (text in listOf("", "NEVER", "idle_1_min", "IDLE_10_MIN")) {
            assertThrows(text, UnreadableDataException::class.java) { SettingsRow(autoLock = text).toAutoLock() }
        }
    }

    // 22: the tab order in the same row, as spec 8's text.
    @Test
    fun noSettingsRow_orNoOrderInIt_isTheDefaultOrder() {
        val none: SettingsRow? = null
        assertEquals(TabOrder.DEFAULT, none.toTabOrder())
        assertEquals(TabOrder.DEFAULT, SettingsRow(autoLock = AutoLock.IDLE_1_MIN.name).toTabOrder())
    }

    @Test
    fun aTabOrder_isStoredAsItsText_andReadBack() {
        val order = TabOrder.of(listOf(Tab.OVERALL, Tab.PORTFOLIOS, Tab.CASH, Tab.MORE))!!
        val row = null.withTabOrder(order.text)
        assertEquals("overall;portfolios;cash;more", row.tabOrder)
        assertEquals(order, row.toTabOrder())
    }

    @Test
    fun aStoredOrderThatIsNotEachTabOnce_isUnreadable_notTheDefault() {
        val bad = listOf("", "more;cash;portfolios", "more;cash;cash;overall", "more;cash;portfolios;settings", "more,cash,portfolios,overall")
        for (text in bad) {
            val row = SettingsRow(autoLock = AutoLock.EVERY_LEAVE.name, tabOrder = text)
            assertThrows(text, UnreadableDataException::class.java) { row.toTabOrder() }
        }
    }

    @Test
    fun savingTheTabOrder_keepsTheAutoLockChoice_andTheOtherWayRound() {
        val old = "overall;more;cash;portfolios"
        val new = "cash;more;portfolios;overall"
        val both = SettingsRow(autoLock = AutoLock.IDLE_5_MIN.name, tabOrder = old)
        assertEquals(SettingsRow(autoLock = AutoLock.IDLE_5_MIN.name, tabOrder = new), both.withTabOrder(new))
        assertEquals(SettingsRow(autoLock = AutoLock.IDLE_1_MIN.name, tabOrder = old), both.withAutoLock(AutoLock.IDLE_1_MIN.name))
    }

    @Test
    fun theFirstSave_makesTheRow_withTheOtherSettingAtItsDefault() {
        // A tab order saved first leaves the auto-lock choice at its default; an auto-lock choice saved first, no order.
        val order = "cash;more;portfolios;overall"
        assertEquals(SettingsRow(autoLock = AutoLock.EVERY_LEAVE.name, tabOrder = order), null.withTabOrder(order))
        assertEquals(SettingsRow(autoLock = AutoLock.IDLE_1_MIN.name, tabOrder = null), null.withAutoLock(AutoLock.IDLE_1_MIN.name))
    }
}
