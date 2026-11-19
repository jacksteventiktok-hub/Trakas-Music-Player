package com.example.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.palette.graphics.Palette
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.ui.theme.LocalTrakasTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ArtworkImage(
    artUri: Uri?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val tokens = LocalTrakasTokens.current

    if (artUri == null) {
        Image(
            painter = painterResource(id = R.drawable.img_default_album_art),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    } else {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(artUri)
                .size(512)
                .crossfade(tokens.animationsEnabled)
                .build(),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(tokens.surfaceElevated)
                )
            },
            error = {
                Image(
                    painter = painterResource(id = R.drawable.img_default_album_art),
                    contentDescription = contentDescription,
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize()
                )
            }
        )
    }
}

@Composable
fun rememberDynamicArtColors(
    artUri: Uri?,
    fallbackPrimary: Color,
    fallbackSecondary: Color
): Pair<Color, Color> {
    val context = LocalContext.current
    var colors by remember(artUri, fallbackPrimary, fallbackSecondary) {
        mutableStateOf(fallbackPrimary to fallbackSecondary)
    }

    LaunchedEffect(artUri, fallbackPrimary, fallbackSecondary) {
        if (artUri == null) {
            colors = fallbackPrimary to fallbackSecondary
            return@LaunchedEffect
        }
        val extracted = extractPaletteColors(context, artUri)
        colors = extracted ?: (fallbackPrimary to fallbackSecondary)
    }

    return colors
}

private suspend fun extractPaletteColors(context: Context, uri: Uri): Pair<Color, Color>? =
    withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                val bmp = BitmapFactory.decodeStream(stream, null, opts) ?: return@withContext null
                val palette = Palette.from(bmp).generate()
                bmp.recycle()
                val vibrant = palette.getVibrantColor(0)
                val darkMuted = palette.getDarkMutedColor(0)
                val dominant = palette.getDominantColor(0)
                val c1 = when {
                    vibrant != 0 -> Color(vibrant)
                    dominant != 0 -> Color(dominant)
                    else -> return@withContext null
                }
                val c2 = when {
                    darkMuted != 0 -> Color(darkMuted)
                    else -> c1.copy(alpha = 0.6f)
                }
                c1 to c2
            }
        }.getOrNull()
    }
