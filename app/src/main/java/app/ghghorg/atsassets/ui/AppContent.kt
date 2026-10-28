package app.ghghorg.atsassets.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.ghghorg.atsassets.R

/** Shows the screen the ViewModel names. Only the unlocked branch of MainActivity calls this. */
@Composable
fun AppContent(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    when (state.screen) {
        Screen.LOADING -> MessageScreen(stringResource(R.string.loading), null, modifier)

        Screen.UNREADABLE -> MessageScreen(
            stringResource(R.string.unreadable_title),
            stringResource(R.string.unreadable_body),
            modifier,
        )

        // The bar shows on the tabs, the form and the review (spec 6: always visible; 02b decision 1).
        Screen.TABS, Screen.FORM, Screen.REVIEW -> Column(modifier = modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                BarContent(state, viewModel)
            }
            BottomBar(DEFAULT_TAB_ORDER, state.tab, viewModel::selectTab)
        }
    }
}

@Composable
private fun BarContent(
    state: AppState,
    viewModel: AppViewModel,
) {
    BackHandler(enabled = state.backGoesToOverall) { viewModel.selectTab(Tab.OVERALL) }
    when (state.screen) {
        Screen.FORM -> OpeningBalanceForm(
            state = state,
            onDateChange = viewModel::onDateChange,
            onAmountChange = viewModel::onAmountChange,
            onNoteChange = viewModel::onNoteChange,
            onReview = viewModel::review,
            onBack = viewModel::closeForm,
        )

        Screen.REVIEW -> OpeningBalanceReview(state, viewModel::confirm, viewModel::backToEdit)

        else -> when (state.tab) {
            Tab.OVERALL -> OverallScreen(state, viewModel::openForm)
            Tab.CASH -> CashScreen(state, viewModel::openForm)
            Tab.PORTFOLIOS -> PortfoliosScreen()
            Tab.MORE -> MoreScreen()
        }
    }
}

@Composable
private fun MessageScreen(
    title: String,
    body: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
        )
        body?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}
