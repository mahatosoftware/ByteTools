package `in`.mahato.bytetools.ui.tools.pdf

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import `in`.mahato.bytetools.utils.OCRManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PDFOCRScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    
    var pdfUri by remember { mutableStateOf<Uri?>(null) }
    var selectedPage by remember { mutableIntStateOf(0) }
    var totalPages by remember { mutableIntStateOf(0) }
    var recognizedText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    val ocrManager = remember { OCRManager(context) }
    
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            pdfUri = uri
            uri?.let {
                totalPages = getCount(it, context)
                scope.launch {
                    currentBitmap = loadBitmap(it, selectedPage, context)
                }
            }
        }
    )

    DisposableEffect(Unit) {
        onDispose { ocrManager.close() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PDF OCR", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (recognizedText.isNotEmpty()) {
                        IconButton(onClick = { 
                            clipboardManager.setText(AnnotatedString(recognizedText))
                            android.widget.Toast.makeText(context, "Text copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
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
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Scanner, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(8.dp))
                                Text("Select PDF for OCR", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    currentBitmap?.let {
                        Card(
                            modifier = Modifier.fillMaxWidth().height(300.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            androidx.compose.foundation.Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            if (selectedPage > 0) {
                                selectedPage--
                                scope.launch { currentBitmap = loadBitmap(pdfUri!!, selectedPage, context) }
                            }
                        }) { Icon(Icons.Default.ChevronLeft, null) }
                        Text("Page ${selectedPage + 1} of $totalPages")
                        IconButton(onClick = {
                            if (selectedPage < totalPages - 1) {
                                selectedPage++
                                scope.launch { currentBitmap = loadBitmap(pdfUri!!, selectedPage, context) }
                            }
                        }) { Icon(Icons.Default.ChevronRight, null) }
                    }
                    
                    Button(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                currentBitmap?.let {
                                    recognizedText = ocrManager.recognizeText(it)
                                }
                                isLoading = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.TextSnippet, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Extract Text from Page")
                    }
                    
                    if (recognizedText.isNotEmpty()) {
                        Spacer(Modifier.height(24.dp))
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Recognized Text", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(8.dp))
                                Text(recognizedText)
                            }
                        }
                    }
                }
            }
            
            if (isLoading) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black.copy(alpha = 0.4f)) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.White)
                            Spacer(Modifier.height(16.dp))
                            Text("Processing OCR...", color = Color.White)
                            Text("This might take a moment...", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

private fun getCount(uri: Uri, context: android.content.Context): Int {
    return try {
        val fd = context.contentResolver.openFileDescriptor(uri, "r")
        val renderer = PdfRenderer(fd!!)
        val c = renderer.pageCount
        renderer.close()
        fd.close()
        c
    } catch (e: Exception) { 0 }
}

private suspend fun loadBitmap(uri: Uri, pageIdx: Int, context: android.content.Context): Bitmap? {
    return withContext(Dispatchers.IO) {
        try {
            val fd = context.contentResolver.openFileDescriptor(uri, "r")
            val renderer = PdfRenderer(fd!!)
            val page = renderer.openPage(pageIdx)
            // Render at higher resolution for better OCR
            val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            renderer.close()
            fd.close()
            bitmap
        } catch (e: Exception) { null }
    }
}
