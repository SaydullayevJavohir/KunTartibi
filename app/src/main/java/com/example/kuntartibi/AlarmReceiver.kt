package com.example.kuntartibi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val id = i.getIntExtra("id", -1)
        val t = Store.load(c).firstOrNull { it.id == id } ?: return
        Notifier.show(c, id, t.title, String.format("%02d:%02d — vaqti keldi", t.hour, t.minute))
        Scheduler.schedule(c, t) // keyingi haftaga (yoki keyingi mos kunga) qayta qo'yadi
    }
}
