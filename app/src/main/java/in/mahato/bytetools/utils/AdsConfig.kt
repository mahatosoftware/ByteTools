package `in`.mahato.bytetools.utils

import `in`.mahato.bytetools.BuildConfig

object AdsConfig {
    /**
     * Centralized AdMob configuration.
     * IDs are strictly managed via build.gradle.kts using buildConfigField.
     */
    
    // Interstitial Ad Unit ID
    val INTERSTITIAL_ID = BuildConfig.AD_INTERSTITIAL_ID
    
    // Banner Ad Unit ID
    val BANNER_ID = BuildConfig.AD_BANNER_ID
    
    // Native Ad Unit ID
    val NATIVE_ID = BuildConfig.AD_NATIVE_ID

    // Global toggle for ads (can be used to disable ads for Pro users)
    var isAdsEnabled = true
}
