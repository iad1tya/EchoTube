package com.echotube.iad1tya.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.echotube.iad1tya.ui.theme.extendedColors
import androidx.compose.ui.res.vectorResource
import com.echotube.iad1tya.R
import androidx.compose.ui.res.stringResource


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNavigateToHistory: () -> Unit,
    onNavigateToPlaylists: () -> Unit,
    onNavigateToLikedVideos: () -> Unit,
    onNavigateToWatchLater: () -> Unit,
    onNavigateToSavedShorts: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onManageData: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    // Initialize view model
    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }
    
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.background
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(R.string.library),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                LibraryHeaderCard(
                    totalItems = uiState.watchHistoryCount + uiState.playlistsCount + uiState.likedVideosCount + uiState.watchLaterCount + uiState.savedShortsCount + uiState.downloadedVideosCount + uiState.downloadedSongsCount,
                    subtitle = context.getString(R.string.library_section_header)
                )
            }

            item { LibrarySectionLabel(context.getString(R.string.library_section_header)) }
            item {
                LibraryRow(
                    icon = Icons.Outlined.History,
                    title = context.getString(R.string.library_history_label),
                    subtitle = stringResource(R.string.videos_count_template, uiState.watchHistoryCount),
                    onClick = onNavigateToHistory
                )
            }

            item {
                LibraryRow(
                    icon = Icons.Outlined.PlaylistPlay,
                    title = context.getString(R.string.library_playlists_label),
                    subtitle = stringResource(R.string.playlists_count_template, uiState.playlistsCount),
                    onClick = onNavigateToPlaylists
                )
            }

            item {
                LibraryRow(
                    icon = Icons.Outlined.ThumbUp,
                    title = context.getString(R.string.library_liked_videos_label),
                    subtitle = stringResource(R.string.videos_count_template, uiState.likedVideosCount),
                    onClick = onNavigateToLikedVideos
                )
            }

            item {
                LibraryRow(
                    icon = ImageVector.vectorResource(id = R.drawable.ic_shorts),
                    title = context.getString(R.string.library_saved_shorts_label),
                    subtitle = stringResource(R.string.shorts_count_template, uiState.savedShortsCount),
                    onClick = onNavigateToSavedShorts
                )
            }

            item {
                LibraryRow(
                    icon = Icons.Outlined.WatchLater,
                    title = context.getString(R.string.library_watch_later_label),
                    subtitle = stringResource(R.string.videos_count_template, uiState.watchLaterCount),
                    onClick = onNavigateToWatchLater
                )
            }

            item {
                LibraryRow(
                    icon = Icons.Outlined.Download,
                    title = context.getString(R.string.library_downloads_label),
                    subtitle = buildString {
                        val v = uiState.downloadedVideosCount
                        val s = uiState.downloadedSongsCount
                        if (v == 0 && s == 0) {
                            append(context.getString(R.string.empty_downloads))
                        } else {
                            if (v > 0) append(context.getString(R.string.videos_count_template, v))
                            if (v > 0 && s > 0) append(" · ")
                            if (s > 0) append(context.getString(R.string.songs_count_template, s))
                        }
                    },
                    onClick = onNavigateToDownloads
                )
            }
            
            item { LibrarySectionLabel(context.getString(R.string.library_settings_data_header)) }
            item {
                LibraryRow(
                    icon = Icons.Outlined.Storage,
                    title = context.getString(R.string.library_manage_data_label),
                    subtitle = context.getString(R.string.library_manage_data_subtitle),
                    onClick = onManageData
                )
            }
        }
    }
}

@Composable
private fun LibraryHeaderCard(
    totalItems: Int,
    subtitle: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Your Library",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$totalItems items organized for quick access",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LibrarySectionLabel(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}
