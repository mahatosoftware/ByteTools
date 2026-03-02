package `in`.mahato.bytetools.ui.tools.battery

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryInfoScreen(navController: NavController) {
    val context = LocalContext.current
    var batteryInfo by remember { mutableStateOf(BatteryData()) }

    DisposableEffect(Unit) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent?.let {
                    val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val percentage = (level * 100 / scale.toFloat()).toInt()
                    
                    val temp = it.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f
                    val health = getHealthString(it.getIntExtra(BatteryManager.EXTRA_HEALTH, 0))
                    val status = getStatusString(it.getIntExtra(BatteryManager.EXTRA_STATUS, 0))
                    val plugged = getPluggedString(it.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0))
                    val voltage = it.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
                    val technology = it.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Unknown"

                    batteryInfo = BatteryData(
                        percentage = percentage,
                        temperature = temp,
                        health = health,
                        status = status,
                        plugged = plugged,
                        voltage = voltage,
                        technology = technology
                    )
                }
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Battery Info") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().height(150.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${batteryInfo.percentage}%", style = MaterialTheme.typography.displayLarge)
                        Text(batteryInfo.status, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            InfoRow("Health", batteryInfo.health)
            InfoRow("Temperature", "${batteryInfo.temperature}°C")
            InfoRow("Charging Type", batteryInfo.plugged)
            InfoRow("Voltage", "${batteryInfo.voltage} mV")
            InfoRow("Technology", batteryInfo.technology)
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.outline)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

data class BatteryData(
    val percentage: Int = 0,
    val temperature: Float = 0f,
    val health: String = "Unknown",
    val status: String = "Unknown",
    val plugged: String = "Unknown",
    val voltage: Int = 0,
    val technology: String = "Unknown"
)

fun getHealthString(health: Int): String = when (health) {
    BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
    BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
    BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
    BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
    BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
    BatteryManager.BATTERY_HEALTH_UNKNOWN -> "Unknown"
    BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Unspecified Failure"
    else -> "Unknown"
}

fun getStatusString(status: Int): String = when (status) {
    BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
    BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
    BatteryManager.BATTERY_STATUS_FULL -> "Full"
    BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
    BatteryManager.BATTERY_STATUS_UNKNOWN -> "Unknown"
    else -> "Unknown"
}

fun getPluggedString(plugged: Int): String = when (plugged) {
    BatteryManager.BATTERY_PLUGGED_AC -> "AC Charger"
    BatteryManager.BATTERY_PLUGGED_USB -> "USB Port"
    BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
    0 -> "Battery"
    else -> "Unknown"
}
