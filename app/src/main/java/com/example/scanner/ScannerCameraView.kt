package com.example.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.nio.ByteBuffer
import java.util.concurrent.Executors

@Composable
fun ScannerCameraView(
    pageCount: Int,
    onImageCaptured: (Bitmap) -> Unit,
    onGalleryPickRequest: () -> Unit,
    onDoneScanning: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_AUTO) }
    var isCapturing by remember { mutableStateOf(false) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        // Camera Preview
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val provider = cameraProviderFuture.get()
                    cameraProvider = provider

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val capture = ImageCapture.Builder()
                        .setFlashMode(flashMode)
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .build()
                    imageCapture = capture

                    val cameraSelector = CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build()

                    try {
                        provider.unbindAll()
                        provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, capture)
                    } catch (exc: Exception) {
                        Log.e("ScannerCameraView", "Use case binding failed", exc)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            update = {
                imageCapture?.flashMode = flashMode
            }
        )

        // Document framing overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height
            val frameW = canvasW * 0.86f
            val frameH = frameW * 1.414f // A4 ratio
            val left = (canvasW - frameW) / 2f
            val top = (canvasH - frameH) / 2f - 40.dp.toPx()

            // Dark semi-transparent mask outside framing rect
            drawRect(
                color = Color.Black.copy(alpha = 0.45f),
                size = size
            )

            // Clear view inside document frame (transparent cutout)
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(left, top),
                size = Size(frameW, frameH),
                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
            )

            // High-contrast document boundary stroke
            drawRoundRect(
                color = Color(0xFF60A5FA),
                topLeft = Offset(left, top),
                size = Size(frameW, frameH),
                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                style = Stroke(width = 3.dp.toPx())
            )

            // Scanning corner accents
            val cornerLen = 32.dp.toPx()
            val strokeW = 5.dp.toPx()
            val cornerColor = Color(0xFF3B82F6)

            // Top-Left
            drawLine(cornerColor, Offset(left, top), Offset(left + cornerLen, top), strokeW)
            drawLine(cornerColor, Offset(left, top), Offset(left, top + cornerLen), strokeW)

            // Top-Right
            drawLine(cornerColor, Offset(left + frameW, top), Offset(left + frameW - cornerLen, top), strokeW)
            drawLine(cornerColor, Offset(left + frameW, top), Offset(left + frameW, top + cornerLen), strokeW)

            // Bottom-Left
            drawLine(cornerColor, Offset(left, top + frameH), Offset(left + cornerLen, top + frameH), strokeW)
            drawLine(cornerColor, Offset(left, top + frameH), Offset(left, top + frameH - cornerLen), strokeW)

            // Bottom-Right
            drawLine(cornerColor, Offset(left + frameW, top + frameH), Offset(left + frameW - cornerLen, top + frameH), strokeW)
            drawLine(cornerColor, Offset(left + frameW, top + frameH), Offset(left + frameW, top + frameH - cornerLen), strokeW)
        }

        // Top Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 40.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }

            // Document guidance pill
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Align document in frame",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Flash mode toggle
            IconButton(
                onClick = {
                    flashMode = when (flashMode) {
                        ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                        ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_OFF
                        else -> ImageCapture.FLASH_MODE_AUTO
                    }
                },
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                val (icon, desc) = when (flashMode) {
                    ImageCapture.FLASH_MODE_ON -> Pair(Icons.Default.FlashOn, "Flash On")
                    ImageCapture.FLASH_MODE_OFF -> Pair(Icons.Default.FlashOff, "Flash Off")
                    else -> Pair(Icons.Default.FlashAuto, "Flash Auto")
                }
                Icon(icon, contentDescription = desc, tint = Color.White)
            }
        }

        // Bottom Controls Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.8f))
                .padding(bottom = 32.dp, top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery import button
                IconButton(
                    onClick = onGalleryPickRequest,
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.PhotoLibrary,
                        contentDescription = "Import from gallery",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Shutter Capture Button
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .border(4.dp, Color.White, CircleShape)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(if (isCapturing) Color.LightGray else Color.White)
                        .clickable(enabled = !isCapturing) {
                            val capture = imageCapture ?: return@clickable
                            isCapturing = true

                            capture.takePicture(
                                cameraExecutor,
                                object : ImageCapture.OnImageCapturedCallback() {
                                    override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                        val bitmap = imageProxyToBitmap(imageProxy)
                                        imageProxy.close()
                                        ContextCompat.getMainExecutor(context).execute {
                                            isCapturing = false
                                            if (bitmap != null) {
                                                onImageCaptured(bitmap)
                                            }
                                        }
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        Log.e("ScannerCameraView", "Capture failed", exception)
                                        ContextCompat.getMainExecutor(context).execute {
                                            isCapturing = false
                                        }
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isCapturing) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Pages Count / Review Button
                if (pageCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { onDoneScanning() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$pageCount",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                } else {
                    // Switch camera toggle
                    IconButton(
                        onClick = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                CameraSelector.LENS_FACING_FRONT
                            } else {
                                CameraSelector.LENS_FACING_BACK
                            }
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Cameraswitch,
                            contentDescription = "Switch Camera",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            if (pageCount > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "$pageCount page${if (pageCount > 1) "s" else ""} scanned - Tap circle to finish",
                    color = Color(0xFF93C5FD),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
    val buffer: ByteBuffer = imageProxy.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null

    val rotation = imageProxy.imageInfo.rotationDegrees
    return if (rotation != 0) {
        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        val rotated = Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
        if (rotated != original) {
            original.recycle()
        }
        rotated
    } else {
        original
    }
}
