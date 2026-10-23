package `in`.mahato.bytetools.ui.tools.audio

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioConverterScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var inputUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var fileName by rememberSaveable { mutableStateOf("") }
    var inputMetadata by remember { mutableStateOf<AudioMetadata?>(null) }

    var selectedFormat by rememberSaveable { mutableStateOf("m4a") } // "m4a", "wav", "mp3"
    var selectedBitrate by rememberSaveable { mutableIntStateOf(192000) } // 64k, 96k, 128k, 192k, 256k, 320k
    var selectedSampleRate by rememberSaveable { mutableIntStateOf(44100) } // 44100, 48000, 32000
    var selectedChannels by rememberSaveable { mutableIntStateOf(2) } // 1 (Mono), 2 (Stereo)

    var isConverting by remember { mutableStateOf(false) }
    var conversionProgress by remember { mutableFloatStateOf(0f) }
    var conversionJob by remember { mutableStateOf<Job?>(null) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    var convertedFile by remember { mutableStateOf<File?>(null) }
    var savedToHistory by rememberSaveable { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }

    val bitrates = listOf(
        64000 to "64 kbps (Voice)",
        96000 to "96 kbps",
        128000 to "128 kbps (Standard)",
        192000 to "192 kbps (High)",
        256000 to "256 kbps (Very High)",
        320000 to "320 kbps (Max)"
    )

    val sampleRates = listOf(
        44100 to "44.1 kHz (CD Quality)",
        48000 to "48.0 kHz (Studio)",
        32000 to "32.0 kHz (Voice)"
    )

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("audio/*"),
        onResult = { destinationUri ->
            if (destinationUri != null && convertedFile != null) {
                scope.launch(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                            convertedFile!!.inputStream().use { it.copyTo(out) }
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Saved to device successfully", Toast.LENGTH_SHORT).show()
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
                convertedFile = null
                errorMessage = null
                savedToHistory = false
                scope.launch(Dispatchers.IO) {
                    val meta = AudioEngine.getAudioMetadata(context, uri)
                    withContext(Dispatchers.Main) {
                        inputMetadata = meta
                    }
                }
            }
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    fun startConversion() {
        val uri = inputUriString?.let(Uri::parse) ?: return
        isConverting = true
        conversionProgress = 0f
        errorMessage = null

        conversionJob = scope.launch {
            try {
                val cleanBase = fileName.substringBeforeLast(".")
                val ext = selectedFormat
                val outFile = File(context.cacheDir, "${cleanBase}_converted.$ext")

                AudioEngine.convertAudio(
                    context = context,
                    inputUri = uri,
                    outputFormat = selectedFormat,
                    targetBitrate = selectedBitrate,
                    targetSampleRate = selectedSampleRate,
                    targetChannels = selectedChannels,
                    outFile = outFile,
                    onProgress = { progress ->
                        conversionProgress = progress
                    }
                )

                convertedFile = outFile
                isConverting = false
                Toast.makeText(context, "Audio converted successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                isConverting = false
                errorMessage = "Conversion failed: ${e.localizedMessage}. This input format or codec might be unsupported."
            }
        }
    }

    fun stopConversion() {
        conversionJob?.cancel()
        conversionJob = null
        isConverting = false
        Toast.makeText(context, "Conversion cancelled", Toast.LENGTH_SHORT).show()
    }

    fun togglePlayback(file: File) {
        if (isPlaying) {
            mediaPlayer?.pause()
            isPlaying = false
        } else {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(file.absolutePath)
                    prepare()
                    setOnCompletionListener {
                        isPlaying = false
                    }
                }
            }
            mediaPlayer?.start()
            isPlaying = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audio & MP3 Converter", fontWeight = FontWeight.Bold) },
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
            // Step 1: Select Audio File Card
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
                                Icon(Icons.Default.AudioFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                            }
                            Spacer(Modifier.height(10.dp))
                            Text("Select Audio File", fontWeight = FontWeight.Bold)
                            Text("Supports WAV, M4A, AAC, FLAC, OGG, MP3", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            modifier = Modifier.size(46.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(fileName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            inputMetadata?.let { meta ->
                                Text(
                                    "${AudioEngine.formatDuration(meta.durationMs)} • ${meta.sampleRate} Hz • ${if (meta.channelCount == 1) "Mono" else "Stereo"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        TextButton(onClick = { pickerLauncher.launch(arrayOf("audio/*")) }) {
                            Text("Change")
                        }
                    }
                }
            }

            // Step 2: Format & Encoding Settings
            if (inputUriString != null && convertedFile == null) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Output Format", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf("m4a" to "M4A (AAC)", "wav" to "WAV (Lossless)", "mp3" to "MP3").forEach { (fmt, label) ->
                                FilterChip(
                                    selected = selectedFormat == fmt,
                                    onClick = { selectedFormat = fmt },
                                    label = { Text(label, fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        if (selectedFormat != "wav") {
                            HorizontalDivider()
                            Text("Bitrate Quality", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                bitrates.chunked(3).forEach { row ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        row.forEach { (br, label) ->
                                            FilterChip(
                                                selected = selectedBitrate == br,
                                                onClick = { selectedBitrate = br },
                                                label = { Text(label, fontSize = 11.sp) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider()
                        Text("Sample Rate", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            sampleRates.forEach { (sr, label) ->
                                FilterChip(
                                    selected = selectedSampleRate == sr,
                                    onClick = { selectedSampleRate = sr },
                                    label = { Text(label.substringBefore(" "), fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        HorizontalDivider()
                        Text("Channels", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            FilterChip(
                                selected = selectedChannels == 2,
                                onClick = { selectedChannels = 2 },
                                label = { Text("Stereo (2 Channels)") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedChannels == 1,
                                onClick = { selectedChannels = 1 },
                                label = { Text("Mono (1 Channel)") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Error Message if any
                if (errorMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(10.dp))
                            Text(errorMessage!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }

                // Convert Button / Progress
                if (isConverting) {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Converting Audio...", fontWeight = FontWeight.Bold)
                                Text("${(conversionProgress * 100).toInt()}%", fontWeight = FontWeight.Bold)
                            }
                            LinearProgressIndicator(
                                progress = conversionProgress,
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                            )
                            OutlinedButton(
                                onClick = { stopConversion() },
                                modifier = Modifier.align(Alignment.End),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Cancel")
                            }
                        }
                    }
                } else {
                    Button(
                        onClick = { startConversion() },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Transform, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Convert Audio Now", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Step 3: Success Card
            if (convertedFile != null) {
                val file = convertedFile!!
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
                                Text("Conversion Complete!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(file.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Output Size", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(AudioEngine.formatFileSize(file.length()), fontWeight = FontWeight.Bold)
                            }
                        }

                        // Actions
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { togglePlayback(file) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(if (isPlaying) "Pause" else "Play")
                            }

                            OutlinedButton(
                                onClick = {
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "audio/*"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Audio"))
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
                            Text("Export / Save to Device")
                        }

                        TextButton(
                            onClick = {
                                inputUriString = null
                                convertedFile = null
                                mediaPlayer?.release()
                                mediaPlayer = null
                                isPlaying = false
                            },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("Convert Another File")
                        }
                    }
                }
            }
        }
    }
}
