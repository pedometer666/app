package com.pedometer.app

import com.pedometer.app.data.export.ExportJsonBuilder
import com.pedometer.app.data.export.ExportSummary
import com.pedometer.app.data.local.StepEntity
import com.pedometer.app.data.settings.UserPreferences
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ExportJsonBuilderTest {

    @Test
    fun build_emitsAllSectionsAndDays() {
        val json = ExportJsonBuilder.build(
            generatedAt = Instant.parse("2026-11-06T12:00:00Z"),
            profile = UserPreferences(strideLengthCm = 80f, bodyWeightKg = 82.5f, dailyGoalSteps = 12_000, useMetric = false),
            summary = ExportSummary(
                lifetimeSteps = 123_456,
                lifetimeDistanceMeters = 92_592.0,
                lifetimeActiveDays = 98,
                bestDaySteps = 21_337,
                bestDayDate = "2026-10-12",
                bestDayDistanceMeters = 17_069.6,
                bestDayDistanceDate = "2026-10-12",
                currentStreakDays = 6
            ),
            days = listOf(
                StepEntity("2026-11-04", 8_000, 300.0, 6_400.0, 0L),
                StepEntity("2026-11-05", 9_500, 356.25, 7_600.0, 0L)
            )
        )

        assertTrue(json.contains("\"app\": \"pedometer\""))
        assertTrue(json.contains("\"schemaVersion\": 1"))
        assertTrue(json.contains("\"exportedAt\": \"2026-11-06T12:00:00Z\""))
        assertTrue(json.contains("\"dailyGoalSteps\": 12000"))
        assertTrue(json.contains("\"useMetric\": false"))
        assertTrue(json.contains("\"lifetimeSteps\": 123456"))
        assertTrue(json.contains("\"currentStreakDays\": 6"))
        assertTrue(json.contains("\"date\": \"2026-11-04\", \"steps\": 8000"))
        assertTrue(json.contains("\"date\": \"2026-11-05\", \"steps\": 9500"))
        // Valid-ish structure: every opening brace has a matching close (crude but useful here).
        assertTrue(json.count { it == '{' } == json.count { it == '}' })
        assertTrue(json.trim().endsWith("}"))
    }

    @Test
    fun build_nullDates_emitJsonNull() {
        val json = ExportJsonBuilder.build(
            generatedAt = Instant.parse("2026-11-06T12:00:00Z"),
            profile = UserPreferences(),
            summary = ExportSummary(0, 0.0, 0, 0, null, 0.0, null, 0),
            days = emptyList()
        )
        assertTrue(json.contains("\"bestDayDate\": null"))
        assertTrue(json.contains("\"bestDayDistanceDate\": null"))
        assertTrue(json.contains("\"days\": [\n  ]"))
    }
}
