package `in`.mahato.bytetools.ui.tools.nfc

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import `in`.mahato.bytetools.nfc.*
import `in`.mahato.bytetools.domain.model.ScanResult
import `in`.mahato.bytetools.ui.tools.qr.QRScannerViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NFCScannerHistoryScreen(navController: NavController, viewModel: QRScannerViewModel = hiltViewModel()) {
    val history by viewModel.scanHistory.collectAsState()
    val nfcHistory = history.filter { it.format == "NFC" || it.format == "NFC_RECORD" }
    
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Scanner History") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (nfcHistory.isEmpty()) {
                Spacer(modifier = Modifier.height(100.dp))
                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))
                Text("No History Yet", style = MaterialTheme.typography.titleMedium)
                Text("Your scanned NFC tags will theoretically appear here after you save them.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            } else {
                val groupedHistory = remember(nfcHistory) {
                    val groups = mutableListOf<List<ScanResult>>()
                    var currentGroup = mutableListOf<ScanResult>()
                    
                    val sortedItems = nfcHistory.sortedByDescending { it.timestamp }
                    
                    for (item in sortedItems) {
                        if (currentGroup.isEmpty()) {
                            currentGroup.add(item)
                        } else {
                            val timeDiff = kotlin.math.abs(item.timestamp - currentGroup.first().timestamp)
                            if (timeDiff <= 2000) {
                                currentGroup.add(item)
                            } else {
                                groups.add(currentGroup.toList())
                                currentGroup = mutableListOf(item)
                            }
                        }
                    }
                    if (currentGroup.isNotEmpty()) {
                        groups.add(currentGroup.toList())
                    }
                    groups
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(groupedHistory) { group ->
                         NfcGroupedHistoryItem(group, onDelete = { scan -> viewModel.deleteScanResult(scan.id) }, context, clipboardManager)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NfcGroupedHistoryItem(group: List<ScanResult>, onDelete: (ScanResult) -> Unit, context: android.content.Context, clipboardManager: androidx.compose.ui.platform.ClipboardManager) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            val firstItem = group.firstOrNull() ?: return@Column
            val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(firstItem.timestamp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("NFC Scan • $dateStr", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = { group.forEach { onDelete(it) } }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Group", tint = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            group.forEachIndexed { index, scan ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )
                }
                NfcHistoryItemContent(scan, context, clipboardManager)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NfcHistoryItemContent(scan: ScanResult, context: android.content.Context, clipboardManager: androidx.compose.ui.platform.ClipboardManager) {
    var showDetailsDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
                val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(scan.timestamp))

                if (scan.format == "NFC_RECORD") {
                    val recordParts = scan.content.split("::::", limit = 2)
                    val recordType = try { RecordType.valueOf(recordParts[0]) } catch(e: Exception) { null }
                    
                    if (recordType != null) {
                        val recordData = recordParts.getOrNull(1) ?: ""
                        val parsedRecord = ParsedNdefRecord(recordType, recordData)
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(getRecordIcon(recordType), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${recordType.name} Action", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Text(dateStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(recordData, style = MaterialTheme.typography.bodyMedium, maxLines = 4, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        val actions = getRecordActions(context, clipboardManager, parsedRecord)
                        
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp), 
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            actions.forEach { action ->
                                FilledTonalButton(onClick = action.second) {
                                    Text(action.first)
                                }
                            }
                            FilledTonalButton(onClick = { showDetailsDialog = true }) {
                                Text("View Details")
                            }
                        }
                    } else {
                        Text("Unknown Record", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(dateStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(scan.content, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                } else {
                    Text("NFC Metadata", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(dateStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(scan.content, style = MaterialTheme.typography.bodyMedium, maxLines = 6, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Backwards compatibility for raw text records
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(onClick = { 
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(scan.content))
                            android.widget.Toast.makeText(context, "Copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                        }) {
                            Text("Copy Raw Data")
                        }
                        FilledTonalButton(onClick = { showDetailsDialog = true }) {
                            Text("View Details")
                        }
                    }
                }
            }

    if (showDetailsDialog) {
        val title = if (scan.format == "NFC_RECORD") {
            try {
                scan.content.split("::::", limit = 2)[0] + " Record"
            } catch (e: Exception) {
                "Unknown Record"
            }
        } else {
            "NFC Metadata"
        }
    
        val contentText = if (scan.format == "NFC_RECORD") {
            scan.content.split("::::", limit = 2).getOrNull(1) ?: scan.content
        } else {
            scan.content
        }
    
        AlertDialog(
            onDismissRequest = { showDetailsDialog = false },
            title = {
                Text(text = title, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(text = contentText, style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetailsDialog = false }) {
                    Text("Close")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(contentText))
                    android.widget.Toast.makeText(context, "Copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                }) {
                    Text("Copy")
                }
            }
        )
    }
}
