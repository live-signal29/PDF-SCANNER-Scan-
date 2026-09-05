package com.example.ui.home

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ads.BannerAdView
import com.example.data.local.DocumentEntity
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.DocumentItemCard
import com.example.ui.components.EmptyStateView
import com.example.ui.components.RenameDialog
import java.io.File

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToScan: () -> Unit,
    onNavigateToImageToPdf: () -> Unit,
    onNavigateToCompress: () -> Unit,
    onNavigateToMerge: () -> Unit,
    onNavigateToSplit: () -> Unit,
    onNavigateToPdfToImage: () -> Unit,
    onNavigateToOcr: () -> Unit,
    onNavigateToSign: () -> Unit,
    onNavigateToPro: () -> Unit,
    onNavigateToDocuments: () -> Unit,
    onOpenDocument: (DocumentEntity) -> Unit
) {
    val context = LocalContext.current
    val recentDocs by viewModel.recentDocuments.collectAsStateWithLifecycle()
    val isProUser by viewModel.isProUser.collectAsStateWithLifecycle()

    var docToRename by remember { mutableStateOf<DocumentEntity?>(null) }
    var docToDelete by remember { mutableStateOf<DocumentEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Header
            item {
                HomeHeader(
                    isProUser = isProUser,
                    onProClick = onNavigateToPro
                )
            }

            // Main Primary Action: Scan Document
            item {
                MainScanBanner(
                    onScanClick = onNavigateToScan
                )
            }

            // Tools Quick Grid
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Quick Tools",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )

                QuickToolsGrid(
                    onScan = onNavigateToScan,
                    onImageToPdf = onNavigateToImageToPdf,
                    onCompress = onNavigateToCompress,
                    onMerge = onNavigateToMerge,
                    onSplit = onNavigateToSplit,
                    onPdfToImage = onNavigateToPdfToImage,
                    onOcr = onNavigateToOcr,
                    onSign = onNavigateToSign
                )
            }

            // Recent Documents Section Header
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Documents",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    if (recentDocs.isNotEmpty()) {
                        TextButton(
                            onClick = onNavigateToDocuments,
                            modifier = Modifier.testTag("view_all_documents_button")
                        ) {
                            Text(
                                "View All",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Recent Documents List or Empty State
            if (recentDocs.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.Description,
                        title = "No documents yet",
                        description = "Scan your first document to see it here.",
                        actionLabel = "Scan Document",
                        onActionClick = onNavigateToScan
                    )
                }
            } else {
                items(recentDocs, key = { it.id }) { doc ->
                    DocumentItemCard(
                        document = doc,
                        onClick = { onOpenDocument(doc) },
                        onRename = { docToRename = doc },
                        onShare = { shareDocument(context, doc) },
                        onDelete = { docToDelete = doc },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // Centralized Banner Ad for free users
        if (!isProUser) {
            BannerAdView(
                isProUser = isProUser,
                modifier = Modifier.fillMaxWidth()
            )
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

@Composable
private fun HomeHeader(
    isProUser: Boolean,
    onProClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.app_scanner_icon),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "PDF Scanner",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Scan, create & manage documents",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Pro Badge / Button
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable(onClick = onProClick)
                .testTag("pro_badge_button"),
            color = if (isProUser) Color(0xFF10B981) else Color(0xFFF59E0B),
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (isProUser) "PRO ACTIVE" else "GO PRO",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun MainScanBanner(
    onScanClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clickable(onClick = onScanClick)
            .testTag("main_scan_document_button"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF1E56D2), Color(0xFF2563EB), Color(0xFF0284C7))
                    )
                )
                .padding(22.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Scan Document",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Camera scan with edge detection & auto filters",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Scan",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickToolsGrid(
    onScan: () -> Unit,
    onImageToPdf: () -> Unit,
    onCompress: () -> Unit,
    onMerge: () -> Unit,
    onSplit: () -> Unit,
    onPdfToImage: () -> Unit,
    onOcr: () -> Unit,
    onSign: () -> Unit
) {
    val tools = listOf(
        ToolItem("Scan", Icons.Default.CameraAlt, Color(0xFF1E56D2), onScan, "tool_scan"),
        ToolItem("Images to PDF", Icons.Default.PhotoLibrary, Color(0xFF0284C7), onImageToPdf, "tool_image_to_pdf"),
        ToolItem("Compress PDF", Icons.Default.Compress, Color(0xFF0D9488), onCompress, "tool_compress"),
        ToolItem("Merge PDF", Icons.Default.MergeType, Color(0xFF8B5CF6), onMerge, "tool_merge"),
        ToolItem("Split PDF", Icons.Default.ContentCut, Color(0xFFEC4899), onSplit, "tool_split"),
        ToolItem("PDF to JPG", Icons.Default.Transform, Color(0xFFF97316), onPdfToImage, "tool_pdf_to_image"),
        ToolItem("OCR Text", Icons.Default.TextFields, Color(0xFF10B981), onOcr, "tool_ocr"),
        ToolItem("Sign PDF", Icons.Default.Draw, Color(0xFF6366F1), onSign, "tool_sign")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 4 items per row (2 rows)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            tools.take(4).forEach { item ->
                ToolGridCard(item, modifier = Modifier.weight(1f))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            tools.drop(4).take(4).forEach { item ->
                ToolGridCard(item, modifier = Modifier.weight(1f))
            }
        }
    }
}

data class ToolItem(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit,
    val testTag: String
)

@Composable
private fun ToolGridCard(
    tool: ToolItem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = tool.onClick)
            .testTag(tool.testTag),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(tool.color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = tool.title,
                    tint = tool.color,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = tool.title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

fun shareDocument(context: Context, document: DocumentEntity) {
    try {
        val file = File(document.filePath)
        if (!file.exists()) return

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (document.fileName.endsWith(".txt")) "text/plain" else "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Document"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
