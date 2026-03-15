package `in`.mahato.bytetools.ui.tools.qr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.mahato.bytetools.domain.model.ScanResult
import `in`.mahato.bytetools.domain.repository.ScanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QRScannerViewModel @Inject constructor(
    private val repository: ScanRepository
) : ViewModel() {

    private val _scanHistory = MutableStateFlow<List<ScanResult>>(emptyList())
    val scanHistory: StateFlow<List<ScanResult>> = _scanHistory.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getScanHistory().collect {
                _scanHistory.value = it
            }
        }
    }

    fun onScanDetected(content: String, format: String) {
        viewModelScope.launch {
            repository.saveScan(ScanResult(content = content, format = format))
        }
    }
    
    fun deleteScanResult(id: Int) {
        viewModelScope.launch {
            repository.deleteScan(id)
        }
    }
}
