use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FcmToken {
    pub token: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PermissionStatus {
    pub status: String,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
pub struct PermissionOptions {
    pub sound: Option<bool>,
    pub badge: Option<bool>,
    pub alert: Option<bool>,
}
