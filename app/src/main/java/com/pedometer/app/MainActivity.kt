package com.pedometer.app

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pedometer.app.data.ads.AdConsentManager
import com.pedometer.app.di.AppGraph
import com.pedometer.app.service.StepCounterService
import com.pedometer.app.ui.DashboardScreen
import com.pedometer.app.ui.HomebaseScreen
import com.pedometer.app.ui.HomebaseViewModel
import com.pedometer.app.ui.StepViewModel
import com.pedometer.app.ui.components.PremiumSheet
import com.pedometer.app.ui.components.SettingsSheet
import com.pedometer.app.ui.theme.PedometerTheme
import com.pedometer.app.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PedometerTheme {
                PedometerShell(this@MainActivity)
            }
        }
        AdConsentManager.requestConsent(this)
    }
}

private enum class ShellTab { DASHBOARD, HOMEBASE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PedometerShell(activity: Activity) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val lifecycleOwner = LocalLifecycleOwner.current

    val stepViewModel: StepViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    )
    val homebaseViewModel: HomebaseViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    )

    val uiState by stepViewModel.uiState.collectAsStateWithLifecycle()
    val homebaseState by homebaseViewModel.uiState.collectAsStateWithLifecycle()
    val premiumState by AppGraph.premium().uiState.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableStateOf(ShellTab.DASHBOARD) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showPremium by rememberSaveable { mutableStateOf(false) }
    var activityRecognitionGranted by remember {
        mutableStateOf(StepCounterService.hasActivityRecognitionPermission(context))
    }

    val scope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        activityRecognitionGranted = StepCounterService.hasActivityRecognitionPermission(context)
        if (activityRecognitionGranted) {
            StepCounterService.start(context)
        }
    }

    // Free tracking runs only while the app is visible. Premium can keep the service alive.
    val keepTrackingInBackground =
        premiumState.isPremium && uiState.backgroundTrackingEnabled
    DisposableEffect(
        lifecycleOwner,
        activityRecognitionGranted,
        keepTrackingInBackground
    ) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    if (StepCounterService.hasActivityRecognitionPermission(context)) {
                        StepCounterService.start(context)
                    }
                }
                Lifecycle.Event.ON_STOP -> {
                    if (!keepTrackingInBackground) StepCounterService.stop(context)
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) &&
            StepCounterService.hasActivityRecognitionPermission(context)
        ) {
            StepCounterService.start(context)
        }
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Request missing runtime permissions when the shell first enters composition.
    LaunchedEffect(Unit) {
        if (missingPermissions(context).isNotEmpty()) {
            permissionLauncher.launch(missingPermissions(context))
        }
    }

    // Shared export/share action (top bar icon + Homebase button).
    val exportAndShare: () -> Unit = {
        if (premiumState.isPremium) {
            scope.launch {
                runCatching {
                    val json = homebaseViewModel.buildExportJson()
                    withContext(Dispatchers.IO) { shareExportJson(context, json) }
                }.onFailure { error ->
                    android.util.Log.e("PedometerShell", "Export failed", error)
                    Toast.makeText(context, R.string.export_failed, Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            showPremium = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            if (selectedTab == ShellTab.DASHBOARD) R.string.app_name
                            else R.string.homebase_title
                        )
                    )
                },
                actions = {
                    IconButton(
                        onClick = exportAndShare
                    ) {
                        Icon(
                            imageVector = Icons.Filled.IosShare,
                            contentDescription = stringResource(R.string.export_action)
                        )
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.settings_title)
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == ShellTab.DASHBOARD,
                    onClick = { selectedTab = ShellTab.DASHBOARD },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.DirectionsWalk,
                            contentDescription = null
                        )
                    },
                    label = { Text(stringResource(R.string.tab_dashboard)) }
                )
                NavigationBarItem(
                    selected = selectedTab == ShellTab.HOMEBASE,
                    onClick = { selectedTab = ShellTab.HOMEBASE },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Insights,
                            contentDescription = null
                        )
                    },
                    label = { Text(stringResource(R.string.tab_homebase)) }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        when (selectedTab) {
            ShellTab.DASHBOARD -> DashboardScreen(
                uiState = uiState,
                permissionGranted = activityRecognitionGranted,
                onRequestPermission = { permissionLauncher.launch(missingPermissions(context)) },
                onOpenAppSettings = { openAppSettings(context) },
                modifier = Modifier.padding(paddingValues)
            )
            ShellTab.HOMEBASE -> HomebaseScreen(
                uiState = homebaseState,
                onExportClick = exportAndShare,
                onUpgradeClick = { showPremium = true },
                onPrivacyOptionsClick = { AdConsentManager.showPrivacyOptions(activity) },
                modifier = Modifier.padding(paddingValues)
            )
        }
    }

    if (showSettings) {
        SettingsSheet(
            uiState = uiState,
            onDismiss = { showSettings = false },
            onStrideLengthChange = stepViewModel::setStrideLengthCm,
            onBodyWeightChange = stepViewModel::setBodyWeightKg,
            onDailyGoalChange = stepViewModel::setDailyGoalSteps,
            onUseMetricChange = stepViewModel::setUseMetric,
            onBackgroundTrackingChange = stepViewModel::setBackgroundTrackingEnabled,
            onUpgradeClick = {
                showSettings = false
                showPremium = true
            },
            privacyOptionsRequired = homebaseState.privacyOptionsRequired,
            onPrivacyOptionsClick = { AdConsentManager.showPrivacyOptions(activity) }
        )
    }

    if (showPremium) {
        PremiumSheet(
            uiState = premiumState,
            onDismiss = { showPremium = false },
            onPurchase = { AppGraph.premium().purchase(activity) },
            onRestore = { AppGraph.premium().restorePurchases() }
        )
    }
}

private fun missingPermissions(context: android.content.Context): Array<String> {
    val missing = mutableListOf<String>()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
        !StepCounterService.hasActivityRecognitionPermission(context)
    ) {
        missing += Manifest.permission.ACTIVITY_RECOGNITION
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        !StepCounterService.hasNotificationPermission(context)
    ) {
        missing += Manifest.permission.POST_NOTIFICATIONS
    }
    return missing.toTypedArray()
}

private fun openAppSettings(context: android.content.Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
}

/**
 * Writes [json] into the app cache and opens the system share sheet with a
 * content:// URI backed by [FileProvider] (cache-path/exports).
 * Runs on Dispatchers.IO — file writes stay off the main thread.
 */
private fun shareExportJson(context: android.content.Context, json: String) {
    val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
    val fileName = "pedometer-export-${DateUtils.dateKey(LocalDate.now())}.json"
    val exportFile = File(exportsDir, fileName)
    exportFile.writeText(json, Charsets.UTF_8)

    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        exportFile
    )
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.export_chooser_title))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(
        Intent.createChooser(sendIntent, context.getString(R.string.export_chooser_title))
    )
}
