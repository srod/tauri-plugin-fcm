const COMMANDS: &[&str] = &[
    "get_token",
    "request_permission",
    "check_permissions",
    "register",
    "delete_token",
];

fn main() {
    tauri_plugin::Builder::new(COMMANDS)
        .android_path("android")
        .ios_path("ios")
        .build();
}
