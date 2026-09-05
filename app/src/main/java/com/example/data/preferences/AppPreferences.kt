package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class DefaultPdfQuality(val title: String, val qualityPercent: Int) {
    HIGH("High Quality (Best for printing)", 90),
    MEDIUM("Standard Quality (Recommended)", 70),
    LOW("Compact Quality (Smallest size)", 45)
}

enum class DefaultPageSize(val title: String, val widthPt: Int, val heightPt: Int) {
    A4("A4 (595 x 842 pt)", 595, 842),
    LETTER("US Letter (612 x 792 pt)", 612, 792),
    ORIGINAL("Original Image Size", 0, 0)
}

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("pdf_scanner_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(getThemeModeInternal())
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _isOnboardingCompleted = MutableStateFlow(prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false))
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

    private val _isProUser = MutableStateFlow(prefs.getBoolean(KEY_IS_PRO_USER, false))
    val isProUser: StateFlow<Boolean> = _isProUser.asStateFlow()

    private val _defaultPdfQuality = MutableStateFlow(getDefaultPdfQualityInternal())
    val defaultPdfQuality: StateFlow<DefaultPdfQuality> = _defaultPdfQuality.asStateFlow()

    private val _defaultPageSize = MutableStateFlow(getDefaultPageSizeInternal())
    val defaultPageSize: StateFlow<DefaultPageSize> = _defaultPageSize.asStateFlow()

    private val _ocrLanguage = MutableStateFlow(prefs.getString(KEY_OCR_LANGUAGE, "English") ?: "English")
    val ocrLanguage: StateFlow<String> = _ocrLanguage.asStateFlow()

    private fun getThemeModeInternal(): AppThemeMode {
        val name = prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name
        return try {
            AppThemeMode.valueOf(name)
        } catch (e: Exception) {
            AppThemeMode.SYSTEM
        }
    }

    private fun getDefaultPdfQualityInternal(): DefaultPdfQuality {
        val name = prefs.getString(KEY_PDF_QUALITY, DefaultPdfQuality.HIGH.name) ?: DefaultPdfQuality.HIGH.name
        return try {
            DefaultPdfQuality.valueOf(name)
        } catch (e: Exception) {
            DefaultPdfQuality.HIGH
        }
    }

    private fun getDefaultPageSizeInternal(): DefaultPageSize {
        val name = prefs.getString(KEY_PAGE_SIZE, DefaultPageSize.A4.name) ?: DefaultPageSize.A4.name
        return try {
            DefaultPageSize.valueOf(name)
        } catch (e: Exception) {
            DefaultPageSize.A4
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
        _isOnboardingCompleted.value = completed
    }

    fun setProUser(isPro: Boolean) {
        prefs.edit().putBoolean(KEY_IS_PRO_USER, isPro).apply()
        _isProUser.value = isPro
    }

    fun setDefaultPdfQuality(quality: DefaultPdfQuality) {
        prefs.edit().putString(KEY_PDF_QUALITY, quality.name).apply()
        _defaultPdfQuality.value = quality
    }

    fun setDefaultPageSize(size: DefaultPageSize) {
        prefs.edit().putString(KEY_PAGE_SIZE, size.name).apply()
        _defaultPageSize.value = size
    }

    fun setOcrLanguage(language: String) {
        prefs.edit().putString(KEY_OCR_LANGUAGE, language).apply()
        _ocrLanguage.value = language
    }

    companion object {
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_ONBOARDING_COMPLETED = "key_onboarding_completed"
        private const val KEY_IS_PRO_USER = "key_is_pro_user"
        private const val KEY_PDF_QUALITY = "key_pdf_quality"
        private const val KEY_PAGE_SIZE = "key_page_size"
        private const val KEY_OCR_LANGUAGE = "key_ocr_language"
    }
}
