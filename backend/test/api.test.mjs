import { test } from 'node:test';
import assert from 'node:assert/strict';
import { once } from 'node:events';
import { mkdtempSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, resolve, dirname, basename } from 'node:path';
import jwt from 'jsonwebtoken';
import { createApp } from '../src/app.mjs';
import { openDatabase } from '../src/db.mjs';
import { hashPassword } from '../src/auth.mjs';
import { score, stars } from '../src/scoring.mjs';

const config = { teacherUser: 'docente', teacherHash: hashPassword('clave-docente-test'), jwtSecret: 'clave-de-pruebas-aislada-de-32-caracteres' };
const readingBody = { titulo: 'El árbol y los vecinos', contenido: 'Ana invitó a los vecinos a cuidar un árbol. Entre todos lo regaron cada mañana y sus hojas volvieron a crecer.', grado: 3, dificultad: 'BAJA', categoria: 'Comunidad' };
const questionBody = order => ({ enunciado: `Pregunta de prueba número ${order}`, tipo: 'LITERAL', orden: order, opciones: [
  { texto: 'Opción correcta', orden: 1, esCorrecta: true }, { texto: 'Otra opción', orden: 2, esCorrecta: false },
  { texto: 'Tercera opción', orden: 3, esCorrecta: false }
] });

test('API REST del contrato y reglas de negocio mediante HTTP real', async t => {
  const db = openDatabase(':memory:');
  const server = createApp(db, config).listen(0, '127.0.0.1');
  await once(server, 'listening');
  const base = `http://127.0.0.1:${server.address().port}`;
  const call = async (method, path, body, token) => {
    const response = await fetch(base + path, { method,
      headers: { ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}), ...(token ? { Authorization: `Bearer ${token}` } : {}) },
      ...(body !== undefined ? { body: JSON.stringify(body) } : {}) });
    const raw = await response.text();
    return { status: response.status, body: raw ? JSON.parse(raw) : null, raw, headers: response.headers };
  };
  const expectError = (result, status, code) => {
    assert.equal(result.status, status); assert.equal(result.body.error.codigo, code);
    assert.ok(Array.isArray(result.body.error.detalles)); assert.equal(typeof result.body.error.mensaje, 'string');
  };
  let teacherToken, studentToken, secondToken, studentId, readingId, questionIds = [], attemptId;
  let choices, answers75, result75;
  try {
    await t.test('Salud del servidor y base', async () => { const r=await call('GET','/health'); assert.equal(r.status,200); assert.equal(r.body.estado,'OK'); });
    await t.test('Sesión docente válida', async () => { const r=await call('POST','/api/sesiones/docente',{usuario:'docente',contrasena:'clave-docente-test'}); assert.equal(r.status,200); teacherToken=r.body.token; assert.equal(r.body.rol,'DOCENTE'); assert.equal(r.body.expiraEnSegundos,28800); });
    await t.test('Credenciales incorrectas', async () => { expectError(await call('POST','/api/sesiones/docente',{usuario:'docente',contrasena:'incorrecta'}),401,'CREDENCIALES_INVALIDAS'); });
    await t.test('Registro anónimo sin exponer hash ni código', async () => {
      const r=await call('POST','/api/estudiantes',{alias:'ÁguilaTest',grado:3,codigoAcceso:'123456'});
      assert.equal(r.status,201); studentId=r.body.idEstudiante;
      assert.deepEqual(Object.keys(r.body).sort(),['alias','estado','grado','idEstudiante']);
      const stored=db.prepare('SELECT * FROM ESTUDIANTE WHERE idEstudiante=?').get(studentId);
      assert.notEqual(stored.codigoAccesoHash,'123456'); assert.match(stored.codigoAccesoHash,/^scrypt\$/);
    });
    await t.test('Alias único tras normalización Unicode y mayúsculas', async () => {
      expectError(await call('POST','/api/estudiantes',{alias:' A\u0301GUILATEST ',grado:3,codigoAcceso:'654321'}),409,'ALIAS_OCUPADO');
    });
    await t.test('Registro rechaza grado no permitido y campos calculados', async () => {
      expectError(await call('POST','/api/estudiantes',{alias:'OtroAlias',grado:2,codigoAcceso:'123456'}),400,'ENTRADA_INVALIDA');
      expectError(await call('POST','/api/estudiantes',{alias:'OtroAlias',grado:3,codigoAcceso:'123456',puntaje:100}),400,'ENTRADA_INVALIDA');
    });
    await t.test('Sesión estudiantil y segundo perfil', async () => {
      const r=await call('POST','/api/sesiones/estudiante',{alias:'águilaTEST',codigoAcceso:'123456'});
      assert.equal(r.status,200); studentToken=r.body.token;
      await call('POST','/api/estudiantes',{alias:'SegundoTest',grado:3,codigoAcceso:'654321'});
      const second=await call('POST','/api/sesiones/estudiante',{alias:'SegundoTest',codigoAcceso:'654321'}); secondToken=second.body.token;
    });
    await t.test('Sin token, token alterado y token vencido devuelven 401', async () => {
      expectError(await call('GET','/api/lecturas'),401,'SESION_INVALIDA');
      expectError(await call('GET','/api/lecturas',undefined,studentToken+'x'),401,'SESION_INVALIDA');
      const expired=jwt.sign({rol:'ESTUDIANTE'},config.jwtSecret,{algorithm:'HS256',subject:String(studentId),issuer:'cana-dulce-api',audience:'cana-dulce-android',expiresIn:-1});
      expectError(await call('GET','/api/lecturas',undefined,expired),401,'SESION_INVALIDA');
    });
    await t.test('Un estudiante no administra ni consulta perfiles ajenos', async () => {
      expectError(await call('POST','/api/lecturas',readingBody,studentToken),403,'SIN_PERMISO');
      expectError(await call('GET','/api/estudiantes',undefined,studentToken),403,'SIN_PERMISO');
      expectError(await call('GET',`/api/estudiantes/${studentId+1}/progreso`,undefined,studentToken),403,'SIN_PERMISO');
    });
    await t.test('Progreso vacío distingue null de cero y devuelve insignias', async () => {
      const r=await call('GET',`/api/estudiantes/${studentId}/progreso`,undefined,studentToken);
      assert.equal(r.status,200); assert.equal(r.body.puntajePromedio,null); assert.equal(r.body.mejorPuntaje,null);
      assert.equal(r.body.puntosAcumulados,0); assert.equal(r.body.insignias.length,3); assert.ok(r.body.insignias.every(i=>!i.obtenida));
    });
    await t.test('CRUD crear lectura inactiva con Location', async () => {
      const r=await call('POST','/api/lecturas',readingBody,teacherToken); assert.equal(r.status,201); readingId=r.body.idLectura;
      assert.equal(r.body.estado,'INACTIVA'); assert.equal(r.body.totalPreguntasActivas,0); assert.equal(r.headers.get('location'),`/api/lecturas/${readingId}`);
    });
    await t.test('Un borrador no se muestra al estudiante y no se activa vacío', async () => {
      assert.equal((await call('GET','/api/lecturas',undefined,studentToken)).body.total,0);
      expectError(await call('GET',`/api/lecturas/${readingId}`,undefined,studentToken),404,'NO_ENCONTRADO');
      expectError(await call('PUT',`/api/lecturas/${readingId}`,{...readingBody,estado:'ACTIVA'},teacherToken),409,'LECTURA_NO_PUBLICABLE');
    });
    await t.test('Opciones inválidas no dejan preguntas parciales', async () => {
      const body=questionBody(1); body.opciones[1].esCorrecta=true;
      expectError(await call('POST',`/api/lecturas/${readingId}/preguntas`,body,teacherToken),400,'ENTRADA_INVALIDA');
      assert.equal(db.prepare('SELECT count(*) AS n FROM PREGUNTA').get().n,0);
    });
    await t.test('Crear cuatro preguntas relacionadas y opciones', async () => {
      for(let n=1;n<=4;n++){ const r=await call('POST',`/api/lecturas/${readingId}/preguntas`,questionBody(n),teacherToken); assert.equal(r.status,201); questionIds.push(r.body.idPregunta); assert.equal(r.body.opciones.filter(o=>o.esCorrecta).length,1); }
    });
    await t.test('Orden ocupado devuelve 409', async () => { expectError(await call('POST',`/api/lecturas/${readingId}/preguntas`,questionBody(1),teacherToken),409,'ORDEN_OCUPADO'); });
    await t.test('Editar pregunta sustituye opciones antes de intentos', async () => {
      const r=await call('PUT',`/api/preguntas/${questionIds[0]}`,{...questionBody(1),enunciado:'Pregunta corregida antes de publicar',estado:'ACTIVA'},teacherToken);
      assert.equal(r.status,200); assert.equal(r.body.enunciado,'Pregunta corregida antes de publicar'); assert.equal(r.body.opciones.length,3);
    });
    await t.test('DELETE pregunta es lógico, idempotente y conserva opciones', async () => {
      assert.equal((await call('DELETE',`/api/preguntas/${questionIds[3]}`,undefined,teacherToken)).status,204);
      assert.equal((await call('DELETE',`/api/preguntas/${questionIds[3]}`,undefined,teacherToken)).raw,'');
      assert.equal(db.prepare('SELECT count(*) n FROM OPCION WHERE idPregunta=?').get(questionIds[3]).n,3);
      assert.equal((await call('PUT',`/api/preguntas/${questionIds[3]}`,{...questionBody(4),estado:'ACTIVA'},teacherToken)).status,200);
    });
    await t.test('Activación de lectura y detalle completo', async () => {
      assert.equal((await call('PUT',`/api/lecturas/${readingId}`,{...readingBody,estado:'ACTIVA'},teacherToken)).status,200);
      const r=await call('GET',`/api/lecturas/${readingId}`,undefined,studentToken); assert.equal(r.status,200); assert.equal(r.body.contenido,readingBody.contenido); assert.equal(r.body.totalPreguntasActivas,4);
    });
    await t.test('Consulta parametrizada y búsqueda Unicode sin mayúsculas', async () => {
      const r=await call('GET','/api/lecturas?grado=3&q=%C3%81RBOL',undefined,studentToken); assert.equal(r.body.total,1); assert.equal(r.body.datos[0].idLectura,readingId); assert.ok(!('contenido' in r.body.datos[0]));
      assert.equal((await call('GET','/api/lecturas?q=no-coincide',undefined,studentToken)).body.total,0);
      assert.equal((await call('GET','/api/estudiantes?grado=3&q=%C3%81GUILA',undefined,teacherToken)).body.total,1);
    });
    await t.test('Filtros repetidos, desconocidos e inválidos devuelven 400', async () => {
      for(const path of ['/api/lecturas?grado=3&grado=4','/api/lecturas?grado=abc','/api/lecturas?q=','/api/lecturas?otro=1']) expectError(await call('GET',path,undefined,teacherToken),400,'ENTRADA_INVALIDA');
      expectError(await call('GET','/api/lecturas?grado=4',undefined,studentToken),403,'SIN_PERMISO');
    });
    await t.test('Preguntas seguras no revelan ninguna clave aun a docente', async () => {
      for(const token of [studentToken,teacherToken]) {
        const r=await call('GET',`/api/lecturas/${readingId}/preguntas`,undefined,token);
        assert.equal(r.body.total,4); for(const q of r.body.datos) for(const o of q.opciones) assert.deepEqual(Object.keys(o).sort(),['idOpcion','orden','texto']);
      }
      expectError(await call('GET',`/api/docente/lecturas/${readingId}/preguntas`,undefined,studentToken),403,'SIN_PERMISO');
      const admin=await call('GET',`/api/docente/lecturas/${readingId}/preguntas`,undefined,teacherToken); choices=admin.body.datos;
      answers75=choices.map((q,n)=>({idPregunta:q.idPregunta,idOpcion:q.opciones.find(o=>o.esCorrecta === (n!==3)).idOpcion}));
    });
    await t.test('Inicio simultáneo crea un solo intento y conflicto recuperable', async () => {
      const results=await Promise.all([call('POST','/api/intentos',{idLectura:readingId},studentToken),call('POST','/api/intentos',{idLectura:readingId},studentToken)]);
      assert.deepEqual(results.map(r=>r.status).sort(),[201,409]); const created=results.find(r=>r.status===201); attemptId=created.body.idIntento;
      assert.equal(created.body.puntaje,null); assert.equal(created.body.estrellas,null); assert.deepEqual(created.body.respuestas,[]);
      assert.equal(results.find(r=>r.status===409).body.error.idIntentoExistente,attemptId);
    });
    await t.test('El servidor rechaza autoría y puntaje enviados por Android', async () => {
      expectError(await call('POST','/api/intentos',{idLectura:readingId,idEstudiante:studentId},studentToken),400,'ENTRADA_INVALIDA');
      expectError(await call('POST',`/api/intentos/${attemptId}/finalizar`,{respuestas:answers75,puntaje:100},studentToken),400,'ENTRADA_INVALIDA');
    });
    await t.test('Contenido con intentos queda congelado', async () => {
      expectError(await call('PUT',`/api/lecturas/${readingId}`,{...readingBody,titulo:'Cambio indebido',estado:'ACTIVA'},teacherToken),409,'CONTENIDO_BLOQUEADO');
      expectError(await call('PUT',`/api/preguntas/${questionIds[0]}`,{...questionBody(1),estado:'ACTIVA'},teacherToken),409,'CONTENIDO_BLOQUEADO');
      expectError(await call('POST',`/api/lecturas/${readingId}/preguntas`,questionBody(5),teacherToken),409,'CONTENIDO_BLOQUEADO');
      expectError(await call('DELETE',`/api/preguntas/${questionIds[0]}`,undefined,teacherToken),409,'CONTENIDO_BLOQUEADO');
    });
    await t.test('Respuestas incompletas conservan intento pendiente y cero filas', async () => {
      expectError(await call('POST',`/api/intentos/${attemptId}/finalizar`,{respuestas:answers75.slice(0,3)},studentToken),400,'RESPUESTAS_INCOMPLETAS');
      assert.equal(db.prepare('SELECT count(*) n FROM RESPUESTA').get().n,0); assert.equal(db.prepare('SELECT estado FROM INTENTO WHERE idIntento=?').get(attemptId).estado,'EN_PROGRESO');
    });
    await t.test('Preguntas duplicadas, ajenas y opciones de otra pregunta', async () => {
      expectError(await call('POST',`/api/intentos/${attemptId}/finalizar`,{respuestas:[answers75[0],answers75[0]]},studentToken),400,'RESPUESTA_DUPLICADA');
      expectError(await call('POST',`/api/intentos/${attemptId}/finalizar`,{respuestas:[{idPregunta:999999,idOpcion:1}]},studentToken),400,'PREGUNTA_AJENA');
      expectError(await call('POST',`/api/intentos/${attemptId}/finalizar`,{respuestas:[{idPregunta:questionIds[0],idOpcion:answers75[1].idOpcion}]},studentToken),400,'OPCION_AJENA');
    });
    await t.test('Otro estudiante no ve ni finaliza el intento', async () => {
      expectError(await call('GET',`/api/intentos/${attemptId}`,undefined,secondToken),403,'SIN_PERMISO');
      expectError(await call('POST',`/api/intentos/${attemptId}/finalizar`,{respuestas:answers75},secondToken),403,'SIN_PERMISO');
      expectError(await call('GET',`/api/intentos?estudianteId=${studentId}`,undefined,secondToken),403,'SIN_PERMISO');
    });
    await t.test('Una falla al insertar respuestas revierte toda la transacción', async () => {
      db.exec(`CREATE TRIGGER falla_prueba BEFORE INSERT ON RESPUESTA WHEN NEW.idPregunta=${questionIds[2]} BEGIN SELECT RAISE(ABORT,'FALLO_SIMULADO'); END;`);
      expectError(await call('POST',`/api/intentos/${attemptId}/finalizar`,{respuestas:answers75},studentToken),500,'ERROR_INTERNO');
      assert.equal(db.prepare('SELECT count(*) n FROM RESPUESTA').get().n,0); assert.equal(db.prepare('SELECT estado FROM INTENTO WHERE idIntento=?').get(attemptId).estado,'EN_PROGRESO');
      db.exec('DROP TRIGGER falla_prueba');
    });
    await t.test('DELETE lectura conserva historia y bloquea nuevos intentos', async () => {
      const r=await call('DELETE',`/api/lecturas/${readingId}`,undefined,teacherToken); assert.equal(r.status,204); assert.equal(r.raw,'');
      expectError(await call('POST','/api/intentos',{idLectura:readingId},secondToken),404,'NO_ENCONTRADO');
      assert.equal((await call('GET',`/api/lecturas/${readingId}`,undefined,studentToken)).status,200);
    });
    await t.test('Finalizar después de desactivación: 3/4 = 75 y dos estrellas', async () => {
      const r=await call('POST',`/api/intentos/${attemptId}/finalizar`,{respuestas:answers75},studentToken); assert.equal(r.status,200); result75=r.body;
      assert.equal(r.body.puntaje,75); assert.equal(r.body.estrellas,2); assert.equal(r.body.cantidadCorrectas,3); assert.equal(r.body.estado,'COMPLETADO'); assert.equal(r.body.respuestas.length,4);
      assert.equal(r.body.respuestas.filter(a=>a.esCorrecta).length,3); assert.match(r.body.fechaFinalizacion,/Z$/);
    });
    await t.test('Reenvío idéntico en otro orden no duplica respuestas ni premios', async () => {
      const r=await call('POST',`/api/intentos/${attemptId}/finalizar`,{respuestas:[...answers75].reverse()},studentToken); assert.equal(r.status,200); assert.deepEqual(r.body,result75);
      assert.equal(db.prepare('SELECT count(*) n FROM RESPUESTA').get().n,4);
    });
    await t.test('Reenvío diferente de completado devuelve 409', async () => {
      const changed=answers75.map(a=>({...a})); changed[0].idOpcion=choices[0].opciones.find(o=>!o.esCorrecta).idOpcion;
      expectError(await call('POST',`/api/intentos/${attemptId}/finalizar`,{respuestas:changed},studentToken),409,'INTENTO_YA_COMPLETADO');
    });
    await t.test('Progreso suma puntaje y estrellas del resultado persistido', async () => {
      const r=await call('GET',`/api/estudiantes/${studentId}/progreso`,undefined,studentToken);
      assert.equal(r.body.puntosAcumulados,75); assert.equal(r.body.estrellasAcumuladas,2); assert.equal(r.body.intentosCompletados,1); assert.equal(r.body.lecturasCompletadas,1); assert.equal(r.body.insignias[0].obtenida,true); assert.equal(r.body.insignias[1].obtenida,false); assert.equal(r.body.insignias[2].obtenida,false);
    });
    await t.test('Historial filtrado y detalle, sin claves de otras opciones', async () => {
      const r=await call('GET',`/api/intentos?estudianteId=${studentId}&idLectura=${readingId}&estado=COMPLETADO`,undefined,teacherToken);
      assert.equal(r.body.total,1); assert.ok(!('respuestas' in r.body.datos[0]));
      assert.deepEqual((await call('GET',`/api/intentos/${attemptId}`,undefined,teacherToken)).body,result75);
    });
    await t.test('Repetir acumula puntos y mantiene una misión distinta', async () => {
      assert.equal((await call('PUT',`/api/lecturas/${readingId}`,{...readingBody,estado:'ACTIVA'},teacherToken)).status,200);
      const r=await call('POST','/api/intentos',{idLectura:readingId},studentToken); assert.equal(r.status,201);
      const allCorrect=choices.map(q=>({idPregunta:q.idPregunta,idOpcion:q.opciones.find(o=>o.esCorrecta).idOpcion}));
      assert.equal((await call('POST',`/api/intentos/${r.body.idIntento}/finalizar`,{respuestas:allCorrect},studentToken)).body.puntaje,100);
      const p=(await call('GET',`/api/estudiantes/${studentId}/progreso`,undefined,studentToken)).body;
      assert.equal(p.puntosAcumulados,175); assert.equal(p.puntajePromedio,87.5); assert.equal(p.estrellasAcumuladas,5); assert.equal(p.lecturasCompletadas,1); assert.equal(p.intentosCompletados,2); assert.equal(p.insignias[2].obtenida,true);
    });
    await t.test('Explorador exige tres lecturas distintas, no repeticiones', async () => {
      for(let n=2;n<=3;n++){
        const r=await call('POST','/api/lecturas',{...readingBody,titulo:`Misión distinta ${n}`},teacherToken); const rid=r.body.idLectura;
        const q=await call('POST',`/api/lecturas/${rid}/preguntas`,questionBody(1),teacherToken);
        await call('PUT',`/api/lecturas/${rid}`,{...readingBody,titulo:`Misión distinta ${n}`,estado:'ACTIVA'},teacherToken);
        const a=await call('POST','/api/intentos',{idLectura:rid},studentToken);
        assert.equal((await call('POST',`/api/intentos/${a.body.idIntento}/finalizar`,{respuestas:[{idPregunta:q.body.idPregunta,idOpcion:q.body.opciones.find(o=>o.esCorrecta).idOpcion}]},studentToken)).status,200);
      }
      const p=(await call('GET',`/api/estudiantes/${studentId}/progreso`,undefined,studentToken)).body; assert.equal(p.lecturasCompletadas,3); assert.equal(p.intentosCompletados,4); assert.equal(p.insignias[1].obtenida,true);
    });
    await t.test('Lectura de otro grado no permite iniciar intento', async () => {
      const r=await call('POST','/api/lecturas',{...readingBody,grado:4},teacherToken); const rid=r.body.idLectura;
      await call('POST',`/api/lecturas/${rid}/preguntas`,questionBody(1),teacherToken);
      await call('PUT',`/api/lecturas/${rid}`,{...readingBody,grado:4,estado:'ACTIVA'},teacherToken);
      expectError(await call('POST','/api/intentos',{idLectura:rid},studentToken),403,'SIN_PERMISO');
    });
    await t.test('Última pregunta activa no puede desactivarse', async () => {
      const r=await call('POST','/api/lecturas',readingBody,teacherToken); const rid=r.body.idLectura;
      const q=await call('POST',`/api/lecturas/${rid}/preguntas`,questionBody(1),teacherToken);
      await call('PUT',`/api/lecturas/${rid}`,{...readingBody,estado:'ACTIVA'},teacherToken);
      expectError(await call('DELETE',`/api/preguntas/${q.body.idPregunta}`,undefined,teacherToken),409,'LECTURA_SIN_PREGUNTAS');
    });
    await t.test('Claves foráneas y congelación también se aplican en SQLite', async () => {
      assert.equal(db.prepare('PRAGMA foreign_keys').get().foreign_keys,1);
      assert.throws(()=>db.prepare("UPDATE LECTURA SET titulo='Cambio SQL indebido' WHERE idLectura=?").run(readingId),/CONTENIDO_BLOQUEADO/);
      assert.throws(()=>db.prepare('INSERT INTO PREGUNTA(idLectura,enunciado,tipo,orden) VALUES(?,?,?,?)').run(999999,'Pregunta ajena','LITERAL',1),/FOREIGN KEY/);
      assert.deepEqual(db.prepare('PRAGMA foreign_key_check').all(),[]);
    });
    await t.test('Perfil inactivo bloquea token existente y no inicia sesión', async () => {
      db.prepare("UPDATE ESTUDIANTE SET estado='INACTIVO' WHERE idEstudiante=?").run(studentId);
      expectError(await call('GET','/api/lecturas',undefined,studentToken),403,'SIN_PERMISO');
      expectError(await call('POST','/api/sesiones/estudiante',{alias:'ÁguilaTest',codigoAcceso:'123456'}),401,'CREDENCIALES_INVALIDAS');
    });
    await t.test('IDs inválidos, recursos inexistentes y JSON mal formado', async () => {
      expectError(await call('GET','/api/lecturas/abc',undefined,teacherToken),400,'ENTRADA_INVALIDA');
      expectError(await call('GET','/api/lecturas/999999',undefined,teacherToken),404,'NO_ENCONTRADO');
      const bad=await fetch(base+'/api/lecturas',{method:'POST',headers:{'Content-Type':'application/json',Authorization:`Bearer ${teacherToken}`},body:'{mal json'});
      assert.equal(bad.status,400); assert.equal((await bad.json()).error.codigo,'ENTRADA_INVALIDA');
    });
  } finally {
    await new Promise(resolve => server.close(resolve)); db.close();
  }
});

test('Puntaje decimal con mitad hacia arriba y límites de estrellas', () => {
  assert.equal(score(3,4),75); assert.equal(score(1,6),16.67); assert.equal(score(1,32),3.13); assert.equal(score(0,4),0); assert.equal(score(4,4),100);
  assert.deepEqual([null,0,0.01,49.99,50,79.99,80,100].map(stars),[null,0,1,1,2,2,3,3]);
});

test('La base conserva registros al cerrar y abrir una nueva conexión', () => {
  const directory=mkdtempSync(join(tmpdir(),'cana-dulce-test-'));
  const file=join(directory,'persistencia.sqlite'); let db;
  try {
    db=openDatabase(file); db.prepare('INSERT INTO LECTURA(titulo,contenido,grado,dificultad) VALUES(?,?,?,?)').run('Lectura persistente','Este texto se conserva después de reiniciar el backend.',3,'BAJA'); db.close();
    db=openDatabase(file); assert.equal(db.prepare('SELECT titulo FROM LECTURA').get().titulo,'Lectura persistente');
    assert.equal(db.prepare('PRAGMA integrity_check').get().integrity_check,'ok');
  } finally {
    db?.close();
    const checked=resolve(directory);
    assert.equal(dirname(checked),resolve(tmpdir()));
    assert.ok(basename(checked).startsWith('cana-dulce-test-'));
    rmSync(checked,{recursive:true,force:true});
  }
});
