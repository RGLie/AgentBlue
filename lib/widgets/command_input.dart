import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../services/firebase_service.dart';
import '../theme/app_colors.dart';

class CommandInput extends StatefulWidget {
  const CommandInput({
    super.key,
    required this.visible,
    required this.onSubmit,
    required this.onClose,
    this.lastCommandId,
    this.agentStateStream,
    this.compact = false,
  });

  final bool visible;
  final String? lastCommandId;
  final Future<void> Function(String command) onSubmit;
  final Future<void> Function() onClose;
  final Stream<AgentState>? agentStateStream;
  final bool compact;

  @override
  State<CommandInput> createState() => _CommandInputState();
}

class _CommandInputState extends State<CommandInput> {
  final _controller = TextEditingController();
  bool _submitting = false;

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Future<void> _handleSubmit() async {
    final text = _controller.text.trim();
    if (text.isEmpty || _submitting) return;

    setState(() => _submitting = true);
    try {
      await widget.onSubmit(text);
      _controller.clear();
    } finally {
      if (mounted) {
        setState(() => _submitting = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    if (!widget.visible) {
      return const SizedBox.shrink();
    }

    return Material(
      color: Colors.transparent,
      child: Container(
        width: widget.compact ? 500 : double.infinity,
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
        decoration: BoxDecoration(
          color: AppColors.background,
          borderRadius: BorderRadius.circular(14),
          border: Border.all(color: AppColors.border),
          boxShadow: widget.compact
              ? const [
                  BoxShadow(
                    color: Colors.black54,
                    blurRadius: 20,
                    offset: Offset(0, 12),
                  ),
                ]
              : null,
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (widget.agentStateStream != null && !widget.compact)
              StreamBuilder<AgentState>(
                stream: widget.agentStateStream,
                builder: (context, snapshot) {
                  final state = snapshot.data;
                  if (state == null || state.isIdle) return const SizedBox.shrink();
                  return Padding(
                    padding: const EdgeInsets.only(bottom: 10),
                    child: _MiniStatusBar(state: state),
                  );
                },
              ),
            Focus(
              autofocus: true,
              onKeyEvent: (_, event) {
                if (event.logicalKey == LogicalKeyboardKey.escape &&
                    event is KeyDownEvent) {
                  widget.onClose();
                  return KeyEventResult.handled;
                }
                return KeyEventResult.ignored;
              },
              child: TextField(
                controller: _controller,
                autofocus: true,
                enabled: !_submitting,
                textInputAction: TextInputAction.send,
                onSubmitted: (_) => _handleSubmit(),
                style: const TextStyle(color: Colors.white),
                decoration: InputDecoration(
                  hintText: '예: 내일 아침 7시 알람 맞춰줘',
                  hintStyle: const TextStyle(color: AppColors.textMuted),
                  border: InputBorder.none,
                  suffixIcon: IconButton(
                    icon: _submitting
                        ? const SizedBox(
                            width: 18,
                            height: 18,
                            child: CircularProgressIndicator(
                              strokeWidth: 2,
                              color: AppColors.agentBlue,
                            ),
                          )
                        : const Icon(
                            Icons.arrow_circle_up_rounded,
                            color: AppColors.agentBlue,
                          ),
                    onPressed: _submitting ? null : _handleSubmit,
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _MiniStatusBar extends StatelessWidget {
  final AgentState state;
  const _MiniStatusBar({required this.state});

  @override
  Widget build(BuildContext context) {
    final color = AppColors.statusColor(state.status);
    final progress = state.maxSteps > 0
        ? state.currentStep / state.maxSteps
        : 0.0;

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      decoration: BoxDecoration(
        color: color.withOpacity(0.08),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: color.withOpacity(0.2)),
      ),
      child: Row(
        children: [
          if (state.isRunning)
            Container(
              width: 6,
              height: 6,
              margin: const EdgeInsets.only(right: 8),
              decoration: BoxDecoration(
                color: color,
                shape: BoxShape.circle,
              ),
            ),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Flexible(
                      child: Text(
                        state.currentCommand ?? '',
                        style: const TextStyle(
                          fontSize: 12,
                          color: Colors.white70,
                          fontWeight: FontWeight.w500,
                        ),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                    Text(
                      '${state.currentStep}/${state.maxSteps}',
                      style: TextStyle(
                        fontSize: 11,
                        color: color,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ],
                ),
                if (state.isRunning) ...[
                  const SizedBox(height: 4),
                  ClipRRect(
                    borderRadius: BorderRadius.circular(2),
                    child: LinearProgressIndicator(
                      value: progress,
                      minHeight: 3,
                      backgroundColor: color.withOpacity(0.15),
                      valueColor: AlwaysStoppedAnimation<Color>(color),
                    ),
                  ),
                ],
                if (state.currentReasoning != null) ...[
                  const SizedBox(height: 4),
                  Text(
                    state.currentReasoning!,
                    style: const TextStyle(
                      fontSize: 11,
                      color: AppColors.textSecondary,
                    ),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                ],
              ],
            ),
          ),
        ],
      ),
    );
  }
}
