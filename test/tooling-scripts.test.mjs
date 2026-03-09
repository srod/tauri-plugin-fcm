import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

async function readPackageJson() {
  const raw = await readFile(
    new URL("../package.json", import.meta.url),
    "utf8",
  );
  return JSON.parse(raw);
}

test("package.json exposes biome lint and format scripts", async () => {
  const pkg = await readPackageJson();

  assert.equal(pkg.scripts?.lint, "bunx biome check .");
  assert.equal(pkg.scripts?.["lint:fix"], "bunx biome check --write .");
  assert.equal(pkg.scripts?.format, "bunx biome format --write .");
  assert.equal(pkg.scripts?.["format:check"], "bunx biome format .");
});

test("biome config exists and ignores generated output", async () => {
  const raw = await readFile(new URL("../biome.json", import.meta.url), "utf8");
  const config = JSON.parse(raw);

  assert.equal(config.$schema, "https://biomejs.dev/schemas/2.4.6/schema.json");
  assert.ok(config.files?.includes?.includes("guest-js/**/*.ts"));
  assert.ok(config.files?.includes?.includes("examples/*/src/**/*.ts"));
  assert.ok(config.files?.includes?.includes("examples/*/src/**/*.html"));
  assert.ok(
    config.files?.includes?.includes(
      "examples/*/src-tauri/capabilities/**/*.json",
    ),
  );
  assert.ok(config.files?.includes?.includes("test/**/*.mjs"));
});

test("package.json pins the current Biome version", async () => {
  const pkg = await readPackageJson();

  assert.equal(pkg.devDependencies?.["@biomejs/biome"], "2.4.6");
});
