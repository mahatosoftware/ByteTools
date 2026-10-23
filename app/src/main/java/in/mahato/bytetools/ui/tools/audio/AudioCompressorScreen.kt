package `in`.mahato.bytetools.ui.tools.audio

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import kotlinx.coroutines.*
import java.io.File
import kotlin.math.roundToInt

enum class CompressionPreset(val title: String, val desc: String, val bitrate: Int, val channels: Int) {
    HIGH_QUALITY("High Quality", "192 kbps Stereo (Crisp audio for music)", 192000, 2),
    BALANCED("Balanced", "128 kbps Stereo (Optimal size/clarity for sharing)", 128000, 2),
    SMALL_SIZE("Small Size", "64 kbps Mono (Smallest size for voice notes)", 64000, 1)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioCompressorScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var inputUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var fileName by rememberSaveable { mutableStateOf("") }
    var originalSizeBytes by rememberSaveable { mutableLongStateOf(0L) }
    var durationMs by rememberSaveable { mutableLongStateOf(0L) }

    var selectedPreset by rememberSaveable { mutableStateOf(CompressionPreset.BALANCED) }

    var isCompressing by remember { mutableStateOf(false) }
    var compressProgress by remember { mutableFloatStateOf(0f) }
    var compressedFile by remember { mutableStateOf<File?>(null) }
    var savedToHistory by rememberSaveable { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("audio/*"),
        onResult = { destinationUri ->
            if (destinationUri != null && compressedFile != null) {
                scope.launch(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                            compressedFile!!.inputStream().use { it.copyTo(out) }
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Saved compressed audio to device", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    )

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                inputUriString = uri.toString()
                fileName = AudioEngine.getFileName(context, uri)
                compressedFile = null
                savedToHistory = false

                scope.launch(Dispatchers.IO) {
                    val size = context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L
                    val meta = AudioEngine.getAudioMetadata(context, uri)
                    withContext(Dispatchers.Main) {
                        originalSizeBytes = size
                        durationMs = meta.durationMs
                    }
                }
            }
        }
    )

    fun startCompressing() {
        val uri = inputUriString?.let(Uri::parse) ?: return
        isCompressing = true
        compressProgress = 0f

        scope.launch {
            try {
                val cleanBase = fileName.substringBeforeLast(".")
                val outFile = File(context.cacheDir, "${cleanBase}_compressed.m4a")

                AudioEngine.convertAudio(
                    context = context,
                    inputUri = uri,
                    outputFormat = "m4a",
                    targetBitrate = selectedPreset.bitrate,
                    targetChannels = selectedPreset.channels,
                    outFile = outFile,
                    onProgress = { compressProgress = it }
                )

                compressedFile = outFile
                isCompressing = false
                Toast.makeText(context, "Audio compressed successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                isCompressing = false
                Toast.makeText(context, "Compression failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Estimate output size: (duration in seconds) * (bitrate in bits/sec / 8)
    val estimatedBytes = if (durationMs > 0) {
        val secs = durationMs / 1000.0
        (secs * (selectedPreset.bitrate / 8.0)).toLong()
    } else 0L

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audio Compressor", fontWeight = FontWeight.Bold) },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Select Audio File
            if (inputUriString == null) {
                Card(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    onClick = { pickerLauncher.launch(arrayOf("audio/*")) },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier.size(54.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Compress, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                            }
                            Spacer(Modifier.height(10.dp))
                            Text("Select Audio to Compress", fontWeight = FontWeight.Bold)
                            Text("Reduce file size for WhatsApp, Email & Storage", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AudioFile, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(fileName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "Original Size: ${AudioEngine.formatFileSize(originalSizeBytes)} • ${AudioEngine.formatDuration(durationMs)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { pickerLauncher.launch(arrayOf("audio/*")) }) {
                            Text("Change")
                        }
                    }
                }

                // Preset Selection
                if (compressedFile == null) {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Select Quality Preset", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                            CompressionPreset.entries.forEach { preset ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selectedPreset == preset) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    onClick = { selectedPreset = preset }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = selectedPreset == preset,
                                            onClick = { selectedPreset = preset }
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Column {
                                            Text(preset.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            Text(preset.desc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Size Estimation & Quality Trade-Off Notice
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Original Size", style = MaterialTheme.typography.bodySmall)
                                Text(AudioEngine.formatFileSize(originalSizeBytes), fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Estimated Output", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                Text(
                                    if (estimatedBytes > 0) "~${AudioEngine.formatFileSize(estimatedBytes)}" else "Calculating...",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Quality Notice: Lower bitrates reduce file size substantially while preserving speech intelligibility, but may subtly reduce high-frequency clarity in music.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Compress Button / Progress
                    if (isCompressing) {
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Compressing Audio...", fontWeight = FontWeight.Bold)
                                LinearProgressIndicator(progress = compressProgress, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)))
                            }
                        }
                    } else {
                        Button(
                            onClick = { startCompressing() },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Compress, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Compress Audio Now", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Results Card
            if (compressedFile != null) {
                val file = compressedFile!!
                val newSize = file.length()
                val savedPercent = if (originalSizeBytes > 0) {
                    (((originalSizeBytes - newSize).toDouble() / originalSizeBytes) * 100).roundToInt().coerceAtLeast(0)
                } else 0

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Compression Complete!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Reduced by $savedPercent%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Original Size:", style = MaterialTheme.typography.bodySmall)
                                    Text(AudioEngine.formatFileSize(originalSizeBytes))
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("New Size:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    Text(AudioEngine.formatFileSize(newSize), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = {
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "audio/*"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Compressed Audio"))
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Share")
                            }

                            Button(
                                onClick = {
                                    val saved = AudioEngine.saveAudioToHistory(context, file)
                                    if (saved != null) {
                                        savedToHistory = true
                                        Toast.makeText(context, "Saved to Audio History", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !savedToHistory
                            ) {
                                Text(if (savedToHistory) "Saved" else "Save")
                            }
                        }

                        OutlinedButton(
                            onClick = { exportLauncher.launch(file.name) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Export to Device")
                        }
                    }
                }
            }
        }
    }
}
