import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { once } from 'node:events';
import { Script, createContext } from 'node:vm';
import { createApp } from '../src/app.mjs';
import { openDatabase } from '../src/db.mjs';
import { loadConfig } from '../src/config.mjs';

// Ejecutor equivalente para esta colección. Usa sus solicitudes y scripts reales.
// No sustituye Postman: facilita repetir las mismas pruebas desde la terminal.
const collection=JSON.parse(readFileSync('postman/Cana_Dulce.postman_collection.json','utf8'));
const config=loadConfig();
const db=openDatabase('./data/pruebas-coleccion.sqlite');
const server=createApp(db,config).listen(0,'127.0.0.1');
await once(server,'listening');
const variables=new Map(collection.variable.map(v=>[v.key,v.value]));
variables.set('baseUrl',`http://127.0.0.1:${server.address().port}`);
variables.set('usuarioDocente',config.teacherUser);
if(process.env.DOCENTE_TEST_PASSWORD) variables.set('contrasenaDocente',process.env.DOCENTE_TEST_PASSWORD);
const replace=value=>value.replace(/\{\{([^{}]+)\}\}/g,(_,key)=>{
  if(!variables.has(key)) throw new Error(`Variable sin preparar: ${key}`);
  return String(variables.get(key));
});
const redact=value=>{
  if(Array.isArray(value))return value.map(redact);
  if(value&&typeof value==='object')return Object.fromEntries(Object.entries(value).map(([key,item])=>[key,/token|contrasena|codigoAcceso/i.test(key)?'<OMITIDO>':redact(item)]));
  return value;
};
const report={fechaUTC:new Date().toISOString(),ejecutor:'Node.js ejecutando solicitudes y scripts de la colección Postman',solicitudes:[]};
let failed=false;
try {
  outer: for(const folder of collection.item) for(const item of folder.item) {
    const checks=[];
    const pm={collectionVariables:{get:key=>variables.get(key),set:(key,value)=>variables.set(key,String(value))},
      test:(name,fn)=>{try{fn();checks.push({nombre:name,correcta:true});}catch(error){checks.push({nombre:name,correcta:false,mensaje:error.message});}}};
    const context=createContext({pm});
    const execute=kind=>{for(const event of item.event??[])if(event.listen===kind)new Script(event.script.exec.join('\n')).runInContext(context,{timeout:1000});};
    try {
      execute('prerequest');
      const request=item.request;
      const response=await fetch(replace(request.url),{method:request.method,
        headers:Object.fromEntries(request.header.map(h=>[h.key,replace(h.value)])),
        ...(request.body?{body:replace(request.body.raw)}:{}),signal:AbortSignal.timeout(10000)});
      const raw=await response.text();
      pm.response={code:response.status,headers:{get:key=>response.headers.get(key)},text:()=>raw,json:()=>JSON.parse(raw)};
      execute('test');
      const correct=checks.length>0&&checks.every(c=>c.correcta);
      report.solicitudes.push({carpeta:folder.name,nombre:item.name,metodo:request.method,ruta:new URL(replace(request.url)).pathname,codigoHTTP:response.status,correcta:correct,comprobaciones:checks,respuesta:raw?redact(JSON.parse(raw)):null});
      console.log(`${correct?'OK':'ERROR'} ${response.status} ${item.name}`);
      if(!correct){failed=true;break outer;}
    } catch(error) {
      failed=true;report.solicitudes.push({nombre:item.name,correcta:false,mensaje:error.message,comprobaciones:checks});break outer;
    }
  }
  report.totalSolicitudes=report.solicitudes.length;
  report.totalComprobaciones=report.solicitudes.reduce((n,r)=>n+r.comprobaciones.length,0);
  report.correctas=report.solicitudes.filter(r=>r.correcta).length;
  report.fallos=report.solicitudes.filter(r=>!r.correcta).length;
  report.integridad=db.prepare('PRAGMA integrity_check').get().integrity_check;
  report.clavesForaneasSinErrores=db.prepare('PRAGMA foreign_key_check').all().length===0;
  report.baseDeEvidencia='data/pruebas-coleccion.sqlite';
  report.registros=Object.fromEntries(['ESTUDIANTE','LECTURA','PREGUNTA','OPCION','INTENTO','RESPUESTA'].map(table=>[table,db.prepare(`SELECT count(*) AS cantidad FROM ${table}`).get().cantidad]));
  mkdirSync('evidencia',{recursive:true});
  writeFileSync('evidencia/coleccion-resultados.json',JSON.stringify(report,null,2)+'\n');
  console.log(`Solicitudes: ${report.totalSolicitudes}; correctas: ${report.correctas}; fallos: ${report.fallos}; comprobaciones: ${report.totalComprobaciones}.`);
  if(failed) process.exitCode=1;
} finally { await new Promise(resolve=>server.close(resolve));db.close(); }
