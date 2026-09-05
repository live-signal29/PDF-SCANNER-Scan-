package com.example.ui.documents

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ads.BannerAdView
import com.example.data.local.DocumentCategory
import com.example.data.local.DocumentEntity
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.DocumentItemCard
import com.example.ui.components.EmptyStateView
import com.example.ui.components.RenameDialog
import com.example.ui.home.shareDocument

@Composable
fun DocumentsScreen(
    viewModel: DocumentsViewModel,
    onOpenDocument: (DocumentEntity) -> Unit,
    onNavigateToScan: () -> Unit
) {
    val context = LocalContext.current
    val documents by viewModel.documents.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isProUser by viewModel.isProUser.collectAsStateWithLifecycle()

    var docToRename by remember { mutableStateOf<DocumentEntity?>(null) }
    var docToDelete by remember { mutableStateOf<DocumentEntity?>(null) }

    val filteredDocuments = remember(documents, searchQuery) {
        if (searchQuery.isBlank()) {
            documents
        } else {
            documents.filter { it.fileName.contains(searchQuery, ignoreCase = true) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "My Documents",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search by document name...", fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("document_search_field"),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                singleLine = true
            )
        }

        // Category Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val categories = listOf(
                DocumentCategory.ALL,
                DocumentCategory.SCANNED,
                DocumentCategory.IMAGE_TO_PDF,
                DocumentCategory.COMPRESSED,
                DocumentCategory.SIGNED,
                DocumentCategory.OCR_TEXT
            )

            categories.forEach { cat ->
                val isSelected = selectedCategory == cat
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setCategory(cat) },
                    label = { Text(cat.displayName, fontSize = 13.sp) },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("filter_chip_${cat.name}")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Document list or empty state
        if (filteredDocuments.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                EmptyStateView(
                    icon = Icons.Default.FolderOpen,
                    title = if (searchQuery.isNotEmpty()) "No matching documents" else "Folder is empty",
                    description = if (searchQuery.isNotEmpty()) "Try searching with a different filename" else "Create or scan documents to see them organized here",
                    actionLabel = if (searchQuery.isEmpty()) "Scan Document" else null,
                    onActionClick = if (searchQuery.isEmpty()) onNavigateToScan else null
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredDocuments, key = { it.id }) { doc ->
                    DocumentItemCard(
                        document = doc,
                        onClick = { onOpenDocument(doc) },
                        onRename = { docToRename = doc },
                        onShare = { shareDocument(context, doc) },
                        onDelete = { docToDelete = doc }
                    )
                }
            }
        }

        if (!isProUser) {
            BannerAdView(isProUser = isProUser, modifier = Modifier.fillMaxWidth())
        }
    }

    // Rename Dialog
    docToRename?.let { doc ->
        RenameDialog(
            currentName = doc.fileName,
            onDismiss = { docToRename = null },
            onConfirm = { newName ->
                viewModel.renameDocument(doc, newName)
                docToRename = null
            }
        )
    }

    // Confirm Delete Dialog
    docToDelete?.let { doc ->
        ConfirmDeleteDialog(
            fileName = doc.fileName,
            onDismiss = { docToDelete = null },
            onConfirm = {
                viewModel.deleteDocument(doc)
                docToDelete = null
            }
        )
    }
}
