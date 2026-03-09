import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { createRequire } from "node:module";
import test from "node:test";

const require = createRequire(import.meta.url);

async function readPackageJson() {
  const raw = await readFile(
    new URL("../package.json", import.meta.url),
    "utf8",
  );
  return JSON.parse(raw);
}

test("package.json exposes import and require entry points", async () => {
  const pkg = await readPackageJson();

  assert.equal(pkg.type, "module");
  assert.equal(pkg.main, "./dist-js/index.cjs");
  assert.equal(pkg.module, "./dist-js/index.js");
  assert.deepEqual(pkg.exports, {
    ".": {
      types: "./dist-js/index.d.ts",
      import: "./dist-js/index.js",
      require: "./dist-js/index.cjs",
    },
  });
});

test("package depends on @tauri-apps/api directly", async () => {
  const pkg = await readPackageJson();

  assert.equal(typeof pkg.dependencies?.["@tauri-apps/api"], "string");
  assert.equal(pkg.peerDependencies?.["@tauri-apps/api"], undefined);
});

test("esm build exports canonical permission helpers", async () => {
  const mod = await import("../dist-js/index.js");

  assert.equal(typeof mod.requestPermissions, "function");
  assert.equal(typeof mod.checkPermissions, "function");
  assert.equal(typeof mod.requestPermission, "undefined");
  assert.equal(typeof mod.createChannel, "function");
  assert.equal(typeof mod.sendNotification, "function");
});

test("cjs build exports the same public API", () => {
  const mod = require("../dist-js/index.cjs");

  assert.equal(typeof mod.requestPermissions, "function");
  assert.equal(typeof mod.checkPermissions, "function");
  assert.equal(typeof mod.requestPermission, "undefined");
  assert.equal(typeof mod.createChannel, "function");
  assert.equal(typeof mod.sendNotification, "function");
});
