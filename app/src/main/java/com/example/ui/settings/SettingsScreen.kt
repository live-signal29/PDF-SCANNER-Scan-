package com.example.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.preferences.AppPreferences
import com.example.data.preferences.AppThemeMode
import com.example.data.preferences.DefaultPageSize
import com.example.data.preferences.DefaultPdfQuality
import com.example.ocr.OcrEngine

@Composable
fun SettingsScreen(
    preferences: AppPreferences,
    onNavigateToPro: () -> Unit,
    onNavigateToPrivacy: () -> Unit
) {
    val context = LocalContext.current
    val currentTheme by preferences.themeMode.collectAsStateWithLifecycle()
    val isProUser by preferences.isProUser.collectAsStateWithLifecycle()
    val currentQuality by preferences.defaultPdfQuality.collectAsStateWithLifecycle()
    val currentPageSize by preferences.defaultPageSize.collectAsStateWithLifecycle()
    val currentOcrLang by preferences.ocrLanguage.collectAsStateWithLifecycle()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showPageSizeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

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
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Pro Membership Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onNavigateToPro() }
                        .testTag("settings_pro_banner"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isProUser) Color(0xFFECFDF5) else Color(0xFFFEF3C7)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(if (isProUser) Color(0xFF10B981) else Color(0xFFF59E0B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color.White)
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isProUser) "PDF Scanner PRO Active" else "Upgrade to PRO",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (isProUser) Color(0xFF065F46) else Color(0xFF92400E)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isProUser) "All premium features unlocked & no ads" else "Ad-free, batch compression & OCR",
                                fontSize = 12.sp,
                                color = if (isProUser) Color(0xFF047857) else Color(0xFFB45309)
                            )
                        }

                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = if (isProUser) Color(0xFF059669) else Color(0xFFD97706),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // General Section
            item {
                SectionHeader("General Preferences")
            }

            item {
                SettingsItem(
                    icon = Icons.Default.DarkMode,
                    title = "App Appearance",
                    subtitle = currentTheme.name.lowercase().replaceFirstChar { it.uppercase() },
                    onClick = { showThemeDialog = true }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.PictureAsPdf,
                    title = "Default PDF Quality",
                    subtitle = currentQuality.title.substringBefore(" ("),
                    onClick = { showQualityDialog = true }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.ViewAgenda,
                    title = "Default Page Size",
                    subtitle = currentPageSize.title.substringBefore(" ("),
                    onClick = { showPageSizeDialog = true }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Language,
                    title = "Default OCR Language",
                    subtitle = currentOcrLang,
                    onClick = { showLanguageDialog = true }
                )
            }

            // Privacy & System
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader("Security & Storage")
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Security,
                    title = "Privacy & Offline Guarantee",
                    subtitle = "100% on-device processing. No data collection.",
                    onClick = onNavigateToPrivacy
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.CleaningServices,
                    title = "Clear Temporary Cache",
                    subtitle = "Free up device storage",
                    onClick = {
                        try {
                            context.cacheDir.deleteRecursively()
                            context.cacheDir.mkdirs()
                            Toast.makeText(context, "Temporary cache cleared", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                )
            }

            // About Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader("About & Feedback")
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Share,
                    title = "Share PDF Scanner",
                    subtitle = "Recommend this app to friends and colleagues",
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "PDF Scanner – Scan, Compress & OCR")
                            putExtra(Intent.EXTRA_TEXT, "Check out PDF Scanner – Fast, lightweight, and private PDF utility: https://play.google.com/store/apps/details?id=${context.packageName}")
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share App"))
                    }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Policy,
                    title = "App Version",
                    subtitle = "v1.0.0 (Production)",
                    onClick = {}
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Theme Selection Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("App Appearance", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    AppThemeMode.values().forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    preferences.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = currentTheme == mode, onClick = {
                                preferences.setThemeMode(mode)
                                showThemeDialog = false
                            })
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 15.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Quality Selection Dialog
    if (showQualityDialog) {
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            title = { Text("Default PDF Quality", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    DefaultPdfQuality.values().forEach { q ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    preferences.setDefaultPdfQuality(q)
                                    showQualityDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = currentQuality == q, onClick = {
                                preferences.setDefaultPdfQuality(q)
                                showQualityDialog = false
                            })
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(q.title, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showQualityDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Page Size Dialog
    if (showPageSizeDialog) {
        AlertDialog(
            onDismissRequest = { showPageSizeDialog = false },
            title = { Text("Default Page Size", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    DefaultPageSize.values().forEach { size ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    preferences.setDefaultPageSize(size)
                                    showPageSizeDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = currentPageSize == size, onClick = {
                                preferences.setDefaultPageSize(size)
                                showPageSizeDialog = false
                            })
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(size.title, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPageSizeDialog = false }) { Text("Cancel") }
            }
        )
    }

    // OCR Language Dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Default OCR Language", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OcrEngine.supportedLanguages.forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    preferences.setOcrLanguage(lang)
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = currentOcrLang == lang, onClick = {
                                preferences.setOcrLanguage(lang)
                                showLanguageDialog = false
                            })
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(lang, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
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
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
