package `in`.mahato.bytetools.ui.tools.network

import android.content.Context
import android.net.ConnectivityManager
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.math.abs
import kotlin.math.max

data class PingRecord(val sequence: Int, val latencyMs: Float, val isSuccess: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PingTesterScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var targetHost by rememberSaveable { mutableStateOf("8.8.8.8") }
    var isPinging by remember { mutableStateOf(false) }

    val history = remember { mutableStateListOf<PingRecord>() }
    var packetsSent by remember { mutableIntStateOf(0) }
    var packetsReceived by remember { mutableIntStateOf(0) }
    var minLatency by remember { mutableFloatStateOf(Float.MAX_VALUE) }
    var maxLatency by remember { mutableFloatStateOf(0f) }
    var sumLatency by remember { mutableFloatStateOf(0f) }
    var currentJitter by remember { mutableFloatStateOf(0f) }

    val presets = listOf(
        "Google (8.8.8.8)" to "8.8.8.8",
        "Cloudflare (1.1.1.1)" to "1.1.1.1",
        "OpenDNS" to "208.67.222.222",
        "Google.com" to "google.com"
    )

    fun resetStats() {
        history.clear()
        packetsSent = 0
        packetsReceived = 0
        minLatency = Float.MAX_VALUE
        maxLatency = 0f
        sumLatency = 0f
        currentJitter = 0f
    }

    fun startPing() {
        if (targetHost.isBlank() || isPinging) return
        focusManager.clearFocus()
        resetStats()
        isPinging = true

        scope.launch {
            var seq = 1
            var prevLatency: Float? = null

            while (isActive && isPinging) {
                packetsSent++
                val latency = performSinglePing(targetHost)

                if (latency != null && latency >= 0f) {
                    packetsReceived++
                    sumLatency += latency
                    if (latency < minLatency) minLatency = latency
                    if (latency > maxLatency) maxLatency = latency

                    if (prevLatency != null) {
                        currentJitter = (currentJitter * 0.8f) + (abs(latency - prevLatency) * 0.2f)
                    }
                    prevLatency = latency

                    history.add(0, PingRecord(seq, latency, true))
                } else {
                    history.add(0, PingRecord(seq, 0f, false))
                }

                // Keep maximum 40 points for graph and list
                if (history.size > 40) {
                    history.removeAt(history.lastIndex)
                }

                seq++
                delay(1000)
            }
        }
    }

    fun stopPing() {
        isPinging = false
    }

    val avgLatency = if (packetsReceived > 0) sumLatency / packetsReceived else 0f
    val packetLoss = if (packetsSent > 0) ((packetsSent - packetsReceived).toFloat() / packetsSent * 100f) else 0f

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ping & Latency Tester", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            // Target Host Input
            OutlinedTextField(
                value = targetHost,
                onValueChange = { if (!isPinging) targetHost = it },
                label = { Text("Target IP / Hostname") },
                placeholder = { Text("e.g. 8.8.8.8 or google.com") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isPinging,
                leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { startPing() })
            )

            // Preset Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(presets) { (label, host) ->
                    FilterChip(
                        selected = targetHost == host,
                        onClick = {
                            if (!isPinging) {
                                targetHost = host
                            }
                        },
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }

            // Start / Stop Action Button
            Button(
                onClick = {
                    if (isPinging) stopPing() else startPing()
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPinging) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = if (isPinging) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(if (isPinging) "Stop Ping" else "Start Ping Test", fontWeight = FontWeight.SemiBold)
            }

            // Live Latency & Jitter Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    StatBox(
                        label = "Current",
                        value = if (history.isNotEmpty() && history[0].isSuccess) "${history[0].latencyMs.toInt()} ms" else "—",
                        color = getLatencyColor(history.firstOrNull()?.latencyMs ?: 0f)
                    )
                    StatBox(label = "Avg", value = if (packetsReceived > 0) "${avgLatency.toInt()} ms" else "—")
                    StatBox(label = "Jitter", value = if (packetsReceived > 1) "${currentJitter.toInt()} ms" else "—")
                    StatBox(
                        label = "Loss",
                        value = "${packetLoss.toInt()}%",
                        color = if (packetLoss > 10f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Live Ping Real-Time Graph
            Card(
                modifier = Modifier.fillMaxWidth().height(160.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    Text("Latency Chart (Last 30 Pings)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxSize()) {
                        PingCanvas(history = history.take(30).reversed())
                    }
                }
            }

            // History Log List
            Text(
                "Ping History Logs",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (history.isEmpty()) {
                    item {
                        Text(
                            "Press 'Start Ping Test' to begin real-time diagnosis.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                } else {
                    items(history) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("#${item.sequence} to $targetHost", style = MaterialTheme.typography.bodySmall)
                                if (item.isSuccess) {
                                    Text(
                                        "${item.latencyMs.toInt()} ms",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = getLatencyColor(item.latencyMs)
                                    )
                                } else {
                                    Text("Request Timeout", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
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
fun StatBox(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun PingCanvas(history: List<PingRecord>) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Draw horizontal grid lines (0, 100, 200 ms)
        val maxScale = 250f
        val stepY = height / 3f
        drawLine(gridColor, Offset(0f, 0f), Offset(width, 0f), strokeWidth = 1f)
        drawLine(gridColor, Offset(0f, stepY), Offset(width, stepY), strokeWidth = 1f)
        drawLine(gridColor, Offset(0f, stepY * 2), Offset(width, stepY * 2), strokeWidth = 1f)
        drawLine(gridColor, Offset(0f, height), Offset(width, height), strokeWidth = 1f)

        if (history.size < 2) return@Canvas

        val stepX = width / (max(history.size - 1, 1).toFloat())
        val path = Path()
        val fillPath = Path()

        var firstPoint = true
        history.forEachIndexed { index, record ->
            val x = index * stepX
            val lat = if (record.isSuccess) record.latencyMs.coerceIn(0f, maxScale) else maxScale
            val y = height - (lat / maxScale * height)

            if (firstPoint) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
                firstPoint = false
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        fillPath.lineTo(width, height)
        fillPath.close()

        // Draw gradient fill
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(primaryColor.copy(alpha = 0.35f), Color.Transparent)
            )
        )

        // Draw stroke line
        drawPath(
            path = path,
            color = primaryColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

private fun getLatencyColor(ms: Float): Color {
    return when {
        ms <= 0f -> Color.Gray
        ms < 60f -> Color(0xFF2E7D32) // Green
        ms < 120f -> Color(0xFF1976D2) // Blue
        ms < 220f -> Color(0xFFF57C00) // Orange
        else -> Color(0xFFD32F2F) // Red
    }
}

private suspend fun performSinglePing(host: String): Float? = withContext(Dispatchers.IO) {
    // 1. Try ICMP ping command
    try {
        val process = Runtime.getRuntime().exec("ping -c 1 -W 2 $host")
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        var line: String?
        var timeMs: Float? = null

        while (reader.readLine().also { line = it } != null) {
            val l = line.orEmpty()
            if (l.contains("time=") || l.contains("time<")) {
                val token = l.substringAfter("time=").substringAfter("time<").substringBefore(" ms").trim()
                timeMs = token.toFloatOrNull()
                break
            }
        }
        process.waitFor()
        reader.close()
        if (timeMs != null) return@withContext timeMs
    } catch (_: Exception) {}

    // 2. Fallback to TCP Socket Connect timing
    try {
        val start = System.nanoTime()
        val socket = Socket()
        // Try DNS port 53 or HTTPS 443
        val port = if (host.contains("8.8.8.8") || host.contains("1.1.1.1")) 53 else 443
        socket.connect(InetSocketAddress(host, port), 2000)
        val elapsed = (System.nanoTime() - start) / 1_000_000f
        socket.close()
        return@withContext elapsed
    } catch (_: Exception) {}

    null
}
