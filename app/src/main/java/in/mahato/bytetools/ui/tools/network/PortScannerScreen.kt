package `in`.mahato.bytetools.ui.tools.network

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.net.InetSocketAddress
import java.net.Socket

data class PortResult(
    val port: Int,
    val serviceName: String,
    val isOpen: Boolean,
    val responseTimeMs: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortScannerScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var targetHost by rememberSaveable { mutableStateOf("192.168.1.1") }
    var scanMode by rememberSaveable { mutableIntStateOf(0) } // 0: Common Ports, 1: Custom Range
    var startPortText by rememberSaveable { mutableStateOf("20") }
    var endPortText by rememberSaveable { mutableStateOf("100") }

    var isScanning by remember { mutableStateOf(false) }
    var scannedCount by remember { mutableIntStateOf(0) }
    var totalToScan by remember { mutableIntStateOf(0) }
    var scanJob by remember { mutableStateOf<Job?>(null) }

    val results = remember { mutableStateListOf<PortResult>() }

    val commonPorts = remember {
        listOf(
            21 to "FTP (File Transfer)",
            22 to "SSH (Secure Shell)",
            23 to "Telnet",
            25 to "SMTP (Email)",
            53 to "DNS (Domain Name)",
            80 to "HTTP (Web Server)",
            110 to "POP3 (Email)",
            135 to "Microsoft RPC",
            139 to "NetBIOS",
            143 to "IMAP (Email)",
            443 to "HTTPS (Secure Web)",
            445 to "SMB (File Sharing)",
            993 to "IMAPS (Secure Email)",
            995 to "POP3S (Secure Email)",
            1433 to "Microsoft SQL Server",
            1521 to "Oracle DB",
            3306 to "MySQL Database",
            3389 to "RDP (Remote Desktop)",
            5432 to "PostgreSQL Database",
            5900 to "VNC Remote Access",
            6379 to "Redis Database",
            8080 to "HTTP Alternate / Proxy",
            8443 to "HTTPS Alternate",
            27017 to "MongoDB Database"
        )
    }

    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        isScanning = false
    }

    fun startScan() {
        if (targetHost.isBlank() || isScanning) return
        focusManager.clearFocus()
        results.clear()
        scannedCount = 0

        val portsToScan: List<Pair<Int, String>> = if (scanMode == 0) {
            commonPorts
        } else {
            val start = startPortText.toIntOrNull()?.coerceIn(1, 65535) ?: 1
            val end = endPortText.toIntOrNull()?.coerceIn(start, 65535) ?: 100
            (start..end).map { it to (commonPorts.find { p -> p.first == it }?.second ?: "Unknown Service") }
        }

        totalToScan = portsToScan.size
        isScanning = true

        scanJob = scope.launch(Dispatchers.IO) {
            val semaphore = Semaphore(20) // 20 concurrent probes
            val host = targetHost.trim()

            val jobs = portsToScan.map { (port, service) ->
                launch {
                    semaphore.withPermit {
                        if (!isActive) return@withPermit
                        val start = System.currentTimeMillis()
                        var open = false
                        try {
                            val socket = Socket()
                            socket.connect(InetSocketAddress(host, port), 300)
                            socket.close()
                            open = true
                        } catch (_: Exception) {}

                        val time = maxOf(1L, System.currentTimeMillis() - start)
                        if (open) {
                            withContext(Dispatchers.Main) {
                                results.add(PortResult(port, service, true, time))
                                results.sortBy { it.port }
                            }
                        }
                        withContext(Dispatchers.Main) {
                            scannedCount++
                        }
                    }
                }
            }

            jobs.joinAll()
            withContext(Dispatchers.Main) {
                isScanning = false
                Toast.makeText(context, "Scan complete: Found ${results.size} open ports", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun copyResults() {
        val text = buildString {
            appendLine("=== Port Scan Report for $targetHost ===")
            appendLine("Open Ports: ${results.size}")
            results.forEach { r ->
                appendLine("Port ${r.port} [OPEN] - ${r.serviceName} (${r.responseTimeMs} ms)")
            }
        }
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("Port Scan", text))
        Toast.makeText(context, "Copied port scan report", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Port Scanner", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        stopScan()
                        navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (results.isNotEmpty()) {
                        IconButton(onClick = { copyResults() }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Report")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Target Input
            OutlinedTextField(
                value = targetHost,
                onValueChange = { if (!isScanning) targetHost = it },
                label = { Text("Target Host / IP") },
                placeholder = { Text("e.g. 192.168.1.1 or scanme.nmap.org") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isScanning,
                leadingIcon = { Icon(Icons.Default.Lan, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done)
            )

            // Scan Mode Selection
            TabRow(selectedTabIndex = scanMode) {
                Tab(
                    selected = scanMode == 0,
                    onClick = { if (!isScanning) scanMode = 0 },
                    text = { Text("Common Ports (${commonPorts.size})") }
                )
                Tab(
                    selected = scanMode == 1,
                    onClick = { if (!isScanning) scanMode = 1 },
                    text = { Text("Custom Range") }
                )
            }

            if (scanMode == 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = startPortText,
                        onValueChange = { if (!isScanning) startPortText = it },
                        label = { Text("Start Port") },
                        modifier = Modifier.weight(1f),
                        enabled = !isScanning,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = endPortText,
                        onValueChange = { if (!isScanning) endPortText = it },
                        label = { Text("End Port") },
                        modifier = Modifier.weight(1f),
                        enabled = !isScanning,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }

            // Start / Stop Scan Button
            Button(
                onClick = {
                    if (isScanning) stopScan() else startScan()
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isScanning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = if (isScanning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isScanning) "Stop Scanning ($scannedCount / $totalToScan)" else "Scan Ports",
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Progress Bar
            if (isScanning && totalToScan > 0) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = scannedCount / totalToScan.toFloat(),
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Probing ports...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${(scannedCount * 100 / totalToScan)}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Results Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Open Ports (${results.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            // Results List
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (results.isEmpty() && !isScanning) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(10.dp))
                                Text("No Open Ports Detected Yet", fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Tap 'Scan Ports' to detect active network services and security ports on this host.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(results, key = { it.port }) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFE8F5E9)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "${item.port}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(item.serviceName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text("Port ${item.port} • TCP", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Badge(containerColor = Color(0xFF4CAF50)) {
                                    Text("OPEN", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
