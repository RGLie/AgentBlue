import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:window_manager/window_manager.dart';

import 'services/firebase_service.dart';
import 'services/hotkey_service.dart';
import 'services/notification_service.dart';
import 'widgets/command_input.dart';

bool get isDesktop {
  if (kIsWeb) return false;
  return [
    TargetPlatform.windows,
    TargetPlatform.linux,
    TargetPlatform.macOS,
  ].contains(defaultTargetPlatform);
}

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  if (isDesktop) {
    await windowManager.ensureInitialized();
    await HotkeyService.instance.initialize();

    // [수정] 알림 기능은 데스크톱에서만 초기화되도록 이곳으로 이동!
    await NotificationService.instance.initialize();
  }

  await FirebaseService.instance.initialize();
  // await NotificationService.instance.initialize(); // [삭제] 여기 있으면 웹에서 에러 남

  runApp(const AgentBlueDesktopApp());
}

class AgentBlueDesktopApp extends StatelessWidget {
  const AgentBlueDesktopApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'AgentBlue Commander',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        brightness: Brightness.dark,
        colorSchemeSeed: const Color(0xFF4F8CFF),
        useMaterial3: true,
      ),
      home: const CommandOverlayPage(),
    );
  }
}

class CommandOverlayPage extends StatefulWidget {
  const CommandOverlayPage({super.key});

  @override
  State<CommandOverlayPage> createState() => _CommandOverlayPageState();
}

class _CommandOverlayPageState extends State<CommandOverlayPage>
    with WindowListener {
  bool _isVisible = !isDesktop;
  String? _lastCommandId;

  @override
  void initState() {
    super.initState();
    if (isDesktop) {
      _initWindow();
      _registerHotkey();
    }
  }

  Future<void> _initWindow() async {
    await windowManager.setPreventClose(true);
    await windowManager.setAlwaysOnTop(true);
    await windowManager.setSkipTaskbar(true);
    await windowManager.setResizable(false);
    await windowManager.setTitleBarStyle(TitleBarStyle.hidden,
        windowButtonVisibility: false);
    await windowManager.setSize(const Size(520, 110));
    await windowManager.center();
    await windowManager.hide();
    windowManager.addListener(this);
  }

  Future<void> _registerHotkey() async {
    await HotkeyService.instance.registerShowCommandWindow(() async {
      setState(() => _isVisible = true);
      await windowManager.center();
      await windowManager.show();
      await windowManager.focus();
    });
  }

  Future<void> _closeOverlay() async {
    if (!mounted) return;
    setState(() => _isVisible = false);
    if (isDesktop) {
      await windowManager.hide();
    }
  }

  Future<void> _submitCommand(String command) async {
    final commandId =
        await FirebaseService.instance.sendCommand(command: command);
    setState(() => _lastCommandId = commandId);
    await _closeOverlay();
    _listenCommandResult(commandId);
  }

  void _listenCommandResult(String commandId) {
    FirebaseService.instance.listenCommandResult(
      commandId,
      onCompleted: (result) {
        NotificationService.instance.show(
          title: 'AgentBlue',
          body: result.isEmpty ? '작업이 완료되었습니다.' : result,
        );
      },
      onFailed: (result) {
        NotificationService.instance.show(
          title: 'AgentBlue',
          body: result.isEmpty ? '작업 실행에 실패했습니다.' : '실패: $result',
        );
      },
    );
  }

  @override
  void dispose() {
    if (isDesktop) {
      windowManager.removeListener(this);
      HotkeyService.instance.dispose();
    }
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.transparent,
      body: Center(
        child: CommandInput(
          visible: _isVisible,
          lastCommandId: _lastCommandId,
          onSubmit: _submitCommand,
          onClose: _closeOverlay,
        ),
      ),
    );
  }
}
