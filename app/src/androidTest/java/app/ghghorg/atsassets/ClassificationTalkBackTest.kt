package app.ghghorg.atsassets

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.ghghorg.atsassets.core.AssetType
import app.ghghorg.atsassets.core.ClassList
import app.ghghorg.atsassets.core.ClassLists
import app.ghghorg.atsassets.core.ClassOverride
import app.ghghorg.atsassets.core.Instrument
import app.ghghorg.atsassets.core.Portfolio
import app.ghghorg.atsassets.core.SourceProfile
import app.ghghorg.atsassets.core.SplitRatio
import app.ghghorg.atsassets.core.Tab
import app.ghghorg.atsassets.core.Transaction
import app.ghghorg.atsassets.core.TransactionType
import app.ghghorg.atsassets.ui.AppState
import app.ghghorg.atsassets.ui.HoldingActions
import app.ghghorg.atsassets.ui.HoldingDetailScreen
import app.ghghorg.atsassets.ui.HoldingTab
import app.ghghorg.atsassets.ui.Screen
import app.ghghorg.atsassets.ui.theme.ATSAssetSystemTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Holding detail's Classification tab (23a, AC-61 (d), AC-12): a source value reads with "from the source", a missing
 * one reads "unclassified"; each split reads its date and ratio, a reversed one says "reversed"; no split reads the plain
 * line; and the tab is the third, after Transactions and Lots, and opens. From 24: an override reads "override", each row
 * has its own Edit and only an overridden one Clear override, each naming its list (decision 2), and each calls its list.
 */
@RunWith(AndroidJUnit4::class)
class ClassificationTalkBackTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun text(
        id: Int,
        vararg args: Any,
    ): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private fun transaction(
        id: String,
        type: TransactionType,
        date: String,
        seq: Long,
        quantity: String = "0",
        totalCents: Long = 0L,
        split: SplitRatio? = null,
        reversesId: String? = null,
    ): Transaction = Transaction(
        id = id,
        portfolioId = "p1",
        ticker = "AAPL",
        type = type,
        date = LocalDate.parse(date),
        enteredSeq = seq,
        quantity = BigDecimal(quantity),
        totalCents = totalCents,
        split = split,
        reversesId = reversesId,
    )

    private val buy = transaction("b1", TransactionType.BUY, "2026-01-02", 2L, "10", 150_000L)

    // 10 AAPL in Portfolio P; the source gave "technology" and no country; the sector list holds "Technology".
    // 24: the geography list holds "Global", so an override can name it.
    private fun state(
        transactions: List<Transaction>,
        holdingTab: HoldingTab = HoldingTab.CLASSIFICATION,
        overrides: List<ClassOverride> = emptyList(),
    ): AppState = AppState(
        screen = Screen.HOLDING,
        tab = Tab.PORTFOLIOS,
        transactions = transactions,
        today = LocalDate.of(2026, 6, 1),
        portfolios = listOf(Portfolio("p1", "Portfolio P", 1L)),
        portfolioId = "p1",
        holdingTicker = "AAPL",
        holdingTab = holdingTab,
        instruments = listOf(Instrument("AAPL", AssetType.STOCK, verified = true, profile = SourceProfile("technology", null))),
        classLists = ClassLists(sectors = listOf("Technology"), geographies = listOf("Global")),
        overrides = overrides,
    )

    private val globalOverride = ClassOverride("p1", "AAPL", ClassList.GEOGRAPHY, "Global")

    private fun button(id: Int): SemanticsNodeInteraction = composeRule.onNode(hasContentDescription(text(id)) and hasClickAction())

    private fun show(state: AppState) {
        composeRule.setContent {
            ATSAssetSystemTheme { HoldingDetailScreen(state, HoldingActions({}, {}, {}, {}, {}, {}, {})) }
        }
    }

    @Test
    fun aSourceValue_readsFromTheSource_andAMissingOne_readsUnclassified() {
        show(state(listOf(buy)))
        composeRule.onNode(hasContentDescription(text(R.string.class_row_spoken, text(R.string.class_sector), "Technology"))).assertExists()
        composeRule.onNode(hasContentDescription(text(R.string.class_row_unclassified_spoken, text(R.string.class_geography)))).assertExists()
    }

    @Test
    fun eachSplit_readsItsDateAndRatio_aReversedOneSaysReversed() {
        val transactions =
            listOf(
                buy,
                transaction("s1", TransactionType.SPLIT, "2026-03-01", 3L, split = SplitRatio(2, 1)),
                transaction("r1", TransactionType.REVERSAL, "2026-03-02", 4L, reversesId = "s1"),
                transaction("s2", TransactionType.SPLIT, "2026-05-01", 5L, split = SplitRatio(3, 1)),
            )
        show(state(transactions))
        val reversed = text(R.string.split_history_reversed_spoken, "2026-03-01", text(R.string.split_ratio, "2", "1"))
        val kept = text(R.string.holding_transaction_split_spoken, "2026-05-01", text(R.string.split_ratio, "3", "1"))
        composeRule.onNode(hasContentDescription(reversed)).assertExists()
        composeRule.onNode(hasContentDescription(kept)).assertExists()
        composeRule.onNodeWithText(text(R.string.split_history_none)).assertDoesNotExist()
    }

    @Test
    fun noSplit_readsThePlainLine() {
        show(state(listOf(buy)))
        composeRule.onNodeWithText(text(R.string.split_history_none)).assertExists()
    }

    @Test
    fun theThirdTab_isClassification_andOpens() {
        var shown by mutableStateOf(state(listOf(buy), HoldingTab.TRANSACTIONS))
        composeRule.setContent {
            ATSAssetSystemTheme {
                HoldingDetailScreen(shown, HoldingActions({}, { shown = shown.copy(holdingTab = it) }, {}, {}, {}, {}, {}))
            }
        }
        val labels = listOf(R.string.holding_tab_transactions, R.string.holding_tab_lots, R.string.holding_tab_classification)
        val lefts = labels.map { composeRule.onNode(isSelectable() and hasText(text(it))).fetchSemanticsNode().boundsInRoot.left }
        assertTrue("Transactions, Lots, Classification from left to right: $lefts", lefts[0] < lefts[1] && lefts[1] < lefts[2])
        val classification = composeRule.onNode(isSelectable() and hasText(text(R.string.holding_tab_classification)))
        classification.performClick()
        classification.assertIsSelected()
        composeRule.onNode(hasContentDescription(text(R.string.class_row_spoken, text(R.string.class_sector), "Technology"))).assertExists()
    }

    @Test
    fun anOverride_readsOverride_andOnlyItsRowHasClearOverride() {
        show(state(listOf(buy), overrides = listOf(globalOverride)))
        composeRule.onNode(hasContentDescription(text(R.string.class_row_override_spoken, text(R.string.class_geography), "Global"))).assertExists()
        composeRule.onNode(hasContentDescription(text(R.string.class_row_spoken, text(R.string.class_sector), "Technology"))).assertExists()
        button(R.string.class_edit_sector_spoken).assertExists()
        button(R.string.class_edit_geography_spoken).assertExists()
        button(R.string.class_clear_geography_spoken).assertExists()
        button(R.string.class_clear_sector_spoken).assertDoesNotExist()
    }

    @Test
    fun editAndClearOverride_nameTheirOwnList() {
        val edited = mutableListOf<ClassList>()
        val cleared = mutableListOf<ClassList>()
        val actions = HoldingActions({}, {}, {}, {}, {}, {}, {}, onEditClass = { edited += it }, onClearClass = { cleared += it })
        composeRule.setContent { ATSAssetSystemTheme { HoldingDetailScreen(state(listOf(buy), overrides = listOf(globalOverride)), actions) } }
        button(R.string.class_edit_sector_spoken).performScrollTo().performClick()
        button(R.string.class_edit_geography_spoken).performScrollTo().performClick()
        button(R.string.class_clear_geography_spoken).performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals("Edit sector, then Edit geography", listOf(ClassList.SECTOR, ClassList.GEOGRAPHY), edited)
        assertEquals("Clear geography override", listOf(ClassList.GEOGRAPHY), cleared)
    }
}
