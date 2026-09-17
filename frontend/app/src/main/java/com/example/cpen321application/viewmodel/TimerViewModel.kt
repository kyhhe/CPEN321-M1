package com.example.cpen321application.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

data class TimerUiState(
    val minutesInput: String = "0",
    val secondsInput: String = "30",
    val remainingMillis: Long = 0L,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false,
    val isPokeballOpened: Boolean = false,
    val showingCollection: Boolean = false,
    val error: String? = null
)

class TimerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState

    private var tickJob: Job? = null

    fun onMinutesChange(value: String) {
        if (value.all { it.isDigit() } && value.length <= 3) {
            _uiState.value = _uiState.value.copy(minutesInput = value)
        }
    }

    fun onSecondsChange(value: String) {
        if (value.all { it.isDigit() } && value.length <= 2) {
            _uiState.value = _uiState.value.copy(secondsInput = value)
        }
    }

    fun start() {
        val mins = _uiState.value.minutesInput.toLongOrNull() ?: 0L
        val secs = _uiState.value.secondsInput.toLongOrNull() ?: 0L
        val totalMillis = (mins * 60 + secs) * 1000

        if (totalMillis <= 0) {
            _uiState.value = _uiState.value.copy(error = "Enter a time greater than zero")
            return
        }

        _uiState.value = _uiState.value.copy(
            remainingMillis = totalMillis,
            isRunning = true,
            isFinished = false,
            error = null
        )

        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (_uiState.value.remainingMillis > 0) {
                delay(1000)
                val next = (_uiState.value.remainingMillis - 1000).coerceAtLeast(0)
                _uiState.value = _uiState.value.copy(remainingMillis = next)
            }
            _uiState.value = _uiState.value.copy(isRunning = false, isFinished = true)
        }
    }

    fun reset() {
        tickJob?.cancel()
        _uiState.value = _uiState.value.copy(
            remainingMillis = 0L,
            isRunning = false,
            isFinished = false,
            isPokeballOpened = false,
            showingCollection = false,
            error = null
        )
    }

    fun openPokeball() {
        _uiState.value = _uiState.value.copy(isPokeballOpened = true)
    }

    fun showCollection(show: Boolean) {
        _uiState.value = _uiState.value.copy(showingCollection = show)
    }
}