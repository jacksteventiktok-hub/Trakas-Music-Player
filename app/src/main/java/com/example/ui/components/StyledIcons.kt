package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.IconStyle
import com.example.ui.theme.LocalIconStyle
import com.example.ui.theme.LocalTrakasTokens

enum class TrakasIconType {
    HOME,
    LIBRARY,
    SONGS,
    ALBUMS,
    ARTISTS,
    PLAYLISTS,
    FAVORITES,
    EQUALIZER,
    VISUALIZER,
    EXPLORE,
    SETTINGS,
    DOWNLOADS,
    HISTORY
}

fun resolveVector(type: TrakasIconType, style: IconStyle, active: Boolean): ImageVector {
    return when (style) {
        IconStyle.MINIMAL -> when (type) {
            TrakasIconType.HOME -> Icons.Outlined.Home
            TrakasIconType.LIBRARY -> Icons.Outlined.LibraryMusic
            TrakasIconType.SONGS -> Icons.Outlined.MusicNote
            TrakasIconType.ALBUMS -> Icons.Outlined.Album
            TrakasIconType.ARTISTS -> Icons.Outlined.Person
            TrakasIconType.PLAYLISTS -> Icons.Outlined.QueueMusic
            TrakasIconType.FAVORITES -> Icons.Outlined.FavoriteBorder
            TrakasIconType.EQUALIZER -> Icons.Outlined.Equalizer
            TrakasIconType.VISUALIZER -> Icons.Outlined.GraphicEq
            TrakasIconType.EXPLORE -> Icons.Outlined.AutoAwesome
            TrakasIconType.SETTINGS -> Icons.Outlined.Settings
            TrakasIconType.DOWNLOADS -> Icons.Outlined.Download
            TrakasIconType.HISTORY -> Icons.Outlined.History
        }
        IconStyle.TRAKAS -> when (type) {
            TrakasIconType.HOME -> Icons.Rounded.Home
            TrakasIconType.LIBRARY -> Icons.Rounded.LibraryMusic
            TrakasIconType.SONGS -> Icons.Rounded.MusicNote
            TrakasIconType.ALBUMS -> Icons.Rounded.Album
            TrakasIconType.ARTISTS -> Icons.Rounded.Person
            TrakasIconType.PLAYLISTS -> Icons.Rounded.QueueMusic
            TrakasIconType.FAVORITES -> Icons.Rounded.Favorite
            TrakasIconType.EQUALIZER -> Icons.Filled.Tune
            TrakasIconType.VISUALIZER -> Icons.Rounded.GraphicEq
            TrakasIconType.EXPLORE -> Icons.Filled.Insights
            TrakasIconType.SETTINGS -> Icons.Rounded.Settings
            TrakasIconType.DOWNLOADS -> Icons.Rounded.Download
            TrakasIconType.HISTORY -> Icons.Rounded.History
        }
        IconStyle.RETRO -> when (type) {
            TrakasIconType.HOME -> Icons.Filled.Home
            TrakasIconType.LIBRARY -> Icons.Filled.LibraryMusic
            TrakasIconType.SONGS -> Icons.Filled.MusicNote
            TrakasIconType.ALBUMS -> Icons.Filled.Album
            TrakasIconType.ARTISTS -> Icons.Filled.Person
            TrakasIconType.PLAYLISTS -> Icons.Filled.QueueMusic
            TrakasIconType.FAVORITES -> Icons.Filled.Favorite
            TrakasIconType.EQUALIZER -> Icons.Filled.Equalizer
            TrakasIconType.VISUALIZER -> Icons.Filled.GraphicEq
            TrakasIconType.EXPLORE -> Icons.Filled.AutoAwesome
            TrakasIconType.SETTINGS -> Icons.Filled.Settings
            TrakasIconType.DOWNLOADS -> Icons.Filled.Download
            TrakasIconType.HISTORY -> Icons.Filled.History
        }
        IconStyle.MATERIAL, IconStyle.NEON, IconStyle.CYBER -> {
            if (active || style != IconStyle.MATERIAL) {
                when (type) {
                    TrakasIconType.HOME -> Icons.Rounded.Home
                    TrakasIconType.LIBRARY -> Icons.Rounded.LibraryMusic
                    TrakasIconType.SONGS -> Icons.Rounded.MusicNote
                    TrakasIconType.ALBUMS -> Icons.Rounded.Album
                    TrakasIconType.ARTISTS -> Icons.Rounded.Person
                    TrakasIconType.PLAYLISTS -> Icons.Rounded.QueueMusic
                    TrakasIconType.FAVORITES -> Icons.Rounded.Favorite
                    TrakasIconType.EQUALIZER -> Icons.Rounded.Equalizer
                    TrakasIconType.VISUALIZER -> Icons.Rounded.GraphicEq
                    TrakasIconType.EXPLORE -> Icons.Rounded.AutoAwesome
                    TrakasIconType.SETTINGS -> Icons.Rounded.Settings
                    TrakasIconType.DOWNLOADS -> Icons.Rounded.Download
                    TrakasIconType.HISTORY -> Icons.Rounded.History
                }
            } else {
                when (type) {
                    TrakasIconType.HOME -> Icons.Outlined.Home
                    TrakasIconType.LIBRARY -> Icons.Outlined.LibraryMusic
                    TrakasIconType.SONGS -> Icons.Outlined.MusicNote
                    TrakasIconType.ALBUMS -> Icons.Outlined.Album
                    TrakasIconType.ARTISTS -> Icons.Outlined.Person
                    TrakasIconType.PLAYLISTS -> Icons.Outlined.QueueMusic
                    TrakasIconType.FAVORITES -> Icons.Outlined.FavoriteBorder
                    TrakasIconType.EQUALIZER -> Icons.Outlined.Equalizer
                    TrakasIconType.VISUALIZER -> Icons.Outlined.GraphicEq
                    TrakasIconType.EXPLORE -> Icons.Outlined.AutoAwesome
                    TrakasIconType.SETTINGS -> Icons.Outlined.Settings
                    TrakasIconType.DOWNLOADS -> Icons.Outlined.Download
                    TrakasIconType.HISTORY -> Icons.Outlined.History
                }
            }
        }
    }
}

@Composable
fun TrakasStyledIcon(
    type: TrakasIconType,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    size: Dp = 24.dp,
    tintOverride: Color? = null,
    showBadgeContainer: Boolean = false
) {
    val tokens = LocalTrakasTokens.current
    val style = LocalIconStyle.current
    val vector = resolveVector(type, style, active)
    val baseTint = tintOverride ?: if (active) tokens.primary else tokens.mutedTextColor

    if (!showBadgeContainer && style == IconStyle.MATERIAL) {
        Icon(
            imageVector = vector,
            contentDescription = contentDescription,
            tint = baseTint,
            modifier = modifier.size(size)
        )
        return
    }

    val containerShape: Shape = when (style) {
        IconStyle.CYBER -> CutCornerShape(7.dp)
        IconStyle.NEON -> RoundedCornerShape(10.dp)
        IconStyle.TRAKAS -> CircleShape
        IconStyle.RETRO -> RoundedCornerShape(4.dp)
        IconStyle.MINIMAL, IconStyle.MATERIAL -> RoundedCornerShape(10.dp)
    }

    if (showBadgeContainer) {
        val bgBrush = when (style) {
            IconStyle.NEON -> Brush.linearGradient(
                listOf(tokens.primary.copy(alpha = 0.24f), tokens.secondary.copy(alpha = 0.18f))
            )
            IconStyle.CYBER -> Brush.linearGradient(
                listOf(tokens.primary.copy(alpha = 0.28f), tokens.surfaceElevated)
            )
            IconStyle.TRAKAS -> Brush.radialGradient(
                listOf(tokens.secondary.copy(alpha = 0.28f), tokens.primary.copy(alpha = 0.14f))
            )
            IconStyle.RETRO -> Brush.linearGradient(
                listOf(tokens.secondary.copy(alpha = 0.30f), tokens.primary.copy(alpha = 0.22f))
            )
            IconStyle.MINIMAL -> Brush.linearGradient(
                listOf(Color.Transparent, Color.Transparent)
            )
            IconStyle.MATERIAL -> Brush.linearGradient(
                listOf(tokens.primary.copy(alpha = 0.16f), tokens.primary.copy(alpha = 0.16f))
            )
        }
        val borderColor = when (style) {
            IconStyle.NEON -> tokens.primary.copy(alpha = 0.65f)
            IconStyle.CYBER -> tokens.secondary.copy(alpha = 0.60f)
            IconStyle.TRAKAS -> tokens.primary.copy(alpha = 0.55f)
            IconStyle.RETRO -> tokens.textColor.copy(alpha = 0.35f)
            IconStyle.MINIMAL -> tokens.mutedTextColor.copy(alpha = 0.25f)
            IconStyle.MATERIAL -> Color.Transparent
        }

        Box(
            modifier = modifier
                .size(size + 14.dp)
                .clip(containerShape)
                .background(bgBrush)
                .then(
                    if (borderColor != Color.Transparent) {
                        Modifier.border(1.dp, borderColor, containerShape)
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = vector,
                contentDescription = contentDescription,
                tint = tintOverride ?: tokens.primary,
                modifier = Modifier.size(size)
            )
        }
    } else {
        Icon(
            imageVector = vector,
            contentDescription = contentDescription,
            tint = baseTint,
            modifier = modifier.size(size)
        )
    }
}
