package app.ghghorg.atsassets.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.ui.design.AtsPickField
import app.ghghorg.atsassets.ui.design.cardShadow
import app.ghghorg.atsassets.ui.theme.AtsIcon
import app.ghghorg.atsassets.ui.theme.AtsTheme
import app.ghghorg.atsassets.ui.theme.LucideIcon
import app.ghghorg.atsassets.ui.theme.tokenDp
import app.ghghorg.atsassets.ui.theme.typeStyle

/**
 * "Ticker" with "All tickers" or the ticker picked (wireframes 07 and 08); one button opening the menu. Made for
 * Transaction history (27 decision 3) and shared with the Cash tab (28 decision 2). In the design system (44a step 8 group
 * (c); drawings 07 and 08): "Ticker" in 14 beside the dense pick field with its chevron-down (D-30), read by TalkBack as
 * one sentence, "Ticker, All tickers".
 */
@Composable
internal fun TickerField(
    ticker: String?,
    onOpen: () -> Unit,
) {
    val label = stringResource(R.string.history_ticker_label)
    val shown = ticker ?: stringResource(R.string.history_all_tickers)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(tokenDp("fieldDensePadding")),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = typeStyle("small"), color = AtsTheme.colors.text, modifier = Modifier.clearAndSetSemantics { })
        AtsPickField(
            label = label,
            value = shown,
            onOpen = onOpen,
            modifier = Modifier.weight(1f),
            dense = true,
            spoken = stringResource(R.string.history_ticker_spoken, shown),
        )
    }
}

/**
 * The Ticker menu (27 decision 3; 28 decision 2), drawn inside the activity window as the ⋮ menu: the scrim, which
 * closes it on a tap outside, and a card with "All tickers" and [tickers], scrolling when they do not fit, the chosen one
 * with Lucide's check (44a step 8 group (c); D-34). No DropdownMenu or Popup: a separate window would take the focus
 * (CLAUDE.md). The scrim takes taps through a gesture, not a click, so the card's pane title is not merged into it.
 */
@Composable
internal fun TickerMenu(
    ticker: String?,
    tickers: List<String>,
    onPick: (String?) -> Unit,
    onClose: () -> Unit,
) {
    val colors = AtsTheme.colors
    val title = stringResource(R.string.history_ticker_menu)
    val shape = RoundedCornerShape(tokenDp("radiusDefault"))
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.scrim)
            .pointerInput(Unit) { detectTapGestures { onClose() } }
            .padding(tokenDp("spaceMargin")),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { paneTitle = title }
                .cardShadow()
                .clip(shape)
                .background(colors.float)
                .verticalScroll(rememberScrollState()),
        ) {
            MenuItem(stringResource(R.string.history_all_tickers), ticker == null) { onPick(null) }
            tickers.forEach { each -> MenuItem(each, ticker == each) { onPick(each) } }
        }
    }
}

/** One item: 48 tall, Asap 16, the chosen one with a check at its end (D-34). */
@Composable
private fun MenuItem(
    label: String,
    chosen: Boolean,
    onPick: () -> Unit,
) {
    val colors = AtsTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = tokenDp("spaceTouch"))
            .clickable(role = Role.Button, onClick = onPick)
            .semantics { selected = chosen }
            .padding(horizontal = tokenDp("layerMenuItemPadding")),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = typeStyle("body"), color = colors.text, modifier = Modifier.weight(1f))
        if (chosen) AtsIcon(LucideIcon.CHECK, tint = colors.iconAction, size = tokenDp("fieldMarkSize"))
    }
}
