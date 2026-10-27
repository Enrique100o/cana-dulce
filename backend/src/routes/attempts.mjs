import { transaction } from '../db.mjs';
import { ApiError, missing, invalid } from '../errors.mjs';
import { student, ownStudent } from '../auth.mjs';
import { id, integer, object, query, enumeration, noBody } from '../validation.mjs';
import { findReading, publishable, questions, options } from './readings.mjs';
import { score, stars } from '../scoring.mjs';

function findAttempt(db, attemptId) {
  const row = db
    .prepare(
      `
SELECT
  i.*,
  l.titulo AS tituloLectura
FROM
  INTENTO i
  JOIN LECTURA l USING (idLectura)
WHERE
  i.idIntento = ?
`,
    )
    .get(attemptId);
  if (!row) missing('El intento');
  return row;
}
export function attemptDto(db, row, detail = true) {
  const result = {
    idIntento: row.idIntento,
    idEstudiante: row.idEstudiante,
    idLectura: row.idLectura,
    tituloLectura: row.tituloLectura,
    fechaInicio: row.fechaInicio,
    fechaFinalizacion: row.fechaFinalizacion,
    cantidadCorrectas: row.cantidadCorrectas,
    totalPreguntas: row.totalPreguntas,
    puntaje: row.puntaje,
    estrellas: stars(row.puntaje),
    estado: row.estado,
  };
  if (detail)
    result.respuestas = db
      .prepare(
        `
    SELECT r.idRespuesta,r.idPregunta,p.enunciado,p.tipo,p.orden,r.idOpcion,
           o.texto AS textoOpcionElegida,r.esCorrecta
    FROM RESPUESTA r JOIN PREGUNTA p USING(idPregunta) JOIN OPCION o ON o.idOpcion=r.idOpcion
    WHERE r.idIntento=? ORDER BY p.orden,p.idPregunta`,
      )
      .all(row.idIntento)
      .map((r) => ({ ...r, esCorrecta: Boolean(r.esCorrecta) }));
  return result;
}
function answersInput(body) {
  object(body, ['respuestas']);
  if (!Array.isArray(body.respuestas)) invalid('respuestas', 'Debe ser un arreglo de selecciones.');
  if (body.respuestas.length > 999)
    invalid('respuestas', 'El arreglo supera la cantidad máxima de preguntas.');
  const selections = body.respuestas.map((answer, index) => {
    const prefix = `respuestas[${index}].`;
    object(answer, ['idPregunta', 'idOpcion'], [], prefix);
    return {
      idPregunta: integer(answer.idPregunta, prefix + 'idPregunta'),
      idOpcion: integer(answer.idOpcion, prefix + 'idOpcion'),
    };
  });
  if (new Set(selections.map((a) => a.idPregunta)).size !== selections.length) {
    throw new ApiError(400, 'RESPUESTA_DUPLICADA', 'Envía una sola respuesta por pregunta.');
  }
  return selections;
}

export function attemptRoutes(app, db) {
  app.post('/api/intentos', student, (req, res) => {
    object(req.body, ['idLectura']);
    const readingId = integer(req.body.idLectura, 'idLectura');
    const studentId = req.user.estudiante.idEstudiante;
    const result = transaction(db, () => {
      const reading = findReading(db, readingId);
      if (reading.estado !== 'ACTIVA' || !publishable(db, readingId)) missing('La lectura');
      if (reading.grado !== req.user.estudiante.grado)
        throw new ApiError(403, 'SIN_PERMISO', 'Elige una lectura de tu grado.');
      const existing = db
        .prepare(
          'SELECT idIntento FROM INTENTO WHERE idEstudiante=? AND idLectura=? AND ' +
            "estado='EN_PROGRESO'",
        )
        .get(studentId, readingId);
      if (existing)
        throw new ApiError(
          409,
          'INTENTO_EN_PROGRESO_EXISTENTE',
          'Ya tienes una actividad en curso para esta lectura.',
          [],
          { idIntentoExistente: existing.idIntento },
        );
      const total = questions(db, readingId, 'ACTIVA').length;
      const insert = db
        .prepare(
          'INSERT INTO INTENTO(idEstudiante,idLectura,fechaInicio,totalPreguntas) ' +
            'VALUES(?,?,?,?)',
        )
        .run(studentId, readingId, new Date().toISOString(), total);
      return attemptDto(db, findAttempt(db, Number(insert.lastInsertRowid)));
    });
    res.status(201).location(`/api/intentos/${result.idIntento}`).json(result);
  });
  app.get('/api/intentos', (req, res) => {
    noBody(req);
    const filters = query(req.query, ['estudianteId', 'idLectura', 'estado']);
    const where = [],
      args = [];
    const selectedStudent =
      filters.estudianteId !== undefined ? id(filters.estudianteId, 'estudianteId') : undefined;
    if (selectedStudent !== undefined) ownStudent(req, selectedStudent);
    const studentId =
      req.user.rol === 'ESTUDIANTE' ? req.user.estudiante.idEstudiante : selectedStudent;
    if (studentId !== undefined) {
      if (!db.prepare('SELECT 1 FROM ESTUDIANTE WHERE idEstudiante=?').get(studentId))
        missing('El estudiante');
      where.push('i.idEstudiante=?');
      args.push(studentId);
    }
    if (filters.idLectura !== undefined) {
      const readingId = id(filters.idLectura, 'idLectura');
      findReading(db, readingId);
      where.push('i.idLectura=?');
      args.push(readingId);
    }
    if (filters.estado !== undefined) {
      where.push('i.estado=?');
      args.push(enumeration(filters.estado, 'estado', ['EN_PROGRESO', 'COMPLETADO']));
    }
    const rows = db
      .prepare(
        `SELECT i.*, l.titulo AS tituloLectura
         FROM INTENTO i JOIN LECTURA l USING(idLectura)
         ${where.length ? 'WHERE ' + where.join(' AND ') : ''}
         ORDER BY i.fechaInicio DESC, i.idIntento DESC`,
      )
      .all(...args);
    const datos = rows.map((row) => attemptDto(db, row, false));
    res.json({ datos, total: datos.length });
  });
  app.get('/api/intentos/:id', (req, res) => {
    noBody(req);
    query(req.query, []);
    const row = findAttempt(db, id(req.params.id));
    ownStudent(req, row.idEstudiante);
    res.json(attemptDto(db, row));
  });
  app.post('/api/intentos/:id/finalizar', student, (req, res) => {
    const attemptId = id(req.params.id),
      selections = answersInput(req.body);
    const result = transaction(db, () => {
      const attempt = findAttempt(db, attemptId);
      ownStudent(req, attempt.idEstudiante);
      // La lectura puede estar inactiva: se termina la actividad que ya había empezado.
      const active = questions(db, attempt.idLectura, 'ACTIVA');
      const byId = new Map(active.map((q) => [q.idPregunta, q]));
      const evaluated = selections.map((selection) => {
        const question = byId.get(selection.idPregunta);
        if (!question)
          throw new ApiError(400, 'PREGUNTA_AJENA', 'Una pregunta no pertenece a esta actividad.', [
            { campo: 'respuestas', mensaje: `Pregunta ${selection.idPregunta} no válida.` },
          ]);
        const choice = options(db, question.idPregunta).find(
          (o) => o.idOpcion === selection.idOpcion,
        );
        if (!choice)
          throw new ApiError(400, 'OPCION_AJENA', 'Una opción no pertenece a su pregunta.', [
            {
              campo: 'respuestas',
              mensaje: `Opción no válida para pregunta ${question.idPregunta}.`,
            },
          ]);
        return { ...selection, esCorrecta: choice.esCorrecta };
      });
      if (attempt.estado === 'COMPLETADO') {
        const saved = db
          .prepare('SELECT idPregunta,idOpcion FROM RESPUESTA WHERE idIntento=?')
          .all(attemptId);
        if (
          saved.length === selections.length &&
          saved.every((a) =>
            selections.some((s) => s.idPregunta === a.idPregunta && s.idOpcion === a.idOpcion),
          )
        ) {
          return attemptDto(db, attempt);
        }
        throw new ApiError(
          409,
          'INTENTO_YA_COMPLETADO',
          'El intento ya tiene un resultado guardado y no puede modificarse.',
        );
      }
      const selectedIds = new Set(selections.map((a) => a.idPregunta));
      const missingQuestions = active.filter((q) => !selectedIds.has(q.idPregunta));
      if (missingQuestions.length || selections.length !== attempt.totalPreguntas) {
        throw new ApiError(
          400,
          'RESPUESTAS_INCOMPLETAS',
          'Responde todas las preguntas antes de finalizar.',
          missingQuestions.map((q) => ({
            campo: 'respuestas',
            mensaje: `Falta responder la pregunta ${q.idPregunta}.`,
          })),
        );
      }
      const correct = evaluated.filter((a) => a.esCorrecta === 1).length;
      const value = score(correct, attempt.totalPreguntas);
      const insert = db.prepare(
        'INSERT INTO RESPUESTA(idIntento,idPregunta,idOpcion,esCorrecta) VALUES(?,?,?,?)',
      );
      for (const answer of evaluated)
        insert.run(attemptId, answer.idPregunta, answer.idOpcion, answer.esCorrecta);
      db.prepare(
        `
UPDATE INTENTO
SET
  fechaFinalizacion = ?,
  cantidadCorrectas = ?,
  puntaje = ?,
  estado = 'COMPLETADO'
WHERE
  idIntento = ?
`,
      ).run(new Date().toISOString(), correct, value, attemptId);
      return attemptDto(db, findAttempt(db, attemptId));
    });
    res.json(result);
  });
}
