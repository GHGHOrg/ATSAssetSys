package app.ghghorg.atsassets.ui

/*
 * The price source and its API key as Settings draws them (wireframe 10f marker 9; CR2; 30). A saved key is never in a
 * model: only its last characters (decision 4), and those spaced out for TalkBack, which then reads them one by one.
 * Plain Kotlin: the screen picks the approved string for each.
 */

/**
 * Settings' Price source row: the [source]'s name, whether a key is saved, and the key's ending, empty when nothing of
 * it may be shown.
 */
data class PriceSourceRow(
    val source: String,
    val keySet: Boolean,
    val keyEnding: String,
    val spokenEnding: String,
)

/**
 * The key entry (30): the source, the saved key's state as in the row, the text as typed, why the last Save or Clear did
 * not go through, [busy] while the key is checked or stored (the field and the buttons then rest), whether Save is
 * active (something typed and not busy), and whether the clear confirmation shows in place of the field.
 */
data class KeyEntryModel(
    val source: String,
    val keySet: Boolean,
    val keyEnding: String,
    val spokenEnding: String,
    val text: String,
    val refusal: KeyRefusal?,
    val busy: Boolean,
    val saveEnabled: Boolean,
    val confirmingClear: Boolean,
)

/** Builds the two models from the state (30). */
object PriceKeys {
    fun row(
        source: String,
        keySet: Boolean,
        ending: String,
    ): PriceSourceRow = PriceSourceRow(source = source, keySet = keySet, keyEnding = ending, spokenEnding = spoken(ending))

    /** The entry while it is open, null otherwise. */
    fun entry(
        entry: KeyEntry?,
        source: String,
        keySet: Boolean,
        ending: String,
        saving: Boolean,
    ): KeyEntryModel? {
        if (entry == null) return null
        return KeyEntryModel(
            source = source,
            keySet = keySet,
            keyEnding = ending,
            spokenEnding = spoken(ending),
            text = entry.text,
            refusal = entry.refusal,
            busy = saving,
            saveEnabled = !saving && entry.text.isNotBlank(),
            confirmingClear = entry.confirmingClear,
        )
    }

    // "a1b2" as "a 1 b 2", so TalkBack reads four characters and not a word.
    private fun spoken(ending: String): String = ending.toList().joinToString(" ")
}
