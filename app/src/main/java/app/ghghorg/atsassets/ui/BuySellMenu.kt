package app.ghghorg.atsassets.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.ghghorg.atsassets.R

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

/** The Buy/Sell button on Overall, Portfolios and Cash (navigation outline; wireframe 10a frame G, marker 17). */
@Composable
fun BuySellButton(
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spoken = stringResource(R.string.buy_sell_spoken)
    OutlinedButton(onClick = onOpen, modifier = modifier.semantics { contentDescription = spoken }) {
        Text(text = stringResource(R.string.buy_sell_button))
    }
}

/**
 * The Buy/Sell menu, or the notice that replaces an entry screen (wireframe 10a frame G, marker 18; 09 decision 2).
 * Drawn inside the activity window like the ⋮ menu: a clear layer that closes it on a tap outside, and a card above
 * the button. No DropdownMenu or Popup: a separate window would take the focus (CLAUDE.md). Back closes it too.
 */
@Composable
fun BuySellLayer(
    state: AppState,
    actions: BuySellActions,
) {
    val notice = state.buyNotice
    val close = if (notice != null) actions.onDismissNotice else actions.onClose
    BackHandler(onBack = close)
    val title = stringResource(R.string.buy_sell_menu)
    Box(
        modifier = Modifier.fillMaxSize().pointerInput(notice) { detectTapGestures { close() } },
        contentAlignment = Alignment.BottomEnd,
    ) {
        Surface(
            modifier = Modifier.padding(bottom = 64.dp, end = 16.dp, start = 16.dp).widthIn(max = 360.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 3.dp,
            shadowElevation = 3.dp,
        ) {
            if (notice == null) {
                Column(modifier = Modifier.padding(8.dp).semantics { paneTitle = title }) {
                    TextButton(onClick = actions.onBuy) {
                        Text(text = stringResource(R.string.buy_button), style = MaterialTheme.typography.bodyLarge)
                    }
                    TextButton(onClick = actions.onSell) {
                        Text(text = stringResource(R.string.sell_button), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            } else {
                NoticeCard(notice, actions)
            }
        }
    }
}

@Composable
private fun NoticeCard(
    notice: BuyNotice,
    actions: BuySellActions,
) {
    val text =
        when (notice) {
            BuyNotice.NEEDS_OPENING_BALANCE -> stringResource(R.string.opening_balance_first)
            BuyNotice.NEEDS_PORTFOLIO -> stringResource(R.string.buy_needs_portfolio)
            BuyNotice.NOTHING_TO_SELL -> stringResource(R.string.sell_nothing_held)
            BuyNotice.SELL_LATER -> stringResource(R.string.sell_later_build)
        }
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        when (notice) {
            BuyNotice.NEEDS_OPENING_BALANCE -> Button(onClick = actions.onSetOpeningBalance) {
                Text(text = stringResource(R.string.guided_set_opening_balance))
            }

            BuyNotice.NEEDS_PORTFOLIO -> Button(onClick = actions.onCreatePortfolio) {
                Text(text = stringResource(R.string.create_portfolio))
            }

            BuyNotice.NOTHING_TO_SELL, BuyNotice.SELL_LATER -> Unit
        }
    }
}
