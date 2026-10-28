import mysql from 'mysql2/promise';

function queries(connection) {
  return {
    async all(sql, parameters = []) {
      const [rows] = await connection.execute(sql, parameters);
      return rows;
    },
    async one(sql, parameters = []) {
      const [rows] = await connection.execute(sql, parameters);
      return rows[0];
    },
    async run(sql, parameters = []) {
      const [result] = await connection.execute(sql, parameters);
      return result;
    },
  };
}

export async function openDatabase(config) {
  const pool = mysql.createPool({
    ...config,
    charset: 'utf8mb4',
    timezone: 'Z',
    dateStrings: false,
    // Los puntajes están limitados a DECIMAL(5,2); el contrato JSON usa números.
    decimalNumbers: true,
    connectionLimit: 5,
    waitForConnections: true,
    queueLimit: 50,
    connectTimeout: 10000,
    multipleStatements: false,
  });
  try {
    await pool.execute('SELECT 1');
  } catch (error) {
    await pool.end();
    throw error;
  }
  return { ...queries(pool), pool, close: () => pool.end() };
}

export async function transaction(db, operation) {
  const connection = await db.pool.getConnection();
  try {
    // La transacción usa una sola conexión y ve los cambios ya confirmados.
    await connection.query('SET TRANSACTION ISOLATION LEVEL READ COMMITTED');
    await connection.beginTransaction();
    const result = await operation(queries(connection));
    await connection.commit();
    return result;
  } catch (error) {
    await connection.rollback();
    throw error;
  } finally {
    connection.release();
  }
}
