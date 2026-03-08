package `in`.mahato.bytetools.ui.tools.qr

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
fun QRBarcodeDashboardScreen(navController: NavController) {
    val tools = listOf(
        QRToolItem("QR Scanner", "Scan any QR code", Icons.Default.QrCodeScanner, Screen.QRScanner.route, Color(0xFFE3F2FD), Color(0xFF2196F3)),
        QRToolItem("Barcode Scan", "Scan product barcodes", Icons.Default.CenterFocusStrong, Screen.BarcodeScanner.route, Color(0xFFF3E5F5), Color(0xFF9C27B0)),
        QRToolItem("QR Generator", "Create custom QR", Icons.Default.QrCode, Screen.QRGenerator.route, Color(0xFFE8F5E9), Color(0xFF4CAF50)),
        QRToolItem("Barcode Gen", "Create product codes", Icons.Default.AddBox, Screen.BarcodeGenerator.route, Color(0xFFFBE9E7), Color(0xFFFF5722)),
        QRToolItem("History", "Previous scans", Icons.Default.History, Screen.QRHistory.route, Color(0xFFFFF3E0), Color(0xFFFF9800))
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("QR & Barcode", fontWeight = FontWeight.Bold)
                        Text("Scan, Generate & Manage", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            items(tools) { tool ->
                QRToolCard(tool) {
                    navController.navigate(tool.route)
                }
            }
        }
    }
}

data class QRToolItem(val name: String, val description: String, val icon: ImageVector, val route: String, val bgColor: androidx.compose.ui.graphics.Color, val accentColor: androidx.compose.ui.graphics.Color)

@Composable
fun QRToolCard(item: QRToolItem, onClick: () -> Unit) {
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
                    color = androidx.compose.ui.graphics.Color.Black,
                    maxLines = 1
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = androidx.compose.ui.graphics.Color.Gray,
                    maxLines = 1
                )
            }
        }
    }
}
