package `in`.mahato.bytetools.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calc_history")
data class CalcResult(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val expression: String,
    val result: String,
    val timestamp: Long = System.currentTimeMillis()
)
