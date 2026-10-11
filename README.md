# Caña Dulce

Raíz del código de la aplicación de comprensión lectora para estudiantes de tercero y cuarto de primaria.

```text
CanaDulce/
  android/   Proyecto Android en Kotlin con Jetpack Compose
  backend/   API Express, base de datos SQLite y pruebas
```

El proyecto Android está implementado en Kotlin con Jetpack Compose. Abre la carpeta android desde Android Studio. El paquete es com.canadulce.app y la versión mínima compatible es Android 7.0 (API 24). Incluye los recorridos de estudiante y docente, gamificación y consumo real de la API. Consulta android/README.md para ejecutar y probar la aplicación.

El backend ya está implementado. Para iniciarlo desde PowerShell:

```powershell
Set-Location -LiteralPath 'C:\Users\faber\Documents\Codex\CanaDulce\backend'
npm.cmd start
```

La comprobación de arranque está en `http://127.0.0.1:8080/health`. Consulta `backend/README.md` para la instalación, las pruebas y las credenciales de demostración.

Los PDF de la asignatura, el documento Word y los paquetes de entrega se conservan fuera de esta raíz, en `C:\Users\faber\OneDrive\Documentos\ChatGPT\APP MOVILES`. No es necesario copiarlos dentro del proyecto de Android.
