pub fn run() {
    let builder = tauri::Builder::default();
    #[cfg(mobile)]
    let builder = builder.plugin(tauri_plugin_fcm::init());

    builder
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
