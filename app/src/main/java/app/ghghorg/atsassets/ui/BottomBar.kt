package app.ghghorg.atsassets.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.core.Tab
import app.ghghorg.atsassets.ui.theme.AtsIcon
import app.ghghorg.atsassets.ui.theme.AtsTheme
import app.ghghorg.atsassets.ui.theme.LucideIcon
import app.ghghorg.atsassets.ui.theme.TokenBase
import app.ghghorg.atsassets.ui.theme.typeStyle

private fun size(name: String) = TokenBase.sizes.getValue(name).dp

/**
 * The bottom bar (spec 6, navigation outline and Visual design (5); D-37): tabs in [order], left to right (02b decision 7),
 * each an icon and its name. The selected tab's icon and name are gold and bold; in light a pill of one size on every tab
 * sits behind them, in dark a gold line sits under them (C28). TalkBack reads each tab's name and whether it is selected.
 */
@Composable
fun BottomBar(
    order: List<Tab>,
    selected: Tab,
    onSelect: (Tab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AtsTheme.colors.bar)
            .heightIn(min = size("barHeight"))
            .selectableGroup(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (tab in order) {
            TabItem(
                tab = tab,
                selected = tab == selected,
                onClick = { onSelect(tab) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TabItem(
    tab: Tab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AtsTheme.colors
    val ink = if (selected) colors.barSelected else colors.barLabel
    Column(
        modifier = modifier
            .heightIn(min = size("barHeight"))
            .selectable(selected = selected, onClick = onClick, role = Role.Tab),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = size("barPillMinWidth"))
                .clip(RoundedCornerShape(size("radiusDefault")))
                .background(if (selected) colors.barPill else Color.Transparent)
                .padding(
                    start = size("barPillPaddingSide"),
                    end = size("barPillPaddingSide"),
                    top = size("barPillPaddingTop"),
                    bottom = size("barPillPaddingBottom"),
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(size("barIconGap")),
        ) {
            AtsIcon(tab.icon, tint = ink)
            Text(
                text = stringResource(tab.labelRes),
                style = typeStyle("tab"),
                fontWeight = if (selected) FontWeight.W700 else null,
                color = ink,
                textAlign = TextAlign.Center,
            )
        }
        Box(
            modifier = Modifier
                .padding(top = size("barLineGap"))
                .width(size("barLineWidth"))
                .height(size("barLineHeight"))
                .clip(RoundedCornerShape(size("barLineHeight")))
                .background(if (selected) colors.barLine else Color.Transparent),
        )
    }
}

/** A tab's icon (D-37). */
internal val Tab.icon: LucideIcon
    get() = when (this) {
        Tab.MORE -> LucideIcon.ELLIPSIS
        Tab.CASH -> LucideIcon.WALLET
        Tab.PORTFOLIOS -> LucideIcon.BRIEFCASE
        Tab.OVERALL -> LucideIcon.CHART_PIE
    }

/** A tab's name, as the bar shows it; also the Tab order screen's and Settings' (22). */
@get:StringRes
internal val Tab.labelRes: Int
    get() = when (this) {
        Tab.MORE -> R.string.tab_more
        Tab.CASH -> R.string.tab_cash
        Tab.PORTFOLIOS -> R.string.tab_portfolios
        Tab.OVERALL -> R.string.overall_title
    }
