package `in`.mahato.bytetools.ui.tools.barcode

import android.graphics.Bitmap
import android.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.mahato.bytetools.data.local.QRDao
import `in`.mahato.bytetools.domain.model.QRRecord
import `in`.mahato.bytetools.domain.model.ScanResult
import `in`.mahato.bytetools.domain.repository.ScanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BarcodeViewModel @Inject constructor(
    private val repository: ScanRepository,
    private val qrDao: QRDao
) : ViewModel() {

    private val _generatedBarcode = MutableStateFlow<Bitmap?>(null)
    val generatedBarcode: StateFlow<Bitmap?> = _generatedBarcode.asStateFlow()

    val scanHistory: StateFlow<List<ScanResult>> = repository.getScanHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun generateBarcode(content: String, format: BarcodeFormat, width: Int = 800, height: Int = 400) {
        viewModelScope.launch {
            try {
                val bitMatrix: BitMatrix = MultiFormatWriter().encode(content, format, width, height)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                for (x in 0 until width) {
                    for (y in 0 until height) {
                        bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                    }
                }
                _generatedBarcode.value = bitmap
                qrDao.insertQR(
                    QRRecord(
                        type = "BARCODE",
                        barcodeFormat = format.name,
                        content = content
                    )
                )
            } catch (e: Exception) {
                _generatedBarcode.value = null
            }
        }
    }

    fun saveScanResult(content: String, format: String) {
        viewModelScope.launch {
            repository.saveScan(ScanResult(content = content, format = format))
        }
    }

    fun deleteScan(id: Int) {
        viewModelScope.launch {
            repository.deleteScan(id)
        }
    }
}
