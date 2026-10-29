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
import androidx.compose.ui.semantics.clearAndSetSemantics
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

        // The bar shows on every screen after loading (spec 6: always visible; 02b decision 1).
        Screen.TABS, Screen.FORM, Screen.REVIEW, Screen.DETAIL, Screen.REVERSAL -> Box(modifier = modifier.fillMaxSize()) {
            val dialog = state.portfolioDialog
            // While the dialog is open, TalkBack reads only the dialog, not the screen under it.
            val under = if (dialog != null) Modifier.clearAndSetSemantics { } else Modifier
            Column(modifier = Modifier.fillMaxSize().then(under)) {
                Box(modifier = Modifier.weight(1f)) {
                    BarContent(state, viewModel)
                }
                BottomBar(DEFAULT_TAB_ORDER, state.tab, viewModel::selectTab)
            }
            // The New portfolio dialog, over Portfolios or Overall and the dimmed bar (wireframe 10d frame A).
            if (dialog != null) {
                NewPortfolioDialog(
                    dialog = dialog,
                    saving = state.saving,
                    onNameChange = viewModel::onPortfolioNameChange,
                    onCreate = viewModel::createPortfolio,
                    onCancel = viewModel::cancelPortfolioDialog,
                )
            }
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
        Screen.FORM -> CashEntryForm(
            state = state,
            onTypeChange = viewModel::onTypeChange,
            onDirectionChange = viewModel::onDirectionChange,
            onDateChange = viewModel::onDateChange,
            onAmountChange = viewModel::onAmountChange,
            onNoteChange = viewModel::onNoteChange,
            onReview = viewModel::review,
            onBack = viewModel::closeForm,
        )

        Screen.REVIEW -> CashEntryReview(state, viewModel::confirm, viewModel::backToEdit)

        Screen.DETAIL -> CashEntryDetail(
            state = state,
            onOpenEntry = viewModel::openDetail,
            onReverse = viewModel::openReversal,
            onBack = viewModel::closeDetail,
        )

        Screen.REVERSAL -> CashReversal(
            state = state,
            onDateChange = viewModel::onReversalDateChange,
            onNoteChange = viewModel::onReversalNoteChange,
            onConfirm = viewModel::confirmReversal,
            onCancel = viewModel::cancelReversal,
        )

        else -> TabContent(state, viewModel)
    }
}

@Composable
private fun TabContent(
    state: AppState,
    viewModel: AppViewModel,
) {
    when (state.tab) {
        Tab.OVERALL -> OverallScreen(state, viewModel::openForm, viewModel::openPortfolioDialog)

        Tab.CASH -> CashScreen(
            state = state,
            onCashEntry = viewModel::openForm,
            onOpenEntry = viewModel::openDetail,
            filterActions =
            CashFilterActions(
                onFromChange = viewModel::onFilterFromChange,
                onToChange = viewModel::onFilterToChange,
                onTypeChange = viewModel::onFilterTypeChange,
                onClear = viewModel::clearFilters,
            ),
        )

        Tab.PORTFOLIOS -> PortfoliosScreen(state, viewModel::openPortfolioDialog, viewModel::openForm)

        Tab.MORE -> MoreScreen()
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
