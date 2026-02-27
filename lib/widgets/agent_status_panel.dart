import 'package:flutter/material.dart';

import '../services/firebase_service.dart';
import '../theme/app_colors.dart';

class AgentStatusPanel extends StatelessWidget {
  final Stream<AgentState> stateStream;

  const AgentStatusPanel({super.key, required this.stateStream});

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<AgentState>(
      stream: stateStream,
      builder: (context, snapshot) {
        final state = snapshot.data ?? const AgentState();
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
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const Text(
                    '에이전트 상태',
                    style: TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                      color: Colors.white,
                    ),
                  ),
                  _StatusBadge(status: state.status),
                ],
              ),
              const SizedBox(height: 16),
              if (state.isRunning || state.isCompleted || state.isFailed || state.isCancelled) ...[
                _buildCurrentCommand(state),
                const SizedBox(height: 12),
                _buildProgressBar(state),
                const SizedBox(height: 12),
                if (state.currentReasoning != null)
                  _buildReasoning(state),
                if (state.liveSteps.isNotEmpty) ...[
                  const SizedBox(height: 16),
                  _buildStepsList(state),
                ],
              ] else ...[
                _buildIdleState(),
              ],
            ],
          ),
        );
      },
    );
  }

  Widget _buildCurrentCommand(AgentState state) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: AppColors.background,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: AppColors.border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            '현재 명령',
            style: TextStyle(
              fontSize: 11,
              color: AppColors.textMuted,
              fontWeight: FontWeight.w600,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            state.currentCommand ?? '-',
            style: const TextStyle(
              fontSize: 14,
              color: Colors.white,
              fontWeight: FontWeight.w500,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildProgressBar(AgentState state) {
    final progress = state.maxSteps > 0
        ? state.currentStep / state.maxSteps
        : 0.0;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              '진행률',
              style: TextStyle(
                fontSize: 12,
                color: AppColors.textMuted,
                fontWeight: FontWeight.w600,
              ),
            ),
            Text(
              '${state.currentStep} / ${state.maxSteps} 스텝',
              style: const TextStyle(
                fontSize: 12,
                color: AppColors.textSecondary,
              ),
            ),
          ],
        ),
        const SizedBox(height: 6),
        ClipRRect(
          borderRadius: BorderRadius.circular(4),
          child: LinearProgressIndicator(
            value: progress,
            minHeight: 6,
            backgroundColor: AppColors.border,
            valueColor: AlwaysStoppedAnimation<Color>(
              AppColors.statusColor(state.status),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildReasoning(AgentState state) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: AppColors.agentBlue.withOpacity(0.08),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: AppColors.agentBlue.withOpacity(0.2)),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(
            state.isRunning ? Icons.psychology_rounded : Icons.info_outline_rounded,
            size: 16,
            color: AppColors.agentBlue,
          ),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              state.currentReasoning ?? '',
              style: const TextStyle(
                fontSize: 13,
                color: Colors.white70,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildStepsList(AgentState state) {
    final recentSteps = state.liveSteps.reversed.take(5).toList();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          '최근 스텝 (${state.liveSteps.length})',
          style: const TextStyle(
            fontSize: 12,
            color: AppColors.textMuted,
            fontWeight: FontWeight.w600,
          ),
        ),
        const SizedBox(height: 8),
        ...recentSteps.map((step) => _StepRow(step: step)),
      ],
    );
  }

  Widget _buildIdleState() {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(vertical: 24),
      child: const Column(
        children: [
          Icon(
            Icons.smart_toy_outlined,
            size: 36,
            color: AppColors.textMuted,
          ),
          SizedBox(height: 8),
          Text(
            '대기 중',
            style: TextStyle(
              fontSize: 14,
              color: AppColors.textMuted,
            ),
          ),
          SizedBox(height: 4),
          Text(
            '명령을 보내면 에이전트가 실행됩니다.',
            style: TextStyle(
              fontSize: 12,
              color: AppColors.textSecondary,
            ),
          ),
        ],
      ),
    );
  }
}

class _StatusBadge extends StatelessWidget {
  final String status;
  const _StatusBadge({required this.status});

  @override
  Widget build(BuildContext context) {
    final color = AppColors.statusColor(status);
    final label = switch (status.toUpperCase()) {
      'RUNNING' => '실행 중',
      'COMPLETED' => '완료',
      'FAILED' => '실패',
      'CANCELLED' => '취소됨',
      _ => '대기',
    };

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: color.withOpacity(0.15),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (status.toUpperCase() == 'RUNNING')
            Container(
              width: 6,
              height: 6,
              margin: const EdgeInsets.only(right: 6),
              decoration: BoxDecoration(
                color: color,
                shape: BoxShape.circle,
              ),
            ),
          Text(
            label,
            style: TextStyle(
              color: color,
              fontSize: 12,
              fontWeight: FontWeight.w600,
            ),
          ),
        ],
      ),
    );
  }
}

class _StepRow extends StatelessWidget {
  final Map<String, dynamic> step;
  const _StepRow({required this.step});

  @override
  Widget build(BuildContext context) {
    final actionType = (step['actionType'] as String?) ?? '';
    final success = step['success'] as bool? ?? false;
    final reasoning = step['reasoning'] as String? ?? '';
    final targetText = step['targetText'] as String?;
    final stepNum = (step['step'] as num?)?.toInt() ?? 0;

    final Color color;
    if (actionType == 'DONE') {
      color = AppColors.statusCompleted;
    } else if (actionType == 'ERROR') {
      color = AppColors.stepError;
    } else if (success) {
      color = AppColors.stepSuccess;
    } else {
      color = AppColors.stepFailed;
    }

    final label = switch (actionType.toUpperCase()) {
      'CLICK' => 'TAP',
      'TYPE' => 'TYPE',
      'SCROLL' => 'SCROLL',
      'BACK' => 'BACK',
      'DONE' => 'DONE',
      'ERROR' => 'ERR',
      _ => actionType,
    };

    return Container(
      margin: const EdgeInsets.only(bottom: 6),
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(
        color: AppColors.background,
        borderRadius: BorderRadius.circular(10),
      ),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
            decoration: BoxDecoration(
              color: color.withOpacity(0.15),
              borderRadius: BorderRadius.circular(6),
            ),
            child: Text(
              label,
              style: TextStyle(
                color: color,
                fontSize: 10,
                fontWeight: FontWeight.bold,
              ),
            ),
          ),
          const SizedBox(width: 8),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Text(
                      'Step $stepNum',
                      style: const TextStyle(
                        fontSize: 11,
                        fontWeight: FontWeight.w600,
                        color: Colors.white,
                      ),
                    ),
                    if (targetText != null) ...[
                      Text(
                        '  →  $targetText',
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
                if (reasoning.isNotEmpty) ...[
                  const SizedBox(height: 2),
                  Text(
                    reasoning,
                    style: const TextStyle(
                      fontSize: 11,
                      color: AppColors.textMuted,
                    ),
                    maxLines: 2,
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
