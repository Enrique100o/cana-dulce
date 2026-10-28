import { transaction } from '../db.mjs';
import { ApiError, missing } from '../errors.mjs';
import { student, ownStudent } from '../auth.mjs';
import { id, integer, object, query, enumeration, noBody } from '../validation.mjs';
import { findReading, publishable, questions, options } from './readings.mjs';
import { score, stars } from '../scoring.mjs';

async function findAttempt(db, attemptId, lock = false) {
  const row = await db.one(
    'SELECT * FROM INTENTO WHERE idIntento=?' + (lock ? ' FOR UPDATE' : ''),
    [attemptId],
  );
  if (!row) missing('El intento');
  const reading = await findReading(db, row.idLectura);
  return { ...row, tituloLectura: reading.titulo };
}

export async function attemptDto(db, row, detail = true) {
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
  if (detail) {
    const answers = await db.all(
      `SELECT r.idRespuesta,r.idPregunta,p.enunciado,p.tipo,p.orden,r.idOpcion,
              o.texto AS textoOpcionElegida,r.esCorrecta
       FROM RESPUESTA r
       JOIN PREGUNTA p USING(idPregunta)
       JOIN OPCION o ON o.idOpcion=r.idOpcion
       WHERE r.idIntento=? ORDER BY p.orden,p.idPregunta`,
      [row.idIntento],
    );
    result.respuestas = answers.map((answer) => ({
      ...answer,
      esCorrecta: Boolean(answer.esCorrecta),
    }));
  }
  return result;
}

function answersInput(body) {
  object(body, ['respuestas']);
  if (!Array.isArray(body.respuestas)) {
    throw new ApiError(400, 'ENTRADA_INVALIDA', 'Revisa los datos enviados.', [
      { campo: 'respuestas', mensaje: 'Debe ser un arreglo de selecciones.' },
    ]);
  }
  if (body.respuestas.length > 999) {
    throw new ApiError(400, 'ENTRADA_INVALIDA', 'Revisa los datos enviados.', [
      { campo: 'respuestas', mensaje: 'El arreglo supera la cantidad máxima de preguntas.' },
    ]);
  }
  const selections = body.respuestas.map((answer, index) => {
    const prefix = `respuestas[${index}].`;
    object(answer, ['idPregunta', 'idOpcion'], [], prefix);
    return {
      idPregunta: integer(answer.idPregunta, prefix + 'idPregunta'),
      idOpcion: integer(answer.idOpcion, prefix + 'idOpcion'),
    };
  });
  if (new Set(selections.map((answer) => answer.idPregunta)).size !== selections.length) {
    throw new ApiError(400, 'RESPUESTA_DUPLICADA', 'Envía una sola respuesta por pregunta.');
  }
  return selections;
}

export function attemptRoutes(app, db) {
  app.post('/api/intentos', student, async (req, res) => {
    object(req.body, ['idLectura']);
    const readingId = integer(req.body.idLectura, 'idLectura');
    const studentId = req.user.estudiante.idEstudiante;
    const result = await transaction(db, async (tx) => {
      const reading = await findReading(tx, readingId, true);
      if (reading.estado !== 'ACTIVA' || !(await publishable(tx, readingId))) missing('La lectura');
      if (reading.grado !== req.user.estudiante.grado) {
        throw new ApiError(403, 'SIN_PERMISO', 'Elige una lectura de tu grado.');
      }
      const existing = await tx.one(
        "SELECT idIntento FROM INTENTO WHERE idEstudiante=? AND idLectura=? AND estado='EN_PROGRESO'",
        [studentId, readingId],
      );
      if (existing) {
        throw new ApiError(
          409,
          'INTENTO_EN_PROGRESO_EXISTENTE',
          'Ya tienes una actividad en curso para esta lectura.',
          [],
          { idIntentoExistente: existing.idIntento },
        );
      }
      const total = (await questions(tx, readingId, 'ACTIVA')).length;
      const insert = await tx.run(
        'INSERT INTO INTENTO(idEstudiante,idLectura,fechaInicio,totalPreguntas) VALUES(?,?,?,?)',
        [studentId, readingId, new Date(), total],
      );
      return attemptDto(tx, await findAttempt(tx, insert.insertId));
    });
    res.status(201).location(`/api/intentos/${result.idIntento}`).json(result);
  });

  app.get('/api/intentos', async (req, res) => {
    noBody(req);
    const filters = query(req.query, ['estudianteId', 'idLectura', 'estado']);
    const where = [];
    const args = [];
    const selectedStudent =
      filters.estudianteId !== undefined ? id(filters.estudianteId, 'estudianteId') : undefined;
    if (selectedStudent !== undefined) ownStudent(req, selectedStudent);
    const studentId =
      req.user.rol === 'ESTUDIANTE' ? req.user.estudiante.idEstudiante : selectedStudent;
    if (studentId !== undefined) {
      if (!(await db.one('SELECT 1 FROM ESTUDIANTE WHERE idEstudiante=?', [studentId]))) {
        missing('El estudiante');
      }
      where.push('i.idEstudiante=?');
      args.push(studentId);
    }
    if (filters.idLectura !== undefined) {
      const readingId = id(filters.idLectura, 'idLectura');
      await findReading(db, readingId);
      where.push('i.idLectura=?');
      args.push(readingId);
    }
    if (filters.estado !== undefined) {
      where.push('i.estado=?');
      args.push(enumeration(filters.estado, 'estado', ['EN_PROGRESO', 'COMPLETADO']));
    }
    const rows = await db.all(
      `SELECT i.*, l.titulo AS tituloLectura
       FROM INTENTO i JOIN LECTURA l USING(idLectura)
       ${where.length ? 'WHERE ' + where.join(' AND ') : ''}
       ORDER BY i.fechaInicio DESC, i.idIntento DESC`,
      args,
    );
    const datos = [];
    for (const row of rows) datos.push(await attemptDto(db, row, false));
    res.json({ datos, total: datos.length });
  });

  app.get('/api/intentos/:id', async (req, res) => {
    noBody(req);
    query(req.query, []);
    const row = await findAttempt(db, id(req.params.id));
    ownStudent(req, row.idEstudiante);
    res.json(await attemptDto(db, row));
  });

  app.post('/api/intentos/:id/finalizar', student, async (req, res) => {
    const attemptId = id(req.params.id);
    const selections = answersInput(req.body);
    const result = await transaction(db, async (tx) => {
      // El bloqueo del intento hace que un segundo envío espere al primer resultado.
      const attempt = await findAttempt(tx, attemptId, true);
      ownStudent(req, attempt.idEstudiante);
      const active = await questions(tx, attempt.idLectura, 'ACTIVA');
      const byId = new Map(active.map((question) => [question.idPregunta, question]));
      const evaluated = [];
      for (const selection of selections) {
        const question = byId.get(selection.idPregunta);
        if (!question) {
          throw new ApiError(400, 'PREGUNTA_AJENA', 'Una pregunta no pertenece a esta actividad.', [
            { campo: 'respuestas', mensaje: `Pregunta ${selection.idPregunta} no válida.` },
          ]);
        }
        const choice = (await options(tx, question.idPregunta)).find(
          (option) => option.idOpcion === selection.idOpcion,
        );
        if (!choice) {
          throw new ApiError(400, 'OPCION_AJENA', 'Una opción no pertenece a su pregunta.', [
            {
              campo: 'respuestas',
              mensaje: `Opción no válida para pregunta ${question.idPregunta}.`,
            },
          ]);
        }
        evaluated.push({ ...selection, esCorrecta: choice.esCorrecta });
      }
      if (attempt.estado === 'COMPLETADO') {
        const saved = await tx.all('SELECT idPregunta,idOpcion FROM RESPUESTA WHERE idIntento=?', [
          attemptId,
        ]);
        if (
          saved.length === selections.length &&
          saved.every((answer) =>
            selections.some(
              (selection) =>
                selection.idPregunta === answer.idPregunta &&
                selection.idOpcion === answer.idOpcion,
            ),
          )
        )
          return attemptDto(tx, attempt);
        throw new ApiError(
          409,
          'INTENTO_YA_COMPLETADO',
          'El intento ya tiene un resultado guardado y no puede modificarse.',
        );
      }
      const selectedIds = new Set(selections.map((answer) => answer.idPregunta));
      const missingQuestions = active.filter((question) => !selectedIds.has(question.idPregunta));
      if (missingQuestions.length || selections.length !== attempt.totalPreguntas) {
        throw new ApiError(
          400,
          'RESPUESTAS_INCOMPLETAS',
          'Responde todas las preguntas antes de finalizar.',
          missingQuestions.map((question) => ({
            campo: 'respuestas',
            mensaje: `Falta responder la pregunta ${question.idPregunta}.`,
          })),
        );
      }
      const correct = evaluated.filter((answer) => answer.esCorrecta === 1).length;
      const value = score(correct, attempt.totalPreguntas);
      for (const answer of evaluated) {
        await tx.run(
          'INSERT INTO RESPUESTA(idIntento,idPregunta,idOpcion,esCorrecta) VALUES(?,?,?,?)',
          [attemptId, answer.idPregunta, answer.idOpcion, answer.esCorrecta],
        );
      }
      await tx.run(
        `UPDATE INTENTO
         SET fechaFinalizacion=?,cantidadCorrectas=?,puntaje=?,estado='COMPLETADO'
         WHERE idIntento=?`,
        [new Date(), correct, value, attemptId],
      );
      return attemptDto(tx, await findAttempt(tx, attemptId));
    });
    res.json(result);
  });
}
