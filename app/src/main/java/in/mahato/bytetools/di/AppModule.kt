package `in`.mahato.bytetools.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import `in`.mahato.bytetools.data.local.AppDatabase
import `in`.mahato.bytetools.data.local.ScanDao
import `in`.mahato.bytetools.data.local.CalcDao
import `in`.mahato.bytetools.data.local.HealthDao
import `in`.mahato.bytetools.data.local.QRDao
import `in`.mahato.bytetools.data.local.WheelOptionDao
import `in`.mahato.bytetools.data.repository.ScanRepositoryImpl
import `in`.mahato.bytetools.data.repository.HealthRepositoryImpl
import `in`.mahato.bytetools.domain.repository.ScanRepository
import `in`.mahato.bytetools.domain.repository.HealthRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "etools_pro_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideScanDao(database: AppDatabase): ScanDao = database.scanDao()

    @Provides
    fun provideCalcDao(database: AppDatabase): `in`.mahato.bytetools.data.local.CalcDao = database.calcDao()
    
    @Provides
    fun provideHealthDao(database: AppDatabase): `in`.mahato.bytetools.data.local.HealthDao = database.healthDao()

    @Provides
    fun provideQRDao(database: AppDatabase): QRDao = database.qrDao()

    @Provides
    fun provideWheelOptionDao(database: AppDatabase): WheelOptionDao = database.wheelOptionDao()

    @Provides
    @Singleton
    fun provideScanRepository(scanDao: ScanDao): ScanRepository {
        return ScanRepositoryImpl(scanDao)
    }

    @Provides
    @Singleton
    fun provideHealthRepository(healthDao: `in`.mahato.bytetools.data.local.HealthDao): HealthRepository {
        return HealthRepositoryImpl(healthDao)
    }
}
