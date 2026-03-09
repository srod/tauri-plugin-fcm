use tauri::{AppHandle, Manager, Runtime};

use crate::mobile as platform;
use crate::models::{CreateChannelArgs, FcmToken, PermissionStatus, SendNotificationArgs};

#[tauri::command]
pub async fn get_token<R: Runtime>(app: AppHandle<R>) -> crate::Result<FcmToken> {
    app.state::<platform::Fcm<R>>().inner().get_token()
}

#[tauri::command]
pub async fn request_permissions<R: Runtime>(app: AppHandle<R>) -> crate::Result<PermissionStatus> {
    app.state::<platform::Fcm<R>>()
        .inner()
        .request_permissions()
}

#[tauri::command]
pub async fn check_permissions<R: Runtime>(app: AppHandle<R>) -> crate::Result<PermissionStatus> {
    app.state::<platform::Fcm<R>>().inner().check_permissions()
}

#[tauri::command]
pub async fn register<R: Runtime>(app: AppHandle<R>) -> crate::Result<()> {
    app.state::<platform::Fcm<R>>().inner().register()
}

#[tauri::command]
pub async fn delete_token<R: Runtime>(app: AppHandle<R>) -> crate::Result<()> {
    app.state::<platform::Fcm<R>>().inner().delete_token()
}

#[tauri::command]
pub async fn create_channel<R: Runtime>(
    app: AppHandle<R>,
    args: CreateChannelArgs,
) -> crate::Result<()> {
    app.state::<platform::Fcm<R>>().inner().create_channel(args)
}

#[tauri::command]
pub async fn send_notification<R: Runtime>(
    app: AppHandle<R>,
    args: SendNotificationArgs,
) -> crate::Result<()> {
    app.state::<platform::Fcm<R>>()
        .inner()
        .send_notification(args)
}
