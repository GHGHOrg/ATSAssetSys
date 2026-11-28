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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.min
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.core.AssetType
import app.ghghorg.atsassets.ui.design.AtsButton
import app.ghghorg.atsassets.ui.design.AtsCard
import app.ghghorg.atsassets.ui.design.AtsPickField
import app.ghghorg.atsassets.ui.design.AtsRadioGroup
import app.ghghorg.atsassets.ui.design.AtsRadioRow
import app.ghghorg.atsassets.ui.design.AtsSectionHeading
import app.ghghorg.atsassets.ui.design.ButtonKind
import app.ghghorg.atsassets.ui.design.cardShadow
import app.ghghorg.atsassets.ui.theme.AtsTheme
import app.ghghorg.atsassets.ui.theme.sliceColor
import app.ghghorg.atsassets.ui.theme.sliceLabelColor
import app.ghghorg.atsassets.ui.theme.tokenDp
import app.ghghorg.atsassets.ui.theme.tokenNumber
import app.ghghorg.atsassets.ui.theme.typeStyle
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
        AllocationBy.SECTOR -> R.string.allocation_sector
        AllocationBy.GEOGRAPHY -> R.string.allocation_geography
    },
)

@Composable
internal fun allocationHeading(by: AllocationBy): String = stringResource(
    when (by) {
        AllocationBy.ASSET_TYPE -> R.string.allocation_by_asset_type
        AllocationBy.PORTFOLIO -> R.string.allocation_by_portfolio
        AllocationBy.HOLDING -> R.string.allocation_by_holding
        AllocationBy.SECTOR -> R.string.allocation_by_sector
        AllocationBy.GEOGRAPHY -> R.string.allocation_by_geography
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
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(tokenDp("spaceGap"))) {
        Donut(model.rows, colors, onOpen)
        Column {
            model.rows.forEachIndexed { index, row -> AllocationRow(row, colors[index], onOpen) }
        }
    }
}

/** A breakdown under its section heading, in a card (Net Worth (Total) and Allocation; wireframes 02 and 04). */
@Composable
internal fun BreakdownCard(model: AllocationModel) {
    AtsSectionHeading(allocationHeading(model.by))
    AtsCard { AllocationBreakdown(model) }
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
    SliceName.Unclassified -> stringResource(R.string.class_unclassified)
}

/**
 * The donut (view 1; D-38): 168 across with a 30 ring, one arc per slice with a share above 0.0%, in the rows' order from
 * the top, clockwise, with a small gap between slices; a slice of 5% or more carries its percentage on the ring in Asap
 * Condensed 12 medium in the slice-label color. It grows with the font size, up to the width there is. The arcs come from
 * the shown percentages, so they match the figures; the angles are worked out only to draw (4.10).
 */
@Composable
private fun Donut(
    rows: List<AllocationRowModel>,
    colors: List<Color>,
    onOpen: (() -> Unit)?,
) {
    val measurer = rememberTextMeasurer()
    val labelStyle = typeStyle("dayHead").copy(color = sliceLabelColor())
    val labels = rows.map { stringResource(R.string.allocation_percent, it.percent) }
    val fontScale = LocalDensity.current.fontScale
    val ringPart = tokenNumber("donutRing") / tokenNumber("donutSize")
    val gapDegrees = tokenNumber("donutGapDegrees")
    BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val side = min(maxWidth, tokenDp("donutSize") * maxOf(1f, fontScale))
        val tap = if (onOpen == null) Modifier else Modifier.clickable(onClick = onOpen)
        Canvas(modifier = Modifier.size(side).then(tap).clearAndSetSemantics { }) {
            val thickness = this.size.minDimension * ringPart
            val inset = thickness / 2
            val ring = Size(this.size.minDimension - thickness, this.size.minDimension - thickness)
            val drawn = rows.indices.filter { rows[it].tenths > 0 }
            val gap = if (drawn.size > 1) gapDegrees else 0f
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
 * One row (D-38): 48 tall, a 16 round swatch, the name and the amount in 16; a slice under 5% shows its percentage in
 * secondary 14 after the name (view 1). TalkBack reads its name, amount and percentage in full (section 6, accessibility
 * (6)); on Overall the row is a button.
 */
@Composable
private fun AllocationRow(
    row: AllocationRowModel,
    color: Color,
    onOpen: (() -> Unit)?,
) {
    val colors = AtsTheme.colors
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
        modifier = Modifier.fillMaxWidth().heightIn(min = tokenDp("spaceTouch")).then(semantics),
        horizontalArrangement = Arrangement.spacedBy(tokenDp("rowControlGap")),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(tokenDp("donutSwatch")).background(color, CircleShape))
        Text(text = name, style = typeStyle("body"), color = colors.text, modifier = Modifier.weight(1f))
        if (row.percentInRow) {
            Text(text = stringResource(R.string.allocation_percent, row.percent), style = typeStyle("small"), color = colors.textSecondary)
        }
        Text(text = row.amount, style = typeStyle("body"), color = colors.text, textAlign = TextAlign.End)
    }
}

/**
 * Overall's "Allocation by" (view 1; decision 1; D-38): the dense pick field with its label beside it. TalkBack reads it as
 * "Allocation by, <option>". A tap opens the choice layer; it never opens Net Worth (Total).
 */
@Composable
internal fun AllocationByButton(
    by: AllocationBy,
    onOpenChoice: () -> Unit,
) {
    val option = allocationOption(by)
    ChoiceButton(stringResource(R.string.allocation_by_label), option, stringResource(R.string.allocation_by_spoken, option), onOpenChoice, dense = true)
}

/**
 * A chooser that opens a [ChoiceLayer] (D-30, D-41): the dense pick field with [label] beside it, or the standard pick field
 * with [label] on its outline; TalkBack reads [spoken].
 */
@Composable
internal fun ChoiceButton(
    label: String,
    shown: String,
    spoken: String,
    onOpenChoice: () -> Unit,
    dense: Boolean = false,
) {
    if (dense) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(tokenDp("fieldDensePadding")),
        ) {
            Text(text = label, style = typeStyle("small"), color = AtsTheme.colors.text, modifier = Modifier.clearAndSetSemantics { })
            AtsPickField(label, shown, onOpenChoice, modifier = Modifier.weight(1f), dense = true, spoken = spoken)
        }
    } else {
        AtsPickField(label, shown, onOpenChoice, modifier = Modifier.fillMaxWidth(), spoken = spoken)
    }
}

/** Overall's "Allocation by" choice (view 1; 18) in the shared [ChoiceLayer]. */
@Composable
internal fun AllocationChoiceLayer(
    selected: AllocationBy,
    onChoose: (AllocationBy) -> Unit,
    onClose: () -> Unit,
) {
    val options = AllocationBy.entries.map { it to allocationOption(it) }
    ChoiceLayer(stringResource(R.string.allocation_by_label), options, selected, onChoose, onClose)
}

/**
 * A choice layer (view 1, view 4; 18 and 19; Settings' Auto-lock, 20), drawn inside the activity window: a layer that
 * closes on a tap outside or Back, and a card titled [title] with [options] (a value and its label) as radio rows, the
 * [selected] one selected (D-36), and an optional [note] under them, scrolling when taller than the space (defect 18-6).
 * With [closeButton], Close as a text button at the end (a choice that saves on tap, as Auto-lock; 10f frame C). No
 * DropdownMenu or Popup: a separate window would take MainActivity's focus (CLAUDE.md, 07).
 */
@Composable
internal fun <T> ChoiceLayer(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onChoose: (T) -> Unit,
    onClose: () -> Unit,
    note: String? = null,
    closeButton: Boolean = false,
) {
    LayerCard(title, onClose) {
        AtsRadioGroup {
            options.forEach { (value, label) -> AtsRadioRow(label, selected = value == selected, onClick = { onChoose(value) }) }
        }
        note?.let {
            Text(
                text = it,
                style = typeStyle("small"),
                color = AtsTheme.colors.textSecondary,
                modifier = Modifier.padding(horizontal = tokenDp("spaceInset"), vertical = tokenDp("fieldDensePadding")),
            )
        }
        if (closeButton) LayerClose(onClose)
    }
}

/** A layer's Close, a text button at the end under its content (10f frames C and D). */
@Composable
internal fun LayerClose(onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = tokenDp("layerDialogPadding")),
        horizontalArrangement = Arrangement.End,
    ) {
        AtsButton(stringResource(R.string.close_button), onClose, kind = ButtonKind.TEXT)
    }
}

/**
 * The frame of a layer drawn inside the activity window (18 to 21; D-34): a scrim that closes on a tap outside or Back, and
 * a raised card titled [title] in 22 with [content] under the title, scrolling when taller than the space (defect 18-6). No
 * Dialog or Popup: a separate window would take MainActivity's focus (CLAUDE.md, 07). TalkBack reads only the layer while
 * it is open.
 */
@Composable
internal fun LayerCard(
    title: String,
    onClose: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = AtsTheme.colors
    val shape = RoundedCornerShape(tokenDp("radiusDefault"))
    BackHandler(onBack = onClose)
    Box(
        modifier = Modifier.fillMaxSize().background(colors.scrim).pointerInput(Unit) { detectTapGestures { onClose() } },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier =
            Modifier
                .padding(tokenDp("layerDialogSide"))
                .widthIn(max = tokenDp("layerChoiceMaxWidth"))
                .cardShadow()
                .clip(shape)
                .background(colors.float)
                .pointerInput(Unit) { detectTapGestures { } }
                .semantics { paneTitle = title }
                .verticalScroll(rememberScrollState())
                .padding(vertical = tokenDp("fieldDensePadding")),
        ) {
            Text(
                text = title,
                style = typeStyle("screenTitle"),
                color = colors.text,
                modifier = Modifier.padding(horizontal = tokenDp("layerDialogPadding"), vertical = tokenDp("fieldDensePadding")).semantics { heading() },
            )
            content()
        }
    }
}
