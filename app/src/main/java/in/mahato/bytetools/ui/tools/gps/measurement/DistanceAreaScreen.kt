package `in`.mahato.bytetools.ui.tools.gps.measurement

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import kotlin.math.*

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DistanceAreaScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val points = remember { mutableStateListOf<LatLng>() }
    var totalDistance by remember { mutableDoubleStateOf(0.0) }
    var totalArea by remember { mutableDoubleStateOf(0.0) }

    val permissionsState = rememberMultiplePermissionsState(
        listOf(Manifest.permission.ACCESS_FINE_LOCATION)
    )

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    fun updateCalculations() {
        // Distance calculation
        var dist = 0.0
        for (i in 0 until points.size - 1) {
            dist += calculateHaversineDistance(points[i], points[i + 1])
        }
        totalDistance = dist

        // Area calculation (Spherical polygon area)
        totalArea = if (points.size >= 3) calculateArea(points) else 0.0
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Distance & Area", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        points.clear()
                        updateCalculations()
                    }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (permissionsState.allPermissionsGranted) {
                        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                            .addOnSuccessListener { location ->
                                location?.let {
                                    points.add(LatLng(it.latitude, it.longitude))
                                    updateCalculations()
                                }
                            }
                    } else {
                        permissionsState.launchMultiplePermissionRequest()
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.AddLocation, contentDescription = "Add Current Point")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), MaterialTheme.colorScheme.background)))
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            // Stats Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MeasurementCard("Distance", "${String.format("%.2f", totalDistance)} km", Icons.Default.Straighten, Modifier.weight(1f))
                MeasurementCard("Area", "${String.format("%.2f", totalArea)} m²", Icons.Default.SquareFoot, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Points Collected (${points.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(points) { index, point ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("${index + 1}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Lat: ${String.format("%.6f", point.latitude)}", fontSize = 12.sp)
                                Text("Lng: ${String.format("%.6f", point.longitude)}", fontSize = 12.sp)
                            }
                            IconButton(onClick = {
                                points.removeAt(index)
                                updateCalculations()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (points.isNotEmpty()) {
                        val path = points.joinToString("|") { "${it.latitude},${it.longitude}" }
                        val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&waypoints=$path")
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = points.isNotEmpty(),
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Default.Map, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("View Path in Google Maps")
            }
        }
    }
}

@Composable
fun MeasurementCard(label: String, value: String, icon: android.graphics.drawable.Icon? = null, modifier: Modifier, iconVector: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(iconVector, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        }
    }
}

// Fixed MeasurementCard to use ImageVector correctly
@Composable
fun MeasurementCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        }
    }
}

fun calculateHaversineDistance(p1: LatLng, p2: LatLng): Double {
    val r = 6371.0 // Earth radius in km
    val dLat = Math.toRadians(p2.latitude - p1.latitude)
    val dLon = Math.toRadians(p2.longitude - p1.longitude)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(p1.latitude)) * cos(Math.toRadians(p2.latitude)) * sin(dLon / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}

fun calculateArea(nodes: List<LatLng>): Double {
    if (nodes.size < 3) return 0.0
    var area = 0.0
    val radius = 6378137.0 // Earth radius in meters
    for (i in nodes.indices) {
        val p1 = nodes[i]
        val p2 = nodes[(i + 1) % nodes.size]
        area += Math.toRadians(p2.longitude - p1.longitude) * (2 + sin(Math.toRadians(p1.latitude)) + sin(Math.toRadians(p2.latitude)))
    }
    area = area * radius * radius / 2.0
    return abs(area)
}
