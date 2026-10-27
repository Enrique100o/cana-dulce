import { resolve } from 'node:path';

export function loadConfig(env = process.env) {
  const port = Number(env.PORT ?? 8080);
  if (!Number.isInteger(port) || port < 1 || port > 65535)
    throw new Error('PORT debe ser un puerto entre 1 y 65535.');
  if (!env.JWT_SECRET || env.JWT_SECRET.length < 32)
    throw new Error('Falta JWT_SECRET de al menos 32 caracteres. Ejecuta npm run setup.');
  if (!/^scrypt\$[a-f0-9]{32}\$[a-f0-9]{128}$/.test(env.DOCENTE_PASSWORD_HASH ?? ''))
    throw new Error('Falta el hash docente. Ejecuta npm run setup.');
  return {
    port,
    host: env.HOST || '127.0.0.1',
    dbPath: resolve(env.DB_PATH || './data/cana-dulce.sqlite'),
    teacherUser: env.DOCENTE_USUARIO || 'docente',
    teacherHash: env.DOCENTE_PASSWORD_HASH,
    jwtSecret: env.JWT_SECRET,
  };
}
