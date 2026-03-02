package `in`.mahato.bytetools.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_health_profile")
data class UserHealthProfile(
    @PrimaryKey
    val id: Int = 1,
    val heightCm: Double,
    val birthDate: Long? = null,
    val gender: String? = null
)
