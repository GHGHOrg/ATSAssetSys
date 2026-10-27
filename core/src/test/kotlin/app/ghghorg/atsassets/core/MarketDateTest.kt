package app.ghghorg.atsassets.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class MarketDateTest {
    private fun todayAt(
        instant: String,
        clockZone: ZoneId = ZoneOffset.UTC,
    ): LocalDate = MarketDate(Clock.fixed(Instant.parse(instant), clockZone)).today()

    @Test
    fun today_lateEveningInNewYork_isStillThePreviousUtcDate() {
        // 03:07 UTC on Oct 24 is 23:07 in New York (EDT) on Oct 23. Tokyo is already on Oct 24.
        assertEquals(LocalDate.of(2026, 10, 23), todayAt("2026-10-24T03:07:55Z"))
    }

    @Test
    fun today_atNewYorkMidnightInSummer_changesTheDate() {
        assertEquals(LocalDate.of(2026, 10, 23), todayAt("2026-10-24T03:59:59Z"))
        assertEquals(LocalDate.of(2026, 10, 24), todayAt("2026-10-24T04:00:00Z"))
    }

    @Test
    fun today_atNewYorkMidnightInWinter_changesTheDate() {
        assertEquals(LocalDate.of(2027, 1, 14), todayAt("2027-01-15T04:59:59Z"))
        assertEquals(LocalDate.of(2027, 1, 15), todayAt("2027-01-15T05:00:00Z"))
    }

    @Test
    fun today_afterDaylightSavingEnds_usesTheWinterOffset() {
        // Daylight saving ends on Sunday 2026-11-01. After that New York is UTC-5.
        assertEquals(LocalDate.of(2026, 11, 1), todayAt("2026-11-01T04:00:00Z"))
        assertEquals(LocalDate.of(2026, 11, 1), todayAt("2026-11-02T04:59:59Z"))
        assertEquals(LocalDate.of(2026, 11, 2), todayAt("2026-11-02T05:00:00Z"))
    }

    @Test
    fun today_ignoresTheClockZone() {
        val tokyo = ZoneId.of("Asia/Tokyo")
        assertEquals(LocalDate.of(2026, 10, 23), todayAt("2026-10-24T03:07:55Z", tokyo))
    }

    @Test
    fun zone_isNewYork() {
        assertEquals("America/New_York", MarketDate.ZONE.id)
    }

    @Test
    fun parse_realDate_isAccepted() {
        assertEquals(LocalDate.of(2026, 1, 1), MarketDate.parse("2026-01-01"))
        assertEquals(LocalDate.of(2028, 2, 29), MarketDate.parse("2028-02-29"))
    }

    @Test
    fun parse_spacesAroundText_areTrimmed() {
        assertEquals(LocalDate.of(2026, 1, 1), MarketDate.parse(" 2026-01-01 "))
    }

    @Test
    fun parse_notARealDate_isNull() {
        val inputs = listOf("2026-02-30", "2026-02-29", "2026-13-01", "2026-00-10", "2026-01-32")
        for (text in inputs) {
            assertNull(text, MarketDate.parse(text))
        }
    }

    @Test
    fun parse_wrongShape_isNull() {
        val inputs = listOf("", "   ", "abc", "2026-1-5", "2026/01/01", "01-01-2026", "20260101")
        for (text in inputs) {
            assertNull(text, MarketDate.parse(text))
        }
    }
}
