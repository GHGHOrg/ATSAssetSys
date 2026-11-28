package app.ghghorg.atsassets.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

// 44a step 7 (spec 6, Date fields: the date picker; D-18, D-28): what the picker shows is worked out here, not in the
// screen (CR2). 2026-11-01 and 2026-02-01 are Sundays; 2026-08-01 is a Saturday.
class DatePickerModelTest {
    private val today = LocalDate.of(2026, 11, 25)

    @Test
    fun opensOnTheTypedDatesMonth_withDashesOrDigits() {
        assertEquals(YearMonth.of(2026, 3), DatePicker.open("2026-03-01", today, dense = false).month)
        assertEquals(YearMonth.of(2025, 12), DatePicker.open("20251219", today, dense = true).month)
        assertEquals(LocalDate.of(2025, 12, 19), DatePicker.open("20251219", today, dense = true).picked)
    }

    @Test
    fun opensOnTodaysMonth_whenTheFieldIsEmptyOrNotADate() {
        for (text in listOf("", "2026-13-40", "2026", "abc")) {
            val model = DatePicker.open(text, today, dense = false)
            assertEquals(text, YearMonth.of(2026, 11), model.month)
            assertNull(text, model.picked)
        }
    }

    @Test
    fun weeksStartOnSunday_withNoDayOutsideTheMonth() {
        val november = DatePicker.open("", today, dense = false)
        assertEquals(5, november.weeks.size)
        assertEquals(LocalDate.of(2026, 11, 1), november.weeks.first().first())
        assertEquals(listOf(LocalDate.of(2026, 11, 29), LocalDate.of(2026, 11, 30), null, null, null, null, null), november.weeks.last())

        val february = DatePicker.open("2026-02-10", today, dense = false)
        assertEquals(4, february.weeks.size)
        assertEquals(LocalDate.of(2026, 2, 28), february.weeks.last().last())

        val august = DatePicker.open("2026-08-10", today, dense = false)
        assertEquals(6, august.weeks.size)
        assertEquals(listOf(null, null, null, null, null, null, LocalDate.of(2026, 8, 1)), august.weeks.first())
        august.weeks.forEach { assertEquals(7, it.size) }
    }

    @Test
    fun movesByMonth_acrossYears() {
        val november = DatePicker.open("", today, dense = false)
        assertEquals(YearMonth.of(2026, 12), DatePicker.move(november, 1).month)
        assertEquals(YearMonth.of(2027, 1), DatePicker.move(DatePicker.move(november, 1), 1).month)
        assertEquals(YearMonth.of(2025, 12), DatePicker.move(november, -11).month)
    }

    @Test
    fun titleAndWeekdays_areFixedUsEnglish() {
        assertEquals("November 2026", DatePicker.open("", today, dense = false).title)
        assertEquals(listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"), DatePicker.open("", today, dense = false).weekdays)
        assertEquals(listOf("S", "M", "T", "W", "T", "F", "S"), DatePicker.open("", today, dense = true).weekdays)
    }

    @Test
    fun todayAndThePick_areMarked_andMovingKeepsThePick() {
        val model = DatePicker.pick(DatePicker.open("", today, dense = false), LocalDate.of(2026, 11, 19))
        assertEquals(LocalDate.of(2026, 11, 19), model.picked)
        assertEquals(today, model.today)
        assertEquals(LocalDate.of(2026, 11, 19), DatePicker.move(model, 2).picked)
    }

    @Test
    fun thePickGoesBack_inTheFieldsOwnForm() {
        val day = LocalDate.of(2026, 3, 1)
        assertEquals("2026-03-01", DatePicker.fieldText(day, digitsOnly = false))
        assertEquals("20260301", DatePicker.fieldText(day, digitsOnly = true))
    }

    @Test
    fun aDaysNumber_andThePickOnTheConfirmButton() {
        assertEquals("1", DatePicker.dayText(LocalDate.of(2026, 11, 1)))
        assertEquals("30", DatePicker.dayText(LocalDate.of(2026, 11, 30)))
        val open = DatePicker.open("", today, dense = false)
        assertEquals("", DatePicker.pickedText(open))
        assertEquals("2026-11-19", DatePicker.pickedText(DatePicker.pick(open, LocalDate.of(2026, 11, 19))))
    }

    @Test
    fun aDayIsSpoken_byItsWeekdayAndItsDate() {
        assertEquals("Sunday" to "2026-11-01", DatePicker.spoken(LocalDate.of(2026, 11, 1)))
        assertEquals("Saturday" to "2026-08-01", DatePicker.spoken(LocalDate.of(2026, 8, 1)))
    }
}
