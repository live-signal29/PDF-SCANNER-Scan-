package com.example.ui.tools

import android.app.Activity
import android.net.Uri
import android.text.format.Formatter
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MergePdfScreen(
    onSaved: (Long) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember { AppPreferences(context) }

    val selectedFiles = remember { mutableStateListOf<File>() }
    var mergedFileName by remember {
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        mutableStateOf("Merged_$dateStr.pdf")
    }
    var isMerging by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            scope.launch(Dispatchers.IO) {
                try {
                    val stream = context.contentResolver.openInputStream(uri)
                    val temp = File(context.cacheDir, "merge_input_${System.currentTimeMillis()}_${selectedFiles.size}.pdf")
                    FileOutputStream(temp).use { fos -> stream?.copyTo(fos) }
                    selectedFiles.add(temp)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    Scaffold(
        topBar = { AppTopBar(title = "Merge PDFs", onBackClick = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .navigationBarsPadding()
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Combine Multiple PDFs",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Select 2 or more documents to merge into a single PDF. Reorder pages with the arrows.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { filePickerLauncher.launch("application/pdf") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("add_pdfs_to_merge_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add PDF Files (${selectedFiles.size} Selected)")
                    }
                }

                if (selectedFiles.isNotEmpty()) {
                    item {
                        OutlinedTextField(
                            value = mergedFileName,
                            onValueChange = { mergedFileName = it },
                            label = { Text("Merged File Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    itemsIndexed(selectedFiles) { index, file ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("${index + 1}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(file.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
                                    Text(
                                        Formatter.formatFileSize(context, file.length()),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Move up
                                if (index > 0) {
                                    IconButton(onClick = {
                                        val item = selectedFiles.removeAt(index)
                                        selectedFiles.add(index - 1, item)
                                    }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move up", modifier = Modifier.size(18.dp))
                                    }
                                }

                                // Move down
                                if (index < selectedFiles.size - 1) {
                                    IconButton(onClick = {
                                        val item = selectedFiles.removeAt(index)
                                        selectedFiles.add(index + 1, item)
                                    }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move down", modifier = Modifier.size(18.dp))
                                    }
                                }

                                // Remove
                                IconButton(onClick = { selectedFiles.removeAt(index) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Merge Button
            if (selectedFiles.size >= 2) {
                Box(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    Button(
                        onClick = {
                            if (isMerging) return@Button
                            isMerging = true

                            scope.launch {
                                val docDir = File(context.filesDir, "documents").apply { mkdirs() }
                                val cleanName = if (mergedFileName.endsWith(".pdf")) mergedFileName else "$mergedFileName.pdf"
                                val outFile = File(docDir, cleanName)

                                val result = PdfEngine.mergePdfs(
                                    sourceFiles = selectedFiles.toList(),
                                    outputFile = outFile
                                )

                                if (result.isSuccess) {
                                    val db = AppDatabase.getDatabase(context)
                                    val repo = DocumentRepository(db.documentDao(), context)
                                    val totalPages = PdfEngine.getPageCount(outFile)
                                    val docId = repo.insertDocument(
                                        file = outFile,
                                        category = DocumentCategory.MERGED,
                                        pageCount = totalPages
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
                                isMerging = false
                            }
                        },
                        enabled = !isMerging,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("merge_pdfs_action_btn")
                    ) {
                        if (isMerging) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Default.MergeType, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Merge ${selectedFiles.size} PDFs", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
