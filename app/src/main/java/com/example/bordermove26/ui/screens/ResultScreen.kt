package com.example.bordermove26.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.launch

@Composable
fun ResultScreen(
    croppedVideoUri: Uri?,
    onSaveToGallery: suspend () -> Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var saveStatus by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val exoPlayer = remember(croppedVideoUri) {
        croppedVideoUri?.let {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(it))
                repeatMode = Player.REPEAT_MODE_ONE
                prepare()
                playWhenReady = true
            }
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer?.release()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Cropped Video",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (croppedVideoUri != null && exoPlayer != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = true
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Preview your 720x720 cropped video with dynamic hip tracking",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Button(
                onClick = {
                    isSaving = true
                    scope.launch {
                        val success = onSaveToGallery()
                        saveStatus = if (success) {
                            "Video saved to gallery successfully!"
                        } else {
                            "Failed to save video to gallery"
                        }
                        isSaving = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Save to Gallery")
                }
            }

            if (saveStatus != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = saveStatus!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (saveStatus!!.contains("successfully")) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Start Over")
            }

        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No cropped video available",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
