package `in`.mahato.bytetools

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ByteToolsApp : Application() {
    @javax.inject.Inject
    lateinit var adManager: `in`.mahato.bytetools.utils.AdManager

    override fun onCreate() {
        super.onCreate()
        adManager.initialize(this)
        com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(this)
    }
}
