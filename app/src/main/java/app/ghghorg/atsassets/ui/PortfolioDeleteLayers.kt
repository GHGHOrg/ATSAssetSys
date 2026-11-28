package app.ghghorg.atsassets.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.ui.design.AtsBlockedMessage
import app.ghghorg.atsassets.ui.design.AtsButton
import app.ghghorg.atsassets.ui.design.AtsDialog
import app.ghghorg.atsassets.ui.design.AtsTextField
import app.ghghorg.atsassets.ui.design.AtsValueRow
import app.ghghorg.atsassets.ui.design.AtsWarningLine
import app.ghghorg.atsassets.ui.design.ButtonKind
import app.ghghorg.atsassets.ui.theme.AtsTheme
import app.ghghorg.atsassets.ui.theme.gainLossColor
import app.ghghorg.atsassets.ui.theme.typeStyle

/** The Delete dialog's callbacks, passed together (29). */
class DeleteDialogActions(
    val onNameChange: (String) -> Unit,
    val onConfirm: () -> Unit,
    val onCancel: () -> Unit,
)

/**
 * The Delete dialog or the blocked message, whichever is open (29). AppContent draws them over the whole content, the
 * bottom bar included, as Rename's dialog (08), so the bar takes no tap and TalkBack reads only the layer (step 10).
 */
@Composable
internal fun PortfolioDeleteLayers(
    dialog: DeleteDialogModel?,
    blocked: DeleteBlockedModel?,
    actions: DeleteDialogActions,
    onCloseBlocked: () -> Unit,
) {
    if (dialog != null) DeleteDialogLayer(dialog, actions)
    if (blocked != null) DeleteBlockedLayer(blocked, onCloseBlocked)
}

/**
 * The Delete dialog (wireframe 10d frames D and E; 29), drawn from its model (CR2) as the catalog's dialog (44a step 8
 * group (e); drawing spec/wireframes/design/10d-portfolio-dialogs.html): the title names the portfolio; the summary as
 * label-and-value rows, the cash balance now and after as two rows, the after as the total line (10d note 4), read by
 * TalkBack as one sentence with its figures; the line about past net worth; the warning with triangle-alert, not bronze
 * (note 5); the prompt and the Name field, whose note says why Delete portfolio waits until the name is typed. Delete
 * portfolio is the gold filled button, the catalog's disabled one until then (note 7); Cancel and Back close it.
 */
@Composable
internal fun DeleteDialogLayer(
    model: DeleteDialogModel,
    actions: DeleteDialogActions,
) {
    BackHandler(enabled = !model.saving, onBack = actions.onCancel)
    val colors = AtsTheme.colors
    val summary =
        stringResource(
            R.string.delete_summary_spoken,
            model.transactionCount,
            model.tradeLineCount,
            spoken(model.spokenRealizedGain),
            spoken(model.spokenBalanceNow),
            spoken(model.spokenBalanceAfter),
        )
    AtsDialog(
        title = stringResource(R.string.delete_title, model.name),
        actions = {
            AtsButton(stringResource(R.string.cancel_button), actions.onCancel, kind = ButtonKind.TEXT, enabled = !model.saving)
            AtsButton(stringResource(R.string.delete_confirm_button), actions.onConfirm, kind = ButtonKind.FILLED, enabled = model.deleteEnabled)
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = summary }) {
            AtsValueRow(stringResource(R.string.delete_transactions_label), model.transactionCount.toString())
            AtsValueRow(stringResource(R.string.delete_trade_lines_label), model.tradeLineCount.toString())
            AtsValueRow(
                stringResource(R.string.delete_realized_label),
                model.realizedGain,
                valueColor = gainLossColor(model.spokenRealizedGain.sign.signum),
            )
            AtsValueRow(stringResource(R.string.delete_cash_now_label), model.balanceNow)
            AtsValueRow(stringResource(R.string.delete_cash_after_label), model.balanceAfter, total = true)
        }
        Text(stringResource(R.string.delete_history_line), style = typeStyle("small"), color = colors.textSecondary)
        AtsWarningLine(stringResource(R.string.delete_warning))
        Text(stringResource(R.string.delete_type_prompt), style = typeStyle("small"), color = colors.text)
        AtsTextField(
            value = model.typedName,
            onValueChange = actions.onNameChange,
            label = stringResource(R.string.portfolio_name_label),
            modifier = Modifier.fillMaxWidth(),
            refusal = if (model.saveFailed) stringResource(R.string.save_failed) else null,
            note = if (!model.deleteEnabled && !model.saving && !model.saveFailed) stringResource(R.string.delete_not_matching) else null,
            enabled = !model.saving,
            raised = true,
            announce = true,
        )
    }
}

/**
 * The blocked message (wireframe 10d frames F and G; 4.7) as the catalog's dialog: titled "Can't delete" and the
 * portfolio's name, D-31's blocked card inside with the message as one text, read when it appears, its "Not deleted."
 * in medium bronze; Close the one button, gold. The Delete dialog never opens then. Back closes it too.
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
    AtsDialog(
        title = stringResource(R.string.delete_blocked_title, model.name),
        actions = { AtsButton(stringResource(R.string.close_button), onClose, kind = ButtonKind.FILLED) },
    ) {
        AtsBlockedMessage(message, prefix = stringResource(R.string.delete_blocked_prefix))
    }
}

// The dimmed layer under the price key entry's dialog (30): it takes every tap, so a tap outside does nothing, and it gives
// way to the keyboard (the app draws edge to edge; 08 phone check 9a). It ends where MainActivity's Scaffold padding ends,
// above the navigation strip, so that strip is consumed first and the room ends at the keyboard's top (29 step 10). The
// portfolio dialogs use AtsDialog since 44a step 8 group (e); the key entry moves to it with Settings.
@Composable
internal fun DialogScrim(content: @Composable () -> Unit) {
    Box(
        modifier =
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = DELETE_SCRIM_ALPHA))
            .pointerInput(Unit) { detectTapGestures { } }
            .consumeWindowInsets(WindowInsets.navigationBars)
            .imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

// The dim behind the key entry, as Material's own dialogs use.
private const val DELETE_SCRIM_ALPHA = 0.32f

/**
 * An inactive control's dashed outline in [color], drawn on its own [shape] (wireframe 07 marker 3; 28 decision 3): Backup's
 * inactive Restore. The design system shows an inactive control at 38% instead (D-29, 10d note 7).
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
