package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.AutoLock

/*
 * Settings as the screen draws it (section 6, More and Settings entry; wireframe 10f frames B and C; CR2, 20): the
 * Security section's Auto-lock row with the choice in force, and the four choices with the default marked. Plain
 * Kotlin: the screen picks the approved string for each.
 */

/** One choice of the Auto-lock layer, in the order of section 9; [isDefault] carries "(default)" (marker 7). */
data class AutoLockOption(
    val choice: AutoLock,
    val isDefault: Boolean,
)

/** [autoLock] is the choice the lock follows; [notSaved] when the last choice could not be saved. */
data class SettingsModel(
    val autoLock: AutoLock,
    val options: List<AutoLockOption>,
    val notSaved: Boolean,
)

object Settings {
    /** A choice not yet loaded shows "every time you leave", the rule the lock keeps until then (20). */
    fun of(
        autoLock: AutoLock?,
        refusal: SaveRefusal?,
    ): SettingsModel = SettingsModel(
        autoLock = autoLock ?: AutoLock.EVERY_LEAVE,
        options = AutoLock.entries.map { AutoLockOption(it, isDefault = it == AutoLock.EVERY_LEAVE) },
        notSaved = refusal != null,
    )
}
