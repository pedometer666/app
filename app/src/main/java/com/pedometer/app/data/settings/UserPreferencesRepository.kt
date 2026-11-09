package com.pedometer.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pedometer.app.domain.FitnessCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/** Snapshot of the user-configurable preferences the fitness engine consumes. */
data class UserPreferences(
    val strideLengthCm: Float = FitnessCalculator.DEFAULT_STRIDE_LENGTH_CM,
    val bodyWeightKg: Float = FitnessCalculator.DEFAULT_BODY_WEIGHT_KG,
    val dailyGoalSteps: Int = DEFAULT_DAILY_GOAL_STEPS,
    /** true = kilometres / km/h, false = miles / mph. */
    val useMetric: Boolean = true,
    /** Whether the background step service should be kept running. */
    val backgroundTrackingEnabled: Boolean = false
) {
    companion object {
        const val DEFAULT_DAILY_GOAL_STEPS: Int = 10_000
    }
}

private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences"
)

/**
 * Jetpack DataStore-backed settings store. All reads are exposed as cold flows;
 * writes are coerced into sane ranges so bad sensor input can never corrupt
 * distance/calorie math.
 */
class UserPreferencesRepository(context: Context) {

    private val store = context.applicationContext.userPreferencesDataStore

    val userPreferences: Flow<UserPreferences> = store.data
        .catch { throwable ->
            // If the preferences file is unreadable, fall back to defaults instead of crashing.
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { prefs ->
            UserPreferences(
                strideLengthCm = prefs[STRIDE_LENGTH_CM] ?: FitnessCalculator.DEFAULT_STRIDE_LENGTH_CM,
                bodyWeightKg = prefs[BODY_WEIGHT_KG] ?: FitnessCalculator.DEFAULT_BODY_WEIGHT_KG,
                dailyGoalSteps = prefs[DAILY_GOAL_STEPS] ?: UserPreferences.DEFAULT_DAILY_GOAL_STEPS,
                useMetric = prefs[USE_METRIC] ?: true,
                backgroundTrackingEnabled = prefs[BACKGROUND_TRACKING] ?: false
            )
        }

    suspend fun setStrideLengthCm(value: Float) {
        store.edit { it[STRIDE_LENGTH_CM] = value.coerceIn(MIN_STRIDE_CM, MAX_STRIDE_CM) }
    }

    suspend fun setBodyWeightKg(value: Float) {
        store.edit { it[BODY_WEIGHT_KG] = value.coerceIn(MIN_WEIGHT_KG, MAX_WEIGHT_KG) }
    }

    suspend fun setDailyGoalSteps(value: Int) {
        store.edit { it[DAILY_GOAL_STEPS] = value.coerceIn(MIN_GOAL_STEPS, MAX_GOAL_STEPS) }
    }

    suspend fun setUseMetric(useMetric: Boolean) {
        store.edit { it[USE_METRIC] = useMetric }
    }

    suspend fun setBackgroundTrackingEnabled(enabled: Boolean) {
        store.edit { it[BACKGROUND_TRACKING] = enabled }
    }

    companion object {
        const val MIN_STRIDE_CM = 30f
        const val MAX_STRIDE_CM = 150f
        const val MIN_WEIGHT_KG = 30f
        const val MAX_WEIGHT_KG = 200f
        const val MIN_GOAL_STEPS = 2_000
        const val MAX_GOAL_STEPS = 30_000

        private val STRIDE_LENGTH_CM = floatPreferencesKey("stride_length_cm")
        private val BODY_WEIGHT_KG = floatPreferencesKey("body_weight_kg")
        private val DAILY_GOAL_STEPS = intPreferencesKey("daily_goal_steps")
        private val USE_METRIC = booleanPreferencesKey("use_metric")
        private val BACKGROUND_TRACKING = booleanPreferencesKey("background_tracking_enabled")
    }
}
