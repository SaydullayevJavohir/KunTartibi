package com.example.kuntartibi

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Calendar

class MainActivity : Activity() {
    private var plan = 0
    private lateinit var tasks: MutableList<Task>
    private lateinit var list: LinearLayout
    private lateinit var info: TextView
    private lateinit var tabs: List<Button>

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        Notifier.ensureChannel(this)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)

        tasks = Store.load(this)
        val today = Scheduler.planOf(Calendar.getInstance().get(Calendar.DAY_OF_WEEK))
        plan = if (today == 1) 1 else 0

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(40), dp(16), dp(24))
        }
        box.addView(TextView(this).apply {
            text = "Kun tartibi"; textSize = 26f; setTypeface(null, Typeface.BOLD)
        })

        val row = LinearLayout(this)
        tabs = listOf("Toq kunlar\nDu · Chor · Ju", "Juft kunlar\nSesh · Pay · Sha").mapIndexed { i, s ->
            Button(this).apply {
                text = s; isAllCaps = false
                setOnClickListener { plan = i; render() }
                row.addView(this, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            }
        }
        box.addView(row)

        info = TextView(this).apply { setPadding(0, dp(8), 0, dp(8)) }
        box.addView(info)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(list)

        box.addView(Button(this).apply {
            text = "+ Yangi ish qo'shish"
            setOnClickListener { addTask() }
        })

        box.addView(TextView(this).apply {
            text = "\nFonda ishlashi uchun (Xiaomi/Redmi):"
            setTypeface(null, Typeface.BOLD)
        })
        fun btn(label: String, vararg intents: Intent) = box.addView(Button(this).apply {
            text = label; isAllCaps = false
            setOnClickListener { tryStart(*intents) }
        })
        val pkg = packageName
        btn("1. Batareya optimizatsiyasini o'chirish",
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$pkg")))
        btn("2. Avtostartni yoqish",
            Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
            Intent("miui.intent.action.OP_AUTO_START").addCategory(Intent.CATEGORY_DEFAULT))
        btn("3. Batareya: «Cheklovsiz» qilish",
            Intent().setComponent(ComponentName("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"))
                .putExtra("package_name", pkg).putExtra("package_label", "Kun tartibi"),
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg")))
        btn("4. Bildirishnoma sozlamalari",
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, pkg))
        box.addView(Button(this).apply {
            text = "Sinov signali"; isAllCaps = false
            setOnClickListener { Notifier.show(this@MainActivity, 9999, "Sinov", "Signal shunday eshitiladi") }
        })
        box.addView(TextView(this).apply {
            text = "\nMuhim: «Oxirgi ilovalar» ro'yxatida ilova kartochkasini ushlab turib, qulf (🔒) belgisini bosing."
        })

        setContentView(ScrollView(this).apply { addView(box) })
        render()
        Scheduler.scheduleAll(this)

        val p = getSharedPreferences("app", MODE_PRIVATE)
        if (!p.getBoolean("intro", false)) {
            p.edit().putBoolean("intro", true).apply()
            AlertDialog.Builder(this)
                .setTitle("Muhim sozlamalar")
                .setMessage("Xiaomi ilovani fonda o'chirib qo'yishi mumkin. Pastdagi 1–3 tugmalarni bosib, ruxsat bering va ilovani «Oxirgi ilovalar»da qulflang.")
                .setPositiveButton("Tushundim", null).show()
        }
    }

    private fun tryStart(vararg intents: Intent) {
        for (i in intents) {
            try { startActivity(i); return } catch (_: Exception) {}
        }
        try {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        } catch (_: Exception) {}
    }

    private fun render() {
        tabs.forEachIndexed { i, t -> t.alpha = if (i == plan) 1f else 0.45f }
        val today = Scheduler.planOf(Calendar.getInstance().get(Calendar.DAY_OF_WEEK))
        info.text = when (today) {
            0 -> "Bugun: toq kun rejasi"
            1 -> "Bugun: juft kun rejasi"
            else -> "Bugun: yakshanba — dam olish kuni"
        }
        list.removeAllViews()
        tasks.filter { it.plan == plan }.sortedBy { it.hour * 60 + it.minute }.forEach { t ->
            val r = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            }
            r.addView(Button(this).apply {
                text = String.format("%02d:%02d", t.hour, t.minute)
                setOnClickListener { pickTime(t.hour, t.minute) { h, m -> t.hour = h; t.minute = m; changed(t) } }
            })
            r.addView(TextView(this).apply {
                text = t.title; textSize = 17f; setPadding(dp(10), dp(10), dp(10), dp(10))
                setOnClickListener {
                    askTitle("Ish nomi", t.title) { s -> t.title = s; changed(t) }
                }
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            r.addView(Button(this).apply {
                text = "✕"
                setOnClickListener {
                    Scheduler.cancel(this@MainActivity, t)
                    tasks.remove(t); Store.save(this@MainActivity, tasks); render()
                }
            })
            list.addView(r)
        }
    }

    private fun changed(t: Task) {
        Store.save(this, tasks)
        Scheduler.schedule(this, t)
        render()
    }

    private fun pickTime(h: Int, m: Int, done: (Int, Int) -> Unit) {
        TimePickerDialog(this, { _, hh, mm -> done(hh, mm) }, h, m, true).show()
    }

    private fun askTitle(title: String, cur: String, done: (String) -> Unit) {
        val et = EditText(this).apply { setText(cur); setSelection(cur.length) }
        AlertDialog.Builder(this).setTitle(title).setView(et)
            .setPositiveButton("Saqlash") { _, _ ->
                val s = et.text.toString().trim()
                if (s.isNotEmpty()) done(s)
            }
            .setNegativeButton("Bekor", null).show()
    }

    private fun addTask() {
        askTitle("Yangi ish nomi", "") { name ->
            pickTime(8, 0) { h, m ->
                val t = Task(Store.nextId(tasks), plan, h, m, name)
                tasks.add(t); changed(t)
            }
        }
    }
}
