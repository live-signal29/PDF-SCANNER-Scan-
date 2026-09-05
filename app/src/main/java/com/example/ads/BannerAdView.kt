package com.example.ads

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

private const val TAG = "BannerAdView"

/**
 * Lifecycle-aware Banner Ad Composable.
 * Automatically cleans up the AdView when leaving the composition.
 *
 * Privacy Guarantee:
 * No personal documents, images, or scanned data are attached to ad requests.
 */
@Composable
fun BannerAdView(
    modifier: Modifier = Modifier,
    isProUser: Boolean = false
) {
    if (isProUser) return

    val context = LocalContext.current
    var isAdLoaded by remember { mutableStateOf(false) }

    val adView = remember {
        AdView(context).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = AdConfig.bannerAdUnitId
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    super.onAdLoaded()
                    isAdLoaded = true
                    Log.d(TAG, "Banner loaded successfully: ${AdConfig.bannerAdUnitId} (TestMode=${AdConfig.USE_TEST_ADS})")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    super.onAdFailedToLoad(error)
                    isAdLoaded = false
                    Log.w(TAG, "Banner failed to load: ${error.message} (code ${error.code})")
                }
            }
            loadAd(AdRequest.Builder().build())
        }
    }

    DisposableEffect(adView) {
        onDispose {
            try {
                adView.destroy()
            } catch (e: Exception) {
                Log.e(TAG, "Error destroying AdView: ${e.message}")
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { adView }
        )
    }
}
