package `in`.mahato.bytetools.ui.tools.device

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceInfoScreen(navController: NavController) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("General", "Hardware", "Sensors")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Device Info") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            when (selectedTab) {
                0 -> GeneralInfoPage()
                1 -> HardwareInfoPage()
                2 -> SensorsInfoPage(context)
            }
        }
    }
}

@Composable
fun GeneralInfoPage() {
    val data = listOf(
        "Model" to Build.MODEL,
        "Manufacturer" to Build.MANUFACTURER,
        "Brand" to Build.BRAND,
        "Device" to Build.DEVICE,
        "Product" to Build.PRODUCT,
        "Android Version" to Build.VERSION.RELEASE,
        "SDK Version" to Build.VERSION.SDK_INT.toString(),
        "Security Patch" to (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Build.VERSION.SECURITY_PATCH else "N/A"),
        "Build ID" to Build.ID
    )
    InfoList(data)
}

@Composable
fun HardwareInfoPage() {
    val data = listOf(
        "CPU ABI" to Build.SUPPORTED_ABIS.joinToString(", "),
        "Board" to Build.BOARD,
        "Hardware" to Build.HARDWARE,
        "Bootloader" to Build.BOOTLOADER,
        "Radio Version" to Build.getRadioVersion(),
        "Display" to Build.DISPLAY,
        "Fingerprint" to Build.FINGERPRINT
    )
    InfoList(data)
}

@Composable
fun SensorsInfoPage(context: Context) {
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val sensors = sensorManager.getSensorList(Sensor.TYPE_ALL)
    
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(sensors) { sensor ->
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(sensor.name, style = MaterialTheme.typography.titleSmall)
                Text("Vendor: ${sensor.vendor} | Type: ${getSensorType(sensor.type)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                Divider(modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
fun InfoList(data: List<Pair<String, String>>) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(data) { (label, value) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(value, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
            }
            Divider()
        }
    }
}

fun getSensorType(type: Int): String = when (type) {
    Sensor.TYPE_ACCELEROMETER -> "Accelerometer"
    Sensor.TYPE_GYROSCOPE -> "Gyroscope"
    Sensor.TYPE_MAGNETIC_FIELD -> "Magnetometer"
    Sensor.TYPE_LIGHT -> "Light"
    Sensor.TYPE_PROXIMITY -> "Proximity"
    Sensor.TYPE_PRESSURE -> "Pressure"
    Sensor.TYPE_AMBIENT_TEMPERATURE -> "Temperature"
    Sensor.TYPE_HEART_RATE -> "Heart Rate"
    else -> "Type $type"
}
