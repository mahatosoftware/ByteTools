package `in`.mahato.bytetools.ui.tools.audio

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RingtoneMakerScreen(navController: NavController) {
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
    var isLoopingSelection by rememberSaveable { mutableStateOf(true) }

    var selectedFormat by rememberSaveable { mutableStateOf("m4a") }
    var isTrimming by remember { mutableStateOf(false) }
    var trimProgress by remember { mutableFloatStateOf(0f) }
    var ringtoneFile by remember { mutableStateOf<File?>(null) }
    var savedRingtoneUri by remember { mutableStateOf<Uri?>(null) }

    var showPermissionDialog by remember { mutableStateOf(false) }
    var showManualInstructionsDialog by remember { mutableStateOf(false) }

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

    // Playback loop tracking
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
            if (destinationUri != null && ringtoneFile != null) {
                scope.launch(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                            ringtoneFile!!.inputStream().use { it.copyTo(out) }
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Saved ringtone to device", Toast.LENGTH_SHORT).show()
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
                ringtoneFile = null
                savedRingtoneUri = null

                scope.launch {
                    isExtractingWaveform = true
                    val meta = AudioEngine.getAudioMetadata(context, uri)
                    totalDurationMs = meta.durationMs
                    startMs = 0L
                    endMs = if (meta.durationMs > 0) min(meta.durationMs, 30000L) else 30000L
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
        if (endMs - startMs < 1000) {
            Toast.makeText(context, "Ringtone must be at least 1 second long", Toast.LENGTH_SHORT).show()
            return
        }

        stopPlayback()
        isTrimming = true
        trimProgress = 0f

        scope.launch {
            try {
                val cleanBase = fileName.substringBeforeLast(".")
                val outFile = File(context.cacheDir, "${cleanBase}_ringtone.$selectedFormat")

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

                ringtoneFile = outFile
                isTrimming = false
                Toast.makeText(context, "Ringtone created successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                isTrimming = false
                Toast.makeText(context, "Creation failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun applyAsRingtone(file: File) {
        // Ensure file is in MediaStore Ringtones folder first
        var rUri = savedRingtoneUri
        if (rUri == null) {
            rUri = AudioEngine.saveToRingtonesFolder(context, file, file.nameWithoutExtension)
            savedRingtoneUri = rUri
        }

        if (rUri == null) {
            Toast.makeText(context, "Could not save ringtone to system folder.", Toast.LENGTH_SHORT).show()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(context)) {
            showPermissionDialog = true
            return
        }

        val success = AudioEngine.setAsDefaultRingtone(context, rUri)
        if (success) {
            Toast.makeText(context, "Default ringtone set successfully! 🔔", Toast.LENGTH_LONG).show()
        } else {
            showManualInstructionsDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ringtone Maker", fontWeight = FontWeight.Bold) },
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
            // Header / Intro Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Trim any song or sound clip to create custom phone ringtones, alarms, and notification sounds.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Audio Picker / Audio Info
            if (inputUriString == null) {
                Card(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    onClick = { pickerLauncher.launch(arrayOf("audio/*")) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                        }
                        Spacer(Modifier.height(10.dp))
                        Text("Select Audio File", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("MP3, M4A, WAV, AAC, OGG", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
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
                            Text("Duration: ${AudioEngine.formatDuration(totalDurationMs)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { pickerLauncher.launch(arrayOf("audio/*")) }) {
                            Text("Change")
                        }
                    }
                }

                // Waveform & Trimmer Card
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Ringtone Range", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            // 30s preset recommendation chip
                            FilterChip(
                                selected = false,
                                onClick = {
                                    startMs = 0L
                                    endMs = min(totalDurationMs, 30000L)
                                },
                                label = { Text("Standard 30s", fontSize = 11.sp) }
                            )
                        }

                        // Waveform View
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1A1A24))
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

                        // Range Slider
                        RangeSlider(
                            value = (startMs.toFloat())..(endMs.toFloat()),
                            onValueChange = { range ->
                                startMs = range.start.toLong()
                                endMs = range.endInclusive.toLong()
                            },
                            valueRange = 0f..(max(1L, totalDurationMs).toFloat()),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Duration indicators
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Start", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(AudioEngine.formatDuration(startMs), fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Ringtone Length", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                val clipLen = max(0L, endMs - startMs)
                                Text(
                                    AudioEngine.formatDuration(clipLen),
                                    fontWeight = FontWeight.Bold,
                                    color = if (clipLen > 40000L) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("End", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                    contentDescription = "Loop Ringtone Preview",
                                    tint = if (isLoopingSelection) MaterialTheme.colorScheme.primary else Color.Gray
                                )
                            }

                            Spacer(Modifier.width(16.dp))

                            FilledIconButton(
                                onClick = {
                                    if (isPlaying) stopPlayback() else playSelection(fromStart = false)
                                },
                                modifier = Modifier.size(52.dp)
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

                // Format & Create Button
                if (ringtoneFile == null) {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Output Format", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                listOf("m4a" to "M4A (High Quality)", "mp3" to "MP3", "wav" to "WAV").forEach { (fmt, label) ->
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
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Creating ringtone...", fontWeight = FontWeight.Bold)
                                LinearProgressIndicator(progress = trimProgress, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)))
                            }
                        }
                    } else {
                        Button(
                            onClick = { startTrimming() },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Trim & Create Ringtone", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Ringtone Ready Card
            ringtoneFile?.let { file ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF4CAF50)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Ringtone Created!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("${file.name} • ${AudioEngine.formatFileSize(file.length())}", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        // Big Action: Set As Default Ringtone
                        Button(
                            onClick = { applyAsRingtone(file) },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.RingVolume, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Set as Phone Ringtone", fontWeight = FontWeight.Bold)
                        }

                        // Secondary actions
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = {
                                    val rUri = AudioEngine.saveToRingtonesFolder(context, file, file.nameWithoutExtension)
                                    if (rUri != null) {
                                        savedRingtoneUri = rUri
                                        showManualInstructionsDialog = true
                                    } else {
                                        Toast.makeText(context, "Could not save to Ringtones folder", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Save Tone", maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = {
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "audio/*"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Ringtone"))
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Share", maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = { exportLauncher.launch(file.name) },
                                modifier = Modifier.weight(1.1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Export", maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }

    // Permission Dialog for WRITE_SETTINGS
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            icon = { Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Permission Required") },
            text = {
                Text(
                    "To set your phone ringtone automatically, Android requires the 'Modify system settings' permission.\n\n" +
                    "Tap 'Allow in Settings' to enable it, or choose 'Set Manually' to select it from your phone's sound settings."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDialog = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                                data = Uri.parse("package:" + context.packageName)
                            }
                            context.startActivity(intent)
                        }
                    }
                ) {
                    Text("Allow in Settings")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPermissionDialog = false
                        showManualInstructionsDialog = true
                    }
                ) {
                    Text("Set Manually")
                }
            }
        )
    }

    // Manual Instructions Dialog
    if (showManualInstructionsDialog) {
        AlertDialog(
            onDismissRequest = { showManualInstructionsDialog = false },
            icon = { Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Set Ringtone Manually") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Your ringtone has been added to the system Ringtones list:")
                    Text("1. Open your phone's **Settings** app.", fontWeight = FontWeight.Medium)
                    Text("2. Navigate to **Sound & vibration** -> **Phone ringtone**.", fontWeight = FontWeight.Medium)
                    Text("3. Look for **${ringtoneFile?.nameWithoutExtension ?: "your ringtone"}** and select it.", fontWeight = FontWeight.Medium)
                }
            },
            confirmButton = {
                Button(onClick = { showManualInstructionsDialog = false }) {
                    Text("Got It")
                }
            }
        )
    }
}
