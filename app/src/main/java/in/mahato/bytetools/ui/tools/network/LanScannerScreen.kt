package `in`.mahato.bytetools.ui.tools.network

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.ConnectivityManager
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import `in`.mahato.bytetools.ui.navigation.Screen
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket

data class LanDevice(
    val ip: String,
    val hostname: String,
    val isCurrentDevice: Boolean,
    val isGateway: Boolean,
    val responseTimeMs: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanScannerScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var subnetPrefix by rememberSaveable { mutableStateOf("192.168.1.") }
    var localIp by rememberSaveable { mutableStateOf("") }
    var gatewayIp by rememberSaveable { mutableStateOf("") }

    var isScanning by remember { mutableStateOf(false) }
    var progressCount by remember { mutableIntStateOf(0) }
    var scanJob by remember { mutableStateOf<Job?>(null) }

    val discoveredDevices = remember { mutableStateListOf<LanDevice>() }

    LaunchedEffect(Unit) {
        val (ip, gw) = detectSubnetAndGateway(context)
        localIp = ip
        gatewayIp = gw
        if (ip.isNotEmpty() && ip.contains(".")) {
            subnetPrefix = ip.substring(0, ip.lastIndexOf(".") + 1)
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        isScanning = false
    }

    fun startScan() {
        if (isScanning) return
        discoveredDevices.clear()
        progressCount = 0
        isScanning = true

        scanJob = scope.launch(Dispatchers.IO) {
            val semaphore = Semaphore(30) // 30 concurrent pingers
            val prefix = subnetPrefix

            val jobs = (1..254).map { hostNum ->
                launch {
                    semaphore.withPermit {
                        if (!isActive) return@withPermit
                        val testIp = "$prefix$hostNum"
                        val device = probeHost(testIp, localIp, gatewayIp)
                        if (device != null) {
                            withContext(Dispatchers.Main) {
                                discoveredDevices.add(device)
                                discoveredDevices.sortBy { dev ->
                                    val last = dev.ip.substringAfterLast(".").toIntOrNull() ?: 0
                                    if (dev.isGateway) -2 else if (dev.isCurrentDevice) -1 else last
                                }
                            }
                        }
                        withContext(Dispatchers.Main) {
                            progressCount++
                        }
                    }
                }
            }

            jobs.joinAll()
            withContext(Dispatchers.Main) {
                isScanning = false
                Toast.makeText(context, "Scan complete: Found ${discoveredDevices.size} devices", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun copyDevicesList() {
        val text = buildString {
            appendLine("=== LAN Devices on $subnetPrefix* ===")
            discoveredDevices.forEach { dev ->
                val tag = if (dev.isGateway) "[Router/Gateway]" else if (dev.isCurrentDevice) "[This Phone]" else ""
                appendLine("${dev.ip} - ${dev.hostname} $tag (${dev.responseTimeMs} ms)")
            }
        }
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("LAN Devices", text))
        Toast.makeText(context, "Copied ${discoveredDevices.size} devices to clipboard", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WiFi / LAN Scanner", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        stopScan()
                        navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (discoveredDevices.isNotEmpty()) {
                        IconButton(onClick = { copyDevicesList() }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy List")
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
            // Subnet Target Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Router, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Subnet: ${subnetPrefix}1 - 254", fontWeight = FontWeight.Bold)
                        Text(
                            "My IP: ${if (localIp.isNotEmpty()) localIp else "—"} • Gateway: ${if (gatewayIp.isNotEmpty()) gatewayIp else "—"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                    imageVector = if (isScanning) Icons.Default.Stop else Icons.Default.Search,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isScanning) "Stop Scan ($progressCount / 254)" else "Scan Local Network",
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Progress Bar
            if (isScanning) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = progressCount / 254f,
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Scanning IPs...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${(progressCount * 100 / 254)}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Header for Devices List
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Discovered Devices (${discoveredDevices.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            // Devices List
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (discoveredDevices.isEmpty() && !isScanning) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.DevicesOther, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(12.dp))
                                Text("No Scan Performed Yet", fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Tap 'Scan Local Network' to discover all active devices and potential intruders on your WiFi.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(discoveredDevices, key = { it.ip }) { device ->
                        DeviceCard(device = device, navController = navController)
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceCard(device: LanDevice, navController: NavController) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (device.isGateway) Color(0xFFE8F5E9)
            else if (device.isCurrentDevice) Color(0xFFE3F2FD)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (device.isGateway) Color(0xFF4CAF50)
                        else if (device.isCurrentDevice) Color(0xFF2196F3)
                        else MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (device.isGateway) Icons.Default.Router
                    else if (device.isCurrentDevice) Icons.Default.Smartphone
                    else Icons.Default.Devices,
                    contentDescription = null,
                    tint = if (device.isGateway || device.isCurrentDevice) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(device.ip, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                    if (device.isGateway) {
                        Spacer(Modifier.width(6.dp))
                        Badge(containerColor = Color(0xFF2E7D32)) { Text("Gateway", fontSize = 10.sp, color = Color.White) }
                    } else if (device.isCurrentDevice) {
                        Spacer(Modifier.width(6.dp))
                        Badge(containerColor = Color(0xFF1976D2)) { Text("This Phone", fontSize = 10.sp, color = Color.White) }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${device.hostname} • ${device.responseTimeMs} ms",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun detectSubnetAndGateway(context: Context): Pair<String, String> {
    var localIp = ""
    var gatewayIp = ""
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces()?.toList() ?: emptyList()
        for (intf in interfaces) {
            if (intf.isLoopback || !intf.isUp) continue
            val addrs = intf.inetAddresses.toList()
            val v4 = addrs.firstOrNull { it is Inet4Address && !it.isLoopbackAddress }
            if (v4 != null) {
                localIp = v4.hostAddress ?: ""
                break
            }
        }
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val linkProps = cm?.getLinkProperties(cm.activeNetwork)
        gatewayIp = linkProps?.routes?.firstOrNull { it.isDefaultRoute && it.gateway != null }?.gateway?.hostAddress ?: ""
    } catch (_: Exception) {}
    return Pair(localIp, gatewayIp)
}

private suspend fun probeHost(ip: String, localIp: String, gatewayIp: String): LanDevice? = withContext(Dispatchers.IO) {
    val isMe = ip == localIp
    val isGw = ip == gatewayIp

    val startTime = System.currentTimeMillis()
    var isAlive = isMe

    // Fast probe: check common ports (80, 443, 53, 445, 22) or socket
    val testPorts = intArrayOf(80, 443, 53, 445, 22, 8080)
    for (port in testPorts) {
        try {
            val socket = Socket()
            socket.connect(InetSocketAddress(ip, port), 120)
            socket.close()
            isAlive = true
            break
        } catch (_: Exception) {}
    }

    if (!isAlive) {
        try {
            val addr = InetAddress.getByName(ip)
            if (addr.isReachable(150)) {
                isAlive = true
            }
        } catch (_: Exception) {}
    }

    if (isAlive) {
        val latency = maxOf(1L, System.currentTimeMillis() - startTime)
        val hostname = try {
            val h = InetAddress.getByName(ip).hostName
            if (h != ip) h else if (isGw) "Router / Gateway" else if (isMe) "This Android Device" else "Connected Host"
        } catch (_: Exception) {
            if (isGw) "Router / Gateway" else if (isMe) "This Android Device" else "Connected Host"
        }
        return@withContext LanDevice(
            ip = ip,
            hostname = hostname,
            isCurrentDevice = isMe,
            isGateway = isGw,
            responseTimeMs = latency
        )
    }

    null
}
