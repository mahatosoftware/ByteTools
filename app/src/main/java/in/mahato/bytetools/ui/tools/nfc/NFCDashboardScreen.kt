package `in`.mahato.bytetools.ui.tools.nfc

import androidx.compose.animation.core.*
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import `in`.mahato.bytetools.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NFCDashboardScreen(navController: NavController) {
    val nfcTools = listOf(
        NFCToolItem("NFC Tag Reader", "Scan & view NFC data", Icons.Default.Nfc, Screen.NFCTagReader.route, Color(0xFFE3F2FD), Color(0xFF2196F3)),
        NFCToolItem("Write NFC Tag", "Write data to blank tags", Icons.Default.Edit, Screen.NFCWriter.route, Color(0xFFF3E5F5), Color(0xFF9C27B0)),
        NFCToolItem("Business Card", "Digital business card", Icons.Default.ContactMail, Screen.NFCBusinessCard.route, Color(0xFFE8F5E9), Color(0xFF4CAF50)),
        NFCToolItem("WiFi NFC Tag", "Share WiFi via NFC", Icons.Default.WifiTethering, Screen.NFCWiFi.route, Color(0xFFFBE9E7), Color(0xFFFF5722)),
        NFCToolItem("NFC Automation", "Trigger settings", Icons.Default.SmartButton, Screen.NFCAutomation.route, Color(0xFFFFF3E0), Color(0xFFFF9800)),
        NFCToolItem("Card Reader", "Read basic card info", Icons.Default.CreditCard, Screen.NFCPaymentReader.route, Color(0xFFF1F8E9), Color(0xFF8BC34A)),
        NFCToolItem("Clone NFC Tag", "Copy tag to another", Icons.Default.ContentCopy, Screen.NFCClone.route, Color(0xFFE0F7FA), Color(0xFF00BCD4)),
        NFCToolItem("Erase / Format", "Manage & lock tags", Icons.Default.DeleteForever, Screen.NFCFormatter.route, Color(0xFFFFEBEE), Color(0xFFF44336)),
        NFCToolItem("QR & NFC Share", "Share via QR + NFC", Icons.Default.Share, Screen.NFCQRHybrid.route, Color(0xFFE8EAF6), Color(0xFF3F51B5)),
        NFCToolItem("Tap Counter", "Count tag scans", Icons.Default.PlusOne, Screen.NFCTapCounter.route, Color(0xFFFFF8E1), Color(0xFFFFC107)),
        NFCToolItem("Scanner History", "Past scans & logs", Icons.Default.History, Screen.NFCScannerHistory.route, Color(0xFFECEFF1), Color(0xFF607D8B))
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("NFC Tools", fontWeight = FontWeight.Bold)
                        Text("Connect with physical tags", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            items(nfcTools) { tool ->
                NFCToolCard(tool) {
                    navController.navigate(tool.route)
                }
            }
        }
    }
}

data class NFCToolItem(val name: String, val description: String, val icon: ImageVector, val route: String, val bgColor: Color, val accentColor: Color)

@Composable
fun NFCToolCard(item: NFCToolItem, onClick: () -> Unit) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NFCPlaceholderScreen(navController: NavController, title: String) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Text("Coming Soon!", style = MaterialTheme.typography.headlineMedium)
        }
    }
}
