package com.pedometer.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.pedometer.app.di.AppGraph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Restarts background step tracking after a reboot or app update.
 *
 * `BOOT_COMPLETED` is a legitimate background entry point, but Android 12+ still
 * allows denial of the foreground-service promotion, so the start is best-effort
 * and wrapped in try/catch inside [StepCounterService.start].
 *
 * Only restarts when the user has background tracking enabled and the activity
 * recognition permission is held — we never defeat a permission denial.
 */
class BootReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }
        if (!StepCounterService.hasActivityRecognitionPermission(context)) {
            Log.i(TAG, "Not starting after $action — activity recognition permission missing")
            return
        }

        val pendingResult = goAsync()
        scope.launch {
            try {
                AppGraph.init(context.applicationContext)
                val enabled = AppGraph.preferences().userPreferences.first().backgroundTrackingEnabled
                if (enabled) {
                    StepCounterService.start(context)
                } else {
                    Log.i(TAG, "Background tracking disabled by user — not starting")
                }
            } catch (exception: Exception) {
                Log.w(TAG, "Failed to restart step tracking after $action", exception)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
