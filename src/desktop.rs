use std::marker::PhantomData;

use tauri::{plugin::PluginApi, AppHandle, Runtime};

use crate::models::{FcmToken, PermissionOptions, PermissionStatus};

pub struct Fcm<R: Runtime>(PhantomData<fn() -> R>);

impl<R: Runtime> Fcm<R> {
    pub fn get_token(&self) -> crate::Result<FcmToken> {
        Err(crate::Error::MobileOnly)
    }

    pub fn request_permission(
        &self,
        _options: PermissionOptions,
    ) -> crate::Result<PermissionStatus> {
        Err(crate::Error::MobileOnly)
    }

    pub fn check_permissions(&self) -> crate::Result<PermissionStatus> {
        Err(crate::Error::MobileOnly)
    }

    pub fn register(&self) -> crate::Result<()> {
        Err(crate::Error::MobileOnly)
    }

    pub fn delete_token(&self) -> crate::Result<()> {
        Err(crate::Error::MobileOnly)
    }
}

pub fn init<R: Runtime>(
    _app: &AppHandle<R>,
    _api: PluginApi<R, ()>,
) -> std::result::Result<Fcm<R>, Box<dyn std::error::Error>> {
    Ok(Fcm(PhantomData::<fn() -> R>))
}
