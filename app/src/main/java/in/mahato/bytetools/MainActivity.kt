package `in`.mahato.bytetools

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import `in`.mahato.bytetools.ui.theme.ByteToolsTheme
import `in`.mahato.bytetools.ui.navigation.AppNavigation
import androidx.navigation.compose.rememberNavController
import `in`.mahato.bytetools.ui.navigation.BottomNavigationBar

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by mainViewModel.themeMode.collectAsState(initial = "system")
            val dynamicColor by mainViewModel.dynamicColor.collectAsState(initial = true)

            val isDarkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            ByteToolsTheme(
                darkTheme = isDarkTheme,
                dynamicColor = dynamicColor
            ) {
                val navController = rememberNavController()
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = { BottomNavigationBar(navController) }
                ) { innerPadding ->
                    // The innerPadding contains the heights of the top and bottom bars (including navigation bar)
                    AppNavigation(
                        modifier = Modifier.padding(innerPadding),
                        navController = navController
                    )
                }
            }
        }
    }
}
