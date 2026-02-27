import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../services/firebase_service.dart';
import '../theme/app_colors.dart';

class SessionPanel extends StatefulWidget {
  final VoidCallback? onSessionCreated;
  final VoidCallback? onDisconnected;

  const SessionPanel({
    super.key,
    this.onSessionCreated,
    this.onDisconnected,
  });

  @override
  State<SessionPanel> createState() => _SessionPanelState();
}

class _SessionPanelState extends State<SessionPanel>
    with SingleTickerProviderStateMixin {
  final _firebase = FirebaseService.instance;
  bool _isCreating = false;
  bool _isCollapsed = false;
  String? _sessionCode;
  String _pairingStatus = 'none'; // none, waiting, paired, disconnected

  late final AnimationController _collapseController;
  late final Animation<double> _collapseAnimation;

  @override
  void initState() {
    super.initState();
    _collapseController = AnimationController(
      duration: const Duration(milliseconds: 250),
      vsync: this,
    );
    _collapseAnimation = CurvedAnimation(
      parent: _collapseController,
      curve: Curves.easeInOut,
    );
    _collapseController.value = 1.0;
    _restoreSessionState();
  }

  @override
  void dispose() {
    _collapseController.dispose();
    super.dispose();
  }

  Future<void> _restoreSessionState() async {
    if (!_firebase.hasSession) return;

    _sessionCode = _firebase.sessionCode;

    final status = await _firebase.getSessionStatus();
    if (!mounted) return;

    setState(() {
      if (status == 'paired') {
        _pairingStatus = 'paired';
        widget.onSessionCreated?.call();
      } else if (status == 'waiting') {
        _pairingStatus = 'waiting';
        _startListening();
        widget.onSessionCreated?.call();
      } else if (status == 'disconnected') {
        _pairingStatus = 'disconnected';
      } else {
        _pairingStatus = 'none';
      }
    });
  }

  void _startListening() {
    _firebase.listenSessionStatus(
      onPaired: () {
        if (mounted) {
          setState(() => _pairingStatus = 'paired');
          widget.onSessionCreated?.call();
        }
      },
      onDisconnected: () {
        if (mounted) {
          setState(() => _pairingStatus = 'disconnected');
        }
      },
    );
  }

  Future<void> _createSession() async {
    setState(() => _isCreating = true);
    try {
      final code = await _firebase.createSession();
      setState(() {
        _sessionCode = code;
        _pairingStatus = 'waiting';
        _isCreating = false;
      });

      _startListening();
      widget.onSessionCreated?.call();
    } catch (e) {
      setState(() => _isCreating = false);
    }
  }

  Future<void> _disconnect() async {
    await _firebase.disconnectSession();
    if (mounted) {
      setState(() {
        _sessionCode = null;
        _pairingStatus = 'none';
      });
      widget.onDisconnected?.call();
    }
  }

  void _toggleCollapse() {
    setState(() => _isCollapsed = !_isCollapsed);
    if (_isCollapsed) {
      _collapseController.reverse();
    } else {
      _collapseController.forward();
    }
  }

  bool get _canCollapse => _pairingStatus == 'paired';

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: AppColors.surfaceCardDark,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppColors.border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          GestureDetector(
            onTap: _canCollapse ? _toggleCollapse : null,
            behavior: HitTestBehavior.opaque,
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Row(
                  children: [
                    const Text(
                      '세션 연결',
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                        color: Colors.white,
                      ),
                    ),
                    if (_canCollapse) ...[
                      const SizedBox(width: 6),
                      AnimatedRotation(
                        turns: _isCollapsed ? -0.25 : 0,
                        duration: const Duration(milliseconds: 250),
                        child: const Icon(
                          Icons.expand_more_rounded,
                          size: 20,
                          color: AppColors.textMuted,
                        ),
                      ),
                    ],
                  ],
                ),
                _buildStatusBadge(),
              ],
            ),
          ),
          SizeTransition(
            sizeFactor: _collapseAnimation,
            axisAlignment: -1,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const SizedBox(height: 8),
                Text(
                  _statusDescription,
                  style: const TextStyle(
                    fontSize: 13,
                    color: AppColors.textMuted,
                  ),
                ),
                const SizedBox(height: 16),
                if (_pairingStatus == 'none') _buildCreateButton(),
                if (_sessionCode != null && _pairingStatus != 'none') ...[
                  _buildSessionCodeDisplay(),
                  const SizedBox(height: 12),
                  _buildDisconnectButton(),
                ],
              ],
            ),
          ),
        ],
      ),
    );
  }

  String get _statusDescription {
    switch (_pairingStatus) {
      case 'waiting':
        return 'Android 기기에서 아래 코드를 입력하여 연결하세요.';
      case 'paired':
        return 'Android 기기와 연결되었습니다.';
      case 'disconnected':
        return '연결이 해제되었습니다. 새 세션을 생성하세요.';
      default:
        return '새 세션을 생성하여 Android 기기와 연결하세요.';
    }
  }

  Widget _buildStatusBadge() {
    final Color color;
    final String label;

    switch (_pairingStatus) {
      case 'waiting':
        color = AppColors.statusCancelled;
        label = '대기 중';
      case 'paired':
        color = AppColors.statusRunning;
        label = '연결됨';
      case 'disconnected':
        color = AppColors.statusFailed;
        label = '해제됨';
      default:
        color = AppColors.statusIdle;
        label = '미연결';
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: color.withOpacity(0.15),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Text(
        label,
        style: TextStyle(
          color: color,
          fontSize: 12,
          fontWeight: FontWeight.w600,
        ),
      ),
    );
  }

  Widget _buildCreateButton() {
    return SizedBox(
      width: double.infinity,
      child: ElevatedButton(
        onPressed: _isCreating ? null : _createSession,
        style: ElevatedButton.styleFrom(
          backgroundColor: AppColors.agentBlue,
          foregroundColor: Colors.white,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
          ),
          padding: const EdgeInsets.symmetric(vertical: 14),
        ),
        child: Text(
          _isCreating ? '생성 중...' : '새 세션 생성',
          style: const TextStyle(fontWeight: FontWeight.w600),
        ),
      ),
    );
  }

  Widget _buildSessionCodeDisplay() {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.background,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: AppColors.border),
      ),
      child: Column(
        children: [
          const Text(
            '세션 코드',
            style: TextStyle(
              fontSize: 12,
              color: AppColors.textMuted,
            ),
          ),
          const SizedBox(height: 8),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text(
                _sessionCode ?? '',
                style: const TextStyle(
                  fontSize: 32,
                  fontWeight: FontWeight.bold,
                  color: AppColors.agentBlue,
                  letterSpacing: 8,
                  fontFamily: 'monospace',
                ),
              ),
              const SizedBox(width: 12),
              IconButton(
                icon: const Icon(Icons.copy_rounded, size: 20),
                color: AppColors.textMuted,
                onPressed: () {
                  if (_sessionCode != null) {
                    Clipboard.setData(ClipboardData(text: _sessionCode!));
                  }
                },
                tooltip: '코드 복사',
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildDisconnectButton() {
    return SizedBox(
      width: double.infinity,
      child: OutlinedButton(
        onPressed: _disconnect,
        style: OutlinedButton.styleFrom(
          foregroundColor: AppColors.textMuted,
          side: const BorderSide(color: AppColors.border),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
          ),
          padding: const EdgeInsets.symmetric(vertical: 12),
        ),
        child: const Text('세션 연결 해제'),
      ),
    );
  }
}
