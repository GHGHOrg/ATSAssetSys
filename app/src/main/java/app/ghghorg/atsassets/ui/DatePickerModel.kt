package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.MarketDate
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * The date picker as it shows (44a step 7; spec 6, Date fields: the date picker; D-18, D-28): the month, its title, the
 * weekday heads and the weeks, Sunday first, with null where a cell is outside the month; today in New York and the
 * picked day. One fixed US English, whatever the phone's language (spec 6, Number format and color).
 */
internal data class DatePickerModel(
    val month: YearMonth,
    val title: String,
    val weekdays: List<String>,
    val weeks: List<List<LocalDate?>>,
    val today: LocalDate,
    val picked: LocalDate?,
    val dense: Boolean,
)

internal object DatePicker {
    private const val DAYS_IN_WEEK = 7
    private val TITLE: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)
    private val SUNDAY_FIRST = listOf(DayOfWeek.SUNDAY) + DayOfWeek.entries.filter { it != DayOfWeek.SUNDAY }

    /** Opens on the month of the date in the field (YYYY-MM-DD, or the filters' eight digits), else on today's (New York). */
    fun open(
        text: String,
        today: LocalDate,
        dense: Boolean,
    ): DatePickerModel {
        val typed = DateDigits.toDate(text) ?: MarketDate.parse(text)
        return build(YearMonth.from(typed ?: today), today, typed, dense)
    }

    /** The month [by] months on (negative: back), keeping today and the pick. */
    fun move(
        model: DatePickerModel,
        by: Long,
    ): DatePickerModel = build(model.month.plusMonths(by), model.today, model.picked, model.dense)

    fun pick(
        model: DatePickerModel,
        date: LocalDate,
    ): DatePickerModel = model.copy(picked = date)

    /** The pick as the field holds it: YYYY-MM-DD, or eight digits in a filter's field (DateDigits). */
    fun fieldText(
        date: LocalDate,
        digitsOnly: Boolean,
    ): String = if (digitsOnly) date.toString().replace("-", "") else date.toString()

    /** A day's number in its cell. */
    fun dayText(date: LocalDate): String = date.dayOfMonth.toString()

    /** The pick as the confirm button shows it (YYYY-MM-DD), or empty while nothing is picked. */
    fun pickedText(model: DatePickerModel): String = model.picked?.toString() ?: ""

    /** A day for TalkBack: its weekday and its date, put in the approved sentence by the screen (D9). */
    fun spoken(date: LocalDate): Pair<String, String> = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.US) to date.toString()

    private fun build(
        month: YearMonth,
        today: LocalDate,
        picked: LocalDate?,
        dense: Boolean,
    ): DatePickerModel {
        val lead = SUNDAY_FIRST.indexOf(month.atDay(1).dayOfWeek)
        val cells: List<LocalDate?> = List(lead) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
        val weeks = cells.chunked(DAYS_IN_WEEK).map { week -> week + List(DAYS_IN_WEEK - week.size) { null } }
        val heads =
            SUNDAY_FIRST.map { day ->
                val short = day.getDisplayName(TextStyle.SHORT, Locale.US)
                if (dense) short.take(1) else short.take(2)
            }
        return DatePickerModel(month, month.format(TITLE), heads, weeks, today, picked, dense)
    }
}
