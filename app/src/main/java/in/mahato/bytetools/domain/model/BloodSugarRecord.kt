package `in`.mahato.bytetools.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blood_sugar_records")
data class BloodSugarRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val value: Float,
    val unit: String, // "mg/dL" or "mmol/L"
    val type: String, // "Fasting", "After Meal", "Random"
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)
