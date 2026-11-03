package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.UserRole
import com.example.ui.screens.admin.AdminDashboardScreen
import com.example.ui.screens.auth.AuthScreen
import com.example.ui.screens.mistri.MistriHomeScreen
import com.example.ui.theme.NightShiftTheme
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NightShiftTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NightShiftApp()
                }
            }
        }
    }
}

@Composable
fun NightShiftApp(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val userRole by viewModel.userRole.collectAsState()
    val language by viewModel.language.collectAsState()

    // Handle toast events
    LaunchedEffect(Unit) {
        viewModel.toastEvent.collectLatest { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // Back handling for logged in users
    if (userRole != UserRole.GUEST) {
        BackHandler {
            viewModel.logout()
        }
    }

    when (userRole) {
        UserRole.GUEST -> {
            AuthScreen(
                viewModel = viewModel,
                language = language
            )
        }
        UserRole.ADMIN -> {
            AdminDashboardScreen(
                viewModel = viewModel,
                language = language
            )
        }
        UserRole.MISTRI -> {
            MistriHomeScreen(
                viewModel = viewModel,
                language = language
            )
        }
    }
}
