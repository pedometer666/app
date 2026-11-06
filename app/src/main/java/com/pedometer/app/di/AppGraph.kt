package com.pedometer.app.di

import android.content.Context
import com.pedometer.app.data.local.AppDatabase
import com.pedometer.app.data.repository.HomebaseRepository
import com.pedometer.app.data.repository.FitnessRepository
import com.pedometer.app.data.settings.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Minimal manual dependency graph. Keeps the project free of DI-framework
 * ceremony while remaining fully testable (every collaborator is an interface
 * boundary at the repository level).
 *
 * [init] must be called once from `PedometerApplication.onCreate()` before any
 * other access.
 *
 * [currentSpeedMps] and [isTracking] are in-memory channels shared by the
 * foreground service (producer) and the UI (consumer); they reset to 0 when the
 * service process dies, which is the correct semantics for a live speedometer.
 */
object AppGraph {

    val currentSpeedMps: MutableStateFlow<Float> = MutableStateFlow(0f)
    val isTracking: MutableStateFlow<Boolean> = MutableStateFlow(false)

    @Volatile
    private var database: AppDatabase? = null

    @Volatile
    private var preferences: UserPreferencesRepository? = null

    @Volatile
    private var fitness: FitnessRepository? = null

    @Volatile
    private var homebase: HomebaseRepository? = null

    @Synchronized
    fun init(context: Context) {
        if (database != null) return
        val db = AppDatabase.getInstance(context)
        val prefs = UserPreferencesRepository(context)
        database = db
        preferences = prefs
        fitness = FitnessRepository(db.stepDao(), prefs)
        homebase = HomebaseRepository(db.stepDao(), prefs)
    }

    fun preferences(): UserPreferencesRepository =
        checkNotNull(preferences) { "AppGraph.init() must run from Application.onCreate() first" }

    fun fitness(): FitnessRepository =
        checkNotNull(fitness) { "AppGraph.init() must run from Application.onCreate() first" }

    fun homebase(): HomebaseRepository =
        checkNotNull(homebase) { "AppGraph.init() must run from Application.onCreate() first" }
}
