package com.example.cpen321application.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cpen321application.viewmodel.GRID_SIZE
import com.example.cpen321application.viewmodel.LiveUpdatesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveUpdateScreen(onBack: () -> Unit, viewModel: LiveUpdatesViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    DisposableEffect(Unit) {
        viewModel.connect()
        onDispose { viewModel.disconnect() }
    }

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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = when {
                    uiState.connectionError != null -> "Connection error: ${uiState.connectionError}"
                    uiState.isConnected -> "Connected: Assembling live pixel art"
                    else -> "Connecting..."
                },
                color = if (uiState.connectionError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (!uiState.isConnected && uiState.connectionError == null) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
            }

            Canvas(modifier = Modifier.size(320.dp)) {
                val cellSize = size.width / GRID_SIZE
                uiState.grid.forEachIndexed { index, color ->
                    val x = index % GRID_SIZE
                    val y = index / GRID_SIZE
                    drawRect(
                        color = color,
                        topLeft = Offset(x * cellSize, y * cellSize),
                        size = Size(cellSize, cellSize)
                    )
                }
            }
        }
    }
}
