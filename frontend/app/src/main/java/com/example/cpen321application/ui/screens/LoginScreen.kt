package com.example.cpen321application.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cpen321application.viewmodel.LoginViewModel

@Composable
fun LoginScreen(viewModel: LoginViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when {
            !uiState.isSignedIn && !uiState.isLoadingAuth -> {
                Button(onClick = { viewModel.signInWithGoogle(context) }) {
                    Text("Sign in with Google")
                }
                if (uiState.authError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Auth error: ${uiState.authError}")
                }
            }

            uiState.isLoadingAuth -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Signing in...")
            }

            uiState.isLoadingBackend -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Loading server info...")
            }

            uiState.backendError != null -> {
                Column(horizontalAlignment = Alignment.Start) {
                    Text("Signed in as: ${uiState.googleFirstName} ${uiState.googleLastName}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Error: ${uiState.backendError}")
                }
            }

            else -> {
                Column(horizontalAlignment = Alignment.Start) {
                    Text("Server IP Address: ${uiState.serverIp}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Client IP Address: ${uiState.clientIp}")
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Server Local Time: ${uiState.serverTime}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Client Local Time: ${uiState.clientTime}")
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("App by: ${uiState.myFirstName} ${uiState.myLastName}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Signed in as: ${uiState.googleFirstName} ${uiState.googleLastName}")
                }
            }
        }
    }
}