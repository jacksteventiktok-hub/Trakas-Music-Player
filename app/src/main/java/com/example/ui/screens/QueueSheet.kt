package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.model.Song
import com.example.player.PlaybackUiState
import com.example.ui.components.ArtworkImage
import com.example.ui.components.formatDuration
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LocalTrakasTokens
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    playbackState: PlaybackUiState,
    librarySongs: List<Song>,
    onDismiss: () -> Unit,
    onJumpTo: (Int) -> Unit,
    onMoveItem: (Int, Int) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onAddSongToQueue: (Song) -> Unit,
    onClearQueue: () -> Unit
) {
    val tokens = LocalTrakasTokens.current
    val strings = LocalAppStrings.current
    var showAddSongsDialog by remember { mutableStateOf(false) }

    val itemHeightPx = with(LocalDensity.current) { 72.dp.toPx() }
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    val currentIdx = playbackState.currentIndex
    val currentSong = playbackState.currentSong ?: playbackState.queue.getOrNull(currentIdx)
    val upcomingEntries = playbackState.queue
        .mapIndexed { idx, song -> idx to song }
        .filter { (idx, _) -> idx != currentIdx }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = tokens.surfaceElevated,
        contentColor = tokens.textColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${strings.queue} (${playbackState.queue.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = tokens.textColor
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = { showAddSongsDialog = true },
                        modifier = Modifier.testTag("queue_add_songs_btn")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = strings.addSongs, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.addSongs, maxLines = 1)
                    }
                    if (playbackState.queue.isNotEmpty()) {
                        IconButton(
                            onClick = onClearQueue,
                            modifier = Modifier.testTag("queue_clear_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteSweep,
                                contentDescription = strings.clearQueue,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            if (playbackState.queue.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strings.noMusicTitle,
                        color = tokens.mutedTextColor
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (currentSong != null) {
                        item(key = "header_now_playing") {
                            Text(
                                text = strings.nowPlaying,
                                style = MaterialTheme.typography.labelLarge,
                                color = tokens.primary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        item(key = "current_song_${currentSong.id}_$currentIdx") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(tokens.primary.copy(alpha = 0.16f))
                                    .border(1.dp, tokens.primary.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                                    .clickable {
                                        if (currentIdx >= 0) onJumpTo(currentIdx)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ArtworkImage(
                                    artUri = currentSong.albumArtUri,
                                    contentDescription = currentSong.title,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentSong.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = tokens.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${currentSong.artist} • ${formatDuration(currentSong.durationMs)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = tokens.mutedTextColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Rounded.GraphicEq,
                                    contentDescription = strings.nowPlaying,
                                    tint = tokens.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    if (upcomingEntries.isNotEmpty()) {
                        item(key = "header_upcoming") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = strings.upcomingSongs,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = tokens.secondary
                                )
                                Text(
                                    text = "Arrastra ≡ para reordenar",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tokens.mutedTextColor
                                )
                            }
                        }
                    }

                    itemsIndexed(upcomingEntries, key = { _, pair -> "q_${pair.first}_${pair.second.id}" }) { _, (queueIdx, song) ->
                        val isDragging = draggingIndex == queueIdx
                        val rowOffset = if (isDragging) dragOffsetY.roundToInt() else 0

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .zIndex(if (isDragging) 1f else 0f)
                                .offset { IntOffset(0, rowOffset) }
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isDragging) tokens.primary.copy(alpha = 0.22f) else tokens.surfaceCard
                                )
                                .border(
                                    1.dp,
                                    if (isDragging) tokens.primary else tokens.textColor.copy(alpha = 0.08f),
                                    RoundedCornerShape(12.dp)
                                )
                                .pointerInput(queueIdx, playbackState.queue.size) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            draggingIndex = queueIdx
                                            dragOffsetY = 0f
                                        },
                                        onDragCancel = {
                                            draggingIndex = null
                                            dragOffsetY = 0f
                                        },
                                        onDragEnd = {
                                            draggingIndex = null
                                            dragOffsetY = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffsetY += dragAmount.y
                                            val curIdx = draggingIndex ?: return@detectDragGesturesAfterLongPress
                                            if (dragOffsetY > itemHeightPx * 0.7f && curIdx < playbackState.queue.lastIndex) {
                                                onMoveItem(curIdx, curIdx + 1)
                                                draggingIndex = curIdx + 1
                                                dragOffsetY -= itemHeightPx
                                            } else if (dragOffsetY < -itemHeightPx * 0.7f && curIdx > 0) {
                                                onMoveItem(curIdx, curIdx - 1)
                                                draggingIndex = curIdx - 1
                                                dragOffsetY += itemHeightPx
                                            }
                                        }
                                    )
                                }
                                .clickable { onJumpTo(queueIdx) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DragHandle,
                                contentDescription = "Reordenar",
                                tint = if (isDragging) tokens.primary else tokens.mutedTextColor,
                                modifier = Modifier
                                    .size(28.dp)
                                    .padding(4.dp)
                                    .pointerInput(queueIdx, playbackState.queue.size) {
                                        detectVerticalDragGestures(
                                            onDragStart = {
                                                draggingIndex = queueIdx
                                                dragOffsetY = 0f
                                            },
                                            onDragCancel = {
                                                draggingIndex = null
                                                dragOffsetY = 0f
                                            },
                                            onDragEnd = {
                                                draggingIndex = null
                                                dragOffsetY = 0f
                                            },
                                            onVerticalDrag = { change, dragAmount ->
                                                change.consume()
                                                dragOffsetY += dragAmount
                                                val curIdx = draggingIndex ?: return@detectVerticalDragGestures
                                                if (dragOffsetY > itemHeightPx * 0.7f && curIdx < playbackState.queue.lastIndex) {
                                                    onMoveItem(curIdx, curIdx + 1)
                                                    draggingIndex = curIdx + 1
                                                    dragOffsetY -= itemHeightPx
                                                } else if (dragOffsetY < -itemHeightPx * 0.7f && curIdx > 0) {
                                                    onMoveItem(curIdx, curIdx - 1)
                                                    draggingIndex = curIdx - 1
                                                    dragOffsetY += itemHeightPx
                                                }
                                            }
                                        )
                                    }
                            )
                            Spacer(Modifier.width(6.dp))
                            ArtworkImage(
                                artUri = song.albumArtUri,
                                contentDescription = song.title,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = tokens.textColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${song.artist} • ${formatDuration(song.durationMs)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = tokens.mutedTextColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { onRemoveItem(queueIdx) }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Eliminar de cola", tint = tokens.mutedTextColor, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }

    if (showAddSongsDialog) {
        AlertDialog(
            onDismissRequest = { showAddSongsDialog = false },
            containerColor = tokens.surfaceElevated,
            title = { Text(strings.addSongs, color = tokens.textColor) },
            text = {
                LazyColumn(modifier = Modifier.height(300.dp)) {
                    items(librarySongs, key = { "add_q_${it.id}" }) { song ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    onAddSongToQueue(song)
                                    showAddSongsDialog = false
                                }
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ArtworkImage(
                                artUri = song.albumArtUri,
                                contentDescription = song.title,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(song.title, color = tokens.textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(song.artist, color = tokens.mutedTextColor, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                            }
                            Icon(Icons.Rounded.Add, contentDescription = null, tint = tokens.primary)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddSongsDialog = false }) {
                    Text(strings.cancel, color = tokens.primary)
                }
            }
        )
    }
}
