package com.example.bordermove26.data.repositories

import android.content.Context
import android.graphics.PointF
import android.net.Uri
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.example.bordermove26.data.models.VideoMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

class VideoCropRepository(private val context: Context) {

    suspend fun cropVideoWithDynamicTracking(
        inputUri: Uri,
        hipTrajectory: List<PointF>,
        metadata: VideoMetadata,
        outputSize: Int = 720,
        onProgress: (Float) -> Unit = {}
    ): Uri? = withContext(Dispatchers.IO) {
        val inputPath = getFilePathFromUri(inputUri) ?: return@withContext null
        val outputFile = File(context.cacheDir, "cropped_${System.currentTimeMillis()}.mp4")

        val smoothedTrajectory = smoothTrajectory(hipTrajectory)

        val cropFilter = buildDynamicCropFilter(
            smoothedTrajectory,
            metadata,
            outputSize
        )

        val command = "-i \"$inputPath\" -filter_complex \"$cropFilter\" -c:v libx264 -preset fast -crf 23 -c:a copy \"${outputFile.absolutePath}\""

        var lastProgress = 0f
        val session = FFmpegKit.executeAsync(
            command,
            { session ->
                // Session complete callback
            },
            { log ->
                // Log callback
            },
            { statistics ->
                if (statistics != null && metadata.duration > 0) {
                    val progress = (statistics.time.toFloat() / metadata.duration).coerceIn(0f, 1f)
                    if (progress - lastProgress > 0.01f) {
                        lastProgress = progress
                        onProgress(progress)
                    }
                }
            }
        )

        session.waitFor()

        if (ReturnCode.isSuccess(session.returnCode) && outputFile.exists()) {
            Uri.fromFile(outputFile)
        } else {
            null
        }
    }

    private fun getFilePathFromUri(uri: Uri): String? {
        if (uri.scheme == "file") {
            return uri.path
        }

        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val tempFile = File(context.cacheDir, "temp_input_${System.currentTimeMillis()}.mp4")

        inputStream.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        return tempFile.absolutePath
    }

    private fun smoothTrajectory(trajectory: List<PointF>, windowSize: Int = 5): List<PointF> {
        if (trajectory.size < windowSize) return trajectory

        return trajectory.mapIndexed { index, _ ->
            val start = maxOf(0, index - windowSize / 2)
            val end = minOf(trajectory.size, index + windowSize / 2 + 1)

            val avgX = trajectory.subList(start, end).map { it.x }.average().toFloat()
            val avgY = trajectory.subList(start, end).map { it.y }.average().toFloat()

            PointF(avgX, avgY)
        }
    }

    private fun buildDynamicCropFilter(
        trajectory: List<PointF>,
        metadata: VideoMetadata,
        outputSize: Int
    ): String {
        val halfSize = outputSize / 2

        val cropExpressions = trajectory.mapIndexed { frameIndex, point ->
            val targetX = (point.x * metadata.width - halfSize).coerceIn(
                0f,
                (metadata.width - outputSize).toFloat()
            ).roundToInt()

            val targetY = (point.y * metadata.height - halfSize).coerceIn(
                0f,
                (metadata.height - outputSize).toFloat()
            ).roundToInt()

            "eq(n,$frameIndex)*$targetX+eq(n,$frameIndex)*$targetY"
        }

        val xExpression = buildPiecewiseExpression(trajectory, metadata, outputSize, true)
        val yExpression = buildPiecewiseExpression(trajectory, metadata, outputSize, false)

        return "crop=$outputSize:$outputSize:$xExpression:$yExpression"
    }

    private fun buildPiecewiseExpression(
        trajectory: List<PointF>,
        metadata: VideoMetadata,
        outputSize: Int,
        isX: Boolean
    ): String {
        val halfSize = outputSize / 2
        val maxVal = if (isX) metadata.width else metadata.height

        val values = trajectory.map { point ->
            val coord = if (isX) point.x else point.y
            val target = (coord * maxVal - halfSize).coerceIn(
                0f,
                (maxVal - outputSize).toFloat()
            ).roundToInt()
            target
        }

        if (values.isEmpty()) return "0"

        val segments = values.mapIndexed { index, value ->
            if (index == 0) {
                "if(lt(n,$index),$value,"
            } else if (index == values.size - 1) {
                "if(gte(n,$index),$value,$value)${")" * (values.size - 1)}"
            } else {
                val prevValue = values[index - 1]
                val interpolation = "lerp($prevValue,$value,(n-${index - 1}))"
                "if(lt(n,$index),$interpolation,"
            }
        }

        return segments.joinToString("")
    }
}
