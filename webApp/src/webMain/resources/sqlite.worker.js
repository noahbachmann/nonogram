importScripts("sqlite3.js");

let db = null;
const ready = sqlite3InitModule().then(async (sqlite3) => {
  const poolUtil = await sqlite3.installOpfsSAHPoolVfs({});
  db = new poolUtil.OpfsSAHPoolDb("/nonogram.db");
});

self.onmessage = (event) => {
  const data = event.data;
  ready
    .then(() => {
      switch (data && data.action) {
        case "exec": {
          if (!data.sql) throw new Error("exec: Missing query string");
          const rows = [];
          db.exec({ sql: data.sql, bind: data.params ?? [], rowMode: "array", resultRows: rows });
          return postMessage({ id: data.id, results: { values: rows } });
        }
        case "begin_transaction":
          db.exec("BEGIN TRANSACTION;");
          return postMessage({ id: data.id, results: { values: [] } });
        case "end_transaction":
          db.exec("END TRANSACTION;");
          return postMessage({ id: data.id, results: { values: [] } });
        case "rollback_transaction":
          db.exec("ROLLBACK TRANSACTION;");
          return postMessage({ id: data.id, results: { values: [] } });
        default:
          throw new Error("Unsupported action: " + (data && data.action));
      }
    })
    .catch((err) => {
      setTimeout(() => {
        throw err;
      });
    });
};
