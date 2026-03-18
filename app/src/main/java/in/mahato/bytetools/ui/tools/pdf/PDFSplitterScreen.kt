package `in`.mahato.bytetools.ui.tools.pdf

import android.content.Intent
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import `in`.mahato.bytetools.ui.navigation.Screen
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

enum class SplitMode {
    Manual, EveryPage, Range
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PDFSplitterScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pdfUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var pageCount by rememberSaveable { mutableIntStateOf(0) }
    var selectedPages by rememberSaveable { mutableStateOf<List<Int>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val thumbnails = remember { mutableStateListOf<Bitmap>() }
    var splitModeOrdinal by rememberSaveable { mutableIntStateOf(SplitMode.Manual.ordinal) }
    var rangeText by rememberSaveable { mutableStateOf("") }
    var generatedPaths by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var savedPaths by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }

    val pdfUri = pdfUriString?.let(Uri::parse)
    val splitMode = SplitMode.entries[splitModeOrdinal]

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            pdfUriString = uri?.toString()
            selectedPages = emptyList()
            generatedPaths = emptyList()
            savedPaths = emptyList()
            thumbnails.clear()
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
                                val generatedFiles = splitPdf(
                                    uri = pdfUri!!,
                                    manualIndices = selectedPages,
                                    mode = splitMode,
                                    rangeText = rangeText,
                                    maxPages = pageCount,
                                    context = context
                                )
                                generatedPaths = generatedFiles.map(File::getAbsolutePath)
                                savedPaths = emptyList()
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
                        Tab(selected = splitMode == SplitMode.Manual, onClick = { splitModeOrdinal = SplitMode.Manual.ordinal }) {
                            Text("Manual", modifier = Modifier.padding(12.dp))
                        }
                        Tab(selected = splitMode == SplitMode.EveryPage, onClick = { splitModeOrdinal = SplitMode.EveryPage.ordinal }) {
                            Text("Every Page", modifier = Modifier.padding(12.dp))
                        }
                        Tab(selected = splitMode == SplitMode.Range, onClick = { splitModeOrdinal = SplitMode.Range.ordinal }) {
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
                                                        selectedPages = if (selectedPages.contains(index)) {
                                                            selectedPages - index
                                                        } else {
                                                            selectedPages + index
                                                        }
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

                    if (generatedPaths.isNotEmpty()) {
                        val generatedFiles = generatedPaths.map(::File)
                        val currentSavedPaths = savedPaths
                        val savedFiles = currentSavedPaths.map(::File)
                        val activeFiles = if (savedFiles.isNotEmpty()) savedFiles else generatedFiles

                        if (generatedFiles.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Split Result Ready", fontWeight = FontWeight.Bold)
                                            Text(
                                                if (splitMode == SplitMode.EveryPage) {
                                                    "${activeFiles.size} split PDFs"
                                                } else {
                                                    activeFiles.first().name
                                                },
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                val fileToView = activeFiles.first()
                                                val uriString = Uri.fromFile(fileToView).toString()
                                                navController.navigate(Screen.PDFViewer.route + "?uri=${Uri.encode(uriString)}")
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(if (splitMode == SplitMode.EveryPage) "View First" else "View")
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                if (splitMode == SplitMode.EveryPage) {
                                                    shareFilesAsZip(context, activeFiles)
                                                } else {
                                                    sharePdf(context, activeFiles.first())
                                                }
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Share")
                                        }
                                        Button(
                                            onClick = {
                                                val saved = saveSplitResultToHistory(context, generatedFiles)
                                                if (saved.isNotEmpty()) {
                                                    savedPaths = saved.map(File::getAbsolutePath)
                                                    android.widget.Toast.makeText(context, "Saved to PDF history", android.widget.Toast.LENGTH_SHORT).show()
                                                } else {
                                                    android.widget.Toast.makeText(context, "Could not save split PDF", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            enabled = currentSavedPaths.isEmpty()
                                        ) {
                                            Text("Save")
                                            }
                                        }
                                    }

                                    if (splitMode == SplitMode.EveryPage) {
                                        HorizontalDivider()
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            activeFiles.forEachIndexed { index, file ->
                                                OutlinedCard(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    onClick = {
                                                        val uriString = Uri.fromFile(file).toString()
                                                        navController.navigate(Screen.PDFViewer.route + "?uri=${Uri.encode(uriString)}")
                                                    }
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(12.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            Icons.Default.PictureAsPdf,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                        Spacer(Modifier.width(12.dp))
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                "Split File ${index + 1}",
                                                                fontWeight = FontWeight.Medium
                                                            )
                                                            Text(
                                                                file.name,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                maxLines = 1
                                                            )
                                                        }
                                                        TextButton(
                                                            onClick = {
                                                                val uriString = Uri.fromFile(file).toString()
                                                                navController.navigate(Screen.PDFViewer.route + "?uri=${Uri.encode(uriString)}")
                                                            }
                                                        ) {
                                                            Text("View")
                                                        }
                                                    }
                                                }
                                            }
                                        }
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

private suspend fun splitPdf(
    uri: Uri, 
    manualIndices: List<Int>, 
    mode: SplitMode, 
    rangeText: String,
    maxPages: Int,
    context: android.content.Context
) : List<File> {
    return withContext(Dispatchers.IO) {
        try {
            val outDir = File(context.cacheDir, "Split_${System.currentTimeMillis()}")
            if (!outDir.exists()) outDir.mkdirs()

            val indicesToExtract = when (mode) {
                SplitMode.Manual -> manualIndices.sorted()
                SplitMode.Range -> parseRange(rangeText, maxPages)
                SplitMode.EveryPage -> (0 until maxPages).toList()
            }

            if (indicesToExtract.isEmpty()) return@withContext emptyList()

            if (mode == SplitMode.EveryPage) {
                val outFiles = mutableListOf<File>()
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
                        outFiles += outFile
                    }
                    renderer.close()
                    fileDescriptor.close()
                }
                outFiles
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
                listOf(outFile)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
            emptyList()
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

private fun sharePdf(context: android.content.Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share PDF"))
}

private fun shareFilesAsZip(context: android.content.Context, files: List<File>) {
    val zipFile = File(context.cacheDir, "Split_${System.currentTimeMillis()}.zip")
    runCatching {
        ZipOutputStream(FileOutputStream(zipFile)).use { zip ->
            files.forEach { file ->
                zip.putNextEntry(ZipEntry(file.name))
                file.inputStream().use { input -> input.copyTo(zip) }
                zip.closeEntry()
            }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", zipFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Split PDFs"))
    }.onFailure {
        android.widget.Toast.makeText(context, "Could not share split PDFs", android.widget.Toast.LENGTH_SHORT).show()
    }
}

private fun saveSplitResultToHistory(context: android.content.Context, files: List<File>): List<File> {
    return runCatching {
        val outDir = File(context.getExternalFilesDir(null), "ByteToolsPDF").apply { mkdirs() }
        files.map { source ->
            val targetName = if (source.name == "Split_Result.pdf") {
                "Split_${System.currentTimeMillis()}.pdf"
            } else {
                "Split_${source.nameWithoutExtension}_${System.currentTimeMillis()}.pdf"
            }
            val target = File(outDir, targetName)
            source.copyTo(target, overwrite = true)
            target
        }
    }.getOrElse { emptyList() }
}
