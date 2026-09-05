package com.example.ui.tools

import android.app.Activity
import android.net.Uri
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import com.example.pdf.PdfEngine
import com.example.ui.components.AppTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun SplitPdfScreen(
    onSaved: (Long) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember { AppPreferences(context) }

    var selectedFile by remember { mutableStateOf<File?>(null) }
    var totalPages by remember { mutableIntStateOf(0) }
    val selectedPages = remember { mutableStateListOf<Int>() }
    var isSplitting by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val stream = context.contentResolver.openInputStream(uri)
                    val temp = File(context.cacheDir, "split_input_${System.currentTimeMillis()}.pdf")
                    FileOutputStream(temp).use { fos -> stream?.copyTo(fos) }
                    val count = PdfEngine.getPageCount(temp)
                    selectedFile = temp
                    totalPages = count
                    selectedPages.clear()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    Scaffold(
        topBar = { AppTopBar(title = "Split PDF", onBackClick = onBack) }
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
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                            .testTag("pick_pdf_to_split"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.FileOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(44.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Select PDF Document", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Choose a PDF to extract pages", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Pages to Extract (${selectedPages.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        TextButton(onClick = {
                            if (selectedPages.size == totalPages) {
                                selectedPages.clear()
                            } else {
                                selectedPages.clear()
                                for (i in 0 until totalPages) selectedPages.add(i)
                            }
                        }) {
                            Text(if (selectedPages.size == totalPages) "Deselect All" else "Select All")
                        }
                    }

                    // Grid of page numbers
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 60.dp),
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(totalPages) { pageIndex ->
                            val isSelected = selectedPages.contains(pageIndex)
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        if (isSelected) selectedPages.remove(pageIndex)
                                        else selectedPages.add(pageIndex)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${pageIndex + 1}",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Split Button
            if (selectedFile != null && selectedPages.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    Button(
                        onClick = {
                            val src = selectedFile ?: return@Button
                            isSplitting = true

                            scope.launch {
                                val docDir = File(context.filesDir, "documents").apply { mkdirs() }
                                val outFiles = selectedPages.map { pageIdx ->
                                    File(docDir, "Split_${src.nameWithoutExtension}_p${pageIdx + 1}.pdf")
                                }

                                val result = PdfEngine.splitPdf(
                                    sourceFile = src,
                                    selectedPages = selectedPages.sorted(),
                                    outputFiles = outFiles
                                )

                                if (result.isSuccess && outFiles.isNotEmpty()) {
                                    val db = AppDatabase.getDatabase(context)
                                    val repo = DocumentRepository(db.documentDao(), context)
                                    var firstId: Long = 0
                                    outFiles.forEachIndexed { i, f ->
                                        val id = repo.insertDocument(f, DocumentCategory.SPLIT, pageCount = 1)
                                        if (i == 0) firstId = id
                                    }

                                    val activity = context as? Activity
                                    if (activity != null) {
                                        AdManager.showInterstitial(activity, preferences.isProUser.value) {
                                            onSaved(firstId)
                                        }
                                    } else {
                                        onSaved(firstId)
                                    }
                                }
                                isSplitting = false
                            }
                        },
                        enabled = !isSplitting,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("split_pdf_action_btn")
                    ) {
                        if (isSplitting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Default.ContentCut, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Extract ${selectedPages.size} Page${if (selectedPages.size > 1) "s" else ""}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
