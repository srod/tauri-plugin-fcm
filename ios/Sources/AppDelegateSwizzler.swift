import UIKit
import Tauri
import FirebaseMessaging
import ObjectiveC.runtime

enum AppDelegateSwizzler {
  static weak var plugin: FcmPlugin?

  private static var hasSwizzled = false

  static func swizzlePushCallbacks() {
    guard !hasSwizzled else { return }
    guard let delegate = UIApplication.shared.delegate else { return }
    let delegateClass: AnyClass = type(of: delegate)

    swizzle(
      delegateClass,
      #selector(UIApplicationDelegate.application(_:didRegisterForRemoteNotificationsWithDeviceToken:)),
      #selector(FcmPushForwarder.fcm_application(_:didRegisterForRemoteNotificationsWithDeviceToken:))
    )

    swizzle(
      delegateClass,
      #selector(UIApplicationDelegate.application(_:didFailToRegisterForRemoteNotificationsWithError:)),
      #selector(FcmPushForwarder.fcm_application(_:didFailToRegisterForRemoteNotificationsWithError:))
    )

    hasSwizzled = true
  }

  private static func swizzle(_ cls: AnyClass, _ original: Selector, _ replacement: Selector) {
    guard let swizzledMethod = class_getInstanceMethod(FcmPushForwarder.self, replacement) else { return }

    if let originalMethod = class_getInstanceMethod(cls, original) {
      class_addMethod(
        cls,
        replacement,
        method_getImplementation(swizzledMethod),
        method_getTypeEncoding(swizzledMethod)
      )

      guard let addedMethod = class_getInstanceMethod(cls, replacement) else { return }
      method_exchangeImplementations(originalMethod, addedMethod)
    } else {
      class_addMethod(
        cls,
        original,
        method_getImplementation(swizzledMethod),
        method_getTypeEncoding(swizzledMethod)
      )
    }
  }
}

final class FcmPushForwarder: NSObject {
  @objc func fcm_application(
    _ application: UIApplication,
    didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
  ) {
    Messaging.messaging().apnsToken = deviceToken

    if responds(to: #selector(fcm_application(_:didRegisterForRemoteNotificationsWithDeviceToken:))) {
      fcm_application(application, didRegisterForRemoteNotificationsWithDeviceToken: deviceToken)
    }
  }

  @objc func fcm_application(
    _ application: UIApplication,
    didFailToRegisterForRemoteNotificationsWithError error: Error
  ) {
    try? AppDelegateSwizzler.plugin?.trigger("push-error", data: ["error": error.localizedDescription])

    if responds(to: #selector(fcm_application(_:didFailToRegisterForRemoteNotificationsWithError:))) {
      fcm_application(application, didFailToRegisterForRemoteNotificationsWithError: error)
    }
  }
}
