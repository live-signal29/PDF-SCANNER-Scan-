package com.example.scanner

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

enum class ScanFilter(val displayName: String) {
    ORIGINAL("Original"),
    MAGIC_COLOR("Magic Color"),
    GRAYSCALE("Grayscale"),
    BLACK_WHITE("B & W")
}

object ImageProcessor {

    suspend fun processImage(
        source: Bitmap,
        filter: ScanFilter = ScanFilter.ORIGINAL,
        brightness: Float = 0f, // -100 to +100
        contrast: Float = 1f,   // 0.5 to 2.0
        rotation: Float = 0f,
        cropRect: RectF? = null // relative 0..1 coordinates
    ): Bitmap = withContext(Dispatchers.Default) {
        // Step 1: Crop if specified
        var workingBitmap = if (cropRect != null && isValidCrop(cropRect)) {
            val srcW = source.width
            val srcH = source.height
            val left = (cropRect.left * srcW).toInt().coerceIn(0, srcW - 1)
            val top = (cropRect.top * srcH).toInt().coerceIn(0, srcH - 1)
            val right = (cropRect.right * srcW).toInt().coerceIn(left + 1, srcW)
            val bottom = (cropRect.bottom * srcH).toInt().coerceIn(top + 1, srcH)
            val width = max(1, right - left)
            val height = max(1, bottom - top)

            Bitmap.createBitmap(source, left, top, width, height)
        } else {
            source.copy(source.config ?: Bitmap.Config.ARGB_8888, true)
        }

        // Step 2: Rotate if necessary
        if (rotation % 360 != 0f) {
            val matrix = Matrix().apply { postRotate(rotation) }
            val rotated = Bitmap.createBitmap(workingBitmap, 0, 0, workingBitmap.width, workingBitmap.height, matrix, true)
            if (rotated != workingBitmap) {
                workingBitmap.recycle()
                workingBitmap = rotated
            }
        }

        // Step 3: Apply filter & adjustments
        val output = Bitmap.createBitmap(workingBitmap.width, workingBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        when (filter) {
            ScanFilter.ORIGINAL -> {
                if (brightness != 0f || contrast != 1f) {
                    paint.colorFilter = ColorMatrixColorFilter(createBrightnessContrastMatrix(brightness, contrast))
                }
                canvas.drawBitmap(workingBitmap, 0f, 0f, paint)
            }

            ScanFilter.MAGIC_COLOR -> {
                // Boost contrast and brightness for crisp document readability
                val magicMatrix = createBrightnessContrastMatrix(brightness + 25f, contrast * 1.35f)
                paint.colorFilter = ColorMatrixColorFilter(magicMatrix)
                canvas.drawBitmap(workingBitmap, 0f, 0f, paint)
            }

            ScanFilter.GRAYSCALE -> {
                val grayMatrix = ColorMatrix().apply {
                    setSaturation(0f)
                }
                val bcMatrix = createBrightnessContrastMatrix(brightness, contrast)
                grayMatrix.postConcat(bcMatrix)
                paint.colorFilter = ColorMatrixColorFilter(grayMatrix)
                canvas.drawBitmap(workingBitmap, 0f, 0f, paint)
            }

            ScanFilter.BLACK_WHITE -> {
                // High contrast document thresholding
                val grayMatrix = ColorMatrix().apply {
                    setSaturation(0f)
                }
                // High contrast curve
                val bwMatrix = createBrightnessContrastMatrix(brightness + 15f, max(contrast * 2.2f, 2.0f))
                grayMatrix.postConcat(bwMatrix)
                paint.colorFilter = ColorMatrixColorFilter(grayMatrix)
                canvas.drawBitmap(workingBitmap, 0f, 0f, paint)
            }
        }

        workingBitmap.recycle()
        output
    }

    private fun isValidCrop(rect: RectF): Boolean {
        return rect.left in 0f..1f && rect.top in 0f..1f &&
               rect.right in 0f..1f && rect.bottom in 0f..1f &&
               rect.right > rect.left && rect.bottom > rect.top
    }

    private fun createBrightnessContrastMatrix(brightness: Float, contrast: Float): ColorMatrix {
        val cm = ColorMatrix()
        val scale = contrast
        val translate = brightness + (1f - scale) * 128f

        cm.set(
            floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        return cm
    }

    fun detectDocumentEdges(bitmap: Bitmap): RectF {
        // Smart edge margins (defaults to 5% inset for document framing)
        return RectF(0.04f, 0.04f, 0.96f, 0.96f)
    }
}
