package `in`.mahato.bytetools.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import `in`.mahato.bytetools.ui.navigation.Screen
import `in`.mahato.bytetools.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: HomeViewModel = hiltViewModel()) {
    val scrollState = rememberScrollState()
    val haptic = LocalHapticFeedback.current

    val recentRoutes by viewModel.recentTools.collectAsState()
    val mostUsedRoute by viewModel.mostUsedTool.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.mipmap.ic_launcher_round),
                            contentDescription = "Logo",
                            modifier = Modifier.size(32.dp).clip(CircleShape)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("Byte Tools", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    }
                },
                actions = {
                    IconButton(onClick = { /* Search */ }) {
                        Icon(Icons.Default.Search, "Search")
                    }
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
        ) {
            // 1. Top Section (Greeting & Stats)
            GreetingSection(mostUsedRoute)

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Quick Action Row
            Text(
                "Quick Actions",
                modifier = Modifier.padding(horizontal = 20.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            QuickActionRow(navController, viewModel)

            Spacer(modifier = Modifier.height(32.dp))

            // 3. Main Tool Categories
            Text(
                "Tool Categories",
                modifier = Modifier.padding(horizontal = 20.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            CategoryGrid(navController, viewModel)

            if (recentRoutes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(32.dp))

                // 4. Recently Used Section
                Text(
                    "Recently Used",
                    modifier = Modifier.padding(horizontal = 20.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                RecentToolsRow(navController, recentRoutes, viewModel)
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun GreetingSection(mostUsedRoute: String) {
    val mostUsedName = getToolNameFromRoute(mostUsedRoute)

    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(
            "Welcome Back 👋",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (mostUsedName.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Most Used", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(mostUsedName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionRow(navController: NavController, viewModel: HomeViewModel) {
    val haptic = LocalHapticFeedback.current
    val quickTools = listOf(
        QuickTool("Scan QR", Icons.Default.QrCodeScanner, Screen.QRScanner.route),
        QuickTool("GPS Tracker", Icons.Default.GpsFixed, Screen.GPSDashboard.route),
        QuickTool("Spin Wheel", Icons.Default.Casino, Screen.SpinWheel.route),
        QuickTool("Compass", Icons.Default.Explore, Screen.DigitalCompass.route),
        QuickTool("Calculator", Icons.Default.Calculate, Screen.Calculator.route)
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        items(quickTools) { tool ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.addRecentTool(tool.route)
                        navController.navigate(tool.route)
                    }
                )
            ) {
                Surface(
                    modifier = Modifier.size(64.dp).shadow(4.dp, CircleShape),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(tool.icon, contentDescription = tool.name, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(tool.name, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun CategoryGrid(navController: NavController, viewModel: HomeViewModel) {
    val categories = listOf(
        CategoryItem("GPS Tools", "Location & Speed", Icons.Default.MyLocation, Screen.GPSDashboard.route, Color(0xFFE3F2FD), Color(0xFF2196F3)),
        CategoryItem("Image Tools", "Crop & Convert", Icons.Default.PhotoLibrary, Screen.ImageDashboard.route, Color(0xFFF3E5F5), Color(0xFF9C27B0)),
        CategoryItem("QR & Barcode", "Scan & Generate", Icons.Default.QrCode, Screen.QRBarcodeDashboard.route, Color(0xFFE8F5E9), Color(0xFF4CAF50)),
        CategoryItem("PDF Tools", "View, Merge, Split", Icons.Default.PictureAsPdf, Screen.PDFDashboard.route, Color(0xFFFBE9E7), Color(0xFFFF5722)),
        CategoryItem("Fun Tools", "Games & Decisions", Icons.Default.Nightlife, Screen.DecisionDashboard.route, Color(0xFFFFF3E0), Color(0xFFFF9800)),
        CategoryItem("Utility", "Math & Converters", Icons.Default.Construction, Screen.Tools.route, Color(0xFFF1F8E9), Color(0xFF8BC34A)),
        CategoryItem("NFC Tools", "Read & Write Tags", Icons.Default.Nfc, Screen.NFCDashboard.route, Color(0xFFE0F7FA), Color(0xFF00BCD4))
    )

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        for (i in categories.indices step 2) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CategoryCard(categories[i], modifier = Modifier.weight(1f)) {
                    viewModel.addRecentTool(categories[i].route)
                    navController.navigate(categories[i].route)
                }
                if (i + 1 < categories.size) {
                    CategoryCard(categories[i+1], modifier = Modifier.weight(1f)) {
                        viewModel.addRecentTool(categories[i+1].route)
                        navController.navigate(categories[i+1].route)
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun CategoryCard(item: CategoryItem, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier.height(140.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = item.bgColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(item.icon, contentDescription = item.title, tint = item.accentColor, modifier = Modifier.size(32.dp))
            Column {
                Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black)
                Text(item.subtitle, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}

data class QuickTool(val name: String, val icon: ImageVector, val route: String)
data class CategoryItem(val title: String, val subtitle: String, val icon: ImageVector, val route: String, val bgColor: Color, val accentColor: Color)
data class RecentTool(val name: String, val icon: ImageVector, val route: String)

@Composable
fun RecentToolsRow(navController: NavController, recentRoutes: List<String>, viewModel: HomeViewModel) {
    val recents = recentRoutes.mapNotNull { route ->
        val name = getToolNameFromRoute(route)
        val icon = getIconForRoute(route)
        if (name.isNotEmpty() && icon != null) {
            RecentTool(name, icon, route)
        } else {
            null
        }
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(recents) { recent ->
            Card(
                onClick = {
                    viewModel.addRecentTool(recent.route)
                    navController.navigate(recent.route)
                },
                modifier = Modifier.width(180.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(recent.icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(recent.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
    }
}

fun getToolNameFromRoute(route: String): String {
    return when (route) {
        Screen.QRScanner.route -> "QR Scanner"
        Screen.GPSDashboard.route -> "GPS Tools"
        Screen.SpinWheel.route -> "Spin Wheel"
        Screen.DigitalCompass.route -> "Compass"
        Screen.Calculator.route -> "Calculator"
        Screen.ImageDashboard.route -> "Image Tools"
        Screen.QRBarcodeDashboard.route -> "QR & Barcode"
        Screen.PDFDashboard.route -> "PDF Tools"
        Screen.DecisionDashboard.route -> "Fun Tools"
        Screen.Tools.route -> "Utility"
        Screen.NFCDashboard.route -> "NFC Tools"
        // add more specific routes as needed based on the AppNavigation map
        else -> ""
    }
}

fun getIconForRoute(route: String): ImageVector? {
    return when (route) {
        Screen.QRScanner.route -> Icons.Default.QrCodeScanner
        Screen.GPSDashboard.route -> Icons.Default.MyLocation
        Screen.SpinWheel.route -> Icons.Default.Casino
        Screen.DigitalCompass.route -> Icons.Default.Explore
        Screen.Calculator.route -> Icons.Default.Calculate
        Screen.ImageDashboard.route -> Icons.Default.PhotoLibrary
        Screen.QRBarcodeDashboard.route -> Icons.Default.QrCode
        Screen.PDFDashboard.route -> Icons.Default.PictureAsPdf
        Screen.DecisionDashboard.route -> Icons.Default.Nightlife
        Screen.Tools.route -> Icons.Default.Construction
        Screen.NFCDashboard.route -> Icons.Default.Nfc
        else -> null
    }
}
