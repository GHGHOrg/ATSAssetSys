package app.ghghorg.atsassets.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.ghghorg.atsassets.R

/**
 * "Ticker" with "All tickers v" or the ticker picked (wireframes 07 and 08); one button opening the menu. Made for
 * Transaction history (27 decision 3) and shared with the Cash tab (28 decision 2).
 */
@Composable
internal fun TickerField(
    ticker: String?,
    onOpen: () -> Unit,
) {
    val shown = ticker ?: stringResource(R.string.history_all_tickers)
    val spoken = stringResource(R.string.history_ticker_spoken, shown)
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onOpen)
            .clearAndSetSemantics {
                contentDescription = spoken
                role = Role.Button
                onClick {
                    onOpen()
                    true
                }
            },
    ) {
        Text(
            text = stringResource(R.string.history_ticker_label),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = shown, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text(text = "v", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

/**
 * The Ticker menu (27 decision 3; 28 decision 2), drawn inside the activity window as the ⋮ menu: a clear layer that
 * closes it on a tap outside, and a card with "All tickers" and [tickers], scrolling when they do not fit. No
 * DropdownMenu or Popup: a separate window would take the focus (CLAUDE.md).
 */
@Composable
internal fun TickerMenu(
    ticker: String?,
    tickers: List<String>,
    onPick: (String?) -> Unit,
    onClose: () -> Unit,
) {
    val title = stringResource(R.string.history_ticker_menu)
    Box(
        modifier = Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { onClose() } }.padding(16.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().semantics { paneTitle = title },
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 3.dp,
            shadowElevation = 3.dp,
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
                MenuItem(stringResource(R.string.history_all_tickers), ticker == null) { onPick(null) }
                tickers.forEach { each -> MenuItem(each, ticker == each) { onPick(each) } }
            }
        }
    }
}

@Composable
private fun MenuItem(
    label: String,
    chosen: Boolean,
    onPick: () -> Unit,
) {
    TextButton(
        onClick = onPick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).semantics { selected = chosen },
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (chosen) FontWeight.Medium else FontWeight.Normal,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
