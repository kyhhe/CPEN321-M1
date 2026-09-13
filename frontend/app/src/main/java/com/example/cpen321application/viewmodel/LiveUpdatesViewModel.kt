package com.example.cpen321application.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cpen321application.BuildConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import androidx.core.graphics.toColorInt

const val GRID_SIZE = 16
private const val IDLE_RESET_MS = 5000L // 5 second pause between images

data class LiveUpdatesUiState(
    val grid: List<Color> = List(GRID_SIZE * GRID_SIZE) { Color.White },
    val isConnected: Boolean = false,
    val connectionError: String? = null
)

class LiveUpdatesViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LiveUpdatesUiState())
    val uiState: StateFlow<LiveUpdatesUiState> = _uiState

    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null
    private var idleResetJob: Job? = null

    fun connect() {
        if (webSocket != null) return

        val wsUrl = BuildConfig.API_BASE_URL
            .replace("https://", "wss://")
            .replace("http://", "ws://")
            .trimEnd('/') + "/live-updates" // live update path from backend

        val request = Request.Builder().url(wsUrl).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _uiState.value = _uiState.value.copy(isConnected = true, connectionError = null)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handlePixelUpdate(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _uiState.value = _uiState.value.copy(
                    isConnected = false,
                    connectionError = t.message ?: "Connection failed"
                )
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _uiState.value = _uiState.value.copy(isConnected = false)
            }
        })
    }

    private fun handlePixelUpdate(text: String) {
        try {
            val json = JSONObject(text)
            val x = json.getInt("x")
            val y = json.getInt("y")
            val colorHex = json.getString("color")

            if (x !in 0 until GRID_SIZE || y !in 0 until GRID_SIZE) return

            val color = Color(colorHex.toColorInt())
            val index = y * GRID_SIZE + x

            val newGrid = _uiState.value.grid.toMutableList()
            newGrid[index] = color
            _uiState.value = _uiState.value.copy(grid = newGrid)

            scheduleIdleReset()
        } catch (e: Exception) {
            // Malformed payload — skip this update, keep the stream alive
        }
    }

    private fun scheduleIdleReset() {
        idleResetJob?.cancel()
        idleResetJob = viewModelScope.launch {
            delay(IDLE_RESET_MS)
            _uiState.value = _uiState.value.copy(grid = List(GRID_SIZE * GRID_SIZE) { Color.White })
        }
    }

    fun disconnect() {
        idleResetJob?.cancel()
        webSocket?.close(1000, "Screen closed")
        webSocket = null
        _uiState.value = _uiState.value.copy(isConnected = false)
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}