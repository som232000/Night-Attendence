package com.example.util

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.local.entity.AdminEntity
import com.example.data.local.entity.LabourEntity
import com.example.data.local.entity.MistriEntity
import com.example.data.local.entity.NightShiftEntryEntity
import com.example.data.repository.NightShiftRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

object FirestoreSyncManager {

    private const val TAG = "FirestoreSync"

    private var firestore: FirebaseFirestore? = null
    private var isInitialized = false

    private var mistrisListener: ListenerRegistration? = null
    private var laboursListener: ListenerRegistration? = null
    private var entriesListener: ListenerRegistration? = null
    private var adminListener: ListenerRegistration? = null

    private val _isCloudConnected = MutableStateFlow(false)
    val isCloudConnected: StateFlow<Boolean> = _isCloudConnected.asStateFlow()

    private val _lastSyncTime = MutableStateFlow(System.currentTimeMillis())
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    fun getFirestore(context: Context): FirebaseFirestore {
        firestore?.let { return it }
        val appContext = context.applicationContext

        if (FirebaseApp.getApps(appContext).isEmpty()) {
            FirebaseApp.initializeApp(appContext)
        }

        val dbId = appContext.getString(R.string.firestore_database_id)
        val instance = FirebaseFirestore.getInstance(dbId)

        try {
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            instance.firestoreSettings = settings
        } catch (_: Exception) {}

        firestore = instance
        return instance
    }

    /**
     * Starts continuous two-way real-time Firestore synchronization.
     * All devices running this app will instantly see data created, modified, or deleted
     * by any other device across the internet in real time.
     */
    fun startRealtimeSync(
        context: Context,
        repository: NightShiftRepository,
        scope: CoroutineScope
    ) {
        if (isInitialized) return
        isInitialized = true

        val db = getFirestore(context)

        // 1. Listen for real-time changes to ADMINS collection
        adminListener = db.collection("admins").document("primary_admin")
            .addSnapshotListener { doc, error ->
                if (error != null) {
                    Log.e(TAG, "Admin listen failed: ${error.message}")
                    return@addSnapshotListener
                }
                if (doc != null && doc.exists()) {
                    if (!doc.metadata.isFromCache) {
                        _isCloudConnected.value = true
                    }
                    val username = doc.getString("username") ?: return@addSnapshotListener
                    val password = doc.getString("password") ?: return@addSnapshotListener
                    scope.launch(Dispatchers.IO) {
                        repository.updateAdminCredentials(username, password)
                    }
                }
            }

        // 2. Listen for real-time changes to MISTRIS collection
        mistrisListener = db.collection("mistris").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Mistris listen failed: ${error.message}")
                _isCloudConnected.value = false
                return@addSnapshotListener
            }

            if (snapshot != null && !snapshot.metadata.isFromCache) {
                _isCloudConnected.value = true
            }
            _lastSyncTime.value = System.currentTimeMillis()

            if (snapshot != null) {
                scope.launch(Dispatchers.IO) {
                    for (change in snapshot.documentChanges) {
                        val doc = change.document
                        val mobile = (doc.getString("mobileNumber") ?: doc.id).trim()
                        val fullName = (doc.getString("fullName") ?: "Mistri").trim()
                        val password = doc.getString("password") ?: "password123"
                        val isActive = doc.getBoolean("isActive") ?: true
                        val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()

                        val existing = repository.getMistriByMobile(mobile)
                        if (change.type == DocumentChange.Type.REMOVED) {
                            if (existing != null) {
                                repository.deleteMistri(existing)
                                Log.d(TAG, "Deleted mistri removed from cloud: $mobile")
                            }
                        } else if (existing == null) {
                            repository.insertDirectMistri(
                                MistriEntity(
                                    id = 0L,
                                    fullName = fullName,
                                    mobileNumber = mobile,
                                    password = password,
                                    isActive = isActive,
                                    createdAt = createdAt
                                )
                            )
                            Log.d(TAG, "Added mistri from cloud: $fullName ($mobile)")
                        } else if (
                            existing.isActive != isActive ||
                            existing.fullName != fullName ||
                            existing.password != password
                        ) {
                            repository.updateMistri(
                                existing.copy(
                                    fullName = fullName,
                                    password = password,
                                    isActive = isActive
                                )
                            )
                            Log.d(TAG, "Updated mistri from cloud: $fullName ($isActive)")
                        }
                    }
                }
            }
        }

        // 3. Listen for real-time changes to LABOURS collection
        laboursListener = db.collection("labours").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Labours listen failed: ${error.message}")
                return@addSnapshotListener
            }

            if (snapshot != null && !snapshot.metadata.isFromCache) {
                _isCloudConnected.value = true
            }

            if (snapshot != null) {
                scope.launch(Dispatchers.IO) {
                    for (change in snapshot.documentChanges) {
                        val doc = change.document
                        val cloudId = doc.id
                        val name = (doc.getString("name") ?: "").trim()
                        if (name.isBlank()) continue

                        val mistriPhone = (doc.getString("mistriPhone") ?: "").trim()
                        val mobileNumber = (doc.getString("mobileNumber") ?: "").trim()
                        val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()

                        if (change.type == DocumentChange.Type.REMOVED) {
                            val existingByCloud = repository.getLabourByCloudId(cloudId)
                            if (existingByCloud != null) {
                                repository.deleteLabour(existingByCloud)
                                Log.d(TAG, "Deleted labour from cloud: $cloudId")
                            } else if (mistriPhone.isNotBlank()) {
                                val mistri = repository.getMistriByMobile(mistriPhone)
                                if (mistri != null) {
                                    val labour = repository.findLabourByName(mistri.id, name)
                                    if (labour != null) repository.deleteLabour(labour)
                                }
                            }
                        } else {
                            var mistri = if (mistriPhone.isNotBlank()) {
                                repository.getMistriByMobile(mistriPhone)
                            } else {
                                null
                            }

                            if (mistri == null && mistriPhone.isNotBlank()) {
                                val mistriName = doc.getString("mistriName") ?: "Mistri $mistriPhone"
                                val newId = repository.insertDirectMistri(
                                    MistriEntity(
                                        fullName = mistriName,
                                        mobileNumber = mistriPhone,
                                        password = "password123",
                                        isActive = true
                                    )
                                )
                                mistri = repository.getMistriById(newId)
                            }

                            val mistriId = mistri?.id ?: doc.getLong("mistriId") ?: 1L
                            val existing = repository.getLabourByCloudId(cloudId)
                                ?: repository.findLabourByName(mistriId, name)

                            if (existing == null) {
                                repository.insertLabourDirect(
                                    LabourEntity(
                                        id = 0L,
                                        cloudId = cloudId,
                                        mistriId = mistriId,
                                        name = name,
                                        mobileNumber = mobileNumber,
                                        createdAt = createdAt
                                    )
                                )
                                Log.d(TAG, "Added labour from cloud: $name ($cloudId)")
                            } else if (
                                existing.name != name ||
                                existing.mobileNumber != mobileNumber ||
                                existing.mistriId != mistriId ||
                                existing.cloudId != cloudId
                            ) {
                                repository.updateLabourDirect(
                                    existing.copy(
                                        cloudId = cloudId,
                                        mistriId = mistriId,
                                        name = name,
                                        mobileNumber = mobileNumber
                                    )
                                )
                                Log.d(TAG, "Updated labour from cloud: $name ($cloudId)")
                            }
                        }
                    }
                }
            }
        }

        // 4. Listen for real-time changes to NIGHT_SHIFT_ENTRIES collection
        entriesListener = db.collection("night_shift_entries").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Entries listen failed: ${error.message}")
                return@addSnapshotListener
            }

            if (snapshot != null && !snapshot.metadata.isFromCache) {
                _isCloudConnected.value = true
            }
            _lastSyncTime.value = System.currentTimeMillis()

            if (snapshot != null) {
                scope.launch(Dispatchers.IO) {
                    for (change in snapshot.documentChanges) {
                        val doc = change.document
                        val cloudId = doc.id
                        val mistriName = (doc.getString("mistriName") ?: "").trim()
                        val mistriPhone = (doc.getString("mistriPhone") ?: "").trim()
                        val labourName = (doc.getString("labourName") ?: "").trim()
                        val workPlace = (doc.getString("workPlace") ?: "").trim()
                        val workDate = (doc.getString("workDate") ?: "").trim()
                        val shiftDetails = (doc.getString("shiftDetails") ?: "").trim()
                        val remarks = (doc.getString("remarks") ?: "").trim()
                        val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()

                        if (change.type == DocumentChange.Type.REMOVED) {
                            val existing = repository.getEntryByCloudId(cloudId)
                                ?: repository.findExistingEntry(
                                    mistriName = mistriName,
                                    labourName = labourName,
                                    workPlace = workPlace,
                                    workDate = workDate,
                                    shiftDetails = shiftDetails
                                )
                            if (existing != null) {
                                repository.deleteEntry(existing)
                                Log.d(TAG, "Deleted entry removed from cloud: $cloudId")
                            }
                        } else {
                            val mistri = if (mistriPhone.isNotBlank()) {
                                repository.getMistriByMobile(mistriPhone)
                            } else {
                                null
                            }
                            val mistriId = mistri?.id ?: doc.getLong("mistriId") ?: 1L
                            val labourId = doc.getLong("labourId") ?: 0L

                            val existing = repository.getEntryByCloudId(cloudId)
                                ?: repository.findExistingEntry(
                                    mistriName = mistriName,
                                    labourName = labourName,
                                    workPlace = workPlace,
                                    workDate = workDate,
                                    shiftDetails = shiftDetails
                                )

                            if (existing == null) {
                                repository.insertEntryDirect(
                                    NightShiftEntryEntity(
                                        id = 0L,
                                        cloudId = cloudId,
                                        mistriId = mistriId,
                                        mistriName = mistriName,
                                        labourId = labourId,
                                        labourName = labourName,
                                        workPlace = workPlace,
                                        workDate = workDate,
                                        shiftDetails = shiftDetails,
                                        remarks = remarks,
                                        createdAt = createdAt
                                    )
                                )
                                Log.d(TAG, "Synced new night shift entry from cloud: $cloudId")
                            } else if (
                                existing.cloudId != cloudId ||
                                existing.mistriId != mistriId ||
                                existing.mistriName != mistriName ||
                                existing.labourName != labourName ||
                                existing.workPlace != workPlace ||
                                existing.workDate != workDate ||
                                existing.shiftDetails != shiftDetails ||
                                existing.remarks != remarks
                            ) {
                                repository.updateEntryDirect(
                                    existing.copy(
                                        cloudId = cloudId,
                                        mistriId = mistriId,
                                        mistriName = mistriName,
                                        labourName = labourName,
                                        workPlace = workPlace,
                                        workDate = workDate,
                                        shiftDetails = shiftDetails,
                                        remarks = remarks
                                    )
                                )
                                Log.d(TAG, "Updated night shift entry from cloud: $cloudId")
                            }
                        }
                    }
                }
            }
        }

        // 5. Initial Cloud Seed (only if cloud is empty; never overwrites or resurrects deleted sample data)
        scope.launch(Dispatchers.IO) {
            try {
                seedCloudIfEmpty(context, repository)
            } catch (e: Exception) {
                Log.w(TAG, "Initial cloud check notice: ${e.message}")
            }
        }
    }

    private suspend fun seedCloudIfEmpty(context: Context, repository: NightShiftRepository) {
        val db = getFirestore(context)
        val seedDoc = db.collection("admins").document("seed_status").get().await()
        _isCloudConnected.value = true
        if (seedDoc.exists()) return

        val existingMistris = db.collection("mistris").limit(1).get().await()
        val existingEntries = db.collection("night_shift_entries").limit(1).get().await()

        // Only push initial seed data if Firestore database is completely fresh/empty
        if (existingMistris.isEmpty && existingEntries.isEmpty) {
            uploadExistingLocalDataToCloud(context, repository)
        }
        db.collection("admins").document("seed_status")
            .set(hashMapOf("seeded" to true, "seededAt" to System.currentTimeMillis()))
            .await()
    }

    /**
     * Uploads any local Room records to Firestore using their stable cloudId.
     */
    suspend fun uploadExistingLocalDataToCloud(context: Context, repository: NightShiftRepository) {
        val db = getFirestore(context)

        // Ensure admin exists in cloud if not set
        val adminSnap = db.collection("admins").document("primary_admin").get().await()
        if (!adminSnap.exists()) {
            val localAdmin = repository.getPrimaryAdmin() ?: AdminEntity("admin", "admin123")
            pushAdminCredentials(context, localAdmin)
        }

        // Sync mistris
        val localMistris = repository.getAllMistris().first()
        for (m in localMistris) {
            val docRef = db.collection("mistris").document(m.mobileNumber)
            val data = hashMapOf(
                "id" to m.id,
                "fullName" to m.fullName,
                "mobileNumber" to m.mobileNumber,
                "password" to m.password,
                "isActive" to m.isActive,
                "createdAt" to m.createdAt
            )
            docRef.set(data, SetOptions.merge()).await()
        }

        // Sync labours
        val localLabours = repository.getAllLabours().first()
        for (l in localLabours) {
            val mistri = repository.getMistriById(l.mistriId)
            val phone = mistri?.mobileNumber ?: "unknown_${l.mistriId}"
            val docId = l.cloudId.ifBlank {
                val generated = UUID.randomUUID().toString()
                repository.updateLabourDirect(l.copy(cloudId = generated))
                generated
            }
            val data = hashMapOf(
                "id" to l.id,
                "cloudId" to docId,
                "mistriId" to l.mistriId,
                "mistriPhone" to phone,
                "mistriName" to (mistri?.fullName ?: ""),
                "name" to l.name,
                "mobileNumber" to l.mobileNumber,
                "createdAt" to l.createdAt
            )
            db.collection("labours").document(docId).set(data, SetOptions.merge()).await()
        }

        // Sync shift entries
        val localEntries = repository.getAllEntries().first()
        for (e in localEntries) {
            val mistri = repository.getMistriById(e.mistriId)
            val docId = e.cloudId.ifBlank {
                val generated = UUID.randomUUID().toString()
                repository.updateEntryDirect(e.copy(cloudId = generated))
                generated
            }
            val data = hashMapOf(
                "id" to e.id,
                "cloudId" to docId,
                "mistriId" to e.mistriId,
                "mistriName" to e.mistriName,
                "mistriPhone" to (mistri?.mobileNumber ?: ""),
                "labourId" to e.labourId,
                "labourName" to e.labourName,
                "workPlace" to e.workPlace,
                "workDate" to e.workDate,
                "shiftDetails" to e.shiftDetails,
                "remarks" to e.remarks,
                "createdAt" to e.createdAt
            )
            db.collection("night_shift_entries").document(docId).set(data, SetOptions.merge()).await()
        }

        _isCloudConnected.value = true
        _lastSyncTime.value = System.currentTimeMillis()
        Log.d(TAG, "Uploaded local database records to Firestore cloud successfully")
    }

    // DIRECT ONLINE AUTH & REGISTRATION HELPERS

    suspend fun verifyAdminLoginOnline(
        context: Context,
        repository: NightShiftRepository,
        username: String,
        pass: String
    ): Boolean = withContext(Dispatchers.IO) {
        val cleanUser = username.trim()
        try {
            val db = getFirestore(context)
            val doc = db.collection("admins").document("primary_admin").get().await()
            if (doc.exists()) {
                _isCloudConnected.value = true
                val cloudUser = doc.getString("username") ?: "admin"
                val cloudPass = doc.getString("password") ?: "admin123"
                repository.updateAdminCredentials(cloudUser, cloudPass)
                return@withContext (cloudUser == cleanUser && cloudPass == pass)
            } else {
                // First time admin login on cloud: seed default or current local admin to Firestore
                val localOk = repository.authenticateAdmin(cleanUser, pass)
                val primary = repository.getPrimaryAdmin() ?: AdminEntity("admin", "admin123")
                pushAdminCredentials(context, primary)
                return@withContext localOk
            }
        } catch (e: Exception) {
            Log.w(TAG, "Online admin check failed, using offline fallback: ${e.message}")
            return@withContext repository.authenticateAdmin(cleanUser, pass)
        }
    }

    suspend fun pushAdminCredentials(context: Context, admin: AdminEntity) {
        try {
            val db = getFirestore(context)
            val data = hashMapOf(
                "username" to admin.username,
                "password" to admin.password
            )
            db.collection("admins").document("primary_admin").set(data, SetOptions.merge()).await()
            _isCloudConnected.value = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to push admin credentials", e)
        }
    }

    suspend fun verifyMistriLoginOnline(
        context: Context,
        repository: NightShiftRepository,
        mobile: String,
        pass: String
    ): Result<MistriEntity> = withContext(Dispatchers.IO) {
        val cleanMobile = mobile.trim()
        try {
            val db = getFirestore(context)
            val doc = db.collection("mistris").document(cleanMobile).get().await()
            _isCloudConnected.value = true
            if (doc.exists()) {
                val fullName = (doc.getString("fullName") ?: "Mistri").trim()
                val cloudPass = doc.getString("password") ?: ""
                val isActive = doc.getBoolean("isActive") ?: true
                val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()

                var local = repository.getMistriByMobile(cleanMobile)
                if (local == null) {
                    val id = repository.insertDirectMistri(
                        MistriEntity(
                            fullName = fullName,
                            mobileNumber = cleanMobile,
                            password = cloudPass,
                            isActive = isActive,
                            createdAt = createdAt
                        )
                    )
                    local = repository.getMistriById(id)
                } else {
                    val updated = local.copy(fullName = fullName, password = cloudPass, isActive = isActive)
                    repository.updateMistri(updated)
                    local = updated
                }

                if (cloudPass != pass) {
                    return@withContext Result.failure(Exception("Incorrect password."))
                }
                if (!isActive) {
                    return@withContext Result.failure(Exception("Your account is deactivated by Admin. Please contact Admin."))
                }
                return@withContext Result.success(local!!)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Online mistri login fallback to local: ${e.message}")
        }
        return@withContext repository.loginMistri(cleanMobile, pass)
    }

    suspend fun registerMistriOnline(
        context: Context,
        repository: NightShiftRepository,
        fullName: String,
        mobile: String,
        pass: String
    ): Result<MistriEntity> = withContext(Dispatchers.IO) {
        val cleanMobile = mobile.trim()
        val cleanName = fullName.trim()
        try {
            val db = getFirestore(context)
            val doc = db.collection("mistris").document(cleanMobile).get().await()
            _isCloudConnected.value = true
            if (doc.exists()) {
                return@withContext Result.failure(Exception("Mobile number is already registered."))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Online duplicate check failed, checking local: ${e.message}")
        }

        val localResult = repository.registerMistri(cleanName, cleanMobile, pass)
        localResult.onSuccess { mistri ->
            pushMistri(context, mistri)
        }
        return@withContext localResult
    }

    // PUSH HELPERS CALLED DIRECTLY ON USER ACTIONS

    suspend fun pushMistri(context: Context, mistri: MistriEntity) {
        try {
            val db = getFirestore(context)
            val docRef = db.collection("mistris").document(mistri.mobileNumber)
            val data = hashMapOf(
                "id" to mistri.id,
                "fullName" to mistri.fullName,
                "mobileNumber" to mistri.mobileNumber,
                "password" to mistri.password,
                "isActive" to mistri.isActive,
                "createdAt" to mistri.createdAt
            )
            docRef.set(data, SetOptions.merge()).await()
            _isCloudConnected.value = true
            _lastSyncTime.value = System.currentTimeMillis()
            Log.d(TAG, "Pushed Mistri to Firestore: ${mistri.mobileNumber}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to push mistri", e)
        }
    }

    suspend fun updateMistriStatus(context: Context, mobileNumber: String, isActive: Boolean) {
        try {
            val db = getFirestore(context)
            db.collection("mistris").document(mobileNumber)
                .update("isActive", isActive).await()
            _isCloudConnected.value = true
            _lastSyncTime.value = System.currentTimeMillis()
            Log.d(TAG, "Updated Mistri status on Firestore: $mobileNumber -> $isActive")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update mistri status", e)
        }
    }

    suspend fun pushLabour(context: Context, mistriPhone: String, mistriName: String, labour: LabourEntity) {
        try {
            val db = getFirestore(context)
            val docKey = labour.cloudId.ifBlank { "${mistriPhone}_${labour.name.replace(" ", "_")}" }
            val data = hashMapOf(
                "id" to labour.id,
                "cloudId" to docKey,
                "mistriId" to labour.mistriId,
                "mistriPhone" to mistriPhone,
                "mistriName" to mistriName,
                "name" to labour.name,
                "mobileNumber" to labour.mobileNumber,
                "createdAt" to labour.createdAt
            )
            db.collection("labours").document(docKey).set(data, SetOptions.merge()).await()
            _isCloudConnected.value = true
            _lastSyncTime.value = System.currentTimeMillis()
            Log.d(TAG, "Pushed Labour to Firestore: $docKey")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to push labour", e)
        }
    }

    suspend fun deleteMistri(
        context: Context,
        mistri: MistriEntity,
        labours: List<LabourEntity>
    ) {
        try {
            val db = getFirestore(context)
            for (l in labours) {
                val docKey = l.cloudId.ifBlank { "${mistri.mobileNumber}_${l.name.replace(" ", "_")}" }
                db.collection("labours").document(docKey).delete().await()
            }
            db.collection("mistris").document(mistri.mobileNumber).delete().await()
            _isCloudConnected.value = true
            _lastSyncTime.value = System.currentTimeMillis()
            Log.d(TAG, "Deleted Mistri from Firestore: ${mistri.mobileNumber}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete mistri from Firestore", e)
        }
    }

    suspend fun deleteLabour(context: Context, mistriPhone: String, labour: LabourEntity) {
        try {
            val db = getFirestore(context)
            val docKey = labour.cloudId.ifBlank { "${mistriPhone}_${labour.name.replace(" ", "_")}" }
            db.collection("labours").document(docKey).delete().await()
            _isCloudConnected.value = true
            _lastSyncTime.value = System.currentTimeMillis()
            Log.d(TAG, "Deleted Labour from Firestore: $docKey")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete labour", e)
        }
    }

    suspend fun pushNightShiftEntry(
        context: Context,
        entry: NightShiftEntryEntity,
        mistriPhone: String = ""
    ) {
        try {
            val db = getFirestore(context)
            val docKey = entry.cloudId.ifBlank {
                "entry_${entry.mistriName.replace(" ", "_")}_${entry.labourName.replace(" ", "_")}_${entry.workDate}_${entry.createdAt}"
            }
            val data = hashMapOf(
                "id" to entry.id,
                "cloudId" to docKey,
                "mistriId" to entry.mistriId,
                "mistriName" to entry.mistriName,
                "mistriPhone" to mistriPhone,
                "labourId" to entry.labourId,
                "labourName" to entry.labourName,
                "workPlace" to entry.workPlace,
                "workDate" to entry.workDate,
                "shiftDetails" to entry.shiftDetails,
                "remarks" to entry.remarks,
                "createdAt" to entry.createdAt
            )
            db.collection("night_shift_entries").document(docKey).set(data, SetOptions.merge()).await()
            _isCloudConnected.value = true
            _lastSyncTime.value = System.currentTimeMillis()
            Log.d(TAG, "Pushed Night Shift Entry to Firestore: $docKey")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to push night shift entry", e)
        }
    }

    suspend fun deleteNightShiftEntry(context: Context, entry: NightShiftEntryEntity) {
        try {
            val db = getFirestore(context)
            val docKey = entry.cloudId.ifBlank {
                "entry_${entry.mistriName.replace(" ", "_")}_${entry.labourName.replace(" ", "_")}_${entry.workDate}_${entry.createdAt}"
            }
            db.collection("night_shift_entries").document(docKey).delete().await()
            _isCloudConnected.value = true
            _lastSyncTime.value = System.currentTimeMillis()
            Log.d(TAG, "Deleted Night Shift Entry from Firestore: $docKey")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete night shift entry", e)
        }
    }
}
