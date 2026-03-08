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
