package cl.ortiz.ayuno

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

/** Notificación cuando se cumple la meta del ayuno. */
class GoalReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (FastStore.startMs(context) == 0L) return
        FastStore.ensureChannel(context)
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(context, FastStore.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle("¡Meta cumplida!")
            .setContentText("Completaste ${FastStore.goalHours(context)} h de ayuno")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(1, n)
        FastWidget.refresh(context)
    }
}

/** Refresca el widget periódicamente. */
class WidgetRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        FastWidget.refresh(context)
    }
}

/** Restaura alarmas al reiniciar el teléfono. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            FastStore.restoreAlarms(context)
            FastWidget.refresh(context)
        }
    }
}
