import { writeFileSync, existsSync } from 'node:fs';
import { randomBytes } from 'node:crypto';
import { hashPassword } from '../src/auth.mjs';

if (existsSync('.env')) {
  console.log('.env ya existe; se conserva la configuración actual.');
} else {
  const password = process.env.DOCENTE_PASSWORD || 'Lectura2026!';
  if (password.length < 8 || password.length > 128) throw new Error('DOCENTE_PASSWORD debe tener entre 8 y 128 caracteres.');
  writeFileSync('.env', [
    'PORT=8080', 'HOST=127.0.0.1', 'DB_PATH=./data/cana-dulce.sqlite',
    'DOCENTE_USUARIO=docente', `DOCENTE_PASSWORD_HASH=${hashPassword(password)}`,
    `JWT_SECRET=${randomBytes(32).toString('hex')}`, ''
  ].join('\n'), { flag: 'wx', mode: 0o600 });
  console.log('Configuración local creada. Usuario docente: docente.');
  console.log(process.env.DOCENTE_PASSWORD ? 'Se usó la contraseña que proporcionaste en DOCENTE_PASSWORD.' : 'Contraseña de demostración local: Lectura2026!');
}
