package `in`.mahato.bytetools.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import `in`.mahato.bytetools.domain.model.ScanResult
import `in`.mahato.bytetools.domain.model.CalcResult
import `in`.mahato.bytetools.domain.model.BloodPressureRecord
import `in`.mahato.bytetools.domain.model.BloodSugarRecord
import `in`.mahato.bytetools.domain.model.QRRecord
import `in`.mahato.bytetools.domain.model.WheelOption
import `in`.mahato.bytetools.domain.model.WeightRecord
import `in`.mahato.bytetools.domain.model.WeightGoal
import `in`.mahato.bytetools.domain.model.UserHealthProfile

@Database(entities = [ScanResult::class, CalcResult::class, BloodPressureRecord::class, BloodSugarRecord::class, QRRecord::class, WheelOption::class, WeightRecord::class, WeightGoal::class, UserHealthProfile::class], version = 8, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scanDao(): ScanDao
    abstract fun calcDao(): CalcDao
    abstract fun healthDao(): HealthDao
    abstract fun qrDao(): QRDao
    abstract fun wheelOptionDao(): WheelOptionDao
}
