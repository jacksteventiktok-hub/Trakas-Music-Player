package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AppLanguage
import com.example.model.PlaylistWithSongs
import com.example.model.SmartPlaylistKind
import com.example.model.Song
import com.example.ui.components.ArtworkImage
import com.example.ui.components.TrakasIconType
import com.example.ui.components.TrakasStyledIcon
import com.example.ui.components.formatDuration
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LocalTrakasTokens
import com.example.ui.theme.contrastText
import com.example.viewmodel.LibraryUiState

@Composable
fun PlaylistsScreen(
    libraryState: LibraryUiState,
    language: AppLanguage,
    onCreatePlaylist: (String) -> Unit,
    onCreatePlaylistFromSongs: (String, List<Song>) -> Unit,
    onRenamePlaylist: (Long, String) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onAddSongToPlaylist: (Long, Long) -> Unit,
    onRemoveSongFromPlaylist: (Long, Long) -> Unit,
    onMovePlaylistSong: (Long, Int, Int) -> Unit,
    onPlayPlaylist: (List<Song>, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalTrakasTokens.current
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val playlists = libraryState.playlists
    val allSongs = libraryState.visibleSongs

    var selectedPlaylistId by remember { mutableStateOf<Long?>(null) }
    var selectedSmartKind by remember { mutableStateOf<SmartPlaylistKind?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var renamingPlaylist by remember { mutableStateOf<PlaylistWithSongs?>(null) }
    var showAddSongDialog by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }

    val activePlaylist = playlists.find { it.id == selectedPlaylistId }
    BackHandler(enabled = activePlaylist != null || selectedSmartKind != null) {
        selectedPlaylistId = null
        selectedSmartKind = null
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (selectedSmartKind != null) {
            val kind = selectedSmartKind!!
            val smartTitle = if (language == AppLanguage.SPANISH) kind.labelEs else kind.labelEn
            val smartSongs = libraryState.buildSmartPlaylist(kind)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        IconButton(onClick = { selectedSmartKind = null }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = tokens.primary)
                        }
                        Column {
                            Text(smartTitle, style = MaterialTheme.typography.headlineSmall, color = tokens.textColor, maxLines = 1)
                            Text("${smartSongs.size} ${strings.songs.lowercase()}", style = MaterialTheme.typography.bodySmall, color = tokens.mutedTextColor)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (smartSongs.isNotEmpty()) {
                            OutlinedButton(onClick = { onCreatePlaylistFromSongs(smartTitle, smartSongs) }) {
                                Text(strings.save, maxLines = 1)
                            }
                            Button(
                                onClick = { onPlayPlaylist(smartSongs, 0) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = tokens.primary,
                                    contentColor = tokens.primary.contrastText()
                                )
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = "Play", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    itemsIndexed(smartSongs, key = { idx, s -> "sp_${kind.name}_${idx}_${s.id}" }) { idx, song ->
                        SongRow(
                            song = song,
                            isPlayingCurrent = false,
                            isFavorite = song.id in libraryState.favoriteIds,
                            onClick = { onPlayPlaylist(smartSongs, idx) },
                            onLongClick = {},
                            onToggleFavorite = {}
                        )
                    }
                }
            }
        } else if (activePlaylist == null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = strings.smartPlaylists,
                        style = MaterialTheme.typography.titleLarge,
                        color = tokens.primary
                    )
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(SmartPlaylistKind.entries, key = { it.name }) { kind ->
                            val label = if (language == AppLanguage.SPANISH) kind.labelEs else kind.labelEn
                            val count = libraryState.buildSmartPlaylist(kind).size
                            Column(
                                modifier = Modifier
                                    .width(146.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(tokens.surfaceCard)
                                    .border(1.dp, tokens.primary.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                                    .clickable { selectedSmartKind = kind }
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = tokens.primary, modifier = Modifier.size(20.dp))
                                Text(label, style = MaterialTheme.typography.titleSmall, color = tokens.textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("$count ${strings.songs.lowercase()}", style = MaterialTheme.typography.bodySmall, color = tokens.mutedTextColor)
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = strings.playlists,
                        style = MaterialTheme.typography.headlineSmall,
                        color = tokens.textColor
                    )
                }

                if (playlists.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(tokens.surfaceCard)
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TrakasStyledIcon(TrakasIconType.PLAYLISTS, null, active = true, showBadgeContainer = true, size = 32.dp)
                            Text(strings.createPlaylist, style = MaterialTheme.typography.titleMedium, color = tokens.textColor)
                            Button(
                                onClick = {
                                    textInput = ""
                                    showCreateDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = tokens.buttonColor,
                                    contentColor = tokens.buttonColor.contrastText()
                                )
                            ) {
                                Text(strings.createPlaylist)
                            }
                        }
                    }
                } else {
                    items(playlists, key = { it.id }) { pl ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(tokens.surfaceCard)
                                .border(1.dp, tokens.textColor.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                .clickable { selectedPlaylistId = pl.id }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TrakasStyledIcon(TrakasIconType.PLAYLISTS, pl.name, active = true, showBadgeContainer = true, size = 22.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(pl.name, style = MaterialTheme.typography.titleMedium, color = tokens.textColor)
                                Text("${pl.songs.size} ${strings.songs.lowercase()}", style = MaterialTheme.typography.bodySmall, color = tokens.mutedTextColor)
                            }
                            IconButton(onClick = {
                                textInput = pl.name
                                renamingPlaylist = pl
                            }) {
                                Icon(Icons.Rounded.Edit, contentDescription = strings.renamePlaylist, tint = tokens.mutedTextColor)
                            }
                            IconButton(onClick = {
                                val shareBody = buildString {
                                    append("Trakas Playlist: ${pl.name}\n")
                                    pl.songs.forEachIndexed { i, s ->
                                        append("${i + 1}. ${s.title} - ${s.artist} (${formatDuration(s.durationMs)})\n")
                                    }
                                }
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, pl.name)
                                    putExtra(Intent.EXTRA_TEXT, shareBody)
                                }
                                context.startActivity(Intent.createChooser(intent, strings.sharePlaylist))
                            }) {
                                Icon(Icons.Rounded.Share, contentDescription = strings.sharePlaylist, tint = tokens.primary)
                            }
                            IconButton(onClick = { onDeletePlaylist(pl.id) }) {
                                Icon(Icons.Rounded.Delete, contentDescription = strings.deletePlaylist, tint = tokens.secondary)
                            }
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = {
                    textInput = ""
                    showCreateDialog = true
                },
                containerColor = tokens.primary,
                contentColor = tokens.primary.contrastText(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 92.dp)
                    .testTag("create_playlist_fab")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = strings.createPlaylist)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        IconButton(onClick = { selectedPlaylistId = null }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = tokens.primary)
                        }
                        Column {
                            Text(activePlaylist.name, style = MaterialTheme.typography.headlineSmall, color = tokens.textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${activePlaylist.songs.size} ${strings.songs.lowercase()}", style = MaterialTheme.typography.bodySmall, color = tokens.mutedTextColor)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showAddSongDialog = true }) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(strings.addSongs)
                        }
                        if (activePlaylist.songs.isNotEmpty()) {
                            Button(
                                onClick = { onPlayPlaylist(activePlaylist.songs, 0) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = tokens.primary,
                                    contentColor = tokens.primary.contrastText()
                                )
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    itemsIndexed(activePlaylist.songs, key = { idx, s -> "pl_${activePlaylist.id}_${idx}_${s.id}" }) { idx, song ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(tokens.surfaceCard)
                                .clickable { onPlayPlaylist(activePlaylist.songs, idx) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ArtworkImage(
                                artUri = song.albumArtUri,
                                contentDescription = song.title,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(song.title, style = MaterialTheme.typography.titleSmall, color = tokens.textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(song.artist, style = MaterialTheme.typography.bodySmall, color = tokens.mutedTextColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(
                                onClick = { if (idx > 0) onMovePlaylistSong(activePlaylist.id, idx, idx - 1) },
                                enabled = idx > 0
                            ) {
                                Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move up", tint = tokens.mutedTextColor, modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = { if (idx < activePlaylist.songs.lastIndex) onMovePlaylistSong(activePlaylist.id, idx, idx + 1) },
                                enabled = idx < activePlaylist.songs.lastIndex
                            ) {
                                Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move down", tint = tokens.mutedTextColor, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { onRemoveSongFromPlaylist(activePlaylist.id, song.id) }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Remove", tint = tokens.secondary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = tokens.surfaceElevated,
            title = { Text(strings.createPlaylist, color = tokens.textColor) },
            text = {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    singleLine = true,
                    label = { Text("Nombre") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (textInput.isNotBlank()) {
                        onCreatePlaylist(textInput)
                        showCreateDialog = false
                    }
                }) { Text(strings.save, color = tokens.primary) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text(strings.cancel, color = tokens.mutedTextColor) }
            }
        )
    }

    val targetRename = renamingPlaylist
    if (targetRename != null) {
        AlertDialog(
            onDismissRequest = { renamingPlaylist = null },
            containerColor = tokens.surfaceElevated,
            title = { Text(strings.renamePlaylist, color = tokens.textColor) },
            text = {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (textInput.isNotBlank()) {
                        onRenamePlaylist(targetRename.id, textInput)
                        renamingPlaylist = null
                    }
                }) { Text(strings.save, color = tokens.primary) }
            },
            dismissButton = {
                TextButton(onClick = { renamingPlaylist = null }) { Text(strings.cancel, color = tokens.mutedTextColor) }
            }
        )
    }

    if (showAddSongDialog && activePlaylist != null) {
        AlertDialog(
            onDismissRequest = { showAddSongDialog = false },
            containerColor = tokens.surfaceElevated,
            title = { Text(strings.addSongs, color = tokens.textColor) },
            text = {
                LazyColumn(modifier = Modifier.height(300.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(allSongs, key = { "add_pl_${it.id}" }) { s ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    onAddSongToPlaylist(activePlaylist.id, s.id)
                                    showAddSongDialog = false
                                }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(s.title, color = tokens.textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(s.artist, style = MaterialTheme.typography.bodySmall, color = tokens.mutedTextColor)
                            }
                            Icon(Icons.Rounded.Add, contentDescription = null, tint = tokens.primary)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddSongDialog = false }) { Text(strings.cancel, color = tokens.primary) }
            }
        )
    }
}
