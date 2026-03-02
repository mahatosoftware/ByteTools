package `in`.mahato.bytetools.ui.tools.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import `in`.mahato.bytetools.ui.tools.barcode.BarcodeViewModel
import `in`.mahato.bytetools.ui.tools.qr.QRGeneratorViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeHistoryScreen(
    navController: NavController,
    barcodeViewModel: BarcodeViewModel = hiltViewModel(),
    qrViewModel: QRGeneratorViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Scanned", "Generated")

    val scannedHistory by barcodeViewModel.scanHistory.collectAsState()
    val generatedHistory by qrViewModel.qrHistory.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Code History", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> HistoryList(
                    items = scannedHistory.map { HistoryItemData(it.id.toLong(), it.content, it.format, it.timestamp, true) },
                    onDelete = { id -> barcodeViewModel.deleteScan(id.toInt()) }
                )
                1 -> HistoryList(
                    items = generatedHistory.map { HistoryItemData(it.id, it.content, it.type, it.timestamp, false) },
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
    val isScanned: Boolean
)

@Composable
fun HistoryList(items: List<HistoryItemData>, onDelete: (Long) -> Unit) {
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
                HistoryCard(item, onDelete)
            }
        }
    }
}

@Composable
fun HistoryCard(item: HistoryItemData, onDelete: (Long) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (item.isScanned) Icons.Default.QrCodeScanner else Icons.Default.QrCode,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.content,
                    maxLines = 1,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "${item.label} • ${SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(item.timestamp))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
            IconButton(onClick = { onDelete(item.id) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
            }
        }
    }
}
