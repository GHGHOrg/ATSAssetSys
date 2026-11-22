package app.ghghorg.atsassets.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** The digits-only date input of the Cash filters (iteration 06, after phone check 12). */
class DateDigitsTest {
    @Test
    fun format_insertsTheDashesAsTheDigitsArrive() {
        val expected =
            mapOf(
                "" to "",
                "2" to "2",
                "2026" to "2026",
                "20260" to "2026-0",
                "202601" to "2026-01",
                "2026010" to "2026-01-0",
                "20260105" to "2026-01-05",
            )
        for ((digits, shown) in expected) {
            assertEquals(digits, shown, DateDigits.format(digits))
        }
    }

    @Test
    fun format_leavesATextWithANonDigitAsTyped() {
        assertEquals("2a", DateDigits.format("2a"))
    }

    @Test
    fun theCursorMovesPastTheInsertedDashes() {
        val text = "20260105"
        val shown = listOf(0, 1, 2, 3, 4, 6, 7, 9, 10)
        for (offset in 0..text.length) {
            assertEquals("to shown $offset", shown[offset], DateDigits.toShownOffset(offset, text))
        }
    }

    @Test
    fun theCursorMapsBackFromTheShownText() {
        val text = "20260105"
        val typed = listOf(0, 1, 2, 3, 4, 4, 5, 6, 6, 7, 8)
        for (offset in 0..DateDigits.format(text).length) {
            assertEquals("to typed $offset", typed[offset], DateDigits.toTypedOffset(offset, text))
        }
    }

    @Test
    fun theCursorOfAShortTextStaysInsideTheShownText() {
        assertEquals(6, DateDigits.toShownOffset(5, "20260"))
        assertEquals(2, DateDigits.toShownOffset(2, "2a"))
    }

    @Test
    fun toDate_needsEightDigitsOfARealDate() {
        assertEquals(LocalDate.of(2026, 1, 5), DateDigits.toDate("20260105"))
        assertEquals(null, DateDigits.toDate("2026010"))
        assertEquals(null, DateDigits.toDate("20261301"))
        assertEquals(null, DateDigits.toDate("2026-01-05"))
    }

    @Test
    fun refusal_isNotDigitsAtOnce_notADateOnlyAtEightDigits() {
        assertEquals(null, DateDigits.refusal(""))
        assertEquals(null, DateDigits.refusal("2026013"))
        assertEquals(DateRefusal.NOT_DIGITS, DateDigits.refusal("x"))
        assertEquals(DateRefusal.NOT_A_DATE, DateDigits.refusal("20260230"))
        assertEquals(null, DateDigits.refusal("20260228"))
    }
}
