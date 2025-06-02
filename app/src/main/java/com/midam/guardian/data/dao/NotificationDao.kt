package com.midam.guardian.data.dao

import androidx.room.*
import com.midam.guardian.model.Notification
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<Notification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: Notification)

    @Delete
    suspend fun deleteNotification(notification: Notification)

    @Query("DELETE FROM notifications")
    suspend fun deleteAllNotifications()

    @Query("UPDATE notifications SET isRead = :isRead WHERE id = :notificationId")
    suspend fun updateReadStatus(notificationId: Long, isRead: Boolean)
} 