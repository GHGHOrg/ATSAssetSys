package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.CashType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// 15d: the Cash list's model reaches the state, is kept while nothing it reads changes, and follows a filter and a save.
@OptIn(ExperimentalCoroutinesApi::class)
class CashListFlowTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun AppState.freshCashList(): CashListModel = CashList.of(entries, transactions, portfolios, cashFilterForm)

    @Test
    fun theModel_reachesTheState_andOpeningTheCashTab_onlyReadsIt() {
        val vm = started(FakeStore(stored = listOf(opening())))
        val loaded = vm.state.value.cashList
        assertEquals(vm.state.value.freshCashList(), loaded)
        assertEquals(listOf("o1"), loaded.rows.map { it.key })
        vm.selectTab(Tab.CASH)
        assertSame(loaded, vm.state.value.cashList)
    }

    @Test
    fun aFilterChange_givesAFreshModel() {
        val vm = started(FakeStore(stored = listOf(opening())))
        vm.selectTab(Tab.CASH)
        val before = vm.state.value.cashList
        vm.onFilterTypeChange(CashType.DEPOSIT)
        assertNotSame(before, vm.state.value.cashList)
        assertEquals(vm.state.value.freshCashList(), vm.state.value.cashList)
        assertTrue(vm.state.value.cashList.filterActive)
        assertEquals(emptyList<CashRowModel>(), vm.state.value.cashList.rows)
    }

    @Test
    fun aSavedDeposit_showsAsANewRow_withTheBalanceAfterIt() {
        val vm = started(FakeStore(stored = listOf(opening())), newId = { "d1" })
        vm.selectTab(Tab.CASH)
        vm.openForm()
        vm.onDateChange("2026-03-01")
        vm.onAmountChange("500.00")
        vm.review()
        vm.confirm()
        val model = vm.state.value.cashList
        assertEquals(vm.state.value.freshCashList(), model)
        assertEquals(listOf("d1", "o1"), model.rows.map { it.key })
        assertEquals("$10,500.00", model.rows.first().balanceAfter)
        assertEquals("$10,500.00", model.balance)
    }
}
