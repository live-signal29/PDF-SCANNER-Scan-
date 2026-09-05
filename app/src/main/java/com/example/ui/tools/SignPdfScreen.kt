package com.example.ui.tools

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
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
import com.example.pdf.PdfEngine
import com.example.ui.components.AppTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SignPdfScreen(
    onSaved: (Long) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember { AppPreferences(context) }

    var selectedFile by remember { mutableStateOf<File?>(null) }
    var totalPages by remember { mutableIntStateOf(0) }
    var targetPage by remember { mutableIntStateOf(0) }
    var pagePreviewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Signature path state
    val signaturePaths = remember { mutableStateListOf<androidx.compose.ui.graphics.Path>() }
    var currentPath by remember { mutableStateOf<androidx.compose.ui.graphics.Path?>(null) }
    var signatureBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Signature stamp positioning
    var sigX by remember { mutableFloatStateOf(0.5f) } // 0..1
    var sigY by remember { mutableFloatStateOf(0.85f) } // 0..1
    var sigScale by remember { mutableFloatStateOf(0.5f) }
    var sigRotation by remember { mutableFloatStateOf(0f) }

    var isSigning by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val stream = context.contentResolver.openInputStream(uri)
                    val temp = File(context.cacheDir, "sign_input_${System.currentTimeMillis()}.pdf")
                    FileOutputStream(temp).use { fos -> stream?.copyTo(fos) }
                    val count = PdfEngine.getPageCount(temp)
                    selectedFile = temp
                    totalPages = count
                    targetPage = 0
                    val bmp = PdfEngine.renderPageToBitmap(temp, 0, 720)
                    pagePreviewBitmap = bmp
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    LaunchedEffect(targetPage, selectedFile) {
        val file = selectedFile ?: return@LaunchedEffect
        val bmp = withContext(Dispatchers.IO) {
            PdfEngine.renderPageToBitmap(file, targetPage, 720)
        }
        pagePreviewBitmap = bmp
    }

    Scaffold(
        topBar = { AppTopBar(title = "Sign PDF", onBackClick = onBack) }
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
                // PDF Selector
                if (selectedFile == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .clickable { filePickerLauncher.launch("application/pdf") }
                            .testTag("pick_pdf_to_sign"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.FileOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(44.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Select PDF to Sign", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(selectedFile!!.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
                                Text("Page ${targetPage + 1} of $totalPages", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            if (totalPages > 1) {
                                IconButton(
                                    onClick = { if (targetPage > 0) targetPage-- },
                                    enabled = targetPage > 0
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev page")
                                }
                                IconButton(
                                    onClick = { if (targetPage < totalPages - 1) targetPage++ },
                                    enabled = targetPage < totalPages - 1
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next page")
                                }
                            }
                        }
                    }

                    // Signature Drawing Canvas Pad
                    Text(
                        text = "1. Draw Signature Below",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .pointerInput(Unit) {
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                val p = androidx.compose.ui.graphics.Path().apply {
                                                    moveTo(offset.x, offset.y)
                                                }
                                                currentPath = p
                                            },
                                            onDrag = { change, _ ->
                                                currentPath?.lineTo(change.position.x, change.position.y)
                                            },
                                            onDragEnd = {
                                                currentPath?.let { signaturePaths.add(it) }
                                                currentPath = null
                                            }
                                        )
                                    }
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    signaturePaths.forEach { path ->
                                        drawPath(
                                            path = path,
                                            color = Color.Black,
                                            style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                        )
                                    }
                                    currentPath?.let { path ->
                                        drawPath(
                                            path = path,
                                            color = Color.Black,
                                            style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                        )
                                    }
                                }

                                if (signaturePaths.isEmpty() && currentPath == null) {
                                    Text(
                                        text = "Sign here with your finger",
                                        color = Color.LightGray,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            }

                            // Clear button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF8FAFC))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = {
                                    signaturePaths.clear()
                                    currentPath = null
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Clear Signature")
                                }

                                Button(
                                    onClick = {
                                        // Render drawn vector paths to bitmap
                                        val bmp = Bitmap.createBitmap(500, 200, Bitmap.Config.ARGB_8888)
                                        val canvas = Canvas(bmp)
                                        val paint = Paint().apply {
                                            color = android.graphics.Color.BLACK
                                            strokeWidth = 10f
                                            style = Paint.Style.STROKE
                                            strokeCap = Paint.Cap.ROUND
                                            strokeJoin = Paint.Join.ROUND
                                            isAntiAlias = true
                                        }
                                        signaturePaths.forEach { p ->
                                            canvas.drawPath(p.asAndroidPath(), paint)
                                        }
                                        signatureBitmap = bmp
                                    },
                                    enabled = signaturePaths.isNotEmpty(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Apply Signature")
                                }
                            }
                        }
                    }

                    // 2. Position & Preview on Page
                    if (signatureBitmap != null && pagePreviewBitmap != null) {
                        Text(
                            text = "2. Position & Adjust on Page",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        // Preview page with signature overlay
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.DarkGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = pagePreviewBitmap!!.asImageBitmap(),
                                contentDescription = "Page Preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )

                            // Overlay stamp indicator
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(
                                        start = (sigX * 200).dp,
                                        top = (sigY * 200).dp
                                    )
                                    .size((120 * sigScale).dp, (60 * sigScale).dp)
                                    .background(Color.Blue.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .border(1.dp, Color.Blue, RoundedCornerShape(4.dp))
                            ) {
                                Image(
                                    bitmap = signatureBitmap!!.asImageBitmap(),
                                    contentDescription = "Signature Overlay",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }

                        // Adjustment Sliders
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Vertical Position", fontSize = 12.sp, modifier = Modifier.width(110.dp))
                                Slider(value = sigY, onValueChange = { sigY = it }, valueRange = 0.1f..0.95f, modifier = Modifier.weight(1f))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Horizontal Position", fontSize = 12.sp, modifier = Modifier.width(110.dp))
                                Slider(value = sigX, onValueChange = { sigX = it }, valueRange = 0.1f..0.9f, modifier = Modifier.weight(1f))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Signature Size", fontSize = 12.sp, modifier = Modifier.width(110.dp))
                                Slider(value = sigScale, onValueChange = { sigScale = it }, valueRange = 0.3f..1.2f, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Bottom Stamp Button
            if (selectedFile != null && signatureBitmap != null) {
                Box(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    Button(
                        onClick = {
                            val src = selectedFile ?: return@Button
                            val sigBmp = signatureBitmap ?: return@Button
                            isSigning = true

                            scope.launch {
                                val docDir = File(context.filesDir, "documents").apply { mkdirs() }
                                val outName = "Signed_${System.currentTimeMillis()}.pdf"
                                val outFile = File(docDir, outName)

                                val result = PdfEngine.signPdf(
                                    sourceFile = src,
                                    outputFile = outFile,
                                    targetPageIndex = targetPage,
                                    signatureBitmap = sigBmp,
                                    normX = sigX,
                                    normY = sigY,
                                    scaleMultiplier = sigScale,
                                    rotationDegrees = sigRotation
                                )

                                if (result.isSuccess) {
                                    val db = AppDatabase.getDatabase(context)
                                    val repo = DocumentRepository(db.documentDao(), context)
                                    val pageCount = PdfEngine.getPageCount(outFile)
                                    val docId = repo.insertDocument(
                                        file = outFile,
                                        category = DocumentCategory.SIGNED,
                                        pageCount = pageCount
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
                                isSigning = false
                            }
                        },
                        enabled = !isSigning,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("stamp_sign_pdf_btn")
                    ) {
                        if (isSigning) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Default.Draw, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Stamp Signature & Save PDF", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
