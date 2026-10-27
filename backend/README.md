# API de Caña Dulce

API REST en JavaScript con Node.js, Express y SQLite. Android consume la información mediante HTTP y
JSON. El servidor valida los accesos, administra las actividades y calcula los resultados.

## Instalación y arranque

Utiliza Node.js 24.x desde 24.14.1. Desde esta carpeta, en PowerShell:

```powershell
npm.cmd ci
npm.cmd run setup
npm.cmd run seed
npm.cmd start
```

En los siguientes arranques basta con npm.cmd start. Detén el servidor con Ctrl+C. npm.cmd run dev
reinicia la API cuando cambian sus archivos.

- ci instala las versiones de package-lock.json.
- setup crea .env con la clave JWT aleatoria y la contraseña docente protegida.
- seed crea las tablas y agrega dos estudiantes y cuatro lecturas ficticias.
- start inicia la API con la configuración de .env.

setup conserva una configuración existente. seed conserva los registros anteriores y omite los datos
de demostración que ya existan. Comprueba el arranque en http://127.0.0.1:8080/health:

```json
{ "estado": "OK", "servicio": "cana-dulce-api" }
```

## Configuración

| Variable              | Uso                                                   |
| --------------------- | ----------------------------------------------------- |
| HOST                  | Dirección de escucha; por defecto 127.0.0.1.          |
| PORT                  | Puerto; por defecto 8080.                             |
| DB_PATH               | Archivo SQLite; por defecto ./data/cana-dulce.sqlite. |
| DOCENTE_USUARIO       | Usuario del docente.                                  |
| DOCENTE_PASSWORD_HASH | Hash generado por setup.                              |
| JWT_SECRET            | Clave de las sesiones generada por setup.             |

Accesos ficticios: docente / Lectura2026!, Lector03 / 728461 y Lector04 / 864209. Para elegir otra
contraseña docente en una instalación nueva, define DOCENTE_PASSWORD antes de ejecutar setup.

.env.example documenta los campos. La configuración privada y los archivos SQLite se excluyen de
Git.

## Código

```text
src/
├── server.mjs        Arranque y cierre del servidor
├── app.mjs           Express y registro de rutas
├── config.mjs        Configuración local
├── db.mjs            SQLite y transacciones
├── auth.mjs          Contraseñas, sesiones y permisos
├── errors.mjs        Respuestas de error
├── validation.mjs    Validación de entradas
├── scoring.mjs       Puntajes, estrellas e insignias
└── routes/
    ├── students.mjs  Perfiles, ingreso y progreso
    ├── readings.mjs  Lecturas, preguntas y opciones
    └── attempts.mjs  Intentos, respuestas e historial
```

database/schema.sql define las seis tablas. database/demo.mjs contiene las lecturas iniciales.
scripts/ contiene la configuración y la carga de datos de demostración.

## Base de datos

| Tabla      | Contenido                                             |
| ---------- | ----------------------------------------------------- |
| ESTUDIANTE | Alias, grado, estado y hash del código de acceso.     |
| LECTURA    | Texto, clasificación y disponibilidad de cada misión. |
| PREGUNTA   | Enunciado, habilidad y orden en la lectura.           |
| OPCION     | Alternativas de respuesta y clave correcta.           |
| INTENTO    | Participación, fechas y resultado.                    |
| RESPUESTA  | Selecciones y corrección calculada por el servidor.   |

SQLite conserva los datos al reiniciar la API. Para trasladarlos, cierra el backend y todas las
conexiones a la base antes de copiar data/cana-dulce.sqlite.

## Endpoints

Todas las rutas de la tabla comienzan por /api. E = estudiante; D = docente.

| Método | Ruta                            | Acceso           |
| ------ | ------------------------------- | ---------------- |
| POST   | /estudiantes                    | Público          |
| POST   | /sesiones/estudiante            | Público          |
| POST   | /sesiones/docente               | Público          |
| GET    | /estudiantes                    | D                |
| GET    | /lecturas                       | E, D             |
| GET    | /lecturas/:id                   | E, D             |
| POST   | /lecturas                       | D                |
| PUT    | /lecturas/:id                   | D                |
| DELETE | /lecturas/:id                   | D                |
| GET    | /lecturas/:id/preguntas         | E, D             |
| GET    | /docente/lecturas/:id/preguntas | D                |
| POST   | /lecturas/:id/preguntas         | D                |
| PUT    | /preguntas/:id                  | D                |
| DELETE | /preguntas/:id                  | D                |
| POST   | /intentos                       | E                |
| GET    | /intentos                       | E, D             |
| GET    | /intentos/:id                   | E propietario, D |
| POST   | /intentos/:id/finalizar         | E propietario    |
| GET    | /estudiantes/:id/progreso       | E propietario, D |

Las rutas protegidas requieren Authorization: Bearer TOKEN; las sesiones duran ocho horas. Las
listas admiten filtros según la ruta: grado, búsqueda q, estado, estudiante y lectura. Un estudiante
accede a sus propios intentos y a las misiones permitidas para su grado.

## Reglas de negocio

- Una lectura nueva queda inactiva hasta tener al menos una pregunta activa válida.
- Cada pregunta tiene entre dos y cuatro opciones y exactamente una correcta.
- Las preguntas del estudiante no revelan la clave correcta.
- Un intento iniciado protege el contenido de la lectura y sus preguntas para conservar el
  historial.
- Al finalizar, se verifican las parejas pregunta/opción y se guardan en una transacción.
- Reenviar las mismas respuestas devuelve el resultado guardado sin duplicar premios.
- Desactivar una lectura impide nuevos intentos y conserva los anteriores.

El puntaje es 100 × correctas / total, redondeado a dos decimales. Cero otorga cero estrellas; menos
de 50 otorga una; desde 50 hasta menos de 80 otorga dos; desde 80 otorga tres.

Las insignias corresponden a completar una lectura, completar tres lecturas distintas y lograr 100
puntos en un intento. La aplicación Android muestra los resultados y las recompensas.

Las contraseñas usan scrypt y las sesiones JWT. Las consultas usan parámetros enlazados. Los errores
incluyen código, mensaje y, cuando corresponde, los campos que deben corregirse.
