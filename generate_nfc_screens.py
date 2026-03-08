import os

path = "/Users/debasish/Developer/MobileApps/ByteTools/app/src/main/java/in/mahato/bytetools/ui/tools/nfc/"

def write_file(name, content):
    with open(os.path.join(path, name), "w") as f:
        f.write(content)

base_imports = """package `in`.mahato.bytetools.ui.tools.nfc

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
"""

# 1. NFC Tag Reader
write_file("NFCTagReaderScreen.kt", base_imports + """
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NFCTagReaderScreen(navController: NavController) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("NFC Tag Reader") },
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
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Nfc, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Ready to Scan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Hold your device near the NFC tag to read its contents.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
""")

# 2. NFC Writer
write_file("NFCWriterScreen.kt", base_imports + """
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NFCWriterScreen(navController: NavController) {
    val options = listOf("URL", "Contact card (vCard)", "WiFi credentials", "Text note", "Phone number", "Email")
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Write NFC Tag") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Select Data Type", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 16.dp))
            }
            items(options) { option ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(option, style = MaterialTheme.typography.bodyLarge)
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                }
            }
        }
    }
}
""")

# 3. NFC Business Card
write_file("NFCBusinessCardScreen.kt", base_imports + """
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NFCBusinessCardScreen(navController: NavController) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("NFC Business Card") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = website, onValueChange = { website = it }, label = { Text("Website") }, modifier = Modifier.fillMaxWidth())
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = { /* Handle write */ },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Default.Nfc, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Write to NFC Tag", fontSize = MaterialTheme.typography.titleMedium.fontSize)
            }
        }
    }
}
""")

# 4. NFC WiFi
write_file("NFCWiFiScreen.kt", base_imports + """
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NFCWiFiScreen(navController: NavController) {
    var network by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var security by remember { mutableStateOf("WPA/WPA2") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("WiFi NFC Tag") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(value = network, onValueChange = { network = it }, label = { Text("Network (SSID)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = security, onValueChange = { security = it }, label = { Text("Security Type") }, modifier = Modifier.fillMaxWidth(), readOnly = true)
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = { /* Handle write */ },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Default.Nfc, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Write to NFC Tag", fontSize = MaterialTheme.typography.titleMedium.fontSize)
            }
        }
    }
}
""")

# 5. NFC Automation
write_file("NFCAutomationScreen.kt", base_imports + """
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NFCAutomationScreen(navController: NavController) {
    val triggers = listOf("Silent mode + alarm (Bedside)", "Turn WiFi ON (Office desk)", "Open Maps (Car)", "Start playlist (Gym)")

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("NFC Automation") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Existing Automations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 16.dp))
            }
            items(triggers) { trigger ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.SmartButton, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(trigger, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { /* Add new */ },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add New Execution", fontSize = MaterialTheme.typography.titleMedium.fontSize)
                }
            }
        }
    }
}
""")

# 6. NFCPaymentReaderScreen
write_file("NFCPaymentReaderScreen.kt", base_imports + """
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NFCPaymentReaderScreen(navController: NavController) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("NFC Card Reader") },
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
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Ready to Read Card", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Hold your device near a metro card or hotel key to read basic info (UID).", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
""")

# 7. NFC Clone
write_file("NFCCloneScreen.kt", base_imports + """
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NFCCloneScreen(navController: NavController) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Clone NFC Tag") },
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
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Step 1: Scan Original Tag", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Hold your device near the tag you want to clone.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
""")

# 8. NFC Formatter
write_file("NFCFormatterScreen.kt", base_imports + """
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NFCFormatterScreen(navController: NavController) {
    val actions = listOf("Erase tag", "Format tag", "Lock tag (read-only)", "Check memory")

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Erase / Format Tag") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Management Operations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 16.dp))
            }
            items(actions) { action ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(action, style = MaterialTheme.typography.bodyLarge)
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                }
            }
        }
    }
}
""")

# 9. NFC QR Hybrid
write_file("NFCQRHybridScreen.kt", base_imports + """
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NFCQRHybridScreen(navController: NavController) {
    var content by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("QR & NFC Share") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Share Data (URL, text, contact)") }, modifier = Modifier.fillMaxWidth())
            
            Text("Choose Sharing Method:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = { /* Handle share */ }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Nfc, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("NFC")
                }
                Button(onClick = { /* Handle share */ }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.QrCode, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("QR Code")
                }
            }
            Button(onClick = { /* Handle share */ }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Link, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Link")
            }
        }
    }
}
""")

# 10. NFC Tap Counter
write_file("NFCTapCounterScreen.kt", base_imports + """
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NFCTapCounterScreen(navController: NavController) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Tap Counter") },
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Text("Total Scans", style = MaterialTheme.typography.titleLarge)
            Text("0", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(32.dp))
            OutlinedButton(onClick = { /* Reset */ }) {
                Text("Reset Counter")
            }
        }
    }
}
""")

# 11. Scanner History
write_file("NFCScannerHistoryScreen.kt", base_imports + """
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NFCScannerHistoryScreen(navController: NavController) {
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
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))
            Text("No History Yet", style = MaterialTheme.typography.titleMedium)
            Text("Your scanned NFC tags will appear here.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
""")

print("Files generated.")
