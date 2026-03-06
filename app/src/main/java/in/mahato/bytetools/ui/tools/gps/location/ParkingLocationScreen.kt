package `in`.mahato.bytetools.ui.tools.gps.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ParkingLocationScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val locationPermissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var address by remember { mutableStateOf("Fetching current address...") }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    
    val sharedPrefs = remember { context.getSharedPreferences("parking_prefs", Context.MODE_PRIVATE) }
    var savedParkingLat by remember { mutableStateOf(sharedPrefs.getFloat("parking_lat", 0f)) }
    var savedParkingLng by remember { mutableStateOf(sharedPrefs.getFloat("parking_lng", 0f)) }
    var savedParkingTime by remember { mutableStateOf(sharedPrefs.getString("parking_time", "")) }

    val hasSavedParking = savedParkingLat != 0f && savedParkingLng != 0f

    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    currentLocation = LatLng(location.latitude, location.longitude)
                    scope.launch {
                        address = getAddressFromLocation(context, location.latitude, location.longitude)
                    }
                }
            }
        }
    }

    LaunchedEffect(locationPermissionsState.allPermissionsGranted) {
        if (locationPermissionsState.allPermissionsGranted) {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000)
                .build()
            
            try {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    context.mainLooper
                )
            } catch (e: SecurityException) {
                // Handle permission dynamically
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Parking Location", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (locationPermissionsState.allPermissionsGranted) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Current Location Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MyLocation, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Current Location", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        if (currentLocation != null) {
                            Text(address, style = MaterialTheme.typography.bodyMedium)
                            Text("Lat: ${currentLocation!!.latitude}, Lng: ${currentLocation!!.longitude}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Text("Getting location...", style = MaterialTheme.typography.bodyMedium)
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = {
                                currentLocation?.let {
                                    val time = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
                                    sharedPrefs.edit()
                                        .putFloat("parking_lat", it.latitude.toFloat())
                                        .putFloat("parking_lng", it.longitude.toFloat())
                                        .putString("parking_time", time)
                                        .apply()
                                    
                                    savedParkingLat = it.latitude.toFloat()
                                    savedParkingLng = it.longitude.toFloat()
                                    savedParkingTime = time
                                    
                                    android.widget.Toast.makeText(context, "Parking location saved!", android.widget.Toast.LENGTH_SHORT).show()
                                } ?: android.widget.Toast.makeText(context, "Waiting for GPS lock...", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.Default.LocalParking, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Parking Location")
                        }
                    }
                }

                // Saved Parking Card
                if (hasSavedParking) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Saved Parking", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            
                            Text("Lat: $savedParkingLat, Lng: $savedParkingLng", style = MaterialTheme.typography.bodyMedium)
                            Text("Saved on: $savedParkingTime", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        val gmmIntentUri = Uri.parse("google.navigation:q=$savedParkingLat,$savedParkingLng&mode=w")
                                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                        mapIntent.setPackage("com.google.android.apps.maps")
                                        try {
                                            context.startActivity(mapIntent)
                                        } catch (e: Exception) {
                                            // Fallback if google maps is not installed
                                            val bUri = Uri.parse("https://maps.google.com/?daddr=$savedParkingLat,$savedParkingLng&dirflg=w")
                                            context.startActivity(Intent(Intent.ACTION_VIEW, bUri))
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = MaterialTheme.shapes.medium,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.Navigation, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Navigate")
                                }
                                
                                OutlinedButton(
                                    onClick = {
                                        sharedPrefs.edit()
                                            .remove("parking_lat")
                                            .remove("parking_lng")
                                            .remove("parking_time")
                                            .apply()
                                            
                                        savedParkingLat = 0f
                                        savedParkingLng = 0f
                                        savedParkingTime = ""
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Clear")
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.LocationOff, contentDescription = null, modifier = Modifier.size(100.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Location Permission Required", style = MaterialTheme.typography.titleLarge)
                    Text("Please grant location permissions to use this tool.", textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { locationPermissionsState.launchMultiplePermissionRequest() }) {
                        Text("Grant Permission")
                    }
                }
            }
        }
    }
}
