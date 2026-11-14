package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.AssetType
import app.ghghorg.atsassets.core.CashEntry
import app.ghghorg.atsassets.core.CashType
import app.ghghorg.atsassets.core.Instrument
import app.ghghorg.atsassets.core.KeyCheck
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.Quote
import app.ghghorg.atsassets.core.Tab
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import app.ghghorg.atsassets.core.UnreadableDataException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

// 30 step 4: the price source's API key in the screen state (decisions 0 to 4): loaded at start, entered, changed and
// cleared in Settings, checked with the source before it is saved, and never held by the state beyond its last 4 characters.
@OptIn(ExperimentalCoroutinesApi::class)
class PriceKeyFlowTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val oldKey = "c0ffee0123456789a1b2"
    private val newKey = "0ddba11987654321z9y8"

    private val buy =
        Transaction("b1", "p1", "AAPL", TransactionType.BUY, LocalDate.parse("2026-01-02"), 2L, BigDecimal.TEN, 100_000L)

    /** One portfolio holding AAPL, so a refresh has a ticker to ask for; [key] is the stored key, null for none. */
    private fun store(key: String? = oldKey): FakeStore {
        val line = CashEntry("c-b1", buy.date, -buy.totalCents, CashType.TRADE, "", buy.enteredSeq, transactionId = buy.id)
        val store = FakeStore(stored = listOf(opening(), line))
        store.storedPortfolios = listOf(Portfolio(id = "p1", name = "Portfolio P", createdSeq = 1L))
        store.storedTransactions = listOf(buy)
        store.storedInstruments = listOf(Instrument("AAPL", AssetType.STOCK, verified = true))
        store.storedPriceKey = key
        return store
    }

    /** Started, then More and its gear: Settings. */
    private fun onSettings(
        store: FakeStore = store(),
        source: FakeSource = FakeSource(),
        online: () -> Boolean = { true },
    ): AppViewModel {
        val vm = started(store, source = source, online = online)
        vm.selectTab(Tab.MORE)
        vm.openSettings()
        return vm
    }

    /** The key entry open with [text] typed. */
    private fun typed(
        text: String,
        store: FakeStore = store(),
        source: FakeSource = FakeSource(),
        online: () -> Boolean = { true },
    ): AppViewModel {
        val vm = onSettings(store, source, online)
        vm.openKeyEntry()
        vm.onKeyTextChange(text)
        return vm
    }

    @Test
    fun atStart_theStoredKeyGoesToTheSource_andTheStateKeepsOnlyItsEnding() {
        val source = FakeSource()
        val vm = started(store(), source = source)
        assertEquals(listOf<String?>(oldKey), source.usedKeys)
        val state = vm.state.value
        assertTrue(state.priceKeySet)
        assertEquals("a1b2", state.priceKeyEnding)
        assertFalse(state.toString().contains(oldKey))
    }

    @Test
    fun atStart_theSourceHasTheKey_beforeTheFirstRefreshAsks() {
        val source = FakeSource()
        started(store(), source = source)
        assertEquals(listOf("AAPL"), source.quoted)
        assertEquals(listOf<String?>(oldKey), source.quotedWithKey)
    }

    @Test
    fun atStart_withNoKeyStored_theSourceGetsNone() {
        val source = FakeSource()
        val vm = started(store(key = null), source = source)
        assertEquals(listOf<String?>(null), source.usedKeys)
        assertFalse(vm.state.value.priceKeySet)
        assertEquals("", vm.state.value.priceKeyEnding)
    }

    @Test
    fun theKeyEntry_opensOnSettingsOnly_empty() {
        val vm = started(store())
        vm.openKeyEntry()
        assertNull(vm.state.value.keyEntry)
        vm.selectTab(Tab.MORE)
        vm.openSettings()
        vm.openKeyEntry()
        assertEquals(KeyEntry(), vm.state.value.keyEntry)
    }

    @Test
    fun theKeyEntry_doesNotOpenOverAnotherLayer() {
        val vm = onSettings()
        vm.openAutoLockChoice()
        vm.openKeyEntry()
        assertNull(vm.state.value.keyEntry)
    }

    @Test
    fun typing_keepsTheText_andClearsARefusal() {
        val vm = typed("abc 123")
        vm.saveKey()
        assertEquals(KeyRefusal.NotAKey, vm.state.value.keyEntry?.refusal)
        vm.onKeyTextChange("abc123")
        assertEquals(KeyEntry(text = "abc123"), vm.state.value.keyEntry)
    }

    @Test
    fun save_aTextThatIsNoKey_isRefused_andNothingIsSentOrStored() {
        for (text in listOf("", "   ", "abc 123", "abc-123", "abc\n123")) {
            val store = store()
            val source = FakeSource()
            val vm = typed(text, store, source)
            vm.saveKey()
            assertEquals(text, KeyRefusal.NotAKey, vm.state.value.keyEntry?.refusal)
            assertEquals(text, emptyList<String>(), source.checkedKeys)
            assertEquals(text, emptyList<String?>(), store.savedPriceKeys)
            assertFalse(vm.state.value.saving)
        }
    }

    @Test
    fun save_offline_isRefused_andNothingIsSentOrStored() {
        val store = store()
        val source = FakeSource()
        val vm = typed(newKey, store, source, online = { false })
        vm.saveKey()
        assertEquals(KeyEntry(text = newKey, refusal = KeyRefusal.Offline), vm.state.value.keyEntry)
        assertEquals(emptyList<String>(), source.checkedKeys)
        assertEquals(emptyList<String?>(), store.savedPriceKeys)
    }

    @Test
    fun save_anAcceptedKey_isStored_givenToTheSource_andTheEntryCloses() {
        val store = store()
        val source = FakeSource()
        val vm = typed(newKey, store, source)
        vm.saveKey()
        assertEquals(listOf(newKey), source.checkedKeys)
        assertEquals(listOf<String?>(newKey), store.savedPriceKeys)
        assertEquals(listOf<String?>(oldKey, newKey), source.usedKeys)
        val state = vm.state.value
        assertNull(state.keyEntry)
        assertFalse(state.saving)
        assertTrue(state.priceKeySet)
        assertEquals("z9y8", state.priceKeyEnding)
        assertEquals(Screen.SETTINGS, state.screen)
        assertFalse(state.toString().contains(newKey))
    }

    @Test
    fun save_spacesAndLineBreaksAroundAPastedKey_areLeftOut() {
        val store = store()
        val source = FakeSource()
        val vm = typed("  $newKey\n", store, source)
        vm.saveKey()
        assertEquals(listOf(newKey), source.checkedKeys)
        assertEquals(listOf<String?>(newKey), store.savedPriceKeys)
    }

    @Test
    fun save_anAcceptedKey_isFollowedByARefreshWithIt() {
        val source = FakeSource()
        val vm = typed(newKey, source = source)
        vm.saveKey()
        // The start's refresh with the old key, then the one after the save with the new key.
        assertEquals(listOf("AAPL", "AAPL"), source.quoted)
        assertEquals(listOf<String?>(oldKey, newKey), source.quotedWithKey)
    }

    @Test
    fun save_theFirstKeyOfAnAppWithoutOne_isStoredAndUsed() {
        val store = store(key = null)
        val source = FakeSource()
        val vm = typed(newKey, store, source)
        vm.saveKey()
        assertEquals(listOf<String?>(newKey), store.savedPriceKeys)
        assertEquals(listOf<String?>(null, newKey), source.usedKeys)
        assertTrue(vm.state.value.priceKeySet)
    }

    @Test
    fun save_aKeyTheSourceDoesNotAccept_isNotStored_andTheOldKeyStaysInUse() {
        val answers =
            mapOf(
                KeyCheck.Refused(401) to KeyRefusal.Refused(401),
                KeyCheck.Refused(403) to KeyRefusal.Refused(403),
                KeyCheck.TooManyCalls to KeyRefusal.TooManyCalls,
                KeyCheck.NoAnswer(503) to KeyRefusal.NoAnswer(503),
                KeyCheck.NoAnswer(null) to KeyRefusal.NoAnswer(null),
                KeyCheck.NotAKey to KeyRefusal.NotAKey,
            )
        for ((answer, refusal) in answers) {
            val store = store()
            val source = FakeSource().also { it.keyCheck = answer }
            val vm = typed(newKey, store, source)
            vm.saveKey()
            val state = vm.state.value
            assertEquals(answer.toString(), KeyEntry(text = newKey, refusal = refusal), state.keyEntry)
            assertEquals(answer.toString(), emptyList<String?>(), store.savedPriceKeys)
            assertEquals(answer.toString(), listOf<String?>(oldKey), source.usedKeys)
            assertEquals(answer.toString(), "a1b2", state.priceKeyEnding)
            assertFalse(state.saving)
            // No refresh follows a key that was not saved: only the start's.
            assertEquals(answer.toString(), listOf("AAPL"), source.quoted)
        }
    }

    @Test
    fun save_whileTheCheckRuns_theAppIsSaving_andNothingElseHappens() {
        val store = store()
        val gate = CompletableDeferred<Unit>()
        val source = FakeSource().also { it.keyGate = gate }
        val vm = typed(newKey, store, source)
        vm.saveKey()
        assertTrue(vm.state.value.saving)
        vm.saveKey()
        vm.cancelKeyEntry()
        vm.onKeyTextChange("other")
        vm.askClearKey()
        vm.closeSettings()
        assertEquals(listOf(newKey), source.checkedKeys)
        assertEquals(KeyEntry(text = newKey), vm.state.value.keyEntry)
        assertEquals(Screen.SETTINGS, vm.state.value.screen)
        assertEquals(emptyList<String?>(), store.savedPriceKeys)
        gate.complete(Unit)
        assertFalse(vm.state.value.saving)
        assertEquals(listOf<String?>(newKey), store.savedPriceKeys)
        assertNull(vm.state.value.keyEntry)
    }

    @Test
    fun save_anAcceptedKeyTheStoreDoesNotTake_isNotUsed() {
        val store = store().also { it.priceKeySaveError = IllegalStateException("disk") }
        val source = FakeSource()
        val vm = typed(newKey, store, source)
        vm.saveKey()
        val state = vm.state.value
        assertEquals(KeyEntry(text = newKey, refusal = KeyRefusal.SaveFailed), state.keyEntry)
        assertEquals(listOf<String?>(oldKey), source.usedKeys)
        assertEquals("a1b2", state.priceKeyEnding)
        assertFalse(state.saving)
    }

    @Test
    fun save_unreadableData_showsItsScreen_andLeavesNoEntry() {
        val store = store().also { it.priceKeySaveError = UnreadableDataException("unreadable") }
        val source = FakeSource()
        val vm = typed(newKey, store, source)
        vm.saveKey()
        val state = vm.state.value
        assertEquals(Screen.UNREADABLE, state.screen)
        assertNull(state.keyEntry)
        assertFalse(state.saving)
        assertEquals(listOf<String?>(oldKey), source.usedKeys)
    }

    @Test
    fun cancel_closesTheEntry_andNothingIsSentOrStored() {
        val store = store()
        val source = FakeSource()
        val vm = typed(newKey, store, source)
        vm.cancelKeyEntry()
        assertNull(vm.state.value.keyEntry)
        assertEquals(emptyList<String>(), source.checkedKeys)
        assertEquals(emptyList<String?>(), store.savedPriceKeys)
        // Opened again, the entry is empty: the typed text is not kept.
        vm.openKeyEntry()
        assertEquals(KeyEntry(), vm.state.value.keyEntry)
    }

    @Test
    fun clear_asksFirst_thenRemovesTheKey_fromTheStoreAndTheSource() {
        val store = store()
        val source = FakeSource()
        val vm = onSettings(store, source)
        vm.openKeyEntry()
        vm.askClearKey()
        assertEquals(KeyEntry(confirmingClear = true), vm.state.value.keyEntry)
        assertEquals(emptyList<String?>(), store.savedPriceKeys)
        vm.confirmClearKey()
        assertEquals(listOf<String?>(null), store.savedPriceKeys)
        assertEquals(listOf<String?>(oldKey, null), source.usedKeys)
        val state = vm.state.value
        assertNull(state.keyEntry)
        assertFalse(state.priceKeySet)
        assertEquals("", state.priceKeyEnding)
        assertFalse(state.saving)
        // No key is tested for a clear.
        assertEquals(emptyList<String>(), source.checkedKeys)
    }

    @Test
    fun clear_isFollowedByARefresh_soTheLineSaysThereIsNoKey() {
        val source = FakeSource()
        val vm = onSettings(store(), source)
        vm.openKeyEntry()
        vm.askClearKey()
        // As the real source answers without a key.
        source.quotes = mapOf("AAPL" to Quote.NoKey)
        vm.confirmClearKey()
        assertEquals(listOf<String?>(oldKey, null), source.quotedWithKey)
        assertTrue(vm.state.value.refreshFailed)
        assertTrue(vm.state.value.refreshNoKey)
    }

    @Test
    fun clear_cancelled_keepsTheKey_andTheEntryOpen() {
        val store = store()
        val vm = onSettings(store)
        vm.openKeyEntry()
        vm.onKeyTextChange("abc")
        vm.askClearKey()
        vm.cancelClearKey()
        assertEquals(KeyEntry(text = "abc"), vm.state.value.keyEntry)
        assertEquals(emptyList<String?>(), store.savedPriceKeys)
    }

    @Test
    fun clear_withoutAKey_orWithoutItsConfirmation_doesNothing() {
        val none = store(key = null)
        val vm = onSettings(none)
        vm.openKeyEntry()
        vm.askClearKey()
        assertEquals(KeyEntry(), vm.state.value.keyEntry)
        val kept = store()
        val other = onSettings(kept)
        other.openKeyEntry()
        other.confirmClearKey()
        assertEquals(emptyList<String?>(), kept.savedPriceKeys)
        assertNotNull(other.state.value.keyEntry)
    }

    @Test
    fun clear_theStoreDoesNotTake_keepsTheKeyInUse() {
        val store = store().also { it.priceKeySaveError = IllegalStateException("disk") }
        val source = FakeSource()
        val vm = onSettings(store, source)
        vm.openKeyEntry()
        vm.askClearKey()
        vm.confirmClearKey()
        val state = vm.state.value
        assertEquals(KeyEntry(refusal = KeyRefusal.SaveFailed), state.keyEntry)
        assertTrue(state.priceKeySet)
        assertEquals(listOf<String?>(oldKey), source.usedKeys)
    }

    @Test
    fun aFailedRefresh_withoutAKey_isTheNoKeyReason_andWithAKeyItIsNot() {
        val without = FakeSource().also { it.quotes = mapOf("AAPL" to Quote.NoKey) }
        val none = started(store(key = null), source = without)
        assertTrue(none.state.value.refreshFailed)
        assertTrue(none.state.value.refreshNoKey)
        val with = FakeSource().also { it.quotes = mapOf("AAPL" to Quote.NoAnswer(503)) }
        val keyed = started(store(), source = with)
        assertTrue(keyed.state.value.refreshFailed)
        assertFalse(keyed.state.value.refreshNoKey)
    }

    @Test
    fun withoutAKey_aRefreshThatDidNotFail_isNotTheNoKeyReason() {
        // No holding, so nothing to ask for and nothing failed.
        val vm = started(FakeStore(stored = listOf(opening())).also { it.storedPriceKey = null })
        assertFalse(vm.state.value.refreshFailed)
        assertFalse(vm.state.value.refreshNoKey)
    }

    // Deploy finding 1: a save or a clear while a refresh runs is followed by one more refresh, with the new key or none.
    @Test
    fun save_whileARefreshRuns_isFollowedByARefreshWithTheNewKey() {
        val gate = CompletableDeferred<Unit>()
        val source = FakeSource().also { it.quoteGate = gate }
        val vm = typed(newKey, source = source)
        assertTrue(vm.state.value.refreshing)
        vm.saveKey()
        assertEquals(listOf<String?>(oldKey, newKey), source.usedKeys)
        gate.complete(Unit)
        assertEquals(listOf<String?>(oldKey, newKey), source.quotedWithKey)
        assertFalse(vm.state.value.refreshing)
    }

    @Test
    fun clear_whileARefreshRuns_isFollowedByARefreshWithoutAKey_andTheNoKeyLine() {
        val gate = CompletableDeferred<Unit>()
        val source = FakeSource().also { it.quoteGate = gate }
        val vm = onSettings(store(), source)
        vm.openKeyEntry()
        vm.askClearKey()
        vm.confirmClearKey()
        // As the real source answers without a key.
        source.quotes = mapOf("AAPL" to Quote.NoKey)
        gate.complete(Unit)
        assertEquals(listOf<String?>(oldKey, null), source.quotedWithKey)
        assertTrue(vm.state.value.refreshNoKey)
    }

    @Test
    fun anUnrelatedRefreshAsked_whileOneRuns_isNotRepeated() {
        // Only a key change asks for one more: a pull during a refresh is ignored, as before 30.
        val gate = CompletableDeferred<Unit>()
        val source = FakeSource().also { it.quoteGate = gate }
        val vm = started(store(), source = source)
        vm.onPullToRefresh()
        gate.complete(Unit)
        assertEquals(listOf("AAPL"), source.quoted)
    }

    // Step 5: the wiring of the models (CR2).
    @Test
    fun theState_buildsSettingsRow_theEntry_andTheRefreshLine_fromItsOwnFields() {
        val source = FakeSource()
        val vm = typed("abc", source = source)
        val state = vm.state.value
        assertEquals(PriceKeys.row("Test source", keySet = true, ending = "a1b2"), state.settings.priceSource)
        assertEquals(PriceKeys.entry(KeyEntry(text = "abc"), "Test source", keySet = true, ending = "a1b2", saving = false), state.keyEntryModel)
        assertEquals(PriceRefreshModel(refreshing = false, failed = false, noKey = false), state.priceRefresh)
        vm.cancelKeyEntry()
        assertNull(vm.state.value.keyEntryModel)
    }

    @Test
    fun withoutAKey_theRowSaysSo_andAFailedRefreshsLineNamesTheKey_onHoldingDetailToo() {
        val source = FakeSource().also { it.quotes = mapOf("AAPL" to Quote.NoKey) }
        val vm = started(store(key = null), source = source)
        assertEquals(PriceKeys.row("Test source", keySet = false, ending = ""), vm.state.value.settings.priceSource)
        assertEquals(PriceRefreshModel(refreshing = false, failed = true, noKey = true), vm.state.value.priceRefresh)
        vm.selectTab(Tab.PORTFOLIOS)
        vm.openPortfolio("p1")
        vm.openHolding("AAPL")
        val header = vm.state.value.holdingHeader
        assertNotNull(header)
        assertTrue(header!!.refreshFailed)
        assertTrue(header.refreshNoKey)
    }

    @Test
    fun whileTheKeyIsChecked_theEntryIsBusy() {
        val source = FakeSource().also { it.keyGate = CompletableDeferred() }
        val vm = typed(newKey, source = source)
        vm.saveKey()
        assertTrue(vm.state.value.keyEntryModel!!.busy)
        assertFalse(vm.state.value.keyEntryModel!!.saveEnabled)
    }

    @Test
    fun anUnlockWhileTheCheckRuns_dropsTheEntry_andARefusalDoesNotBringItBack() {
        val gate = CompletableDeferred<Unit>()
        val source = FakeSource().also {
            it.keyGate = gate
            it.keyCheck = KeyCheck.Refused(401)
        }
        val vm = typed(newKey, source = source)
        vm.saveKey()
        vm.onUnlocked()
        assertNull(vm.state.value.keyEntry)
        gate.complete(Unit)
        assertNull(vm.state.value.keyEntry)
        assertFalse(vm.state.value.saving)
        assertFalse(vm.state.value.toString().contains(newKey))
    }

    @Test
    fun anUnlock_aTabChange_andLeavingSettings_dropTheEntryAndItsText() {
        val unlocked = typed(newKey)
        unlocked.onUnlocked()
        assertNull(unlocked.state.value.keyEntry)
        val tabbed = typed(newKey)
        tabbed.selectTab(Tab.CASH)
        assertNull(tabbed.state.value.keyEntry)
        val left = typed(newKey)
        left.closeSettings()
        assertNull(left.state.value.keyEntry)
        assertFalse(left.state.value.toString().contains(newKey))
    }
}
