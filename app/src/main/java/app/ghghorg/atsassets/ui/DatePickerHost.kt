package app.ghghorg.atsassets.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.res.stringResource
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.ui.design.AtsDatePicker
import app.ghghorg.atsassets.ui.design.AtsIconButton
import app.ghghorg.atsassets.ui.design.DatePickerActions
import app.ghghorg.atsassets.ui.theme.LucideIcon
import java.time.LocalDate

// 44a step 7 (spec 6, Date fields: the date picker): a date field asks the host to open the picker; the host draws it over
// the screen and the bar, inside the window (CLAUDE.md pitfall), and gives the pick back in the field's own form. What is
// open is the screen's, not stored and not in the backup; a pick goes through the field's own change, so every date rule
// stays where it is.

/** A field's request: its text now, whether it is a filter's (dense picker) and whether it holds eight digits. */
internal class DatePickerRequest(
    val text: String,
    val dense: Boolean,
    val digitsOnly: Boolean,
    val onPicked: (String) -> Unit,
)

internal class DatePickerOpen(
    val model: DatePickerModel,
    val request: DatePickerRequest,
)

/** Opens the picker; a screen drawn without the host (a screen test) has none, and its calendar icon does nothing. */
internal val LocalDatePicker = staticCompositionLocalOf<(DatePickerRequest) -> Unit> { {} }

/** The host's state: null while no picker is open. */
@Composable
internal fun rememberDatePickerState(): MutableState<DatePickerOpen?> = remember { mutableStateOf(null) }

/** How a field's request opens the picker on [today] (New York, AppState.today). */
internal fun opener(
    state: MutableState<DatePickerOpen?>,
    today: LocalDate,
): (DatePickerRequest) -> Unit = { request -> state.value = DatePickerOpen(DatePicker.open(request.text, today, request.dense), request) }

/** The open picker, if any; drawn last, over everything, so it and its Back come first. */
@Composable
internal fun DatePickerLayerHost(state: MutableState<DatePickerOpen?>) {
    val open = state.value ?: return
    val close = { state.value = null }
    BackHandler(onBack = close)
    AtsDatePicker(
        model = open.model,
        actions = DatePickerActions(
            onMove = { by -> state.value = DatePickerOpen(DatePicker.move(open.model, by), open.request) },
            onDay = { day ->
                if (open.model.dense) {
                    open.request.onPicked(DatePicker.fieldText(day, open.request.digitsOnly))
                    close()
                } else {
                    state.value = DatePickerOpen(DatePicker.pick(open.model, day), open.request)
                }
            },
            onConfirm = {
                open.model.picked?.let { open.request.onPicked(DatePicker.fieldText(it, open.request.digitsOnly)) }
                close()
            },
            onCancel = close,
        ),
    )
}

/** The calendar icon at the end of a date field: opens the picker on the field's date (spec 6, Date fields). */
@Composable
internal fun CalendarButton(
    text: String,
    onPicked: (String) -> Unit,
    dense: Boolean = false,
    digitsOnly: Boolean = true,
) {
    val open = LocalDatePicker.current
    AtsIconButton(LucideIcon.CALENDAR, stringResource(R.string.date_pick_open), { open(DatePickerRequest(text, dense, digitsOnly, onPicked)) })
}
