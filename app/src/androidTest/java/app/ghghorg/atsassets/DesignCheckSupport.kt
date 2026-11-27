package app.ghghorg.atsassets

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import app.ghghorg.atsassets.ui.theme.LucideIconKey
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import kotlin.math.abs

// AC-61 (e) (CR7, 44a step 5): what the screen tests use to check a screen's look against the design tokens. A text's
// style is read from the layout Compose made for it; an icon by the Lucide file it draws (LucideIconKey, a semantics key
// for the tests only); a surface's color by sampling a flat area of a captured image.

/** The style a text node was laid out with: its family, weight, size and color (spec 6, Visual design (2)). */
internal fun SemanticsNodeInteraction.laidOutStyle(): TextStyle {
    val results = mutableListOf<TextLayoutResult>()
    val action = fetchSemanticsNode().config.getOrNull(SemanticsActions.GetTextLayoutResult)
    assertNotNull("The node has a text layout", action)
    action!!.action!!.invoke(results)
    assertTrue("The node gave its text layout", results.isNotEmpty())
    return results.first().layoutInput.style
}

/** Every icon drawn, as (Lucide file, bounds in the root), left to right then top to bottom. */
internal fun SemanticsNodeInteractionsProvider.drawnIcons(): List<Pair<String, Rect>> {
    val nodes = onAllNodes(SemanticsMatcher.keyIsDefined(LucideIconKey), useUnmergedTree = true).fetchSemanticsNodes()
    return nodes
        .map { node: SemanticsNode -> node.config[LucideIconKey] to node.boundsInRoot }
        .sortedWith(compareBy({ it.second.top }, { it.second.left }))
}

/** The color at ([x], [y]) of this node's captured image, in px from the node's top left. */
internal fun SemanticsNodeInteraction.colorAt(
    x: Float,
    y: Float,
): Color = captureToImage().toPixelMap()[x.toInt(), y.toInt()]

/** True when [actual] equals [expected] within [tolerance] in each channel (flat fills are drawn exactly; edges are not sampled). */
internal fun sameColor(
    actual: Color,
    expected: Color,
    tolerance: Int = 3,
): Boolean {
    val a = actual.toArgb()
    val e = expected.toArgb()
    return listOf(24, 16, 8, 0).all { shift -> abs(((a shr shift) and 0xFF) - ((e shr shift) and 0xFF)) <= tolerance }
}

internal fun describe(color: Color): String = "#%08X".format(color.toArgb())
