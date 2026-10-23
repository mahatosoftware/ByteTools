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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

data class AudioTrackItem(
    val uri: Uri,
    val name: String,
    val durationMs: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioMergerScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val trackList = remember { mutableStateListOf<AudioTrackItem>() }
    var selectedFormat by rememberSaveable { mutableStateOf("m4a") }
    var normalizeVolume by rememberSaveable { mutableStateOf(false) }

    var isMerging by remember { mutableStateOf(false) }
    var mergeProgress by remember { mutableFloatStateOf(0f) }
    var mergedFile by remember { mutableStateOf<File?>(null) }
    var savedToHistory by rememberSaveable { mutableStateOf(false) }

    var activePlayingIndex by remember { mutableIntStateOf(-1) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    fun stopPlayback() {
        mediaPlayer?.pause()
        mediaPlayer?.release()
        mediaPlayer = null
        activePlayingIndex = -1
    }

    fun playTrack(index: Int) {
        if (activePlayingIndex == index) {
            stopPlayback()
            return
        }
        stopPlayback()
        if (index in trackList.indices) {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, trackList[index].uri)
                prepare()
                setOnCompletionListener {
                    stopPlayback()
                }
                start()
            }
            activePlayingIndex = index
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            stopPlayback()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("audio/*"),
        onResult = { destinationUri ->
            if (destinationUri != null && mergedFile != null) {
                scope.launch(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                            mergedFile!!.inputStream().use { it.copyTo(out) }
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Saved merged audio to device", Toast.LENGTH_SHORT).show()
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
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                scope.launch(Dispatchers.IO) {
                    val newTracks = uris.map { uri ->
                        val name = AudioEngine.getFileName(context, uri)
                        val meta = AudioEngine.getAudioMetadata(context, uri)
                        AudioTrackItem(uri, name, meta.durationMs)
                    }
                    withContext(Dispatchers.Main) {
                        trackList.addAll(newTracks)
                        mergedFile = null
                    }
                }
            }
        }
    )

    fun startMerging() {
        if (trackList.size < 2) {
            Toast.makeText(context, "Please select at least 2 tracks to merge", Toast.LENGTH_SHORT).show()
            return
        }
        stopPlayback()
        isMerging = true
        mergeProgress = 0f

        scope.launch {
            try {
                val outFile = File(context.cacheDir, "Merged_${System.currentTimeMillis()}.$selectedFormat")
                AudioEngine.mergeAudioFiles(
                    context = context,
                    uris = trackList.map { it.uri },
                    outputFormat = selectedFormat,
                    normalizeVolume = normalizeVolume,
                    bitrate = 192000,
                    outFile = outFile,
                    onProgress = { mergeProgress = it }
                )
                mergedFile = outFile
                isMerging = false
                Toast.makeText(context, "Audio merged successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                isMerging = false
                Toast.makeText(context, "Merge failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val totalEstimatedDurationMs = trackList.sumOf { it.durationMs }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audio Merger & Joiner", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        stopPlayback()
                        navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { pickerLauncher.launch(arrayOf("audio/*")) }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Tracks")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Add Tracks Banner / Summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(46.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Merge, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${trackList.size} Tracks Added", fontWeight = FontWeight.Bold)
                        Text(
                            "Total Duration: ${AudioEngine.formatDuration(totalEstimatedDurationMs)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(onClick = { pickerLauncher.launch(arrayOf("audio/*")) }) {
                        Text("Add")
                    }
                }
            }

            // Track List
            Text("Track Sequence (Reorder below)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (trackList.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.PlaylistAdd, contentDescription = null, modifier = Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(10.dp))
                                Text("No Audio Files Selected", fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Tap 'Add' to choose audio files to join together into one single track.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    itemsIndexed(trackList) { index, track ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (activePlayingIndex == index) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { playTrack(index) }) {
                                    Icon(
                                        imageVector = if (activePlayingIndex == index) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                        contentDescription = "Preview",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${index + 1}. ${track.name}",
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(AudioEngine.formatDuration(track.durationMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                // Move Up
                                IconButton(
                                    onClick = {
                                        if (index > 0) {
                                            val item = trackList.removeAt(index)
                                            trackList.add(index - 1, item)
                                        }
                                    },
                                    enabled = index > 0
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(18.dp))
                                }

                                // Move Down
                                IconButton(
                                    onClick = {
                                        if (index < trackList.size - 1) {
                                            val item = trackList.removeAt(index)
                                            trackList.add(index + 1, item)
                                        }
                                    },
                                    enabled = index < trackList.size - 1
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(18.dp))
                                }

                                // Delete
                                IconButton(onClick = {
                                    if (activePlayingIndex == index) stopPlayback()
                                    trackList.removeAt(index)
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Output Format & Merge Button
            if (trackList.size >= 2 && mergedFile == null) {
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

                if (isMerging) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Joining tracks...", fontWeight = FontWeight.Bold)
                            Text("${(mergeProgress * 100).toInt()}%", fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(progress = mergeProgress, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)))
                    }
                } else {
                    Button(
                        onClick = { startMerging() },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Merge, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Join ${trackList.size} Files Into One", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Success Result Card
            if (mergedFile != null) {
                val file = mergedFile!!
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Joined File Ready!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${file.name} • ${AudioEngine.formatFileSize(file.length())}", style = MaterialTheme.typography.bodySmall)

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = {
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "audio/*"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Joined Audio"))
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
