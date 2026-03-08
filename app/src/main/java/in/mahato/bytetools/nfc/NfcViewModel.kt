package `in`.mahato.bytetools.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NfcViewModel @Inject constructor(
    private val nfcManager: NfcManager
) : ViewModel() {

    private val _nfcState = MutableStateFlow<NfcState>(NfcState.Idle)
    val nfcState: StateFlow<NfcState> = _nfcState.asStateFlow()

    private var currentMode: NfcMode = NfcMode.READ
    private var pendingNdefMessage: NdefMessage? = null

    init {
        viewModelScope.launch {
            nfcManager.tagFlow.collectLatest { tag ->
                handleTag(tag)
            }
        }
    }

    fun setMode(mode: NfcMode) {
        currentMode = mode
        if (mode == NfcMode.READ) {
            _nfcState.value = NfcState.Ready("Ready to scan.")
        }
    }

    fun setPendingWriteMessage(message: NdefMessage) {
        pendingNdefMessage = message
        currentMode = NfcMode.WRITE
        _nfcState.value = NfcState.Ready("Bring NFC tag near device to write.")
    }

    fun setPendingFormat() {
        currentMode = NfcMode.FORMAT
        _nfcState.value = NfcState.Ready("Bring NFC tag near device to format.")
    }

    fun setPendingLock() {
        currentMode = NfcMode.LOCK
        _nfcState.value = NfcState.Ready("Bring NFC tag near device to lock (WARNING: Irreversible).")
    }
    
    fun setPendingClone(readData: String) {
        // Simple clone for text data
        val record = NfcUtils.createTextRecord(readData)
        setPendingWriteMessage(NdefMessage(arrayOf(record)))
    }

    private fun handleTag(tag: Tag) {
        when (currentMode) {
            NfcMode.READ -> {
                val idBytes = tag.id
                val serialNumber = idBytes?.joinToString(":") { String.format("%02X", it) } ?: "Unknown"
                val techList = tag.techList?.joinToString(", ") { it.substringAfterLast(".") } ?: "Unknown"
                
                val metadata = NfcUtils.readTagMetadata(tag)
                val data = NfcUtils.readNdefMessage(tag) ?: "Failed to read tag records."
                
                val displayString = """
                    Tag ID (Hex): $serialNumber
                    
                    Supported Technologies:
                    $techList
                    
                    $metadata
                    
                    NDEF Records:
                    $data
                """.trimIndent()
                
                _nfcState.value = NfcState.Success(displayString)
            }
            NfcMode.WRITE -> {
                val msg = pendingNdefMessage
                if (msg != null) {
                    val success = NfcUtils.writeNdefMessage(tag, msg)
                    if (success) {
                        _nfcState.value = NfcState.Success("Write successful.")
                    } else {
                        _nfcState.value = NfcState.Error("Failed to write to tag.")
                    }
                } else {
                    _nfcState.value = NfcState.Error("No data to write.")
                }
            }
            NfcMode.FORMAT -> {
                val success = NfcUtils.formatTag(tag)
                if (success) {
                    _nfcState.value = NfcState.Success("Tag formatted successfully.")
                } else {
                    _nfcState.value = NfcState.Error("Failed to format tag.")
                }
            }
            NfcMode.LOCK -> {
                val success = NfcUtils.lockTag(tag)
                if (success) {
                    _nfcState.value = NfcState.Success("Tag is now permanently read-only.")
                } else {
                    _nfcState.value = NfcState.Error("Failed to lock tag.")
                }
            }
            NfcMode.CLONE_READ -> {
                val data = NfcUtils.readNdefMessage(tag) ?: ""
                _nfcState.value = NfcState.Success("Cloned Data:\n$data")
            }
        }
        
        // Return to idle after delay
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            _nfcState.value = NfcState.Ready("Ready for next operation.")
        }
    }

    fun resetState() {
        _nfcState.value = NfcState.Idle
    }
}

enum class NfcMode {
    READ, WRITE, FORMAT, LOCK, CLONE_READ
}

sealed class NfcState {
    object Idle : NfcState()
    data class Ready(val message: String) : NfcState()
    data class Success(val data: String) : NfcState()
    data class Error(val error: String) : NfcState()
}
