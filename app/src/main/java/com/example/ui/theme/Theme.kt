package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = BrandBlueDarkPrimary,
    onPrimary = BrandBlueDarkOnPrimary,
    primaryContainer = BrandBlueDarkPrimaryContainer,
    onPrimaryContainer = BrandBlueDarkOnPrimaryContainer,
    secondary = BrandDarkSecondary,
    onSecondary = Color(0xFF00354E),
    secondaryContainer = BrandDarkSecondaryContainer,
    onSecondaryContainer = Color(0xFFCBE6FF),
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = BrandBluePrimary,
    onPrimary = BrandBlueOnPrimary,
    primaryContainer = BrandBluePrimaryContainer,
    onPrimaryContainer = BrandBlueOnPrimaryContainer,
    secondary = BrandSecondary,
    onSecondary = Color.White,
    secondaryContainer = BrandSecondaryContainer,
    onSecondaryContainer = Color(0xFF001E2F),
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Use our brand colors consistently
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

