package `in`.mahato.bytetools.ui.tools.pdf

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas as DrawingCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Path as ComposePath
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import `in`.mahato.bytetools.ui.navigation.Screen

data class ColoredPath(val path: ComposePath, val color: Color)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PDFSignScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pdfUri by remember { mutableStateOf<Uri?>(null) }
    var signatureBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showSignaturePad by remember { mutableStateOf(false) }
    var selectedPage by remember { mutableIntStateOf(0) }
    var totalPages by remember { mutableIntStateOf(0) }
    var signAllPages by remember { mutableStateOf(false) }
    var generatedPdf by remember { mutableStateOf<File?>(null) }
    var savedPdf by remember { mutableStateOf<File?>(null) }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            pdfUri = uri
            generatedPdf = null
            savedPdf = null
            uri?.let {
                totalPages = getCountWithPdfBox(it, context)
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sign PDF", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (pdfUri != null && signatureBitmap != null) {
                        IconButton(onClick = {
                            scope.launch {
                                isLoading = true
                                val pagesToSign = if (signAllPages) (0 until totalPages).toList() else listOf(selectedPage)
                                generatedPdf = signWithPdfBox(pdfUri!!, signatureBitmap!!, pagesToSign, context)
                                savedPdf = null
                                isLoading = false
                            }
                        }) {
                            Icon(Icons.Default.Done, contentDescription = "Process PDF")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (pdfUri == null) {
                    Card(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        onClick = { pickerLauncher.launch(arrayOf("application/pdf")) },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(8.dp))
                                Text("Select PDF to Sign", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Text("PDF Selected: ${pdfUri?.lastPathSegment}", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(16.dp))

                    if (signatureBitmap == null) {
                        Button(onClick = { showSignaturePad = true }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Create, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Create Signature")
                        }
                    } else {
                        // Signature Preview Card
                        Card(
                            modifier = Modifier.fillMaxWidth().height(250.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f))
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                androidx.compose.foundation.Image(
                                    bitmap = signatureBitmap!!.asImageBitmap(),
                                    contentDescription = "Signature",
                                    modifier = Modifier.padding(24.dp).fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                                Row(modifier = Modifier.align(Alignment.TopEnd)) {
                                    IconButton(onClick = { 
                                        val matrix = Matrix().apply { postRotate(90f) }
                                        signatureBitmap = Bitmap.createBitmap(signatureBitmap!!, 0, 0, signatureBitmap!!.width, signatureBitmap!!.height, matrix, true)
                                    }) {
                                        Icon(Icons.Default.RotateRight, contentDescription = "Rotate")
                                    }
                                    IconButton(onClick = { signatureBitmap = null }) {
                                        Icon(Icons.Default.Close, null)
                                    }
                                }
                            }
                        }
                        
                        Spacer(Modifier.height(24.dp))
                        
                        // Page Selection Controls
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Apply to All Pages", fontWeight = FontWeight.Medium)
                                    Switch(checked = signAllPages, onCheckedChange = { signAllPages = it })
                                }
                                
                                AnimatedVisibility(!signAllPages) {
                                    Column {
                                        Spacer(Modifier.height(16.dp))
                                        Text("Select Page: ${selectedPage + 1} of $totalPages", style = MaterialTheme.typography.bodyMedium)
                                        Slider(
                                            value = selectedPage.toFloat(),
                                            onValueChange = { selectedPage = it.toInt() },
                                            valueRange = 0f..(if (totalPages > 0) totalPages - 1 else 0).toFloat(),
                                            steps = if (totalPages > 1) totalPages - 2 else 0
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        generatedPdf?.let { file ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
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
                                            Text("PDF Ready", fontWeight = FontWeight.Bold)
                                            Text((savedPdf ?: file).name, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                val uriString = Uri.fromFile(savedPdf ?: file).toString()
                                                navController.navigate(Screen.PDFViewer.route + "?uri=${Uri.encode(uriString)}")
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("View")
                                        }
                                        OutlinedButton(
                                            onClick = { sharePdf(context, savedPdf ?: file) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Share")
                                        }
                                        Button(
                                            onClick = {
                                                val result = savePdfToHistory(context, file)
                                                if (result != null) {
                                                    savedPdf = result
                                                    android.widget.Toast.makeText(context, "Saved to PDF history", android.widget.Toast.LENGTH_SHORT).show()
                                                } else {
                                                    android.widget.Toast.makeText(context, "Could not save PDF", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            enabled = savedPdf == null
                                        ) {
                                            Text("Save")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showSignaturePad) {
                SignaturePad(
                    onDismiss = { showSignaturePad = false },
                    onCapture = {
                        signatureBitmap = it
                        showSignaturePad = false
                    }
                )
            }

            if (isLoading) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black.copy(alpha = 0.3f)) {
                    Box(contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
            }
        }
    }
}

@Composable
fun SignaturePad(onDismiss: () -> Unit, onCapture: (Bitmap) -> Unit) {
    val paths = remember { mutableStateListOf<ColoredPath>() }
    var currentPath by remember { mutableStateOf<ComposePath?>(null) }
    var selectedColor by remember { mutableStateOf(Color.Black) }
    
    val colors = listOf(Color.Black, Color.Blue, Color.Red, Color(0xFF006400) /* Dark Green */)
    
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Draw Signature", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Row {
                        IconButton(onClick = { paths.clear() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear")
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                // Color Selector
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Color: ", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.width(8.dp))
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (selectedColor == color) 3.dp else 1.dp,
                                    color = if (selectedColor == color) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = color }
                        )
                    }
                }
                
                Spacer(Modifier.height(16.dp))

                // Canvas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .onGloballyPositioned { canvasSize = it.size }
                        .pointerInput(selectedColor) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    currentPath = ComposePath().apply { moveTo(offset.x, offset.y) }
                                    paths.add(ColoredPath(currentPath!!, selectedColor))
                                },
                                onDrag = { change, _ ->
                                    currentPath?.lineTo(change.position.x, change.position.y)
                                    val last = paths.last()
                                    paths.removeAt(paths.size - 1)
                                    paths.add(last)
                                },
                                onDragEnd = { currentPath = null }
                            )
                        }
                ) {
                    DrawingCanvas(modifier = Modifier.fillMaxSize()) {
                        paths.forEach { coloredPath ->
                            drawPath(
                                path = coloredPath.path,
                                color = coloredPath.color,
                                style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                    }
                    
                    if (paths.isEmpty()) {
                        Text(
                            "Sign here",
                            modifier = Modifier.align(Alignment.Center),
                            color = Color.LightGray,
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                }

                // Footer Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (paths.isEmpty()) return@Button
                            
                            val width = if (canvasSize.width > 0) canvasSize.width else 1000
                            val height = if (canvasSize.height > 0) canvasSize.height else 600
                            
                            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            val canvas = android.graphics.Canvas(bitmap)
                            canvas.drawColor(android.graphics.Color.WHITE)
                            
                            // Draw each path with its specific color
                            paths.forEach { coloredPath ->
                                val paint = android.graphics.Paint().apply {
                                    color = coloredPath.color.toArgb()
                                    strokeWidth = 10f
                                    style = android.graphics.Paint.Style.STROKE
                                    strokeJoin = android.graphics.Paint.Join.ROUND
                                    strokeCap = android.graphics.Paint.Cap.ROUND
                                    isAntiAlias = true
                                }
                                canvas.drawPath(coloredPath.path.asAndroidPath(), paint)
                            }
                            
                            onCapture(bitmap)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Confirm Signature")
                    }
                }
                
                if (!isLandscape) {
                    Text(
                        "Tip: Rotate for more space.",
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .align(Alignment.CenterHorizontally),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun getCountWithPdfBox(uri: Uri, context: android.content.Context): Int {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val doc = PDDocument.load(inputStream)
        val count = doc.numberOfPages
        doc.close()
        inputStream?.close()
        count
    } catch (e: Exception) { 0 }
}

private suspend fun signWithPdfBox(uri: Uri, sig: Bitmap, pagesIdx: List<Int>, context: android.content.Context): File? {
    return withContext(Dispatchers.IO) {
        try {
            val outFileName = "Signed_Multi_${System.currentTimeMillis()}.pdf"
            val outFile = File(context.cacheDir, outFileName)

            val inputStream = context.contentResolver.openInputStream(uri)
            val doc = PDDocument.load(inputStream)
            
            val transparentSig = makeTransparent(sig)
            val pdImage = LosslessFactory.createFromImage(doc, transparentSig)
            
            pagesIdx.forEach { idx ->
                val page = doc.getPage(idx)
                val contentStream = PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)
                
                val scale = 0.35f
                val w = page.mediaBox.width * scale
                val h = (w * sig.height) / sig.width
                val x = page.mediaBox.width - w - 50
                val y = 50f
                
                contentStream.drawImage(pdImage, x, y, w, h)
                contentStream.close()
            }
            
            doc.save(FileOutputStream(outFile))
            doc.close()
            inputStream?.close()

            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(context, "Signed PDF generated", android.widget.Toast.LENGTH_LONG).show()
            }
            outFile
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
            null
        }
    }
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

private fun savePdfToHistory(context: android.content.Context, sourceFile: File): File? {
    return runCatching {
        val outDir = File(context.getExternalFilesDir(null), "ByteToolsPDF").apply { mkdirs() }
        val outFile = File(outDir, sourceFile.name)
        sourceFile.copyTo(outFile, overwrite = true)
        outFile
    }.getOrNull()
}

private fun makeTransparent(bitmap: Bitmap): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    val outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    for (y in 0 until height) {
        for (x in 0 until width) {
            val color = bitmap.getPixel(x, y)
            val r = android.graphics.Color.red(color)
            val g = android.graphics.Color.green(color)
            val b = android.graphics.Color.blue(color)
            if (r > 240 && g > 240 && b > 240) {
                outBitmap.setPixel(x, y, android.graphics.Color.TRANSPARENT)
            } else {
                outBitmap.setPixel(x, y, color)
            }
        }
    }
    return outBitmap
}
