package app.ghghorg.atsassets.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import app.ghghorg.atsassets.ui.theme.AtsIcon
import app.ghghorg.atsassets.ui.theme.AtsTheme
import app.ghghorg.atsassets.ui.theme.LucideIcon
import app.ghghorg.atsassets.ui.theme.tokenDp
import app.ghghorg.atsassets.ui.theme.typeStyle

// 44a step 6 (spec 6, Visual design; D-17, D-18, D-29 to D-31, D-36): chips, checkboxes, radio buttons and the tabs inside
// a screen. Each row or chip is its own touch target, at least 48 dp; a choice that cannot be made yet is drawn at 38%.

/**
 * A filter chip, or one choice of several (D-31): selected shows a check and the selected colors (D-17). Selectable with
 * the checkbox role, as Material's filter chip was, so TalkBack and the screen tests read it as before (44a step 8 group
 * (c); CashTickerScreenTest, TransactionHistoryScreenTest).
 */
@Composable
internal fun AtsChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = AtsTheme.colors
    val shape = RoundedCornerShape(tokenDp("radiusDefault"))
    val ink = if (selected) colors.onChipOn else colors.buttonText
    Box(
        modifier = modifier
            .heightIn(min = tokenDp("spaceTouch"))
            .selectable(selected = selected, enabled = enabled, role = Role.Checkbox, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .unavailable(enabled)
                .height(tokenDp("chipHeight"))
                .clip(shape)
                .background(if (selected) colors.chipOn else Color.Transparent)
                .border(tokenDp("buttonOutline"), if (selected) colors.chipOn else colors.buttonLine, shape)
                .padding(horizontal = tokenDp("chipPadding")),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(tokenDp("chipGap")),
        ) {
            if (selected) AtsIcon(LucideIcon.CHECK, tint = ink, size = tokenDp("chipCheckSize"))
            Text(text, style = typeStyle("tab"), color = ink)
        }
    }
}

/** A checkbox row: the whole row toggles it (D-18). Dense: a 16 box with a 1.5 outline and Asap 14. */
@Composable
internal fun AtsCheckRow(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    dense: Boolean = false,
) {
    val colors = AtsTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = tokenDp("spaceTouch"))
            .toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = onCheckedChange)
            .unavailable(enabled)
            .padding(horizontal = tokenDp("spaceInset")),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokenDp(if (dense) "fieldDensePadding" else "rowControlGap")),
    ) {
        AtsCheckBox(checked, dense)
        Text(text, style = typeStyle(if (dense) "small" else "body"), color = colors.text)
    }
}

/**
 * The checkbox square alone (D-18): 18 with a 2 outline, or 16 with 1.5 when [dense]; checked, filled with the check
 * color and Lucide's check. Its row carries the toggle and what TalkBack reads (a lot on the sell screen; 44a step 8 (d)).
 */
@Composable
internal fun AtsCheckBox(
    checked: Boolean,
    dense: Boolean = false,
) {
    val colors = AtsTheme.colors
    val shape = RoundedCornerShape(tokenDp("radiusCheckbox"))
    Box(
        modifier = Modifier
            .size(tokenDp(if (dense) "checkboxDenseSize" else "checkboxSize"))
            .clip(shape)
            .background(if (checked) colors.check else Color.Transparent)
            .border(tokenDp(if (dense) "checkboxDenseOutline" else "checkboxOutline"), if (checked) colors.check else colors.buttonLine, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) AtsIcon(LucideIcon.CHECK, tint = colors.onCheck, size = tokenDp("checkboxMarkSize"))
    }
}

/** A group of radio rows (D-36): one choice of several, read by TalkBack as selected or not selected. */
@Composable
internal fun AtsRadioGroup(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.selectableGroup()) { content() }
}

/** One radio row: a 20 circle with a 2 outline, a 10 dot when selected; [tag] (the default) in Asap Condensed 12. */
@Composable
internal fun AtsRadioRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String? = null,
    enabled: Boolean = true,
) {
    val colors = AtsTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = tokenDp("spaceTouch"))
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .unavailable(enabled)
            .padding(horizontal = tokenDp("spaceInset")),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokenDp("rowControlGap")),
    ) {
        Box(
            modifier = Modifier
                .size(tokenDp("radioSize"))
                .border(tokenDp("radioOutline"), if (selected) colors.check else colors.buttonLine, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Box(modifier = Modifier.size(tokenDp("radioDot")).clip(CircleShape).background(colors.check))
        }
        Text(text, style = typeStyle("body"), color = colors.text, modifier = Modifier.weight(1f))
        if (tag != null) Text(tag, style = typeStyle("oneDay"), color = colors.textSecondary)
    }
}

/** Tabs inside a screen (Holding detail's Lots, Transactions, Classification): Asap Condensed 14 medium; selected in
 * the button-text color over a 3 dp line (D-17, D-43). */
@Composable
internal fun AtsTabs(
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AtsTheme.colors
    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().selectableGroup()) {
            labels.forEachIndexed { index, label ->
                val on = index == selected
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = tokenDp("spaceTouch"))
                        .selectable(selected = on, role = Role.Tab, onClick = { onSelect(index) }),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    Text(
                        label,
                        style = typeStyle("tab"),
                        fontWeight = if (on) FontWeight.W700 else null,
                        color = if (on) colors.buttonText else colors.textSecondary,
                        modifier = Modifier.padding(vertical = tokenDp("fieldDensePadding")),
                    )
                    Box(
                        modifier = Modifier
                            .width(tokenDp("barPillMinWidth"))
                            .height(tokenDp("barLineHeight"))
                            .clip(RoundedCornerShape(topStart = tokenDp("barLineHeight"), topEnd = tokenDp("barLineHeight")))
                            .background(if (on) colors.buttonText else Color.Transparent),
                    )
                }
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(tokenDp("buttonOutline")).background(colors.divider))
    }
}
