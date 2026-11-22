package app.ghghorg.atsassets.core

import java.util.Locale

/** A portfolio (spec 3): an id, a name and the order it was created in. */
data class Portfolio(
    val id: String,
    val name: String,
    val createdSeq: Long,
)

sealed interface PortfolioRefusal {
    data object NoOpeningBalance : PortfolioRefusal

    data object TooShort : PortfolioRefusal

    data object NoLetter : PortfolioRefusal

    data class Duplicate(
        val otherName: String,
    ) : PortfolioRefusal
}

sealed interface PortfolioResult {
    data class Created(
        val portfolio: Portfolio,
    ) : PortfolioResult

    data class Refused(
        val reason: PortfolioRefusal,
    ) : PortfolioResult
}

sealed interface PortfolioRenameResult {
    data class Renamed(
        val portfolio: Portfolio,
    ) : PortfolioRenameResult

    /** The trimmed name equals the current one: nothing to save (wireframe 10d marker 4). */
    data object Unchanged : PortfolioRenameResult

    data class Refused(
        val reason: PortfolioRefusal,
    ) : PortfolioRenameResult

    data object NotFound : PortfolioRenameResult
}

/** The portfolio rules of spec 4.7 (create in iteration 07, rename in 08). */
object Portfolios {
    private const val MIN_LENGTH = 3

    /**
     * The first rule that fails decides the reason: the opening balance (4.2), then the name.
     * The name is saved without its leading and trailing spaces, with the typed casing (wireframe 10d).
     */
    fun create(
        existing: List<Portfolio>,
        cashEntries: List<CashEntry>,
        id: String,
        typedName: String,
    ): PortfolioResult {
        if (cashEntries.none { it.type == CashType.OPENING_BALANCE }) {
            return PortfolioResult.Refused(PortfolioRefusal.NoOpeningBalance)
        }
        nameRefusal(typedName, existing)?.let { return PortfolioResult.Refused(it) }
        val nextSeq = (existing.maxOfOrNull { it.createdSeq } ?: 0L) + 1L
        return PortfolioResult.Created(Portfolio(id = id, name = typedName.trim(), createdSeq = nextSeq))
    }

    /**
     * Renames the portfolio [id]: only the name changes (4.7). The rules are checked against the other portfolios
     * only, so a change of case or spaces of its own name is accepted (wireframe 10d marker 4, criterion 55).
     */
    fun rename(
        existing: List<Portfolio>,
        id: String,
        typedName: String,
    ): PortfolioRenameResult {
        val portfolio = existing.firstOrNull { it.id == id } ?: return PortfolioRenameResult.NotFound
        val name = typedName.trim()
        if (name == portfolio.name) return PortfolioRenameResult.Unchanged
        nameRefusal(name, existing.filter { it.id != id })?.let { return PortfolioRenameResult.Refused(it) }
        return PortfolioRenameResult.Renamed(portfolio.copy(name = name))
    }

    /**
     * The name rules in the order of wireframe 10d: at least 3 code points after trimming, at least one
     * character of the Unicode category Letter, and no match with [others] (4.7, D7). Null if the name passes.
     */
    fun nameRefusal(
        typedName: String,
        others: List<Portfolio>,
    ): PortfolioRefusal? {
        val name = typedName.trim()
        if (name.codePointCount(0, name.length) < MIN_LENGTH) return PortfolioRefusal.TooShort
        if (name.codePoints().noneMatch(Character::isLetter)) return PortfolioRefusal.NoLetter
        val key = nameKey(name)
        val match = others.firstOrNull { nameKey(it.name) == key }
        return match?.let { PortfolioRefusal.Duplicate(it.name) }
    }

    /**
     * The comparison key of 4.9 used by 4.7: leading and trailing spaces removed, inner runs of spaces made one,
     * case ignored. My reading: "spaces" are Unicode whitespace, and case is folded through upper case, so "ß"
     * matches "SS".
     */
    fun nameKey(name: String): String {
        val collapsed = StringBuilder()
        var inSpace = false
        for (char in name.trim()) {
            if (char.isWhitespace()) {
                inSpace = true
            } else {
                if (inSpace) collapsed.append(' ')
                collapsed.append(char)
                inSpace = false
            }
        }
        return collapsed.toString().uppercase(Locale.ROOT).lowercase(Locale.ROOT)
    }

    /** The list order of spec 3: the order the portfolios were created in. */
    fun inCreationOrder(portfolios: List<Portfolio>): List<Portfolio> = portfolios.sortedBy { it.createdSeq }
}
