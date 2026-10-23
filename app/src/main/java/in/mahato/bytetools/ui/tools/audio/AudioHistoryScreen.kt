package `in`.mahato.bytetools.ui.tools.audio

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class AudioHistoryFileItem(
    val file: File,
    val durationMs: Long = 0L,
    val formattedDate: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioHistoryScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var audioItems by remember { mutableStateOf(listOf<AudioHistoryFileItem>()) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedPaths by remember { mutableStateOf(setOf<String>()) }
    val selectionMode = selectedPaths.isNotEmpty()

    // Audio Playback Preview State
    var activePlayingPath by remember { mutableStateOf<String?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var playProgressMs by remember { mutableLongStateOf(0L) }

    // Dialog States
    var fileToDelete by remember { mutableStateOf<File?>(null) }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }
    var fileToExport by remember { mutableStateOf<File?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("audio/*"),
        onResult = { destinationUri ->
            val src = fileToExport
            if (destinationUri != null && src != null) {
                scope.launch(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                            src.inputStream().use { it.copyTo(out) }
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Exported successfully", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Export error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    )

    fun stopPlayback() {
        try {
            mediaPlayer?.pause()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        isPlaying = false
        activePlayingPath = null
        playProgressMs = 0L
    }

    fun playAudio(file: File) {
        if (activePlayingPath == file.absolutePath) {
            if (isPlaying) {
                mediaPlayer?.pause()
                isPlaying = false
            } else {
                mediaPlayer?.start()
                isPlaying = true
            }
            return
        }

        stopPlayback()
        try {
            val player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener {
                    isPlaying = false
                    playProgressMs = 0L
                }
                start()
            }
            mediaPlayer = player
            activePlayingPath = file.absolutePath
            isPlaying = true
        } catch (e: Exception) {
            Toast.makeText(context, "Could not play file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    // Playback progress ticker loop
    LaunchedEffect(isPlaying, activePlayingPath) {
        while (isActive && isPlaying) {
            mediaPlayer?.let { player ->
                playProgressMs = player.currentPosition.toLong()
            }
            delay(100)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            stopPlayback()
        }
    }

    fun refreshFiles() {
        scope.launch(Dispatchers.IO) {
            val dir = File(context.getExternalFilesDir(null), "ByteToolsAudio")
            val supportedExts = listOf("mp3", "m4a", "wav", "aac", "ogg", "flac")
            val files = if (dir.exists()) {
                dir.listFiles { f -> f.isFile && f.extension.lowercase() in supportedExts }
                    ?.sortedByDescending { it.lastModified() } ?: emptyList()
            } else {
                emptyList()
            }

            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            val items = files.map { file ->
                val meta = AudioEngine.getAudioMetadata(context, Uri.fromFile(file))
                AudioHistoryFileItem(
                    file = file,
                    durationMs = meta.durationMs,
                    formattedDate = sdf.format(Date(file.lastModified()))
                )
            }

            withContext(Dispatchers.Main) {
                audioItems = items
                selectedPaths = selectedPaths.filter { path -> items.any { it.file.absolutePath == path } }.toSet()
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshFiles()
    }

    fun shareFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Audio"))
        } catch (e: Exception) {
            Toast.makeText(context, "Share error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareMultipleFiles(files: List<File>) {
        try {
            val uris = files.map { file ->
                FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            }
            val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "audio/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Audio Files"))
        } catch (e: Exception) {
            Toast.makeText(context, "Share error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    val filteredItems = remember(audioItems, searchQuery) {
        if (searchQuery.isBlank()) {
            audioItems
        } else {
            audioItems.filter { it.file.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    val totalBytes = remember(audioItems) {
        audioItems.sumOf { it.file.length() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (selectionMode) "${selectedPaths.size} selected" else "Audio History",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        stopPlayback()
                        navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (selectionMode) {
                        IconButton(
                            onClick = {
                                val selectedFiles = audioItems.filter { it.file.absolutePath in selectedPaths }.map { it.file }
                                if (selectedFiles.size == 1) {
                                    shareFile(selectedFiles.first())
                                } else if (selectedFiles.isNotEmpty()) {
                                    shareMultipleFiles(selectedFiles)
                                }
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share Selected")
                        }
                        IconButton(
                            onClick = { showBatchDeleteDialog = true }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Selected", tint = MaterialTheme.colorScheme.error)
                        }
                        IconButton(onClick = { selectedPaths = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Selection")
                        }
                    } else {
                        IconButton(onClick = { refreshFiles() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search & Storage Summary Bar
            if (audioItems.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search saved audio files...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(26.dp)),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${audioItems.size} audio files saved",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Storage: ${AudioEngine.formatFileSize(totalBytes)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Audio Files List or Empty State
            if (audioItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.MusicNote,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary,
                                contentDescription = null
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No Saved Audio Files",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Converted, trimmed, merged, compressed, or recorded audio files saved to history will appear here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No files match \"$searchQuery\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredItems, key = { it.file.absolutePath }) { item ->
                        val isCurrentPlaying = activePlayingPath == item.file.absolutePath
                        AudioHistoryCard(
                            item = item,
                            isSelected = item.file.absolutePath in selectedPaths,
                            selectionMode = selectionMode,
                            isPlaying = isCurrentPlaying && isPlaying,
                            playProgressMs = if (isCurrentPlaying) playProgressMs else 0L,
                            onPlayToggle = { playAudio(item.file) },
                            onShare = { shareFile(item.file) },
                            onExport = {
                                fileToExport = item.file
                                exportLauncher.launch(item.file.name)
                            },
                            onDelete = { fileToDelete = item.file },
                            onToggleSelection = {
                                selectedPaths = if (item.file.absolutePath in selectedPaths) {
                                    selectedPaths - item.file.absolutePath
                                } else {
                                    selectedPaths + item.file.absolutePath
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Single File Delete Confirmation Dialog
    fileToDelete?.let { file ->
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Audio File?") },
            text = { Text("Are you sure you want to permanently delete \"${file.name}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        if (activePlayingPath == file.absolutePath) {
                            stopPlayback()
                        }
                        file.delete()
                        fileToDelete = null
                        refreshFiles()
                        Toast.makeText(context, "File deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Batch Delete Confirmation Dialog
    if (showBatchDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteDialog = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete ${selectedPaths.size} Files?") },
            text = { Text("Are you sure you want to permanently delete all selected audio files?") },
            confirmButton = {
                Button(
                    onClick = {
                        audioItems.filter { it.file.absolutePath in selectedPaths }.forEach { item ->
                            if (activePlayingPath == item.file.absolutePath) {
                                stopPlayback()
                            }
                            item.file.delete()
                        }
                        selectedPaths = emptySet()
                        showBatchDeleteDialog = false
                        refreshFiles()
                        Toast.makeText(context, "Selected files deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AudioHistoryCard(
    item: AudioHistoryFileItem,
    isSelected: Boolean,
    selectionMode: Boolean,
    isPlaying: Boolean,
    playProgressMs: Long,
    onPlayToggle: () -> Unit,
    onShare: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    onToggleSelection: () -> Unit
) {
    val file = item.file
    val ext = file.extension.uppercase()
    val badgeColor = when (ext) {
        "MP3" -> Color(0xFFE91E63)
        "M4A" -> Color(0xFF9C27B0)
        "WAV" -> Color(0xFF2196F3)
        "AAC" -> Color(0xFF009688)
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (selectionMode) onToggleSelection() else onPlayToggle()
                },
                onLongClick = { onToggleSelection() }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelection() },
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }

                // Play / Pause Icon Button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(badgeColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = onPlayToggle) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = badgeColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                // File Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = file.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = buildString {
                            append(AudioEngine.formatFileSize(file.length()))
                            if (item.durationMs > 0) {
                                append(" • ")
                                append(AudioEngine.formatDuration(item.durationMs))
                            }
                            if (item.formattedDate.isNotEmpty()) {
                                append(" • ")
                                append(item.formattedDate)
                            }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Format Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeColor.copy(alpha = 0.15f),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = ext,
                        color = badgeColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Real-time Playback Progress (when actively playing)
            if (isPlaying && item.durationMs > 0) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = (playProgressMs.toFloat() / item.durationMs).coerceIn(0f, 1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = badgeColor
                )
            }

            // Item Action Bar (Share, Export, Delete)
            if (!selectionMode) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onShare, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                        Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Share", fontSize = 12.sp)
                    }

                    TextButton(onClick = onExport, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                        Icon(Icons.Default.Download, contentDescription = "Export", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Export", fontSize = 12.sp)
                    }

                    TextButton(
                        onClick = onDelete,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Delete", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
