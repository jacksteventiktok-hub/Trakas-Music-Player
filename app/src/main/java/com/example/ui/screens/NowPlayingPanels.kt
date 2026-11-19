package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.EqPreset
import com.example.model.Song
import com.example.model.TextScalePreset
import com.example.model.VisualizerStyle
import com.example.player.EqUiState
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LocalTrakasTokens
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingEqSheet(
    eqState: EqUiState,
    onDismiss: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onSelectPreset: (EqPreset) -> Unit,
    onBandChange: (Int, Int) -> Unit,
    onBassBoostChange: (Int) -> Unit,
    onVirtualizerChange: (Int) -> Unit,
    onBalanceChange: (Float) -> Unit,
    onBassToneChange: (Float) -> Unit,
    onTrebleToneChange: (Float) -> Unit,
    onOpenFullEqualizer: () -> Unit
) {
    val tokens = LocalTrakasTokens.current
    val strings = LocalAppStrings.current

    val playerPresets = listOf(
        EqPreset.FLAT,
        EqPreset.ROCK,
        EqPreset.POP,
        EqPreset.ELECTRONIC,
        EqPreset.HIP_HOP,
        EqPreset.CLASSICAL,
        EqPreset.VOCAL,
        EqPreset.BASS_BOOST,
        EqPreset.BASS_TREBLE
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = tokens.surfaceElevated,
        contentColor = tokens.textColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Tune, contentDescription = null, tint = tokens.primary)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(strings.equalizer, style = MaterialTheme.typography.titleLarge, color = tokens.textColor)
                        Text(
                            text = "Preset actual: ${eqState.preset.labelEs}",
                            style = MaterialTheme.typography.labelMedium,
                            color = tokens.primary
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {
                        onDismiss()
                        onOpenFullEqualizer()
                    }) {
                        Text("Panel completo", color = tokens.secondary)
                    }
                    Switch(
                        checked = eqState.enabled,
                        onCheckedChange = onToggleEnabled,
                        colors = SwitchDefaults.colors(checkedTrackColor = tokens.primary, checkedThumbColor = tokens.background)
                    )
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(playerPresets, key = { it.id }) { preset ->
                    FilterChip(
                        selected = eqState.preset == preset,
                        onClick = { onSelectPreset(preset) },
                        label = { Text(preset.labelEs) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = tokens.primary.copy(alpha = 0.24f),
                            selectedLabelColor = tokens.primary
                        )
                    )
                }
            }

            // 5-Band EQ
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(tokens.surfaceCard)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                eqState.bands.forEach { band ->
                    val freq = if (band.centerFreqHz >= 1000) {
                        String.format(Locale.US, "%.1f kHz", band.centerFreqHz / 1000f)
                    } else "${band.centerFreqHz} Hz"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(freq, style = MaterialTheme.typography.labelMedium, color = tokens.textColor, modifier = Modifier.width(64.dp))
                        Slider(
                            value = band.levelMb.toFloat(),
                            onValueChange = { onBandChange(band.index, it.toInt()) },
                            valueRange = band.minMb.toFloat()..band.maxMb.toFloat(),
                            enabled = eqState.enabled,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(thumbColor = tokens.primary, activeTrackColor = tokens.primary)
                        )
                        Text(
                            text = String.format(Locale.US, "%+.0fdB", band.levelMb / 100f),
                            style = MaterialTheme.typography.labelSmall,
                            color = tokens.primary,
                            modifier = Modifier.width(44.dp)
                        )
                    }
                }
            }

            // Bass Boost, Virtualizer, Balance, Bass & Treble
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(tokens.surfaceCard)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LabeledSliderRow(
                    label = strings.bassBoost,
                    valueText = "${eqState.bassBoostStrength / 10}%",
                    value = eqState.bassBoostStrength.toFloat(),
                    range = 0f..1000f,
                    enabled = eqState.enabled,
                    onValueChange = { onBassBoostChange(it.toInt()) }
                )
                LabeledSliderRow(
                    label = strings.virtualizer,
                    valueText = "${eqState.virtualizerStrength / 10}%",
                    value = eqState.virtualizerStrength.toFloat(),
                    range = 0f..1000f,
                    enabled = eqState.enabled,
                    onValueChange = { onVirtualizerChange(it.toInt()) }
                )
                val balStr = when {
                    eqState.balance < -0.08f -> "L ${(-eqState.balance * 100).toInt()}%"
                    eqState.balance > 0.08f -> "R ${(eqState.balance * 100).toInt()}%"
                    else -> "Centro"
                }
                LabeledSliderRow(
                    label = strings.balanceLR,
                    valueText = balStr,
                    value = eqState.balance,
                    range = -1f..1f,
                    enabled = true,
                    onValueChange = onBalanceChange
                )
                HorizontalDivider(color = tokens.textColor.copy(alpha = 0.08f))
                LabeledSliderRow(
                    label = strings.bassLabel,
                    valueText = String.format(Locale.US, "%+.1f dB", eqState.bassDb),
                    value = eqState.bassDb,
                    range = -12f..12f,
                    enabled = eqState.enabled,
                    onValueChange = onBassToneChange
                )
                LabeledSliderRow(
                    label = strings.trebleLabel,
                    valueText = String.format(Locale.US, "%+.1f dB", eqState.trebleDb),
                    value = eqState.trebleDb,
                    range = -12f..12f,
                    enabled = eqState.enabled,
                    onValueChange = onTrebleToneChange
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun LabeledSliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    onValueChange: (Float) -> Unit
) {
    val tokens = LocalTrakasTokens.current
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = tokens.textColor)
            Text(valueText, style = MaterialTheme.typography.labelMedium, color = tokens.secondary)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            enabled = enabled,
            colors = SliderDefaults.colors(thumbColor = tokens.secondary, activeTrackColor = tokens.secondary)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoverOptionsSheet(
    song: Song,
    onDismiss: () -> Unit,
    onUpdateCoverArt: (String?) -> Unit,
    onOpenMetadataEditor: () -> Unit
) {
    val tokens = LocalTrakasTokens.current
    val strings = LocalAppStrings.current
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onUpdateCoverArt(uri.toString())
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = tokens.surfaceElevated,
        contentColor = tokens.textColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Opciones de portada", style = MaterialTheme.typography.titleLarge, color = tokens.textColor)
            Text(song.title, style = MaterialTheme.typography.bodySmall, color = tokens.mutedTextColor)
            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(tokens.surfaceCard)
                    .clickable {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                    .padding(14.dp)
                    .testTag("cover_pick_gallery_btn"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Image, contentDescription = null, tint = tokens.primary)
                Spacer(Modifier.width(12.dp))
                Text(strings.changeCoverArt, color = tokens.textColor, style = MaterialTheme.typography.titleSmall)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(tokens.surfaceCard)
                    .clickable {
                        onUpdateCoverArt(null)
                        onDismiss()
                    }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Restore, contentDescription = null, tint = tokens.secondary)
                Spacer(Modifier.width(12.dp))
                Text("Restaurar portada original", color = tokens.textColor, style = MaterialTheme.typography.titleSmall)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(tokens.surfaceCard)
                    .clickable {
                        onDismiss()
                        onOpenMetadataEditor()
                    }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Edit, contentDescription = null, tint = tokens.textColor)
                Spacer(Modifier.width(12.dp))
                Text(strings.editMetadata, color = tokens.textColor, style = MaterialTheme.typography.titleSmall)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerDisplayOptionsSheet(
    dynamicBgEnabled: Boolean,
    visualizerEnabled: Boolean,
    visualizerStyle: VisualizerStyle,
    immersiveMode: Boolean,
    textScaleFactor: Float,
    volume: Float,
    playbackSpeed: Float,
    sleepTimerRemainingSec: Int?,
    smartShuffleEnabled: Boolean,
    onDismiss: () -> Unit,
    onToggleDynamicBg: (Boolean) -> Unit,
    onToggleVisualizer: (Boolean) -> Unit,
    onSelectVisualizerStyle: (VisualizerStyle) -> Unit,
    onToggleImmersive: (Boolean) -> Unit,
    onSelectTextScale: (Float) -> Unit,
    onSetVolume: (Float) -> Unit,
    onCycleSpeed: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onToggleSmartShuffle: () -> Unit
) {
    val tokens = LocalTrakasTokens.current
    val strings = LocalAppStrings.current
    val currentScale = TextScalePreset.fromScale(textScaleFactor)
    val allowedVisStyles = listOf(
        VisualizerStyle.SPECTRUM,
        VisualizerStyle.WAVEFORM,
        VisualizerStyle.BARS,
        VisualizerStyle.CIRCLE,
        VisualizerStyle.PARTICLES
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = tokens.surfaceElevated,
        contentColor = tokens.textColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Personalización del Reproductor", style = MaterialTheme.typography.titleLarge, color = tokens.textColor)

            // Dynamic Background toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(tokens.surfaceCard)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${strings.dynamicBackground}: ${if (dynamicBgEnabled) "Activado" else "Desactivado"}",
                        style = MaterialTheme.typography.titleSmall,
                        color = tokens.textColor
                    )
                    Text(
                        text = "Colores de la portada, desenfoque e iluminación ambiental",
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.mutedTextColor
                    )
                }
                Switch(
                    checked = dynamicBgEnabled,
                    onCheckedChange = onToggleDynamicBg,
                    modifier = Modifier.testTag("player_dynamic_bg_switch"),
                    colors = SwitchDefaults.colors(checkedTrackColor = tokens.primary, checkedThumbColor = tokens.background)
                )
            }

            // Immersive Player toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(tokens.surfaceCard)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Fullscreen, contentDescription = null, tint = tokens.primary)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(strings.immersivePlayer, style = MaterialTheme.typography.titleSmall, color = tokens.textColor)
                        Text("Maximiza portada y visualizador en pantalla completa", style = MaterialTheme.typography.bodySmall, color = tokens.mutedTextColor)
                    }
                }
                Switch(
                    checked = immersiveMode,
                    onCheckedChange = {
                        onToggleImmersive(it)
                        if (it) onDismiss()
                    },
                    modifier = Modifier.testTag("player_immersive_switch"),
                    colors = SwitchDefaults.colors(checkedTrackColor = tokens.primary, checkedThumbColor = tokens.background)
                )
            }

            // Visualizer toggle + styles
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(tokens.surfaceCard)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = tokens.secondary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${strings.visualizer}: ${if (visualizerEnabled) "Activado" else "Desactivado"}",
                            style = MaterialTheme.typography.titleSmall,
                            color = tokens.textColor
                        )
                    }
                    Switch(
                        checked = visualizerEnabled,
                        onCheckedChange = onToggleVisualizer,
                        modifier = Modifier.testTag("player_visualizer_switch"),
                        colors = SwitchDefaults.colors(checkedTrackColor = tokens.secondary, checkedThumbColor = tokens.background)
                    )
                }
                if (visualizerEnabled) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(allowedVisStyles, key = { it.id }) { style ->
                            FilterChip(
                                selected = visualizerStyle == style,
                                onClick = { onSelectVisualizerStyle(style) },
                                label = { Text(style.labelEs) }
                            )
                        }
                    }
                }
            }

            // Accessibility Text Size
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(tokens.surfaceCard)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(strings.textSize, style = MaterialTheme.typography.titleSmall, color = tokens.textColor)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextScalePreset.entries.forEach { preset ->
                        FilterChip(
                            selected = currentScale == preset,
                            onClick = { onSelectTextScale(preset.scale) },
                            label = { Text(preset.labelEs, maxLines = 1) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Volume, Smart Shuffle, Speed & Timer
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = strings.volume, tint = tokens.mutedTextColor)
                Spacer(Modifier.width(10.dp))
                Slider(
                    value = volume,
                    onValueChange = onSetVolume,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = tokens.primary, activeTrackColor = tokens.primary)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onToggleSmartShuffle, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.AutoAwesome, null, modifier = Modifier.size(16.dp), tint = if (smartShuffleEnabled) tokens.primary else tokens.mutedTextColor)
                    Spacer(Modifier.width(4.dp))
                    Text("Smart", maxLines = 1)
                }
                OutlinedButton(onClick = onCycleSpeed, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Speed, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(String.format(Locale.US, "%.2gx", playbackSpeed), maxLines = 1)
                }
                OutlinedButton(onClick = {
                    onDismiss()
                    onOpenSleepTimer()
                }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Bedtime, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (sleepTimerRemainingSec != null) "${sleepTimerRemainingSec / 60}m" else "Timer", maxLines = 1)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
