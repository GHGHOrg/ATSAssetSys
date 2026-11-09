package app.ghghorg.atsassets.ui

import org.junit.Assert.assertEquals
import org.junit.Test

// 17: More as the screen draws it (section 6, More; wireframe 10f; decision 1; CR2).
class MoreModelTest {
    // No backup until 31: "never backed up". Net Worth (Total) is the only entry built so far.
    @Test
    fun more_isNeverBackedUp_withTheNetWorthEntry() {
        assertEquals(MoreModel(lastBackup = null, entries = listOf(MoreEntry.NET_WORTH)), More.of())
    }
}
