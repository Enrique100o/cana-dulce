import { transaction } from './db.mjs';
import { ApiError, missing, forbidden } from './errors.mjs';
import { id, query, queryGrade, enumeration, STATES, text, normalize, readingInput, questionInput, noBody } from './validation.mjs';
import { teacher } from './auth.mjs';

export function findReading(db, readingId) {
  const row = db.prepare('SELECT * FROM LECTURA WHERE idLectura=?').get(readingId);
  if (!row) missing('La lectura');
  return row;
}
export function questions(db, readingId, state) {
  return db.prepare(`SELECT * FROM PREGUNTA WHERE idLectura=? ${state ? 'AND estado=?' : ''} ORDER BY orden,idPregunta`)
    .all(...(state ? [readingId,state] : [readingId]));
}
export function options(db, questionId) {
  return db.prepare('SELECT * FROM OPCION WHERE idPregunta=? ORDER BY orden,idOpcion').all(questionId);
}
export function publishable(db, readingId) {
  const active = questions(db, readingId, 'ACTIVA');
  return active.length > 0 && active.every(question => {
    const choices = options(db, question.idPregunta);
    return choices.length >= 2 && choices.length <= 4 && choices.filter(o => o.esCorrecta === 1).length === 1;
  });
}
export function readingDto(db, row, summary = false) {
  return {
    idLectura: row.idLectura, titulo: row.titulo, ...(!summary ? { contenido: row.contenido } : {}),
    grado: row.grado, dificultad: row.dificultad, categoria: row.categoria, estado: row.estado,
    totalPreguntasActivas: questions(db, row.idLectura, 'ACTIVA').length,
    bloqueadaParaEdicion: Boolean(db.prepare('SELECT 1 FROM INTENTO WHERE idLectura=? LIMIT 1').get(row.idLectura))
  };
}
export function questionDto(db, row, administrative = false) {
  // Lista explícita de propiedades: la clave de corrección no se serializa para alumnos.
  return {
    idPregunta: row.idPregunta, idLectura: row.idLectura, enunciado: row.enunciado, tipo: row.tipo, orden: row.orden,
    ...(administrative ? { estado: row.estado } : {}),
    opciones: options(db, row.idPregunta).map(o => ({
      idOpcion: o.idOpcion, texto: o.texto, orden: o.orden,
      ...(administrative ? { esCorrecta: Boolean(o.esCorrecta) } : {})
    }))
  };
}
export function checkReadingAccess(db, req, row) {
  if (req.user.rol === 'DOCENTE') return;
  const student = req.user.estudiante;
  const historical = db.prepare('SELECT 1 FROM INTENTO WHERE idEstudiante=? AND idLectura=? LIMIT 1').get(student.idEstudiante, row.idLectura);
  if (!historical && !(row.grado === student.grado && row.estado === 'ACTIVA' && publishable(db, row.idLectura))) missing('La lectura');
}
export function ensureMutable(db, readingId) {
  if (db.prepare('SELECT 1 FROM INTENTO WHERE idLectura=? LIMIT 1').get(readingId)) {
    throw new ApiError(409, 'CONTENIDO_BLOQUEADO', 'Esta lectura tiene intentos. Crea otra lectura para cambiar su contenido.');
  }
}
function ensureQuestionOrder(db, readingId, order, questionId = 0) {
  if (db.prepare('SELECT 1 FROM PREGUNTA WHERE idLectura=? AND orden=? AND idPregunta<>?').get(readingId, order, questionId)) {
    throw new ApiError(409, 'ORDEN_OCUPADO', 'Ese orden ya está ocupado por otra pregunta de la lectura.');
  }
}
function saveOptions(db, questionId, choices) {
  const insert = db.prepare('INSERT INTO OPCION(idPregunta,texto,orden,esCorrecta) VALUES(?,?,?,?)');
  for (const choice of choices) insert.run(questionId, choice.texto, choice.orden, Number(choice.esCorrecta));
}
function findQuestion(db, questionId) {
  const question = db.prepare('SELECT * FROM PREGUNTA WHERE idPregunta=?').get(questionId);
  if (!question) missing('La pregunta');
  return question;
}
function keepActiveQuestion(db, reading, question, newState) {
  if (reading.estado === 'ACTIVA' && question.estado === 'ACTIVA' && newState === 'INACTIVA'
      && questions(db, reading.idLectura, 'ACTIVA').length === 1) {
    throw new ApiError(409, 'LECTURA_SIN_PREGUNTAS', 'Desactiva primero la lectura; no puede quedar activa sin preguntas.');
  }
}

export function readingRoutes(app, db) {
  app.get('/api/lecturas', (req, res) => {
    noBody(req);
    const filters = query(req.query, ['grado','q','estado']);
    const where = [], args = [];
    const filterGrade = filters.grado !== undefined ? queryGrade(filters.grado) : undefined;
    const filterState = filters.estado !== undefined ? enumeration(filters.estado, 'estado', STATES) : undefined;
    if (req.user.rol === 'ESTUDIANTE') {
      if ((filterGrade !== undefined && filterGrade !== req.user.estudiante.grado) || filterState === 'INACTIVA') forbidden();
      where.push("grado=? AND estado='ACTIVA'"); args.push(req.user.estudiante.grado);
    } else {
      if (filterGrade !== undefined) { where.push('grado=?'); args.push(filterGrade); }
      if (filterState !== undefined) { where.push('estado=?'); args.push(filterState); }
    }
    if (filters.q !== undefined) { where.push('instr(normalizar(titulo),?)>0'); args.push(normalize(text(filters.q, 'q', 1, 100))); }
    let rows = db.prepare(`SELECT * FROM LECTURA ${where.length ? 'WHERE '+where.join(' AND ') : ''} ORDER BY normalizar(titulo),idLectura`).all(...args);
    if (req.user.rol === 'ESTUDIANTE') rows = rows.filter(row => publishable(db, row.idLectura));
    const datos = rows.map(row => readingDto(db, row, true));
    res.json({ datos, total: datos.length });
  });
  app.get('/api/lecturas/:id', (req, res) => {
    noBody(req); query(req.query, []);
    const row = findReading(db, id(req.params.id));
    checkReadingAccess(db, req, row);
    res.json(readingDto(db, row));
  });
  app.post('/api/lecturas', teacher, (req, res) => {
    const input = readingInput(req.body);
    const row = transaction(db, () => {
      const result = db.prepare('INSERT INTO LECTURA(titulo,contenido,grado,dificultad,categoria) VALUES(?,?,?,?,?)')
        .run(input.titulo, input.contenido, input.grado, input.dificultad, input.categoria);
      return findReading(db, Number(result.lastInsertRowid));
    });
    res.status(201).location(`/api/lecturas/${row.idLectura}`).json(readingDto(db, row));
  });
  app.put('/api/lecturas/:id', teacher, (req, res) => {
    const readingId = id(req.params.id), input = readingInput(req.body, true);
    const row = transaction(db, () => {
      const old = findReading(db, readingId);
      const used = db.prepare('SELECT 1 FROM INTENTO WHERE idLectura=? LIMIT 1').get(readingId);
      if (used && ['titulo','contenido','grado','dificultad','categoria'].some(key => old[key] !== input[key])) ensureMutable(db, readingId);
      if (input.estado === 'ACTIVA' && !publishable(db, readingId)) {
        throw new ApiError(409, 'LECTURA_NO_PUBLICABLE', 'Agrega al menos una pregunta activa válida antes de activar la lectura.');
      }
      db.prepare('UPDATE LECTURA SET titulo=?,contenido=?,grado=?,dificultad=?,categoria=?,estado=? WHERE idLectura=?')
        .run(input.titulo,input.contenido,input.grado,input.dificultad,input.categoria,input.estado,readingId);
      return findReading(db, readingId);
    });
    res.json(readingDto(db, row));
  });
  app.delete('/api/lecturas/:id', teacher, (req, res) => {
    noBody(req);
    const readingId = id(req.params.id);
    transaction(db, () => { findReading(db, readingId); db.prepare("UPDATE LECTURA SET estado='INACTIVA' WHERE idLectura=?").run(readingId); });
    res.status(204).end();
  });
  app.get('/api/lecturas/:id/preguntas', (req, res) => {
    noBody(req); query(req.query, []);
    const readingId = id(req.params.id), row = findReading(db, readingId);
    checkReadingAccess(db, req, row);
    const datos = questions(db, readingId, 'ACTIVA').map(q => questionDto(db, q));
    res.json({ datos, total: datos.length });
  });
  app.get('/api/docente/lecturas/:id/preguntas', teacher, (req, res) => {
    noBody(req);
    const readingId = id(req.params.id); findReading(db, readingId);
    const filters = query(req.query, ['estado']);
    const state = filters.estado !== undefined ? enumeration(filters.estado, 'estado', STATES) : undefined;
    const datos = questions(db, readingId, state).map(q => questionDto(db, q, true));
    res.json({ datos, total: datos.length });
  });
  app.post('/api/lecturas/:id/preguntas', teacher, (req, res) => {
    const readingId = id(req.params.id), input = questionInput(req.body);
    const result = transaction(db, () => {
      findReading(db, readingId); ensureMutable(db, readingId); ensureQuestionOrder(db, readingId, input.orden);
      const insert = db.prepare('INSERT INTO PREGUNTA(idLectura,enunciado,tipo,orden) VALUES(?,?,?,?)').run(readingId,input.enunciado,input.tipo,input.orden);
      const questionId = Number(insert.lastInsertRowid);
      saveOptions(db, questionId, input.opciones);
      return questionDto(db, findQuestion(db, questionId), true);
    });
    res.status(201).json(result);
  });
  app.put('/api/preguntas/:id', teacher, (req, res) => {
    const questionId = id(req.params.id), input = questionInput(req.body, true);
    const result = transaction(db, () => {
      const old = findQuestion(db, questionId), reading = findReading(db, old.idLectura);
      ensureMutable(db, reading.idLectura); ensureQuestionOrder(db, reading.idLectura, input.orden, questionId);
      keepActiveQuestion(db, reading, old, input.estado);
      db.prepare('UPDATE PREGUNTA SET enunciado=?,tipo=?,orden=?,estado=? WHERE idPregunta=?').run(input.enunciado,input.tipo,input.orden,input.estado,questionId);
      db.prepare('DELETE FROM OPCION WHERE idPregunta=?').run(questionId);
      saveOptions(db, questionId, input.opciones);
      return questionDto(db, findQuestion(db, questionId), true);
    });
    res.json(result);
  });
  app.delete('/api/preguntas/:id', teacher, (req, res) => {
    noBody(req);
    const questionId = id(req.params.id);
    transaction(db, () => {
      const question = findQuestion(db, questionId), reading = findReading(db, question.idLectura);
      ensureMutable(db, reading.idLectura); keepActiveQuestion(db, reading, question, 'INACTIVA');
      db.prepare("UPDATE PREGUNTA SET estado='INACTIVA' WHERE idPregunta=?").run(questionId);
    });
    res.status(204).end();
  });
}
