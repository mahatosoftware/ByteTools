package `in`.mahato.bytetools.ui.tools.pdf

import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.util.Matrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PDFWatermarkScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pdfUri by remember { mutableStateOf<Uri?>(null) }
    var watermarkUri by remember { mutableStateOf<Uri?>(null) }
    var watermarkText by remember { mutableStateOf("WATERMARK") }
    var opacity by remember { mutableFloatStateOf(0.3f) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedType by remember { mutableIntStateOf(0) } // 0: Text, 1: Image

    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { pdfUri = it }
    )

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { watermarkUri = it }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PDF Watermark", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (pdfUri != null) {
                        IconButton(onClick = {
                            scope.launch {
                                isLoading = true
                                addWatermarkWithPdfBox(
                                    pdfUri!!,
                                    if (selectedType == 0) watermarkText else null,
                                    if (selectedType == 1) watermarkUri else null,
                                    opacity,
                                    context
                                )
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
                Card(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    onClick = { pdfPicker.launch(arrayOf("application/pdf")) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        if (pdfUri == null) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(8.dp))
                                Text("Select PDF", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(16.dp))
                                Text(pdfUri?.lastPathSegment ?: "PDF Selected", maxLines = 1)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                TabRow(selectedTabIndex = selectedType) {
                    Tab(selected = selectedType == 0, onClick = { selectedType = 0 }) {
                        Text("Text Watermark", modifier = Modifier.padding(12.dp))
                    }
                    Tab(selected = selectedType == 1, onClick = { selectedType = 1 }) {
                        Text("Image Watermark", modifier = Modifier.padding(12.dp))
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (selectedType == 0) {
                    OutlinedTextField(
                        value = watermarkText,
                        onValueChange = { watermarkText = it },
                        label = { Text("Watermark Text") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        onClick = { imagePicker.launch("image/*") },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            if (watermarkUri == null) {
                                Text("Select Watermark Image")
                            } else {
                                Text("Image Selected: ${watermarkUri?.lastPathSegment}")
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Text("Opacity: ${(opacity * 100).toInt()}%", modifier = Modifier.align(Alignment.Start))
                Slider(
                    value = opacity,
                    onValueChange = { opacity = it },
                    valueRange = 0.05f..0.8f
                )

                Spacer(Modifier.height(40.dp))
            }

            if (isLoading) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black.copy(alpha = 0.3f)) {
                    Box(contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
            }
        }
    }
}

private suspend fun addWatermarkWithPdfBox(
    uri: Uri,
    text: String?,
    imageUri: Uri?,
    opacity: Float,
    context: android.content.Context
) {
    withContext(Dispatchers.IO) {
        try {
            val outFileName = "Watermarked_${System.currentTimeMillis()}.pdf"
            val outDir = File(context.getExternalFilesDir(null), "ByteToolsPDF")
            if (!outDir.exists()) outDir.mkdirs()
            val outFile = File(outDir, outFileName)

            val inputStream = context.contentResolver.openInputStream(uri)
            val document = PDDocument.load(inputStream)
            
            for (page in document.pages) {
                val contentStream = PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true)
                
                contentStream.setGraphicsStateParameters(com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState().apply {
                    setStrokingAlphaConstant(opacity)
                    setNonStrokingAlphaConstant(opacity)
                })

                if (text != null) {
                    val font = PDType1Font.HELVETICA_BOLD
                    val fontSize = 80f
                    val textWidth = font.getStringWidth(text) / 1000 * fontSize
                    val textHeight = font.getFontDescriptor().getCapHeight() / 1000 * fontSize
                    
                    val width = page.mediaBox.width
                    val height = page.mediaBox.height
                    
                    contentStream.beginText()
                    contentStream.setFont(font, fontSize)
                    contentStream.setNonStrokingColor(128, 128, 128)
                    
                    // Center and rotate
                    val matrix = Matrix.getRotateInstance(Math.toRadians(45.0), width / 2f, height / 2f)
                    matrix.translate(-textWidth / 2f, -textHeight / 2f)
                    contentStream.setTextMatrix(matrix)
                    contentStream.showText(text)
                    contentStream.endText()
                } else if (imageUri != null) {
                    val imgInputStream = context.contentResolver.openInputStream(imageUri)
                    val bitmap = android.graphics.BitmapFactory.decodeStream(imgInputStream)
                    val pdImage = LosslessFactory.createFromImage(document, bitmap)
                    
                    val w = pdImage.width.toFloat()
                    val h = pdImage.height.toFloat()
                    val scale = Math.min(page.mediaBox.width / w, page.mediaBox.height / h) * 0.5f
                    
                    val drawW = w * scale
                    val drawH = h * scale
                    val x = (page.mediaBox.width - drawW) / 2
                    val y = (page.mediaBox.height - drawH) / 2
                    
                    contentStream.drawImage(pdImage, x, y, drawW, drawH)
                    imgInputStream?.close()
                }
                
                contentStream.close()
            }
            
            document.save(FileOutputStream(outFile))
            document.close()
            inputStream?.close()

            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(context, "Saved to ${outFile.absolutePath}", android.widget.Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
}
