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
