package `in`.mahato.bytetools.ui.tools.pdf

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import `in`.mahato.bytetools.ui.navigation.Screen
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PDFScannerScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isScanning by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var scannedPdfPath by rememberSaveable { mutableStateOf<String?>(null) }
    var savedPdfPath by rememberSaveable { mutableStateOf<String?>(null) }
    var scannedPageUris by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }

    val generatedPdf = scannedPdfPath?.let(::File)
    val savedPdf = savedPdfPath?.let(::File)
    val activePdf = savedPdf ?: generatedPdf

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        isScanning = false
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            val pdfUri = scanResult?.pdf?.uri
            if (pdfUri == null) return@rememberLauncherForActivityResult

            scope.launch {
                val tempFile = saveScannedPdfToCache(context, pdfUri)
                if (tempFile != null) {
                    scannedPdfPath = tempFile.absolutePath
                    savedPdfPath = null
                    scannedPageUris = scanResult.pages?.mapNotNull { page -> page.imageUri?.toString() } ?: emptyList()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Document Scanner") },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (generatedPdf == null) {
                EmptyScannerState(
                    isScanning = isScanning,
                    onStartScan = {
                        startDocumentScan(
                            context = context,
                            onStart = { isScanning = true },
                            onError = { isScanning = false },
                            launcher = scannerLauncher
                        )
                    }
                )
            } else {
                ScanResultContent(
                    pageUris = scannedPageUris,
                    currentFile = activePdf,
                    onView = {
                        activePdf?.let { file ->
                            val uriString = Uri.fromFile(file).toString()
                            navController.navigate(Screen.PDFViewer.route + "?uri=${Uri.encode(uriString)}")
                        }
                    },
                    onShare = {
                        activePdf?.let { sharePdf(context, it) }
                    },
                    onSave = {
                        generatedPdf?.let { file ->
                            scope.launch {
                                isSaving = true
                                val saved = savePdfToHistory(context, file)
                                savedPdfPath = saved?.absolutePath
                                isSaving = false
                            }
                        }
                    },
                    saveEnabled = savedPdf == null && !isSaving
                )
            }

            if (isScanning || isSaving) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.35f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Card {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(16.dp))
                                Text(if (isScanning) "Scanning document..." else "Saving PDF...")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyScannerState(
    isScanning: Boolean,
    onStartScan: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.DocumentScanner,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Scan one or more pages into a single PDF.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onStartScan,
            enabled = !isScanning,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(56.dp)
        ) {
            Text("Start Scanning")
        }
    }
}

@Composable
private fun ScanResultContent(
    pageUris: List<String>,
    currentFile: File?,
    onView: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
    saveEnabled: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
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
                        Text("Scan Ready", fontWeight = FontWeight.Bold)
                        Text(currentFile?.name ?: "Document", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onView,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("View")
                    }
                    OutlinedButton(
                        onClick = onShare,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Share")
                    }
                    FilledTonalButton(
                        onClick = onSave,
                        enabled = saveEnabled,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save")
                    }
                }
            }
        }

        if (pageUris.isNotEmpty()) {
            Text("Pages (${pageUris.size})", fontWeight = FontWeight.Bold)
            LazyRow(
                contentPadding = PaddingValues(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(pageUris) { index, pageUri ->
                    PageThumbnailCard(index = index, uri = pageUri)
                }
            }
        }
    }
}

@Composable
private fun PageThumbnailCard(index: Int, uri: String) {
    Card(
        modifier = Modifier.width(130.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.TopStart
            ) {
                AsyncImage(
                    model = uri,
                    contentDescription = "Scanned page ${index + 1}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Surface(
                    modifier = Modifier.padding(8.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "${index + 1}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Page ${index + 1}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun startDocumentScan(
    context: Context,
    onStart: () -> Unit,
    onError: () -> Unit,
    launcher: androidx.activity.compose.ManagedActivityResultLauncher<IntentSenderRequest, androidx.activity.result.ActivityResult>
) {
    onStart()
    try {
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(50)
            .setResultFormats(
                GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                GmsDocumentScannerOptions.RESULT_FORMAT_PDF
            )
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
        val scanner = GmsDocumentScanning.getClient(options)
        scanner.getStartScanIntent(context as Activity).addOnSuccessListener { intentSender ->
            launcher.launch(IntentSenderRequest.Builder(intentSender).build())
        }.addOnFailureListener {
            onError()
        }
    } catch (_: Exception) {
        onError()
    }
}

private suspend fun saveScannedPdfToCache(context: Context, pdfUri: Uri): File? {
    return runCatching {
        val file = File(context.cacheDir, "Scanned_Document_${System.currentTimeMillis()}.pdf")
        context.contentResolver.openInputStream(pdfUri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        file
    }.getOrNull()
}

private fun savePdfToHistory(context: Context, sourceFile: File): File? {
    return runCatching {
        val dir = File(context.getExternalFilesDir(null), "ByteToolsPDF").apply { mkdirs() }
        val destinationFile = File(dir, sourceFile.name)
        sourceFile.copyTo(destinationFile, overwrite = true)
        destinationFile
    }.getOrNull()
}

private fun sharePdf(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share PDF"))
}
