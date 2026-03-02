package `in`.mahato.bytetools.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.net.URLEncoder

object WhatsAppUtils {

    /**
     * Opens a WhatsApp chat with a specific phone number using https://wa.me/ format.
     * Integrates an optional pre-filled message and provides a fallback to the browser 
     * (or optionally Google Play) if the app isn't installed.
     *
     * @param context The Context to start the intent.
     * @param phoneNumber The phone number in international format (e.g., 919876543210).
     * @param message An optional pre-filled text message.
     */
    fun openChat(context: Context, phoneNumber: String, message: String? = null) {
        val cleanPhone = phoneNumber.replace(Regex("[^0-9]"), "")

        val isWhatsAppInstalled = isPackageInstalled(context, "com.whatsapp")
        val isWaBusinessInstalled = isPackageInstalled(context, "com.whatsapp.w4b")

        try {
            if (isWhatsAppInstalled && isWaBusinessInstalled) {
                // Both installed: create explicit intents and show them in a custom chooser
                val intents = mutableListOf<Intent>()
                
                val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone" + if (!message.isNullOrBlank()) "&text=${URLEncoder.encode(message, "UTF-8")}" else "")
                
                val waIntent = Intent(Intent.ACTION_VIEW, uri)
                waIntent.setPackage("com.whatsapp")
                intents.add(waIntent)

                val waBusinessIntent = Intent(Intent.ACTION_VIEW, uri)
                waBusinessIntent.setPackage("com.whatsapp.w4b")
                intents.add(waBusinessIntent)

                // Create a chooser with the first intent, and add the second as EXTRA_INITIAL_INTENTS
                val chooser = Intent.createChooser(intents.first(), "Select WhatsApp")
                chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(intents[1]))
                context.startActivity(chooser)
                
            } else if (isWaBusinessInstalled) {
                // ONLY WA Business is installed, force it
                val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone" + if (!message.isNullOrBlank()) "&text=${URLEncoder.encode(message, "UTF-8")}" else "")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                intent.setPackage("com.whatsapp.w4b")
                context.startActivity(intent)
            } else if (isWhatsAppInstalled) {
                // ONLY normal WA is installed, force it
                val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone" + if (!message.isNullOrBlank()) "&text=${URLEncoder.encode(message, "UTF-8")}" else "")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                intent.setPackage("com.whatsapp")
                context.startActivity(intent)
            } else {
                // Neither is installed, fallback to Play Store
                try {
                    val storeIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=com.whatsapp")
                    )
                    context.startActivity(storeIntent)
                } catch (playStoreEx: ActivityNotFoundException) {
                    Toast.makeText(
                        context,
                        "Unable to open WhatsApp. Please install it to continue.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } catch (e: ActivityNotFoundException) {
            // This catch block would typically handle cases where the explicit intent fails,
            // but with the package checks, it's less likely to be hit for the main WhatsApp apps.
            // It might catch issues if the package is reported installed but the activity isn't found.
            Toast.makeText(
                context,
                "WhatsApp app not found or an error occurred. Please ensure it's installed.",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Toast.makeText(context, "An unexpected error occurred: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }
}
