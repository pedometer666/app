package com.pedometer.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pedometer.app.R
import com.pedometer.app.domain.HomebaseStats
import com.pedometer.app.ui.components.FreeHomebaseAd
import com.pedometer.app.util.FormatUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/**
 * Homebase — the analytics & data-sharing hub. Shows rolling weekly averages,
 * like-for-like monthly comparisons, personal records, lifetime totals and a
 * prominent export/share action (wired to the JSON builder by the caller).
 */
@Composable
fun HomebaseScreen(
    uiState: HomebaseUiState,
    onExportClick: () -> Unit,
    onUpgradeClick: () -> Unit,
    onPrivacyOptionsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stats = uiState.stats
    val useMetric = uiState.useMetric

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.homebase_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.homebase_subtitle),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        if (!uiState.isPremium) {
            if (uiState.premiumLoading) {
                Text(stringResource(R.string.premium_checking))
            } else {
                if (uiState.adsReady) FreeHomebaseAd()
                if (uiState.privacyOptionsRequired) {
                    TextButton(
                        onClick = onPrivacyOptionsClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.ad_privacy_options))
                    }
                }
                Spacer(Modifier.height(12.dp))
                PremiumOfferCard(onUpgradeClick)
            }
            Spacer(Modifier.height(28.dp))
            return@Column
        }

        ExportCard(onExportClick = onExportClick)
        Spacer(Modifier.height(12.dp))

        WeeklyTrendCard(stats = stats)
        Spacer(Modifier.height(12.dp))

        MonthComparisonCard(
            monthLabel = uiState.monthLabel,
            stats = stats,
            useMetric = useMetric
        )
        Spacer(Modifier.height(12.dp))

        PersonalBestsCard(stats = stats, useMetric = useMetric)
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StreakCard(
                streakDays = stats.currentStreakDays,
                modifier = Modifier.weight(1f)
            )
            LifetimeCard(
                lifetimeSteps = stats.lifetimeSteps,
                lifetimeDistanceMeters = stats.lifetimeDistanceMeters,
                activeDays = stats.lifetimeActiveDays,
                useMetric = useMetric,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun PremiumOfferCard(onUpgradeClick: () -> Unit) {
    ElevatedCard(
        shape = CardDefaults.elevatedShape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text(
                text = stringResource(R.string.premium_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.premium_description),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(14.dp))
            Button(onClick = onUpgradeClick, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.premium_unlock))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Export & share
// ---------------------------------------------------------------------------

@Composable
private fun ExportCard(onExportClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.IosShare,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.export_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.export_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onExportClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.IosShare,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.export_action))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Weekly trend (rolling 7-day average + delta chip + mini bars)
// ---------------------------------------------------------------------------

@Composable
private fun WeeklyTrendCard(stats: HomebaseStats) {
    ElevatedCard(
        shape = CardDefaults.elevatedShape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            SectionHeader(
                icon = Icons.Filled.Timeline,
                title = stringResource(R.string.homebase_weekly_title),
                caption = stringResource(R.string.homebase_weekly_caption)
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = FormatUtils.formatSteps(stats.weeklyAverageSteps.toLong()),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.steps_per_day),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 5.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            DeltaChip(
                percent = stats.weekOverWeekPercent,
                nullLabel = stringResource(R.string.week_no_baseline),
                baselineLabel = stringResource(R.string.vs_previous_week)
            )
            Spacer(Modifier.height(14.dp))
            MiniBars(
                values = stats.last7Days.map { it.steps },
                labels = stats.last7Days.map { weekdayNarrow(it.date) }
            )
        }
    }
}

@Composable
private fun MiniBars(values: List<Int>, labels: List<String>) {
    val maxValue = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        values.forEachIndexed { index, value ->
            val isToday = index == values.lastIndex
            val fraction = value.toFloat() / maxValue
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(72.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp)
                            .height((72.dp * fraction).coerceAtLeast(3.dp)),
                        shape = RoundedCornerShape(6.dp),
                        color = if (isToday) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)
                    ) {}
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = labels.getOrElse(index) { "" },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isToday) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Month-to-date vs previous month, like-for-like
// ---------------------------------------------------------------------------

@Composable
private fun MonthComparisonCard(
    monthLabel: String,
    stats: HomebaseStats,
    useMetric: Boolean
) {
    ElevatedCard(
        shape = CardDefaults.elevatedShape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            SectionHeader(
                icon = Icons.Filled.CalendarMonth,
                title = stringResource(R.string.homebase_month_title),
                caption = monthLabel
            )
            Spacer(Modifier.height(12.dp))

            val maxSteps = maxOf(stats.monthToDateSteps, stats.previousMonthToDateSteps, 1L).toFloat()

            ComparisonBar(
                label = stringResource(R.string.month_this),
                valueLabel = FormatUtils.formatSteps(stats.monthToDateSteps),
                fraction = stats.monthToDateSteps / maxSteps,
                emphasized = true
            )
            Spacer(Modifier.height(10.dp))
            ComparisonBar(
                label = stringResource(R.string.month_previous),
                valueLabel = FormatUtils.formatSteps(stats.previousMonthToDateSteps),
                fraction = stats.previousMonthToDateSteps / maxSteps,
                emphasized = false
            )

            Spacer(Modifier.height(10.dp))
            DeltaChip(
                percent = stats.monthDeltaPercent,
                nullLabel = stringResource(R.string.week_no_baseline),
                baselineLabel = stringResource(R.string.month_this)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(
                    R.string.month_detail,
                    FormatUtils.formatDistance(stats.monthToDateDistanceMeters, useMetric),
                    FormatUtils.formatCalories(stats.monthToDateCalories)
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ComparisonBar(
    label: String,
    valueLabel: String,
    fraction: Float,
    emphasized: Boolean
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(6.dp))
        Surface(
            modifier = Modifier.fillMaxWidth().height(10.dp),
            shape = RoundedCornerShape(5.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                    .height(10.dp),
                shape = RoundedCornerShape(5.dp),
                color = if (emphasized) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
            ) {}
        }
    }
}

// ---------------------------------------------------------------------------
// Personal bests
// ---------------------------------------------------------------------------

@Composable
private fun PersonalBestsCard(stats: HomebaseStats, useMetric: Boolean) {
    ElevatedCard(
        shape = CardDefaults.elevatedShape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            SectionHeader(
                icon = Icons.Filled.EmojiEvents,
                title = stringResource(R.string.homebase_bests_title)
            )
            Spacer(Modifier.height(10.dp))
            BestRow(
                value = FormatUtils.formatSteps(stats.bestDaySteps.toLong()),
                label = stringResource(R.string.best_day_steps),
                date = stats.bestDayDate
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            )
            BestRow(
                value = FormatUtils.formatDistance(stats.bestDayDistanceMeters, useMetric),
                label = stringResource(R.string.best_day_distance),
                date = stats.bestDayDistanceDate
            )
        }
    }
}

@Composable
private fun BestRow(value: String, label: String, date: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (!date.isNullOrBlank()) {
            Text(
                text = formatShortDate(date),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Streak + lifetime
// ---------------------------------------------------------------------------

@Composable
private fun StreakCard(streakDays: Int, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier,
        shape = CardDefaults.elevatedShape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                imageVector = Icons.Filled.LocalFireDepartment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = streakDays.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.streak_days),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.homebase_streak_title),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun LifetimeCard(
    lifetimeSteps: Long,
    lifetimeDistanceMeters: Double,
    activeDays: Long,
    useMetric: Boolean,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        shape = CardDefaults.elevatedShape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                imageVector = Icons.Filled.Insights,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = FormatUtils.formatSteps(lifetimeSteps),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = stringResource(R.string.lifetime_steps),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${FormatUtils.formatDistance(lifetimeDistanceMeters, useMetric)} · $activeDays ${stringResource(R.string.active_days)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Shared bits
// ---------------------------------------------------------------------------

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    caption: String? = null
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        if (!caption.isNullOrBlank()) {
            Text(
                text = caption,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DeltaChip(percent: Float?, nullLabel: String, baselineLabel: String) {
    if (percent == null) {
        ChipSurface(
            text = nullLabel,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
        return
    }
    val improved = percent >= 0f
    val arrow = if (improved) "▲" else "▼"
    val text = String.format(Locale.getDefault(), "%s %.1f%% %s", arrow, abs(percent), baselineLabel)
    val container = if (improved) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.errorContainer
    val content = if (improved) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onErrorContainer
    ChipSurface(text = text, contentColor = content, containerColor = container)
}

@Composable
private fun ChipSurface(
    text: String,
    contentColor: androidx.compose.ui.graphics.Color,
    containerColor: androidx.compose.ui.graphics.Color
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = containerColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

private fun weekdayNarrow(isoDate: String): String = runCatching {
    LocalDate.parse(isoDate).dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, Locale.getDefault())
}.getOrDefault("")

private fun formatShortDate(isoDate: String): String = runCatching {
    LocalDate.parse(isoDate).format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.getDefault()))
}.getOrDefault(isoDate)
