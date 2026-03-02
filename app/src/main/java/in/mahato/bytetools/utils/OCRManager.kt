package `in`.mahato.bytetools.utils

import android.content.Context
import android.graphics.Bitmap
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class OCRManager(private val context: Context) {
    private var tessBaseAPI: TessBaseAPI? = null
    private val tessDataPath = File(context.getExternalFilesDir(null), "tessdata").absolutePath
    private val lang = "eng"

    suspend fun init(): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = File(context.getExternalFilesDir(null), "tessdata")
            if (!root.exists()) root.mkdirs()
            
            val trainedDataFile = File(root, "$lang.traineddata")
            if (!trainedDataFile.exists()) {
                // Download from official source if missing
                val url = URL("https://github.com/tesseract-ocr/tessdata/raw/4.0.0/$lang.traineddata")
                url.openStream().use { input ->
                    FileOutputStream(trainedDataFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }

            tessBaseAPI = TessBaseAPI()
            // The path passed to init must be the PARENT of the tessdata folder
            val parentPath = context.getExternalFilesDir(null)!!.absolutePath
            tessBaseAPI?.init(parentPath, lang)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun recognizeText(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        if (tessBaseAPI == null) init()
        tessBaseAPI?.let { api ->
            api.setImage(bitmap)
            val text = api.utF8Text
            text ?: ""
        } ?: "OCR initialization failed"
    }

    fun close() {
        tessBaseAPI?.recycle()
        tessBaseAPI = null
    }
}
