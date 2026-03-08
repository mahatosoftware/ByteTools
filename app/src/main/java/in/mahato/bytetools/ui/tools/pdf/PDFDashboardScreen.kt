package `in`.mahato.bytetools.ui.tools.pdf

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
fun PDFDashboardScreen(navController: NavController) {
    val pdfTools = listOf(
        PDFToolItem("Doc Scanner", "Scan physical documents", Icons.Default.DocumentScanner, Screen.PDFScanner.route, Color(0xFFE3F2FD), Color(0xFF2196F3)),
        PDFToolItem("PDF Viewer", "Read & browse PDFs", Icons.Default.MenuBook, Screen.PDFViewer.route, Color(0xFFF3E5F5), Color(0xFF9C27B0)),
        PDFToolItem("PDF Splitter", "Split pages by range", Icons.Default.CallSplit, Screen.PDFSplitter.route, Color(0xFFE8F5E9), Color(0xFF4CAF50)),
        PDFToolItem("PDF Merger", "Combine multiple PDFs", Icons.Default.Merge, Screen.PDFMerger.route, Color(0xFFFBE9E7), Color(0xFFFF5722)),
        PDFToolItem("Sign PDF", "Draw & place signatures", Icons.Default.Draw, Screen.PDFSign.route, Color(0xFFFFF3E0), Color(0xFFFF9800)),
        PDFToolItem("Watermark", "Add or remove text/image", Icons.Default.WaterDrop, Screen.PDFWatermark.route, Color(0xFFF1F8E9), Color(0xFF8BC34A)),
        PDFToolItem("Redact", "Hide area from PDF", Icons.Default.HighlightAlt, Screen.PDFRedact.route, Color(0xFFE0F7FA), Color(0xFF00BCD4)),
        PDFToolItem("OCR PDF", "Extract text from pages", Icons.Default.Scanner, Screen.PDFOCR.route, Color(0xFFFFEBEE), Color(0xFFF44336)),
        PDFToolItem("Img to PDF", "Convert images", Icons.Default.PictureAsPdf, Screen.ImageToPdf.route, Color(0xFFE8EAF6), Color(0xFF3F51B5)),
        PDFToolItem("History", "Managed transformed PDFs", Icons.Default.History, Screen.PDFHistory.route, Color(0xFFFFF8E1), Color(0xFFFFC107))
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("PDF Tools", fontWeight = FontWeight.Bold)
                        Text("Manage your documents", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            items(pdfTools) { tool ->
                PDFToolCard(tool) {
                    navController.navigate(tool.route)
                }
            }
        }
    }
}

data class PDFToolItem(val name: String, val description: String, val icon: ImageVector, val route: String, val bgColor: androidx.compose.ui.graphics.Color, val accentColor: androidx.compose.ui.graphics.Color)

@Composable
fun PDFToolCard(item: PDFToolItem, onClick: () -> Unit) {
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
