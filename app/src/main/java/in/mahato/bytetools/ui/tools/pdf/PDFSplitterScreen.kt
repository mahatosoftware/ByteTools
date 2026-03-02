package `in`.mahato.bytetools.ui.tools.pdf

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import java.io.File
import java.io.FileOutputStream

enum class SplitMode {
    Manual, EveryPage, Range
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PDFSplitterScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pdfUri by remember { mutableStateOf<Uri?>(null) }
    var pageCount by remember { mutableIntStateOf(0) }
    val selectedPages = remember { mutableStateListOf<Int>() }
    var isLoading by remember { mutableStateOf(false) }
    val thumbnails = remember { mutableStateListOf<Bitmap>() }
    var splitMode by remember { mutableStateOf(SplitMode.Manual) }
    var rangeText by remember { mutableStateOf("") }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            pdfUri = uri
            uri?.let {
                scope.launch {
                    loadThumbnails(it, context, thumbnails, { pageCount = it }, { isLoading = it })
                }
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PDF Splitter", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (pdfUri != null && (selectedPages.isNotEmpty() || splitMode != SplitMode.Manual)) {
                        IconButton(onClick = {
                            if (splitMode == SplitMode.Manual && selectedPages.isEmpty()) return@IconButton
                            if (splitMode == SplitMode.Range && rangeText.isEmpty()) return@IconButton
                            
                            scope.launch {
                                isLoading = true
                                splitPdfAndSave(pdfUri!!, selectedPages.toList(), splitMode, rangeText, pageCount, context)
                                isLoading = false
                            }
                        }) {
                            Icon(Icons.Default.Done, contentDescription = "Process Split")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (pdfUri == null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.CallSplit, modifier = Modifier.size(80.dp), contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { pickerLauncher.launch(arrayOf("application/pdf")) }) {
                        Text("Select PDF to Split")
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Split Mode Selector
                    TabRow(selectedTabIndex = splitMode.ordinal, containerColor = Color.Transparent) {
                        Tab(selected = splitMode == SplitMode.Manual, onClick = { splitMode = SplitMode.Manual }) {
                            Text("Manual", modifier = Modifier.padding(12.dp))
                        }
                        Tab(selected = splitMode == SplitMode.EveryPage, onClick = { splitMode = SplitMode.EveryPage }) {
                            Text("Every Page", modifier = Modifier.padding(12.dp))
                        }
                        Tab(selected = splitMode == SplitMode.Range, onClick = { splitMode = SplitMode.Range }) {
                            Text("Range", modifier = Modifier.padding(12.dp))
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        when (splitMode) {
                            SplitMode.Manual -> {
                                if (isLoading && thumbnails.isEmpty()) {
                                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                                } else {
                                    Column {
                                        Text(
                                            "Tap to select pages to extract into a new PDF",
                                            modifier = Modifier.padding(16.dp),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        LazyVerticalGrid(
                                            columns = GridCells.Fixed(3),
                                            modifier = Modifier.fillMaxSize(),
                                            contentPadding = PaddingValues(16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(thumbnails.size) { index ->
                                                ThumbnailCard(
                                                    bitmap = thumbnails[index],
                                                    index = index,
                                                    isSelected = selectedPages.contains(index),
                                                    onClick = {
                                                        if (selectedPages.contains(index)) selectedPages.remove(index)
                                                        else selectedPages.add(index)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            SplitMode.EveryPage -> {
                                Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    Icon(Icons.Default.Pages, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.height(16.dp))
                                    Text("Each page will be saved as a separate PDF file.", style = MaterialTheme.typography.bodyLarge, textAlign = androidx.compose.ui.text.`style`.TextAlign.Center)
                                    Text("Total Files: $pageCount", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            SplitMode.Range -> {
                                Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    OutlinedTextField(
                                        value = rangeText,
                                        onValueChange = { rangeText = it },
                                        label = { Text("Enter Ranges") },
                                        placeholder = { Text("e.g. 1-3, 5, 10-12") },
                                        modifier = Modifier.fillMaxWidth(),
                                        supportingText = { Text("Use commas for multiple segments and hyphens for ranges.") },
                                        singleLine = true
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    Text("This will extract the specified pages into a single new PDF document.", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
            
            if (isLoading && splitMode != SplitMode.Manual) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black.copy(alpha = 0.3f)) {
                    Box(contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
            }
        }
    }
}

@Composable
fun ThumbnailCard(bitmap: Bitmap, index: Int, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .aspectRatio(0.7f)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Box {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Surface(
                modifier = Modifier.align(Alignment.TopStart).padding(4.dp).size(20.dp),
                shape = CircleShape,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.5f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("${index + 1}", color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
            }
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.Center).size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private suspend fun loadThumbnails(
    uri: Uri,
    context: android.content.Context,
    thumbs: MutableList<Bitmap>,
    onPageCount: (Int) -> Unit,
    onLoading: (Boolean) -> Unit
) {
    onLoading(true)
    thumbs.clear()
    withContext(Dispatchers.IO) {
        try {
            val fileDescriptor: ParcelFileDescriptor? = context.contentResolver.openFileDescriptor(uri, "r")
            fileDescriptor?.let { fd ->
                val renderer = PdfRenderer(fd)
                onPageCount(renderer.pageCount)
                // Only load first 20 thumbnails to avoid OOM, or more if needed
                val count = minOf(renderer.pageCount, 100) 
                for (i in 0 until count) {
                    val page = renderer.openPage(i)
                    val bitmap = Bitmap.createBitmap(page.width / 4, page.height / 4, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    withContext(Dispatchers.Main) { thumbs.add(bitmap) }
                    page.close()
                }
                renderer.close()
            }
        } catch (e: Exception) {}
    }
    onLoading(false)
}

private suspend fun splitPdfAndSave(
    uri: Uri, 
    manualIndices: List<Int>, 
    mode: SplitMode, 
    rangeText: String,
    maxPages: Int,
    context: android.content.Context
) {
    withContext(Dispatchers.IO) {
        try {
            val outDir = File(context.getExternalFilesDir(null), "EToolsPDF/Split_${System.currentTimeMillis()}")
            if (!outDir.exists()) outDir.mkdirs()

            val indicesToExtract = when (mode) {
                SplitMode.Manual -> manualIndices.sorted()
                SplitMode.Range -> parseRange(rangeText, maxPages)
                SplitMode.EveryPage -> (0 until maxPages).toList()
            }

            if (indicesToExtract.isEmpty()) return@withContext

            if (mode == SplitMode.EveryPage) {
                // Save each page as a separate PDF
                val fd = context.contentResolver.openFileDescriptor(uri, "r")
                fd?.let { fileDescriptor ->
                    val renderer = PdfRenderer(fileDescriptor)
                    for (i in indicesToExtract) {
                        val pdfDocument = android.graphics.pdf.PdfDocument()
                        val page = renderer.openPage(i)
                        val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        
                        val info = android.graphics.pdf.PdfDocument.PageInfo.Builder(page.width, page.height, 0).create()
                        val pdfPage = pdfDocument.startPage(info)
                        pdfPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                        pdfDocument.finishPage(pdfPage)
                        
                        val outFile = File(outDir, "Page_${i + 1}.pdf")
                        pdfDocument.writeTo(FileOutputStream(outFile))
                        pdfDocument.close()
                        page.close()
                    }
                    renderer.close()
                    fileDescriptor.close()
                }
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Saved ${indicesToExtract.size} files to ${outDir.absolutePath}", android.widget.Toast.LENGTH_LONG).show()
                }
            } else {
                // Manual or Range: Save into a single PDF
                val pdfDocument = android.graphics.pdf.PdfDocument()
                val fd = context.contentResolver.openFileDescriptor(uri, "r")
                fd?.let { fileDescriptor ->
                    val renderer = PdfRenderer(fileDescriptor)
                    indicesToExtract.forEachIndexed { newIndex, oldIndex ->
                        val page = renderer.openPage(oldIndex)
                        val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        
                        val info = android.graphics.pdf.PdfDocument.PageInfo.Builder(page.width, page.height, newIndex).create()
                        val pdfPage = pdfDocument.startPage(info)
                        pdfPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                        pdfDocument.finishPage(pdfPage)
                        page.close()
                    }
                    renderer.close()
                    fileDescriptor.close()
                }
                val outFile = File(outDir, "Split_Result.pdf")
                pdfDocument.writeTo(FileOutputStream(outFile))
                pdfDocument.close()
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Saved to ${outFile.absolutePath}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
}

private fun parseRange(rangeStr: String, maxPages: Int): List<Int> {
    val result = mutableSetOf<Int>()
    try {
        val parts = rangeStr.split(",")
        for (part in parts) {
            val trimmed = part.trim()
            if (trimmed.isEmpty()) continue
            if (trimmed.contains("-")) {
                val bounds = trimmed.split("-")
                if (bounds.size == 2) {
                    val start = bounds[0].trim().toIntOrNull()?.minus(1)
                    val end = bounds[1].trim().toIntOrNull()?.minus(1)
                    if (start != null && end != null) {
                        val rangeStart = minOf(start, end).coerceIn(0, maxPages - 1)
                        val rangeEnd = maxOf(start, end).coerceIn(0, maxPages - 1)
                        for (i in rangeStart..rangeEnd) result.add(i)
                    }
                }
            } else {
                val page = trimmed.toIntOrNull()?.minus(1)
                if (page != null && page in 0 until maxPages) {
                    result.add(page)
                }
            }
        }
    } catch (e: Exception) {}
    return result.toList().sorted()
}
