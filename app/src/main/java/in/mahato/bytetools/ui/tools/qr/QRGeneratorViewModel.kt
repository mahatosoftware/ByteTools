package `in`.mahato.bytetools.ui.tools.qr

import android.graphics.Bitmap
import android.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.mahato.bytetools.data.local.QRDao
import `in`.mahato.bytetools.domain.model.QRRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QRGeneratorViewModel @Inject constructor(
    private val qrDao: QRDao
) : ViewModel() {

    private val _qrBitmap = MutableStateFlow<Bitmap?>(null)
    val qrBitmap: StateFlow<Bitmap?> = _qrBitmap.asStateFlow()

    val qrHistory: StateFlow<List<QRRecord>> = qrDao.getAllQR()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun generateQRCode(
        content: String,
        qrColor: Int = Color.BLACK,
        backgroundColor: Int = Color.WHITE,
        size: Int = 512,
        errorCorrectionLevel: ErrorCorrectionLevel = ErrorCorrectionLevel.M
    ) {
        viewModelScope.launch {
            try {
                val hints = mapOf(
                    EncodeHintType.ERROR_CORRECTION to errorCorrectionLevel,
                    EncodeHintType.MARGIN to 1
                )
                val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
                val pixels = IntArray(size * size)
                for (y in 0 until size) {
                    val offset = y * size
                    for (x in 0 until size) {
                        pixels[offset + x] = if (bitMatrix.get(x, y)) qrColor else backgroundColor
                    }
                }
                val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
                _qrBitmap.value = bitmap
                
                qrDao.insertQR(QRRecord(type = "GENERATED", content = content))
            } catch (e: Exception) {
                _qrBitmap.value = null
            }
        }
    }

    fun deleteHistory(record: QRRecord) {
        viewModelScope.launch {
            qrDao.deleteQR(record)
        }
    }
}
