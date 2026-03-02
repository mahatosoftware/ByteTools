package `in`.mahato.bytetools.ui.tools.gps.signal

import android.Manifest
import android.content.Context
import android.location.GnssStatus
import android.location.LocationManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GPSSignalScreen(navController: NavController) {
    val context = LocalContext.current
    val locationManager = remember { context.getSystemService(Context.LOCATION_SERVICE) as LocationManager }
    
    var satelliteCount by remember { mutableIntStateOf(0) }
    var satList by remember { mutableStateOf<List<SatInfo>>(emptyList()) }
    var isEnabled by remember { mutableStateOf(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) }

    val permissionsState = rememberMultiplePermissionsState(
        listOf(Manifest.permission.ACCESS_FINE_LOCATION)
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        val gnssStatusCallback = remember {
            object : GnssStatus.Callback() {
                override fun onSatelliteStatusChanged(status: GnssStatus) {
                    val list = mutableListOf<SatInfo>()
                    for (i in 0 until status.satelliteCount) {
                        list.add(
                            SatInfo(
                                svid = status.getSvid(i),
                                cn0 = status.getCn0DbHz(i),
                                constellation = getConstellationName(status.getConstellationType(i)),
                                usedInFix = status.usedInFix(i)
                            )
                        )
                    }
                    satList = list
                    satelliteCount = status.satelliteCount
                }
            }
        }

        DisposableEffect(permissionsState.allPermissionsGranted) {
            if (permissionsState.allPermissionsGranted) {
                try {
                    locationManager.registerGnssStatusCallback(gnssStatusCallback, null)
                } catch (e: SecurityException) {
                }
            }
            onDispose {
                locationManager.unregisterGnssStatusCallback(gnssStatusCallback)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GPS Signal Info", fontWeight = FontWeight.Bold) },
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
                .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), MaterialTheme.colorScheme.background)))
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Signal Strength Summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Satellites", style = MaterialTheme.typography.labelMedium)
                        Text("$satelliteCount", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black)
                    }
                    val signalIcon = when {
                        satelliteCount > 10 -> Icons.Default.GpsFixed
                        satelliteCount > 0 -> Icons.Default.GpsNotFixed
                        else -> Icons.Default.GpsOff
                    }
                    Icon(signalIcon, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tech Specs Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SignalDetailCard("Status", if (isEnabled) "Active" else "Disabled", Icons.Default.GpsFixed, Modifier.weight(1f))
                SignalDetailCard("Provider", "GPS / GNSS", Icons.Default.SettingsInputAntenna, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Satellite List", style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold)
            
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(satList) { sat ->
                    SatelliteItem(sat)
                }
            }
        }
    }
}

data class SatInfo(
    val svid: Int,
    val cn0: Float,
    val constellation: String,
    val usedInFix: Boolean
)

@Composable
fun SatelliteItem(sat: SatInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (sat.usedInFix) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (sat.usedInFix) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (sat.usedInFix) MaterialTheme.colorScheme.primary else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("ID: ${sat.svid} (${sat.constellation})", fontWeight = FontWeight.Bold)
                    Text("Signal: ${String.format("%.1f", sat.cn0)} dB-Hz", style = MaterialTheme.typography.labelSmall)
                }
            }
            if (sat.usedInFix) {
                Text("USED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SignalDetailCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall)
                Text(value, fontWeight = FontWeight.Bold)
            }
        }
    }
}

fun getConstellationName(type: Int): String {
    return when (type) {
        GnssStatus.CONSTELLATION_GPS -> "GPS"
        GnssStatus.CONSTELLATION_GLONASS -> "GLONASS"
        GnssStatus.CONSTELLATION_BEIDOU -> "BEIDOU"
        GnssStatus.CONSTELLATION_GALILEO -> "GALILEO"
        GnssStatus.CONSTELLATION_QZSS -> "QZSS"
        GnssStatus.CONSTELLATION_IRNSS -> "IRNSS"
        else -> "UNKNOWN"
    }
}
