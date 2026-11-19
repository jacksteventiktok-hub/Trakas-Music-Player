package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BluetoothAudio
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.AppPreferences
import com.example.model.VisualizerConfig
import com.example.player.AudioOutputStatus
import com.example.player.EqUiState
import com.example.player.PlaybackUiState
import com.example.player.VisualizerFrame
import com.example.ui.components.ArtworkImage
import com.example.ui.components.AudioVisualizerCanvas
import com.example.ui.components.formatDuration
import com.example.ui.components.rememberDynamicArtColors
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LocalTrakasTokens
import com.example.ui.theme.contrastText
import java.util.Locale

@Composable
fun PartyModeScreen(
    playbackState: PlaybackUiState,
    visualizerFrame: VisualizerFrame,
    visualizerConfig: VisualizerConfig,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onCycleVisualizerStyle: () -> Unit,
    onExit: () -> Unit
) {
    BackHandler(onBack = onExit)
    val tokens = LocalTrakasTokens.current
    val song = playbackState.currentSong
    val albumColors = rememberDynamicArtColors(song?.albumArtUri, tokens.primary, tokens.secondary)
    val peak = visualizerFrame.peakLevel.coerceIn(0f, 1f)
    val pulseColor by animateColorAsState(
        targetValue = if (peak > 0.45f) tokens.secondary else tokens.primary,
        label = "partyGlow"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(pulseColor.copy(alpha = 0.35f + peak * 0.3f), tokens.background)
                )
            )
            .systemBarsPadding()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("TRAKAS PARTY", style = MaterialTheme.typography.headlineSmall, color = pulseColor)
            IconButton(onClick = onExit, modifier = Modifier.testTag("party_exit_btn")) {
                Icon(Icons.Rounded.Close, contentDescription = "Salir de Party", tint = Color.White)
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ArtworkImage(
                artUri = song?.albumArtUri,
                contentDescription = song?.title,
                modifier = Modifier
                    .size(168.dp)
                    .clip(CircleShape)
                    .border(3.dp, pulseColor, CircleShape)
            )
            Spacer(Modifier.height(14.dp))
            Text(song?.title ?: "Listo para la fiesta", style = MaterialTheme.typography.headlineMedium, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song?.artist ?: "Trakas Music Player", style = MaterialTheme.typography.titleMedium, color = pulseColor, maxLines = 1)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color.Black.copy(alpha = 0.45f))
                .border(1.5.dp, pulseColor.copy(alpha = 0.55f), RoundedCornerShape(22.dp))
                .clickable(onClick = onCycleVisualizerStyle)
                .padding(14.dp)
        ) {
            AudioVisualizerCanvas(
                frame = visualizerFrame,
                config = visualizerConfig.copy(intensity = 1.35f),
                isPlaying = playbackState.isPlaying,
                albumColors = albumColors
            )
            Text(
                text = "Tocar para cambiar visualizador: ${visualizerConfig.style.labelEs}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onSkipPrevious, modifier = Modifier.size(64.dp)) {
                Icon(Icons.Rounded.SkipPrevious, contentDescription = "Anterior", tint = Color.White, modifier = Modifier.size(44.dp))
            }
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(pulseColor)
                    .clickable(onClick = onTogglePlayPause),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.Black,
                    modifier = Modifier.size(46.dp)
                )
            }
            IconButton(onClick = onSkipNext, modifier = Modifier.size(64.dp)) {
                Icon(Icons.Rounded.SkipNext, contentDescription = "Siguiente", tint = Color.White, modifier = Modifier.size(44.dp))
            }
        }
    }
}

@Composable
fun DriveModeScreen(
    playbackState: PlaybackUiState,
    isFavorite: Boolean,
    audioOutputStatus: AudioOutputStatus,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSetVolume: (Float) -> Unit,
    onExit: () -> Unit
) {
    BackHandler(onBack = onExit)
    val song = playbackState.currentSong
    val accent = Color(0xFF00E5FF)
    val highContrastBg = Color(0xFF000000)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(highContrastBg)
            .systemBarsPadding()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.DirectionsCar, contentDescription = null, tint = accent, modifier = Modifier.size(28.dp))
                Text("TRAKAS DRIVE", style = MaterialTheme.typography.titleLarge, color = accent, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = if (audioOutputStatus.isBluetooth) Icons.Rounded.BluetoothAudio else Icons.Rounded.Speaker,
                    contentDescription = audioOutputStatus.deviceName,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                IconButton(onClick = onExit, modifier = Modifier.size(48.dp).testTag("drive_exit_btn")) {
                    Icon(Icons.Rounded.Close, contentDescription = "Salir de Modo Auto", tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            ArtworkImage(
                artUri = song?.albumArtUri,
                contentDescription = song?.title,
                modifier = Modifier
                    .size(160.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(2.dp, accent, RoundedCornerShape(24.dp))
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = song?.title ?: "Sin canción activa",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = song?.artist ?: audioOutputStatus.deviceName,
                style = MaterialTheme.typography.headlineSmall,
                color = accent,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${formatDuration(playbackState.positionMs)} / ${formatDuration(playbackState.durationMs)}",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
        }

        // Extra-large high-contrast car transport buttons (>= 72dp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(78.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A2234))
                    .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                    .clickable(onClick = onSkipPrevious)
                    .testTag("drive_prev_btn"),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.SkipPrevious, contentDescription = "Anterior", tint = Color.White, modifier = Modifier.size(48.dp))
            }

            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(CircleShape)
                    .background(accent)
                    .clickable(onClick = onTogglePlayPause)
                    .testTag("drive_play_pause_btn"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.Black,
                    modifier = Modifier.size(62.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(78.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A2234))
                    .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                    .clickable(onClick = onSkipNext)
                    .testTag("drive_next_btn"),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.SkipNext, contentDescription = "Siguiente", tint = Color.White, modifier = Modifier.size(48.dp))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A2234))
                    .testTag("drive_fav_btn")
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Favorito",
                    tint = if (isFavorite) Color(0xFFFF4081) else Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null, tint = accent, modifier = Modifier.size(28.dp))
            Slider(
                value = playbackState.volume,
                onValueChange = onSetVolume,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent, inactiveTrackColor = Color.DarkGray)
            )
        }
    }
}

@Composable
fun StudioModeScreen(
    playbackState: PlaybackUiState,
    prefs: AppPreferences,
    eqState: EqUiState,
    visualizerFrame: VisualizerFrame,
    visualizerConfig: VisualizerConfig,
    audioOutputStatus: AudioOutputStatus,
    onOpenEqualizer: () -> Unit,
    onExit: () -> Unit
) {
    BackHandler(onBack = onExit)
    val tokens = LocalTrakasTokens.current
    val strings = LocalAppStrings.current
    val song = playbackState.currentSong

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onExit) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver", tint = tokens.primary)
                }
                Text("TRAKAS STUDIO", style = MaterialTheme.typography.headlineSmall, color = tokens.primary)
            }
            Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = tokens.secondary)
        }

        // High-precision Visualizer Panel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(tokens.surfaceCard)
                .border(1.dp, tokens.primary.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                .padding(12.dp)
        ) {
            AudioVisualizerCanvas(
                frame = visualizerFrame,
                config = visualizerConfig,
                isPlaying = playbackState.isPlaying
            )
        }

        // Technical Telemetry Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(tokens.surfaceCard)
                .border(1.dp, tokens.textColor.copy(alpha = 0.1f), RoundedCornerShape(18.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Monitorización de Audio", style = MaterialTheme.typography.titleMedium, color = tokens.primary)
            StudioLine("Pista activa", song?.title ?: "---")
            StudioLine("Formato / Calidad", "${song?.format ?: "---"} • ${prefs.audioQuality.labelEs}")
            StudioLine("Dispositivo de salida", audioOutputStatus.deviceName)
            StudioLine("Velocidad de reproducción", String.format(Locale.US, "%.2fx", playbackState.playbackSpeed))
            StudioLine("Normalización / ReplayGain", "${if (prefs.normalizeVolume) "ON" else "OFF"} / ${if (prefs.replayGainEnabled) "ON" else "OFF"}")
            StudioLine("Crossfade / Gapless", "${prefs.crossfadeSeconds}s / ${if (prefs.gaplessPlayback) "ON" else "OFF"}")
            StudioLine("EQ / Bass & Treble", "${eqState.preset.labelEs} • Bass ${eqState.bassDb}dB • Treble ${eqState.trebleDb}dB")
        }

        Button(
            onClick = onOpenEqualizer,
            colors = ButtonDefaults.buttonColors(containerColor = tokens.buttonColor, contentColor = tokens.buttonColor.contrastText()),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Rounded.Equalizer, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(strings.equalizer)
        }
    }
}

@Composable
private fun StudioLine(label: String, value: String) {
    val tokens = LocalTrakasTokens.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = tokens.mutedTextColor)
        Text(value, style = MaterialTheme.typography.labelLarge, color = tokens.textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
