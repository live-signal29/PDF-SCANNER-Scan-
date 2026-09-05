package com.example.ui.tools

import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.example.data.preferences.DefaultPageSize
import com.example.data.preferences.DefaultPdfQuality
import com.example.pdf.PdfEngine
import com.example.ui.components.AppTopBar
import com.example.ui.scanner.loadBitmapFromUri
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ImageToPdfScreen(
    onSaved: (Long) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember { AppPreferences(context) }

    val selectedBitmaps = remember { mutableStateListOf<Bitmap>() }
    var pageSize by remember { mutableStateOf(preferences.defaultPageSize.value) }
    var quality by remember { mutableStateOf(preferences.defaultPdfQuality.value) }
    var fileName by remember {
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        mutableStateOf("Images_$dateStr.pdf")
    }
    var isConverting by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            val bmp = loadBitmapFromUri(context, uri)
            if (bmp != null) {
                selectedBitmaps.add(bmp)
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(title = "Images to PDF", onBackClick = onBack)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Image selector box / list
                Text(
                    text = "Selected Images (${selectedBitmaps.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (selectedBitmaps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .clickable { galleryLauncher.launch("image/*") }
                            .testTag("pick_images_empty_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Select photos from gallery",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "PNG, JPG, HEIC formats supported",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { galleryLauncher.launch("image/*") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("add_more_photos_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add More Photos")
                        }
                    }

                    // Grid of selected images
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(selectedBitmaps) { index, bmp ->
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.LightGray)
                            ) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Photo ${index + 1}",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )

                                // Remove button
                                IconButton(
                                    onClick = { selectedBitmaps.removeAt(index) },
                                    modifier = Modifier
                                        .size(24.dp)
                                        .align(Alignment.TopEnd)
                                        .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                // Index badge
                                Box(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .align(Alignment.BottomStart),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("${index + 1}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // File Name
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("PDF File Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("image_to_pdf_filename_input")
                )

                // Page Size selection
                Column {
                    Text(
                        text = "Page Format",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DefaultPageSize.values().forEach { size ->
                            FilterChip(
                                selected = pageSize == size,
                                onClick = { pageSize = size },
                                label = { Text(size.title, fontSize = 12.sp) }
                            )
                        }
                    }
                }

                // PDF Quality
                Column {
                    Text(
                        text = "PDF Quality",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DefaultPdfQuality.values().forEach { q ->
                            FilterChip(
                                selected = quality == q,
                                onClick = { quality = q },
                                label = { Text(q.title.substringBefore(" ("), fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }

            // Bottom Convert Button
            Box(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Button(
                    onClick = {
                        if (selectedBitmaps.isEmpty() || isConverting) return@Button
                        isConverting = true

                        scope.launch {
                            val docDir = File(context.filesDir, "documents").apply { mkdirs() }
                            val cleanName = if (fileName.endsWith(".pdf")) fileName else "$fileName.pdf"
                            val outFile = File(docDir, cleanName)

                            val result = PdfEngine.createPdfFromBitmaps(
                                bitmaps = selectedBitmaps.toList(),
                                outputFile = outFile,
                                pageSize = pageSize,
                                qualityPercent = quality.qualityPercent
                            )

                            if (result.isSuccess) {
                                val db = AppDatabase.getDatabase(context)
                                val repo = DocumentRepository(db.documentDao(), context)
                                val docId = repo.insertDocument(
                                    file = outFile,
                                    category = DocumentCategory.IMAGE_TO_PDF,
                                    pageCount = selectedBitmaps.size
                                )

                                val activity = context as? Activity
                                if (activity != null) {
                                    AdManager.showInterstitial(activity, preferences.isProUser.value) {
                                        onSaved(docId)
                                    }
                                } else {
                                    onSaved(docId)
                                }
                            }
                            isConverting = false
                        }
                    },
                    enabled = selectedBitmaps.isNotEmpty() && !isConverting,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("convert_images_to_pdf_btn")
                ) {
                    if (isConverting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Create PDF (${selectedBitmaps.size} Pages)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
