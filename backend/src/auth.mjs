import { randomBytes, scryptSync, timingSafeEqual } from 'node:crypto';
import jwt from 'jsonwebtoken';
import { ApiError, forbidden } from './errors.mjs';

export function hashPassword(password) {
  const salt = randomBytes(16).toString('hex');
  const hash = scryptSync(password, salt, 64).toString('hex');
  return `scrypt$${salt}$${hash}`;
}
export function verifyPassword(password, stored) {
  if (!/^scrypt\$[a-f0-9]{32}\$[a-f0-9]{128}$/.test(stored)) return false;
  const [, salt, expected] = stored.split('$');
  const actual = scryptSync(password, salt, 64);
  return timingSafeEqual(actual, Buffer.from(expected, 'hex'));
}
export function session(config, role, student) {
  const token = jwt.sign({ rol: role }, config.jwtSecret, {
    algorithm: 'HS256',
    subject: student ? String(student.idEstudiante) : 'docente',
    issuer: 'cana-dulce-api',
    audience: 'cana-dulce-android',
    expiresIn: 28800,
  });
  return {
    token,
    tipoToken: 'Bearer',
    expiraEnSegundos: 28800,
    rol: role,
    ...(student ? { estudiante: student } : {}),
  };
}
export function authenticate(db, config) {
  return async (req, res, next) => {
    const match = /^Bearer ([A-Za-z0-9._-]+)$/i.exec(req.get('Authorization') ?? '');
    let payload;
    try {
      if (!match) throw new Error('Token ausente');
      payload = jwt.verify(match[1], config.jwtSecret, {
        algorithms: ['HS256'],
        issuer: 'cana-dulce-api',
        audience: 'cana-dulce-android',
      });
      if (!payload || !['ESTUDIANTE', 'DOCENTE'].includes(payload.rol))
        throw new Error('Rol inválido');
      if (payload.rol === 'DOCENTE' && payload.sub !== 'docente')
        throw new Error('Sujeto inválido');
      if (
        payload.rol === 'ESTUDIANTE' &&
        (!/^[1-9]\d*$/.test(payload.sub) || !Number.isSafeInteger(Number(payload.sub)))
      )
        throw new Error('Sujeto inválido');
    } catch {
      throw new ApiError(401, 'SESION_INVALIDA', 'Inicia sesión nuevamente para continuar.');
    }
    req.user = { rol: payload.rol };
    if (payload.rol === 'ESTUDIANTE') {
      const student = await db.one(
        'SELECT idEstudiante,alias,grado,estado FROM ESTUDIANTE WHERE idEstudiante=?',
        [Number(payload.sub)],
      );
      if (!student || student.estado !== 'ACTIVO') forbidden();
      req.user.estudiante = student;
    }
    next();
  };
}
export function teacher(req, res, next) {
  if (req.user.rol !== 'DOCENTE') forbidden();
  next();
}
export function student(req, res, next) {
  if (req.user.rol !== 'ESTUDIANTE') forbidden();
  next();
}
export function ownStudent(req, studentId) {
  if (req.user.rol === 'ESTUDIANTE' && req.user.estudiante.idEstudiante !== studentId) forbidden();
}
