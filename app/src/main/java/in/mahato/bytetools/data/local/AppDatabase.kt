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
import `in`.mahato.bytetools.ui.tools.quickorder.data.local.*

@Database(
    entities = [
        ScanResult::class,
        CalcResult::class,
        BloodPressureRecord::class,
        BloodSugarRecord::class,
        QRRecord::class,
        WheelOption::class,
        WeightRecord::class,
        WeightGoal::class,
        UserHealthProfile::class,
        GroupOrderEntity::class,
        ParticipantEntity::class,
        ParticipantOrderItemEntity::class,
        BillItemEntity::class,
        ItemAllocationEntity::class,
        BillSummaryEntity::class,
        PaymentRecordEntity::class,
        PurchasePaymentEntity::class
    ],
    version = 11,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scanDao(): ScanDao
    abstract fun calcDao(): CalcDao
    abstract fun healthDao(): HealthDao
    abstract fun qrDao(): QRDao
    abstract fun wheelOptionDao(): WheelOptionDao
    abstract fun quickOrderDao(): QuickOrderDao
}
