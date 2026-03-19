package `in`.mahato.bytetools.ui.tools.nfc

import android.Manifest
import android.annotation.SuppressLint
import android.nfc.NdefMessage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SmartButton
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationServices
import `in`.mahato.bytetools.nfc.NfcAutomationExecutor
import `in`.mahato.bytetools.nfc.NfcState
import `in`.mahato.bytetools.nfc.NfcUtils
import `in`.mahato.bytetools.nfc.NfcViewModel

private enum class AutomationType(
    val title: String,
    val shortTitle: String,
    val helper: String
) {
    PHONE_CALL(
        title = "Make a phone call",
        shortTitle = "Call",
        helper = "Ask for a phone number and start the call flow on NFC tap."
    ),
    OPEN_APP(
        title = "Open an app",
        shortTitle = "App",
        helper = "Use an app name or package name. Package names are more reliable."
    ),
    OPEN_GEOLOCATION(
        title = "Open a geolocation",
        shortTitle = "Maps",
        helper = "Use latitude and longitude or paste a Google Maps location link."
    ),
    LAUNCH_URL(
        title = "Launch a URL",
        shortTitle = "URL",
        helper = "Open a website in the default browser as soon as the tag is scanned."
    )
}

private data class StepCard(
    val title: String,
    val detail: String
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun NFCAutomationScreen(navController: NavController, viewModel: NfcViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val state by viewModel.nfcState.collectAsState()
    var selectedType by rememberSaveable { mutableStateOf(AutomationType.PHONE_CALL) }
    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var appIdentifier by rememberSaveable { mutableStateOf("") }
    var latitude by rememberSaveable { mutableStateOf("") }
    var longitude by rememberSaveable { mutableStateOf("") }
    var mapLink by rememberSaveable { mutableStateOf("") }
    var websiteUrl by rememberSaveable { mutableStateOf("") }
    var isFetchingCurrentLocation by rememberSaveable { mutableStateOf(false) }
    var locationFetchMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val locationPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
    val fusedLocationClient = remember(context) {
        LocationServices.getFusedLocationProviderClient(context)
    }
    val automationPayload = buildAutomationPayload(
        selectedType = selectedType,
        phoneNumber = phoneNumber,
        appIdentifier = appIdentifier,
        latitude = latitude,
        longitude = longitude,
        mapLink = mapLink,
        websiteUrl = websiteUrl
    )
    val validationMessage = validationMessageFor(
        selectedType = selectedType,
        phoneNumber = phoneNumber,
        appIdentifier = appIdentifier,
        latitude = latitude,
        longitude = longitude,
        mapLink = mapLink,
        websiteUrl = websiteUrl
    )

    val toolsRequired = remember {
        listOf(
            "Android phone with NFC enabled and a writable NFC tag",
            "ByteTools can write and run these tag automations directly",
            "NFC Tools or Tasker are optional Android alternatives if you need more advanced actions"
        )
    }
    val optionalImprovements = remember {
        listOf(
            "Add a confirmation dialog before placing a phone call",
            "Speak the action using text-to-speech after a successful scan",
            "Restrict actions by time, headset connection, or Wi-Fi network with Tasker",
            "Write a backup URL onto the tag so the action still does something useful in other NFC apps"
        )
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.resetState() }
    }

    LaunchedEffect(locationPermissionsState.allPermissionsGranted, isFetchingCurrentLocation) {
        if (locationPermissionsState.allPermissionsGranted && isFetchingCurrentLocation) {
            fetchCurrentLocation(
                fusedLocationClient = fusedLocationClient,
                onSuccess = { lat, lng ->
                    latitude = lat
                    longitude = lng
                    mapLink = ""
                    locationFetchMessage = "Current coordinates loaded."
                    isFetchingCurrentLocation = false
                },
                onError = { error ->
                    locationFetchMessage = error
                    isFetchingCurrentLocation = false
                }
            )
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("NFC Automation") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                StatusCard(state = state)
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Step 1: Remove existing tag actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Tap the erase button below, then touch the old NFC tag to clear the current NDEF payload before writing a new automation.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(onClick = { viewModel.setPendingFormat() }) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Erase Current NFC Action")
                        }
                    }
                }
            }

            item {
                SectionTitle("Tools Required")
            }
            items(toolsRequired) { tool ->
                BulletLine(tool)
            }

            item {
                SectionTitle("Step 2: Create a new NFC-triggered automation")
            }

            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    AutomationType.entries.forEachIndexed { index, type ->
                        SegmentedButton(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = AutomationType.entries.size
                            )
                        ) {
                            Text(type.shortTitle)
                        }
                    }
                }
            }

            item {
                AutomationEditor(
                    selectedType = selectedType,
                    phoneNumber = phoneNumber,
                    onPhoneNumberChange = { phoneNumber = it },
                    appIdentifier = appIdentifier,
                    onAppIdentifierChange = { appIdentifier = it },
                    latitude = latitude,
                    onLatitudeChange = { latitude = it },
                    longitude = longitude,
                    onLongitudeChange = { longitude = it },
                    mapLink = mapLink,
                    onMapLinkChange = { mapLink = it },
                    websiteUrl = websiteUrl,
                    onWebsiteUrlChange = { websiteUrl = it },
                    isFetchingCurrentLocation = isFetchingCurrentLocation,
                    locationFetchMessage = locationFetchMessage,
                    onUseCurrentLocation = {
                        locationFetchMessage = null
                        if (locationPermissionsState.allPermissionsGranted) {
                            isFetchingCurrentLocation = true
                        } else {
                            locationPermissionsState.launchMultiplePermissionRequest()
                        }
                    },
                    validationMessage = validationMessage,
                    canWrite = automationPayload != null,
                    onWrite = {
                        automationPayload?.let { automationUri ->
                            val automationTextRecord = NfcUtils.createTextRecord("AUTO_TASK:$automationUri")
                            val appRecord = android.nfc.NdefRecord.createApplicationRecord("in.mahato.bytetools")
                            viewModel.setPendingWriteMessage(
                                NdefMessage(arrayOf(automationTextRecord, appRecord))
                            )
                        }
                    }
                )
            }

            item {
                SectionTitle("Clear Steps Per Automation")
            }
            items(stepCardsFor(selectedType)) { step ->
                InfoCard(step.title, step.detail)
            }

            item {
                SectionTitle("Fallback Handling")
            }
            item {
                InfoCard(
                    "If NFC fails",
                    "Keep NFC enabled, unlock the phone, hold the top or middle of the device on the tag for 1 to 2 seconds, and use the erase step again if an old payload is still present."
                )
            }
            item {
                InfoCard(
                    "If the target app is missing",
                    "For app launches, ByteTools searches by package first and then by launcher app name. If it cannot find the app, it shows a message instead of failing silently."
                )
            }
            item {
                InfoCard(
                    "If direct actions are limited",
                    "Android can open the dialer, browser, or Maps directly. A true direct phone call needs the Phone permission."
                )
            }

            item {
                SectionTitle("Optional Improvements")
            }
            items(optionalImprovements) { improvement ->
                BulletLine(improvement)
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun StatusCard(state: NfcState) {
    if (state !is NfcState.Ready && state !is NfcState.Success && state !is NfcState.Error) return

    Card(modifier = Modifier.fillMaxWidth()) {
        val message = when (state) {
            is NfcState.Ready -> state.message
            is NfcState.Success -> state.data
            is NfcState.Error -> state.error
            else -> ""
        }
        Text(message, modifier = Modifier.padding(16.dp))
    }
}

@Composable
private fun AutomationEditor(
    selectedType: AutomationType,
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    appIdentifier: String,
    onAppIdentifierChange: (String) -> Unit,
    latitude: String,
    onLatitudeChange: (String) -> Unit,
    longitude: String,
    onLongitudeChange: (String) -> Unit,
    mapLink: String,
    onMapLinkChange: (String) -> Unit,
    websiteUrl: String,
    onWebsiteUrlChange: (String) -> Unit,
    isFetchingCurrentLocation: Boolean,
    locationFetchMessage: String?,
    onUseCurrentLocation: () -> Unit,
    validationMessage: String?,
    canWrite: Boolean,
    onWrite: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(iconFor(selectedType), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(selectedType.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(selectedType.helper, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            when (selectedType) {
                AutomationType.PHONE_CALL -> {
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = onPhoneNumberChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Phone number") },
                        placeholder = { Text("+1 555 123 4567") },
                        singleLine = true
                    )
                }

                AutomationType.OPEN_APP -> {
                    OutlinedTextField(
                        value = appIdentifier,
                        onValueChange = onAppIdentifierChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("App name or package name") },
                        placeholder = { Text("YouTube or com.google.android.youtube") },
                        singleLine = true
                    )
                }

                AutomationType.OPEN_GEOLOCATION -> {
                    Button(
                        onClick = onUseCurrentLocation,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isFetchingCurrentLocation) "Fetching current location..." else "Use Current Location")
                    }
                    OutlinedTextField(
                        value = latitude,
                        onValueChange = onLatitudeChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Latitude") },
                        placeholder = { Text("37.4219983") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = longitude,
                        onValueChange = onLongitudeChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Longitude") },
                        placeholder = { Text("-122.084") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = mapLink,
                        onValueChange = onMapLinkChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Map link") },
                        placeholder = { Text("Optional if latitude/longitude are filled") }
                    )
                    locationFetchMessage?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                AutomationType.LAUNCH_URL -> {
                    OutlinedTextField(
                        value = websiteUrl,
                        onValueChange = onWebsiteUrlChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Website URL") },
                        placeholder = { Text("https://example.com") },
                        singleLine = true
                    )
                }
            }

            validationMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onWrite,
                modifier = Modifier.fillMaxWidth(),
                enabled = canWrite
            ) {
                Icon(Icons.Default.SmartButton, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Write Automation To NFC Tag")
            }
        }
    }
}

private fun validationMessageFor(
    selectedType: AutomationType,
    phoneNumber: String,
    appIdentifier: String,
    latitude: String,
    longitude: String,
    mapLink: String,
    websiteUrl: String
): String? {
    return when (selectedType) {
        AutomationType.PHONE_CALL -> {
            if (phoneNumber.trim().isBlank()) "Enter a phone number to enable writing." else null
        }
        AutomationType.OPEN_APP -> {
            if (appIdentifier.trim().isBlank()) "Enter an app name or package name to enable writing." else null
        }
        AutomationType.OPEN_GEOLOCATION -> {
            val hasCoordinates = latitude.trim().isNotBlank() && longitude.trim().isNotBlank()
            val hasLink = mapLink.trim().isNotBlank()
            if (!hasCoordinates && !hasLink) "Enter latitude and longitude, or paste a map link." else null
        }
        AutomationType.LAUNCH_URL -> {
            if (websiteUrl.trim().isBlank()) "Enter a website URL to enable writing." else null
        }
    }
}

@SuppressLint("MissingPermission")
private fun fetchCurrentLocation(
    fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient,
    onSuccess: (String, String) -> Unit,
    onError: (String) -> Unit
) {
    fusedLocationClient.lastLocation
        .addOnSuccessListener { location ->
            if (location != null) {
                onSuccess(location.latitude.toString(), location.longitude.toString())
            } else {
                onError("Could not get the current location. Make sure GPS is on and try again.")
            }
        }
        .addOnFailureListener {
            onError("Unable to fetch the current location right now.")
        }
}

private fun buildAutomationPayload(
    selectedType: AutomationType,
    phoneNumber: String,
    appIdentifier: String,
    latitude: String,
    longitude: String,
    mapLink: String,
    websiteUrl: String
): String? {
    return when (selectedType) {
        AutomationType.PHONE_CALL -> {
            val sanitized = phoneNumber.trim()
            if (sanitized.isBlank()) null else NfcAutomationExecutor.automationUriForCall(sanitized)
        }

        AutomationType.OPEN_APP -> {
            val sanitized = appIdentifier.trim()
            if (sanitized.isBlank()) null else NfcAutomationExecutor.automationUriForApp(sanitized)
        }

        AutomationType.OPEN_GEOLOCATION -> {
            val lat = latitude.trim()
            val lng = longitude.trim()
            val link = mapLink.trim()
            when {
                lat.isNotBlank() && lng.isNotBlank() -> NfcAutomationExecutor.automationUriForLocation(lat, lng, null)
                link.isNotBlank() -> NfcAutomationExecutor.automationUriForLocation(null, null, link)
                else -> null
            }
        }

        AutomationType.LAUNCH_URL -> {
            val sanitized = websiteUrl.trim()
            if (sanitized.isBlank()) null else NfcAutomationExecutor.automationUriForUrl(sanitized)
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun BulletLine(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text("• ", style = MaterialTheme.typography.bodyMedium)
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun InfoCard(title: String, detail: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(detail, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun stepCardsFor(type: AutomationType): List<StepCard> {
    return when (type) {
        AutomationType.PHONE_CALL -> listOf(
            StepCard("1. Enter the phone number", "Use a full number with country code for the cleanest result."),
            StepCard("2. Write the tag", "Tap Write Automation To NFC Tag and hold the blank or erased tag near the phone."),
            StepCard("3. Scan to run", "ByteTools tries a direct call if permission is granted. Otherwise it opens the dialer.")
        )

        AutomationType.OPEN_APP -> listOf(
            StepCard("1. Enter app name or package", "Package names are best. App names work when they match the installed launcher label."),
            StepCard("2. Write the tag", "Save the launch action onto the NFC tag from this screen."),
            StepCard("3. Scan to run", "ByteTools opens the target app instantly. If the app is not installed, it shows fallback guidance.")
        )

        AutomationType.OPEN_GEOLOCATION -> listOf(
            StepCard("1. Enter coordinates or a map link", "Fill latitude and longitude, or paste a location URL from Google Maps."),
            StepCard("2. Write the tag", "Write the navigation target onto the NFC tag."),
            StepCard("3. Scan to run", "ByteTools opens Google Maps navigation when possible, or falls back to the browser.")
        )

        AutomationType.LAUNCH_URL -> listOf(
            StepCard("1. Enter the website", "Use a full URL like https://example.com. If you omit the scheme, ByteTools adds https://."),
            StepCard("2. Write the tag", "Write the browser action to the tag."),
            StepCard("3. Scan to run", "ByteTools opens the default browser immediately.")
        )
    }
}

private fun iconFor(type: AutomationType) = when (type) {
    AutomationType.PHONE_CALL -> Icons.Default.Call
    AutomationType.OPEN_APP -> Icons.Default.PhoneAndroid
    AutomationType.OPEN_GEOLOCATION -> Icons.Default.LocationOn
    AutomationType.LAUNCH_URL -> Icons.Default.OpenInBrowser
}
