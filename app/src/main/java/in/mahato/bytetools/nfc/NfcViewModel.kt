package `in`.mahato.bytetools.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.mahato.bytetools.domain.model.ScanResult
import `in`.mahato.bytetools.domain.repository.ScanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NfcViewModel @Inject constructor(
    private val nfcManager: NfcManager,
    private val scanRepository: ScanRepository
) : ViewModel() {

    private val _nfcState = MutableStateFlow<NfcState>(NfcState.Idle)
    val nfcState: StateFlow<NfcState> = _nfcState.asStateFlow()
    
    val savedState = MutableStateFlow(false)

    private var currentMode: NfcMode = NfcMode.READ
    private var pendingNdefMessage: NdefMessage? = null

    init {
        viewModelScope.launch {
            nfcManager.tagFlow.collectLatest { tag ->
                handleTag(tag)
                nfcManager.clearLastTag()
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
                val emvData = NfcUtils.readEmvCard(tag)
                
                val displayString = if (emvData != null) {
                    """
                        Scanner Type: EMV Payment Card
                        Tag ID (Hex): $serialNumber
                        
                        Supported Technologies:
                        $techList
                        
                        $emvData
                    """.trimIndent()
                } else {
                    val data = NfcUtils.readNdefMessage(tag) ?: "Failed to read tag records."
                    val otherStats = NfcUtils.readOtherCardStats(tag)
                    
                    val sb = StringBuilder()
                    sb.append("Scanner Type: Universal\n")
                    sb.append("Tag ID (Hex): $serialNumber\n\n")
                    
                    sb.append("Supported Technologies:\n$techList\n\n")
                    
                    if (otherStats != null) {
                        sb.append("Card Type Info:\n$otherStats\n")
                    }
                    
                    sb.append("Hardware Metadata:\n$metadata\n\n")
                    
                    val ndefLabel = if (data.contains("unformatted") || data.contains("empty") || data.contains("unsupported")) {
                        "NDEF Status:\n$data"
                    } else {
                        "NDEF Records:\n$data"
                    }
                    
                    sb.append(ndefLabel)
                    
                    sb.toString()
                }
                
                val parsedRecords = NfcUtils.getParsedNdefRecords(tag)
                
                _nfcState.value = NfcState.Success(displayString, parsedRecords)
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
        
        // Return to idle after delay only for write/format operations
        if (currentMode != NfcMode.READ && currentMode != NfcMode.CLONE_READ) {
            viewModelScope.launch {
                kotlinx.coroutines.delay(3000)
                _nfcState.value = NfcState.Ready("Ready for next operation.")
            }
        }
    }
    
    fun saveNfcScan(data: String) {
        viewModelScope.launch {
            // Save the raw text metadata log
            scanRepository.saveScan(ScanResult(content = data, format = "NFC"))
            
            // Save the parsed interactive records if they exist
            val state = _nfcState.value
            if (state is NfcState.Success) {
                state.parsedRecords.forEach { record ->
                    scanRepository.saveScan(
                        ScanResult(
                            content = "${record.type.name}::::${record.data}",
                            format = "NFC_RECORD"
                        )
                    )
                }
            }
            
            savedState.value = true
        }
    }

    fun resetState() {
        _nfcState.value = NfcState.Idle
        savedState.value = false
    }
}

enum class NfcMode {
    READ, WRITE, FORMAT, LOCK, CLONE_READ
}

sealed class NfcState {
    object Idle : NfcState()
    data class Ready(val message: String) : NfcState()
    data class Success(val data: String, val parsedRecords: List<ParsedNdefRecord> = emptyList()) : NfcState()
    data class Error(val error: String) : NfcState()
}
