import 'package:flutter/material.dart';

import '../services/firebase_service.dart';
import '../theme/app_colors.dart';

class CommandHistory extends StatelessWidget {
  final Stream<List<CommandRecord>> historyStream;

  const CommandHistory({super.key, required this.historyStream});

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
          const Text(
            '명령 히스토리',
            style: TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.bold,
              color: Colors.white,
            ),
          ),
          const SizedBox(height: 16),
          StreamBuilder<List<CommandRecord>>(
            stream: historyStream,
            builder: (context, snapshot) {
              if (snapshot.connectionState == ConnectionState.waiting) {
                return const Center(
                  child: Padding(
                    padding: EdgeInsets.all(24),
                    child: CircularProgressIndicator(
                      strokeWidth: 2,
                      color: AppColors.agentBlue,
                    ),
                  ),
                );
              }

              final records = snapshot.data ?? [];

              if (records.isEmpty) {
                return const Padding(
                  padding: EdgeInsets.symmetric(vertical: 24),
                  child: Center(
                    child: Text(
                      '아직 명령 기록이 없습니다.',
                      style: TextStyle(
                        fontSize: 13,
                        color: AppColors.textMuted,
                      ),
                    ),
                  ),
                );
              }

              return Column(
                children: records.map((r) => _CommandRow(record: r)).toList(),
              );
            },
          ),
        ],
      ),
    );
  }
}

class _CommandRow extends StatelessWidget {
  final CommandRecord record;
  const _CommandRow({required this.record});

  @override
  Widget build(BuildContext context) {
    final color = AppColors.statusColor(record.status);
    final statusLabel = switch (record.status) {
      'pending' => '대기',
      'processing' => '처리 중',
      'completed' => '완료',
      'failed' => '실패',
      _ => record.status,
    };

    final time = record.createdAt;
    final timeStr = time != null
        ? '${time.month.toString().padLeft(2, '0')}/${time.day.toString().padLeft(2, '0')} '
          '${time.hour.toString().padLeft(2, '0')}:${time.minute.toString().padLeft(2, '0')}'
        : '';

    return Container(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: AppColors.background,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: AppColors.border.withOpacity(0.5)),
      ),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  record.command,
                  style: const TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w500,
                    color: Colors.white,
                  ),
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
                const SizedBox(height: 4),
                Row(
                  children: [
                    if (timeStr.isNotEmpty)
                      Text(
                        timeStr,
                        style: const TextStyle(
                          fontSize: 11,
                          color: AppColors.textSecondary,
                        ),
                      ),
                    if (record.result.isNotEmpty) ...[
                      const Text(
                        '  ·  ',
                        style: TextStyle(fontSize: 11, color: AppColors.textSecondary),
                      ),
                      Flexible(
                        child: Text(
                          record.result,
                          style: const TextStyle(
                            fontSize: 11,
                            color: AppColors.textSecondary,
                          ),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                    ],
                  ],
                ),
              ],
            ),
          ),
          const SizedBox(width: 8),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
            decoration: BoxDecoration(
              color: color.withOpacity(0.15),
              borderRadius: BorderRadius.circular(8),
            ),
            child: Text(
              statusLabel,
              style: TextStyle(
                color: color,
                fontSize: 11,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
