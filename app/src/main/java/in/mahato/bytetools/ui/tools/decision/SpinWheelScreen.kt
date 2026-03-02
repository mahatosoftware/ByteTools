package `in`.mahato.bytetools.ui.tools.decision

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpinWheelScreen(
    navController: NavController,
    viewModel: DecisionViewModel = hiltViewModel()
) {
    val options by viewModel.wheelOptions.collectAsState()
    var rotationAngle by remember { mutableStateOf(0f) }
    
    val animatedRotation = animateFloatAsState(
        targetValue = rotationAngle,
        animationSpec = tween(
            durationMillis = 3000,
            easing = CubicBezierEasing(0.1f, 0.8f, 0.3f, 1f)
        ),
        label = "WheelRotation"
    )

    var winningIndex by remember { mutableStateOf<Int?>(null) }
    var showWinner by remember { mutableStateOf(false) }
    var isSpinning by remember { mutableStateOf(false) }

    val colors = listOf(
        Color(0xFFFF5722), Color(0xFFFFC107), Color(0xFF4CAF50),
        Color(0xFF2196F3), Color(0xFF9C27B0), Color(0xFF673AB7),
        Color(0xFFE91E63), Color(0xFF009688)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Spin the Wheel") },
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Winning Result
            Card(
                modifier = Modifier.fillMaxWidth().height(60.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = if (showWinner && winningIndex != null && options.isNotEmpty()) options[winningIndex!!].name else if (isSpinning) "Spinning..." else "Spin to Win!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // The Wheel
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(300.dp)) {
                if (options.isNotEmpty()) {
                    Canvas(modifier = Modifier.fillMaxSize().rotate(animatedRotation.value)) {
                        val sweepAngle = 360f / options.size
                        options.forEachIndexed { index, option ->
                            drawArc(
                                color = colors[index % colors.size],
                                startAngle = index * sweepAngle,
                                sweepAngle = sweepAngle,
                                useCenter = true,
                                size = Size(size.width, size.height)
                            )
                            
                            // Draw Text on Wheel
                            val angle = (index * sweepAngle + sweepAngle / 2) * PI / 180f
                            val x = (size.width / 2) + (size.width / 3.5f) * cos(angle).toFloat()
                            val y = (size.height / 2) + (size.height / 3.5f) * sin(angle).toFloat()
                            
                            drawContext.canvas.nativeCanvas.drawText(
                                if (option.name.length > 8) option.name.take(6) + ".." else option.name,
                                x,
                                y,
                                android.graphics.Paint().apply {
                                    color = android.graphics.Color.WHITE
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    textSize = 40f
                                    isFakeBoldText = true
                                }
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Add options to start", color = Color.Gray)
                    }
                }
                
                // Static Pointer
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).align(Alignment.TopCenter).rotate(90f),
                    tint = Color.Red
                )
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    if (!isSpinning && options.isNotEmpty()) {
                        isSpinning = true
                        showWinner = false
                        val randomSpins = 5 + Random.nextInt(5)
                        val extraAngle = Random.nextFloat() * 360f
                        rotationAngle += randomSpins * 360f + extraAngle
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.large,
                enabled = options.isNotEmpty() && !isSpinning
            ) {
                Text("SPIN", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(24.dp))

            // Options List Input
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                var newOption by remember { mutableStateOf("") }
                TextField(
                    value = newOption,
                    onValueChange = { newOption = it },
                    label = { Text("Add Option") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                IconButton(onClick = {
                    if (newOption.isNotBlank()) {
                        viewModel.addOption(newOption)
                        newOption = ""
                    }
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }

            LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(top = 8.dp)) {
                itemsIndexed(options) { index, option ->
                    ListItem(
                        headlineContent = { Text(option.name) },
                        trailingContent = {
                            IconButton(onClick = { viewModel.removeOption(option) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    )
                }
            }
        }
    }
    
    // Animation completion check
    LaunchedEffect(animatedRotation.value) {
        if (animatedRotation.value == rotationAngle && isSpinning) {
            isSpinning = false
            // Calculate result only after animation stops
            val finalAngleNormalized = (rotationAngle % 360 + 360) % 360
            val targetAngle = (270 - finalAngleNormalized + 360) % 360
            val sweep = if (options.isNotEmpty()) 360f / options.size else 0f
            if (sweep > 0) {
                winningIndex = (targetAngle / sweep).toInt().coerceIn(0, options.size - 1)
                showWinner = true
            }
        }
    }
}
