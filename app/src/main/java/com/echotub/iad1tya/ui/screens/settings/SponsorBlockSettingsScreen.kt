package com.echotube.iad1tya.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.echotube.iad1tya.R
import com.echotube.iad1tya.data.local.PlayerPreferences
import com.echotube.iad1tya.data.local.SponsorBlockAction
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SponsorBlockSettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val playerPreferences = remember { PlayerPreferences(context) }

    val sponsorBlockEnabled by playerPreferences.sponsorBlockEnabled.collectAsState(initial = false)
    val sbSubmitEnabled by playerPreferences.sbSubmitEnabled.collectAsState(initial = false)
    val sbUserId by playerPreferences.sbUserId.collectAsState(initial = null)

    val sbCategoriesAndLabels = listOf(
        "sponsor" to R.string.sb_category_sponsor,
        "intro" to R.string.sb_category_intro,
        "outro" to R.string.sb_category_outro,
        "selfpromo" to R.string.sb_category_selfpromo,
        "interaction" to R.string.sb_category_interaction,
        "music_offtopic" to R.string.sb_category_music_offtopic,
        "filler" to R.string.sb_category_filler,
        "preview" to R.string.sb_category_preview,
        "exclusive_access" to R.string.sb_category_exclusive_access
    )

    val sbActions = sbCategoriesAndLabels.associate { (category, _) ->
        category to playerPreferences.sbActionForCategory(category).collectAsState(initial = SponsorBlockAction.SKIP).value
    }
    val sbColors = sbCategoriesAndLabels.associate { (category, _) ->
        category to playerPreferences.sbColorForCategory(category).collectAsState(initial = null).value
    }

    var showUserIdDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
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
                            text = stringResource(R.string.player_settings_sponsorblock),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            item {
                SettingsGroup {
                    SettingsSwitchItem(
                        icon = painterResource(R.drawable.ic_block),
                        title = stringResource(R.string.player_settings_sponsorblock),
                        subtitle = stringResource(R.string.player_settings_sponsorblock_subtitle),
                        checked = sponsorBlockEnabled,
                        onCheckedChange = { enabled ->
                            coroutineScope.launch {
                                playerPreferences.setSponsorBlockEnabled(enabled)
                            }
                        }
                    )
                }
            }

            item {
                SectionHeader(text = stringResource(R.string.sb_segments_header))
                SettingsGroup {
                    sbCategoriesAndLabels.forEachIndexed { index, (category, labelRes) ->
                        SponsorBlockCategoryRow(
                            label = stringResource(labelRes),
                            selectedAction = sbActions[category] ?: SponsorBlockAction.SKIP,
                            customColorArgb = sbColors[category],
                            onActionSelected = { action ->
                                coroutineScope.launch {
                                    playerPreferences.setSbActionForCategory(category, action)
                                }
                            },
                            onColorChanged = { colorArgb ->
                                coroutineScope.launch {
                                    playerPreferences.setSbColorForCategory(category, colorArgb)
                                }
                            }
                        )
                        if (index < sbCategoriesAndLabels.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }

            item {
                SectionHeader(text = stringResource(R.string.sb_contribute_header))
                SettingsGroup {
                    SettingsSwitchItem(
                        icon = painterResource(R.drawable.ic_block),
                        title = stringResource(R.string.sb_contribute_toggle_title),
                        subtitle = stringResource(R.string.sb_contribute_toggle_subtitle),
                        checked = sbSubmitEnabled,
                        onCheckedChange = { enabled ->
                            coroutineScope.launch {
                                playerPreferences.setSbSubmitEnabled(enabled)
                            }
                        }
                    )
                    if (sbSubmitEnabled) {
                        HorizontalDivider(Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showUserIdDialog = true }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.sb_user_id_title),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = sbUserId?.let { it.take(8) + "..." }
                                        ?: stringResource(R.string.sb_user_id_not_set),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Outlined.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showUserIdDialog) {
        var inputId by remember { mutableStateOf(sbUserId ?: "") }
        AlertDialog(
            onDismissRequest = { showUserIdDialog = false },
            title = { Text(stringResource(R.string.sb_user_id_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.sb_user_id_dialog_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = inputId,
                        onValueChange = { inputId = it },
                        label = { Text(stringResource(R.string.sb_user_id_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    coroutineScope.launch {
                        val id = inputId.trim().ifBlank { playerPreferences.getOrCreateSbUserId() }
                        playerPreferences.setSbUserId(id)
                    }
                    showUserIdDialog = false
                }) { Text(stringResource(R.string.btn_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showUserIdDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SponsorBlockCategoryRow(
    label: String,
    selectedAction: SponsorBlockAction,
    customColorArgb: Int?,
    onActionSelected: (SponsorBlockAction) -> Unit,
    onColorChanged: (Int?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }

    val presetColors = remember {
        listOf(
            Color(0xFF00D400),
            Color(0xFFFFFF00),
            Color(0xFF0000FF),
            Color(0xFFFF0000),
            Color(0xFFFF7700),
            Color(0xFFFF69B4),
            Color(0xFF7700FF),
            Color(0xFF00FFFF),
            Color(0xFFFFFFFF),
            Color(0xFF008080),
            Color(0xFF3F51B5),
            Color(0xFFFFC107),
            Color(0xFFCDDC39),
            Color(0xFF673AB7),
            Color(0xFFFF5722),
            Color(0xFFE91E63),
            Color(0xFF006400),
            Color(0xFF8B4513),
            Color(0xFF808080),
            Color(0xFFC0C0C0),
            Color(0xFFFFD700),
            Color(0xFF40E0D0),
            Color(0xFF4B0082)
        )
    }

    if (showColorPicker) {
        Dialog(onDismissRequest = { showColorPicker = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.sb_color_picker_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        presetColors.forEach { color ->
                            val isSelected = customColorArgb == color.toArgb()
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable {
                                        onColorChanged(color.toArgb())
                                        showColorPicker = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.Black.copy(alpha = 0.7f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                    TextButton(
                        onClick = {
                            onColorChanged(null)
                            showColorPicker = false
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(stringResource(R.string.sb_color_reset))
                    }
                }
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val swatchColor = customColorArgb?.let { Color(it) } ?: MaterialTheme.colorScheme.primary
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(swatchColor)
                .clickable { showColorPicker = true }
        )
        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )

        Box {
            Row(
                modifier = Modifier
                    .clickable { expanded = true }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = actionLabel(selectedAction),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.Outlined.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                SponsorBlockAction.values().forEach { action ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = actionLabel(action),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        onClick = {
                            onActionSelected(action)
                            expanded = false
                        },
                        trailingIcon = if (action == selectedAction) ({
                            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                        }) else null
                    )
                }
            }
        }
    }
}

@Composable
private fun actionLabel(action: SponsorBlockAction): String = when (action) {
    SponsorBlockAction.SKIP -> stringResource(R.string.sb_action_skip)
    SponsorBlockAction.MUTE -> stringResource(R.string.sb_action_mute)
    SponsorBlockAction.SHOW_TOAST -> stringResource(R.string.sb_action_show_toast)
    SponsorBlockAction.IGNORE -> stringResource(R.string.sb_action_ignore)
}
