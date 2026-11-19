package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.model.VisualizerConfig
import com.example.model.VisualizerStyle
import com.example.player.VisualizerFrame
import com.example.ui.theme.LocalTrakasTokens
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun AudioVisualizerCanvas(
    frame: VisualizerFrame,
    config: VisualizerConfig,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    albumColors: Pair<Color, Color>? = null
) {
    val tokens = LocalTrakasTokens.current
    val useAlbum = (config.useAlbumColors || config.style == VisualizerStyle.ALBUM_COLORS) && albumColors != null
    val primary = if (useAlbum && albumColors != null) albumColors.first else tokens.visualizerColor
    val secondary = if (useAlbum && albumColors != null) albumColors.second else tokens.secondary
    val intensity = (config.intensity * tokens.effectIntensity).coerceIn(0.25f, 1.5f)
    val alpha = config.transparency.coerceIn(0.2f, 1f)

    var phase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isPlaying, config.speed, tokens.animationsEnabled) {
        if (isPlaying && tokens.animationsEnabled) {
            while (true) {
                phase = (phase + 0.05f * config.speed) % (2f * PI.toFloat())
                delay(32L)
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val bars = frame.bars
        val wave = frame.waveform
        val idleScale = if (isPlaying) 1.0f else 0.18f

        when (config.style) {
            VisualizerStyle.BARS -> {
                val count = bars.size.coerceAtLeast(1)
                val gap = w * 0.012f
                val barW = ((w - gap * (count - 1)) / count).coerceAtLeast(2f)
                val brush = Brush.verticalGradient(listOf(secondary.copy(alpha = alpha), primary.copy(alpha = alpha)))
                for (i in 0 until count) {
                    val v = (bars[i] * intensity * idleScale).coerceIn(0.06f, 1f)
                    val barH = h * v
                    val x = i * (barW + gap)
                    drawRoundRect(
                        brush = brush,
                        topLeft = Offset(x, h - barH),
                        size = Size(barW, barH),
                        cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                    )
                }
            }

            VisualizerStyle.SPECTRUM -> {
                val count = bars.size.coerceAtLeast(1)
                val gap = w * 0.01f
                val barW = ((w - gap * (count - 1)) / count).coerceAtLeast(2f)
                val midY = h / 2f
                for (i in 0 until count) {
                    val v = (bars[i] * intensity * idleScale).coerceIn(0.05f, 1f)
                    val halfH = (h * 0.46f) * v
                    val x = i * (barW + gap)
                    val color = (if (i % 2 == 0) primary else secondary).copy(alpha = alpha)
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x, midY - halfH),
                        size = Size(barW, halfH * 2f),
                        cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                    )
                }
            }

            VisualizerStyle.WAVEFORM -> {
                val path = Path()
                val n = wave.size.coerceAtLeast(2)
                val midY = h / 2f
                for (i in 0 until n) {
                    val x = (i.toFloat() / (n - 1)) * w
                    val amp = wave[i] * (h * 0.44f) * intensity * idleScale
                    val y = midY + amp
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path = path,
                    brush = Brush.horizontalGradient(
                        listOf(primary.copy(alpha = alpha), secondary.copy(alpha = alpha), primary.copy(alpha = alpha))
                    ),
                    style = Stroke(width = 4.5f, cap = StrokeCap.Round)
                )
                val echo = Path()
                for (i in 0 until n) {
                    val x = (i.toFloat() / (n - 1)) * w
                    val amp = -wave[i] * (h * 0.26f) * intensity * idleScale
                    val y = midY + amp
                    if (i == 0) echo.moveTo(x, y) else echo.lineTo(x, y)
                }
                drawPath(
                    path = echo,
                    color = secondary.copy(alpha = alpha * 0.45f),
                    style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                )
            }

            VisualizerStyle.CIRCLE -> {
                val center = Offset(w / 2f, h / 2f)
                val baseRadius = min(w, h) * 0.28f
                val maxExtend = min(w, h) * 0.20f
                val count = bars.size
                drawCircle(
                    color = primary.copy(alpha = alpha * 0.22f),
                    radius = baseRadius,
                    center = center,
                    style = Stroke(width = 2.5f)
                )
                for (i in 0 until count) {
                    val angle = ((2.0 * PI * i / count) + phase * 0.2f).toFloat()
                    val v = (bars[i] * intensity * idleScale).coerceIn(0.06f, 1f)
                    val rStart = baseRadius
                    val rEnd = baseRadius + maxExtend * v
                    drawLine(
                        color = (if (i % 3 == 0) secondary else primary).copy(alpha = alpha),
                        start = Offset(center.x + cos(angle) * rStart, center.y + sin(angle) * rStart),
                        end = Offset(center.x + cos(angle) * rEnd, center.y + sin(angle) * rEnd),
                        strokeWidth = 4.5f,
                        cap = StrokeCap.Round
                    )
                }
            }

            VisualizerStyle.PARTICLES -> {
                val count = config.particleCount.coerceIn(10, 80)
                val energy = (frame.peakLevel * intensity * idleScale).coerceIn(0.12f, 1.3f)
                for (i in 0 until count) {
                    val barVal = bars[i % bars.size]
                    val px = ((i + 0.5f) / count) * w + sin(phase + i * 0.7f) * 14f
                    val py = h * (0.85f - ((barVal * 0.72f * energy) + ((sin(phase * 1.3f + i) + 1f) * 0.1f)).coerceIn(0.05f, 0.85f))
                    val radius = (3f + barVal * 8f * intensity * idleScale).coerceIn(2f, 14f)
                    val col = if (i % 2 == 0) primary else secondary
                    drawCircle(
                        color = col.copy(alpha = (alpha * 0.35f).coerceIn(0.1f, 0.5f)),
                        radius = radius * 1.8f,
                        center = Offset(px, py)
                    )
                    drawCircle(
                        color = col.copy(alpha = alpha),
                        radius = radius,
                        center = Offset(px, py)
                    )
                }
            }

            VisualizerStyle.ALBUM_COLORS -> {
                val c1 = albumColors?.first ?: primary
                val c2 = albumColors?.second ?: secondary
                val count = bars.size.coerceAtLeast(1)
                val gap = w * 0.012f
                val barW = ((w - gap * (count - 1)) / count).coerceAtLeast(2f)
                val midY = h / 2f
                for (i in 0 until count) {
                    val v = (bars[i] * intensity * idleScale).coerceIn(0.08f, 1f)
                    val barH = h * 0.86f * v
                    val x = i * (barW + gap)
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(c1.copy(alpha = alpha), c2.copy(alpha = alpha))),
                        topLeft = Offset(x, midY - barH / 2f),
                        size = Size(barW, barH),
                        cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                    )
                }
            }
        }
    }
}
