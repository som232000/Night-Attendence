package com.example.ui.screens.mistri

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.LabourEntity
import com.example.data.local.entity.NightShiftEntryEntity
import com.example.ui.MainViewModel
import com.example.ui.components.AppTopBar
import com.example.ui.components.FilterBar
import com.example.ui.components.StatCard
import com.example.util.AppLanguage
import com.example.util.Strings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MistriHomeScreen(
    viewModel: MainViewModel,
    language: AppLanguage
) {
    val context = LocalContext.current
    val currentMistri by viewModel.currentMistri.collectAsState()
    val mistriLabours by viewModel.currentMistriLabours.collectAsState()
    val mistriEntries by viewModel.filteredMistriEntries.collectAsState()
    val allMistriEntries by viewModel.currentMistriEntries.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val selectedDateFilter by viewModel.selectedDateFilter.collectAsState()
    val availableMonths by viewModel.availableMonths.collectAsState()

    var currentTab by remember { mutableIntStateOf(0) } // 0: New Entry, 1: My Records, 2: Labours

    // Labour Dialog State
    var showLabourDialog by remember { mutableStateOf(false) }
    var editingLabour by remember { mutableStateOf<LabourEntity?>(null) }
    var labourNameInput by remember { mutableStateOf("") }
    var labourMobileInput by remember { mutableStateOf("") }

    // Shift Entry Form State
    val todayFormatted = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    var selectedLabourForEntry by remember { mutableStateOf<LabourEntity?>(null) }
    var workPlaceInput by remember { mutableStateOf("") }
    var workDateInput by remember { mutableStateOf(todayFormatted) }
    var shiftDetailsInput by remember { mutableStateOf("") }
    var remarksInput by remember { mutableStateOf("") }
    var labourDropdownExpanded by remember { mutableStateOf(false) }

    // Editing Shift Entry State
    var editingEntry by remember { mutableStateOf<NightShiftEntryEntity?>(null) }

    // Delete Confirmation
    var itemToDelete by remember { mutableStateOf<NightShiftEntryEntity?>(null) }
    var labourToDelete by remember { mutableStateOf<LabourEntity?>(null) }

    Scaffold(
        topBar = {
            AppTopBar(
                role = com.example.ui.UserRole.MISTRI,
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
                    icon = { Icon(Icons.Default.PostAdd, contentDescription = "New Entry") },
                    label = { Text(Strings.get("new_shift_entry", language), fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_mistri_new_entry")
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.Assignment, contentDescription = "Records") },
                    label = { Text(Strings.get("records_and_entries", language), fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_mistri_records")
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Default.Group, contentDescription = "Labours") },
                    label = { Text(Strings.get("labour_management", language), fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_mistri_labours")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8FAFC))
        ) {
            when (currentTab) {
                0 -> {
                    // TAB 0: NEW NIGHT SHIFT ENTRY FORM
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            // Welcome Mistri Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = currentMistri?.fullName ?: "Mistri",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        )
                                        Text(
                                            text = "${Strings.get("mobile_number", language)}: ${currentMistri?.mobileNumber ?: ""}",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            // Entry Form Card
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Text(
                                        text = Strings.get("new_shift_entry", language),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Field 1: Mistri Name (Auto-selected & locked)
                                    OutlinedTextField(
                                        value = currentMistri?.fullName ?: "",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text(Strings.get("mistri_name", language)) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("entry_mistri_name_field"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            disabledContainerColor = Color(0xFFF1F5F9),
                                            focusedContainerColor = Color(0xFFF8FAFC)
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Field 2: Labour Name (Dropdown from Mistri's Labour list)
                                    if (mistriLabours.isEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0xFFFFF9C4))
                                                .padding(12.dp)
                                        ) {
                                            Column {
                                                Text(
                                                    text = Strings.get("no_labours_found", language),
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF854D0E))
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                OutlinedButton(
                                                    onClick = {
                                                        editingLabour = null
                                                        labourNameInput = ""
                                                        labourMobileInput = ""
                                                        showLabourDialog = true
                                                    },
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(Strings.get("add_labour", language), fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    } else {
                                        ExposedDropdownMenuBox(
                                            expanded = labourDropdownExpanded,
                                            onExpandedChange = { labourDropdownExpanded = !labourDropdownExpanded }
                                        ) {
                                            OutlinedTextField(
                                                value = selectedLabourForEntry?.name ?: "",
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text(Strings.get("select_labour", language)) },
                                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = labourDropdownExpanded) },
                                                leadingIcon = { Icon(Icons.Default.Group, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                                modifier = Modifier
                                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                                    .fillMaxWidth()
                                                    .testTag("entry_labour_dropdown"),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            ExposedDropdownMenu(
                                                expanded = labourDropdownExpanded,
                                                onDismissRequest = { labourDropdownExpanded = false }
                                            ) {
                                                mistriLabours.forEach { labour ->
                                                    DropdownMenuItem(
                                                        text = {
                                                            Column {
                                                                Text(labour.name, fontWeight = FontWeight.Bold)
                                                                if (labour.mobileNumber.isNotBlank()) {
                                                                    Text(labour.mobileNumber, fontSize = 11.sp, color = Color.Gray)
                                                                }
                                                            }
                                                        },
                                                        onClick = {
                                                            selectedLabourForEntry = labour
                                                            labourDropdownExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Field 3: Work Place / Site Name
                                    OutlinedTextField(
                                        value = workPlaceInput,
                                        onValueChange = { workPlaceInput = it },
                                        label = { Text(Strings.get("workplace_site", language)) },
                                        placeholder = { Text("e.g. City Mall Tower B, Floor 4") },
                                        leadingIcon = {
                                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("entry_workplace_field"),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Field 4: Work Date (YYYY-MM-DD)
                                    OutlinedTextField(
                                        value = workDateInput,
                                        onValueChange = { workDateInput = it },
                                        label = { Text(Strings.get("work_date", language)) },
                                        leadingIcon = {
                                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("entry_date_field"),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Field 5: Night Shift Details
                                    OutlinedTextField(
                                        value = shiftDetailsInput,
                                        onValueChange = { shiftDetailsInput = it },
                                        label = { Text(Strings.get("night_shift_details", language)) },
                                        placeholder = { Text("e.g. 08:00 PM - 04:00 AM (8 hrs) - Shuttering & Casting") },
                                        leadingIcon = {
                                            Icon(Icons.Default.NightsStay, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("entry_details_field"),
                                        minLines = 2,
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Field 6: Remarks (Optional)
                                    OutlinedTextField(
                                        value = remarksInput,
                                        onValueChange = { remarksInput = it },
                                        label = { Text(Strings.get("remarks", language)) },
                                        placeholder = { Text("Optional remarks or progress notes") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("entry_remarks_field"),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    Spacer(modifier = Modifier.height(20.dp))

                                    // Submit Button
                                    Button(
                                        onClick = {
                                            if (selectedLabourForEntry == null) {
                                                Toast.makeText(context, Strings.get("select_labour", language), Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }
                                            if (workPlaceInput.isBlank() || workDateInput.isBlank() || shiftDetailsInput.isBlank()) {
                                                Toast.makeText(context, Strings.get("fill_all_fields", language), Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }

                                            viewModel.submitNightShiftEntry(
                                                labourId = selectedLabourForEntry!!.id,
                                                labourName = selectedLabourForEntry!!.name,
                                                workPlace = workPlaceInput,
                                                workDate = workDateInput,
                                                shiftDetails = shiftDetailsInput,
                                                remarks = remarksInput,
                                                onSuccess = {
                                                    Toast.makeText(context, Strings.get("success_saved", language), Toast.LENGTH_SHORT).show()
                                                    // Reset form inputs except date
                                                    workPlaceInput = ""
                                                    shiftDetailsInput = ""
                                                    remarksInput = ""
                                                    currentTab = 1 // Switch to view records
                                                },
                                                onError = { err ->
                                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                                }
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .testTag("submit_shift_entry_button"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text(
                                            text = Strings.get("submit_entry", language),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 1: MISTRI SHIFT RECORDS
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // Stats Summary Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatCard(
                                title = Strings.get("total_entries", language),
                                value = allMistriEntries.size.toString(),
                                icon = Icons.Default.Assignment,
                                accentColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = Strings.get("total_labours", language),
                                value = mistriLabours.size.toString(),
                                icon = Icons.Default.Group,
                                accentColor = Color(0xFF00897B),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Live Search, Date-wise & Month Filter
                        FilterBar(
                            searchQuery = searchQuery,
                            onSearchQueryChange = { viewModel.searchQuery.value = it },
                            selectedDate = selectedDateFilter,
                            onDateSelected = { viewModel.selectedDateFilter.value = it },
                            selectedMonth = selectedMonth,
                            onMonthSelected = { viewModel.selectedMonth.value = it },
                            availableMonths = availableMonths,
                            selectedMistriId = null,
                            onMistriSelected = {},
                            mistris = emptyList(),
                            language = language,
                            showMistriFilter = false
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (mistriEntries.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Assignment,
                                        contentDescription = null,
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(54.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = Strings.get("no_entries_found", language),
                                        color = Color(0xFF64748B),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 80.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(mistriEntries, key = { it.id }) { entry ->
                                    ShiftEntryCard(
                                        entry = entry,
                                        language = language,
                                        canEdit = true,
                                        onEdit = { editingEntry = entry },
                                        onDelete = { itemToDelete = entry }
                                    )
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 2: LABOUR MANAGEMENT FOR CURRENT MISTRI
                    Scaffold(
                        floatingActionButton = {
                            FloatingActionButton(
                                onClick = {
                                    editingLabour = null
                                    labourNameInput = ""
                                    labourMobileInput = ""
                                    showLabourDialog = true
                                },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White,
                                modifier = Modifier.testTag("fab_add_labour")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Labour")
                            }
                        }
                    ) { pad ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(pad)
                                .padding(horizontal = 16.dp)
                        ) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = Strings.get("labour_management", language),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                )
                                Text(
                                    text = "${mistriLabours.size} ${Strings.get("total_labours", language)}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (mistriLabours.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.Group,
                                            contentDescription = null,
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(54.dp)
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = Strings.get("no_labours_found", language),
                                            color = Color(0xFF64748B),
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(
                                            onClick = {
                                                editingLabour = null
                                                labourNameInput = ""
                                                labourMobileInput = ""
                                                showLabourDialog = true
                                            }
                                        ) {
                                            Text(Strings.get("add_labour", language))
                                        }
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = 80.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(mistriLabours, key = { it.id }) { labour ->
                                        LabourCard(
                                            labour = labour,
                                            language = language,
                                            onEdit = {
                                                editingLabour = labour
                                                labourNameInput = labour.name
                                                labourMobileInput = labour.mobileNumber
                                                showLabourDialog = true
                                            },
                                            onDelete = { labourToDelete = labour }
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

    // Labour Add / Edit Dialog
    if (showLabourDialog) {
        AlertDialog(
            onDismissRequest = { showLabourDialog = false },
            title = {
                Text(
                    text = if (editingLabour == null) Strings.get("add_labour", language)
                           else Strings.get("edit_labour", language),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = labourNameInput,
                        onValueChange = { labourNameInput = it },
                        label = { Text(Strings.get("labour_name", language)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_labour_name_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = labourMobileInput,
                        onValueChange = { labourMobileInput = it },
                        label = { Text(Strings.get("optional_mobile", language)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_labour_phone_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (labourNameInput.isBlank()) {
                            Toast.makeText(context, Strings.get("fill_all_fields", language), Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.saveLabour(
                            name = labourNameInput,
                            mobile = labourMobileInput,
                            labourId = editingLabour?.id ?: 0L,
                            onSuccess = {
                                showLabourDialog = false
                            }
                        )
                    },
                    modifier = Modifier.testTag("dialog_save_labour_button")
                ) {
                    Text(Strings.get("save", language))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLabourDialog = false }) {
                    Text(Strings.get("cancel", language))
                }
            }
        )
    }

    // Edit Shift Entry Dialog
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
                        text = "${Strings.get("labour_name", language)}: ${entry.labourName}",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
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

    // Delete Labour Confirmation
    labourToDelete?.let { labour ->
        AlertDialog(
            onDismissRequest = { labourToDelete = null },
            title = { Text(Strings.get("delete", language), fontWeight = FontWeight.Bold) },
            text = { Text(Strings.get("confirm_delete", language)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteLabour(labour)
                        labourToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(Strings.get("delete", language))
                }
            },
            dismissButton = {
                TextButton(onClick = { labourToDelete = null }) {
                    Text(Strings.get("cancel", language))
                }
            }
        )
    }
}

@Composable
fun ShiftEntryCard(
    entry: NightShiftEntryEntity,
    language: AppLanguage,
    canEdit: Boolean,
    showMistriName: Boolean = false,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Date Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = entry.workDate,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                // Actions
                if (canEdit) {
                    Row {
                        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF0288D1), modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Labour Name & Workplace
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Group, contentDescription = null, tint = Color(0xFF00897B), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${Strings.get("labour_name", language)}: ",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B))
                )
                Text(
                    text = entry.labourName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                )
            }

            if (showMistriName) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${Strings.get("mistri_name", language)}: ",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B))
                    )
                    Text(
                        text = entry.mistriName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFF57F17), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = entry.workPlace,
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF1E293B), fontWeight = FontWeight.Medium)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF8FAFC),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = entry.shiftDetails,
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF334155))
                    )
                    if (entry.remarks.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${Strings.get("remarks", language)}: ${entry.remarks}",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LabourCard(
    labour: LabourEntity,
    language: AppLanguage,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE0F2FE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF0288D1),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = labour.name,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    )
                    if (labour.mobileNumber.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = labour.mobileNumber,
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B))
                            )
                        }
                    }
                }
            }

            Row {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF0288D1), modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
