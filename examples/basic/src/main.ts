// tauri-plugin-fcm example
// In a real app, import from 'tauri-plugin-fcm' (npm package).
// Here we use relative path to the guest-js source for development.
import {
  getToken,
  requestPermission,
  checkPermissions,
  register,
  deleteToken,
  onTokenRefresh,
  onPushError,
} from "../../../guest-js/index.ts";
import type {
  FcmToken,
  PermissionStatus,
  TokenRefreshEvent,
  PushErrorEvent,
} from "../../../guest-js/index.ts";

const output = document.getElementById("output") as HTMLPreElement;

function log(msg: string): void {
  const timestamp = new Date().toLocaleTimeString();
  output.textContent += `[${timestamp}] ${msg}\n`;
  output.scrollTop = output.scrollHeight;
}

// --- Event listeners ---

async function setupListeners(): Promise<void> {
  await onTokenRefresh((event: TokenRefreshEvent) => {
    log(`TOKEN REFRESH: ${event.token}`);
  });

  await onPushError((event: PushErrorEvent) => {
    log(`PUSH ERROR: ${event.error}`);
  });

  log("Event listeners registered (token-refresh, push-error)");
}

// --- Command handlers ---

async function handleCheckPermissions(): Promise<void> {
  try {
    const result: PermissionStatus = await checkPermissions();
    log(`checkPermissions → ${result.status}`);
  } catch (err) {
    log(`checkPermissions error: ${err}`);
  }
}

async function handleRequestPermission(): Promise<void> {
  try {
    const result: PermissionStatus = await requestPermission({
      sound: true,
      badge: true,
      alert: true,
    });
    log(`requestPermission → ${result.status}`);
  } catch (err) {
    log(`requestPermission error: ${err}`);
  }
}

async function handleRegister(): Promise<void> {
  try {
    await register();
    log("register → success");
  } catch (err) {
    log(`register error: ${err}`);
  }
}

async function handleGetToken(): Promise<void> {
  try {
    const result: FcmToken = await getToken();
    log(`getToken → ${result.token}`);
  } catch (err) {
    log(`getToken error: ${err}`);
  }
}

async function handleDeleteToken(): Promise<void> {
  try {
    await deleteToken();
    log("deleteToken → success");
  } catch (err) {
    log(`deleteToken error: ${err}`);
  }
}

// --- Wire up buttons ---

document.getElementById("btn-check")?.addEventListener("click", handleCheckPermissions);
document.getElementById("btn-request")?.addEventListener("click", handleRequestPermission);
document.getElementById("btn-register")?.addEventListener("click", handleRegister);
document.getElementById("btn-token")?.addEventListener("click", handleGetToken);
document.getElementById("btn-delete")?.addEventListener("click", handleDeleteToken);
document.getElementById("btn-clear")?.addEventListener("click", () => {
  output.textContent = "";
});

// --- Init ---
setupListeners();
log("FCM Example app ready");
