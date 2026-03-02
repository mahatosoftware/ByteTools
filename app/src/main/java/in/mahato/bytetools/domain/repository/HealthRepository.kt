package `in`.mahato.bytetools.domain.repository

import `in`.mahato.bytetools.domain.model.BloodPressureRecord
import `in`.mahato.bytetools.domain.model.BloodSugarRecord
import `in`.mahato.bytetools.domain.model.WeightRecord
import `in`.mahato.bytetools.domain.model.WeightGoal
import `in`.mahato.bytetools.domain.model.UserHealthProfile
import kotlinx.coroutines.flow.Flow

interface HealthRepository {
    fun getAllBloodPressure(): Flow<List<BloodPressureRecord>>
    suspend fun insertBloodPressure(record: BloodPressureRecord)
    suspend fun deleteBloodPressure(record: BloodPressureRecord)
    fun getLatestBloodPressure(): Flow<BloodPressureRecord?>

    fun getAllBloodSugar(): Flow<List<BloodSugarRecord>>
    suspend fun insertBloodSugar(record: BloodSugarRecord)
    suspend fun deleteBloodSugar(record: BloodSugarRecord)
    fun getLatestBloodSugar(): Flow<BloodSugarRecord?>
    
    // Weight
    fun getAllWeight(): Flow<List<WeightRecord>>
    suspend fun insertWeight(record: WeightRecord)
    suspend fun deleteWeight(record: WeightRecord)
    fun getLatestWeight(): Flow<WeightRecord?>
    fun getFirstWeight(): Flow<WeightRecord?>
    
    // Goal
    fun getWeightGoal(): Flow<WeightGoal?>
    suspend fun insertWeightGoal(goal: WeightGoal)
    suspend fun deleteWeightGoal()

    // Profile
    fun getProfile(): Flow<UserHealthProfile?>
    suspend fun insertProfile(profile: UserHealthProfile)
}
