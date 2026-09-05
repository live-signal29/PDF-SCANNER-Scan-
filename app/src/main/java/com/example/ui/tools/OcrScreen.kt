package com.example.ui.tools

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.local.AppDatabase
import com.example.data.local.DocumentCategory
import com.example.data.local.DocumentRepository
import com.example.data.preferences.AppPreferences
import com.example.ocr.OcrEngine
import com.example.ui.components.AppTopBar
import com.example.ui.scanner.loadBitmapFromUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OcrScreen(
    onSaved: (Long) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember { AppPreferences(context) }

    var selectedLanguage by remember { mutableStateOf(preferences.ocrLanguage.value) }
    var extractedText by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var savedDocId by remember { mutableStateOf<Long?>(null) }

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val bmp = loadBitmapFromUri(context, uri)
            if (bmp != null) {
                isProcessing = true
                errorMessage = null
                scope.launch {
                    val result = OcrEngine.extractTextFromBitmap(bmp)
                    if (result.isSuccess) {
                        extractedText = result.getOrNull().orEmpty()
                    } else {
                        errorMessage = result.exceptionOrNull()?.message ?: "Text could not be detected. Try a clearer image."
                    }
                    isProcessing = false
                }
            }
        }
    }

    // PDF Picker Launcher
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isProcessing = true
            errorMessage = null
            scope.launch(Dispatchers.IO) {
                try {
                    val stream = context.contentResolver.openInputStream(uri)
                    val temp = File(context.cacheDir, "ocr_pdf_${System.currentTimeMillis()}.pdf")
                    FileOutputStream(temp).use { fos -> stream?.copyTo(fos) }
                    val result = OcrEngine.extractTextFromPdf(temp)
                    if (result.isSuccess) {
                        extractedText = result.getOrNull().orEmpty()
                    } else {
                        errorMessage = result.exceptionOrNull()?.message ?: "Text could not be detected. Try a clearer image."
                    }
                } catch (e: Exception) {
                    errorMessage = "Text could not be detected. Try a clearer image."
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    Scaffold(
        topBar = { AppTopBar(title = "OCR Text Recognition", onBackClick = onBack) }
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Language selection
                Column {
                    Text(
                        text = "Recognition Language",
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
                        OcrEngine.supportedLanguages.forEach { lang ->
                            FilterChip(
                                selected = selectedLanguage == lang,
                                onClick = {
                                    selectedLanguage = lang
                                    preferences.setOcrLanguage(lang)
                                },
                                label = { Text(lang, fontSize = 12.sp) }
                            )
                        }
                    }
                }

                // Input Source Buttons
                Text(
                    text = "Select Document Source",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(110.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { imagePickerLauncher.launch("image/*") }
                            .testTag("ocr_pick_image_btn"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Pick Image", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(110.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { pdfPickerLauncher.launch("application/pdf") }
                            .testTag("ocr_pick_pdf_btn"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF0D9488), modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Pick PDF", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }
                }

                if (isProcessing) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Analyzing text with OCR engine...", fontSize = 14.sp)
                        }
                    }
                }

                // Error Notice
                errorMessage?.let { err ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(14.dp),
                            fontSize = 14.sp
                        )
                    }
                }

                // Extracted Text Area
                if (extractedText.isNotBlank()) {
                    Text(
                        text = "Extracted Text (Editable)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    OutlinedTextField(
                        value = extractedText,
                        onValueChange = { extractedText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .testTag("ocr_extracted_text_field"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Action Buttons (Copy, Share, Save as TXT)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Extracted Text", extractedText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f).testTag("ocr_copy_btn")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy")
                        }

                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, extractedText)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Extracted Text"))
                            },
                            modifier = Modifier.weight(1f).testTag("ocr_share_btn")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share")
                        }
                    }

                    // Save as TXT Document
                    Button(
                        onClick = {
                            scope.launch {
                                val docDir = File(context.filesDir, "documents").apply { mkdirs() }
                                val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                val txtFile = File(docDir, "OCR_$dateStr.txt")
                                txtFile.writeText(extractedText)

                                val db = AppDatabase.getDatabase(context)
                                val repo = DocumentRepository(db.documentDao(), context)
                                val id = repo.insertDocument(
                                    file = txtFile,
                                    category = DocumentCategory.OCR_TEXT,
                                    pageCount = 1
                                )
                                savedDocId = id
                                Toast.makeText(context, "Saved as TXT document", Toast.LENGTH_SHORT).show()

                                val activity = context as? Activity
                                if (activity != null) {
                                    AdManager.showInterstitial(activity, preferences.isProUser.value) {
                                        onSaved(id)
                                    }
                                } else {
                                    onSaved(id)
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("ocr_save_txt_btn")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save as TXT Document", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
