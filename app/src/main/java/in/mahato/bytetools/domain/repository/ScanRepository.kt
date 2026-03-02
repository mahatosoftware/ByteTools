package `in`.mahato.bytetools.domain.repository

import `in`.mahato.bytetools.domain.model.ScanResult
import kotlinx.coroutines.flow.Flow

interface ScanRepository {
    fun getScanHistory(): Flow<List<ScanResult>>
    suspend fun saveScan(scan: ScanResult)
    suspend fun deleteScan(id: Int)
}
