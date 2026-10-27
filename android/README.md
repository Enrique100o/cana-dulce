# Aplicación Android

Aplicación Kotlin con Jetpack Compose. Consume la API mediante HTTP y JSON; el backend administra la
información persistente.

## Ejecutar

1. Inicia el backend siguiendo [sus instrucciones](../backend/README.md).
2. Abre esta carpeta en Android Studio y sincroniza Gradle.
3. Selecciona un emulador o teléfono con Android 7.0 (API 24) o superior.
4. Ejecuta el módulo app.

Entorno utilizado: SDK 37, Android Gradle Plugin 9.4.1, Gradle 9.6.0 y JDK 25. Las versiones están
declaradas en Gradle y su catálogo.

## Conexión

- Emulador: http://10.0.2.2:8080/api.
- Teléfono por USB: activa depuración USB, ejecuta adb reverse tcp:8080 tcp:8080 y configura
  http://127.0.0.1:8080/api en la pantalla de conexión de aula.
- Mantén el backend encendido en el computador.

La variante debug admite HTTP local para 10.0.2.2, 127.0.0.1 y localhost. Una variante release
requiere configurar una dirección HTTPS de un servidor publicado.

## Organización

Dentro de app/src/main/java/com/canadulce/app/:

| Archivo o carpeta                 | Responsabilidad                                              |
| --------------------------------- | ------------------------------------------------------------ |
| MainActivity.kt                   | Entrada Android y aplicación del tema.                       |
| AppState.kt                       | Estado de la interfaz y destinos de navegación.              |
| AppViewModel.kt                   | Acciones, navegación, operaciones y errores.                 |
| data/Models.kt                    | Datos recibidos de la API.                                   |
| data/FormDrafts.kt                | Valores de los formularios docentes.                         |
| data/InputRules.kt                | Validaciones de formularios.                                 |
| data/ApiClient.kt                 | Solicitudes HTTP, JSON y sesión.                             |
| ui/CanaDulceApp.kt                | Estructura general y selección de pantalla.                  |
| ui/screens/auth/                  | Bienvenida, ingreso, registro y conexión.                    |
| ui/screens/student/               | Misiones, lectura, preguntas, resultados y progreso.         |
| ui/screens/teacher/               | Gestión de lecturas, preguntas y estudiantes.                |
| ui/screens/ReadingDetailScreen.kt | Detalle compartido por los dos perfiles.                     |
| ui/components/                    | Botones, formularios, tarjetas, recompensas e ilustraciones. |
| ui/theme/                         | Colores y tipografía.                                        |

La sesión permanece en memoria y se vuelve a ingresar al cerrar completamente la app. Las respuestas
sin enviar se mantienen mientras está abierto el recorrido y al rotar el dispositivo. Un intento
pendiente se puede retomar desde el servidor, seleccionando nuevamente sus respuestas.

## Compilar un APK

Desde esta carpeta:

```powershell
.\gradlew.bat :app:assembleDebug
```

El APK se genera en app/build/outputs/apk/debug/app-debug.apk.
