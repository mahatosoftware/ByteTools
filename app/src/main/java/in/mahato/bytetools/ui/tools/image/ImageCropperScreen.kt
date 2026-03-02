package `in`.mahato.bytetools.ui.tools.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.navigation.NavController
import `in`.mahato.bytetools.ui.navigation.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageCropperScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    // Selection state (normalized 0..1)
    var selectionLeft by remember { mutableFloatStateOf(0.1f) }
    var selectionTop by remember { mutableFloatStateOf(0.1f) }
    var selectionRight by remember { mutableFloatStateOf(0.9f) }
    var selectionBottom by remember { mutableFloatStateOf(0.9f) }
    
    var containerSize by remember { mutableStateOf(Size.Zero) }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            imageUri = it
            scope.launch {
                currentBitmap = loadBitmapFromUri(context, it)
                // Reset selection
                selectionLeft = 0.1f
                selectionTop = 0.1f
                selectionRight = 0.9f
                selectionBottom = 0.9f
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Image Cropper", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (currentBitmap != null) {
                        IconButton(onClick = {
                            scope.launch {
                                val cropped = cropBitmap(currentBitmap!!, selectionLeft, selectionTop, selectionRight, selectionBottom)
                                val success = withContext(Dispatchers.IO) {
                                    saveBitmapToGallery(context, cropped)
                                }
                                if (success) {
                                    withContext(Dispatchers.Main) {
                                        android.widget.Toast.makeText(context, "Image saved successfully", android.widget.Toast.LENGTH_SHORT).show()
                                        navController.navigate(Screen.ImageDashboard.route) {
                                            popUpTo(Screen.ImageDashboard.route) { 
                                                inclusive = true 
                                            }
                                        }
                                    }
                                }
                            }
                        }) {
                            Icon(Icons.Default.Done, contentDescription = "Done")
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
                        Icon(Icons.Default.Crop, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Select Image to Crop")
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.Black)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    currentBitmap?.let { bitmap ->
                        Box(modifier = Modifier.wrapContentSize()) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Fit
                            )
                            
                            // Interactive Overlay
                            Canvas(
                                modifier = Modifier
                                    .matchParentSize()
                                    .pointerInput(Unit) {
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                // Simplified handle detection
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                val dx = dragAmount.x / size.width
                                                val dy = dragAmount.y / size.height
                                                
                                                // If touch is in bottom-right quadrant, resize. Else move.
                                                // This is a simple way to allow both move and resize without complex handle logic.
                                                val touchX = change.position.x / size.width
                                                val touchY = change.position.y / size.height
                                                
                                                if (touchX > (selectionLeft + selectionRight) / 2 && touchY > (selectionTop + selectionBottom) / 2) {
                                                    // Resize from bottom-right
                                                    selectionRight = (selectionRight + dx).coerceIn(selectionLeft + 0.1f, 1f)
                                                    selectionBottom = (selectionBottom + dy).coerceIn(selectionTop + 0.1f, 1f)
                                                } else {
                                                    // Move the box
                                                    val width = selectionRight - selectionLeft
                                                    val height = selectionBottom - selectionTop
                                                    
                                                    val newLeft = (selectionLeft + dx).coerceIn(0f, 1f - width)
                                                    val newTop = (selectionTop + dy).coerceIn(0f, 1f - height)
                                                    
                                                    selectionLeft = newLeft
                                                    selectionRight = newLeft + width
                                                    selectionTop = newTop
                                                    selectionBottom = newTop + height
                                                }
                                            }
                                        )
                                    }
                            ) {
                                val rect = Rect(
                                    offset = Offset(size.width * selectionLeft, size.height * selectionTop),
                                    size = Size(size.width * (selectionRight - selectionLeft), size.height * (selectionBottom - selectionTop))
                                )
                                
                                // Draw darken background (outside the selection)
                                // Left
                                drawRect(Color.Black.copy(alpha = 0.6f), topLeft = Offset.Zero, size = Size(rect.left, size.height))
                                // Right
                                drawRect(Color.Black.copy(alpha = 0.6f), topLeft = Offset(rect.right, 0f), size = Size(size.width - rect.right, size.height))
                                // Top
                                drawRect(Color.Black.copy(alpha = 0.6f), topLeft = Offset(rect.left, 0f), size = Size(rect.width, rect.top))
                                // Bottom
                                drawRect(Color.Black.copy(alpha = 0.6f), topLeft = Offset(rect.left, rect.bottom), size = Size(rect.width, size.height - rect.bottom))

                                drawRect(
                                    color = Color.White,
                                    topLeft = rect.topLeft,
                                    size = rect.size,
                                    style = Stroke(width = 2.dp.toPx())
                                )
                                
                                // Label for UX
                                // Simplified: no text drawing in Canvas for now to avoid complexity
                                
                                // Corner handles
                                drawCircle(Color.White, radius = 8.dp.toPx(), center = rect.topLeft)
                                drawCircle(Color.White, radius = 8.dp.toPx(), center = rect.bottomRight)
                            }
                        }
                    }
                }
                
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Transform", style = MaterialTheme.typography.labelMedium)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            IconButton(onClick = { 
                                currentBitmap = rotateBitmap(currentBitmap!!, -90f)
                            }) { Icon(Icons.Default.RotateLeft, "Rotate Left") }
                            
                            IconButton(onClick = { 
                                currentBitmap = rotateBitmap(currentBitmap!!, 90f)
                            }) { Icon(Icons.Default.RotateRight, "Rotate Right") }
                            
                            IconButton(onClick = { 
                                currentBitmap = flipBitmap(currentBitmap!!, horizontal = true)
                            }) { Icon(Icons.Default.Flip, "Flip Horizontal") }
                            
                            IconButton(onClick = { 
                                currentBitmap = flipBitmap(currentBitmap!!, horizontal = false)
                            }) { Icon(Icons.Default.FlipCameraAndroid, "Flip Vertical") }
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        Text("• Drag the top-left to move\n• Drag the bottom-right to resize", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(onClick = { pickerLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                            Text("Change Image")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RatioChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 12.sp) }
    )
}

private suspend fun loadBitmapFromUri(context: android.content.Context, uri: Uri): Bitmap = withContext(Dispatchers.IO) {
    val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
    BitmapFactory.decodeStream(inputStream) ?: throw Exception("Failed")
}

private fun rotateBitmap(source: Bitmap, angle: Float): Bitmap {
    val matrix = Matrix()
    matrix.postRotate(angle)
    return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
}

private fun flipBitmap(source: Bitmap, horizontal: Boolean): Bitmap {
    val matrix = Matrix()
    if (horizontal) matrix.preScale(-1.0f, 1.0f)
    else matrix.preScale(1.0f, -1.0f)
    return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
}

private fun cropBitmap(source: Bitmap, left: Float, top: Float, right: Float, bottom: Float): Bitmap {
    val x = (source.width * left).toInt().coerceIn(0, source.width - 1)
    val y = (source.height * top).toInt().coerceIn(0, source.height - 1)
    val width = (source.width * (right - left)).toInt().coerceAtMost(source.width - x)
    val height = (source.height * (bottom - top)).toInt().coerceAtMost(source.height - y)
    
    return try {
        Bitmap.createBitmap(source, x, y, max(1, width), max(1, height))
    } catch (e: Exception) {
        source
    }
}

private fun saveBitmapToGallery(context: android.content.Context, bitmap: Bitmap): Boolean {
    val filename = "Cropped_${System.currentTimeMillis()}.jpg"
    val contentValues = android.content.ContentValues().apply {
        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/ByteTools")
        }
    }
    
    return try {
        val uri = context.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            val success = context.contentResolver.openOutputStream(it)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            } ?: false
            success
        } ?: false
    } catch (e: Exception) {
        false
    }
}
