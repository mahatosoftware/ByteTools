package `in`.mahato.bytetools.ui.tools.barcode

import android.graphics.Bitmap
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.zxing.BarcodeFormat
import `in`.mahato.bytetools.ui.navigation.Screen
import `in`.mahato.bytetools.ui.tools.qr.saveBitmapToGallery
import `in`.mahato.bytetools.ui.tools.qr.shareBitmap

enum class BarcodeType(val label: String, val format: BarcodeFormat) {
    CODE_128("Code 128", BarcodeFormat.CODE_128),
    CODE_39("Code 39", BarcodeFormat.CODE_39),
    EAN_13("EAN-13", BarcodeFormat.EAN_13),
    EAN_8("EAN-8", BarcodeFormat.EAN_8),
    UPC_A("UPC-A", BarcodeFormat.UPC_A),
    ITF("ITF", BarcodeFormat.ITF)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeGeneratorScreen(
    navController: NavController,
    viewModel: BarcodeViewModel = hiltViewModel()
) {
    var selectedType by remember { mutableStateOf(BarcodeType.CODE_128) }
    var inputText by remember { mutableStateOf("") }
    
    val barcodeBitmap by viewModel.generatedBarcode.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Barcode Generator", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.QRHistory.route) }) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Barcode Type Selector
            Text("Select Format", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(BarcodeType.values()) { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(type.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Input Form
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Data to Encode", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Enter numbers/text...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Note: Ensure input is valid for ${selectedType.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Generator Action
            Button(
                onClick = { viewModel.generateBarcode(inputText, selectedType.format) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.large,
                enabled = inputText.isNotEmpty()
            ) {
                Icon(Icons.Default.AutoFixHigh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Generate Barcode", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Barcode Preview
            barcodeBitmap?.let { bitmap ->
                BarcodePreviewCard(bitmap)
            }
        }
    }
}

@Composable
fun BarcodePreviewCard(bitmap: Bitmap) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth().height(200.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Generated Barcode",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
    
    Spacer(modifier = Modifier.height(16.dp))
    
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = { shareBitmap(context, bitmap) },
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Share, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("Share")
        }
        OutlinedButton(
            onClick = { saveBitmapToGallery(context, bitmap) },
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Download, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("Save")
        }
    }
}
