package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.AdminEntity
import com.example.data.local.entity.LabourEntity
import com.example.data.local.entity.MistriEntity
import com.example.data.local.entity.NightShiftEntryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

class NightShiftRepository(private val database: AppDatabase) {

    private val mistriDao = database.mistriDao()
    private val labourDao = database.labourDao()
    private val shiftDao = database.nightShiftDao()
    private val adminDao = database.adminDao()

    // Admin Operations
    suspend fun authenticateAdmin(username: String, pass: String): Boolean {
        adminDao.insertDefaultAdmin(AdminEntity("admin", "admin123"))
        val admin = adminDao.getAdmin(username.trim()) ?: return false
        return admin.password == pass
    }

    suspend fun getPrimaryAdmin(): AdminEntity? {
        adminDao.insertDefaultAdmin(AdminEntity("admin", "admin123"))
        return adminDao.getPrimaryAdmin()
    }

    suspend fun updateAdminCredentials(newUsername: String, newPass: String) {
        adminDao.deleteAllAdmins()
        adminDao.setAdmin(AdminEntity(username = newUsername.trim(), password = newPass))
    }

    // Mistri Operations
    suspend fun registerMistri(fullName: String, mobile: String, pass: String): Result<MistriEntity> {
        val existing = mistriDao.getMistriByMobile(mobile.trim())
        if (existing != null) {
            return Result.failure(Exception("Mobile number is already registered."))
        }
        val newMistri = MistriEntity(
            fullName = fullName.trim(),
            mobileNumber = mobile.trim(),
            password = pass,
            isActive = true
        )
        val id = mistriDao.insertMistri(newMistri)
        return Result.success(newMistri.copy(id = id))
    }

    suspend fun loginMistri(mobile: String, pass: String): Result<MistriEntity> {
        val mistri = mistriDao.getMistriByMobile(mobile.trim())
            ?: return Result.failure(Exception("Mistri account not found with this mobile number."))

        if (mistri.password != pass) {
            return Result.failure(Exception("Incorrect password."))
        }

        if (!mistri.isActive) {
            return Result.failure(Exception("Your account is deactivated by Admin. Please contact Admin."))
        }

        return Result.success(mistri)
    }

    suspend fun updateMistriStatus(mistriId: Long, isActive: Boolean) {
        mistriDao.updateMistriStatus(mistriId, isActive)
    }

    fun getAllMistris(): Flow<List<MistriEntity>> = mistriDao.getAllMistris()

    fun getMistriCount(): Flow<Int> = mistriDao.getMistriCount()
    fun getActiveMistriCount(): Flow<Int> = mistriDao.getActiveMistriCount()

    // Labour Operations
    fun getLaboursForMistri(mistriId: Long): Flow<List<LabourEntity>> = labourDao.getLaboursByMistri(mistriId)
    fun getAllLabours(): Flow<List<LabourEntity>> = labourDao.getAllLabours()
    fun getTotalLabourCount(): Flow<Int> = labourDao.getTotalLabourCount()

    suspend fun saveLabour(mistriId: Long, name: String, mobile: String, labourId: Long = 0): Long {
        val entity = LabourEntity(
            id = labourId,
            mistriId = mistriId,
            name = name.trim(),
            mobileNumber = mobile.trim()
        )
        return if (labourId == 0L) {
            labourDao.insertLabour(entity)
        } else {
            labourDao.updateLabour(entity)
            labourId
        }
    }

    suspend fun deleteLabour(labour: LabourEntity) {
        labourDao.deleteLabour(labour)
    }

    // Night Shift Operations
    fun getAllEntries(): Flow<List<NightShiftEntryEntity>> = shiftDao.getAllEntries()
    fun getEntriesForMistri(mistriId: Long): Flow<List<NightShiftEntryEntity>> = shiftDao.getEntriesByMistri(mistriId)
    fun getTotalEntryCount(): Flow<Int> = shiftDao.getTotalEntryCount()
    fun getTodayEntryCount(today: String): Flow<Int> = shiftDao.getEntryCountForDate(today)
    fun getAllWorkPlaces(): Flow<List<String>> = shiftDao.getAllWorkPlaces()
    fun getAvailableMonths(): Flow<List<String>> = shiftDao.getAvailableMonths()

    suspend fun saveNightShiftEntry(
        id: Long = 0,
        mistriId: Long,
        mistriName: String,
        labourId: Long,
        labourName: String,
        workPlace: String,
        workDate: String,
        shiftDetails: String,
        remarks: String
    ): Long {
        val entry = NightShiftEntryEntity(
            id = id,
            mistriId = mistriId,
            mistriName = mistriName.trim(),
            labourId = labourId,
            labourName = labourName.trim(),
            workPlace = workPlace.trim(),
            workDate = workDate.trim(),
            shiftDetails = shiftDetails.trim(),
            remarks = remarks.trim()
        )
        return if (id == 0L) {
            shiftDao.insertEntry(entry)
        } else {
            shiftDao.updateEntry(entry)
            id
        }
    }

    suspend fun deleteEntry(entry: NightShiftEntryEntity) {
        shiftDao.deleteEntry(entry)
    }

    suspend fun deleteEntryById(id: Long) {
        shiftDao.deleteEntryById(id)
    }

    // CROSS-DEVICE DATA SYNC / BACKUP / RESTORE
    suspend fun exportDatabaseJson(): String {
        val root = JSONObject()
        val mistris = mistriDao.getAllMistris().first()
        val labours = labourDao.getAllLabours().first()
        val entries = shiftDao.getAllEntries().first()

        val mistrisArray = JSONArray()
        mistris.forEach { m ->
            val obj = JSONObject()
            obj.put("id", m.id)
            obj.put("fullName", m.fullName)
            obj.put("mobileNumber", m.mobileNumber)
            obj.put("password", m.password)
            obj.put("isActive", m.isActive)
            obj.put("createdAt", m.createdAt)
            mistrisArray.put(obj)
        }

        val laboursArray = JSONArray()
        labours.forEach { l ->
            val obj = JSONObject()
            obj.put("id", l.id)
            obj.put("mistriId", l.mistriId)
            obj.put("name", l.name)
            obj.put("mobileNumber", l.mobileNumber)
            obj.put("createdAt", l.createdAt)
            laboursArray.put(obj)
        }

        val entriesArray = JSONArray()
        entries.forEach { e ->
            val obj = JSONObject()
            obj.put("id", e.id)
            obj.put("mistriId", e.mistriId)
            obj.put("mistriName", e.mistriName)
            obj.put("labourId", e.labourId)
            obj.put("labourName", e.labourName)
            obj.put("workPlace", e.workPlace)
            obj.put("workDate", e.workDate)
            obj.put("shiftDetails", e.shiftDetails)
            obj.put("remarks", e.remarks)
            obj.put("createdAt", e.createdAt)
            entriesArray.put(obj)
        }

        root.put("mistris", mistrisArray)
        root.put("labours", laboursArray)
        root.put("entries", entriesArray)
        root.put("exportedAt", System.currentTimeMillis())
        return root.toString(2)
    }

    suspend fun importDatabaseJson(jsonString: String): Int {
        val root = JSONObject(jsonString)
        var importedCount = 0

        if (root.has("mistris")) {
            val mistrisArray = root.getJSONArray("mistris")
            for (i in 0 until mistrisArray.length()) {
                val obj = mistrisArray.getJSONObject(i)
                val mobile = obj.getString("mobileNumber")
                val existing = mistriDao.getMistriByMobile(mobile)
                if (existing == null) {
                    mistriDao.insertMistri(
                        MistriEntity(
                            id = obj.optLong("id", 0L),
                            fullName = obj.getString("fullName"),
                            mobileNumber = mobile,
                            password = obj.optString("password", "password123"),
                            isActive = obj.optBoolean("isActive", true),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }
        }

        if (root.has("labours")) {
            val laboursArray = root.getJSONArray("labours")
            for (i in 0 until laboursArray.length()) {
                val obj = laboursArray.getJSONObject(i)
                labourDao.insertLabour(
                    LabourEntity(
                        id = obj.optLong("id", 0L),
                        mistriId = obj.getLong("mistriId"),
                        name = obj.getString("name"),
                        mobileNumber = obj.optString("mobileNumber", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        if (root.has("entries")) {
            val entriesArray = root.getJSONArray("entries")
            for (i in 0 until entriesArray.length()) {
                val obj = entriesArray.getJSONObject(i)
                shiftDao.insertEntry(
                    NightShiftEntryEntity(
                        id = obj.optLong("id", 0L),
                        mistriId = obj.getLong("mistriId"),
                        mistriName = obj.getString("mistriName"),
                        labourId = obj.optLong("labourId", 0L),
                        labourName = obj.getString("labourName"),
                        workPlace = obj.getString("workPlace"),
                        workDate = obj.getString("workDate"),
                        shiftDetails = obj.getString("shiftDetails"),
                        remarks = obj.optString("remarks", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
                importedCount++
            }
        }

        return importedCount
    }
}
