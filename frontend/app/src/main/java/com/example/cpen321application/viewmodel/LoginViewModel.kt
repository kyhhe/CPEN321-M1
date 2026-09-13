package com.example.cpen321application.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cpen321application.auth.GoogleAuthManager
import com.example.cpen321application.network.ApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LoginUiState(
    val isLoadingBackend: Boolean = false,
    val isLoadingAuth: Boolean = false,
    val isSignedIn: Boolean = false,
    val serverIp: String? = null,
    val serverTime: String? = null,
    val clientIp: String? = null,
    val clientTime: String? = null,
    val myFirstName: String? = null,
    val myLastName: String? = null,
    val googleFirstName: String? = null,
    val googleLastName: String? = null,
    val backendError: String? = null,
    val authError: String? = null
)

class LoginViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingAuth = true, authError = null)

            val authManager = GoogleAuthManager(context)
            val userInfo = authManager.signIn()

            if (userInfo == null) {
                _uiState.value = _uiState.value.copy(
                    isLoadingAuth = false,
                    authError = "Google sign-in failed"
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                isLoadingAuth = false,
                isSignedIn = true,
                googleFirstName = userInfo.firstName,
                googleLastName = userInfo.lastName
            )

            // Only pull server/user info once auth has succeeded
            fetchBackendInfo()
            fetchClientInfo()
        }
    }

    // Pulls info from server
    private fun fetchBackendInfo() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingBackend = true, backendError = null)

            val ipResult = ApiService.getServerIp()
            val timeResult = ApiService.getServerTime()
            val nameResult = ApiService.getMyName()

            if (ipResult == null || timeResult == null || nameResult == null) {
                _uiState.value = _uiState.value.copy(
                    isLoadingBackend = false,
                    backendError = "Failed to reach backend"
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                isLoadingBackend = false,
                serverIp = ipResult.ip,
                serverTime = timeResult.time,
                myFirstName = nameResult.firstName,
                myLastName = nameResult.lastName
            )
        }
    }

    // Sets client info
    private fun fetchClientInfo() {
        _uiState.value = _uiState.value.copy(
            isLoadingBackend = false,
            backendError = null,
            clientIp = getClientIpAddress(),
            clientTime = getClientLocalTime()
        )
    }
    private fun getClientIpAddress(): String {
        return try {
            NetworkInterface.getNetworkInterfaces().toList()
                .flatMap { it.inetAddresses.toList() }
                .firstOrNull { !it.isLoopbackAddress && it is Inet4Address }
                ?.hostAddress ?: "Unavailable"
        } catch (e: Exception) {
            "Unavailable"
        }
    }

    private fun getClientLocalTime(): String {
        val sdf = SimpleDateFormat("HH:mm:ss 'GMT'XXX", Locale.getDefault())
        return sdf.format(Date())
    }
}