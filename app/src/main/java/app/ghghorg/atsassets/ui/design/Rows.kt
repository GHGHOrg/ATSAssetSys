package app.ghghorg.atsassets.ui.design

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import app.ghghorg.atsassets.ui.theme.AtsIcon
import app.ghghorg.atsassets.ui.theme.AtsTheme
import app.ghghorg.atsassets.ui.theme.LucideIcon
import app.ghghorg.atsassets.ui.theme.tokenDp
import app.ghghorg.atsassets.ui.theme.typeStyle

// 44a step 8 group (e) (spec 6, Visual design; D-36; drawing 10f frames A and B): a row that opens a screen or a layer,
// on More and Settings.

/**
 * A row that opens something, inside a card without its own padding (`AtsCard(padded = false)`): at least 48 tall, its
 * [name] in 16 and, when given, its current [value] under it in secondary 14, so a long value never squeezes the name
 * (10f note 3); Lucide's chevron-right at the end. TalkBack reads it as one button, [spoken] (the name alone by default).
 */
@Composable
internal fun AtsLinkRow(
    name: String,
    onOpen: () -> Unit,
    value: String? = null,
    spoken: String = name,
    enabled: Boolean = true,
) {
    val colors = AtsTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = tokenDp("spaceTouch"))
            .clickable(enabled = enabled, role = Role.Button, onClick = onOpen)
            .clearAndSetSemantics {
                contentDescription = spoken
                role = Role.Button
                if (enabled) {
                    onClick {
                        onOpen()
                        true
                    }
                } else {
                    disabled()
                }
            }.unavailable(enabled)
            .padding(tokenDp("spaceInset")),
        horizontalArrangement = Arrangement.spacedBy(tokenDp("rowControlGap")),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokenDp("cardGap"))) {
            Text(text = name, style = typeStyle("body"), color = colors.text)
            if (value != null) Text(text = value, style = typeStyle("small"), color = colors.textSecondary)
        }
        AtsIcon(LucideIcon.CHEVRON_RIGHT, tint = colors.iconChevron, size = tokenDp("fieldMarkSize"))
    }
}
