package app.ghghorg.atsassets.core

/** The two lists of spec 3 and 4.9. */
enum class ClassList {
    SECTOR,
    GEOGRAPHY,
}

/** Where a holding's effective sector or geography comes from (view 6's labels; 23a, the override from 24). */
enum class ClassOrigin {
    OVERRIDE,
    SOURCE,
    UNCLASSIFIED,
}

/**
 * A remembered source text (spec 3, 4.9; 25): on [list], the source's [text] maps to [value], as the list spells it. A
 * rename remembers the value's old name when a source text matches it (decision 1).
 */
data class RememberedText(
    val list: ClassList,
    val text: String,
    val value: String,
)

/** The two lists' values, each in the order they were added (spec 3, 4.9; 23a), and their remembered texts (25). */
data class ClassLists(
    val sectors: List<String> = emptyList(),
    val geographies: List<String> = emptyList(),
    val remembered: List<RememberedText> = emptyList(),
) {
    operator fun get(list: ClassList): List<String> = if (list == ClassList.SECTOR) sectors else geographies

    /** The remembered texts on [list], in the order they were remembered. */
    fun rememberedOn(list: ClassList): List<RememberedText> = remembered.filter { it.list == list }
}

/**
 * What K10 needs to know which holdings use a value (4.9; 25): every holding as (portfolio id, ticker), hidden ones
 * included and whatever its shares; each instrument's source texts by ticker; every override.
 */
data class ListUsers(
    val holdings: Collection<Pair<String, String>> = emptyList(),
    val profiles: Map<String, SourceProfile> = emptyMap(),
    val overrides: List<ClassOverride> = emptyList(),
)

/** Why K10 refuses an add, a rename or a delete (4.9, wireframe 10f frame G; 25). */
sealed interface ListRefusal {
    /** The name is empty once trimmed. */
    data object Blank : ListRefusal

    /** The name matches (D7) the list's [value]. */
    data class Duplicate(
        val value: String,
    ) : ListRefusal

    /** The name matches (D7) the remembered [text] of the list's [value], the value renamed included (decision 2). */
    data class Remembered(
        val text: String,
        val value: String,
    ) : ListRefusal

    /** [value] is used by [holdings] holdings, hidden ones included. */
    data class InUse(
        val value: String,
        val holdings: Int,
    ) : ListRefusal
}

/** The outcome of an add, a rename or a delete (4.9; 25): refused, or [lists] as they are after it. */
sealed interface ListResult {
    data class Refused(
        val refusal: ListRefusal,
    ) : ListResult

    /** [value] added last, as typed but trimmed. */
    data class Added(
        val lists: ClassLists,
        val value: String,
    ) : ListResult

    /** [from] renamed [to]; [remembered] is the text a rename remembers (decision 1), null when none. */
    data class Renamed(
        val lists: ClassLists,
        val from: String,
        val to: String,
        val remembered: String?,
    ) : ListResult

    /** [value] and its remembered texts removed, and the overrides of holdings no longer there that named it (decision 3). */
    data class Deleted(
        val lists: ClassLists,
        val value: String,
        val overridesRemoved: List<ClassOverride>,
    ) : ListResult
}

/**
 * A holding's override (4.9; 24): the holding ([portfolioId], [ticker]) has [value] on [list], as the list spells it. The
 * store keeps the value's list row, so a rename (25) carries over (decision 3).
 */
data class ClassOverride(
    val portfolioId: String,
    val ticker: String,
    val list: ClassList,
    val value: String,
)

/** A holding's effective sector or geography: [value] as its list spells it, null when [origin] is UNCLASSIFIED (C14). */
data class EffectiveClass(
    val value: String?,
    val origin: ClassOrigin,
)

/**
 * K10 (spec 4.9, section 14): the source's text mapped to a list value through the remembered texts, then by name, with
 * D7's key ([Portfolios.nameKey]: case and spaces ignored), or added to the list; the effective value, the override, the
 * source's value or "Unclassified"; and (25) add, rename and delete with the holdings that use a value. Pure: the store
 * writes what these return.
 */
object Classification {
    private const val NOT_AVAILABLE = "N/A"

    /** The source's text as a value, trimmed, or null when there is none: missing, blank or "N/A" (decision 2). */
    fun sourceText(text: String?): String? {
        val trimmed = text?.trim() ?: return null
        if (trimmed.isEmpty() || Portfolios.nameKey(trimmed) == Portfolios.nameKey(NOT_AVAILABLE)) return null
        return trimmed
    }

    /**
     * The value on [list] that the source's text maps to, in the list's own spelling, or null: through [remembered] (the
     * list's remembered texts) first, then by name (4.9, D7).
     */
    fun valueFor(
        list: List<String>,
        sourceText: String?,
        remembered: List<RememberedText> = emptyList(),
    ): String? {
        val text = sourceText(sourceText) ?: return null
        val key = Portfolios.nameKey(text)
        val throughText = remembered.firstOrNull { Portfolios.nameKey(it.text) == key }?.value
        if (throughText != null) return list.firstOrNull { Portfolios.nameKey(it) == Portfolios.nameKey(throughText) }
        return list.firstOrNull { Portfolios.nameKey(it) == key }
    }

    /**
     * [list] after the source's text: unchanged when the text is no value or maps to a value (through [remembered] or by
     * name); else with it added last.
     */
    fun withSource(
        list: List<String>,
        sourceText: String?,
        remembered: List<RememberedText> = emptyList(),
    ): List<String> {
        val text = sourceText(sourceText) ?: return list
        return if (valueFor(list, text, remembered) != null) list else list + text
    }

    /**
     * The values [profiles] add to [lists] (4.9): each value text not yet on its list, once, sectors first, each in the
     * order of [profiles]. A text that matches a value (D7) or is no value adds nothing.
     */
    fun additions(
        lists: ClassLists,
        profiles: Collection<SourceProfile>,
    ): List<Pair<ClassList, String>> = ClassList.entries.flatMap { list ->
        val texts = profiles.map { textOn(list, it) }
        val remembered = lists.rememberedOn(list)
        val after = texts.fold(lists[list]) { values, text -> withSource(values, text, remembered) }
        after.drop(lists[list].size).map { list to it }
    }

    /** [lists] with the [additions] of [profiles] added, as the store keeps them after a refresh (23a). */
    fun extended(
        lists: ClassLists,
        profiles: Collection<SourceProfile>,
    ): ClassLists = additions(lists, profiles).fold(lists) { kept, (list, value) ->
        if (list == ClassList.SECTOR) kept.copy(sectors = kept.sectors + value) else kept.copy(geographies = kept.geographies + value)
    }

    /**
     * The effective value (4.9): the [override] if set (24, a value of the list); else the list's value for the source's
     * text, or the text itself while the list does not hold it yet; else "Unclassified" when the source gives no value.
     */
    fun effective(
        list: List<String>,
        sourceText: String?,
        override: String? = null,
        remembered: List<RememberedText> = emptyList(),
    ): EffectiveClass {
        if (override != null) return EffectiveClass(override, ClassOrigin.OVERRIDE)
        val text = sourceText(sourceText) ?: return EffectiveClass(null, ClassOrigin.UNCLASSIFIED)
        return EffectiveClass(valueFor(list, text, remembered) ?: text, ClassOrigin.SOURCE)
    }

    /** The source's text on [list] in [profile]: its sector or its geography. */
    fun textOn(
        list: ClassList,
        profile: SourceProfile?,
    ): String? = if (list == ClassList.SECTOR) profile?.sector else profile?.geography

    /**
     * The number of holdings in [users] that use [value] on [list] (4.9; 25): its override names the value, or its
     * instrument's source text maps to it (through the remembered texts, then by name), also while an override wins.
     * Hidden holdings count; an override of a holding not in [users] does not (decision 3).
     */
    fun holdingsUsing(
        lists: ClassLists,
        list: ClassList,
        value: String,
        users: ListUsers,
    ): Int {
        val key = Portfolios.nameKey(value)
        val remembered = lists.rememberedOn(list)
        return users.holdings.toSet().count { (portfolioId, ticker) ->
            val byOverride = users.overrides.any {
                it.portfolioId == portfolioId && it.ticker == ticker && it.list == list && Portfolios.nameKey(it.value) == key
            }
            val bySource = valueFor(lists[list], textOn(list, users.profiles[ticker]), remembered)
            byOverride || (bySource != null && Portfolios.nameKey(bySource) == key)
        }
    }

    /** Adds [name] to [list], trimmed, last (4.9; 25); refused when blank or when it matches a value or a remembered text. */
    fun add(
        lists: ClassLists,
        list: ClassList,
        name: String,
    ): ListResult {
        val trimmed = name.trim()
        nameRefusal(lists, list, trimmed, renaming = null)?.let { return ListResult.Refused(it) }
        return ListResult.Added(lists.withValues(list, lists[list] + trimmed), trimmed)
    }

    /** Why [name] (trimmed) cannot be a value on [list] (4.9): blank, another value's name, or any remembered text. */
    private fun nameRefusal(
        lists: ClassLists,
        list: ClassList,
        name: String,
        renaming: String?,
    ): ListRefusal? {
        if (name.isEmpty()) return ListRefusal.Blank
        val key = Portfolios.nameKey(name)
        lists[list].firstOrNull { it != renaming && Portfolios.nameKey(it) == key }?.let { return ListRefusal.Duplicate(it) }
        val text = lists.rememberedOn(list).firstOrNull { Portfolios.nameKey(it.text) == key } ?: return null
        return ListRefusal.Remembered(text.text, text.value)
    }

    private fun ClassLists.withValues(
        list: ClassList,
        values: List<String>,
    ): ClassLists = if (list == ClassList.SECTOR) copy(sectors = values) else copy(geographies = values)

    /** [value] as [list] spells it; throws when the list does not hold it (the caller names a shown value). */
    private fun ClassLists.spelling(
        list: ClassList,
        value: String,
    ): String = this[list].firstOrNull { it == value } ?: throw IllegalArgumentException("The value is not on its list.")

    /**
     * Renames [value] on [list] to [name], trimmed (4.9; 25). Refused when blank, when it matches another value, or when it
     * matches a remembered text on the list, the value's own included (decision 2). A change of case or spaces only is
     * allowed and remembers nothing. Otherwise the old name is remembered when one of [profiles]' source texts matches it
     * (decision 1); the value's remembered texts and the overrides (by the store's row) follow.
     */
    fun rename(
        lists: ClassLists,
        list: ClassList,
        value: String,
        name: String,
        profiles: Collection<SourceProfile>,
    ): ListResult {
        val from = lists.spelling(list, value)
        val to = name.trim()
        nameRefusal(lists, list, to, renaming = from)?.let { return ListResult.Refused(it) }
        val fromKey = Portfolios.nameKey(from)
        val caseOnly = Portfolios.nameKey(to) == fromKey
        val fromSource = profiles.any { profile -> sourceText(textOn(list, profile))?.let { Portfolios.nameKey(it) } == fromKey }
        val remembered = if (!caseOnly && fromSource) from else null
        val texts = lists.remembered.map { if (it.list == list && it.value == from) it.copy(value = to) else it } +
            listOfNotNull(remembered?.let { RememberedText(list, it, to) })
        val values = lists[list].map { if (it == from) to else it }
        return ListResult.Renamed(lists.withValues(list, values).copy(remembered = texts), from, to, remembered)
    }

    /**
     * Deletes [value] from [list] with its remembered texts (4.9; 25): refused while [holdingsUsing] counts any holding;
     * the overrides naming it of holdings no longer in [users] go with it (decision 3).
     */
    fun delete(
        lists: ClassLists,
        list: ClassList,
        value: String,
        users: ListUsers,
    ): ListResult {
        val gone = lists.spelling(list, value)
        val used = holdingsUsing(lists, list, gone, users)
        if (used > 0) return ListResult.Refused(ListRefusal.InUse(gone, used))
        val key = Portfolios.nameKey(gone)
        val left = lists.withValues(list, lists[list].filter { it != gone })
            .copy(remembered = lists.remembered.filterNot { it.list == list && it.value == gone })
        val overrides = users.overrides.filter { it.list == list && Portfolios.nameKey(it.value) == key }
        return ListResult.Deleted(left, gone, overrides)
    }

    /** The value [choice] picks as an override (4.9, 24): the value on [list] it matches (D7), in the list's spelling, or null. */
    fun overrideValue(
        list: List<String>,
        choice: String,
    ): String? {
        val key = Portfolios.nameKey(choice)
        return list.firstOrNull { Portfolios.nameKey(it) == key }
    }
}
