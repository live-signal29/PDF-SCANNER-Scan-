package com.example.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

data class OnboardingSlide(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color
)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val slides = listOf(
        OnboardingSlide(
            title = "Scan Documents Easily",
            description = "Capture high-clarity document scans with edge detection, perspective correction, and automatic enhancement filters.",
            icon = Icons.Default.DocumentScanner,
            accentColor = Color(0xFF1E56D2)
        ),
        OnboardingSlide(
            title = "Create & Compress PDFs",
            description = "Convert photos to PDF, merge, split, and shrink large PDFs without sacrificing visual clarity.",
            icon = Icons.Default.Compress,
            accentColor = Color(0xFF0284C7)
        ),
        OnboardingSlide(
            title = "Extract Text with OCR",
            description = "Accurately recognize text from your camera or documents in multiple languages and export to TXT.",
            icon = Icons.Default.TextFields,
            accentColor = Color(0xFF0D9488)
        ),
        OnboardingSlide(
            title = "Your Documents Stay Private",
            description = "All document processing happens locally on your device. Zero cloud uploads without your explicit intent.",
            icon = Icons.Default.Security,
            accentColor = Color(0xFF10B981)
        )
    )

    val pagerState = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Skip Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            if (pagerState.currentPage < slides.size - 1) {
                TextButton(
                    onClick = onFinish,
                    modifier = Modifier.testTag("onboarding_skip_button")
                ) {
                    Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Spacer(modifier = Modifier.height(48.dp))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            val slide = slides[page]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(slide.accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = slide.icon,
                        contentDescription = null,
                        tint = slide.accentColor,
                        modifier = Modifier.size(72.dp)
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                Text(
                    text = slide.title,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = slide.description,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )
            }
        }

        // Dot indicators
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(slides.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .height(8.dp)
                        .size(if (isSelected) 24.dp else 8.dp, 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Action Button
        val isLastPage = pagerState.currentPage == slides.size - 1
        Button(
            onClick = {
                if (isLastPage) {
                    onFinish()
                } else {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("onboarding_action_button"),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = if (isLastPage) "Get Started" else "Next",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
