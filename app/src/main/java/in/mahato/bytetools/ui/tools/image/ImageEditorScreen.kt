package `in`.mahato.bytetools.ui.tools.image

import android.graphics.*
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageEditorScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var editedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    
    var isProcessing by remember { mutableStateOf(false) }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            imageUri = it
            scope.launch {
                isProcessing = true
                originalBitmap = loadBitmapFromUri(context, it)
                editedBitmap = originalBitmap
                isProcessing = false
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Basic Image Editor", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (editedBitmap != null) {
                        IconButton(onClick = { saveBitmapToGallery(context, editedBitmap!!) }) {
                            Icon(Icons.Default.Save, contentDescription = "Save")
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
        ) {
            if (imageUri == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Button(onClick = { pickerLauncher.launch("image/*") }) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Select Photo to Edit")
                    }
                }
            } else {
                Box(modifier = Modifier.weight(1f).fillMaxWidth().background(androidx.compose.ui.graphics.Color.Black), contentAlignment = Alignment.Center) {
                    editedBitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                    if (isProcessing) CircularProgressIndicator()
                }
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        EditorSlider(label = "Brightness", value = brightness, range = -100f..100f) {
                            brightness = it
                            scope.launch { editedBitmap = applyFilters(originalBitmap, brightness, contrast, saturation) }
                        }
                        EditorSlider(label = "Contrast", value = contrast, range = 0.1f..2f) {
                            contrast = it
                            scope.launch { editedBitmap = applyFilters(originalBitmap, brightness, contrast, saturation) }
                        }
                        EditorSlider(label = "Saturation", value = saturation, range = 0f..2f) {
                            saturation = it
                            scope.launch { editedBitmap = applyFilters(originalBitmap, brightness, contrast, saturation) }
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                            FilterButton("Grayscale") {
                                scope.launch { editedBitmap = applyGrayscale(originalBitmap) }
                            }
                            FilterButton("Sepia") {
                                scope.launch { editedBitmap = applySepia(originalBitmap) }
                            }
                            FilterButton("Reset") {
                                brightness = 0f
                                contrast = 1f
                                saturation = 1f
                                editedBitmap = originalBitmap
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditorSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    Column {
        Text("$label: ${String.format("%.1f", value)}", style = MaterialTheme.typography.labelMedium)
        Slider(value = value, onValueChange = onValueChange, valueRange = range)
    }
}

@Composable
fun FilterButton(label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 8.dp)) {
        Text(label, fontSize = 12.sp)
    }
}

private suspend fun loadBitmapFromUri(context: android.content.Context, uri: Uri): Bitmap = withContext(Dispatchers.IO) {
    val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
    val options = BitmapFactory.Options().apply { inMutable = true }
    BitmapFactory.decodeStream(inputStream, null, options) ?: throw Exception("Failed")
}

private suspend fun applyFilters(original: Bitmap?, brightness: Float, contrast: Float, saturation: Float): Bitmap? = withContext(Dispatchers.Default) {
    if (original == null) return@withContext null
    
    val bitmap = original.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(bitmap)
    val paint = Paint()
    
    val cm = ColorMatrix().apply {
        // Brightness
        postConcat(ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, brightness,
            0f, 1f, 0f, 0f, brightness,
            0f, 0f, 1f, 0f, brightness,
            0f, 0f, 0f, 1f, 0f
        )))
        
        // Contrast
        val scale = contrast
        val translate = (-0.5f * scale + 0.5f) * 255f
        postConcat(ColorMatrix(floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale, 0f, 0f, translate,
            0f, 0f, scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        )))
        
        // Saturation
        val satMatrix = ColorMatrix()
        satMatrix.setSaturation(saturation)
        postConcat(satMatrix)
    }
    
    paint.colorFilter = ColorMatrixColorFilter(cm)
    canvas.drawBitmap(bitmap, 0f, 0f, paint)
    bitmap
}

private suspend fun applyGrayscale(original: Bitmap?): Bitmap? = withContext(Dispatchers.Default) {
    if (original == null) return@withContext null
    val bitmap = original.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(bitmap)
    val paint = Paint()
    val cm = ColorMatrix()
    cm.setSaturation(0f)
    paint.colorFilter = ColorMatrixColorFilter(cm)
    canvas.drawBitmap(bitmap, 0f, 0f, paint)
    bitmap
}

private suspend fun applySepia(original: Bitmap?): Bitmap? = withContext(Dispatchers.Default) {
    if (original == null) return@withContext null
    val bitmap = original.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(bitmap)
    val paint = Paint()
    val cm = ColorMatrix()
    cm.setScale(1f, 1f, 0.8f, 1f)
    val sepiaMatrix = ColorMatrix(floatArrayOf(
        0.393f, 0.769f, 0.189f, 0f, 0f,
        0.349f, 0.686f, 0.168f, 0f, 0f,
        0.272f, 0.534f, 0.131f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    ))
    paint.colorFilter = ColorMatrixColorFilter(sepiaMatrix)
    canvas.drawBitmap(bitmap, 0f, 0f, paint)
    bitmap
}

private fun saveBitmapToGallery(context: android.content.Context, bitmap: Bitmap) {
    val filename = "Edited_${System.currentTimeMillis()}.jpg"
    val contentValues = android.content.ContentValues().apply {
        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/ByteTools")
    }
    
    val uri = context.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    uri?.let {
        context.contentResolver.openOutputStream(it)?.use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
            android.widget.Toast.makeText(context, "Saved to Gallery", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
