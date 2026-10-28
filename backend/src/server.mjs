import { loadConfig } from './config.mjs';
import { openDatabase } from './db.mjs';
import { createApp } from './app.mjs';

const config = loadConfig();
const db = await openDatabase(config.database);
const server = createApp(db, config).listen(config.port, config.host, () => {
  console.log(`Caña Dulce API: http://${config.host}:${config.port}/api`);
  console.log(`MySQL: ${config.database.host}:${config.database.port}/${config.database.database}`);
});
server.on('error', async (error) => {
  console.error(`No se pudo iniciar el servidor: ${error.code}`);
  await db.close();
  process.exitCode = 1;
});
for (const signal of ['SIGINT', 'SIGTERM']) {
  process.on(signal, () =>
    server.close(async () => {
      await db.close();
      process.exit(0);
    }),
  );
}
