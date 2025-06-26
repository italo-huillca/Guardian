package com.midam.guardian.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class EmergencyNotificationListenerService : NotificationListenerService() {
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        Log.d("EmergencyListener", "onNotificationRemoved llamado para id: ${sbn?.id}")
        
        if (sbn?.id == NotificationService.EMERGENCY_NOTIFICATION_ID) {
            val prefs = getSharedPreferences("emergency_prefs", MODE_PRIVATE)
            val isRead = prefs.getBoolean("emergency_read", false)
            Log.d("EmergencyListener", "emergency_read: $isRead")
            
            // Solo recrear la notificación si NO fue marcada como leída
            if (!isRead) {
                Log.d("EmergencyListener", "Notificación de emergencia removida sin marcar como leída, re-mostrando")
                val message = sbn.notification.extras.getString("android.text") ?: "¡Emergencia!"
                NotificationService(this).showEmergencyNotification(message)
            } else {
                Log.d("EmergencyListener", "Notificación de emergencia removida y ya marcada como leída, NO se recrea")
                // Asegurar que el estado se mantenga como leído
                prefs.edit().putBoolean("emergency_read", true).apply()
            }
        }
    }
    
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn?.id == NotificationService.EMERGENCY_NOTIFICATION_ID) {
            Log.d("EmergencyListener", "Notificación de emergencia publicada: ${sbn.id}")
        }
    }
} 