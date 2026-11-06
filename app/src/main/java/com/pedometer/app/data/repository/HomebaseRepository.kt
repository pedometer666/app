package com.pedometer.app.data.repository

import com.pedometer.app.data.export.ExportJsonBuilder
import com.pedometer.app.data.export.ExportSummary
import com.pedometer.app.data.local.MonthlyTotals
import com.pedometer.app.data.local.StepDao
import com.pedometer.app.data.settings.UserPreferencesRepository
import com.pedometer.app.domain.HomebaseStats
import com.pedometer.app.domain.HomebaseStatsCalculator
import com.pedometer.app.util.DateUtils
import com.pedometer.app.util.midnightTicker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import java.time.LocalDate
import java.time.Instant
import kotlin.math.min

/**
 * Analytics + export facade for the Homebase screen.
 *
 * Everything is derived from the local Room store — there is no network layer,
 * which keeps the export deterministic and the analytics instant.
 */
class HomebaseRepository(
    private val dao: StepDao,
    private val preferencesRepository: UserPreferencesRepository
) {

    /** Reactive Homebase snapshot; re-keys at every midnight via [midnightTicker]. */
    fun observeHomebaseStats(): Flow<HomebaseStats> =
        midnightTicker().flatMapLatest { today ->
            val todayKey = DateUtils.dateKey(today)
            val trailing14Start = DateUtils.dateKey(today.minusDays(13))
            val streakStart = DateUtils.dateKey(today.minusDays(HomebaseStatsCalculator.STREAK_WINDOW_DAYS))
            val monthStart = DateUtils.monthStartKey(today)

            val previousMonth = today.minusMonths(1)
            val previousMonthStart = DateUtils.monthStartKey(previousMonth)
            // Compare like-for-like: same day-of-month range in the previous month.
            val comparisonEndDay = min(today.dayOfMonth, previousMonth.lengthOfMonth())
            val previousMonthToDateEnd = DateUtils.dateKey(previousMonth.withDayOfMonth(comparisonEndDay))

            val windows = combine(
                dao.observeRange(trailing14Start, todayKey),
                dao.observeRange(streakStart, todayKey)
            ) { trailing14, streakWindow -> trailing14 to streakWindow }

            val months = combine(
                dao.observeMonthlyTotals(monthStart, todayKey),
                dao.observeMonthlyTotals(previousMonthStart, previousMonthToDateEnd)
            ) { current, previous -> current to previous }

            val records = combine(
                dao.observeBestStepDay(),
                dao.observeBestDistanceDay(),
                dao.observeLifetimeTotals()
            ) { bestStep, bestDistance, lifetime -> Triple(bestStep, bestDistance, lifetime) }

            combine(
                windows,
                months,
                records,
                preferencesRepository.userPreferences
            ) { windowPair, monthPair, recordTriple, prefs ->
                HomebaseStatsCalculator.build(
                    today = today,
                    trailing14Days = windowPair.first,
                    streakWindow = windowPair.second,
                    monthToDate = monthPair.first,
                    previousMonthToDate = monthPair.second,
                    bestStepDay = recordTriple.first,
                    bestDistanceDay = recordTriple.second,
                    lifetime = recordTriple.third,
                    dailyGoal = prefs.dailyGoalSteps
                )
            }
        }

    /**
     * Compiles the full local history into the portable JSON package used by
     * the share sheet and the desktop companion app. Runs on the caller's
     * dispatcher — invoke from a background coroutine.
     */
    suspend fun buildExportJson(): String {
        val today = LocalDate.now()
        val todayKey = DateUtils.dateKey(today)
        val streakStart = DateUtils.dateKey(today.minusDays(HomebaseStatsCalculator.STREAK_WINDOW_DAYS))

        val days = dao.getAllDays()
        val prefs = preferencesRepository.userPreferences.first()
        val lifetime = dao.observeLifetimeTotals().first()
        val bestStep = dao.observeBestStepDay().first()
        val bestDistance = dao.observeBestDistanceDay().first()
        val streakWindow = dao.observeRange(streakStart, todayKey).first()
        val streak = HomebaseStatsCalculator.currentStreak(today, streakWindow, prefs.dailyGoalSteps)

        val summary = ExportSummary(
            lifetimeSteps = lifetime.totalSteps,
            lifetimeDistanceMeters = lifetime.totalDistanceMeters,
            lifetimeActiveDays = lifetime.activeDays,
            bestDaySteps = bestStep?.steps ?: 0,
            bestDayDate = bestStep?.date,
            bestDayDistanceMeters = bestDistance?.distanceMeters ?: 0.0,
            bestDayDistanceDate = bestDistance?.date,
            currentStreakDays = streak
        )
        return ExportJsonBuilder.build(Instant.now(), prefs, summary, days)
    }
}
