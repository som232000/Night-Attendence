package com.example.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.MainViewModel
import com.example.util.AppLanguage
import com.example.util.Strings

@Composable
fun AuthScreen(
    viewModel: MainViewModel,
    language: AppLanguage
) {
    val isCloudConnected by viewModel.isCloudConnected.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Mistri, 1: Admin
    var isMistriRegister by remember { mutableStateOf(false) }

    // Admin form fields
    var adminUser by remember { mutableStateOf("admin") }
    var adminPass by remember { mutableStateOf("admin123") }
    var adminPassVisible by remember { mutableStateOf(false) }

    // Mistri form fields
    var mistriName by remember { mutableStateOf("") }
    var mistriMobile by remember { mutableStateOf("") }
    var mistriPass by remember { mutableStateOf("") }
    var mistriConfirmPass by remember { mutableStateOf("") }
    var mistriPassVisible by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Logo
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NightsStay,
                        contentDescription = "Night Shift",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = Strings.get("app_title", language),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        fontSize = 20.sp
                    )
                )
                Text(
                    text = Strings.get("app_subtitle", language),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF475569)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    color = if (isCloudConnected) Color(0xFFE8F5E9) else Color(0xFFE3F2FD),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isCloudConnected) Color(0xFF2E7D32) else Color(0xFF1565C0))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = Strings.get("cloud_online", language),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isCloudConnected) Color(0xFF1B5E20) else Color(0xFF0D47A1)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Dual Role Tab
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFFF1F5F9),
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Color(0xFF0D47A1)
                        )
                    },
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                            errorMessage = null
                        },
                        selectedContentColor = Color(0xFF0D47A1),
                        unselectedContentColor = Color(0xFF334155),
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Engineering,
                                    contentDescription = null,
                                    tint = if (selectedTab == 0) Color(0xFF0D47A1) else Color(0xFF334155),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    Strings.get("mistri_panel", language),
                                    color = if (selectedTab == 0) Color(0xFF0D47A1) else Color(0xFF334155),
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        },
                        modifier = Modifier.testTag("tab_mistri_login")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            errorMessage = null
                        },
                        selectedContentColor = Color(0xFF0D47A1),
                        unselectedContentColor = Color(0xFF334155),
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = if (selectedTab == 1) Color(0xFF0D47A1) else Color(0xFF334155),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    Strings.get("admin_panel", language),
                                    color = if (selectedTab == 1) Color(0xFF0D47A1) else Color(0xFF334155),
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        },
                        modifier = Modifier.testTag("tab_admin_login")
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Error Banner
                AnimatedVisibility(visible = errorMessage != null) {
                    errorMessage?.let { msg ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFFEBEE))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = msg,
                                color = Color(0xFFC62828),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                val highContrastFieldColors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFF0F172A),
                    unfocusedTextColor = Color(0xFF0F172A),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Color(0xFF1565C0),
                    unfocusedBorderColor = Color(0xFF94A3B8),
                    focusedLabelColor = Color(0xFF1565C0),
                    unfocusedLabelColor = Color(0xFF334155),
                    cursorColor = Color(0xFF1565C0)
                )

                // ADMIN PANEL FORM
                if (selectedTab == 1) {
                    Text(
                        text = Strings.get("admin_login", language),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        ),
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = adminUser,
                        onValueChange = { adminUser = it },
                        label = { Text(Strings.get("user_id", language)) },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF1565C0))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_user_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = highContrastFieldColors
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = adminPass,
                        onValueChange = { adminPass = it },
                        label = { Text(Strings.get("password", language)) },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF1565C0))
                        },
                        trailingIcon = {
                            IconButton(onClick = { adminPassVisible = !adminPassVisible }) {
                                Icon(
                                    imageVector = if (adminPassVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle password",
                                    tint = Color(0xFF475569)
                                )
                            }
                        },
                        visualTransformation = if (adminPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_password_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = highContrastFieldColors
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE3F2FD))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = Strings.get("admin_only_note", language),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF0D47A1),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            errorMessage = null
                            if (adminUser.isBlank() || adminPass.isBlank()) {
                                errorMessage = Strings.get("fill_all_fields", language)
                                return@Button
                            }
                            isLoading = true
                            viewModel.loginAdmin(
                                user = adminUser,
                                pass = adminPass,
                                onSuccess = { isLoading = false },
                                onError = { msg ->
                                    isLoading = false
                                    errorMessage = Strings.get("admin_invalid", language)
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("admin_login_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1565C0),
                            contentColor = Color.White
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = Strings.get("login", language),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                } else {
                    // MISTRI PANEL FORM (Login or Register)
                    if (!isMistriRegister) {
                        // Mistri Login
                        Text(
                            text = Strings.get("mistri_login", language),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            ),
                            modifier = Modifier.align(Alignment.Start)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = mistriMobile,
                            onValueChange = { if (it.length <= 15) mistriMobile = it },
                            label = { Text(Strings.get("mobile_number", language)) },
                            leadingIcon = {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF1565C0))
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("mistri_mobile_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = highContrastFieldColors
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = mistriPass,
                            onValueChange = { mistriPass = it },
                            label = { Text(Strings.get("password", language)) },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF1565C0))
                            },
                            trailingIcon = {
                                IconButton(onClick = { mistriPassVisible = !mistriPassVisible }) {
                                    Icon(
                                        imageVector = if (mistriPassVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle password",
                                        tint = Color(0xFF475569)
                                    )
                                }
                            },
                            visualTransformation = if (mistriPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("mistri_password_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = highContrastFieldColors
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                errorMessage = null
                                if (mistriMobile.isBlank() || mistriPass.isBlank()) {
                                    errorMessage = Strings.get("fill_all_fields", language)
                                    return@Button
                                }
                                isLoading = true
                                viewModel.loginMistri(
                                    mobile = mistriMobile,
                                    pass = mistriPass,
                                    onSuccess = { isLoading = false },
                                    onError = { msg ->
                                        isLoading = false
                                        errorMessage = if (msg.contains("deactivated", ignoreCase = true)) {
                                            Strings.get("account_deactivated", language)
                                        } else {
                                            Strings.get("invalid_credentials", language)
                                        }
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("mistri_login_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1565C0),
                                contentColor = Color.White
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = Strings.get("login", language),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        TextButton(
                            onClick = {
                                isMistriRegister = true
                                errorMessage = null
                            },
                            modifier = Modifier.testTag("switch_to_register_button")
                        ) {
                            Text(
                                text = Strings.get("dont_have_account", language),
                                color = Color(0xFF0D47A1),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                    } else {
                        // Mistri Registration
                        Text(
                            text = Strings.get("mistri_register", language),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            ),
                            modifier = Modifier.align(Alignment.Start)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = mistriName,
                            onValueChange = { mistriName = it },
                            label = { Text(Strings.get("full_name", language)) },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF1565C0))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_name_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = highContrastFieldColors
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = mistriMobile,
                            onValueChange = { if (it.length <= 15) mistriMobile = it },
                            label = { Text(Strings.get("mobile_number", language)) },
                            leadingIcon = {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF1565C0))
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_mobile_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = highContrastFieldColors
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = mistriPass,
                            onValueChange = { mistriPass = it },
                            label = { Text(Strings.get("password", language)) },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF1565C0))
                            },
                            trailingIcon = {
                                IconButton(onClick = { mistriPassVisible = !mistriPassVisible }) {
                                    Icon(
                                        imageVector = if (mistriPassVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle password",
                                        tint = Color(0xFF475569)
                                    )
                                }
                            },
                            visualTransformation = if (mistriPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_password_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = highContrastFieldColors
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = mistriConfirmPass,
                            onValueChange = { mistriConfirmPass = it },
                            label = { Text(Strings.get("confirm_password", language)) },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF1565C0))
                            },
                            visualTransformation = if (mistriPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_confirm_password_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = highContrastFieldColors
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                errorMessage = null
                                if (mistriName.isBlank() || mistriMobile.isBlank() || mistriPass.isBlank()) {
                                    errorMessage = Strings.get("fill_all_fields", language)
                                    return@Button
                                }
                                if (mistriPass != mistriConfirmPass) {
                                    errorMessage = Strings.get("passwords_dont_match", language)
                                    return@Button
                                }
                                isLoading = true
                                viewModel.registerMistri(
                                    name = mistriName,
                                    mobile = mistriMobile,
                                    pass = mistriPass,
                                    onSuccess = { isLoading = false },
                                    onError = { msg ->
                                        isLoading = false
                                        errorMessage = if (msg.contains("already registered", ignoreCase = true)) {
                                            Strings.get("mobile_registered", language)
                                        } else {
                                            msg
                                        }
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("register_submit_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1565C0),
                                contentColor = Color.White
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = Strings.get("register", language),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        TextButton(
                            onClick = {
                                isMistriRegister = false
                                errorMessage = null
                            },
                            modifier = Modifier.testTag("switch_to_login_button")
                        ) {
                            Text(
                                text = Strings.get("already_have_account", language),
                                color = Color(0xFF0D47A1),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
