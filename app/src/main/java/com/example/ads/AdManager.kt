package com.example.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Centralized AdManager handling initialization, UMP user consent,
 * preloading, lifecycle-safe display, and sensible frequency capping.
 *
 * Privacy Guarantee:
 * This app does NOT upload documents, scanned photos, PDFs, or OCR text to any ad networks.
 * All file operations occur 100% locally on the device.
 */
object AdManager {
    private const val TAG = "AdManager"

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    private val isInitializing = AtomicBoolean(false)
    private val isInitialized = AtomicBoolean(false)
    private val isInterstitialLoading = AtomicBoolean(false)
    private val isRewardedLoading = AtomicBoolean(false)

    // Timestamp tracking for frequency capping
    private var lastInterstitialShownTime: Long = 0L

    /**
     * Initialize Mobile Ads and request User Messaging Platform (UMP) consent.
     * This runs safely without blocking the main UI thread.
     */
    fun initialize(activity: Activity) {
        if (isInitialized.get() || isInitializing.getAndSet(true)) return

        try {
            // Request Consent using Google's current User Messaging Platform (UMP)
            val params = ConsentRequestParameters.Builder().build()
            val consentInformation: ConsentInformation = UserMessagingPlatform.getConsentInformation(activity)

            consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                {
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                        if (formError != null) {
                            Log.w(TAG, "Consent form error: ${formError.message}")
                        }
                        // Initialize MobileAds once consent is handled
                        if (consentInformation.canRequestAds()) {
                            startMobileAds(activity.applicationContext)
                        }
                    }
                },
                { requestConsentError ->
                    Log.w(TAG, "Consent info update failed: ${requestConsentError.message}")
                    // Fallback to initializing MobileAds directly so users are not blocked
                    startMobileAds(activity.applicationContext)
                }
            )

            // If already permitted to request ads, initialize right away
            if (consentInformation.canRequestAds()) {
                startMobileAds(activity.applicationContext)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Consent flow exception: ${e.message}", e)
            startMobileAds(activity.applicationContext)
        }
    }

    private fun startMobileAds(appContext: Context) {
        if (isInitialized.get()) return

        Thread {
            try {
                MobileAds.initialize(appContext) { initStatus ->
                    Log.d(TAG, "MobileAds SDK initialized: $initStatus")
                    isInitialized.set(true)
                    isInitializing.set(false)

                    // Preload first interstitial and rewarded ad on main thread
                    Handler(Looper.getMainLooper()).post {
                        preloadInterstitial(appContext)
                        preloadRewarded(appContext)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize MobileAds: ${e.message}")
                isInitializing.set(false)
            }
        }.start()
    }

    /**
     * Preloads an Interstitial Ad using the configured unit ID.
     */
    fun preloadInterstitial(context: Context) {
        if (interstitialAd != null || isInterstitialLoading.get()) return

        val adUnitId = AdConfig.interstitialAdUnitId
        isInterstitialLoading.set(true)

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context.applicationContext,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isInterstitialLoading.set(false)
                    Log.d(TAG, "Interstitial loaded: $adUnitId (TestMode=${AdConfig.USE_TEST_ADS})")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isInterstitialLoading.set(false)
                    Log.w(TAG, "Interstitial failed to load ($adUnitId): ${error.message} (code ${error.code})")
                }
            }
        )
    }

    /**
     * Displays an interstitial ad ONLY after a natural completed workflow
     * (such as PDF creation, compression, merge, or signing completed).
     *
     * Enforces frequency capping:
     * - Never shows if less than [AdConfig.MIN_INTERSTITIAL_INTERVAL_MS] has elapsed.
     * - Free users only (skipped for Pro users).
     * - If the ad is not ready or fails, immediately continues [onAdDismissed] without blocking.
     */
    fun showInterstitial(
        activity: Activity,
        isProUser: Boolean = false,
        onAdDismissed: () -> Unit
    ) {
        if (isProUser) {
            onAdDismissed()
            return
        }

        val currentTime = System.currentTimeMillis()
        val timeSinceLastAd = currentTime - lastInterstitialShownTime

        // Frequency capping check
        if (timeSinceLastAd < AdConfig.MIN_INTERSTITIAL_INTERVAL_MS) {
            Log.d(TAG, "Interstitial skipped due to frequency cap (${timeSinceLastAd / 1000}s since last ad)")
            onAdDismissed()
            return
        }

        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    lastInterstitialShownTime = System.currentTimeMillis()
                    interstitialAd = null
                }

                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    preloadInterstitial(activity.applicationContext)
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    Log.w(TAG, "Interstitial failed to show: ${error.message}")
                    interstitialAd = null
                    preloadInterstitial(activity.applicationContext)
                    onAdDismissed()
                }
            }
            ad.show(activity)
        } else {
            Log.d(TAG, "Interstitial not ready yet, proceeding normally")
            preloadInterstitial(activity.applicationContext)
            onAdDismissed()
        }
    }

    /**
     * Preloads a Rewarded Ad using the configured unit ID.
     */
    fun preloadRewarded(context: Context) {
        if (rewardedAd != null || isRewardedLoading.get()) return

        val adUnitId = AdConfig.rewardedAdUnitId
        isRewardedLoading.set(true)

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context.applicationContext,
            adUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isRewardedLoading.set(false)
                    Log.d(TAG, "Rewarded ad loaded: $adUnitId (TestMode=${AdConfig.USE_TEST_ADS})")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isRewardedLoading.set(false)
                    Log.w(TAG, "Rewarded ad failed to load ($adUnitId): ${error.message} (code ${error.code})")
                }
            }
        )
    }

    /**
     * Displays an optional Rewarded Ad for premium actions.
     * The user MUST explicitly choose to watch this ad.
     *
     * @param onUserEarnedReward Triggered when user completes watching the rewarded ad
     * @param onDismiss Triggered when ad is closed or if unavailable
     */
    fun showRewarded(
        activity: Activity,
        isProUser: Boolean = false,
        onUserEarnedReward: () -> Unit,
        onDismiss: () -> Unit
    ) {
        if (isProUser) {
            onUserEarnedReward()
            onDismiss()
            return
        }

        val ad = rewardedAd
        if (ad != null) {
            var rewardEarned = false
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    preloadRewarded(activity.applicationContext)
                    if (rewardEarned) {
                        onUserEarnedReward()
                    }
                    onDismiss()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    Log.w(TAG, "Rewarded ad failed to show: ${error.message}")
                    rewardedAd = null
                    preloadRewarded(activity.applicationContext)
                    // Grant reward as fallback so user action is not unfairly lost
                    onUserEarnedReward()
                    onDismiss()
                }
            }
            ad.show(activity) { rewardItem ->
                Log.d(TAG, "User earned reward: ${rewardItem.type} amount=${rewardItem.amount}")
                rewardEarned = true
            }
        } else {
            Log.d(TAG, "Rewarded ad not cached yet, granting reward gracefully")
            preloadRewarded(activity.applicationContext)
            onUserEarnedReward()
            onDismiss()
        }
    }

    val isRewardedAdReady: Boolean
        get() = rewardedAd != null
}
