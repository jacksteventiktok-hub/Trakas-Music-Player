package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.player.PlaybackService

object TrakasWidgetUpdater {

    fun updateAll(
        context: Context,
        title: String,
        artist: String,
        isPlaying: Boolean,
        progressPercent: Int,
        bars: FloatArray
    ) {
        val mgr = AppWidgetManager.getInstance(context)

        val smallIds = mgr.getAppWidgetIds(ComponentName(context, TrakasSmallWidgetProvider::class.java))
        smallIds.forEach { id ->
            val rv = buildSmallViews(context, title, isPlaying)
            mgr.updateAppWidget(id, rv)
        }

        val medIds = mgr.getAppWidgetIds(ComponentName(context, TrakasMediumWidgetProvider::class.java))
        medIds.forEach { id ->
            val rv = buildMediumViews(context, title, artist, isPlaying)
            mgr.updateAppWidget(id, rv)
        }

        val largeIds = mgr.getAppWidgetIds(ComponentName(context, TrakasLargeWidgetProvider::class.java))
        if (largeIds.isNotEmpty()) {
            val visBitmap = renderMiniVisualizerBitmap(bars, isPlaying)
            largeIds.forEach { id ->
                val rv = buildLargeViews(context, title, artist, isPlaying, progressPercent, visBitmap)
                mgr.updateAppWidget(id, rv)
            }
        }
    }

    private fun servicePendingIntent(context: Context, action: String, reqCode: Int): PendingIntent {
        val intent = Intent(context, PlaybackService::class.java).apply { this.action = action }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(context, reqCode, intent, flags)
        } else {
            PendingIntent.getService(context, reqCode, intent, flags)
        }
    }

    private fun launchPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return PendingIntent.getActivity(
            context,
            99,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    internal fun buildSmallViews(context: Context, title: String, isPlaying: Boolean): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.widget_trakas_small)
        rv.setTextViewText(R.id.widget_title, title)
        rv.setImageViewResource(
            R.id.widget_play_pause,
            if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        )
        rv.setOnClickPendingIntent(R.id.widget_root, launchPendingIntent(context))
        rv.setOnClickPendingIntent(
            R.id.widget_play_pause,
            servicePendingIntent(context, PlaybackService.ACTION_PLAY_PAUSE, 10)
        )
        return rv
    }

    internal fun buildMediumViews(context: Context, title: String, artist: String, isPlaying: Boolean): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.widget_trakas_medium)
        rv.setTextViewText(R.id.widget_title, title)
        rv.setTextViewText(R.id.widget_artist, artist)
        rv.setImageViewResource(
            R.id.widget_play_pause,
            if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        )
        rv.setOnClickPendingIntent(R.id.widget_root, launchPendingIntent(context))
        rv.setOnClickPendingIntent(
            R.id.widget_prev,
            servicePendingIntent(context, PlaybackService.ACTION_PREVIOUS, 11)
        )
        rv.setOnClickPendingIntent(
            R.id.widget_play_pause,
            servicePendingIntent(context, PlaybackService.ACTION_PLAY_PAUSE, 12)
        )
        rv.setOnClickPendingIntent(
            R.id.widget_next,
            servicePendingIntent(context, PlaybackService.ACTION_NEXT, 13)
        )
        return rv
    }

    internal fun buildLargeViews(
        context: Context,
        title: String,
        artist: String,
        isPlaying: Boolean,
        progressPercent: Int,
        visBitmap: Bitmap
    ): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.widget_trakas_large)
        rv.setTextViewText(R.id.widget_title, title)
        rv.setTextViewText(R.id.widget_artist, artist)
        rv.setProgressBar(R.id.widget_progress, 100, progressPercent.coerceIn(0, 100), false)
        rv.setImageViewBitmap(R.id.widget_visualizer, visBitmap)
        rv.setImageViewResource(
            R.id.widget_play_pause,
            if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        )
        rv.setOnClickPendingIntent(R.id.widget_root, launchPendingIntent(context))
        rv.setOnClickPendingIntent(
            R.id.widget_prev,
            servicePendingIntent(context, PlaybackService.ACTION_PREVIOUS, 21)
        )
        rv.setOnClickPendingIntent(
            R.id.widget_play_pause,
            servicePendingIntent(context, PlaybackService.ACTION_PLAY_PAUSE, 22)
        )
        rv.setOnClickPendingIntent(
            R.id.widget_next,
            servicePendingIntent(context, PlaybackService.ACTION_NEXT, 23)
        )
        return rv
    }

    internal fun renderMiniVisualizerBitmap(bars: FloatArray, isPlaying: Boolean): Bitmap {
        val w = 320
        val h = 64
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val count = 24
        val gap = 4f
        val barW = (w - gap * (count - 1)) / count
        for (i in 0 until count) {
            val mag = if (isPlaying) bars.getOrElse(i % bars.size) { 0.25f }.coerceIn(0.1f, 1f) else 0.14f
            val barH = (h * mag).coerceAtLeast(6f)
            paint.color = if (i % 2 == 0) 0xFF00E5FF.toInt() else 0xFF7C4DFF.toInt()
            val x = i * (barW + gap)
            canvas.drawRoundRect(x, h - barH, x + barW, h.toFloat(), 4f, 4f, paint)
        }
        return bmp
    }
}

class TrakasSmallWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(
                id,
                TrakasWidgetUpdater.buildSmallViews(context, "Trakas Music Player", false)
            )
        }
    }
}

class TrakasMediumWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(
                id,
                TrakasWidgetUpdater.buildMediumViews(
                    context,
                    "Trakas Music Player",
                    "Tu música. Tu mezcla. Tu estilo.",
                    false
                )
            )
        }
    }
}

class TrakasLargeWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val bmp = TrakasWidgetUpdater.renderMiniVisualizerBitmap(FloatArray(24) { 0.2f }, false)
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(
                id,
                TrakasWidgetUpdater.buildLargeViews(
                    context,
                    "Trakas Music Player",
                    "Tu música. Tu mezcla. Tu estilo.",
                    false,
                    0,
                    bmp
                )
            )
        }
    }
}
