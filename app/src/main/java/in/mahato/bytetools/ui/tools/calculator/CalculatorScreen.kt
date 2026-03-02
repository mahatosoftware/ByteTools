package `in`.mahato.bytetools.ui.tools.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    navController: NavController,
    viewModel: CalculatorViewModel = hiltViewModel()
) {
    val expression by viewModel.expression.collectAsState()
    val result by viewModel.result.collectAsState()
    val history by viewModel.history.collectAsState(initial = emptyList())
    var showHistory by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Calculator", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showHistory = !showHistory }) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            if (showHistory) {
                HistoryList(history)
            } else {
                Display(expression, result)
                Keypad(viewModel)
            }
        }
    }
}

@Composable
fun ColumnScope.Display(expression: String, result: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(24.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.End
    ) {
        Text(expression, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.End)
        Spacer(modifier = Modifier.height(8.dp))
        Text(result, style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.End)
    }
}

@Composable
fun Keypad(viewModel: CalculatorViewModel) {
    val buttons = listOf(
        listOf("C", "÷", "×", "DEL"),
        listOf("7", "8", "9", "-"),
        listOf("4", "5", "6", "+"),
        listOf("1", "2", "3", "="),
        listOf("0", ".", "(", ")") // Simplified
    )

    Column(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp)) {
        buttons.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { char ->
                    CalcButton(char, modifier = Modifier.weight(1f)) {
                        when (char) {
                            "C" -> viewModel.onClear()
                            "=" -> viewModel.onCalculate()
                            "DEL" -> { /* drop last */ }
                            "+", "-", "×", "÷" -> viewModel.onOperator(char)
                            else -> viewModel.onDigit(char)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalcButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = modifier.aspectRatio(1.2f).padding(4.dp),
        colors = ButtonDefaults.textButtonColors(
            containerColor = if (text == "=") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            contentColor = if (text == "=") Color.White else MaterialTheme.colorScheme.onSurface
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(text, fontSize = 24.sp)
    }
}

@Composable
fun HistoryList(history: List<`in`.mahato.bytetools.domain.model.CalcResult>) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(history) { item ->
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text(item.expression, style = MaterialTheme.typography.bodyMedium)
                Text(item.result, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Divider(modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}
