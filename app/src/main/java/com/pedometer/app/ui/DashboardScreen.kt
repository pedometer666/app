package com.pedometer.app.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pedometer.app.R
import com.pedometer.app.ui.components.MonthlyTotalsCard
import com.pedometer.app.ui.components.MetricCard
import com.pedometer.app.ui.components.Speedometer
import com.pedometer.app.util.DateUtils
import com.pedometer.app.util.FormatUtils

/**
 * Main dashboard: today's ring goal, distance/calorie cards, live speedometer
 * and running monthly totals. The shared shell (MainActivity) provides the
 * top bar, bottom navigation and the settings sheet.
 */
@Composable
fun DashboardScreen(
    uiState: StepUiState,
    permissionGranted: Boolean,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        LillyhopWelcome()
        Spacer(Modifier.height(12.dp))

        DateAndStatusRow(isTracking = uiState.isTracking)
        Spacer(Modifier.height(12.dp))

        if (!permissionGranted) {
            PermissionBanner(
                onRequestPermission = onRequestPermission,
                onOpenAppSettings = onOpenAppSettings
            )
            Spacer(Modifier.height(16.dp))
        }

        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            GoalRing(
                progress = uiState.goalProgress,
                stepsToday = uiState.stepsToday,
                dailyGoal = uiState.dailyGoalSteps
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                icon = Icons.Filled.DirectionsWalk,
                label = stringResource(R.string.distance_label),
                value = FormatUtils.formatDistanceValue(uiState.todayDistanceMeters, uiState.useMetric),
                unitSuffix = FormatUtils.distanceUnit(uiState.useMetric),
                caption = stringResource(R.string.today_sublabel),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                icon = Icons.Filled.LocalFireDepartment,
                label = stringResource(R.string.calories_label),
                value = FormatUtils.formatCalories(uiState.todayCalories),
                unitSuffix = "kcal",
                caption = stringResource(R.string.active_sublabel),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(12.dp))

        Speedometer(
            speedMps = uiState.currentSpeedMps,
            useMetric = uiState.useMetric
        )

        Spacer(Modifier.height(12.dp))

        MonthlyTotalsCard(
            monthLabel = uiState.monthLabel,
            monthSteps = uiState.monthSteps,
            monthDistanceMeters = uiState.monthDistanceMeters,
            monthCalories = uiState.monthCalories,
            useMetric = uiState.useMetric
        )

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun LillyhopWelcome() {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "\uD83D\uDC38",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.dashboard_welcome),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun DateAndStatusRow(isTracking: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = DateUtils.todayLongLabel(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                color = if (isTracking) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(
                                if (isTracking) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(
                            if (isTracking) R.string.tracking_active else R.string.tracking_paused
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isTracking) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalRing(progress: Float, stepsToday: Int, dailyGoal: Int) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "goal_ring"
    )
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    val arcColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier.size(230.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 17.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            if (animatedProgress > 0.002f) {
                drawArc(
                    color = arcColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = FormatUtils.formatSteps(stepsToday.toLong()),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 46.sp
                )
            )
            Text(
                text = stringResource(R.string.steps_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(
                    R.string.goal_of,
                    FormatUtils.formatSteps(stepsToday.toLong()),
                    FormatUtils.formatSteps(dailyGoal.toLong())
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
            if (progress >= 1f) {
                Text(
                    text = stringResource(R.string.goal_reached),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun PermissionBanner(
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.permission_banner_title),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = stringResource(R.string.permission_banner_body),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
                )
                Spacer(Modifier.height(8.dp))
                Row {
                    Button(
                        onClick = onRequestPermission,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(stringResource(R.string.permission_allow))
                    }
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = onOpenAppSettings) {
                        Text(stringResource(R.string.permission_open_settings))
                    }
                }
            }
        }
    }
}
