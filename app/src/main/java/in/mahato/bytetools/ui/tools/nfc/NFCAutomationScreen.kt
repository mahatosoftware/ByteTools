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
