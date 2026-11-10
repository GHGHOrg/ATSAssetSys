package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.AutoLock
import app.ghghorg.atsassets.core.Tab
import app.ghghorg.atsassets.core.TabOrder

/*
 * Settings as the screen draws it (section 6, More and Settings entry; wireframe 10f frames B and C; CR2, 20 to 22): the
 * Security section's Auto-lock row with the choice in force, and the four choices with the default marked; the
 * Screenshot window row with its time left (marker 8); the Navigation section's Tab order row with the order in force
 * (marker 5). Plain Kotlin: the screen picks the approved string for each.
 */

/** One choice of the Auto-lock layer, in the order of section 9; [isDefault] carries "(default)" (marker 7). */
data class AutoLockOption(
    val choice: AutoLock,
    val isDefault: Boolean,
)

/** The screenshot window's time left as shown (marker 8): [text] as "m:ss", and [minutes] and [seconds] for TalkBack. */
data class ScreenshotTimeLeft(
    val text: String,
    val minutes: Int,
    val seconds: Int,
)

/**
 * [autoLock] is the choice the lock follows; [notSaved] when the last choice could not be saved; [screenshotLeft] the
 * screenshot window's time left, null while it is off; [tabOrder] the bottom bar's tabs, left to right.
 */
data class SettingsModel(
    val autoLock: AutoLock,
    val options: List<AutoLockOption>,
    val notSaved: Boolean,
    val screenshotLeft: ScreenshotTimeLeft? = null,
    val tabOrder: List<Tab> = emptyList(),
)

object Settings {
    /** A choice not yet loaded shows "every time you leave", the rule the lock keeps until then (20). */
    fun of(
        autoLock: AutoLock?,
        refusal: SaveRefusal?,
        screenshotLeftMs: Long? = null,
        tabOrder: TabOrder = TabOrder.DEFAULT,
    ): SettingsModel = SettingsModel(
        autoLock = autoLock ?: AutoLock.EVERY_LEAVE,
        options = AutoLock.entries.map { AutoLockOption(it, isDefault = it == AutoLock.EVERY_LEAVE) },
        notSaved = refusal != null,
        screenshotLeft = timeLeft(screenshotLeftMs),
        tabOrder = tabOrder.tabs,
    )

    /** Whole seconds rounded up, so the start shows 3:00 and the last second 0:01; nothing left is off. */
    private fun timeLeft(ms: Long?): ScreenshotTimeLeft? {
        if (ms == null || ms <= 0) return null
        val total = ((ms + 999) / 1_000).toInt()
        val minutes = total / 60
        val seconds = total % 60
        return ScreenshotTimeLeft(text = "$minutes:${seconds.toString().padStart(2, '0')}", minutes = minutes, seconds = seconds)
    }
}
