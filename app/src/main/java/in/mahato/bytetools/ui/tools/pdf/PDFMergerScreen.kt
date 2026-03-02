package `in`.mahato.bytetools.ui.tools.pdf

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PDFMergerScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pdfUris = remember { mutableStateListOf<Uri>() }
    var isLoading by remember { mutableStateOf(false) }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris ->
            pdfUris.addAll(uris)
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PDF Merger", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (pdfUris.size >= 2) {
                        IconButton(onClick = {
                            scope.launch {
                                mergePdfsAndSave(pdfUris.toList(), context, { isLoading = it })
                            }
                        }) {
                            Icon(Icons.Default.MergeType, contentDescription = "Merge")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { pickerLauncher.launch(arrayOf("application/pdf")) }) {
                Icon(Icons.Default.Add, contentDescription = "Add PDF")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (pdfUris.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Merge, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), contentDescription = null)
                    Spacer(Modifier.height(16.dp))
                    Text("Select 2 or more PDFs to merge", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { pickerLauncher.launch(arrayOf("application/pdf")) }) {
                        Icon(Icons.Default.FileOpen, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add PDF Files")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(pdfUris) { index, uri ->
                        PdfFileItem(
                            uri = uri,
                            index = index,
                            onRemove = { pdfUris.removeAt(index) },
                            onMoveUp = { if (index > 0) {
                                val item = pdfUris.removeAt(index)
                                pdfUris.add(index - 1, item)
                            }},
                            onMoveDown = { if (index < pdfUris.size - 1) {
                                val item = pdfUris.removeAt(index)
                                pdfUris.add(index + 1, item)
                            }}
                        )
                    }
                }
            }
            if (isLoading) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black.copy(alpha = 0.5f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Card(shape = RoundedCornerShape(16.dp)) {
                            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(16.dp))
                                Text("Merging PDFs...", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PdfFileItem(uri: Uri, index: Int, onRemove: () -> Unit, onMoveUp: () -> Unit, onMoveDown: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, label = "")

    Card(
        modifier = Modifier.fillMaxWidth().scale(scale),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("${index + 1}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    uri.lastPathSegment ?: "Unknown File",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text("Tap to reorder", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            IconButton(onClick = onMoveUp) { Icon(Icons.Default.ArrowUpward, null) }
            IconButton(onClick = onMoveDown) { Icon(Icons.Default.ArrowDownward, null) }
            IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
        }
    }
}

private suspend fun mergePdfsAndSave(uris: List<Uri>, context: android.content.Context, onLoading: (Boolean) -> Unit) {
    onLoading(true)
    withContext(Dispatchers.IO) {
        val outFileName = "Merged_${System.currentTimeMillis()}.pdf"
        val outDir = File(context.getExternalFilesDir(null), "ByteToolsPDF")
        if (!outDir.exists()) outDir.mkdirs()
        val outFile = File(outDir, outFileName)

        val pdfDocument = android.graphics.pdf.PdfDocument()
        
        try {
            uris.forEachIndexed { docIdx, uri ->
                val fileDescriptor: ParcelFileDescriptor? = context.contentResolver.openFileDescriptor(uri, "r")
                fileDescriptor?.let { fd ->
                    val renderer = PdfRenderer(fd)
                    for (i in 0 until renderer.pageCount) {
                        val page = renderer.openPage(i)
                        val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        
                        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(page.width, page.height, pdfDocument.pages.size).create()
                        val pdfPage = pdfDocument.startPage(pageInfo)
                        pdfPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                        pdfDocument.finishPage(pdfPage)
                        page.close()
                    }
                    renderer.close()
                }
            }
            pdfDocument.writeTo(FileOutputStream(outFile))
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            pdfDocument.close()
        }

        withContext(Dispatchers.Main) {
            android.widget.Toast.makeText(context, "Merged into ${outFile.absolutePath}", android.widget.Toast.LENGTH_LONG).show()
        }
    }
    onLoading(false)
}
