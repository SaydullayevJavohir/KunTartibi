package com.example.kuntartibi

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager

object Notifier {
    private const val CH = "alarm_v1"

    fun ensureChannel(c: Context) {
        val nm = c.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CH) == null) {
            val ch = NotificationChannel(CH, "Kun tartibi signali", NotificationManager.IMPORTANCE_HIGH)
            ch.setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
            )
            ch.enableVibration(true)
            ch.vibrationPattern = longArrayOf(0, 500, 300, 500, 300, 500)
            ch.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            nm.createNotificationChannel(ch)
        }
    }

    fun show(c: Context, id: Int, title: String, text: String) {
        ensureChannel(c)
        val open = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(c, CH)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(Notification.CATEGORY_ALARM)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setTimeoutAfter(60_000)
            .build()
        n.flags = n.flags or Notification.FLAG_INSISTENT // signal takrorlanadi (1 daqiqagacha yoki surib tashlaguncha)
        c.getSystemService(NotificationManager::class.java).notify(id, n)
    }
}
