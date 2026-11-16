# Ayuno – app para teléfono Android (con widget)

Kotlin + Jetpack Compose + Glance (widget). Funciona solo en el teléfono.

## Generar la APK
1. Abre la carpeta `AyunoPhone` con Android Studio (Koala o superior) y espera el Gradle sync.
2. Menú Build > Build Bundle(s) / APK(s) > Build APK(s).
3. Cuando termine, pulsa "locate" en el aviso. El archivo queda en
   `app/build/outputs/apk/debug/app-debug.apk`.

## Instalar en el teléfono
1. Pasa el `app-debug.apk` al teléfono (cable USB, Drive, correo o WhatsApp).
2. Ábrelo desde el teléfono; si lo pide, permite "Instalar apps desconocidas" para esa aplicación.
3. Abre "Ayuno" y acepta el permiso de notificaciones.

## Agregar el widget
Mantén pulsado un espacio vacío de la pantalla principal > Widgets > Ayuno.
Muestra horas de ayuno, barra de progreso y tiempo restante; si no hay ayuno activo, tiene un botón "Iniciar" que usa la última meta. Tocar el widget abre la app. Se actualiza cada ~15 minutos (límite de Android para widgets).

## Qué incluye
- Protocolos 16:8, 18:6, 20:4, OMAD y duración personalizada.
- Anillo de progreso, tiempo transcurrido/restante y mensajes de ánimo.
- Ajustar la hora de inicio y terminar con confirmación.
- Aviso al cumplir la meta (alarma inexacta: puede variar unos minutos).
- Registro de peso con diferencia respecto al anterior.
- Historial de ayunos.

## Generar la APK sin computador (GitHub Actions)
1. Crea un repositorio en github.com y sube el contenido de esta carpeta (incluida `.github`).
2. En la pestaña "Actions" se ejecuta "Compilar APK" (unos 5 minutos).
3. Al terminar, abre la ejecución y descarga el artefacto "Ayuno-apk": dentro está `app-debug.apk`.
