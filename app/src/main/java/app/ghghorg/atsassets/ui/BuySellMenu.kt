package app.ghghorg.atsassets.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.ui.design.AtsButton
import app.ghghorg.atsassets.ui.design.cardShadow
import app.ghghorg.atsassets.ui.theme.AtsTheme
import app.ghghorg.atsassets.ui.theme.tokenDp
import app.ghghorg.atsassets.ui.theme.typeStyle

/** The Buy/Sell menu's callbacks, passed together. */
class BuySellActions(
    val onOpen: () -> Unit,
    val onClose: () -> Unit,
    val onBuy: () -> Unit,
    val onSell: () -> Unit,
    val onDismissNotice: () -> Unit,
    val onSetOpeningBalance: () -> Unit,
    val onCreatePortfolio: () -> Unit,
)

/**
 * The Buy/Sell button on Overall, Portfolios and Cash (navigation outline; wireframe 10a frame G, marker 17): an outline
 * button, so no tab has a gold button for it (D-30 (4)).
 */
@Composable
fun BuySellButton(
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spoken = stringResource(R.string.buy_sell_spoken)
    AtsButton(stringResource(R.string.buy_sell_button), onOpen, modifier = modifier.semantics { contentDescription = spoken })
}

/**
 * The Buy/Sell menu, or the notice that replaces an entry screen (wireframe 10a frame G, marker 18; 09 decision 2), in
 * the design system (44a step 8 group (d) part 1; 10a notes 7 and 8): the catalog's menu above its button, inside the
 * activity window over a scrim that closes it on a tap; a notice as a card with its reason in medium 16 and an outline
 * button where one exists. No DropdownMenu or Popup: a separate window would take the focus (CLAUDE.md). Back closes it
 * too.
 */
@Composable
fun BuySellLayer(
    state: AppState,
    actions: BuySellActions,
) {
    val colors = AtsTheme.colors
    val notice = state.buyNotice
    val close = if (notice != null) actions.onDismissNotice else actions.onClose
    BackHandler(onBack = close)
    val title = stringResource(R.string.buy_sell_menu)
    val shape = RoundedCornerShape(tokenDp("radiusDefault"))
    Box(
        modifier = Modifier.fillMaxSize().background(colors.scrim).pointerInput(notice) { detectTapGestures { close() } },
        contentAlignment = Alignment.BottomEnd,
    ) {
        // Above the Buy/Sell button: its height and its margin under the layer's card.
        val card =
            Modifier
                .padding(bottom = tokenDp("spaceTouch") + tokenDp("spaceGap") * 2, end = tokenDp("spaceMargin"), start = tokenDp("spaceMargin"))
                .cardShadow()
                .clip(shape)
                .background(colors.float)
        if (notice == null) {
            Column(modifier = card.width(tokenDp("layerMenuWidth")).semantics { paneTitle = title }) {
                MenuItem(stringResource(R.string.buy_button), actions.onBuy)
                MenuItem(stringResource(R.string.sell_button), actions.onSell)
            }
        } else {
            NoticeCard(notice, actions, card.widthIn(max = tokenDp("layerChoiceMaxWidth")))
        }
    }
}

/** One item of the menu: 48 tall, Asap 16 (D-34). */
@Composable
private fun MenuItem(
    text: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = tokenDp("spaceTouch"))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = tokenDp("layerMenuItemPadding")),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(text, style = typeStyle("body"), color = AtsTheme.colors.text)
    }
}

/** Nothing to enter (10a note 8): the reason in medium 16, read when it appears, and its outline button where one exists. */
@Composable
private fun NoticeCard(
    notice: BuyNotice,
    actions: BuySellActions,
    modifier: Modifier,
) {
    // NOTHING_TO_SELL_IN_HOLDING is raised on Holding detail only, which draws its own line (11 decision 2).
    val text =
        when (notice) {
            BuyNotice.NEEDS_OPENING_BALANCE -> stringResource(R.string.opening_balance_first)
            BuyNotice.NEEDS_PORTFOLIO -> stringResource(R.string.buy_needs_portfolio)
            BuyNotice.NOTHING_TO_SELL, BuyNotice.NOTHING_TO_SELL_IN_HOLDING -> stringResource(R.string.sell_nothing_held)
        }
    Column(
        modifier = modifier.pointerInput(Unit) { detectTapGestures { } }.padding(tokenDp("layerDialogPadding")),
        verticalArrangement = Arrangement.spacedBy(tokenDp("layerDialogGap")),
    ) {
        Text(
            text = text,
            style = typeStyle("sectionHeading"),
            color = AtsTheme.colors.text,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        when (notice) {
            BuyNotice.NEEDS_OPENING_BALANCE -> AtsButton(stringResource(R.string.guided_set_opening_balance), actions.onSetOpeningBalance)
            BuyNotice.NEEDS_PORTFOLIO -> AtsButton(stringResource(R.string.create_portfolio), actions.onCreatePortfolio)
            BuyNotice.NOTHING_TO_SELL, BuyNotice.NOTHING_TO_SELL_IN_HOLDING -> Unit
        }
    }
}
