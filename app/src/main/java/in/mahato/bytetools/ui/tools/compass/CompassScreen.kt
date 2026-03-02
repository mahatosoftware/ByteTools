package `in`.mahato.bytetools.ui.tools.compass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompassScreen(navController: NavController) {
    var azimuth by remember { mutableFloatStateOf(0f) }
    val animatedAzimuth by animateFloatAsState(targetValue = -azimuth)
    val context = LocalContext.current
    var showCalibration by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val gravity = FloatArray(3)
        val geomagnetic = FloatArray(3)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    System.arraycopy(event.values, 0, gravity, 0, event.values.size)
                }
                if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    System.arraycopy(event.values, 0, geomagnetic, 0, event.values.size)
                }

                val R = FloatArray(9)
                val I = FloatArray(9)
                if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(R, orientation)
                    azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_UI)

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Compass", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showCalibration = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Calibration")
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
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${((azimuth + 360) % 360).roundToInt()}°",
                style = MaterialTheme.typography.displayLarge
            )
            Text(
                text = getDirectionText(azimuth),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(48.dp))

            CompassRose(animatedAzimuth)
        }

        if (showCalibration) {
            AlertDialog(
                onDismissRequest = { showCalibration = false },
                title = { Text("Calibration Instruction") },
                text = { Text("To calibrate the compass, move your phone in a figure-8 motion several times in the air.") },
                confirmButton = {
                    TextButton(onClick = { showCalibration = false }) {
                        Text("Got it")
                    }
                }
            )
        }
    }
}

@Composable
fun CompassRose(rotation: Float) {
    Box(modifier = Modifier.size(280.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.width / 2

            // Static outer circle
            drawCircle(
                color = Color.LightGray,
                radius = radius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            rotate(rotation, pivot = center) {
                // North indicator
                drawLine(
                    color = Color.Red,
                    start = center,
                    end = Offset(center.x, center.y - radius + 20.dp.toPx()),
                    strokeWidth = 4.dp.toPx()
                )
                // Directions
                // N, E, S, W text would be better with Canvas.drawText or Layout, 
                // but for simplicity we'll just draw lines
                for (i in 0 until 360 step 30) {
                    rotate(i.toFloat(), pivot = center) {
                        drawLine(
                            color = if (i % 90 == 0) Color.Gray else Color.LightGray,
                            start = Offset(center.x, center.y - radius),
                            end = Offset(center.x, center.y - radius + (if (i % 90 == 0) 20.dp.toPx() else 10.dp.toPx())),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }
            }
            
            // Fixed Needle
            drawCircle(color = Color.Black, radius = 5.dp.toPx(), center = center)
        }
    }
}

fun getDirectionText(azimuth: Float): String {
    val deg = (azimuth + 360) % 360
    return when {
        deg >= 337.5 || deg < 22.5 -> "North"
        deg >= 22.5 && deg < 67.5 -> "North East"
        deg >= 67.5 && deg < 112.5 -> "East"
        deg >= 112.5 && deg < 157.5 -> "South East"
        deg >= 157.5 && deg < 202.5 -> "South"
        deg >= 202.5 && deg < 247.5 -> "South West"
        deg >= 247.5 && deg < 292.5 -> "West"
        deg >= 292.5 && deg < 337.5 -> "North West"
        else -> "N/A"
    }
}
