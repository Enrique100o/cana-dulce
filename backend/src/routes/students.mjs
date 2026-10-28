import { transaction } from '../db.mjs';
import { ApiError, missing } from '../errors.mjs';
import {
  object,
  text,
  grade,
  normalize,
  query,
  queryGrade,
  enumeration,
  noBody,
  id,
} from '../validation.mjs';
import { hashPassword, verifyPassword, session, teacher, ownStudent } from '../auth.mjs';
import { progress } from '../scoring.mjs';

export const publicStudent = (row) => ({
  idEstudiante: row.idEstudiante,
  alias: row.alias,
  grado: row.grado,
  estado: row.estado,
});

export function studentRoutes(app, db, config) {
  const dummyHash = hashPassword('clave-inexistente-solo-para-comparar');
  app.post('/api/estudiantes', async (req, res) => {
    object(req.body, ['alias', 'grado', 'codigoAcceso']);
    const alias = text(req.body.alias, 'alias', 3, 30);
    const studentGrade = grade(req.body.grado);
    const code = text(req.body.codigoAcceso, 'codigoAcceso', 6, 32, false);
    const normalized = normalize(alias);
    const hash = hashPassword(code);
    let result;
    try {
      result = await transaction(db, async (tx) => {
        if (await tx.one('SELECT 1 FROM ESTUDIANTE WHERE aliasNormalizado=?', [normalized])) {
          throw new ApiError(409, 'ALIAS_OCUPADO', 'Ese alias ya está en uso. Elige otro.');
        }
        const insert = await tx.run(
          'INSERT INTO ESTUDIANTE(alias,aliasNormalizado,grado,codigoAccesoHash) VALUES(?,?,?,?)',
          [alias, normalized, studentGrade, hash],
        );
        return publicStudent(
          await tx.one('SELECT * FROM ESTUDIANTE WHERE idEstudiante=?', [insert.insertId]),
        );
      });
    } catch (error) {
      // La clave única también resuelve dos registros simultáneos del mismo alias.
      if (error.code === 'ER_DUP_ENTRY') {
        throw new ApiError(409, 'ALIAS_OCUPADO', 'Ese alias ya está en uso. Elige otro.');
      }
      throw error;
    }
    res.status(201).json(result);
  });

  app.post('/api/sesiones/estudiante', async (req, res) => {
    object(req.body, ['alias', 'codigoAcceso']);
    const alias = text(req.body.alias, 'alias', 3, 30);
    const code = text(req.body.codigoAcceso, 'codigoAcceso', 6, 32, false);
    const row = await db.one('SELECT * FROM ESTUDIANTE WHERE aliasNormalizado=?', [
      normalize(alias),
    ]);
    const valid = verifyPassword(code, row?.codigoAccesoHash ?? dummyHash);
    if (!valid || !row || row.estado !== 'ACTIVO') {
      throw new ApiError(
        401,
        'CREDENCIALES_INVALIDAS',
        'El alias o el código de acceso no son válidos.',
      );
    }
    res.json(session(config, 'ESTUDIANTE', publicStudent(row)));
  });

  app.post('/api/sesiones/docente', (req, res) => {
    object(req.body, ['usuario', 'contrasena']);
    const user = text(req.body.usuario, 'usuario', 1, 50, false);
    const password = text(req.body.contrasena, 'contrasena', 1, 128, false);
    const valid = verifyPassword(password, config.teacherHash);
    if (!valid || user !== config.teacherUser) {
      throw new ApiError(
        401,
        'CREDENCIALES_INVALIDAS',
        'El usuario o la contraseña no son válidos.',
      );
    }
    res.json(session(config, 'DOCENTE'));
  });
}

export function protectedStudentRoutes(app, db) {
  app.get('/api/estudiantes', teacher, async (req, res) => {
    noBody(req);
    const filters = query(req.query, ['grado', 'q', 'estado']);
    const where = [];
    const args = [];
    if (filters.grado !== undefined) {
      where.push('grado=?');
      args.push(queryGrade(filters.grado));
    }
    if (filters.q !== undefined) {
      where.push('LOCATE(?,aliasNormalizado)>0');
      args.push(normalize(text(filters.q, 'q', 1, 100)));
    }
    if (filters.estado !== undefined) {
      where.push('estado=?');
      args.push(enumeration(filters.estado, 'estado', ['ACTIVO', 'INACTIVO']));
    }
    const datos = await db.all(
      `SELECT idEstudiante, alias, grado, estado
       FROM ESTUDIANTE
       ${where.length ? 'WHERE ' + where.join(' AND ') : ''}
       ORDER BY aliasNormalizado, idEstudiante`,
      args,
    );
    res.json({ datos, total: datos.length });
  });

  app.get('/api/estudiantes/:id/progreso', async (req, res) => {
    noBody(req);
    query(req.query, []);
    const studentId = id(req.params.id);
    ownStudent(req, studentId);
    const row = await db.one(
      'SELECT idEstudiante,alias,grado,estado FROM ESTUDIANTE WHERE idEstudiante=?',
      [studentId],
    );
    if (!row) missing('El estudiante');
    res.json(await progress(db, row));
  });
}
