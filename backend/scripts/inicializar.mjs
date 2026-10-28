import { readFileSync } from 'node:fs';
import { openDatabase } from '../src/db.mjs';
import { loadDatabaseConfig } from '../src/config.mjs';

const db = await openDatabase(loadDatabaseConfig());
try {
  const schema = readFileSync(new URL('../database/schema.sql', import.meta.url), 'utf8');
  for (const statement of schema
    .split(';')
    .map((sql) => sql.trim())
    .filter(Boolean)) {
    await db.pool.query(statement);
  }
  const triggers = readFileSync(new URL('../database/triggers.sql', import.meta.url), 'utf8')
    .replace(/^DELIMITER.*$/gm, '')
    .split('$$')
    .map((sql) => sql.trim())
    .filter(Boolean);
  for (const statement of triggers) {
    const name = /CREATE TRIGGER (\w+)/i.exec(statement)?.[1];
    if (!name) throw new Error('Definición de trigger no reconocida.');
    const exists = await db.one(
      'SELECT 1 FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA = DATABASE() AND TRIGGER_NAME = ?',
      [name],
    );
    if (!exists) await db.pool.query(statement);
  }
  console.log('Tablas, índices y reglas de MySQL preparados.');
} finally {
  await db.close();
}
