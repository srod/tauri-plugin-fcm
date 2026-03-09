# tauri-plugin-fcm

[![Crates.io](https://img.shields.io/crates/v/tauri-plugin-fcm.svg)](https://crates.io/crates/tauri-plugin-fcm)
[![npm](https://img.shields.io/npm/v/tauri-plugin-fcm.svg)](https://www.npmjs.com/package/tauri-plugin-fcm)
[![License](https://img.shields.io/badge/license-MIT%2FApache--2.0-blue.svg)](#license)

Tauri 2 plugin for Firebase Cloud Messaging (FCM).

This plugin bridges the gap for iOS push notifications in Tauri applications. While other plugins often return raw APNs hex tokens, `tauri-plugin-fcm` automatically exchanges them for FCM registration tokens, providing a unified interface for cross-platform push notification management.

## Installation

1. Add the plugin to your `Cargo.toml`:

```toml
[dependencies]
tauri-plugin-fcm = "0.1.0"
```

2. Install the JavaScript guest bindings:

```bash
npm install tauri-plugin-fcm
# or
yarn add tauri-plugin-fcm
# or
pnpm add tauri-plugin-fcm
```

3. Register the plugin in `src-tauri/src/lib.rs`:

```rust
fn main() {
    tauri::Builder::default()
        .plugin(tauri_plugin_fcm::init())
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
```

4. Configure permissions in `src-tauri/capabilities/default.json`:

```json
{
  "permissions": [
    "fcm:default"
  ]
}
```

## Platform Setup

### iOS

1. Place your `GoogleService-Info.plist` in the root of your Xcode project and ensure it is added to the app bundle.
2. In your `Info.plist`, set `FirebaseAppDelegateProxyEnabled` to `NO`:

```xml
<key>FirebaseAppDelegateProxyEnabled</key>
<string>NO</string>
```

3. Add the `aps-environment` entitlement to your project.
4. Ensure Firebase dependencies are resolved via Swift Package Manager in Xcode.

### Android

1. Place your `google-services.json` in `src-tauri/gen/android/app`.
2. Apply the Google Services plugin in your `build.gradle.kts`:

```kotlin
plugins {
    id("com.google.gms.google-services") version "4.4.0" apply false
}
```

3. Ensure the necessary permissions are declared in `AndroidManifest.xml`.

## API Reference

### Commands

| Command | Description | Returns |
| --- | --- | --- |
| `getToken()` | Retrieves the current FCM registration token. | `Promise<FcmToken>` |
| `requestPermission(opts?)` | Requests notification permissions. | `Promise<PermissionStatus>` |
| `checkPermissions()` | Checks current notification permission status. | `Promise<PermissionStatus>` |
| `register()` | Registers the device for push notifications. | `Promise<void>` |
| `deleteToken()` | Deletes the current FCM registration token. | `Promise<void>` |

### Events

| Event | Description | Payload |
| --- | --- | --- |
| `onTokenRefresh` | Emitted when the FCM token is refreshed. | `TokenRefreshEvent` |
| `onPushError` | Emitted when a push notification error occurs. | `PushErrorEvent` |

### Types

```typescript
interface FcmToken {
  token: string;
}

interface PermissionStatus {
  status: "granted" | "denied" | "not_determined";
}

interface PermissionOptions {
  sound?: boolean;
  badge?: boolean;
  alert?: boolean;
}

interface TokenRefreshEvent {
  token: string;
}

interface PushErrorEvent {
  error: string;
}
```

## Usage

```typescript
import { 
  getToken, 
  onTokenRefresh, 
  onPushError, 
  requestPermission 
} from 'tauri-plugin-fcm';

// Request permissions
const { status } = await requestPermission({
  sound: true,
  badge: true,
  alert: true
});

if (status === 'granted') {
  // Get current token
  const { token } = await getToken();
  console.log('FCM Token:', token);
}

// Listen for token refreshes
const unlistenRefresh = await onTokenRefresh((event) => {
  console.log('New FCM Token:', event.token);
});

// Handle push errors (e.g. on simulator)
const unlistenError = await onPushError((event) => {
  console.error('Push Error:', event.error);
});

// Delete token on logout
async function logout() {
  await deleteToken();
  unlistenRefresh();
  unlistenError();
}
```

## Troubleshooting

- **Simulator**: iOS simulators do not support APNs transport. The plugin will emit a `push-error` event instead of crashing. Use a physical device for testing push notifications.
- **Missing Config Files**: Ensure `GoogleService-Info.plist` (iOS) and `google-services.json` (Android) are correctly placed and included in the build.
- **Duplicate Symbols**: If you encounter duplicate symbol errors on iOS, ensure you are not manually linking Firebase libraries that are already included via Swift Package Manager.
- **Permission Denied**: Verify that you have enabled the "Push Notifications" and "Background Modes" (Remote notifications) capabilities in Xcode.

## Contributing

Contributions are welcome! Please read our contributing guidelines before submitting a pull request.

## License

This plugin is licensed under either of:

- Apache License, Version 2.0 ([LICENSE-APACHE](LICENSE-APACHE) or http://www.apache.org/licenses/LICENSE-2.0)
- MIT license ([LICENSE-MIT](LICENSE-MIT) or http://opensource.org/licenses/MIT)

at your option.
