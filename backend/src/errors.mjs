export class ApiError extends Error {
  constructor(status, codigo, mensaje, detalles = [], extra = {}) {
    super(mensaje);
    this.status = status;
    this.payload = { error: { codigo, mensaje, detalles, ...extra } };
  }
}
export function invalid(campo, mensaje) {
  throw new ApiError(400, 'ENTRADA_INVALIDA', 'Revisa los datos enviados.', [{ campo, mensaje }]);
}
export function missing(nombre = 'El recurso') {
  throw new ApiError(404, 'NO_ENCONTRADO', `${nombre} no está disponible.`);
}
export function forbidden() {
  throw new ApiError(403, 'SIN_PERMISO', 'No tienes permiso para realizar esta operación.');
}
export function errorHandler(error, req, res, next) {
  if (res.headersSent) return next(error);
  if (error instanceof ApiError) return res.status(error.status).json(error.payload);
  if (error.type === 'entity.parse.failed' || error.type === 'entity.too.large') {
    return res.status(400).json({
      error: {
        codigo: 'ENTRADA_INVALIDA',
        mensaje: 'El cuerpo debe ser JSON válido y no superar 128 KB.',
        detalles: [],
      },
    });
  }
  if (error.message?.includes('CONTENIDO_BLOQUEADO')) {
    return res.status(409).json({
      error: {
        codigo: 'CONTENIDO_BLOQUEADO',
        mensaje: 'Esta lectura tiene intentos. Crea otra lectura para cambiar su contenido.',
        detalles: [],
      },
    });
  }
  // La traza se queda en el servidor, nunca en la respuesta móvil.
  console.error('Error interno de API:', error.code ?? error.name);
  res.status(500).json({
    error: {
      codigo: 'ERROR_INTERNO',
      mensaje: 'No se pudo completar la operación. Inténtalo nuevamente.',
      detalles: [],
    },
  });
}
