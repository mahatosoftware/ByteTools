package `in`.mahato.bytetools.ui.tools.qr

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import `in`.mahato.bytetools.ui.navigation.Screen

enum class QRType(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    TEXT("Text", Icons.Default.TextFields),
    URL("Website", Icons.Default.Language),
    WIFI("WiFi", Icons.Default.Wifi),
    PHONE("Phone", Icons.Default.Phone),
    SMS("SMS", Icons.Default.Sms),
    EMAIL("Email", Icons.Default.Email),
    CONTACT("Contact", Icons.Default.ContactPage),
    UPI("UPI", Icons.Default.AccountBalanceWallet),
    LOCATION("Location", Icons.Default.LocationOn)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRGeneratorScreen(
    navController: NavController,
    viewModel: QRGeneratorViewModel = hiltViewModel()
) {
    var selectedType by remember { mutableStateOf(QRType.TEXT) }
    var inputText by remember { mutableStateOf("") }
    
    // Customization state
    var qrColor by remember { mutableStateOf(androidx.compose.ui.graphics.Color.Black) }
    var bgColor by remember { mutableStateOf(androidx.compose.ui.graphics.Color.White) }
    var ecLevel by remember { mutableStateOf(ErrorCorrectionLevel.M) }
    
    val qrBitmap by viewModel.qrBitmap.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("QR Generator", fontWeight = FontWeight.Bold) },
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
            // QR Type Selector
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(QRType.values()) { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(type.label) },
                        leadingIcon = { Icon(type.icon, contentDescription = null, size = 18.dp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dynamic Input Form
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Input Data", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    when (selectedType) {
                        QRType.WIFI -> WifiInput { inputText = it }
                        QRType.CONTACT -> ContactInput { inputText = it }
                        QRType.UPI -> UpiInput { inputText = it }
                        QRType.LOCATION -> LocationInput { inputText = it }
                        else -> {
                            TextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                placeholder = { Text("Enter ${selectedType.label} here...") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Generator Action
            Button(
                onClick = { viewModel.generateQRCode(inputText, qrColor.toArgb(), bgColor.toArgb(), errorCorrectionLevel = ecLevel) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.large,
                enabled = inputText.isNotEmpty()
            ) {
                Icon(Icons.Default.AutoFixHigh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Generate QR Code", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // QR Preview
            qrBitmap?.let { bitmap ->
                QRPreviewCard(bitmap)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Customization Section (Labeled Pro UI but free)
                Text("Customize (Premium Features)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))
                
                CustomizationOptions(
                    selectedQRColor = qrColor,
                    onQRColorChange = { qrColor = it },
                    selectedBGColor = bgColor,
                    onBGColorChange = { bgColor = it },
                    errorCorrectionLevel = ecLevel,
                    onECLevelChange = { ecLevel = it }
                )
            }
        }
    }
}

@Composable
fun QRPreviewCard(bitmap: Bitmap) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.size(280.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Generated QR Code",
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

fun shareBitmap(context: android.content.Context, bitmap: Bitmap) {
    val path = android.provider.MediaStore.Images.Media.insertImage(context.contentResolver, bitmap, "QR_Code_${System.currentTimeMillis()}", null)
    val uri = android.net.Uri.parse(path)
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND)
    intent.type = "image/png"
    intent.putExtra(android.content.Intent.EXTRA_STREAM, uri)
    context.startActivity(android.content.Intent.createChooser(intent, "Share QR Code"))
}

fun saveBitmapToGallery(context: android.content.Context, bitmap: Bitmap) {
    val filename = "QR_${System.currentTimeMillis()}.png"
    val contentValues = android.content.ContentValues().apply {
        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
        put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES)
    }
    
    val uri = context.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    uri?.let {
        context.contentResolver.openOutputStream(it)?.use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            android.widget.Toast.makeText(context, "Saved to Gallery", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun CustomizationOptions(
    selectedQRColor: androidx.compose.ui.graphics.Color,
    onQRColorChange: (androidx.compose.ui.graphics.Color) -> Unit,
    selectedBGColor: androidx.compose.ui.graphics.Color,
    onBGColorChange: (androidx.compose.ui.graphics.Color) -> Unit,
    errorCorrectionLevel: ErrorCorrectionLevel,
    onECLevelChange: (ErrorCorrectionLevel) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // EC Level
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Error Correction", modifier = Modifier.width(120.dp), style = MaterialTheme.typography.labelMedium)
            val levels = listOf(ErrorCorrectionLevel.L, ErrorCorrectionLevel.M, ErrorCorrectionLevel.Q, ErrorCorrectionLevel.H)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                levels.forEach { level ->
                    FilterChip(
                        selected = errorCorrectionLevel == level,
                        onClick = { onECLevelChange(level) },
                        label = { Text(level.name) }
                    )
                }
            }
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("QR Color", modifier = Modifier.width(120.dp))
            val colors = listOf(androidx.compose.ui.graphics.Color.Black, androidx.compose.ui.graphics.Color.Red, androidx.compose.ui.graphics.Color.Blue, androidx.compose.ui.graphics.Color(0xFF4CAF50))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                colors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(color)
                            .border(2.dp, if (selectedQRColor == color) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent)
                            .clickable { onQRColorChange(color) }
                    )
                }
            }
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("BG Color", modifier = Modifier.width(120.dp))
            val colors = listOf(androidx.compose.ui.graphics.Color.White, androidx.compose.ui.graphics.Color(0xFFF5F5F5), androidx.compose.ui.graphics.Color(0xFFE3F2FD))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                colors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(color)
                            .border(2.dp, if (selectedBGColor == color) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent)
                            .clickable { onBGColorChange(color) }
                    )
                }
            }
        }
    }
}

@Composable
fun WifiInput(onDataChange: (String) -> Unit) {
    var ssid by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var security by remember { mutableStateOf("WPA") }
    
    LaunchedEffect(ssid, password, security) {
        onDataChange("WIFI:S:$ssid;T:$security;P:$password;;")
    }
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextField(value = ssid, onValueChange = { ssid = it }, label = { Text("SSID / Name") }, modifier = Modifier.fillMaxWidth())
        TextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun ContactInput(onDataChange: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    
    LaunchedEffect(name, phone, email) {
        onDataChange("BEGIN:VCARD\nVERSION:3.0\nN:$name\nTEL:$phone\nEMAIL:$email\nEND:VCARD")
    }
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
        TextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth())
        TextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun UpiInput(onDataChange: (String) -> Unit) {
    var upiId by remember { mutableStateOf("") }
    var payeeName by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    
    LaunchedEffect(upiId, payeeName, amount) {
        onDataChange("upi://pay?pa=$upiId&pn=$payeeName&am=$amount&cu=INR")
    }
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextField(value = upiId, onValueChange = { upiId = it }, label = { Text("UPI ID (VPA)") }, modifier = Modifier.fillMaxWidth())
        TextField(value = payeeName, onValueChange = { payeeName = it }, label = { Text("Payee Name") }, modifier = Modifier.fillMaxWidth())
        TextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount (Optional)") }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun LocationInput(onDataChange: (String) -> Unit) {
    var lat by remember { mutableStateOf("") }
    var lng by remember { mutableStateOf("") }
    
    LaunchedEffect(lat, lng) {
        onDataChange("geo:$lat,$lng")
    }
    
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextField(value = lat, onValueChange = { lat = it }, label = { Text("Lat") }, modifier = Modifier.weight(1f))
        TextField(value = lng, onValueChange = { lng = it }, label = { Text("Lng") }, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun Icon(imageVector: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String?, size: androidx.compose.ui.unit.Dp) {
    Icon(imageVector, contentDescription = contentDescription, modifier = Modifier.size(size))
}
