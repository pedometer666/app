package com.pedometer.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Singleton row (id = 1) holding the last raw value reported by
 * `TYPE_STEP_COUNTER`.
 *
 * The hardware sensor reports cumulative steps *since last boot* and resets to
 * zero on every reboot, so daily counts must be derived from deltas between
 * successive readings. Persisting the last raw value inside the same database —
 * and updating it in the same transaction as the daily row — guarantees we
 * never double-count or silently lose steps, even if the process is killed
 * between sensor events.
 */
@Entity(tableName = "sensor_state")
data class SensorStateEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val lastRawValue: Long,
    val updatedAt: Long
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
