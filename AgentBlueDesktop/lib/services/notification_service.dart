import 'package:flutter/foundation.dart';
import 'package:local_notifier/local_notifier.dart';

class NotificationService {
  NotificationService._();

  static final NotificationService instance = NotificationService._();

  bool get _isDesktop {
    if (kIsWeb) return false;
    return [
      TargetPlatform.windows,
      TargetPlatform.linux,
      TargetPlatform.macOS,
    ].contains(defaultTargetPlatform);
  }

  Future<void> initialize() async {
    if (!_isDesktop) return;

    await localNotifier.setup(
      appName: 'AgentBlue',
      shortcutPolicy: ShortcutPolicy.requireCreate,
    );
  }

  void show({required String title, required String body}) {
    if (!_isDesktop) {
      if (kDebugMode) {
        print('Notification (web): $title - $body');
      }
      return;
    }
    final notification = LocalNotification(
      title: title,
      body: body,
    );
    notification.show();
  }
}
