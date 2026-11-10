package app.ghghorg.atsassets.core

/** The two lists of spec 3 and 4.9. */
enum class ClassList {
    SECTOR,
    GEOGRAPHY,
}

/** Where a holding's effective sector or geography comes from (view 6's labels; 23a). The override is added in 24. */
enum class ClassOrigin {
    SOURCE,
    UNCLASSIFIED,
}

/** A holding's effective sector or geography: [value] as its list spells it, null when [origin] is UNCLASSIFIED (C14). */
data class EffectiveClass(
    val value: String?,
    val origin: ClassOrigin,
)

/**
 * K10, the part 23a needs (spec 4.9, section 14): the source's text mapped to a list value by name with D7's key
 * ([Portfolios.nameKey]: case and spaces ignored), or added to the list; and the effective value, the source's value
 * or "Unclassified". No remembered source text exists before a rename (25), so none is looked up yet (decision 4).
 */
object Classification {
    private const val NOT_AVAILABLE = "N/A"

    /** The source's text as a value, trimmed, or null when there is none: missing, blank or "N/A" (decision 2). */
    fun sourceText(text: String?): String? {
        val trimmed = text?.trim() ?: return null
        if (trimmed.isEmpty() || Portfolios.nameKey(trimmed) == Portfolios.nameKey(NOT_AVAILABLE)) return null
        return trimmed
    }

    /** The value on [list] that the source's text matches (D7), in the list's own spelling, or null. */
    fun valueFor(
        list: List<String>,
        sourceText: String?,
    ): String? {
        val text = sourceText(sourceText) ?: return null
        val key = Portfolios.nameKey(text)
        return list.firstOrNull { Portfolios.nameKey(it) == key }
    }

    /** [list] after the source's text: unchanged when the text is no value or matches a value; else with it added last. */
    fun withSource(
        list: List<String>,
        sourceText: String?,
    ): List<String> {
        val text = sourceText(sourceText) ?: return list
        return if (valueFor(list, text) != null) list else list + text
    }

    /**
     * The effective value without an override (24): the list's value for the source's text, or the text itself while
     * the list does not hold it yet, or "Unclassified" when the source gives no value.
     */
    fun effective(
        list: List<String>,
        sourceText: String?,
    ): EffectiveClass {
        val text = sourceText(sourceText) ?: return EffectiveClass(null, ClassOrigin.UNCLASSIFIED)
        return EffectiveClass(valueFor(list, text) ?: text, ClassOrigin.SOURCE)
    }
}
