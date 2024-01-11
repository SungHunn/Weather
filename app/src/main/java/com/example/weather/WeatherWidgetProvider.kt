package com.example.weather

import android.app.ForegroundServiceStartNotAllowedException
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.core.content.ContextCompat


class WeatherAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        appWidgetIds.forEach { appWidgetId ->
            val pendingIntent: PendingIntent = Intent(context, UpdateWeatherService::class.java)
                .let { intent ->
                    PendingIntent.getForegroundService(
                        context,
                        1,
                        intent,
                        PendingIntent.FLAG_IMMUTABLE
                    )
                }
            val views: RemoteViews = RemoteViews(
                context.packageName,
                R.layout.widget_weather
            ).apply {
                setOnClickPendingIntent(R.id.widgetTextView, pendingIntent)
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
        val serviceIntent = Intent(context, UpdateWeatherService::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                ContextCompat.startForegroundService(context, serviceIntent)

            } catch (e: ForegroundServiceStartNotAllowedException) {
                e.printStackTrace()
            }
        } else {
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }
}