package `in`.mahato.bytetools.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import `in`.mahato.bytetools.domain.model.ScanResult
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {
    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    fun getAllScans(): Flow<List<ScanResult>>

    @Insert
    suspend fun insertScan(scan: ScanResult)

    @Query("DELETE FROM scan_history WHERE id = :id")
    suspend fun deleteScan(id: Int)
}
