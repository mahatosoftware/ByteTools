package `in`.mahato.bytetools.ui.tools.common

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.provider.Settings
import android.provider.ContactsContract
import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import `in`.mahato.bytetools.ui.tools.barcode.BarcodeViewModel
import `in`.mahato.bytetools.ui.tools.qr.QRGeneratorViewModel
import `in`.mahato.bytetools.ui.tools.qr.shareBitmap
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CodeHistoryScreen(
    navController: NavController,
    barcodeViewModel: BarcodeViewModel = hiltViewModel(),
    qrViewModel: QRGeneratorViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedKeys by remember { mutableStateOf(setOf<String>()) }
    val tabs = listOf("Scanned", "Generated")
    val selectionMode = selectedKeys.isNotEmpty()

    val scannedHistory by barcodeViewModel.scanHistory.collectAsState()
    val generatedHistory by qrViewModel.qrHistory.collectAsState()
    val scannedItems = scannedHistory.map { HistoryItemData(it.id.toLong(), it.content, it.format, it.timestamp, true) }
    val generatedItems = generatedHistory.map {
        HistoryItemData(
            id = it.id,
            content = it.content,
            label = generatedCodeLabel(it),
            timestamp = it.timestamp,
            isScanned = false,
            rawType = it.barcodeFormat ?: it.type
        )
    }
    val currentItems = if (selectedTab == 0) scannedItems else generatedItems

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (selectionMode) "${selectedKeys.size} selected" else "Code History",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (selectionMode) {
                        IconButton(
                            onClick = {
                                currentItems.filter { it.selectionKey in selectedKeys }.forEach { item ->
                                    if (item.isScanned) {
                                        barcodeViewModel.deleteScan(item.id.toInt())
                                    } else {
                                        generatedHistory.firstOrNull { it.id == item.id }?.let(qrViewModel::deleteHistory)
                                    }
                                }
                                selectedKeys = emptySet()
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Selected")
                        }
                        IconButton(onClick = { selectedKeys = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Selection")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                            selectedKeys = emptySet()
                        },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> HistoryList(
                    items = scannedItems,
                    selectionMode = selectionMode,
                    selectedKeys = selectedKeys,
                    onToggleSelection = { key ->
                        selectedKeys = if (key in selectedKeys) selectedKeys - key else selectedKeys + key
                    },
                    onDelete = { id -> barcodeViewModel.deleteScan(id.toInt()) }
                )
                1 -> HistoryList(
                    items = generatedItems,
                    selectionMode = selectionMode,
                    selectedKeys = selectedKeys,
                    onToggleSelection = { key ->
                        selectedKeys = if (key in selectedKeys) selectedKeys - key else selectedKeys + key
                    },
                    onDelete = { id -> qrViewModel.deleteHistory(generatedHistory.first { it.id == id }) }
                )
            }
        }
    }
}

data class HistoryItemData(
    val id: Long,
    val content: String,
    val label: String,
    val timestamp: Long,
    val isScanned: Boolean,
    val rawType: String = label
) {
    val selectionKey: String = "${if (isScanned) "scanned" else "generated"}:$id"
}

private fun generatedCodeLabel(record: `in`.mahato.bytetools.domain.model.QRRecord): String {
    return if (!record.barcodeFormat.isNullOrBlank() || record.type.equals("BARCODE", ignoreCase = true)) {
        "Barcode"
    } else {
        "QR"
    }
}

@Composable
fun HistoryList(
    items: List<HistoryItemData>,
    selectionMode: Boolean,
    selectedKeys: Set<String>,
    onToggleSelection: (String) -> Unit,
    onDelete: (Long) -> Unit
) {
    if (items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                Spacer(Modifier.height(16.dp))
                Text("No history yet", color = Color.Gray)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items) { item ->
                HistoryCard(
                    item = item,
                    selectionMode = selectionMode,
                    isSelected = item.selectionKey in selectedKeys,
                    onToggleSelection = { onToggleSelection(item.selectionKey) },
                    onDelete = onDelete
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun HistoryCard(
    item: HistoryItemData,
    selectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onDelete: (Long) -> Unit
) {
    val context = LocalContext.current
    var previewBitmap by remember(item.id) { mutableStateOf<Bitmap?>(null) }
    val actions = remember(item.content, item.isScanned, item.rawType) {
        buildHistoryActions(
            context = context,
            item = item,
            onPreview = { previewBitmap = it }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (selectionMode) onToggleSelection()
                },
                onLongClick = onToggleSelection
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelection() }
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Icon(
                    if (item.isScanned) Icons.Default.QrCodeScanner else Icons.Default.QrCode,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.content,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "${item.label} • ${SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(item.timestamp))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
                if (!selectionMode) {
                    IconButton(onClick = { onDelete(item.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                    }
                }
            }

            if (!selectionMode) {
                Spacer(Modifier.height(12.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    actions.forEach { action ->
                        FilledTonalButton(onClick = action.onClick) {
                            Icon(action.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(action.label)
                        }
                    }
                }
            }
        }
    }

    previewBitmap?.let { bitmap ->
        AlertDialog(
            onDismissRequest = { previewBitmap = null },
            confirmButton = {
                TextButton(onClick = { previewBitmap = null }) {
                    Text("Close")
                }
            },
            title = { Text(if (item.label == "QR") "QR Code" else "Barcode") },
            text = {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Generated ${item.label}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (item.label == "QR") 260.dp else 180.dp)
                )
            }
        )
    }
}

private data class HistoryAction(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val onClick: () -> Unit
)

private fun buildHistoryActions(
    context: Context,
    item: HistoryItemData,
    onPreview: (Bitmap) -> Unit
): List<HistoryAction> {
    val actions = mutableListOf<HistoryAction>()
    val content = item.content
    val trimmedContent = content.trim()

    if (!item.isScanned) {
        buildGeneratedCodeBitmap(item)?.let { bitmap ->
            actions += HistoryAction(
                label = if (item.label == "QR") "View QR" else "View Barcode",
                icon = if (item.label == "QR") Icons.Default.QrCode else Icons.Default.QrCodeScanner
            ) {
                onPreview(bitmap)
            }
            actions += HistoryAction("Share", Icons.Default.Share) {
                shareBitmap(context, bitmap)
            }
        }
    } else {
        extractLink(content)?.let { link ->
            actions += HistoryAction("Open Link", Icons.Default.Link) {
                launchIntent(
                    context = context,
                    intent = Intent(Intent.ACTION_VIEW, Uri.parse(link)),
                    errorMessage = "Could not open link"
                )
            }
        }

        if (isVCard(trimmedContent)) {
            actions += HistoryAction("Save Contact", Icons.Default.PersonAdd) {
                saveContactFromVCard(context, trimmedContent)
            }
        }

        extractEmailAddress(trimmedContent)?.let { email ->
            actions += HistoryAction("Email", Icons.Default.Email) {
                launchIntent(
                    context = context,
                    intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${Uri.encode(email)}")),
                    errorMessage = "Could not open email app"
                )
            }
        }

        extractSmsUri(trimmedContent)?.let { smsUri ->
            actions += HistoryAction("SMS", Icons.Default.Sms) {
                launchIntent(
                    context = context,
                    intent = Intent(Intent.ACTION_SENDTO, smsUri),
                    errorMessage = "Could not open messaging app"
                )
            }
        }

        extractGeoUri(trimmedContent)?.let { geoUri ->
            actions += HistoryAction("Open Map", Icons.Default.Map) {
                val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
                launchIntent(
                    context = context,
                    intent = Intent.createChooser(mapIntent, "Select map application"),
                    errorMessage = "Could not open map app"
                )
            }
        }

        extractWifiPayload(trimmedContent)?.let { wifiPayload ->
            actions += HistoryAction("Connect Wi-Fi", Icons.Default.Wifi) {
                openWifiSettings(context, wifiPayload)
            }
        }

        actions += HistoryAction("Share", Icons.Default.Share) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, content)
            }
            launchIntent(
                context = context,
                intent = Intent.createChooser(shareIntent, "Share code content"),
                errorMessage = "Could not share content"
            )
        }
    }

    return actions
}

private fun buildGeneratedCodeBitmap(item: HistoryItemData): Bitmap? {
    return runCatching {
        if (item.label == "QR") {
            generateQrBitmap(item.content)
        } else {
            val format = BarcodeFormat.valueOf(item.rawType)
            generateBarcodeBitmap(item.content, format)
        }
    }.getOrNull()
}

private fun generateQrBitmap(content: String, size: Int = 800): Bitmap {
    val hints = mapOf(
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
        EncodeHintType.MARGIN to 1
    )
    val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
    val pixels = IntArray(size * size)
    for (y in 0 until size) {
        val offset = y * size
        for (x in 0 until size) {
            pixels[offset + x] = if (bitMatrix.get(x, y)) AndroidColor.BLACK else AndroidColor.WHITE
        }
    }
    return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, size, 0, 0, size, size)
    }
}

private fun generateBarcodeBitmap(content: String, format: BarcodeFormat, width: Int = 1200, height: Int = 500): Bitmap {
    val bitMatrix: BitMatrix = MultiFormatWriter().encode(content, format, width, height)
    return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
        for (x in 0 until width) {
            for (y in 0 until height) {
                setPixel(x, y, if (bitMatrix.get(x, y)) AndroidColor.BLACK else AndroidColor.WHITE)
            }
        }
    }
}

private fun extractLink(content: String): String? {
    val trimmed = content.trim()
    if (trimmed.isBlank()) return null

    val candidate = when {
        trimmed.startsWith("http://", ignoreCase = true) -> trimmed
        trimmed.startsWith("https://", ignoreCase = true) -> trimmed
        trimmed.startsWith("www.", ignoreCase = true) -> "https://$trimmed"
        else -> null
    } ?: return null

    val uri = Uri.parse(candidate)
    return if (!uri.host.isNullOrBlank()) candidate else null
}

private fun extractEmailAddress(content: String): String? {
    val trimmed = content.trim()
    val email = if (trimmed.startsWith("mailto:", ignoreCase = true)) {
        trimmed.substringAfter(':', "").substringBefore("?").trim()
    } else {
        trimmed
    }

    return email.takeIf { Patterns.EMAIL_ADDRESS.matcher(it).matches() }
}

private fun isVCard(content: String): Boolean {
    return content.startsWith("BEGIN:VCARD", ignoreCase = true)
}

private fun extractSmsUri(content: String): Uri? {
    val trimmed = content.trim()
    return when {
        trimmed.startsWith("smsto:", ignoreCase = true) -> Uri.parse("smsto:${trimmed.substringAfter(':')}")
        trimmed.startsWith("sms:", ignoreCase = true) -> Uri.parse("smsto:${trimmed.substringAfter(':')}")
        else -> null
    }
}

private fun extractGeoUri(content: String): Uri? {
    val trimmed = content.trim()
    if (!trimmed.startsWith("geo:", ignoreCase = true)) return null

    val geoPayload = trimmed.substringAfter(':').trim()
    val location = geoPayload.substringBefore('?').trim()
    val parts = location.split(",")
    if (parts.size < 2) return null

    val latitude = parts[0].toDoubleOrNull()
    val longitude = parts[1].toDoubleOrNull()
    if (latitude == null || longitude == null) return null

    val extraQuery = geoPayload.substringAfter('?', missingDelimiterValue = "").trim()
    val label = Uri.decode(
        extraQuery
            .substringAfter("q=", missingDelimiterValue = "")
            .substringBefore('&')
            .takeIf { it.isNotBlank() }
            ?: "$latitude,$longitude"
    )

    return Uri.parse("geo:0,0?q=${Uri.encode("$latitude,$longitude ($label)")}")
}

private fun extractWifiPayload(content: String): String? {
    return content.takeIf { it.startsWith("WIFI:", ignoreCase = true) }
}

private fun openWifiSettings(context: Context, wifiPayload: String) {
    val ssid = Regex("(?i)(?:^|;)S:([^;]*)").find(wifiPayload)?.groupValues?.getOrNull(1)?.trim()
    val password = Regex("(?i)(?:^|;)P:([^;]*)").find(wifiPayload)?.groupValues?.getOrNull(1)?.trim()

    launchIntent(
        context = context,
        intent = Intent(Settings.ACTION_WIFI_SETTINGS),
        errorMessage = "Could not open Wi-Fi settings"
    )

    when {
        !ssid.isNullOrBlank() && !password.isNullOrBlank() ->
            Toast.makeText(context, "Connect to $ssid. Password: $password", Toast.LENGTH_LONG).show()
        !ssid.isNullOrBlank() ->
            Toast.makeText(context, "Connect to $ssid in Wi-Fi settings", Toast.LENGTH_LONG).show()
        else ->
            Toast.makeText(context, "Wi-Fi settings opened", Toast.LENGTH_SHORT).show()
    }
}

private fun saveContactFromVCard(context: Context, vCard: String) {
    val name = Regex("(?im)^FN:(.+)$").find(vCard)?.groupValues?.getOrNull(1)?.trim()
    val phone = Regex("(?im)^TEL[^:]*:(.+)$").find(vCard)?.groupValues?.getOrNull(1)?.trim()
    val email = Regex("(?im)^EMAIL[^:]*:(.+)$").find(vCard)?.groupValues?.getOrNull(1)?.trim()

    val intent = Intent(Intent.ACTION_INSERT).apply {
        type = ContactsContract.Contacts.CONTENT_TYPE
        if (!name.isNullOrBlank()) putExtra(ContactsContract.Intents.Insert.NAME, name)
        if (!phone.isNullOrBlank()) putExtra(ContactsContract.Intents.Insert.PHONE, phone)
        if (!email.isNullOrBlank()) putExtra(ContactsContract.Intents.Insert.EMAIL, email)
    }

    launchIntent(context, intent, "Could not save contact")
}

private fun launchIntent(context: Context, intent: Intent, errorMessage: String) {
    runCatching {
        context.startActivity(intent)
    }.onFailure {
        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
    }
}
