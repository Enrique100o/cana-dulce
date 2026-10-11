// Servidor aislado para las pruebas instrumentadas de Android; no modifica la base de demostración.
import { mkdirSync, writeFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { randomBytes } from 'node:crypto';
import { openDatabase, transaction } from '../src/db.mjs';
import { createApp } from '../src/app.mjs';
import { hashPassword } from '../src/auth.mjs';
import { normalize } from '../src/validation.mjs';
import { demoReadings, demoQuestion } from '../database/demo.mjs';

mkdirSync('data', { recursive: true });
mkdirSync('evidencia', { recursive: true });
const dbPath = resolve('data', 'android-e2e-' + Date.now() + '.sqlite');
const db = openDatabase(dbPath);
transaction(db, () => {
  for (const s of [{ alias:'Lector03', grado:3, codigo:'728461' }, { alias:'Lector04', grado:4, codigo:'864209' }]) {
    db.prepare('INSERT INTO ESTUDIANTE(alias,aliasNormalizado,grado,codigoAccesoHash) VALUES(?,?,?,?)')
      .run(s.alias,normalize(s.alias),s.grado,hashPassword(s.codigo));
  }
  for (const r of demoReadings) {
    const rid = Number(db.prepare('INSERT INTO LECTURA(titulo,contenido,grado,dificultad,categoria) VALUES(?,?,?,?,?)')
      .run(r.titulo,r.contenido,r.grado,r.dificultad,r.categoria).lastInsertRowid);
    r.preguntas.forEach((item,i) => {
      const q = demoQuestion(item,i+1);
      const qid = Number(db.prepare('INSERT INTO PREGUNTA(idLectura,enunciado,tipo,orden) VALUES(?,?,?,?)')
        .run(rid,q.enunciado,q.tipo,q.orden).lastInsertRowid);
      for (const o of q.opciones) db.prepare('INSERT INTO OPCION(idPregunta,texto,orden,esCorrecta) VALUES(?,?,?,?)')
        .run(qid,o.texto,o.orden,Number(o.esCorrecta));
    });
    db.prepare("UPDATE LECTURA SET estado='ACTIVA' WHERE idLectura=?").run(rid);
  }
});
const config = { teacherUser:'docente', teacherHash:hashPassword('Lectura2026!'), jwtSecret:randomBytes(32).toString('hex') };
const app = createApp(db, config);
const server = app.listen(8081, '127.0.0.1', () => {
  writeFileSync('evidencia/android-servidor.json', JSON.stringify({ fechaUTC:new Date().toISOString(), dbPath, puerto:8081, aislado:true },null,2)+'\n');
  console.log('Servidor de pruebas Android: http://127.0.0.1:8081/api');
});
for (const signal of ['SIGINT','SIGTERM']) process.on(signal, () => server.close(() => { db.close(); process.exit(0); }));
