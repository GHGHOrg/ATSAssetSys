package app.ghghorg.atsassets.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.core.AutoLock

/** Settings' callbacks, passed together. */
class SettingsActions(
    val onBack: () -> Unit,
    val onOpenChoice: () -> Unit,
    val onCloseChoice: () -> Unit,
    val onChoose: (AutoLock) -> Unit,
)

/**
 * Settings (section 6, wireframe 10f frame B; iteration 20), drawn from its model (CR2): "<" and the title, then the
 * Security section with Auto-lock and its current choice (marker 3), which opens the choice of four in the shared
 * choice layer with the default marked and "Takes effect at once. Saved." (frame C, marker 7). Only what is built
 * shows: the other rows and sections follow with their iterations. Back returns to More.
 */
@Composable
fun SettingsScreen(
    state: AppState,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = !state.saving, onBack = actions.onBack)
    val model = state.settings
    Box(modifier = modifier.fillMaxSize()) {
        // While the choice layer is open, TalkBack reads only the layer.
        val under = if (state.autoLockChoiceOpen) Modifier.clearAndSetSemantics { } else Modifier
        Column(
            modifier = Modifier.fillMaxSize().then(under).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TitleRow(stringResource(R.string.settings_title), actions.onBack, enabled = !state.saving)
            Text(
                text = stringResource(R.string.settings_security),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                val value = autoLockLabel(model.autoLock)
                SettingRow(
                    name = stringResource(R.string.settings_auto_lock),
                    value = value,
                    spoken = stringResource(R.string.settings_auto_lock_spoken, value),
                    onOpen = actions.onOpenChoice,
                    enabled = !state.saving,
                )
            }
            if (model.notSaved) {
                Text(
                    text = stringResource(R.string.save_failed),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }
        if (state.autoLockChoiceOpen) {
            val options = model.options.map { option ->
                val label = autoLockLabel(option.choice)
                option.choice to if (option.isDefault) stringResource(R.string.auto_lock_default, label) else label
            }
            ChoiceLayer(
                title = stringResource(R.string.settings_auto_lock),
                options = options,
                selected = model.autoLock,
                onChoose = actions.onChoose,
                onClose = actions.onCloseChoice,
                note = stringResource(R.string.auto_lock_note),
            )
        }
    }
}

/** An auto-lock choice's words (section 9). */
@Composable
private fun autoLockLabel(choice: AutoLock): String = stringResource(
    when (choice) {
        AutoLock.EVERY_LEAVE -> R.string.auto_lock_every_leave
        AutoLock.IDLE_1_MIN -> R.string.auto_lock_idle_1_min
        AutoLock.IDLE_5_MIN -> R.string.auto_lock_idle_5_min
        AutoLock.PHONE_LOCK_OR_RESTART -> R.string.auto_lock_phone_lock
    },
)

/**
 * One setting: its name, its current value under it and ">", read by TalkBack as [spoken] and a button. The value sits
 * under the name, not beside it, so a long choice fits at the largest font.
 */
@Composable
private fun SettingRow(
    name: String,
    value: String,
    spoken: String,
    onOpen: () -> Unit,
    enabled: Boolean,
) {
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, role = Role.Button, onClick = onOpen)
            .clearAndSetSemantics {
                contentDescription = spoken
                role = Role.Button
                if (enabled) {
                    onClick {
                        onOpen()
                        true
                    }
                }
            }.padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = name, style = MaterialTheme.typography.bodyLarge)
            Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(text = ">", style = MaterialTheme.typography.bodyLarge)
    }
}
