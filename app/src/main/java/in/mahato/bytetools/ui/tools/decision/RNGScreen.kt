package `in`.mahato.bytetools.ui.tools.decision

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RNGScreen(
    navController: NavController,
    viewModel: DecisionViewModel = hiltViewModel()
) {
    var minStr by remember { mutableStateOf("1") }
    var maxStr by remember { mutableStateOf("100") }
    var countStr by remember { mutableStateOf("1") }
    var unique by remember { mutableStateOf(true) }

    val results by viewModel.rngResults.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Random Number Generator") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().height(150.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    if (results.isEmpty()) {
                        Text("?", fontSize = 80.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    } else if (results.size == 1) {
                        Text("${results[0]}", fontSize = 80.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, contentPadding = PaddingValues(16.dp)) {
                            items(results) { num ->
                                Text("$num", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            OutlinedTextField(
                value = minStr,
                onValueChange = { minStr = it },
                label = { Text("Minimum") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = maxStr,
                onValueChange = { maxStr = it },
                label = { Text("Maximum") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = countStr,
                onValueChange = { countStr = it },
                label = { Text("Count") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Unique Numbers", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.weight(1f))
                Switch(checked = unique, onCheckedChange = { unique = it })
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    val min = minStr.toIntOrNull() ?: 1
                    val max = maxStr.toIntOrNull() ?: 100
                    val count = countStr.toIntOrNull() ?: 1
                    viewModel.generateNumbers(min, max, count, unique)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Default.Numbers, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("GENERATE", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
