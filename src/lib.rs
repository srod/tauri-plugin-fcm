mod commands;
mod error;
mod models;

#[cfg(not(mobile))]
mod desktop;
#[cfg(mobile)]
mod mobile;

use tauri::{
    plugin::{Builder, TauriPlugin},
    Manager, Runtime,
};

#[cfg(not(mobile))]
use desktop as platform;
#[cfg(mobile)]
use mobile as platform;

pub use error::{Error, Result};
pub use models::*;

pub type Fcm<R> = platform::Fcm<R>;

pub fn init<R: Runtime>() -> TauriPlugin<R> {
    Builder::new("fcm")
        .invoke_handler(tauri::generate_handler![
            commands::get_token,
            commands::request_permission,
            commands::check_permissions,
            commands::register,
            commands::delete_token,
        ])
        .setup(|app, api| {
            let plugin = platform::init(app, api)?;
            app.manage(plugin);
            Ok(())
        })
        .build()
}
