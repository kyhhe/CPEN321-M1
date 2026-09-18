package com.example.cpen321application.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cpen321application.viewmodel.SurpriseViewModel
import com.example.cpen321application.viewmodel.SurpriseViewModelFactory
import com.example.cpen321application.viewmodel.TimerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(onBack: () -> Unit, timerViewModel: TimerViewModel = viewModel()) {
    val context = LocalContext.current
    val surpriseViewModel: SurpriseViewModel = viewModel(factory = SurpriseViewModelFactory(context))

    val timerUiState by timerViewModel.uiState.collectAsState()
    val surpriseUiState by surpriseViewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when {
                timerUiState.showingCollection -> SurpriseCollectionScreen(
                    collection = surpriseUiState.collection,
                    onBack = { timerViewModel.showCollection(false) },
                    onClearAll = {
                        surpriseViewModel.clearCollection()
                        timerViewModel.showCollection(false)
                    }
                )

                timerUiState.isFinished -> SurpriseRevealScreen(
                    isPokeballOpened = timerUiState.isPokeballOpened,
                    surpriseViewModel = surpriseViewModel,
                    onOpenPokeball = { timerViewModel.openPokeball() },
                    onReset = { timerViewModel.reset() }
                )

                timerUiState.isRunning -> {
                    val minutes = timerUiState.remainingMillis / 1000 / 60
                    val seconds = (timerUiState.remainingMillis / 1000) % 60
                    Text(
                        text = "%02d:%02d".format(minutes, seconds),
                        fontSize = 64.sp,
                        style = MaterialTheme.typography.displayLarge
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { timerViewModel.reset() }) { Text("Stop / Reset") }
                }

                else -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextField(
                            value = timerUiState.minutesInput,
                            onValueChange = { timerViewModel.onMinutesChange(it) },
                            label = { Text("Min") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(100.dp)
                        )
                        Text(" : ", fontSize = 24.sp)
                        TextField(
                            value = timerUiState.secondsInput,
                            onValueChange = { timerViewModel.onSecondsChange(it) },
                            label = { Text("Sec") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(100.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { timerViewModel.start() }) { Text("Start Timer") }

                    if (surpriseUiState.collection.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(48.dp))
                        PokeballView(size = 64.dp, onClick = { timerViewModel.showCollection(true) })
                        Text("View Collection", fontSize = 12.sp)
                    }
                }
            }

            timerUiState.error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
