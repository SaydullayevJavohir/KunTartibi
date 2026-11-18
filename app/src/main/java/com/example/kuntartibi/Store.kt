package com.example.kuntartibi

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

// plan: 0 = toq kunlar (Du, Chor, Ju), 1 = juft kunlar (Sesh, Pay, Sha)
data class Task(val id: Int, val plan: Int, var hour: Int, var minute: Int, var title: String)

object Store {
    private fun prefs(c: Context) = c.getSharedPreferences("tasks", Context.MODE_PRIVATE)

    fun load(c: Context): MutableList<Task> {
        val s = prefs(c).getString("list", null)
        if (s == null) {
            val d = defaults()
            save(c, d)
            return d
        }
        val a = JSONArray(s)
        val l = mutableListOf<Task>()
        for (i in 0 until a.length()) {
            val o = a.getJSONObject(i)
            l.add(Task(o.getInt("id"), o.getInt("plan"), o.getInt("h"), o.getInt("m"), o.getString("t")))
        }
        return l
    }

    fun save(c: Context, l: List<Task>) {
        val a = JSONArray()
        l.forEach {
            a.put(JSONObject().put("id", it.id).put("plan", it.plan).put("h", it.hour).put("m", it.minute).put("t", it.title))
        }
        prefs(c).edit().putString("list", a.toString()).apply()
    }

    fun nextId(l: List<Task>) = (l.maxOfOrNull { it.id } ?: 0) + 1

    private fun defaults(): MutableList<Task> {
        val l = mutableListOf<Task>()
        var id = 1
        for (plan in 0..1) {
            l.add(Task(id++, plan, 6, 0, "Uyg'onish"))
            l.add(Task(id++, plan, 7, 0, "Nonushta"))
            l.add(Task(id++, plan, 9, 0, "Asosiy ish"))
            l.add(Task(id++, plan, 13, 0, "Tushlik"))
            l.add(Task(id++, plan, 18, 0, "Sport"))
            l.add(Task(id++, plan, 22, 0, "Uxlash"))
        }
        return l
    }
}
