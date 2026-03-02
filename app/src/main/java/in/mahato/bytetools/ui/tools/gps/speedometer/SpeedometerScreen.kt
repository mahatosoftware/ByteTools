package `in`.mahato.bytetools.ui.tools.gps.speedometer

import android.Manifest
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SpeedometerScreen(navController: NavController) {
    val context = LocalContext.current
    val locationPermissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    var currentSpeed by remember { mutableFloatStateOf(0f) }
    var maxSpeed by remember { mutableFloatStateOf(0f) }
    var avgSpeed by remember { mutableFloatStateOf(0f) }
    var totalDistance by remember { mutableFloatStateOf(0f) }
    var tripDurationSeconds by remember { mutableLongStateOf(0L) }
    var isTracking by remember { mutableStateOf(false) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val locationCallback = remember {
        object : LocationCallback() {
            var lastLocation: android.location.Location? = null
            var totalSpeedSum = 0f
            var speedCount = 0

            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    val speedKmH = location.speed * 3.6f
                    currentSpeed = speedKmH
                    if (speedKmH > maxSpeed) maxSpeed = speedKmH
                    
                    if (speedKmH > 1.0f) { // Only count if moving
                        totalSpeedSum += speedKmH
                        speedCount++
                        avgSpeed = totalSpeedSum / speedCount
                    }

                    lastLocation?.let { last ->
                        totalDistance += last.distanceTo(location) / 1000f // Convert to km
                    }
                    lastLocation = location
                }
            }
        }
    }

    LaunchedEffect(isTracking) {
        if (isTracking) {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .build()
            
            try {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    context.mainLooper
                )
                
                while (isTracking) {
                    delay(1000)
                    tripDurationSeconds++
                }
            } catch (e: SecurityException) {
                isTracking = false
            }
        } else {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Speedometer", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main Speed Display
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { currentSpeed / 180f }, // Range 0-180 km/h
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 12.dp,
                    color = if (currentSpeed > 80) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${currentSpeed.toInt()}",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 80.sp,
                            fontWeight = FontWeight.Black
                        )
                    )
                    Text("km/h", style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Stats Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Max Speed", "${String.format("%.1f", maxSpeed)}", "km/h", Icons.Default.TrendingUp, Modifier.weight(1f))
                StatCard("Avg Speed", "${String.format("%.1f", avgSpeed)}", "km/h", Icons.Default.Timeline, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Distance", "${String.format("%.2f", totalDistance)}", "km", Icons.Default.Route, Modifier.weight(1f))
                StatCard("Duration", formatDuration(tripDurationSeconds), "", Icons.Default.Timer, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (locationPermissionsState.allPermissionsGranted) {
                        isTracking = !isTracking
                    } else {
                        locationPermissionsState.launchMultiplePermissionRequest()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTracking) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primary,
                    contentColor = if (isTracking) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(if (isTracking) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isTracking) "Stop Trip" else "Start Trip", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, unit: String, icon: ImageVector, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(unit, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
}
