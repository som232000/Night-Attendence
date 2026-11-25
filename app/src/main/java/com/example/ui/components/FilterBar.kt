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
                    color = Color(0xFF475569)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color(0xFF1565C0)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = Color(0xFF475569)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color(0xFF0F172A),
                unfocusedTextColor = Color(0xFF0F172A),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = Color(0xFF1565C0),
                unfocusedBorderColor = Color(0xFF94A3B8),
                cursorColor = Color(0xFF1565C0)
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        val chipColors = FilterChipDefaults.filterChipColors(
            containerColor = Color.White,
            labelColor = Color(0xFF0F172A),
            iconColor = Color(0xFF1565C0),
            selectedContainerColor = Color(0xFFDBEAFE),
            selectedLabelColor = Color(0xFF0D47A1),
            selectedLeadingIconColor = Color(0xFF0D47A1)
        )

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
                            fontWeight = if (selectedDate != null) FontWeight.Bold else FontWeight.Medium
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
                                Icon(Icons.Default.Clear, contentDescription = "Clear date", tint = Color(0xFF475569))
                            }
                        }
                    },
                    colors = chipColors
                )

                DropdownMenu(
                    expanded = dateMenuExpanded,
                    onDismissRequest = { dateMenuExpanded = false },
                    containerColor = Color.White
                ) {
                    DropdownMenuItem(
                        text = { Text(Strings.get("all_dates", language), color = Color(0xFF0F172A)) },
                        onClick = {
                            onDateSelected(null)
                            dateMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("${Strings.get("today", language)} ($todayDate)", color = Color(0xFF0F172A)) },
                        onClick = {
                            onDateSelected(todayDate)
                            dateMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("${Strings.get("yesterday", language)} ($yesterdayDate)", color = Color(0xFF0F172A)) },
                        onClick = {
                            onDateSelected(yesterdayDate)
                            dateMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.EditCalendar, contentDescription = null, tint = Color(0xFF1565C0), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(Strings.get("pick_date", language), color = Color(0xFF0F172A))
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
                            fontSize = 12.sp,
                            fontWeight = if (selectedMonth != null) FontWeight.Bold else FontWeight.Medium
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
                                Icon(Icons.Default.Clear, contentDescription = "Clear month", tint = Color(0xFF475569))
                            }
                        }
                    },
                    colors = chipColors
                )
                DropdownMenu(
                    expanded = monthMenuExpanded,
                    onDismissRequest = { monthMenuExpanded = false },
                    containerColor = Color.White
                ) {
                    DropdownMenuItem(
                        text = { Text(Strings.get("all_months", language), color = Color(0xFF0F172A)) },
                        onClick = {
                            onMonthSelected(null)
                            monthMenuExpanded = false
                        }
                    )
                    availableMonths.forEach { month ->
                        DropdownMenuItem(
                            text = { Text(month, color = Color(0xFF0F172A)) },
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
                                fontSize = 12.sp,
                                fontWeight = if (selectedMistriId != null) FontWeight.Bold else FontWeight.Medium
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
                                    Icon(Icons.Default.Clear, contentDescription = "Clear mistri", tint = Color(0xFF475569))
                                }
                            }
                        },
                        colors = chipColors
                    )
                    DropdownMenu(
                        expanded = mistriMenuExpanded,
                        onDismissRequest = { mistriMenuExpanded = false },
                        containerColor = Color.White
                    ) {
                        DropdownMenuItem(
                            text = { Text(Strings.get("all_mistris", language), color = Color(0xFF0F172A)) },
                            onClick = {
                                onMistriSelected(null)
                                mistriMenuExpanded = false
                            }
                        )
                        mistris.forEach { mistri ->
                            DropdownMenuItem(
                                text = { Text(mistri.fullName, color = Color(0xFF0F172A)) },
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
            containerColor = Color.White,
            titleContentColor = Color(0xFF0F172A),
            textContentColor = Color(0xFF1E293B),
            title = {
                Text(
                    text = Strings.get("pick_date", language),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter or edit date (YYYY-MM-DD):",
                        fontSize = 13.sp,
                        color = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customDateInput,
                        onValueChange = { customDateInput = it },
                        label = { Text("YYYY-MM-DD") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color(0xFF1565C0),
                            unfocusedBorderColor = Color(0xFF94A3B8),
                            focusedLabelColor = Color(0xFF1565C0),
                            unfocusedLabelColor = Color(0xFF334155)
                        )
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
                    Text(Strings.get("save", language), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDateDialog = false }) {
                    Text(Strings.get("cancel", language), color = Color(0xFF334155))
                }
            }
        )
    }
}
