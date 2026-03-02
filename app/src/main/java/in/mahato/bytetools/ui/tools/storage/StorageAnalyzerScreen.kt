package `in`.mahato.bytetools.ui.tools.storage

import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.io.File
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageAnalyzerScreen(navController: NavController) {
    val context = LocalContext.current
    val storageData = remember { getStorageData(context) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storage Analyzer") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            StorageHeader(storageData)
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Storage Summary", style = MaterialTheme.typography.titleMedium)
                Divider()
                StorageDetailItem("Internal Storage", storageData.totalInternal, storageData.usedInternal)
            }
        }
    }
}

@Composable
fun StorageHeader(data: StorageData) {
    val usedPercentage = (data.usedInternal.toFloat() / data.totalInternal.toFloat()).coerceIn(0f, 1f)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total Storage", style = MaterialTheme.typography.labelMedium)
                    Text(formatSize(data.totalInternal), style = MaterialTheme.typography.headlineMedium)
                }
                Text("${(usedPercentage * 100).toInt()}% Used", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LinearProgressIndicator(
                progress = usedPercentage,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp)),
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Used: ${formatSize(data.usedInternal)}", style = MaterialTheme.typography.bodySmall)
                Text("Free: ${formatSize(data.freeInternal)}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun StorageDetailItem(label: String, total: Long, used: Long) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Text("${formatSize(used)} / ${formatSize(total)}")
    }
}

data class StorageData(
    val totalInternal: Long,
    val usedInternal: Long,
    val freeInternal: Long
)

fun getStorageData(context: Context): StorageData {
    val internalPath: File = Environment.getDataDirectory()
    val stat = StatFs(internalPath.path)
    val blockSize = stat.blockSizeLong
    val totalBlocks = stat.blockCountLong
    val availableBlocks = stat.availableBlocksLong
    
    val total = totalBlocks * blockSize
    val free = availableBlocks * blockSize
    val used = total - free
    
    return StorageData(total, used, free)
}

fun formatSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return DecimalFormat("#,##0.#").format(size / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
}
