package `in`.mahato.bytetools.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weight_records")
data class WeightRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val weightKg: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(tableName = "weight_goals")
data class WeightGoal(
    @PrimaryKey
    val id: Int = 1, // Only one goal at a time
    val targetWeightKg: Double,
    val startDate: Long = System.currentTimeMillis(),
    val targetDate: Long? = null,
    val initialWeightKg: Double
)
