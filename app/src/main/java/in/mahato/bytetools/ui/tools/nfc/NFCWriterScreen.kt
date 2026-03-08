package `in`.mahato.bytetools.ui.tools.nfc

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NFCWriterScreen(navController: NavController, viewModel: NfcViewModel = hiltViewModel()) {
    val options = listOf(
        "URL / Website Link",
        "Plain Text",
        "Phone Number",
        "SMS Message",
        "Email",
        "App Launch",
        "WiFi Network Configuration",
        "GPS Location",
        "Social Media Profile",
        "Payment Link (UPI)",
        "Custom Data (JSON)"
    )
    var selectedOption by remember { mutableStateOf<String?>(null) }
    var inputText by remember { mutableStateOf("") }
    var inputText2 by remember { mutableStateOf("") } // Used for SMS body or Email Subject
    var inputText3 by remember { mutableStateOf("WPA") } // Used for WiFi Encryption
    var isEncryptionExpanded by remember { mutableStateOf(false) } // For WiFi Dropdown
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
                
                when (selectedOption) {
                    "URL / Website Link", "Social Media Profile" -> {
                        OutlinedTextField(value = inputText, onValueChange = { inputText = it }, modifier = Modifier.fillMaxWidth(), label = { Text("https://...") })
                    }
                    "Phone Number" -> {
                        OutlinedTextField(value = inputText, onValueChange = { inputText = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Phone Number (+91...)") })
                    }
                    "SMS Message" -> {
                        OutlinedTextField(value = inputText, onValueChange = { inputText = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Phone Number") })
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = inputText2, onValueChange = { inputText2 = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Message Body") })
                    }
                    "Email" -> {
                        OutlinedTextField(value = inputText, onValueChange = { inputText = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Email Address") })
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = inputText2, onValueChange = { inputText2 = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Subject (Optional)") })
                    }
                    "App Launch" -> {
                        OutlinedTextField(value = inputText, onValueChange = { inputText = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Package Name (e.g. com.whatsapp)") })
                    }
                    "WiFi Network Configuration" -> {
                        OutlinedTextField(value = inputText, onValueChange = { inputText = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Network SSID") })
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = inputText2, onValueChange = { inputText2 = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Password") })
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        ExposedDropdownMenuBox(
                            expanded = isEncryptionExpanded,
                            onExpandedChange = { isEncryptionExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = inputText3,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Encryption") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = isEncryptionExpanded)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = isEncryptionExpanded,
                                onDismissRequest = { isEncryptionExpanded = false }
                            ) {
                                listOf("WPA", "WEP", "None", "AES", "TKIP").forEach { selectionOption ->
                                    DropdownMenuItem(
                                        text = { Text(selectionOption) },
                                        onClick = {
                                            inputText3 = selectionOption
                                            isEncryptionExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    "GPS Location" -> {
                        OutlinedTextField(value = inputText, onValueChange = { inputText = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Latitude,Longitude") })
                    }
                    "Payment Link (UPI)" -> {
                        OutlinedTextField(value = inputText, onValueChange = { inputText = it }, modifier = Modifier.fillMaxWidth(), label = { Text("UPI ID (merchant@upi)") })
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = inputText2, onValueChange = { inputText2 = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Payee Name (Optional)") })
                    }
                    else -> {
                        // Plain Text or Custom Json (uses text record)
                        OutlinedTextField(value = inputText, onValueChange = { inputText = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Enter Text/JSON") })
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val record = when (selectedOption) {
                            "URL / Website Link", "Social Media Profile" -> NfcUtils.createUriRecord(inputText)
                            "Phone Number" -> NfcUtils.createUriRecord("tel:$inputText")
                            "SMS Message" -> NfcUtils.createUriRecord("sms:$inputText?body=${android.net.Uri.encode(inputText2)}")
                            "Email" -> {
                                val uri = if (inputText2.isNotBlank()) "mailto:$inputText?subject=${android.net.Uri.encode(inputText2)}" else "mailto:$inputText"
                                NfcUtils.createUriRecord(uri)
                            }
                            "App Launch" -> {
                                NfcUtils.createTextRecord("package:$inputText") // simple text representation based on prompt
                            }
                            "WiFi Network Configuration" -> {
                                val wifiConfig = "WIFI:S:$inputText;T:$inputText3;P:$inputText2;;"
                                NfcUtils.createMimeRecord("application/vnd.wfa.wsc", wifiConfig)
                            }
                            "GPS Location" -> NfcUtils.createUriRecord("geo:$inputText")
                            "Payment Link (UPI)" -> {
                                val uri = if (inputText2.isNotBlank()) "upi://pay?pa=$inputText&pn=${android.net.Uri.encode(inputText2)}" else "upi://pay?pa=$inputText"
                                NfcUtils.createUriRecord(uri)
                            }
                            else -> NfcUtils.createTextRecord(inputText) // Plain Text & Custom Data
                        }
                        viewModel.setPendingWriteMessage(NdefMessage(arrayOf(record)))
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(Icons.Default.Nfc, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Prepare Write", fontSize = MaterialTheme.typography.titleMedium.fontSize)
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(onClick = { 
                    selectedOption = null
                    inputText = ""
                    inputText2 = ""
                    inputText3 = "WPA"
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel")
                }
            }
        }
    }
}
