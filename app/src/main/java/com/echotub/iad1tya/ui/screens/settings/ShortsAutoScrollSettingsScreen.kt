package com.echotube.iad1tya.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.echotube.iad1tya.R
import com.echotube.iad1tya.data.local.PlayerPreferences
import com.echotube.iad1tya.data.local.ShortsAutoScrollMode
import kotlinx.coroutines.launch

@Composable
fun ShortsAutoScrollSettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val preferences = remember(context) { PlayerPreferences(context) }

    val enabled by preferences.shortsAutoScrollEnabled.collectAsState(initial = false)
    val mode by preferences.shortsAutoScrollMode.collectAsState(initial = ShortsAutoScrollMode.FIXED_INTERVAL)
    val intervalSeconds by preferences.shortsAutoScrollIntervalSeconds.collectAsState(initial = 10)

    var sliderValue by remember(intervalSeconds) { mutableFloatStateOf(intervalSeconds.toFloat()) }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.background
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.btn_back))
                    }
                    Text(
                        text = stringResource(R.string.shorts_auto_scroll_settings_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.shorts_auto_scroll_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                SettingsGroup {
                    SettingsSwitchItem(
                        icon = Icons.Outlined.Schedule,
                        title = stringResource(R.string.shorts_auto_scroll),
                        subtitle = stringResource(R.string.shorts_auto_scroll_toggle_subtitle),
                        checked = enabled,
                        onCheckedChange = { isEnabled ->
                            coroutineScope.launch { preferences.setShortsAutoScrollEnabled(isEnabled) }
                        }
                    )
                }
            }

            item {
                SectionHeader(text = stringResource(R.string.shorts_auto_scroll_mode_title))
            }

            item {
                SettingsGroup {
                    AutoScrollModeItem(
                        title = stringResource(R.string.shorts_auto_scroll_mode_fixed),
                        subtitle = stringResource(R.string.shorts_auto_scroll_mode_fixed_subtitle),
                        selected = mode == ShortsAutoScrollMode.FIXED_INTERVAL,
                        onClick = {
                            coroutineScope.launch {
                                preferences.setShortsAutoScrollMode(ShortsAutoScrollMode.FIXED_INTERVAL)
                            }
                        }
                    )
                    HorizontalDivider(
                        Modifier.padding(start = 56.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                    AutoScrollModeItem(
                        title = stringResource(R.string.shorts_auto_scroll_mode_completion),
                        subtitle = stringResource(R.string.shorts_auto_scroll_mode_completion_subtitle),
                        selected = mode == ShortsAutoScrollMode.VIDEO_COMPLETION,
                        onClick = {
                            coroutineScope.launch {
                                preferences.setShortsAutoScrollMode(ShortsAutoScrollMode.VIDEO_COMPLETION)
                            }
                        }
                    )
                }
            }

            item {
                SectionHeader(text = stringResource(R.string.shorts_auto_scroll_interval_title))
            }

            item {
                SettingsGroup {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Text(
                            text = stringResource(R.string.shorts_auto_scroll_interval_value, intervalSeconds),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = sliderValue,
                            onValueChange = { value ->
                                sliderValue = value
                            },
                            onValueChangeFinished = {
                                val rounded = sliderValue.toInt().coerceIn(5, 20)
                                sliderValue = rounded.toFloat()
                                coroutineScope.launch {
                                    preferences.setShortsAutoScrollIntervalSeconds(rounded)
                                }
                            },
                            valueRange = 5f..20f,
                            steps = 14,
                            enabled = mode == ShortsAutoScrollMode.FIXED_INTERVAL
                        )
                        Text(
                            text = stringResource(R.string.shorts_auto_scroll_interval_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AutoScrollModeItem(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}
