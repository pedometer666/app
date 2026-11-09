package com.pedometer.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pedometer.app.R
import com.pedometer.app.data.settings.UserPreferencesRepository
import com.pedometer.app.ui.StepUiState
import com.pedometer.app.util.FormatUtils

/**
 * Modal bottom sheet for editing stride length, body weight, daily goal,
 * distance units, and background tracking. Slider changes update local state
 * immediately but are only committed to DataStore / Room
 * (onValueChangeFinished) so a drag gesture does not spam the database.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    uiState: StepUiState,
    onDismiss: () -> Unit,
    onStrideLengthChange: (Float) -> Unit,
    onBodyWeightChange: (Float) -> Unit,
    onDailyGoalChange: (Int) -> Unit,
    onUseMetricChange: (Boolean) -> Unit,
    onBackgroundTrackingChange: (Boolean) -> Unit,
    onUpgradeClick: () -> Unit,
    privacyOptionsRequired: Boolean,
    onPrivacyOptionsClick: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var stride by remember(uiState.strideLengthCm) { mutableFloatStateOf(uiState.strideLengthCm) }
    var weight by remember(uiState.bodyWeightKg) { mutableFloatStateOf(uiState.bodyWeightKg) }
    var goal by remember(uiState.dailyGoalSteps) { mutableIntStateOf(uiState.dailyGoalSteps) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(20.dp))

            // Stride length -------------------------------------------------
            Text(
                text = stringResource(R.string.stride_length_label),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(
                    R.string.stride_length_value,
                    stride,
                    FormatUtils.formatCentimetersAsInches(stride)
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Slider(
                value = stride,
                onValueChange = { stride = it },
                onValueChangeFinished = { onStrideLengthChange(stride) },
                valueRange = UserPreferencesRepository.MIN_STRIDE_CM..UserPreferencesRepository.MAX_STRIDE_CM
            )
            Spacer(Modifier.height(16.dp))

            // Body weight ---------------------------------------------------
            Text(
                text = stringResource(R.string.body_weight_label),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(
                    R.string.body_weight_value,
                    weight,
                    FormatUtils.formatKilogramsAsPounds(weight)
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Slider(
                value = weight,
                onValueChange = { weight = it },
                onValueChangeFinished = { onBodyWeightChange(weight) },
                valueRange = UserPreferencesRepository.MIN_WEIGHT_KG..UserPreferencesRepository.MAX_WEIGHT_KG
            )
            Spacer(Modifier.height(16.dp))

            // Daily goal ----------------------------------------------------
            Text(
                text = stringResource(R.string.daily_goal_label),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(
                    R.string.daily_goal_value,
                    FormatUtils.formatSteps(goal.toLong())
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Slider(
                value = goal.toFloat(),
                onValueChange = { goal = it.toInt() },
                onValueChangeFinished = { onDailyGoalChange(goal) },
                valueRange = UserPreferencesRepository.MIN_GOAL_STEPS.toFloat()..UserPreferencesRepository.MAX_GOAL_STEPS.toFloat(),
                steps = ((UserPreferencesRepository.MAX_GOAL_STEPS - UserPreferencesRepository.MIN_GOAL_STEPS) / 1000) - 1
            )
            Spacer(Modifier.height(16.dp))

            // Units ---------------------------------------------------------
            Text(
                text = stringResource(R.string.units_label),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Row {
                FilterChip(
                    selected = uiState.useMetric,
                    onClick = { onUseMetricChange(true) },
                    label = { Text(stringResource(R.string.unit_metric)) },
                    colors = FilterChipDefaults.filterChipColors()
                )
                FilterChip(
                    selected = !uiState.useMetric,
                    onClick = { onUseMetricChange(false) },
                    label = { Text(stringResource(R.string.unit_imperial)) },
                    modifier = Modifier.padding(start = 8.dp),
                    colors = FilterChipDefaults.filterChipColors()
                )
            }
            Spacer(Modifier.height(16.dp))

            // Background tracking -------------------------------------------
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.background_tracking_label),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.background_tracking_body),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (uiState.isPremium) {
                        Switch(
                            checked = uiState.backgroundTrackingEnabled,
                            onCheckedChange = onBackgroundTrackingChange
                        )
                    } else {
                        TextButton(onClick = onUpgradeClick) {
                            Text(stringResource(R.string.premium_unlock))
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))

            if (privacyOptionsRequired) {
                TextButton(
                    onClick = onPrivacyOptionsClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.ad_privacy_options))
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.done))
            }
        }
    }
}
