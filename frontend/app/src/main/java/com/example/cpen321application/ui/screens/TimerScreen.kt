package com.example.cpen321application.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cpen321application.viewmodel.TimerViewModel

@Composable
fun TimerScreen(viewModel: TimerViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (!uiState.isRunning && !uiState.isFinished && uiState.surprise == null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = uiState.minutesInput,
                    onValueChange = { viewModel.onMinutesChange(it) },
                    label = { Text("Min") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(100.dp)
                )
                Text(" : ", fontSize = 24.sp)
                TextField(
                    value = uiState.secondsInput,
                    onValueChange = { viewModel.onSecondsChange(it) },
                    label = { Text("Sec") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(100.dp)
                )
            }
        } else if (uiState.isRunning || (uiState.remainingMillis > 0 && !uiState.isFinished)) {
            val minutes = uiState.remainingMillis / 1000 / 60
            val seconds = (uiState.remainingMillis / 1000) % 60
            Text(
                text = "%02d:%02d".format(minutes, seconds),
                fontSize = 64.sp,
                style = MaterialTheme.typography.displayLarge
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row {
            if (!uiState.isRunning && !uiState.isFinished && uiState.surprise == null) {
                Button(onClick = { viewModel.start() }) {
                    Text("Start Timer")
                }
            }
            
            if (uiState.isRunning) {
                Button(onClick = { viewModel.reset() }) {
                    Text("Reset")
                }
            }
        }

        uiState.error?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (uiState.isFinished) {
            Text(text = "Time's up!", style = MaterialTheme.typography.headlineSmall)
            
            if (uiState.isLoadingSurprise) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
            } else if (uiState.surprise != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Your Surprise:", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = uiState.surprise!!.text, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { viewModel.reset() }) {
                Text("Reset")
            }
        }
    }
}
