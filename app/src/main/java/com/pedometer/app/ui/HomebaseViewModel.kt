package com.pedometer.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
    val useMetric: Boolean = true
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
        preferencesRepository.userPreferences
    ) { stats, prefs ->
        HomebaseUiState(
            isLoading = false,
            monthLabel = DateUtils.monthLabel(),
            stats = stats,
            useMetric = prefs.useMetric
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
