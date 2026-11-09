package com.pedometer.app.data.ads

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object AdConsentManager {

    private val _adsReady = MutableStateFlow(false)
    val adsReady = _adsReady.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)
    val privacyOptionsRequired = _privacyOptionsRequired.asStateFlow()

    private var consentInformation: ConsentInformation? = null
    @Volatile
    private var initializationStarted = false

    fun requestConsent(activity: Activity) {
        val information = UserMessagingPlatform.getConsentInformation(activity)
        consentInformation = information
        val parameters = ConsentRequestParameters.Builder().build()
        information.requestConsentInfoUpdate(
            activity,
            parameters,
            {
                _privacyOptionsRequired.value =
                    information.privacyOptionsRequirementStatus ==
                        ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                    if (error != null) {
                        Log.w(TAG, "Consent form could not be shown: ${error.message}")
                    }
                    initializeAdsIfAllowed(activity, information)
                }
            },
            { error ->
                Log.w(TAG, "Consent information request failed: ${error.message}")
                _privacyOptionsRequired.value =
                    information.privacyOptionsRequirementStatus ==
                        ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
                initializeAdsIfAllowed(activity, information)
            }
        )
    }

    fun showPrivacyOptions(activity: Activity) {
        val information = consentInformation ?: return
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            if (error != null) {
                Log.w(TAG, "Privacy options form could not be shown: ${error.message}")
            }
            val isRequired = information.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
            _privacyOptionsRequired.value = isRequired
            _adsReady.value = information.canRequestAds() && _adsReady.value
            initializeAdsIfAllowed(activity, information)
        }
    }

    private fun initializeAdsIfAllowed(
        activity: Activity,
        information: ConsentInformation
    ) {
        if (!information.canRequestAds() || initializationStarted) return
        initializationStarted = true
        MobileAds.initialize(activity) {
            _adsReady.value = true
        }
    }

    private const val TAG = "AdConsentManager"
}
