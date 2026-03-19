package `in`.mahato.bytetools.nfc

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NfcManager @Inject constructor() : NfcAdapter.ReaderCallback {

    private val _tagFlow = MutableSharedFlow<Tag>(replay = 1, extraBufferCapacity = 1)
    val tagFlow = _tagFlow.asSharedFlow()

    fun onNewIntent(intent: Intent) {
        if (intent.action in listOf(
                NfcAdapter.ACTION_TAG_DISCOVERED,
                NfcAdapter.ACTION_TECH_DISCOVERED,
                NfcAdapter.ACTION_NDEF_DISCOVERED
            )
        ) {
            val tag: Tag? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            }
            tag?.let { _tagFlow.tryEmit(it) }
        }
    }

    override fun onTagDiscovered(tag: Tag?) {
        tag?.let { _tagFlow.tryEmit(it) }
    }

    fun clearLastTag() {
        _tagFlow.resetReplayCache()
    }

    fun enableForegroundDispatch(activity: ComponentActivity) {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        if (!adapter.isEnabled) return

        // Use Reader Mode which is far more reliable and overrides system apps like Google Pay from stealing the intent.
        // It allows reading of non-NDEF tags like Credit Cards seamlessly.
        val flags = NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V

        val options = Bundle()
        adapter.enableReaderMode(activity, this, flags, options)
    }

    fun disableForegroundDispatch(activity: ComponentActivity) {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        adapter.disableReaderMode(activity)
    }
}
