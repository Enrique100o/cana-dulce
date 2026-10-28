import { openDatabase, transaction } from '../src/db.mjs';
import { loadDatabaseConfig } from '../src/config.mjs';
import { hashPassword } from '../src/auth.mjs';
import { normalize } from '../src/validation.mjs';
import { demoReadings, demoQuestion } from '../database/demo.mjs';

const db = await openDatabase(loadDatabaseConfig());
try {
  await transaction(db, async (tx) => {
    for (const profile of [
      { alias: 'Lector03', grado: 3, codigo: '728461' },
      { alias: 'Lector04', grado: 4, codigo: '864209' },
    ]) {
      if (
        !(await tx.one('SELECT 1 FROM ESTUDIANTE WHERE aliasNormalizado=?', [
          normalize(profile.alias),
        ]))
      ) {
        await tx.run(
          'INSERT INTO ESTUDIANTE(alias,aliasNormalizado,grado,codigoAccesoHash) VALUES(?,?,?,?)',
          [profile.alias, normalize(profile.alias), profile.grado, hashPassword(profile.codigo)],
        );
      }
    }
    for (const reading of demoReadings) {
      if (
        await tx.one('SELECT 1 FROM LECTURA WHERE titulo=? AND grado=?', [
          reading.titulo,
          reading.grado,
        ])
      )
        continue;
      const insert = await tx.run(
        'INSERT INTO LECTURA(titulo,contenido,grado,dificultad,categoria) VALUES(?,?,?,?,?)',
        [reading.titulo, reading.contenido, reading.grado, reading.dificultad, reading.categoria],
      );
      for (const [index, item] of reading.preguntas.entries()) {
        const question = demoQuestion(item, index + 1);
        const insertedQuestion = await tx.run(
          'INSERT INTO PREGUNTA(idLectura,enunciado,tipo,orden) VALUES(?,?,?,?)',
          [insert.insertId, question.enunciado, question.tipo, question.orden],
        );
        for (const option of question.opciones) {
          await tx.run('INSERT INTO OPCION(idPregunta,texto,orden,esCorrecta) VALUES(?,?,?,?)', [
            insertedQuestion.insertId,
            option.texto,
            option.orden,
            Number(option.esCorrecta),
          ]);
        }
      }
      await tx.run("UPDATE LECTURA SET estado='ACTIVA' WHERE idLectura=?", [insert.insertId]);
    }
  });
  console.log('Datos ficticios preparados sin borrar registros anteriores.');
  console.log('Estudiantes: Lector03 / 728461 y Lector04 / 864209.');
} finally {
  await db.close();
}
