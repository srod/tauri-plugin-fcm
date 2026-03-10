const COMMANDS: &[&str] = &[
    "get_token",
    "request_permissions",
    "check_permissions",
    "register",
    "delete_token",
    "create_channel",
    "send_notification",
    "register_listener",
];

fn main() {
    tauri_plugin::Builder::new(COMMANDS)
        .android_path("android")
        .ios_path("ios")
        .build();
}
