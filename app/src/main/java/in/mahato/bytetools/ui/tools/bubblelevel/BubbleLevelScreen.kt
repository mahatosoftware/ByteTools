package `in`.mahato.bytetools.ui.tools.bubblelevel

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlin.math.atan2
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BubbleLevelScreen(navController: NavController) {
    var roll by remember { mutableFloatStateOf(0f) } // X-axis (left-right)
    var pitch by remember { mutableFloatStateOf(0f) } // Y-axis (top-bottom)
    val context = LocalContext.current

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]

                    // Calculate roll and pitch in degrees
                    roll = x * 10f // Scale for visualization
                    pitch = y * 10f
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bubble Level") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LevelIndicator(roll, pitch)
            
            Spacer(modifier = Modifier.height(64.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                AxisValue("X (Roll)", roll / 10f)
                AxisValue("Y (Pitch)", pitch / 10f)
            }
        }
    }
}

@Composable
fun LevelIndicator(roll: Float, pitch: Float) {
    Box(
        modifier = Modifier
            .size(300.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        // Center Target
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(Color.Transparent, CircleShape)
                .border(2.dp, Color.Gray.copy(alpha = 0.5f), CircleShape)
        )

        // The Bubble
        Box(
            modifier = Modifier
                .offset(x = (-roll).dp, y = pitch.dp) // Negative roll because x is inverted on portrait
                .size(50.dp)
                .background(
                    if (sqrt(roll * roll + pitch * pitch) < 2f) Color(0xFF4CAF50) else Color(0xFFFFC107),
                    CircleShape
                )
        )
        
        // Target Lines
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = size.width / 2
            drawLine(Color.Gray.copy(alpha = 0.3f), start = Offset(0f, center), end = Offset(size.width, center))
            drawLine(Color.Gray.copy(alpha = 0.3f), start = Offset(center, 0f), end = Offset(center, size.height))
        }
    }
}

@Composable
fun AxisValue(label: String, value: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "${String.format("%.1f", value)}°", style = MaterialTheme.typography.headlineSmall)
        Text(text = label, style = MaterialTheme.typography.labelMedium)
    }
}
