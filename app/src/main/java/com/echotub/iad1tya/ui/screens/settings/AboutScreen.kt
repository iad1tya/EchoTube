package com.echotube.iad1tya.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echotube.iad1tya.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDonations: () -> Unit
) {
    val context = LocalContext.current
    var showLicenseDialog by remember { mutableStateOf(false) }
    var showDeviceInfoDialog by remember { mutableStateOf(false) }
    // version info
    val packageInfo = remember {
        try { context.packageManager.getPackageInfo(context.packageName, 0) }
        catch (e: Exception) { null }
    }
    val versionName = packageInfo?.versionName ?: "Unknown"
    val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo?.longVersionCode?.toString() ?: "0"
    } else {
        @Suppress("DEPRECATION")
        packageInfo?.versionCode?.toString() ?: "0"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.btn_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                windowInsets = WindowInsets(0)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                AboutHeroCard(versionName = versionName, versionCode = versionCode)
            }

            item {
                AboutSupportSection(onDonateClick = onNavigateToDonations)
            }

            item {
                AboutSectionContainer(title = stringResource(R.string.section_contact)) {
                    AboutRow(
                        icon = Icons.Outlined.Public,
                        title = stringResource(R.string.about_website),
                        subtitle = "echotube.fun",
                        onClick = { openUrl(context, "https://echotube.fun") }
                    )
                    AboutRowDivider()
                    AboutRow(
                        icon = Icons.Outlined.Email,
                        title = "Email",
                        subtitle = "hello@echotube.fun",
                        onClick = { openUrl(context, "mailto:hello@echotube.fun") }
                    )
                    AboutRowDivider()
                    AboutRowWithPainter(
                        iconPainter = painterResource(id = R.drawable.ic_github),
                        title = stringResource(R.string.github_label),
                        subtitle = stringResource(R.string.github_subtitle),
                        onClick = { openUrl(context, "https://github.com/iad1tya/EchoTube") }
                    )
                    AboutRowDivider()
                    AboutRowWithPainter(
                        iconPainter = painterResource(id = R.drawable.ic_discord),
                        title = "Discord",
                        subtitle = "discord.gg/d6VPTS5Y4W",
                        onClick = { openUrl(context, "https://discord.gg/d6VPTS5Y4W") }
                    )
                    AboutRowDivider()
                    AboutRowWithPainter(
                        iconPainter = painterResource(id = R.drawable.ic_telegram),
                        title = "Telegram",
                        subtitle = "t.me/EchoTubeApp",
                        onClick = { openUrl(context, "https://t.me/EchoTubeApp") }
                    )
                }
            }

            item {
                AboutSectionContainer(title = "Developer") {
                    AboutRow(
                        icon = Icons.Outlined.Person,
                        title = stringResource(R.string.about_creator),
                        subtitle = "Aditya",
                        onClick = { openUrl(context, "https://iad1tya.cyou") }
                    )
                    AboutRowDivider()
                    AboutRowWithPainter(
                        iconPainter = painterResource(id = R.drawable.ic_instagram),
                        title = "Instagram",
                        subtitle = "instagram.com/iad1tya",
                        onClick = { openUrl(context, "https://instagram.com/iad1tya") }
                    )
                    AboutRowDivider()
                    AboutRowWithPainter(
                        iconPainter = painterResource(id = R.drawable.ic_x),
                        title = "X",
                        subtitle = "x.com/xad1tya",
                        onClick = { openUrl(context, "https://x.com/xad1tya") }
                    )
                }
            }

            item {
                AboutSectionContainer(title = stringResource(R.string.section_legal)) {
                    AboutRow(
                        icon = Icons.Outlined.Description,
                        title = stringResource(R.string.about_license),
                        subtitle = "GNU GPL v3",
                        onClick = { showLicenseDialog = true }
                    )
                    AboutRowDivider()
                    AboutRow(
                        icon = Icons.Outlined.Extension,
                        title = stringResource(R.string.newpipe_extractor_title),
                        subtitle = stringResource(R.string.newpipe_extractor_subtitle),
                        onClick = { openUrl(context, "https://github.com/TeamNewPipe/NewPipeExtractor") }
                    )
                }
            }

            item {
                AboutSectionContainer(title = stringResource(R.string.section_device)) {
                    AboutRow(
                        icon = Icons.Outlined.Smartphone,
                        title = stringResource(R.string.about_device_info),
                        subtitle = "${Build.MANUFACTURER} ${Build.MODEL}",
                        onClick = { showDeviceInfoDialog = true }
                    )
                }
            }
        }
    }

    if (showLicenseDialog) LicenseDialog(onDismiss = { showLicenseDialog = false })
    if (showDeviceInfoDialog) DeviceInfoDialog(onDismiss = { showDeviceInfoDialog = false })
}


@Composable
private fun AboutSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall.copy(letterSpacing = 0.2.sp),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
    )
}

@Composable
private fun AboutHeroCard(versionName: String, versionCode: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = R.drawable.icon),
            contentDescription = null,
            modifier = Modifier
                .size(94.dp)
                .padding(top = 10.dp),
            tint = Color.Unspecified
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = stringResource(id = R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "v$versionName ($versionCode)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AboutSectionContainer(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        AboutSectionLabel(title)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun AboutSupportSection(onDonateClick: () -> Unit) {
    val context = LocalContext.current

    AboutSectionContainer(title = "Support") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            AboutRowWithPainterWhiteIcon(
                iconPainter = painterResource(id = R.drawable.ic_upi),
                title = "UPI",
                subtitle = "iad1tya@upi",
                onClick = { openUrl(context, "upi://pay?pa=iad1tya@upi") }
            )
            AboutRowDivider()
            AboutRowWithPainterWhiteIcon(
                iconPainter = painterResource(id = R.drawable.ic_buy_me_coffee),
                title = "Buy Me a Coffee",
                subtitle = "buymeacoffee.com/iad1tya",
                onClick = { openUrl(context, "https://buymeacoffee.com/iad1tya") }
            )
            AboutRowDivider()
            AboutRowWithPainterWhiteIcon(
                iconPainter = painterResource(id = R.drawable.ic_github),
                title = "GitHub Sponsor",
                subtitle = "github.com/sponsors/iad1tya",
                onClick = { openUrl(context, "https://github.com/sponsors/iad1tya") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            FilledTonalButton(
                onClick = onDonateClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.VolunteerActivism,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Open Donations")
            }
        }
    }
}

@Composable
private fun AboutRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun AboutRowWithPainter(
    iconPainter: Painter,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = iconPainter,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun AboutRowWithPainterWhiteIcon(
    iconPainter: Painter,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val iconTint = if (isDarkTheme) Color.White else Color.Black

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = iconPainter,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun AboutRowWithVector(
    iconVector: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = iconVector,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun AboutRowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 54.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}


@Composable
fun LicenseDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var licenseText by remember { mutableStateOf(context.getString(R.string.loading_ellipsis)) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                context.assets.open("license.txt").bufferedReader().use {
                    licenseText = it.readText()
                }
            } catch (e: Exception) {
                licenseText = context.getString(R.string.error_license_load)
                e.printStackTrace()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.gnu_license_full_title)) },
        text = {
            Box(Modifier.heightIn(max = 400.dp)) {
                LazyColumn {
                    item {
                        Text(
                            text = licenseText,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    )
}

@Composable
fun DeviceInfoDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    val deviceInfo = remember {
        buildString {
            append(context.getString(R.string.manufacturer_label, Build.MANUFACTURER) + "\n")
            append(context.getString(R.string.model_label, Build.MODEL) + "\n")
            append(context.getString(R.string.board_label, Build.BOARD) + "\n")
            append(context.getString(R.string.arch_label, Build.SUPPORTED_ABIS.joinToString(", ")) + "\n")
            append(context.getString(R.string.android_sdk_label, Build.VERSION.SDK_INT.toString()) + "\n")
            append(context.getString(R.string.os_label, Build.VERSION.RELEASE) + "\n")
            append(context.getString(R.string.density_label, android.content.res.Resources.getSystem().displayMetrics.density.toString()) + "\n")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.about_device_info)) },
        text = {
            Text(
                text = deviceInfo,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_ok)) }
        },
        dismissButton = {
            TextButton(onClick = {
                clipboardManager.setText(AnnotatedString(deviceInfo))
            }) { Text(stringResource(R.string.btn_copy)) }
        }
    )
}

@Composable
fun ChangelogDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var changelogText by remember { mutableStateOf(context.getString(R.string.loading_ellipsis)) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val assetManager = context.assets
                val files = assetManager.list("changelog") ?: emptyArray()
                val latestFile = files.filter { it.endsWith(".txt") }
                    .sortedWith(compareByDescending { it })
                    .firstOrNull()

                if (latestFile != null) {
                    assetManager.open("changelog/$latestFile").bufferedReader().use {
                        changelogText = it.readText()
                    }
                } else {
                    changelogText = context.getString(R.string.no_changelog_found_message)
                }
            } catch (e: Exception) {
                changelogText = context.getString(R.string.error_changelog_load)
                e.printStackTrace()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.about_changelog)) },
        text = {
            Box(Modifier.heightIn(max = 400.dp)) {
                LazyColumn {
                    item {
                        Text(
                            text = changelogText,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    )
}

private fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// Compat shim — kept for any existing call sites
@Composable
fun CustomIconSettingsItem(
    iconPainter: Painter,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) = AboutRowWithPainter(iconPainter = iconPainter, title = title, subtitle = subtitle, onClick = onClick)
