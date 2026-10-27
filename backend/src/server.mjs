import { loadConfig } from './config.mjs';
import { openDatabase } from './db.mjs';
import { createApp } from './app.mjs';

const config = loadConfig();
const db = openDatabase(config.dbPath);
const server = createApp(db, config).listen(config.port, config.host, () => {
  console.log(`Caña Dulce API: http://${config.host}:${config.port}/api`);
  console.log(`Base persistente: ${config.dbPath}`);
});
server.on('error', (error) => {
  console.error(`No se pudo iniciar el servidor: ${error.code}`);
  db.close();
  process.exitCode = 1;
});
for (const signal of ['SIGINT', 'SIGTERM']) {
  process.on(signal, () =>
    server.close(() => {
      db.close();
      process.exit(0);
    }),
  );
}
