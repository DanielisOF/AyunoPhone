package cl.ortiz.ayuno

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import org.json.JSONArray
import org.json.JSONObject

data class FastRecord(val startMs: Long, val endMs: Long, val goalHours: Int) {
    val durationMs get() = endMs - startMs
    val reachedGoal get() = durationMs >= goalHours * 3_600_000L
}

object FastStore {
    const val CHANNEL_ID = "ayuno_meta"
    private const val PREFS = "ayuno"
    private const val GOAL_CODE = 4001
    private const val REFRESH_CODE = 4002

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun startMs(c: Context): Long = prefs(c).getLong("start", 0L)
    fun goalHours(c: Context): Int = prefs(c).getInt("goal", 16)

    fun start(c: Context, goalHours: Int, at: Long = System.currentTimeMillis()) {
        prefs(c).edit().putLong("start", at).putInt("goal", goalHours).apply()
        scheduleGoal(c)
        scheduleRefresh(c)
        FastWidget.refresh(c)
    }

    /** minutos > 0 adelanta el inicio (empezaste antes); < 0 lo atrasa. Nunca queda en el futuro. */
    fun shiftStart(c: Context, minutes: Int) {
        val s = startMs(c)
        if (s == 0L) return
        val nuevo = (s - minutes * 60_000L).coerceAtMost(System.currentTimeMillis())
        prefs(c).edit().putLong("start", nuevo).apply()
        cancelGoal(c)
        scheduleGoal(c)
        FastWidget.refresh(c)
    }

    fun end(c: Context) {
        val s = startMs(c)
        if (s == 0L) return
        val rec = FastRecord(s, System.currentTimeMillis(), goalHours(c))
        val arr = JSONArray(prefs(c).getString("history", "[]"))
        arr.put(JSONObject().put("s", rec.startMs).put("e", rec.endMs).put("g", rec.goalHours))
        val trimmed = JSONArray()
        for (i in maxOf(0, arr.length() - 100) until arr.length()) trimmed.put(arr.get(i))
        prefs(c).edit().putString("history", trimmed.toString()).putLong("start", 0L).apply()
        cancelGoal(c)
        cancelRefresh(c)
        FastWidget.refresh(c)
    }

    fun history(c: Context): List<FastRecord> {
        val arr = JSONArray(prefs(c).getString("history", "[]"))
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            FastRecord(o.getLong("s"), o.getLong("e"), o.getInt("g"))
        }.reversed()
    }

    /** Registros de peso (fecha, kg), del más antiguo al más reciente. */
    fun weights(c: Context): List<Pair<Long, Float>> {
        val arr = JSONArray(prefs(c).getString("weights", "[]"))
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            o.getLong("t") to o.getDouble("kg").toFloat()
        }
    }

    fun addWeight(c: Context, kg: Float) {
        val arr = JSONArray(prefs(c).getString("weights", "[]"))
        arr.put(JSONObject().put("t", System.currentTimeMillis()).put("kg", kg.toDouble()))
        val trimmed = JSONArray()
        for (i in maxOf(0, arr.length() - 300) until arr.length()) trimmed.put(arr.get(i))
        prefs(c).edit().putString("weights", trimmed.toString()).apply()
    }

    fun ensureChannel(c: Context) {
        val nm = c.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Meta de ayuno", NotificationManager.IMPORTANCE_HIGH)
            )
        }
    }

    private fun pending(c: Context, code: Int, cls: Class<*>): PendingIntent = PendingIntent.getBroadcast(
        c, code, Intent(c, cls),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    fun scheduleGoal(c: Context) {
        val s = startMs(c)
        if (s == 0L) return
        val at = s + goalHours(c) * 3_600_000L
        if (at <= System.currentTimeMillis()) return
        // Alarma inexacta (no requiere permiso de alarmas exactas; puede variar unos minutos)
        c.getSystemService(AlarmManager::class.java)
            .setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending(c, GOAL_CODE, GoalReceiver::class.java))
    }

    fun cancelGoal(c: Context) {
        c.getSystemService(AlarmManager::class.java).cancel(pending(c, GOAL_CODE, GoalReceiver::class.java))
    }

    /** Refresca el widget cada ~15 min mientras haya un ayuno activo. */
    fun scheduleRefresh(c: Context) {
        val every = 15 * 60_000L
        c.getSystemService(AlarmManager::class.java).setInexactRepeating(
            AlarmManager.RTC, System.currentTimeMillis() + every, every,
            pending(c, REFRESH_CODE, WidgetRefreshReceiver::class.java)
        )
    }

    fun cancelRefresh(c: Context) {
        c.getSystemService(AlarmManager::class.java)
            .cancel(pending(c, REFRESH_CODE, WidgetRefreshReceiver::class.java))
    }

    /** Reprograma alarmas tras reiniciar el teléfono. */
    fun restoreAlarms(c: Context) {
        if (startMs(c) != 0L) {
            scheduleGoal(c)
            scheduleRefresh(c)
        }
    }
}
