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
                  ↕ Consultas SQL parametrizadas
Base de datos MySQL (InnoDB)
```

El backend valida los permisos, califica las respuestas y conserva los resultados. Android muestra
la información recibida y nunca recibe las credenciales de MySQL. Las transacciones impiden guardar
resultados incompletos y los bloqueos evitan calificaciones duplicadas ante envíos simultáneos.

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
    ├── src/                 Servidor, seguridad y reglas compartidas
    │   └── routes/          Estudiantes, lecturas e intentos
    ├── database/            Tablas MySQL, triggers y contenido de demostración
    └── scripts/             Configuración, inicialización, importación y carga inicial
```

## Preparar una copia nueva

Requisitos: Git, Node.js 24.x desde 24.14.1, MySQL 8.0.43 o posterior, Android Studio compatible con
Android Gradle Plugin 9.4.1, SDK 37 y JDK 25. El proyecto incluye Gradle Wrapper y configuración de
descarga del JDK.

```powershell
git clone https://github.com/Enrique100o/cana-dulce.git
Set-Location -LiteralPath '.\cana-dulce\backend'
npm.cmd ci
npm.cmd run setup
```

Crea la base y su usuario, ejecuta los archivos SQL y completa la conexión en .env siguiendo
[la instalación del backend](backend/README.md). Después:

```powershell
npm.cmd run seed
npm.cmd start
```

Comprueba la API en http://127.0.0.1:8080/health. Abre android en Android Studio, sincroniza Gradle
y ejecuta app en el emulador. Mantén MySQL y el backend encendidos. La aplicación usa
http://10.0.2.2:8080/api en el emulador.

Clonar el repositorio descarga el código; cada instalación debe configurar MySQL y sus credenciales.
La información privada y los registros del equipo original no se publican en Git.

## Accesos de demostración

| Perfil                | Usuario o alias | Contraseña o código |
| --------------------- | --------------- | ------------------- |
| Docente               | docente         | Lectura2026!        |
| Estudiante de tercero | Lector03        | 728461              |
| Estudiante de cuarto  | Lector04        | 864209              |

Son perfiles ficticios para demostración local. El docente puede elegir otra contraseña antes de
generar la configuración. Cada instalación genera su propia clave JWT. Los estudiantes nuevos eligen
su código de acceso al registrarse.

## Documentación

- [Android](android/README.md): ejecución, conexión por USB y organización de pantallas.
- [Backend](backend/README.md): instalación de MySQL, configuración, tablas, endpoints y reglas.

El servidor actual se ejecuta localmente. El backend admite una conexión MySQL remota con TLS y
configuración mediante variables de entorno, pero el despliegue público aún está pendiente.

Los documentos académicos, el APK y los respaldos se conservan fuera del código fuente. El proyecto
usa .editorconfig y .prettierrc.json para mantener un formato uniforme.
