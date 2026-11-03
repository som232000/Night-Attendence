package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.MistriEntity
import com.example.util.AppLanguage
import com.example.util.Strings
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedDate: String?,
    onDateSelected: (String?) -> Unit,
    selectedMonth: String?,
    onMonthSelected: (String?) -> Unit,
    availableMonths: List<String>,
    selectedMistriId: Long?,
    onMistriSelected: (Long?) -> Unit,
    mistris: List<MistriEntity>,
    language: AppLanguage,
    showMistriFilter: Boolean = true
) {
    var monthMenuExpanded by remember { mutableStateOf(false) }
    var dateMenuExpanded by remember { mutableStateOf(false) }
    var mistriMenuExpanded by remember { mutableStateOf(false) }
    var showCustomDateDialog by remember { mutableStateOf(false) }
    var customDateInput by remember { mutableStateOf("") }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val todayDate = remember { dateFormat.format(Date()) }
    val yesterdayDate = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        dateFormat.format(cal.time)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Main live search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_input_field"),
            placeholder = {
                Text(
                    text = Strings.get("search_hint", language),
                    fontSize = 13.sp,
                    color = Color(0xFF64748B)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = Color.Gray
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color(0xFFCBD5E1)
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Horizontal filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. DATE-WISE FILTER CHIP (Dropdown with All, Today, Yesterday, Custom Date)
            Row {
                FilterChip(
                    selected = selectedDate != null,
                    onClick = { dateMenuExpanded = true },
                    label = {
                        Text(
                            text = when (selectedDate) {
                                null -> Strings.get("filter_by_date", language)
                                todayDate -> "${Strings.get("today", language)} ($todayDate)"
                                yesterdayDate -> "${Strings.get("yesterday", language)} ($yesterdayDate)"
                                else -> selectedDate
                            },
                            fontSize = 12.sp,
                            fontWeight = if (selectedDate != null) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Date",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    trailingIcon = {
                        if (selectedDate != null) {
                            IconButton(
                                onClick = { onDateSelected(null) },
                                modifier = Modifier.size(16.dp)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear date", tint = Color.Gray)
                            }
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFE0F2FE),
                        selectedLabelColor = Color(0xFF0369A1)
                    )
                )

                DropdownMenu(
                    expanded = dateMenuExpanded,
                    onDismissRequest = { dateMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(Strings.get("all_dates", language)) },
                        onClick = {
                            onDateSelected(null)
                            dateMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("${Strings.get("today", language)} ($todayDate)") },
                        onClick = {
                            onDateSelected(todayDate)
                            dateMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("${Strings.get("yesterday", language)} ($yesterdayDate)") },
                        onClick = {
                            onDateSelected(yesterdayDate)
                            dateMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(Strings.get("pick_date", language))
                            }
                        },
                        onClick = {
                            dateMenuExpanded = false
                            customDateInput = selectedDate ?: todayDate
                            showCustomDateDialog = true
                        }
                    )
                }
            }

            // 2. MONTH FILTER CHIP
            Row {
                FilterChip(
                    selected = selectedMonth != null,
                    onClick = { monthMenuExpanded = true },
                    label = {
                        Text(
                            text = if (selectedMonth == null) Strings.get("all_months", language)
                                   else selectedMonth,
                            fontSize = 12.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Month",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    trailingIcon = {
                        if (selectedMonth != null) {
                            IconButton(
                                onClick = { onMonthSelected(null) },
                                modifier = Modifier.size(16.dp)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear month", tint = Color.Gray)
                            }
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
                DropdownMenu(
                    expanded = monthMenuExpanded,
                    onDismissRequest = { monthMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(Strings.get("all_months", language)) },
                        onClick = {
                            onMonthSelected(null)
                            monthMenuExpanded = false
                        }
                    )
                    availableMonths.forEach { month ->
                        DropdownMenuItem(
                            text = { Text(month) },
                            onClick = {
                                onMonthSelected(month)
                                monthMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // 3. MISTRI FILTER (FOR ADMIN)
            if (showMistriFilter && mistris.isNotEmpty()) {
                Row {
                    val currentMistriName = mistris.find { it.id == selectedMistriId }?.fullName
                    FilterChip(
                        selected = selectedMistriId != null,
                        onClick = { mistriMenuExpanded = true },
                        label = {
                            Text(
                                text = currentMistriName ?: Strings.get("all_mistris", language),
                                fontSize = 12.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Mistri",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        trailingIcon = {
                            if (selectedMistriId != null) {
                                IconButton(
                                    onClick = { onMistriSelected(null) },
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear mistri", tint = Color.Gray)
                                }
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.secondary
                        )
                    )
                    DropdownMenu(
                        expanded = mistriMenuExpanded,
                        onDismissRequest = { mistriMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(Strings.get("all_mistris", language)) },
                            onClick = {
                                onMistriSelected(null)
                                mistriMenuExpanded = false
                            }
                        )
                        mistris.forEach { mistri ->
                            DropdownMenuItem(
                                text = { Text(mistri.fullName) },
                                onClick = {
                                    onMistriSelected(mistri.id)
                                    mistriMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Custom Date Selection Dialog
    if (showCustomDateDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDateDialog = false },
            title = {
                Text(
                    text = Strings.get("pick_date", language),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter or edit date (YYYY-MM-DD):",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customDateInput,
                        onValueChange = { customDateInput = it },
                        label = { Text("YYYY-MM-DD") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customDateInput.isNotBlank()) {
                            onDateSelected(customDateInput.trim())
                        }
                        showCustomDateDialog = false
                    }
                ) {
                    Text(Strings.get("save", language))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDateDialog = false }) {
                    Text(Strings.get("cancel", language))
                }
            }
        )
    }
}
