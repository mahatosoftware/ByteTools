package `in`.mahato.bytetools

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
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

import `in`.mahato.bytetools.nfc.NfcManager
import `in`.mahato.bytetools.nfc.NfcUtils
import `in`.mahato.bytetools.nfc.NfcState
import `in`.mahato.bytetools.nfc.NfcViewModel
import `in`.mahato.bytetools.nfc.RecordType
import `in`.mahato.bytetools.nfc.NfcAutomationExecutor
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import `in`.mahato.bytetools.utils.AdManager
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()
    private val nfcViewModel: NfcViewModel by viewModels()

    @Inject
    lateinit var nfcManager: NfcManager

    @Inject
    lateinit var adManager: AdManager

    private var lastAutomationRun: String? = null
    private var pendingAutomationRun: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adManager.initialize(this)
        nfcViewModel
        scheduleNfcIntentHandling(intent)

        lifecycleScope.launch {
            nfcViewModel.nfcState.collect { state ->
                if (state is NfcState.Success) {
                    val automation = state.parsedRecords
                        .firstOrNull { it.type == RecordType.AUTOMATION }
                        ?.data

                    if (!automation.isNullOrBlank() && automation != lastAutomationRun) {
                        pendingAutomationRun = automation
                        runPendingAutomation()
                    }
                } else {
                    lastAutomationRun = null
                }
            }
        }

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

    override fun onResume() {
        super.onResume()
        nfcManager.enableForegroundDispatch(this)
        runPendingAutomation()
    }

    override fun onPause() {
        super.onPause()
        nfcManager.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        scheduleNfcIntentHandling(intent)
    }

    private fun scheduleNfcIntentHandling(intent: Intent) {
        lifecycleScope.launch {
            delay(300)
            processAutomationIntent(intent)
            runPendingAutomation()
            nfcManager.onNewIntent(intent)
        }
    }

    private fun processAutomationIntent(intent: Intent) {
        NfcAutomationExecutor.automationNameFromUri(intent.data)?.let { automation ->
            if (automation != lastAutomationRun) {
                pendingAutomationRun = automation
            }
        }

        val automationFromIntentMessages = NfcUtils.getParsedNdefRecords(
            intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        )
            .firstOrNull { it.type == RecordType.AUTOMATION }
            ?.data

        if (!automationFromIntentMessages.isNullOrBlank() && automationFromIntentMessages != lastAutomationRun) {
            pendingAutomationRun = automationFromIntentMessages
            return
        }

        if (intent.action !in listOf(
                NfcAdapter.ACTION_TAG_DISCOVERED,
                NfcAdapter.ACTION_TECH_DISCOVERED,
                NfcAdapter.ACTION_NDEF_DISCOVERED
            )
        ) {
            return
        }

        val tag: Tag? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }

        val automation = tag
            ?.let { NfcUtils.getParsedNdefRecords(it) }
            ?.firstOrNull { it.type == RecordType.AUTOMATION }
            ?.data

        if (!automation.isNullOrBlank() && automation != lastAutomationRun) {
            pendingAutomationRun = automation
        }
    }

    private fun runPendingAutomation() {
        val automation = pendingAutomationRun ?: return
        if (automation == lastAutomationRun) return

        lifecycleScope.launch {
            delay(300)
            if (pendingAutomationRun == automation && NfcAutomationExecutor.run(this@MainActivity, automation)) {
                lastAutomationRun = automation
                pendingAutomationRun = null
            }
        }
    }
}
