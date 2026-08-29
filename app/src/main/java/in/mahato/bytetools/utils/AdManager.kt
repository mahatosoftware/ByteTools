package `in`.mahato.bytetools.utils

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val interstitialAds = mutableMapOf<String, InterstitialAd?>()
    private val prefs by lazy {
        context.getSharedPreferences("ad_preferences", Context.MODE_PRIVATE)
    }

    private fun getTodayDateString(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }

    fun getAdsShownToday(): Int {
        val today = getTodayDateString()
        val lastAdDate = prefs.getString("last_ad_date", "")
        return if (lastAdDate == today) {
            prefs.getInt("ads_shown_today_count", 0)
        } else {
            0
        }
    }

    private fun incrementAdsShownToday() {
        val today = getTodayDateString()
        val currentCount = getAdsShownToday()
        prefs.edit()
            .putString("last_ad_date", today)
            .putInt("ads_shown_today_count", currentCount + 1)
            .apply()
    }

    fun initialize(context: Context) {
        MobileAds.initialize(context) {}
        // Pre-load common ones if daily limit not reached
        if (getAdsShownToday() < AdsConfig.MAX_ADS_PER_DAY) {
            loadInterstitial(context, AdsConfig.PDF_INTERSTITIAL_ID)
            loadInterstitial(context, AdsConfig.IMAGE_INTERSTITIAL_ID)
            loadInterstitial(context, AdsConfig.GPS_INTERSTITIAL_ID)
            loadInterstitial(context, AdsConfig.QR_INTERSTITIAL_ID)
        }
    }

    fun loadInterstitial(context: Context, adUnitId: String) {
        if (!AdsConfig.isAdsEnabled || getAdsShownToday() >= AdsConfig.MAX_ADS_PER_DAY) return
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
        if (AdsConfig.isAdsEnabled && getAdsShownToday() < AdsConfig.MAX_ADS_PER_DAY && ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAds[adUnitId] = null
                    incrementAdsShownToday()
                    onAdDismissed()
                    // Don't try to load another if we already hit daily limit
                    if (getAdsShownToday() < AdsConfig.MAX_ADS_PER_DAY) {
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
            if (AdsConfig.isAdsEnabled && ad == null && getAdsShownToday() < AdsConfig.MAX_ADS_PER_DAY) {
                loadInterstitial(activity, adUnitId)
            }
        }
    }
}
