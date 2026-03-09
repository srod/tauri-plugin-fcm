import {
  addPluginListener,
  checkPermissions as checkPermissions_,
  invoke,
  type PermissionState,
  type PluginListener,
  requestPermissions as requestPermissions_,
} from "@tauri-apps/api/core";

/**
 * Represents an FCM token response.
 */
export interface FcmToken {
  /** The FCM registration token */
  token: string;
}

export type { PermissionState } from "@tauri-apps/api/core";

/**
 * Event emitted when FCM token is refreshed.
 */
export interface TokenRefreshEvent {
  /** The new FCM registration token */
  token: string;
}

/**
 * Event emitted when a push notification error occurs.
 */
export interface PushErrorEvent {
  /** Error message describing the push notification error */
  error: string;
}

/**
 * Options for creating a notification channel (Android only).
 */
export interface CreateChannelOptions extends Record<string, unknown> {
  /** Unique identifier for the channel */
  id: string;
  /** Display name for the channel */
  name: string;
  /** Importance level for the channel (0-5) */
  importance: number;
}

/**
 * Options for sending a notification.
 */
export interface SendNotificationOptions extends Record<string, unknown> {
  /** Notification title */
  title: string;
  /** Notification body text */
  body?: string;
  /** Icon identifier or URL */
  icon?: string;
  /** Notification ID */
  id?: number;
  /** Channel ID (Android) */
  channelId?: string;
}

/**
 * Retrieves the current FCM registration token.
 * @returns Promise resolving to an object containing the FCM token
 * @throws Error if token retrieval fails
 */
export async function getToken(): Promise<FcmToken> {
  return await invoke<FcmToken>("plugin:fcm|get_token");
}

/**
 * Requests notification permissions from the user.
 * @returns Promise resolving to the notification permission state
 * @throws Error if permission request fails
 */
export async function requestPermissions(): Promise<PermissionState> {
  return await requestPermissions_<{ notification: PermissionState }>(
    "fcm",
  ).then((result) => result.notification);
}

/**
 * Checks the current notification permission status.
 * @returns Promise resolving to the current notification permission state
 * @throws Error if permission check fails
 */
export async function checkPermissions(): Promise<PermissionState> {
  return await checkPermissions_<{ notification: PermissionState }>("fcm").then(
    (result) => result.notification,
  );
}

/**
 * Registers the device for push notifications.
 * On iOS, this triggers the native registration flow.
 * On Android, this is a no-op as FCM auto-registers.
 * @returns Promise that resolves when registration is complete
 * @throws Error if registration fails
 */
export async function register(): Promise<void> {
  await invoke("plugin:fcm|register");
}

/**
 * Deletes the current FCM registration token.
 * @returns Promise that resolves when token deletion is complete
 * @throws Error if token deletion fails
 */
export async function deleteToken(): Promise<void> {
  await invoke("plugin:fcm|delete_token");
}

/**
 * Registers a listener for FCM token refresh events.
 * @param handler - Callback function invoked when token is refreshed
 * @returns Promise resolving to a PluginListener that can be used to unlisten
 */
export async function onTokenRefresh(
  handler: (event: TokenRefreshEvent) => void,
): Promise<PluginListener> {
  return await addPluginListener("fcm", "token-refresh", handler);
}

/**
 * Registers a listener for push notification error events.
 * @param handler - Callback function invoked when a push error occurs
 * @returns Promise resolving to a PluginListener that can be used to unlisten
 */
export async function onPushError(
  handler: (event: PushErrorEvent) => void,
): Promise<PluginListener> {
  return await addPluginListener("fcm", "push-error", handler);
}

/**
 * Creates a notification channel (Android only, no-op on iOS).
 * @param options - Channel configuration options
 * @returns Promise that resolves when channel creation is complete
 * @throws Error if channel creation fails
 */
export async function createChannel(
  options: CreateChannelOptions,
): Promise<void> {
  await invoke("plugin:fcm|create_channel", options);
}

/**
 * Sends a notification to the user.
 * @param options - Notification configuration options
 * @returns Promise that resolves when notification is sent
 * @throws Error if notification sending fails
 */
export async function sendNotification(
  options: SendNotificationOptions,
): Promise<void> {
  await invoke("plugin:fcm|send_notification", options);
}
