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
fun VideoToAudioScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var videoUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var fileName by rememberSaveable { mutableStateOf("") }
    var fileSize by rememberSaveable { mutableLongStateOf(0L) }
    var audioMetadata by remember { mutableStateOf<AudioMetadata?>(null) }

    var selectedFormat by rememberSaveable { mutableStateOf("m4a") } // "m4a", "mp3", "wav"
    var selectedBitrate by rememberSaveable { mutableIntStateOf(192000) }

    var isExtracting by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var extractionJob by remember { mutableStateOf<Job?>(null) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    var extractedFile by remember { mutableStateOf<File?>(null) }
    var savedToHistory by rememberSaveable { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }

    val bitrates = listOf(
        128000 to "128 kbps (Standard)",
        192000 to "192 kbps (High Quality)",
        256000 to "256 kbps (Very High)",
        320000 to "320 kbps (Max)"
    )

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("audio/*"),
        onResult = { destinationUri ->
            if (destinationUri != null && extractedFile != null) {
                scope.launch(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                            extractedFile!!.inputStream().use { it.copyTo(out) }
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Saved audio to device successfully", Toast.LENGTH_SHORT).show()
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

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                videoUriString = uri.toString()
                fileName = AudioEngine.getFileName(context, uri)
                extractedFile = null
                errorMessage = null
                savedToHistory = false
                mediaPlayer?.release()
                mediaPlayer = null
                isPlaying = false

                scope.launch(Dispatchers.IO) {
                    val size = context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L
                    val meta = AudioEngine.getAudioMetadata(context, uri)
                    withContext(Dispatchers.Main) {
                        fileSize = size
                        audioMetadata = meta
                    }
                }
            }
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
        }
    }

    fun startExtraction() {
        val uri = videoUriString?.let(Uri::parse) ?: return
        isExtracting = true
        progress = 0f
        errorMessage = null
        extractedFile = null

        extractionJob = scope.launch {
            try {
                val cleanBase = fileName.substringBeforeLast(".")
                val ext = when (selectedFormat.lowercase()) {
                    "wav" -> "wav"
                    "mp3" -> "mp3"
                    else -> "m4a"
                }
                val outFile = File(context.cacheDir, "${cleanBase}_audio.$ext")

                AudioEngine.extractAudioFromVideo(
                    context = context,
                    videoUri = uri,
                    outputFormat = selectedFormat,
                    bitrate = selectedBitrate,
                    outFile = outFile,
                    onProgress = { p -> progress = p }
                )

                if (isActive && outFile.exists() && outFile.length() > 0) {
                    extractedFile = outFile
                    isExtracting = false
                    Toast.makeText(context, "Audio extracted successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    throw IllegalStateException("Audio extraction output is empty.")
                }
            } catch (e: CancellationException) {
                isExtracting = false
                Toast.makeText(context, "Extraction cancelled", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                isExtracting = false
                errorMessage = e.localizedMessage ?: "Failed to extract audio track. Ensure the video contains a supported audio stream."
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Video to MP3", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (videoUriString != null) {
                        IconButton(onClick = { videoPickerLauncher.launch(arrayOf("video/*")) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Change Video")
                        }
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
            // Privacy Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "100% Offline & Private: Extraction is processed on-device. Your video is never uploaded or modified.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Video Picker / Video Details Card
            if (videoUriString == null) {
                Card(
                    onClick = { videoPickerLauncher.launch(arrayOf("video/*")) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.VideoFile,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Select Video File",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Supports MP4, MKV, WebM, 3GP videos",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFFFF3E0)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.VideoFile, contentDescription = null, tint = Color(0xFFFF9800))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    fileName,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = buildString {
                                        if (fileSize > 0) append(AudioEngine.formatFileSize(fileSize))
                                        val dur = audioMetadata?.durationMs ?: 0L
                                        if (dur > 0) {
                                            if (isNotEmpty()) append(" • ")
                                            append(AudioEngine.formatDuration(dur))
                                        }
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(
                                onClick = { videoPickerLauncher.launch(arrayOf("video/*")) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Change", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        audioMetadata?.let { meta ->
                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Audio Codec: ${meta.mimeType.substringAfterLast("/")}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${meta.sampleRate} Hz • ${if (meta.channelCount == 1) "Mono" else "Stereo"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Format & Quality Selection
                Text(
                    "Extraction Options",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Format selection
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Output Format", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "m4a" to "M4A (AAC)",
                            "mp3" to "MP3",
                            "wav" to "WAV (Lossless)"
                        ).forEach { (fmt, label) ->
                            FilterChip(
                                selected = selectedFormat == fmt,
                                onClick = { selectedFormat = fmt },
                                label = { Text(label) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Bitrate Selection (not needed for uncompressed WAV)
                if (selectedFormat != "wav") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Audio Quality / Bitrate", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            bitrates.forEach { (br, label) ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    RadioButton(
                                        selected = selectedBitrate == br,
                                        onClick = { selectedBitrate = br }
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(label, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }

                // Extract Button / Progress Bar
                if (isExtracting) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Extracting audio...", fontWeight = FontWeight.SemiBold)
                                Text("${(progress * 100).toInt()}%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            LinearProgressIndicator(
                                progress = progress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { extractionJob?.cancel() }) {
                                    Text("Cancel", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                } else {
                    Button(
                        onClick = { startExtraction() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Extract Audio Track", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            // Error display
            errorMessage?.let { err ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text(err, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Extraction Result Card
            extractedFile?.let { file ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4CAF50)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Extraction Complete!", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("${file.name} • ${AudioEngine.formatFileSize(file.length())}", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        // Playback Preview
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (isPlaying) {
                                        mediaPlayer?.pause()
                                        isPlaying = false
                                    } else {
                                        if (mediaPlayer == null) {
                                            mediaPlayer = MediaPlayer().apply {
                                                setDataSource(file.absolutePath)
                                                prepare()
                                                setOnCompletionListener { isPlaying = false }
                                            }
                                        }
                                        mediaPlayer?.start()
                                        isPlaying = true
                                    }
                                }
                            ) {
                                Icon(
                                    if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                            Text(
                                if (isPlaying) "Playing preview..." else "Listen to extracted audio",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Actions Row: Save to History, Share, Export to Device
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val saved = AudioEngine.saveAudioToHistory(context, file)
                                    if (saved != null) {
                                        savedToHistory = true
                                        Toast.makeText(context, "Saved to App Audio History", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f),
                                enabled = !savedToHistory
                            ) {
                                Icon(
                                    if (savedToHistory) Icons.Default.Check else Icons.Default.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(if (savedToHistory) "Saved" else "History", maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = {
                                    try {
                                        val shareUri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.provider",
                                            file
                                        )
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "audio/*"
                                            putExtra(Intent.EXTRA_STREAM, shareUri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Extracted Audio"))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Share error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Share", maxLines = 1)
                            }

                            Button(
                                onClick = {
                                    val ext = file.extension
                                    exportLauncher.launch("${file.nameWithoutExtension}.$ext")
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Save As", maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}
