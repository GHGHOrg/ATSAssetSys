package app.ghghorg.atsassets.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.ghghorg.atsassets.R

// The price refresh's parts shared by every screen that shows prices: Holding detail (16a), the Portfolios list and a
// portfolio's screen (16b).

/**
 * Pull-to-refresh (spec 7; 16a and 16b decision 2): a pull calls [onRefresh]. Material's own spinner shows only for a
 * pull, while it runs; a refresh on unlock shows only the "updating" line (section 6: only the price part shows it).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PricePullToRefresh(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    var pulled by remember { mutableStateOf(false) }
    LaunchedEffect(refreshing) { if (!refreshing) pulled = false }
    PullToRefreshBox(
        isRefreshing = pulled && refreshing,
        onRefresh = {
            pulled = true
            onRefresh()
        },
        modifier = modifier,
        content = content,
    )
}

/**
 * Section 6, error and loading states; AC-42: "Updating prices" while a refresh runs, and one quiet line when the last
 * refresh failed, with no pop-up. TalkBack reads each as it appears. When it failed because no price source key is
 * saved ([noKey]), the line names the missing key and where to enter it (30 decision 3).
 */
@Composable
internal fun PriceRefreshLines(
    refreshing: Boolean,
    failed: Boolean,
    noKey: Boolean = false,
) {
    if (refreshing) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        ) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Text(
                text = stringResource(R.string.prices_updating),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (failed) {
        Text(
            text = stringResource(if (noKey) R.string.price_refresh_no_key else R.string.price_refresh_failed),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}
