import { DatabaseSync } from 'node:sqlite';
import { mkdirSync, readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';

export function openDatabase(filename) {
  if (filename !== ':memory:') mkdirSync(dirname(resolve(filename)), { recursive: true });
  const db = new DatabaseSync(filename, { timeout: 5000 });
  db.function('normalizar', { deterministic: true }, value => value.normalize('NFC').toLowerCase());
  db.exec('PRAGMA foreign_keys=ON; PRAGMA journal_mode=WAL;');
  db.exec(readFileSync(new URL('../database/schema.sql', import.meta.url), 'utf8'));
  return db;
}

// BEGIN IMMEDIATE toma el bloqueo antes de consultar las reglas de negocio.
// El callback es síncrono: no se debe introducir await dentro de una transacción.
export function transaction(db, operation) {
  db.exec('BEGIN IMMEDIATE');
  try {
    const result = operation();
    db.exec('COMMIT');
    return result;
  } catch (error) {
    db.exec('ROLLBACK');
    throw error;
  }
}
