package com.midam.guardian.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.midam.guardian.MainActivity
import com.midam.guardian.R
import com.midam.guardian.presentation.screen.notifications.NotificationsViewModel

class NotificationService(private val context: Context) {
    private val TAG = "NotificationService"
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val channelId = "emergency_channel"
    private val channelName = "Alertas de Emergencia"
    private val channelDescription = "Canal para notificaciones de emergencia"
    private var viewModel: NotificationsViewModel? = null

    init {
        createNotificationChannel()
    }

    fun setViewModel(viewModel: NotificationsViewModel) {
        Log.d(TAG, "ViewModel configurado en NotificationService")
        this.viewModel = viewModel
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = channelDescription
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showEmergencyNotification(message: String) {
        try {
            Log.d(TAG, "Procesando notificación de emergencia: $message")
            
            // Guardar en la base de datos a través del ViewModel
            if (viewModel != null) {
                viewModel?.addNotification("¡Alerta de Emergencia!", message)
                Log.d(TAG, "Notificación enviada al ViewModel")
            } else {
                Log.e(TAG, "ViewModel es null, no se puede guardar la notificación")
            }

            // Mostrar notificación del sistema
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("¡Alerta de Emergencia!")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
            Log.d(TAG, "Notificación del sistema mostrada")
        } catch (e: Exception) {
            Log.e(TAG, "Error al mostrar notificación: ${e.message}")
            e.printStackTrace()
        }
    }
} 