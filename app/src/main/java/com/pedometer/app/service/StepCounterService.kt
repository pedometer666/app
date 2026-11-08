package com.pedometer.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.pedometer.app.MainActivity
import com.pedometer.app.R
import com.pedometer.app.di.AppGraph
import com.pedometer.app.domain.RollingSpeedTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Long-running foreground service that owns `TYPE_STEP_COUNTER`.
 *
 * Design notes
 * ------------
 *  - `TYPE_STEP_COUNTER` reports cumulative steps since the last boot, never
 *    resets at midnight, and zeroes on reboot. All day/reboot handling lives in
 *    [com.pedometer.app.data.repository.FitnessRepository.recordSensorReading]
 *    via deltas against a persisted raw baseline, applied atomically in Room.
 *  - `START_STICKY` asks the system to re-create the service if it is killed.
 *  - The raw 30 s step window feeds the in-memory live speed used by the
 *    speedometer UI ([AppGraph.currentSpeedMps]).
 *  - The notification is updated at most every [NOTIFICATION_UPDATE_INTERVAL_MS]
 *    to avoid flooding the notification manager on fast walkers.
 */
class StepCounterService : Service(), SensorEventListener {

    private data class SensorReading(val rawValue: Long, val timestampMillis: Long)

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var listenerRegistered = false

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sensorReadings = Channel<SensorReading>(Channel.UNLIMITED)
    private val speedTracker = RollingSpeedTracker()

    /** Cumulative session steps; only used for the speed window, never for DB writes. */
    private var sessionSteps = 0L
    private var lastNotificationUpdateMs = 0L

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundWithType()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if (stepSensor == null) {
            Log.w(TAG, "TYPE_STEP_COUNTER not available on this device")
            updateNotification(getString(R.string.notification_sensor_unavailable))
            AppGraph.isTracking.value = false
            stopSelf()
            return
        }

        serviceScope.launch {
            for (reading in sensorReadings) {
                processSensorReading(reading)
            }
        }

        serviceScope.launch {
            if (hasActivityRecognitionPermission(this@StepCounterService)) {
                registerSensor()
            } else {
                Log.w(TAG, "ACTIVITY_RECOGNITION missing — cannot register step sensor")
                AppGraph.isTracking.value = false
                stopSelf()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopTracking()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        if (listenerRegistered) {
            sensorManager.unregisterListener(this)
            listenerRegistered = false
        }
        sensorReadings.close()
        serviceScope.cancel()
        speedTracker.clear()
        AppGraph.currentSpeedMps.value = 0f
        AppGraph.isTracking.value = false
        super.onDestroy()
    }

    // ------------------------------------------------------------------------
    // SensorEventListener
    // ------------------------------------------------------------------------

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_STEP_COUNTER) return
        val result = sensorReadings.trySend(
            SensorReading(
                rawValue = event.values[0].toLong(),
                timestampMillis = System.currentTimeMillis()
            )
        )
        if (result.isFailure) {
            Log.w(TAG, "Discarding step reading because the service is shutting down")
        }
    }

    private suspend fun processSensorReading(reading: SensorReading) {
        try {
            val update = AppGraph.fitness().recordSensorReading(reading.rawValue)
            sessionSteps += update.deltaSteps
            speedTracker.addSample(reading.timestampMillis, sessionSteps)

            val strideMeters =
                AppGraph.preferences().userPreferences.first().strideLengthCm / 100.0
            AppGraph.currentSpeedMps.value =
                speedTracker.speedMetersPerSecond(strideMeters).toFloat()

            maybeUpdateNotification(update.todaySteps, reading.timestampMillis)
        } catch (exception: Exception) {
            // Never let a DB hiccup kill the sensor pipeline.
            Log.e(TAG, "Failed to record sensor reading", exception)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    // ------------------------------------------------------------------------
    // Sensor registration
    // ------------------------------------------------------------------------

    private fun registerSensor() {
        if (listenerRegistered) return
        val sensor = stepSensor ?: return
        listenerRegistered = sensorManager.registerListener(
            this,
            sensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        AppGraph.isTracking.value = listenerRegistered
        if (!listenerRegistered) Log.w(TAG, "registerListener() returned false")
    }

    // ------------------------------------------------------------------------
    // Foreground promotion + notification
    // ------------------------------------------------------------------------

    private fun startForegroundWithType() {
        val notification = buildNotification(getString(R.string.notification_starting))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+: explicit type is mandatory and must match the manifest.
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            REQUEST_OPEN,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getService(
            this,
            REQUEST_STOP,
            Intent(this, StepCounterService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(contentText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(contentIntent)
            .addAction(R.drawable.ic_notification, getString(R.string.action_open), contentIntent)
            .addAction(R.drawable.ic_notification, getString(R.string.action_stop), stopIntent)
            .build()
    }

    private fun maybeUpdateNotification(todaySteps: Int, now: Long) {
        if (now - lastNotificationUpdateMs < NOTIFICATION_UPDATE_INTERVAL_MS && todaySteps != 0) return
        lastNotificationUpdateMs = now
        updateNotification(getString(R.string.notification_steps_today, todaySteps))
    }

    private fun updateNotification(contentText: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(contentText))
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_description)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun stopTracking() {
        // Persist the opt-out synchronously so it survives the immediate teardown
        // that follows (serviceScope is cancelled in onDestroy). The write is a
        // single small DataStore transaction — blocking for a few ms is fine here.
        runCatching {
            kotlinx.coroutines.runBlocking {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    AppGraph.preferences().setBackgroundTrackingEnabled(false)
                }
            }
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    companion object {
        private const val TAG = "StepCounterService"
        private const val CHANNEL_ID = "step_counter_channel"
        private const val NOTIFICATION_ID = 4_206
        private const val REQUEST_OPEN = 10
        private const val REQUEST_STOP = 11
        private const val NOTIFICATION_UPDATE_INTERVAL_MS = 2_000L

        const val ACTION_STOP = "com.pedometer.app.action.STOP_TRACKING"

        /** True when the runtime permission needed for the step sensor is held. */
        fun hasActivityRecognitionPermission(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED

        /** True when notifications may be posted (API 33+ requires POST_NOTIFICATIONS). */
        fun hasNotificationPermission(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

        /**
         * Best-effort start. Returns false instead of crashing when launched from
         * the background is disallowed or the permission is missing — Android 12+
         * enforces this and Play Console flags violations.
         */
        fun start(context: Context): Boolean = try {
            ContextCompat.startForegroundService(
                context,
                Intent(context, StepCounterService::class.java)
            )
            true
        } catch (security: SecurityException) {
            Log.w(TAG, "startForegroundService blocked (permission?)", security)
            false
        } catch (illegalState: IllegalStateException) {
            // Covers ForegroundServiceStartNotAllowedException (API 31+ / 34+),
            // which extends IllegalStateException.
            Log.w(TAG, "startForegroundService not allowed from this state", illegalState)
            false
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, StepCounterService::class.java))
        }
    }
}
