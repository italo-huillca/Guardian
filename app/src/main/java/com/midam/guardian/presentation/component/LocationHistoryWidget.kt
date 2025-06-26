package com.midam.guardian.presentation.component

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.midam.guardian.R
import com.midam.guardian.presentation.screen.history.HistoryViewModel
import com.midam.guardian.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import com.midam.guardian.service.LocationHistoryWidgetService
import com.midam.guardian.service.LocationHistoryCallback
import com.midam.guardian.data.model.HistoryLocationData
import android.os.Handler
import android.os.Looper

class LocationHistoryWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_location_history)

            // Lógica para obtener la última ubicación desde Firebase
            LocationHistoryWidgetService.getLastLocation(context, object : LocationHistoryCallback {
                override fun onResult(location: HistoryLocationData?) {
                    val handler = Handler(Looper.getMainLooper())
                    handler.post {
                        if (location != null) {
                            views.setTextViewText(R.id.widget_title, "Última ubicación")
                            views.setTextViewText(R.id.widget_location, location.ubicacion.direccion_completa)
                            views.setTextViewText(R.id.widget_date, location.fecha_legible)
                        } else {
                            views.setTextViewText(R.id.widget_title, "Última ubicación")
                            views.setTextViewText(R.id.widget_location, "No disponible")
                            views.setTextViewText(R.id.widget_date, "Fecha no disponible")
                        }
                        val intent = Intent(context, MainActivity::class.java)
                        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
                        views.setOnClickPendingIntent(R.id.widget_title, pendingIntent)
                        views.setOnClickPendingIntent(R.id.widget_location, pendingIntent)
                        views.setOnClickPendingIntent(R.id.widget_date, pendingIntent)
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                }
            })
        }
    }
} 