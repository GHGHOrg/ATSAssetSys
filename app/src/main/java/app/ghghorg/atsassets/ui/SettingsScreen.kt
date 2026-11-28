package app.ghghorg.atsassets.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import app.ghghorg.atsassets.R
import app.ghghorg.atsassets.core.AutoLock
import app.ghghorg.atsassets.core.ClassList
import app.ghghorg.atsassets.ui.design.AtsBlockedMessage
import app.ghghorg.atsassets.ui.design.AtsButton
import app.ghghorg.atsassets.ui.design.AtsCard
import app.ghghorg.atsassets.ui.design.AtsLinkRow
import app.ghghorg.atsassets.ui.design.AtsRowDivider
import app.ghghorg.atsassets.ui.design.AtsScreen
import app.ghghorg.atsassets.ui.design.AtsSectionHeading
import app.ghghorg.atsassets.ui.design.AtsWarningLine
import app.ghghorg.atsassets.ui.design.ButtonKind
import app.ghghorg.atsassets.ui.theme.AtsTheme
import app.ghghorg.atsassets.ui.theme.tokenDp
import app.ghghorg.atsassets.ui.theme.typeStyle

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
 * Settings (section 6, wireframe 10f frame B; iterations 20 and 21), drawn from its model (CR2), in the design system (44a
 * step 8 group (e); drawing spec/wireframes/design/10f-more-settings.html frames B to D): under the title band, a medium
 * secondary heading over each section's card (note 3), each row its name with its current value under it and a chevron.
 * Security (marker 3): Auto-lock and its current choice, which opens the choice of four as radio rows with the default
 * marked, "Takes effect at once. Saved." and Close (frame C, marker 7); the Screenshot window with "Off" or its time left,
 * which opens its layer (marker 8). Prices (30), Navigation with the Tab order (marker 5, 22), Lists with each list's
 * number of values (25). A failed save as the blocked card. Back returns to More.
 */
@Composable
fun SettingsScreen(
    state: AppState,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = !state.saving, onBack = actions.onBack)
    val model = state.settings
    val enabled = !state.saving
    Box(modifier = modifier.fillMaxSize()) {
        // While a layer is open, TalkBack reads only the layer.
        val under = if (state.autoLockChoiceOpen || state.screenshotLayerOpen) Modifier.clearAndSetSemantics { } else Modifier
        AtsScreen(
            title = stringResource(R.string.settings_title),
            modifier = under,
            onBack = actions.onBack,
            backLabel = stringResource(R.string.back_button),
            backEnabled = enabled,
        ) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(tokenDp("spaceMargin")),
                verticalArrangement = Arrangement.spacedBy(tokenDp("spaceGap")),
            ) {
                if (model.notSaved) AtsBlockedMessage(stringResource(R.string.save_failed), prefix = null)
                AtsSectionHeading(stringResource(R.string.settings_security))
                AtsCard(padded = false) {
                    val value = autoLockLabel(model.autoLock)
                    AtsLinkRow(
                        name = stringResource(R.string.settings_auto_lock),
                        onOpen = actions.onOpenChoice,
                        value = value,
                        spoken = stringResource(R.string.settings_auto_lock_spoken, value),
                        enabled = enabled,
                    )
                    AtsRowDivider()
                    val left = model.screenshotLeft
                    val shown = if (left == null) stringResource(R.string.screenshot_off) else stringResource(R.string.screenshot_on_row, left.text)
                    AtsLinkRow(
                        name = stringResource(R.string.settings_screenshot),
                        onOpen = actions.onOpenScreenshots,
                        value = shown,
                        spoken = stringResource(R.string.settings_screenshot_spoken, screenshotSpoken(left)),
                        enabled = enabled,
                    )
                }
                // 30: the Prices section (marker 4) with the one price source and its key's state (marker 9).
                model.priceSource?.let { PricesSection(it, actions.onOpenPriceSource, enabled) }
                AtsSectionHeading(stringResource(R.string.settings_navigation))
                AtsCard(padded = false) {
                    val order = model.tabOrder.map { stringResource(it.labelRes) }
                    val value = stringResource(R.string.tab_order_value, *order.toTypedArray())
                    AtsLinkRow(
                        name = stringResource(R.string.settings_tab_order),
                        onOpen = actions.onOpenTabOrder,
                        value = value,
                        spoken = stringResource(R.string.settings_tab_order_spoken, value),
                        enabled = enabled,
                    )
                }
                // 25: the Lists section (marker 6), each list with its number of values; each opens its list screen (frame F).
                AtsSectionHeading(stringResource(R.string.settings_lists))
                AtsCard(padded = false) {
                    model.lists.forEachIndexed { index, row ->
                        if (index > 0) AtsRowDivider()
                        val name = stringResource(if (row.list == ClassList.SECTOR) R.string.settings_sectors else R.string.settings_geographies)
                        val value = pluralStringResource(R.plurals.settings_list_values, row.values, row.values)
                        AtsLinkRow(
                            name = name,
                            onOpen = { actions.onOpenList(row.list) },
                            value = value,
                            spoken = stringResource(R.string.settings_list_spoken, name, value),
                            enabled = enabled,
                        )
                    }
                }
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
                closeButton = true,
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
    AtsSectionHeading(stringResource(R.string.settings_prices))
    AtsCard(padded = false) {
        val value =
            when {
                !row.keySet -> stringResource(R.string.price_source_no_key, row.source)
                row.keyEnding.isEmpty() -> stringResource(R.string.price_source_key_set, row.source)
                else -> stringResource(R.string.price_source_key_ending, row.source, row.keyEnding)
            }
        val spokenValue =
            if (row.keySet && row.keyEnding.isNotEmpty()) stringResource(R.string.price_source_key_ending, row.source, row.spokenEnding) else value
        AtsLinkRow(
            name = stringResource(R.string.settings_price_source),
            onOpen = onOpen,
            value = value,
            spoken = stringResource(R.string.settings_price_source_spoken, spokenValue),
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
 * The Screenshot window's layer (wireframe 10f frames D and F, marker 8), drawn in the window. Off: the state in medium,
 * "Allow screenshots for 3 minutes" gold with how it asks as a note. On: the time left with "Ends when you leave the app."
 * in medium, D-34's warning line that anything can capture the screen, and Turn off gold. Close a text button (note 6).
 * The fingerprint or PIN prompt is the system's own window.
 */
@Composable
private fun ScreenshotLayer(
    left: ScreenshotTimeLeft?,
    actions: SettingsActions,
) {
    val colors = AtsTheme.colors
    LayerCard(title = stringResource(R.string.settings_screenshot), onClose = actions.onCloseScreenshots) {
        Column(
            modifier = Modifier.padding(horizontal = tokenDp("layerDialogPadding"), vertical = tokenDp("fieldDensePadding")),
            verticalArrangement = Arrangement.spacedBy(tokenDp("layerDialogGap")),
        ) {
            if (left == null) {
                Text(text = stringResource(R.string.screenshot_off_line), style = typeStyle("button"), color = colors.text)
                AtsButton(stringResource(R.string.screenshot_allow), actions.onAllowScreenshots, kind = ButtonKind.FILLED)
                Text(text = stringResource(R.string.screenshot_allow_note), style = typeStyle("note"), color = colors.textSecondary)
            } else {
                Text(text = stringResource(R.string.screenshot_on_line, left.text), style = typeStyle("button"), color = colors.text)
                AtsWarningLine(stringResource(R.string.screenshot_on_warning))
                AtsButton(stringResource(R.string.screenshot_turn_off), actions.onTurnOffScreenshots, kind = ButtonKind.FILLED)
            }
        }
        LayerClose(actions.onCloseScreenshots)
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
