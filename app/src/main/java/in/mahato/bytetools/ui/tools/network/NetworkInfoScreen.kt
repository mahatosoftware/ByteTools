package `in`.mahato.bytetools.ui.tools.network

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.RouteInfo
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.NetworkInterface
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkInfoScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isRefreshing by remember { mutableStateOf(false) }
    var networkType by remember { mutableStateOf("Detecting...") }
    var localIpv4 by remember { mutableStateOf("—") }
    var localIpv6 by remember { mutableStateOf("—") }
    var subnetMask by remember { mutableStateOf("—") }
    var defaultGateway by remember { mutableStateOf("—") }
    var dnsServers by remember { mutableStateOf("—") }
    var interfaceName by remember { mutableStateOf("—") }

    // WiFi Specifics
    var isWifiConnected by remember { mutableStateOf(false) }
    var wifiSsid by remember { mutableStateOf("—") }
    var wifiBssid by remember { mutableStateOf("—") }
    var wifiRssi by remember { mutableIntStateOf(0) }
    var wifiLinkSpeed by remember { mutableStateOf("—") }
    var wifiFrequency by remember { mutableStateOf("—") }

    // Public IP & ISP Info
    var publicIp by remember { mutableStateOf("Fetching...") }
    var ispOrg by remember { mutableStateOf("Fetching...") }
    var locationInfo by remember { mutableStateOf("Fetching...") }
    var isPublicInfoLoading by remember { mutableStateOf(true) }

    fun loadLocalNetworkInfo() {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(activeNetwork)
        val linkProps = cm?.getLinkProperties(activeNetwork)

        if (caps == null || !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            networkType = "Disconnected"
            isWifiConnected = false
        } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            networkType = "Wi-Fi Network"
            isWifiConnected = true
        } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            networkType = "Mobile Cellular Data"
            isWifiConnected = false
        } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
            networkType = "Ethernet"
            isWifiConnected = false
        } else {
            networkType = "Connected"
            isWifiConnected = false
        }

        // Interfaces & IPs
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()?.toList() ?: emptyList()
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                val addrs = intf.inetAddresses.toList()
                val v4 = addrs.firstOrNull { it is Inet4Address && !it.isLoopbackAddress }
                if (v4 != null) {
                    localIpv4 = v4.hostAddress ?: "—"
                    interfaceName = intf.displayName ?: intf.name
                    val v6 = addrs.firstOrNull { it is Inet6Address && !it.isLoopbackAddress }
                    localIpv6 = v6?.hostAddress?.substringBefore("%") ?: "—"

                    val intfAddrs = intf.interfaceAddresses
                    val prefix = intfAddrs.firstOrNull { it.address is Inet4Address }?.networkPrefixLength
                    subnetMask = if (prefix != null) prefixToSubnetMask(prefix) else "255.255.255.0"
                    break
                }
            }
        } catch (_: Exception) {}

        // DNS & Gateway from LinkProperties
        if (linkProps != null) {
            val dnsList = linkProps.dnsServers.mapNotNull { it.hostAddress }
            dnsServers = if (dnsList.isNotEmpty()) dnsList.joinToString(", ") else "Default ISP DNS"

            val gateway = linkProps.routes.firstOrNull { it.isDefaultRoute && it.gateway != null }?.gateway?.hostAddress
            defaultGateway = gateway ?: "192.168.1.1"
        }

        // WiFi info
        if (isWifiConnected) {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val info = wm?.connectionInfo
            if (info != null) {
                val rawSsid = info.ssid.replace("\"", "")
                wifiSsid = if (rawSsid != "<unknown ssid>") rawSsid else "Connected Wi-Fi"
                wifiBssid = info.bssid ?: "Hidden"
                wifiRssi = info.rssi
                wifiLinkSpeed = if (info.linkSpeed > 0) "${info.linkSpeed} Mbps" else "—"
                val freq = info.frequency
                wifiFrequency = if (freq > 0) {
                    val band = if (freq >= 5000) "5 GHz" else "2.4 GHz"
                    "$freq MHz ($band)"
                } else "—"
            }
        }
    }

    fun fetchPublicIp() {
        scope.launch {
            isPublicInfoLoading = true
            val res = fetchPublicIpDetails()
            publicIp = res.ip
            ispOrg = res.isp
            locationInfo = res.location
            isPublicInfoLoading = false
        }
    }

    fun refreshAll() {
        scope.launch {
            isRefreshing = true
            loadLocalNetworkInfo()
            fetchPublicIp()
            isRefreshing = false
        }
    }

    LaunchedEffect(Unit) {
        loadLocalNetworkInfo()
        fetchPublicIp()
    }

    fun copySummary() {
        val summary = buildString {
            appendLine("=== ByteTools Network Information ===")
            appendLine("Network Type: $networkType")
            appendLine("Local IPv4: $localIpv4")
            appendLine("Local IPv6: $localIpv6")
            appendLine("Subnet Mask: $subnetMask")
            appendLine("Default Gateway: $defaultGateway")
            appendLine("DNS Servers: $dnsServers")
            appendLine("Interface: $interfaceName")
            if (isWifiConnected) {
                appendLine("WiFi SSID: $wifiSsid")
                appendLine("Signal Strength: $wifiRssi dBm (${calculateSignalQuality(wifiRssi)}%)")
                appendLine("Link Speed: $wifiLinkSpeed")
                appendLine("Frequency: $wifiFrequency")
            }
            appendLine("Public IP: $publicIp")
            appendLine("ISP / Organization: $ispOrg")
            appendLine("Location: $locationInfo")
        }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Network Info", summary))
        Toast.makeText(context, "Network info copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Network & ISP Info", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { copySummary() }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Info")
                    }
                    IconButton(onClick = { refreshAll() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Connection Overview Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isWifiConnected) Icons.Default.Wifi else Icons.Default.Public,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(networkType, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            if (isWifiConnected) wifiSsid else "Active Network Connection",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Public & ISP Info Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Public & ISP Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider()

                    if (isPublicInfoLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(12.dp))
                            Text("Querying public IP & ISP...", style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        InfoRow("Public IP", publicIp, isHighlight = true)
                        InfoRow("ISP / Organization", ispOrg)
                        InfoRow("Location", locationInfo)
                    }
                }
            }

            // Local Network Details Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lan, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(8.dp))
                        Text("Local Network Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider()

                    InfoRow("Local IPv4", localIpv4, isHighlight = true)
                    InfoRow("Subnet Mask", subnetMask)
                    InfoRow("Default Gateway", defaultGateway)
                    InfoRow("DNS Servers", dnsServers)
                    InfoRow("Local IPv6", localIpv6)
                    InfoRow("Network Interface", interfaceName)
                }
            }

            // WiFi Specific Details Section (Only if connected to WiFi)
            if (isWifiConnected) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Wifi, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(Modifier.width(8.dp))
                            Text("Wi-Fi Specifics", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider()

                        InfoRow("SSID", wifiSsid)
                        InfoRow("BSSID / MAC", wifiBssid)
                        InfoRow("Signal (RSSI)", "$wifiRssi dBm (${calculateSignalQuality(wifiRssi)}% quality)")
                        InfoRow("Link Speed", wifiLinkSpeed)
                        InfoRow("Frequency", wifiFrequency)
                    }
                }
            }

            // Quick Copy Button
            Button(
                onClick = { copySummary() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Copy All Network Details")
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, isHighlight: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Medium,
            color = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun prefixToSubnetMask(prefix: Short): String {
    val shift = 0xffffffffL shl (32 - prefix)
    return "${(shift shr 24) and 0xff}.${(shift shr 16) and 0xff}.${(shift shr 8) and 0xff}.${shift and 0xff}"
}

private fun calculateSignalQuality(rssi: Int): Int {
    return when {
        rssi <= -100 -> 0
        rssi >= -50 -> 100
        else -> 2 * (rssi + 100)
    }.coerceIn(0, 100)
}

data class PublicIpResult(
    val ip: String,
    val isp: String,
    val location: String
)

private suspend fun fetchPublicIpDetails(): PublicIpResult = withContext(Dispatchers.IO) {
    try {
        val url = URL("https://ipapi.co/json/")
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 4000
        connection.readTimeout = 4000
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "ByteTools-NetworkApp")

        if (connection.responseCode == 200) {
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = reader.readText()
            reader.close()
            connection.disconnect()
            val json = JSONObject(response)

            val ip = json.optString("ip", "Unavailable")
            val isp = json.optString("org", json.optString("asn", "Unknown ISP"))
            val city = json.optString("city", "")
            val region = json.optString("region", "")
            val country = json.optString("country_name", "")
            val loc = listOf(city, region, country).filter { it.isNotEmpty() }.joinToString(", ")
            return@withContext PublicIpResult(ip, isp, if (loc.isNotEmpty()) loc else "—")
        }
        connection.disconnect()
    } catch (_: Exception) {}

    // Fallback to simple ipify
    try {
        val url = URL("https://api.ipify.org?format=json")
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 3000
        connection.readTimeout = 3000
        if (connection.responseCode == 200) {
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val res = reader.readText()
            reader.close()
            connection.disconnect()
            val json = JSONObject(res)
            val ip = json.optString("ip", "Unavailable")
            return@withContext PublicIpResult(ip, "Connected Network", "—")
        }
        connection.disconnect()
    } catch (_: Exception) {}

    PublicIpResult("Offline / Unavailable", "—", "—")
}
