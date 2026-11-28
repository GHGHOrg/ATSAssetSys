package app.ghghorg.atsassets.ui.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import app.ghghorg.atsassets.ui.theme.AtsIcon
import app.ghghorg.atsassets.ui.theme.AtsTheme
import app.ghghorg.atsassets.ui.theme.LucideIcon
import app.ghghorg.atsassets.ui.theme.iconSize
import app.ghghorg.atsassets.ui.theme.tokenDp
import app.ghghorg.atsassets.ui.theme.tokenNumber
import app.ghghorg.atsassets.ui.theme.typeStyle

// 44a step 6 (spec 6, Visual design (1), (3) and (6); D-17, D-34, D-36): the catalog's buttons. Gold fills only the one
// main action on a screen; the others are outline or text buttons. A button that cannot be tapped yet is drawn at 38% and
// keeps its solid outline; TalkBack reads it as unavailable, and the screen says why on a line of its own.

/** [OUTLINE_ON_SUMMARY]: an outline button on the summary card, in its own line and text colors (Cash entry; drawing 07). */
internal enum class ButtonKind { FILLED, OUTLINE, TEXT, OUTLINE_ON_SUMMARY }

internal enum class ButtonSize { LARGE, MEDIUM, SMALL }

private val ButtonSize.height get() = tokenDp(
    when (this) {
        ButtonSize.LARGE -> "buttonLargeHeight"
        ButtonSize.MEDIUM -> "buttonMediumHeight"
        ButtonSize.SMALL -> "buttonSmallHeight"
    },
)

private fun ButtonSize.padding(kind: ButtonKind) = tokenDp(
    when {
        kind == ButtonKind.TEXT -> "buttonTextPadding"
        this == ButtonSize.LARGE -> "buttonLargePadding"
        this == ButtonSize.MEDIUM -> "buttonMediumPadding"
        else -> "buttonSmallPadding"
    },
)

private val ButtonSize.textStyle: TextStyle
    get() = when (this) {
        ButtonSize.LARGE -> typeStyle("sectionHeading")
        ButtonSize.MEDIUM -> typeStyle("button")
        ButtonSize.SMALL -> typeStyle("tab")
    }

/** The look of a control that cannot be tapped yet: 38% (spec 6, Visual design (6)). */
internal fun Modifier.unavailable(enabled: Boolean): Modifier = if (enabled) this else alpha(tokenNumber("opacityDisabled"))

@Composable
internal fun AtsButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.OUTLINE,
    size: ButtonSize = ButtonSize.MEDIUM,
    enabled: Boolean = true,
    fillWidth: Boolean = false,
    icon: LucideIcon? = null,
) {
    val colors = AtsTheme.colors
    val shape = RoundedCornerShape(tokenDp("radiusDefault"))
    val ink = when (kind) {
        ButtonKind.FILLED -> colors.onGold
        ButtonKind.OUTLINE_ON_SUMMARY -> colors.buttonOnSummaryText
        else -> colors.buttonText
    }
    val fill = if (kind == ButtonKind.FILLED) colors.gold else Color.Transparent
    val border = when (kind) {
        ButtonKind.OUTLINE -> BorderStroke(tokenDp("buttonOutline"), colors.buttonLine)
        ButtonKind.OUTLINE_ON_SUMMARY -> BorderStroke(tokenDp("buttonOutline"), colors.buttonOnSummaryLine)
        else -> null
    }
    // The touch area is at least 48 dp tall, also around a small button (D-17).
    Box(
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .heightIn(min = tokenDp("spaceTouch"))
            .clip(shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
                .unavailable(enabled)
                .heightIn(min = size.height)
                .clip(shape)
                .background(fill)
                .then(if (border != null) Modifier.border(border, shape) else Modifier)
                .padding(horizontal = size.padding(kind)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(tokenDp("chipGap"), Alignment.CenterHorizontally),
        ) {
            if (icon != null) AtsIcon(icon, tint = ink, size = tokenDp("fieldMarkSize"))
            Text(text = text, style = size.textStyle, color = ink, textAlign = TextAlign.Center)
        }
    }
}

/** A 48 dp square button with a 24 dp icon and no outline; [label] names the action for TalkBack (D-36). */
@Composable
internal fun AtsIconButton(
    icon: LucideIcon,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = AtsTheme.colors.iconAction,
) {
    Box(
        modifier = modifier
            .size(tokenDp("spaceTouch"))
            .clip(RoundedCornerShape(tokenDp("radiusDefault")))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label }
            .unavailable(enabled),
        contentAlignment = Alignment.Center,
    ) {
        AtsIcon(icon, tint = tint, modifier = Modifier.widthIn(max = iconSize))
    }
}
