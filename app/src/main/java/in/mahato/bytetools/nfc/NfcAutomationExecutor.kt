package `in`.mahato.bytetools.nfc

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast

object NfcAutomationExecutor {

    fun automationUriFor(name: String): String {
        val slug = when {
            name.startsWith("Silent mode + alarm", ignoreCase = true) -> "silent-alarm"
            name.startsWith("Turn WiFi ON", ignoreCase = true) -> "wifi-on"
            name.startsWith("Open Maps", ignoreCase = true) -> "open-maps"
            name.startsWith("Start playlist", ignoreCase = true) -> "start-playlist"
            else -> name
                .lowercase()
                .replace(Regex("[^a-z0-9]+"), "-")
                .trim('-')
        }
        return "bytetools://automation/$slug"
    }

    fun automationNameFromUri(uri: Uri?): String? {
        if (uri == null || uri.scheme != "bytetools" || uri.host != "automation") return null
        return when (uri.lastPathSegment?.lowercase()) {
            "silent-alarm" -> "Silent mode + alarm (Bedside)"
            "wifi-on" -> "Turn WiFi ON (Office desk)"
            "open-maps" -> "Open Maps (Car)"
            "start-playlist" -> "Start playlist (Gym)"
            else -> null
        }
    }

    fun run(context: Context, automationName: String): Boolean {
        return try {
            when {
                automationName.startsWith("Silent mode + alarm", ignoreCase = true) -> {
                    context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    runCatching {
                        context.startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                    Toast.makeText(
                        context,
                        "Opened sound and alarm settings. Android does not allow silent mode toggling directly from NFC here.",
                        Toast.LENGTH_LONG
                    ).show()
                    true
                }
                automationName.startsWith("Turn WiFi ON", ignoreCase = true) -> {
                    context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    Toast.makeText(
                        context,
                        "Opened Wi-Fi settings. Android does not allow enabling Wi-Fi directly from this NFC action.",
                        Toast.LENGTH_LONG
                    ).show()
                    true
                }
                automationName.startsWith("Open Maps", ignoreCase = true) -> {
                    val mapsLaunchIntent = context.packageManager
                        .getLaunchIntentForPackage("com.google.android.apps.maps")
                        ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

                    if (mapsLaunchIntent != null) {
                        context.startActivity(mapsLaunchIntent)
                    } else {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://www.google.com/maps")
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                    true
                }
                automationName.startsWith("Start playlist", ignoreCase = true) -> {
                    val intent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
                        putExtra(SearchManager.QUERY, "Workout playlist")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    true
                }
                else -> false
            }
        } catch (_: Exception) {
            false
        }
    }
}
