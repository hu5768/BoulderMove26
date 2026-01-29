package com.example.bordermove26.ui.screens

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.bordermove26.data.models.LandmarkType
import com.example.bordermove26.data.models.VideoProcessingState

@Composable
fun EditScreen(
    state: VideoProcessingState,
    onCropVideo: () -> Unit
) {
    val context = LocalContext.current
    val videoUri = state.selectedVideoUri ?: return

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            repeatMode = Player.REPEAT_MODE_ONE
            prepare()
        }
    }

    var currentPosition by remember { mutableLongStateOf(0L) }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    currentPosition = exoPlayer.currentPosition
                }
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            currentPosition = exoPlayer.currentPosition
            kotlinx.coroutines.delay(33)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Edit Video",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
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

            Canvas(modifier = Modifier.matchParentSize()) {
                val fps = state.videoMetadata?.fps ?: 30f
                val frameIndex = ((currentPosition / 1000f) * fps).toInt()

                state.landmarksByFrame[frameIndex]?.let { pose ->
                    pose.landmarks.forEach { (type, landmark) ->
                        val color = when (type) {
                            LandmarkType.LEFT_HIP, LandmarkType.RIGHT_HIP -> Color.Green
                            LandmarkType.LEFT_SHOULDER, LandmarkType.RIGHT_SHOULDER -> Color.Red
                            LandmarkType.LEFT_ELBOW, LandmarkType.RIGHT_ELBOW -> Color.Blue
                            LandmarkType.LEFT_KNEE, LandmarkType.RIGHT_KNEE -> Color.Magenta
                        }

                        drawCircle(
                            color = color,
                            radius = 10f,
                            center = Offset(
                                landmark.x * size.width,
                                landmark.y * size.height
                            )
                        )
                    }

                    val leftHip = pose.landmarks[LandmarkType.LEFT_HIP]
                    val rightHip = pose.landmarks[LandmarkType.RIGHT_HIP]

                    if (leftHip != null && rightHip != null) {
                        val hipMidX = (leftHip.x + rightHip.x) / 2f
                        val hipMidY = (leftHip.y + rightHip.y) / 2f

                        drawCircle(
                            color = Color.Yellow,
                            radius = 15f,
                            center = Offset(
                                hipMidX * size.width,
                                hipMidY * size.height
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Landmarks detected: ${state.landmarksByFrame.size} frames",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Red: Shoulders | Blue: Elbows | Green: Hips | Magenta: Knees | Yellow: Hip midpoint",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (state.isCropping) {
            LinearProgressIndicator(
                progress = { state.croppingProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            )
            Text(
                text = "Cropping video: ${(state.croppingProgress * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            Button(
                onClick = {
                    exoPlayer.pause()
                    onCropVideo()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = state.landmarksByFrame.isNotEmpty()
            ) {
                Text("Crop Video (720x720)")
            }
        }

        if (state.errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Error: ${state.errorMessage}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
