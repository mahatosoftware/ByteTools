package `in`.mahato.bytetools.ui.tools.pdf

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PDFScannerScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var isScanning by remember { mutableStateOf(false) }

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        isScanning = false
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            scanResult?.pdf?.let { pdf ->
                val pdfUri = pdf.uri
                scope.launch {
                    try {
                        val dir = File(context.getExternalFilesDir(null), "ByteToolsPDF")
                        if (!dir.exists()) dir.mkdirs()
                        val fileName = "Scanned_Document_${System.currentTimeMillis()}.pdf"
                        val destinationFile = File(dir, fileName)
                        
                        context.contentResolver.openInputStream(pdfUri)?.use { input ->
                            FileOutputStream(destinationFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        
                        val snackbarResult = snackbarHostState.showSnackbar(
                            message = "Generated PDF saved successfully",
                            actionLabel = "View",
                            duration = SnackbarDuration.Long
                        )
                        if (snackbarResult == SnackbarResult.ActionPerformed) {
                            val uriString = Uri.fromFile(destinationFile).toString()
                            navController.navigate(`in`.mahato.bytetools.ui.navigation.Screen.PDFViewer.route + "?uri=${Uri.encode(uriString)}")
                        }
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Failed to save PDF: ${e.message}")
                    }
                }
            } ?: run {
                scope.launch { snackbarHostState.showSnackbar("Failed to extract scan result") }
            }
        } else {
            scope.launch { snackbarHostState.showSnackbar("Scanning cancelled") }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Document Scanner") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
                "Scan physical documents directly into high-quality PDFs.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    if (isScanning) return@Button
                    isScanning = true
                    try {
                        val options = GmsDocumentScannerOptions.Builder()
                            .setGalleryImportAllowed(true)
                            .setPageLimit(50)
                            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
                            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                            .build()
                        val scanner = GmsDocumentScanning.getClient(options)
                        scanner.getStartScanIntent(context as Activity).addOnSuccessListener { intentSender ->
                            scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                        }.addOnFailureListener {
                            isScanning = false
                            scope.launch { snackbarHostState.showSnackbar("Failed to start scanner") }
                        }
                    } catch (e: Exception) {
                        isScanning = false
                        scope.launch { snackbarHostState.showSnackbar("Scanner error: ${e.message}") }
                    }
                },
                modifier = Modifier.fillMaxWidth(0.8f).height(56.dp)
            ) {
                Text("Start Scanning")
            }
        }
    }
}
