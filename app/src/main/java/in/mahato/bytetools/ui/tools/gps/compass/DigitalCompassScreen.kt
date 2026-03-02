package `in`.mahato.bytetools.ui.tools.gps.compass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigitalCompassScreen(navController: NavController) {
    val context = LocalContext.current
    var azimuth by remember { mutableFloatStateOf(0f) }
    var isTrueNorth by remember { mutableStateOf(false) }
    var showCalibration by remember { mutableStateOf(false) }

    val animatedAzimuth by animateFloatAsState(targetValue = -azimuth)

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

                val r = FloatArray(9)
                val i = FloatArray(9)
                if (SensorManager.getRotationMatrix(r, i, gravity, geomagnetic)) {
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(r, orientation)
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
            TopAppBar(
                title = { Text("Digital Compass", fontWeight = FontWeight.Bold) },
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
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${((azimuth + 360) % 360).roundToInt()}°",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 80.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = getDirectionText(azimuth),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier.size(300.dp),
                contentAlignment = Alignment.Center
            ) {
                CompassDial(animatedAzimuth)
            }

            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("North Reference", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(if (isTrueNorth) "True North" else "Magnetic North", style = MaterialTheme.typography.bodyMedium)
                    }
                    Switch(
                        checked = isTrueNorth,
                        onCheckedChange = { isTrueNorth = it }
                    )
                }
            }
        }

        if (showCalibration) {
            AlertDialog(
                onDismissRequest = { showCalibration = false },
                title = { Text("Calibration Instructions") },
                text = { Text("To improve accuracy, hold your device and wave it in a figure-8 motion several times. Ensure you are away from large metal objects or magnetic fields.") },
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
fun CompassDial(rotation: Float) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val primary = MaterialTheme.colorScheme.primary
    val error = MaterialTheme.colorScheme.error

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.width / 2

        // Outer Ring
        drawCircle(
            color = onSurface.copy(alpha = 0.1f),
            radius = radius,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )

        rotate(rotation, pivot = center) {
            // Degree marks
            for (i in 0 until 360 step 10) {
                val angleRad = Math.toRadians(i.toDouble() - 90).toFloat()
                val lineLength = if (i % 30 == 0) 15.dp.toPx() else 8.dp.toPx()
                val start = Offset(
                    center.x + (radius - lineLength) * kotlin.math.cos(angleRad),
                    center.y + (radius - lineLength) * kotlin.math.sin(angleRad)
                )
                val end = Offset(
                    center.x + radius * kotlin.math.cos(angleRad),
                    center.y + radius * kotlin.math.sin(angleRad)
                )
                drawLine(
                    color = if (i % 90 == 0) primary else onSurface.copy(alpha = 0.3f),
                    start = start,
                    end = end,
                    strokeWidth = if (i % 90 == 0) 3.dp.toPx() else 1.dp.toPx()
                )
            }

            // Direction Labels
            val directions = listOf("N", "E", "S", "W")
            directions.forEachIndexed { index, label ->
                val angle = index * 90f
                rotate(angle, pivot = center) {
                    // Note: Drawing text in Canvas requires NativeCanvas or Layout.
                    // For brevity, we focus on the needle.
                }
            }
        }

        // Fixed Needle (The needle points to North relative to the rotated dial)
        // Red part (North)
        val needlePath = Path().apply {
            moveTo(center.x, center.y - radius + 20.dp.toPx())
            lineTo(center.x - 12.dp.toPx(), center.y)
            lineTo(center.x + 12.dp.toPx(), center.y)
            close()
        }
        drawPath(needlePath, color = error)

        // White part (South)
        val southNeedlePath = Path().apply {
            moveTo(center.x, center.y + radius - 20.dp.toPx())
            lineTo(center.x - 12.dp.toPx(), center.y)
            lineTo(center.x + 12.dp.toPx(), center.y)
            close()
        }
        drawPath(southNeedlePath, color = onSurface.copy(alpha = 0.6f))
        
        drawCircle(color = onSurface, radius = 4.dp.toPx(), center = center)
    }
}

fun getDirectionText(azimuth: Float): String {
    val deg = (azimuth + 360) % 360
    return when {
        deg >= 337.5 || deg < 22.5 -> "NORTH"
        deg >= 22.5 && deg < 67.5 -> "NORTH EAST"
        deg >= 67.5 && deg < 112.5 -> "EAST"
        deg >= 112.5 && deg < 157.5 -> "SOUTH EAST"
        deg >= 157.5 && deg < 202.5 -> "SOUTH"
        deg >= 202.5 && deg < 247.5 -> "SOUTH WEST"
        deg >= 247.5 && deg < 292.5 -> "WEST"
        deg >= 292.5 && deg < 337.5 -> "NORTH WEST"
        else -> "N/A"
    }
}
