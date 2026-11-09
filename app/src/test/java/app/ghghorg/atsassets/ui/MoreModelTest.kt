package app.ghghorg.atsassets.ui

import org.junit.Assert.assertEquals
import org.junit.Test

// 17 and 19: More as the screen draws it (section 6, More; wireframe 10f; 17 and 19 decision 1; CR2).
class MoreModelTest {
    // No backup until 31: "never backed up". Net Worth (Total) and Allocation, in wireframe 10f's order, are built so far.
    @Test
    fun more_isNeverBackedUp_withTheNetWorthAndAllocationEntries() {
        assertEquals(MoreModel(lastBackup = null, entries = listOf(MoreEntry.NET_WORTH, MoreEntry.ALLOCATION)), More.of())
    }
}
