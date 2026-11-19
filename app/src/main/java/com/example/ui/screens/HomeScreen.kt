package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.BluetoothAudio
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.InterfaceMode
import com.example.model.RepeatModeOption
import com.example.model.Song
import com.example.model.VisualizerConfig
import com.example.player.AudioOutputStatus
import com.example.player.PlaybackUiState
import com.example.player.VisualizerFrame
import com.example.ui.components.ArtworkImage
import com.example.ui.components.AudioVisualizerCanvas
import com.example.ui.components.TrakasIconType
import com.example.ui.components.TrakasStyledIcon
import com.example.ui.components.formatDuration
import com.example.ui.components.rememberDynamicArtColors
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LocalTrakasTokens
import com.example.ui.theme.contrastText
import com.example.viewmodel.LibraryUiState
import java.util.Locale

@Composable
fun HomeScreen(
    libraryState: LibraryUiState,
    playbackState: PlaybackUiState,
    visualizerFrame: VisualizerFrame,
    visualizerConfig: VisualizerConfig,
    audioOutputStatus: AudioOutputStatus,
    activeMode: InterfaceMode,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onImportAudio: () -> Unit,
    onCreateDemoTracks: () -> Unit,
    onRescanLibrary: () -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
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
    onToggleFavorite: (Long) -> Unit,
    onSelectInterfaceMode: (InterfaceMode) -> Unit,
    onOpenNowPlaying: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenExploreAi: () -> Unit,
    onSongLongClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalTrakasTokens.current
    val strings = LocalAppStrings.current
    val cardShape = RoundedCornerShape(tokens.buttonCornerDp.coerceAtLeast(14).dp)
    val currentSong = playbackState.currentSong
    val albumColors = rememberDynamicArtColors(
        artUri = currentSong?.albumArtUri,
        fallbackPrimary = tokens.primary,
        fallbackSecondary = tokens.secondary
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Brand Header + Sonic Hero Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.dp, tokens.primary.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
                    .clickable {
                        if (currentSong != null) onOpenNowPlaying() else onOpenExploreAi()
                    }
                    .testTag("home_hero_card")
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_player_hero_banner),
                    contentDescription = strings.appName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    tokens.background.copy(alpha = 0.94f),
                                    tokens.background.copy(alpha = 0.72f),
                                    tokens.primary.copy(alpha = 0.16f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = strings.appName,
                            style = MaterialTheme.typography.headlineMedium,
                            color = tokens.textColor
                        )
                        Text(
                            text = strings.slogan,
                            style = MaterialTheme.typography.bodyMedium,
                            color = tokens.primary
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(tokens.surfaceElevated.copy(alpha = 0.9f))
                                .border(1.dp, tokens.primary.copy(alpha = 0.4f), RoundedCornerShape(50))
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = if (audioOutputStatus.isBluetooth) Icons.Rounded.BluetoothAudio else Icons.Rounded.Speaker,
                                contentDescription = strings.outputDevice,
                                tint = tokens.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = audioOutputStatus.deviceName,
                                style = MaterialTheme.typography.labelSmall,
                                color = tokens.textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "${libraryState.visibleSongs.size} ${strings.songs.lowercase()}",
                            style = MaterialTheme.typography.labelMedium,
                            color = tokens.textColor.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        // 5 Interface Modes Selector
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(InterfaceMode.entries, key = { it.id }) { mode ->
                    val selected = mode == activeMode
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectInterfaceMode(mode) },
                        label = { Text(mode.labelEs) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = tokens.primary.copy(alpha = 0.24f),
                            selectedLabelColor = tokens.primary,
                            containerColor = tokens.surfaceCard,
                            labelColor = tokens.mutedTextColor
                        ),
                        modifier = Modifier.testTag("mode_chip_${mode.id}")
                    )
                }
            }
        }

        // Permission Prompt
        if (!hasPermission) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(cardShape)
                        .background(tokens.surfaceCard)
                        .border(1.dp, tokens.secondary.copy(alpha = 0.45f), cardShape)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(strings.grantPermissionTitle, style = MaterialTheme.typography.titleMedium, color = tokens.textColor)
                    Text(strings.grantPermissionBody, style = MaterialTheme.typography.bodyMedium, color = tokens.mutedTextColor)
                    Button(
                        onClick = onRequestPermission,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = tokens.buttonColor,
                            contentColor = tokens.buttonColor.contrastText()
                        ),
                        shape = RoundedCornerShape(tokens.buttonCornerDp.dp),
                        modifier = Modifier.testTag("grant_audio_permission_button")
                    ) {
                        Text(strings.grantPermissionBtn)
                    }
                }
            }
        }

        // Main Interactive Player Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(tokens.surfaceCard)
                    .border(1.dp, tokens.primary.copy(alpha = 0.28f), RoundedCornerShape(22.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = currentSong != null, onClick = onOpenNowPlaying),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ArtworkImage(
                        artUri = currentSong?.albumArtUri,
                        contentDescription = currentSong?.title ?: strings.nowPlaying,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentSong?.title ?: strings.noMusicTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = tokens.textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = currentSong?.artist ?: strings.slogan,
                            style = MaterialTheme.typography.bodyMedium,
                            color = tokens.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = currentSong?.album ?: "MP3 • FLAC • WAV • AAC • OGG • M4A",
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.mutedTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (currentSong != null) {
                        val isFav = currentSong.id in libraryState.favoriteIds
                        IconButton(onClick = { onToggleFavorite(currentSong.id) }) {
                            Icon(
                                imageVector = if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = strings.toggleFavorite,
                                tint = if (isFav) tokens.secondary else tokens.mutedTextColor
                            )
                        }
                    }
                }

                // Live Audio Visualizer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(tokens.background.copy(alpha = 0.55f))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    AudioVisualizerCanvas(
                        frame = visualizerFrame,
                        config = visualizerConfig,
                        isPlaying = playbackState.isPlaying,
                        albumColors = albumColors
                    )
                }

                // Seekbar + Elapsed / Duration
                val dur = playbackState.durationMs.coerceAtLeast(1L)
                val pos = playbackState.positionMs.coerceIn(0L, dur)
                Column {
                    Slider(
                        value = pos.toFloat() / dur.toFloat(),
                        onValueChange = { frac -> onSeekTo((frac * dur).toLong()) },
                        enabled = currentSong != null,
                        colors = SliderDefaults.colors(
                            thumbColor = tokens.progressColor,
                            activeTrackColor = tokens.progressColor,
                            inactiveTrackColor = tokens.textColor.copy(alpha = 0.16f)
                        ),
                        modifier = Modifier.testTag("home_seek_slider")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatDuration(playbackState.positionMs), style = MaterialTheme.typography.labelMedium, color = tokens.mutedTextColor)
                        Text(formatDuration(playbackState.durationMs), style = MaterialTheme.typography.labelMedium, color = tokens.mutedTextColor)
                    }
                }

                // Transport Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onToggleShuffle,
                        modifier = Modifier.testTag("home_shuffle_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (playbackState.shuffleEnabled) tokens.primary else tokens.mutedTextColor
                        )
                    }
                    IconButton(
                        onClick = onSkipPrevious,
                        modifier = Modifier.testTag("home_prev_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = "Previous",
                            tint = tokens.textColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(tokens.primary, tokens.secondary)))
                            .clickable {
                                if (currentSong == null && libraryState.visibleSongs.isNotEmpty()) {
                                    onPlaySong(libraryState.visibleSongs.first(), libraryState.visibleSongs)
                                } else {
                                    onTogglePlayPause()
                                }
                            }
                            .testTag("home_play_pause_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = tokens.background,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    IconButton(
                        onClick = onSkipNext,
                        modifier = Modifier.testTag("home_next_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Next",
                            tint = tokens.textColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    IconButton(
                        onClick = onCycleRepeat,
                        modifier = Modifier.testTag("home_repeat_btn")
                    ) {
                        Icon(
                            imageVector = if (playbackState.repeatMode == RepeatModeOption.ONE) {
                                Icons.Rounded.RepeatOne
                            } else Icons.Rounded.Repeat,
                            contentDescription = "Repeat",
                            tint = if (playbackState.repeatMode != RepeatModeOption.OFF) tokens.primary else tokens.mutedTextColor
                        )
                    }
                }

                // Volume Control
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = strings.volume,
                        tint = tokens.mutedTextColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Slider(
                        value = playbackState.volume,
                        onValueChange = onSetVolume,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_volume_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = tokens.secondary,
                            activeTrackColor = tokens.secondary,
                            inactiveTrackColor = tokens.textColor.copy(alpha = 0.14f)
                        )
                    )
                }

                // Playback Smart Tools Row: Smart Shuffle, Speed, Sleep Timer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = playbackState.smartShuffleEnabled,
                        onClick = onToggleSmartShuffle,
                        leadingIcon = { Icon(Icons.Rounded.AutoAwesome, null, modifier = Modifier.size(16.dp)) },
                        label = { Text(strings.smartShuffle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = playbackState.playbackSpeed != 1.0f,
                        onClick = onCycleSpeed,
                        leadingIcon = { Icon(Icons.Rounded.Speed, null, modifier = Modifier.size(16.dp)) },
                        label = { Text(String.format(Locale.US, "%.2gx", playbackState.playbackSpeed)) }
                    )
                    val timerSec = playbackState.sleepTimerRemainingSec
                    FilterChip(
                        selected = timerSec != null,
                        onClick = onOpenSleepTimer,
                        leadingIcon = { Icon(Icons.Rounded.Bedtime, null, modifier = Modifier.size(16.dp)) },
                        label = {
                            Text(if (timerSec != null) "${timerSec / 60}m" else "Timer")
                        }
                    )
                }

                // Quick Shortcut Tiles: Equalizer, Queue, Lyrics, AI & Stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionTile(
                        type = TrakasIconType.EQUALIZER,
                        label = strings.equalizer,
                        onClick = onOpenEqualizer,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_open_eq_btn")
                    )
                    QuickActionTile(
                        type = TrakasIconType.PLAYLISTS,
                        label = "${strings.queue} (${playbackState.queue.size})",
                        onClick = onOpenQueue,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_open_queue_btn")
                    )
                    QuickActionTile(
                        type = TrakasIconType.EXPLORE,
                        label = strings.trakasAi,
                        onClick = onOpenExploreAi,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_open_ai_btn")
                    )
                }

                if (currentSong != null) {
                    OutlinedButton(
                        onClick = onOpenLyrics,
                        shape = RoundedCornerShape(tokens.buttonCornerDp.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_open_lyrics_btn")
                    ) {
                        Icon(Icons.Rounded.Lyrics, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(strings.lyrics)
                    }
                }
            }
        }

        // Empty Library Actions
        if (libraryState.visibleSongs.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(cardShape)
                        .background(tokens.surfaceCard)
                        .border(1.dp, tokens.textColor.copy(alpha = 0.12f), cardShape)
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(strings.noMusicTitle, style = MaterialTheme.typography.titleMedium, color = tokens.textColor)
                    Text(strings.noMusicSubtitle, style = MaterialTheme.typography.bodyMedium, color = tokens.mutedTextColor)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onCreateDemoTracks,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = tokens.buttonColor,
                                contentColor = tokens.buttonColor.contrastText()
                            ),
                            shape = RoundedCornerShape(tokens.buttonCornerDp.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("create_demo_tracks_btn")
                        ) {
                            Text(strings.createDemoTracks, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        OutlinedButton(
                            onClick = onImportAudio,
                            shape = RoundedCornerShape(tokens.buttonCornerDp.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("import_audio_btn")
                        ) {
                            Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.importAudio, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    OutlinedButton(
                        onClick = onRescanLibrary,
                        shape = RoundedCornerShape(tokens.buttonCornerDp.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(strings.scanLibrary)
                    }
                }
            }
        }

        // Recently Played Carousel
        if (libraryState.recentlyPlayed.isNotEmpty()) {
            item {
                Text(strings.recentlyPlayed, style = MaterialTheme.typography.titleLarge, color = tokens.textColor)
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(libraryState.recentlyPlayed.take(10), key = { it.id }) { song ->
                        Column(
                            modifier = Modifier
                                .width(134.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(tokens.surfaceCard)
                                .border(1.dp, tokens.textColor.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                .clickable { onPlaySong(song, libraryState.recentlyPlayed) }
                                .padding(10.dp)
                        ) {
                            ArtworkImage(
                                artUri = song.albumArtUri,
                                contentDescription = song.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(song.title, style = MaterialTheme.typography.titleSmall, color = tokens.textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song.artist, style = MaterialTheme.typography.bodySmall, color = tokens.mutedTextColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        // Quick Track List
        val featuredSongs = if (libraryState.favoriteSongs.isNotEmpty()) {
            libraryState.favoriteSongs
        } else {
            libraryState.visibleSongs.take(12)
        }
        if (featuredSongs.isNotEmpty()) {
            item {
                Text(
                    text = if (libraryState.favoriteSongs.isNotEmpty()) strings.favorites else strings.songs,
                    style = MaterialTheme.typography.titleLarge,
                    color = tokens.textColor
                )
            }
            items(featuredSongs, key = { "home_${it.id}" }) { song ->
                SongRow(
                    song = song,
                    isPlayingCurrent = playbackState.currentSong?.id == song.id,
                    isFavorite = song.id in libraryState.favoriteIds,
                    onClick = { onPlaySong(song, featuredSongs) },
                    onLongClick = { onSongLongClick(song) },
                    onToggleFavorite = { onToggleFavorite(song.id) }
                )
            }
        }

        item { Spacer(Modifier.height(88.dp)) }
    }
}

@Composable
private fun QuickActionTile(
    type: TrakasIconType,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalTrakasTokens.current
    val shape = RoundedCornerShape(tokens.buttonCornerDp.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(tokens.surfaceElevated)
            .border(1.dp, tokens.primary.copy(alpha = 0.22f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        TrakasStyledIcon(
            type = type,
            contentDescription = label,
            active = true,
            size = 18.dp
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = tokens.textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
