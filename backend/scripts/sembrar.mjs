import { openDatabase, transaction } from '../src/db.mjs';
import { loadConfig } from '../src/config.mjs';
import { hashPassword } from '../src/auth.mjs';
import { normalize } from '../src/validation.mjs';
import { demoReadings, demoQuestion } from '../database/demo.mjs';

const config=loadConfig(), db=openDatabase(config.dbPath);
try {
  transaction(db, () => {
    for (const profile of [{ alias:'Lector03', grado:3, codigo:'728461' },{ alias:'Lector04', grado:4, codigo:'864209' }]) {
      if (!db.prepare('SELECT 1 FROM ESTUDIANTE WHERE aliasNormalizado=?').get(normalize(profile.alias))) {
        db.prepare('INSERT INTO ESTUDIANTE(alias,aliasNormalizado,grado,codigoAccesoHash) VALUES(?,?,?,?)')
          .run(profile.alias,normalize(profile.alias),profile.grado,hashPassword(profile.codigo));
      }
    }
    for (const reading of demoReadings) {
      if (db.prepare('SELECT 1 FROM LECTURA WHERE titulo=? AND grado=?').get(reading.titulo,reading.grado)) continue;
      const rid=Number(db.prepare('INSERT INTO LECTURA(titulo,contenido,grado,dificultad,categoria) VALUES(?,?,?,?,?)')
        .run(reading.titulo,reading.contenido,reading.grado,reading.dificultad,reading.categoria).lastInsertRowid);
      for (const [index,item] of reading.preguntas.entries()) {
        const q=demoQuestion(item,index+1);
        const qid=Number(db.prepare('INSERT INTO PREGUNTA(idLectura,enunciado,tipo,orden) VALUES(?,?,?,?)').run(rid,q.enunciado,q.tipo,q.orden).lastInsertRowid);
        for(const o of q.opciones) db.prepare('INSERT INTO OPCION(idPregunta,texto,orden,esCorrecta) VALUES(?,?,?,?)').run(qid,o.texto,o.orden,Number(o.esCorrecta));
      }
      db.prepare("UPDATE LECTURA SET estado='ACTIVA' WHERE idLectura=?").run(rid);
    }
  });
  console.log('Datos ficticios preparados sin borrar registros anteriores.');
  console.log('Estudiantes: Lector03 / 728461 y Lector04 / 864209.');
  console.log('Cuatro lecturas activas con cuatro preguntas cada una.');
} finally { db.close(); }
