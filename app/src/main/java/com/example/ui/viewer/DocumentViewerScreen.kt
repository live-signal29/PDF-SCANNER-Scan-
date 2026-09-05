package com.example.ui.viewer

import android.graphics.Bitmap
import android.text.format.Formatter
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AppDatabase
import com.example.data.local.DocumentCategory
import com.example.data.local.DocumentEntity
import com.example.data.local.DocumentRepository
import com.example.pdf.PdfEngine
import com.example.pdf.PdfSecurity
import com.example.ui.components.AppTopBar
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.RenameDialog
import com.example.ui.home.shareDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun DocumentViewerScreen(
    docId: Long,
    onBack: () -> Unit,
    onNavigateToPasswordProtect: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var document by remember { mutableStateOf<DocumentEntity?>(null) }
    var pages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var textContent by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isPasswordLocked by remember { mutableStateOf(false) }
    var enteredPassword by remember { mutableStateOf("") }
    var unlockError by remember { mutableStateOf<String?>(null) }

    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(docId) {
        val db = AppDatabase.getDatabase(context)
        val repo = DocumentRepository(db.documentDao(), context)
        val doc = repo.getDocumentById(docId)
        document = doc

        if (doc != null) {
            val file = File(doc.filePath)
            if (file.exists()) {
                if (PdfSecurity.isProtected(file)) {
                    isPasswordLocked = true
                    isLoading = false
                } else if (doc.category == DocumentCategory.OCR_TEXT.name || doc.fileName.endsWith(".txt")) {
                    textContent = withContext(Dispatchers.IO) {
                        try { file.readText() } catch (e: Exception) { "Error reading file" }
                    }
                    isLoading = false
                } else {
                    val rendered = withContext(Dispatchers.IO) {
                        PdfEngine.renderAllPages(file, maxPages = 50, targetWidth = 1080)
                    }
                    pages = rendered
                    isLoading = false
                }
            } else {
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = document?.fileName ?: "Document Viewer",
                onBackClick = onBack,
                actions = {
                    document?.let { doc ->
                        IconButton(onClick = { showRenameDialog = true }, modifier = Modifier.testTag("viewer_rename_btn")) {
                            Icon(Icons.Default.Edit, contentDescription = "Rename")
                        }
                        IconButton(onClick = { shareDocument(context, doc) }, modifier = Modifier.testTag("viewer_share_btn")) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                        IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.testTag("viewer_delete_btn")) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                isPasswordLocked -> {
                    // Password protected prompt
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "This document is password protected",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Enter the password to unlock and view contents",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        OutlinedTextField(
                            value = enteredPassword,
                            onValueChange = {
                                enteredPassword = it
                                unlockError = null
                            },
                            label = { Text("Password") },
                            isError = unlockError != null,
                            supportingText = unlockError?.let { { Text(it) } },
                            modifier = Modifier.fillMaxWidth().testTag("viewer_password_input"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        androidx.compose.material3.Button(
                            onClick = {
                                scope.launch {
                                    val doc = document ?: return@launch
                                    val src = File(doc.filePath)
                                    val tempOut = File(context.cacheDir, "unlocked_${System.currentTimeMillis()}.pdf")
                                    val result = PdfSecurity.unlockPdf(src, tempOut, enteredPassword)
                                    if (result.isSuccess) {
                                        val rendered = withContext(Dispatchers.IO) {
                                            PdfEngine.renderAllPages(tempOut, maxPages = 50, targetWidth = 1080)
                                        }
                                        pages = rendered
                                        isPasswordLocked = false
                                    } else {
                                        unlockError = "Incorrect password"
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("viewer_unlock_btn")
                        ) {
                            Text("Unlock Document")
                        }
                    }
                }

                textContent != null -> {
                    // OCR text document
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Text(
                                text = textContent.orEmpty(),
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 22.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                pages.isNotEmpty() -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        itemsIndexed(pages) { index, pageBitmap ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(4.dp, RoundedCornerShape(8.dp)),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Image(
                                        bitmap = pageBitmap.asImageBitmap(),
                                        contentDescription = "Page ${index + 1}",
                                        modifier = Modifier.fillMaxWidth(),
                                        contentScale = ContentScale.FillWidth
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFF1F5F9))
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Page ${index + 1} of ${pages.size}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                else -> {
                    Text(
                        text = "Unable to load document preview",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Rename Dialog
    if (showRenameDialog && document != null) {
        RenameDialog(
            currentName = document!!.fileName,
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName ->
                scope.launch {
                    val db = AppDatabase.getDatabase(context)
                    val repo = DocumentRepository(db.documentDao(), context)
                    repo.renameDocument(document!!, newName)
                    document = repo.getDocumentById(docId)
                    showRenameDialog = false
                }
            }
        )
    }

    // Delete Dialog
    if (showDeleteDialog && document != null) {
        ConfirmDeleteDialog(
            fileName = document!!.fileName,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                scope.launch {
                    val db = AppDatabase.getDatabase(context)
                    val repo = DocumentRepository(db.documentDao(), context)
                    repo.deleteDocument(document!!)
                    showDeleteDialog = false
                    onBack()
                }
            }
        )
    }
}
