package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BluetoothAudio
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.AppPreferences
import com.example.model.AppLanguage
import com.example.model.AudioQuality
import com.example.model.ButtonShapeStyle
import com.example.model.FolderGroup
import com.example.model.IconStyle
import com.example.model.TextScalePreset
import com.example.model.TrakasThemeConfig
import com.example.model.VisualizerStyle
import com.example.player.AudioOutputStatus
import com.example.ui.components.TrakasIconType
import com.example.ui.components.TrakasStyledIcon
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LocalTrakasTokens
import com.example.ui.theme.contrastText
import java.util.Locale
import java.util.UUID

@Composable
fun SettingsScreen(
    prefs: AppPreferences,
    themes: List<TrakasThemeConfig>,
    activeTheme: TrakasThemeConfig,
    folders: List<FolderGroup>,
    audioOutputStatus: AudioOutputStatus,
    onSelectTheme: (TrakasThemeConfig) -> Unit,
    onSaveCustomTheme: (TrakasThemeConfig) -> Unit,
    onDeleteCustomTheme: (String) -> Unit,
    onSetIconStyle: (IconStyle) -> Unit,
    onSetVisualizerStyle: (VisualizerStyle) -> Unit,
    onSetVisualizerIntensity: (Float) -> Unit,
    onSetVisualizerSpeed: (Float) -> Unit,
    onSetVisualizerSensitivity: (Float) -> Unit,
    onSetVisualizerAlpha: (Float) -> Unit,
    onSetVisualizerParticles: (Int) -> Unit,
    onSetVisualizerUseAlbumColors: (Boolean) -> Unit,
    onSetAudioQuality: (AudioQuality) -> Unit,
    onSetGapless: (Boolean) -> Unit,
    onSetCrossfade: (Int) -> Unit,
    onSetNormalizeVolume: (Boolean) -> Unit,
    onSetReplayGain: (Boolean) -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    onSetAnimations: (Boolean) -> Unit,
    onSetNotifications: (Boolean) -> Unit,
    onSetLockscreen: (Boolean) -> Unit,
    onSetBluetoothAutoPlay: (Boolean) -> Unit,
    onSetLanguage: (AppLanguage) -> Unit,
    onToggleFolderExcluded: (String) -> Unit,
    onRescanLibrary: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onExportBackup: (Boolean, Boolean, Boolean) -> Unit,
    onImportBackup: () -> Unit,
    onSetPlayerDynamicBg: (Boolean) -> Unit,
    onSetPlayerVisualizerEnabled: (Boolean) -> Unit,
    onSetImmersivePlayer: (Boolean) -> Unit,
    onSetTextScaleFactor: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalTrakasTokens.current
    val strings = LocalAppStrings.current

    var showCustomThemeDialog by remember { mutableStateOf(false) }
    var showFoldersDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(strings.navSettings, style = MaterialTheme.typography.headlineMedium, color = tokens.textColor)
        }

        // 1. Themes (10 Built-in + Extended Custom Theme Creator)
        item {
            SettingsCard(title = strings.themes) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(themes, key = { it.id }) { theme ->
                        val selected = theme.id == activeTheme.id
                        Column(
                            modifier = Modifier
                                .width(120.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(theme.backgroundArgb))
                                .border(
                                    width = if (selected) 2.5.dp else 1.dp,
                                    color = if (selected) Color(theme.primaryArgb) else Color.White.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { onSelectTheme(theme) }
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(Modifier.size(14.dp).clip(CircleShape).background(Color(theme.primaryArgb)))
                                    Box(Modifier.size(14.dp).clip(CircleShape).background(Color(theme.secondaryArgb)))
                                }
                                if (!theme.isBuiltIn) {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = "Delete",
                                        tint = Color(theme.secondaryArgb),
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { onDeleteCustomTheme(theme.id) }
                                    )
                                } else if (selected) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, tint = Color(theme.primaryArgb), modifier = Modifier.size(16.dp))
                                }
                            }
                            Text(
                                text = theme.name,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(theme.textArgb),
                                maxLines = 1
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { showCustomThemeDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = tokens.buttonColor,
                        contentColor = tokens.buttonColor.contrastText()
                    ),
                    shape = RoundedCornerShape(tokens.buttonCornerDp.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_custom_theme_creator")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.customThemeCreator)
                }
            }
        }

        // 2. Icon Style Selector (6 Styles)
        item {
            SettingsCard(title = strings.icons) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(IconStyle.entries, key = { it.id }) { style ->
                        val selected = prefs.iconStyle == style
                        FilterChip(
                            selected = selected,
                            onClick = { onSetIconStyle(style) },
                            label = {
                                Text(if (prefs.language == AppLanguage.SPANISH) style.labelEs else style.labelEn)
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = tokens.primary.copy(alpha = 0.25f),
                                selectedLabelColor = tokens.primary
                            )
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(TrakasIconType.entries) { iconType ->
                        TrakasStyledIcon(
                            type = iconType,
                            contentDescription = iconType.name,
                            active = true,
                            showBadgeContainer = true,
                            size = 20.dp
                        )
                    }
                }
            }
        }

        // 3. Visualizer Style + Tuning Sliders + Equalizer Shortcut
        item {
            SettingsCard(title = strings.visualizer) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(VisualizerStyle.entries, key = { it.id }) { style ->
                        val selected = prefs.visualizerStyle == style
                        FilterChip(
                            selected = selected,
                            onClick = { onSetVisualizerStyle(style) },
                            label = {
                                Text(if (prefs.language == AppLanguage.SPANISH) style.labelEs else style.labelEn)
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = tokens.secondary.copy(alpha = 0.25f),
                                selectedLabelColor = tokens.secondary
                            )
                        )
                    }
                }
                SettingsSwitchRow(
                    "${strings.dynamicBackground}: ${if (prefs.playerDynamicBackground) "Activado" else "Desactivado"}",
                    prefs.playerDynamicBackground,
                    onSetPlayerDynamicBg
                )
                SettingsSwitchRow(strings.visualizer, prefs.playerVisualizerEnabled, onSetPlayerVisualizerEnabled)
                SettingsSwitchRow(strings.immersivePlayer, prefs.immersivePlayerMode, onSetImmersivePlayer)
                SettingsSwitchRow(strings.useAlbumColors, prefs.visualizerUseAlbumColors, onSetVisualizerUseAlbumColors)
                Column {
                    Text(strings.textSize, style = MaterialTheme.typography.bodyLarge, color = tokens.textColor)
                    Spacer(Modifier.height(6.dp))
                    val activePreset = TextScalePreset.fromScale(prefs.textScaleFactor)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextScalePreset.entries.forEach { preset ->
                            FilterChip(
                                selected = activePreset == preset,
                                onClick = { onSetTextScaleFactor(preset.scale) },
                                label = {
                                    Text(if (prefs.language == AppLanguage.SPANISH) preset.labelEs else preset.labelEn)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                SettingsSliderRow("Intensidad", "${(prefs.visualizerIntensity * 100).toInt()}%", prefs.visualizerIntensity, 0.2f..1.5f, onSetVisualizerIntensity)
                SettingsSliderRow("Velocidad visual", String.format(Locale.US, "%.1fx", prefs.visualizerSpeed), prefs.visualizerSpeed, 0.4f..2.0f, onSetVisualizerSpeed)
                SettingsSliderRow("Sensibilidad", String.format(Locale.US, "%.1fx", prefs.visualizerSensitivity), prefs.visualizerSensitivity, 0.4f..2.0f, onSetVisualizerSensitivity)
                SettingsSliderRow("Transparencia", "${(prefs.visualizerAlpha * 100).toInt()}%", prefs.visualizerAlpha, 0.2f..1.0f, onSetVisualizerAlpha)
                SettingsSliderRow("Partículas", "${prefs.visualizerParticleCount}", prefs.visualizerParticleCount.toFloat(), 10f..80f) {
                    onSetVisualizerParticles(it.toInt())
                }
                OutlinedButton(onClick = onOpenEqualizer, modifier = Modifier.fillMaxWidth()) {
                    TrakasStyledIcon(TrakasIconType.EQUALIZER, null, active = true, size = 18.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.equalizer)
                }
            }
        }

        // 4. Playback & Audio Engine
        item {
            SettingsCard(title = strings.audioQuality) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AudioQuality.entries, key = { it.id }) { q ->
                        FilterChip(
                            selected = prefs.audioQuality == q,
                            onClick = { onSetAudioQuality(q) },
                            label = {
                                Text(if (prefs.language == AppLanguage.SPANISH) q.labelEs else q.labelEn)
                            }
                        )
                    }
                }
                SettingsSwitchRow(strings.gaplessPlayback, prefs.gaplessPlayback, onSetGapless)
                SettingsSwitchRow(strings.normalizeVolume, prefs.normalizeVolume, onSetNormalizeVolume)
                SettingsSwitchRow(strings.replayGain, prefs.replayGainEnabled, onSetReplayGain)
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(strings.crossfade, style = MaterialTheme.typography.bodyMedium, color = tokens.textColor)
                        Text("${prefs.crossfadeSeconds}s", style = MaterialTheme.typography.labelMedium, color = tokens.primary)
                    }
                    Slider(
                        value = prefs.crossfadeSeconds.toFloat(),
                        onValueChange = { onSetCrossfade(it.toInt()) },
                        valueRange = 0f..12f,
                        steps = 11,
                        colors = SliderDefaults.colors(thumbColor = tokens.primary, activeTrackColor = tokens.primary)
                    )
                }
                SettingsSliderRow(
                    label = strings.speed,
                    valueLabel = String.format(Locale.US, "%.2fx", prefs.playbackSpeed),
                    value = prefs.playbackSpeed,
                    range = 0.5f..2.0f,
                    onChange = onSetPlaybackSpeed
                )
                SettingsSwitchRow(strings.animations, prefs.animationsEnabled, onSetAnimations)
            }
        }

        // 5. Bluetooth & Hardware Output + Library Folders
        item {
            SettingsCard(title = strings.musicFolders) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(tokens.surfaceElevated)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (audioOutputStatus.isBluetooth) Icons.Rounded.BluetoothAudio else Icons.Rounded.Speaker,
                        contentDescription = strings.outputDevice,
                        tint = tokens.primary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.outputDevice, style = MaterialTheme.typography.labelSmall, color = tokens.mutedTextColor)
                        Text(audioOutputStatus.deviceName, style = MaterialTheme.typography.titleSmall, color = tokens.textColor)
                    }
                }
                SettingsSwitchRow(strings.bluetoothAutoPlay, prefs.bluetoothAutoPlay, onSetBluetoothAutoPlay)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(onClick = onRescanLibrary, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.scanLibrary, maxLines = 1)
                    }
                    OutlinedButton(onClick = { showFoldersDialog = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("${strings.folders} (${folders.size})", maxLines = 1)
                    }
                }
                SettingsSwitchRow(strings.notifications, prefs.notificationsEnabled, onSetNotifications)
                SettingsSwitchRow(strings.lockscreen, prefs.lockscreenControlsEnabled, onSetLockscreen)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.language, style = MaterialTheme.typography.bodyLarge, color = tokens.textColor)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppLanguage.entries.forEach { lang ->
                            FilterChip(
                                selected = prefs.language == lang,
                                onClick = { onSetLanguage(lang) },
                                label = { Text(lang.label) }
                            )
                        }
                    }
                }
            }
        }

        // 6. Backup & Restore
        item {
            SettingsCard(title = strings.backupAndRestore) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { showBackupDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = tokens.buttonColor, contentColor = tokens.buttonColor.contrastText()),
                        modifier = Modifier.weight(1f).testTag("export_backup_btn")
                    ) {
                        Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Exportar JSON", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onImportBackup,
                        modifier = Modifier.weight(1f).testTag("import_backup_btn")
                    ) {
                        Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Importar JSON", maxLines = 1)
                    }
                }
            }
        }

        // 7. About App
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(tokens.surfaceCard)
                    .border(1.dp, tokens.primary.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                    .clickable { showAboutDialog = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Info, contentDescription = strings.about, tint = tokens.primary)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(strings.about, style = MaterialTheme.typography.titleMedium, color = tokens.textColor)
                    Text("${strings.appName} • ${strings.slogan}", style = MaterialTheme.typography.bodySmall, color = tokens.mutedTextColor)
                }
            }
        }

        item { Spacer(Modifier.height(90.dp)) }
    }

    if (showCustomThemeDialog) {
        CustomThemeCreatorDialog(
            baseTheme = activeTheme,
            onDismiss = { showCustomThemeDialog = false },
            onSave = {
                onSaveCustomTheme(it)
                showCustomThemeDialog = false
            }
        )
    }

    if (showBackupDialog) {
        var incPlaylists by remember { mutableStateOf(true) }
        var incThemes by remember { mutableStateOf(true) }
        var incSettings by remember { mutableStateOf(true) }
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            containerColor = tokens.surfaceElevated,
            title = { Text(strings.backupAndRestore, color = tokens.textColor) },
            text = {
                Column {
                    Row(Modifier.fillMaxWidth().clickable { incPlaylists = !incPlaylists }, verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = incPlaylists, onCheckedChange = { incPlaylists = it })
                        Text(strings.playlists, color = tokens.textColor)
                    }
                    Row(Modifier.fillMaxWidth().clickable { incThemes = !incThemes }, verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = incThemes, onCheckedChange = { incThemes = it })
                        Text(strings.themes, color = tokens.textColor)
                    }
                    Row(Modifier.fillMaxWidth().clickable { incSettings = !incSettings }, verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = incSettings, onCheckedChange = { incSettings = it })
                        Text(strings.navSettings, color = tokens.textColor)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showBackupDialog = false
                    onExportBackup(incPlaylists, incThemes, incSettings)
                }) { Text("Exportar", color = tokens.primary) }
            },
            dismissButton = {
                TextButton(onClick = { showBackupDialog = false }) { Text(strings.cancel, color = tokens.mutedTextColor) }
            }
        )
    }

    if (showFoldersDialog) {
        AlertDialog(
            onDismissRequest = { showFoldersDialog = false },
            containerColor = tokens.surfaceElevated,
            title = { Text(strings.musicFolders, color = tokens.textColor) },
            text = {
                if (folders.isEmpty()) {
                    Text(strings.noMusicTitle, color = tokens.mutedTextColor)
                } else {
                    LazyColumn(modifier = Modifier.height(260.dp)) {
                        items(folders, key = { it.path }) { folder ->
                            val included = folder.path !in prefs.excludedFolders
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleFolderExcluded(folder.path) }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = included, onCheckedChange = { onToggleFolderExcluded(folder.path) })
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(folder.name, color = tokens.textColor)
                                    Text("${folder.songs.size} pistas", style = MaterialTheme.typography.bodySmall, color = tokens.mutedTextColor)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFoldersDialog = false }) { Text("OK", color = tokens.primary) }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            containerColor = tokens.surfaceElevated,
            title = { Text(strings.appName, color = tokens.textColor) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("“${strings.slogan}”", style = MaterialTheme.typography.titleSmall, color = tokens.primary)
                    Text("Versión 2.0 • Reproductor musical local con Media3 ExoPlayer, Android Auto, EQ 5 Bandas + Bass & Treble, Letras Karaoke, Trakas AI, Estadísticas y Temas Personalizados.", color = tokens.mutedTextColor)
                    Text("Formatos soportados: MP3, FLAC, WAV, AAC, OGG, M4A.", color = tokens.mutedTextColor)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("OK", color = tokens.primary) }
            }
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    val tokens = LocalTrakasTokens.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(tokens.surfaceCard)
            .border(1.dp, tokens.textColor.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = tokens.primary)
        content()
    }
}

@Composable
private fun SettingsSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val tokens = LocalTrakasTokens.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = tokens.textColor, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = tokens.background,
                checkedTrackColor = tokens.primary
            )
        )
    }
}

@Composable
private fun SettingsSliderRow(
    label: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    val tokens = LocalTrakasTokens.current
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = tokens.textColor)
            Text(valueLabel, style = MaterialTheme.typography.labelMedium, color = tokens.primary)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(thumbColor = tokens.primary, activeTrackColor = tokens.primary)
        )
    }
}

@Composable
private fun CustomThemeCreatorDialog(
    baseTheme: TrakasThemeConfig,
    onDismiss: () -> Unit,
    onSave: (TrakasThemeConfig) -> Unit
) {
    val tokens = LocalTrakasTokens.current
    val strings = LocalAppStrings.current

    var name by remember { mutableStateOf("Mi Tema Trakas") }
    var primaryArgb by remember { mutableLongStateOf(baseTheme.primaryArgb) }
    var secondaryArgb by remember { mutableLongStateOf(baseTheme.secondaryArgb) }
    var backgroundArgb by remember { mutableLongStateOf(baseTheme.backgroundArgb) }
    var buttonArgb by remember { mutableLongStateOf(baseTheme.buttonArgb) }
    var textArgb by remember { mutableLongStateOf(baseTheme.textArgb) }
    var cardArgb by remember { mutableLongStateOf(baseTheme.cardArgb) }
    var progressArgb by remember { mutableLongStateOf(baseTheme.progressArgb) }
    var visualizerArgb by remember { mutableLongStateOf(baseTheme.visualizerArgb) }
    var cardAlpha by remember { mutableFloatStateOf(baseTheme.cardAlpha) }
    var buttonShape by remember { mutableStateOf(ButtonShapeStyle.fromId(baseTheme.buttonShapeId)) }
    var effectIntensity by remember { mutableFloatStateOf(baseTheme.effectIntensity) }
    var visStyle by remember { mutableStateOf(VisualizerStyle.fromId(baseTheme.visualizerStyleId)) }
    var animEnabled by remember { mutableStateOf(baseTheme.animationsEnabled) }

    val accentSwatches = listOf(0xFF00E5FF, 0xFFFF2E93, 0xFF00E676, 0xFFB388FF, 0xFFFF6D00, 0xFF2979FF, 0xFFFFD600, 0xFFFFFFFF)
    val bgSwatches = listOf(0xFF090D16, 0xFF000000, 0xFF0E0718, 0xFF06110B, 0xFF140B12, 0xFF071020)
    val cardSwatches = listOf(0xFF131A2C, 0xFF0B0D12, 0xFF1B102E, 0xFF0E2116, 0xFF24131E, 0xFF18181F)
    val textSwatches = listOf(0xFFF3F7FF, 0xFFFFFFFF, 0xFFCCFFEA, 0xFFFFE6F2, 0xFFB8D4FF)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tokens.surfaceElevated,
        title = { Text(strings.customThemeCreator, color = tokens.textColor) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Nombre del tema") }
                )

                ColorSwatchRow("Color principal", accentSwatches, primaryArgb) { primaryArgb = it }
                ColorSwatchRow("Color secundario", accentSwatches, secondaryArgb) { secondaryArgb = it }
                ColorSwatchRow("Color de fondo", bgSwatches, backgroundArgb) { backgroundArgb = it }
                ColorSwatchRow("Color de tarjetas", cardSwatches, cardArgb) { cardArgb = it }
                ColorSwatchRow("Color de botones", accentSwatches, buttonArgb) { buttonArgb = it }
                ColorSwatchRow("Color del texto", textSwatches, textArgb) { textArgb = it }
                ColorSwatchRow("Color barra progreso", accentSwatches, progressArgb) { progressArgb = it }
                ColorSwatchRow("Color visualizador", accentSwatches, visualizerArgb) { visualizerArgb = it }

                Text("Transparencia: ${(cardAlpha * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = tokens.textColor)
                Slider(value = cardAlpha, onValueChange = { cardAlpha = it }, valueRange = 0.4f..0.98f)

                Text("Intensidad de efectos: ${(effectIntensity * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = tokens.textColor)
                Slider(value = effectIntensity, onValueChange = { effectIntensity = it }, valueRange = 0.3f..1.2f)

                SettingsSwitchRow("Animaciones en este tema", animEnabled) { animEnabled = it }

                Text("Forma de botones", style = MaterialTheme.typography.labelSmall, color = tokens.textColor)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ButtonShapeStyle.entries.forEach { shape ->
                        FilterChip(
                            selected = buttonShape == shape,
                            onClick = { buttonShape = shape },
                            label = { Text(shape.labelEs) }
                        )
                    }
                }

                Text(strings.visualizer, style = MaterialTheme.typography.labelSmall, color = tokens.textColor)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(VisualizerStyle.entries) { vs ->
                        FilterChip(
                            selected = visStyle == vs,
                            onClick = { visStyle = vs },
                            label = { Text(vs.labelEs) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    TrakasThemeConfig(
                        id = "custom_${UUID.randomUUID()}",
                        name = name.ifBlank { "Tema Personalizado" },
                        isBuiltIn = false,
                        primaryArgb = primaryArgb,
                        secondaryArgb = secondaryArgb,
                        backgroundArgb = backgroundArgb,
                        buttonArgb = buttonArgb,
                        textArgb = textArgb,
                        cardArgb = cardArgb,
                        progressArgb = progressArgb,
                        visualizerArgb = visualizerArgb,
                        cardAlpha = cardAlpha,
                        buttonShapeId = buttonShape.id,
                        effectIntensity = effectIntensity,
                        visualizerStyleId = visStyle.id,
                        animationsEnabled = animEnabled
                    )
                )
            }) {
                Text(strings.saveTheme, color = tokens.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel, color = tokens.mutedTextColor)
            }
        }
    )
}

@Composable
private fun ColorSwatchRow(
    label: String,
    swatches: List<Long>,
    selectedArgb: Long,
    onSelect: (Long) -> Unit
) {
    val tokens = LocalTrakasTokens.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = tokens.textColor)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(swatches) { argb ->
                val selected = argb == selectedArgb
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(argb))
                        .border(if (selected) 3.dp else 1.dp, if (selected) Color.White else Color.Gray, CircleShape)
                        .clickable { onSelect(argb) }
                )
            }
        }
    }
}
