package `in`.mahato.bytetools.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import `in`.mahato.bytetools.domain.model.CalcResult
import kotlinx.coroutines.flow.Flow

@Dao
interface CalcDao {
    @Query("SELECT * FROM calc_history ORDER BY timestamp DESC")
    fun getCalcHistory(): Flow<List<CalcResult>>

    @Insert
    suspend fun saveCalcResult(result: CalcResult)

    @Query("DELETE FROM calc_history")
    suspend fun clearHistory()
}
