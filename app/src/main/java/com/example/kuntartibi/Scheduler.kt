package com.example.kuntartibi

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object Scheduler {
    fun planOf(dow: Int): Int = when (dow) {
        Calendar.MONDAY, Calendar.WEDNESDAY, Calendar.FRIDAY -> 0
        Calendar.TUESDAY, Calendar.THURSDAY, Calendar.SATURDAY -> 1
        else -> -1 // yakshanba: dam
    }

    fun nextTime(t: Task): Long {
        val now = System.currentTimeMillis()
        for (d in 0..7) {
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, d)
            c.set(Calendar.HOUR_OF_DAY, t.hour)
            c.set(Calendar.MINUTE, t.minute)
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)
            if (planOf(c.get(Calendar.DAY_OF_WEEK)) == t.plan && c.timeInMillis > now + 1000) return c.timeInMillis
        }
        return -1
    }

    private fun pi(c: Context, id: Int) = PendingIntent.getBroadcast(
        c, id, Intent(c, AlarmReceiver::class.java).putExtra("id", id),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    fun schedule(c: Context, t: Task) {
        val am = c.getSystemService(AlarmManager::class.java)
        val at = nextTime(t)
        if (at < 0) return
        val show = PendingIntent.getActivity(
            c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        // setAlarmClock: Doze rejimida ham aniq vaqtda ishlaydi, maxsus ruxsat kerak emas
        am.setAlarmClock(AlarmManager.AlarmClockInfo(at, show), pi(c, t.id))
    }

    fun cancel(c: Context, t: Task) {
        c.getSystemService(AlarmManager::class.java).cancel(pi(c, t.id))
    }

    fun scheduleAll(c: Context) = Store.load(c).forEach { schedule(c, it) }
}
