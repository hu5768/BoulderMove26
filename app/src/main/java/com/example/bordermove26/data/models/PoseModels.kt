package com.example.bordermove26.data.models

import android.graphics.PointF
import android.net.Uri

data class PoseLandmark(
    val x: Float,
    val y: Float,
    val visibility: Float
)

enum class LandmarkType(val mediaPipeIndex: Int) {
    LEFT_SHOULDER(11),
    RIGHT_SHOULDER(12),
    LEFT_ELBOW(13),
    RIGHT_ELBOW(14),
    LEFT_HIP(23),
    RIGHT_HIP(24),
    LEFT_KNEE(25),
    RIGHT_KNEE(26)
}

data class PoseDetectionResult(
    val frameIndex: Int,
    val timestamp: Long,
    val landmarks: Map<LandmarkType, PoseLandmark>
)

data class VideoMetadata(
    val width: Int,
    val height: Int,
    val duration: Long,
    val fps: Float
)

data class VideoProcessingState(
    val selectedVideoUri: Uri? = null,
    val videoMetadata: VideoMetadata? = null,
    val landmarksByFrame: Map<Int, PoseDetectionResult> = emptyMap(),
    val isProcessing: Boolean = false,
    val processingProgress: Float = 0f,
    val currentEditFrameIndex: Int = 0,
    val croppedVideoUri: Uri? = null,
    val isCropping: Boolean = false,
    val croppingProgress: Float = 0f,
    val errorMessage: String? = null
)
