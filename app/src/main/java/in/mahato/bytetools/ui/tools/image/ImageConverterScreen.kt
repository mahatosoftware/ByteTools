package `in`.mahato.bytetools.ui.tools.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream

enum class OutputFormat(val extension: String, val mimeType: String, val compressFormat: Bitmap.CompressFormat) {
    JPEG("jpg", "image/jpeg", Bitmap.CompressFormat.JPEG),
    PNG("png", "image/png", Bitmap.CompressFormat.PNG),
    WEBP("webp", "image/webp", if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSLESS else Bitmap.CompressFormat.WEBP)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageConverterScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var selectedFormat by remember { mutableStateOf(OutputFormat.JPEG) }
    var quality by remember { mutableFloatStateOf(90f) }
    var isProcessing by remember { mutableStateOf(false) }
    var completedCount by remember { mutableIntStateOf(0) }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            selectedImages = selectedImages + uris
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Image Format Converter", fontWeight = FontWeight.Bold) },
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
            if (selectedImages.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Transform, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { pickerLauncher.launch("image/*") }) {
                            Text("Select Images for Batch Conversion")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Conversion Settings", fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(12.dp))
                                
                                Text("Output Format", style = MaterialTheme.typography.labelMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutputFormat.values().forEach { format ->
                                        FilterChip(
                                            selected = selectedFormat == format,
                                            onClick = { selectedFormat = format },
                                            label = { Text(format.name) }
                                        )
                                    }
                                }
                                
                                if (selectedFormat != OutputFormat.PNG) {
                                    Spacer(Modifier.height(8.dp))
                                    Text("Quality: ${quality.toInt()}%", style = MaterialTheme.typography.labelMedium)
                                    Slider(value = quality, onValueChange = { quality = it }, valueRange = 10f..100f)
                                }
                                
                                Spacer(Modifier.height(16.dp))
                                
                                Button(
                                    onClick = {
                                        scope.launch {
                                            isProcessing = true
                                            completedCount = 0
                                            selectedImages.forEach { uri ->
                                                convertAndSaveImage(context, uri, selectedFormat, quality.toInt())
                                                completedCount++
                                            }
                                            isProcessing = false
                                            android.widget.Toast.makeText(context, "Converted $completedCount images successfully!", android.widget.Toast.LENGTH_LONG).show()
                                            selectedImages = emptyList()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isProcessing
                                ) {
                                    if (isProcessing) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Converting $completedCount/${selectedImages.size}...")
                                    } else {
                                        Text("Convert ${selectedImages.size} Images")
                                    }
                                }
                            }
                        }
                    }

                    items(selectedImages) { uri ->
                        ConverterListItem(uri) {
                            selectedImages = selectedImages.filter { it != uri }
                        }
                    }
                    
                    item {
                        TextButton(onClick = { pickerLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Text("Add More Images")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConverterListItem(uri: Uri, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = uri,
                contentDescription = null,
                modifier = Modifier.size(50.dp).background(Color.LightGray, MaterialTheme.shapes.small),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Text("Image Item", modifier = Modifier.weight(1f))
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.Gray)
            }
        }
    }
}

private suspend fun convertAndSaveImage(context: android.content.Context, uri: Uri, format: OutputFormat, quality: Int) = withContext(Dispatchers.IO) {
    try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        
        if (bitmap != null) {
            val filename = "Converted_${System.currentTimeMillis()}.${format.extension}"
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, format.mimeType)
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/ByteTools_Converted")
            }
            
            val outUri = context.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            outUri?.let {
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    bitmap.compress(format.compressFormat, quality, stream)
                }
            }
            bitmap.recycle()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
