package `in`.mahato.bytetools.ui.tools.flashlight

import android.content.Context
import android.hardware.camera2.CameraManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashlightScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isFlashOn by remember { mutableStateOf(false) }
    var isStrobeOn by remember { mutableStateOf(false) }
    var strobeSpeed by remember { mutableFloatStateOf(500f) }

    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    val cameraId = cameraManager.cameraIdList.firstOrNull()

    fun toggleFlash(on: Boolean) {
        cameraId?.let {
            try {
                cameraManager.setTorchMode(it, on)
                isFlashOn = on
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(isStrobeOn, strobeSpeed) {
        if (isStrobeOn) {
            while (true) {
                toggleFlash(true)
                delay(strobeSpeed.toLong())
                toggleFlash(false)
                delay(strobeSpeed.toLong())
            }
        } else {
            // Restore manual state when strobe is turned off
            // toggleFlash(isFlashOn) // This might be tricky, let's just turn off for safety
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            toggleFlash(false)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Flashlight", fontWeight = FontWeight.Bold) },
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
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            IconButton(
                onClick = { 
                    if (!isStrobeOn) {
                        toggleFlash(!isFlashOn) 
                    }
                },
                modifier = Modifier.size(200.dp),
                enabled = !isStrobeOn
            ) {
                Icon(
                    imageVector = if (isFlashOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                    contentDescription = "Flash Toggle",
                    modifier = Modifier.size(150.dp),
                    tint = if (isFlashOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Strobe Mode (Pro)", style = MaterialTheme.typography.titleMedium)
                        Switch(
                            checked = isStrobeOn,
                            onCheckedChange = { 
                                isStrobeOn = it
                                if (!it) toggleFlash(false)
                            }
                        )
                    }

                    if (isStrobeOn) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Strobe Speed: ${strobeSpeed.toInt()}ms")
                        Slider(
                            value = strobeSpeed,
                            onValueChange = { strobeSpeed = it },
                            valueRange = 50f..1000f,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
