package app.ghghorg.atsassets.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

// 44a step 8 group (b), open item 48 (CR2): Hidden holdings as the screen draws it (view 9, wireframe 09).
class HiddenHoldingsModelTest {
    @Test
    fun eachRow_hasItsTicker_zeroShares_andItsLastTransactionDate() {
        val model =
            HiddenHoldingsView.of(
                portfolioName = "Main",
                rows = listOf(HiddenRow("AAPL", LocalDate.of(2026, 1, 3)), HiddenRow("KO", LocalDate.of(2026, 2, 10))),
                saveFailed = false,
                saving = false,
            )
        assertEquals(
            HiddenHoldingsModel(
                portfolioName = "Main",
                rows = listOf(HiddenHoldingRowModel("AAPL", "0", "2026-01-03"), HiddenHoldingRowModel("KO", "0", "2026-02-10")),
                saveFailed = false,
                canAct = true,
            ),
            model,
        )
    }

    @Test
    fun aRowWithNoTransaction_hasNoDate() {
        val model = HiddenHoldingsView.of("Main", listOf(HiddenRow("OLDCO", null)), saveFailed = false, saving = false)
        assertEquals(listOf(HiddenHoldingRowModel("OLDCO", "0", null)), model.rows)
    }

    @Test
    fun noneHidden_hasNoRows() {
        assertEquals(emptyList<HiddenHoldingRowModel>(), HiddenHoldingsView.of("Main", emptyList(), saveFailed = false, saving = false).rows)
    }

    @Test
    fun whileSaving_nothingCanBeTapped_andAFailedSaveShows() {
        val saving = HiddenHoldingsView.of("Main", emptyList(), saveFailed = false, saving = true)
        assertEquals(false, saving.canAct)
        val failed = HiddenHoldingsView.of("Main", emptyList(), saveFailed = true, saving = false)
        assertEquals(true, failed.saveFailed)
        assertEquals(true, failed.canAct)
    }
}
