package com.example.bordermove26.viewmodels

import android.app.Application
import android.content.ContentValues
import android.graphics.PointF
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bordermove26.data.models.LandmarkType
import com.example.bordermove26.data.models.VideoProcessingState
import com.example.bordermove26.data.repositories.PoseDetectionRepository
import com.example.bordermove26.data.repositories.VideoCropRepository
import com.example.bordermove26.data.repositories.VideoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class VideoEditViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(VideoProcessingState())
    val state: StateFlow<VideoProcessingState> = _state.asStateFlow()

    private val poseDetectionRepository = PoseDetectionRepository(application)
    private val videoRepository = VideoRepository(application)
    private val videoCropRepository = VideoCropRepository(application)

    init {
        poseDetectionRepository.init()
    }

    fun selectVideo(uri: Uri) {
        viewModelScope.launch {
            try {
                val metadata = videoRepository.getVideoMetadata(uri)
                _state.value = _state.value.copy(
                    selectedVideoUri = uri,
                    videoMetadata = metadata,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    errorMessage = "Failed to load video: ${e.message}"
                )
            }
        }
    }

    fun processVideo() {
        val uri = _state.value.selectedVideoUri ?: return

        viewModelScope.launch(Dispatchers.Default) {
            try {
                _state.value = _state.value.copy(
                    isProcessing = true,
                    processingProgress = 0f,
                    landmarksByFrame = emptyMap(),
                    errorMessage = null
                )

                val landmarkMap = mutableMapOf<Int, com.example.bordermove26.data.models.PoseDetectionResult>()
                var totalFrames = 0
                var processedFrames = 0

                videoRepository.extractFrames(uri).collect { (frameIndex, bitmap) ->
                    totalFrames = maxOf(totalFrames, frameIndex + 1)

                    val result = poseDetectionRepository.detectPose(
                        bitmap,
                        frameIndex,
                        (frameIndex * 1000L / 30)
                    )

                    if (result != null && result.landmarks.size == 8) {
                        landmarkMap[frameIndex] = result
                    }

                    bitmap.recycle()

                    processedFrames++
                    val progress = processedFrames.toFloat() / totalFrames.toFloat().coerceAtLeast(1f)
                    _state.value = _state.value.copy(
                        processingProgress = progress,
                        landmarksByFrame = landmarkMap.toMap()
                    )
                }

                _state.value = _state.value.copy(
                    isProcessing = false,
                    processingProgress = 1f
                )

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isProcessing = false,
                    errorMessage = "Failed to process video: ${e.message}"
                )
            }
        }
    }

    fun calculateHipMidpoint(frameIndex: Int): PointF? {
        val pose = _state.value.landmarksByFrame[frameIndex] ?: return null
        val leftHip = pose.landmarks[LandmarkType.LEFT_HIP] ?: return null
        val rightHip = pose.landmarks[LandmarkType.RIGHT_HIP] ?: return null

        return PointF(
            (leftHip.x + rightHip.x) / 2f,
            (leftHip.y + rightHip.y) / 2f
        )
    }

    fun getHipTrajectory(): List<PointF> {
        val maxFrameIndex = _state.value.landmarksByFrame.keys.maxOrNull() ?: return emptyList()

        return (0..maxFrameIndex).mapNotNull { frameIndex ->
            calculateHipMidpoint(frameIndex)
        }
    }

    fun cropVideo() {
        val uri = _state.value.selectedVideoUri ?: return
        val metadata = _state.value.videoMetadata ?: return
        val trajectory = getHipTrajectory()

        if (trajectory.isEmpty()) {
            _state.value = _state.value.copy(
                errorMessage = "No hip trajectory available for cropping"
            )
            return
        }

        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(
                    isCropping = true,
                    croppingProgress = 0f,
                    errorMessage = null
                )

                val croppedUri = videoCropRepository.cropVideoWithDynamicTracking(
                    inputUri = uri,
                    hipTrajectory = trajectory,
                    metadata = metadata,
                    onProgress = { progress ->
                        _state.value = _state.value.copy(croppingProgress = progress)
                    }
                )

                _state.value = _state.value.copy(
                    isCropping = false,
                    croppedVideoUri = croppedUri,
                    croppingProgress = 1f,
                    errorMessage = if (croppedUri == null) "Failed to crop video" else null
                )

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isCropping = false,
                    errorMessage = "Failed to crop video: ${e.message}"
                )
            }
        }
    }

    suspend fun saveVideoToGallery(): Boolean = withContext(Dispatchers.IO) {
        val croppedUri = _state.value.croppedVideoUri ?: return@withContext false

        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, "bordermove_${System.currentTimeMillis()}.mp4")
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES)
            }

            val resolver = getApplication<Application>().contentResolver
            val insertUri = resolver.insert(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: return@withContext false

            resolver.openOutputStream(insertUri)?.use { outputStream ->
                val inputFile = File(croppedUri.path ?: return@withContext false)
                inputFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            true
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                errorMessage = "Failed to save video: ${e.message}"
            )
            false
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
        poseDetectionRepository.release()
    }
}
