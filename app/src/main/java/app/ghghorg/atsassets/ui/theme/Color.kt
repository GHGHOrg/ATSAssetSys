package app.ghghorg.atsassets.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 44a step 4 (CR7; spec 6, Visual design and Number format and color; K9): every color comes from the design tokens
// (design/tokens/, generated into TokenColors), never from a value written here. The palette of the current theme is
// AtsTheme.colors.

/**
 * The color of a gain or loss shown with sign [signum] (section 6): green above 0, red below, and the text's own color
 * at 0. Pass the sign of the shown, rounded figure, so a value shown as $0.00 has no color (4.10).
 */
@Composable
fun gainLossColor(signum: Int): Color {
    val colors = AtsTheme.colors
    return when {
        signum > 0 -> colors.gain
        signum < 0 -> colors.loss
        else -> LocalContentColor.current
    }
}

/**
 * The color of an allocation slice (18): cash's own, otherwise the [index]th of the others in turn, counted without cash,
 * so a portfolio or ticker keeps its color whether cash is listed or not. Every row names its slice, so meaning never
 * rests on the color (section 6, accessibility (5)).
 */
@Composable
fun sliceColor(
    cash: Boolean,
    index: Int,
): Color {
    val colors = AtsTheme.colors
    if (cash) return colors.sliceCash
    val slices = listOf(colors.slice1, colors.slice2, colors.slice3, colors.slice4, colors.slice5, colors.slice6)
    return slices[index % slices.size]
}

/** The percentage drawn on a slice: white on the light theme's dark slices, black on the dark theme's light ones. */
@Composable
fun sliceLabelColor(): Color = AtsTheme.colors.sliceLabel
