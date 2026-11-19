package com.example.ui.screens

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BluetoothAudio
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.DisplaySettings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.EqPreset
import com.example.model.RepeatModeOption
import com.example.model.Song
import com.example.model.VisualizerConfig
import com.example.model.VisualizerStyle
import com.example.player.AudioOutputStatus
import com.example.player.EqUiState
import com.example.player.PlaybackUiState
import com.example.player.VisualizerFrame
import com.example.ui.components.ArtworkImage
import com.example.ui.components.AudioVisualizerCanvas
import com.example.ui.components.formatDuration
import com.example.ui.components.rememberDynamicArtColors
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LocalTrakasTokens
import com.example.ui.theme.contrastText
import kotlinx.coroutines.delay

@Composable
fun NowPlayingScreen(
    playbackState: PlaybackUiState,
    eqState: EqUiState,
    isFavorite: Boolean,
    visualizerFrame: VisualizerFrame,
    visualizerConfig: VisualizerConfig,
    audioOutputStatus: AudioOutputStatus,
    dynamicBackgroundEnabled: Boolean,
    visualizerEnabled: Boolean,
    immersiveMode: Boolean,
    textScaleFactor: Float,
    onToggleDynamicBackground: (Boolean) -> Unit,
    onToggleVisualizerEnabled: (Boolean) -> Unit,
    onSelectVisualizerStyle: (VisualizerStyle) -> Unit,
    onCycleVisualizerStyle: () -> Unit,
    onToggleImmersiveMode: (Boolean) -> Unit,
    onSelectTextScale: (Float) -> Unit,
    onMinimize: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleSmartShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onSetVolume: (Float) -> Unit,
    onCycleSpeed: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenFullEqualizer: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenMetadataEditor: () -> Unit,
    onOpenMoreOptions: (Song) -> Unit,
    onUpdateCoverArt: (Song, String?) -> Unit,
    onArtistClick: (String) -> Unit,
    onAlbumClick: (String) -> Unit,
    onEqToggleEnabled: (Boolean) -> Unit,
    onEqSelectPreset: (EqPreset) -> Unit,
    onEqBandChange: (Int, Int) -> Unit,
    onEqBassBoostChange: (Int) -> Unit,
    onEqVirtualizerChange: (Int) -> Unit,
    onEqBalanceChange: (Float) -> Unit,
    onEqBassToneChange: (Float) -> Unit,
    onEqTrebleToneChange: (Float) -> Unit
) {
    BackHandler {
        if (immersiveMode) onToggleImmersiveMode(false) else onMinimize()
    }

    val tokens = LocalTrakasTokens.current
    val strings = LocalAppStrings.current
    val song = playbackState.currentSong

    val (rawColor1, rawColor2) = rememberDynamicArtColors(
        artUri = song?.albumArtUri,
        fallbackPrimary = tokens.primary,
        fallbackSecondary = tokens.secondary
    )
    val artColor1 by animateColorAsState(
        targetValue = if (dynamicBackgroundEnabled) rawColor1 else tokens.primary,
        animationSpec = tween(durationMillis = 550),
        label = "bgColor1"
    )
    val artColor2 by animateColorAsState(
        targetValue = if (dynamicBackgroundEnabled) rawColor2 else tokens.secondary,
        animationSpec = tween(durationMillis = 550),
        label = "bgColor2"
    )

    var showEqSheet by remember { mutableStateOf(false) }
    var showCoverOptions by remember { mutableStateOf(false) }
    var showDisplayOptions by remember { mutableStateOf(false) }
    var showHeartBurst by remember { mutableStateOf(false) }

    LaunchedEffect(showHeartBurst) {
        if (showHeartBurst) {
            delay(650L)
            showHeartBurst = false
        }
    }

    // Subtle breathing / ambient pulse animation while playing, pauses cleanly when paused
    val infiniteTransition = rememberInfiniteTransition(label = "trakasAmbient")
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambientPulse"
    )
    val targetArtScale = if (playbackState.isPlaying && tokens.animationsEnabled) {
        1.0f + (0.026f * pulseProgress)
    } else 1.0f
    val smoothArtScale by animateFloatAsState(
        targetValue = targetArtScale,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "artScale"
    )

    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.background)
            .pointerInput(song?.id) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (dragX < -90f) onSkipNext()
                        else if (dragX > 90f) onSkipPrevious()
                        dragX = 0f
                    },
                    onHorizontalDrag = { _, amt -> dragX += amt }
                )
            }
            .pointerInput(immersiveMode) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (dragY > 110f) {
                            if (immersiveMode) onToggleImmersiveMode(false) else onMinimize()
                        } else if (dragY < -110f && !immersiveMode) {
                            onOpenQueue()
                        }
                        dragY = 0f
                    },
                    onVerticalDrag = { _, amt -> dragY += amt }
                )
            }
    ) {
        // Dynamic Blurred Cover Art & Ambient Lighting Layer
        if (dynamicBackgroundEnabled && song != null) {
            ArtworkImage(
                artUri = song.albumArtUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(60.dp) else Modifier)
                    .alpha(0.32f)
            )
            Canvas(modifier = Modifier.fillMaxSize()) {
                val glowAlpha = if (playbackState.isPlaying && tokens.animationsEnabled) {
                    0.26f + 0.10f * pulseProgress
                } else 0.22f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(artColor1.copy(alpha = glowAlpha), Color.Transparent),
                        center = Offset(size.width * 0.28f, size.height * 0.22f),
                        radius = size.maxDimension * 0.55f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(artColor2.copy(alpha = glowAlpha * 0.9f), Color.Transparent),
                        center = Offset(size.width * 0.78f, size.height * 0.68f),
                        radius = size.maxDimension * 0.50f
                    )
                )
            }
        }

        // Signature Dark Vignette & Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            if (dynamicBackgroundEnabled) artColor1.copy(alpha = 0.22f) else tokens.background,
                            tokens.background.copy(alpha = 0.72f),
                            tokens.background.copy(alpha = 0.96f)
                        )
                    )
                )
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            val availWidth = maxWidth
            val availHeight = maxHeight
            val isLandscapeOrTablet = availWidth > availHeight || availWidth >= 680.dp

            if (immersiveMode) {
                // MODO PANTALLA COMPLETA ("Reproductor inmersivo")
                ImmersivePlayerLayout(
                    song = song,
                    playbackState = playbackState,
                    artColor1 = artColor1,
                    artColor2 = artColor2,
                    artScale = smoothArtScale,
                    showHeartBurst = showHeartBurst,
                    visualizerFrame = visualizerFrame,
                    visualizerConfig = visualizerConfig,
                    maxAvailableWidth = availWidth,
                    maxAvailableHeight = availHeight,
                    onExitImmersive = { onToggleImmersiveMode(false) },
                    onDoubleTapArt = {
                        onToggleFavorite()
                        showHeartBurst = true
                    },
                    onLongPressArt = { if (song != null) showCoverOptions = true },
                    onCycleVisualizer = onCycleVisualizerStyle,
                    onSeekTo = onSeekTo,
                    onTogglePlayPause = onTogglePlayPause,
                    onSkipNext = onSkipNext,
                    onSkipPrevious = onSkipPrevious
                )
            } else if (isLandscapeOrTablet) {
                // ROTACIÓN HORIZONTAL Y TABLETS: Izquierda Portada, Derecha Info y Controles
                Column(modifier = Modifier.fillMaxSize()) {
                    PlayerTopBar(
                        song = song,
                        audioOutputStatus = audioOutputStatus,
                        dynamicBgEnabled = dynamicBackgroundEnabled,
                        onMinimize = onMinimize,
                        onToggleDynamicBg = { onToggleDynamicBackground(!dynamicBackgroundEnabled) },
                        onOpenDisplayOptions = { showDisplayOptions = true },
                        onEnterImmersive = { onToggleImmersiveMode(true) }
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Pane: Large Rounded Artwork + Optional Visualizer
                        Column(
                            modifier = Modifier
                                .weight(0.45f)
                                .fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            val artSize = minOf(availHeight * 0.60f, availWidth * 0.36f).coerceIn(180.dp, 340.dp)
                            TrakasArtworkCard(
                                song = song,
                                artSize = artSize,
                                artColor = artColor1,
                                scale = smoothArtScale,
                                showHeartBurst = showHeartBurst,
                                onDoubleTap = {
                                    onToggleFavorite()
                                    showHeartBurst = true
                                },
                                onLongPress = { if (song != null) showCoverOptions = true }
                            )
                            if (visualizerEnabled) {
                                Spacer(Modifier.height(14.dp))
                                PlayerVisualizerStrip(
                                    frame = visualizerFrame,
                                    config = visualizerConfig,
                                    isPlaying = playbackState.isPlaying,
                                    artColors = artColor1 to artColor2,
                                    onCycleStyle = onCycleVisualizerStyle,
                                    modifier = Modifier.fillMaxWidth(0.9f).height(58.dp)
                                )
                            }
                        }

                        // Right Pane: Metadata, Progress, Transport, Secondary Controls, Volume
                        Column(
                            modifier = Modifier
                                .weight(0.55f)
                                .fillMaxHeight()
                                .widthIn(max = 580.dp),
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            SongInfoSection(
                                song = song,
                                showExtraInfo = true,
                                onArtistClick = onArtistClick,
                                onAlbumClick = onAlbumClick
                            )
                            PlayerProgressBar(
                                positionMs = playbackState.positionMs,
                                durationMs = playbackState.durationMs,
                                accentColor = tokens.primary,
                                onSeekTo = onSeekTo
                            )
                            PrimaryTransportRow(
                                isPlaying = playbackState.isPlaying,
                                shuffleEnabled = playbackState.shuffleEnabled,
                                repeatMode = playbackState.repeatMode,
                                accentPrimary = tokens.primary,
                                accentSecondary = tokens.secondary,
                                onToggleShuffle = onToggleShuffle,
                                onSkipPrevious = onSkipPrevious,
                                onTogglePlayPause = onTogglePlayPause,
                                onSkipNext = onSkipNext,
                                onCycleRepeat = onCycleRepeat
                            )
                            SecondaryActionsBar(
                                isFavorite = isFavorite,
                                eqEnabled = eqState.enabled,
                                onToggleFavorite = onToggleFavorite,
                                onOpenQueue = onOpenQueue,
                                onOpenEqualizer = { showEqSheet = true },
                                onOpenLyrics = onOpenLyrics,
                                onOpenMore = { song?.let(onOpenMoreOptions) }
                            )
                            CompactVolumeRow(
                                volume = playbackState.volume,
                                onSetVolume = onSetVolume
                            )
                        }
                    }
                }
            } else {
                // PORTRAIT PHONE LAYOUT
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    PlayerTopBar(
                        song = song,
                        audioOutputStatus = audioOutputStatus,
                        dynamicBgEnabled = dynamicBackgroundEnabled,
                        onMinimize = onMinimize,
                        onToggleDynamicBg = { onToggleDynamicBackground(!dynamicBackgroundEnabled) },
                        onOpenDisplayOptions = { showDisplayOptions = true },
                        onEnterImmersive = { onToggleImmersiveMode(true) }
                    )

                    val artSize = minOf(availWidth * 0.78f, availHeight * 0.36f).coerceIn(200.dp, 330.dp)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TrakasArtworkCard(
                            song = song,
                            artSize = artSize,
                            artColor = artColor1,
                            scale = smoothArtScale,
                            showHeartBurst = showHeartBurst,
                            onDoubleTap = {
                                onToggleFavorite()
                                showHeartBurst = true
                            },
                            onLongPress = { if (song != null) showCoverOptions = true }
                        )
                        if (visualizerEnabled) {
                            Spacer(Modifier.height(12.dp))
                            PlayerVisualizerStrip(
                                frame = visualizerFrame,
                                config = visualizerConfig,
                                isPlaying = playbackState.isPlaying,
                                artColors = artColor1 to artColor2,
                                onCycleStyle = onCycleVisualizerStyle,
                                modifier = Modifier.fillMaxWidth().height(54.dp)
                            )
                        }
                    }

                    SongInfoSection(
                        song = song,
                        showExtraInfo = false,
                        onArtistClick = onArtistClick,
                        onAlbumClick = onAlbumClick
                    )

                    PlayerProgressBar(
                        positionMs = playbackState.positionMs,
                        durationMs = playbackState.durationMs,
                        accentColor = tokens.primary,
                        onSeekTo = onSeekTo
                    )

                    PrimaryTransportRow(
                        isPlaying = playbackState.isPlaying,
                        shuffleEnabled = playbackState.shuffleEnabled,
                        repeatMode = playbackState.repeatMode,
                        accentPrimary = tokens.primary,
                        accentSecondary = tokens.secondary,
                        onToggleShuffle = onToggleShuffle,
                        onSkipPrevious = onSkipPrevious,
                        onTogglePlayPause = onTogglePlayPause,
                        onSkipNext = onSkipNext,
                        onCycleRepeat = onCycleRepeat
                    )

                    SecondaryActionsBar(
                        isFavorite = isFavorite,
                        eqEnabled = eqState.enabled,
                        onToggleFavorite = onToggleFavorite,
                        onOpenQueue = onOpenQueue,
                        onOpenEqualizer = { showEqSheet = true },
                        onOpenLyrics = onOpenLyrics,
                        onOpenMore = { song?.let(onOpenMoreOptions) }
                    )
                }
            }
        }
    }

    if (showEqSheet) {
        NowPlayingEqSheet(
            eqState = eqState,
            onDismiss = { showEqSheet = false },
            onToggleEnabled = onEqToggleEnabled,
            onSelectPreset = onEqSelectPreset,
            onBandChange = onEqBandChange,
            onBassBoostChange = onEqBassBoostChange,
            onVirtualizerChange = onEqVirtualizerChange,
            onBalanceChange = onEqBalanceChange,
            onBassToneChange = onEqBassToneChange,
            onTrebleToneChange = onEqTrebleToneChange,
            onOpenFullEqualizer = onOpenFullEqualizer
        )
    }

    if (showCoverOptions && song != null) {
        CoverOptionsSheet(
            song = song,
            onDismiss = { showCoverOptions = false },
            onUpdateCoverArt = { uri -> onUpdateCoverArt(song, uri) },
            onOpenMetadataEditor = onOpenMetadataEditor
        )
    }

    if (showDisplayOptions) {
        PlayerDisplayOptionsSheet(
            dynamicBgEnabled = dynamicBackgroundEnabled,
            visualizerEnabled = visualizerEnabled,
            visualizerStyle = visualizerConfig.style,
            immersiveMode = immersiveMode,
            textScaleFactor = textScaleFactor,
            volume = playbackState.volume,
            playbackSpeed = playbackState.playbackSpeed,
            sleepTimerRemainingSec = playbackState.sleepTimerRemainingSec,
            smartShuffleEnabled = playbackState.smartShuffleEnabled,
            onDismiss = { showDisplayOptions = false },
            onToggleDynamicBg = onToggleDynamicBackground,
            onToggleVisualizer = onToggleVisualizerEnabled,
            onSelectVisualizerStyle = onSelectVisualizerStyle,
            onToggleImmersive = onToggleImmersiveMode,
            onSelectTextScale = onSelectTextScale,
            onSetVolume = onSetVolume,
            onCycleSpeed = onCycleSpeed,
            onOpenSleepTimer = onOpenSleepTimer,
            onToggleSmartShuffle = onToggleSmartShuffle
        )
    }
}

@Composable
private fun PlayerTopBar(
    song: Song?,
    audioOutputStatus: AudioOutputStatus,
    dynamicBgEnabled: Boolean,
    onMinimize: () -> Unit,
    onToggleDynamicBg: () -> Unit,
    onOpenDisplayOptions: () -> Unit,
    onEnterImmersive: () -> Unit
) {
    val tokens = LocalTrakasTokens.current
    val strings = LocalAppStrings.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onMinimize,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("now_playing_minimize_btn")
        ) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = "Minimizar reproductor",
                tint = tokens.textColor,
                modifier = Modifier.size(30.dp)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onToggleDynamicBg)
                .testTag("now_playing_dynamic_bg_toggle")
        ) {
            Text(
                text = strings.nowPlaying.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = tokens.textColor
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (audioOutputStatus.isBluetooth) {
                    Icon(
                        imageVector = Icons.Rounded.BluetoothAudio,
                        contentDescription = audioOutputStatus.deviceName,
                        tint = tokens.primary,
                        modifier = Modifier.size(13.dp)
                    )
                }
                Text(
                    text = if (song != null) "${song.format} • Fondo ${if (dynamicBgEnabled) "Dinámico" else "Fijo"}" else audioOutputStatus.deviceName,
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onEnterImmersive,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("now_playing_immersive_btn")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Fullscreen,
                    contentDescription = strings.immersivePlayer,
                    tint = tokens.textColor
                )
            }
            IconButton(
                onClick = onOpenDisplayOptions,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("now_playing_display_options_btn")
            ) {
                Icon(
                    imageVector = Icons.Rounded.DisplaySettings,
                    contentDescription = "Opciones visuales y audio",
                    tint = tokens.primary
                )
            }
        }
    }
}

@Composable
private fun TrakasArtworkCard(
    song: Song?,
    artSize: Dp,
    artColor: Color,
    scale: Float,
    showHeartBurst: Boolean,
    onDoubleTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val tokens = LocalTrakasTokens.current
    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier = Modifier
            .size(artSize)
            .scale(scale)
            .shadow(
                elevation = 22.dp,
                shape = shape,
                ambientColor = artColor.copy(alpha = 0.55f),
                spotColor = artColor.copy(alpha = 0.55f)
            )
            .clip(shape)
            .border(1.5.dp, tokens.textColor.copy(alpha = 0.14f), shape)
            .pointerInput(song?.id) {
                detectTapGestures(
                    onDoubleTap = { onDoubleTap() },
                    onLongPress = { onLongPress() }
                )
            }
            .semantics { contentDescription = "Portada del álbum. Doble toque para favorito, mantener presionado para opciones de portada" }
            .testTag("now_playing_artwork"),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = song?.albumArtUri to song?.id,
            transitionSpec = { fadeIn(tween(280)) togetherWith fadeOut(tween(220)) },
            label = "artworkCrossfade"
        ) { (uri, _) ->
            ArtworkImage(
                artUri = uri,
                contentDescription = song?.title ?: "Portada",
                modifier = Modifier.fillMaxSize()
            )
        }

        AnimatedVisibility(
            visible = showHeartBurst,
            enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .size(78.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Favorite,
                    contentDescription = null,
                    tint = tokens.secondary,
                    modifier = Modifier.size(44.dp)
                )
            }
        }
    }
}

@Composable
private fun PlayerVisualizerStrip(
    frame: VisualizerFrame,
    config: VisualizerConfig,
    isPlaying: Boolean,
    artColors: Pair<Color, Color>,
    onCycleStyle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalTrakasTokens.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(tokens.surfaceCard.copy(alpha = 0.45f))
            .border(1.dp, tokens.primary.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
            .clickable(onClick = onCycleStyle)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("now_playing_visualizer")
    ) {
        AudioVisualizerCanvas(
            frame = frame,
            config = config,
            isPlaying = isPlaying,
            albumColors = artColors
        )
        Text(
            text = config.style.labelEs.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = tokens.textColor.copy(alpha = 0.65f),
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}

@Composable
private fun SongInfoSection(
    song: Song?,
    showExtraInfo: Boolean,
    onArtistClick: (String) -> Unit,
    onAlbumClick: (String) -> Unit
) {
    val tokens = LocalTrakasTokens.current
    val strings = LocalAppStrings.current

    AnimatedContent(
        targetState = song,
        transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(180)) },
        label = "songInfoTransition",
        modifier = Modifier.fillMaxWidth()
    ) { target ->
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = target?.title ?: strings.noMusicTitle,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = tokens.textColor,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee()
                    .testTag("now_playing_title")
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = target?.artist ?: "---",
                    style = MaterialTheme.typography.titleMedium,
                    color = tokens.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(enabled = target != null) {
                            target?.artist?.let(onArtistClick)
                        }
                        .padding(vertical = 2.dp)
                        .testTag("now_playing_artist")
                )
                if (target != null && target.album.isNotBlank()) {
                    Text("•", color = tokens.mutedTextColor)
                    Text(
                        text = target.album,
                        style = MaterialTheme.typography.bodyLarge,
                        color = tokens.mutedTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onAlbumClick(target.album) }
                            .padding(vertical = 2.dp)
                            .testTag("now_playing_album")
                    )
                }
            }
            if (showExtraInfo && target != null) {
                Text(
                    text = "${target.genre} • ${target.year} • Pista #${target.trackNumber} • ${target.format}",
                    style = MaterialTheme.typography.labelMedium,
                    color = tokens.mutedTextColor
                )
            }
        }
    }
}

@Composable
private fun PlayerProgressBar(
    positionMs: Long,
    durationMs: Long,
    accentColor: Color,
    onSeekTo: (Long) -> Unit
) {
    val tokens = LocalTrakasTokens.current
    val dur = durationMs.coerceAtLeast(1L)
    val liveFraction = (positionMs.coerceIn(0L, dur).toFloat() / dur.toFloat()).coerceIn(0f, 1f)

    var isScrubbing by remember { mutableStateOf(false) }
    var scrubFraction by remember { mutableFloatStateOf(liveFraction) }
    val displayFraction = if (isScrubbing) scrubFraction else liveFraction
    val displayPosMs = if (isScrubbing) (scrubFraction * dur).toLong() else positionMs

    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = displayFraction,
            onValueChange = {
                isScrubbing = true
                scrubFraction = it
            },
            onValueChangeFinished = {
                onSeekTo((scrubFraction * dur).toLong())
                isScrubbing = false
            },
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = tokens.textColor.copy(alpha = 0.20f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("now_playing_seekbar")
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(displayPosMs),
                fontFamily = JetBrainsMonoFontFamily,
                style = MaterialTheme.typography.labelMedium,
                color = tokens.textColor.copy(alpha = 0.85f)
            )
            Text(
                text = formatDuration(durationMs),
                fontFamily = JetBrainsMonoFontFamily,
                style = MaterialTheme.typography.labelMedium,
                color = tokens.mutedTextColor
            )
        }
    }
}

@Composable
private fun PrimaryTransportRow(
    isPlaying: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: RepeatModeOption,
    accentPrimary: Color,
    accentSecondary: Color,
    onToggleShuffle: () -> Unit,
    onSkipPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onCycleRepeat: () -> Unit
) {
    val tokens = LocalTrakasTokens.current
    var pressedPulse by remember { mutableStateOf(false) }
    LaunchedEffect(isPlaying) {
        pressedPulse = true
        delay(140L)
        pressedPulse = false
    }
    val playScale by animateFloatAsState(
        targetValue = if (pressedPulse) 0.92f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "playBtnScale"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onToggleShuffle,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("now_playing_shuffle_btn")
        ) {
            Icon(
                imageVector = Icons.Rounded.Shuffle,
                contentDescription = "Aleatorio",
                tint = if (shuffleEnabled) accentPrimary else tokens.mutedTextColor,
                modifier = Modifier.size(24.dp)
            )
        }

        IconButton(
            onClick = onSkipPrevious,
            modifier = Modifier.size(56.dp).testTag("now_playing_prev_btn")
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipPrevious,
                contentDescription = "Canción anterior",
                tint = tokens.textColor,
                modifier = Modifier.size(38.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(76.dp)
                .scale(playScale)
                .shadow(16.dp, CircleShape, ambientColor = accentPrimary, spotColor = accentPrimary)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(accentPrimary, accentSecondary)))
                .clickable(onClick = onTogglePlayPause)
                .semantics { contentDescription = if (isPlaying) "Pausar" else "Reproducir" }
                .testTag("now_playing_play_pause_btn"),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = isPlaying,
                transitionSpec = { scaleIn(tween(180)) + fadeIn() togetherWith scaleOut(tween(150)) + fadeOut() },
                label = "playPauseIcon"
            ) { playing ->
                Icon(
                    imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = accentPrimary.contrastText(),
                    modifier = Modifier.size(42.dp)
                )
            }
        }

        IconButton(
            onClick = onSkipNext,
            modifier = Modifier.size(56.dp).testTag("now_playing_next_btn")
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = "Canción siguiente",
                tint = tokens.textColor,
                modifier = Modifier.size(38.dp)
            )
        }

        IconButton(
            onClick = onCycleRepeat,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("now_playing_repeat_btn")
        ) {
            Icon(
                imageVector = if (repeatMode == RepeatModeOption.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                contentDescription = "Repetición",
                tint = if (repeatMode != RepeatModeOption.OFF) accentPrimary else tokens.mutedTextColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun SecondaryActionsBar(
    isFavorite: Boolean,
    eqEnabled: Boolean,
    onToggleFavorite: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenMore: () -> Unit
) {
    val tokens = LocalTrakasTokens.current
    val strings = LocalAppStrings.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(tokens.surfaceCard.copy(alpha = 0.55f))
            .border(1.dp, tokens.textColor.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("now_playing_fav_btn")
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = strings.toggleFavorite,
                tint = if (isFavorite) tokens.secondary else tokens.mutedTextColor
            )
        }
        IconButton(
            onClick = onOpenQueue,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("now_playing_queue_btn")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                contentDescription = strings.queue,
                tint = tokens.textColor
            )
        }
        IconButton(
            onClick = onOpenEqualizer,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("now_playing_eq_btn")
        ) {
            Icon(
                imageVector = Icons.Rounded.Tune,
                contentDescription = strings.equalizer,
                tint = if (eqEnabled) tokens.primary else tokens.mutedTextColor
            )
        }
        IconButton(
            onClick = onOpenLyrics,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("now_playing_lyrics_btn")
        ) {
            Icon(
                imageVector = Icons.Rounded.Lyrics,
                contentDescription = strings.lyrics,
                tint = tokens.textColor
            )
        }
        IconButton(
            onClick = onOpenMore,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("now_playing_more_btn")
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "Más opciones",
                tint = tokens.textColor
            )
        }
    }
}

@Composable
private fun CompactVolumeRow(
    volume: Float,
    onSetVolume: (Float) -> Unit
) {
    val tokens = LocalTrakasTokens.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
            contentDescription = "Volumen",
            tint = tokens.mutedTextColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Slider(
            value = volume,
            onValueChange = onSetVolume,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = tokens.secondary,
                activeTrackColor = tokens.secondary,
                inactiveTrackColor = tokens.textColor.copy(alpha = 0.16f)
            )
        )
    }
}

@Composable
private fun ImmersivePlayerLayout(
    song: Song?,
    playbackState: PlaybackUiState,
    artColor1: Color,
    artColor2: Color,
    artScale: Float,
    showHeartBurst: Boolean,
    visualizerFrame: VisualizerFrame,
    visualizerConfig: VisualizerConfig,
    maxAvailableWidth: Dp,
    maxAvailableHeight: Dp,
    onExitImmersive: () -> Unit,
    onDoubleTapArt: () -> Unit,
    onLongPressArt: () -> Unit,
    onCycleVisualizer: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit
) {
    val tokens = LocalTrakasTokens.current
    val artSize = minOf(maxAvailableWidth * 0.86f, maxAvailableHeight * 0.46f).coerceIn(220.dp, 400.dp)

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(tokens.surfaceCard.copy(alpha = 0.65f))
                    .clickable(onClick = onExitImmersive)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("immersive_exit_btn"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.FullscreenExit, contentDescription = "Salir de inmersivo", tint = tokens.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Salir", style = MaterialTheme.typography.labelMedium, color = tokens.textColor)
            }
        }

        TrakasArtworkCard(
            song = song,
            artSize = artSize,
            artColor = artColor1,
            scale = artScale,
            showHeartBurst = showHeartBurst,
            onDoubleTap = onDoubleTapArt,
            onLongPress = onLongPressArt
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = song?.title ?: "Trakas Music Player",
                style = MaterialTheme.typography.headlineSmall,
                color = tokens.textColor,
                maxLines = 1,
                modifier = Modifier.basicMarquee()
            )
            Text(
                text = song?.artist ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.primary,
                maxLines = 1
            )
        }

        PlayerVisualizerStrip(
            frame = visualizerFrame,
            config = visualizerConfig,
            isPlaying = playbackState.isPlaying,
            artColors = artColor1 to artColor2,
            onCycleStyle = onCycleVisualizer,
            modifier = Modifier.fillMaxWidth().height(92.dp)
        )

        PlayerProgressBar(
            positionMs = playbackState.positionMs,
            durationMs = playbackState.durationMs,
            accentColor = tokens.primary,
            onSeekTo = onSeekTo
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onSkipPrevious, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Rounded.SkipPrevious, contentDescription = "Anterior", tint = tokens.textColor, modifier = Modifier.size(38.dp))
            }
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(tokens.primary, tokens.secondary)))
                    .clickable(onClick = onTogglePlayPause),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = tokens.primary.contrastText(),
                    modifier = Modifier.size(40.dp)
                )
            }
            IconButton(onClick = onSkipNext, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Rounded.SkipNext, contentDescription = "Siguiente", tint = tokens.textColor, modifier = Modifier.size(38.dp))
            }
        }
    }
}
