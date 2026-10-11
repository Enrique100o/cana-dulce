# Caña Dulce · Etapa 3 Backend

Proyecto individual de Dispositivos Móviles de Faber Enrique Uni Gómez. Aplicación de comprensión lectora para tercero y cuarto de primaria de la Institución Educativa Caña Dulce.

Esta etapa implementa la API propia y la base persistente del diseño. La aplicación Android se desarrollará en Android Studio con Kotlin. La fecha de esta entrega, según la guía, es el martes 13 de octubre de 2026.

## Lo que se entrega

| Evidencia solicitada | Archivo o comprobación |
|---|---|
| API funcional | Código en `src/`; 19 operaciones del contrato y una ruta operativa `/health` |
| Base de datos | `database/schema.sql` y `data/cana-dulce.sqlite` con seis entidades relacionadas |
| Endpoints probados | `test/api.test.mjs`; solicitudes HTTP reales contra un servidor local |
| Informe de integración | `evidencia/pruebas-integracion.tap`, con los casos y el resultado de Node |
| Colección de pruebas | `postman/Cana_Dulce.postman_collection.json` con 65 solicitudes |
| Evidencia equivalente de ejecución | `evidencia/coleccion-resultados.json`, con respuestas y 177 comprobaciones |
| Arranque con base persistente | `evidencia/arranque-local.json` |

La colección se ejecutó con el programa local `scripts/probar-coleccion.mjs`, que utiliza sus solicitudes y scripts de prueba. También puede importarse en Postman. La ejecución registrada no se realizó desde la interfaz de Postman.

## Tecnologías elegidas

El backend usa Node.js, Express y SQLite. El entorno comprobado fue Node.js **24.14.1**, Express **5.3.0** y jsonwebtoken **9.0.3**. `package-lock.json` fija las dependencias instaladas.

SQLite guarda la información en un archivo; no requiere un servidor de base de datos separado. El módulo `node:sqlite` incluido en Node 24 permite ejecutar SQL y transacciones. La arquitectura queda:

```text
App Android en Kotlin → HTTP/JSON → API Express → SQLite
```

Android recibirá datos de la API; las credenciales de la base y el cálculo del puntaje quedan en el backend.

## Ejecutar en Windows

Abre una terminal de PowerShell dentro de la carpeta `backend`. Verifica que `node --version` muestre una versión **24.x igual o posterior a 24.14.1**. Después ejecuta:

```powershell
npm.cmd ci
npm.cmd run setup
npm.cmd run seed
npm.cmd start
```

`ci` instala las versiones del archivo de bloqueo. `setup` crea `.env` con una clave JWT aleatoria y el hash de la contraseña docente; conserva el archivo si ya existe. `seed` agrega datos ficticios sin borrar ni modificar los registros anteriores. `start` mantiene el servidor abierto en la terminal.

La API queda en `http://127.0.0.1:8080/api`. Abre `http://127.0.0.1:8080/health` para comprobar el servidor; debe responder:

```json
{"estado":"OK","servicio":"cana-dulce-api"}
```

Detén el servidor con Ctrl+C. Al volver a ejecutar `npm.cmd start`, los datos siguen en `data/cana-dulce.sqlite`.

## Datos de demostración

| Uso | Identificación | Contraseña o código de prueba |
|---|---|---|
| Docente | `docente` | `Lectura2026!` |
| Estudiante de tercero | `Lector03` | `728461` |
| Estudiante de cuarto | `Lector04` | `864209` |

Son credenciales públicas para demostración local con perfiles ficticios. La contraseña y los códigos se guardan como hashes scrypt, con sal aleatoria. La clave de firma JWT se genera al configurar cada copia del proyecto. `.env` no se incluye en el paquete entregable.

Para elegir otra contraseña docente al configurar una copia nueva, define `DOCENTE_PASSWORD` en la terminal antes de `npm.cmd run setup`. Para ejecutar la colección con esa contraseña, cambia la variable `contrasenaDocente` en Postman o define `DOCENTE_TEST_PASSWORD` para el ejecutor de terminal. El script no reemplaza una configuración existente.

Los datos iniciales contienen cuatro lecturas activas con cuatro preguntas cada una: tres de tercero y una de cuarto. Las preguntas trabajan información literal, inferencias, idea principal, vocabulario y secuencias.

## Base de datos y organización del código

| Entidad | Qué conserva |
|---|---|
| ESTUDIANTE | Alias, grado, estado y hash de acceso |
| LECTURA | Texto de una misión y su clasificación |
| PREGUNTA | Ejercicio, habilidad y orden dentro de una lectura |
| OPCION | Alternativas y clave de corrección docente |
| INTENTO | Participación, fechas, total de preguntas y resultado |
| RESPUESTA | Opción elegida y corrección calculada por servidor |

La implementación física añade `aliasNormalizado` como clave técnica de unicidad Unicode. Permite conservar la escritura visible del alias y evitar duplicados por mayúsculas o caracteres equivalentes. La API no devuelve esta columna. En SQLite las fechas UTC se almacenan como TEXT, los booleanos como INTEGER 0/1 y el puntaje redondeado como REAL. Estos son ajustes del modelo lógico al motor elegido.

Hay claves primarias, foráneas, restricciones CHECK y UNIQUE. Un índice único parcial impide dos intentos pendientes del mismo estudiante en la misma lectura. La FK compuesta de RESPUESTA hacia OPCION garantiza que la opción pertenezca a su pregunta. Los triggers protegen el contenido usado y la inmutabilidad de respuestas guardadas. El servicio aplica las reglas que abarcan varias filas dentro de transacciones `BEGIN IMMEDIATE`.

```text
src/
  server.mjs       Arranque y cierre del servidor
  config.mjs       Configuración desde .env
  app.mjs          Express, JSON, autenticación y manejo de errores
  db.mjs           Apertura de SQLite y transacciones
  auth.mjs         Hashes, sesiones JWT y permisos
  validation.mjs   Tipos, campos permitidos y límites
  students.mjs     Registro, ingreso y consulta de perfiles
  readings.mjs     CRUD de lecturas, preguntas y opciones
  attempts.mjs     Intentos, historial y finalización
  scoring.mjs      Puntaje, estrellas, progreso e insignias
database/
  schema.sql       Esquema físico de seis tablas
  demo.mjs         Lecturas ficticias de demostración
scripts/
  configurar.mjs          Genera configuración sin sobrescribirla
  sembrar.mjs             Agrega datos de prueba
  generar-coleccion.mjs   Genera la colección importable
  probar-coleccion.mjs    Ejecuta solicitudes y scripts de esa colección
test/
  api.test.mjs     Pruebas de integración y persistencia
```

Todas las consultas con datos del cliente usan parámetros enlazados. La búsqueda usa normalización NFC y comparación sin mayúsculas; los caracteres `%` y `_` se tratan como texto de búsqueda.

## Operaciones implementadas

A = sin sesión, E = estudiante, D = docente. Las rutas empiezan por `/api`. Las operaciones E sobre intentos o progreso comprueban la propiedad del perfil.

| Método | Ruta | Permiso | Éxito |
|---|---|---|---|
| POST | `/estudiantes` | A | 201 |
| POST | `/sesiones/estudiante` | A | 200 |
| POST | `/sesiones/docente` | A | 200 |
| GET | `/estudiantes` | D | 200 |
| GET | `/lecturas` | E, D | 200 |
| GET | `/lecturas/{id}` | E con acceso, D | 200 |
| POST | `/lecturas` | D | 201 |
| PUT | `/lecturas/{id}` | D | 200 |
| DELETE | `/lecturas/{id}` | D | 204 |
| GET | `/lecturas/{id}/preguntas` | E con acceso, D | 200 |
| GET | `/docente/lecturas/{id}/preguntas` | D | 200 |
| POST | `/lecturas/{id}/preguntas` | D | 201 |
| PUT | `/preguntas/{id}` | D | 200 |
| DELETE | `/preguntas/{id}` | D | 204 |
| POST | `/intentos` | E | 201 |
| GET | `/intentos` | E propios, D | 200 |
| GET | `/intentos/{id}` | E propietario, D | 200 |
| POST | `/intentos/{id}/finalizar` | E propietario | 200 |
| GET | `/estudiantes/{id}/progreso` | E propietario, D | 200 |

GET `/lecturas` admite `grado`, `q` y `estado`. GET `/estudiantes` admite esos filtros con los estados de perfil ACTIVO/INACTIVO. GET `/intentos` admite `estudianteId`, `idLectura` y `estado`. GET `/docente/lecturas/{id}/preguntas` admite `estado`. Las listas se devuelven como `{ "datos": [...], "total": n }`.

POST y PUT requieren `Content-Type: application/json`. Las rutas protegidas requieren `Authorization: Bearer TOKEN`. Las sesiones duran ocho horas. Un estudiante solo inicia misiones activas de su grado y consulta su propio historial.

Los errores usan 400, 401, 403, 404, 409 o 500 según el caso y tienen la forma:

```json
{
  "error": {
    "codigo": "RESPUESTAS_INCOMPLETAS",
    "mensaje": "Responde todas las preguntas antes de finalizar.",
    "detalles": [{"campo":"respuestas","mensaje":"Falta responder la pregunta 4."}]
  }
}
```

## Regla de negocio para explicar en la sustentación

Crear una lectura la deja INACTIVA. La docente agrega preguntas con dos a cuatro opciones y exactamente una correcta. Al activar la lectura, el servidor comprueba que exista al menos una pregunta activa válida.

Cuando un estudiante inicia una misión, se crea un INTENTO pendiente con el total de preguntas. Desde ese momento queda congelado el contenido de la lectura, sus preguntas y opciones. La docente puede desactivarla y crear otra para modificar el material.

El estudiante carga las preguntas mediante la ruta estudiantil. El JSON no incluye `esCorrecta` ni la clave de la opción correcta. La ruta docente sí incluye la información de corrección, y exige sesión docente.

Al finalizar, el cliente envía solamente las parejas pregunta/opción:

```json
{
  "respuestas": [
    {"idPregunta":1,"idOpcion":1},
    {"idPregunta":2,"idOpcion":4},
    {"idPregunta":3,"idOpcion":7},
    {"idPregunta":4,"idOpcion":11}
  ]
}
```

Los IDs ilustran el formato; para una ejecución se usan los devueltos por la API. El servidor verifica propiedad, pertenencia, ausencia de duplicados y cobertura de todas las preguntas. Compara las selecciones con las claves guardadas y calcula `100 × correctas / total`, redondeado a dos decimales con mitad hacia arriba. Guarda respuestas y resultado en una sola transacción. Una falla revierte todo.

Tres aciertos de cuatro producen 75 puntos y dos estrellas. Un reenvío del mismo intento con las mismas parejas devuelve el resultado guardado, incluso si cambia el orden del arreglo. Un envío diferente sobre un intento completado devuelve 409. Desactivar la lectura impide comenzar nuevos intentos, pero permite terminar los ya iniciados y consultar resultados históricos.

## Gamificación implementada en el servidor

| Puntaje de un intento | Estrellas |
|---|---|
| 0 | 0 |
| Mayor que 0 y menor que 50 | 1 |
| Desde 50 y menor que 80 | 2 |
| Desde 80 hasta 100 | 3 |

Los puntos acumulados suman puntajes de intentos completados; pueden superar 100. Las estrellas acumuladas suman los premios. Las misiones completadas cuentan lecturas distintas; repetir una lectura suma práctica y premios, pero sigue siendo una sola misión distinta.

El progreso devuelve tres insignias con su estado `obtenida`: Primera aventura al completar una lectura; Explorador de historias al completar tres distintas; Lectura brillante al obtener 100 en algún intento. Promedio y mejor puntaje son null antes de terminar una actividad, mientras los contadores son cero.

El backend devuelve las recompensas. La app Kotlin mostrará las ilustraciones, el personaje guía y las celebraciones diseñadas en la Etapa 2.

## Usar Postman

1. Inicia el servidor con `npm.cmd start` y déjalo abierto.
2. Importa `postman/Cana_Dulce.postman_collection.json`.
3. Revisa las variables de la colección: `baseUrl` sin `/api` al final, `usuarioDocente` y `contrasenaDocente`.
4. Ejecuta la colección completa, en su orden. La primera solicitud genera un alias y una lectura nuevos para esta ejecución.
5. Revisa los resultados de las pruebas. Las respuestas 400/401/403/404/409 son casos negativos preparados; su código y estructura deben coincidir con lo esperado.

La colección obtiene tokens e IDs de las respuestas: no exige copiar identificadores manualmente. Deja una lectura de prueba inactiva, dos intentos completados y el historial de perfiles ficticios. No elimina registros anteriores. Conviene ejecutarla contra datos de demostración.

Para ejecutar la misma colección desde otra terminal, sin necesitar un servidor previo:

```powershell
npm.cmd run test:collection
```

El ejecutor inicia una instancia local aislada en un puerto libre y usa `data/pruebas-coleccion.sqlite`. Guarda el informe en `evidencia/coleccion-resultados.json` y detiene esa instancia al finalizar. No toca `data/cana-dulce.sqlite`. En sus informes omite tokens y credenciales.

## Pruebas registradas

Ejecutadas el 11 de octubre de 2026 con datos ficticios:

| Validación | Resultado |
|---|---|
| `npm.cmd test` | 44 pruebas reportadas por Node, 44 correctas, cero fallos |
| `npm.cmd run test:collection` | 65 solicitudes, 177 comprobaciones, cero fallos |
| Base de evidencia | Integridad `ok`, claves foráneas sin errores |
| Arranque normal con `npm start` | Salud 200, sesión de prueba 200, listado de tercero 200 con tres lecturas |

Las pruebas cubren el CRUD, filtros, permisos, sesiones vencidas, ocultación de claves, preguntas válidas, intento pendiente único, contenido congelado, respuestas completas, finalización tras desactivación, reenvío sin duplicados, progreso, las tres insignias, persistencia al reabrir el archivo y reversión de una falla simulada durante la escritura. La traza de error de esa falla simulada es esperada dentro de la prueba; se comprueba la respuesta 500 y el rollback.

`npm.cmd test` usa una base aislada en memoria y un archivo temporal para la comprobación de persistencia. No altera las bases de demostración. El conteo de 44 incluye el caso agrupador del flujo HTTP, sus 41 subcasos y dos casos adicionales; las 177 comprobaciones de la colección son un conteo separado, con cobertura que se solapa.

## Orden sugerido de demostración

1. Mostrar `schema.sql` y explicar las seis entidades.
2. Iniciar la API y consultar `/health`.
3. Ingresar como docente; crear una lectura, agregar preguntas, editarla y activarla.
4. Buscar lecturas mediante `grado=3` y `q`.
5. Ingresar como estudiante y mostrar que el JSON de preguntas no revela la clave.
6. Crear un intento; enviar respuestas incompletas y observar 400 sin resultado guardado.
7. Finalizar con tres aciertos; mostrar 75 puntos, dos estrellas e historial.
8. Reenviar y comprobar que no se duplican premios; enviar selecciones distintas y observar 409.
9. Desactivar la lectura y consultar el resultado histórico.
10. Reiniciar el servidor y consultar los datos persistidos.

Para entender el código de forma incremental, empieza por `schema.sql` y `db.mjs`; sigue con `readings.mjs`; después revisa `attempts.mjs` y `scoring.mjs`; termina con autenticación y colección. La decisión central para defender es que el servidor valida y califica, y Android presenta el resultado recibido.

## Conexión posterior con Kotlin

La integración Android pertenece a la siguiente etapa. Con el emulador estándar de Android Studio, la URL de desarrollo será `http://10.0.2.2:8080/api/`: esa dirección permite llegar al equipo anfitrión desde el emulador. En un dispositivo físico la dirección se configura según la red; no se usa la dirección especial del emulador.

La app requerirá permiso de Internet y configuración de HTTP local para su variante de desarrollo. Si posteriormente se publica un servidor, se configurará su URL HTTPS. La URL debe quedar configurable, y la app deberá enviar el token de la sesión en las rutas protegidas.

## Problemas comunes

- Si PowerShell bloquea `npm.ps1`, usa `npm.cmd`, como en los comandos anteriores.
- Si falta `.env`, ejecuta `npm.cmd run setup`.
- Si aparece `EADDRINUSE`, el puerto 8080 está ocupado: detén tu instancia anterior o cambia PORT en `.env` y `baseUrl` en Postman.
- Node 24 puede mostrar `ExperimentalWarning` al importar `node:sqlite`. El entorno fue probado con ese módulo y esa versión; el mensaje no equivale a un fallo de las pruebas.
- Si cambias la contraseña docente, actualiza la variable correspondiente de la colección o `DOCENTE_TEST_PASSWORD` al usar el ejecutor.

## Referencias técnicas

- [Instalación oficial de Express](https://expressjs.com/en/starter/installing/).
- [API SQLite de Node 24.14.1](https://github.com/nodejs/node/blob/v24.14.1/doc/api/sqlite.md).
- [Firma y verificación de JWT con jsonwebtoken](https://github.com/auth0/node-jsonwebtoken).
- [Respuestas y pruebas en scripts de Postman](https://learning.postman.com/docs/tests-and-scripts/write-scripts/postman-sandbox-reference/pm-response/).
- [Esquema de colección Postman 2.1](https://schema.postman.com/json/collection/v2.1.0/collection.json).
- [Direcciones de red del emulador Android](https://developer.android.com/studio/run/emulator-networking-address).
