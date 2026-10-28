import { DatabaseSync } from 'node:sqlite';
import { resolve } from 'node:path';
import { existsSync } from 'node:fs';
import { openDatabase, transaction } from '../src/db.mjs';
import { loadDatabaseConfig } from '../src/config.mjs';

const sourcePath = process.argv[2];
if (!sourcePath || !existsSync(sourcePath)) {
  throw new Error('Indica un archivo SQLite existente: npm run db:import -- RUTA_DEL_RESPALDO');
}
const tables = [
  [
    'ESTUDIANTE',
    ['idEstudiante', 'alias', 'aliasNormalizado', 'grado', 'estado', 'codigoAccesoHash'],
  ],
  ['LECTURA', ['idLectura', 'titulo', 'contenido', 'grado', 'dificultad', 'categoria', 'estado']],
  ['PREGUNTA', ['idPregunta', 'idLectura', 'enunciado', 'tipo', 'orden', 'estado']],
  ['OPCION', ['idOpcion', 'idPregunta', 'texto', 'orden', 'esCorrecta']],
  [
    'INTENTO',
    [
      'idIntento',
      'idEstudiante',
      'idLectura',
      'fechaInicio',
      'fechaFinalizacion',
      'cantidadCorrectas',
      'totalPreguntas',
      'puntaje',
      'estado',
    ],
  ],
  ['RESPUESTA', ['idRespuesta', 'idIntento', 'idPregunta', 'idOpcion', 'esCorrecta']],
];
const source = new DatabaseSync(resolve(sourcePath), { readOnly: true });
let db;
try {
  source.exec('BEGIN');
  const integrity = source.prepare('PRAGMA integrity_check').get();
  if (
    integrity.integrity_check !== 'ok' ||
    source.prepare('PRAGMA foreign_key_check').all().length
  ) {
    throw new Error('La base de origen no supera la revisión de integridad.');
  }
  const records = new Map(
    tables.map(([table, columns]) => [
      table,
      source.prepare(`SELECT ${columns.join(',')} FROM ${table} ORDER BY ${columns[0]}`).all(),
    ]),
  );
  db = await openDatabase(loadDatabaseConfig());
  await transaction(db, async (tx) => {
    for (const [table] of tables) {
      if ((await tx.one(`SELECT COUNT(*) AS total FROM ${table}`)).total > 0) {
        throw new Error('La base MySQL debe estar vacía. No se sobrescribieron datos.');
      }
    }
    for (const [table, columns] of tables) {
      for (const record of records.get(table)) {
        const values = columns.map((column) => {
          if (table === 'INTENTO') {
            // Se importan las respuestas antes de cerrar el intento; los triggers siguen activos.
            if (column === 'estado') return 'EN_PROGRESO';
            if (['fechaFinalizacion', 'cantidadCorrectas', 'puntaje'].includes(column)) return null;
          }
          return column === 'fechaInicio' ? new Date(record[column]) : record[column];
        });
        await tx.run(
          `INSERT INTO ${table}(${columns.join(',')}) VALUES(${columns.map(() => '?').join(',')})`,
          values,
        );
      }
    }
    for (const record of records.get('INTENTO')) {
      if (record.estado !== 'COMPLETADO') continue;
      await tx.run(
        `UPDATE INTENTO
         SET fechaFinalizacion=?,cantidadCorrectas=?,puntaje=?,estado='COMPLETADO'
         WHERE idIntento=?`,
        [
          new Date(record.fechaFinalizacion),
          record.cantidadCorrectas,
          record.puntaje,
          record.idIntento,
        ],
      );
    }
    // Se comparan todos los campos, incluidos hashes, identificadores y fechas.
    for (const [table, columns] of tables) {
      const imported = await tx.all(
        `SELECT ${columns.join(',')} FROM ${table} ORDER BY ${columns[0]}`,
      );
      if (JSON.stringify(imported) !== JSON.stringify(records.get(table))) {
        throw new Error(`La comprobación de ${table} falló. Se deshizo la importación.`);
      }
    }
  });
  for (const [table] of tables)
    console.log(`${table}: ${records.get(table).length} registros verificados.`);
  console.log('Migración completada. El archivo SQLite original se conserva.');
} finally {
  if (db) await db.close();
  source.close();
}
