package cl.ortiz.ayuno

import android.content.Context
import android.content.Intent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.LocalContext
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val Fondo = ColorProvider(Color(0xFF0B1623))
private val Blanco = ColorProvider(Color.White)
private val Gris = ColorProvider(Color(0xFF8A94A6))
private val Verde = ColorProvider(Color(0xFF3DDC97))
private val Pista = ColorProvider(Color(0xFF22344D))
private val Boton = ColorProvider(Color(0xFF1E3A5F))

/** Widget de pantalla principal: horas de ayuno, barra de progreso y botón de inicio rápido. */
class FastWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val start = FastStore.startMs(context)
        val goal = FastStore.goalHours(context)
        val now = System.currentTimeMillis()
        provideContent { Contenido(start, goal, now) }
    }

    companion object {
        fun refresh(c: Context) {
            val app = c.applicationContext
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    FastWidget().updateAll(app)
                } catch (_: Exception) {
                }
            }
        }
    }
}

@Composable
private fun Contenido(start: Long, goal: Int, now: Long) {
    val ctx = LocalContext.current
    Box(
        modifier = GlanceModifier.fillMaxSize()
            .appWidgetBackground()
            .background(Fondo)
            .cornerRadius(16.dp)
            .clickable(actionStartActivity(Intent(ctx, MainActivity::class.java))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (start == 0L) {
                Text("Sin ayuno activo", style = TextStyle(color = Blanco, fontSize = 15.sp, fontWeight = FontWeight.Bold))
                Spacer(GlanceModifier.height(8.dp))
                Box(
                    modifier = GlanceModifier.background(Boton).cornerRadius(12.dp)
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                        .clickable(actionRunCallback<StartFastAction>()),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Iniciar $goal h", style = TextStyle(color = Blanco, fontSize = 14.sp))
                }
            } else {
                val goalMs = goal * 3_600_000L
                val elapsed = (now - start).coerceAtLeast(0L)
                val cumplida = elapsed >= goalMs
                val frac = (elapsed.toFloat() / goalMs).coerceIn(0f, 1f)
                val endTime = start + goalMs
                val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
                val endStr = fmt.format(Date(endTime))
                Text("Ayuno · meta $goal h · termina $endStr", style = TextStyle(color = Gris, fontSize = 11.sp))
                Text(hm(elapsed), style = TextStyle(color = Blanco, fontSize = 28.sp, fontWeight = FontWeight.Bold))
                Spacer(GlanceModifier.height(4.dp))
                LinearProgressIndicator(
                    progress = frac,
                    modifier = GlanceModifier.fillMaxWidth(),
                    color = Verde,
                    backgroundColor = Pista
                )
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    if (cumplida) "¡Meta cumplida!" else "Faltan ${hm(goalMs - elapsed)} · fin $endStr",
                    style = TextStyle(color = if (cumplida) Verde else Blanco, fontSize = 12.sp)
                )
            }
        }
    }
}

private fun hm(ms: Long): String {
    val m = ms / 60_000
    return "${m / 60} h ${"%02d".format(m % 60)} min"
}

class FastWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FastWidget()
}

/** Botón "Iniciar" del widget: arranca un ayuno con la última meta usada. */
class StartFastAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        FastStore.start(context, FastStore.goalHours(context))
        FastWidget().updateAll(context)
    }
}
