package com.arh.event.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.arh.event.MainActivity
import com.arh.event.R

object NotificationsHelper {

    private const val CHANNEL_ID = "event_channel"
    private const val CHANNEL_NAME = "События Архангельск"
    private const val CHANNEL_DESC = "Уведомления о происшествиях"

    fun createChannel(context: Context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun sendNotification(context: Context, title: String, priority: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Новое событие")
            .setContentText(title)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        when (priority) {
            0 -> builder.setPriority(NotificationCompat.PRIORITY_LOW)
                .setSilent(true)

            1 -> builder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVibrate(longArrayOf(0, 400, 100, 400))
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))

            2 -> builder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVibrate(longArrayOf(0, 600, 200, 600))
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))

            3 -> builder.setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVibrate(longArrayOf(0, 800, 300, 800, 200, 800))
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))

            4 -> builder.setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVibrate(longArrayOf(0, 1000, 400, 1000, 400, 1000))
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                .setFullScreenIntent(pendingIntent, true)

            5 -> builder.setPriority(NotificationCompat.PRIORITY_MAX)
                .setVibrate(longArrayOf(0, 1500, 500, 1500, 500, 1500, 500, 1500))
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                .setFullScreenIntent(pendingIntent, true)
                .setLights(Color.RED, 1000, 1000)
        }

        NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), builder.build())
    }

}
