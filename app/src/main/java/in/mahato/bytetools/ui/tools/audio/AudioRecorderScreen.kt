package `in`.mahato.bytetools.ui.tools.audio

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

enum class RecordingState {
    IDLE,
    RECORDING,
    PAUSED,
    FINISHED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioRecorderScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    var recordingState by remember { mutableStateOf(RecordingState.IDLE) }
    var currentRecordDurationMs by remember { mutableLongStateOf(0L) }
    var currentFileSize by remember { mutableLongStateOf(0L) }
    var tempAudioFile by remember { mutableStateOf<File?>(null) }

    // Live amplitude bars for dynamic visualizer
    val amplitudeHistory = remember { mutableStateListOf<Float>() }

    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPreviewPlaying by remember { mutableStateOf(false) }
    var previewPlayMs by remember { mutableLongStateOf(0L) }

    var editableFileName by rememberSaveable { mutableStateOf("") }
    var savedToHistory by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasMicPermission = granted
            if (!granted) {
                Toast.makeText(context, "Microphone permission is required to record audio.", Toast.LENGTH_SHORT).show()
            }
        }
    )

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("audio/*"),
        onResult = { destinationUri ->
            if (destinationUri != null && tempAudioFile != null) {
                scope.launch(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                            tempAudioFile!!.inputStream().use { it.copyTo(out) }
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Recording saved to device", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Error saving: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    )

    fun releaseRecorder() {
        try {
            mediaRecorder?.stop()
        } catch (_: Exception) {}
        try {
            mediaRecorder?.release()
        } catch (_: Exception) {}
        mediaRecorder = null
    }

    fun releasePlayer() {
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {}
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        isPreviewPlaying = false
    }

    // Lifecycle cleanup
    DisposableEffect(Unit) {
        onDispose {
            releaseRecorder()
            releasePlayer()
        }
    }

    // Recording duration & amplitude tracking loop
    LaunchedEffect(recordingState) {
        if (recordingState == RecordingState.RECORDING) {
            val startTime = System.currentTimeMillis() - currentRecordDurationMs
            while (isActive && recordingState == RecordingState.RECORDING) {
                currentRecordDurationMs = System.currentTimeMillis() - startTime
                tempAudioFile?.let { file ->
                    if (file.exists()) {
                        currentFileSize = file.length()
                    }
                }
                // Read amplitude
                val amp = try {
                    mediaRecorder?.maxAmplitude ?: 0
                } catch (_: Exception) {
                    0
                }
                val normalizedAmp = (amp / 32767f).coerceIn(0.08f, 1f)
                if (amplitudeHistory.size >= 32) {
                    amplitudeHistory.removeAt(0)
                }
                amplitudeHistory.add(normalizedAmp)

                delay(100)
            }
        }
    }

    // Playback tracking loop
    LaunchedEffect(isPreviewPlaying) {
        while (isActive && isPreviewPlaying) {
            mediaPlayer?.let { player ->
                previewPlayMs = player.currentPosition.toLong()
            }
            delay(50)
        }
    }

    fun startRecording() {
        if (!hasMicPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        // Low-storage check: Ensure at least 15MB available
        val freeBytes = context.cacheDir.usableSpace
        if (freeBytes < 15 * 1024 * 1024) {
            Toast.makeText(context, "Low storage! Free up space on device before recording.", Toast.LENGTH_LONG).show()
            return
        }

        releaseRecorder()
        releasePlayer()

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        editableFileName = "Voice_$timeStamp"
        val outFile = File(context.cacheDir, "${editableFileName}.m4a")
        tempAudioFile = outFile
        currentRecordDurationMs = 0L
        currentFileSize = 0L
        amplitudeHistory.clear()

        try {
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(outFile.absolutePath)
                setOnErrorListener { _, _, _ ->
                    releaseRecorder()
                    recordingState = RecordingState.IDLE
                    Toast.makeText(context, "Recording error occurred", Toast.LENGTH_SHORT).show()
                }
                prepare()
                start()
            }
            mediaRecorder = recorder
            recordingState = RecordingState.RECORDING
        } catch (e: Exception) {
            releaseRecorder()
            recordingState = RecordingState.IDLE
            Toast.makeText(context, "Unable to start recording: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun pauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && recordingState == RecordingState.RECORDING) {
            try {
                mediaRecorder?.pause()
                recordingState = RecordingState.PAUSED
            } catch (e: Exception) {
                Toast.makeText(context, "Pause failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun resumeRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && recordingState == RecordingState.PAUSED) {
            try {
                mediaRecorder?.resume()
                recordingState = RecordingState.RECORDING
            } catch (e: Exception) {
                Toast.makeText(context, "Resume failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun stopRecording() {
        releaseRecorder()
        recordingState = RecordingState.FINISHED
        savedToHistory = false
        tempAudioFile?.let { file ->
            if (file.exists()) {
                currentFileSize = file.length()
            }
        }
    }

    fun togglePlayback() {
        val file = tempAudioFile ?: return
        if (isPreviewPlaying) {
            mediaPlayer?.pause()
            isPreviewPlaying = false
        } else {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(file.absolutePath)
                    prepare()
                    setOnCompletionListener {
                        isPreviewPlaying = false
                        previewPlayMs = 0L
                    }
                }
            }
            mediaPlayer?.start()
            isPreviewPlaying = true
        }
    }

    // Pulsing circle animation during recording
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audio Recorder", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        releaseRecorder()
                        releasePlayer()
                        navController.popBackStack()
                    }) {
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Permission Banner (if not granted)
            if (!hasMicPermission) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MicOff, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(10.dp))
                            Text("Microphone Permission Required", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                        Text(
                            "ByteTools needs access to your microphone to record audio notes. Tap below to grant permission.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Grant Permission")
                        }
                    }
                }
            }

            // Duration and Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (recordingState) {
                        RecordingState.RECORDING -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        RecordingState.PAUSED -> MaterialTheme.colorScheme.surfaceVariant
                        RecordingState.FINISHED -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                        RecordingState.IDLE -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    }
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp, horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Recording Status Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val badgeColor = when (recordingState) {
                            RecordingState.RECORDING -> Color(0xFFE53935)
                            RecordingState.PAUSED -> Color(0xFFFB8C00)
                            RecordingState.FINISHED -> Color(0xFF43A047)
                            RecordingState.IDLE -> Color.Gray
                        }
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(badgeColor)
                        )
                        Text(
                            text = when (recordingState) {
                                RecordingState.RECORDING -> "RECORDING"
                                RecordingState.PAUSED -> "PAUSED"
                                RecordingState.FINISHED -> "RECORDING READY"
                                RecordingState.IDLE -> "READY TO RECORD"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }

                    // Large Timer Display
                    Text(
                        text = AudioEngine.formatDuration(currentRecordDurationMs),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // File size & format
                    Text(
                        text = "AAC (M4A) • 44.1 kHz • ${AudioEngine.formatFileSize(currentFileSize)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Real-time Audio Amplitude Waveform Visualizer
                    if (recordingState == RecordingState.RECORDING || recordingState == RecordingState.PAUSED) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF181824)),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp)) {
                                val barCount = max(amplitudeHistory.size, 1)
                                val barWidth = size.width / 32f
                                amplitudeHistory.forEachIndexed { index, amp ->
                                    val barH = (amp * size.height).coerceAtLeast(4.dp.toPx())
                                    val x = index * barWidth + barWidth * 0.2f
                                    val y = (size.height - barH) / 2f
                                    drawRoundRect(
                                        color = if (recordingState == RecordingState.RECORDING) Color(0xFF4CAF50) else Color(0xFFFFB300),
                                        topLeft = Offset(x, y),
                                        size = Size(barWidth * 0.6f, barH),
                                        cornerRadius = CornerRadius(4f, 4f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Recorder Action Controls
            when (recordingState) {
                RecordingState.IDLE -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Big Record Button
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            FilledIconButton(
                                onClick = { startRecording() },
                                modifier = Modifier.size(76.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = "Start Recording", modifier = Modifier.size(40.dp))
                            }
                        }
                        Text("Tap to Start Recording", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "High-fidelity voice & sound recording saved directly to your device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }

                RecordingState.RECORDING, RecordingState.PAUSED -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pause / Resume Button
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            OutlinedIconButton(
                                onClick = {
                                    if (recordingState == RecordingState.RECORDING) pauseRecording() else resumeRecording()
                                },
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(
                                    imageVector = if (recordingState == RecordingState.RECORDING) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (recordingState == RecordingState.RECORDING) "Pause" else "Resume",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        // Stop & Finish Button
                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .scale(if (recordingState == RecordingState.RECORDING) pulseScale else 1f)
                                .clip(CircleShape)
                                .background(Color(0xFFFFEBEE)),
                            contentAlignment = Alignment.Center
                        ) {
                            FilledIconButton(
                                onClick = { stopRecording() },
                                modifier = Modifier.size(68.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFE53935))
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = "Stop Recording", tint = Color.White, modifier = Modifier.size(36.dp))
                            }
                        }
                    }
                }

                RecordingState.FINISHED -> {
                    // Result & Review Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text("Recording Saved & Ready", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                            // Editable File Name
                            OutlinedTextField(
                                value = editableFileName,
                                onValueChange = { editableFileName = it },
                                label = { Text("Recording Name") },
                                trailingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            // Playback Preview Controls
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { togglePlayback() }) {
                                        Icon(
                                            imageVector = if (isPreviewPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                            contentDescription = if (isPreviewPlaying) "Pause" else "Play",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(42.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            if (isPreviewPlaying) "Playing recording..." else "Listen to preview",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        LinearProgressIndicator(
                                            progress = if (currentRecordDurationMs > 0) (previewPlayMs.toFloat() / currentRecordDurationMs).coerceIn(0f, 1f) else 0f,
                                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                                        )
                                    }
                                }
                            }

                            // Actions: Save to History, Share, Export to Device
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val file = tempAudioFile ?: return@OutlinedButton
                                        val renamedFile = File(context.cacheDir, "${editableFileName.trim()}.m4a")
                                        if (renamedFile != file) {
                                            file.copyTo(renamedFile, overwrite = true)
                                        }
                                        val saved = AudioEngine.saveAudioToHistory(context, renamedFile)
                                        if (saved != null) {
                                            savedToHistory = true
                                            Toast.makeText(context, "Saved to Audio History", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
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
                                        val file = tempAudioFile ?: return@OutlinedButton
                                        try {
                                            val shareUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "audio/*"
                                                putExtra(Intent.EXTRA_STREAM, shareUri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share Recording"))
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Share error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Share", maxLines = 1)
                                }

                                Button(
                                    onClick = {
                                        val cleanName = editableFileName.ifBlank { "Voice_Recording" }
                                        exportLauncher.launch("${cleanName}.m4a")
                                    },
                                    modifier = Modifier.weight(1.2f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Export", maxLines = 1)
                                }
                            }

                            // Record Another Button
                            TextButton(
                                onClick = {
                                    releasePlayer()
                                    recordingState = RecordingState.IDLE
                                    currentRecordDurationMs = 0L
                                    currentFileSize = 0L
                                    tempAudioFile = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Record Another Clip")
                            }
                        }
                    }
                }
            }
        }
    }
}
