package com.pedometer.app.data.export

import com.pedometer.app.data.local.StepEntity
import com.pedometer.app.data.settings.UserPreferences
import java.time.Instant

/** Summary block embedded in the export payload. */
data class ExportSummary(
    val lifetimeSteps: Long,
    val lifetimeDistanceMeters: Double,
    val lifetimeActiveDays: Long,
    val bestDaySteps: Int,
    val bestDayDate: String?,
    val bestDayDistanceMeters: Double,
    val bestDayDistanceDate: String?,
    val currentStreakDays: Int
)

/**
 * Builds the portable JSON data package shared by the Android "Export & Share"
 * action and the desktop companion app.
 *
 * Schema `pedometer-export/v1`:
 * {
 *   "app": "pedometer",
 *   "schemaVersion": 1,
 *   "exportedAt": "<ISO-8601 UTC>",
 *   "profile": { dailyGoalSteps, strideLengthCm, bodyWeightKg, useMetric, backgroundTrackingEnabled },
 *   "summary": { lifetimeSteps, lifetimeDistanceMeters, lifetimeActiveDays,
 *                bestDaySteps, bestDayDate, bestDayDistanceMeters, bestDayDistanceDate,
 *                currentStreakDays },
 *   "days": [ { "date": "YYYY-MM-DD", "steps": Int, "calories": Double, "distanceMeters": Double } ]
 * }
 *
 * Hand-rolled (no org.json) so the builder is usable from plain unit tests.
 */
object ExportJsonBuilder {

    const val APP_ID = "pedometer"
    const val SCHEMA_VERSION = 1

    fun build(
        generatedAt: Instant,
        profile: UserPreferences,
        summary: ExportSummary,
        days: List<StepEntity>
    ): String {
        val builder = StringBuilder(days.size * 96 + 512)
        builder.append("{\n")
        builder.append("  \"app\": \"").append(APP_ID).append("\",\n")
        builder.append("  \"schemaVersion\": ").append(SCHEMA_VERSION).append(",\n")
        builder.append("  \"exportedAt\": \"").append(escape(generatedAt.toString())).append("\",\n")

        builder.append("  \"profile\": {\n")
        builder.append("    \"dailyGoalSteps\": ").append(profile.dailyGoalSteps).append(",\n")
        builder.append("    \"strideLengthCm\": ").append(profile.strideLengthCm).append(",\n")
        builder.append("    \"bodyWeightKg\": ").append(profile.bodyWeightKg).append(",\n")
        builder.append("    \"useMetric\": ").append(profile.useMetric).append(",\n")
        builder.append("    \"backgroundTrackingEnabled\": ").append(profile.backgroundTrackingEnabled).append("\n")
        builder.append("  },\n")

        builder.append("  \"summary\": {\n")
        builder.append("    \"lifetimeSteps\": ").append(summary.lifetimeSteps).append(",\n")
        builder.append("    \"lifetimeDistanceMeters\": ").append(summary.lifetimeDistanceMeters).append(",\n")
        builder.append("    \"lifetimeActiveDays\": ").append(summary.lifetimeActiveDays).append(",\n")
        builder.append("    \"bestDaySteps\": ").append(summary.bestDaySteps).append(",\n")
        builder.append("    \"bestDayDate\": ").append(jsonNullableString(summary.bestDayDate)).append(",\n")
        builder.append("    \"bestDayDistanceMeters\": ").append(summary.bestDayDistanceMeters).append(",\n")
        builder.append("    \"bestDayDistanceDate\": ").append(jsonNullableString(summary.bestDayDistanceDate)).append(",\n")
        builder.append("    \"currentStreakDays\": ").append(summary.currentStreakDays).append("\n")
        builder.append("  },\n")

        builder.append("  \"days\": [\n")
        days.forEachIndexed { index, day ->
            builder.append("    { \"date\": \"").append(escape(day.date))
                .append("\", \"steps\": ").append(day.steps)
                .append(", \"calories\": ").append(day.calories)
                .append(", \"distanceMeters\": ").append(day.distanceMeters)
                .append(" }")
                .append(if (index < days.lastIndex) ",\n" else "\n")
        }
        builder.append("  ]\n")
        builder.append("}\n")
        return builder.toString()
    }

    private fun jsonNullableString(value: String?): String =
        if (value == null) "null" else "\"${escape(value)}\""

    private fun escape(value: String): String = buildString(value.length + 8) {
        for (char in value) {
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (char.code < 0x20) {
                    append(String.format("\\u%04x", char.code))
                } else {
                    append(char)
                }
            }
        }
    }
}
