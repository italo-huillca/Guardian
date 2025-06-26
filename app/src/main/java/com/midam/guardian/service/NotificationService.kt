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
import android.app.Notification
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.app.Service
import android.content.BroadcastReceiver
import android.os.Handler
import android.os.Looper
import android.content.SharedPreferences
import android.os.Vibrator
import android.os.VibrationEffect

class NotificationService(private val context: Context) {
    private val TAG = "NotificationService"
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val channelId = "emergency_channel"
    private val channelName = "Alertas de Emergencia"
    private val channelDescription = "Canal para notificaciones de emergencia"
    private val geofenceChannelId = "geofence_channel"
    private val geofenceChannelName = "Alertas de Geofencing"
    private val geofenceChannelDescription = "Canal para notificaciones de geofencing"
    private var viewModel: NotificationsViewModel? = null
    private var vibrationHandler: Handler? = null
    private var vibrationRunnable: Runnable? = null

    init {
        createNotificationChannel()
        createGeofenceChannel()
    }

    fun setViewModel(viewModel: NotificationsViewModel) {
        Log.d(TAG, "ViewModel configurado en NotificationService")
        this.viewModel = viewModel
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = channelDescription
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000, 500, 1000)
                setSound(soundUri, AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
                enableLights(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createGeofenceChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                geofenceChannelId,
                geofenceChannelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = geofenceChannelDescription
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500) // Patrón de vibración personalizado
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun onNewEmergency(message: String) {
        val prefs = context.getSharedPreferences("emergency_prefs", Context.MODE_PRIVATE)
        // Resetear el estado para nueva emergencia
        prefs.edit()
            .putBoolean("emergency_read", false)
            .putLong("emergency_timestamp", System.currentTimeMillis())
            .apply()
        Log.d(TAG, "Flag emergency_read reiniciado a false por nueva emergencia")
        stopRepeatingVibration()
        showEmergencyNotification(message)
    }

    fun showEmergencyNotification(message: String) {
        try {
            // Verificar si ya hay una notificación activa y si fue marcada como leída
            val prefs = context.getSharedPreferences("emergency_prefs", Context.MODE_PRIVATE)
            val isRead = prefs.getBoolean("emergency_read", false)
            
            if (isRead) {
                Log.d(TAG, "Notificación ya fue marcada como leída, no se muestra nuevamente")
                return
            }

            // Forzar recreación del canal de emergencia
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationManager.deleteNotificationChannel(channelId)
                createNotificationChannel()
            }
            
            Log.d(TAG, "Procesando notificación de emergencia: $message")
            
            // Guardar en la base de datos a través del ViewModel
            if (viewModel != null) {
                viewModel?.addNotification("¡Alerta de Emergencia!", message)
                Log.d(TAG, "Notificación enviada al ViewModel")
            } else {
                Log.e(TAG, "ViewModel es null, no se puede guardar la notificación")
            }

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("¡Alerta de Emergencia!")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVibrate(longArrayOf(0, 1500, 500, 1500, 500, 1500, 500, 1500))
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                .setAutoCancel(true)
                .setOngoing(false)
                .addAction(
                    R.drawable.ic_check, "Leído",
                    getMarkAsReadPendingIntent(context)
                )
                .build()

            notificationManager.notify(EMERGENCY_NOTIFICATION_ID, notification)
            Log.d(TAG, "Notificación del sistema mostrada (sin startForeground)")
            
            // Iniciar vibración periódica
            startRepeatingVibration()

            // Reintentar mostrar la notificación si no se marca como leída después de 30 segundos
            Handler(Looper.getMainLooper()).postDelayed({
                val active = (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    .activeNotifications.any { it.id == EMERGENCY_NOTIFICATION_ID }
                val stillNotRead = !prefs.getBoolean("emergency_read", false)
                if (active && stillNotRead) {
                    Log.d(TAG, "Reintentando mostrar notificación de emergencia")
                    notificationManager.notify(EMERGENCY_NOTIFICATION_ID, notification)
                }
            }, 30000)
        } catch (e: Exception) {
            Log.e(TAG, "Error al mostrar notificación: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun getMarkAsReadPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MarkAsReadReceiver::class.java)
        intent.putExtra("mark_read", true)
        intent.action = "MARK_AS_READ_ACTION"
        
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        return PendingIntent.getBroadcast(context, 1001, intent, flags)
    }

    fun showGeofenceNotification(title: String, message: String) {
        try {
            Log.d(TAG, "Procesando notificación de geofencing: $title - $message")
            
            // Guardar en la base de datos a través del ViewModel
            viewModel?.addNotification(title, message)
            
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

            val notification = NotificationCompat.Builder(context, geofenceChannelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(longArrayOf(0, 500, 200, 500)) // Mismo patrón que el canal
                .build()

            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
            Log.d(TAG, "Notificación de geofencing mostrada")
        } catch (e: Exception) {
            Log.e(TAG, "Error al mostrar notificación de geofencing: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun startRepeatingVibration() {
        stopRepeatingVibration()
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrationHandler = Handler(Looper.getMainLooper())
        vibrationRunnable = object : Runnable {
            override fun run() {
                val prefs = context.getSharedPreferences("emergency_prefs", Context.MODE_PRIVATE)
                val shouldVibrate = !prefs.getBoolean("emergency_read", false)
                if (shouldVibrate) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        vibrator.vibrate(
                            VibrationEffect.createWaveform(longArrayOf(0, 1000, 500, 1000), -1)
                        )
                    } else {
                        vibrator.vibrate(longArrayOf(0, 1000, 500, 1000), -1)
                    }
                    vibrationHandler?.postDelayed(this, 2500)
                } else {
                    vibrator.cancel()
                    vibrationHandler?.removeCallbacksAndMessages(null)
                }
            }
        }
        vibrationHandler?.post(vibrationRunnable!!)
    }

    fun stopRepeatingVibration() {
        vibrationHandler?.removeCallbacksAndMessages(null)
        vibrationHandler = null
        vibrationRunnable = null
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.cancel()
    }

    fun dismissEmergencyNotification() {
        try {
            Log.d(TAG, "Iniciando proceso de eliminación de notificación de emergencia")
            
            // Marcar como leída PRIMERO
            val prefs = context.getSharedPreferences("emergency_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("emergency_read", true).apply()
            Log.d(TAG, "Estado marcado como leído")
            
            // Detener vibración
            stopRepeatingVibration()
            Log.d(TAG, "Vibración detenida")
            
            // Verificar si la notificación está activa
            val activeNotifications = notificationManager.activeNotifications
            val isActive = activeNotifications.any { it.id == EMERGENCY_NOTIFICATION_ID }
            Log.d(TAG, "Notificación activa antes de eliminar: $isActive")
            
            if (isActive) {
                // Intentar cancelar la notificación
                notificationManager.cancel(EMERGENCY_NOTIFICATION_ID)
                Log.d(TAG, "Comando de cancelación enviado")
                
                // Esperar un momento y verificar
                Handler(Looper.getMainLooper()).postDelayed({
                    val stillActive = notificationManager.activeNotifications.any { it.id == EMERGENCY_NOTIFICATION_ID }
                    Log.d(TAG, "Verificación después de 500ms - Notificación activa: $stillActive")
                    
                    if (stillActive) {
                        Log.w(TAG, "⚠️ La notificación persiste, intentando método alternativo")
                        // Intentar con un ID diferente para forzar la eliminación
                        notificationManager.cancelAll()
                        Handler(Looper.getMainLooper()).postDelayed({
                            // Restaurar solo la notificación de servicio en primer plano
                            if (context is Service) {
                                updateForegroundNotification()
                            }
                        }, 100)
                    } else {
                        Log.d(TAG, "✅ Notificación eliminada exitosamente")
                    }
                }, 500)
            } else {
                Log.d(TAG, "Notificación no estaba activa")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error al eliminar notificación: ${e.message}")
            e.printStackTrace()
        }
    }

    fun createForegroundNotification(): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, geofenceChannelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Guardian Activo")
            .setContentText("Monitoreando ubicación y emergencias")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
    }

    fun updateForegroundNotification() {
        if (context is Service) {
            val notification = createForegroundNotification()
            notificationManager.notify(FOREGROUND_NOTIFICATION_ID, notification)
        }
    }

    companion object {
        const val EMERGENCY_NOTIFICATION_ID = 1001
        const val FOREGROUND_NOTIFICATION_ID = 1002
    }
} 