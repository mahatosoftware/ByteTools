package `in`.mahato.bytetools.data.local

import androidx.room.*
import `in`.mahato.bytetools.domain.model.WheelOption
import kotlinx.coroutines.flow.Flow

@Dao
interface WheelOptionDao {
    @Query("SELECT * FROM wheel_options")
    fun getAllOptions(): Flow<List<WheelOption>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOption(option: WheelOption)

    @Delete
    suspend fun deleteOption(option: WheelOption)

    @Query("DELETE FROM wheel_options")
    suspend fun clearAll()
}
