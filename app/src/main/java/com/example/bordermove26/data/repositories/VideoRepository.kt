package com.example.bordermove26.data.repositories

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.example.bordermove26.data.models.VideoMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class VideoRepository(private val context: Context) {

    suspend fun getVideoMetadata(uri: Uri): VideoMetadata = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)

            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull() ?: 0
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L

            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                ?.toIntOrNull() ?: 0

            val (finalWidth, finalHeight) = if (rotation == 90 || rotation == 270) {
                height to width
            } else {
                width to height
            }

            val frameRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
                ?.toFloatOrNull() ?: 30f

            VideoMetadata(
                width = finalWidth,
                height = finalHeight,
                duration = duration,
                fps = frameRate
            )
        } finally {
            retriever.release()
        }
    }

    fun extractFrames(
        uri: Uri,
        targetFps: Float = 30f
    ): Flow<Pair<Int, Bitmap>> = flow {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)

            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L

            val frameDurationUs = (1_000_000f / targetFps).toLong()

            var frameIndex = 0
            var currentTimeUs = 0L

            while (currentTimeUs <= duration * 1000) {
                val bitmap = retriever.getFrameAtTime(
                    currentTimeUs,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )

                if (bitmap != null) {
                    val downsampledBitmap = downsampleBitmap(bitmap, 640, 480)
                    emit(Pair(frameIndex, downsampledBitmap))

                    if (downsampledBitmap != bitmap) {
                        bitmap.recycle()
                    }
                }

                frameIndex++
                currentTimeUs += frameDurationUs
            }
        } finally {
            retriever.release()
        }
    }

    private fun downsampleBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }

        val ratio = minOf(
            maxWidth.toFloat() / width,
            maxHeight.toFloat() / height
        )

        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
