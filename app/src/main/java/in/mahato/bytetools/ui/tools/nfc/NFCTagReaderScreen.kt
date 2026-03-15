package `in`.mahato.bytetools.ui.tools.nfc

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import `in`.mahato.bytetools.nfc.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NFCTagReaderScreen(navController: NavController, viewModel: NfcViewModel = hiltViewModel()) {
    val state by viewModel.nfcState.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val savedState by viewModel.savedState.collectAsState()
    
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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (state is NfcState.Ready || state is NfcState.Idle) Arrangement.Center else Arrangement.Top
        ) {
            when (val currentState = state) {
                is NfcState.Idle, is NfcState.Ready -> {
                    Icon(Icons.Default.Nfc, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(if (currentState is NfcState.Ready) currentState.message else "Ready to Scan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Hold your device near the NFC tag to read its contents.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                is NfcState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        item {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Button(
                                    onClick = { viewModel.saveNfcScan(currentState.data) },
                                    enabled = !savedState
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = "Save")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (savedState) "Saved to History" else "Save to History")
                                }
                            }
                            
                            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Metadata", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(currentState.data, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                        
                        items(currentState.parsedRecords) { record ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(getRecordIcon(record.type), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(record.type.name.replace("_", " "), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(record.data, style = MaterialTheme.typography.bodyMedium)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    val actions = getRecordActions(context, clipboardManager, record)
                                    @OptIn(ExperimentalLayoutApi::class)
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
                                    }
                                }
                            }
                        }
                    }
                }
                is NfcState.Error -> {
                    Icon(Icons.Default.Error, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(currentState.error, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

fun getRecordIcon(type: RecordType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (type) {
        RecordType.TEXT -> Icons.Default.TextFields
        RecordType.URI -> Icons.Default.Link
        RecordType.VCARD -> Icons.Default.Contacts
        RecordType.WIFI -> Icons.Default.Wifi
        RecordType.APP_LAUNCH -> Icons.Default.Launch
        RecordType.MIME -> Icons.Default.Description
        else -> Icons.Default.Nfc
    }
}

fun getRecordActions(
    context: Context,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager,
    record: ParsedNdefRecord
): List<Pair<String, () -> Unit>> {
    val actions = mutableListOf<Pair<String, () -> Unit>>()
    
    actions.add("Copy" to {
        clipboardManager.setText(AnnotatedString(record.data))
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    })

    when (record.type) {
        RecordType.URI -> {
            actions.add("Open URL" to {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(record.data))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
                }
            })
        }
        RecordType.VCARD -> {
            actions.add("Save Contact" to {
                try {
                    val nameMatch = Regex("FN:(.*?)(?:\n|\r)").find(record.data)
                    val phoneMatch = Regex("TEL.*?:(.*?)(?:\n|\r)").find(record.data)
                    val emailMatch = Regex("EMAIL.*?:(.*?)(?:\n|\r)").find(record.data)
                    
                    val name = nameMatch?.groups?.get(1)?.value?.trim()
                    val phone = phoneMatch?.groups?.get(1)?.value?.trim()
                    val email = emailMatch?.groups?.get(1)?.value?.trim()

                    val intent = Intent(Intent.ACTION_INSERT).apply {
                        type = ContactsContract.Contacts.CONTENT_TYPE
                        if (name != null) putExtra(ContactsContract.Intents.Insert.NAME, name)
                        if (phone != null) putExtra(ContactsContract.Intents.Insert.PHONE, phone)
                        if (email != null) putExtra(ContactsContract.Intents.Insert.EMAIL, email)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Could not save contact", Toast.LENGTH_SHORT).show()
                }
            })
        }
        RecordType.APP_LAUNCH -> {
            actions.add("Launch App" to {
                try {
                    val intent = context.packageManager.getLaunchIntentForPackage(record.data.trim())
                    if (intent != null) {
                        context.startActivity(intent)
                    } else {
                        val playStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${record.data.trim()}"))
                        context.startActivity(playStoreIntent)
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Could not launch app", Toast.LENGTH_SHORT).show()
                }
            })
        }
        RecordType.WIFI -> {
            actions.add("Connect" to {
                try {
                    // Extract SSID using regex matches like typical WIFI:T:WPA;S:MyNetwork;;
                    val ssidMatch = Regex("S:(.*?);").find(record.data)
                    val ssid = ssidMatch?.groups?.get(1)?.value
                    
                    if (ssid != null) {
                        val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
                        context.startActivity(intent)
                        val passMatch = Regex("P:(.*?);").find(record.data)
                        val pass = passMatch?.groups?.get(1)?.value
                        if (pass != null) {
                            clipboardManager.setText(AnnotatedString(pass))
                            Toast.makeText(context, "Copied password. Connect to $ssid in Settings.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Connect to $ssid in Settings.", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(context, "Could not parse WiFi details", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Could not open WiFi settings", Toast.LENGTH_SHORT).show()
                }
            })
        }
        else -> {}
    }
    
    return actions
}
