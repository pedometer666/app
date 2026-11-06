package com.pedometer.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface StepDao {

    /** Reactive stream of a single day. Emits null when the day has no row yet. */
    @Query("SELECT * FROM steps WHERE date = :date")
    fun observeDate(date: String): Flow<StepEntity?>

    /**
     * Running totals for a calendar month (or any inclusive ISO-date span).
     * Date keys are ISO `YYYY-MM-DD`, which compare lexicographically in
     * chronological order, so a BETWEEN range on first/last day works.
     * COALESCE keeps the projection non-null for empty ranges.
     */
    @Query(
        """
        SELECT COALESCE(SUM(steps), 0)          AS totalSteps,
               COALESCE(SUM(calories), 0.0)     AS totalCalories,
               COALESCE(SUM(distanceMeters), 0.0) AS totalDistanceMeters
        FROM steps
        WHERE date BETWEEN :monthStart AND :monthEnd
        """
    )
    fun observeMonthlyTotals(monthStart: String, monthEnd: String): Flow<MonthlyTotals>

    /** Full history, oldest first — used for export and long-range aggregation. */
    @Query("SELECT * FROM steps ORDER BY date ASC")
    suspend fun getAllDays(): List<StepEntity>

    /** Reactive window query, inclusive on both ends. */
    @Query("SELECT * FROM steps WHERE date BETWEEN :start AND :end ORDER BY date ASC")
    fun observeRange(start: String, end: String): Flow<List<StepEntity>>

    /** The single highest-step day ever recorded, or null when the table is empty. */
    @Query("SELECT date, steps FROM steps ORDER BY steps DESC LIMIT 1")
    fun observeBestStepDay(): Flow<BestDayRecord?>

    /** The single longest-distance day ever recorded, or null when the table is empty. */
    @Query("SELECT date, distanceMeters FROM steps ORDER BY distanceMeters DESC LIMIT 1")
    fun observeBestDistanceDay(): Flow<BestDistanceRecord?>

    /** Lifetime aggregates across all recorded days. */
    @Query(
        """
        SELECT COALESCE(SUM(steps), 0)          AS totalSteps,
               COALESCE(SUM(calories), 0.0)     AS totalCalories,
               COALESCE(SUM(distanceMeters), 0.0) AS totalDistanceMeters,
               COUNT(*)                          AS activeDays
        FROM steps
        """
    )
    fun observeLifetimeTotals(): Flow<LifetimeTotals>

    @Query("SELECT * FROM steps WHERE date = :date")
    suspend fun getByDate(date: String): StepEntity?

    @Query("SELECT * FROM sensor_state WHERE id = 1")
    suspend fun getSensorState(): SensorStateEntity?

    @Upsert
    suspend fun upsertStep(entity: StepEntity)

    @Upsert
    suspend fun upsertSensorState(state: SensorStateEntity)

    /**
     * Rewrites every stored derived metric so historical rows stay consistent
     * after the user changes stride length or body weight.
     */
    @Query("UPDATE steps SET distanceMeters = steps * :metersPerStep, calories = steps * :caloriesPerStep")
    suspend fun recomputeDerivedMetrics(metersPerStep: Double, caloriesPerStep: Double)

    /**
     * Atomically applies one sensor delta: bumps today's row and advances the
     * persisted raw sensor baseline, all in a single transaction.
     * @return the updated daily step total.
     */
    @Transaction
    suspend fun recordSensorDelta(
        date: String,
        deltaSteps: Long,
        distanceDelta: Double,
        caloriesDelta: Double,
        rawValue: Long,
        timestamp: Long
    ): Int {
        val current = getByDate(date)
        val updatedSteps = ((current?.steps ?: 0) + deltaSteps).coerceAtLeast(0L).toInt()
        upsertStep(
            StepEntity(
                date = date,
                steps = updatedSteps,
                calories = (current?.calories ?: 0.0) + caloriesDelta,
                distanceMeters = (current?.distanceMeters ?: 0.0) + distanceDelta,
                updatedAt = timestamp
            )
        )
        upsertSensorState(
            SensorStateEntity(
                id = SensorStateEntity.SINGLETON_ID,
                lastRawValue = rawValue,
                updatedAt = timestamp
            )
        )
        return updatedSteps
    }
}
