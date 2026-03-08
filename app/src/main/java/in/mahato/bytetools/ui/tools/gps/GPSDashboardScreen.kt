package `in`.mahato.bytetools.ui.tools.gps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import `in`.mahato.bytetools.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GPSDashboardScreen(navController: NavController) {
    val tools = listOf(
        GPSToolItem("Live Location Tracker", Icons.Default.MyLocation, "Real-time location, Address, Sharing", Screen.LiveLocation.route, androidx.compose.ui.graphics.Color(0xFFE3F2FD), androidx.compose.ui.graphics.Color(0xFF2196F3)),
        GPSToolItem("Parking Location", Icons.Default.LocalParking, "Save and strictly navigate to your parked vehicle", Screen.ParkingLocation.route, androidx.compose.ui.graphics.Color(0xFFF3E5F5), androidx.compose.ui.graphics.Color(0xFF9C27B0)),
        GPSToolItem("GPS Camera", Icons.Default.CameraAlt, "Take a picture with location overlay", Screen.GPSCamera.route, androidx.compose.ui.graphics.Color(0xFFE8F5E9), androidx.compose.ui.graphics.Color(0xFF4CAF50)),
        GPSToolItem("Digital Compass", Icons.Default.Explore, "True/Magnetic North, Degree display", Screen.DigitalCompass.route, androidx.compose.ui.graphics.Color(0xFFFBE9E7), androidx.compose.ui.graphics.Color(0xFFFF5722)),
        GPSToolItem("Distance & Area", Icons.Default.SquareFoot, "Measure distance and polygonal area", Screen.DistanceArea.route, androidx.compose.ui.graphics.Color(0xFFFFF3E0), androidx.compose.ui.graphics.Color(0xFFFF9800)),
        GPSToolItem("Speedometer", Icons.Default.Speed, "Real-time speed and trip tracking", Screen.Speedometer.route, androidx.compose.ui.graphics.Color(0xFFF1F8E9), androidx.compose.ui.graphics.Color(0xFF8BC34A)),
        GPSToolItem("GPS Signal Info", Icons.Default.SettingsInputAntenna, "Satellite status, Signal strength", Screen.GPSSignal.route, androidx.compose.ui.graphics.Color(0xFFE0F7FA), androidx.compose.ui.graphics.Color(0xFF00BCD4)),
        GPSToolItem("GPS Gallery", Icons.Default.PhotoLibrary, "View photos captured with GPS Camera", Screen.ImageGallery.route, androidx.compose.ui.graphics.Color(0xFFFFEBEE), androidx.compose.ui.graphics.Color(0xFFF44336))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GPS Utilities Pack", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(1),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                ),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(tools) { tool ->
                GPSCard(tool) {
                    navController.navigate(tool.route)
                }
            }
        }
    }
}

data class GPSToolItem(
    val name: String,
    val icon: ImageVector,
    val description: String,
    val route: String,
    val bgColor: androidx.compose.ui.graphics.Color,
    val accentColor: androidx.compose.ui.graphics.Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GPSCard(tool: GPSToolItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = tool.bgColor)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                tool.icon,
                contentDescription = tool.name,
                tint = tool.accentColor,
                modifier = Modifier.size(40.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = tool.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color.Black
                )
                Text(
                    text = tool.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.ui.graphics.Color.Gray
                )
            }
        }
    }
}
