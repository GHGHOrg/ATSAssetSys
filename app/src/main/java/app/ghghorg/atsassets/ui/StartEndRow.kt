package app.ghghorg.atsassets.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A row of two parts (CR6, 43; spec C26): [start] at the left and [end] at the right edge on one line while both fit with
 * [gap] between them; otherwise [end] goes under [start], [lineGap] lower, still at the right edge. Neither part is
 * squeezed: each is measured with the row's whole width and wraps its own text only when that is too narrow. The row
 * takes the whole width it is given. Replaces a FlowRow whose right part had weight(1f), which gave that part only what
 * was left of the line and cut the gain off at the largest font (screen tests on pull request #245).
 */
@Composable
internal fun StartEndRow(
    modifier: Modifier = Modifier,
    gap: Dp = 12.dp,
    lineGap: Dp = 2.dp,
    start: @Composable () -> Unit,
    end: @Composable () -> Unit,
) {
    Layout(contents = listOf(start, end), modifier = modifier) { (starts, ends), constraints ->
        val parts = constraints.copy(minWidth = 0, minHeight = 0)
        val first = starts.map { it.measure(parts) }
        val second = ends.map { it.measure(parts) }
        val firstWidth = first.maxOfOrNull { it.width } ?: 0
        val firstHeight = first.sumOf { it.height }
        val secondWidth = second.maxOfOrNull { it.width } ?: 0
        val secondHeight = second.sumOf { it.height }
        val gapPx = gap.roundToPx()
        val together = firstWidth + gapPx + secondWidth
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else together
        val oneLine = together <= width
        val secondTop = if (oneLine || secondHeight == 0) 0 else firstHeight + lineGap.roundToPx()
        val height = maxOf(firstHeight, secondTop + secondHeight).coerceIn(constraints.minHeight, constraints.maxHeight)
        layout(width, height) {
            var y = 0
            first.forEach {
                it.place(0, y)
                y += it.height
            }
            y = secondTop
            second.forEach {
                it.place(width - it.width, y)
                y += it.height
            }
        }
    }
}
