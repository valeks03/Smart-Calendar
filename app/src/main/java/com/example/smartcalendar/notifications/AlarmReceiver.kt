package com.example.smartcalendar.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.smartcalendar.R

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Событие"
        val id = intent.getLongExtra("event_id", 0L).toInt()
        val channelId = "smartcalendar_events"

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_notification_overlay) // можно заменить на свой ico
            .setContentTitle("Скоро событие")
            .setContentText(title)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }
}
