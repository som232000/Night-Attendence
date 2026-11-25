package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.LabourEntity
import com.example.data.local.entity.MistriEntity
import com.example.data.local.entity.NightShiftEntryEntity
import com.example.data.repository.NightShiftRepository
import com.example.util.AppLanguage
import com.example.util.FirestoreSyncManager
import com.example.util.OnlineSyncManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class UserRole {
    GUEST, ADMIN, MISTRI
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NightShiftRepository

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = NightShiftRepository(database)
        // Automatically start real-time continuous Cloud Firestore synchronization across all devices
        FirestoreSyncManager.startRealtimeSync(application, repository, viewModelScope)
    }

    val isCloudConnected: StateFlow<Boolean> = FirestoreSyncManager.isCloudConnected
    val lastSyncTime: StateFlow<Long> = FirestoreSyncManager.lastSyncTime

    // Language state
    private val _language = MutableStateFlow(AppLanguage.BENGALI) // Bengali by default as requested
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    fun toggleLanguage() {
        _language.value = if (_language.value == AppLanguage.ENGLISH) {
            AppLanguage.BENGALI
        } else {
            AppLanguage.ENGLISH
        }
    }

    fun setLanguage(lang: AppLanguage) {
        _language.value = lang
    }

    // Auth & Navigation State
    private val _userRole = MutableStateFlow(UserRole.GUEST)
    val userRole: StateFlow<UserRole> = _userRole.asStateFlow()

    private val _currentMistri = MutableStateFlow<MistriEntity?>(null)
    val currentMistri: StateFlow<MistriEntity?> = _currentMistri.asStateFlow()

    // Feedback messages
    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    // Sync in progress state
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // Search and Filter criteria
    val searchQuery = MutableStateFlow("")
    val selectedMonth = MutableStateFlow<String?>(null) // e.g. "2026-11"
    val selectedDateFilter = MutableStateFlow<String?>(null) // e.g. "2026-11-03" (DATE-WISE FILTER)
    val selectedMistriFilter = MutableStateFlow<Long?>(null) // null = all

    // Data streams from repository
    val allMistris: StateFlow<List<MistriEntity>> = repository.getAllMistris()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLabours: StateFlow<List<LabourEntity>> = repository.getAllLabours()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allEntries: StateFlow<List<NightShiftEntryEntity>> = repository.getAllEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalMistriCount = repository.getMistriCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val activeMistriCount = repository.getActiveMistriCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalLabourCount = repository.getTotalLabourCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalEntryCount = repository.getTotalEntryCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val todayString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val todayEntryCount = repository.getTodayEntryCount(todayString)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val availableMonths = repository.getAvailableMonths()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableSites = repository.getAllWorkPlaces()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Mistri-specific Labour list
    val currentMistriLabours: StateFlow<List<LabourEntity>> = combine(
        allLabours,
        _currentMistri
    ) { labours, mistri ->
        if (mistri == null) emptyList()
        else labours.filter { it.mistriId == mistri.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Mistri-specific entries
    val currentMistriEntries: StateFlow<List<NightShiftEntryEntity>> = combine(
        allEntries,
        _currentMistri
    ) { entries, mistri ->
        if (mistri == null) emptyList()
        else entries.filter { it.mistriId == mistri.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered entries for Admin panel with live search, date-wise filter, month filter, and mistri filter
    val filteredAdminEntries: StateFlow<List<NightShiftEntryEntity>> = combine(
        allEntries,
        searchQuery,
        selectedMonth,
        selectedDateFilter,
        selectedMistriFilter
    ) { entries, query, month, dateFilter, mistriId ->
        entries.filter { item ->
            val matchesQuery = query.isBlank() ||
                item.mistriName.contains(query, ignoreCase = true) ||
                item.labourName.contains(query, ignoreCase = true) ||
                item.workPlace.contains(query, ignoreCase = true) ||
                item.workDate.contains(query, ignoreCase = true) ||
                item.shiftDetails.contains(query, ignoreCase = true) ||
                item.remarks.contains(query, ignoreCase = true)

            val matchesMonth = month == null || item.workDate.startsWith(month)
            val matchesDate = dateFilter == null || item.workDate == dateFilter
            val matchesMistri = mistriId == null || item.mistriId == mistriId

            matchesQuery && matchesMonth && matchesDate && matchesMistri
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered entries for Mistri panel with live search, date-wise filter, and month filter
    val filteredMistriEntries: StateFlow<List<NightShiftEntryEntity>> = combine(
        currentMistriEntries,
        searchQuery,
        selectedMonth,
        selectedDateFilter
    ) { entries, query, month, dateFilter ->
        entries.filter { item ->
            val matchesQuery = query.isBlank() ||
                item.labourName.contains(query, ignoreCase = true) ||
                item.workPlace.contains(query, ignoreCase = true) ||
                item.workDate.contains(query, ignoreCase = true) ||
                item.shiftDetails.contains(query, ignoreCase = true) ||
                item.remarks.contains(query, ignoreCase = true)

            val matchesMonth = month == null || item.workDate.startsWith(month)
            val matchesDate = dateFilter == null || item.workDate == dateFilter

            matchesQuery && matchesMonth && matchesDate
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // AUTH ACTIONS
    fun loginAdmin(user: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val success = FirestoreSyncManager.verifyAdminLoginOnline(
                context = getApplication(),
                repository = repository,
                username = user.trim(),
                pass = pass
            )
            if (success) {
                _userRole.value = UserRole.ADMIN
                _currentMistri.value = null
                onSuccess()
            } else {
                onError("Access Denied: Only authorized Main Admin can login.")
            }
        }
    }

    fun updateAdminPassword(newUsername: String, newPass: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.updateAdminCredentials(newUsername, newPass)
            launch {
                FirestoreSyncManager.pushAdminCredentials(
                    getApplication(),
                    com.example.data.local.entity.AdminEntity(newUsername.trim(), newPass)
                )
            }
            _toastEvent.emit("Admin credentials updated & synced online!")
            onSuccess()
        }
    }

    fun loginMistri(mobile: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = FirestoreSyncManager.verifyMistriLoginOnline(
                context = getApplication(),
                repository = repository,
                mobile = mobile,
                pass = pass
            )
            result.onSuccess { mistri ->
                _currentMistri.value = mistri
                _userRole.value = UserRole.MISTRI
                onSuccess()
            }.onFailure { ex ->
                onError(ex.message ?: "Login failed")
            }
        }
    }

    fun registerMistri(
        name: String,
        mobile: String,
        pass: String,
        onSuccess: (MistriEntity) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = FirestoreSyncManager.registerMistriOnline(
                context = getApplication(),
                repository = repository,
                fullName = name,
                mobile = mobile,
                pass = pass
            )
            result.onSuccess { mistri ->
                _currentMistri.value = mistri
                _userRole.value = UserRole.MISTRI
                onSuccess(mistri)
            }.onFailure { ex ->
                onError(ex.message ?: "Registration failed")
            }
        }
    }

    fun logout() {
        _userRole.value = UserRole.GUEST
        _currentMistri.value = null
        searchQuery.value = ""
        selectedMonth.value = null
        selectedDateFilter.value = null
        selectedMistriFilter.value = null
    }

    // ADMIN ACTIONS
    fun toggleMistriStatus(mistriId: Long, currentStatus: Boolean) {
        viewModelScope.launch {
            repository.updateMistriStatus(mistriId, !currentStatus)
            _toastEvent.emit(if (!currentStatus) "Mistri account activated" else "Mistri account deactivated")
            val mistri = repository.getMistriById(mistriId)
            if (mistri != null) {
                launch {
                    FirestoreSyncManager.updateMistriStatus(getApplication(), mistri.mobileNumber, !currentStatus)
                }
            }
        }
    }

    fun deleteMistri(mistri: MistriEntity) {
        viewModelScope.launch {
            val labours = repository.getLaboursListByMistri(mistri.id)
            repository.deleteMistri(mistri)
            if (_currentMistri.value?.id == mistri.id) {
                logout()
            }
            _toastEvent.emit("Mistri deleted")
            launch {
                FirestoreSyncManager.deleteMistri(getApplication(), mistri, labours)
            }
        }
    }

    // LABOUR ACTIONS
    fun saveLabour(name: String, mobile: String, labourId: Long = 0, onSuccess: () -> Unit) {
        val mistri = _currentMistri.value ?: return
        viewModelScope.launch {
            val savedId = repository.saveLabour(mistri.id, name, mobile, labourId)
            _toastEvent.emit("Labour details saved")
            val savedEntity = repository.getLabourById(savedId)
            if (savedEntity != null) {
                launch {
                    FirestoreSyncManager.pushLabour(getApplication(), mistri.mobileNumber, mistri.fullName, savedEntity)
                }
            }
            onSuccess()
        }
    }

    fun deleteLabour(labour: LabourEntity) {
        val mistri = _currentMistri.value
        viewModelScope.launch {
            repository.deleteLabour(labour)
            _toastEvent.emit("Labour removed")
            launch {
                val phone = mistri?.mobileNumber ?: repository.getMistriById(labour.mistriId)?.mobileNumber ?: ""
                FirestoreSyncManager.deleteLabour(getApplication(), phone, labour)
            }
        }
    }

    // NIGHT SHIFT ENTRY ACTIONS
    fun submitNightShiftEntry(
        entryId: Long = 0,
        labourId: Long,
        labourName: String,
        workPlace: String,
        workDate: String,
        shiftDetails: String,
        remarks: String,
        customMistriId: Long? = null,
        customMistriName: String? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val mistriId = customMistriId ?: _currentMistri.value?.id ?: 0L
        val mistriName = customMistriName ?: _currentMistri.value?.fullName ?: ""

        if (mistriId == 0L || mistriName.isBlank()) {
            onError("Mistri account session required")
            return
        }

        viewModelScope.launch {
            try {
                val savedId = repository.saveNightShiftEntry(
                    id = entryId,
                    mistriId = mistriId,
                    mistriName = mistriName,
                    labourId = labourId,
                    labourName = labourName,
                    workPlace = workPlace,
                    workDate = workDate,
                    shiftDetails = shiftDetails,
                    remarks = remarks
                )
                _toastEvent.emit("Shift entry saved successfully")
                val mistri = repository.getMistriById(mistriId)
                val savedEntry = repository.getEntryById(savedId)
                if (savedEntry != null) {
                    // Instantly sync to Firestore cloud so Admin device sees this entry in real time
                    launch {
                        FirestoreSyncManager.pushNightShiftEntry(
                            context = getApplication(),
                            entry = savedEntry,
                            mistriPhone = mistri?.mobileNumber ?: ""
                        )
                    }
                }
                onSuccess()
            } catch (e: Exception) {
                onError("Failed to save entry: ${e.localizedMessage}")
            }
        }
    }

    fun deleteEntry(entry: NightShiftEntryEntity) {
        viewModelScope.launch {
            repository.deleteEntry(entry)
            _toastEvent.emit("Shift record deleted")
            launch {
                FirestoreSyncManager.deleteNightShiftEntry(getApplication(), entry)
            }
        }
    }

    // CROSS-DEVICE CLOUD SYNC & BACKUP ACTIONS
    fun syncOnlineCloud() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                FirestoreSyncManager.uploadExistingLocalDataToCloud(getApplication(), repository)
                _toastEvent.emit("Online Cloud: All records synced in real-time!")
            } catch (e: Exception) {
                _toastEvent.emit("Sync notice: ${e.message}")
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun exportBackup(context: Context) {
        viewModelScope.launch {
            OnlineSyncManager.exportBackupFile(context, repository, _language.value)
        }
    }

    fun importBackupData(jsonString: String) {
        viewModelScope.launch {
            try {
                val count = repository.importDatabaseJson(jsonString)
                _toastEvent.emit("Successfully imported $count entries!")
            } catch (e: Exception) {
                _toastEvent.emit("Import error: ${e.message}")
            }
        }
    }
}
