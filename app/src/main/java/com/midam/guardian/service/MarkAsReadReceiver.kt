package com.midam.guardian.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class MarkAsReadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("MarkAsReadReceiver", "onReceive llamado con action: ${intent.action}")
        
        if (intent.getBooleanExtra("mark_read", false)) {
            Log.d("MarkAsReadReceiver", "Procesando marcado como leído")
            
            // Usar el método del servicio para eliminar la notificación
            val notificationService = NotificationService(context)
            notificationService.dismissEmergencyNotification()
            
            Log.d("MarkAsReadReceiver", "Comando de eliminación enviado al servicio")
        }
    }
} 