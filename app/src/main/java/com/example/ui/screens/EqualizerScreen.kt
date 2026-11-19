package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.EqPreset
import com.example.player.EqUiState
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LocalTrakasTokens
import java.util.Locale

@Composable
fun EqualizerScreen(
    eqState: EqUiState,
    onToggleEnabled: (Boolean) -> Unit,
    onSelectPreset: (EqPreset) -> Unit,
    onBandChange: (Int, Int) -> Unit,
    onBassBoostChange: (Int) -> Unit,
    onVirtualizerChange: (Int) -> Unit,
    onBalanceChange: (Float) -> Unit,
    onBassToneChange: (Float) -> Unit,
    onTrebleToneChange: (Float) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val tokens = LocalTrakasTokens.current
    val strings = LocalAppStrings.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = tokens.primary)
                    }
                }
                Text(strings.equalizer, style = MaterialTheme.typography.headlineMedium, color = tokens.textColor)
            }
            Switch(
                checked = eqState.enabled,
                onCheckedChange = onToggleEnabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = tokens.background,
                    checkedTrackColor = tokens.primary
                ),
                modifier = Modifier.testTag("eq_master_switch")
            )
        }

        // Real-time EQ Curve Graph
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(tokens.surfaceCard)
                .border(1.dp, tokens.primary.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                .padding(16.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val midY = h / 2f
                drawLine(
                    color = tokens.textColor.copy(alpha = 0.14f),
                    start = Offset(0f, midY),
                    end = Offset(w, midY),
                    strokeWidth = 2f
                )
                val bands = eqState.bands
                if (bands.isNotEmpty()) {
                    val path = Path()
                    val step = w / (bands.size - 1).coerceAtLeast(1)
                    val range = (eqState.maxBandMb - eqState.minBandMb).coerceAtLeast(1).toFloat()
                    bands.forEachIndexed { idx, band ->
                        val norm = ((band.levelMb - eqState.minBandMb) / range).coerceIn(0f, 1f)
                        val x = idx * step
                        val y = h - (norm * h)
                        if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        drawCircle(
                            color = if (eqState.enabled) tokens.secondary else tokens.mutedTextColor,
                            radius = 7f,
                            center = Offset(x, y)
                        )
                    }
                    drawPath(
                        path = path,
                        brush = Brush.horizontalGradient(listOf(tokens.primary, tokens.secondary)),
                        style = Stroke(width = 5f, cap = StrokeCap.Round)
                    )
                }
            }
        }

        // Presets
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(strings.presets, style = MaterialTheme.typography.titleMedium, color = tokens.textColor)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(EqPreset.entries, key = { it.id }) { preset ->
                    val selected = eqState.preset == preset
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectPreset(preset) },
                        label = { Text(preset.labelEs) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = tokens.primary.copy(alpha = 0.25f),
                            selectedLabelColor = tokens.primary,
                            containerColor = tokens.surfaceCard,
                            labelColor = tokens.mutedTextColor
                        )
                    )
                }
            }
        }

        // 5-Band Sliders
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(tokens.surfaceCard)
                .border(1.dp, tokens.textColor.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            eqState.bands.forEach { band ->
                val freqLabel = if (band.centerFreqHz >= 1000) {
                    String.format(Locale.US, "%.1f kHz", band.centerFreqHz / 1000f)
                } else {
                    "${band.centerFreqHz} Hz"
                }
                val db = band.levelMb / 100f
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(freqLabel, style = MaterialTheme.typography.labelLarge, color = tokens.textColor)
                        Text(
                            text = String.format(Locale.US, "%+.1f dB", db),
                            style = MaterialTheme.typography.labelMedium,
                            color = tokens.primary
                        )
                    }
                    Slider(
                        value = band.levelMb.toFloat(),
                        onValueChange = { onBandChange(band.index, it.toInt()) },
                        valueRange = band.minMb.toFloat()..band.maxMb.toFloat(),
                        enabled = eqState.enabled,
                        colors = SliderDefaults.colors(
                            thumbColor = tokens.primary,
                            activeTrackColor = tokens.primary,
                            inactiveTrackColor = tokens.textColor.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }

        // Bass & Treble Tone Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(tokens.surfaceCard)
                .border(1.dp, tokens.primary.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(strings.bassAndTreble, style = MaterialTheme.typography.titleMedium, color = tokens.primary)
                Text(
                    text = "0 dB",
                    style = MaterialTheme.typography.labelMedium,
                    color = tokens.mutedTextColor,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(tokens.surfaceElevated)
                        .clickable {
                            onBassToneChange(0f)
                            onTrebleToneChange(0f)
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("eq_bass_treble_reset")
                )
            }

            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(strings.bassLabel, style = MaterialTheme.typography.titleSmall, color = tokens.textColor)
                    Text(
                        text = String.format(Locale.US, "%+.1f dB", eqState.bassDb),
                        style = MaterialTheme.typography.labelMedium,
                        color = tokens.secondary
                    )
                }
                Slider(
                    value = eqState.bassDb,
                    onValueChange = onBassToneChange,
                    valueRange = -12f..12f,
                    enabled = eqState.enabled,
                    colors = SliderDefaults.colors(
                        thumbColor = tokens.secondary,
                        activeTrackColor = tokens.secondary,
                        inactiveTrackColor = tokens.textColor.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("eq_bass_tone_slider")
                )
            }

            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(strings.trebleLabel, style = MaterialTheme.typography.titleSmall, color = tokens.textColor)
                    Text(
                        text = String.format(Locale.US, "%+.1f dB", eqState.trebleDb),
                        style = MaterialTheme.typography.labelMedium,
                        color = tokens.primary
                    )
                }
                Slider(
                    value = eqState.trebleDb,
                    onValueChange = onTrebleToneChange,
                    valueRange = -12f..12f,
                    enabled = eqState.enabled,
                    colors = SliderDefaults.colors(
                        thumbColor = tokens.primary,
                        activeTrackColor = tokens.primary,
                        inactiveTrackColor = tokens.textColor.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("eq_treble_tone_slider")
                )
            }
        }

        // Bass Boost, Virtualizer, Stereo Balance
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(tokens.surfaceCard)
                .border(1.dp, tokens.textColor.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(strings.bassBoost, style = MaterialTheme.typography.titleSmall, color = tokens.textColor)
                    Text("${eqState.bassBoostStrength / 10}%", style = MaterialTheme.typography.labelMedium, color = tokens.secondary)
                }
                Slider(
                    value = eqState.bassBoostStrength.toFloat(),
                    onValueChange = { onBassBoostChange(it.toInt()) },
                    valueRange = 0f..1000f,
                    enabled = eqState.enabled,
                    colors = SliderDefaults.colors(thumbColor = tokens.secondary, activeTrackColor = tokens.secondary)
                )
            }

            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(strings.virtualizer, style = MaterialTheme.typography.titleSmall, color = tokens.textColor)
                    Text("${eqState.virtualizerStrength / 10}%", style = MaterialTheme.typography.labelMedium, color = tokens.primary)
                }
                Slider(
                    value = eqState.virtualizerStrength.toFloat(),
                    onValueChange = { onVirtualizerChange(it.toInt()) },
                    valueRange = 0f..1000f,
                    enabled = eqState.enabled,
                    colors = SliderDefaults.colors(thumbColor = tokens.primary, activeTrackColor = tokens.primary)
                )
            }

            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(strings.balanceLR, style = MaterialTheme.typography.titleSmall, color = tokens.textColor)
                    val balLabel = when {
                        eqState.balance < -0.08f -> "L ${(-eqState.balance * 100).toInt()}%"
                        eqState.balance > 0.08f -> "R ${(eqState.balance * 100).toInt()}%"
                        else -> "CENTER"
                    }
                    Text(balLabel, style = MaterialTheme.typography.labelMedium, color = tokens.primary)
                }
                Slider(
                    value = eqState.balance,
                    onValueChange = onBalanceChange,
                    valueRange = -1f..1f,
                    colors = SliderDefaults.colors(thumbColor = tokens.textColor, activeTrackColor = tokens.primary)
                )
            }
        }

        Spacer(Modifier.height(92.dp))
    }
}
