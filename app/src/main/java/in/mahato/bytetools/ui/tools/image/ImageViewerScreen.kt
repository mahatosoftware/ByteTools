package `in`.mahato.bytetools.ui.tools.image

import android.net.Uri
import android.provider.MediaStore
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ImageViewerScreen(navController: NavController, initialUriString: String?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var images by remember { mutableStateOf<List<GalleryImage>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Fetch images and find the initial index
    LaunchedEffect(Unit) {
        val result = fetchProcessedImagesInternal(context)
        images = result
        isLoading = false
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    if (images.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Text("No images found", color = Color.White)
        }
        return
    }

    val initialIndex = remember(images, initialUriString) {
        val index = images.indexOfFirst { it.uri.toString() == initialUriString }
        if (index != -1) index else 0
    }

    val pagerState = rememberPagerState(initialPage = initialIndex) { images.size }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Image Viewer", color = Color.White, style = MaterialTheme.typography.titleMedium)
                        Text("${pagerState.currentPage + 1} / ${images.size}", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black.copy(alpha = 0.5f))
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(padding)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                pageSpacing = 16.dp
            ) { page ->
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = images[page].uri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            // Navigation overlays
            if (pagerState.currentPage > 0) {
                IconButton(
                    onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                    modifier = Modifier.align(Alignment.CenterStart).padding(8.dp)
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous", tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                }
            }

            if (pagerState.currentPage < images.size - 1) {
                IconButton(
                    onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                    modifier = Modifier.align(Alignment.CenterEnd).padding(8.dp)
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next", tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                }
            }
        }
    }

    if (showDeleteConfirm) {
        val currentImage = images[pagerState.currentPage]
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Image") },
            text = { Text("Are you sure you want to delete this image?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        try {
                            context.contentResolver.delete(currentImage.uri, null, null)
                            val newList = images.toMutableList()
                            newList.removeAt(pagerState.currentPage)
                            images = newList
                            if (images.isEmpty()) {
                                navController.popBackStack()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
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

private suspend fun fetchProcessedImagesInternal(context: android.content.Context): List<GalleryImage> = withContext(Dispatchers.IO) {
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
