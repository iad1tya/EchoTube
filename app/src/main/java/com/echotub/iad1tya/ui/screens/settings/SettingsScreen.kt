package com.echotube.iad1tya.ui.screens.settings

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echotube.iad1tya.BuildConfig
import com.echotube.iad1tya.data.recommendation.EchoTubeNeuroEngine
import com.echotube.iad1tya.data.recommendation.UserBrain
import com.echotube.iad1tya.ui.theme.ThemeMode
import com.echotube.iad1tya.ui.theme.extendedColors
import com.echotube.iad1tya.data.local.PlayerPreferences
import com.echotube.iad1tya.updater.ApkUpdateHelper
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.activity.compose.BackHandler
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToPlayerAppearance: () -> Unit,
    onNavigateToDonations: () -> Unit,
    onNavigateToPersonality: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToTimeManagement: () -> Unit,
    onNavigateToImport: () -> Unit,
    onNavigateToPlayerSettings: () -> Unit,
    onNavigateToVideoQuality: () -> Unit,
    onNavigateToShortsQuality: () -> Unit,
    onNavigateToContentSettings: () -> Unit,
    onNavigateToBufferSettings: () -> Unit,
    onNavigateToSearchHistory: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToUserPreferences: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToDiagnostics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val playerPreferences = remember { PlayerPreferences(context) }
    val backupRepo = remember { com.echotube.iad1tya.data.local.BackupRepository(context) }
    
    // Brain State
    var userBrain by remember { mutableStateOf<UserBrain?>(null) }
    var refreshBrainTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(refreshBrainTrigger) {
        userBrain = EchoTubeNeuroEngine.getBrainSnapshot()
    }
    
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri ->
            uri?.let {
                coroutineScope.launch {
                    val result = backupRepo.exportData(it)
                    if (result.isSuccess) {
                        android.widget.Toast.makeText(context, context.getString(com.echotube.iad1tya.R.string.settings_export_success), android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        android.widget.Toast.makeText(context, context.getString(com.echotube.iad1tya.R.string.settings_export_failed, result.exceptionOrNull()?.message), android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    )

    // Brain / engine export launcher
    val exportBrainLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri ->
            uri?.let {
                coroutineScope.launch {
                    val success = context.contentResolver.openOutputStream(it)?.use { out ->
                        EchoTubeNeuroEngine.exportBrainToStream(out)
                    } ?: false
                    android.widget.Toast.makeText(
                        context,
                        context.getString(if (success) com.echotube.iad1tya.R.string.export_engine_success else com.echotube.iad1tya.R.string.export_engine_failed),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    )

    var showRegionDialog by remember { mutableStateOf(false) }
    var showResetBrainDialog by remember { mutableStateOf(false) }
    // Update checker state (github flavor only)
    var isCheckingUpdate by remember { mutableStateOf(false) }
    // null = no dialog; non-null = tag string of the available update
    var updateAvailableTag by remember { mutableStateOf<String?>(null) }
    var updateAvailableUrl by remember { mutableStateOf<String?>(null) }
    var isUpdateDownloading by remember { mutableStateOf(false) }
    var updateDownloadProgress by remember { mutableStateOf(0) }
    var downloadedUpdatePath by remember { mutableStateOf<String?>(null) }
    var pendingInstallPath by remember { mutableStateOf<String?>(null) }

    val installPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val path = pendingInstallPath
        val activity = context as? Activity
        if (path.isNullOrEmpty() || activity == null) return@rememberLauncherForActivityResult

        if (ApkUpdateHelper.canInstallPackages(context)) {
            val result = ApkUpdateHelper.installDownloadedApk(activity, File(path))
            if (result.isFailure) {
                android.widget.Toast.makeText(context, "Failed to start installer", android.widget.Toast.LENGTH_SHORT).show()
            }
        } else {
            android.widget.Toast.makeText(context, "Allow install unknown apps to continue", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    // Player preferences states
    val currentRegion by playerPreferences.trendingRegion.collectAsState(initial = "US")

    // Optimize Region Dialog: compute list only once
    val regionList = remember { REGION_NAMES.toList() }

    // Search state
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
    LaunchedEffect(isSearchActive) {
        if (isSearchActive) runCatching { searchFocusRequester.requestFocus() }
    }
    BackHandler(enabled = isSearchActive) { isSearchActive = false; searchQuery = "" }

    val onCheckForUpdatesClick: () -> Unit = {
        if (BuildConfig.UPDATER_ENABLED && !isCheckingUpdate) {
            isCheckingUpdate = true
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val client = OkHttpClient()
                    val request = Request.Builder()
                        .url("https://api.github.com/repos/iad1tya/EchoTube/releases/latest")
                        .header("Accept", "application/vnd.github.v3+json")
                        .build()
                    val response = client.newCall(request).execute()
                    withContext(Dispatchers.Main) {
                        isCheckingUpdate = false
                        if (response.isSuccessful) {
                            val body = response.body?.string()
                            if (body != null) {
                                val json = JsonParser.parseString(body).asJsonObject
                                val latestTag = json.get("tag_name").asString
                                val cleanLatest = latestTag.removePrefix("v")
                                val cleanCurrent = BuildConfig.VERSION_NAME.removePrefix("v")
                                val latestParts = cleanLatest.split(".").mapNotNull { it.toIntOrNull() }
                                val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }
                                var isNewer = false
                                val size = maxOf(latestParts.size, currentParts.size)
                                for (i in 0 until size) {
                                    val l = latestParts.getOrNull(i) ?: 0
                                    val c = currentParts.getOrNull(i) ?: 0
                                    if (l > c) { isNewer = true; break }
                                    if (l < c) break
                                }
                                if (isNewer) {
                                    updateAvailableTag = latestTag
                                    val assets = json.getAsJsonArray("assets")
                                    var downloadUrl: String? = null
                                    if (assets != null) {
                                        for (i in 0 until assets.size()) {
                                            val asset = assets[i].asJsonObject
                                            val assetName = asset.get("name")?.asString ?: continue
                                            if (assetName.endsWith(".apk")) {
                                                downloadUrl = asset.get("browser_download_url")?.asString
                                                break
                                            }
                                        }
                                    }
                                    updateAvailableUrl = downloadUrl ?: (json.get("html_url")?.asString ?: "https://github.com/iad1tya/EchoTube/releases/latest")
                                    downloadedUpdatePath = null
                                    updateDownloadProgress = 0
                                } else {
                                    android.widget.Toast.makeText(
                                        context,
                                        context.getString(com.echotube.iad1tya.R.string.flow_is_up_to_date),
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        } else {
                            android.widget.Toast.makeText(
                                context,
                                context.getString(com.echotube.iad1tya.R.string.update_check_failed),
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        isCheckingUpdate = false
                        android.widget.Toast.makeText(
                            context,
                            context.getString(com.echotube.iad1tya.R.string.update_check_failed),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    // Section label strings for the search index
    val secEchoTubeEngine = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_flow_engine_header)
    val secAppearance = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_header_appearance)
    val secContentPlayback = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_header_content_playback)
    val secNotifications = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_header_notifications)
    val secDataManagement = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_header_data_management)
    val secAbout = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_header_about)

    var showThemeModeDialog by remember { mutableStateOf(false) }
    
    val currentThemeLabel = when (currentTheme) {
        ThemeMode.LIGHT, ThemeMode.MINT_LIGHT, ThemeMode.ROSE_LIGHT, ThemeMode.SKY_LIGHT, ThemeMode.CREAM_LIGHT -> {
            androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_mode_day)
        }
        ThemeMode.SYSTEM -> {
            androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_mode_system)
        }
        else -> {
            androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_mode_night)
        }
    }

    val allSettingsEntries = listOf(
        SettingSearchEntry(Icons.Outlined.Psychology, androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.flow_control_center), androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.neural_interest_map_subtitle), secEchoTubeEngine, onNavigateToPersonality),
        SettingSearchEntry(
            Icons.Outlined.DarkMode,
            androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_day_night_mode),
            currentThemeLabel,
            secAppearance
        ) {
            showThemeModeDialog = true
        },
        SettingSearchEntry(Icons.Outlined.Tune, androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_player_appearance), androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_player_appearance_subtitle), secAppearance, onNavigateToPlayerAppearance),
        SettingSearchEntry(Icons.Outlined.GridView, androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_content_display), androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_content_display_subtitle), secAppearance, onNavigateToContentSettings),
        SettingSearchEntry(Icons.Outlined.FilterAlt, androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_content_prefs), androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_content_prefs_subtitle), secContentPlayback, onNavigateToUserPreferences),
        SettingSearchEntry(Icons.Outlined.PlayCircle, androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_player), androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_player_subtitle), secContentPlayback, onNavigateToPlayerSettings),
        SettingSearchEntry(Icons.Outlined.HighQuality, androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_quality), androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_quality_subtitle), secContentPlayback, onNavigateToVideoQuality),
        SettingSearchEntry(Icons.Outlined.Slideshow, androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.shorts_quality_settings_title), androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.shorts_quality_settings_subtitle), secContentPlayback, onNavigateToShortsQuality),
        SettingSearchEntry(Icons.Outlined.Speed, androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_buffer), androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_buffer_subtitle), secContentPlayback, onNavigateToBufferSettings),
        SettingSearchEntry(Icons.Outlined.Download, androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_downloads), androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_downloads_subtitle), secContentPlayback, onNavigateToDownloads),
        SettingSearchEntry(Icons.Outlined.TrendingUp, androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_region), REGION_NAMES[currentRegion] ?: currentRegion, secContentPlayback) { showRegionDialog = true },
        SettingSearchEntry(Icons.Outlined.NotificationsNone, androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_notifications), androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_notifications_subtitle), secNotifications, onNavigateToNotifications),
        SettingSearchEntry(Icons.Outlined.History, androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_search_history), androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_search_history_subtitle), secDataManagement, onNavigateToSearchHistory),
        SettingSearchEntry(Icons.Outlined.Schedule, androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_time_management), androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_time_management_subtitle), secDataManagement, onNavigateToTimeManagement),
        SettingSearchEntry(Icons.Outlined.FileUpload, androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_export_data), androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_export_data_subtitle), secDataManagement) { exportLauncher.launch("echo_backup_${System.currentTimeMillis()}.json") },
        SettingSearchEntry(Icons.Outlined.FileDownload, androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_import_data), androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_import_data_subtitle), secDataManagement, onNavigateToImport),
        SettingSearchEntry(Icons.Outlined.Psychology, androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.export_engine_data), androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.export_engine_data_subtitle), secDataManagement) { exportBrainLauncher.launch("echo_brain_${System.currentTimeMillis()}.json") },
        SettingSearchEntry(Icons.Outlined.Info, androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_about_flow), androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_about_flow_subtitle), secAbout, onNavigateToAbout),
        SettingSearchEntry(Icons.Outlined.BugReport, androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_diagnostics), androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_diagnostics_subtitle), secAbout, onNavigateToDiagnostics),
        SettingSearchEntry(Icons.Outlined.VolunteerActivism, androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_support), androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_support_subtitle), secAbout, onNavigateToDonations)
    ) + if (BuildConfig.UPDATER_ENABLED) listOf(
        SettingSearchEntry(Icons.Outlined.Update, androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.check_for_updates), androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.check_for_updates_subtitle), secAbout, onCheckForUpdatesClick)
    ) else emptyList()
    val filteredEntries = if (searchQuery.isBlank()) emptyList() else allSettingsEntries.filter { entry ->
        entry.title.contains(searchQuery, ignoreCase = true) ||
        entry.subtitle.contains(searchQuery, ignoreCase = true) ||
        entry.sectionLabel.contains(searchQuery, ignoreCase = true)
    }

    if (showThemeModeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeModeDialog = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text(androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_day_night_mode)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Choose how the app looks right now.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    ThemeModeOptionCard(
                        mode = ThemeMode.LIGHT,
                        currentTheme = currentTheme,
                        title = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_mode_day),
                        subtitle = "Bright surfaces with dark text.",
                        icon = Icons.Outlined.LightMode,
                        onClick = {
                            onThemeChange(ThemeMode.LIGHT)
                            showThemeModeDialog = false
                        }
                    )
                    ThemeModeOptionCard(
                        mode = ThemeMode.DARK,
                        currentTheme = currentTheme,
                        title = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_mode_night),
                        subtitle = "Dim surfaces with light text.",
                        icon = Icons.Outlined.DarkMode,
                        onClick = {
                            onThemeChange(ThemeMode.DARK)
                            showThemeModeDialog = false
                        }
                    )
                    ThemeModeOptionCard(
                        mode = ThemeMode.SYSTEM,
                        currentTheme = currentTheme,
                        title = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_mode_system),
                        subtitle = "Follow the device setting automatically.",
                        icon = Icons.Outlined.Computer,
                        onClick = {
                            onThemeChange(ThemeMode.SYSTEM)
                            showThemeModeDialog = false
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeModeDialog = false }) {
                    Text(androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.btn_close))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.background
            ) {
                if (isSearchActive) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { isSearchActive = false; searchQuery = "" }) {
                            Icon(Icons.Default.ArrowBack, "Close search")
                        }
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(searchFocusRequester),
                            placeholder = { Text("Search settings…") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {}),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Outlined.Close, "Clear search")
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.btn_back))
                        }
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_title),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Outlined.Search, "Search settings")
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        if (isSearchActive && searchQuery.isNotBlank()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(top = 0.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (filteredEntries.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No settings found for \"$searchQuery\"",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    items(filteredEntries.size) { index ->
                        SettingsSearchResultItem(
                            entry = filteredEntries[index],
                            onNavigate = {
                                isSearchActive = false
                                searchQuery = ""
                                filteredEntries[index].onClick()
                            }
                        )
                    }
                }
            }
        } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(top = 0.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // =================================================
            // ECHO TUBE CONTROL CENTER
            // =================================================
            item { SectionHeader(text = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.flow_control_center)) }
            item {
                SettingsGroup {
                    SettingsItem(
                        icon = Icons.Outlined.Psychology,
                        title = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_flow_engine_header),
                        subtitle = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.neural_interest_map_subtitle),
                        onClick = onNavigateToPersonality
                    )
                    HorizontalDivider(Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    SettingsItem(
                        icon = Icons.Outlined.Refresh,
                        title = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_reset_everything),
                        subtitle = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_reset_brain_short),
                        onClick = { showResetBrainDialog = true }
                    )
                }
            }

            // =================================================
            // APPEARANCE
            // =================================================
            item { SectionHeader(text = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_header_appearance)) }
            item {
                SettingsGroup { 
                    SettingsItem(
                        icon = Icons.Outlined.DarkMode,
                        title = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_day_night_mode),
                        subtitle = currentThemeLabel,
                        onClick = { showThemeModeDialog = true }
                    )
                    HorizontalDivider(Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    SettingsItem(
                        icon = Icons.Outlined.Tune,
                        title = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_player_appearance),
                        subtitle = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_player_appearance_subtitle),
                        onClick = onNavigateToPlayerAppearance
                    )
                    HorizontalDivider(Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    SettingsItem(
                         icon = Icons.Outlined.GridView,
                         title = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_content_display),
                         subtitle = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_content_display_subtitle),
                         onClick = onNavigateToContentSettings
                    )
                }
            }

            // =================================================
            // CONTENT & PLAYBACK
            // =================================================
            item { SectionHeader(text = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_header_content_playback)) }
            
            item {
                SettingsGroup {
                    SettingsItem(
                        icon = Icons.Outlined.FilterAlt,
                        title = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_content_prefs),
                        subtitle = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_content_prefs_subtitle),
                        onClick = onNavigateToUserPreferences
                    )
                    HorizontalDivider(Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    SettingsItem(
                         icon = Icons.Outlined.PlayCircle,
                         title = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_player),
                         subtitle = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_player_subtitle),
                         onClick = onNavigateToPlayerSettings
                    )
                    HorizontalDivider(Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    SettingsItem(
                         icon = Icons.Outlined.HighQuality,
                         title = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_quality),
                         subtitle = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_quality_subtitle),
                         onClick = onNavigateToVideoQuality
                    )
                    HorizontalDivider(Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    SettingsItem(
                         icon = Icons.Outlined.Slideshow,
                         title = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.shorts_quality_settings_title),
                         subtitle = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.shorts_quality_settings_subtitle),
                         onClick = onNavigateToShortsQuality
                    )
                    HorizontalDivider(Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    SettingsItem(
                        icon = Icons.Outlined.Speed,
                        title = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_buffer),
                        subtitle = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_buffer_subtitle),
                        onClick = onNavigateToBufferSettings
                    )
                    HorizontalDivider(Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    SettingsItem(
                        icon = Icons.Outlined.Download,
                        title = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_downloads),
                        subtitle = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_downloads_subtitle),
                        onClick = onNavigateToDownloads
                    )
                    HorizontalDivider(Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    SettingsItem(
                        icon = Icons.Outlined.TrendingUp,
                        title = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_region),
                        subtitle = REGION_NAMES[currentRegion] ?: currentRegion,
                        onClick = { showRegionDialog = true }
                    )
                }
            }
            
            // =================================================
            // NOTIFICATIONS
            // =================================================
            item { SectionHeader(text = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_header_notifications)) }

            item {
                SettingsGroup {
                    SettingsItem(
                        icon = Icons.Outlined.NotificationsNone,
                        title = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_notifications),
                        subtitle = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_notifications_subtitle),
                        onClick = onNavigateToNotifications
                    )
                }
            }

            // =================================================
            // DATA MANAGEMENT
            // =================================================
            item { SectionHeader(text = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_header_data_management)) }
            
            item {
                SettingsGroup {
                    SettingsItem(
                        icon = Icons.Outlined.History,
                        title = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_search_history),
                        subtitle = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_search_history_subtitle),
                        onClick = onNavigateToSearchHistory
                    )
                    HorizontalDivider(Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    SettingsItem(
                        icon = Icons.Outlined.Schedule,
                        title = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_time_management),
                        subtitle = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_time_management_subtitle),
                        onClick = onNavigateToTimeManagement
                    )
                    HorizontalDivider(Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    SettingsItem(
                        icon = Icons.Outlined.FileUpload,
                        title = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_export_data),
                        subtitle = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_export_data_subtitle),
                        onClick = { exportLauncher.launch("echo_backup_${System.currentTimeMillis()}.json") }
                    )
                    HorizontalDivider(Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    SettingsItem(
                        icon = Icons.Outlined.FileDownload,
                        title = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_import_data),
                        subtitle = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_import_data_subtitle),
                        onClick = onNavigateToImport
                    )
                    HorizontalDivider(Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    SettingsItem(
                        icon = Icons.Outlined.Psychology,
                        title = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.export_engine_data),
                        subtitle = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.export_engine_data_subtitle),
                        onClick = { exportBrainLauncher.launch("echo_brain_${System.currentTimeMillis()}.json") }
                    )
                }
            }
            
            // =================================================
            // ABOUT
            // =================================================
            item { SectionHeader(text = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_header_about)) }
            item {
                SettingsGroup {
                    SettingsItem(
                        icon = Icons.Outlined.Info,
                        title = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_about_flow),
                        subtitle = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_about_flow_subtitle),
                        onClick = onNavigateToAbout
                    )
                    HorizontalDivider(Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    SettingsItem(
                        icon = Icons.Outlined.BugReport,
                        title = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_diagnostics),
                        subtitle = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_diagnostics_subtitle),
                        onClick = onNavigateToDiagnostics
                    )
                    HorizontalDivider(Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    if (BuildConfig.UPDATER_ENABLED) {
                        SettingsItem(
                            icon = if (isCheckingUpdate) Icons.Outlined.Sync else Icons.Outlined.Update,
                            title = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.check_for_updates),
                            subtitle = if (isCheckingUpdate)
                                androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.checking_for_updates)
                            else
                                androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.check_for_updates_subtitle),
                            onClick = onCheckForUpdatesClick
                        )
                        HorizontalDivider(Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    }
                    SettingsItem(
                        icon = Icons.Outlined.VolunteerActivism,
                        title = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_support),
                        subtitle = androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_item_support_subtitle),
                        onClick = onNavigateToDonations
                    )
                }
            }
        }
        }
    }

    // Reset Brain Dialog
    if (showResetBrainDialog) {
        AlertDialog(
            onDismissRequest = { showResetBrainDialog = false },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_reset_brain_title)) },
            text = { 
                Text(
                    androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_reset_brain_body),
                    style = MaterialTheme.typography.bodyMedium
                ) 
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            EchoTubeNeuroEngine.resetBrain(context)
                            refreshBrainTrigger++
                            showResetBrainDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White
                    )
                ) { Text(androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_reset_everything)) }
            },
            dismissButton = {
                TextButton(onClick = { showResetBrainDialog = false }) { Text(androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.cancel)) }
            }
        )
    }

    // Update Available Dialog (github flavor only)
    if (BuildConfig.UPDATER_ENABLED) {
        val tag = updateAvailableTag
        if (tag != null) {
            AlertDialog(
                onDismissRequest = {
                    if (!isUpdateDownloading) {
                        updateAvailableTag = null
                        updateAvailableUrl = null
                    }
                },
                icon = { Icon(Icons.Outlined.Update, null, tint = MaterialTheme.colorScheme.primary) },
                title = { Text(androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.new_update_available), fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.update_available_template, tag),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (isUpdateDownloading) {
                            Spacer(Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { updateDownloadProgress.coerceIn(0, 100) / 100f },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Downloading... ${updateDownloadProgress.coerceIn(0, 100)}%",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (isUpdateDownloading) return@Button

                        val activity = context as? Activity
                        if (activity == null) {
                            android.widget.Toast.makeText(context, "Unable to start update", android.widget.Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val downloadedPath = downloadedUpdatePath
                        if (!downloadedPath.isNullOrEmpty()) {
                            val apkFile = File(downloadedPath)
                            if (!apkFile.exists()) {
                                downloadedUpdatePath = null
                                android.widget.Toast.makeText(context, "Downloaded file not found", android.widget.Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            if (ApkUpdateHelper.canInstallPackages(context)) {
                                val installResult = ApkUpdateHelper.installDownloadedApk(activity, apkFile)
                                if (installResult.isFailure) {
                                    android.widget.Toast.makeText(context, "Failed to start installer", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                pendingInstallPath = downloadedPath
                                installPermissionLauncher.launch(ApkUpdateHelper.createUnknownSourcesIntent(context))
                            }
                            return@Button
                        }

                        val url = updateAvailableUrl
                        if (url.isNullOrBlank()) {
                            android.widget.Toast.makeText(context, "No download URL available", android.widget.Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        coroutineScope.launch {
                            isUpdateDownloading = true
                            updateDownloadProgress = 0
                            val result = ApkUpdateHelper.downloadApk(
                                context = context,
                                apkUrl = url,
                                onProgress = { progress ->
                                    updateDownloadProgress = progress
                                }
                            )
                            isUpdateDownloading = false
                            if (result.isSuccess) {
                                downloadedUpdatePath = result.getOrNull()?.absolutePath
                                android.widget.Toast.makeText(context, "Update downloaded", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                updateDownloadProgress = 0
                                android.widget.Toast.makeText(context, "Failed to download update", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) {
                        Text(if (downloadedUpdatePath != null) "Install" else androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.download))
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        if (!isUpdateDownloading) {
                            updateAvailableTag = null
                            updateAvailableUrl = null
                        }
                    }) {
                        Text(androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.cancel))
                    }
                }
            )
        }
    }

    // Region Selection Dialog
    if (showRegionDialog) {
        var regionSearchQuery by remember { mutableStateOf("") }
        val filteredRegions = remember(regionSearchQuery) {
            if (regionSearchQuery.isBlank()) regionList
            else regionList.filter { (code, name) ->
                name.contains(regionSearchQuery, ignoreCase = true) ||
                code.contains(regionSearchQuery, ignoreCase = true)
            }
        }
        AlertDialog(
            onDismissRequest = { showRegionDialog = false },
            title = { Text(androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.settings_region_dialog_title)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = regionSearchQuery,
                        onValueChange = { regionSearchQuery = it },
                        placeholder = { Text(androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.search_hint)) },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(Modifier.heightIn(max = 260.dp)) {
                        items(filteredRegions.size) { index ->
                            val (code, name) = filteredRegions[index]
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        coroutineScope.launch { playerPreferences.setTrendingRegion(code); showRegionDialog = false }
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = currentRegion == code, onClick = null)
                                Spacer(Modifier.width(8.dp))
                                Text(name)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showRegionDialog = false }) { Text(androidx.compose.ui.res.stringResource(com.echotube.iad1tya.R.string.cancel)) } }
        )
    }
    
}

@Composable
fun BrainTraitRow(label: String, value: Double, leftLabel: String, rightLabel: String) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text("${(value * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = value.toFloat(), // Fixed: No lambda
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(leftLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Text(rightLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }
    }
}

private val REGION_NAMES = mapOf(
    "DZ" to "Algeria", "AS" to "American Samoa", "AI" to "Anguilla", "AR" to "Argentina",
    "AW" to "Aruba", "AU" to "Australia", "AT" to "Austria", "AZ" to "Azerbaijan",
    "BH" to "Bahrain", "BD" to "Bangladesh", "BY" to "Belarus", "BE" to "Belgium",
    "BM" to "Bermuda", "BO" to "Bolivia", "BA" to "Bosnia and Herzegovina", "BR" to "Brazil",
    "IO" to "British Indian Ocean Territory", "VG" to "British Virgin Islands", "BG" to "Bulgaria", "KH" to "Cambodia",
    "CA" to "Canada", "KY" to "Cayman Islands", "CL" to "Chile", "CO" to "Colombia",
    "CR" to "Costa Rica", "HR" to "Croatia", "CY" to "Cyprus", "CZ" to "Czech Republic",
    "DK" to "Denmark", "DO" to "Dominican Republic", "EC" to "Ecuador", "EG" to "Egypt",
    "SV" to "El Salvador", "EE" to "Estonia", "FK" to "Falkland Islands", "FO" to "Faroe Islands",
    "FI" to "Finland", "FR" to "France", "GF" to "French Guiana", "PF" to "French Polynesia",
    "GE" to "Georgia", "DE" to "Germany", "GH" to "Ghana", "GI" to "Gibraltar",
    "GR" to "Greece", "GL" to "Greenland", "GP" to "Guadeloupe", "GU" to "Guam",
    "GT" to "Guatemala", "HN" to "Honduras", "HK" to "Hong Kong", "HU" to "Hungary",
    "IS" to "Iceland", "IN" to "India", "ID" to "Indonesia", "IQ" to "Iraq",
    "IE" to "Ireland", "IL" to "Israel", "IT" to "Italy", "JM" to "Jamaica",
    "JP" to "Japan", "JO" to "Jordan", "KZ" to "Kazakhstan", "KE" to "Kenya",
    "KW" to "Kuwait", "LA" to "Laos", "LV" to "Latvia", "LB" to "Lebanon",
    "LY" to "Libya", "LI" to "Liechtenstein", "LT" to "Lithuania", "LU" to "Luxembourg",
    "MY" to "Malaysia", "MT" to "Malta", "MQ" to "Martinique", "YT" to "Mayotte",
    "MX" to "Mexico", "MD" to "Moldova", "ME" to "Montenegro", "MS" to "Montserrat",
    "MA" to "Morocco", "NP" to "Nepal", "NL" to "Netherlands", "NC" to "New Caledonia",
    "NZ" to "New Zealand", "NI" to "Nicaragua", "NG" to "Nigeria", "NF" to "Norfolk Island",
    "MP" to "Northern Mariana Islands", "NO" to "Norway", "OM" to "Oman", "PK" to "Pakistan",
    "PA" to "Panama", "PG" to "Papua New Guinea", "PY" to "Paraguay", "PE" to "Peru",
    "PH" to "Philippines", "PL" to "Poland", "PT" to "Portugal", "PR" to "Puerto Rico",
    "QA" to "Qatar", "RE" to "Reunion", "RO" to "Romania", "RU" to "Russia",
    "SH" to "Saint Helena", "PM" to "Saint Pierre and Miquelon", "SA" to "Saudi Arabia", "SN" to "Senegal",
    "RS" to "Serbia", "SG" to "Singapore", "SK" to "Slovakia", "SI" to "Slovenia",
    "ZA" to "South Africa", "KR" to "South Korea", "ES" to "Spain", "LK" to "Sri Lanka",
    "SJ" to "Svalbard and Jan Mayen", "SE" to "Sweden", "CH" to "Switzerland", "TW" to "Taiwan",
    "TZ" to "Tanzania", "TH" to "Thailand", "TN" to "Tunisia", "TR" to "Turkey",
    "TC" to "Turks and Caicos Islands", "UG" to "Uganda", "UA" to "Ukraine", "AE" to "United Arab Emirates",
    "GB" to "United Kingdom", "US" to "United States", "VI" to "U.S. Virgin Islands", "UY" to "Uruguay",
    "VE" to "Venezuela", "VN" to "Vietnam"
).toList().sortedBy { it.second }.toMap()


private data class SettingSearchEntry(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val sectionLabel: String,
    val onClick: () -> Unit
)

@Composable
private fun ThemeModeOptionCard(
    mode: ThemeMode,
    currentTheme: ThemeMode,
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val isSelected = isThemeModeSelected(currentTheme, mode)
    val containerColor = if (isSelected) {
        Color(0xFFF5F5F5)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    }
    val contentColor = if (isSelected) {
        Color.Black
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) Color(0xFFE6E6E6) else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = contentColor.copy(alpha = 0.12f)
            ) {
                Box(
                    modifier = Modifier.size(44.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.78f)
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = Color.Black
                )
            }
        }
    }
}

private fun isThemeModeSelected(currentTheme: ThemeMode, mode: ThemeMode): Boolean {
    return when (mode) {
        ThemeMode.LIGHT -> currentTheme in setOf(
            ThemeMode.LIGHT,
            ThemeMode.MINT_LIGHT,
            ThemeMode.ROSE_LIGHT,
            ThemeMode.SKY_LIGHT,
            ThemeMode.CREAM_LIGHT
        )
        ThemeMode.DARK -> currentTheme !in setOf(
            ThemeMode.LIGHT,
            ThemeMode.MINT_LIGHT,
            ThemeMode.ROSE_LIGHT,
            ThemeMode.SKY_LIGHT,
            ThemeMode.CREAM_LIGHT,
            ThemeMode.SYSTEM
        )
        else -> currentTheme == mode
    }
}

@Composable
private fun SettingsSearchResultItem(
    entry: SettingSearchEntry,
    onNavigate: () -> Unit
) {
    Column {
        Text(
            text = entry.sectionLabel.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 72.dp, top = 8.dp, bottom = 2.dp)
        )
        SettingsItem(
            icon = entry.icon,
            title = entry.title,
            subtitle = entry.subtitle,
            onClick = onNavigate
        )
        HorizontalDivider(
            Modifier.padding(start = 56.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    }
}
