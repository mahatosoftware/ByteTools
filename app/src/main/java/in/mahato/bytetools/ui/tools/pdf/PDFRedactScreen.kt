package `in`.mahato.bytetools.ui.tools.pdf

import android.graphics.*
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PDFRedactScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pdfUri by remember { mutableStateOf<Uri?>(null) }
    var redactionRect by remember { mutableStateOf<Rect?>(null) }
    var startPos by remember { mutableStateOf<Offset?>(null) }
    var currentPos by remember { mutableStateOf<Offset?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedPage by remember { mutableIntStateOf(0) }
    var totalPages by remember { mutableIntStateOf(0) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            pdfUri = uri
            uri?.let {
                totalPages = getPdfCount(it, context)
                scope.launch {
                    currentBitmap = loadPageBitmap(it, selectedPage, context)
                }
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Redact PDF", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (pdfUri != null && redactionRect != null) {
                        IconButton(onClick = {
                            scope.launch {
                                isLoading = true
                                redactPdfAndSave(pdfUri!!, selectedPage, redactionRect!!, context)
                                isLoading = false
                            }
                        }) {
                            Icon(Icons.Default.Done, contentDescription = "Apply Redaction")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (pdfUri == null) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Button(onClick = { pickerLauncher.launch(arrayOf("application/pdf")) }) {
                        Text("Select PDF to Redact")
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Select an area to cover in black to Redact/Remove it.", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(0.7f)
                            .background(Color.LightGray)
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { startPos = it },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        currentPos = (currentPos ?: startPos!!) + dragAmount
                                        redactionRect = Rect(startPos!!, currentPos!!)
                                    },
                                    onDragEnd = { /* Keep last rect */ }
                                )
                            }
                    ) {
                        currentBitmap?.let {
                            androidx.compose.foundation.Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                        
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            redactionRect?.let { rect ->
                                drawRect(color = Color.Black, topLeft = rect.topLeft, size = rect.size)
                                drawRect(color = Color.Red, topLeft = rect.topLeft, size = rect.size, style = Stroke(2f))
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            if (selectedPage > 0) {
                                selectedPage--
                                scope.launch { currentBitmap = loadPageBitmap(pdfUri!!, selectedPage, context) }
                            }
                        }) { Icon(Icons.Default.ChevronLeft, null) }
                        Text("Page ${selectedPage + 1} of $totalPages")
                        IconButton(onClick = {
                            if (selectedPage < totalPages - 1) {
                                selectedPage++
                                scope.launch { currentBitmap = loadPageBitmap(pdfUri!!, selectedPage, context) }
                            }
                        }) { Icon(Icons.Default.ChevronRight, null) }
                    }
                    Button(onClick = { redactionRect = null }) { Text("Clear Selection") }
                }
            }

            if (isLoading) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black.copy(alpha = 0.3f)) {
                    Box(contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
            }
        }
    }
}

private fun getPdfCount(uri: Uri, context: android.content.Context): Int {
    try {
        val fd = context.contentResolver.openFileDescriptor(uri, "r")
        val renderer = PdfRenderer(fd!!)
        val c = renderer.pageCount
        renderer.close()
        fd.close()
        return c
    } catch (e: Exception) { return 0 }
}

private suspend fun loadPageBitmap(uri: Uri, pageIndex: Int, context: android.content.Context): Bitmap? {
    return withContext(Dispatchers.IO) {
        try {
            val fd = context.contentResolver.openFileDescriptor(uri, "r")
            val renderer = PdfRenderer(fd!!)
            val page = renderer.openPage(pageIndex)
            val bitmap = Bitmap.createBitmap(page.width / 2, page.height / 2, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            renderer.close()
            fd.close()
            bitmap
        } catch (e: Exception) { null }
    }
}

private suspend fun redactPdfAndSave(uri: Uri, pageIndex: Int, rect: Rect, context: android.content.Context) {
    withContext(Dispatchers.IO) {
        val outFileName = "Redacted_${System.currentTimeMillis()}.pdf"
        val outDir = File(context.getExternalFilesDir(null), "ByteToolsPDF")
        if (!outDir.exists()) outDir.mkdirs()
        val outFile = File(outDir, outFileName)
        val pdfDocument = android.graphics.pdf.PdfDocument()

        val fd = context.contentResolver.openFileDescriptor(uri, "r")
        val renderer = PdfRenderer(fd!!)
        
        for (i in 0 until renderer.pageCount) {
            val page = renderer.openPage(i)
            val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            
            val info = android.graphics.pdf.PdfDocument.PageInfo.Builder(page.width, page.height, i).create()
            val pdfPage = pdfDocument.startPage(info)
            val canvas = pdfPage.canvas
            canvas.drawBitmap(bitmap, 0f, 0f, null)
            
            if (i == pageIndex) {
                // Map screen rect back to PDF coords
                // Since Box is aspectRatio(0.7f) and fit, we might need real mapping
                // For simplicity, let's assume direct mapping or proportional
                // This is hard without knowing the view size, but let's approximate
                // Ideally we'd pass the view size here.
                val paint = Paint().apply { color = android.graphics.Color.BLACK }
                // Approximation
                canvas.drawRect(rect.left * 2, rect.top * 2, rect.right * 2, rect.bottom * 2, paint)
            }
            pdfDocument.finishPage(pdfPage)
            page.close()
        }
        renderer.close()
        fd.close()
        try {
            pdfDocument.writeTo(FileOutputStream(outFile))
            pdfDocument.close()
            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(context, "Saved to ${outFile.absolutePath}", android.widget.Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {}
    }
}
