package com.pedometer.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pedometer.app.data.ads.AdConsentManager
import com.pedometer.app.di.AppGraph
import com.pedometer.app.domain.HomebaseStats
import com.pedometer.app.util.DateUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Display-ready snapshot for the Homebase screen. */
data class HomebaseUiState(
    val isLoading: Boolean = true,
    val monthLabel: String = "",
    val stats: HomebaseStats = HomebaseStats.EMPTY,
    val useMetric: Boolean = true,
    val isPremium: Boolean = false,
    val premiumLoading: Boolean = true,
    val adsReady: Boolean = false,
    val privacyOptionsRequired: Boolean = false
)

/**
 * Exposes Homebase analytics and the export builder. Shares the same Room
 * database and midnight ticker as the dashboard, so both screens update live.
 */
class HomebaseViewModel(application: Application) : AndroidViewModel(application) {

    private val homebaseRepository = AppGraph.homebase()
    private val preferencesRepository = AppGraph.preferences()

    val uiState: StateFlow<HomebaseUiState> = combine(
        homebaseRepository.observeHomebaseStats(),
        preferencesRepository.userPreferences,
        AppGraph.premium().uiState,
        AdConsentManager.adsReady,
        AdConsentManager.privacyOptionsRequired
    ) { stats, prefs, premium, adsReady, privacyOptionsRequired ->
        HomebaseUiState(
            isLoading = false,
            monthLabel = DateUtils.monthLabel(),
            stats = stats,
            useMetric = prefs.useMetric,
            isPremium = premium.isPremium,
            premiumLoading = premium.isLoading,
            adsReady = adsReady,
            privacyOptionsRequired = privacyOptionsRequired
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = HomebaseUiState()
    )

    /** Compiles the portable JSON data package. Call from a coroutine. */
    suspend fun buildExportJson(): String = homebaseRepository.buildExportJson()

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
