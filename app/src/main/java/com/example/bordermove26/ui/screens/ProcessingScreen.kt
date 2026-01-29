package com.example.bordermove26.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bordermove26.data.models.VideoProcessingState

@Composable
fun ProcessingScreen(
    state: VideoProcessingState,
    onProcessingComplete: () -> Unit,
    onStartProcessing: () -> Unit
) {
    LaunchedEffect(Unit) {
        onStartProcessing()
    }

    LaunchedEffect(state.isProcessing) {
        if (!state.isProcessing && state.landmarksByFrame.isNotEmpty()) {
            onProcessingComplete()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Processing Video",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        CircularProgressIndicator(
            progress = { state.processingProgress },
            modifier = Modifier.size(80.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "${(state.processingProgress * 100).toInt()}%",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Detecting pose landmarks in video frames...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (state.errorMessage != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Error: ${state.errorMessage}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
