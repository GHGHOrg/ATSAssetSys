package app.ghghorg.atsassets.core

import java.util.Locale

/**
 * The bottom bar's order, left to right (spec 6, navigation outline), a setting (section 3): each tab once, so an order
 * can only be made by [of] or [parse], which refuse anything else (spec 8's state rows). [text] is spec 8's `tab_order`
 * value, which the store keeps as it is (22).
 */
class TabOrder private constructor(
    val tabs: List<Tab>,
) {
    /** The tabs' keys separated by semicolons, for example `more;cash;portfolios;overall`. */
    val text: String get() = tabs.joinToString(SEPARATOR) { it.key }

    fun canMoveUp(tab: Tab): Boolean = tabs.indexOf(tab) > 0

    fun canMoveDown(tab: Tab): Boolean = tabs.indexOf(tab) < tabs.lastIndex

    /** [tab] one place left; the first stays where it is (22 decision 1). */
    fun moveUp(tab: Tab): TabOrder = if (canMoveUp(tab)) swap(tabs.indexOf(tab) - 1) else this

    /** [tab] one place right; the last stays where it is (22 decision 1). */
    fun moveDown(tab: Tab): TabOrder = if (canMoveDown(tab)) swap(tabs.indexOf(tab)) else this

    /** The tabs at [left] and the one after it change places. */
    private fun swap(left: Int): TabOrder {
        val moved = tabs.toMutableList()
        moved[left] = tabs[left + 1]
        moved[left + 1] = tabs[left]
        return TabOrder(moved)
    }

    override fun equals(other: Any?): Boolean = other is TabOrder && other.tabs == tabs

    override fun hashCode(): Int = tabs.hashCode()

    override fun toString(): String = text

    companion object {
        private const val SEPARATOR = ";"

        /** More, Cash, Portfolios, Overall (spec 6; spec 8's value when the row is missing). */
        val DEFAULT = TabOrder(listOf(Tab.MORE, Tab.CASH, Tab.PORTFOLIOS, Tab.OVERALL))

        /** [tabs] as an order, or null unless it holds each of the four tabs once. */
        fun of(tabs: List<Tab>): TabOrder? = if (tabs.size == Tab.entries.size && tabs.toSet().size == tabs.size) TabOrder(tabs) else null

        /** Spec 8's text form, case ignored, or null when a key is unknown or the tabs are not each there once. */
        fun parse(text: String): TabOrder? {
            val tabs = text.split(SEPARATOR).map { key -> Tab.entries.find { it.key == key.lowercase(Locale.ROOT) } ?: return null }
            return of(tabs)
        }
    }
}
