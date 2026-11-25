package `in`.mahato.bytetools.ui.tools.worldclock

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import `in`.mahato.bytetools.ui.navigation.Screen
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldClockScreen(
    navController: NavController,
    viewModel: WorldClockViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val use24Hour by viewModel.use24HourFormat.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "World Clock",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Time zones, meeting planner & diff",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // 12H / 24H Toggle
                    FilterChip(
                        selected = use24Hour,
                        onClick = { viewModel.toggleTimeFormat() },
                        label = {
                            Text(
                                if (use24Hour) "24H" else "12H",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(onClick = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    }) {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = "Home",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Navigation Tabs
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("World Clocks", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Public, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Meeting Planner", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Groups, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Time Difference", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = null) }
                )
            }

            // Tab Content
            when (selectedTab) {
                0 -> WorldClocksTab(viewModel)
                1 -> MeetingPlannerTab(viewModel)
                2 -> TimeDifferenceTab(viewModel)
            }
        }
    }
}

// ==========================================
// TAB 1: WORLD CLOCKS (MULTIPLE TIME ZONES)
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldClocksTab(viewModel: WorldClockViewModel) {
    val context = LocalContext.current
    val currentMillis by viewModel.currentMillis.collectAsState()
    val use24Hour by viewModel.use24HourFormat.collectAsState()
    val trackedZones by viewModel.trackedZones.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    val currentDate = remember(currentMillis) { Date(currentMillis) }
    val localZone = remember { WorldClockHelper.getLocalZone() }
    val localTz = remember { TimeZone.getDefault() }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Hero Banner: Local Device Time
            item {
                LocalTimeHeroCard(
                    localZone = localZone,
                    localTz = localTz,
                    currentDate = currentDate,
                    use24Hour = use24Hour
                )
            }

            // Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Tracked Cities (${trackedZones.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    TextButton(onClick = { viewModel.resetToDefaults() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset Defaults", fontSize = 12.sp)
                    }
                }
            }

            // Tracked Cities List
            if (trackedZones.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Public,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No Cities Added",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Add cities around the world to track their current time and offset.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { showAddDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add First City")
                            }
                        }
                    }
                }
            } else {
                items(trackedZones, key = { it.id }) { zone ->
                    CityClockCard(
                        zone = zone,
                        localTz = localTz,
                        currentDate = currentDate,
                        use24Hour = use24Hour,
                        onDelete = {
                            viewModel.removeTrackedZone(zone.id)
                            Toast.makeText(context, "Removed ${zone.city}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Floating Action Button
        ExtendedFloatingActionButton(
            onClick = { showAddDialog = true },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Add City", fontWeight = FontWeight.Bold) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        )
    }

    if (showAddDialog) {
        CityPickerBottomSheet(
            title = "Add World City",
            alreadySelectedIds = trackedZones.map { it.id }.toSet(),
            onSelectZone = { selected ->
                val added = viewModel.addTrackedZone(selected)
                if (added) {
                    Toast.makeText(context, "Added ${selected.city}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "${selected.city} already added", Toast.LENGTH_SHORT).show()
                }
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
fun LocalTimeHeroCard(
    localZone: WorldZone,
    localTz: TimeZone,
    currentDate: Date,
    use24Hour: Boolean
) {
    val isDay = WorldClockHelper.isDaytime(localTz, currentDate)
    val offset = WorldClockHelper.formatUtcOffset(localTz, currentDate)
    val timeStr = WorldClockHelper.formatTime(currentDate, localTz, use24Hour, includeSeconds = true)
    val dateStr = WorldClockHelper.formatDate(currentDate, localTz, "EEEE, MMMM d, yyyy")

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF3730A3), // Deep Indigo
                            Color(0xFF4F46E5), // Indigo 600
                            Color(0xFF6366F1)  // Indigo 500
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "YOUR LOCAL TIME",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            localZone.city,
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (isDay) Icons.Default.WbSunny else Icons.Default.NightsStay,
                                contentDescription = null,
                                tint = if (isDay) Color(0xFFFDE047) else Color(0xFFE2E8F0),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = timeStr,
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = dateStr,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = offset,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = localTz.id,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CityClockCard(
    zone: WorldZone,
    localTz: TimeZone,
    currentDate: Date,
    use24Hour: Boolean,
    onDelete: () -> Unit
) {
    val targetTz = remember(zone.id) { TimeZone.getTimeZone(zone.id) }
    val isDay = WorldClockHelper.isDaytime(targetTz, currentDate)
    val timeStr = WorldClockHelper.formatTime(currentDate, targetTz, use24Hour)
    val dateStr = WorldClockHelper.formatDate(currentDate, targetTz, "EEE, dd MMM")
    val offset = WorldClockHelper.formatUtcOffset(targetTz, currentDate)
    val diffText = WorldClockHelper.formatDifferenceText(targetTz, localTz, currentDate)
    val dayShift = WorldClockHelper.getDayShift(targetTz, localTz, currentDate)
    val isWorking = WorldClockHelper.isWorkingHours(targetTz, currentDate)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Left Column: City, Country, Relative Offset
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = zone.city,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = "${zone.country} • $offset",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Relative Offset Pill
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = diffText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        // Day shift Pill if not same day
                        if (dayShift != 0) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (dayShift > 0) Color(0xFFFEF3C7) else Color(0xFFF3E8FF)
                            ) {
                                Text(
                                    text = if (dayShift > 0) "Tomorrow" else "Yesterday",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (dayShift > 0) Color(0xFFB45309) else Color(0xFF6B21A8),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Right Column: Digital Time, Day/Night icon, and Delete
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isDay) Icons.Default.WbSunny else Icons.Default.NightsStay,
                            contentDescription = null,
                            tint = if (isDay) Color(0xFFEAB308) else Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = timeStr,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = dateStr,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isWorking) Color(0xFFDCFCE7) else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = if (isWorking) "● In Office" else "○ Off Hours",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isWorking) Color(0xFF15803D) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 2: MEETING TIME CONVERTER
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingPlannerTab(viewModel: WorldClockViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val baseZone by viewModel.meetingBaseZone.collectAsState()
    val meetingTimeCal by viewModel.meetingTime.collectAsState()
    val durationMinutes by viewModel.meetingDurationMinutes.collectAsState()
    val meetingTitle by viewModel.meetingTitle.collectAsState()
    val trackedZones by viewModel.trackedZones.collectAsState()
    val use24Hour by viewModel.use24HourFormat.collectAsState()

    var showBaseZonePicker by remember { mutableStateOf(false) }

    val baseDate = meetingTimeCal.time
    val baseTz = remember(baseZone.id) { TimeZone.getTimeZone(baseZone.id) }
    val baseMinutesOfDay = meetingTimeCal.get(Calendar.HOUR_OF_DAY) * 60 + meetingTimeCal.get(Calendar.MINUTE)

    val allMeetingZones = remember(baseZone, trackedZones) {
        (listOf(baseZone) + trackedZones.filter { it.id != baseZone.id }).distinctBy { it.id }
    }

    // Suitability breakdown
    val suitabilities = allMeetingZones.map { z ->
        val tz = TimeZone.getTimeZone(z.id)
        WorldClockHelper.getWorkSuitability(tz, baseDate)
    }
    val convenientCount = suitabilities.count { it == WorkSuitability.CONVENIENT }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Meeting Config Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Meeting Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = meetingTitle,
                        onValueChange = { viewModel.setMeetingTitle(it) },
                        label = { Text("Meeting Title") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Event, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Date & Base Timezone Pickers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Date Picker Button
                        OutlinedButton(
                            onClick = {
                                val cal = meetingTimeCal
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        viewModel.setMeetingDate(year, month, dayOfMonth)
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            val dateLabel = SimpleDateFormat("MMM d, yyyy", Locale.US).apply { timeZone = baseTz }.format(baseDate)
                            Text(dateLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }

                        // Base Timezone Button
                        OutlinedButton(
                            onClick = { showBaseZonePicker = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(baseZone.city, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Duration Chips
                    Text("Duration:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(15, 30, 45, 60, 90, 120).forEach { mins ->
                            val label = if (mins >= 60) "${mins / 60}h${if (mins % 60 != 0) " ${mins % 60}m" else ""}" else "${mins}m"
                            FilterChip(
                                selected = durationMinutes == mins,
                                onClick = { viewModel.setMeetingDuration(mins) },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }
        }

        // Interactive Meeting Time Slider Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val endDate = Date(baseDate.time + durationMinutes * 60 * 1000L)
                    val startTimeStr = WorldClockHelper.formatTime(baseDate, baseTz, use24Hour)
                    val endTimeStr = WorldClockHelper.formatTime(endDate, baseTz, use24Hour)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Base Time (${baseZone.city})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "$startTimeStr – $endTimeStr",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilledTonalIconButton(
                                onClick = { viewModel.adjustMeetingTime(-30) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text("-30", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            FilledTonalIconButton(
                                onClick = { viewModel.adjustMeetingTime(30) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text("+30", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Slider(
                        value = baseMinutesOfDay.toFloat(),
                        onValueChange = { newVal ->
                            // Snap to 15-minute increments
                            val rounded = (newVal / 15f).roundToInt() * 15
                            viewModel.setMeetingTimeMinutes(rounded)
                        },
                        valueRange = 0f..1425f,
                        steps = 94,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("12 AM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("6 AM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("12 PM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("6 PM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("11:45 PM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Suitability Indicator Banner
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (convenientCount == allMeetingZones.size) Color(0xFFDCFCE7)
                else if (convenientCount >= (allMeetingZones.size / 2)) Color(0xFFFEF3C7)
                else Color(0xFFFFE4E6),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (convenientCount == allMeetingZones.size) Icons.Default.CheckCircle
                        else if (convenientCount >= (allMeetingZones.size / 2)) Icons.Default.Warning
                        else Icons.Default.Bedtime,
                        contentDescription = null,
                        tint = if (convenientCount == allMeetingZones.size) Color(0xFF15803D)
                        else if (convenientCount >= (allMeetingZones.size / 2)) Color(0xFFB45309)
                        else Color(0xFFBE123C),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$convenientCount of ${allMeetingZones.size} locations during standard working hours (9 AM - 6 PM)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (convenientCount == allMeetingZones.size) Color(0xFF15803D)
                        else if (convenientCount >= (allMeetingZones.size / 2)) Color(0xFFB45309)
                        else Color(0xFFBE123C)
                    )
                }
            }
        }

        // Cross-timezone converted times header
        item {
            Text(
                "Converted Times for All Participants",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Multi-zone Time Cards
        items(allMeetingZones, key = { it.id }) { zone ->
            MeetingZoneResultCard(
                zone = zone,
                baseDate = baseDate,
                durationMinutes = durationMinutes,
                baseTz = baseTz,
                use24Hour = use24Hour
            )
        }

        // Action Buttons: Copy Invite & Share
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val text = viewModel.generateMeetingShareText(allMeetingZones)
                        clipboardManager.setText(AnnotatedString(text))
                        Toast.makeText(context, "Meeting schedule copied!", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Schedule")
                }

                Button(
                    onClick = {
                        val text = viewModel.generateMeetingShareText(allMeetingZones)
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, text)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Meeting Times"))
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share Invite")
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showBaseZonePicker) {
        CityPickerBottomSheet(
            title = "Select Base Timezone",
            alreadySelectedIds = emptySet(),
            onSelectZone = { selected ->
                viewModel.setMeetingBaseZone(selected)
                showBaseZonePicker = false
            },
            onDismiss = { showBaseZonePicker = false }
        )
    }
}

@Composable
fun MeetingZoneResultCard(
    zone: WorldZone,
    baseDate: Date,
    durationMinutes: Int,
    baseTz: TimeZone,
    use24Hour: Boolean
) {
    val targetTz = remember(zone.id) { TimeZone.getTimeZone(zone.id) }
    val endDate = remember(baseDate, durationMinutes) { Date(baseDate.time + durationMinutes * 60 * 1000L) }
    val startTimeStr = WorldClockHelper.formatTime(baseDate, targetTz, use24Hour)
    val endTimeStr = WorldClockHelper.formatTime(endDate, targetTz, use24Hour)
    val dateStr = WorldClockHelper.formatDate(baseDate, targetTz, "EEE, MMM d")
    val dayShift = WorldClockHelper.getDayShift(targetTz, baseTz, baseDate)
    val suitability = WorldClockHelper.getWorkSuitability(targetTz, baseDate)

    val (badgeBg, badgeText, badgeLabel) = when (suitability) {
        WorkSuitability.CONVENIENT -> Triple(Color(0xFFDCFCE7), Color(0xFF15803D), "Office Hours (9am-6pm)")
        WorkSuitability.BORDERLINE -> Triple(Color(0xFFFEF3C7), Color(0xFFB45309), "Early / Late Hours")
        WorkSuitability.SLEEP -> Triple(Color(0xFFFFE4E6), Color(0xFFBE123C), "Sleep / Night Hours")
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = zone.city,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${zone.country} (${WorldClockHelper.formatUtcOffset(targetTz, baseDate)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = badgeLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeText,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$startTimeStr – $endTimeStr",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dateStr,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (dayShift != 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (dayShift > 0) "(+1d)" else "(-1d)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (dayShift > 0) Color(0xFFB45309) else Color(0xFF7C3AED)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 3: TIME DIFFERENCE CALCULATOR
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeDifferenceTab(viewModel: WorldClockViewModel) {
    val cityA by viewModel.diffCityA.collectAsState()
    val cityB by viewModel.diffCityB.collectAsState()
    val diffMinutesOfDay by viewModel.diffMinuteOfDay.collectAsState()
    val use24Hour by viewModel.use24HourFormat.collectAsState()

    var showPickerCityA by remember { mutableStateOf(false) }
    var showPickerCityB by remember { mutableStateOf(false) }

    val zoneATz = remember(cityA.id) { TimeZone.getTimeZone(cityA.id) }
    val zoneBTz = remember(cityB.id) { TimeZone.getTimeZone(cityB.id) }

    val now = Date()
    val diffMinutes = remember(cityA.id, cityB.id) {
        WorldClockHelper.getDifferenceMinutes(zoneBTz, zoneATz, now)
    }
    val absDiff = abs(diffMinutes)
    val diffHours = absDiff / 60
    val diffMins = absDiff % 60
    val isBAhead = diffMinutes > 0
    val isSame = diffMinutes == 0L

    // Simulated Time calculation based on interactive slider
    val simulatedCalA = remember(cityA.id, diffMinutesOfDay) {
        Calendar.getInstance(zoneATz).apply {
            time = now
            set(Calendar.HOUR_OF_DAY, diffMinutesOfDay / 60)
            set(Calendar.MINUTE, diffMinutesOfDay % 60)
            set(Calendar.SECOND, 0)
        }
    }
    val simulatedDateA = simulatedCalA.time

    // Corresponding time in City B
    val simulatedCalB = remember(simulatedDateA, cityB.id) {
        Calendar.getInstance(zoneBTz).apply {
            timeInMillis = simulatedDateA.time
        }
    }
    val simulatedDateB = simulatedCalB.time
    val dayShift = WorldClockHelper.getDayShift(zoneBTz, zoneATz, simulatedDateA)

    // Business overlap result
    val overlap = remember(cityA.id, cityB.id) {
        WorldClockHelper.calculateBusinessHoursOverlap(zoneATz, zoneBTz, now)
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // City Selectors & Swap Button
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Compare Time Zones",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // City A Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { showPickerCityA = true }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("FROM (CITY A)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(cityA.city, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${cityA.country} • ${WorldClockHelper.formatUtcOffset(zoneATz, now)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }

                    // Swap Button Row
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        FilledTonalIconButton(
                            onClick = { viewModel.swapDiffCities() },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.SwapVert, contentDescription = "Swap Cities", modifier = Modifier.size(20.dp))
                        }
                    }

                    // City B Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { showPickerCityB = true }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("TO (CITY B)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            Text(cityB.city, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${cityB.country} • ${WorldClockHelper.formatUtcOffset(zoneBTz, now)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // Difference Summary Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        "TIME DIFFERENCE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val differenceSentence = when {
                        isSame -> "${cityB.city} is in the same time zone as ${cityA.city}"
                        isBAhead -> "${cityB.city} is ${if (diffMins == 0L) "$diffHours hours" else "${diffHours}h ${diffMins}m"} ahead of ${cityA.city}"
                        else -> "${cityB.city} is ${if (diffMins == 0L) "$diffHours hours" else "${diffHours}h ${diffMins}m"} behind ${cityA.city}"
                    }

                    Text(
                        text = differenceSentence,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                text = if (isSame) "Same Time" else if (isBAhead) "Ahead (+)" else "Behind (-)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                text = "Δ ${diffHours}h ${diffMins}m",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Interactive 24-Hour Comparison Slider
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Interactive Time Synchronizer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { viewModel.resetDiffToCurrentTime() }) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Now", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Side-by-Side Display
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // City A Simulated Box
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(cityA.city, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(
                                    WorldClockHelper.formatTime(simulatedDateA, zoneATz, use24Hour),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    WorldClockHelper.formatDate(simulatedDateA, zoneATz, "EEE, MMM d"),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // City B Simulated Box
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(cityB.city, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                Text(
                                    WorldClockHelper.formatTime(simulatedDateB, zoneBTz, use24Hour),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        WorldClockHelper.formatDate(simulatedDateB, zoneBTz, "EEE, MMM d"),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (dayShift != 0) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            if (dayShift > 0) "(+1d)" else "(-1d)",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (dayShift > 0) Color(0xFFB45309) else Color(0xFF7C3AED)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Slider(
                        value = diffMinutesOfDay.toFloat(),
                        onValueChange = { viewModel.setDiffMinuteOfDay(it.toInt()) },
                        valueRange = 0f..1439f,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("12 AM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("6 AM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("12 PM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("6 PM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("11:59 PM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Business Working Hours Overlap Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (overlap.hasOverlap) Icons.Default.Work else Icons.Default.WorkOff,
                            contentDescription = null,
                            tint = if (overlap.hasOverlap) Color(0xFF15803D) else Color(0xFFB45309),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Working Hours Overlap (9 AM – 6 PM)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = overlap.description,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (overlap.hasOverlap) Color(0xFF15803D) else MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("${cityA.city} Window:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(overlap.zoneAWindow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("${cityB.city} Window:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(overlap.zoneBWindow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showPickerCityA) {
        CityPickerBottomSheet(
            title = "Select Origin City (City A)",
            alreadySelectedIds = setOf(cityB.id),
            onSelectZone = {
                viewModel.setDiffCityA(it)
                showPickerCityA = false
            },
            onDismiss = { showPickerCityA = false }
        )
    }

    if (showPickerCityB) {
        CityPickerBottomSheet(
            title = "Select Destination City (City B)",
            alreadySelectedIds = setOf(cityA.id),
            onSelectZone = {
                viewModel.setDiffCityB(it)
                showPickerCityB = false
            },
            onDismiss = { showPickerCityB = false }
        )
    }
}

// ==========================================
// REUSABLE SEARCHABLE CITY PICKER
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityPickerBottomSheet(
    title: String,
    alreadySelectedIds: Set<String>,
    onSelectZone: (WorldZone) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedRegion by remember { mutableStateOf("All") }

    val allZones = remember { WorldClockHelper.getAllZones() }
    val regions = remember { listOf("All", "Americas", "Europe", "Asia", "Middle East", "Oceania", "Africa") }

    val filteredZones = remember(searchQuery, selectedRegion, allZones) {
        allZones.filter { zone ->
            (selectedRegion == "All" || zone.region.equals(selectedRegion, ignoreCase = true)) &&
            (searchQuery.isBlank() ||
             zone.city.contains(searchQuery, ignoreCase = true) ||
             zone.country.contains(searchQuery, ignoreCase = true) ||
             zone.id.contains(searchQuery, ignoreCase = true))
        }
    }

    val now = remember { Date() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search TextField
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search city, country or timezone...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Region Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                regions.forEach { reg ->
                    FilterChip(
                        selected = selectedRegion == reg,
                        onClick = { selectedRegion = reg },
                        label = { Text(reg, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // City Results List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (filteredZones.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No matching cities found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(filteredZones, key = { it.id }) { zone ->
                        val isSelected = alreadySelectedIds.contains(zone.id)
                        val tz = remember(zone.id) { TimeZone.getTimeZone(zone.id) }
                        val timePreview = remember(zone.id) { WorldClockHelper.formatTime(now, tz) }
                        val offsetPreview = remember(zone.id) { WorldClockHelper.formatUtcOffset(tz, now) }

                        Surface(
                            onClick = { onSelectZone(zone) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = zone.city,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "${zone.country} • $offsetPreview",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = timePreview,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Added",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.AddCircleOutline,
                                            contentDescription = "Add",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
