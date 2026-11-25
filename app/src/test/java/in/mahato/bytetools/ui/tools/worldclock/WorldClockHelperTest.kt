package `in`.mahato.bytetools.ui.tools.worldclock

import org.junit.Assert.*
import org.junit.Test
import java.util.*

class WorldClockHelperTest {

    @Test
    fun testFindZone() {
        val ny = WorldClockHelper.findZone("America/New_York")
        assertEquals("New York", ny.city)
        assertEquals("United States", ny.country)

        val tokyo = WorldClockHelper.findZone("Asia/Tokyo")
        assertEquals("Tokyo", tokyo.city)
        assertEquals("Japan", tokyo.country)

        val london = WorldClockHelper.findZone("Europe/London")
        assertEquals("London", london.city)

        // Custom unknown zone ID should fallback gracefully
        val custom = WorldClockHelper.findZone("Pacific/Tahiti")
        assertEquals("Tahiti", custom.city)
    }

    @Test
    fun testCuratedZonesNonEmpty() {
        val zones = WorldClockHelper.getAllZones()
        assertTrue("Curated zones should contain more than 50 cities", zones.size > 50)
        assertTrue(zones.any { it.id == "Asia/Tokyo" })
        assertTrue(zones.any { it.id == "America/New_York" })
        assertTrue(zones.any { it.id == "Europe/London" })
        assertTrue(zones.any { it.id == "Asia/Dubai" })
    }

    @Test
    fun testTimeDifferenceMinutes() {
        val tokyoTz = TimeZone.getTimeZone("Asia/Tokyo") // UTC+9 always (no DST)
        val utcTz = TimeZone.getTimeZone("UTC")

        val diffMinutes = WorldClockHelper.getDifferenceMinutes(tokyoTz, utcTz, Date())
        assertEquals(540L, diffMinutes) // 9 hours * 60 = 540
    }

    @Test
    fun testFormatDifferenceText() {
        val tokyoTz = TimeZone.getTimeZone("Asia/Tokyo")
        val utcTz = TimeZone.getTimeZone("UTC")

        val diffTextAhead = WorldClockHelper.formatDifferenceText(tokyoTz, utcTz, Date())
        assertEquals("9h ahead", diffTextAhead)

        val diffTextBehind = WorldClockHelper.formatDifferenceText(utcTz, tokyoTz, Date())
        assertEquals("9h behind", diffTextBehind)

        val diffSame = WorldClockHelper.formatDifferenceText(utcTz, utcTz, Date())
        assertEquals("Same time", diffSame)
    }

    @Test
    fun testDayShift() {
        val utcTz = TimeZone.getTimeZone("UTC")
        val tokyoTz = TimeZone.getTimeZone("Asia/Tokyo") // +9h

        // When UTC is 23:00 on Nov 24, Tokyo is 08:00 on Nov 25 (+1 day)
        val cal = Calendar.getInstance(utcTz).apply {
            set(2026, Calendar.NOVEMBER, 24, 23, 0, 0)
        }
        val shift = WorldClockHelper.getDayShift(tokyoTz, utcTz, cal.time)
        assertEquals(1, shift)

        // Inverse should be -1 day
        val shiftReverse = WorldClockHelper.getDayShift(utcTz, tokyoTz, cal.time)
        assertEquals(-1, shiftReverse)
    }

    @Test
    fun testBusinessHoursOverlap() {
        val londonTz = TimeZone.getTimeZone("Europe/London")
        val parisTz = TimeZone.getTimeZone("Europe/Paris") // 1 hour difference

        // London and Paris (1 hour apart) should have 8 hours of overlap in 9am-6pm window
        val result = WorldClockHelper.calculateBusinessHoursOverlap(londonTz, parisTz, Date())
        assertTrue(result.hasOverlap)
        assertTrue(result.overlapMinutes > 0)
    }

    @Test
    fun testWorkSuitability() {
        val utcTz = TimeZone.getTimeZone("UTC")

        // 14:00 UTC (2 PM) should be CONVENIENT (office hours 9-18)
        val afternoonCal = Calendar.getInstance(utcTz).apply {
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 0)
        }
        assertEquals(WorkSuitability.CONVENIENT, WorldClockHelper.getWorkSuitability(utcTz, afternoonCal.time))

        // 08:00 UTC (8 AM) should be BORDERLINE
        val morningCal = Calendar.getInstance(utcTz).apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
        }
        assertEquals(WorkSuitability.BORDERLINE, WorldClockHelper.getWorkSuitability(utcTz, morningCal.time))

        // 02:00 UTC (2 AM) should be SLEEP
        val nightCal = Calendar.getInstance(utcTz).apply {
            set(Calendar.HOUR_OF_DAY, 2)
            set(Calendar.MINUTE, 0)
        }
        assertEquals(WorkSuitability.SLEEP, WorldClockHelper.getWorkSuitability(utcTz, nightCal.time))
    }
}
