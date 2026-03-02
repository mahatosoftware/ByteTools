package `in`.mahato.bytetools.ui.tools.decision

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinFlipScreen(
    navController: NavController,
    viewModel: DecisionViewModel = hiltViewModel()
) {
    val result by viewModel.coinResult.collectAsState()
    var isFlipping by remember { mutableStateOf(false) }

    val rotation = animateFloatAsState(
        targetValue = if (isFlipping) 360f else 0f,
        animationSpec = repeatable(
            iterations = 4,
            animation = tween(durationMillis = 150),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CoinRotation"
    )

    val scale = animateFloatAsState(
        targetValue = if (isFlipping) 1.5f else 1f,
        animationSpec = tween(durationMillis = 300),
        label = "CoinScale"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Coin Flip") },
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
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp).scale(scale.value).rotate(rotation.value)) {
                Surface(
                    color = Color(0xFFFFD700), // Gold
                    shape = CircleShape,
                    modifier = Modifier.fillMaxSize(),
                    shadowElevation = 12.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (isFlipping) "?" else (result?.take(1) ?: "?"),
                            fontSize = 80.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFB8860B)
                        )
                    }
                }
            }

            Spacer(Modifier.height(64.dp))

            Text(
                text = if (isFlipping) "Flipping..." else (result ?: "Flip the Coin!"),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(64.dp))

            Button(
                onClick = {
                    if (!isFlipping) {
                        isFlipping = true
                        viewModel.flipCoin()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.large,
                enabled = !isFlipping
            ) {
                Icon(Icons.Default.MonetizationOn, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("FLIP COIN", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    LaunchedEffect(isFlipping) {
        if (isFlipping) {
            kotlinx.coroutines.delay(1000) // Increase delay to match animation feel
            isFlipping = false
        }
    }
}
