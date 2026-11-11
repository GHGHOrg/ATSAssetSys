package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.ClassList
import org.junit.Assert.assertEquals
import org.junit.Test

// 24 step 5: the Edit picker as its screen draws it (wireframe 10f frame H, markers 16 and 17; 4.9; decision 1; CR2).
class ClassPickerModelTest {
    private val sectors = listOf("Technology", "Health Care", "Global")

    private fun of(
        values: List<String> = sectors,
        choice: String? = "Health Care",
        saving: Boolean = false,
        refusal: SaveRefusal? = null,
    ): ClassPickerModel = ClassPicker.of(ClassList.SECTOR, "AAPL", values, choice, saving, refusal)

    // Marker 16: every value of the list, in the list's order, the pick checked; Save can be tapped.
    @Test
    fun theRows_areTheList_inItsOrder_withThePickChecked() {
        assertEquals(
            ClassPickerModel(
                list = ClassList.SECTOR,
                ticker = "AAPL",
                rows =
                listOf(
                    ClassPickerRowModel("Technology", selected = false),
                    ClassPickerRowModel("Health Care", selected = true),
                    ClassPickerRowModel("Global", selected = false),
                ),
                canSave = true,
                notSaved = false,
            ),
            of(),
        )
    }

    // Marker 17: an empty list has no rows and no Save (the plain line instead).
    @Test
    fun anEmptyList_hasNoRows_andNoSave() {
        val model = of(values = emptyList(), choice = null)
        assertEquals(emptyList<ClassPickerRowModel>(), model.rows)
        assertEquals(false, model.canSave)
    }

    // 4.9: nothing picked yet (an ETF with no value), or a save running, and Save cannot be tapped.
    @Test
    fun withNothingPicked_orWhileSaving_saveCannotBeTapped() {
        assertEquals(false, of(choice = null).canSave)
        assertEquals(listOf(false, false, false), of(choice = null).rows.map { it.selected })
        assertEquals(false, of(saving = true).canSave)
    }

    // Risk 2: a failed save is said (the shared "Not saved" line).
    @Test
    fun aFailedSave_isSaid() {
        assertEquals(true, of(refusal = SaveRefusal.SAVE_FAILED).notSaved)
        assertEquals(false, of().notSaved)
    }
}
