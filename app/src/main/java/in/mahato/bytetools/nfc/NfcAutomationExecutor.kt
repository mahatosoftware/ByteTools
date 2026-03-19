package `in`.mahato.bytetools.nfc

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object NfcAutomationExecutor {

    private const val REQUEST_CALL_PERMISSION = 3301
    private const val SCHEME = "bytetools"
    private const val HOST = "automation"
    private const val PATH_EXECUTE = "execute"

    fun automationUriFor(name: String): String = name

    fun automationUriForCall(phoneNumber: String): String {
        return Uri.Builder()
            .scheme(SCHEME)
            .authority(HOST)
            .appendPath(PATH_EXECUTE)
            .appendQueryParameter("action", "call")
            .appendQueryParameter("phone", phoneNumber)
            .build()
            .toString()
    }

    fun automationUriForApp(appIdentifier: String): String {
        return Uri.Builder()
            .scheme(SCHEME)
            .authority(HOST)
            .appendPath(PATH_EXECUTE)
            .appendQueryParameter("action", "open_app")
            .appendQueryParameter("target", appIdentifier)
            .build()
            .toString()
    }

    fun automationUriForLocation(latitude: String?, longitude: String?, mapLink: String?): String {
        return Uri.Builder()
            .scheme(SCHEME)
            .authority(HOST)
            .appendPath(PATH_EXECUTE)
            .appendQueryParameter("action", "open_location")
            .apply {
                latitude?.takeIf { it.isNotBlank() }?.let { appendQueryParameter("lat", it) }
                longitude?.takeIf { it.isNotBlank() }?.let { appendQueryParameter("lng", it) }
                mapLink?.takeIf { it.isNotBlank() }?.let { appendQueryParameter("link", it) }
            }
            .build()
            .toString()
    }

    fun automationUriForUrl(url: String): String {
        return Uri.Builder()
            .scheme(SCHEME)
            .authority(HOST)
            .appendPath(PATH_EXECUTE)
            .appendQueryParameter("action", "open_url")
            .appendQueryParameter("url", url)
            .build()
            .toString()
    }

    fun automationNameFromUri(uri: Uri?): String? {
        val config = parseAutomation(uri) ?: return null
        return config.uri.toString()
    }

    fun run(context: Context, automationName: String): Boolean {
        val config = parseAutomation(automationName) ?: parseLegacyAutomation(automationName) ?: return false
        return try {
            when (config.action) {
                "call" -> runCall(context, config)
                "open_app" -> runOpenApp(context, config)
                "open_location" -> runLocation(context, config)
                "open_url" -> runUrl(context, config)
                else -> false
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun runCall(context: Context, config: AutomationConfig): Boolean {
        val phone = config.uri.getQueryParameter("phone")?.trim().orEmpty()
        if (phone.isBlank()) return false

        val callUri = Uri.parse("tel:${Uri.encode(phone)}")
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            context.startActivity(
                Intent(Intent.ACTION_CALL, callUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return true
        }

        if (context is Activity) {
            ActivityCompat.requestPermissions(
                context,
                arrayOf(Manifest.permission.CALL_PHONE),
                REQUEST_CALL_PERMISSION
            )
            Toast.makeText(
                context,
                "Phone permission is needed for a direct call. Tap the tag again after granting it.",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(
                context,
                "Direct call permission is missing. Opening the dialer instead.",
                Toast.LENGTH_LONG
            ).show()
        }

        context.startActivity(
            Intent(Intent.ACTION_DIAL, callUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        return true
    }

    private fun runOpenApp(context: Context, config: AutomationConfig): Boolean {
        val target = config.uri.getQueryParameter("target")?.trim().orEmpty()
        if (target.isBlank()) return false

        val intent = context.packageManager.getLaunchIntentForPackage(target)
            ?: findLaunchIntentByAppName(context, target)

        if (intent == null) {
            Toast.makeText(
                context,
                "App not found. Use the exact package name for the most reliable result.",
                Toast.LENGTH_LONG
            ).show()
            return false
        }

        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return true
    }

    private fun runLocation(context: Context, config: AutomationConfig): Boolean {
        val lat = config.uri.getQueryParameter("lat")?.trim().orEmpty()
        val lng = config.uri.getQueryParameter("lng")?.trim().orEmpty()
        val link = config.uri.getQueryParameter("link")?.trim().orEmpty()

        val navigationUri = when {
            lat.isNotBlank() && lng.isNotBlank() -> Uri.parse("google.navigation:q=${Uri.encode("$lat,$lng")}")
            link.isNotBlank() -> Uri.parse(link)
            else -> return false
        }

        val googleMapsIntent = Intent(Intent.ACTION_VIEW, navigationUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            setPackage("com.google.android.apps.maps")
        }

        val fallbackIntent = Intent(Intent.ACTION_VIEW, navigationUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        if (googleMapsIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(googleMapsIntent)
        } else {
            context.startActivity(fallbackIntent)
            Toast.makeText(
                context,
                "Google Maps is not installed. Opened the location with another available app.",
                Toast.LENGTH_LONG
            ).show()
        }
        return true
    }

    private fun runUrl(context: Context, config: AutomationConfig): Boolean {
        val rawUrl = config.uri.getQueryParameter("url")?.trim().orEmpty()
        if (rawUrl.isBlank()) return false

        val normalizedUrl = normalizeUrl(rawUrl)
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(normalizedUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        return true
    }

    private fun findLaunchIntentByAppName(context: Context, target: String): Intent? {
        val packageManager = context.packageManager
        val launcherQuery = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val activities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                launcherQuery,
                PackageManager.ResolveInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(launcherQuery, 0)
        }

        val lowerTarget = target.lowercase()
        val matched = activities.firstOrNull { resolveInfo ->
            val label = resolveInfo.loadLabel(packageManager).toString().lowercase()
            label == lowerTarget || label.contains(lowerTarget)
        } ?: return null

        val packageName = matched.activityInfo?.packageName ?: return null
        return packageManager.getLaunchIntentForPackage(packageName)
    }

    private fun parseAutomation(raw: String): AutomationConfig? {
        return runCatching { Uri.parse(raw) }
            .getOrNull()
            ?.let { parseAutomation(it) }
    }

    private fun parseAutomation(uri: Uri?): AutomationConfig? {
        if (uri == null || uri.scheme != SCHEME || uri.host != HOST) return null
        if (uri.pathSegments.firstOrNull() != PATH_EXECUTE) return null
        val action = uri.getQueryParameter("action") ?: return null
        return AutomationConfig(action = action, uri = uri)
    }

    private fun parseLegacyAutomation(name: String): AutomationConfig? {
        return when {
            name.startsWith("Open Maps", ignoreCase = true) -> {
                parseAutomation(automationUriForLocation(null, null, "https://www.google.com/maps"))
            }
            else -> null
        }
    }

    private fun normalizeUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        return if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }

    private data class AutomationConfig(
        val action: String,
        val uri: Uri
    )
}
