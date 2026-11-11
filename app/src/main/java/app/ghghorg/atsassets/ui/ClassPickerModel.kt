package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.ClassList

/*
 * The Edit picker of a holding's sector or geography as its screen draws it (wireframe 10f frame H; CR2, 24). Plain
 * Kotlin: the screen picks the approved string and puts these in.
 */

/** One value of the list, in the list's order, and whether it is the one picked. */
data class ClassPickerRowModel(
    val value: String,
    val selected: Boolean,
)

/**
 * The picker for [ticker]'s [list]: its [rows] (empty: the plain "No sectors yet." line and no Save, marker 17), whether
 * Save can be tapped, and [notSaved] after a failed save (the shared "Not saved" line).
 */
data class ClassPickerModel(
    val list: ClassList,
    val ticker: String,
    val rows: List<ClassPickerRowModel>,
    val canSave: Boolean,
    val notSaved: Boolean,
)

object ClassPicker {
    /** [values] of [list] in its order, [choice] checked; Save while a value is picked and no save runs (4.9, frame H). */
    fun of(
        list: ClassList,
        ticker: String,
        values: List<String>,
        choice: String?,
        saving: Boolean,
        refusal: SaveRefusal?,
    ): ClassPickerModel = ClassPickerModel(
        list = list,
        ticker = ticker,
        rows = values.map { ClassPickerRowModel(it, selected = it == choice) },
        canSave = choice != null && choice in values && !saving,
        notSaved = refusal != null,
    )
}
