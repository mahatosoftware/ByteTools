package `in`.mahato.bytetools.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "qr_records")
data class QRRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // "TEXT", "URL", "WIFI", etc.
    val barcodeFormat: String? = null,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
