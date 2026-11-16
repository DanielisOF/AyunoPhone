package cl.ortiz.ayuno

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private val Verde = Color(0xFF3DDC97)
private val Azul = Color(0xFF4DA3FF)
private val Fondo = Color(0xFF0B1623)
private val Tarjeta = Color(0xFF132238)
private val Gris = Color(0xFF8A94A6)

private val protocolos = listOf("16:8" to 16, "18:6" to 18, "20:4" to 20, "OMAD" to 23)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FastStore.ensureChannel(this)
        FastWidget.refresh(this)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Verde,
                    onPrimary = Color(0xFF04210F),
                    background = Fondo,
                    surface = Tarjeta,
                    onSurface = Color.White,
                    onBackground = Color.White
                )
            ) { Pantalla() }
        }
    }
}

@Composable
private fun Pantalla() {
    val ctx = LocalContext.current
    var start by remember { mutableLongStateOf(FastStore.startMs(ctx)) }
    var goal by remember { mutableIntStateOf(FastStore.goalHours(ctx)) }
    var pesos by remember { mutableStateOf(FastStore.weights(ctx)) }
    var historial by remember { mutableStateOf(FastStore.history(ctx)) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var dlgPeso by remember { mutableStateOf(false) }
    var dlgAjuste by remember { mutableStateOf(false) }
    var dlgOtro by remember { mutableStateOf(false) }
    var dlgTerminar by remember { mutableStateOf(false) }

    val permiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) { permiso.launch(Manifest.permission.POST_NOTIFICATIONS) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }

    fun iniciar(h: Int) {
        FastStore.start(ctx, h)
        start = FastStore.startMs(ctx)
        goal = h
    }

    val actual = pesos.lastOrNull()?.second
    val previo = pesos.getOrNull(pesos.size - 2)?.second

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Fondo)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Ayuno intermitente", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)

        // Peso
        Card(colors = CardDefaults.cardColors(containerColor = Tarjeta), modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Peso", color = Gris, fontSize = 13.sp)
                    Text(
                        if (actual != null) "${fmtKg(actual)} kg" else "--",
                        fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White
                    )
                    if (actual != null && previo != null) {
                        val d = actual - previo
                        val txt = when {
                            abs(d) < 0.05f -> "= igual que el registro anterior"
                            d < 0 -> "▼ ${fmtKg(-d)} kg desde el anterior"
                            else -> "▲ ${fmtKg(d)} kg desde el anterior"
                        }
                        Text(txt, fontSize = 12.sp, color = if (d < -0.05f) Verde else Gris)
                    }
                }
                FilledTonalButton(onClick = { dlgPeso = true }) {
                    Text(if (actual == null) "Agregar" else "Actualizar")
                }
            }
        }

        // Ayuno
        Card(colors = CardDefaults.cardColors(containerColor = Tarjeta), modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (start > 0L) {
                    val goalMs = goal * 3_600_000L
                    val elapsed = (now - start).coerceAtLeast(0L)
                    val prog = (elapsed.toFloat() / goalMs).coerceIn(0f, 1f)
                    val cumplida = elapsed >= goalMs
                    Text(animo(prog, cumplida), color = Verde, fontSize = 16.sp)
                    Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { prog },
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 14.dp,
                            color = if (cumplida) Verde else Azul,
                            trackColor = Color(0xFF22344D)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(formatHMS(elapsed), fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("de $goal h", color = Gris)
                        }
                    }
                    Text(
                        if (cumplida) "¡Meta de $goal h cumplida!" else "Faltan ${formatHM(goalMs - elapsed)}",
                        color = Color.White
                    )
                    Text(
                        "Inicio: " + SimpleDateFormat("EEE d MMM HH:mm", Locale("es")).format(Date(start)),
                        color = Gris, fontSize = 13.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { dlgAjuste = true }) { Text("Ajustar inicio") }
                        Button(onClick = { dlgTerminar = true }) { Text("Terminar") }
                    }
                } else {
                    Text("Elige un protocolo para empezar", color = Color.White)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        protocolos.take(2).forEach { (n, h) ->
                            Button(onClick = { iniciar(h) }, modifier = Modifier.weight(1f)) { Text(n) }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        protocolos.drop(2).forEach { (n, h) ->
                            Button(onClick = { iniciar(h) }, modifier = Modifier.weight(1f)) { Text(n) }
                        }
                    }
                    OutlinedButton(onClick = { dlgOtro = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Otra duración…")
                    }
                }
            }
        }

        // Historial
        Text("Historial", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        if (historial.isEmpty()) {
            Text("Aún no hay ayunos registrados", color = Gris)
        }
        val fmt = SimpleDateFormat("EEE d MMM HH:mm", Locale("es"))
        historial.take(30).forEach { r ->
            Card(colors = CardDefaults.cardColors(containerColor = Tarjeta), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        formatHM(r.durationMs) + if (r.reachedGoal) "  ✓" else "",
                        color = if (r.reachedGoal) Verde else Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text("${fmt.format(Date(r.startMs))} · meta ${r.goalHours} h", color = Gris, fontSize = 12.sp)
                }
            }
        }
    }

    if (dlgPeso) {
        var texto by remember { mutableStateOf(actual?.let { fmtKg(it) } ?: "") }
        var error by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { dlgPeso = false },
            title = { Text("Tu peso (kg)") },
            text = {
                Column {
                    OutlinedTextField(
                        value = texto,
                        onValueChange = { texto = it; error = false },
                        singleLine = true,
                        isError = error,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    if (error) Text("Ingresa un peso entre 20 y 300", color = Color(0xFFFF8A80), fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val kg = texto.replace(',', '.').toFloatOrNull()
                    if (kg == null || kg < 20f || kg > 300f) {
                        error = true
                    } else {
                        FastStore.addWeight(ctx, kg)
                        pesos = FastStore.weights(ctx)
                        dlgPeso = false
                    }
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { dlgPeso = false }) { Text("Cancelar") } }
        )
    }

    if (dlgAjuste) {
        val opciones = listOf(
            "Empecé 30 min antes" to 30,
            "Empecé 1 h antes" to 60,
            "Empecé 2 h antes" to 120,
            "Empecé 30 min después" to -30
        )
        AlertDialog(
            onDismissRequest = { dlgAjuste = false },
            title = { Text("Ajustar hora de inicio") },
            text = {
                Column {
                    opciones.forEach { (t, min) ->
                        TextButton(onClick = {
                            FastStore.shiftStart(ctx, min)
                            start = FastStore.startMs(ctx)
                            dlgAjuste = false
                        }) { Text(t) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { dlgAjuste = false }) { Text("Cerrar") } }
        )
    }

    if (dlgOtro) {
        var horas by remember { mutableStateOf("") }
        var error by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { dlgOtro = false },
            title = { Text("Duración del ayuno (horas)") },
            text = {
                Column {
                    OutlinedTextField(
                        value = horas,
                        onValueChange = { horas = it; error = false },
                        singleLine = true,
                        isError = error,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    if (error) Text("Ingresa entre 1 y 72 horas", color = Color(0xFFFF8A80), fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val h = horas.toIntOrNull()
                    if (h == null || h < 1 || h > 72) {
                        error = true
                    } else {
                        iniciar(h)
                        dlgOtro = false
                    }
                }) { Text("Iniciar") }
            },
            dismissButton = { TextButton(onClick = { dlgOtro = false }) { Text("Cancelar") } }
        )
    }

    if (dlgTerminar) {
        AlertDialog(
            onDismissRequest = { dlgTerminar = false },
            title = { Text("¿Terminar el ayuno?") },
            text = { Text("Se guardará en tu historial.") },
            confirmButton = {
                TextButton(onClick = {
                    FastStore.end(ctx)
                    start = 0L
                    historial = FastStore.history(ctx)
                    dlgTerminar = false
                }) { Text("Sí, terminar") }
            },
            dismissButton = { TextButton(onClick = { dlgTerminar = false }) { Text("Seguir") } }
        )
    }
}

private fun fmtKg(kg: Float): String = String.format(Locale("es"), "%.1f", kg)

private fun animo(progreso: Float, cumplida: Boolean): String = when {
    cumplida -> "¡Lo lograste!"
    progreso < 0.25f -> "¡Buen comienzo!"
    progreso < 0.5f -> "¡Sigue así!"
    progreso < 0.75f -> "¡Más de la mitad!"
    else -> "¡Ya casi llegas!"
}

private fun formatHMS(ms: Long): String {
    val s = ms / 1000
    return "%02d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60)
}

private fun formatHM(ms: Long): String {
    val m = ms / 60_000
    return "${m / 60} h ${"%02d".format(m % 60)} min"
}
