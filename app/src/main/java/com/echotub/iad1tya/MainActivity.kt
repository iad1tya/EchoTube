package com.echotube.iad1tya

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.content.Context
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import com.echotube.iad1tya.data.local.LocalDataManager
import com.echotube.iad1tya.player.GlobalPlayerState
import com.echotube.iad1tya.ui.EchoTubeApp
import com.echotube.iad1tya.ui.theme.EchoTubeTheme
import com.echotube.iad1tya.ui.theme.ThemeMode
import com.echotube.iad1tya.notification.UpdateCheckWorker
import com.echotube.iad1tya.updater.ApkUpdateHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import android.util.Log
import com.echotube.iad1tya.data.recommendation.EchoTubeNeuroEngine
import com.echotube.iad1tya.ui.screens.CrashReporterScreen
import com.echotube.iad1tya.utils.EchoTubeCrashHandler
import com.echotube.iad1tya.utils.UpdateManager
import com.echotube.iad1tya.utils.UpdateInfo
import com.echotube.iad1tya.ui.components.UpdateDialog
import com.echotube.iad1tya.BuildConfig
import androidx.activity.SystemBarStyle
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val _deeplinkVideoId = mutableStateOf<String?>(null)
    val deeplinkVideoId: State<String?> = _deeplinkVideoId
    
    private val _isDeeplinkShort = mutableStateOf(false)
    val isDeeplinkShort: State<Boolean> = _isDeeplinkShort

    private val _pendingUpdateInfo = mutableStateOf<UpdateInfo?>(null)
    val pendingUpdateInfo: State<UpdateInfo?> = _pendingUpdateInfo

    // Tracks whether playback was active when PiP window closed, so we can
    // resume automatically if the user taps to expand back to the app.
    private var wasPlayingWhenPipExited = false

    // Cached auto-PiP preference
    private var cachedAutoPipEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        
        // Initialize global player state
        GlobalPlayerState.initialize(applicationContext)

        // Keep auto-PiP preference cached so onUserLeaveHint can read it synchronously
        lifecycleScope.launch {
            com.echotube.iad1tya.data.local.PlayerPreferences(applicationContext)
                .autoPipEnabled
                .collect { enabled -> cachedAutoPipEnabled = enabled }
        }
        
        // Initialize Neuro Engine (Recommendation System)
        lifecycleScope.launch(Dispatchers.IO) {
            EchoTubeNeuroEngine.initialize(applicationContext)
        }

        val dataManager = LocalDataManager(applicationContext)

        handleIntent(intent)

        
        // Check for updates on each launch in background (updater-enabled builds)
        if (BuildConfig.UPDATER_ENABLED) {
            UpdateCheckWorker.enqueueLaunchCheck(this)
        }

        setContent {
            val scope = rememberCoroutineScope()
            var themeMode by remember { mutableStateOf(ThemeMode.LIGHT) }

            val context = LocalContext.current

            // Check for a crash that happened last session.
            // If found, show the CrashReporterScreen instead of the normal UI.
            var pendingCrashLog by remember {
                mutableStateOf(EchoTubeCrashHandler.getLastCrash(applicationContext))
            }

            if (pendingCrashLog != null) {
                EchoTubeTheme(themeMode = themeMode) {
                    CrashReporterScreen(
                        crashLog = pendingCrashLog!!,
                        onClearAndRestart = {
                            EchoTubeCrashHandler.clearLastCrash(applicationContext)
                            pendingCrashLog = null
                        }
                    )
                }
                return@setContent
            }

            var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
            var isUpdateDownloading by remember { mutableStateOf(false) }
            var updateDownloadProgress by remember { mutableStateOf(0) }
            var downloadedUpdatePath by remember { mutableStateOf<String?>(null) }
            var pendingInstallPath by remember { mutableStateOf<String?>(null) }

            val installPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) {
                val path = pendingInstallPath
                if (path.isNullOrEmpty()) return@rememberLauncherForActivityResult

                if (ApkUpdateHelper.canInstallPackages(context)) {
                    val result = ApkUpdateHelper.installDownloadedApk(this@MainActivity, File(path))
                    if (result.isFailure) {
                        Toast.makeText(context, "Failed to start installer", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Allow install unknown apps to continue", Toast.LENGTH_SHORT).show()
                }
            }
            
            // Load theme preference and keep it reactive
            LaunchedEffect(Unit) {
                dataManager.themeMode.collect { mode ->
                    themeMode = mode
                }
            }
            
            // Initialize EchoTube Neuro Engine
            LaunchedEffect(Unit) {
                com.echotube.iad1tya.data.recommendation.EchoTubeNeuroEngine.initialize(applicationContext)
            }

            val useDarkSystemBarIcons = when (themeMode) {
                ThemeMode.LIGHT, ThemeMode.MINT_LIGHT, ThemeMode.ROSE_LIGHT, ThemeMode.SKY_LIGHT, ThemeMode.CREAM_LIGHT -> true
                else -> false
            }

            SideEffect {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.isAppearanceLightStatusBars = useDarkSystemBarIcons
                insetsController.isAppearanceLightNavigationBars = useDarkSystemBarIcons
            }

            EchoTubeTheme(themeMode = themeMode) {
                // Show Dialog Overlay if update exists (github flavor only)
                if (BuildConfig.UPDATER_ENABLED && updateInfo != null) {
                    UpdateDialog(
                        updateInfo = updateInfo!!,
                        isDownloading = isUpdateDownloading,
                        downloadProgress = updateDownloadProgress,
                        isDownloaded = downloadedUpdatePath != null,
                        onDismiss = {
                            updateInfo = null
                            if (!isUpdateDownloading) {
                                downloadedUpdatePath = null
                                updateDownloadProgress = 0
                            }
                        },
                        onUpdate = {
                            if (isUpdateDownloading) return@UpdateDialog

                            scope.launch {
                                isUpdateDownloading = true
                                updateDownloadProgress = 0
                                downloadedUpdatePath = null

                                val result = ApkUpdateHelper.downloadApk(
                                    context = context,
                                    apkUrl = updateInfo!!.downloadUrl,
                                    onProgress = { progress ->
                                        updateDownloadProgress = progress
                                    }
                                )

                                isUpdateDownloading = false
                                if (result.isSuccess) {
                                    downloadedUpdatePath = result.getOrNull()?.absolutePath
                                    Toast.makeText(context, "Update downloaded", Toast.LENGTH_SHORT).show()
                                } else {
                                    updateDownloadProgress = 0
                                    Toast.makeText(context, "Failed to download update", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onInstall = {
                            val path = downloadedUpdatePath
                            if (path.isNullOrEmpty()) {
                                Toast.makeText(context, "Download update first", Toast.LENGTH_SHORT).show()
                                return@UpdateDialog
                            }

                            val apkFile = File(path)
                            if (!apkFile.exists()) {
                                downloadedUpdatePath = null
                                Toast.makeText(context, "Downloaded file not found", Toast.LENGTH_SHORT).show()
                                return@UpdateDialog
                            }

                            if (ApkUpdateHelper.canInstallPackages(context)) {
                                val installResult = ApkUpdateHelper.installDownloadedApk(this@MainActivity, apkFile)
                                if (installResult.isFailure) {
                                    Toast.makeText(context, "Failed to start installer", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                pendingInstallPath = path
                                installPermissionLauncher.launch(ApkUpdateHelper.createUnknownSourcesIntent(context))
                            }
                        }
                    )
                }

                // Handle update from notification (github flavor only)
                if (BuildConfig.UPDATER_ENABLED) {
                    val pendingUpdate by this@MainActivity.pendingUpdateInfo
                    LaunchedEffect(pendingUpdate) {
                        if (pendingUpdate != null) {
                            updateInfo = pendingUpdate
                        }
                    }
                }

                // Request notification permission for Android 13+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
                    ) { isGranted ->
                        if (isGranted) {
                            android.util.Log.d("MainActivity", "Notification permission granted")
                        } else {
                            android.util.Log.w("MainActivity", "Notification permission denied")
                        }
                    }

                    LaunchedEffect(Unit) {
                        if (androidx.core.content.ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.POST_NOTIFICATIONS
                            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                val deeplinkVideoId by this@MainActivity.deeplinkVideoId
                val isDeeplinkShort by this@MainActivity.isDeeplinkShort

                EchoTubeApp(
                    currentTheme = themeMode,
                    onThemeChange = { newTheme ->
                        themeMode = newTheme
                        scope.launch {
                            dataManager.setThemeMode(newTheme)
                        }
                    },
                    deeplinkVideoId = deeplinkVideoId,
                    isShort = isDeeplinkShort,
                    onDeeplinkConsumed = {
                        consumeDeeplink()
                    }
                )
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Release player when app is destroyed
        GlobalPlayerState.release()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val data = intent.data
        val notificationVideoId = intent.getStringExtra("notification_video_id") ?: intent.getStringExtra("video_id")
        
        // Reset shorts flag
        _isDeeplinkShort.value = false

        val videoId = if (data != null && intent.action == Intent.ACTION_VIEW) {
            val urlString = data.toString()
            if (urlString.contains("shorts/")) {
                _isDeeplinkShort.value = true
            }
            extractVideoId(urlString)
        } else if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (sharedText != null) {
                if (sharedText.contains("shorts/")) {
                    _isDeeplinkShort.value = true
                }
                extractVideoId(sharedText)
            } else null
        } else {
            notificationVideoId
        }
        
        // Check extra
        if (intent.getBooleanExtra("is_short", false) || intent.getBooleanExtra("is_shorts", false)) {
            _isDeeplinkShort.value = true
        }
        
        if (videoId != null) {
            _deeplinkVideoId.value = videoId
            intent.putExtra("deeplink_video_id", videoId)
        }

        // Check for Update Notification extras
        if (intent.hasExtra("EXTRA_UPDATE_VERSION")) {
            val version = intent.getStringExtra("EXTRA_UPDATE_VERSION") ?: ""
            val changelog = intent.getStringExtra("EXTRA_UPDATE_CHANGELOG") ?: ""
            val url = intent.getStringExtra("EXTRA_UPDATE_URL") ?: ""
            _pendingUpdateInfo.value = UpdateInfo(version, changelog, url, true)
        }
    }

    fun consumeDeeplink() {
        _deeplinkVideoId.value = null
        _isDeeplinkShort.value = false
    }

    private fun extractVideoId(url: String): String? {
        val patterns = listOf(
            Regex("v=([^&]+)"),
            Regex("shorts/([^/?]+)"),
            Regex("youtu.be/([^/?]+)"),
            Regex("embed/([^/?]+)"),
            Regex("v/([^/?]+)")
        )
        for (pattern in patterns) {
            val match = pattern.find(url)
            if (match != null) return match.groupValues[1]
        }
        return url.substringAfterLast("/").substringBefore("?").ifEmpty { null }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: android.content.res.Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        GlobalPlayerState.setPipMode(isInPictureInPictureMode)

        if (!isInPictureInPictureMode) {
            val pm = com.echotube.iad1tya.player.EnhancedPlayerManager.getInstance()
            wasPlayingWhenPipExited = pm.isPlaying()
            pm.pause()
            pm.stopBackgroundService()
        }
    }

    override fun onResume() {
        super.onResume()
        if (wasPlayingWhenPipExited) {
            wasPlayingWhenPipExited = false
            val pm = com.echotube.iad1tya.player.EnhancedPlayerManager.getInstance()
            pm.play()
            val video = GlobalPlayerState.currentVideo.value
            if (video != null) {
                pm.startBackgroundService(
                    videoId   = video.id,
                    title     = video.title,
                    channel   = video.channelName,
                    thumbnail = video.thumbnailUrl
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        wasPlayingWhenPipExited = false  
        if (!isInPictureInPictureMode) {
            requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Only enter PiP mode if video is playing and has progressed
        // We use the EnhancedPlayerManager directly to get the immediate state
        val playerManager = com.echotube.iad1tya.player.EnhancedPlayerManager.getInstance()
        val musicManager = com.echotube.iad1tya.player.EnhancedMusicPlayerManager
        
        val isVideoPlaying = playerManager.playerState.value.isPlaying && 
                           playerManager.playerState.value.currentVideoId != null &&
                           playerManager.getCurrentPosition() > 500 // At least 0.5s in
        
        val isMusicPlaying = musicManager.playerState.value.isPlaying
        
        // Only enter PiP for video, not for music (which uses background service)
        if (isVideoPlaying && !isMusicPlaying && cachedAutoPipEnabled) {
            enterPictureInPictureMode(
                android.app.PictureInPictureParams.Builder()
                    .setAspectRatio(android.util.Rational(16, 9))
                    .build()
            )
        }
    }

    /**
     * Ask Android to whitelist this app from battery optimization / Doze mode.
     *
     * Without this, on aggressive OEM ROMs (Xiaomi MIUI, Samsung OneUI DeX, CRDroid, Huawei)
     * the OS can throttle network access or kill the background playback service after a few
     * minutes of screen-off. 
     *
     * The system shows a standard dialog asking the user to confirm.  We only request this once
     * per install (if the app is not already exempt).  No spammy repeat prompts.
     */
    private fun requestBatteryOptimizationExemptionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
        if (powerManager.isIgnoringBatteryOptimizations(packageName)) return // already exempt
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.w("MainActivity", "Could not request battery optimization exemption: ${e.message}")
        }
    }
}