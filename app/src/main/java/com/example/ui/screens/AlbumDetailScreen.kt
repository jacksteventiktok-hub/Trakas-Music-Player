package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AlbumGroup
import com.example.model.PlaylistWithSongs
import com.example.model.Song
import com.example.player.PlaybackUiState
import com.example.ui.components.ArtworkImage
import com.example.ui.components.formatDuration
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LocalTrakasTokens
import com.example.ui.theme.contrastText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    album: AlbumGroup,
    playbackState: PlaybackUiState,
    favoriteIds: Set<Long>,
    playlists: List<PlaylistWithSongs>,
    onBack: () -> Unit,
    onArtistClick: (String) -> Unit,
    onPlayAlbumOrdered: (List<Song>, Int) -> Unit,
    onShuffleAlbum: (List<Song>) -> Unit,
    onToggleAlbumFavorite: (AlbumGroup) -> Unit,
    onPlaySongsNext: (List<Song>) -> Unit,
    onAddSongsToQueue: (List<Song>) -> Unit,
    onAddAlbumToPlaylist: (Long, List<Song>) -> Unit,
    onCreatePlaylistFromAlbum: (String, List<Song>) -> Unit,
    onSongMoreOptions: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val tokens = LocalTrakasTokens.current
    val strings = LocalAppStrings.current

    val isAlbumFavorite = remember(album.songs, favoriteIds) {
        album.songs.isNotEmpty() && album.songs.all { it.id in favoriteIds }
    }

    var showAlbumMoreSheet by remember { mutableStateOf(false) }
    var showPlaylistPicker by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember(album.name) { mutableStateOf(album.name) }

    val countLabel = if (album.songs.size == 1) "1 canción" else "${album.songs.size} canciones"
    val durationLabel = formatDuration(album.totalDurationMs)
    val metaLine = buildString {
        if (album.year in 1900..2100) {
            append(album.year)
            append(" • ")
        }
        append(countLabel)
        append(" • ")
        append(durationLabel)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("album_detail_screen")
    ) {
        val isWide = maxWidth >= 600.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top App Bar
            item(key = "album_topbar") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("album_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Atrás",
                            tint = tokens.primary
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (album.isCompilation) strings.compilations else strings.albumLabel,
                        style = MaterialTheme.typography.titleLarge,
                        color = tokens.textColor,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Album Header Card
            item(key = "album_hero") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    tokens.secondary.copy(alpha = 0.14f),
                                    tokens.surfaceCard
                                )
                            )
                        )
                        .border(1.dp, tokens.primary.copy(alpha = 0.24f), RoundedCornerShape(24.dp))
                        .padding(18.dp)
                ) {
                    if (isWide) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(22.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ArtworkImage(
                                artUri = album.artUri,
                                contentDescription = album.name,
                                modifier = Modifier
                                    .size(190.dp)
                                    .shadow(16.dp, RoundedCornerShape(20.dp))
                                    .clip(RoundedCornerShape(20.dp))
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = album.name,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = tokens.textColor,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = album.artist,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = tokens.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable(enabled = !album.isCompilation) {
                                            onArtistClick(album.artist)
                                        }
                                        .padding(vertical = 2.dp)
                                )
                                Text(
                                    text = metaLine,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = tokens.mutedTextColor
                                )
                                Spacer(Modifier.height(6.dp))
                                AlbumActionBar(
                                    hasSongs = album.songs.isNotEmpty(),
                                    isFavorite = isAlbumFavorite,
                                    onPlay = { onPlayAlbumOrdered(album.songs, 0) },
                                    onShuffle = { onShuffleAlbum(album.songs) },
                                    onToggleFavorite = { onToggleAlbumFavorite(album) },
                                    onMoreOptions = { showAlbumMoreSheet = true }
                                )
                            }
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ArtworkImage(
                                artUri = album.artUri,
                                contentDescription = album.name,
                                modifier = Modifier
                                    .size(196.dp)
                                    .shadow(18.dp, RoundedCornerShape(22.dp))
                                    .clip(RoundedCornerShape(22.dp))
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = album.name,
                                style = MaterialTheme.typography.headlineSmall,
                                color = tokens.textColor,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = album.artist,
                                style = MaterialTheme.typography.titleMedium,
                                color = tokens.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(enabled = !album.isCompilation) {
                                        onArtistClick(album.artist)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                            Text(
                                text = metaLine,
                                style = MaterialTheme.typography.bodySmall,
                                color = tokens.mutedTextColor,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(2.dp))
                            AlbumActionBar(
                                hasSongs = album.songs.isNotEmpty(),
                                isFavorite = isAlbumFavorite,
                                onPlay = { onPlayAlbumOrdered(album.songs, 0) },
                                onShuffle = { onShuffleAlbum(album.songs) },
                                onToggleFavorite = { onToggleAlbumFavorite(album) },
                                onMoreOptions = { showAlbumMoreSheet = true }
                            )
                        }
                    }
                }
            }

            // Track List Header
            item(key = "album_tracks_header") {
                Text(
                    text = strings.songs,
                    style = MaterialTheme.typography.titleMedium,
                    color = tokens.textColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Ordered Track Rows
            itemsIndexed(
                items = album.songs,
                key = { idx, song -> "alb_trk_${album.id}_${song.id}_$idx" }
            ) { index, song ->
                val isCurrent = playbackState.currentSong?.id == song.id
                AlbumTrackRow(
                    trackNumber = if (song.trackNumber > 0) song.trackNumber else index + 1,
                    song = song,
                    showTrackArtist = album.isCompilation || !song.artist.equals(album.artist, ignoreCase = true),
                    isPlayingCurrent = isCurrent,
                    onClick = { onPlayAlbumOrdered(album.songs, index) },
                    onMoreOptions = { onSongMoreOptions(song) }
                )
            }
        }
    }

    if (showAlbumMoreSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAlbumMoreSheet = false },
            containerColor = tokens.surfaceElevated,
            contentColor = tokens.textColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ArtworkImage(
                        artUri = album.artUri,
                        contentDescription = album.name,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(album.name, style = MaterialTheme.typography.titleMedium, color = tokens.textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${album.artist} • $countLabel", style = MaterialTheme.typography.bodySmall, color = tokens.mutedTextColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = tokens.textColor.copy(alpha = 0.1f))
                Spacer(Modifier.height(6.dp))

                AlbumSheetOption(Icons.Rounded.PlayArrow, strings.playNext) {
                    onPlaySongsNext(album.songs)
                    showAlbumMoreSheet = false
                }
                AlbumSheetOption(Icons.AutoMirrored.Rounded.QueueMusic, strings.addToQueue) {
                    onAddSongsToQueue(album.songs)
                    showAlbumMoreSheet = false
                }
                AlbumSheetOption(Icons.AutoMirrored.Rounded.PlaylistAdd, strings.addToPlaylist) {
                    showAlbumMoreSheet = false
                    showPlaylistPicker = true
                }
                AlbumSheetOption(
                    if (isAlbumFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    if (isAlbumFavorite) "Quitar álbum de favoritos" else "Agregar álbum a favoritos",
                    iconTint = if (isAlbumFavorite) tokens.secondary else tokens.textColor
                ) {
                    onToggleAlbumFavorite(album)
                    showAlbumMoreSheet = false
                }
                if (!album.isCompilation) {
                    AlbumSheetOption(Icons.Rounded.Person, strings.viewArtist, iconTint = tokens.primary) {
                        showAlbumMoreSheet = false
                        onArtistClick(album.artist)
                    }
                }
                AlbumSheetOption(Icons.Rounded.Share, strings.shareSong) {
                    val trackList = album.songs.joinToString("\n") { "${it.trackNumber}. ${it.title}" }
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "${album.name} — ${album.artist}\n$trackList")
                    }
                    context.startActivity(Intent.createChooser(intent, album.name))
                    showAlbumMoreSheet = false
                }

                Spacer(Modifier.height(22.dp))
            }
        }
    }

    if (showPlaylistPicker) {
        AlertDialog(
            onDismissRequest = { showPlaylistPicker = false },
            containerColor = tokens.surfaceElevated,
            title = { Text(strings.addToPlaylist, color = tokens.textColor) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                showPlaylistPicker = false
                                showCreatePlaylistDialog = true
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, tint = tokens.primary)
                        Spacer(Modifier.width(10.dp))
                        Text(strings.createPlaylist, color = tokens.primary)
                    }
                    playlists.forEach { pl ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    onAddAlbumToPlaylist(pl.id, album.songs)
                                    showPlaylistPicker = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(pl.name, color = tokens.textColor)
                            Text("${pl.songs.size}", color = tokens.mutedTextColor)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPlaylistPicker = false }) {
                    Text(strings.cancel, color = tokens.primary)
                }
            }
        )
    }

    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            containerColor = tokens.surfaceElevated,
            title = { Text(strings.createPlaylist, color = tokens.textColor) },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    singleLine = true,
                    label = { Text("Nombre") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newPlaylistName.isNotBlank()) {
                        onCreatePlaylistFromAlbum(newPlaylistName, album.songs)
                        showCreatePlaylistDialog = false
                    }
                }) {
                    Text(strings.save, color = tokens.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text(strings.cancel, color = tokens.mutedTextColor)
                }
            }
        )
    }
}

@Composable
private fun AlbumActionBar(
    hasSongs: Boolean,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMoreOptions: () -> Unit
) {
    val tokens = LocalTrakasTokens.current
    val strings = LocalAppStrings.current
    val shape = RoundedCornerShape(tokens.buttonCornerDp.dp)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onPlay,
            enabled = hasSongs,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = tokens.buttonColor,
                contentColor = tokens.buttonColor.contrastText()
            ),
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("album_play_btn")
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(4.dp))
            Text(strings.playAll, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        OutlinedButton(
            onClick = onShuffle,
            enabled = hasSongs,
            shape = shape,
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("album_shuffle_btn")
        ) {
            Icon(Icons.Rounded.Shuffle, contentDescription = null, tint = tokens.primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(strings.shufflePlay, color = tokens.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        FilledTonalIconButton(
            onClick = onToggleFavorite,
            enabled = hasSongs,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = if (isFavorite) tokens.secondary.copy(alpha = 0.22f) else tokens.surfaceElevated,
                contentColor = if (isFavorite) tokens.secondary else tokens.textColor
            ),
            modifier = Modifier
                .size(48.dp)
                .testTag("album_favorite_btn")
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = strings.toggleFavorite
            )
        }
        FilledTonalIconButton(
            onClick = onMoreOptions,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = tokens.surfaceElevated,
                contentColor = tokens.textColor
            ),
            modifier = Modifier
                .size(48.dp)
                .testTag("album_more_btn")
        ) {
            Icon(Icons.Rounded.MoreVert, contentDescription = "Más opciones")
        }
    }
}

@Composable
private fun AlbumTrackRow(
    trackNumber: Int,
    song: Song,
    showTrackArtist: Boolean,
    isPlayingCurrent: Boolean,
    onClick: () -> Unit,
    onMoreOptions: () -> Unit
) {
    val tokens = LocalTrakasTokens.current
    val shape = RoundedCornerShape(tokens.buttonCornerDp.dp)
    val bg = if (isPlayingCurrent) tokens.primary.copy(alpha = 0.14f) else tokens.surfaceCard
    val border = if (isPlayingCurrent) tokens.primary.copy(alpha = 0.55f) else tokens.textColor.copy(alpha = 0.08f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .clickable(onClick = onClick)
            .padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
            .testTag("album_track_row_${song.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.width(30.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (isPlayingCurrent) {
                Icon(
                    imageVector = Icons.Rounded.GraphicEq,
                    contentDescription = "Reproduciendo",
                    tint = tokens.primary,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = trackNumber.toString(),
                    style = MaterialTheme.typography.titleSmall,
                    color = tokens.mutedTextColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleSmall,
                color = if (isPlayingCurrent) tokens.primary else tokens.textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (showTrackArtist) {
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.mutedTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = formatDuration(song.durationMs),
            style = MaterialTheme.typography.bodySmall,
            color = tokens.mutedTextColor
        )
        IconButton(
            onClick = onMoreOptions,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("album_track_more_${song.id}")
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "Más opciones",
                tint = tokens.mutedTextColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun AlbumSheetOption(
    icon: ImageVector,
    label: String,
    iconTint: Color? = null,
    onClick: () -> Unit
) {
    val tokens = LocalTrakasTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = iconTint ?: tokens.textColor, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = tokens.textColor)
    }
}
