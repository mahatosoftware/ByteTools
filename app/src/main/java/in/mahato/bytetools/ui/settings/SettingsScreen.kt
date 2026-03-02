package `in`.mahato.bytetools.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val themeMode by viewModel.themeMode.collectAsState(initial = "system")
    val dynamicColor by viewModel.dynamicColor.collectAsState(initial = true)

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            SettingsItem(
                title = "Dark Mode",
                subtitle = "Current: ${themeMode.replaceFirstChar { it.uppercase() }}"
            ) {
                viewModel.cycleThemeMode(themeMode)
            }
            SettingsItem(
                title = "Dynamic Color",
                subtitle = if (dynamicColor) "Enabled" else "Disabled"
            ) {
                viewModel.toggleDynamicColor(!dynamicColor)
            }
            HorizontalDivider()
            SettingsItem("Privacy Policy", "Read our privacy policy") {
                val intent = android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://sites.google.com/view/bytetools")
                )
                context.startActivity(intent)
            }
            val appVersion = try {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                packageInfo.versionName
            } catch (e: Exception) {
                "Unknown"
            }

            SettingsItem("About", "App Version $appVersion") {}
        }
    }
}

@Composable
fun SettingsItem(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
