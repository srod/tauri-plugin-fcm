import Foundation

/// Thread-safe in-memory buffer for the latest FCM token.
///
/// Solves the cold-start race condition where `MessagingDelegate.didReceiveRegistrationToken`
/// fires before JS event listeners are attached. The `getToken` command reads from this buffer
/// as a fallback when `Messaging.messaging().token` fails.
final class TokenBuffer {
  private let lock = NSLock()
  private var latestToken: String?

  /// Store the latest FCM token. Thread-safe.
  func store(token: String) {
    lock.lock()
    defer { lock.unlock() }
    latestToken = token
  }

  /// Return the buffered token. Does NOT clear it — the token remains available
  /// for subsequent reads until overwritten by a new store().
  /// Thread-safe.
  func consume() -> String? {
    lock.lock()
    defer { lock.unlock() }
    return latestToken
  }
}
