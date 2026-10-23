package `in`.mahato.bytetools.ui.tools.audio

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import `in`.mahato.bytetools.ui.navigation.Screen
import java.io.File

data class AudioToolItem(
    val name: String,
    val description: String,
    val icon: ImageVector,
    val route: String,
    val bgColor: Color,
    val accentColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioDashboardScreen(navController: NavController) {
    val context = LocalContext.current
    var historyCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        val dir = File(context.getExternalFilesDir(null), "ByteToolsAudio")
        if (dir.exists()) {
            historyCount = dir.listFiles()?.count { it.isFile && (it.extension in listOf("mp3", "m4a", "wav", "aac")) } ?: 0
        }
    }

    val tools = listOf(
        AudioToolItem(
            name = "Audio Converter",
            description = "Convert WAV, M4A to MP3",
            icon = Icons.Default.Transform,
            route = Screen.AudioConverter.route,
            bgColor = Color(0xFFFCE4EC),
            accentColor = Color(0xFFE91E63)
        ),
        AudioToolItem(
            name = "Audio Cutter",
            description = "Trim audio files precisely",
            icon = Icons.Default.ContentCut,
            route = Screen.AudioCutter.route,
            bgColor = Color(0xFFEDE7F6),
            accentColor = Color(0xFF673AB7)
        ),
        AudioToolItem(
            name = "Ringtone Maker",
            description = "Create & set phone ringtone",
            icon = Icons.Default.NotificationsActive,
            route = Screen.RingtoneMaker.route,
            bgColor = Color(0xFFFFF8E1),
            accentColor = Color(0xFFFFB300)
        ),
        AudioToolItem(
            name = "Audio Recorder",
            description = "Record high-quality voice",
            icon = Icons.Default.Mic,
            route = Screen.AudioRecorder.route,
            bgColor = Color(0xFFFFEBEE),
            accentColor = Color(0xFFE53935)
        ),
        AudioToolItem(
            name = "Audio Merger",
            description = "Combine multiple tracks",
            icon = Icons.Default.Merge,
            route = Screen.AudioMerger.route,
            bgColor = Color(0xFFE3F2FD),
            accentColor = Color(0xFF2196F3)
        ),
        AudioToolItem(
            name = "Compressor",
            description = "Reduce audio file size",
            icon = Icons.Default.Compress,
            route = Screen.AudioCompressor.route,
            bgColor = Color(0xFFE8F5E9),
            accentColor = Color(0xFF4CAF50)
        ),
        AudioToolItem(
            name = "Video to MP3",
            description = "Extract audio from video",
            icon = Icons.Default.VideoFile,
            route = Screen.VideoToAudio.route,
            bgColor = Color(0xFFFFF3E0),
            accentColor = Color(0xFFFF9800)
        ),
        AudioToolItem(
            name = "Audio History",
            description = "Saved files, share & delete",
            icon = Icons.Default.History,
            route = Screen.AudioHistory.route,
            bgColor = Color(0xFFF1F8E9),
            accentColor = Color(0xFF8BC34A)
        )
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Audio & MP3 Tools", fontWeight = FontWeight.Bold)
                        Text(
                            "Edit, Convert & Compress Audio",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.AudioHistory.route) }) {
                        BadgedBox(
                            badge = {
                                if (historyCount > 0) {
                                    Badge { Text("$historyCount") }
                                }
                            }
                        ) {
                            Icon(Icons.Default.History, contentDescription = "Audio History")
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header summary banner
            item(span = { GridItemSpan(2) }) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "On-Device Audio Studio",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "100% private & offline audio processing",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Grid items
            items(tools) { tool ->
                AudioToolCard(tool) {
                    navController.navigate(tool.route)
                }
            }
        }
    }
}

@Composable
fun AudioToolCard(item: AudioToolItem, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "PressAnimation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .scale(scale)
            .shadow(
                elevation = if (isPressed) 2.dp else 4.dp,
                shape = RoundedCornerShape(24.dp),
                clip = false
            )
            .clip(RoundedCornerShape(24.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = item.bgColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.name,
                modifier = Modifier.size(32.dp),
                tint = item.accentColor
            )

            Column {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    maxLines = 1
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.DarkGray,
                    maxLines = 1
                )
            }
        }
    }
}
