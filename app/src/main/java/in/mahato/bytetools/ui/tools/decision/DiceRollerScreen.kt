package `in`.mahato.bytetools.ui.tools.decision

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.layout.FlowRow
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DiceRollerScreen(
    navController: NavController,
    viewModel: DecisionViewModel = hiltViewModel()
) {
    val results by viewModel.diceResults.collectAsState()
    var diceCount by remember { mutableIntStateOf(1) }
    var isRolling by remember { mutableStateOf(false) }

    val rotation = animateFloatAsState(
        targetValue = if (isRolling) 360f else 0f,
        animationSpec = if (isRolling) {
            repeatable(
                iterations = 10,
                animation = tween(durationMillis = 100, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        } else {
            tween(durationMillis = 500)
        },
        label = "DiceRotation"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dice Roller") },
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Total: ${if (isRolling) "?" else results.sum()}",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(48.dp))

            FlowRow(
                maxItemsInEachRow = 2,
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                results.forEach { result ->
                    val displayValue = if (isRolling) (1..6).random() else result
                    DiceView(value = displayValue, rotation = rotation.value)
                }
            }

            Spacer(Modifier.height(64.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Number of Dice", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = diceCount.toFloat(),
                    onValueChange = { diceCount = it.toInt() },
                    valueRange = 1f..4f,
                    steps = 2,
                    modifier = Modifier.width(200.dp),
                    enabled = !isRolling
                )
                Text("$diceCount", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    if (!isRolling) {
                        isRolling = true
                        viewModel.rollDice(diceCount)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.large,
                enabled = !isRolling
            ) {
                Icon(Icons.Default.Casino, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("ROLL DICE", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    LaunchedEffect(isRolling) {
        if (isRolling) {
            kotlinx.coroutines.delay(1000)
            isRolling = false
        }
    }
}

@Composable
fun DiceView(value: Int, rotation: Float) {
    Card(
        modifier = Modifier.size(100.dp).rotate(rotation),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
