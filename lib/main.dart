import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:window_manager/window_manager.dart';

import 'services/firebase_service.dart';
import 'services/hotkey_service.dart';
import 'services/notification_service.dart';
import 'theme/app_colors.dart';
import 'widgets/agent_status_panel.dart';
import 'widgets/command_history.dart';
import 'widgets/command_input.dart';
import 'widgets/session_panel.dart';

bool get isDesktop {
  if (kIsWeb) return false;
  return [
    TargetPlatform.windows,
    TargetPlatform.linux,
    TargetPlatform.macOS,
  ].contains(defaultTargetPlatform);
}

const _mainWindowSize = Size(480, 720);
const _compactWindowSize = Size(520, 110);

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  if (isDesktop) {
    await windowManager.ensureInitialized();
    await HotkeyService.instance.initialize();
    await NotificationService.instance.initialize();
  }

  await FirebaseService.instance.initialize();

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
        colorSchemeSeed: AppColors.agentBlue,
        useMaterial3: true,
        scaffoldBackgroundColor: AppColors.background,
      ),
      home: const MainPage(),
    );
  }
}

class MainPage extends StatefulWidget {
  const MainPage({super.key});

  @override
  State<MainPage> createState() => _MainPageState();
}

class _MainPageState extends State<MainPage> with WindowListener {
  bool _isCompactMode = false;
  Stream<AgentState>? _agentStateStream;
  Stream<List<CommandRecord>>? _historyStream;

  @override
  void initState() {
    super.initState();
    if (isDesktop) {
      _initWindow();
      _registerHotkey();
    }
    _refreshStreams();
  }

  Future<void> _initWindow() async {
    await windowManager.setPreventClose(true);
    await windowManager.setTitleBarStyle(TitleBarStyle.hidden,
        windowButtonVisibility: false);
    await windowManager.setSize(_mainWindowSize);
    await windowManager.center();
    await windowManager.show();
    windowManager.addListener(this);
  }

  Future<void> _registerHotkey() async {
    await HotkeyService.instance.registerShowCommandWindow(() async {
      if (_isCompactMode) {
        await _switchToMainMode();
      } else {
        await _switchToCompactMode();
      }
    });
  }

  Future<void> _switchToCompactMode() async {
    setState(() => _isCompactMode = true);
    if (isDesktop) {
      await windowManager.setSize(_compactWindowSize);
      await windowManager.setAlwaysOnTop(true);
      await windowManager.setSkipTaskbar(true);
      await windowManager.setResizable(false);
      await windowManager.center();
      await windowManager.show();
      await windowManager.focus();
    }
  }

  Future<void> _switchToMainMode() async {
    setState(() => _isCompactMode = false);
    if (isDesktop) {
      await windowManager.setSize(_mainWindowSize);
      await windowManager.setAlwaysOnTop(false);
      await windowManager.setSkipTaskbar(false);
      await windowManager.setResizable(true);
      await windowManager.center();
      await windowManager.show();
      await windowManager.focus();
    }
  }

  void _refreshStreams() {
    final firebase = FirebaseService.instance;
    if (firebase.hasSession) {
      setState(() {
        _agentStateStream = firebase.agentStateStream();
        _historyStream = firebase.commandHistoryStream();
      });
    }
  }

  Future<void> _submitCommand(String command) async {
    final firebase = FirebaseService.instance;
    final commandId = await firebase.sendCommand(command: command);

    if (_isCompactMode) {
      await _switchToMainMode();
    }

    firebase.listenCommandResult(
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
  void onWindowClose() async {
    if (_isCompactMode) {
      await _switchToMainMode();
    } else {
      await windowManager.destroy();
    }
  }

  @override
  void dispose() {
    if (isDesktop) {
      windowManager.removeListener(this);
      HotkeyService.instance.dispose();
    }
    FirebaseService.instance.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (_isCompactMode) {
      return Scaffold(
        backgroundColor: Colors.transparent,
        body: Center(
          child: CommandInput(
            visible: true,
            compact: true,
            onSubmit: _submitCommand,
            onClose: _switchToMainMode,
            agentStateStream: _agentStateStream,
          ),
        ),
      );
    }

    return Scaffold(
      backgroundColor: AppColors.background,
      body: _MainWindowBody(
        agentStateStream: _agentStateStream,
        historyStream: _historyStream,
        onSubmit: _submitCommand,
        onSessionCreated: _refreshStreams,
        onDisconnected: () {
          setState(() {
            _agentStateStream = null;
            _historyStream = null;
          });
        },
        onCompactMode: _switchToCompactMode,
      ),
    );
  }
}

class _MainWindowBody extends StatelessWidget {
  final Stream<AgentState>? agentStateStream;
  final Stream<List<CommandRecord>>? historyStream;
  final Future<void> Function(String) onSubmit;
  final VoidCallback onSessionCreated;
  final VoidCallback onDisconnected;
  final VoidCallback onCompactMode;

  const _MainWindowBody({
    required this.agentStateStream,
    required this.historyStream,
    required this.onSubmit,
    required this.onSessionCreated,
    required this.onDisconnected,
    required this.onCompactMode,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        _buildTitleBar(context),
        Expanded(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                SessionPanel(
                  onSessionCreated: onSessionCreated,
                  onDisconnected: onDisconnected,
                ),
                const SizedBox(height: 16),
                CommandInput(
                  visible: true,
                  onSubmit: onSubmit,
                  onClose: () async {},
                  agentStateStream: agentStateStream,
                ),
                if (agentStateStream != null) ...[
                  const SizedBox(height: 16),
                  AgentStatusPanel(stateStream: agentStateStream!),
                ],
                if (historyStream != null) ...[
                  const SizedBox(height: 16),
                  CommandHistory(historyStream: historyStream!),
                ],
              ],
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildTitleBar(BuildContext context) {
    return GestureDetector(
      onPanStart: (_) => windowManager.startDragging(),
      child: Container(
        height: 48,
        padding: const EdgeInsets.symmetric(horizontal: 16),
        decoration: const BoxDecoration(
          color: AppColors.surfaceCardDark,
          border: Border(
            bottom: BorderSide(color: AppColors.border, width: 1),
          ),
        ),
        child: Row(
          children: [
            const SizedBox(width: 70),
            const Expanded(
              child: Text(
                'AgentBlue Commander',
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontSize: 13,
                  fontWeight: FontWeight.w600,
                  color: Colors.white70,
                ),
              ),
            ),
            Tooltip(
              message: '빠른 입력 모드 (Cmd+Shift+Space)',
              child: IconButton(
                icon: const Icon(
                  Icons.terminal_rounded,
                  size: 18,
                  color: AppColors.textMuted,
                ),
                onPressed: onCompactMode,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
