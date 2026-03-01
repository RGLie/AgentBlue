import 'package:hotkey_manager/hotkey_manager.dart';
import 'package:flutter/services.dart';

class HotkeyService {
  HotkeyService._();

  static final HotkeyService instance = HotkeyService._();

  final _showWindowHotkey = HotKey(
    key: PhysicalKeyboardKey.space,
    modifiers: [HotKeyModifier.meta, HotKeyModifier.shift],
    scope: HotKeyScope.system,
  );

  Future<void> initialize() async {
    await hotKeyManager.unregisterAll();
  }

  Future<void> registerShowCommandWindow(Future<void> Function() onPressed) async {
    await hotKeyManager.register(
      _showWindowHotkey,
      keyDownHandler: (_) async {
        await onPressed();
      },
    );
  }

  Future<void> dispose() async {
    await hotKeyManager.unregister(_showWindowHotkey);
  }
}
