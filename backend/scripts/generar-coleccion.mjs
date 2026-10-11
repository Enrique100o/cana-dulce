import { mkdirSync, writeFileSync } from 'node:fs';
import { randomUUID } from 'node:crypto';
import { demoReadings, demoQuestion } from '../database/demo.mjs';

const fixture=demoReadings[0];
const baseReading=Object.fromEntries(Object.entries(fixture).filter(([key])=>key!=='preguntas'));
const collection={ info: {
  _postman_id: randomUUID(), name:'Caña Dulce · Etapa 3 · Pruebas del backend',
  description:'Ejecutar completa y en orden contra una base de demostración. Crea perfiles y lecturas ficticios sin borrar registros previos. Valida las 19 operaciones del contrato y sus errores. La primera solicitud genera variables nuevas para cada ejecución. Android se desarrollará en Kotlin en la etapa de integración.',
  schema:'https://schema.getpostman.com/json/collection/v2.1.0/collection.json'
}, variable: [
  {key:'baseUrl',value:'http://127.0.0.1:8080',type:'string'},
  {key:'usuarioDocente',value:'docente',type:'string'},
  {key:'contrasenaDocente',value:'Lectura2026!',type:'string'},
  ...['tokenDocente','tokenEstudiante','tokenOtro'].map(key=>({key,value:'',type:'string'}))
], item: [] };
let folder;
const section = name => { folder={name,item:[]}; collection.item.push(folder); };
function request(name,method,path,status,{role='docente',body,code,checks='',before=''}={}) {
  const header=[];
  if(role) header.push({key:'Authorization',value:`Bearer {{${{docente:'tokenDocente',estudiante:'tokenEstudiante',otro:'tokenOtro'}[role]}}}`});
  if(body!==undefined) header.push({key:'Content-Type',value:'application/json'});
  const request={method,header,url:'{{baseUrl}}'+path,auth:{type:'noauth'},
    description:`Resultado esperado: HTTP ${status}${code ? ', '+code : ''}. Datos exclusivos de prueba.`};
  if(body!==undefined) request.body={mode:'raw',raw:typeof body==='string'?body:JSON.stringify(body,null,2),options:{raw:{language:'json'}}};
  const event=[];
  if(before) event.push({listen:'prerequest',script:{type:'text/javascript',exec:before.split('\n')}});
  let tests=`function verificar(condicion,mensaje){if(!condicion)throw new Error(mensaje);}
pm.test('HTTP ${status}',function(){verificar(pm.response.code===${status},'Código recibido: '+pm.response.code);});
`;
  if(status===204) tests+=`pm.test('204 sin cuerpo',function(){verificar(pm.response.text()==='','El cuerpo debe estar vacío');});\n`;
  else tests+=`pm.test('Respuesta JSON',function(){verificar((pm.response.headers.get('Content-Type')||'').includes('application/json'),'Content-Type JSON requerido');pm.response.json();});\n`;
  if(code) tests+=`if(pm.response.code===${status}){pm.test('Error ${code}',function(){const e=pm.response.json().error;verificar(e.codigo==='${code}','Código de error');verificar(Array.isArray(e.detalles),'Detalles en arreglo');verificar(typeof e.mensaje==='string','Mensaje comprensible');});}\n`;
  if(checks) tests+=`if(pm.response.code===${status}){const data=pm.response.json();${checks}\n}`;
  event.push({listen:'test',script:{type:'text/javascript',exec:tests.split('\n')}});
  folder.item.push({name,request,event,response:[]});
}
const save = (key,property) => `pm.collectionVariables.set('${key}',String(data.${property}));`;
const test = (title,condition) => `pm.test(${JSON.stringify(title)},function(){verificar(${condition},${JSON.stringify(title)});});`;

section('01 Preparación e identificación');
request('01 Salud y variables nuevas','GET','/health',200,{role:null,before:`
const run=Date.now().toString()+Math.random().toString(36).slice(2,6);
pm.collectionVariables.set('runId',run);
pm.collectionVariables.set('aliasPrueba','Lector_'+run);
pm.collectionVariables.set('aliasOtro','Otro_'+run);
pm.collectionVariables.set('sesionDocente',JSON.stringify({usuario:pm.collectionVariables.get('usuarioDocente'),contrasena:pm.collectionVariables.get('contrasenaDocente')}));
const reading=${JSON.stringify(baseReading)};
reading.titulo+=' · prueba '+run;
pm.collectionVariables.set('lecturaNueva',JSON.stringify(reading));
reading.titulo+=' editada';
pm.collectionVariables.set('lecturaInactiva',JSON.stringify({...reading,estado:'INACTIVA'}));
pm.collectionVariables.set('lecturaActiva',JSON.stringify({...reading,estado:'ACTIVA'}));
pm.collectionVariables.set('lecturaCambiada',JSON.stringify({...reading,titulo:'Cambio prohibido',estado:'ACTIVA'}));
`});
request('02 Sesión docente','POST','/api/sesiones/docente',200,{role:null,body:'{{sesionDocente}}',checks:save('tokenDocente','token')});
request('03 Registrar estudiante de prueba','POST','/api/estudiantes',201,{role:null,body:{alias:'{{aliasPrueba}}',grado:3,codigoAcceso:'728461'},checks:save('idEstudiante','idEstudiante')+test('No expone código ni hash',"!('codigoAcceso' in data)&&!('codigoAccesoHash' in data)")});
request('04 Iniciar sesión estudiante','POST','/api/sesiones/estudiante',200,{role:null,body:{alias:'{{aliasPrueba}}',codigoAcceso:'728461'},checks:save('tokenEstudiante','token')});
request('05 Registrar otro estudiante','POST','/api/estudiantes',201,{role:null,body:{alias:'{{aliasOtro}}',grado:3,codigoAcceso:'864209'},checks:save('idOtro','idEstudiante')});
request('06 Iniciar sesión de otro estudiante','POST','/api/sesiones/estudiante',200,{role:null,body:{alias:'{{aliasOtro}}',codigoAcceso:'864209'},checks:save('tokenOtro','token')});
request('07 Alias ocupado','POST','/api/estudiantes',409,{role:null,body:{alias:'{{aliasPrueba}}',grado:3,codigoAcceso:'728461'},code:'ALIAS_OCUPADO'});
request('08 Credencial incorrecta','POST','/api/sesiones/estudiante',401,{role:null,body:{alias:'{{aliasPrueba}}',codigoAcceso:'000000'},code:'CREDENCIALES_INVALIDAS'});
request('09 Consultar estudiantes con filtro','GET','/api/estudiantes?grado=3&q={{aliasPrueba}}',200,{checks:test('Un perfil y sin hash',"data.total===1&&!('codigoAccesoHash' in data.datos[0])")});

section('02 CRUD de lecturas y preguntas');
request('10 Crear lectura inactiva','POST','/api/lecturas',201,{body:'{{lecturaNueva}}',checks:save('idLectura','idLectura')+test('Se crea inactiva y sin preguntas',"data.estado==='INACTIVA'&&data.totalPreguntasActivas===0")});
request('11 Consultar detalle como docente','GET','/api/lecturas/{{idLectura}}',200,{checks:test('Detalle contiene el texto',"typeof data.contenido==='string'")});
request('12 Editar lectura antes de intentos','PUT','/api/lecturas/{{idLectura}}',200,{body:'{{lecturaInactiva}}',checks:test('Edición aplicada',"data.titulo.endsWith('editada')")});
request('13 Activar sin preguntas se rechaza','PUT','/api/lecturas/{{idLectura}}',409,{body:'{{lecturaActiva}}',code:'LECTURA_NO_PUBLICABLE'});
const invalidQuestion=demoQuestion(fixture.preguntas[0],1); invalidQuestion.opciones[1].esCorrecta=true;
request('14 Dos opciones correctas se rechazan','POST','/api/lecturas/{{idLectura}}/preguntas',400,{body:invalidQuestion,code:'ENTRADA_INVALIDA'});
for(let n=1;n<=4;n++) request(`Crear pregunta ${n}`,'POST','/api/lecturas/{{idLectura}}/preguntas',201,{body:demoQuestion(fixture.preguntas[n-1],n),checks:save(`idPregunta${n}`,'idPregunta')});
request('Orden de pregunta ocupado','POST','/api/lecturas/{{idLectura}}/preguntas',409,{body:demoQuestion(fixture.preguntas[0],1),code:'ORDEN_OCUPADO'});
request('Editar pregunta y lista completa de opciones','PUT','/api/preguntas/{{idPregunta1}}',200,{body:{...demoQuestion(fixture.preguntas[0],1),estado:'ACTIVA'}});
request('Desactivar pregunta antes de usarla','DELETE','/api/preguntas/{{idPregunta4}}',204);
request('Consultar preguntas inactivas como docente','GET','/api/docente/lecturas/{{idLectura}}/preguntas?estado=INACTIVA',200,{checks:test('Una pregunta inactiva',"data.total===1&&data.datos[0].estado==='INACTIVA'")});
request('Reactivar cuarta pregunta','PUT','/api/preguntas/{{idPregunta4}}',200,{body:{...demoQuestion(fixture.preguntas[3],4),estado:'ACTIVA'}});
request('Activar lectura con preguntas completas','PUT','/api/lecturas/{{idLectura}}',200,{body:'{{lecturaActiva}}',checks:test('Publicación válida',"data.estado==='ACTIVA'&&data.totalPreguntasActivas===4")});
request('Listado con grado y búsqueda','GET','/api/lecturas?grado=3&q={{runId}}',200,{role:'estudiante',checks:test('Filtro devuelve una lectura',"data.total===1&&data.datos[0].idLectura===Number(pm.collectionVariables.get('idLectura'))")});
request('Detalle disponible para el estudiante','GET','/api/lecturas/{{idLectura}}',200,{role:'estudiante'});
request('Preguntas estudiantiles sin clave de corrección','GET','/api/lecturas/{{idLectura}}/preguntas',200,{role:'estudiante',checks:test('Cuatro preguntas sin esCorrecta',"data.total===4&&data.datos.every(q=>q.opciones.every(o=>!('esCorrecta' in o)&&!('idOpcionCorrecta' in o)))")});
request('La ruta estudiantil también oculta claves a docente','GET','/api/lecturas/{{idLectura}}/preguntas',200,{checks:test('Claves omitidas del JSON',"data.datos.every(q=>q.opciones.every(o=>!('esCorrecta' in o)))")});
request('Obtener claves por ruta docente y preparar selecciones','GET','/api/docente/lecturas/{{idLectura}}/preguntas',200,{checks:`
const all=data.datos.map(q=>({idPregunta:q.idPregunta,idOpcion:q.opciones.find(o=>o.esCorrecta).idOpcion}));
const three=all.map(a=>({...a}));three[3].idOpcion=data.datos[3].opciones.find(o=>!o.esCorrecta).idOpcion;
const changed=three.map(a=>({...a}));changed[0].idOpcion=data.datos[0].opciones.find(o=>!o.esCorrecta).idOpcion;
pm.collectionVariables.set('respuestas75',JSON.stringify({respuestas:three}));
pm.collectionVariables.set('respuestas100',JSON.stringify({respuestas:all}));
pm.collectionVariables.set('respuestasDistintas',JSON.stringify({respuestas:changed}));
pm.collectionVariables.set('respuestasIncompletas',JSON.stringify({respuestas:three.slice(0,3)}));
pm.collectionVariables.set('respuestasDuplicadas',JSON.stringify({respuestas:[three[0],three[0]]}));
pm.collectionVariables.set('respuestasOpcionAjena',JSON.stringify({respuestas:[{idPregunta:all[0].idPregunta,idOpcion:all[1].idOpcion}]}));
pm.collectionVariables.set('puntajeInyectado',JSON.stringify({respuestas:three,puntaje:100}));
`});

section('03 Intentos y regla de calificación');
request('Iniciar un intento','POST','/api/intentos',201,{role:'estudiante',body:'{"idLectura":{{idLectura}}}',checks:save('idIntento','idIntento')+test('Pendiente sin evaluación',"data.puntaje===null&&data.estrellas===null&&data.respuestas.length===0")});
request('Detectar intento pendiente existente','POST','/api/intentos',409,{role:'estudiante',body:'{"idLectura":{{idLectura}}}',code:'INTENTO_EN_PROGRESO_EXISTENTE',checks:test('ID permite retomar',"data.error.idIntentoExistente===Number(pm.collectionVariables.get('idIntento'))")});
request('Consultar intento pendiente','GET','/api/intentos/{{idIntento}}',200,{role:'estudiante',checks:test('Estado en progreso',"data.estado==='EN_PROGRESO'")});
request('Progreso inicial sin resultados','GET','/api/estudiantes/{{idEstudiante}}/progreso',200,{role:'estudiante',checks:test('Sin premio anticipado',"data.intentosCompletados===0&&data.puntajePromedio===null&&data.puntosAcumulados===0&&data.insignias.every(i=>!i.obtenida)")});
request('Respuestas incompletas','POST','/api/intentos/{{idIntento}}/finalizar',400,{role:'estudiante',body:'{{respuestasIncompletas}}',code:'RESPUESTAS_INCOMPLETAS'});
request('Pregunta repetida en el envío','POST','/api/intentos/{{idIntento}}/finalizar',400,{role:'estudiante',body:'{{respuestasDuplicadas}}',code:'RESPUESTA_DUPLICADA'});
request('Opción de otra pregunta','POST','/api/intentos/{{idIntento}}/finalizar',400,{role:'estudiante',body:'{{respuestasOpcionAjena}}',code:'OPCION_AJENA'});
request('Android no decide el puntaje','POST','/api/intentos/{{idIntento}}/finalizar',400,{role:'estudiante',body:'{{puntajeInyectado}}',code:'ENTRADA_INVALIDA'});
request('Una validación no completó el intento','GET','/api/intentos/{{idIntento}}',200,{role:'estudiante',checks:test('Sin resultado ni filas parciales',"data.estado==='EN_PROGRESO'&&data.puntaje===null&&data.respuestas.length===0")});
request('Bloqueo de edición tras crear intento','PUT','/api/lecturas/{{idLectura}}',409,{body:'{{lecturaCambiada}}',code:'CONTENIDO_BLOQUEADO'});
request('Bloqueo de pregunta utilizada','DELETE','/api/preguntas/{{idPregunta1}}',409,{code:'CONTENIDO_BLOQUEADO'});
request('Desactivar lectura conservando actividad en curso','DELETE','/api/lecturas/{{idLectura}}',204);
request('Finalizar aun después de desactivar: 75 puntos','POST','/api/intentos/{{idIntento}}/finalizar',200,{role:'estudiante',body:'{{respuestas75}}',checks:test('3 de 4, 75 puntos y dos estrellas',"data.cantidadCorrectas===3&&data.totalPreguntas===4&&data.puntaje===75&&data.estrellas===2&&data.estado==='COMPLETADO'&&data.respuestas.length===4")+"pm.collectionVariables.set('resultado75',JSON.stringify(data));"});
request('Reenviar sin duplicar resultado','POST','/api/intentos/{{idIntento}}/finalizar',200,{role:'estudiante',body:'{{respuestas75}}',checks:test('Resultado inmutable',"JSON.stringify(data)===pm.collectionVariables.get('resultado75')")});
request('Cambiar respuestas de completado se rechaza','POST','/api/intentos/{{idIntento}}/finalizar',409,{role:'estudiante',body:'{{respuestasDistintas}}',code:'INTENTO_YA_COMPLETADO'});

section('04 Historial y gamificación');
request('Historial del estudiante','GET','/api/intentos?estado=COMPLETADO',200,{role:'estudiante',checks:test('Un intento, sin detalle de respuestas',"data.total===1&&!('respuestas' in data.datos[0])")});
request('Historial filtrado por docente','GET','/api/intentos?estudianteId={{idEstudiante}}&idLectura={{idLectura}}',200,{checks:test('Historial filtrado',"data.total===1")});
request('Docente consulta resultado completo','GET','/api/intentos/{{idIntento}}',200,{checks:test('Resultado guardado',"data.puntaje===75&&data.respuestas.length===4")});
request('Primera aventura y premios acumulados','GET','/api/estudiantes/{{idEstudiante}}/progreso',200,{role:'estudiante',checks:test('75 puntos, dos estrellas y primera insignia',"data.puntosAcumulados===75&&data.estrellasAcumuladas===2&&data.lecturasCompletadas===1&&data.intentosCompletados===1&&data.insignias.find(i=>i.codigo==='PRIMERA_AVENTURA').obtenida")});
request('Reactivar sin cambiar contenido histórico','PUT','/api/lecturas/{{idLectura}}',200,{body:'{{lecturaActiva}}'});
request('Nuevo intento de práctica','POST','/api/intentos',201,{role:'estudiante',body:'{"idLectura":{{idLectura}}}',checks:save('idIntento2','idIntento')});
request('Lectura brillante: 100 puntos y tres estrellas','POST','/api/intentos/{{idIntento2}}/finalizar',200,{role:'estudiante',body:'{{respuestas100}}',checks:test('Resultado perfecto',"data.puntaje===100&&data.estrellas===3")});
request('Acumular práctica sin duplicar misiones','GET','/api/estudiantes/{{idEstudiante}}/progreso',200,{role:'estudiante',checks:test('175 puntos, cinco estrellas, dos intentos y una misión',"data.puntosAcumulados===175&&data.estrellasAcumuladas===5&&data.intentosCompletados===2&&data.lecturasCompletadas===1&&data.puntajePromedio===87.5&&data.insignias.find(i=>i.codigo==='LECTURA_BRILLANTE').obtenida&&!data.insignias.find(i=>i.codigo==='EXPLORADOR_HISTORIAS').obtenida")});

section('05 Permisos y errores HTTP');
request('Sin token','GET','/api/lecturas',401,{role:null,code:'SESION_INVALIDA'});
request('Estudiante no consulta cuentas','GET','/api/estudiantes',403,{role:'estudiante',code:'SIN_PERMISO'});
request('Estudiante no consulta claves docentes','GET','/api/docente/lecturas/{{idLectura}}/preguntas',403,{role:'estudiante',code:'SIN_PERMISO'});
request('Otro perfil no consulta resultado ajeno','GET','/api/intentos/{{idIntento}}',403,{role:'otro',code:'SIN_PERMISO'});
request('Otro perfil no consulta progreso ajeno','GET','/api/estudiantes/{{idEstudiante}}/progreso',403,{role:'otro',code:'SIN_PERMISO'});
request('Filtro de otro grado se rechaza','GET','/api/lecturas?grado=4',403,{role:'estudiante',code:'SIN_PERMISO'});
request('Grado mal formado','GET','/api/lecturas?grado=abc',400,{code:'ENTRADA_INVALIDA'});
request('ID inválido','GET','/api/lecturas/0',400,{code:'ENTRADA_INVALIDA'});
request('ID inexistente','GET','/api/lecturas/999999999',404,{code:'NO_ENCONTRADO'});
request('Consulta sin coincidencias es lista vacía','GET','/api/lecturas?q=NoExiste_{{runId}}',200,{checks:test('Lista vacía válida',"data.total===0&&Array.isArray(data.datos)&&data.datos.length===0")});
request('Desactivar al terminar la demostración','DELETE','/api/lecturas/{{idLectura}}',204);
request('Desactivación repetida es idempotente','DELETE','/api/lecturas/{{idLectura}}',204);
request('Resultado histórico sigue disponible','GET','/api/intentos/{{idIntento}}',200,{role:'estudiante',checks:test('Historia conservada',"data.puntaje===75&&data.respuestas.length===4")});

mkdirSync('postman',{recursive:true});
writeFileSync('postman/Cana_Dulce.postman_collection.json',JSON.stringify(collection,null,2)+'\n');
console.log(`${collection.item.reduce((n,f)=>n+f.item.length,0)} solicitudes de prueba generadas.`);
