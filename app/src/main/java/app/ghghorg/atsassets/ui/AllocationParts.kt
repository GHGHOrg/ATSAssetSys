package app.ghghorg.atsassets.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.core.AssetType
import app.ghghorg.atsassets.ui.theme.sliceColor
import app.ghghorg.atsassets.ui.theme.sliceLabelColor
import kotlin.math.cos
import kotlin.math.sin

// The allocation's donut and rows (view 1, view 2; wireframe 01 marker 3, wireframe 02 markers 3 and 4; 18), shared by
// Overall and Net Worth (Total). They draw the model's values and pick the approved words.

/** A breakdown's heading on Net Worth (Total) and its option on Overall. */
@Composable
internal fun allocationOption(by: AllocationBy): String = stringResource(
    when (by) {
        AllocationBy.ASSET_TYPE -> R.string.allocation_asset_type
        AllocationBy.PORTFOLIO -> R.string.allocation_portfolio
        AllocationBy.HOLDING -> R.string.allocation_holding
    },
)

@Composable
internal fun allocationHeading(by: AllocationBy): String = stringResource(
    when (by) {
        AllocationBy.ASSET_TYPE -> R.string.allocation_by_asset_type
        AllocationBy.PORTFOLIO -> R.string.allocation_by_portfolio
        AllocationBy.HOLDING -> R.string.allocation_by_holding
    },
)

/**
 * The donut and one row per slice. With [onOpen] (Overall) a tap on the donut or a row opens Net Worth (Total) (view 1);
 * TalkBack reads each row as a button. The donut itself is left to the rows for TalkBack: they carry its figures
 * (section 6, accessibility (6)).
 */
@Composable
internal fun AllocationBreakdown(
    model: AllocationModel,
    modifier: Modifier = Modifier,
    onOpen: (() -> Unit)? = null,
) {
    val colors = sliceColors(model.rows)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Donut(model.rows, colors, onOpen)
        Column {
            model.rows.forEachIndexed { index, row -> AllocationRow(row, colors[index], onOpen) }
        }
    }
}

/** Cash's color, then the others in turn, counted without cash (theme's sliceColor). */
@Composable
private fun sliceColors(rows: List<AllocationRowModel>): List<Color> {
    var others = 0
    return rows.map { row -> if (row.name == SliceName.Cash) sliceColor(cash = true, index = 0) else sliceColor(cash = false, index = others++) }
}

@Composable
private fun sliceName(name: SliceName): String = when (name) {
    SliceName.Cash -> stringResource(R.string.net_worth_composition_cash)
    is SliceName.Type -> stringResource(if (name.type == AssetType.ETF) R.string.allocation_etfs else R.string.allocation_stocks)
    is SliceName.Named -> name.name
}

/**
 * The donut (view 1): one arc per slice with a share above 0.0%, in the rows' order from the top, clockwise, with a small
 * gap between slices; a slice of 5% or more carries its percentage on the ring (decision 4). It grows with the font size,
 * up to the width there is. The arcs come from the shown percentages, so they match the figures; the angles are worked
 * out only to draw (4.10).
 */
@Composable
private fun Donut(
    rows: List<AllocationRowModel>,
    colors: List<Color>,
    onOpen: (() -> Unit)?,
) {
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelLarge.copy(color = sliceLabelColor(), fontWeight = FontWeight.Medium)
    val labels = rows.map { stringResource(R.string.allocation_percent, it.percent) }
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val side = min(maxWidth, DONUT_SIZE * maxOf(1f, fontScale))
        val tap = if (onOpen == null) Modifier else Modifier.clickable(onClick = onOpen)
        Canvas(modifier = Modifier.size(side).then(tap).clearAndSetSemantics { }) {
            val thickness = this.size.minDimension * RING_PART
            val inset = thickness / 2
            val ring = Size(this.size.minDimension - thickness, this.size.minDimension - thickness)
            val drawn = rows.indices.filter { rows[it].tenths > 0 }
            val gap = if (drawn.size > 1) GAP_DEGREES else 0f
            var start = -90f
            drawn.forEach { index ->
                val sweep = rows[index].tenths * 360f / 1000f
                drawArc(
                    color = colors[index],
                    startAngle = start + gap / 2,
                    sweepAngle = sweep - gap,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = ring,
                    style = Stroke(width = thickness),
                )
                if (!rows[index].percentInRow) {
                    val middle = Math.toRadians((start + sweep / 2).toDouble())
                    val radius = ring.width / 2
                    val text = measurer.measure(labels[index], labelStyle)
                    val at = Offset(center.x + radius * cos(middle).toFloat(), center.y + radius * sin(middle).toFloat())
                    drawText(text, topLeft = Offset(at.x - text.size.width / 2f, at.y - text.size.height / 2f))
                }
                start += sweep
            }
        }
    }
}

/**
 * One row: the swatch, the name, the percentage only for a slice under 5% (view 1) and the amount. TalkBack reads its
 * name, amount and percentage in full (section 6, accessibility (6)); on Overall the row is a button.
 */
@Composable
private fun AllocationRow(
    row: AllocationRowModel,
    color: Color,
    onOpen: (() -> Unit)?,
) {
    val name = sliceName(row.name)
    val spoken = stringResource(R.string.allocation_row_spoken, name, row.spokenAmount, row.percent)
    val semantics =
        if (onOpen == null) {
            Modifier.clearAndSetSemantics { contentDescription = spoken }
        } else {
            Modifier
                .clickable(role = Role.Button, onClick = onOpen)
                .clearAndSetSemantics {
                    contentDescription = spoken
                    role = Role.Button
                    onClick {
                        onOpen()
                        true
                    }
                }
        }
    Row(
        modifier = Modifier.fillMaxWidth().then(semantics).padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(16.dp).background(color, CircleShape))
        Text(text = name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (row.percentInRow) {
            Text(
                text = stringResource(R.string.allocation_percent, row.percent),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(text = row.amount, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.End)
    }
}

/**
 * Overall's "Allocation by" (view 1; decision 1): the label and a button with the shown option. TalkBack reads it as
 * "Allocation by, <option>" and a button. A tap opens the choice layer; it never opens Net Worth (Total).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AllocationByButton(
    by: AllocationBy,
    onOpenChoice: () -> Unit,
) {
    val option = allocationOption(by)
    val spoken = stringResource(R.string.allocation_by_spoken, option)
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.allocation_by_label),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.CenterVertically).clearAndSetSemantics { },
        )
        OutlinedButton(onClick = onOpenChoice, modifier = Modifier.semantics { contentDescription = spoken }) {
            Text(text = "$option ▾", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/**
 * The choice layer (view 1), drawn inside the activity window: a clear layer that closes on a tap outside or Back, and a
 * card with the options, the shown one selected, scrolling when taller than the space (defect 18-6). No DropdownMenu or Popup: a separate window would take MainActivity's
 * focus (CLAUDE.md, 07). TalkBack reads only the layer while it is open.
 */
@Composable
internal fun AllocationChoiceLayer(
    selected: AllocationBy,
    onChoose: (AllocationBy) -> Unit,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)
    val title = stringResource(R.string.allocation_by_label)
    Box(
        modifier = Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { onClose() } },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier =
            Modifier
                .padding(16.dp)
                .widthIn(max = 360.dp)
                .pointerInput(Unit) { detectTapGestures { } }
                .semantics { paneTitle = title },
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 3.dp,
            shadowElevation = 3.dp,
        ) {
            // Defect 18-6: at the largest font in landscape the options are taller than the space; they scroll.
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(vertical = 8.dp).selectableGroup()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).semantics { heading() },
                )
                AllocationBy.entries.forEach { by ->
                    Row(
                        modifier =
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = by == selected, role = Role.RadioButton, onClick = { onChoose(by) })
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = by == selected, onClick = null)
                        Text(text = allocationOption(by), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

// The donut's side at the normal font size, the ring's part of it, and the gap between slices (wireframes 01 and 02).
private val DONUT_SIZE = 200.dp
private const val RING_PART = 0.22f
private const val GAP_DEGREES = 2f
