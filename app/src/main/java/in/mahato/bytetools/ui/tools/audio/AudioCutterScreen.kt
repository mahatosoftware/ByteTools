package `in`.mahato.bytetools.ui.tools.audio

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import kotlinx.coroutines.*
import java.io.File
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioCutterScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var inputUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var fileName by rememberSaveable { mutableStateOf("") }
    var totalDurationMs by rememberSaveable { mutableLongStateOf(0L) }

    var startMs by rememberSaveable { mutableLongStateOf(0L) }
    var endMs by rememberSaveable { mutableLongStateOf(0L) }
    var currentPlayMs by remember { mutableLongStateOf(0L) }

    val waveform = remember { mutableStateListOf<Float>() }
    var isExtractingWaveform by remember { mutableStateOf(false) }

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var isLoopingSelection by rememberSaveable { mutableStateOf(false) }

    var selectedFormat by rememberSaveable { mutableStateOf("m4a") } // "m4a", "wav", "mp3"
    var isTrimming by remember { mutableStateOf(false) }
    var trimProgress by remember { mutableFloatStateOf(0f) }
    var trimmedFile by remember { mutableStateOf<File?>(null) }
    var savedToHistory by rememberSaveable { mutableStateOf(false) }

    fun stopPlayback() {
        mediaPlayer?.pause()
        isPlaying = false
    }

    fun playSelection(fromStart: Boolean = false) {
        val uri = inputUriString?.let(Uri::parse) ?: return
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, uri)
                prepare()
            }
        }

        mediaPlayer?.let { player ->
            if (fromStart || player.currentPosition < startMs || player.currentPosition >= endMs) {
                player.seekTo(startMs.toInt())
            }
            player.start()
            isPlaying = true
        }
    }

    // Playback tracking loop
    LaunchedEffect(isPlaying) {
        while (isActive && isPlaying) {
            mediaPlayer?.let { player ->
                currentPlayMs = player.currentPosition.toLong()
                if (currentPlayMs >= endMs) {
                    if (isLoopingSelection) {
                        player.seekTo(startMs.toInt())
                        currentPlayMs = startMs
                    } else {
                        stopPlayback()
                        currentPlayMs = startMs
                    }
                }
            }
            delay(50)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("audio/*"),
        onResult = { destinationUri ->
            if (destinationUri != null && trimmedFile != null) {
                scope.launch(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                            trimmedFile!!.inputStream().use { it.copyTo(out) }
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Saved clip to device", Toast.LENGTH_SHORT).show()
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
                stopPlayback()
                mediaPlayer?.release()
                mediaPlayer = null

                inputUriString = uri.toString()
                fileName = AudioEngine.getFileName(context, uri)
                trimmedFile = null
                savedToHistory = false

                scope.launch {
                    isExtractingWaveform = true
                    val meta = AudioEngine.getAudioMetadata(context, uri)
                    totalDurationMs = meta.durationMs
                    startMs = 0L
                    endMs = if (meta.durationMs > 0) meta.durationMs else 30000L
                    currentPlayMs = 0L

                    waveform.clear()
                    val amps = AudioEngine.extractWaveformAmplitudes(context, uri, barCount = 70)
                    waveform.addAll(amps)
                    isExtractingWaveform = false
                }
            }
        }
    )

    fun startTrimming() {
        val uri = inputUriString?.let(Uri::parse) ?: return
        if (endMs <= startMs) {
            Toast.makeText(context, "End time must be greater than start time", Toast.LENGTH_SHORT).show()
            return
        }
        if (endMs - startMs < 500) {
            Toast.makeText(context, "Selected clip must be at least 0.5s long", Toast.LENGTH_SHORT).show()
            return
        }

        stopPlayback()
        isTrimming = true
        trimProgress = 0f

        scope.launch {
            try {
                val cleanBase = fileName.substringBeforeLast(".")
                val outFile = File(context.cacheDir, "${cleanBase}_trimmed.$selectedFormat")

                AudioEngine.trimAudio(
                    context = context,
                    inputUri = uri,
                    outputFormat = selectedFormat,
                    startMs = startMs,
                    endMs = endMs,
                    bitrate = 192000,
                    outFile = outFile,
                    onProgress = { trimProgress = it }
                )

                trimmedFile = outFile
                isTrimming = false
                Toast.makeText(context, "Audio trimmed successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                isTrimming = false
                Toast.makeText(context, "Trim failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audio Cutter & Trimmer", fontWeight = FontWeight.Bold) },
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
            // Select File Header
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
                                Icon(Icons.Default.ContentCut, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                            }
                            Spacer(Modifier.height(10.dp))
                            Text("Select Audio to Trim", fontWeight = FontWeight.Bold)
                            Text("MP3, WAV, M4A, AAC, OGG", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(fileName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Total Duration: ${AudioEngine.formatDuration(totalDurationMs)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { pickerLauncher.launch(arrayOf("audio/*")) }) {
                            Text("Change")
                        }
                    }
                }

                // Interactive Waveform & Trimmer Card
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Waveform & Trimmer Range", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                        // Waveform Canvas
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E1E2C))
                        ) {
                            if (isExtractingWaveform) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                }
                            } else {
                                InteractiveWaveformCanvas(
                                    waveform = waveform,
                                    durationMs = totalDurationMs,
                                    startMs = startMs,
                                    endMs = endMs,
                                    currentPlayMs = currentPlayMs,
                                    onSeek = { seekPosMs ->
                                        mediaPlayer?.seekTo(seekPosMs.toInt())
                                        currentPlayMs = seekPosMs
                                    }
                                )
                            }
                        }

                        // Range Sliders
                        RangeSlider(
                            value = (startMs.toFloat())..(endMs.toFloat()),
                            onValueChange = { range ->
                                startMs = range.start.toLong()
                                endMs = range.endInclusive.toLong()
                            },
                            valueRange = 0f..(max(1L, totalDurationMs).toFloat()),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Duration summary & precise timing display
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Start Time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(AudioEngine.formatDuration(startMs), fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Clip Duration", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Text(AudioEngine.formatDuration(max(0L, endMs - startMs)), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("End Time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(AudioEngine.formatDuration(endMs), fontWeight = FontWeight.Bold)
                            }
                        }

                        // Playback Controls Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { isLoopingSelection = !isLoopingSelection }) {
                                Icon(
                                    Icons.Default.Repeat,
                                    contentDescription = "Loop Selection",
                                    tint = if (isLoopingSelection) MaterialTheme.colorScheme.primary else Color.Gray
                                )
                            }

                            Spacer(Modifier.width(16.dp))

                            FilledIconButton(
                                onClick = {
                                    if (isPlaying) stopPlayback() else playSelection(fromStart = false)
                                },
                                modifier = Modifier.size(54.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            Spacer(Modifier.width(16.dp))

                            IconButton(onClick = { playSelection(fromStart = true) }) {
                                Icon(Icons.Default.Replay, contentDescription = "Play From Start", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                // Format & Export Selection
                if (trimmedFile == null) {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Output Format", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                listOf("m4a" to "M4A (AAC)", "wav" to "WAV", "mp3" to "MP3").forEach { (fmt, label) ->
                                    FilterChip(
                                        selected = selectedFormat == fmt,
                                        onClick = { selectedFormat = fmt },
                                        label = { Text(label, fontSize = 12.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    if (isTrimming) {
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Trimming Audio...", fontWeight = FontWeight.Bold)
                                LinearProgressIndicator(progress = trimProgress, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)))
                            }
                        }
                    } else {
                        Button(
                            onClick = { startTrimming() },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.ContentCut, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Trim & Save Clip", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Trimmed Result Card
            if (trimmedFile != null) {
                val file = trimmedFile!!
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
                                Text("Audio Trimmed Successfully!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(file.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Clip Duration", style = MaterialTheme.typography.bodySmall)
                                Text(AudioEngine.formatDuration(endMs - startMs), fontWeight = FontWeight.Bold)
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
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Clip"))
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

@Composable
fun InteractiveWaveformCanvas(
    waveform: List<Float>,
    durationMs: Long,
    startMs: Long,
    endMs: Long,
    currentPlayMs: Long,
    onSeek: (Long) -> Unit
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(durationMs) {
                detectTapGestures { offset ->
                    if (durationMs > 0) {
                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeek((fraction * durationMs).toLong())
                    }
                }
            }
    ) {
        val width = size.width
        val height = size.height

        if (waveform.isEmpty()) return@Canvas

        val maxDuration = max(1L, durationMs).toFloat()
        val startX = (startMs.toFloat() / maxDuration) * width
        val endX = (endMs.toFloat() / maxDuration) * width

        // Darkened overlay for excluded areas
        drawRect(Color.Black.copy(alpha = 0.5f), Offset(0f, 0f), Size(startX, height))
        drawRect(Color.Black.copy(alpha = 0.5f), Offset(endX, 0f), Size(width - endX, height))

        // Selected range background highlight
        drawRect(Color(0xFF2196F3).copy(alpha = 0.15f), Offset(startX, 0f), Size(endX - startX, height))

        // Draw waveform amplitude bars
        val barWidth = width / waveform.size
        waveform.forEachIndexed { index, amp ->
            val barX = index * barWidth + barWidth * 0.15f
            val barH = amp * (height * 0.75f)
            val barY = (height - barH) / 2f
            val inRange = barX in startX..endX

            drawRoundRect(
                color = if (inRange) Color(0xFF64B5F6) else Color.Gray.copy(alpha = 0.4f),
                topLeft = Offset(barX, barY),
                size = Size(barWidth * 0.7f, barH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )
        }

        // Draw Start Marker line
        drawLine(
            color = Color(0xFF4CAF50),
            start = Offset(startX, 0f),
            end = Offset(startX, height),
            strokeWidth = 3.dp.toPx()
        )

        // Draw End Marker line
        drawLine(
            color = Color(0xFFF44336),
            start = Offset(endX, 0f),
            end = Offset(endX, height),
            strokeWidth = 3.dp.toPx()
        )

        // Draw Current Playback needle
        if (currentPlayMs in startMs..endMs) {
            val playX = (currentPlayMs.toFloat() / maxDuration) * width
            drawLine(
                color = Color.White,
                start = Offset(playX, 0f),
                end = Offset(playX, height),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}
