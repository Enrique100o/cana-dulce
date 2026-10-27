// Aritmética entera para redondeo de mitad hacia arriba, sin error binario.
export function score(correct, total) {
  const denominator = BigInt(total);
  const cents = (BigInt(correct) * 10000n + denominator / 2n) / denominator;
  return Number(cents) / 100;
}
export function stars(value) {
  if (value === null) return null;
  return value === 0 ? 0 : value < 50 ? 1 : value < 80 ? 2 : 3;
}
export function progress(db, student) {
  const completed = db
    .prepare("SELECT idLectura,puntaje FROM INTENTO WHERE idEstudiante=? AND estado='COMPLETADO'")
    .all(student.idEstudiante);
  const distinct = new Set(completed.map((i) => i.idLectura)).size;
  const cents = completed.reduce((sum, i) => sum + Math.round(i.puntaje * 100), 0);
  const count = completed.length;
  return {
    idEstudiante: student.idEstudiante,
    alias: student.alias,
    grado: student.grado,
    intentosCompletados: count,
    lecturasCompletadas: distinct,
    puntajePromedio: count
      ? Number((BigInt(cents) + BigInt(count) / 2n) / BigInt(count)) / 100
      : null,
    mejorPuntaje: count ? Math.max(...completed.map((i) => i.puntaje)) : null,
    puntosAcumulados: cents / 100,
    estrellasAcumuladas: completed.reduce((sum, i) => sum + stars(i.puntaje), 0),
    insignias: [
      { codigo: 'PRIMERA_AVENTURA', nombre: 'Primera aventura', obtenida: distinct >= 1 },
      {
        codigo: 'EXPLORADOR_HISTORIAS',
        nombre: 'Explorador de historias',
        obtenida: distinct >= 3,
      },
      {
        codigo: 'LECTURA_BRILLANTE',
        nombre: 'Lectura brillante',
        obtenida: completed.some((i) => i.puntaje === 100),
      },
    ],
  };
}
