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
fun NFCBusinessCardScreen(navController: NavController, viewModel: NfcViewModel = hiltViewModel()) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var whatsapp by remember { mutableStateOf("") }
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
            OutlinedTextField(value = whatsapp, onValueChange = { whatsapp = it }, label = { Text("WhatsApp (e.g., 919876543210)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = website, onValueChange = { website = it }, label = { Text("Website") }, modifier = Modifier.fillMaxWidth())
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = {
                    val vcard = java.lang.StringBuilder("BEGIN:VCARD\nVERSION:3.0\nFN:$name")
                    if (phone.isNotBlank()) vcard.append("\nTEL:$phone")
                    if (whatsapp.isNotBlank()) {
                        val cleanWa = whatsapp.replace("[^0-9]".toRegex(), "")
                        vcard.append("\nURL;TYPE=WhatsApp:https://wa.me/$cleanWa")
                    }
                    if (email.isNotBlank()) vcard.append("\nEMAIL:$email")
                    if (website.isNotBlank()) vcard.append("\nURL:$website")
                    vcard.append("\nEND:VCARD")
                    
                    val record = NfcUtils.createMimeRecord("text/vcard", vcard.toString())
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
