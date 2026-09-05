package com.example.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.example.data.preferences.DefaultPageSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

enum class CompressionLevel(val title: String, val description: String, val scaleFactor: Float, val jpegQuality: Int) {
    LOW("Low Compression", "High Quality - Best for reading & printing", 0.9f, 85),
    MEDIUM("Medium Compression", "Balanced - Good quality & smaller file", 0.75f, 65),
    HIGH("High Compression", "Maximum space saving - Smallest file", 0.55f, 40)
}

object PdfEngine {

    suspend fun createPdfFromBitmaps(
        bitmaps: List<Bitmap>,
        outputFile: File,
        pageSize: DefaultPageSize = DefaultPageSize.A4,
        qualityPercent: Int = 85,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val pdfDocument = PdfDocument()
            val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)

            bitmaps.forEachIndexed { index, bitmap ->
                val (pageWidth, pageHeight) = when (pageSize) {
                    DefaultPageSize.A4 -> Pair(595, 842)
                    DefaultPageSize.LETTER -> Pair(612, 792)
                    DefaultPageSize.ORIGINAL -> Pair(bitmap.width, bitmap.height)
                }

                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // Fill canvas with clean white background
                canvas.drawColor(Color.WHITE)

                // Scale and fit bitmap inside page preserving aspect ratio
                val srcWidth = bitmap.width.toFloat()
                val srcHeight = bitmap.height.toFloat()
                val scale = minOf(pageWidth / srcWidth, pageHeight / srcHeight)

                val destWidth = srcWidth * scale
                val destHeight = srcHeight * scale
                val destLeft = (pageWidth - destWidth) / 2f
                val destTop = (pageHeight - destHeight) / 2f

                val destRect = RectF(destLeft, destTop, destLeft + destWidth, destTop + destHeight)
                canvas.drawBitmap(bitmap, null, destRect, paint)

                pdfDocument.finishPage(page)
                onProgress((index + 1) / bitmaps.size.toFloat())
            }

            FileOutputStream(outputFile).use { fos ->
                pdfDocument.writeTo(fos)
            }
            pdfDocument.close()
            Result.success(outputFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun getPageCount(file: File): Int = withContext(Dispatchers.IO) {
        try {
            if (!file.exists() || file.length() == 0L) return@withContext 0
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    renderer.pageCount
                }
            }
        } catch (e: Exception) {
            0
        }
    }

    suspend fun renderPageToBitmap(
        file: File,
        pageIndex: Int,
        targetWidth: Int = 1080
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            if (!file.exists() || file.length() == 0L) return@withContext null
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withContext null
                    renderer.openPage(pageIndex).use { page ->
                        val aspectRatio = page.height.toFloat() / page.width.toFloat()
                        val width = targetWidth
                        val height = (targetWidth * aspectRatio).roundToInt()

                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        canvas.drawColor(Color.WHITE)

                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun renderAllPages(
        file: File,
        maxPages: Int = 50,
        targetWidth: Int = 900
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Bitmap>()
        try {
            if (!file.exists() || file.length() == 0L) return@withContext emptyList()
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val pagesToRender = minOf(renderer.pageCount, maxPages)
                    for (i in 0 until pagesToRender) {
                        renderer.openPage(i).use { page ->
                            val aspectRatio = page.height.toFloat() / page.width.toFloat()
                            val width = targetWidth
                            val height = (targetWidth * aspectRatio).roundToInt()

                            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            val canvas = Canvas(bitmap)
                            canvas.drawColor(Color.WHITE)

                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            list.add(bitmap)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    suspend fun compressPdf(
        sourceFile: File,
        outputFile: File,
        level: CompressionLevel,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val pdfDocument = PdfDocument()
            val paint = Paint(Paint.FILTER_BITMAP_FLAG)

            ParcelFileDescriptor.open(sourceFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val count = renderer.pageCount
                    if (count == 0) return@withContext Result.failure(Exception("PDF has 0 pages"))

                    for (i in 0 until count) {
                        renderer.openPage(i).use { page ->
                            val scaledWidth = (page.width * level.scaleFactor).roundToInt()
                            val scaledHeight = (page.height * level.scaleFactor).roundToInt()

                            val tempBitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)
                            val canvas = Canvas(tempBitmap)
                            canvas.drawColor(Color.WHITE)
                            page.render(tempBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                            // Compress with JPEG stream to reduce raster density
                            val stream = ByteArrayOutputStream()
                            tempBitmap.compress(Bitmap.CompressFormat.JPEG, level.jpegQuality, stream)
                            tempBitmap.recycle()

                            val compressedBytes = stream.toByteArray()
                            val finalBitmap = BitmapFactory.decodeByteArray(compressedBytes, 0, compressedBytes.size)

                            val pageInfo = PdfDocument.PageInfo.Builder(page.width, page.height, i + 1).create()
                            val docPage = pdfDocument.startPage(pageInfo)
                            val pageCanvas = docPage.canvas
                            pageCanvas.drawColor(Color.WHITE)
                            val destRect = Rect(0, 0, page.width, page.height)
                            pageCanvas.drawBitmap(finalBitmap, null, destRect, paint)
                            pdfDocument.finishPage(docPage)
                            finalBitmap.recycle()
                        }
                        onProgress((i + 1) / count.toFloat())
                    }
                }
            }

            FileOutputStream(outputFile).use { fos ->
                pdfDocument.writeTo(fos)
            }
            pdfDocument.close()
            Result.success(outputFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun mergePdfs(
        sourceFiles: List<File>,
        outputFile: File,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val pdfDocument = PdfDocument()
            val paint = Paint(Paint.FILTER_BITMAP_FLAG)
            var currentPageNumber = 1

            // Count total pages first
            var totalPages = 0
            sourceFiles.forEach { file ->
                totalPages += getPageCount(file)
            }
            if (totalPages == 0) totalPages = 1

            var processedPages = 0

            for (file in sourceFiles) {
                if (!file.exists()) continue
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        for (i in 0 until renderer.pageCount) {
                            renderer.openPage(i).use { page ->
                                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                                val canvas = Canvas(bitmap)
                                canvas.drawColor(Color.WHITE)
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                                val pageInfo = PdfDocument.PageInfo.Builder(page.width, page.height, currentPageNumber++).create()
                                val docPage = pdfDocument.startPage(pageInfo)
                                docPage.canvas.drawBitmap(bitmap, 0f, 0f, paint)
                                pdfDocument.finishPage(docPage)
                                bitmap.recycle()
                            }
                            processedPages++
                            onProgress(processedPages / totalPages.toFloat())
                        }
                    }
                }
            }

            FileOutputStream(outputFile).use { fos ->
                pdfDocument.writeTo(fos)
            }
            pdfDocument.close()
            Result.success(outputFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun splitPdf(
        sourceFile: File,
        selectedPages: List<Int>, // 0-indexed
        outputFiles: List<File>,
        onProgress: (Float) -> Unit = {}
    ): Result<List<File>> = withContext(Dispatchers.IO) {
        try {
            val generatedFiles = mutableListOf<File>()
            val paint = Paint(Paint.FILTER_BITMAP_FLAG)

            ParcelFileDescriptor.open(sourceFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    selectedPages.forEachIndexed { index, pageIdx ->
                        if (pageIdx in 0 until renderer.pageCount) {
                            val outFile = outputFiles.getOrElse(index) {
                                File(sourceFile.parentFile, "${sourceFile.nameWithoutExtension}_page_${pageIdx + 1}.pdf")
                            }
                            val singleDoc = PdfDocument()
                            renderer.openPage(pageIdx).use { page ->
                                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                                val canvas = Canvas(bitmap)
                                canvas.drawColor(Color.WHITE)
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                                val pageInfo = PdfDocument.PageInfo.Builder(page.width, page.height, 1).create()
                                val docPage = singleDoc.startPage(pageInfo)
                                docPage.canvas.drawBitmap(bitmap, 0f, 0f, paint)
                                singleDoc.finishPage(docPage)
                                bitmap.recycle()
                            }
                            FileOutputStream(outFile).use { fos ->
                                singleDoc.writeTo(fos)
                            }
                            singleDoc.close()
                            generatedFiles.add(outFile)
                        }
                        onProgress((index + 1) / selectedPages.size.toFloat())
                    }
                }
            }
            Result.success(generatedFiles)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun pdfToImages(
        sourceFile: File,
        outputDir: File,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
        quality: Int = 90,
        targetWidth: Int = 1200,
        onProgress: (Float) -> Unit = {}
    ): Result<List<File>> = withContext(Dispatchers.IO) {
        try {
            val imageFiles = mutableListOf<File>()
            val ext = if (format == Bitmap.CompressFormat.PNG) "png" else "jpg"

            ParcelFileDescriptor.open(sourceFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val total = renderer.pageCount
                    for (i in 0 until total) {
                        renderer.openPage(i).use { page ->
                            val aspectRatio = page.height.toFloat() / page.width.toFloat()
                            val width = targetWidth
                            val height = (targetWidth * aspectRatio).roundToInt()

                            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            val canvas = Canvas(bitmap)
                            canvas.drawColor(Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                            val outFile = File(outputDir, "${sourceFile.nameWithoutExtension}_page_${i + 1}.$ext")
                            FileOutputStream(outFile).use { fos ->
                                bitmap.compress(format, quality, fos)
                            }
                            bitmap.recycle()
                            imageFiles.add(outFile)
                        }
                        onProgress((i + 1) / total.toFloat())
                    }
                }
            }
            Result.success(imageFiles)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun signPdf(
        sourceFile: File,
        outputFile: File,
        targetPageIndex: Int,
        signatureBitmap: Bitmap,
        normX: Float, // 0..1 relative to page width
        normY: Float, // 0..1 relative to page height
        scaleMultiplier: Float = 1.0f,
        rotationDegrees: Float = 0f
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val pdfDocument = PdfDocument()
            val paint = Paint(Paint.FILTER_BITMAP_FLAG)

            ParcelFileDescriptor.open(sourceFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    for (i in 0 until renderer.pageCount) {
                        renderer.openPage(i).use { page ->
                            val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                            val canvas = Canvas(bitmap)
                            canvas.drawColor(Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                            // Overlay signature on target page
                            if (i == targetPageIndex) {
                                val matrix = Matrix()
                                val sigW = signatureBitmap.width * scaleMultiplier
                                val sigH = signatureBitmap.height * scaleMultiplier
                                val posX = normX * page.width - (sigW / 2f)
                                val posY = normY * page.height - (sigH / 2f)

                                matrix.postScale(scaleMultiplier, scaleMultiplier)
                                matrix.postRotate(rotationDegrees, sigW / 2f, sigH / 2f)
                                matrix.postTranslate(posX, posY)

                                canvas.drawBitmap(signatureBitmap, matrix, paint)
                            }

                            val pageInfo = PdfDocument.PageInfo.Builder(page.width, page.height, i + 1).create()
                            val docPage = pdfDocument.startPage(pageInfo)
                            docPage.canvas.drawBitmap(bitmap, 0f, 0f, paint)
                            pdfDocument.finishPage(docPage)
                            bitmap.recycle()
                        }
                    }
                }
            }

            FileOutputStream(outputFile).use { fos ->
                pdfDocument.writeTo(fos)
            }
            pdfDocument.close()
            Result.success(outputFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
