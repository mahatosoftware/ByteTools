package `in`.mahato.bytetools.ui.tools.image

import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.exifinterface.media.ExifInterface
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageMetadataScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var metadata by remember { mutableStateOf<List<MetadataItem>>(emptyList()) }
    var isProcessing by remember { mutableStateOf(false) }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            imageUri = it
            scope.launch {
                isProcessing = true
                metadata = extractMetadata(context, it)
                isProcessing = false
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Image Metadata Viewer", fontWeight = FontWeight.Bold) },
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
        ) {
            if (imageUri == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Button(onClick = { pickerLauncher.launch("image/*") }) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Select Image")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().height(250.dp),
                            shape = MaterialTheme.shapes.large
                        ) {
                            AsyncImage(
                                model = imageUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Extracted Metadata", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { 
                                // Reset
                                imageUri = null
                                metadata = emptyList()
                            }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Clear")
                            }
                        }
                    }

                    if (isProcessing) {
                        item {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }

                    items(metadata) { item ->
                        MetadataRow(item)
                    }

                    if (metadata.isNotEmpty()) {
                        item {
                            Button(
                                onClick = { 
                                    // Remove EXIF logic would go here
                                    // In android, it usually requires creating a copy without EXIF tags
                                    android.widget.Toast.makeText(context, "Metadata removal is coming soon!", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.DeleteForever, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Remove All Metadata & Save Copy")
                            }
                        }
                    }
                }
            }
        }
    }
}

data class MetadataItem(val label: String, val value: String, val category: String = "General")

@Composable
fun MetadataRow(item: MetadataItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(item.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(item.value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            }
        }
    }
}

private suspend fun extractMetadata(context: android.content.Context, uri: Uri): List<MetadataItem> = withContext(Dispatchers.IO) {
    val list = mutableListOf<MetadataItem>()
    
    // General Info from Content Resolver
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
        val sizeIndex = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)
        val mimeIndex = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)
        
        if (cursor.moveToFirst()) {
            if (nameIndex != -1) list.add(MetadataItem("Filename", cursor.getString(nameIndex)))
            if (sizeIndex != -1) list.add(MetadataItem("Size", formatFileSize(cursor.getLong(sizeIndex))))
            if (mimeIndex != -1) list.add(MetadataItem("Format", cursor.getString(mimeIndex)))
        }
    }

    // EXIF Info
    try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        if (inputStream != null) {
            val exif = ExifInterface(inputStream)
            
            val tags = listOf(
                ExifInterface.TAG_IMAGE_WIDTH to "Width",
                ExifInterface.TAG_IMAGE_LENGTH to "Height",
                ExifInterface.TAG_DATETIME to "Date Time",
                ExifInterface.TAG_MAKE to "Camera Maker",
                ExifInterface.TAG_MODEL to "Camera Model",
                ExifInterface.TAG_EXPOSURE_TIME to "Exposure Time",
                ExifInterface.TAG_F_NUMBER to "F-Number",
                ExifInterface.TAG_ISO_SPEED_RATINGS to "ISO",
                ExifInterface.TAG_FLASH to "Flash",
                ExifInterface.TAG_FOCAL_LENGTH to "Focal Length",
                ExifInterface.TAG_GPS_LATITUDE to "Lat",
                ExifInterface.TAG_GPS_LONGITUDE to "Lng",
                ExifInterface.TAG_SOFTWARE to "Software",
                ExifInterface.TAG_ORIENTATION to "Orientation"
            )

            tags.forEach { (tag, label) ->
                exif.getAttribute(tag)?.let { value ->
                    if (value.isNotBlank()) {
                        list.add(MetadataItem(label, value, "EXIF"))
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    list
}

private fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = listOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
