package `in`.mahato.bytetools.ui.tools.worldclock

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class WorldClockViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val prefs = context.getSharedPreferences("world_clock_prefs", Context.MODE_PRIVATE)

    // Current live timestamp, ticking every second
    private val _currentMillis = MutableStateFlow(System.currentTimeMillis())
    val currentMillis: StateFlow<Long> = _currentMillis.asStateFlow()

    // 12-hour vs 24-hour display format preference
    private val _use24HourFormat = MutableStateFlow(prefs.getBoolean("use_24h", false))
    val use24HourFormat: StateFlow<Boolean> = _use24HourFormat.asStateFlow()

    // List of tracked / saved world clock zones
    private val _trackedZones = MutableStateFlow<List<WorldZone>>(loadTrackedZones())
    val trackedZones: StateFlow<List<WorldZone>> = _trackedZones.asStateFlow()

    // --- Meeting Planner State ---
    private val _meetingBaseZone = MutableStateFlow(WorldClockHelper.getLocalZone())
    val meetingBaseZone: StateFlow<WorldZone> = _meetingBaseZone.asStateFlow()

    // Calendar anchored to the meeting base zone
    private val _meetingTime = MutableStateFlow(Calendar.getInstance(_meetingBaseZone.value.let { TimeZone.getTimeZone(it.id) }))
    val meetingTime: StateFlow<Calendar> = _meetingTime.asStateFlow()

    private val _meetingDurationMinutes = MutableStateFlow(60) // 15, 30, 45, 60, 90, 120
    val meetingDurationMinutes: StateFlow<Int> = _meetingDurationMinutes.asStateFlow()

    private val _meetingTitle = MutableStateFlow("Global Team Sync")
    val meetingTitle: StateFlow<String> = _meetingTitle.asStateFlow()

    // --- Time Difference Calculator State ---
    private val defaultCityA = WorldClockHelper.getLocalZone()
    private val defaultCityB = if (defaultCityA.id == "America/New_York") {
        WorldClockHelper.findZone("Europe/London")
    } else {
        WorldClockHelper.findZone("America/New_York")
    }

    private val _diffCityA = MutableStateFlow(defaultCityA)
    val diffCityA: StateFlow<WorldZone> = _diffCityA.asStateFlow()

    private val _diffCityB = MutableStateFlow(defaultCityB)
    val diffCityB: StateFlow<WorldZone> = _diffCityB.asStateFlow()

    // Interactive minutes of day slider for Diff tab (0..1439), defaults to current local time
    private val initialMinuteOfDay = Calendar.getInstance().let {
        it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE)
    }
    private val _diffMinuteOfDay = MutableStateFlow(initialMinuteOfDay)
    val diffMinuteOfDay: StateFlow<Int> = _diffMinuteOfDay.asStateFlow()

    init {
        // Ticker loop
        viewModelScope.launch {
            while (true) {
                _currentMillis.value = System.currentTimeMillis()
                delay(1000L)
            }
        }
    }

    private fun loadTrackedZones(): List<WorldZone> {
        val savedRaw = prefs.getString("tracked_zone_ids", null)
        return if (!savedRaw.isNullOrBlank()) {
            val ids = savedRaw.split(",")
            ids.map { WorldClockHelper.findZone(it) }
        } else {
            // Default initial world cities
            val localId = TimeZone.getDefault().id
            val defaultIds = listOf(
                "Europe/London",
                "America/New_York",
                "Asia/Tokyo",
                "Asia/Dubai",
                "Asia/Singapore",
                "Australia/Sydney"
            ).filter { it != localId }
            defaultIds.map { WorldClockHelper.findZone(it) }
        }
    }

    private fun saveTrackedZones(zones: List<WorldZone>) {
        val raw = zones.joinToString(",") { it.id }
        prefs.edit().putString("tracked_zone_ids", raw).apply()
        _trackedZones.value = zones
    }

    fun toggleTimeFormat() {
        val newValue = !_use24HourFormat.value
        _use24HourFormat.value = newValue
        prefs.edit().putBoolean("use_24h", newValue).apply()
    }

    fun addTrackedZone(zone: WorldZone): Boolean {
        if (_trackedZones.value.any { it.id == zone.id }) {
            return false // Already added
        }
        val updated = _trackedZones.value + zone
        saveTrackedZones(updated)
        return true
    }

    fun removeTrackedZone(zoneId: String) {
        val updated = _trackedZones.value.filter { it.id != zoneId }
        saveTrackedZones(updated)
    }

    fun resetToDefaults() {
        prefs.edit().remove("tracked_zone_ids").apply()
        _trackedZones.value = loadTrackedZones()
    }

    // --- Meeting Planner Actions ---
    fun setMeetingBaseZone(zone: WorldZone) {
        _meetingBaseZone.value = zone
        val current = _meetingTime.value
        val newCal = Calendar.getInstance(TimeZone.getTimeZone(zone.id)).apply {
            timeInMillis = current.timeInMillis
        }
        _meetingTime.value = newCal
    }

    fun setMeetingTitle(title: String) {
        _meetingTitle.value = title
    }

    fun setMeetingDate(year: Int, month: Int, dayOfMonth: Int) {
        val cal = (_meetingTime.value.clone() as Calendar).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, dayOfMonth)
        }
        _meetingTime.value = cal
    }

    fun setMeetingTimeMinutes(totalMinutesOfDay: Int) {
        val safeMinutes = totalMinutesOfDay.coerceIn(0, 1439)
        val h = safeMinutes / 60
        val m = safeMinutes % 60
        val cal = (_meetingTime.value.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0)
        }
        _meetingTime.value = cal
    }

    fun adjustMeetingTime(minutesDelta: Int) {
        val cal = (_meetingTime.value.clone() as Calendar).apply {
            add(Calendar.MINUTE, minutesDelta)
        }
        _meetingTime.value = cal
    }

    fun setMeetingDuration(minutes: Int) {
        _meetingDurationMinutes.value = minutes
    }

    fun generateMeetingShareText(zonesToInclude: List<WorldZone>): String {
        val baseDate = _meetingTime.value.time
        val durationMin = _meetingDurationMinutes.value
        val endDate = Date(baseDate.time + durationMin * 60 * 1000L)
        val title = _meetingTitle.value.ifBlank { "Meeting" }

        val baseZone = _meetingBaseZone.value
        val baseTz = TimeZone.getTimeZone(baseZone.id)
        val dateHeaderFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US).apply {
            timeZone = baseTz
        }

        val sb = StringBuilder()
        sb.append("🗓️ *$title*\n")
        sb.append("📅 ${dateHeaderFormat.format(baseDate)}\n")
        sb.append("⏱️ Duration: $durationMin minutes\n\n")
        sb.append("🌍 *Time in each location:*\n")

        // Include Base Zone first
        val allZones = (listOf(baseZone) + zonesToInclude.filter { it.id != baseZone.id }).distinctBy { it.id }

        for (z in allZones) {
            val tz = TimeZone.getTimeZone(z.id)
            val startTimeStr = WorldClockHelper.formatTime(baseDate, tz, _use24HourFormat.value)
            val endTimeStr = WorldClockHelper.formatTime(endDate, tz, _use24HourFormat.value)
            val offset = WorldClockHelper.formatUtcOffset(tz, baseDate)
            val dayShift = WorldClockHelper.getDayShift(tz, baseTz, baseDate)
            val dayShiftText = when (dayShift) {
                1 -> " (+1 day)"
                -1 -> " (-1 day)"
                else -> ""
            }

            sb.append("• *${z.city}* ($offset): $startTimeStr – $endTimeStr$dayShiftText\n")
        }

        sb.append("\n_Created with ByteTools World Clock_")
        return sb.toString()
    }

    // --- Time Difference Actions ---
    fun setDiffCityA(zone: WorldZone) {
        _diffCityA.value = zone
    }

    fun setDiffCityB(zone: WorldZone) {
        _diffCityB.value = zone
    }

    fun swapDiffCities() {
        val a = _diffCityA.value
        val b = _diffCityB.value
        _diffCityA.value = b
        _diffCityB.value = a
    }

    fun setDiffMinuteOfDay(minutesOfDay: Int) {
        _diffMinuteOfDay.value = minutesOfDay.coerceIn(0, 1439)
    }

    fun resetDiffToCurrentTime() {
        val cal = Calendar.getInstance(TimeZone.getTimeZone(_diffCityA.value.id))
        _diffMinuteOfDay.value = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }
}
