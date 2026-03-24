package `in`.mahato.bytetools.utils

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdManager @Inject constructor() {

    private val interstitialAds = mutableMapOf<String, InterstitialAd?>()
    private var adsShownCount = 0

    fun initialize(context: Context) {
        MobileAds.initialize(context) {}
        // Pre-load common ones if limit not reached
        if (adsShownCount < AdsConfig.MAX_ADS_PER_SESSION) {
            loadInterstitial(context, AdsConfig.PDF_INTERSTITIAL_ID)
            loadInterstitial(context, AdsConfig.IMAGE_INTERSTITIAL_ID)
            loadInterstitial(context, AdsConfig.GPS_INTERSTITIAL_ID)
            loadInterstitial(context, AdsConfig.QR_INTERSTITIAL_ID)
        }
    }

    fun loadInterstitial(context: Context, adUnitId: String) {
        if (!AdsConfig.isAdsEnabled || adsShownCount >= AdsConfig.MAX_ADS_PER_SESSION) return
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(context, adUnitId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                interstitialAds[adUnitId] = ad
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                interstitialAds[adUnitId] = null
            }
        })
    }

    fun showInterstitial(activity: Activity, adUnitId: String, onAdDismissed: () -> Unit) {
        val ad = interstitialAds[adUnitId]
        if (AdsConfig.isAdsEnabled && adsShownCount < AdsConfig.MAX_ADS_PER_SESSION && ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAds[adUnitId] = null
                    adsShownCount++
                    onAdDismissed()
                    // Don't try to load another if we already hit limit
                    if (adsShownCount < AdsConfig.MAX_ADS_PER_SESSION) {
                        loadInterstitial(activity, adUnitId)
                    }
                }
                
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    interstitialAds[adUnitId] = null
                    onAdDismissed()
                }
            }
            ad.show(activity)
        } else {
            onAdDismissed()
            // If ad was null but we still have budget, try to load it for next time
            if (AdsConfig.isAdsEnabled && ad == null && adsShownCount < AdsConfig.MAX_ADS_PER_SESSION) {
                loadInterstitial(activity, adUnitId)
            }
        }
    }
}
