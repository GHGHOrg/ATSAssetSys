package app.ghghorg.atsassets

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.ghghorg.atsassets.ui.CalendarButton
import app.ghghorg.atsassets.ui.DatePickerLayerHost
import app.ghghorg.atsassets.ui.LocalDatePicker
import app.ghghorg.atsassets.ui.opener
import app.ghghorg.atsassets.ui.rememberDatePickerState
import app.ghghorg.atsassets.ui.theme.ATSAssetSystemTheme
import app.ghghorg.atsassets.ui.theme.TokenColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

private val TODAY: LocalDate = LocalDate.of(2026, 11, 25)

/**
 * The date picker (44a step 7; spec 6, Date fields: the date picker; AC-61 (b), (d) and (e); AC-73): the calendar icon opens
 * it on the field's month, drawn inside the window so the activity keeps its focus; the standard picker needs "Use <date>"
 * and writes the field's own form back, Cancel changes nothing; the dense picker picks and closes on a tap; TalkBack reads
 * each day by its weekday and date, and today as today; the picked day is filled in the selected color.
 */
@RunWith(AndroidJUnit4::class)
class DatePickerScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var written: String? = null

    @Composable
    private fun Field(
        start: String,
        dense: Boolean,
        digitsOnly: Boolean,
    ) {
        val picker = rememberDatePickerState()
        var text by remember { mutableStateOf(start) }
        Box(modifier = Modifier.fillMaxSize()) {
            CompositionLocalProvider(LocalDatePicker provides opener(picker, TODAY)) {
                val write: (String) -> Unit = { picked ->
                    text = picked
                    written = picked
                }
                CalendarButton(text, write, dense = dense, digitsOnly = digitsOnly)
            }
            DatePickerLayerHost(picker)
        }
    }

    private fun show(
        start: String,
        dense: Boolean = false,
        digitsOnly: Boolean = true,
    ) {
        composeRule.setContent { ATSAssetSystemTheme { Field(start, dense, digitsOnly) } }
        composeRule.onNode(hasContentDescription(string(R.string.date_pick_open)) and hasClickAction()).performClick()
        composeRule.waitForIdle()
    }

    private fun day(
        weekday: String,
        date: String,
        today: Boolean = false,
    ) = composeRule.onNode(
        hasContentDescription(
            InstrumentationRegistryText.text(if (today) R.string.date_pick_today else R.string.date_pick_day, weekday, date),
        ) and hasClickAction(),
    )

    @Test
    fun opensOnTheFieldsMonth_insideTheWindow() {
        show("20260301")
        composeRule.onNode(isHeading() and hasText("March 2026")).assertExists()
        assertTrue("The picker is drawn inside the window", composeRule.runOnUiThread { composeRule.activity.hasWindowFocus() })
    }

    @Test
    fun standardPicker_needsUse_andWritesTheFieldsOwnForm() {
        show("20261102")
        val use = { date: String -> composeRule.onNode(hasText(InstrumentationRegistryText.text(R.string.date_pick_use, date)) and hasClickAction()) }
        day("Thursday", "2026-11-19").performClick()
        composeRule.waitForIdle()
        day("Thursday", "2026-11-19").assertIsSelected()
        use("2026-11-19").assertIsEnabled().performClick()
        composeRule.waitForIdle()
        assertEquals("20261119", written)
        composeRule.onNode(isHeading() and hasText("November 2026")).assertDoesNotExist()
    }

    @Test
    fun standardPicker_withNothingPicked_cannotUse_andCancelChangesNothing() {
        show("")
        composeRule.onNode(hasText(InstrumentationRegistryText.text(R.string.date_pick_use, "")) and hasClickAction()).assertIsNotEnabled()
        composeRule.onNode(hasText(string(R.string.cancel_button)) and hasClickAction()).performClick()
        composeRule.waitForIdle()
        assertEquals(null, written)
        composeRule.onNode(isHeading() and hasText("November 2026")).assertDoesNotExist()
    }

    @Test
    fun densePicker_picksAndClosesOnATap_inTheDashedForm() {
        show("", dense = true, digitsOnly = false)
        composeRule.onNode(hasContentDescription(string(R.string.date_pick_previous)) and hasClickAction()).performClick()
        composeRule.waitForIdle()
        composeRule.onNode(isHeading() and hasText("October 2026")).assertExists()
        day("Saturday", "2026-10-31").performClick()
        composeRule.waitForIdle()
        assertEquals("2026-10-31", written)
        composeRule.onNode(isHeading() and hasText("October 2026")).assertDoesNotExist()
    }

    @Test
    fun todayIsReadAsToday_andThePickIsFilled() {
        show("20261119")
        day("Wednesday", "2026-11-25", today = true).assertExists()
        val picked = day("Thursday", "2026-11-19").assertIsSelected().fetchSemanticsNode().boundsInRoot
        // A few dp in from the picked cell's left edge, inside its 40 dp fill: the selected color.
        val fill = composeRule.onRoot().colorAt(picked.center.x - with(composeRule.density) { 14.dp.toPx() }, picked.center.y)
        assertTrue("Fill ${describe(fill)}, expected ${describe(TokenColors.light.chipOn)}", sameColor(fill, TokenColors.light.chipOn))
    }
}

private object InstrumentationRegistryText {
    fun text(
        id: Int,
        vararg args: Any,
    ): String = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)
}

private val Int.dp get() = androidx.compose.ui.unit.Dp(this.toFloat())
