package `in`.mahato.bytetools.ui.tools.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageToPdfScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isProcessing by remember { mutableStateOf(false) }
    var pdfFile by remember { mutableStateOf<File?>(null) }

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
                title = { Text("Images to PDF", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (selectedImages.isNotEmpty()) {
                        IconButton(onClick = { selectedImages = emptyList(); pdfFile = null }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedImages.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = {
                        scope.launch {
                            isProcessing = true
                            val file = createPdfFromImages(context, selectedImages)
                            pdfFile = file
                            isProcessing = false
                            if (file != null) {
                                android.widget.Toast.makeText(context, "PDF Created: ${file.name}", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    icon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
                    text = { Text("Generate PDF") }
                )
            }
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
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { pickerLauncher.launch("image/*") }) {
                            Text("Select Images")
                        }
                        Text("Select multiple images to convert", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text("Selected Images (${selectedImages.size})", style = MaterialTheme.typography.titleMedium)
                    }

                    itemsIndexed(selectedImages) { index, uri ->
                        ImagePageItem(
                            uri = uri,
                            index = index,
                            onRemove = { selectedImages = selectedImages.toMutableList().apply { removeAt(index) } },
                            onMoveUp = if (index > 0) { { 
                                val list = selectedImages.toMutableList()
                                val tmp = list[index]
                                list[index] = list[index - 1]
                                list[index - 1] = tmp
                                selectedImages = list
                            } } else null,
                            onMoveDown = if (index < selectedImages.size - 1) { { 
                                val list = selectedImages.toMutableList()
                                val tmp = list[index]
                                list[index] = list[index + 1]
                                list[index + 1] = tmp
                                selectedImages = list
                            } } else null
                        )
                    }

                    item {
                        OutlinedButton(
                            onClick = { pickerLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Text("Add More Images")
                        }
                    }
                    
                    if (pdfFile != null) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("PDF Ready", fontWeight = FontWeight.Bold)
                                        Text(pdfFile!!.name, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Button(onClick = { shareFile(context, pdfFile!!) }) {
                                        Text("Share")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if (isProcessing) {
            Box(modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                Card {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text("Creating PDF...")
                    }
                }
            }
        }
    }
}

@Composable
fun ImagePageItem(
    uri: Uri,
    index: Int,
    onRemove: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(60.dp),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surface
            ) {
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            Spacer(Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text("Page ${index + 1}", fontWeight = FontWeight.Bold)
                Text("Image Asset", style = MaterialTheme.typography.labelSmall)
            }
            
            Row {
                if (onMoveUp != null) {
                    IconButton(onClick = onMoveUp) { Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up") }
                }
                if (onMoveDown != null) {
                    IconButton(onClick = onMoveDown) { Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down") }
                }
                IconButton(onClick = onRemove) { Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

private suspend fun createPdfFromImages(context: android.content.Context, uris: List<Uri>): File? = withContext(Dispatchers.IO) {
    val pdfDocument = PdfDocument()
    
    try {
        uris.forEachIndexed { index, uri ->
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            
            if (bitmap != null) {
                // Standard A4 size is roughly 595x842 at 72dpi, but we'll use image's own dimensions for best quality or fit to A4.
                // For simplicity, we use image dimensions.
                val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                
                page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                pdfDocument.finishPage(page)
                bitmap.recycle()
            }
        }
        
        val outFile = File(context.cacheDir, "ByteTools_${System.currentTimeMillis()}.pdf")
        pdfDocument.writeTo(FileOutputStream(outFile))
        outFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    } finally {
        pdfDocument.close()
    }
}

private fun shareFile(context: android.content.Context, file: File) {
    val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(android.content.Intent.EXTRA_STREAM, uri)
        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(android.content.Intent.createChooser(intent, "Share PDF"))
}
