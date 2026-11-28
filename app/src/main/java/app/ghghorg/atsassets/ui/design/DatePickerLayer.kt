package app.ghghorg.atsassets.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.ui.DatePicker
import app.ghghorg.atsassets.ui.DatePickerModel
import app.ghghorg.atsassets.ui.theme.AtsTheme
import app.ghghorg.atsassets.ui.theme.LucideIcon
import app.ghghorg.atsassets.ui.theme.tokenDp
import app.ghghorg.atsassets.ui.theme.typeStyle
import java.time.LocalDate

/** What the picker's buttons and days do; the host decides what a pick means for its field (DatePickerHost). */
internal class DatePickerActions(
    val onMove: (Long) -> Unit,
    val onDay: (LocalDate) -> Unit,
    val onConfirm: () -> Unit,
    val onCancel: () -> Unit,
)

/**
 * The date picker (44a step 7; spec 6, Date fields: the date picker; D-18, D-28), drawn inside the window over a scrim,
 * never as a separate window. Standard: the New York note, ‹ month ›, the days, Cancel and "Use <date>". Dense (a filter's
 * From and To): one row of ‹ month › ✕ and the days, a tap on a day picks it and closes. Days are 48 apart each way; today
 * in New York is outlined, the picked day filled. All it shows comes from [model] (DatePicker, CR2).
 */
@Composable
internal fun AtsDatePicker(
    model: DatePickerModel,
    actions: DatePickerActions,
) {
    val colors = AtsTheme.colors
    val shape = RoundedCornerShape(tokenDp("radiusDefault"))
    val dense = model.dense
    AtsScrim(onDismiss = actions.onCancel) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = tokenDp(if (dense) "pickerDenseTop" else "pickerTop"))
                .padding(horizontal = if (dense) tokenDp("pickerDensePadding") else tokenDp("spaceMargin"))
                .fillMaxWidth()
                .cardShadow()
                .clip(shape)
                .background(colors.float)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                .padding(tokenDp(if (dense) "pickerDensePadding" else "pickerPadding")),
            verticalArrangement = Arrangement.spacedBy(tokenDp(if (dense) "cardGap" else "spaceGap")),
        ) {
            if (!dense) Text(stringResource(R.string.date_note), style = typeStyle("note"), color = colors.textSecondary)
            Row(modifier = Modifier.fillMaxWidth().heightIn(min = tokenDp("spaceTouch")), verticalAlignment = Alignment.CenterVertically) {
                AtsIconButton(LucideIcon.CHEVRON_LEFT, stringResource(R.string.date_pick_previous), { actions.onMove(-1) })
                Text(
                    model.title,
                    style = typeStyle(if (dense) "small" else "sectionHeading"),
                    fontWeight = FontWeight.W500,
                    color = colors.text,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                AtsIconButton(LucideIcon.CHEVRON_RIGHT, stringResource(R.string.date_pick_next), { actions.onMove(1) })
                if (dense) AtsIconButton(LucideIcon.X, stringResource(R.string.close_button), actions.onCancel, tint = colors.iconChevron)
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                model.weekdays.forEach { head ->
                    Text(head, style = typeStyle("dayHead"), color = colors.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.weight(1f).clearAndSetSemantics { })
                }
            }
            model.weeks.forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { day ->
                        Box(modifier = Modifier.weight(1f).heightIn(min = tokenDp("spaceTouch")), contentAlignment = Alignment.Center) {
                            if (day != null) Day(day, model, actions)
                        }
                    }
                }
            }
            if (!dense) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(tokenDp("fieldDensePadding"), Alignment.End)) {
                    AtsButton(stringResource(R.string.cancel_button), actions.onCancel, kind = ButtonKind.TEXT)
                    AtsButton(
                        text = stringResource(R.string.date_pick_use, DatePicker.pickedText(model)),
                        onClick = actions.onConfirm,
                        kind = ButtonKind.FILLED,
                        enabled = model.picked != null,
                    )
                }
            }
        }
    }
}

@Composable
private fun Day(
    day: LocalDate,
    model: DatePickerModel,
    actions: DatePickerActions,
) {
    val colors = AtsTheme.colors
    val shape = RoundedCornerShape(tokenDp("radiusDefault"))
    val picked = day == model.picked
    val today = day == model.today
    val (weekday, date) = DatePicker.spoken(day)
    val label = stringResource(if (today) R.string.date_pick_today else R.string.date_pick_day, weekday, date)
    Box(
        modifier = Modifier
            .size(tokenDp("spaceTouch"))
            .selectable(selected = picked, role = Role.Button, onClick = { actions.onDay(day) })
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(tokenDp(if (model.dense) "pickerDenseCell" else "pickerCell"))
                .clip(shape)
                .background(if (picked) colors.chipOn else Color.Transparent)
                .then(if (today && !picked) Modifier.border(tokenDp("buttonOutline"), colors.buttonLine, shape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                DatePicker.dayText(day),
                style = typeStyle("small"),
                fontWeight = if (picked) FontWeight.W500 else null,
                color = if (picked) colors.onChipOn else colors.text,
                modifier = Modifier.clearAndSetSemantics { },
            )
        }
    }
}
