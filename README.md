# tauri-plugin-fcm

[![Crates.io](https://img.shields.io/crates/v/tauri-plugin-fcm.svg)](https://crates.io/crates/tauri-plugin-fcm)
[![npm](https://img.shields.io/npm/v/tauri-plugin-fcm.svg)](https://www.npmjs.com/package/tauri-plugin-fcm)
[![License](https://img.shields.io/badge/license-MIT%2FApache--2.0-blue.svg)](#license)

Firebase Cloud Messaging for Tauri 2 mobile apps.

The plugin returns FCM registration tokens on both iOS and Android. On iOS it handles the APNs to FCM exchange so your app uses one token type across both platforms.

| Platform | Supported |
| --- | --- |
| Android | Yes |
| iOS | Yes |
| macOS | No |
| Windows | No |
| Linux | No |

## Install

Add the Rust crate to `src-tauri/Cargo.toml`:

```toml
[dependencies]
tauri-plugin-fcm = "0.1.0"
```

Install the JavaScript guest bindings:

```sh
pnpm add tauri-plugin-fcm
# or
bun add tauri-plugin-fcm
# or
npm add tauri-plugin-fcm
# or
yarn add tauri-plugin-fcm
```

Register the plugin in your Tauri app:

```rust
fn main() {
    let builder = tauri::Builder::default();
    #[cfg(mobile)]
    let builder = builder.plugin(tauri_plugin_fcm::init());

    builder
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
```

Add the capability permission:

```json
{
  "permissions": [
    "fcm:default"
  ]
}
```

## API

```ts
import {
  checkPermissions,
  deleteToken,
  getToken,
  onPushError,
  onTokenRefresh,
  register,
  requestPermissions,
} from "tauri-plugin-fcm";
```

### Functions

- `checkPermissions(): Promise<PermissionState>`
- `requestPermissions(): Promise<PermissionState>`
- `register(): Promise<void>`
- `getToken(): Promise<{ token: string }>`
- `deleteToken(): Promise<void>`
- `onTokenRefresh(handler): Promise<PluginListener>`
- `onPushError(handler): Promise<PluginListener>`

`PermissionState` comes from `@tauri-apps/api/core` and can be:

- `granted`
- `denied`
- `prompt`
- `prompt-with-rationale` (Android only)

## Usage

```ts
import {
  checkPermissions,
  getToken,
  onPushError,
  onTokenRefresh,
  register,
  requestPermissions,
} from "tauri-plugin-fcm";

let permission = await checkPermissions();

if (permission === "prompt" || permission === "prompt-with-rationale") {
  permission = await requestPermissions();
}

if (permission === "granted") {
  await register();
  const { token } = await getToken();
  console.log("FCM token:", token);
}

const tokenListener = await onTokenRefresh((event) => {
  console.log("New FCM token:", event.token);
});

const errorListener = await onPushError((event) => {
  console.error("Push error:", event.error);
});

// Later:
await tokenListener.unregister();
await errorListener.unregister();
```

## Platform setup

### iOS

1. Add `GoogleService-Info.plist` to your generated iOS project under `src-tauri/gen/apple/<app-name>_iOS/` and make sure it is included in the iOS app target.
2. Enable the `aps-environment` entitlement.
3. Enable Push Notifications and Background Modes with Remote notifications.
4. If you disable `FirebaseAppDelegateProxyEnabled`, make sure APNs callbacks still reach Firebase.

### Android

1. Add `google-services.json` to `src-tauri/gen/android/app`.
2. Apply the Google Services Gradle plugin in the Android app.
3. Keep `POST_NOTIFICATIONS` available for Android 13 and later.

## Notes

- This is a mobile-only plugin. If you share Tauri setup code across desktop and mobile targets, gate registration with `#[cfg(mobile)]`.
- On Android, `register()` is effectively a no-op because FCM registration is automatic.
- On iOS simulators, remote notification transport is unavailable. The plugin emits `push-error` instead of crashing.

## License

Licensed under either of:

- Apache License, Version 2.0 ([LICENSE-APACHE](LICENSE-APACHE))
- MIT License ([LICENSE-MIT](LICENSE-MIT))
