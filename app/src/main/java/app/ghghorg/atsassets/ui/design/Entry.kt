package app.ghghorg.atsassets.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import app.ghghorg.atsassets.ui.theme.AtsIcon
import app.ghghorg.atsassets.ui.theme.AtsTheme
import app.ghghorg.atsassets.ui.theme.LucideIcon
import app.ghghorg.atsassets.ui.theme.tokenDp
import app.ghghorg.atsassets.ui.theme.typeStyle

// 44a step 8 group (d) part 1 (spec 6, Visual design; D-31, D-32): the parts the entry flows share, built with the first
// screens that use them (step 6's departure): the foot, the review card's headings, and a blocked message as one text.

/**
 * The foot of an entry or review (D-31 (2)): [primary] the large full-width gold button (Review, Confirm and save), then
 * [secondary] a large full-width outline button under it (Back to edit) when given, or a text button with
 * [secondaryText] (the reversal's Cancel; D-31 (2)).
 */
@Composable
internal fun AtsEntryFoot(
    primary: String,
    onPrimary: () -> Unit,
    enabled: Boolean = true,
    secondary: String? = null,
    onSecondary: () -> Unit = {},
    secondaryText: Boolean = false,
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(tokenDp("spaceGap"))) {
        AtsButton(primary, onPrimary, kind = ButtonKind.FILLED, size = ButtonSize.LARGE, enabled = enabled, fillWidth = true)
        if (secondary != null) {
            val kind = if (secondaryText) ButtonKind.TEXT else ButtonKind.OUTLINE
            AtsButton(secondary, onSecondary, kind = kind, size = ButtonSize.LARGE, enabled = enabled, fillWidth = true)
        }
    }
}

/** A review's part heading in medium 16 ("1. Reverse the original"; 10c note 3), read as a heading. */
@Composable
internal fun AtsPartHeading(text: String) {
    Text(text, style = typeStyle("sectionHeading"), color = AtsTheme.colors.text, modifier = Modifier.semantics { heading() })
}

/** A sub-heading inside a review card ("Lots used"; 10a note 6): medium 14 in the secondary color. */
@Composable
internal fun AtsReviewSubheading(text: String) {
    Text(text, style = typeStyle("label"), color = AtsTheme.colors.textSecondary, modifier = Modifier.padding(top = tokenDp("cardGap")))
}

/** A review card (D-31 (4)): the catalog's card with its rows. */
@Composable
internal fun AtsReviewCard(content: @Composable ColumnScope.() -> Unit) {
    AtsCard(content = content)
}

/**
 * A blocked save as the catalog's blocked card (D-31 (8); D-16): the 2 dp bronze outline and circle-alert, and [text] as
 * one text, its [prefix] ("Not saved.", "Not saved. Nothing was changed.") in medium bronze when it starts with it. One
 * text, so TalkBack reads it whole and the tests find it by its words; read when it appears.
 */
@Composable
internal fun AtsBlockedMessage(
    text: String,
    prefix: String?,
    modifier: Modifier = Modifier,
) {
    val colors = AtsTheme.colors
    val shape = RoundedCornerShape(tokenDp("radiusDefault"))
    val head = prefix?.takeIf { text.startsWith(it) }
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
        Text(
            text =
            buildAnnotatedString {
                if (head != null) {
                    withStyle(SpanStyle(color = colors.error, fontWeight = typeStyle("button").fontWeight)) { append(head) }
                    append(text.substring(head.length))
                } else {
                    append(text)
                }
            },
            style = typeStyle("small"),
            color = colors.text,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}
