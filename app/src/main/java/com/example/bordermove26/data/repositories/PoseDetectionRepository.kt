package com.example.bordermove26.data.repositories

import android.content.Context
import android.graphics.Bitmap
import com.example.bordermove26.data.models.LandmarkType
import com.example.bordermove26.data.models.PoseDetectionResult
import com.example.bordermove26.data.models.PoseLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerOptions
import com.google.mediapipe.framework.image.BitmapImageBuilder

class PoseDetectionRepository(private val context: Context) {

    private var poseLandmarker: PoseLandmarker? = null

    fun init() {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("pose_landmarker_lite.task")
            .build()

        val options = PoseLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.IMAGE)
            .setNumPoses(1)
            .build()

        poseLandmarker = PoseLandmarker.createFromOptions(context, options)
    }

    fun detectPose(bitmap: Bitmap, frameIndex: Int, timestamp: Long): PoseDetectionResult? {
        val mpImage = BitmapImageBuilder(bitmap).build()
        val result = poseLandmarker?.detect(mpImage) ?: return null

        if (result.landmarks().isEmpty()) {
            return null
        }

        val landmarks = extractRequiredLandmarks(result.landmarks()[0])

        return if (landmarks.isNotEmpty()) {
            PoseDetectionResult(
                frameIndex = frameIndex,
                timestamp = timestamp,
                landmarks = landmarks
            )
        } else {
            null
        }
    }

    private fun extractRequiredLandmarks(
        allLandmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>
    ): Map<LandmarkType, PoseLandmark> {
        val result = mutableMapOf<LandmarkType, PoseLandmark>()

        for (type in LandmarkType.values()) {
            val index = type.mediaPipeIndex
            if (index < allLandmarks.size) {
                val landmark = allLandmarks[index]
                result[type] = PoseLandmark(
                    x = landmark.x(),
                    y = landmark.y(),
                    visibility = landmark.visibility().orElse(0f)
                )
            }
        }

        return result
    }

    fun release() {
        poseLandmarker?.close()
        poseLandmarker = null
    }
}
