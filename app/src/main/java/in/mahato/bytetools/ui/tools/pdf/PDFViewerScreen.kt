package `in`.mahato.bytetools.ui.tools.pdf

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.util.FitPolicy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PDFViewerScreen(navController: NavController, initialUriString: String? = null) {
    val context = LocalContext.current
    var pdfUri by remember { mutableStateOf<Uri?>(initialUriString?.let { Uri.parse(it) }) }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            pdfUri = uri
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PDF Viewer", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { pickerLauncher.launch(arrayOf("application/pdf")) }) {
                        Icon(Icons.Default.FileOpen, contentDescription = "Open")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF111111)),
            contentAlignment = Alignment.Center
        ) {
            if (pdfUri == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(onClick = { pickerLauncher.launch(arrayOf("application/pdf")) }) {
                        Text("Open PDF")
                    }
                }
            } else {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        PDFView(ctx, null)
                    },
                    update = { pdfView ->
                        pdfUri?.let { uri ->
                            pdfView.fromUri(uri)
                                .enableSwipe(true)
                                .swipeHorizontal(false)
                                .enableDoubletap(true)
                                .defaultPage(0)
                                .enableAnnotationRendering(false)
                                .password(null)
                                .scrollHandle(null)
                                .enableAntialiasing(true)
                                .spacing(10)
                                .pageFitPolicy(FitPolicy.WIDTH)
                                .load()
                        }
                    }
                )
            }
        }
    }
}