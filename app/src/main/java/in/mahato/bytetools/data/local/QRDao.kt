package `in`.mahato.bytetools.data.local

import androidx.room.*
import `in`.mahato.bytetools.domain.model.QRRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface QRDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQR(record: QRRecord)

    @Query("SELECT * FROM qr_records ORDER BY timestamp DESC")
    fun getAllQR(): Flow<List<QRRecord>>

    @Delete
    suspend fun deleteQR(record: QRRecord)

    @Query("DELETE FROM qr_records")
    suspend fun deleteAllQR()
}
