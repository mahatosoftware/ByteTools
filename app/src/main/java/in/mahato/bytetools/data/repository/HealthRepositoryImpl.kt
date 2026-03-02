package `in`.mahato.bytetools.data.repository

import `in`.mahato.bytetools.data.local.HealthDao
import `in`.mahato.bytetools.domain.model.BloodPressureRecord
import `in`.mahato.bytetools.domain.model.BloodSugarRecord
import `in`.mahato.bytetools.domain.model.WeightRecord
import `in`.mahato.bytetools.domain.model.WeightGoal
import `in`.mahato.bytetools.domain.model.UserHealthProfile
import `in`.mahato.bytetools.domain.repository.HealthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class HealthRepositoryImpl @Inject constructor(
    private val healthDao: HealthDao
) : HealthRepository {
    override fun getAllBloodPressure(): Flow<List<BloodPressureRecord>> = healthDao.getAllBloodPressure()
    override suspend fun insertBloodPressure(record: BloodPressureRecord) = healthDao.insertBloodPressure(record)
    override suspend fun deleteBloodPressure(record: BloodPressureRecord) = healthDao.deleteBloodPressure(record)
    override fun getLatestBloodPressure(): Flow<BloodPressureRecord?> = healthDao.getLatestBloodPressure()

    override fun getAllBloodSugar(): Flow<List<BloodSugarRecord>> = healthDao.getAllBloodSugar()
    override suspend fun insertBloodSugar(record: BloodSugarRecord) = healthDao.insertBloodSugar(record)
    override suspend fun deleteBloodSugar(record: BloodSugarRecord) = healthDao.deleteBloodSugar(record)
    override fun getLatestBloodSugar(): Flow<BloodSugarRecord?> = healthDao.getLatestBloodSugar()

    // Weight
    override fun getAllWeight(): Flow<List<WeightRecord>> = healthDao.getAllWeight()
    override suspend fun insertWeight(record: WeightRecord) = healthDao.insertWeight(record)
    override suspend fun deleteWeight(record: WeightRecord) = healthDao.deleteWeight(record)
    override fun getLatestWeight(): Flow<WeightRecord?> = healthDao.getLatestWeight()
    override fun getFirstWeight(): Flow<WeightRecord?> = healthDao.getFirstWeight()

    // Goal
    override fun getWeightGoal(): Flow<WeightGoal?> = healthDao.getWeightGoal()
    override suspend fun insertWeightGoal(goal: WeightGoal) = healthDao.insertWeightGoal(goal)
    override suspend fun deleteWeightGoal() = healthDao.deleteWeightGoal()

    // Profile
    override fun getProfile(): Flow<UserHealthProfile?> = healthDao.getProfile()
    override suspend fun insertProfile(profile: UserHealthProfile) = healthDao.insertProfile(profile)
}
