package `in`.mahato.bytetools.ui.tools.network

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL

data class DnsRecord(
    val type: String,
    val value: String,
    val ttl: Int
)

data class WhoisData(
    val domain: String,
    val registrar: String,
    val createdDate: String,
    val expiresDate: String,
    val updatedDate: String,
    val status: String,
    val nameservers: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnsLookupScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var domainInput by rememberSaveable { mutableStateOf("google.com") }
    var selectedType by rememberSaveable { mutableStateOf("ALL") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    val dnsRecords = remember { mutableStateListOf<DnsRecord>() }
    var whoisResult by remember { mutableStateOf<WhoisData?>(null) }

    val recordTypes = listOf("ALL", "A", "AAAA", "MX", "TXT", "NS", "CNAME", "WHOIS")

    fun performLookup() {
        val domain = domainInput.trim().removePrefix("https://").removePrefix("http://").removeSuffix("/")
        if (domain.isBlank()) {
            errorMessage = "Please enter a valid domain name"
            return
        }

        focusManager.clearFocus()
        isLoading = true
        errorMessage = null
        dnsRecords.clear()
        whoisResult = null

        scope.launch {
            if (selectedType == "WHOIS") {
                queryWhoisRdap(
                    domain = domain,
                    onSuccess = { whois ->
                        whoisResult = whois
                        isLoading = false
                    },
                    onError = { err ->
                        errorMessage = err
                        isLoading = false
                    }
                )
            } else {
                queryDnsRecords(
                    domain = domain,
                    typeFilter = selectedType,
                    onSuccess = { records ->
                        dnsRecords.addAll(records)
                        isLoading = false
                        if (records.isEmpty()) {
                            errorMessage = "No $selectedType records found for $domain"
                        }
                    },
                    onError = { err ->
                        errorMessage = err
                        isLoading = false
                    }
                )
            }
        }
    }

    fun copyReport() {
        val text = buildString {
            appendLine("=== DNS / WHOIS Report for $domainInput ===")
            if (whoisResult != null) {
                val w = whoisResult!!
                appendLine("Domain: ${w.domain}")
                appendLine("Registrar: ${w.registrar}")
                appendLine("Created: ${w.createdDate}")
                appendLine("Expires: ${w.expiresDate}")
                appendLine("Updated: ${w.updatedDate}")
                appendLine("Status: ${w.status}")
                appendLine("Nameservers: ${w.nameservers.joinToString(", ")}")
            } else {
                dnsRecords.forEach { r ->
                    appendLine("[${r.type}] ${r.value} (TTL: ${r.ttl}s)")
                }
            }
        }
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("DNS Report", text))
        Toast.makeText(context, "Copied report to clipboard", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DNS & WHOIS Lookup", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (dnsRecords.isNotEmpty() || whoisResult != null) {
                        IconButton(onClick = { copyReport() }) {
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
            // Domain input field
            OutlinedTextField(
                value = domainInput,
                onValueChange = {
                    domainInput = it
                    errorMessage = null
                },
                label = { Text("Domain Name") },
                placeholder = { Text("e.g. google.com or github.com") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { performLookup() }, enabled = !isLoading) {
                        Icon(Icons.Default.Search, contentDescription = "Lookup")
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { performLookup() })
            )

            // Record Type Selector Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(recordTypes) { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = {
                            selectedType = type
                            performLookup()
                        },
                        label = { Text(type, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            // Error banner if any
            if (errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Loading state
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator()
                        Text("Querying $selectedType records...", style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else if (whoisResult != null) {
                // WHOIS Result Card
                val whois = whoisResult!!
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Domain, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("WHOIS Registration Data", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider()

                        WhoisRow("Domain", whois.domain, isBold = true)
                        WhoisRow("Registrar", whois.registrar)
                        WhoisRow("Created Date", whois.createdDate)
                        WhoisRow("Expires Date", whois.expiresDate)
                        WhoisRow("Last Updated", whois.updatedDate)
                        WhoisRow("Domain Status", whois.status)
                        if (whois.nameservers.isNotEmpty()) {
                            WhoisRow("Nameservers", whois.nameservers.joinToString("\n"))
                        }
                    }
                }
            } else {
                // DNS Records List
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Resolved Records (${dnsRecords.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (dnsRecords.isEmpty() && errorMessage == null) {
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
                                    Icon(Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.height(10.dp))
                                    Text("Lookup Any Domain DNS", fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Enter a domain name and choose a record type (A, AAAA, MX, TXT, NS, or WHOIS) to inspect its DNS setup.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        items(dnsRecords) { record ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = getDnsTypeColor(record.type),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            record.type,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            record.value,
                                            fontWeight = FontWeight.Medium,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            "TTL: ${record.ttl}s",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WhoisRow(label: String, value: String, isBold: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium)
    }
}

private fun getDnsTypeColor(type: String): Color {
    return when (type) {
        "A" -> Color(0xFF1976D2) // Blue
        "AAAA" -> Color(0xFF7B1FA2) // Purple
        "MX" -> Color(0xFFF57C00) // Orange
        "TXT" -> Color(0xFF388E3C) // Green
        "NS" -> Color(0xFF0097A7) // Cyan
        "CNAME" -> Color(0xFFD32F2F) // Red
        else -> Color(0xFF455A64) // Slate
    }
}

private suspend fun queryDnsRecords(
    domain: String,
    typeFilter: String,
    onSuccess: (List<DnsRecord>) -> Unit,
    onError: (String) -> Unit
) = withContext(Dispatchers.IO) {
    val typesToQuery = if (typeFilter == "ALL") {
        listOf("A", "AAAA", "MX", "TXT", "NS", "CNAME")
    } else {
        listOf(typeFilter)
    }

    val collected = mutableListOf<DnsRecord>()

    try {
        for (type in typesToQuery) {
            val url = URL("https://dns.google/resolve?name=$domain&type=$type")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 4000
            connection.readTimeout = 4000
            connection.requestMethod = "GET"

            if (connection.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val json = JSONObject(reader.readText())
                reader.close()

                val answers = json.optJSONArray("Answer")
                if (answers != null) {
                    for (i in 0 until answers.length()) {
                        val item = answers.getJSONObject(i)
                        val typeNum = item.optInt("type", 1)
                        val data = item.optString("data", "").trim('"')
                        val ttl = item.optInt("TTL", 300)
                        val typeStr = dnsTypeNumToString(typeNum, type)
                        collected.add(DnsRecord(typeStr, data, ttl))
                    }
                }
            }
            connection.disconnect()
        }

        if (collected.isEmpty()) {
            // Local fallback using InetAddress for A records
            try {
                val addrs = InetAddress.getAllByName(domain)
                for (a in addrs) {
                    val isV6 = a.hostAddress?.contains(":") == true
                    collected.add(
                        DnsRecord(
                            type = if (isV6) "AAAA" else "A",
                            value = a.hostAddress ?: "",
                            ttl = 300
                        )
                    )
                }
            } catch (_: Exception) {}
        }

        withContext(Dispatchers.Main) {
            onSuccess(collected)
        }
    } catch (e: Exception) {
        // Fallback to InetAddress if DoH fails
        try {
            val addrs = InetAddress.getAllByName(domain)
            for (a in addrs) {
                val isV6 = a.hostAddress?.contains(":") == true
                collected.add(
                    DnsRecord(
                        type = if (isV6) "AAAA" else "A",
                        value = a.hostAddress ?: "",
                        ttl = 300
                    )
                )
            }
            withContext(Dispatchers.Main) {
                onSuccess(collected)
            }
        } catch (_: Exception) {
            withContext(Dispatchers.Main) {
                onError("Failed to lookup DNS for $domain: ${e.localizedMessage}")
            }
        }
    }
}

private suspend fun queryWhoisRdap(
    domain: String,
    onSuccess: (WhoisData) -> Unit,
    onError: (String) -> Unit
) = withContext(Dispatchers.IO) {
    try {
        val url = URL("https://rdap.org/domain/$domain")
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/rdap+json")

        if (connection.responseCode in 200..299) {
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val json = JSONObject(reader.readText())
            reader.close()

            val domainName = json.optString("ldhName", domain)
            var registrar = "Unknown"
            val entities = json.optJSONArray("entities")
            if (entities != null) {
                for (i in 0 until entities.length()) {
                    val ent = entities.getJSONObject(i)
                    val roles = ent.optJSONArray("roles")
                    if (roles != null) {
                        for (j in 0 until roles.length()) {
                            if (roles.getString(j) == "registrar") {
                                registrar = ent.optString("vcardArray", ent.optString("handle", "Registrar"))
                                val vcards = ent.optJSONArray("vcardArray")
                                if (vcards != null && vcards.length() > 1) {
                                    val properties = vcards.getJSONArray(1)
                                    for (k in 0 until properties.length()) {
                                        val prop = properties.getJSONArray(k)
                                        if (prop.getString(0) == "fn") {
                                            registrar = prop.getString(3)
                                            break
                                        }
                                    }
                                }
                                break
                            }
                        }
                    }
                }
            }

            var created = "—"
            var expires = "—"
            var updated = "—"

            val events = json.optJSONArray("events")
            if (events != null) {
                for (i in 0 until events.length()) {
                    val ev = events.getJSONObject(i)
                    when (ev.optString("eventAction")) {
                        "registration" -> created = ev.optString("eventDate").take(10)
                        "expiration" -> expires = ev.optString("eventDate").take(10)
                        "last changed", "last update" -> updated = ev.optString("eventDate").take(10)
                    }
                }
            }

            val statuses = mutableListOf<String>()
            val statusArr = json.optJSONArray("status")
            if (statusArr != null) {
                for (i in 0 until statusArr.length()) {
                    statuses.add(statusArr.getString(i))
                }
            }

            val nameservers = mutableListOf<String>()
            val nsArr = json.optJSONArray("nameservers")
            if (nsArr != null) {
                for (i in 0 until nsArr.length()) {
                    val ns = nsArr.getJSONObject(i).optString("ldhName")
                    if (ns.isNotEmpty()) nameservers.add(ns)
                }
            }

            val whoisData = WhoisData(
                domain = domainName,
                registrar = registrar,
                createdDate = created,
                expiresDate = expires,
                updatedDate = updated,
                status = if (statuses.isNotEmpty()) statuses.joinToString(", ") else "Active",
                nameservers = nameservers
            )

            withContext(Dispatchers.Main) {
                onSuccess(whoisData)
            }
        } else {
            withContext(Dispatchers.Main) {
                onError("WHOIS lookup returned HTTP ${connection.responseCode} for $domain")
            }
        }
        connection.disconnect()
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            onError("WHOIS lookup failed: ${e.localizedMessage}")
        }
    }
}

private fun dnsTypeNumToString(num: Int, fallback: String): String {
    return when (num) {
        1 -> "A"
        28 -> "AAAA"
        15 -> "MX"
        16 -> "TXT"
        2 -> "NS"
        5 -> "CNAME"
        6 -> "SOA"
        12 -> "PTR"
        else -> fallback
    }
}
