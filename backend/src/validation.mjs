import { invalid } from './errors.mjs';

export const normalize = value => value.normalize('NFC').toLowerCase();
export const STATES = ['ACTIVA', 'INACTIVA'];
export const TYPES = ['LITERAL', 'INFERENCIAL', 'IDEA_PRINCIPAL', 'VOCABULARIO', 'SECUENCIA'];

export function object(value, required, optional = [], prefix = '') {
  if (!value || typeof value !== 'object' || Array.isArray(value)) invalid(prefix || 'cuerpo', 'Debe ser un objeto JSON.');
  for (const key of Object.keys(value)) {
    if (![...required, ...optional].includes(key)) invalid(prefix + key, 'Campo no permitido; los datos calculados los decide el servidor.');
  }
  for (const key of required) {
    if (!Object.hasOwn(value, key)) invalid(prefix + key, 'Campo obligatorio.');
  }
  return value;
}
export function text(value, field, min, max, trim = true) {
  if (typeof value !== 'string') invalid(field, 'Debe ser texto.');
  const result = trim ? value.trim().normalize('NFC') : value;
  const length = Array.from(result).length;
  if (length < min || length > max) invalid(field, `Debe tener entre ${min} y ${max} caracteres.`);
  return result;
}
export function integer(value, field, min = 1, max = Number.MAX_SAFE_INTEGER) {
  if (!Number.isSafeInteger(value) || value < min || value > max) invalid(field, `Debe ser un entero entre ${min} y ${max}.`);
  return value;
}
export function id(value, field = 'id') {
  if (typeof value !== 'string' || !/^[1-9]\d*$/.test(value)) invalid(field, 'Debe ser un entero positivo.');
  return integer(Number(value), field);
}
export function enumeration(value, field, allowed) {
  if (!allowed.includes(value)) invalid(field, `Valores permitidos: ${allowed.join(', ')}.`);
  return value;
}
export function grade(value) { return enumeration(integer(value, 'grado', 3, 4), 'grado', [3,4]); }
export function query(queryObject, allowed) {
  object(queryObject, [], allowed);
  for (const [key, value] of Object.entries(queryObject)) {
    if (typeof value !== 'string') invalid(key, 'Envía un solo valor para este filtro.');
  }
  return queryObject;
}
export function queryGrade(value) { return grade(id(value, 'grado')); }
export function noBody(req) {
  if (req.body !== undefined) invalid('cuerpo', 'Esta operación no recibe cuerpo.');
}
export function readingInput(body, editing = false) {
  object(body, ['titulo','contenido','grado','dificultad', ...(editing ? ['categoria','estado'] : [])], editing ? [] : ['categoria']);
  let categoria = body.categoria ?? null;
  if (categoria !== null) categoria = text(categoria, 'categoria', 0, 50) || null;
  return {
    titulo: text(body.titulo, 'titulo', 3, 120), contenido: text(body.contenido, 'contenido', 20, 8000),
    grado: grade(body.grado), dificultad: enumeration(body.dificultad, 'dificultad', ['BAJA','MEDIA','ALTA']),
    categoria, estado: editing ? enumeration(body.estado, 'estado', STATES) : 'INACTIVA'
  };
}
export function questionInput(body, editing = false) {
  object(body, ['enunciado','tipo','orden','opciones', ...(editing ? ['estado'] : [])]);
  if (!Array.isArray(body.opciones) || body.opciones.length < 2 || body.opciones.length > 4) invalid('opciones', 'Se requieren de 2 a 4 opciones.');
  const opciones = body.opciones.map((option, index) => {
    const prefix = `opciones[${index}].`;
    object(option, ['texto','orden','esCorrecta'], [], prefix);
    if (typeof option.esCorrecta !== 'boolean') invalid(prefix + 'esCorrecta', 'Debe ser booleano.');
    return { texto: text(option.texto, prefix + 'texto', 1, 250),
      orden: integer(option.orden, prefix + 'orden', 1, 4), esCorrecta: option.esCorrecta };
  });
  if (new Set(opciones.map(o => o.orden)).size !== opciones.length) invalid('opciones', 'El orden de las opciones no puede repetirse.');
  if (new Set(opciones.map(o => normalize(o.texto))).size !== opciones.length) invalid('opciones', 'El texto de las opciones no puede repetirse.');
  if (opciones.filter(o => o.esCorrecta).length !== 1) invalid('opciones', 'Debe existir exactamente una opción correcta.');
  return { enunciado: text(body.enunciado, 'enunciado', 5, 400), tipo: enumeration(body.tipo, 'tipo', TYPES),
    orden: integer(body.orden, 'orden', 1, 999), estado: editing ? enumeration(body.estado, 'estado', STATES) : 'ACTIVA', opciones };
}
