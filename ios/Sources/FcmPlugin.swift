import SwiftRs
import Tauri
import UIKit
import WebKit
import FirebaseCore
import FirebaseMessaging
import UserNotifications

// MARK: - Command Arguments

struct CreateChannelArgs: Decodable {
  let id: String
  let name: String
  let importance: UInt32
}

struct SendNotificationArgs: Decodable {
  let title: String
  let body: String?
  let icon: String?
  let id: Int?
  let channelId: String?
}

class FcmPlugin: Plugin, MessagingDelegate {

  private let tokenBuffer = TokenBuffer()

  // MARK: - Lifecycle

  override func load(webview: WKWebView) {
    if FirebaseApp.app() == nil {
      FirebaseApp.configure()
    }

    Messaging.messaging().delegate = self

    AppDelegateSwizzler.plugin = self
    AppDelegateSwizzler.swizzlePushCallbacks()

    #if !targetEnvironment(simulator)
      DispatchQueue.main.async {
        UIApplication.shared.registerForRemoteNotifications()
      }
    #else
      // Defer so JS event listeners have time to attach after plugin init.
      DispatchQueue.main.asyncAfter(deadline: .now() + 1) { [weak self] in
        try? self?.trigger("push-error", data: ["error": "Push notifications not available on simulator"])
      }
    #endif
  }

  // MARK: - Commands

  @objc public func getToken(_ invoke: Invoke) throws {
    Messaging.messaging().token { [weak self] token, error in
      if let error = error {
        if let buffered = self?.tokenBuffer.consume() {
          invoke.resolve(["token": buffered])
        } else {
          invoke.reject(error.localizedDescription)
        }
        return
      }

      if let token = token {
        invoke.resolve(["token": token])
      } else if let buffered = self?.tokenBuffer.consume() {
        invoke.resolve(["token": buffered])
      } else {
        invoke.reject("FCM token not available")
      }
    }
  }

  @objc public func requestPermissions(_ invoke: Invoke) throws {
    UNUserNotificationCenter.current().requestAuthorization(options: [.sound, .badge, .alert]) { granted, error in
      if let error = error {
        invoke.reject(error.localizedDescription)
        return
      }

      if granted {
        #if !targetEnvironment(simulator)
          DispatchQueue.main.async {
            UIApplication.shared.registerForRemoteNotifications()
          }
        #endif
      }

      invoke.resolve(["notification": granted ? "granted" : "denied"])
    }
  }

  @objc public override func checkPermissions(_ invoke: Invoke) {
    UNUserNotificationCenter.current().getNotificationSettings { settings in
      let status: String

      switch settings.authorizationStatus {
      case .authorized, .provisional, .ephemeral:
        status = "granted"
      case .denied:
        status = "denied"
      case .notDetermined:
        status = "prompt"
      @unknown default:
        status = "prompt"
      }

      invoke.resolve(["notification": status])
    }
  }

  @objc public func register(_ invoke: Invoke) throws {
    #if !targetEnvironment(simulator)
      DispatchQueue.main.async {
        UIApplication.shared.registerForRemoteNotifications()
      }
      invoke.resolve()
    #else
      trigger("push-error", data: ["error": "Push notifications not available on simulator"])
      invoke.resolve()
    #endif
  }

  @objc public func deleteToken(_ invoke: Invoke) throws {
    Messaging.messaging().deleteToken { error in
      if let error = error {
        invoke.reject(error.localizedDescription)
        return
      }

      invoke.resolve()
    }
  }

  @objc public func createChannel(_ invoke: Invoke) throws {
    let _ = try invoke.parseArgs(CreateChannelArgs.self)
    invoke.resolve()
  }

  @objc public func sendNotification(_ invoke: Invoke) throws {
    let args = try invoke.parseArgs(SendNotificationArgs.self)

    let content = UNMutableNotificationContent()
    content.title = args.title
    content.body = args.body ?? ""
    content.sound = .default

    let identifier = args.id.map { String($0) } ?? String(Int.random(in: 0..<Int.max))
    let request = UNNotificationRequest(identifier: identifier, content: content, trigger: nil)

    UNUserNotificationCenter.current().add(request) { error in
      if let error = error {
        invoke.reject(error.localizedDescription)
      } else {
        invoke.resolve()
      }
    }
  }

  // MARK: - MessagingDelegate

  func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
    guard let token = fcmToken else {
      return
    }

    tokenBuffer.store(token: token)
    try? trigger("token-refresh", data: ["token": token])
  }
}

// MARK: - Plugin Entry Point

@_cdecl("init_plugin_fcm")
func initPlugin() -> Plugin {
  FcmPlugin()
}
