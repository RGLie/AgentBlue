import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class CommandInput extends StatefulWidget {
  const CommandInput({
    super.key,
    required this.visible,
    required this.onSubmit,
    required this.onClose,
    this.lastCommandId,
  });

  final bool visible;
  final String? lastCommandId;
  final Future<void> Function(String command) onSubmit;
  final Future<void> Function() onClose;

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
        width: 500,
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
        decoration: BoxDecoration(
          color: const Color(0xFF16181D),
          borderRadius: BorderRadius.circular(14),
          border: Border.all(color: const Color(0xFF2C3340)),
          boxShadow: const [
            BoxShadow(
              color: Colors.black54,
              blurRadius: 20,
              offset: Offset(0, 12),
            ),
          ],
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Focus(
              autofocus: true,
              onKeyEvent: (_, event) {
                if (event.logicalKey == LogicalKeyboardKey.escape && event is KeyDownEvent) {
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
                  hintStyle: const TextStyle(color: Color(0xFF8C96A8)),
                  suffixIcon: IconButton(
                    icon: _submitting
                        ? const SizedBox(
                            width: 18,
                            height: 18,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : const Icon(Icons.arrow_circle_up_rounded),
                    onPressed: _submitting ? null : _handleSubmit,
                  ),
                ),
              ),
            ),
            if (widget.lastCommandId != null) ...[
              const SizedBox(height: 8),
              Align(
                alignment: Alignment.centerLeft,
                child: Text(
                  'Last Command ID: ${widget.lastCommandId}',
                  style: const TextStyle(fontSize: 11, color: Color(0xFF7A869C)),
                ),
              ),
            ]
          ],
        ),
      ),
    );
  }
}
