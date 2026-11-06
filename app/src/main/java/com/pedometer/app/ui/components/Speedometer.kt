package com.pedometer.app.ui.components

import android.graphics.Paint
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pedometer.app.R
import com.pedometer.app.util.FormatUtils
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * Live semicircular speed gauge (0–12 km/h or 0–8 mph) fed by the rolling
 * 30-second window maintained by the step service. Pure Canvas — no chart
 * library required.
 */
@Composable
fun Speedometer(
    speedMps: Float,
    useMetric: Boolean,
    modifier: Modifier = Modifier
) {
    val maxSpeed = if (useMetric) 12f else 8f
    val displaySpeed = FormatUtils.speedValue(speedMps, useMetric).coerceIn(0f, maxSpeed)
    val animatedSpeed by animateFloatAsState(
        targetValue = displaySpeed,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "speedo_needle"
    )

    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val arcColor = MaterialTheme.colorScheme.primary
    val needleColor = MaterialTheme.colorScheme.secondary
    val tickLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = CardDefaults.elevatedShape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Speed,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.speed_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(4.dp))

            GaugeArc(
                fraction = if (maxSpeed > 0f) animatedSpeed / maxSpeed else 0f,
                maxSpeed = maxSpeed,
                trackColor = trackColor,
                arcColor = arcColor,
                needleColor = needleColor,
                tickLabelColor = tickLabelColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f", animatedSpeed),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(if (useMetric) R.string.speed_kmh else R.string.speed_mph),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.speed_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
            }
        }
    }
}

@Composable
private fun GaugeArc(
    fraction: Float,
    maxSpeed: Float,
    trackColor: Color,
    arcColor: Color,
    needleColor: Color,
    tickLabelColor: Int,
    modifier: Modifier = Modifier
) {
    val tickLabels = remember(maxSpeed) {
        (0..4).map { index -> maxSpeed * index / 4f }
    }

    Canvas(modifier = modifier) {
        val stroke = 13.dp.toPx()
        val bottomInset = 6.dp.toPx()
        val center = Offset(size.width / 2f, size.height - bottomInset)
        val arcRadius = minOf(size.height - 14.dp.toPx(), size.width / 2f) - stroke
        val topLeft = Offset(center.x - arcRadius, center.y - arcRadius)
        val arcSize = Size(arcRadius * 2f, arcRadius * 2f)

        // Track
        drawArc(
            color = trackColor,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )

        // Active arc
        val clamped = fraction.coerceIn(0f, 1f)
        if (clamped > 0.004f) {
            drawArc(
                color = arcColor,
                startAngle = 180f,
                sweepAngle = 180f * clamped,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }

        // Tick marks + labels
        val labelRadius = arcRadius - stroke / 2f - 20.dp.toPx()
        val paint = Paint().apply {
            isAntiAlias = true
            color = tickLabelColor
            textSize = 10.5.sp.toPx()
            textAlign = Paint.Align.CENTER
        }
        tickLabels.forEachIndexed { index, value ->
            val angleDeg = 180f + 180f * index / (tickLabels.size - 1)
            val radians = Math.toRadians(angleDeg.toDouble())
            val cosA = cos(radians).toFloat()
            val sinA = sin(radians).toFloat()
            drawLine(
                color = Color(tickLabelColor),
                start = Offset(center.x + (arcRadius - stroke / 2f - 7.dp.toPx()) * cosA,
                               center.y + (arcRadius - stroke / 2f - 7.dp.toPx()) * sinA),
                end = Offset(center.x + (arcRadius - stroke / 2f) * cosA,
                             center.y + (arcRadius - stroke / 2f) * sinA),
                strokeWidth = 2.dp.toPx()
            )
            drawContext.canvas.nativeCanvas.drawText(
                String.format(Locale.getDefault(), "%.0f", value),
                center.x + labelRadius * cosA,
                center.y + labelRadius * sinA + 4.dp.toPx(),
                paint
            )
        }

        // Needle
        val needleRadians = Math.toRadians((180f + 180f * clamped).toDouble())
        val needleLength = arcRadius * 0.74f
        drawLine(
            color = needleColor,
            start = center,
            end = Offset(
                x = center.x + needleLength * cos(needleRadians).toFloat(),
                y = center.y + needleLength * sin(needleRadians).toFloat()
            ),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawCircle(color = needleColor, radius = 7.dp.toPx(), center = center)
        drawCircle(color = arcColor, radius = 3.dp.toPx(), center = center)
    }
}
