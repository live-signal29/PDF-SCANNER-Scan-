package com.example.ocr

import android.content.Context
import android.graphics.Bitmap
import com.example.pdf.PdfEngine
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

object OcrEngine {

    val supportedLanguages = listOf(
        "English",
        "Spanish (Español)",
        "Hindi (हिन्दी)",
        "Arabic (العربية)",
        "Urdu (اردو)"
    )

    suspend fun extractTextFromBitmap(bitmap: Bitmap): Result<String> = withContext(Dispatchers.IO) {
        try {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val image = InputImage.fromBitmap(bitmap, 0)

            suspendCancellableCoroutine { continuation ->
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val text = visionText.text.trim()
                        if (text.isNotEmpty()) {
                            continuation.resume(Result.success(text))
                        } else {
                            continuation.resume(Result.failure(Exception("Text could not be detected. Try a clearer image.")))
                        }
                    }
                    .addOnFailureListener { error ->
                        continuation.resume(Result.failure(Exception(error.message ?: "Text could not be detected. Try a clearer image.")))
                    }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Text could not be detected. Try a clearer image."))
        }
    }

    suspend fun extractTextFromPdf(pdfFile: File, maxPages: Int = 10): Result<String> = withContext(Dispatchers.IO) {
        try {
            val pages = PdfEngine.renderAllPages(pdfFile, maxPages = maxPages, targetWidth = 1200)
            if (pages.isEmpty()) {
                return@withContext Result.failure(Exception("Unable to read PDF pages for OCR."))
            }

            val fullText = StringBuilder()
            var detectedAny = false

            pages.forEachIndexed { index, bitmap ->
                val result = extractTextFromBitmap(bitmap)
                if (result.isSuccess) {
                    val pageText = result.getOrNull()
                    if (!pageText.isNullOrBlank()) {
                        detectedAny = true
                        fullText.append("--- Page ${index + 1} ---\n")
                        fullText.append(pageText)
                        fullText.append("\n\n")
                    }
                }
                bitmap.recycle()
            }

            if (detectedAny) {
                Result.success(fullText.toString().trim())
            } else {
                Result.failure(Exception("Text could not be detected. Try a clearer image."))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Text could not be detected. Try a clearer image."))
        }
    }
}
