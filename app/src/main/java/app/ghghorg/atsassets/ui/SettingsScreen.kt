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
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
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
import app.ghghorg.atsassets.core.ClassList

/** Settings' callbacks, passed together. Allow and Turn off are MainActivity's, which owns the screenshot window (21). */
class SettingsActions(
    val onBack: () -> Unit,
    val onOpenChoice: () -> Unit,
    val onCloseChoice: () -> Unit,
    val onChoose: (AutoLock) -> Unit,
    val onOpenScreenshots: () -> Unit = {},
    val onCloseScreenshots: () -> Unit = {},
    val onAllowScreenshots: () -> Unit = {},
    val onTurnOffScreenshots: () -> Unit = {},
    val onOpenTabOrder: () -> Unit = {},
    val onOpenList: (ClassList) -> Unit = {},
    // 30: the Price source row opens the key entry, which AppContent draws over the bar.
    val onOpenPriceSource: () -> Unit = {},
)

/**
 * Settings (section 6, wireframe 10f frame B; iterations 20 and 21), drawn from its model (CR2): "<" and the title,
 * then the Security section (marker 3) with Auto-lock and its current choice, which opens the choice of four in the
 * shared choice layer with the default marked and "Takes effect at once. Saved." (frame C, marker 7), and the Screenshot
 * window with "Off" or its time left, which opens its layer (marker 8); the Navigation section with the Tab order and
 * the order in force, which opens the Tab order screen (marker 5, 22). Only what is built shows: the other sections
 * follow with their iterations. Back returns to More.
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
        // While a layer is open, TalkBack reads only the layer.
        val under = if (state.autoLockChoiceOpen || state.screenshotLayerOpen) Modifier.clearAndSetSemantics { } else Modifier
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
                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                val left = model.screenshotLeft
                val shown = if (left == null) stringResource(R.string.screenshot_off) else stringResource(R.string.screenshot_on_row, left.text)
                SettingRow(
                    name = stringResource(R.string.settings_screenshot),
                    value = shown,
                    spoken = stringResource(R.string.settings_screenshot_spoken, screenshotSpoken(left)),
                    onOpen = actions.onOpenScreenshots,
                    enabled = !state.saving,
                )
            }
            // 30: the Prices section (marker 4) with the one price source and its key's state (marker 9).
            model.priceSource?.let { PricesSection(it, actions.onOpenPriceSource, enabled = !state.saving) }
            Text(
                text = stringResource(R.string.settings_navigation),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                val order = model.tabOrder.map { stringResource(it.labelRes) }
                val value = stringResource(R.string.tab_order_value, *order.toTypedArray())
                SettingRow(
                    name = stringResource(R.string.settings_tab_order),
                    value = value,
                    spoken = stringResource(R.string.settings_tab_order_spoken, value),
                    onOpen = actions.onOpenTabOrder,
                    enabled = !state.saving,
                )
            }
            // 25: the Lists section (marker 6), each list with its number of values; each opens its list screen (frame F).
            Text(
                text = stringResource(R.string.settings_lists),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                model.lists.forEachIndexed { index, row ->
                    if (index > 0) HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                    val name = stringResource(if (row.list == ClassList.SECTOR) R.string.settings_sectors else R.string.settings_geographies)
                    val value = pluralStringResource(R.plurals.settings_list_values, row.values, row.values)
                    SettingRow(
                        name = name,
                        value = value,
                        spoken = stringResource(R.string.settings_list_spoken, name, value),
                        onOpen = { actions.onOpenList(row.list) },
                        enabled = !state.saving,
                    )
                }
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
        if (state.screenshotLayerOpen) {
            ScreenshotLayer(model.screenshotLeft, actions)
        }
    }
}

/**
 * The Prices section (wireframe 10f markers 4 and 9; 30): the source by its own name and "key ending in a1b2", "key
 * saved" when nothing of the key may be shown, or "no key"; TalkBack reads the ending's characters one by one. The row
 * opens the key entry; with one source there is no list to pick from (the 30 file, section 14).
 */
@Composable
private fun PricesSection(
    row: PriceSourceRow,
    onOpen: () -> Unit,
    enabled: Boolean,
) {
    Text(
        text = stringResource(R.string.settings_prices),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { heading() },
    )
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        val value =
            when {
                !row.keySet -> stringResource(R.string.price_source_no_key, row.source)
                row.keyEnding.isEmpty() -> stringResource(R.string.price_source_key_set, row.source)
                else -> stringResource(R.string.price_source_key_ending, row.source, row.keyEnding)
            }
        val spokenValue =
            if (row.keySet && row.keyEnding.isNotEmpty()) stringResource(R.string.price_source_key_ending, row.source, row.spokenEnding) else value
        SettingRow(
            name = stringResource(R.string.settings_price_source),
            value = value,
            spoken = stringResource(R.string.settings_price_source_spoken, spokenValue),
            onOpen = onOpen,
            enabled = enabled,
        )
    }
}

/** The screenshot window's state for TalkBack: "Off", or "on, 2 minutes 41 seconds left". */
@Composable
private fun screenshotSpoken(left: ScreenshotTimeLeft?): String {
    if (left == null) return stringResource(R.string.screenshot_off)
    val minutes = pluralStringResource(R.plurals.screenshot_minutes_spoken, left.minutes, left.minutes)
    val seconds = pluralStringResource(R.plurals.screenshot_seconds_spoken, left.seconds, left.seconds)
    return stringResource(R.string.screenshot_on_spoken, minutes, seconds)
}

/**
 * The Screenshot window's layer (wireframe 10f frame C, marker 8), drawn in the window. Off: the blocked line,
 * "Allow screenshots for 3 minutes" and the check line. On: the time left with "Ends when you leave the app.", the line
 * that anything can capture the screen, and Turn off. The fingerprint or PIN prompt is the system's own window.
 */
@Composable
private fun ScreenshotLayer(
    left: ScreenshotTimeLeft?,
    actions: SettingsActions,
) {
    LayerCard(title = stringResource(R.string.settings_screenshot), onClose = actions.onCloseScreenshots) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (left == null) {
                Text(text = stringResource(R.string.screenshot_off_line), style = MaterialTheme.typography.bodyLarge)
                Button(onClick = actions.onAllowScreenshots) {
                    Text(text = stringResource(R.string.screenshot_allow))
                }
                Text(
                    text = stringResource(R.string.screenshot_allow_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(text = stringResource(R.string.screenshot_on_line, left.text), style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = stringResource(R.string.screenshot_on_warning),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(onClick = actions.onTurnOffScreenshots) {
                    Text(text = stringResource(R.string.screenshot_turn_off))
                }
            }
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
