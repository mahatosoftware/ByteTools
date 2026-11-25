package `in`.mahato.bytetools.ui.tools.worldclock

import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

data class WorldZone(
    val id: String,
    val city: String,
    val country: String,
    val region: String
)

enum class WorkSuitability {
    CONVENIENT,  // 09:00 - 18:00
    BORDERLINE,  // 07:00 - 09:00 or 18:00 - 22:00
    SLEEP        // 22:00 - 07:00
}

data class BusinessOverlapResult(
    val hasOverlap: Boolean,
    val overlapMinutes: Int,
    val description: String,
    val zoneAWindow: String,
    val zoneBWindow: String
)

object WorldClockHelper {

    // Curated catalog of major world business and capital cities
    val CURATED_ZONES: List<WorldZone> = listOf(
        // North America
        WorldZone("America/New_York", "New York", "United States", "Americas"),
        WorldZone("America/Los_Angeles", "Los Angeles", "United States", "Americas"),
        WorldZone("America/Chicago", "Chicago", "United States", "Americas"),
        WorldZone("America/Toronto", "Toronto", "Canada", "Americas"),
        WorldZone("America/Vancouver", "Vancouver", "Canada", "Americas"),
        WorldZone("America/Mexico_City", "Mexico City", "Mexico", "Americas"),
        WorldZone("America/Denver", "Denver", "United States", "Americas"),
        WorldZone("America/Phoenix", "Phoenix", "United States", "Americas"),
        WorldZone("America/Anchorage", "Anchorage", "United States", "Americas"),
        WorldZone("Pacific/Honolulu", "Honolulu", "United States", "Americas"),
        WorldZone("America/Montreal", "Montreal", "Canada", "Americas"),
        WorldZone("America/Miami", "Miami", "United States", "Americas"),
        WorldZone("America/San_Francisco", "San Francisco", "United States", "Americas"),
        WorldZone("America/Seattle", "Seattle", "United States", "Americas"),
        WorldZone("America/Sao_Paulo", "São Paulo", "Brazil", "Americas"),
        WorldZone("America/Argentina/Buenos_Aires", "Buenos Aires", "Argentina", "Americas"),
        WorldZone("America/Santiago", "Santiago", "Chile", "Americas"),
        WorldZone("America/Bogota", "Bogotá", "Colombia", "Americas"),
        WorldZone("America/Lima", "Lima", "Peru", "Americas"),
        WorldZone("America/Caracas", "Caracas", "Venezuela", "Americas"),

        // Europe
        WorldZone("Europe/London", "London", "United Kingdom", "Europe"),
        WorldZone("Europe/Paris", "Paris", "France", "Europe"),
        WorldZone("Europe/Berlin", "Berlin", "Germany", "Europe"),
        WorldZone("Europe/Rome", "Rome", "Italy", "Europe"),
        WorldZone("Europe/Madrid", "Madrid", "Spain", "Europe"),
        WorldZone("Europe/Amsterdam", "Amsterdam", "Netherlands", "Europe"),
        WorldZone("Europe/Brussels", "Brussels", "Belgium", "Europe"),
        WorldZone("Europe/Zurich", "Zurich", "Switzerland", "Europe"),
        WorldZone("Europe/Vienna", "Vienna", "Austria", "Europe"),
        WorldZone("Europe/Stockholm", "Stockholm", "Sweden", "Europe"),
        WorldZone("Europe/Oslo", "Oslo", "Norway", "Europe"),
        WorldZone("Europe/Copenhagen", "Copenhagen", "Denmark", "Europe"),
        WorldZone("Europe/Dublin", "Dublin", "Ireland", "Europe"),
        WorldZone("Europe/Warsaw", "Warsaw", "Poland", "Europe"),
        WorldZone("Europe/Prague", "Prague", "Czech Republic", "Europe"),
        WorldZone("Europe/Budapest", "Budapest", "Hungary", "Europe"),
        WorldZone("Europe/Athens", "Athens", "Greece", "Europe"),
        WorldZone("Europe/Helsinki", "Helsinki", "Finland", "Europe"),
        WorldZone("Europe/Lisbon", "Lisbon", "Portugal", "Europe"),
        WorldZone("Europe/Bucharest", "Bucharest", "Romania", "Europe"),
        WorldZone("Europe/Kyiv", "Kyiv", "Ukraine", "Europe"),
        WorldZone("Europe/Moscow", "Moscow", "Russia", "Europe"),
        WorldZone("Europe/Istanbul", "Istanbul", "Turkey", "Europe"),

        // Asia
        WorldZone("Asia/Tokyo", "Tokyo", "Japan", "Asia"),
        WorldZone("Asia/Kolkata", "New Delhi / Mumbai", "India", "Asia"),
        WorldZone("Asia/Singapore", "Singapore", "Singapore", "Asia"),
        WorldZone("Asia/Hong_Kong", "Hong Kong", "Hong Kong", "Asia"),
        WorldZone("Asia/Shanghai", "Beijing / Shanghai", "China", "Asia"),
        WorldZone("Asia/Seoul", "Seoul", "South Korea", "Asia"),
        WorldZone("Asia/Taipei", "Taipei", "Taiwan", "Asia"),
        WorldZone("Asia/Bangkok", "Bangkok", "Thailand", "Asia"),
        WorldZone("Asia/Jakarta", "Jakarta", "Indonesia", "Asia"),
        WorldZone("Asia/Manila", "Manila", "Philippines", "Asia"),
        WorldZone("Asia/Kuala_Lumpur", "Kuala Lumpur", "Malaysia", "Asia"),
        WorldZone("Asia/Ho_Chi_Minh", "Ho Chi Minh City", "Vietnam", "Asia"),
        WorldZone("Asia/Dhaka", "Dhaka", "Bangladesh", "Asia"),
        WorldZone("Asia/Karachi", "Karachi", "Pakistan", "Asia"),
        WorldZone("Asia/Colombo", "Colombo", "Sri Lanka", "Asia"),
        WorldZone("Asia/Kathmandu", "Kathmandu", "Nepal", "Asia"),
        WorldZone("Asia/Yangon", "Yangon", "Myanmar", "Asia"),
        WorldZone("Asia/Tashkent", "Tashkent", "Uzbekistan", "Asia"),
        WorldZone("Asia/Almaty", "Almaty", "Kazakhstan", "Asia"),

        // Middle East
        WorldZone("Asia/Dubai", "Dubai", "United Arab Emirates", "Middle East"),
        WorldZone("Asia/Riyadh", "Riyadh", "Saudi Arabia", "Middle East"),
        WorldZone("Asia/Doha", "Doha", "Qatar", "Middle East"),
        WorldZone("Asia/Kuwait", "Kuwait City", "Kuwait", "Middle East"),
        WorldZone("Asia/Muscat", "Muscat", "Oman", "Middle East"),
        WorldZone("Asia/Bahrain", "Manama", "Bahrain", "Middle East"),
        WorldZone("Asia/Jerusalem", "Tel Aviv / Jerusalem", "Israel", "Middle East"),
        WorldZone("Asia/Amman", "Amman", "Jordan", "Middle East"),
        WorldZone("Asia/Beirut", "Beirut", "Lebanon", "Middle East"),
        WorldZone("Asia/Baghdad", "Baghdad", "Iraq", "Middle East"),
        WorldZone("Asia/Tehran", "Tehran", "Iran", "Middle East"),

        // Oceania / Pacific
        WorldZone("Australia/Sydney", "Sydney", "Australia", "Oceania"),
        WorldZone("Australia/Melbourne", "Melbourne", "Australia", "Oceania"),
        WorldZone("Australia/Brisbane", "Brisbane", "Australia", "Oceania"),
        WorldZone("Australia/Perth", "Perth", "Australia", "Oceania"),
        WorldZone("Australia/Adelaide", "Adelaide", "Australia", "Oceania"),
        WorldZone("Pacific/Auckland", "Auckland", "New Zealand", "Oceania"),
        WorldZone("Pacific/Wellington", "Wellington", "New Zealand", "Oceania"),
        WorldZone("Pacific/Fiji", "Suva", "Fiji", "Oceania"),
        WorldZone("Pacific/Guam", "Guam", "Guam", "Oceania"),

        // Africa
        WorldZone("Africa/Cairo", "Cairo", "Egypt", "Africa"),
        WorldZone("Africa/Johannesburg", "Johannesburg", "South Africa", "Africa"),
        WorldZone("Africa/Nairobi", "Nairobi", "Kenya", "Africa"),
        WorldZone("Africa/Lagos", "Lagos", "Nigeria", "Africa"),
        WorldZone("Africa/Casablanca", "Casablanca", "Morocco", "Africa"),
        WorldZone("Africa/Accra", "Accra", "Ghana", "Africa"),
        WorldZone("Africa/Addis_Ababa", "Addis Ababa", "Ethiopia", "Africa"),
        WorldZone("Africa/Dar_es_Salaam", "Dar es Salaam", "Tanzania", "Africa"),
        WorldZone("Africa/Tunis", "Tunis", "Tunisia", "Africa"),
        WorldZone("Africa/Algiers", "Algiers", "Algeria", "Africa")
    )

    fun getAllZones(): List<WorldZone> {
        val curatedMap = CURATED_ZONES.associateBy { it.id }
        val allSystemIds = TimeZone.getAvailableIDs()
        val additional = allSystemIds.mapNotNull { id ->
            if (curatedMap.containsKey(id)) null
            else {
                val parts = id.split("/")
                if (parts.size >= 2) {
                    val rawCity = parts.last().replace('_', ' ')
                    val region = parts.first().replace('_', ' ')
                    WorldZone(id, rawCity, region, region)
                } else null
            }
        }.distinctBy { it.id }

        return CURATED_ZONES + additional
    }

    fun findZone(id: String): WorldZone {
        return CURATED_ZONES.firstOrNull { it.id == id }
            ?: run {
                val parts = id.split("/")
                val city = if (parts.size >= 2) parts.last().replace('_', ' ') else id
                val region = if (parts.size >= 2) parts.first().replace('_', ' ') else "World"
                WorldZone(id, city, region, region)
            }
    }

    fun getLocalZone(): WorldZone {
        val localTz = TimeZone.getDefault()
        return findZone(localTz.id)
    }

    fun formatUtcOffset(timeZone: TimeZone, date: Date = Date()): String {
        val offsetMillis = timeZone.getOffset(date.time)
        val isNegative = offsetMillis < 0
        val totalMinutes = abs(offsetMillis) / (1000 * 60)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        val sign = if (isNegative) "-" else "+"
        return if (minutes == 0) {
            "GMT$sign$hours"
        } else {
            String.format(Locale.US, "GMT%s%d:%02d", sign, hours, minutes)
        }
    }

    fun getDifferenceMinutes(targetZone: TimeZone, baseZone: TimeZone, date: Date = Date()): Long {
        val targetOffset = targetZone.getOffset(date.time)
        val baseOffset = baseZone.getOffset(date.time)
        return (targetOffset - baseOffset).toLong() / (1000L * 60L)
    }

    fun formatDifferenceText(targetZone: TimeZone, baseZone: TimeZone, date: Date = Date()): String {
        val diffMinutes = getDifferenceMinutes(targetZone, baseZone, date)
        if (diffMinutes == 0L) return "Same time"
        val isAhead = diffMinutes > 0
        val absMinutes = abs(diffMinutes)
        val hours = absMinutes / 60
        val mins = absMinutes % 60
        val durationStr = when {
            mins == 0L -> "${hours}h"
            hours == 0L -> "${mins}m"
            else -> "${hours}h ${mins}m"
        }
        return if (isAhead) "$durationStr ahead" else "$durationStr behind"
    }

    fun getDayShift(targetZone: TimeZone, baseZone: TimeZone, date: Date = Date()): Int {
        val baseCal = Calendar.getInstance(baseZone).apply { time = date }
        val targetCal = Calendar.getInstance(targetZone).apply { time = date }
        val baseDayOfYear = baseCal.get(Calendar.DAY_OF_YEAR)
        val baseYear = baseCal.get(Calendar.YEAR)
        val targetDayOfYear = targetCal.get(Calendar.DAY_OF_YEAR)
        val targetYear = targetCal.get(Calendar.YEAR)

        return when {
            targetYear > baseYear -> 1
            targetYear < baseYear -> -1
            targetDayOfYear > baseDayOfYear -> 1
            targetDayOfYear < baseDayOfYear -> -1
            else -> 0
        }
    }

    fun isDaytime(timeZone: TimeZone, date: Date = Date()): Boolean {
        val cal = Calendar.getInstance(timeZone).apply { time = date }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        return hour in 6..17 // 6 AM to 5:59 PM
    }

    fun isWorkingHours(timeZone: TimeZone, date: Date = Date()): Boolean {
        val cal = Calendar.getInstance(timeZone).apply { time = date }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val isWeekday = dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY
        return isWeekday && (hour in 9..17)
    }

    fun getWorkSuitability(timeZone: TimeZone, date: Date): WorkSuitability {
        val cal = Calendar.getInstance(timeZone).apply { time = date }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 9..17 -> WorkSuitability.CONVENIENT // 9:00 AM - 5:59 PM
            in 7..8, in 18..21 -> WorkSuitability.BORDERLINE // 7-9 AM or 6-10 PM
            else -> WorkSuitability.SLEEP // 10 PM - 7 AM
        }
    }

    fun formatTime(date: Date, timeZone: TimeZone, use24Hour: Boolean = false, includeSeconds: Boolean = false): String {
        val pattern = when {
            use24Hour && includeSeconds -> "HH:mm:ss"
            use24Hour -> "HH:mm"
            includeSeconds -> "hh:mm:ss a"
            else -> "hh:mm a"
        }
        val sdf = SimpleDateFormat(pattern, Locale.getDefault()).apply {
            this.timeZone = timeZone
        }
        return sdf.format(date)
    }

    fun formatDate(date: Date, timeZone: TimeZone, pattern: String = "EEE, dd MMM yyyy"): String {
        val sdf = SimpleDateFormat(pattern, Locale.getDefault()).apply {
            this.timeZone = timeZone
        }
        return sdf.format(date)
    }

    fun calculateBusinessHoursOverlap(zoneA: TimeZone, zoneB: TimeZone, baseDate: Date = Date()): BusinessOverlapResult {
        // Business hours are defined as 9:00 AM (540m) to 6:00 PM (1080m) in local clock time
        val diffMinutes = getDifferenceMinutes(zoneB, zoneA, baseDate)

        // Express zone B's working hours (9:00 to 18:00) in zone A's time frame:
        // Zone B working window in Zone A minutes of day:
        val zoneBStartInA = 540 - diffMinutes
        val zoneBEndInA = 1080 - diffMinutes

        // Zone A working hours in Zone A:
        val zoneAStart = 540L
        val zoneAEnd = 1080L

        // Find intersection within a 24-hour cycle (-1440, 0, 1440 normalization)
        var maxOverlap = 0L
        var bestAStart = 0L
        var bestAEnd = 0L

        for (cycle in listOf(-1440L, 0L, 1440L)) {
            val shiftedBStart = zoneBStartInA + cycle
            val shiftedBEnd = zoneBEndInA + cycle
            val overlapStart = maxOf(zoneAStart, shiftedBStart)
            val overlapEnd = minOf(zoneAEnd, shiftedBEnd)
            val overlap = overlapEnd - overlapStart
            if (overlap > maxOverlap) {
                maxOverlap = overlap
                bestAStart = overlapStart
                bestAEnd = overlapEnd
            }
        }

        fun minutesToTimeString(m: Long): String {
            val normalizedM = ((m % 1440) + 1440) % 1440
            val h = normalizedM / 60
            val min = normalizedM % 60
            val period = if (h >= 12) "PM" else "AM"
            val displayH = when {
                h == 0L -> 12L
                h > 12L -> h - 12L
                else -> h
            }
            return String.format(Locale.US, "%d:%02d %s", displayH, min, period)
        }

        return if (maxOverlap > 0) {
            val bestBStart = bestAStart + diffMinutes
            val bestBEnd = bestAEnd + diffMinutes
            val hours = maxOverlap / 60
            val mins = maxOverlap % 60
            val durationStr = if (mins == 0L) "${hours} hours" else "${hours}h ${mins}m"

            BusinessOverlapResult(
                hasOverlap = true,
                overlapMinutes = maxOverlap.toInt(),
                description = "$durationStr of overlapping business hours (9 AM - 6 PM)",
                zoneAWindow = "${minutesToTimeString(bestAStart)} – ${minutesToTimeString(bestAEnd)}",
                zoneBWindow = "${minutesToTimeString(bestBStart)} – ${minutesToTimeString(bestBEnd)}"
            )
        } else {
            // No direct overlap within 9 AM - 6 PM
            val compromiseA = "07:30 PM"
            val compromiseB = minutesToTimeString(1170 + diffMinutes)
            BusinessOverlapResult(
                hasOverlap = false,
                overlapMinutes = 0,
                description = "No direct business hours overlap (Best compromise: Evening in one city / Morning in the other)",
                zoneAWindow = "No common 9-6 window",
                zoneBWindow = "Try scheduling around $compromiseA / $compromiseB"
            )
        }
    }
}
