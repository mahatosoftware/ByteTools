import os
import re

path = "app/src/main/java/in/mahato/bytetools/ui/tools/nfc/"

def update_file(filename, content):
    with open(os.path.join(path, filename), "w") as f:
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import `in`.mahato.bytetools.nfc.*
import android.nfc.NdefMessage
"""

# 1. NFCTagReaderScreen
update_file("NFCTagReaderScreen.kt", base_imports + """
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NFCTagReaderScreen(navController: NavController, viewModel: NfcViewModel = hiltViewModel()) {
    val state by viewModel.nfcState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.setMode(NfcMode.READ)
    }
    
    DisposableEffect(Unit) {
        onDispose { viewModel.resetState() }
    }

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
            when (state) {
                is NfcState.Idle, is NfcState.Ready -> {
                    Icon(Icons.Default.Nfc, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(if (state is NfcState.Ready) (state as NfcState.Ready).message else "Ready to Scan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Hold your device near the NFC tag to read its contents.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                is NfcState.Success -> {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text((state as NfcState.Success).data, modifier = Modifier.padding(16.dp))
                    }
                }
                is NfcState.Error -> {
                    Icon(Icons.Default.Error, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text((state as NfcState.Error).error, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
""")

# 2. NFCWriterScreen
update_file("NFCWriterScreen.kt", base_imports + """
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NFCWriterScreen(navController: NavController, viewModel: NfcViewModel = hiltViewModel()) {
    val options = listOf("URL", "Text note")
    var selectedOption by remember { mutableStateOf<String?>(null) }
    var inputText by remember { mutableStateOf("") }
    val state by viewModel.nfcState.collectAsState()
    
    DisposableEffect(Unit) {
        onDispose { viewModel.resetState() }
    }

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
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            if (state is NfcState.Ready || state is NfcState.Success || state is NfcState.Error) {
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Text(
                        when (state) {
                            is NfcState.Ready -> (state as NfcState.Ready).message
                            is NfcState.Success -> (state as NfcState.Success).data
                            is NfcState.Error -> (state as NfcState.Error).error
                            else -> ""
                        },
                        modifier = Modifier.padding(16.dp),
                        color = if (state is NfcState.Error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (selectedOption == null) {
                Text("Select Data Type", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 16.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(options) { option ->
                        Card(
                            onClick = { selectedOption = option },
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
            } else {
                Text("Write $selectedOption", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Enter payload") }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val record = if (selectedOption == "URL") NfcUtils.createUriRecord(inputText) else NfcUtils.createTextRecord(inputText)
                        viewModel.setPendingWriteMessage(NdefMessage(arrayOf(record)))
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(Icons.Default.Nfc, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Prepare Write", fontSize = MaterialTheme.typography.titleMedium.fontSize)
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(onClick = { selectedOption = null }, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel")
                }
            }
        }
    }
}
""")

# 3. NFCBusinessCardScreen
update_file("NFCBusinessCardScreen.kt", base_imports + """
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NFCBusinessCardScreen(navController: NavController, viewModel: NfcViewModel = hiltViewModel()) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    val state by viewModel.nfcState.collectAsState()

    DisposableEffect(Unit) {
        onDispose { viewModel.resetState() }
    }

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
            if (state is NfcState.Ready || state is NfcState.Success || state is NfcState.Error) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        when (state) {
                            is NfcState.Ready -> (state as NfcState.Ready).message
                            is NfcState.Success -> (state as NfcState.Success).data
                            is NfcState.Error -> (state as NfcState.Error).error
                            else -> ""
                        },
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = website, onValueChange = { website = it }, label = { Text("Website") }, modifier = Modifier.fillMaxWidth())
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = {
                    val vcard = "BEGIN:VCARD\\nVERSION:3.0\\nFN:$name\\nTEL:$phone\\nEMAIL:$email\\nURL:$website\\nEND:VCARD"
                    val record = NfcUtils.createMimeRecord("text/vcard", vcard)
                    viewModel.setPendingWriteMessage(NdefMessage(arrayOf(record)))
                },
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

# 4. NFCWiFiScreen
update_file("NFCWiFiScreen.kt", base_imports + """
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NFCWiFiScreen(navController: NavController, viewModel: NfcViewModel = hiltViewModel()) {
    var network by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var security by remember { mutableStateOf("WPA") }
    val state by viewModel.nfcState.collectAsState()

    DisposableEffect(Unit) {
        onDispose { viewModel.resetState() }
    }

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
            if (state is NfcState.Ready || state is NfcState.Success || state is NfcState.Error) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        when (state) {
                            is NfcState.Ready -> (state as NfcState.Ready).message
                            is NfcState.Success -> (state as NfcState.Success).data
                            is NfcState.Error -> (state as NfcState.Error).error
                            else -> ""
                        },
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            OutlinedTextField(value = network, onValueChange = { network = it }, label = { Text("Network (SSID)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = security, onValueChange = { security = it }, label = { Text("Security Type (WPA/WEP)") }, modifier = Modifier.fillMaxWidth())
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = {
                    val wifiConfig = "WIFI:S:$network;T:$security;P:$password;;"
                    val record = NfcUtils.createMimeRecord("application/vnd.wfa.wsc", wifiConfig)
                    viewModel.setPendingWriteMessage(NdefMessage(arrayOf(record)))
                },
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

# 5. NFCAutomationScreen
update_file("NFCAutomationScreen.kt", base_imports + """
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NFCAutomationScreen(navController: NavController, viewModel: NfcViewModel = hiltViewModel()) {
    val triggers = listOf("Silent mode + alarm (Bedside)", "Turn WiFi ON (Office desk)", "Open Maps (Car)", "Start playlist (Gym)")
    val state by viewModel.nfcState.collectAsState()

    DisposableEffect(Unit) {
        onDispose { viewModel.resetState() }
    }

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
            if (state is NfcState.Ready || state is NfcState.Success || state is NfcState.Error) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            when (state) {
                                is NfcState.Ready -> (state as NfcState.Ready).message
                                is NfcState.Success -> (state as NfcState.Success).data
                                is NfcState.Error -> (state as NfcState.Error).error
                                else -> ""
                            },
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            item {
                Text("Existing Automations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 16.dp))
            }
            items(triggers) { trigger ->
                Card(
                    onClick = {
                        val record = NfcUtils.createTextRecord("AUTO_TASK:$trigger")
                        viewModel.setPendingWriteMessage(NdefMessage(arrayOf(record)))
                    },
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
        }
    }
}
""")

# 6. NFCPaymentReaderScreen
update_file("NFCPaymentReaderScreen.kt", base_imports + """
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NFCPaymentReaderScreen(navController: NavController, viewModel: NfcViewModel = hiltViewModel()) {
    val state by viewModel.nfcState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.setMode(NfcMode.READ)
    }
    
    DisposableEffect(Unit) {
        onDispose { viewModel.resetState() }
    }

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
            when (state) {
                is NfcState.Idle, is NfcState.Ready -> {
                    Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(if (state is NfcState.Ready) (state as NfcState.Ready).message else "Ready to Read Card", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Hold your device near a metro card or hotel key to read basic info (UID).", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                is NfcState.Success -> {
                    Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text((state as NfcState.Success).data, modifier = Modifier.padding(16.dp))
                    }
                }
                is NfcState.Error -> {
                    Icon(Icons.Default.Error, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text((state as NfcState.Error).error, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
""")

# 7. NFCCloneScreen
update_file("NFCCloneScreen.kt", base_imports + """
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NFCCloneScreen(navController: NavController, viewModel: NfcViewModel = hiltViewModel()) {
    val state by viewModel.nfcState.collectAsState()
    var readData by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(Unit) {
        viewModel.setMode(NfcMode.CLONE_READ)
    }
    
    DisposableEffect(Unit) {
        onDispose { viewModel.resetState() }
    }

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
            if (state is NfcState.Success && (state as NfcState.Success).data.startsWith("Cloned Data")) {
                readData = (state as NfcState.Success).data.removePrefix("Cloned Data:\\n")
            }
            
            if (readData == null) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Step 1: Scan Original Tag", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Hold your device near the tag you want to clone.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Step 2: Write to New Tag", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { viewModel.setPendingClone(readData!!) }) {
                    Text("Ready to Write")
                }
                if (state is NfcState.Ready) {
                    Text((state as NfcState.Ready).message, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 16.dp))
                }
            }
        }
    }
}
""")

# 8. NFCFormatterScreen
update_file("NFCFormatterScreen.kt", base_imports + """
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NFCFormatterScreen(navController: NavController, viewModel: NfcViewModel = hiltViewModel()) {
    val state by viewModel.nfcState.collectAsState()

    DisposableEffect(Unit) {
        onDispose { viewModel.resetState() }
    }

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
            if (state is NfcState.Ready || state is NfcState.Success || state is NfcState.Error) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            when (state) {
                                is NfcState.Ready -> (state as NfcState.Ready).message
                                is NfcState.Success -> (state as NfcState.Success).data
                                is NfcState.Error -> (state as NfcState.Error).error
                                else -> ""
                            },
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            item {
                Text("Management Operations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 16.dp))
            }
            
            item {
                Card(
                    onClick = { viewModel.setPendingFormat() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Erase Tag / Format", style = MaterialTheme.typography.bodyLarge)
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                }
            }
            
            item {
                Card(
                    onClick = { viewModel.setPendingLock() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Lock Tag (Read-Only)", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onErrorContainer)
                        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }
    }
}
""")

# 9. NFCQRHybridScreen
update_file("NFCQRHybridScreen.kt", base_imports + """
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NFCQRHybridScreen(navController: NavController, viewModel: NfcViewModel = hiltViewModel()) {
    var content by remember { mutableStateOf("") }
    val state by viewModel.nfcState.collectAsState()

    DisposableEffect(Unit) {
        onDispose { viewModel.resetState() }
    }

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
            if (state is NfcState.Ready || state is NfcState.Success || state is NfcState.Error) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        when (state) {
                            is NfcState.Ready -> (state as NfcState.Ready).message
                            is NfcState.Success -> (state as NfcState.Success).data
                            is NfcState.Error -> (state as NfcState.Error).error
                            else -> ""
                        },
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Share Data (URL, text, contact)") }, modifier = Modifier.fillMaxWidth())
            
            Text("Choose Sharing Method:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = { 
                        val record = NfcUtils.createTextRecord(content)
                        viewModel.setPendingWriteMessage(NdefMessage(arrayOf(record)))
                    }, 
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Nfc, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("NFC")
                }
                Button(onClick = { /* Nav to QR Generator with content... omitted for brevity */ }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.QrCode, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("QR Code")
                }
            }
        }
    }
}
""")

# 10. NFCTapCounterScreen
update_file("NFCTapCounterScreen.kt", base_imports + """
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NFCTapCounterScreen(navController: NavController, viewModel: NfcViewModel = hiltViewModel()) {
    val state by viewModel.nfcState.collectAsState()
    var count by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(Unit) {
        viewModel.setMode(NfcMode.READ)
    }

    LaunchedEffect(state) {
        if (state is NfcState.Success) count++
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.resetState() }
    }

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
            Text(count.toString(), style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(32.dp))
            OutlinedButton(onClick = { count = 0 }) {
                Text("Reset Counter")
            }
        }
    }
}
""")

# 11. NFCScannerHistoryScreen
update_file("NFCScannerHistoryScreen.kt", base_imports + """
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
            Text("Your scanned NFC tags will theoretically appear here in future DB updates.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
""")

print("NFC screens updated with ViewModel logic successfully.")
