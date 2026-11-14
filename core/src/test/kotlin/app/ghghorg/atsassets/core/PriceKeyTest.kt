package app.ghghorg.atsassets.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The price source's API key as text (30): what counts as a key, and the part of it a screen may show. */
class PriceKeyTest {
    @Test
    fun lettersAndDigits_areAKey() {
        assertTrue(PriceKey.isKey("abc123XYZ789"))
        assertTrue(PriceKey.isKey("a"))
        assertTrue(PriceKey.isKey("0"))
    }

    @Test
    fun nothingTyped_isNoKey() {
        assertFalse(PriceKey.isKey(""))
    }

    @Test
    fun anyOtherCharacter_isNoKey() {
        // A space, a line break or a colon would change the request header the key travels in (30, risk 3).
        val bad = listOf("abc 123", " abc123", "abc123 ", "abc-123", "abc_123", "abc.123", "abc:123", "abc\n123", "abc\r123", "abc\t123")
        for (text in bad) assertFalse(text, PriceKey.isKey(text))
    }

    @Test
    fun lettersOutsideAToZ_areNoKey() {
        // A header takes plain letters only: an accented letter, a full-width letter and a non-Latin digit are refused.
        for (text in listOf("abcé123", "abcｋ123", "abc٣123")) assertFalse(text, PriceKey.isKey(text))
    }

    @Test
    fun theEnding_isTheLastFourCharacters() {
        assertEquals("a1b2", PriceKey.ending("c0ffee0123456789a1b2"))
        assertEquals("2345", PriceKey.ending("12345"))
    }

    @Test
    fun aKeyOfFourCharactersOrFewer_showsNothing() {
        // Its ending would be the whole key, which is never shown (30 decision 4).
        assertEquals("", PriceKey.ending("a1b2"))
        assertEquals("", PriceKey.ending("ab"))
        assertEquals("", PriceKey.ending(""))
    }
}
