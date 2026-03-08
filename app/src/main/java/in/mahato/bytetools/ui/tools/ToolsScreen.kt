package `in`.mahato.bytetools.ui.tools

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import `in`.mahato.bytetools.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(navController: NavController) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    
    val allGroups = listOf(
        ToolGroup("Daily Essentials", "Quick daily shortcuts", listOf(
            ToolItem("Flashlight", "Quick light access", Icons.Default.FlashlightOn, Screen.Flashlight.route, "Quick", Color(0xFFE3F2FD), Color(0xFF2196F3)),
            ToolItem("Magnifier", "Zoom in on small text", Icons.Default.ZoomIn, Screen.Magnifier.route, "Quick", Color(0xFFF3E5F5), Color(0xFF9C27B0)),
            ToolItem("Direct WhatsApp", "Direct message anyone", Icons.Default.Chat, Screen.DirectWhatsApp.route, "Quick", Color(0xFFE8F5E9), Color(0xFF4CAF50)),
            ToolItem("Age Calculator", "Birth date helper", Icons.Default.Cake, Screen.AgeCalculator.route, "Productivity", Color(0xFFFBE9E7), Color(0xFFFF5722)),
            ToolItem("Date Duration", "Calculate days between dates", Icons.Default.DateRange, Screen.DateDuration.route, "Productivity", Color(0xFFFFF3E0), Color(0xFFFF9800))
        )),
        ToolGroup("Measurement Tools", "Precise measuring utility", listOf(
            ToolItem("Unit Converter", "Convert anything", Icons.Default.SyncAlt, Screen.UnitConverter.route, "Measurement", Color(0xFFF1F8E9), Color(0xFF8BC34A)),
            ToolItem("Sound Meter", "Measure noise levels", Icons.Default.Mic, Screen.SoundMeter.route, "Measurement", Color(0xFFE0F7FA), Color(0xFF00BCD4)),
            ToolItem("Bubble Level", "Align surfaces perfectly", Icons.Default.AlignHorizontalCenter, Screen.BubbleLevel.route, "Measurement", Color(0xFFFFEBEE), Color(0xFFF44336)),
            ToolItem("Calculator", "Standard math utility", Icons.Default.Calculate, Screen.Calculator.route, "Productivity", Color(0xFFE8EAF6), Color(0xFF3F51B5))
        )),
        ToolGroup("Device Information", "View hardware status", listOf(
            ToolItem("Battery Info", "Current battery state", Icons.Default.BatteryStd, Screen.BatteryInfo.route, "Device", Color(0xFFFFF8E1), Color(0xFFFFC107)),
            ToolItem("Storage Analyzer", "Manage storage space", Icons.Default.Storage, Screen.StorageAnalyzer.route, "Device", Color(0xFFECEFF1), Color(0xFF607D8B)),
            ToolItem("RAM Monitor", "Real-time RAM usage", Icons.Default.Memory, Screen.RAMMonitor.route, "Device", Color(0xFFE3F2FD), Color(0xFF2196F3)),
            ToolItem("Device Info", "Detailed hardware info", Icons.Default.Info, Screen.DeviceInfo.route, "Device", Color(0xFFF3E5F5), Color(0xFF9C27B0))
        ))
    )

    val categories = listOf("All") + allGroups.flatMap { it.items.map { item -> item.category } }.distinct()

    val filteredGroups = allGroups.map { group ->
        val filteredItems = group.items.filter { item ->
            (selectedCategory == "All" || item.category == selectedCategory) &&
            (searchQuery.isEmpty() || item.name.contains(searchQuery, ignoreCase = true) || item.description.contains(searchQuery, ignoreCase = true))
        }
        group.copy(items = filteredItems)
    }.filter { it.items.isNotEmpty() }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Tools",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "All essential utilities in one place",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Search Bar
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search tools...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(28.dp)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Category Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(categories) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category) },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                enabled = true,
                                selected = selectedCategory == category
                            )
                        )
                    }
                }
            }
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
            contentPadding = PaddingValues(bottom = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            filteredGroups.forEach { group ->
                item(span = { GridItemSpan(2) }) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = group.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
                items(group.items) { item ->
                    ToolCard(item) {
                        navController.navigate(item.route)
                    }
                }
            }
        }
    }
}

data class ToolGroup(val name: String, val subtitle: String, val items: List<ToolItem>)
data class ToolItem(val name: String, val description: String, val icon: ImageVector, val route: String, val category: String, val bgColor: Color, val accentColor: Color)

@Composable
fun ToolCard(item: ToolItem, onClick: () -> Unit) {
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
                indication = LocalIndication.current,
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
                    color = Color.Gray,
                    maxLines = 1
                )
            }
        }
    }
}

