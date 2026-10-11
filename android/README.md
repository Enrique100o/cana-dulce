# Caña Dulce - Aplicación Android

Aplicación Kotlin con Jetpack Compose que consume la API propia mediante HTTP/JSON. No accede directamente a SQLite.

## Abrir y ejecutar

1. Abre esta carpeta desde Android Studio y sincroniza Gradle.
2. Selecciona un emulador o dispositivo Android 7.0 (API 24) o superior.
3. Inicia el backend en otra terminal, siguiendo ../backend/README.md.
4. Ejecuta el módulo app desde Android Studio.

El APK de pruebas usa http://10.0.2.2:8080/api en el emulador de Android Studio. Ese host apunta al computador. La bienvenida permite cambiar la conexión de aula.

Para un teléfono por USB, ejecuta adb reverse tcp:8080 tcp:8080 y configura http://127.0.0.1:8080/api. El backend local debe seguir encendido.

El APK debug permite HTTP únicamente para 10.0.2.2, localhost y 127.0.0.1. Una compilación release requiere configurar una API HTTPS; no se entrega un servidor publicado.

## Recorridos

- Estudiante: registro con alias y grado, ingreso, búsqueda en la API, misiones, lectura con texto ajustable, preguntas, consulta de la historia, finalización, resultados, puntos, estrellas, insignias e historial.
- Docente: ingreso, búsqueda y filtros en la API, creación y edición de lecturas y preguntas, opciones con una clave correcta, activación, desactivación y consulta de resultados por estudiante.
- Estados: carga, listas vacías, validación, error de conexión, conflicto de negocio, sesión vencida y confirmaciones.
- El servidor decide el puntaje, las recompensas y los permisos. La app respeta el bloqueo de contenido con intentos, también validado por backend.

## Código

    app/src/main/java/com/canadulce/app/
      MainActivity.kt       Entrada de Android
      AppViewModel.kt       Estado, navegación y operaciones
      data/Models.kt        Modelos y validaciones
      data/ApiClient.kt     HTTP y JSON
      ui/CanaDulceApp.kt    Pantallas y componentes
      ui/theme/Theme.kt     Colores y tipografía

La sesión permanece en memoria. Cerrar sesión la elimina; después de cerrar completamente la app se vuelve a ingresar. Las respuestas sin enviar se mantienen mientras está abierto el recorrido y al rotar el dispositivo. Al abandonar o cerrar la app se puede retomar el intento desde el servidor y responder nuevamente. No se implementa funcionamiento sin conexión.

## Compilar

Entorno comprobado: SDK 37, Android Gradle Plugin 9.4.1, Gradle 9.6.0 y JDK 25. Las versiones están definidas en Gradle.

    .\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug

El APK está en app/build/outputs/apk/debug/app-debug.apk.

## Pruebas en emulador con API real

Desde backend, inicia:

    node scripts/servidor-android-pruebas.mjs

Usa el puerto 8081 y una base aislada nueva en cada arranque. Con el emulador encendido, ejecuta desde android:

    .\gradlew.bat :app:connectedDebugAndroidTest

Reinicia el servidor de pruebas antes de repetir la suite. Las pruebas cubren lectura, consulta del texto, retoma, resultado 75/100, estrellas, historial, búsqueda HTTP, CRUD docente y errores de ingreso/registro. Restauran la conexión de demostración al finalizar.

Resultados XML: app/build/outputs/androidTest-results/connected/debug. Capturas: app/build/outputs/connected_android_test_additional_output. Pruebas unitarias: app/build/test-results/testDebugUnitTest.
