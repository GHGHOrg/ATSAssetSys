package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.ClassList
import app.ghghorg.atsassets.core.ClassLists
import app.ghghorg.atsassets.core.Classification
import app.ghghorg.atsassets.core.ListRefusal
import app.ghghorg.atsassets.core.ListResult
import app.ghghorg.atsassets.core.ListUsers

/*
 * A sector or geography list as its screen draws it (section 6, More and Settings entry; wireframe 10f frames F and G;
 * CR2, 25): each value with the holdings that use it and the remembered texts that map to it, the line under the list
 * after an add, rename or delete, and the Name layer with its line. Plain Kotlin: the screen picks the approved string
 * and puts these in.
 */

/** One value: [holdings] that use it, hidden ones included (4.9); [alsoMatches] its remembered texts joined, or null. */
data class ClassValueRowModel(
    val value: String,
    val holdings: Int,
    val alsoMatches: String?,
)

/** The line under the list (frame G, marker 15): what the last add, rename or delete did, or why a delete was refused. */
sealed interface ClassListLine {
    data class Added(
        val value: String,
    ) : ClassListLine

    /** A rename that remembered nothing (decision 1). */
    data class Renamed(
        val from: String,
        val to: String,
    ) : ClassListLine

    /** A rename that remembered the source's [text], which still maps to [value] (4.9). */
    data class RenamedRemembering(
        val text: String,
        val value: String,
    ) : ClassListLine

    data class Deleted(
        val value: String,
    ) : ClassListLine

    /** "Not deleted": [value] is used by [holdings] holdings, hidden ones included. */
    data class InUse(
        val value: String,
        val holdings: Int,
    ) : ClassListLine

    /** The store could not delete it. */
    data object NotDeleted : ClassListLine
}

/** The Name layer's line (frame G): why the typed name was refused, or that the store could not save it. */
sealed interface ClassNameLine {
    data object Blank : ClassNameLine

    data class Duplicate(
        val value: String,
    ) : ClassNameLine

    data class Remembered(
        val text: String,
        val value: String,
    ) : ClassNameLine

    data object NotSaved : ClassNameLine
}

/** The Name layer (marker 14): Add value when [renaming] is null, else Rename of it; [text] as typed. */
data class ClassNameEditModel(
    val renaming: String?,
    val text: String,
    val line: ClassNameLine?,
)

/** [list]'s screen; [enabled] is false while a save runs. */
data class ClassListModel(
    val list: ClassList,
    val rows: List<ClassValueRowModel>,
    val line: ClassListLine?,
    val edit: ClassNameEditModel?,
    val enabled: Boolean,
)

object ClassListScreens {
    /**
     * [list] of [lists] in its order, each value's holdings among [users] by K10's rule and its remembered texts; with
     * [edit] open its line comes from [outcome] (a refused name) or [saveRefusal]; otherwise the screen's line does.
     */
    fun of(
        list: ClassList,
        lists: ClassLists,
        users: ListUsers,
        edit: ClassValueEdit?,
        outcome: ListResult?,
        saveRefusal: SaveRefusal?,
        saving: Boolean,
    ): ClassListModel {
        val remembered = lists.rememberedOn(list)
        val rows =
            lists[list].map { value ->
                val texts = remembered.filter { it.value == value }.map { it.text }
                ClassValueRowModel(
                    value = value,
                    holdings = Classification.holdingsUsing(lists, list, value, users),
                    alsoMatches = texts.takeIf { it.isNotEmpty() }?.joinToString(", "),
                )
            }
        val editModel = edit?.let { ClassNameEditModel(it.renaming, it.text, nameLine(outcome, saveRefusal)) }
        return ClassListModel(
            list = list,
            rows = rows,
            line = if (edit == null) listLine(outcome, saveRefusal) else null,
            edit = editModel,
            enabled = !saving,
        )
    }

    private fun nameLine(
        outcome: ListResult?,
        saveRefusal: SaveRefusal?,
    ): ClassNameLine? {
        if (saveRefusal != null) return ClassNameLine.NotSaved
        val refusal = (outcome as? ListResult.Refused)?.refusal ?: return null
        return when (refusal) {
            ListRefusal.Blank -> ClassNameLine.Blank
            is ListRefusal.Duplicate -> ClassNameLine.Duplicate(refusal.value)
            is ListRefusal.Remembered -> ClassNameLine.Remembered(refusal.text, refusal.value)
            is ListRefusal.InUse -> null
        }
    }

    private fun listLine(
        outcome: ListResult?,
        saveRefusal: SaveRefusal?,
    ): ClassListLine? {
        if (saveRefusal != null) return ClassListLine.NotDeleted
        return when (outcome) {
            null -> null
            is ListResult.Added -> ClassListLine.Added(outcome.value)
            is ListResult.Renamed -> renamedLine(outcome)
            is ListResult.Deleted -> ClassListLine.Deleted(outcome.value)
            is ListResult.Refused -> (outcome.refusal as? ListRefusal.InUse)?.let { ClassListLine.InUse(it.value, it.holdings) }
        }
    }

    private fun renamedLine(renamed: ListResult.Renamed): ClassListLine {
        val text = renamed.remembered ?: return ClassListLine.Renamed(renamed.from, renamed.to)
        return ClassListLine.RenamedRemembering(text, renamed.to)
    }
}
