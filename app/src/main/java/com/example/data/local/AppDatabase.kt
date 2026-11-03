package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.AdminDao
import com.example.data.local.dao.LabourDao
import com.example.data.local.dao.MistriDao
import com.example.data.local.dao.NightShiftDao
import com.example.data.local.entity.AdminEntity
import com.example.data.local.entity.LabourEntity
import com.example.data.local.entity.MistriEntity
import com.example.data.local.entity.NightShiftEntryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Database(
    entities = [
        MistriEntity::class,
        LabourEntity::class,
        NightShiftEntryEntity::class,
        AdminEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mistriDao(): MistriDao
    abstract fun labourDao(): LabourDao
    abstract fun nightShiftDao(): NightShiftDao
    abstract fun adminDao(): AdminDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "night_shift_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database)
                }
            }
        }

        private suspend fun populateInitialData(database: AppDatabase) {
            val adminDao = database.adminDao()
            val mistriDao = database.mistriDao()
            val labourDao = database.labourDao()
            val shiftDao = database.nightShiftDao()

            // Default Admin Account
            adminDao.insertDefaultAdmin(AdminEntity("admin", "admin123"))

            // Initial Sample Mistri
            val sampleMistriId = mistriDao.insertMistri(
                MistriEntity(
                    fullName = "Rajesh Sharma (Mistri)",
                    mobileNumber = "9876543210",
                    password = "password123",
                    isActive = true
                )
            )

            val sampleMistri2Id = mistriDao.insertMistri(
                MistriEntity(
                    fullName = "Anisur Rahman (Mistri)",
                    mobileNumber = "9123456789",
                    password = "password123",
                    isActive = true
                )
            )

            // Initial Sample Labours for Rajesh
            val l1 = labourDao.insertLabour(LabourEntity(mistriId = sampleMistriId, name = "Bikash Mondal", mobileNumber = "9800112233"))
            val l2 = labourDao.insertLabour(LabourEntity(mistriId = sampleMistriId, name = "Sunil Sardar", mobileNumber = "9800223344"))
            val l3 = labourDao.insertLabour(LabourEntity(mistriId = sampleMistriId, name = "Tarun Roy", mobileNumber = "9800334455"))

            // Initial Sample Labours for Anisur
            val l4 = labourDao.insertLabour(LabourEntity(mistriId = sampleMistri2Id, name = "Kalam Sheikh", mobileNumber = "9700112233"))
            val l5 = labourDao.insertLabour(LabourEntity(mistriId = sampleMistri2Id, name = "Raju Mallick", mobileNumber = "9700223344"))

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val today = dateFormat.format(Date())

            // Sample Shifts
            shiftDao.insertEntry(
                NightShiftEntryEntity(
                    mistriId = sampleMistriId,
                    mistriName = "Rajesh Sharma (Mistri)",
                    labourId = l1,
                    labourName = "Bikash Mondal",
                    workPlace = "Metro Line 3 Pillar 42",
                    workDate = today,
                    shiftDetails = "09:00 PM - 05:00 AM (8 hrs) - Rebar tying & shuttering",
                    remarks = "Materials delivered on time"
                )
            )

            shiftDao.insertEntry(
                NightShiftEntryEntity(
                    mistriId = sampleMistriId,
                    mistriName = "Rajesh Sharma (Mistri)",
                    labourId = l2,
                    labourName = "Sunil Sardar",
                    workPlace = "Metro Line 3 Pillar 42",
                    workDate = today,
                    shiftDetails = "09:00 PM - 05:00 AM (8 hrs) - Concrete pouring & vibrator",
                    remarks = "Smooth finish inspected"
                )
            )

            shiftDao.insertEntry(
                NightShiftEntryEntity(
                    mistriId = sampleMistri2Id,
                    mistriName = "Anisur Rahman (Mistri)",
                    labourId = l4,
                    labourName = "Kalam Sheikh",
                    workPlace = "New Hospital Wing B",
                    workDate = today,
                    shiftDetails = "08:00 PM - 04:00 AM (8 hrs) - Brick laying & plastering",
                    remarks = "Ground floor outer wall"
                )
            )
        }
    }
}
