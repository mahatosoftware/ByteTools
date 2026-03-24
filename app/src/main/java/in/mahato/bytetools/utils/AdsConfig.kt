package `in`.mahato.bytetools.utils

import `in`.mahato.bytetools.BuildConfig

object AdsConfig {
    /**
     * Centralized AdMob configuration.
     * IDs are strictly managed via build.gradle.kts using buildConfigField.
     */
    
    // Interstitial Ad Unit IDs
    val PDF_INTERSTITIAL_ID = BuildConfig.AD_PDF_INTERSTITIAL_ID
    val IMAGE_INTERSTITIAL_ID = BuildConfig.AD_IMAGE_INTERSTITIAL_ID
    val GPS_INTERSTITIAL_ID = BuildConfig.AD_GPS_INTERSTITIAL_ID
    val QR_INTERSTITIAL_ID = BuildConfig.AD_QR_INTERSTITIAL_ID
    
    // Global toggle for ads (can be used to disable ads for Pro users)
    var isAdsEnabled = true
    
    // Limits
    const val MAX_ADS_PER_SESSION = 1
}
