package com.pedometer.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pedometer.app.di.AppGraph
import com.pedometer.app.service.StepCounterService
import com.pedometer.app.util.DateUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Bridges the data layer and Compose. Combines the preference store, Room's
 * reactive "today"/"this month" queries, and the in-memory live speed channel
 * into a single [StepUiState] stream.
 *
 * Thread-safety: Flows are collected with viewModelScope (main-immediate) and
 * all heavy work happens inside the repositories on Dispatchers.IO.
 */
class StepViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesRepository = AppGraph.preferences()
    private val fitnessRepository = AppGraph.fitness()

    val uiState: StateFlow<StepUiState> = combine(
        preferencesRepository.userPreferences,
        fitnessRepository.observeToday(),
        fitnessRepository.observeCurrentMonth(),
        AppGraph.currentSpeedMps,
        AppGraph.isTracking
    ) { preferences, today, month, speedMps, tracking ->
        val stepsToday = today?.steps ?: 0
        val goal = preferences.dailyGoalSteps

        StepUiState(
            isLoading = false,
            stepsToday = stepsToday,
            dailyGoalSteps = goal,
            goalProgress = (stepsToday.toFloat() / goal).coerceIn(0f, 1f),
            todayDistanceMeters = today?.distanceMeters ?: 0.0,
            todayCalories = today?.calories ?: 0.0,
            monthLabel = DateUtils.monthLabel(),
            monthSteps = month.totalSteps,
            monthDistanceMeters = month.totalDistanceMeters,
            monthCalories = month.totalCalories,
            strideLengthCm = preferences.strideLengthCm,
            bodyWeightKg = preferences.bodyWeightKg,
            useMetric = preferences.useMetric,
            backgroundTrackingEnabled = preferences.backgroundTrackingEnabled,
            isTracking = tracking,
            currentSpeedMps = speedMps
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = StepUiState()
    )

    fun setStrideLengthCm(value: Float) = updatePreferences {
        preferencesRepository.setStrideLengthCm(value)
        fitnessRepository.refreshDerivedMetrics()
    }

    fun setBodyWeightKg(value: Float) = updatePreferences {
        preferencesRepository.setBodyWeightKg(value)
        fitnessRepository.refreshDerivedMetrics()
    }

    fun setDailyGoalSteps(value: Int) = updatePreferences {
        preferencesRepository.setDailyGoalSteps(value)
    }

    fun setUseMetric(useMetric: Boolean) = updatePreferences {
        preferencesRepository.setUseMetric(useMetric)
    }

    /**
     * Flips background tracking and immediately starts/stops the foreground
     * service to match. When enabled while the permission is missing the next
     * app launch / permission grant will start it instead.
     */
    fun setBackgroundTrackingEnabled(enabled: Boolean) = updatePreferences {
        preferencesRepository.setBackgroundTrackingEnabled(enabled)
        val app = getApplication<Application>()
        if (enabled) {
            if (StepCounterService.hasActivityRecognitionPermission(app)) {
                StepCounterService.start(app)
            }
        } else {
            StepCounterService.stop(app)
        }
    }

    private fun updatePreferences(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L
    }
}
