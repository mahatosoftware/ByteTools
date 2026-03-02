package `in`.mahato.bytetools.data.repository

import `in`.mahato.bytetools.data.local.ScanDao
import `in`.mahato.bytetools.domain.model.ScanResult
import `in`.mahato.bytetools.domain.repository.ScanRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ScanRepositoryImpl @Inject constructor(
    private val scanDao: ScanDao
) : ScanRepository {
    override fun getScanHistory(): Flow<List<ScanResult>> = scanDao.getAllScans()
    
    override suspend fun saveScan(scan: ScanResult) {
        scanDao.insertScan(scan)
    }

    override suspend fun deleteScan(id: Int) {
        scanDao.deleteScan(id)
    }
}
