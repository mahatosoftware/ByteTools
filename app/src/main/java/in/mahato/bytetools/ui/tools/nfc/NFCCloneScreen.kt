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
                readData = (state as NfcState.Success).data.removePrefix("Cloned Data:\n")
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
