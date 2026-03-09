#![cfg(mobile)]

mod commands;
mod error;
mod mobile;
mod models;

use tauri::{
    plugin::{Builder, TauriPlugin},
    Manager, Runtime,
};

use mobile as platform;

pub use error::{Error, Result};
pub use models::*;

pub type Fcm<R> = platform::Fcm<R>;

pub trait FcmExt<R: Runtime> {
    fn fcm(&self) -> &Fcm<R>;
}

impl<R: Runtime, T: Manager<R>> FcmExt<R> for T {
    fn fcm(&self) -> &Fcm<R> {
        self.state::<Fcm<R>>().inner()
    }
}

pub fn init<R: Runtime>() -> TauriPlugin<R> {
    Builder::new("fcm")
        .invoke_handler(tauri::generate_handler![
            commands::get_token,
            commands::request_permissions,
            commands::check_permissions,
            commands::register,
            commands::delete_token,
            commands::create_channel,
            commands::send_notification,
        ])
        .setup(|app, api| {
            let plugin = platform::init(app, api)?;
            app.manage(plugin);
            Ok(())
        })
        .build()
}
