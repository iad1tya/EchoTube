package com.echotube.iad1tya.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import androidx.hilt.navigation.compose.hiltViewModel
import com.echotube.iad1tya.data.model.Video
import com.echotube.iad1tya.player.GlobalPlayerState
import com.echotube.iad1tya.ui.components.PlayerSheetValue
import com.echotube.iad1tya.ui.components.PlayerDraggableState
import com.echotube.iad1tya.ui.components.MusicPlayerSheetState
import com.echotube.iad1tya.ui.screens.home.HomeScreen
import com.echotube.iad1tya.ui.screens.history.HistoryScreen
import com.echotube.iad1tya.ui.screens.library.LibraryScreen
import com.echotube.iad1tya.ui.screens.library.WatchLaterScreen
import com.echotube.iad1tya.ui.screens.likedvideos.LikedVideosScreen
import com.echotube.iad1tya.ui.screens.playlists.PlaylistsScreen
import com.echotube.iad1tya.ui.screens.playlists.PlaylistDetailScreen
import com.echotube.iad1tya.ui.screens.notifications.NotificationScreen
import com.echotube.iad1tya.ui.screens.player.VideoPlayerViewModel
import com.echotube.iad1tya.ui.screens.player.VideoPlayerUiState
import com.echotube.iad1tya.ui.screens.search.SearchScreen
import com.echotube.iad1tya.ui.screens.settings.SettingsScreen
import com.echotube.iad1tya.ui.screens.settings.ImportDataScreen
import com.echotube.iad1tya.ui.screens.personality.EchoTubePersonalityScreen
import com.echotube.iad1tya.ui.screens.shorts.ShortsScreen
import com.echotube.iad1tya.ui.screens.subscriptions.SubscriptionsScreen
import com.echotube.iad1tya.ui.screens.subscriptions.SubscriptionShortsScreen
import com.echotube.iad1tya.ui.screens.subscriptions.SubscriptionsViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.echotube.iad1tya.ui.screens.channel.ChannelScreen
import com.echotube.iad1tya.ui.screens.onboarding.OnboardingScreen
import com.echotube.iad1tya.ui.theme.ThemeMode
import androidx.media3.common.util.UnstableApi
import java.net.URLEncoder

@UnstableApi
fun NavGraphBuilder.flowAppGraph(
    navController: NavHostController,
    currentRoute: MutableState<String>,
    showBottomNav: MutableState<Boolean>,
    selectedBottomNavIndex: MutableIntState,
    playerSheetState: PlayerDraggableState,
    musicPlayerSheetState: MusicPlayerSheetState,
    playerViewModel: VideoPlayerViewModel,
    playerUiStateResult: State<VideoPlayerUiState>, 
    playerVisibleState: MutableState<Boolean>, 
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    onOnboardingComplete: () -> Unit = {},
    disableShortsPlayer: Boolean = false
) {
    // =============================================
    // ONBOARDING (First-time user experience)
    // =============================================
    composable("onboarding") {
        currentRoute.value = "onboarding"
        showBottomNav.value = false
        OnboardingScreen(
            onComplete = {
                onOnboardingComplete()
                // Navigate to home and clear the backstack so user can't go back to onboarding
                navController.navigate("home") {
                    popUpTo("onboarding") { inclusive = true }
                }
            }
        )
    }
    
    composable("home") {
        currentRoute.value = "home"
        showBottomNav.value = playerSheetState.currentValue != PlayerSheetValue.Expanded
        selectedBottomNavIndex.intValue = 0
        val density = LocalDensity.current
        val config = LocalConfiguration.current
        // Use miniSizeScale live value: wide = screenWidth * 9/16 height, normal = 0
        val inlinePlayerHeight by remember {
            derivedStateOf {
                val scale = playerSheetState.miniSizeScale.value
                val isMini = playerSheetState.expandFraction.value > 0.5f
                if (isMini && scale > 1.5f) {
                    with(density) { (config.screenWidthDp.dp.toPx() * (9f / 16f)).toDp() }
                } else 0.dp
            }
        }
        HomeScreen(
            currentTheme = currentTheme,
            onVideoClick = { video ->
                if (video.isShort && !disableShortsPlayer) {
                    navController.navigate("shorts?startVideoId=${video.id}")
                } else {
                    playerViewModel.playVideo(video)
                    GlobalPlayerState.setCurrentVideo(video)
                }
            },
            onShortClick = { video ->
                navController.navigate("shorts?startVideoId=${video.id}")
            },
            onSearchClick = {
                navController.navigate("search")
            },
            onNotificationClick = {
                navController.navigate("notifications")
            },
            onSettingsClick = {
                navController.navigate("settings")
            },
            onChannelClick = { channelId ->
                val encodedUrl = java.net.URLEncoder.encode("https://www.youtube.com/channel/$channelId", "UTF-8")
                navController.navigate("channel?url=$encodedUrl")
            }
        )
    }

    // Notifications Screen
    composable("notifications") {
        currentRoute.value = "notifications"
        showBottomNav.value = false
        NotificationScreen(
            onBackClick = { navController.popBackStack() },
            onNotificationClick = { videoId ->
                navController.navigate("player/$videoId")
            }
        )
    }

    composable(
        route = "shorts?startVideoId={startVideoId}",
        arguments = listOf(
            navArgument("startVideoId") { 
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    ) { backStackEntry ->
        currentRoute.value = "shorts"
        showBottomNav.value = true
        selectedBottomNavIndex.intValue = 1
        val startVideoId = backStackEntry.arguments?.getString("startVideoId")
        ShortsScreen(
            startVideoId = startVideoId,
            onBack = {
                navController.popBackStack()
            },
            onChannelClick = { channelId ->
                navController.navigate("channel?url=$channelId")
            }
        )
    }

    composable("subscriptions") {
        currentRoute.value = "subscriptions"
        showBottomNav.value = true
        selectedBottomNavIndex.intValue = 3
        SubscriptionsScreen(
            onVideoClick = { video ->
                if (video.isShort && !disableShortsPlayer) {
                    navController.navigate("shorts?startVideoId=${video.id}")
                } else {
                    navController.navigate("player/${video.id}")
                }
            },
            onShortClick = { videoId ->
                navController.navigate("shorts?startVideoId=$videoId")
            },
            onChannelClick = { channelUrl ->
                val encodedUrl = channelUrl.replace("/", "%2F").replace(":", "%3A")
                navController.navigate("channel?url=$encodedUrl")
            },
            onViewAllShortsClick = {
                navController.navigate("subscription_shorts")
            }
        )
    }

    composable("subscription_shorts") {
        currentRoute.value = "subscription_shorts"
        showBottomNav.value = false
        // Share the exact same ViewModel instance that SubscriptionsScreen already loaded —
        // using the 'subscriptions' back stack entry as the owner so data is not re-fetched.
        val subsEntry = remember(navController) {
            navController.getBackStackEntry("subscriptions")
        }
        val subsViewModel: SubscriptionsViewModel = viewModel(subsEntry)
        SubscriptionShortsScreen(
            onBackClick = { navController.popBackStack() },
            onShortClick = { videoId ->
                navController.navigate("shorts?startVideoId=$videoId")
            },
            viewModel = subsViewModel
        )
    }

    composable("library") {
        currentRoute.value = "library"
        showBottomNav.value = true
        selectedBottomNavIndex.intValue = 4
        LibraryScreen(
            onNavigateToHistory = { 
                navController.navigate("history")
            },
            onNavigateToPlaylists = { 
                navController.navigate("playlists")
            },
            onNavigateToLikedVideos = { 
                navController.navigate("likedVideos")
            },
            onNavigateToWatchLater = {
                navController.navigate("watchLater")
            },
            onNavigateToSavedShorts = {
                navController.navigate("savedShorts")
            },
            onNavigateToDownloads = {
                navController.navigate("downloads")
            },
            onManageData = {
                navController.navigate("settings")
            }
        )
    }

    composable("search") {
        currentRoute.value = "search"
        showBottomNav.value = true
        selectedBottomNavIndex.intValue = 5
        SearchScreen(
            onVideoClick = { video ->
                if (video.isShort && !disableShortsPlayer) {
                    navController.navigate("shorts?startVideoId=${video.id}")
                } else {
                    navController.navigate("player/${video.id}")
                }
            },
            onChannelClick = { channel ->
                val channelUrl = if (channel.url.isNotBlank()) {
                    channel.url
                } else {
                    "https://www.youtube.com/channel/${channel.id}"
                }
                val encodedUrl = java.net.URLEncoder.encode(channelUrl, "UTF-8")
                navController.navigate("channel?url=$encodedUrl")
            },
            onPlaylistClick = { playlist ->
                navController.navigate("playlist/${playlist.id}")
            }
        )
    }

    composable("categories") {
        currentRoute.value = "categories"
        showBottomNav.value = true
        selectedBottomNavIndex.intValue = 6
        com.echotube.iad1tya.ui.screens.categories.CategoriesScreen(
            onBackClick = { navController.popBackStack() },
            onVideoClick = { video ->
                if (video.isShort && !disableShortsPlayer) {
                    navController.navigate("shorts?startVideoId=${video.id}")
                } else {
                    navController.navigate("player/${video.id}")
                }
            }
        )
    }

    composable("settings") {
        currentRoute.value = "settings"
        showBottomNav.value = false
        SettingsScreen(
            currentTheme = currentTheme,
            onThemeChange = onThemeChange,

            onNavigateBack = { navController.popBackStack() },
            onNavigateToPlayerAppearance = { navController.navigate("settings/player_appearance") },
            onNavigateToDonations = { navController.navigate("donations") },
            onNavigateToPersonality = { navController.navigate("personality") },
            onNavigateToDownloads = { navController.navigate("settings/downloads") },
            onNavigateToTimeManagement = { navController.navigate("settings/time_management") },
            onNavigateToImport = { navController.navigate("settings/import") },
            onNavigateToPlayerSettings = { navController.navigate("settings/player") },
            onNavigateToSponsorBlock = { navController.navigate("settings/sponsorblock") },
            onNavigateToVideoQuality = { navController.navigate("settings/video_quality") },
            onNavigateToShortsQuality = { navController.navigate("settings/shorts_quality") },
            onNavigateToShortsAutoScroll = { navController.navigate("settings/shorts_auto_scroll") },
            onNavigateToContentSettings = { navController.navigate("settings/content") },
            onNavigateToBufferSettings = { navController.navigate("settings/buffer") },
            onNavigateToSearchHistory = { navController.navigate("settings/search_history") },
            onNavigateToAbout = { navController.navigate("settings/about") },
            onNavigateToUserPreferences = { navController.navigate("settings/user_preferences") },
            onNavigateToNotifications = { navController.navigate("settings/notifications") },
            onNavigateToDiagnostics = { navController.navigate("settings/diagnostics") }
        )
    }

    composable("settings/user_preferences") {
        currentRoute.value = "settings/user_preferences"
        showBottomNav.value = false
        com.echotube.iad1tya.ui.screens.settings.UserPreferencesScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable("settings/player") {
        currentRoute.value = "settings/player"
        showBottomNav.value = false
        com.echotube.iad1tya.ui.screens.settings.PlayerSettingsScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable("settings/sponsorblock") {
        currentRoute.value = "settings/sponsorblock"
        showBottomNav.value = false
        com.echotube.iad1tya.ui.screens.settings.SponsorBlockSettingsScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }
    
    composable("settings/buffer") {
        currentRoute.value = "settings/buffer"
        showBottomNav.value = false
        com.echotube.iad1tya.ui.screens.settings.BufferSettingsScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }
    
    composable("settings/search_history") {
        currentRoute.value = "settings/search_history"
        showBottomNav.value = false
        com.echotube.iad1tya.ui.screens.settings.SearchHistorySettingsScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable("settings/video_quality") {
        currentRoute.value = "settings/video_quality"
        showBottomNav.value = false
        com.echotube.iad1tya.ui.screens.settings.VideoQualitySettingsScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable("settings/shorts_quality") {
        currentRoute.value = "settings/shorts_quality"
        showBottomNav.value = false
        com.echotube.iad1tya.ui.screens.settings.ShortsVideoQualitySettingsScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable("settings/shorts_auto_scroll") {
        currentRoute.value = "settings/shorts_auto_scroll"
        showBottomNav.value = false
        com.echotube.iad1tya.ui.screens.settings.ShortsAutoScrollSettingsScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }
    
    composable("settings/content") {
        currentRoute.value = "settings/content"
        showBottomNav.value = false
        com.echotube.iad1tya.ui.screens.settings.ContentSettingsScreen(
            onBackClick = { navController.popBackStack() }
        )
    }
    
    composable("settings/import") {
        currentRoute.value = "settings/import"
        ImportDataScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable("settings/time_management") {
        currentRoute.value = "settings/time_management"
        showBottomNav.value = false
        com.echotube.iad1tya.ui.screens.settings.TimeManagementScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable("settings/about") {
        currentRoute.value = "settings/about"
        showBottomNav.value = false
        com.echotube.iad1tya.ui.screens.settings.AboutScreen(
            currentTheme = currentTheme,
            onNavigateBack = { navController.popBackStack() },
            onNavigateToDonations = { navController.navigate("donations") }
        )
    }

    composable("settings/appearance") {
        currentRoute.value = "settings/appearance"
        showBottomNav.value = false
        com.echotube.iad1tya.ui.screens.settings.AppearanceScreen(
            currentTheme = currentTheme,
            onThemeChange = onThemeChange,
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable("settings/player_appearance") {
        currentRoute.value = "settings/player_appearance"
        showBottomNav.value = false
        com.echotube.iad1tya.ui.screens.settings.PlayerAppearanceScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable("settings/downloads") {
        currentRoute.value = "settings/downloads"
        showBottomNav.value = false
        com.echotube.iad1tya.ui.screens.settings.DownloadSettingsScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable("settings/notifications") {
        currentRoute.value = "settings/notifications"
        showBottomNav.value = false
        com.echotube.iad1tya.ui.screens.settings.NotificationSettingsScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable("settings/diagnostics") {
        currentRoute.value = "settings/diagnostics"
        showBottomNav.value = false
        com.echotube.iad1tya.ui.screens.settings.DiagnosticsScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable("donations") {
        currentRoute.value = "donations"
        showBottomNav.value = false
        com.echotube.iad1tya.ui.screens.settings.DonationsScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable("personality") {
        currentRoute.value = "personality"
        showBottomNav.value = false
        EchoTubePersonalityScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }
    
    composable(
        route = "channel?url={channelUrl}",
        arguments = listOf(navArgument("channelUrl") { type = NavType.StringType })
    ) { backStackEntry ->
        currentRoute.value = "channel"
        showBottomNav.value = false
        val channelUrl = backStackEntry.arguments?.getString("channelUrl")?.let {
            java.net.URLDecoder.decode(it, "UTF-8")
        } ?: ""
        
        ChannelScreen(
            channelUrl = channelUrl,
            onVideoClick = { video ->
                if (video.isShort && !disableShortsPlayer) {
                    navController.navigate("shorts?startVideoId=${video.id}")
                } else {
                    navController.navigate("player/${video.id}")
                }
            },
            onShortClick = { videoId ->
                navController.navigate("shorts?startVideoId=$videoId")
            },
            onPlaylistClick = { playlistId ->
                navController.navigate("playlist/$playlistId")
            },
            onBackClick = { navController.popBackStack() }
        )
    }

    // History Screen
    composable("history") {
        currentRoute.value = "history"
        showBottomNav.value = false
        HistoryScreen(
            onVideoClick = { track ->
                navController.navigate("player/${track.videoId}")
            },
            onBackClick = { navController.popBackStack() },
            onArtistClick = { channelId ->
                navController.navigate("channel?url=$channelId")
            }
        )
    }

    // Liked Videos Screen
    composable("likedVideos") {
        currentRoute.value = "likedVideos"
        showBottomNav.value = false
        LikedVideosScreen(
            onVideoClick = { track ->
                navController.navigate("player/${track.videoId}")
            },
            onBackClick = { navController.popBackStack() },
            onArtistClick = { channelId ->
                navController.navigate("channel?url=$channelId")
            }
        )
    }

    // Watch Later Screen
    composable("watchLater") {
        currentRoute.value = "watchLater"
        showBottomNav.value = false
        WatchLaterScreen(
            onBackClick = { navController.popBackStack() },
            onVideoClick = { video ->
                if (video.isShort && !disableShortsPlayer) {
                    navController.navigate("shorts?startVideoId=${video.id}")
                } else {
                    navController.navigate("player/${video.id}")
                }
            },
            onPlayPlaylist = { videos, index ->
                playerViewModel.playPlaylist(videos, index, "Watch Later")
            }
        )
    }

    // Playlists Screen
    composable("playlists") {
        currentRoute.value = "playlists"
        showBottomNav.value = false
        PlaylistsScreen(
            onBackClick = { navController.popBackStack() },
            onPlaylistClick = { playlist ->
                navController.navigate("playlist/${playlist.id}")
            },
            onNavigateToWatchLater = { navController.navigate("watchLater") },
            onNavigateToLikedVideos = { navController.navigate("likedVideos") }
        )
    }

    // Playlist Detail Screen
    composable("playlist/{playlistId}") { _ ->
        currentRoute.value = "playlist"
        showBottomNav.value = false
        PlaylistDetailScreen(
            // playlistId is handled by ViewModel via SavedStateHandle
            // playlistRepository is injected by Hilt
            onNavigateBack = { navController.popBackStack() },
            onVideoClick = { video ->
                if (video.isShort && !disableShortsPlayer) {
                    navController.navigate("shorts?startVideoId=${video.id}")
                } else {
                    navController.navigate("player/${video.id}")
                }
            },
            onPlayPlaylist = { videos, index ->
                playerViewModel.playPlaylist(videos, index, "Playlist")
            }
        )
    }

    // Saved Shorts Grid
    composable("savedShorts") {
        currentRoute.value = "savedShorts"
        showBottomNav.value = false
        com.echotube.iad1tya.ui.screens.library.SavedShortsGridScreen(
            onBackClick = { navController.popBackStack() },
            onVideoClick = { videoId ->
                navController.navigate("savedShortsPlayer/$videoId")
            }
        )
    }

    // Saved Shorts Player
    composable(
        route = "savedShortsPlayer/{startVideoId}",
        arguments = listOf(navArgument("startVideoId") { type = NavType.StringType })
    ) { backStackEntry ->
        currentRoute.value = "savedShortsPlayer"
        showBottomNav.value = false
        val startVideoId = backStackEntry.arguments?.getString("startVideoId")
        ShortsScreen(
            startVideoId = startVideoId,
            isSavedMode = true,
            onBack = {
                navController.popBackStack()
            },
            onChannelClick = { channelId ->
                navController.navigate("channel?url=$channelId")
            }
        )
    }
    composable("downloads") {
        currentRoute.value = "downloads"
        showBottomNav.value = false

        com.echotube.iad1tya.ui.screens.library.DownloadsScreen(
            onBackClick = { navController.popBackStack() },
            onVideoClick = { videos, index ->
                val videoList = videos.map { it.video }
                playerViewModel.playPlaylist(videoList, index, "Downloads")
                GlobalPlayerState.setCurrentVideo(videoList[index])
            },
            onHomeClick = {
                navController.navigate("home") {
                    popUpTo("home") { inclusive = true }
                }
            }
        )
    }
    composable(
        route = "player/{videoId}",
        arguments = listOf(navArgument("videoId") { type = NavType.StringType }),
        deepLinks = listOf(
            navDeepLink {
                uriPattern = "http://www.youtube.com/watch?v={videoId}"
                action = android.content.Intent.ACTION_VIEW
            },
            navDeepLink {
                uriPattern = "https://www.youtube.com/watch?v={videoId}"
                action = android.content.Intent.ACTION_VIEW
            },
            navDeepLink {
                uriPattern = "http://youtube.com/watch?v={videoId}"
                action = android.content.Intent.ACTION_VIEW
            },
            navDeepLink {
                uriPattern = "https://youtube.com/watch?v={videoId}"
                action = android.content.Intent.ACTION_VIEW
            },
            navDeepLink {
                uriPattern = "http://youtu.be/{videoId}"
                action = android.content.Intent.ACTION_VIEW
            },
            navDeepLink {
                uriPattern = "https://youtu.be/{videoId}"
                action = android.content.Intent.ACTION_VIEW
            },
            navDeepLink {
                uriPattern = "http://m.youtube.com/watch?v={videoId}"
                action = android.content.Intent.ACTION_VIEW
            },
            navDeepLink {
                uriPattern = "https://m.youtube.com/watch?v={videoId}"
                action = android.content.Intent.ACTION_VIEW
            },
            navDeepLink {
                uriPattern = "https://www.youtube.com/shorts/{videoId}"
                action = android.content.Intent.ACTION_VIEW
            },
            navDeepLink {
                uriPattern = "https://youtube.com/shorts/{videoId}"
                action = android.content.Intent.ACTION_VIEW
            }
        )
    ) { backStackEntry ->
        val videoId = backStackEntry.arguments?.getString("videoId")
        val effectiveVideoId = when {
            !videoId.isNullOrEmpty() && videoId != "sample" -> videoId
            else -> "jNQXAC9IVRw"
        }

        // Use passed state
        val playerUiState = playerUiStateResult.value
        val playerVisible = playerVisibleState.value

        LaunchedEffect(effectiveVideoId) {
            if (playerUiState.cachedVideo?.id != effectiveVideoId || !playerVisible) {
                val placeholder = Video(
                    id = effectiveVideoId,
                    title = "",
                    channelName = "",
                    channelId = "",
                    thumbnailUrl = "",
                    duration = 0,
                    viewCount = 0L,
                    uploadDate = "",
                    description = "",
                    channelThumbnailUrl = ""
                )
                playerViewModel.playVideo(placeholder)
                GlobalPlayerState.setCurrentVideo(placeholder)
            } else {
                playerSheetState.expand()
            }
            navController.popBackStack()
        }
        
        Box(modifier = Modifier.fillMaxSize())
    }
}