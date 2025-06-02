package com.midam.guardian.presentation.screen.notifications

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.midam.guardian.data.AppDatabase
import com.midam.guardian.model.Notification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NotificationsViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "NotificationsViewModel"
    private val database = AppDatabase.getDatabase(application)
    private val notificationDao = database.notificationDao()

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    init {
        loadNotifications()
    }

    private fun loadNotifications() {
        viewModelScope.launch {
            notificationDao.getAllNotifications().collectLatest { notifications ->
                Log.d(TAG, "Notificaciones cargadas: ${notifications.size}")
                _notifications.value = notifications
            }
        }
    }

    fun addNotification(title: String, message: String) {
        viewModelScope.launch {
            try {
                val notification = Notification(
                    title = title,
                    message = message,
                    timestamp = System.currentTimeMillis()
                )
                Log.d(TAG, "Guardando notificación: $title - $message")
                notificationDao.insertNotification(notification)
                Log.d(TAG, "Notificación guardada exitosamente")
            } catch (e: Exception) {
                Log.e(TAG, "Error al guardar notificación: ${e.message}")
            }
        }
    }

    fun deleteNotification(notification: Notification) {
        viewModelScope.launch {
            notificationDao.deleteNotification(notification)
        }
    }

    fun markAsRead(notificationId: Long) {
        viewModelScope.launch {
            notificationDao.updateReadStatus(notificationId, true)
        }
    }

    fun deleteAllNotifications() {
        viewModelScope.launch {
            notificationDao.deleteAllNotifications()
        }
    }
}