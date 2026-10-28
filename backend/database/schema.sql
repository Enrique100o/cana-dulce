-- MySQL 8.0.43 o posterior. InnoDB conserva las relaciones y transacciones.
-- Los textos se normalizan a NFC en la API. La comparación binaria evita
-- confundir alias distintos por sus acentos.
CREATE TABLE IF NOT EXISTS ESTUDIANTE (
  idEstudiante INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  alias VARCHAR(30) NOT NULL,
  aliasNormalizado VARCHAR(60) NOT NULL,
  grado TINYINT UNSIGNED NOT NULL,
  estado VARCHAR(8) NOT NULL DEFAULT 'ACTIVO',
  codigoAccesoHash VARCHAR(200) NOT NULL,
  CONSTRAINT estudiante_alias_unico UNIQUE (aliasNormalizado),
  CONSTRAINT estudiante_alias_longitud CHECK (CHAR_LENGTH(alias) BETWEEN 3 AND 30),
  CONSTRAINT estudiante_grado CHECK (grado IN (3, 4)),
  CONSTRAINT estudiante_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE IF NOT EXISTS LECTURA (
  idLectura INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  titulo VARCHAR(120) NOT NULL,
  contenido TEXT NOT NULL,
  grado TINYINT UNSIGNED NOT NULL,
  dificultad VARCHAR(5) NOT NULL,
  categoria VARCHAR(50),
  estado VARCHAR(8) NOT NULL DEFAULT 'INACTIVA',
  CONSTRAINT lectura_titulo CHECK (CHAR_LENGTH(titulo) BETWEEN 3 AND 120),
  CONSTRAINT lectura_contenido CHECK (CHAR_LENGTH(contenido) BETWEEN 20 AND 8000),
  CONSTRAINT lectura_grado CHECK (grado IN (3, 4)),
  CONSTRAINT lectura_dificultad CHECK (dificultad IN ('BAJA', 'MEDIA', 'ALTA')),
  CONSTRAINT lectura_categoria CHECK (categoria IS NULL OR CHAR_LENGTH(categoria) BETWEEN 1 AND 50),
  CONSTRAINT lectura_estado CHECK (estado IN ('ACTIVA', 'INACTIVA'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE IF NOT EXISTS PREGUNTA (
  idPregunta INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  idLectura INT UNSIGNED NOT NULL,
  enunciado VARCHAR(400) NOT NULL,
  tipo VARCHAR(14) NOT NULL,
  orden SMALLINT UNSIGNED NOT NULL,
  estado VARCHAR(8) NOT NULL DEFAULT 'ACTIVA',
  CONSTRAINT pregunta_lectura FOREIGN KEY (idLectura) REFERENCES LECTURA (idLectura),
  CONSTRAINT pregunta_orden_unico UNIQUE (idLectura, orden),
  CONSTRAINT pregunta_enunciado CHECK (CHAR_LENGTH(enunciado) BETWEEN 5 AND 400),
  CONSTRAINT pregunta_tipo CHECK (
    tipo IN ('LITERAL', 'INFERENCIAL', 'IDEA_PRINCIPAL', 'VOCABULARIO', 'SECUENCIA')
  ),
  CONSTRAINT pregunta_orden CHECK (orden BETWEEN 1 AND 999),
  CONSTRAINT pregunta_estado CHECK (estado IN ('ACTIVA', 'INACTIVA'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE IF NOT EXISTS OPCION (
  idOpcion INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  idPregunta INT UNSIGNED NOT NULL,
  texto VARCHAR(250) NOT NULL,
  orden TINYINT UNSIGNED NOT NULL,
  esCorrecta TINYINT UNSIGNED NOT NULL,
  CONSTRAINT opcion_pregunta FOREIGN KEY (idPregunta) REFERENCES PREGUNTA (idPregunta),
  CONSTRAINT opcion_orden_unico UNIQUE (idPregunta, orden),
  CONSTRAINT opcion_identidad UNIQUE (idOpcion, idPregunta),
  CONSTRAINT opcion_texto CHECK (CHAR_LENGTH(texto) BETWEEN 1 AND 250),
  CONSTRAINT opcion_orden CHECK (orden BETWEEN 1 AND 4),
  CONSTRAINT opcion_correcta CHECK (esCorrecta IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE IF NOT EXISTS INTENTO (
  idIntento INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  idEstudiante INT UNSIGNED NOT NULL,
  idLectura INT UNSIGNED NOT NULL,
  fechaInicio DATETIME(3) NOT NULL,
  fechaFinalizacion DATETIME(3),
  cantidadCorrectas SMALLINT UNSIGNED,
  totalPreguntas SMALLINT UNSIGNED NOT NULL,
  puntaje DECIMAL(5,2),
  estado VARCHAR(11) NOT NULL DEFAULT 'EN_PROGRESO',
  -- MySQL permite varios NULL en una clave única. Solo los pendientes usan 1.
  pendiente TINYINT GENERATED ALWAYS AS (
    CASE WHEN estado = 'EN_PROGRESO' THEN 1 ELSE NULL END
  ) STORED,
  CONSTRAINT intento_estudiante FOREIGN KEY (idEstudiante) REFERENCES ESTUDIANTE (idEstudiante),
  CONSTRAINT intento_lectura FOREIGN KEY (idLectura) REFERENCES LECTURA (idLectura),
  CONSTRAINT un_intento_pendiente UNIQUE (idEstudiante, idLectura, pendiente),
  INDEX intentos_historial (idEstudiante, fechaInicio DESC, idIntento DESC),
  INDEX intentos_lectura (idLectura),
  CONSTRAINT intento_total CHECK (totalPreguntas > 0),
  CONSTRAINT intento_puntaje CHECK (puntaje IS NULL OR puntaje BETWEEN 0 AND 100),
  CONSTRAINT intento_estado CHECK (estado IN ('EN_PROGRESO', 'COMPLETADO')),
  CONSTRAINT intento_resultado CHECK (
    (estado = 'EN_PROGRESO' AND fechaFinalizacion IS NULL
      AND cantidadCorrectas IS NULL AND puntaje IS NULL)
    OR
    (estado = 'COMPLETADO' AND fechaFinalizacion IS NOT NULL
      AND cantidadCorrectas IS NOT NULL
      AND cantidadCorrectas BETWEEN 0 AND totalPreguntas AND puntaje IS NOT NULL)
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE IF NOT EXISTS RESPUESTA (
  idRespuesta INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  idIntento INT UNSIGNED NOT NULL,
  idPregunta INT UNSIGNED NOT NULL,
  idOpcion INT UNSIGNED NOT NULL,
  esCorrecta TINYINT UNSIGNED NOT NULL,
  CONSTRAINT respuesta_intento FOREIGN KEY (idIntento) REFERENCES INTENTO (idIntento),
  CONSTRAINT respuesta_pregunta FOREIGN KEY (idPregunta) REFERENCES PREGUNTA (idPregunta),
  CONSTRAINT respuesta_opcion FOREIGN KEY (idOpcion, idPregunta)
    REFERENCES OPCION (idOpcion, idPregunta),
  CONSTRAINT respuesta_unica UNIQUE (idIntento, idPregunta),
  CONSTRAINT respuesta_correcta CHECK (esCorrecta IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
