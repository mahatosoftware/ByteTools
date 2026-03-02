package `in`.mahato.bytetools.ui.tools.decision

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
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
fun NamePickerScreen(
    navController: NavController,
    viewModel: DecisionViewModel = hiltViewModel()
) {
    var namesText by remember { mutableStateOf("") }
    var countStr by remember { mutableStateOf("1") }
    val pickedResults by viewModel.pickedNames.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Random Name Picker") },
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    if (pickedResults.isEmpty()) {
                        Text("Pick a Name!", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, contentPadding = PaddingValues(16.dp)) {
                            items(pickedResults) { name ->
                                Text(name, fontSize = 48.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            OutlinedTextField(
                value = namesText,
                onValueChange = { namesText = it },
                label = { Text("Paste Names (One per line)") },
                modifier = Modifier.fillMaxWidth().weight(1f),
                minLines = 5
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = countStr,
                onValueChange = { countStr = it },
                label = { Text("Number of Winners") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    val names = namesText.split("\n")
                    val count = countStr.toIntOrNull() ?: 1
                    viewModel.pickNames(names, count)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.large,
                enabled = namesText.isNotBlank()
            ) {
                Icon(Icons.Default.Group, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("PICK WINNERS", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
