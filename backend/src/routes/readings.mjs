import { transaction } from '../db.mjs';
import { ApiError, missing, forbidden } from '../errors.mjs';
import {
  id,
  query,
  queryGrade,
  enumeration,
  STATES,
  text,
  normalize,
  readingInput,
  questionInput,
  noBody,
} from '../validation.mjs';
import { teacher } from '../auth.mjs';

export async function findReading(db, readingId, lock = false) {
  const row = await db.one(
    'SELECT * FROM LECTURA WHERE idLectura=?' + (lock ? ' FOR UPDATE' : ''),
    [readingId],
  );
  if (!row) missing('La lectura');
  return row;
}

export function questions(db, readingId, state) {
  return db.all(
    `SELECT * FROM PREGUNTA
     WHERE idLectura=? ${state ? 'AND estado=?' : ''}
     ORDER BY orden, idPregunta`,
    state ? [readingId, state] : [readingId],
  );
}

export function options(db, questionId) {
  return db.all('SELECT * FROM OPCION WHERE idPregunta=? ORDER BY orden,idOpcion', [questionId]);
}

export async function publishable(db, readingId) {
  const active = await questions(db, readingId, 'ACTIVA');
  if (!active.length) return false;
  for (const question of active) {
    const choices = await options(db, question.idPregunta);
    if (
      choices.length < 2 ||
      choices.length > 4 ||
      choices.filter((choice) => choice.esCorrecta === 1).length !== 1
    )
      return false;
  }
  return true;
}

export async function readingDto(db, row, summary = false) {
  const active = await questions(db, row.idLectura, 'ACTIVA');
  const used = await db.one('SELECT 1 FROM INTENTO WHERE idLectura=? LIMIT 1', [row.idLectura]);
  return {
    idLectura: row.idLectura,
    titulo: row.titulo,
    ...(!summary ? { contenido: row.contenido } : {}),
    grado: row.grado,
    dificultad: row.dificultad,
    categoria: row.categoria,
    estado: row.estado,
    totalPreguntasActivas: active.length,
    bloqueadaParaEdicion: Boolean(used),
  };
}

export async function questionDto(db, row, administrative = false) {
  const choices = await options(db, row.idPregunta);
  // La clave de corrección solo se incluye en la consulta administrativa.
  return {
    idPregunta: row.idPregunta,
    idLectura: row.idLectura,
    enunciado: row.enunciado,
    tipo: row.tipo,
    orden: row.orden,
    ...(administrative ? { estado: row.estado } : {}),
    opciones: choices.map((choice) => ({
      idOpcion: choice.idOpcion,
      texto: choice.texto,
      orden: choice.orden,
      ...(administrative ? { esCorrecta: Boolean(choice.esCorrecta) } : {}),
    })),
  };
}

export async function checkReadingAccess(db, req, row) {
  if (req.user.rol === 'DOCENTE') return;
  const student = req.user.estudiante;
  const historical = await db.one(
    'SELECT 1 FROM INTENTO WHERE idEstudiante=? AND idLectura=? LIMIT 1',
    [student.idEstudiante, row.idLectura],
  );
  if (
    !historical &&
    !(
      row.grado === student.grado &&
      row.estado === 'ACTIVA' &&
      (await publishable(db, row.idLectura))
    )
  )
    missing('La lectura');
}

export async function ensureMutable(db, readingId) {
  if (await db.one('SELECT 1 FROM INTENTO WHERE idLectura=? LIMIT 1', [readingId])) {
    throw new ApiError(
      409,
      'CONTENIDO_BLOQUEADO',
      'Esta lectura tiene intentos. Crea otra lectura para cambiar su contenido.',
    );
  }
}

async function ensureQuestionOrder(db, readingId, order, questionId = 0) {
  if (
    await db.one('SELECT 1 FROM PREGUNTA WHERE idLectura=? AND orden=? AND idPregunta<>?', [
      readingId,
      order,
      questionId,
    ])
  ) {
    throw new ApiError(
      409,
      'ORDEN_OCUPADO',
      'Ese orden ya está ocupado por otra pregunta de la lectura.',
    );
  }
}

async function saveOptions(db, questionId, choices) {
  for (const choice of choices) {
    await db.run('INSERT INTO OPCION(idPregunta,texto,orden,esCorrecta) VALUES(?,?,?,?)', [
      questionId,
      choice.texto,
      choice.orden,
      Number(choice.esCorrecta),
    ]);
  }
}

async function findQuestion(db, questionId) {
  const question = await db.one('SELECT * FROM PREGUNTA WHERE idPregunta=?', [questionId]);
  if (!question) missing('La pregunta');
  return question;
}

async function keepActiveQuestion(db, reading, question, newState) {
  if (
    reading.estado === 'ACTIVA' &&
    question.estado === 'ACTIVA' &&
    newState === 'INACTIVA' &&
    (await questions(db, reading.idLectura, 'ACTIVA')).length === 1
  ) {
    throw new ApiError(
      409,
      'LECTURA_SIN_PREGUNTAS',
      'Desactiva primero la lectura; no puede quedar activa sin preguntas.',
    );
  }
}

export function readingRoutes(app, db) {
  app.get('/api/lecturas', async (req, res) => {
    noBody(req);
    const filters = query(req.query, ['grado', 'q', 'estado']);
    const where = [];
    const args = [];
    const filterGrade = filters.grado !== undefined ? queryGrade(filters.grado) : undefined;
    const filterState =
      filters.estado !== undefined ? enumeration(filters.estado, 'estado', STATES) : undefined;
    if (req.user.rol === 'ESTUDIANTE') {
      if (
        (filterGrade !== undefined && filterGrade !== req.user.estudiante.grado) ||
        filterState === 'INACTIVA'
      )
        forbidden();
      where.push("grado=? AND estado='ACTIVA'");
      args.push(req.user.estudiante.grado);
    } else {
      if (filterGrade !== undefined) {
        where.push('grado=?');
        args.push(filterGrade);
      }
      if (filterState !== undefined) {
        where.push('estado=?');
        args.push(filterState);
      }
    }
    if (filters.q !== undefined) {
      where.push('LOCATE(?,LOWER(titulo))>0');
      args.push(normalize(text(filters.q, 'q', 1, 100)));
    }
    const rows = await db.all(
      `SELECT * FROM LECTURA
       ${where.length ? 'WHERE ' + where.join(' AND ') : ''}
       ORDER BY LOWER(titulo), idLectura`,
      args,
    );
    const datos = [];
    for (const row of rows) {
      if (req.user.rol === 'ESTUDIANTE' && !(await publishable(db, row.idLectura))) continue;
      datos.push(await readingDto(db, row, true));
    }
    res.json({ datos, total: datos.length });
  });

  app.get('/api/lecturas/:id', async (req, res) => {
    noBody(req);
    query(req.query, []);
    const row = await findReading(db, id(req.params.id));
    await checkReadingAccess(db, req, row);
    res.json(await readingDto(db, row));
  });

  app.post('/api/lecturas', teacher, async (req, res) => {
    const input = readingInput(req.body);
    const result = await transaction(db, async (tx) => {
      const insert = await tx.run(
        'INSERT INTO LECTURA(titulo,contenido,grado,dificultad,categoria) VALUES(?,?,?,?,?)',
        [input.titulo, input.contenido, input.grado, input.dificultad, input.categoria],
      );
      return readingDto(tx, await findReading(tx, insert.insertId));
    });
    res.status(201).location(`/api/lecturas/${result.idLectura}`).json(result);
  });

  app.put('/api/lecturas/:id', teacher, async (req, res) => {
    const readingId = id(req.params.id);
    const input = readingInput(req.body, true);
    const result = await transaction(db, async (tx) => {
      // Todas las escrituras de contenido y los nuevos intentos bloquean primero la lectura.
      const old = await findReading(tx, readingId, true);
      if (
        ['titulo', 'contenido', 'grado', 'dificultad', 'categoria'].some(
          (key) => old[key] !== input[key],
        )
      )
        await ensureMutable(tx, readingId);
      if (input.estado === 'ACTIVA' && !(await publishable(tx, readingId))) {
        throw new ApiError(
          409,
          'LECTURA_NO_PUBLICABLE',
          'Agrega al menos una pregunta activa válida antes de activar la lectura.',
        );
      }
      await tx.run(
        `UPDATE LECTURA SET titulo=?,contenido=?,grado=?,dificultad=?,categoria=?,estado=?
         WHERE idLectura=?`,
        [
          input.titulo,
          input.contenido,
          input.grado,
          input.dificultad,
          input.categoria,
          input.estado,
          readingId,
        ],
      );
      return readingDto(tx, await findReading(tx, readingId));
    });
    res.json(result);
  });

  app.delete('/api/lecturas/:id', teacher, async (req, res) => {
    noBody(req);
    const readingId = id(req.params.id);
    await transaction(db, async (tx) => {
      await findReading(tx, readingId, true);
      await tx.run("UPDATE LECTURA SET estado='INACTIVA' WHERE idLectura=?", [readingId]);
    });
    res.status(204).end();
  });

  app.get('/api/lecturas/:id/preguntas', async (req, res) => {
    noBody(req);
    query(req.query, []);
    const readingId = id(req.params.id);
    const row = await findReading(db, readingId);
    await checkReadingAccess(db, req, row);
    const datos = [];
    for (const question of await questions(db, readingId, 'ACTIVA')) {
      datos.push(await questionDto(db, question));
    }
    res.json({ datos, total: datos.length });
  });

  app.get('/api/docente/lecturas/:id/preguntas', teacher, async (req, res) => {
    noBody(req);
    const readingId = id(req.params.id);
    await findReading(db, readingId);
    const filters = query(req.query, ['estado']);
    const state =
      filters.estado !== undefined ? enumeration(filters.estado, 'estado', STATES) : undefined;
    const datos = [];
    for (const question of await questions(db, readingId, state)) {
      datos.push(await questionDto(db, question, true));
    }
    res.json({ datos, total: datos.length });
  });

  app.post('/api/lecturas/:id/preguntas', teacher, async (req, res) => {
    const readingId = id(req.params.id);
    const input = questionInput(req.body);
    const result = await transaction(db, async (tx) => {
      await findReading(tx, readingId, true);
      await ensureMutable(tx, readingId);
      await ensureQuestionOrder(tx, readingId, input.orden);
      const insert = await tx.run(
        'INSERT INTO PREGUNTA(idLectura,enunciado,tipo,orden) VALUES(?,?,?,?)',
        [readingId, input.enunciado, input.tipo, input.orden],
      );
      await saveOptions(tx, insert.insertId, input.opciones);
      return questionDto(tx, await findQuestion(tx, insert.insertId), true);
    });
    res.status(201).json(result);
  });

  app.put('/api/preguntas/:id', teacher, async (req, res) => {
    const questionId = id(req.params.id);
    const input = questionInput(req.body, true);
    const result = await transaction(db, async (tx) => {
      const initial = await findQuestion(tx, questionId);
      const reading = await findReading(tx, initial.idLectura, true);
      const old = await findQuestion(tx, questionId);
      await ensureMutable(tx, reading.idLectura);
      await ensureQuestionOrder(tx, reading.idLectura, input.orden, questionId);
      await keepActiveQuestion(tx, reading, old, input.estado);
      await tx.run('UPDATE PREGUNTA SET enunciado=?,tipo=?,orden=?,estado=? WHERE idPregunta=?', [
        input.enunciado,
        input.tipo,
        input.orden,
        input.estado,
        questionId,
      ]);
      await tx.run('DELETE FROM OPCION WHERE idPregunta=?', [questionId]);
      await saveOptions(tx, questionId, input.opciones);
      return questionDto(tx, await findQuestion(tx, questionId), true);
    });
    res.json(result);
  });

  app.delete('/api/preguntas/:id', teacher, async (req, res) => {
    noBody(req);
    const questionId = id(req.params.id);
    await transaction(db, async (tx) => {
      const initial = await findQuestion(tx, questionId);
      const reading = await findReading(tx, initial.idLectura, true);
      const question = await findQuestion(tx, questionId);
      await ensureMutable(tx, reading.idLectura);
      await keepActiveQuestion(tx, reading, question, 'INACTIVA');
      await tx.run("UPDATE PREGUNTA SET estado='INACTIVA' WHERE idPregunta=?", [questionId]);
    });
    res.status(204).end();
  });
}
