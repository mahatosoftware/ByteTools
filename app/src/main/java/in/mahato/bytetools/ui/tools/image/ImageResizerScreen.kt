package `in`.mahato.bytetools.ui.tools.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageResizerScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var resizedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    var widthStr by remember { mutableStateOf("") }
    var heightStr by remember { mutableStateOf("") }
    var percentage by remember { mutableFloatStateOf(100f) }
    var quality by remember { mutableFloatStateOf(80f) }
    var maintainAspectRatio by remember { mutableStateOf(true) }
    
    var originalSize by remember { mutableLongStateOf(0L) }
    var newSize by remember { mutableLongStateOf(0L) }
    var isProcessing by remember { mutableStateOf(false) }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            imageUri = it
            scope.launch {
                isProcessing = true
                val bitmap = loadBitmapFromUri(context, it)
                originalBitmap = bitmap
                originalSize = getFileSize(context, it)
                widthStr = bitmap.width.toString()
                heightStr = bitmap.height.toString()
                isProcessing = false
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Resizer & Compressor", fontWeight = FontWeight.Bold) },
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (imageUri == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { pickerLauncher.launch("image/*") }) {
                            Text("Select Image")
                        }
                    }
                }
            } else {
                originalBitmap?.let { bitmap ->
                    Image(
                        bitmap = (resizedBitmap ?: bitmap).asImageBitmap(),
                        contentDescription = "Preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium),
                        contentScale = ContentScale.Fit
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        InfoChip(label = "Original", value = formatFileSize(originalSize))
                        InfoChip(label = "New", value = formatFileSize(newSize))
                    }
                    
                    Spacer(Modifier.height(24.dp))
                    
                    Text("Resize Options", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))
                    Spacer(Modifier.height(8.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Percentage: ${percentage.toInt()}%", modifier = Modifier.weight(1f))
                        Slider(
                            value = percentage,
                            onValueChange = { 
                                percentage = it
                                val scale = it / 100f
                                widthStr = (bitmap.width * scale).toInt().toString()
                                heightStr = (bitmap.height * scale).toInt().toString()
                            },
                            valueRange = 10f..200f,
                            modifier = Modifier.weight(2f)
                        )
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = widthStr,
                            onValueChange = { 
                                widthStr = it
                                if (maintainAspectRatio) {
                                    it.toIntOrNull()?.let { w ->
                                        heightStr = (w * (bitmap.height.toFloat() / bitmap.width)).toInt().toString()
                                    }
                                }
                            },
                            label = { Text("Width") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = heightStr,
                            onValueChange = { 
                                heightStr = it
                                if (maintainAspectRatio) {
                                    it.toIntOrNull()?.let { h ->
                                        widthStr = (h * (bitmap.width.toFloat() / bitmap.height)).toInt().toString()
                                    }
                                }
                            },
                            label = { Text("Height") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(checked = maintainAspectRatio, onCheckedChange = { maintainAspectRatio = it })
                        Text("Maintain Aspect Ratio")
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    Text("Compression Qualilty: ${quality.toInt()}%", modifier = Modifier.align(Alignment.Start))
                    Slider(value = quality, onValueChange = { quality = it }, valueRange = 1f..100f)
                    
                    Spacer(Modifier.height(24.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(
                            onClick = { 
                                scope.launch {
                                    isProcessing = true
                                    val w = widthStr.toIntOrNull() ?: bitmap.width
                                    val h = heightStr.toIntOrNull() ?: bitmap.height
                                    val result = processImage(bitmap, w, h, quality.toInt())
                                    resizedBitmap = result.first
                                    newSize = result.second
                                    isProcessing = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isProcessing
                        ) {
                            if (isProcessing) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            else Text("Preview")
                        }
                        
                        Button(
                            onClick = { 
                                resizedBitmap?.let { saveBitmapToGallery(context, it, quality.toInt()) }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = resizedBitmap != null && !isProcessing
                        ) {
                            Text("Save")
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = { 
                        imageUri = null
                        originalBitmap = null
                        resizedBitmap = null
                        newSize = 0
                    }) {
                        Text("Clear and Start New")
                    }
                }
            }
        }
    }
}

@Composable
fun InfoChip(label: String, value: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Text("$label: $value", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium)
    }
}

private suspend fun loadBitmapFromUri(context: android.content.Context, uri: Uri): Bitmap = withContext(Dispatchers.IO) {
    val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
    BitmapFactory.decodeStream(inputStream) ?: throw Exception("Failed to decode bitmap")
}

private fun getFileSize(context: android.content.Context, uri: Uri): Long {
    var size = 0L
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (cursor.moveToFirst()) {
            size = cursor.getLong(sizeIndex)
        }
    }
    return size
}

private fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = listOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

private suspend fun processImage(original: Bitmap, width: Int, height: Int, quality: Int): Pair<Bitmap, Long> = withContext(Dispatchers.IO) {
    val resized = Bitmap.createScaledBitmap(original, width, height, true)
    val stream = ByteArrayOutputStream()
    resized.compress(Bitmap.CompressFormat.JPEG, quality, stream)
    val size = stream.toByteArray().size.toLong()
    resized to size
}

private fun saveBitmapToGallery(context: android.content.Context, bitmap: Bitmap, quality: Int) {
    val filename = "Resized_${System.currentTimeMillis()}.jpg"
    val contentValues = android.content.ContentValues().apply {
        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/ByteTools")
    }
    
    val uri = context.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    uri?.let {
        context.contentResolver.openOutputStream(it)?.use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            android.widget.Toast.makeText(context, "Saved to Gallery", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
