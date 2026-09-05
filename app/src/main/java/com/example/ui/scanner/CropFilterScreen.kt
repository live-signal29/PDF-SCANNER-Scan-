package com.example.ui.scanner

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ads.AdManager
import com.example.data.local.AppDatabase
import com.example.data.local.DocumentCategory
import com.example.data.local.DocumentRepository
import com.example.data.preferences.AppPreferences
import com.example.pdf.PdfEngine
import com.example.scanner.ImageProcessor
import com.example.scanner.ScanFilter
import com.example.ui.components.AppTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CropFilterScreen(
    onSaved: (Long) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember { AppPreferences(context) }

    if (ScanSessionState.scannedPages.isEmpty()) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    var currentPageIndex by remember { mutableIntStateOf(ScanSessionState.editingPageIndex) }
    var selectedFilter by remember { mutableStateOf(ScanFilter.MAGIC_COLOR) }
    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var rotation by remember { mutableFloatStateOf(0f) }

    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessingPreview by remember { mutableStateOf(false) }
    var isSavingPdf by remember { mutableStateOf(false) }

    var pdfFileName by remember {
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        mutableStateOf("Scan_$dateStr.pdf")
    }

    // Refresh preview whenever adjustments change
    LaunchedEffect(currentPageIndex, selectedFilter, brightness, contrast, rotation) {
        val original = ScanSessionState.scannedPages.getOrNull(currentPageIndex) ?: return@LaunchedEffect
        isProcessingPreview = true
        previewBitmap = ImageProcessor.processImage(
            source = original,
            filter = selectedFilter,
            brightness = brightness,
            contrast = contrast,
            rotation = rotation
        )
        isProcessingPreview = false
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Adjust & Filters",
                onBackClick = onBack,
                actions = {
                    TextButton(
                        onClick = {
                            if (isSavingPdf) return@TextButton
                            isSavingPdf = true

                            scope.launch {
                                val processedBitmaps = mutableListOf<Bitmap>()
                                ScanSessionState.scannedPages.forEach { page ->
                                    val processed = ImageProcessor.processImage(
                                        source = page,
                                        filter = selectedFilter,
                                        brightness = brightness,
                                        contrast = contrast,
                                        rotation = rotation
                                    )
                                    processedBitmaps.add(processed)
                                }

                                val docDir = File(context.filesDir, "documents").apply { mkdirs() }
                                val cleanName = if (pdfFileName.endsWith(".pdf")) pdfFileName else "$pdfFileName.pdf"
                                val outFile = File(docDir, cleanName)

                                val result = PdfEngine.createPdfFromBitmaps(
                                    bitmaps = processedBitmaps,
                                    outputFile = outFile,
                                    pageSize = preferences.defaultPageSize.value,
                                    qualityPercent = preferences.defaultPdfQuality.value.qualityPercent
                                )

                                if (result.isSuccess) {
                                    val db = AppDatabase.getDatabase(context)
                                    val repo = DocumentRepository(db.documentDao(), context)
                                    val docId = repo.insertDocument(
                                        file = outFile,
                                        category = DocumentCategory.SCANNED,
                                        pageCount = processedBitmaps.size
                                    )

                                    ScanSessionState.clear()

                                    // Show natural transition interstitial ad if free user
                                    val activity = context as? Activity
                                    if (activity != null) {
                                        AdManager.showInterstitial(activity, preferences.isProUser.value) {
                                            onSaved(docId)
                                        }
                                    } else {
                                        onSaved(docId)
                                    }
                                }
                                isSavingPdf = false
                            }
                        },
                        enabled = !isSavingPdf,
                        modifier = Modifier.testTag("save_pdf_top_btn")
                    ) {
                        if (isSavingPdf) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp))
                        } else {
                            Text("Save PDF", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .navigationBarsPadding()
        ) {
            // Image Preview Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                previewBitmap?.let { bmp ->
                    Card(
                        modifier = Modifier
                            .fillMaxSize(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Scan Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                if (isProcessingPreview) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }

                // Page switcher overlay if multi-page
                if (ScanSessionState.scannedPages.size > 1) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (currentPageIndex > 0) currentPageIndex--
                            },
                            enabled = currentPageIndex > 0,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous", tint = Color.White)
                        }

                        Text(
                            text = "Page ${currentPageIndex + 1} of ${ScanSessionState.scannedPages.size}",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        IconButton(
                            onClick = {
                                if (currentPageIndex < ScanSessionState.scannedPages.size - 1) currentPageIndex++
                            },
                            enabled = currentPageIndex < ScanSessionState.scannedPages.size - 1,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next", tint = Color.White)
                        }
                    }
                }
            }

            // Controls Panel
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Filter presets
                    Text(
                        text = "Document Filters",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ScanFilter.values().forEach { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = { Text(filter.displayName, fontSize = 12.sp) },
                                shape = RoundedCornerShape(16.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.testTag("filter_chip_${filter.name}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Adjustments (Rotate, Brightness, Contrast)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Fine Adjustments",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Rotate button
                        Button(
                            onClick = { rotation = (rotation + 90f) % 360f },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("rotate_button")
                        ) {
                            Icon(Icons.Default.RotateRight, contentDescription = "Rotate", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rotate 90°", fontSize = 12.sp)
                        }
                    }

                    // Brightness slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Brightness6, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Brightness", fontSize = 12.sp, modifier = Modifier.width(70.dp))
                        Slider(
                            value = brightness,
                            onValueChange = { brightness = it },
                            valueRange = -80f..80f,
                            modifier = Modifier.weight(1f).testTag("brightness_slider")
                        )
                    }

                    // Contrast slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Contrast, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Contrast", fontSize = 12.sp, modifier = Modifier.width(70.dp))
                        Slider(
                            value = contrast,
                            onValueChange = { contrast = it },
                            valueRange = 0.5f..2.0f,
                            modifier = Modifier.weight(1f).testTag("contrast_slider")
                        )
                    }
                }
            }
        }
    }
}
