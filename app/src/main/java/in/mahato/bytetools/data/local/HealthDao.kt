package `in`.mahato.bytetools.data.local

import androidx.room.*
import `in`.mahato.bytetools.domain.model.BloodPressureRecord
import `in`.mahato.bytetools.domain.model.BloodSugarRecord
import `in`.mahato.bytetools.domain.model.WeightRecord
import `in`.mahato.bytetools.domain.model.WeightGoal
import `in`.mahato.bytetools.domain.model.UserHealthProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthDao {
    // Blood Pressure
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBloodPressure(record: BloodPressureRecord)

    @Query("SELECT * FROM blood_pressure_records ORDER BY timestamp DESC")
    fun getAllBloodPressure(): Flow<List<BloodPressureRecord>>

    @Delete
    suspend fun deleteBloodPressure(record: BloodPressureRecord)

    @Query("SELECT * FROM blood_pressure_records ORDER BY timestamp DESC LIMIT 1")
    fun getLatestBloodPressure(): Flow<BloodPressureRecord?>

    // Blood Sugar
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBloodSugar(record: BloodSugarRecord)

    @Query("SELECT * FROM blood_sugar_records ORDER BY timestamp DESC")
    fun getAllBloodSugar(): Flow<List<BloodSugarRecord>>

    @Delete
    suspend fun deleteBloodSugar(record: BloodSugarRecord)

    @Query("SELECT * FROM blood_sugar_records ORDER BY timestamp DESC LIMIT 1")
    fun getLatestBloodSugar(): Flow<BloodSugarRecord?>

    // Weight
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeight(record: WeightRecord)

    @Query("SELECT * FROM weight_records ORDER BY timestamp DESC")
    fun getAllWeight(): Flow<List<WeightRecord>>

    @Delete
    suspend fun deleteWeight(record: WeightRecord)

    @Query("SELECT * FROM weight_records ORDER BY timestamp DESC LIMIT 1")
    fun getLatestWeight(): Flow<WeightRecord?>

    @Query("SELECT * FROM weight_records ORDER BY timestamp ASC LIMIT 1")
    fun getFirstWeight(): Flow<WeightRecord?>

    // Weight Goal
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightGoal(goal: WeightGoal)

    @Query("SELECT * FROM weight_goals WHERE id = 1")
    fun getWeightGoal(): Flow<WeightGoal?>

    @Query("DELETE FROM weight_goals")
    suspend fun deleteWeightGoal()

    // Profile
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserHealthProfile)

    @Query("SELECT * FROM user_health_profile WHERE id = 1")
    fun getProfile(): Flow<UserHealthProfile?>
}
