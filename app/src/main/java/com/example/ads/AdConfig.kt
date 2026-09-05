package com.example.ads

/**
 * Centralized AdMob Configuration
 *
 * Switch [USE_TEST_ADS] between `true` (Testing mode with Google sample ads)
 * and `false` (Production mode with your real AdMob Ad Units).
 */
object AdConfig {
    /**
     * TOGGLE TEST / PRODUCTION MODE HERE:
     * - Set to `true` during development and QA testing to load Google's safe test ads and avoid policy violations.
     * - Set to `false` for Google Play Store release to earn revenue with your real AdMob IDs.
     */
    const val USE_TEST_ADS: Boolean = false

    // REAL ADMOB IDs
    const val REAL_APP_ID = "ca-app-pub-1895906484640218~4336412356"
    const val REAL_BANNER_ID = "ca-app-pub-1895906484640218/5894860434"
    const val REAL_INTERSTITIAL_ID = "ca-app-pub-1895906484640218/2318577500"
    const val REAL_REWARDED_ID = "ca-app-pub-1895906484640218/3761697286"

    // OFFICIAL GOOGLE TEST AD UNIT IDs
    const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"

    // Frequency capping: minimum milliseconds required between showing interstitial ads
    const val MIN_INTERSTITIAL_INTERVAL_MS = 45_000L // 45 seconds

    val bannerAdUnitId: String
        get() = if (USE_TEST_ADS) TEST_BANNER_ID else REAL_BANNER_ID

    val interstitialAdUnitId: String
        get() = if (USE_TEST_ADS) TEST_INTERSTITIAL_ID else REAL_INTERSTITIAL_ID

    val rewardedAdUnitId: String
        get() = if (USE_TEST_ADS) TEST_REWARDED_ID else REAL_REWARDED_ID
}
