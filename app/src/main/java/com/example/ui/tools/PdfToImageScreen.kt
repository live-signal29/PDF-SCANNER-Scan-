package com.example.ui.tools

import android.app.Activity
import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ads.AdManager
import com.example.data.preferences.AppPreferences
import com.example.pdf.PdfEngine
import com.example.ui.components.AppTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

@Composable
fun PdfToImageScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember { AppPreferences(context) }

    var selectedFile by remember { mutableStateOf<File?>(null) }
    var totalPages by remember { mutableIntStateOf(0) }
    var format by remember { mutableStateOf(Bitmap.CompressFormat.JPEG) }
    var quality by remember { mutableFloatStateOf(90f) }
    var isConverting by remember { mutableStateOf(false) }
    var conversionProgress by remember { mutableFloatStateOf(0f) }
    var exportedCount by remember { mutableStateOf<Int?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val stream = context.contentResolver.openInputStream(uri)
                    val temp = File(context.cacheDir, "pdf_to_img_${System.currentTimeMillis()}.pdf")
                    FileOutputStream(temp).use { fos -> stream?.copyTo(fos) }
                    val count = PdfEngine.getPageCount(temp)
                    selectedFile = temp
                    totalPages = count
                    exportedCount = null
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    Scaffold(
        topBar = { AppTopBar(title = "PDF to JPG / PNG", onBackClick = onBack) }
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
                if (selectedFile == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .clickable { filePickerLauncher.launch("application/pdf") }
                            .testTag("pick_pdf_to_image"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.FileOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(44.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Select PDF to Convert", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Extract all pages as high resolution photos", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(selectedFile!!.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1)
                                Text("Total Pages: $totalPages", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = { filePickerLauncher.launch("application/pdf") }) {
                                Text("Change")
                            }
                        }
                    }

                    // Format Selection (JPG vs PNG)
                    Text(
                        text = "Image Output Format",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(
                            selected = format == Bitmap.CompressFormat.JPEG,
                            onClick = { format = Bitmap.CompressFormat.JPEG },
                            label = { Text("JPG (Recommended)", fontSize = 13.sp) }
                        )
                        FilterChip(
                            selected = format == Bitmap.CompressFormat.PNG,
                            onClick = { format = Bitmap.CompressFormat.PNG },
                            label = { Text("PNG (Lossless)", fontSize = 13.sp) }
                        )
                    }

                    // Quality slider (for JPG)
                    if (format == Bitmap.CompressFormat.JPEG) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Image Quality", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("${quality.toInt()}%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(
                                value = quality,
                                onValueChange = { quality = it },
                                valueRange = 50f..100f
                            )
                        }
                    }

                    // Result card
                    exportedCount?.let { count ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(44.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Successfully Exported!", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF065F46))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("$count images saved to Pictures folder", fontSize = 14.sp, color = Color(0xFF047857))
                            }
                        }
                    }
                }
            }

            // Bottom Convert Button
            if (selectedFile != null) {
                Box(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    Button(
                        onClick = {
                            val src = selectedFile ?: return@Button
                            isConverting = true

                            scope.launch {
                                val tempOutDir = File(context.cacheDir, "exported_imgs_${System.currentTimeMillis()}").apply { mkdirs() }
                                val result = PdfEngine.pdfToImages(
                                    sourceFile = src,
                                    outputDir = tempOutDir,
                                    format = format,
                                    quality = quality.toInt(),
                                    onProgress = { conversionProgress = it }
                                )

                                if (result.isSuccess) {
                                    val files = result.getOrNull().orEmpty()
                                    // Copy files to MediaStore Pictures/PDF_Scanner
                                    withContext(Dispatchers.IO) {
                                        files.forEach { imgFile ->
                                            saveImageToGallery(context, imgFile, format == Bitmap.CompressFormat.PNG)
                                        }
                                    }
                                    exportedCount = files.size
                                    Toast.makeText(context, "${files.size} pages saved to Pictures", Toast.LENGTH_SHORT).show()

                                    val activity = context as? Activity
                                    if (activity != null) {
                                        AdManager.showInterstitial(activity, preferences.isProUser.value) {}
                                    }
                                }
                                isConverting = false
                            }
                        },
                        enabled = !isConverting,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("convert_pdf_to_images_btn")
                    ) {
                        if (isConverting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Exporting... ${(conversionProgress * 100).toInt()}%")
                        } else {
                            Icon(Icons.Default.Transform, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Convert to Images ($totalPages Pages)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private fun saveImageToGallery(context: android.content.Context, file: File, isPng: Boolean) {
    try {
        val mimeType = if (isPng) "image/png" else "image/jpeg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PDF_Scanner")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (uri != null) {
            context.contentResolver.openOutputStream(uri).use { out ->
                FileInputStream(file).use { input ->
                    if (out != null) input.copyTo(out)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
