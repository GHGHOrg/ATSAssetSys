package app.ghghorg.atsassets.ui.design

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import app.ghghorg.atsassets.ui.theme.AtsIcon
import app.ghghorg.atsassets.ui.theme.AtsTheme
import app.ghghorg.atsassets.ui.theme.LucideIcon
import app.ghghorg.atsassets.ui.theme.tokenDp
import app.ghghorg.atsassets.ui.theme.typeStyle

// 44a step 6 (spec 6, Error, empty and loading states; D-16, D-27, D-33 to D-35): the catalog's messages. Bronze marks an
// error only; a notice or a warning uses the icon color, and a finished step is never green, which is for gains. Each one's
// meaning is in its words, never in the color or the icon alone (spec 6, Accessibility (5)).

/** A blocked save or action: one card with a 2 dp bronze outline, circle-alert, [title] ("Not saved") in medium bronze and
 * [reason] in normal text (D-16, D-27, D-31). */
@Composable
internal fun AtsBlockedCard(
    title: String,
    reason: String,
    modifier: Modifier = Modifier,
) {
    val colors = AtsTheme.colors
    val shape = RoundedCornerShape(tokenDp("radiusDefault"))
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.card)
            .border(tokenDp("layerBlockedOutline"), colors.error, shape)
            .padding(tokenDp("spaceInset")),
        horizontalArrangement = Arrangement.spacedBy(tokenDp("fieldDensePadding")),
    ) {
        AtsIcon(LucideIcon.CIRCLE_EXCLAMATION_POINT, tint = colors.error, size = tokenDp("fieldMarkSize"))
        Column(verticalArrangement = Arrangement.spacedBy(tokenDp("cardGap"))) {
            Text(title, style = typeStyle("button"), color = colors.error)
            Text(reason, style = typeStyle("small"), color = colors.text)
        }
    }
}

/** A notice at the top of a screen: a card with Lucide's info in the icon color and the text in 14; never bronze (D-33). */
@Composable
internal fun AtsNoticeBanner(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = AtsTheme.colors
    AtsCard(modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(tokenDp("fieldDensePadding"))) {
            AtsIcon(LucideIcon.INFO, tint = colors.iconAction, size = tokenDp("fieldMarkSize"))
            Text(text, style = typeStyle("small"), color = colors.text)
        }
    }
}

/** A consequence before it happens: medium 14 with Lucide's triangle-alert in the icon color; never bronze (D-34). */
@Composable
internal fun AtsWarningLine(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = AtsTheme.colors
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(tokenDp("fieldDensePadding"))) {
        AtsIcon(LucideIcon.TRIANGLE_EXCLAMATION_POINT, tint = colors.iconAction, size = tokenDp("fieldMarkSize"))
        Text(text, style = typeStyle("button"), color = colors.text)
    }
}

/** A finished result: Lucide's circle-check in the icon color beside a medium 16 heading; never green (D-35). */
@Composable
internal fun AtsDoneHeading(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = AtsTheme.colors
    Row(
        modifier = modifier.semantics(mergeDescendants = true) { heading() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokenDp("fieldDensePadding")),
    ) {
        AtsIcon(LucideIcon.CIRCLE_CHECK, tint = colors.iconAction)
        Text(text, style = typeStyle("sectionHeading"), color = colors.text)
    }
}

/**
 * One step of a running job, 48 tall: a check when [done], otherwise loader-circle turning, and the state in words (D-35).
 * The catalog keeps the loader turning under reduced motion; [UNVERIFIED] whether Android's "remove animations" stops it,
 * checked on the phone (step 9).
 */
@Composable
internal fun AtsProgressRow(
    text: String,
    done: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = AtsTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = tokenDp("spaceTouch")),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokenDp("rowControlGap")),
    ) {
        if (done) {
            AtsIcon(LucideIcon.CHECK, tint = colors.iconAction)
        } else {
            TurningLoader(colors.iconAction)
        }
        Text(text, style = typeStyle("body"), color = colors.text)
    }
}

/** Lucide's loader-circle turning, for something that runs (a step, a price refresh; D-35). */
@Composable
internal fun TurningLoader(
    tint: androidx.compose.ui.graphics.Color,
    size: androidx.compose.ui.unit.Dp = tokenDp("iconSize"),
) {
    val turn by rememberInfiniteTransition(label = "loader").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1000, easing = LinearEasing), RepeatMode.Restart),
        label = "loader turn",
    )
    AtsIcon(LucideIcon.LOADER_CIRCLE, tint = tint, size = size, modifier = Modifier.rotate(turn))
}
