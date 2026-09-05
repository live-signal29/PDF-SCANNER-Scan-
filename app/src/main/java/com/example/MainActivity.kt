package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ads.AdManager
import com.example.data.preferences.AppPreferences
import com.example.data.preferences.AppThemeMode
import com.example.ui.navigation.AppNavHost
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Initialize AdMob in background
    AdManager.initialize(this)

    val preferences = AppPreferences(this)

    setContent {
      val themeMode by preferences.themeMode.collectAsStateWithLifecycle()
      val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
      }

      MyApplicationTheme(darkTheme = isDark) {
        Surface(modifier = Modifier.fillMaxSize()) {
          AppNavHost(preferences = preferences)
        }
      }
    }
  }
}

