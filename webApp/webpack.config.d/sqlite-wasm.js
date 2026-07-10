const CopyWebpackPlugin = require("copy-webpack-plugin");
const path = require("path");

config.plugins.push(
  new CopyWebpackPlugin({
    patterns: [
      {
        from: path.join(
          path.dirname(require.resolve("@sqlite.org/sqlite-wasm/package.json")),
          "sqlite-wasm/jswasm"
        ),
        to: ".",
        filter: (f) => /sqlite3\.(js|wasm)$/.test(f),
      },
    ],
  })
);
