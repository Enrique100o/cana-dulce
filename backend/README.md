# API de Caña Dulce

API REST en JavaScript con Node.js, Express y MySQL. Android envía y recibe JSON. El servidor valida
los accesos, administra las actividades y calcula los resultados.

## Requisitos

- Node.js 24.x desde 24.14.1.
- MySQL 8.0.43 o posterior, con InnoDB. La migración se verificó con MySQL Community 8.0.43.
- Una cuenta administradora de MySQL para preparar la base y sus reglas.
- MySQL Workbench es opcional: permite ejecutar el esquema y consultar las tablas.

## Instalación en otro computador

Desde backend:

```powershell
npm.cmd ci
npm.cmd run setup
```

setup crea .env con la clave JWT y el hash de la contraseña docente. Si el archivo existe, lo
conserva. No instala MySQL ni crea usuarios dentro de ese servidor.

### Crear la base y su usuario

En MySQL Workbench, entra con una cuenta administradora y ejecuta:

```sql
CREATE DATABASE cana_dulce
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin;

CREATE USER 'cana_dulce_app'@'localhost'
  IDENTIFIED BY 'REEMPLAZAR_POR_UNA_CLAVE_PROPIA';

GRANT SELECT, INSERT, UPDATE, DELETE
  ON cana_dulce.* TO 'cana_dulce_app'@'localhost';
```

Sustituye la contraseña del ejemplo antes de ejecutar el SQL. Usa estos comandos solo para una
instalación nueva: no elimines una base existente para repetirlos.

Selecciona cana_dulce como esquema predeterminado en Workbench. Abre y ejecuta, en este orden, los
archivos database/schema.sql y database/triggers.sql con la cuenta administradora.

El primer archivo crea las seis tablas y sus relaciones. El segundo protege el historial. En
instalaciones con registro binario, MySQL puede requerir permisos administrativos para crear
triggers. La API trabaja con el usuario limitado; no necesita ejecutarse como root.

También existe npm.cmd run db:init para inicializar el esquema con las credenciales del entorno.
Requiere permisos de creación en una base ya existente; omite tablas y triggers que ya están
preparados. No es un mecanismo de actualización de esquemas futuros.

### Configurar y arrancar

Completa en .env:

```dotenv
DB_HOST=127.0.0.1
DB_PORT=3306
DB_NAME=cana_dulce
DB_USER=cana_dulce_app
DB_PASSWORD=TU_CLAVE_MYSQL
DB_SSL=false
```

Conserva los valores de JWT_SECRET y DOCENTE_PASSWORD_HASH generados por setup. Después:

```powershell
npm.cmd run seed
npm.cmd start
```

seed agrega dos estudiantes y cuatro lecturas ficticias si no existen. No elimina los registros
anteriores. Si importaste una base anterior, no necesitas cargar los ejemplos.

Comprueba http://127.0.0.1:8080/health:

```json
{ "estado": "OK", "servicio": "cana-dulce-api" }
```

En los siguientes arranques basta con tener MySQL encendido y ejecutar npm.cmd start. npm.cmd run
dev reinicia la API cuando cambian sus archivos.

## Configuración

| Variable              | Uso                                                                      |
| --------------------- | ------------------------------------------------------------------------ |
| HOST                  | Interfaz del servidor HTTP; por defecto 127.0.0.1.                       |
| PORT                  | Puerto HTTP; por defecto 8080.                                           |
| DB_HOST               | Servidor MySQL.                                                          |
| DB_PORT               | Puerto MySQL; por defecto 3306.                                          |
| DB_NAME               | Nombre de la base; por defecto cana_dulce.                               |
| DB_USER               | Usuario MySQL; por defecto cana_dulce_app.                               |
| DB_PASSWORD           | Contraseña de MySQL; obligatoria.                                        |
| DB_SSL                | true para cifrar la conexión con MySQL; false para la instalación local. |
| DB_SSL_CA_PATH        | Ruta opcional al certificado CA del proveedor.                           |
| DB_SSL_CA             | Certificado CA alternativo en una variable de entorno.                   |
| DOCENTE_USUARIO       | Usuario de acceso docente.                                               |
| DOCENTE_PASSWORD_HASH | Hash de la contraseña docente generado por setup.                        |
| JWT_SECRET            | Clave privada para firmar las sesiones.                                  |

Sin DB_SSL explícito, se habilita TLS para servidores que no sean localhost o una dirección de
loopback. Con TLS se verifica el certificado del servidor; no se desactiva su validación.

Para una instalación docente nueva con otra contraseña, define DOCENTE_PASSWORD antes de ejecutar
setup. Los accesos de ejemplo son docente / Lectura2026!, Lector03 / 728461 y Lector04 / 864209.
Deben distinguirse de las credenciales de MySQL.

.env y las copias de datos se excluyen de Git. Las credenciales no se incluyen en Android. Los
comandos admiten variables del alojamiento aunque no exista un .env local. Para publicar el backend,
configura HOST=0.0.0.0, el PORT del proveedor y la conexión MySQL correspondiente. Esta versión
todavía no tiene una URL pública desplegada.

## Organización del código

| Archivo o carpeta         | Responsabilidad                                             |
| ------------------------- | ----------------------------------------------------------- |
| src/server.mjs            | Arranque y cierre del servidor y de las conexiones.         |
| src/app.mjs               | Configuración de Express y registro de rutas.               |
| src/config.mjs            | Lectura y validación de variables del entorno.              |
| src/db.mjs                | Conexiones MySQL, consultas parametrizadas y transacciones. |
| src/auth.mjs              | Hashes de contraseñas, JWT y permisos por perfil.           |
| src/errors.mjs            | Respuestas de error sin revelar datos internos.             |
| src/validation.mjs        | Validación de cuerpos, filtros y tipos.                     |
| src/scoring.mjs           | Puntajes, estrellas, progreso e insignias.                  |
| src/routes/students.mjs   | Registro, ingreso, consulta de estudiantes y progreso.      |
| src/routes/readings.mjs   | Lecturas, preguntas y opciones.                             |
| src/routes/attempts.mjs   | Intentos, respuestas e historial.                           |
| database/schema.sql       | Tablas, índices, claves y restricciones MySQL.              |
| database/triggers.sql     | Protección de contenido utilizado y resultados.             |
| scripts/inicializar.mjs   | Preparación de tablas y triggers faltantes.                 |
| scripts/migrar-sqlite.mjs | Importación de la versión anterior desde SQLite.            |
| scripts/sembrar.mjs       | Datos iniciales para una instalación de demostración.       |

El servidor usa mysql2/promise y un máximo de cinco conexiones por proceso. Las consultas utilizan
parámetros; cada transacción utiliza una sola conexión.

## Modelo de datos

| Tabla      | Contenido                                             |
| ---------- | ----------------------------------------------------- |
| ESTUDIANTE | Alias, grado, estado y hash del código de acceso.     |
| LECTURA    | Texto, clasificación y disponibilidad de cada misión. |
| PREGUNTA   | Enunciado, habilidad y orden dentro de la lectura.    |
| OPCION     | Alternativas y clave de corrección.                   |
| INTENTO    | Participación, fechas y resultado.                    |
| RESPUESTA  | Selecciones y corrección calculada por el servidor.   |

Las fechas se guardan como DATETIME(3) en UTC y se devuelven como fechas ISO terminadas en Z. El
puntaje usa DECIMAL(5,2). La columna generada INTENTO.pendiente permite que la clave única admita
varios intentos finalizados y solo uno en progreso por estudiante y lectura; es un campo interno que
no aparece en la API.

Las escrituras de contenido y el inicio de una misión bloquean la lectura dentro de su transacción.
La finalización bloquea el intento, comprueba las respuestas y guarda todo junto. Así, dos
peticiones simultáneas no generan dos resultados. Las relaciones se conservan mediante claves
foráneas y los triggers impiden modificar contenido utilizado o resultados terminados.

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

Las rutas protegidas requieren Authorization: Bearer TOKEN; la sesión dura ocho horas. Las listas
admiten filtros según la ruta: grado, q, estado, estudiante y lectura. /health comprueba también la
conexión con MySQL y no cuenta como operación de negocio.

## Reglas de negocio

- Una lectura nueva queda inactiva hasta tener al menos una pregunta activa válida.
- Cada pregunta tiene de dos a cuatro opciones y exactamente una correcta.
- La consulta estudiantil de preguntas no devuelve la clave de corrección.
- Un intento iniciado protege el contenido de su lectura, preguntas y opciones.
- Finalizar exige todas las respuestas, de la lectura y del estudiante correspondientes.
- Reenviar las mismas respuestas devuelve el resultado guardado, sin duplicar premios.
- Desactivar una lectura impide nuevos intentos y permite terminar los ya iniciados.
- El puntaje es 100 × correctas / total, con redondeo de mitad hacia arriba a dos decimales.
- Cero puntos otorga cero estrellas; menos de 50 otorga una; desde 50, dos; desde 80, tres.
- Las insignias corresponden a una lectura completada, tres lecturas distintas y un intento de 100
  puntos.

## Importar una instalación anterior

El backend funciona únicamente con MySQL. SQLite se utiliza solo para leer un respaldo de la versión
anterior durante la importación.

1. Detén la API anterior y crea un respaldo consistente de su archivo SQLite.
2. Prepara una base MySQL vacía y ejecuta ambos archivos SQL.
3. Configura .env para apuntar a esa base.
4. Ejecuta:

```powershell
npm.cmd run db:import -- 'C:\Respaldos\cana-dulce.sqlite'
```

El importador conserva IDs, hashes de acceso, fechas y resultados. Importa las respuestas antes de
cerrar los intentos para respetar los triggers. Compara todos los campos antes de confirmar la
transacción. Si detecta diferencias, revierte los registros importados; si el destino ya tiene
datos, se detiene. No modifica el SQLite original.

## Copia de seguridad MySQL

Con el cliente mysqldump disponible, usa una cuenta con permisos de lectura y triggers:

```powershell
mysqldump --host=127.0.0.1 --user=root --password --single-transaction --no-tablespaces --set-gtid-purged=OFF --result-file=cana-dulce.sql cana_dulce
```

La contraseña se solicita de forma interactiva. Guarda el archivo fuera del repositorio: contiene
los datos del proyecto y hashes de acceso. Para restaurarlo, prepara una base vacía y ábrelo con una
cuenta administradora en MySQL Workbench. Si cambia el servidor o la cuenta administradora, revisa
los definidores de los triggers antes de restaurar.
