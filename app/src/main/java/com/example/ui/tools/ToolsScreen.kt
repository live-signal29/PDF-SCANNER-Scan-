package com.example.ui.tools

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ads.BannerAdView

data class FullToolItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit,
    val testTag: String
)

@Composable
fun ToolsScreen(
    onNavigateToScan: () -> Unit,
    onNavigateToImageToPdf: () -> Unit,
    onNavigateToCompress: () -> Unit,
    onNavigateToMerge: () -> Unit,
    onNavigateToSplit: () -> Unit,
    onNavigateToPdfToImage: () -> Unit,
    onNavigateToOcr: () -> Unit,
    onNavigateToSign: () -> Unit,
    onNavigateToPasswordProtect: () -> Unit,
    isProUser: Boolean = false
) {
    val tools = listOf(
        FullToolItem(
            "Camera Scanner",
            "Scan physical documents, receipts, book pages with auto edge framing",
            Icons.Default.CameraAlt,
            Color(0xFF1E56D2),
            onNavigateToScan,
            "tool_card_scan"
        ),
        FullToolItem(
            "Image to PDF",
            "Convert gallery photos, receipts, or screenshots into formatted PDFs",
            Icons.Default.PhotoLibrary,
            Color(0xFF0284C7),
            onNavigateToImageToPdf,
            "tool_card_image_to_pdf"
        ),
        FullToolItem(
            "Compress PDF",
            "Shrink PDF size with custom compression quality while preserving clarity",
            Icons.Default.Compress,
            Color(0xFF0D9488),
            onNavigateToCompress,
            "tool_card_compress"
        ),
        FullToolItem(
            "Merge PDF",
            "Combine multiple separate PDF files into a single organized document",
            Icons.Default.MergeType,
            Color(0xFF8B5CF6),
            onNavigateToMerge,
            "tool_card_merge"
        ),
        FullToolItem(
            "Split PDF",
            "Extract specific pages, page ranges, or split into individual files",
            Icons.Default.ContentCut,
            Color(0xFFEC4899),
            onNavigateToSplit,
            "tool_card_split"
        ),
        FullToolItem(
            "PDF to JPG / PNG",
            "Export PDF pages as high-resolution images to share anywhere",
            Icons.Default.Transform,
            Color(0xFFF97316),
            onNavigateToPdfToImage,
            "tool_card_pdf_to_image"
        ),
        FullToolItem(
            "OCR — Text Recognition",
            "Recognize and extract text from camera, photo, or PDF into editable text",
            Icons.Default.TextFields,
            Color(0xFF10B981),
            onNavigateToOcr,
            "tool_card_ocr"
        ),
        FullToolItem(
            "Sign PDF",
            "Draw finger signature, position, resize, rotate, and stamp onto PDF",
            Icons.Default.Draw,
            Color(0xFF6366F1),
            onNavigateToSign,
            "tool_card_sign"
        ),
        FullToolItem(
            "Password Protect",
            "Secure private documents with military-grade AES-256 encryption",
            Icons.Default.Lock,
            Color(0xFFE11D48),
            onNavigateToPasswordProtect,
            "tool_card_password"
        )
    )

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
                text = "PDF Tools",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Everything you need to work with PDF documents",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(tools.size) { index ->
                val item = tools[index]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = item.onClick)
                        .testTag(item.testTag),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(item.color.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = item.color,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        if (!isProUser) {
            BannerAdView(isProUser = isProUser, modifier = Modifier.fillMaxWidth())
        }
    }
}
