# Caña Dulce

Aplicación de comprensión lectora para tercero y cuarto de primaria de la Institución Educativa Caña
Dulce. Proyecto individual de Dispositivos Móviles de Faber Enrique Uni Gómez.

El estudiante realiza misiones y obtiene puntos, estrellas e insignias. El docente administra las
lecturas y preguntas y consulta el progreso del aula.

## Arquitectura

```text
Aplicación Android (Kotlin y Jetpack Compose)
                  ↕ HTTP / JSON
API REST (Node.js y Express)
                  ↕ SQL
Base de datos (SQLite)
```

El backend valida los permisos, califica las respuestas y conserva los resultados. Android muestra
la información recibida. SQLite se ejecuta dentro del backend y guarda los datos en un archivo
local.

## Estructura

```text
cana-dulce/
├── android/                 Proyecto para Android Studio
│   └── app/src/main/java/com/canadulce/app/
│       ├── MainActivity.kt  Entrada de la aplicación
│       ├── AppState.kt      Estado y destinos de navegación
│       ├── AppViewModel.kt  Acciones y coordinación con la API
│       ├── data/            Modelos, formularios, validación y cliente HTTP
│       └── ui/
│           ├── components/  Controles e ilustraciones reutilizables
│           ├── screens/     Acceso, estudiante y docente
│           └── theme/       Colores y tipografía
└── backend/
    ├── src/                 Arranque, seguridad y reglas compartidas
    │   └── routes/          Estudiantes, lecturas e intentos
    ├── database/            Esquema SQL y contenido de demostración
    ├── scripts/             Configuración y carga de datos
    └── data/                Base local, excluida de Git
```

## Preparar una copia nueva

Requisitos: Git, Node.js 24.x desde 24.14.1, Android Studio compatible con Android Gradle Plugin
9.4.1, SDK de Android 37 y JDK 25 para Gradle. El proyecto incluye Gradle Wrapper y configuración de
descarga del JDK. La primera instalación necesita conexión para descargar las dependencias.

Desde PowerShell:

```powershell
git clone https://github.com/Enrique100o/cana-dulce.git
Set-Location -LiteralPath '.\cana-dulce\backend'
npm.cmd ci
npm.cmd run setup
npm.cmd run seed
npm.cmd start
```

El comando setup genera .env; seed crea las tablas y agrega datos ficticios sin borrar los registros
existentes. Comprueba la API en http://127.0.0.1:8080/health.

Después abre android en Android Studio, sincroniza Gradle y ejecuta app en el emulador. Mantén el
backend encendido. La aplicación usa http://10.0.2.2:8080/api en el emulador.

## Accesos de demostración

| Perfil                | Usuario o alias | Contraseña o código |
| --------------------- | --------------- | ------------------- |
| Docente               | docente         | Lectura2026!        |
| Estudiante de tercero | Lector03        | 728461              |
| Estudiante de cuarto  | Lector04        | 864209              |

Son perfiles ficticios para demostración local. Las claves de sesión se generan en cada instalación.

## Documentación

- [Android](android/README.md): ejecución, conexión por USB y organización de las pantallas.
- [Backend](backend/README.md): configuración, tablas, endpoints y reglas de negocio.

GitHub conserva el código necesario para reconstruir el proyecto. La configuración privada, las
bases locales, las dependencias y las compilaciones están excluidas. El código usa .editorconfig:
cuatro espacios para Kotlin, dos para JavaScript y líneas de referencia de 100 caracteres.

Los PDF, el Word, el APK y las evidencias académicas se conservan fuera de esta carpeta. El backend
se ejecuta localmente; el repositorio no publica un servidor. La aplicación requiere conexión con la
API.
