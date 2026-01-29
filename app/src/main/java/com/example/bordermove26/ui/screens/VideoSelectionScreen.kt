package com.example.bordermove26.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun VideoSelectionScreen(
    onVideoSelected: (android.net.Uri) -> Unit
) {
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    val permissionsState = rememberMultiplePermissionsState(permissions)

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { onVideoSelected(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Video Pose Detection & Cropping",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        if (permissionsState.allPermissionsGranted) {
            Button(
                onClick = { videoPickerLauncher.launch("video/*") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Select Video from Gallery")
            }

            Text(
                text = "Select a video (30 seconds to 2 minutes)",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 16.dp)
            )
        } else {
            Text(
                text = "This app needs storage permission to access videos",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Button(
                onClick = { permissionsState.launchMultiplePermissionRequest() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Grant Permission")
            }
        }
    }
}
