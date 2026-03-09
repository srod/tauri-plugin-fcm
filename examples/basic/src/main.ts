// tauri-plugin-fcm example
// In a real app, import from 'tauri-plugin-fcm' (npm package).
// Here we use relative path to the guest-js source for development.

import type {
  FcmToken,
  PermissionState,
  PushErrorEvent,
  TokenRefreshEvent,
} from "../../../guest-js/index.ts";
import {
  checkPermissions,
  deleteToken,
  getToken,
  onPushError,
  onTokenRefresh,
  register,
  requestPermissions,
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
    const result: PermissionState = await checkPermissions();
    log(`checkPermissions → ${result}`);
  } catch (err) {
    log(`checkPermissions error: ${err}`);
  }
}

async function handleRequestPermissions(): Promise<void> {
  try {
    const result: PermissionState = await requestPermissions();
    log(`requestPermissions → ${result}`);
  } catch (err) {
    log(`requestPermissions error: ${err}`);
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

document
  .getElementById("btn-check")
  ?.addEventListener("click", handleCheckPermissions);
document
  .getElementById("btn-request")
  ?.addEventListener("click", handleRequestPermissions);
document
  .getElementById("btn-register")
  ?.addEventListener("click", handleRegister);
document.getElementById("btn-token")?.addEventListener("click", handleGetToken);
document
  .getElementById("btn-delete")
  ?.addEventListener("click", handleDeleteToken);
document.getElementById("btn-clear")?.addEventListener("click", () => {
  output.textContent = "";
});

// --- Init ---
setupListeners();
log("FCM Example app ready");
