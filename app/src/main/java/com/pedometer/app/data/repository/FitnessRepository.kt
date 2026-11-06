package com.pedometer.app.data.repository

import com.pedometer.app.data.local.MonthlyTotals
import com.pedometer.app.data.local.StepDao
import com.pedometer.app.data.local.StepEntity
import com.pedometer.app.data.settings.UserPreferencesRepository
import com.pedometer.app.domain.FitnessCalculator
import com.pedometer.app.util.DateUtils
import com.pedometer.app.util.midnightTicker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest

/** Result of folding one raw sensor reading into the database. */
data class SensorUpdate(
    val todaySteps: Int,
    val deltaSteps: Long
)

/**
 * Single source of truth for fitness data. Owns all Room access and all
 * derivation logic; the service and UI both go through this layer.
 */
class FitnessRepository(
    private val dao: StepDao,
    private val preferencesRepository: UserPreferencesRepository
) {

    /** Emits today's record (null until the first step is recorded), re-keying at midnight. */
    fun observeToday(): Flow<StepEntity?> =
        midnightTicker().flatMapLatest { date -> dao.observeDate(DateUtils.dateKey(date)) }

    /** Emits running totals for the current calendar month, re-keying at midnight. */
    fun observeCurrentMonth(): Flow<MonthlyTotals> =
        midnightTicker().flatMapLatest { date ->
            dao.observeMonthlyTotals(DateUtils.monthStartKey(date), DateUtils.monthEndKey(date))
        }

    /**
     * Folds one raw `TYPE_STEP_COUNTER` reading into the database.
     *
     * Handles every reset scenario by comparing against the last persisted raw value:
     *  - Normal case:          delta = raw - lastRaw
     *  - Reboot / sensor reset: raw < lastRaw (or a fresh baseline) -> delta = raw
     *  - First run:            baseline is adopted, historical pre-install steps are
     *                          intentionally NOT counted (documented behaviour).
     *
     * Steps are attributed to the calendar day of the moment the event was
     * processed — sub-minute attribution drift at midnight is unavoidable with
     * batched sensor delivery and is considered acceptable.
     */
    suspend fun recordSensorReading(rawValue: Long): SensorUpdate {
        val prefs = preferencesRepository.userPreferences.first()
        val lastRaw = dao.getSensorState()?.lastRawValue

        val deltaSteps: Long = when {
            lastRaw == null -> 0L
            rawValue >= lastRaw -> rawValue - lastRaw
            else -> rawValue // sensor counter went backwards -> reboot/reset
        }.coerceAtLeast(0L)

        val metersPerStep = prefs.strideLengthCm / 100.0
        val caloriesPerStep = FitnessCalculator.caloriesPerStep(prefs.bodyWeightKg.toDouble())
        val now = System.currentTimeMillis()

        val newTodayTotal = dao.recordSensorDelta(
            date = DateUtils.todayKey(),
            deltaSteps = deltaSteps,
            distanceDelta = deltaSteps * metersPerStep,
            caloriesDelta = deltaSteps * caloriesPerStep,
            rawValue = rawValue,
            timestamp = now
        )
        return SensorUpdate(todaySteps = newTodayTotal, deltaSteps = deltaSteps)
    }

    /**
     * Rewrites every stored distance/calorie figure from raw step counts using
     * the *current* preferences. Call after stride length or body weight changes
     * so history stays consistent.
     */
    suspend fun refreshDerivedMetrics() {
        val prefs = preferencesRepository.userPreferences.first()
        dao.recomputeDerivedMetrics(
            metersPerStep = prefs.strideLengthCm / 100.0,
            caloriesPerStep = FitnessCalculator.caloriesPerStep(prefs.bodyWeightKg.toDouble())
        )
    }
}
