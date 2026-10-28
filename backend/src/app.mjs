import express from 'express';
import { authenticate } from './auth.mjs';
import { ApiError, errorHandler } from './errors.mjs';
import { studentRoutes, protectedStudentRoutes } from './routes/students.mjs';
import { readingRoutes } from './routes/readings.mjs';
import { attemptRoutes } from './routes/attempts.mjs';

export function createApp(db, config) {
  const app = express();
  app.disable('x-powered-by');
  app.set('query parser', 'simple');
  app.use(express.json({ limit: '128kb' }));
  app.use((req, res, next) => {
    res.set('Cache-Control', 'no-store');
    res.set('X-Content-Type-Options', 'nosniff');
    if (['POST', 'PUT'].includes(req.method) && !req.is('application/json')) {
      throw new ApiError(
        400,
        'ENTRADA_INVALIDA',
        'Envía el cuerpo con Content-Type: application/json.',
      );
    }
    next();
  });
  // Comprobación operativa; no se cuenta entre las 19 operaciones del contrato.
  app.get('/health', async (req, res) => {
    await db.one('SELECT 1');
    res.json({ estado: 'OK', servicio: 'cana-dulce-api' });
  });
  studentRoutes(app, db, config);
  app.use('/api', authenticate(db, config));
  protectedStudentRoutes(app, db);
  readingRoutes(app, db);
  attemptRoutes(app, db);
  app.use((req, res) => {
    throw new ApiError(404, 'NO_ENCONTRADO', 'La ruta solicitada no existe.');
  });
  app.use(errorHandler);
  return app;
}
