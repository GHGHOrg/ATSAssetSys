package app.ghghorg.atsassets.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.ui.theme.gainLossColor

/** The Delete dialog's callbacks, passed together (29). */
class DeleteDialogActions(
    val onNameChange: (String) -> Unit,
    val onConfirm: () -> Unit,
    val onCancel: () -> Unit,
)

/**
 * The Delete dialog (wireframe 10d frames D and E; 29), drawn from its model (CR2) inside the activity window over a
 * dimmed layer, as PortfolioNameDialog (CLAUDE.md: a separate window loops with the cover screen), with imePadding so
 * the field stays above the keyboard. The title names the portfolio; the summary is read by TalkBack as one sentence
 * with its figures; then the line about past net worth, the warning, the prompt and the Name field. Delete portfolio is
 * inactive, with a dashed outline and one line saying why, until the name is typed; Cancel and Back close it.
 */
@Composable
internal fun DeleteDialogLayer(
    model: DeleteDialogModel,
    actions: DeleteDialogActions,
) {
    BackHandler(enabled = !model.saving, onBack = actions.onCancel)
    val title = stringResource(R.string.delete_title, model.name)
    val summary =
        stringResource(
            R.string.delete_summary_spoken,
            model.transactionCount,
            model.tradeLineCount,
            spoken(model.spokenRealizedGain),
            spoken(model.spokenBalanceNow),
            spoken(model.spokenBalanceAfter),
        )
    Scrim {
        Surface(
            modifier = Modifier.padding(24.dp).widthIn(max = 560.dp).semantics { paneTitle = title },
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(text = title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                Column(modifier = Modifier.clearAndSetSemantics { contentDescription = summary }) {
                    Text(text = stringResource(R.string.delete_transactions, model.transactionCount), style = MaterialTheme.typography.bodyLarge)
                    Text(text = stringResource(R.string.delete_trade_lines, model.tradeLineCount), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = stringResource(R.string.delete_realized, model.realizedGain),
                        style = MaterialTheme.typography.bodyLarge,
                        color = gainLossColor(model.spokenRealizedGain.sign.signum),
                    )
                    Text(
                        text = stringResource(R.string.delete_cash, model.balanceNow, model.balanceAfter),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                Text(
                    text = stringResource(R.string.delete_history_line),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(text = stringResource(R.string.delete_warning), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(text = stringResource(R.string.delete_type_prompt), style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = model.typedName,
                    onValueChange = actions.onNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = stringResource(R.string.portfolio_name_label)) },
                    supportingText =
                    if (model.saveFailed) {
                        { Text(text = stringResource(R.string.save_failed), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
                    } else {
                        null
                    },
                    isError = model.saveFailed,
                    enabled = !model.saving,
                    singleLine = true,
                )
                if (!model.deleteEnabled && !model.saving) {
                    Text(
                        text = stringResource(R.string.delete_not_matching),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    TextButton(onClick = actions.onCancel, enabled = !model.saving) {
                        Text(text = stringResource(R.string.cancel_button))
                    }
                    val shape = ButtonDefaults.outlinedShape
                    val dashed = MaterialTheme.colorScheme.outline
                    OutlinedButton(
                        onClick = actions.onConfirm,
                        enabled = model.deleteEnabled,
                        shape = shape,
                        border = if (model.deleteEnabled) ButtonDefaults.outlinedButtonBorder(enabled = true) else null,
                        modifier = if (model.deleteEnabled) Modifier else Modifier.dashedOutline(shape, dashed),
                    ) {
                        Text(text = stringResource(R.string.delete_confirm_button))
                    }
                }
            }
        }
    }
}

/**
 * The blocked message (wireframe 10d frame F and the opening-date case; 4.7), drawn in the window over a dimmed layer:
 * plain text starting "Not deleted", read by TalkBack when it appears, and Close, the only button; Back closes it too.
 */
@Composable
internal fun DeleteBlockedLayer(
    model: DeleteBlockedModel,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)
    val message =
        if (model.cannotDelete) {
            stringResource(R.string.delete_cannot, model.date, model.shortfall)
        } else {
            stringResource(R.string.delete_blocked, model.date, model.shortfall, model.exampleDate.orEmpty())
        }
    Scrim {
        Surface(
            modifier = Modifier.padding(24.dp).widthIn(max = 560.dp).semantics { paneTitle = message },
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onClose) { Text(text = stringResource(R.string.close_button)) }
                }
            }
        }
    }
}

// The dimmed layer under a dialog in the window: it takes every tap, so a tap outside does nothing, and it gives way
// to the keyboard (the app draws edge to edge; 08 phone check 9a).
@Composable
private fun Scrim(content: @Composable () -> Unit) {
    Box(
        modifier =
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = DELETE_SCRIM_ALPHA))
            .pointerInput(Unit) { detectTapGestures { } }
            .imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

// The dim behind the dialog, as Material's own dialogs and PortfolioNameDialog use.
private const val DELETE_SCRIM_ALPHA = 0.32f

/**
 * An inactive control's dashed outline in [color], drawn on its own [shape] (wireframe 07 marker 3 and 10d marker 11;
 * 28 decision 3): Cash's disabled chips and the inactive Delete portfolio.
 */
internal fun Modifier.dashedOutline(
    shape: Shape,
    color: Color,
): Modifier = drawBehind {
    val width = 1.dp.toPx()
    val dash = 4.dp.toPx()
    val outline = shape.createOutline(Size(size.width - width, size.height - width), layoutDirection, this)
    translate(width / 2, width / 2) {
        drawOutline(outline, color, style = Stroke(width, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash))))
    }
}

// A figure's sign as a number, for its color (section 6).
private val FigureSign.signum: Int
    get() = when (this) {
        FigureSign.PLUS -> 1
        FigureSign.MINUS -> -1
        FigureSign.NONE -> 0
    }
