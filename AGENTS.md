# AGENTS.md

Mobile-only Tauri 2 plugin (iOS + Android). No desktop target.

## Checks

Run before committing:

```sh
cargo fmt --check
cargo clippy -- -D warnings
bun run lint
bun run format:check
bun run build
bun run typecheck
bun run test
```

## Structure

- `src/` — Rust plugin (gated with `#![cfg(mobile)]`, no desktop module)
- `guest-js/` — TypeScript bindings consumed via `@tauri-apps/api/core`
- `ios/Sources/` — Swift native side
- `android/src/` — Kotlin native side
- `permissions/` — Tauri permission TOML definitions
- `test/` — Node test runner tests against built `dist-js/` artifacts

## Conventions

- Permission API follows Tauri's canonical pattern: `checkPermissions`/`requestPermissions` returning `PermissionState` wrapped in `{ notification: ... }`
- `PermissionState` values: `granted`, `denied`, `prompt`, `prompt-with-rationale` (Android only)
- Formatting: `cargo fmt` for Rust, Biome for TS/JS/JSON
- `@tauri-apps/api` is a direct dependency, not a peer dependency
