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
