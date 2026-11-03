package com.example.ui.screens.admin

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.MistriEntity
import com.example.data.local.entity.NightShiftEntryEntity
import com.example.ui.MainViewModel
import com.example.ui.components.AppTopBar
import com.example.ui.components.FilterBar
import com.example.ui.components.StatCard
import com.example.ui.screens.mistri.ShiftEntryCard
import com.example.util.AppLanguage
import com.example.util.ExportHelper
import com.example.util.Strings

@Composable
fun AdminDashboardScreen(
    viewModel: MainViewModel,
    language: AppLanguage
) {
    val context = LocalContext.current
    val totalMistris by viewModel.totalMistriCount.collectAsState()
    val activeMistris by viewModel.activeMistriCount.collectAsState()
    val totalLabours by viewModel.totalLabourCount.collectAsState()
    val totalEntries by viewModel.totalEntryCount.collectAsState()
    val todayEntries by viewModel.todayEntryCount.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    val allMistris by viewModel.allMistris.collectAsState()
    val allLabours by viewModel.allLabours.collectAsState()
    val allEntries by viewModel.allEntries.collectAsState()
    val filteredEntries by viewModel.filteredAdminEntries.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val selectedDateFilter by viewModel.selectedDateFilter.collectAsState()
    val selectedMistriFilter by viewModel.selectedMistriFilter.collectAsState()
    val availableMonths by viewModel.availableMonths.collectAsState()

    var currentTab by remember { mutableIntStateOf(0) } // 0: Records, 1: Mistris & Labours, 2: Reports

    // Dialog states
    var editingEntry by remember { mutableStateOf<NightShiftEntryEntity?>(null) }
    var itemToDelete by remember { mutableStateOf<NightShiftEntryEntity?>(null) }
    var mistriToToggle by remember { mutableStateOf<MistriEntity?>(null) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }

    // Admin password fields
    var newAdminUser by remember { mutableStateOf("admin") }
    var newAdminPass by remember { mutableStateOf("") }
    var confirmAdminPass by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            AppTopBar(
                role = com.example.ui.UserRole.ADMIN,
                language = language,
                onToggleLanguage = { viewModel.toggleLanguage() },
                onLogout = { viewModel.logout() }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.Assignment, contentDescription = "Records") },
                    label = { Text(Strings.get("records_and_entries", language), fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("admin_nav_records")
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.Engineering, contentDescription = "Mistris") },
                    label = { Text(Strings.get("total_mistris", language), fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("admin_nav_mistris")
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Default.Analytics, contentDescription = "Reports") },
                    label = { Text(Strings.get("reports", language), fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("admin_nav_reports")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8FAFC))
        ) {
            // Persistent Quick Metric Row on Top
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    title = Strings.get("total_mistris", language),
                    value = totalMistris.toString(),
                    subtitle = "$activeMistris ${Strings.get("active", language)}",
                    icon = Icons.Default.Engineering,
                    accentColor = Color(0xFF1565C0),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = Strings.get("total_labours", language),
                    value = totalLabours.toString(),
                    icon = Icons.Default.Group,
                    accentColor = Color(0xFF00897B),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = Strings.get("total_entries", language),
                    value = totalEntries.toString(),
                    subtitle = "$todayEntries ${Strings.get("today_entries", language)}",
                    icon = Icons.Default.NightsStay,
                    accentColor = Color(0xFF0288D1),
                    modifier = Modifier.weight(1f)
                )
            }

            // Cross-Device Sync & Admin Security Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cloud Sync Button
                OutlinedButton(
                    onClick = { viewModel.syncOnlineCloud() },
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("btn_sync_cloud"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0D47A1))
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = Strings.get("sync_now", language),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                // Backup Share Button
                OutlinedButton(
                    onClick = { viewModel.exportBackup(context) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_backup_data"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Backup", fontSize = 11.sp, maxLines = 1)
                }

                // Import / Restore Button
                OutlinedButton(
                    onClick = { showImportDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_import_data"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Import", fontSize = 11.sp, maxLines = 1)
                }

                // Change Password Button
                IconButton(
                    onClick = { showChangePasswordDialog = true },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFE2E8F0), CircleShape)
                        .testTag("btn_change_admin_pass")
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Admin Security",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Export & Print Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        ExportHelper.exportToExcel(
                            context = context,
                            entries = if (currentTab == 0) filteredEntries else allEntries,
                            language = language
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("export_excel_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1E88E5))
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(Strings.get("export_excel", language), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        ExportHelper.printReport(
                            context = context,
                            entries = if (currentTab == 0) filteredEntries else allEntries,
                            title = "Night_Shift_Report",
                            language = language
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("print_report_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(Strings.get("print_report", language), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            when (currentTab) {
                0 -> {
                    // TAB 0: ALL SHIFT RECORDS WITH LIVE SEARCH, DATE-WISE & MONTH FILTERS
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp)
                    ) {
                        Spacer(modifier = Modifier.height(2.dp))
                        FilterBar(
                            searchQuery = searchQuery,
                            onSearchQueryChange = { viewModel.searchQuery.value = it },
                            selectedDate = selectedDateFilter,
                            onDateSelected = { viewModel.selectedDateFilter.value = it },
                            selectedMonth = selectedMonth,
                            onMonthSelected = { viewModel.selectedMonth.value = it },
                            availableMonths = availableMonths,
                            selectedMistriId = selectedMistriFilter,
                            onMistriSelected = { viewModel.selectedMistriFilter.value = it },
                            mistris = allMistris,
                            language = language,
                            showMistriFilter = true
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${filteredEntries.size} ${Strings.get("entries", language)}" +
                                    (if (selectedDateFilter != null) " • Date: $selectedDateFilter" else ""),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64748B)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (filteredEntries.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = Strings.get("no_entries_found", language),
                                    color = Color(0xFF64748B)
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredEntries, key = { it.id }) { entry ->
                                    ShiftEntryCard(
                                        entry = entry,
                                        language = language,
                                        canEdit = true,
                                        showMistriName = true,
                                        onEdit = { editingEntry = entry },
                                        onDelete = { itemToDelete = entry }
                                    )
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 1: MISTRIS & LABOURS MANAGEMENT
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        contentPadding = PaddingValues(top = 10.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = Strings.get("total_mistris", language),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            )
                        }

                        if (allMistris.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = Strings.get("no_mistris_found", language),
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        } else {
                            items(allMistris, key = { it.id }) { mistri ->
                                val mistriLabours = allLabours.filter { it.mistriId == mistri.id }
                                val mistriEntryCount = allEntries.count { it.mistriId == mistri.id }
                                var expandedLabours by remember { mutableStateOf(false) }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp)),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(42.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (mistri.isActive) Color(0xFFE3F2FD) else Color(0xFFFFEBEE)
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Engineering,
                                                        contentDescription = null,
                                                        tint = if (mistri.isActive) MaterialTheme.colorScheme.primary else Color(0xFFD32F2F),
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(
                                                        text = mistri.fullName,
                                                        style = MaterialTheme.typography.bodyLarge.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF0F172A)
                                                        )
                                                    )
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Phone, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = mistri.mobileNumber,
                                                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B))
                                                        )
                                                    }
                                                }
                                            }

                                            // Status Badge & Activate/Deactivate Switch
                                            Column(horizontalAlignment = Alignment.End) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (mistri.isActive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                                                ) {
                                                    Text(
                                                        text = if (mistri.isActive) Strings.get("active", language) else Strings.get("inactive", language),
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (mistri.isActive) Color(0xFF2E7D32) else Color(0xFFC62828)
                                                        ),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Switch(
                                                    checked = mistri.isActive,
                                                    onCheckedChange = { mistriToToggle = mistri },
                                                    colors = SwitchDefaults.colors(
                                                        checkedThumbColor = Color.White,
                                                        checkedTrackColor = Color(0xFF2E7D32),
                                                        uncheckedTrackColor = Color(0xFFE2E8F0)
                                                    ),
                                                    modifier = Modifier.testTag("toggle_mistri_${mistri.id}")
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Labour & Shift Count row + Collapsible Labour Details
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFF8FAFC))
                                                .clickable { expandedLabours = !expandedLabours }
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "${mistriLabours.size} ${Strings.get("total_labours", language)} | $mistriEntryCount ${Strings.get("total_entries", language)}",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color(0xFF334155)
                                                    )
                                                )
                                            }
                                            Icon(
                                                imageVector = if (expandedLabours) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Expand",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        // Collapsible Labour list
                                        AnimatedVisibility(visible = expandedLabours) {
                                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                                if (mistriLabours.isEmpty()) {
                                                    Text(
                                                        text = Strings.get("no_labours_found", language),
                                                        style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
                                                        modifier = Modifier.padding(vertical = 4.dp)
                                                    )
                                                } else {
                                                    mistriLabours.forEach { labour ->
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(vertical = 4.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Text(
                                                                text = "• ${labour.name}",
                                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                                                            )
                                                            if (labour.mobileNumber.isNotBlank()) {
                                                                Text(
                                                                    text = labour.mobileNumber,
                                                                    style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 2: REPORTS & ANALYTICS BREAKDOWN
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        contentPadding = PaddingValues(top = 10.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Mistri-wise Summary
                        item {
                            ReportCard(
                                title = Strings.get("mistri_summary", language),
                                icon = Icons.Default.Engineering,
                                language = language
                            ) {
                                val mistriStats = allEntries.groupBy { it.mistriName }
                                if (mistriStats.isEmpty()) {
                                    Text(Strings.get("no_entries_found", language), color = Color.Gray, fontSize = 12.sp)
                                } else {
                                    mistriStats.forEach { (mistri, list) ->
                                        ReportRow(label = mistri, count = list.size, language = language)
                                    }
                                }
                            }
                        }

                        // Workplace / Site-wise Summary
                        item {
                            ReportCard(
                                title = Strings.get("site_summary", language),
                                icon = Icons.Default.LocationOn,
                                language = language
                            ) {
                                val siteStats = allEntries.groupBy { it.workPlace }
                                if (siteStats.isEmpty()) {
                                    Text(Strings.get("no_entries_found", language), color = Color.Gray, fontSize = 12.sp)
                                } else {
                                    siteStats.forEach { (site, list) ->
                                        ReportRow(label = site, count = list.size, language = language)
                                    }
                                }
                            }
                        }

                        // Labour-wise Summary
                        item {
                            ReportCard(
                                title = Strings.get("labour_summary", language),
                                icon = Icons.Default.Group,
                                language = language
                            ) {
                                val labourStats = allEntries.groupBy { it.labourName }
                                if (labourStats.isEmpty()) {
                                    Text(Strings.get("no_entries_found", language), color = Color.Gray, fontSize = 12.sp)
                                } else {
                                    labourStats.forEach { (labour, list) ->
                                        ReportRow(label = labour, count = list.size, language = language)
                                    }
                                }
                            }
                        }

                        // Date-wise Summary
                        item {
                            ReportCard(
                                title = Strings.get("date_summary", language),
                                icon = Icons.Default.Assignment,
                                language = language
                            ) {
                                val dateStats = allEntries.groupBy { it.workDate }
                                if (dateStats.isEmpty()) {
                                    Text(Strings.get("no_entries_found", language), color = Color.Gray, fontSize = 12.sp)
                                } else {
                                    dateStats.entries.sortedByDescending { it.key }.take(10).forEach { (date, list) ->
                                        ReportRow(label = date, count = list.size, language = language)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Change Admin Password Dialog
    if (showChangePasswordDialog) {
        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            title = { Text(Strings.get("admin_security", language), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Update the secret username and password for Main Admin:",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newAdminUser,
                        onValueChange = { newAdminUser = it },
                        label = { Text(Strings.get("user_id", language)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newAdminPass,
                        onValueChange = { newAdminPass = it },
                        label = { Text(Strings.get("new_password", language)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmAdminPass,
                        onValueChange = { confirmAdminPass = it },
                        label = { Text(Strings.get("confirm_password", language)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newAdminUser.isBlank() || newAdminPass.isBlank()) {
                            Toast.makeText(context, Strings.get("fill_all_fields", language), Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (newAdminPass != confirmAdminPass) {
                            Toast.makeText(context, Strings.get("passwords_dont_match", language), Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.updateAdminPassword(
                            newUsername = newAdminUser,
                            newPass = newAdminPass,
                            onSuccess = {
                                showChangePasswordDialog = false
                                Toast.makeText(context, Strings.get("password_updated", language), Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                ) {
                    Text(Strings.get("save", language))
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }) {
                    Text(Strings.get("cancel", language))
                }
            }
        )
    }

    // Import Backup JSON Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text(Strings.get("import_backup", language), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Paste Backup JSON content or import all entries from another device:",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        label = { Text("Backup JSON String") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        maxLines = 6
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importJsonText.isBlank()) {
                            Toast.makeText(context, "Please enter backup JSON", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.importBackupData(importJsonText.trim())
                        showImportDialog = false
                    }
                ) {
                    Text(Strings.get("save", language))
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text(Strings.get("cancel", language))
                }
            }
        )
    }

    // Toggle Mistri Confirmation Dialog
    mistriToToggle?.let { mistri ->
        val newStatus = !mistri.isActive
        AlertDialog(
            onDismissRequest = { mistriToToggle = null },
            title = {
                Text(
                    text = if (newStatus) Strings.get("activate", language) else Strings.get("deactivate", language),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "${mistri.fullName}: ${if (newStatus) "Activate this Mistri account?" else "Deactivate this Mistri account? They will not be able to log in until reactivated."}"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.toggleMistriStatus(mistri.id, mistri.isActive)
                        mistriToToggle = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (newStatus) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(if (newStatus) Strings.get("activate", language) else Strings.get("deactivate", language))
                }
            },
            dismissButton = {
                TextButton(onClick = { mistriToToggle = null }) {
                    Text(Strings.get("cancel", language))
                }
            }
        )
    }

    // Edit Shift Entry Dialog (Admin can edit any record)
    editingEntry?.let { entry ->
        var editPlace by remember { mutableStateOf(entry.workPlace) }
        var editDate by remember { mutableStateOf(entry.workDate) }
        var editDetails by remember { mutableStateOf(entry.shiftDetails) }
        var editRemarks by remember { mutableStateOf(entry.remarks) }

        AlertDialog(
            onDismissRequest = { editingEntry = null },
            title = { Text(Strings.get("update_entry", language), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "${Strings.get("mistri_name", language)}: ${entry.mistriName}",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${Strings.get("labour_name", language)}: ${entry.labourName}",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editPlace,
                        onValueChange = { editPlace = it },
                        label = { Text(Strings.get("workplace_site", language)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editDate,
                        onValueChange = { editDate = it },
                        label = { Text(Strings.get("work_date", language)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editDetails,
                        onValueChange = { editDetails = it },
                        label = { Text(Strings.get("night_shift_details", language)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editRemarks,
                        onValueChange = { editRemarks = it },
                        label = { Text(Strings.get("remarks", language)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.submitNightShiftEntry(
                            entryId = entry.id,
                            labourId = entry.labourId,
                            labourName = entry.labourName,
                            workPlace = editPlace,
                            workDate = editDate,
                            shiftDetails = editDetails,
                            remarks = editRemarks,
                            customMistriId = entry.mistriId,
                            customMistriName = entry.mistriName,
                            onSuccess = { editingEntry = null },
                            onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                        )
                    }
                ) {
                    Text(Strings.get("save", language))
                }
            },
            dismissButton = {
                TextButton(onClick = { editingEntry = null }) {
                    Text(Strings.get("cancel", language))
                }
            }
        )
    }

    // Delete Shift Confirmation
    itemToDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text(Strings.get("delete", language), fontWeight = FontWeight.Bold) },
            text = { Text(Strings.get("confirm_delete", language)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteEntry(entry)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(Strings.get("delete", language))
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text(Strings.get("cancel", language))
                }
            }
        )
    }
}

@Composable
fun ReportCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    language: AppLanguage,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
fun ReportRow(label: String, count: Int, language: AppLanguage) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF334155))
        )
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Text(
                text = "$count ${Strings.get("entries", language)}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}
