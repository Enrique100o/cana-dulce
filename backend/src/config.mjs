import { readFileSync } from 'node:fs';

function portNumber(value, name) {
  const port = Number(value);
  if (!Number.isInteger(port) || port < 1 || port > 65535) {
    throw new Error(`${name} debe ser un puerto entre 1 y 65535.`);
  }
  return port;
}

export function loadDatabaseConfig(env = process.env) {
  const host = env.DB_HOST || '127.0.0.1';
  const database = env.DB_NAME || 'cana_dulce';
  const user = env.DB_USER || 'cana_dulce_app';
  if (!env.DB_PASSWORD) throw new Error('Falta DB_PASSWORD para conectar con MySQL.');
  if (!/^[a-zA-Z0-9_]+$/.test(database)) throw new Error('DB_NAME contiene caracteres no válidos.');
  const local = ['127.0.0.1', 'localhost', '::1'].includes(host);
  const tls = env.DB_SSL ?? (local ? 'false' : 'true');
  if (!['true', 'false'].includes(tls)) throw new Error('DB_SSL debe ser true o false.');
  let ca = env.DB_SSL_CA?.replaceAll('\\n', '\n');
  if (env.DB_SSL_CA_PATH) ca = readFileSync(env.DB_SSL_CA_PATH, 'utf8');
  return {
    host,
    port: portNumber(env.DB_PORT || 3306, 'DB_PORT'),
    database,
    user,
    password: env.DB_PASSWORD,
    ...(tls === 'true' ? { ssl: { rejectUnauthorized: true, ...(ca ? { ca } : {}) } } : {}),
  };
}

export function loadConfig(env = process.env) {
  if (!env.JWT_SECRET || env.JWT_SECRET.length < 32) {
    throw new Error('Falta JWT_SECRET de al menos 32 caracteres. Ejecuta npm run setup.');
  }
  if (!/^scrypt\$[a-f0-9]{32}\$[a-f0-9]{128}$/.test(env.DOCENTE_PASSWORD_HASH ?? '')) {
    throw new Error('Falta el hash docente. Ejecuta npm run setup.');
  }
  return {
    port: portNumber(env.PORT ?? 8080, 'PORT'),
    host: env.HOST || '127.0.0.1',
    database: loadDatabaseConfig(env),
    teacherUser: env.DOCENTE_USUARIO || 'docente',
    teacherHash: env.DOCENTE_PASSWORD_HASH,
    jwtSecret: env.JWT_SECRET,
  };
}
