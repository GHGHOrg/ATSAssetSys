package app.ghghorg.atsassets.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import app.ghghorg.atsassets.ui.theme.AtsIcon
import app.ghghorg.atsassets.ui.theme.AtsTheme
import app.ghghorg.atsassets.ui.theme.LucideIcon
import app.ghghorg.atsassets.ui.theme.tokenDp
import app.ghghorg.atsassets.ui.theme.typeStyle

// 44a step 6 (spec 6, Visual design (1), (3) and (7); D-08, D-34): cards, the title band, and the layers drawn inside the
// window (dialogs and menus over a scrim, never a separate window; CLAUDE.md pitfall).

internal enum class CardKind { PLAIN, SUMMARY }

/** The card's two-layer shadow from the tokens (D-08); cards have no border. */
@Composable
internal fun Modifier.cardShadow(): Modifier {
    val shape = RoundedCornerShape(tokenDp("radiusDefault"))
    return AtsTheme.colors.cardShadow.fold(this) { modifier, layer ->
        modifier.dropShadow(
            shape,
            Shadow(
                radius = layer.blur.dp,
                color = layer.color,
                spread = layer.spread.dp,
                offset = DpOffset(layer.offsetX.dp, layer.offsetY.dp),
            ),
        )
    }
}

/** A card: the card color (or the summary card's), radius 8, the shadow, padding 10 inside (D-08, D-15). */
@Composable
internal fun AtsCard(
    modifier: Modifier = Modifier,
    kind: CardKind = CardKind.PLAIN,
    padded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = AtsTheme.colors
    val shape = RoundedCornerShape(tokenDp("radiusDefault"))
    Column(
        modifier = modifier
            .fillMaxWidth()
            .cardShadow()
            .clip(shape)
            .background(if (kind == CardKind.SUMMARY) colors.summary else colors.card)
            .then(if (padded) Modifier.padding(tokenDp("spaceInset")) else Modifier),
        verticalArrangement = Arrangement.spacedBy(tokenDp("cardGap")),
        content = content,
    )
}

/**
 * The title band (D-08): in light the band color with a white title, in dark the title on the page. A back arrow when
 * [onBack] is given, read by TalkBack as [backLabel]; [actions] at the end (the ⋮ menu, Settings).
 */
@Composable
internal fun AtsTitleBand(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    backLabel: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = AtsTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.band)
            .heightIn(min = tokenDp("bandHeight"))
            .padding(horizontal = tokenDp("bandSide")),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null && backLabel != null) AtsIconButton(LucideIcon.CHEVRON_LEFT, backLabel, onBack, tint = colors.iconBand)
        Text(
            title,
            style = typeStyle("screenTitle"),
            color = colors.onBand,
            modifier = Modifier
                .weight(1f)
                .padding(start = if (onBack == null) tokenDp("spaceMargin") else 0.dp)
                .semantics { heading() },
        )
        actions()
    }
}

/** The scrim behind a layer; a tap on it calls [onDismiss] when given. */
@Composable
internal fun AtsScrim(
    onDismiss: (() -> Unit)?,
    content: @Composable BoxScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AtsTheme.colors.scrim)
            .then(
                if (onDismiss != null) {
                    Modifier.clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onDismiss)
                } else {
                    Modifier.clickable(interactionSource = source, indication = null, onClick = {})
                },
            ),
        content = content,
    )
}

/**
 * A dialog inside the window over a scrim (D-34): the title in 22, then [content], then [actions] at the bottom right (Cancel
 * a text button, the one main action filled gold). The caller draws it in AppContent over the bar and pads it for the
 * keyboard (CLAUDE.md pitfalls).
 */
@Composable
internal fun AtsDialog(
    title: String,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = AtsTheme.colors
    val shape = RoundedCornerShape(tokenDp("radiusDefault"))
    AtsScrim(onDismiss) {
        Column(
            modifier = modifier
                .align(Alignment.Center)
                .padding(horizontal = tokenDp("layerDialogSide"))
                .fillMaxWidth()
                .cardShadow()
                .clip(shape)
                .background(colors.float)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                .padding(tokenDp("layerDialogPadding")),
            verticalArrangement = Arrangement.spacedBy(tokenDp("layerDialogGap")),
        ) {
            Text(title, style = typeStyle("screenTitle"), color = colors.text, modifier = Modifier.semantics { heading() })
            content()
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(tokenDp("fieldDensePadding"), Alignment.End), content = actions)
        }
    }
}

/** One item of a menu drawn inside the window (the ⋮ menu): 48 tall, Asap 16 (D-34). */
internal class AtsMenuItem(
    val text: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
)

/** A menu inside the window, at the top end of the screen, over a scrim that closes it (D-34). */
@Composable
internal fun AtsMenu(
    items: List<AtsMenuItem>,
    onDismiss: () -> Unit,
) {
    val colors = AtsTheme.colors
    val shape = RoundedCornerShape(tokenDp("radiusDefault"))
    AtsScrim(onDismiss) {
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(tokenDp("spaceMargin"))
                .width(tokenDp("layerMenuWidth"))
                .cardShadow()
                .clip(shape)
                .background(colors.float),
        ) {
            for (item in items) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = tokenDp("spaceTouch"))
                        .clickable(enabled = item.enabled, role = Role.Button, onClick = item.onClick)
                        .unavailable(item.enabled)
                        .padding(horizontal = tokenDp("layerMenuItemPadding")),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(item.text, style = typeStyle("body"), color = colors.text)
                }
            }
        }
    }
}
