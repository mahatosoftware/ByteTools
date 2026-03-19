package `in`.mahato.bytetools.ui.tools.image

import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import `in`.mahato.bytetools.ui.navigation.Screen
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageGalleryScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var images by remember { mutableStateOf<List<GalleryImage>>(emptyList()) }
    var isRefreshing by remember { mutableStateOf(false) }
    var totalStorage by remember { mutableLongStateOf(0L) }
    
    var selectedUris by remember { mutableStateOf(setOf<Uri>()) }
    val isSelectionMode by remember { derivedStateOf { selectedUris.isNotEmpty() } }

    val loadImages = {
        scope.launch {
            isRefreshing = true
            val result = fetchProcessedImages(context)
            images = result
            totalStorage = result.sumOf { it.size }
            isRefreshing = false
        }
    }

    LaunchedEffect(Unit) {
        loadImages()
    }

    BackHandler(enabled = isSelectionMode) {
        selectedUris = emptySet()
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = { Text("${selectedUris.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = { selectedUris = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                        }
                    },
                    actions = {
                        IconButton(onClick = { shareImages(context, selectedUris.toList()) }) {
                            Icon(Icons.Default.Share, contentDescription = "Share selected")
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete selected")
                        }
                    }
                )
            } else {
                CenterAlignedTopAppBar(
                    title = { Text("Gallery & History", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { loadImages() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Storage Summary
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Storage, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Processed Storage Usage", fontWeight = FontWeight.Bold)
                        Text("${images.size} items • ${formatFileSize(totalStorage)} used", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (images.isEmpty() && !isRefreshing) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No processed images found yet.", color = Color.Gray)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(images) { image ->
                        val isSelected = selectedUris.contains(image.uri)
                        GalleryItem(
                            image = image,
                            isSelected = isSelected,
                            isSelectionMode = isSelectionMode,
                            onToggleSelection = {
                                selectedUris = if (isSelected) {
                                    selectedUris - image.uri
                                } else {
                                    selectedUris + image.uri
                                }
                            },
                            onView = {
                                navController.navigate(Screen.FullScreenImage.route + "?uri=${Uri.encode(image.uri.toString())}")
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Images") },
            text = { Text("Are you sure you want to delete ${selectedUris.size} selected images?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            deleteImages(context, selectedUris.toList())
                            selectedUris = emptySet()
                            loadImages()
                        }
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryItem(
    image: GalleryImage,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onView: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                else Color.LightGray, 
                MaterialTheme.shapes.small
            )
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) onToggleSelection() else onView()
                },
                onLongClick = {
                    if (!isSelectionMode) onToggleSelection()
                }
            )
    ) {
        AsyncImage(
            model = image.uri,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isSelected) 8.dp else 0.dp),
            contentScale = ContentScale.Crop
        )
        
        if (isSelectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelection() },
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}

private suspend fun fetchProcessedImages(context: android.content.Context): List<GalleryImage> = withContext(Dispatchers.IO) {
    val list = mutableListOf<GalleryImage>()
    
    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.SIZE,
        MediaStore.Images.Media.DATE_ADDED,
        MediaStore.Images.Media.RELATIVE_PATH
    )
    
    val selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
    val selectionArgs = arrayOf("%Pictures/ByteTools%")
    val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

    context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        selectionArgs,
        sortOrder
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
        val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
        val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val name = cursor.getString(nameColumn)
            val size = cursor.getLong(sizeColumn)
            val date = cursor.getLong(dateColumn)
            val contentUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
            list.add(GalleryImage(contentUri, name, size, date))
        }
    }
    list
}

private suspend fun deleteImages(context: android.content.Context, uris: List<Uri>) = withContext(Dispatchers.IO) {
    try {
        uris.forEach { uri ->
            context.contentResolver.delete(uri, null, null)
        }
        withContext(Dispatchers.Main) {
            android.widget.Toast.makeText(context, "${uris.size} images deleted", android.widget.Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun shareImages(context: android.content.Context, uris: List<Uri>) {
    if (uris.isEmpty()) return

    val shareIntent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uris.first())
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        }
    }.apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val chooserTitle = if (uris.size == 1) "Share image" else "Share images"
    context.startActivity(Intent.createChooser(shareIntent, chooserTitle))
}

private fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = listOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
